package lkuich.xdroid

import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.provider.Settings

// TODO: Maybe use?
class UsbActivity : Activity() {
    private val usbDaemonRunning = true

    private fun showUsbPrompt(prompt: AlertDialog.Builder) {
        prompt.setTitle("USB Debugging Disabled")
        prompt.setCancelable(false)
        prompt.setMessage(
            "You must enable developer options, then USB debugging." +
            "\n\n" + "Would you like to go there now?"
        )
        prompt.setPositiveButton("Yes") { dialogInterface, id ->
            startActivityForResult(
                    Intent(Settings.ACTION_APPLICATION_DEVELOPMENT_SETTINGS),0)
        }
        prompt.setNegativeButton( "No") { dialogInterface, id ->
            dialogInterface.cancel()
        }
        prompt.create().show()
    }

    private fun startUsbDaemon() {
        Thread().run {
            while (usbDaemonRunning) {
                try {
                    Thread.sleep(1000) // TODO: Increase polling if power is connected -
                    runOnUiThread {

                    }
                } catch (e: InterruptedException) {
                    e.printStackTrace()
                }

            }
        }
    }
}