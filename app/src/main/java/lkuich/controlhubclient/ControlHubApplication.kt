package lkuich.controlhubclient

import android.app.Application
import android.content.res.Configuration
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import java.util.*


class ControlLayout(val name: String, val controls: MutableList<ElementPosition>)
data class FirebaseControls(val id: String, val x: String, val y: String) // Mapped by firebase
data class FirebaseLayout(val name: String, val controls: MutableList<FirebaseControls>) // Mapped by firebase

class ControlHubApplication : Application() {
    private var singleton: ControlHubApplication? = null

    var layoutNames = arrayOf("Default", "Custom 1", "Custom 2")
    val cachedLayouts = mutableListOf<FirebaseLayout>()
    var layouts = mutableListOf<ControlLayout>()
    var selectedLayout: String = "Default"
    var database: DatabaseReference? = null
    var firebaseLayouts: DataSnapshot? = null

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