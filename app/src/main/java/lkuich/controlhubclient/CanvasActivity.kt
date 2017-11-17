package lkuich.controlhubclient

import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import android.view.WindowManager
import android.content.pm.ActivityInfo
import android.app.Activity
import android.os.AsyncTask
import android.text.TextUtils
import io.grpc.ManagedChannel
import io.grpc.ManagedChannelBuilder
import java.io.PrintWriter
import java.io.StringWriter
import java.util.concurrent.TimeUnit

import service.RobotGrpc
import service.Services

class CanvasActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_canvas)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        fullscreen()

        sendGrpcMessage()
    }

    fun fullscreen() {
        // Don't dim display
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        // Set landscape
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
        window.decorView.systemUiVisibility = (View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                or View.SYSTEM_UI_FLAG_FULLSCREEN
                or View.SYSTEM_UI_FLAG_IMMERSIVE)

        setContentView(CanvasView(this)) //show the drawing view
    }

    fun sendGrpcMessage() {
        GrpcTask().execute()
    }
}

private class GrpcTask : AsyncTask<Void, Void, String>() {
    private val host: String = "172.16.101.18"
    private val port: Int = 50051

    private var channel: ManagedChannel? = null
    private var stub: RobotGrpc.RobotBlockingStub? = null

    override fun onPreExecute() {
        // Can get the command to send here first

        channel = ManagedChannelBuilder.forAddress(host, port)
                .usePlaintext(true)
                .build()
        stub = RobotGrpc.newBlockingStub(channel)
    }

    override fun doInBackground(vararg nothing: Void): String {
        try {
            val message = Services.Key.newBuilder().setId(0x34).build()

            val response = stub?.pressKey(message)
            return response?.received.toString()
        } catch (e: Exception) {
            val sw = StringWriter()
            val pw = PrintWriter(sw)
            e.printStackTrace(pw)
            pw.flush()
            return String.format("Failed... : %n%s", sw)
        }

    }

    override fun onPostExecute(result: String) {
        try {
            channel!!.shutdown().awaitTermination(1, TimeUnit.SECONDS)
        } catch (e: InterruptedException) {
            Thread.currentThread().interrupt()
        }

        result // TODO: This is the recieved message
    }
}
