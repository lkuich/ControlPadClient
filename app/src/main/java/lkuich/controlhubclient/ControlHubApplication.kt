package lkuich.controlhubclient

import android.app.Activity
import android.app.AlertDialog
import android.app.Application
import android.content.Context
import android.content.IntentFilter
import android.content.res.Configuration
import android.net.ConnectivityManager
import android.net.NetworkInfo
import android.net.wifi.WifiManager
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseReference

class ControlLayout(val name: String, val controls: MutableList<ElementPosition>)
data class FirebaseControls(val tag: String, val key: MutableList<String>, val x: Float, val y: Float) // Mapped by firebase
data class FirebaseLayout(val name: String, val controls: MutableList<FirebaseControls>) // Mapped by firebase

class ControlHubApplication : Application() {
    private var singleton: ControlHubApplication? = null

    var layoutNames = arrayOf("Default", "Custom 1", "Custom 2")
    val cachedLayouts = mutableListOf<FirebaseLayout>()
    var layouts = mutableListOf<ControlLayout>()
    var selectedLayout: String = "Default"
    var database: DatabaseReference? = null
    var firebaseLayouts: DataSnapshot? = null
    var homeLoaded: Boolean = false
    var firstRun: Boolean = true
    var lastIp: String = ""
    val defaultControls = mutableListOf( //TODO: Use strings
            FirebaseControls("left_directional_pad", mutableListOf("shift", "true"), 20.9375f, 25.74074074074074f), // ctrl
            FirebaseControls("right_directional_pad", mutableListOf("ctrl", "true"), 73.28125f, 54.81481481481482f), // v
            FirebaseControls("buttons", mutableListOf(" ", "left,ctrl", "2", "r"), 58.59375f, 16.85185185185185f), // A, B, Y, X
            FirebaseControls("dpad", mutableListOf("down", "right", "up", "left"), 6.71875f, 61.38888888888889f),
            FirebaseControls("left_shoulder", mutableListOf("cancel", "right,click"), 0.625f, 1.8518518518518516f), // Left Bumper / Left Trigger
            FirebaseControls("right_shoulder", mutableListOf("cancel", "left,click"), 81.25f, 1.8518518518518516f), // Right Bumper / Right Trigger
            FirebaseControls("menu_buttons", mutableListOf("tab", "esc"), 40.364583f, 87.96296296296296f) // Select / Start
    )

    fun getInstance(): ControlHubApplication? {
        return singleton
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
    }

    override fun onCreate() {
        super.onCreate()
        singleton = this
    }

    override fun onLowMemory() {
        super.onLowMemory()
    }

    override fun onTerminate() {
        super.onTerminate()
    }

    fun checkNetwork(activity: Activity) {
        if (!isWifiConnected()) {
            val builder: AlertDialog.Builder = AlertDialog.Builder(activity)
            builder.setTitle("Error")
                    .setMessage(getString(R.string.no_wifi_error))
                    .setNeutralButton(android.R.string.ok) { _, _ ->
                        activity.finish()
                    }
                .show()
        }
    }

    private fun isWifiConnected(): Boolean {
        val wifi = applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
        val networkInfo: NetworkInfo
        if (wifi.isWifiEnabled) {
            val connectivityManager = applicationContext.getSystemService(
                    Context.CONNECTIVITY_SERVICE) as ConnectivityManager
            networkInfo = connectivityManager.getNetworkInfo(ConnectivityManager.TYPE_WIFI)
        } else
            return false

        return networkInfo.isConnected
    }

    fun isUsbConnected(): Boolean {
        val intent = applicationContext.registerReceiver(null, IntentFilter(
                "android.hardware.usb.action.USB_STATE")
        )
        return intent != null && intent.extras!!.getBoolean("connected")
    }
}