package lkuich.controlhubclient

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        app = applicationContext as ControlHubApplication
        mAuth = FirebaseAuth.getInstance()

        findViewById<SignInButton>(R.id.sign_in_button).setOnClickListener({
            googleSignin()
        })
    }

    override fun onStart() {
        super.onStart()
        val currentUser: FirebaseUser? = mAuth?.currentUser
        if (currentUser != null)
            showHome(currentUser)
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

                    val factory: LayoutInflater = layoutInflater
                    factory.inflate(R.layout.activity_canvas, null)

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
        app?.getInstance()!!.database?.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                // Show loading
                findViewById<TextView>(R.id.loading).visibility = View.VISIBLE
                findViewById<SignInButton>(R.id.sign_in_button).isEnabled = false

                val selectedLayout: Any? = dataSnapshot.child("selectedLayout").value
                if (selectedLayout == null) {
                    // No layout exists, create it!
                    app?.getInstance()!!.database?.child("selectedLayout")?.setValue(
                            app?.getInstance()!!.selectedLayout)

                    val defaultControls = mutableListOf(
                        FirebaseControls(R.id.left_directional_pad.toString(), "0", "0"),
                        FirebaseControls(R.id.right_directional_pad.toString(), "0", "0"),
                        FirebaseControls(R.id.buttons.toString(), "0", "0"),
                        FirebaseControls(R.id.dpad.toString(), "0", "0"),
                        FirebaseControls(R.id.left_shoulder.toString(), "0", "0"),
                        FirebaseControls(R.id.right_shoulder.toString(), "0", "0")
                    )

                    val layouts = mutableListOf<FirebaseLayout>()
                    app?.getInstance()!!.layoutNames.forEach {
                        layouts.add(FirebaseLayout(it, defaultControls))
                    }
                    app?.getInstance()!!.database?.child("layouts")?.setValue(layouts)
                }
                app?.getInstance()?.selectedLayout = selectedLayout.toString()

                app?.getInstance()?.firebaseLayouts = dataSnapshot.child("layouts")
                app?.getInstance()?.firebaseLayouts!!.children.forEach {
                    val name = it.child("name").value.toString()
                    val controls = mutableListOf<FirebaseControls>()
                    it.child("controls").children.forEach { config ->
                        config.value
                        controls.add(FirebaseControls(
                                config.child("id").value.toString(),
                                config.child("x").value.toString(),
                                config.child("y").value.toString()
                        ))
                    }
                    val firebaseLayout = FirebaseLayout(name, controls)
                    app?.getInstance()!!.cachedLayouts.add(firebaseLayout)
                }

                val intent = Intent(applicationContext, HomeActivity::class.java)
                startActivity(intent)
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