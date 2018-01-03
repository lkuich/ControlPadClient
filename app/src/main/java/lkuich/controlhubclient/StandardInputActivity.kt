package lkuich.controlhubclient

import android.view.View
import android.view.MotionEvent
import android.widget.*
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

class StandardInputActivity : BaseCanvasActivity() {
    private var mouseStream: MouseStream? = null
    private var keyboardStream: KeyboardStream? = null

    override fun onCreate() {
        app?.getInstance()?.layouts?.first { controlLayout -> controlLayout.name == app!!.getInstance()?.selectedLayout }?.controls?.forEach { control ->
            val drawerItems = resources.getStringArray(R.array.config_options_live)
            val mDrawerList = findViewById<ListView>(R.id.left_drawer)
            mDrawerList?.adapter = ArrayAdapter<String>(this, R.layout.drawer_list_item, drawerItems)
            mDrawerList?.setOnItemClickListener({ _: AdapterView<*>, _: View, position: Int, _: Long ->
                when(position) {
                    0 -> { // Done
                        finish()
                    }
                }
            })

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
                    val parsedKey: Int = parseKey(control.keys[0])

                    analogStick(R.id.left_analog_inner, { x, y ->
                        val axis = Axis(x, y)

                        val keys = axis.greatestKey()
                        sendKey(keys[0], if (keys.size > 1) keys[1] else 0)
                    }, {

                        sendKey(parsedKey)
                    }, true)
                }
                R.id.right_directional_pad -> {
                    val parsedKey: Int = parseKey(control.keys[0])

                    analogStick(R.id.right_analog_inner, {x, y ->
                        sendMouse(x.toInt(), y.toInt())
                    }, {
                        sendKey(parsedKey)
                    }, false)
                }
                R.id.menu_buttons -> {
                    button(R.id.select, control.keys[0])
                    button(R.id.start, control.keys[1])
                }
            }
        }

        // Set dpad
        button(R.id.left_button, "left")
        button(R.id.right_button, "right")
        button(R.id.up_button, "up")
        button(R.id.down_button, "down")

        val IP = intent.getStringExtra(HomeActivity.IP)
        val stub = createStub(IP)
        mouseStream = MouseStream(stub)
        keyboardStream = KeyboardStream(stub)

        // Send disconnect
    }

    fun createStub(ip: String): StandardInputGrpc.StandardInputStub {
        val host: String = ip
        val port: Int = getString(R.string.grpc_port).toInt()

        val channel: ManagedChannel = ManagedChannelBuilder.forAddress(host, port).usePlaintext(true).build()
        return StandardInputGrpc.newStub(channel)
    }

    fun button(id: Int, key: String) {
        val parsedKey: Int = parseKey(key)

        val button = findViewById<View>(id)
        button.setOnTouchListener(
            View.OnTouchListener { v, evt ->
                when (evt.action) {
                    MotionEvent.ACTION_DOWN -> {
                        fullscreen()

                        sendKey(parsedKey)
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
        val analog = findViewById<RelativeLayout>(id)
        var analogStartCoords: FloatArray? = null
        var analogCoords: FloatArray? = null
        var startCoords: FloatArray? = null

        analog.setOnTouchListener(
                View.OnTouchListener { v, evt ->
            when (evt.action) {
                MotionEvent.ACTION_DOWN -> {
                    fullscreen()

                    analogStartCoords = floatArrayOf(v.x, v.y)
                    analogCoords = floatArrayOf(v.x - evt.rawX, v.y - evt.rawY)
                    startCoords = floatArrayOf(evt.rawX, evt.rawY)
                }
                MotionEvent.ACTION_MOVE -> {
                    if (evt.pressure > 1.3)
                        onPressure()

                    val evtX = evt.rawX
                    val evtY = evt.rawY

                    v.animate()
                            .x(evtX + analogCoords!![0])
                            .y(evtY + analogCoords!![1])
                            .setDuration(0)
                            .start()

                    val relativeX = startCoords!![0] - evtX
                    val relativeY = startCoords!![1] - evtY

                    onMove(relativeX, relativeY)
                }
                MotionEvent.ACTION_UP -> {
                    v.animate()
                            .x(analogStartCoords!![0])
                            .y(analogStartCoords!![1])
                            .setDuration(0)
                            .start()

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

private class KeyboardStream(stub: StandardInputGrpc.StandardInputStub) : GrpcStream() {
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

private class MouseStream(stub: StandardInputGrpc.StandardInputStub) : GrpcStream() {
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