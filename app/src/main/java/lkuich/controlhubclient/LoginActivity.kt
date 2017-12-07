package lkuich.controlhubclient

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.SignInButton
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.tasks.Task
import com.google.firebase.auth.*
import com.google.firebase.database.*

class LoginActivity : Activity() {
    private val RC_SIGN_IN = 9001
    private var app : ControlHubApplication? = null
    private var mAuth: FirebaseAuth? = null
    private var signinButton: SignInButton? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        app = applicationContext as ControlHubApplication
        mAuth = FirebaseAuth.getInstance()

        signinButton = findViewById(R.id.sign_in_button)
        signinButton?.isEnabled = false
        signinButton?.setOnClickListener({
            googleSignin()
        })
        findViewById<LinearLayout>(R.id.login_progress).visibility = View.INVISIBLE
    }

    override fun onStart() {
        super.onStart()
        val user: FirebaseUser? = mAuth?.currentUser
        if (user != null) {
            showHome(user)
        }
        else
            signinButton?.isEnabled = true
    }

    fun googleSignin() {
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestEmail()
                .requestIdToken(getString(R.string.web_client_id))
                .build()

        // Build a GoogleSignInClient with the options specified by gso.
        val mGoogleSignInClient = GoogleSignIn.getClient(this, gso)
        val signInIntent = mGoogleSignInClient.signInIntent
        startActivityForResult(signInIntent, RC_SIGN_IN)
    }

    private fun firebaseAuthWithGoogle(acct: GoogleSignInAccount) {
        val credential: AuthCredential = GoogleAuthProvider.getCredential(acct.idToken, null)
        mAuth?.signInWithCredential(credential)
            ?.addOnCompleteListener({ task: Task<AuthResult> ->
                if (task.isSuccessful) {
                    // Sign in success, update UI with the signed-in user's information
                    val user: FirebaseUser = mAuth?.currentUser!!

                    // Load persisted values, boot up the layout manager if there are none saved
                    showHome(user)
                } else {
                    // If sign in fails, display a message to the user.
                    Log.w("error", "couldn't auth firebase")
                }
            })
    }

    fun showHome(user: FirebaseUser) {
        app?.getInstance()!!.database = FirebaseDatabase.getInstance().reference.child(user.uid)
        app?.getInstance()!!.database?.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                // Show loading
                findViewById<LinearLayout>(R.id.login_progress).visibility = View.VISIBLE
                findViewById<SignInButton>(R.id.sign_in_button).isEnabled = false

                val selectedLayout: Any? = dataSnapshot.child("selectedLayout").value
                if (selectedLayout == null) {
                    // No layout exists, create it!
                    app?.getInstance()!!.database?.child("selectedLayout")?.setValue(
                            app?.getInstance()!!.selectedLayout)

                    val defaultControls = mutableListOf(
                        FirebaseControls(R.id.left_directional_pad.toString(), mutableListOf("0x11"), "402", "278"), // ctrl
                        FirebaseControls(R.id.right_directional_pad.toString(), mutableListOf("0x56"), "1407", "592"), // v
                        FirebaseControls(R.id.buttons.toString(), mutableListOf("0x20", "0x11", "0x32", "0x52"), "1125", "182"), // A, B, Y, X
                        FirebaseControls(R.id.dpad.toString(), mutableListOf("0"), "129", "663"),
                        FirebaseControls(R.id.left_shoulder.toString(), mutableListOf("0", "0x0002"), "12", "20"), // Left Bumper / Left Trigger
                        FirebaseControls(R.id.right_shoulder.toString(), mutableListOf("0", "0x0008"), "1640", "20") // Right Bumper / Right Trigger
                    )

                    val layouts = mutableListOf<FirebaseLayout>()
                    app?.getInstance()!!.layoutNames.forEach {
                        layouts.add(FirebaseLayout(it, defaultControls))
                    }
                    app?.getInstance()!!.database?.child("layouts")?.setValue(layouts)
                } else {
                    app?.getInstance()?.selectedLayout = selectedLayout.toString()

                    app?.getInstance()?.firebaseLayouts = dataSnapshot.child("layouts")
                    app?.getInstance()?.firebaseLayouts!!.children.forEach {
                        val name = it.child("name").value.toString()
                        val controls = mutableListOf<FirebaseControls>()
                        it.child("controls").children.forEach { config ->
                            config.value
                            controls.add(FirebaseControls(
                                    config.child("id").value.toString(),
                                    config.child("key").value as MutableList<String>,
                                    config.child("x").value.toString(),
                                    config.child("y").value.toString()
                            ))
                        }
                        val firebaseLayout = FirebaseLayout(name, controls)
                        app?.getInstance()!!.cachedLayouts.add(firebaseLayout)
                    }

                    if (app?.getInstance()!!.cachedLayouts.size > 0 && !app?.getInstance()?.homeLoaded!!) {
                        // app?.getInstance()!!.database?.removeEventListener(this)
                        val intent = Intent(applicationContext, HomeActivity::class.java)
                        startActivity(intent)
                    }
                }
            }

            override fun onCancelled(databaseError: DatabaseError) {
                println("loadPost:onCancelled ${databaseError.toException()}")
            }
        })
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent) {
        super.onActivityResult(requestCode, resultCode, data)

        // Result returned from launching the Intent from GoogleSignInClient.getSignInIntent(...);
        if (requestCode == RC_SIGN_IN) {
            // The Task returned from this call is always completed, no need to attach
            // a listener.
            val task: Task<GoogleSignInAccount> = GoogleSignIn.getSignedInAccountFromIntent(data)
            try {
                // Google Sign In was successful, authenticate with Firebase
                val account: GoogleSignInAccount = task.getResult(ApiException::class.java)
                firebaseAuthWithGoogle(account)
            } catch (e: ApiException) {
                // Google Sign In failed, update UI appropriately
                Log.w("error", e.message)
            }
        }
    }
}