package lkuich.controlhubclient

import android.app.Application
import android.content.res.Configuration
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import java.util.*


class ControlLayout(val name: String, val controls: MutableList<ElementPosition>)
data class FirebaseControls(val tag: String, val key: MutableList<Int>, val x: Float, val y: Float) // Mapped by firebase
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
            FirebaseControls("left_directional_pad", mutableListOf(0x11), 402f, 278f), // ctrl
            FirebaseControls("right_directional_pad", mutableListOf(0x56), 1407f, 592f), // v
            FirebaseControls("buttons", mutableListOf(0x20, 0x11, 0x32, 0x52), 1125f, 182f), // A, B, Y, X
            FirebaseControls("dpad", mutableListOf(0x03), 129f, 663f),
            FirebaseControls("left_shoulder", mutableListOf(0x03, 0x02), 12f, 20f), // Left Bumper / Left Trigger
            FirebaseControls("right_shoulder", mutableListOf(0x03, 0x01), 1640f, 20f) // Right Bumper / Right Trigger
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
}