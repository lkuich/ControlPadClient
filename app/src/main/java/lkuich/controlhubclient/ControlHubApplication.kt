package lkuich.controlhubclient

import android.app.Application
import android.content.res.Configuration
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import java.util.*


class ControlLayout(val name: String, val controls: MutableList<ElementPosition>)
data class FirebaseControls(val id: String, val key: MutableList<String>, val x: String, val y: String) // Mapped by firebase
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
            FirebaseControls(R.id.left_directional_pad.toString(), mutableListOf("0x11"), "402", "278"), // ctrl
            FirebaseControls(R.id.right_directional_pad.toString(), mutableListOf("0x56"), "1407", "592"), // v
            FirebaseControls(R.id.buttons.toString(), mutableListOf("0x20", "0x11", "0x32", "0x52"), "1125", "182"), // A, B, Y, X
            FirebaseControls(R.id.dpad.toString(), mutableListOf("0x03"), "129", "663"),
            FirebaseControls(R.id.left_shoulder.toString(), mutableListOf("0x03", "0x01"), "12", "20"), // Left Bumper / Left Trigger
            FirebaseControls(R.id.right_shoulder.toString(), mutableListOf("0x03", "0x02"), "1640", "20") // Right Bumper / Right Trigger
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