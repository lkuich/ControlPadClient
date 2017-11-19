package lkuich.controlhubclient

import android.annotation.SuppressLint
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import android.view.WindowManager
import android.content.pm.ActivityInfo
import android.app.Activity
import android.os.AsyncTask
import android.text.TextUtils
import android.util.Log
import android.view.MotionEvent
import android.widget.ImageView
import com.google.common.primitives.UnsignedInteger
import io.grpc.ManagedChannel
import io.grpc.ManagedChannelBuilder
import java.io.PrintWriter
import java.io.StringWriter
import java.util.concurrent.TimeUnit

import service.RobotGrpc
import service.Services

class Axis (var x: Float, var y: Float) {

    fun keyCode(key: Char): Int {
        when (key.toLowerCase()) {
            'w' -> return 0x57
            'a' -> return 0x44
            's' -> return 0x53
            'd' -> return 0x41
        }
        return 0x03
    }

    fun greatestKey(): Int {
        if (y > 0 && x > 0) { // Up, right
            if (x > y)
                return keyCode('w')
            else if (y > x)
                return keyCode('d')
        }
        if (y > 0 && x < 0) { // Up, left
            if (x *-1 > y)
                return keyCode('s')
            else if (y > x *-1)
                return keyCode('d')
        }
        if (y < 0 && x > 0) { // Down, right
            if (x > y *-1)
                return keyCode('w')
            else if (y *-1 > x)
                return keyCode('a')
        }
        if (y < 0 && x < 0) { // Down, left
            if (x < y)
                return keyCode('s')
            else if (y < x)
                return keyCode('a')
        }

        /*else if (x < 0) {
            if (x > y)
                return keyCode('d')
            else
                return keyCode('w')
        } else if (y < 0) {
            if (x > 0) {
                if (x > y)
                    return keyCode('a')
                else
                    return keyCode('s')
            } else if (x < 0) {
                if (x > y)
                    return keyCode('d')
                else
                    return keyCode('s')
            }
        }

        if (x > 0) {
            if (y > 0) {
                if (y > x)
                    return keyCode('w')
                else if (x > y)
                    return keyCode('d')
            } else {
                if (y > x)
                    return keyCode('w')
                else if (x > y)
                    return keyCode('d')
            }
        } else if (x < 0) {
            if (y > 0) {

            } else {

            }
        }*/

        return 0x03
    }

    private fun findMax(vararg vals: Float): Float {
        var max = java.lang.Float.NEGATIVE_INFINITY
        for (d in vals)
            if (d > max) max = d
        return max
    }

    private fun findMin(vararg vals: Float): Float {
        var min = java.lang.Float.POSITIVE_INFINITY
        for (d in vals)
            if (d < min) min = d
        return min
    }

}

class CanvasActivity : AppCompatActivity() {
    private var previouslySent: Int = 0x03

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_canvas)
        // supportActionBar?.setDisplayHomeAsUpEnabled(true)

        // Task = GrpcTask()

        fullscreen()
        analogTouchEvents()
    }

    fun analogTouchEvents() {
        val analog = findViewById<ImageView>(R.id.left_analog_inner)
        var startCoords: FloatArray? = null

        analog.setOnTouchListener(
                View.OnTouchListener { v, evt ->
            when (evt.action) {
                MotionEvent.ACTION_DOWN -> {
                    startCoords = floatArrayOf(analog.x, analog.y)
                }
                MotionEvent.ACTION_MOVE -> {
                    val evtX = evt.rawX
                    val evtY = evt.rawY

                    Log.v("X", evtX.toString())
                    Log.v("Y", evtY.toString())

                    analog.x = evtX
                    analog.y = evtY

                    val relativeX = startCoords!![0] - evtX
                    val relativeY = startCoords!![1] - evtY

                    val axis = Axis(relativeY, relativeX)

                    // val sensitivity = 1

                    val toSend = axis.greatestKey()
                    if (previouslySent != toSend)
                        sendKey(toSend)
                }
                MotionEvent.ACTION_UP -> {
                    analog.x = startCoords!![0]
                    analog.y = startCoords!![1]

                    sendKey(0x03)
                }
            }
            return@OnTouchListener true
        })
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
    }

    fun sendKey(keyCode: Int) {
        val task = GrpcTask(Services.Key.newBuilder().setId(keyCode).build())
        task.execute()

        previouslySent = keyCode
    }
}

private class GrpcTask(private var keyToSend: Services.Key) : AsyncTask<Void, Void, String>() {
    private val host: String = "10.78.78.130"
    private val port: Int = 50051

    private var channel: ManagedChannel? = null
    private var stub: RobotGrpc.RobotBlockingStub? = null

    // public var keyToSend: Services.Key? = null

    override fun onPreExecute() {
        // Can get the command to send here first
        channel = ManagedChannelBuilder.forAddress(host, port)
                .usePlaintext(true)
                .build()
        stub = RobotGrpc.newBlockingStub(channel)
    }

    override fun doInBackground(vararg nothing: Void): String {
        try {
            // val mouseCoords = Services.MouseCoords.newBuilder().setX(x).setY(y)
            val response = stub?.pressKey(keyToSend)
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
