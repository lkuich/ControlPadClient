package lkuich.controlhubclient

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.drawable.BitmapDrawable
import android.view.View
import android.view.MotionEvent
import android.widget.ImageView
import android.widget.RelativeLayout
import io.grpc.ManagedChannel
import io.grpc.ManagedChannelBuilder
import service.StandardInputGrpc
import service.Services


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

class CanvasActivity : BaseCanvasActivity() {
    private var mouseStream: MouseStream? = null
    private var keyboardStream: KeyboardStream? = null

    override fun onCreate() {
        app?.getInstance()?.layouts?.first { controlLayout -> controlLayout.name == app!!.getInstance()?.selectedLayout }?.controls?.forEach { control ->
            // Move controls into position
            control.move(findViewById(control.elm.id))

            // Get keymaps
            when (control.elm.id) {
                R.id.buttons -> {
                    button(R.id.a_button, control.keys[0])
                    button(R.id.b_button, control.keys[1])
                    button(R.id.y_button, control.keys[2])
                    button(R.id.x_button, control.keys[3])
                }
                R.id.left_shoulder -> {
                    button(R.id.lb, control.keys[0])
                    button(R.id.lt, control.keys[1])
                }
                R.id.right_shoulder -> {
                    button(R.id.rb, control.keys[0])
                    button(R.id.rt, control.keys[1])
                }
                R.id.left_directional_pad -> {
                    analogStick(R.id.left_analog_inner, { x, y ->
                        val axis = Axis(x, y)

                        // val sensitivity = 1
                        val keys = axis.greatestKey()
                        sendKey(keys[0], if (keys.size > 1) keys[1] else 0)
                    }, {
                        sendKey(control.keys[0])
                    }, true)
                }
                R.id.right_directional_pad -> {
                    analogStick(R.id.right_analog_inner, {x, y ->
                        // Log.v("mouse:", x.toString() + "," + y.toString())
                        sendMouse(x.toInt(), y.toInt())
                    }, {
                        sendKey(control.keys[0])
                    }, false)
                }
            }
        }

        val IP = intent.getStringExtra(HomeActivity.IP)
        val stub = createStub(IP)
        mouseStream = MouseStream(stub, { imageRecieved(it) })
        keyboardStream = KeyboardStream(stub, { imageRecieved(it) })

        // Send disconnect
    }

    fun imageRecieved(data: Services.ScreenshotData) {
        val view = findViewById<RelativeLayout>(R.id.mainContent)
        val byteArray = data.content.toByteArray()

        val bmp: Bitmap = BitmapFactory.decodeByteArray(byteArray, 0, byteArray.size)
        // ImageView image = (ImageView) findViewById(R.id.imageView1)
        val drawable = BitmapDrawable(resources, Bitmap.createScaledBitmap(bmp, 1920, 1080, false))

        view.background = drawable
    }

    fun createStub(ip: String): StandardInputGrpc.StandardInputStub {
        val host: String = ip
        val port: Int = 50051

        val channel: ManagedChannel = ManagedChannelBuilder.forAddress(host, port).usePlaintext(true).build()
        return StandardInputGrpc.newStub(channel)
    }

    fun button(id: Int, key: Int) {
        val button = findViewById<View>(id)
        button.setOnTouchListener(
            View.OnTouchListener { v, evt ->
                 when (evt.action) {
                    MotionEvent.ACTION_DOWN -> {
                        sendKey(key)
                    }
                    MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                        sendKey(0x03)
                    }
                }
                return@OnTouchListener true
            })
    }

    // Replace bool with function
    fun analogStick(id: Int, onMove: (relativeX: Float, relativeY: Float) -> Unit, onPressure: () -> Unit, sendCancel: Boolean) {
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
                    if (evt.pressure > 1.3)
                        onPressure()

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

private class KeyboardStream(stub: StandardInputGrpc.StandardInputStub, val screenshotDataRecieved: (data: Services.ScreenshotData) -> Unit) : GrpcStream() {
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
            keyboardRequestObserver?.onError(e)
            throw e
        }
        // requestObserver?.onCompleted()

        if (failed != null) {
            throw RuntimeException(failed)
        }

        lastSent = intArrayOf(firstKey, secondKey)
    }

    override fun onResponseNext(response: Services.ScreenshotData) {
        // Response
        screenshotDataRecieved(response)
    }

    override fun onResponseError(t: Throwable) {
        // Error
    }

    override fun onResponseCompleted() {
        // Complete
    }
}

private class MouseStream(stub: StandardInputGrpc.StandardInputStub, val screenshotDataRecieved: (data: Services.ScreenshotData) -> Unit) : GrpcStream() {
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
            mouseRequestObserver?.onError(e)
            throw e
        }
        // requestObserver?.onCompleted()

        if (failed != null) {
            throw RuntimeException(failed)
        }
        lastSent = x.toString() + "," + y.toString()
    }

    override fun onResponseNext(response: Services.ScreenshotData) {
        // Response
        screenshotDataRecieved(response)
    }

    override fun onResponseError(t: Throwable) {
        // Error
    }

    override fun onResponseCompleted() {
        // Complete
    }
}