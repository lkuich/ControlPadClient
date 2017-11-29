package lkuich.controlhubclient

import android.app.Application
import android.content.res.Configuration

class ControlHubApplication : Application() {
    private var singleton: ControlHubApplication? = null

    var layouts = mutableListOf<ControlLayout>()

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