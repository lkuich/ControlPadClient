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
import android.R.attr.countDown
import io.grpc.stub.StreamObserver
import java.util.*
import java.util.concurrent.CountDownLatch


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
                return keyCode('d')
            else if (y > x)
                return keyCode('w')
        }
        if (y > 0 && x < 0) { // Up, left
            if (x *-1 > y)
                return keyCode('a')
            else if (y > x *-1)
                return keyCode('w')
        }
        if (y < 0 && x > 0) { // Down, right
            if (x > y *-1)
                return keyCode('d')
            else if (y *-1 > x)
                return keyCode('s')
        }
        if (y < 0 && x < 0) { // Down, left
            if (x < y)
                return keyCode('a')
            else if (y < x)
                return keyCode('s')
        }

        return 0x03
    }
}

class CanvasActivity : AppCompatActivity() {
    private var mouseStream: MouseStream? = null
    private var keyboardStream: KeyboardStream? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_canvas)
        // supportActionBar?.setDisplayHomeAsUpEnabled(true)
        fullscreen()

        val stub = createStub()
        mouseStream = MouseStream(stub)
        keyboardStream = KeyboardStream(stub)

        analogTouchEvents(R.id.left_analog_inner, { x, y ->
            val axis = Axis(x, y)

            // val sensitivity = 1
            sendKey(axis.greatestKey())
        })

        analogTouchEvents(R.id.right_analog_inner, {x, y ->
            // Log.v("mouse:", x.toString() + "," + y.toString())
            sendMouse(x.toInt(), y.toInt())
        })
    }

    fun createStub(): RobotGrpc.RobotStub {
        val host: String = "10.78.78.130"
        val port: Int = 50051

        val channel: ManagedChannel = ManagedChannelBuilder.forAddress(host, port).usePlaintext(true).build()
        return RobotGrpc.newStub(channel)
    }

    // Replace bool with function
    fun analogTouchEvents(id: Int, onMove: (relativeX: Float, relativeY: Float) -> Unit) {
        val analog = findViewById<ImageView>(id)
        var analogStartCoords: FloatArray? = null
        var startCoords: FloatArray? = null

        analog.setOnTouchListener(
                View.OnTouchListener { v, evt ->
            when (evt.action) {
                MotionEvent.ACTION_DOWN -> {
                    analogStartCoords = floatArrayOf(analog.x, analog.y)
                    startCoords = floatArrayOf(evt.rawX, evt.rawY)
                }
                MotionEvent.ACTION_MOVE -> {
                    val evtX = evt.rawX
                    val evtY = evt.rawY

                    analog.x = evtX - analog.width / 2
                    analog.y = evtY - analog.height / 2

                    val relativeX = startCoords!![0] - evtX
                    val relativeY = startCoords!![1] - evtY

                    onMove(relativeX, relativeY)
                }
                MotionEvent.ACTION_UP -> {
                    analog.x = analogStartCoords!![0]
                    analog.y = analogStartCoords!![1]

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
        if (keyboardStream?.lastSent != keyCode)
            keyboardStream?.sendKey(keyCode)
    }

    fun sendMouse(x: Int, y: Int) {
        val concat = x.toString() + "," + y.toString()
        if (mouseStream?.lastSent != concat)
            mouseStream?.moveMouse(x, y)
    }
}

private class KeyboardStream(stub: RobotGrpc.RobotStub) : GrpcStream() {
    var lastSent: Int = 0

    init {
        keyboardRequestObserver = stub.pressKey(responseObserver)
    }

    fun sendKey(keyCode: Int) {
        try {
            val request = Services.Key.newBuilder().setId(keyCode).build()
            keyboardRequestObserver?.onNext(request) // Sends the coords
        } catch (e: RuntimeException) {
            // Cancel RPC
            responseObserver?.onError(e)
            throw e
        }
        // requestObserver?.onCompleted()

        if (failed != null) {
            throw RuntimeException(failed)
        }

        lastSent = keyCode
    }

    override fun onResponseNext(response: Services.Response) {
        // Response
    }

    override fun onResponseError(t: Throwable) {
        // Error
    }

    override fun onResponseCompleted() {
        // Complete
    }
}

private class MouseStream(stub: RobotGrpc.RobotStub) : GrpcStream() {
    var lastSent: String = ""

    init {
        mouseRequestObserver = stub.moveMouse(responseObserver)
    }

    fun moveMouse(x: Int, y: Int) {
        try {
            val request = Services.MouseCoords.newBuilder().setX(x).setY(y).build()
            mouseRequestObserver?.onNext(request) // Sends the coords
        } catch (e: RuntimeException) {
            // Cancel RPC
            responseObserver?.onError(e)
            throw e
        }
        // requestObserver?.onCompleted()

        if (failed != null) {
            throw RuntimeException(failed)
        }
        lastSent = x.toString() + "," + y.toString();
    }

    override fun onResponseNext(response: Services.Response) {
        // Response
    }

    override fun onResponseError(t: Throwable) {
        // Error
    }

    override fun onResponseCompleted() {
        // Complete
    }
}

abstract private class GrpcStream {
    abstract fun onResponseNext(response: Services.Response)
    abstract fun onResponseError(t: Throwable)
    abstract fun onResponseCompleted()

    protected var responseObserver: StreamObserver<Services.Response>? = null
    protected var mouseRequestObserver: StreamObserver<Services.MouseCoords>? = null
    protected var keyboardRequestObserver: StreamObserver<Services.Key>? = null

    protected var failed: Throwable? = null

    init {
        responseObserver = object : StreamObserver<Services.Response> {
            override fun onNext(response: Services.Response) {
                onResponseNext(response)
            }

            override fun onError(t: Throwable) {
                onResponseError(t)
            }

            override fun onCompleted() {
                onResponseCompleted()
            }
        }
    }
}

private abstract class GrpcTask : AsyncTask<Void, Void, String>() {
    private val host: String = "10.78.78.130"
    private val port: Int = 50051

    private var channel: ManagedChannel? = null
    protected var stub: RobotGrpc.RobotBlockingStub? = null

    abstract fun commands(): String

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
            return commands()
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

/*
private class MouseTask(private var mousCoords: Services.MouseCoords) : GrpcTask() {
    override fun commands(): String {
        val response = stub?.moveMouse(mousCoords)
        return response?.received.toString()
    }
}

private class KeyboardTask(private var keyToSend: Services.Key) : GrpcTask() {
    override fun commands(): String {
        val response = stub?.pressKey(keyToSend)
        return response?.received.toString()
    }
}
*/