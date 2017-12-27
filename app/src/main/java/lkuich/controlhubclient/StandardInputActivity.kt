package lkuich.controlhubclient

import android.support.v4.widget.DrawerLayout
import android.view.KeyEvent
import android.view.View
import android.view.MotionEvent
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.ImageView
import android.widget.ListView
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
                R.id.dpad -> {
                    button(R.id.left_button, "left")
                    button(R.id.right_button, "right")
                    button(R.id.up_button, "up")
                    button(R.id.down_button, "down")
                }
            }
        }

        val IP = intent.getStringExtra(HomeActivity.IP)
        val stub = createStub(IP)
        mouseStream = MouseStream(stub)
        keyboardStream = KeyboardStream(stub)

        // Send disconnect
    }

    fun singleKeyMap(key: String): Int = when (key) {
        "a" -> JKeyEvent.VK_A; "b" ->  JKeyEvent.VK_B
        "c" -> JKeyEvent.VK_C; "d" -> JKeyEvent.VK_D
        "e" -> JKeyEvent.VK_E; "f" -> JKeyEvent.VK_F
        "g" -> JKeyEvent.VK_G; "h" -> JKeyEvent.VK_H
        "i" -> JKeyEvent.VK_I; "j" -> JKeyEvent.VK_J
        "k" -> JKeyEvent.VK_K; "l" -> JKeyEvent.VK_L
        "m" -> JKeyEvent.VK_M; "n" -> JKeyEvent.VK_N
        "o" -> JKeyEvent.VK_O; "p" -> JKeyEvent.VK_P
        "q" -> JKeyEvent.VK_Q; "r" -> JKeyEvent.VK_R
        "s" -> JKeyEvent.VK_S; "t" -> JKeyEvent.VK_T
        "u" -> JKeyEvent.VK_U; "v" -> JKeyEvent.VK_V
        "w" -> JKeyEvent.VK_W; "x" -> JKeyEvent.VK_X
        "y" -> JKeyEvent.VK_Y; "z" -> JKeyEvent.VK_Z
        "1" -> JKeyEvent.VK_1; "2" -> JKeyEvent.VK_2
        "3" -> JKeyEvent.VK_3; "4" -> JKeyEvent.VK_4
        "5" -> JKeyEvent.VK_5; "6" -> JKeyEvent.VK_6
        "7" -> JKeyEvent.VK_7; "8" -> JKeyEvent.VK_8
        "9" -> JKeyEvent.VK_9; "0" -> JKeyEvent.VK_0
        "left" -> JKeyEvent.VK_LEFT; "right" -> JKeyEvent.VK_RIGHT
        "up" -> JKeyEvent.VK_UP; "down" -> JKeyEvent.VK_DOWN
        "shift" -> JKeyEvent.VK_SHIFT
        "ctrl" -> JKeyEvent.VK_CONTROL
        "alt" -> JKeyEvent.VK_ALT
        else -> JKeyEvent.VK_UNDEFINED
    }

    fun parseKey(k: String): Int {
        if (k.isEmpty())
            throw Exception("Key cannot be empty")
        val key = k.toLowerCase()

        val split = key.split(',')
        when (split.size) {
            1 -> { // There's no comma, no side spec
                // No split, just treat as single char
                val char = key[0].toString() // make sure we have the 1 char
                return singleKeyMap(char)
            }
            2 -> {
                val side = split[0]
                val value = split[1]

                if (side == "left") {
                    when (value) {
                        "ctrl" -> return JKeyEvent.VK_CONTROL_LEFT
                        "alt" -> return JKeyEvent.VK_ALT_LEFT
                        "click" -> return JKeyEvent.LBUTTON
                    }
                } else { // Right
                    when (value) {
                        "ctrl" -> return JKeyEvent.VK_CONTROL_RIGHT
                        "alt" -> return JKeyEvent.VK_ALT_RIGHT
                        "click" -> return JKeyEvent.RBUTTON
                    }
                }
            }
        }
        return JKeyEvent.VK_UNDEFINED
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
        val analog = findViewById<ImageView>(id)
        var analogStartCoords: FloatArray? = null
        var analogCoords: FloatArray? = null
        var startCoords: FloatArray? = null

        analog.setOnTouchListener(
                View.OnTouchListener { v, evt ->
            when (evt.action) {
                MotionEvent.ACTION_DOWN -> {
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
        val i = 0
    }

    override fun onResponseError(t: Throwable) {
        // Error
    }

    override fun onResponseCompleted() {
        // Complete
    }
}