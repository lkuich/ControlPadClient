package lkuich.controlhubclient

import android.view.MotionEvent
import android.view.View
import io.grpc.ManagedChannel
import io.grpc.ManagedChannelBuilder
import service.XboxButtonsGrpc
import service.Services
import android.widget.*
import androidx.core.view.MotionEventCompat

private object Xbox360Buttons
{
    val Up = 0x0001
    val Down = 0x0002
    val Left = 0x0004
    val Right = 0x0008
    val Start = 0x0010
    val Back = 0x0020
    val LeftThumb = 0x0040
    val RightThumb = 0x0080
    val LeftShoulder = 0x0100
    val RightShoulder = 0x0200
    val Guide = 0x0400
    val A = 0x1000
    val B = 0x2000
    val X = 0x4000
    val Y = 0x8000
}


class XboxActivity : BaseCanvasActivity() {
    private var xboxStream: XboxStream? = null

    override fun onTouchEvent(event: MotionEvent?): Boolean {
        val action = MotionEventCompat.getActionMasked(event)
        when (action) {
            MotionEvent.ACTION_DOWN -> {
            }
            MotionEvent.ACTION_MOVE -> {
            }
            MotionEvent.ACTION_UP -> {
            }
            MotionEvent.ACTION_CANCEL -> {
            }
            MotionEvent.ACTION_OUTSIDE -> {
            }
        }

        return super.onTouchEvent(event)
    }

    override fun onCreate() {
        val drawerItems = resources.getStringArray(R.array.config_options_live)
        val mDrawerList = findViewById<ListView>(R.id.left_drawer)
        mDrawerList?.adapter = ArrayAdapter<String>(this, R.layout.drawer_list_item, drawerItems)
        mDrawerList?.setOnItemClickListener { _: AdapterView<*>, _: View, position: Int, _: Long ->
            when(position) {
                0 -> { // Done
                    finish()
                }
            }
        }

        app?.getInstance()?.layouts?.first { controlLayout -> controlLayout.name == app!!.getInstance()?.selectedLayout }?.controls?.forEach { control ->
            control.move(findViewById(control.elm.id))
        }

        val IP = intent.getStringExtra(HomeActivity.IP)
        xboxStream = XboxStream(createStub(IP))
        analogStick(R.id.left_analog_inner, { x, y ->
            xboxStream?.leftThumbAxis(x.toShort(), y.toShort())
        }, {
            // Release
            xboxStream?.leftThumbAxis(0, 0)
            xboxStream?.setTrigger(Trigger.LEFT, 0)
        }, {
            // Pressure
            // TODO: xboxStream?.setTrigger(Trigger.LEFT, Short.MAX_VALUE.toInt())
            xboxStream?.pressButton(Xbox360Buttons.LeftThumb)
        }, {
            // Pressure up
            xboxStream?.depressButton(Xbox360Buttons.LeftThumb)
        })

        analogStick(R.id.right_analog_inner, { x, y ->
            xboxStream?.rightThumbAxis(x.toShort(), y.toShort())
        }, {
            // Release
            xboxStream?.rightThumbAxis(0, 0)
            xboxStream?.setTrigger(Trigger.RIGHT, 0)
        }, {
            // Pressure
            // TODO: Pull trigger: xboxStream?.setTrigger(Trigger.RIGHT, Short.MAX_VALUE.toInt())
            xboxStream?.pressButton(Xbox360Buttons.RightThumb) // R3
        }, {
            // Pressure up
            xboxStream?.depressButton(Xbox360Buttons.RightThumb) // R3
        })

        button(R.id.a_button, Xbox360Buttons.A)
        button(R.id.b_button, Xbox360Buttons.B)
        button(R.id.x_button, Xbox360Buttons.X)
        button(R.id.y_button, Xbox360Buttons.Y)
        button(R.id.lb, Xbox360Buttons.LeftShoulder)
        button(R.id.rb, Xbox360Buttons.RightShoulder)

        button(R.id.left_button, Xbox360Buttons.Left)
        button(R.id.right_button, Xbox360Buttons.Right)
        button(R.id.up_button, Xbox360Buttons.Up)
        button(R.id.down_button, Xbox360Buttons.Down)

        button(R.id.start, Xbox360Buttons.Start)
        button(R.id.select, Xbox360Buttons.Back)

        trigger(R.id.lt, Trigger.LEFT)
        trigger(R.id.rt, Trigger.RIGHT)

        // Send start config to server
        xboxStream?.pressButton(0x9001)
    }

    fun createStub(ip: String): XboxButtonsGrpc.XboxButtonsStub {
        val host: String = ip
        val port: Int = getString(R.string.grpc_port).toInt()

        val channel: ManagedChannel = ManagedChannelBuilder.forAddress(host, port).usePlaintext().build()
        return XboxButtonsGrpc.newStub(channel)
    }

    fun trigger(id: Int, side: Trigger) {
        val trigger = findViewById<ImageView>(id)
        trigger.setOnTouchListener(
            View.OnTouchListener { v, evt ->
                when (evt.action) {
                    MotionEvent.ACTION_DOWN -> {
                        fullscreen()

                        xboxStream?.setTrigger(side, Short.MAX_VALUE.toInt())
                    }
                    MotionEvent.ACTION_UP -> {
                        xboxStream?.setTrigger(side, Short.MIN_VALUE.toInt())
                    }
                }
                return@OnTouchListener true
            })
    }

    fun button(id: Int, key: Int) {
        val button = findViewById<View>(id)
        button.setOnTouchListener(
            View.OnTouchListener { v, evt ->
                when (evt.action) {
                    MotionEvent.ACTION_DOWN -> {
                        fullscreen()

                        pressButton(key)
                    }
                    MotionEvent.ACTION_UP -> {
                        depressButton(key)
                    }
                }
                return@OnTouchListener true
            })
    }

    // Replace bool with function
    fun analogStick(innerAnalogId: Int, onMove: (relativeX: Float, relativeY: Float) -> Unit, onRelease: () -> Unit, onPressure: () -> Unit, onPressureLifted: () -> Unit) {
        val analog = findViewById<RelativeLayout>(innerAnalogId)

        var analogStartCoords: FloatArray? = null
        var analogCoords: FloatArray? = null
        var startCoords: FloatArray? = null
        var pressureDown = false

        analog.setOnTouchListener(
                View.OnTouchListener { v, evt ->
            when (evt.action) {
                MotionEvent.ACTION_DOWN -> {
                    fullscreen()

                    analogStartCoords = floatArrayOf(analog.x, analog.y)
                    analogCoords = floatArrayOf(v.x - evt.rawX, v.y - evt.rawY)
                    startCoords = floatArrayOf(evt.rawX, evt.rawY)
                }
                MotionEvent.ACTION_MOVE -> {
                    if (evt.pressure > 1.3) {
                        onPressure()
                        pressureDown = true
                    } else if (pressureDown) {
                        pressureDown = false
                        onPressureLifted()
                    }

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

                    onRelease()
                }
            }
            return@OnTouchListener true
        })
    }

    fun pressButton(buttonCode: Int) {
        xboxStream?.pressButton(buttonCode)
    }

    fun depressButton(buttonCode: Int) {
        xboxStream?.depressButton(buttonCode)
    }
}

enum class Trigger {
    LEFT,
    RIGHT
}

private class XboxStream(stub: XboxButtonsGrpc.XboxButtonsStub) : GrpcStream() {
    var lastSent: Int = 0

    init {
        xboxPressButtonRequestObserver = stub.pressXboxButton(responseObserver)
        xboxDepressButtonRequestObserver = stub.depressXboxButton(responseObserver)

        xboxLeftThumbAxisRequestObserver = stub.xboxLeftThumbAxis(responseObserver)
        xboxRightThumbAxisRequestObserver = stub.xboxRightThumbAxis(responseObserver)

        xboxLeftTriggerRequestObserver = stub.xboxLeftTrigger(responseObserver)
        xboxRightTriggerRequestObserver = stub.xboxRightTrigger(responseObserver)
    }

    fun pressButton(buttonCode: Int) {
        try {
            val request = Services.XboxButton.newBuilder().setId(buttonCode).build()
            xboxPressButtonRequestObserver?.onNext(request)
        } catch (e: RuntimeException) {
            // Cancel RPC
            responseObserver?.onError(e)
            throw e
        }
        // requestObserver?.onCompleted()

        if (failed != null) {
            throw RuntimeException(failed)
        }

        lastSent = buttonCode
    }

    fun depressButton(buttonCode: Int) {
        try {
            val request = Services.XboxButton.newBuilder().setId(buttonCode).build()
            xboxDepressButtonRequestObserver?.onNext(request) // Sends the coords
        } catch (e: RuntimeException) {
            // Cancel RPC
            responseObserver?.onError(e)
            throw e
        }
        // requestObserver?.onCompleted()

        if (failed != null) {
            throw RuntimeException(failed)
        }

        lastSent = buttonCode
    }

    fun rightThumbAxis(x: Short, y: Short) {
        try {
            val request = Services.XboxThumbAxis.newBuilder().setX(x.toInt()).setY(y.toInt()).build()
            xboxRightThumbAxisRequestObserver?.onNext(request)
        } catch (e: RuntimeException) {
            // Cancel RPC
            responseObserver?.onError(e)
            throw e
        }
        // requestObserver?.onCompleted()

        if (failed != null) {
            throw RuntimeException(failed)
        }
    }

    fun leftThumbAxis(x: Short, y: Short) {
        try {
            val request = Services.XboxThumbAxis.newBuilder().setX(x.toInt()).setY(y.toInt()).build()
            xboxLeftThumbAxisRequestObserver?.onNext(request)
        } catch (e: RuntimeException) {
            // Cancel RPC
            responseObserver?.onError(e)
            throw e
        }
        // requestObserver?.onCompleted()

        if (failed != null) {
            throw RuntimeException(failed)
        }
    }

    fun setTrigger(trigger: Trigger, pressure: Int) {
        try {
            val request = Services.XboxTrigger.newBuilder().setPressure(pressure).build()
            if (trigger == Trigger.LEFT)
                xboxLeftTriggerRequestObserver?.onNext(request)
            else
                xboxRightTriggerRequestObserver?.onNext(request)
        } catch (e: RuntimeException) {
            // Cancel RPC
            responseObserver?.onError(e)
            throw e
        }

        if (failed != null) {
            throw RuntimeException(failed)
        }
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