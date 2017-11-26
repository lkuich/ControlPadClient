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

    fun keyCode(vararg keys: Char): IntArray {
        val out = mutableListOf<Int>()
        for (key in keys) {
            when (key.toLowerCase()) {
                'w' -> out.add(0x57)
                'a' -> out.add(0x44)
                's' -> out.add(0x53)
                'd' -> out.add(0x41)
            }
        }
        return out.toIntArray()
    }

    fun greatestKey(): IntArray {
        val min = 1
        val max = 4

        if (y > 0 && x > 0) { // Up, right
            if (x > y)
                if (x / y in min..max)
                    return keyCode('d','w')
                else
                    return keyCode('d')
            else if (y > x)
                if (y / x in min..max)
                    return keyCode('w','d')
                else
                    return keyCode('w')
        }
        if (y > 0 && x < 0) { // Up, left
            if (x *-1 > y)
                if (x *-1 / y in min..max)
                    return keyCode('a','w')
                else
                    return keyCode('a')
            else if (y > x *-1)
                if (y / x *-1 in min..max)
                    return keyCode('w','a')
                else
                    return keyCode('w')
        }
        if (y < 0 && x > 0) { // Down, right
            if (x > y *-1)
                if (x / y *-1 in min..max)
                    return keyCode('d', 's')
                else
                    return keyCode('d')
            else if (x < y *-1)
                if (y *-1 / x in min..max)
                    return keyCode('s', 'd')
                else
                    return keyCode('s')
        }
        if (y < 0 && x < 0) { // Down, left
            if (x < y)
                if (y *-1 / x *-1 in min..max)
                    return keyCode('a', 's')
                else
                    return keyCode('a')
            else if (y < x)
                if (x *-1 / y *-1 in min..max)
                    return keyCode('s', 'a')
                else
                    return keyCode('s')
        }

        return intArrayOf(0x03)
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

        analogStick(R.id.left_analog_inner, { x, y ->
            val axis = Axis(x, y)

            // val sensitivity = 1
            val keys = axis.greatestKey()
            sendKey(keys[0], if (keys.size > 1) keys[1] else 0)

            Log.v("y/x:", (y / x).toString())
        }, true)

        analogStick(R.id.right_analog_inner, {x, y ->
            // Log.v("mouse:", x.toString() + "," + y.toString())
            sendMouse(x.toInt(), y.toInt())
        }, false)

        button(R.id.a_button, 0x20) // Spacebar
        button(R.id.b_button, 0x11) // Ctrl
        button(R.id.x_button, 0x52) // R
        button(R.id.y_button, 0x32) // 2
    }

    fun createStub(): RobotGrpc.RobotStub {
        val host: String = getString(R.string.grpc_ip)
        val port: Int = 50051

        val channel: ManagedChannel = ManagedChannelBuilder.forAddress(host, port).usePlaintext(true).build()
        return RobotGrpc.newStub(channel)
    }

    fun button(id: Int, key: Int) {
        val button = findViewById<ImageView>(id)
        button.setOnTouchListener(
            View.OnTouchListener { v, evt ->
                when (evt.action) {
                    MotionEvent.ACTION_DOWN -> {
                        sendKey(key)
                    }
                    MotionEvent.ACTION_UP -> {
                        sendKey(0x03)
                    }
                }
                return@OnTouchListener true
            })
    }

    // Replace bool with function
    fun analogStick(id: Int, onMove: (relativeX: Float, relativeY: Float) -> Unit, sendCancel: Boolean) {
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

                    if (sendCancel)
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

    fun sendKey(firstKey: Int, secondKey: Int = 0) {
        if (keyboardStream?.lastSent?.get(0) != firstKey || keyboardStream?.lastSent?.get(1) != secondKey)
            keyboardStream?.sendKeys(firstKey, secondKey)
    }

    fun sendMouse(x: Int, y: Int) {
        val concat = x.toString() + "," + y.toString()
        if (mouseStream?.lastSent != concat)
            mouseStream?.moveMouse(x, y)
    }
}

private class KeyboardStream(stub: RobotGrpc.RobotStub) : GrpcStream() {
    var lastSent: IntArray = intArrayOf(0, 0)

    init {
        keyboardRequestObserver = stub.pressKey(responseObserver)
    }

    fun sendKeys(firstKey: Int, secondKey: Int = 0) {
        try {
            val request = Services.Key.newBuilder().setFirstId(firstKey).setSecondId(secondKey).build()
            keyboardRequestObserver?.onNext(request)
        } catch (e: java.lang.RuntimeException) {
            // Cancel RPC
            responseObserver?.onError(e)
            throw e
        }
        // requestObserver?.onCompleted()

        if (failed != null) {
            throw RuntimeException(failed)
        }

        lastSent = intArrayOf(firstKey, secondKey)
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