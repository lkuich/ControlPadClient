package lkuich.controlhubclient

import android.app.Activity
import android.app.Application
import android.content.Context
import android.content.IntentFilter
import android.content.res.Configuration
import android.net.ConnectivityManager
import android.net.NetworkInfo
import android.net.wifi.WifiManager
import android.os.Build
import android.support.v7.app.AlertDialog
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import java.util.*


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
    val defaultControls = mutableListOf(
            FirebaseControls("left_directional_pad", mutableListOf("ctrl"), 402f, 278f), // ctrl
            FirebaseControls("right_directional_pad", mutableListOf("v"), 1407f, 592f), // v
            FirebaseControls("buttons", mutableListOf(" ", "left,ctrl", "2", "r"), 1125f, 182f), // A, B, Y, X
            FirebaseControls("dpad", mutableListOf("down", "right", "up", "left"), 129f, 663f),
            FirebaseControls("left_shoulder", mutableListOf("cancel", "right,click"), 12f, 20f), // Left Bumper / Left Trigger
            FirebaseControls("right_shoulder", mutableListOf("cancel", "left,click"), 1560f, 20f) // Right Bumper / Right Trigger
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
                    .setNeutralButton(android.R.string.ok, { dialog, which ->
                        activity.finish()
                    })
                    .show()
        }
    }

    fun isWifiConnected(): Boolean {
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