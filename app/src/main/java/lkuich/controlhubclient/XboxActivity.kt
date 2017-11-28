package lkuich.controlhubclient

import android.content.pm.ActivityInfo
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.util.Log
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.ImageView
import io.grpc.ManagedChannel
import io.grpc.ManagedChannelBuilder
import service.XboxButtonsGrpc
import service.Services
import android.support.v4.view.MotionEventCompat

class XboxActivity : AppCompatActivity() {
    private var xboxStream: XboxStream? = null

    override fun onTouchEvent(event: MotionEvent?): Boolean {
        val action = MotionEventCompat.getActionMasked(event)
        when (action) {
            MotionEvent.ACTION_DOWN -> {
            }
            MotionEvent.ACTION_MOVE -> {
            }
            MotionEvent.ACTION_UP -> {
                // fullscreen()
            }
            MotionEvent.ACTION_CANCEL -> {
            }
            MotionEvent.ACTION_OUTSIDE -> {
            }
        }

        return super.onTouchEvent(event)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val IP = intent.getStringExtra(HomeActivity.IP)

        setContentView(R.layout.activity_canvas)
        fullscreen()

        xboxStream = XboxStream(createStub(IP))
        analogStick(R.id.left_analog_inner, R.id.left_analog_outer, { x, y ->
            xboxStream?.leftThumbAxis(x.toShort(), y.toShort())
        }, {
            // Release
            xboxStream?.leftThumbAxis(0, 0)
            xboxStream?.setTrigger(Trigger.LEFT, 0)
        }, {
            // Pressure
            xboxStream?.setTrigger(Trigger.LEFT, Short.MAX_VALUE.toInt())
        })

        analogStick(R.id.right_analog_inner, R.id.right_analog_outer, { x, y ->
            xboxStream?.rightThumbAxis(x.toShort(), y.toShort())
        }, {
            // Release
            xboxStream?.rightThumbAxis(0, 0)
            xboxStream?.setTrigger(Trigger.RIGHT, 0)
        }, {
            // Pressure
            xboxStream?.setTrigger(Trigger.RIGHT, Short.MAX_VALUE.toInt())
        })

        button(R.id.a_button, 0x1000)
        button(R.id.b_button, 0x2000)
        button(R.id.x_button, 0x4000)
        button(R.id.y_button, 0x8000)
    }

    fun createStub(ip: String): XboxButtonsGrpc.XboxButtonsStub {
        val host: String = ip
        val port: Int = 50051

        val channel: ManagedChannel = ManagedChannelBuilder.forAddress(host, port).usePlaintext(true).build()
        return XboxButtonsGrpc.newStub(channel)
    }

    fun button(id: Int, key: Int) {
        val button = findViewById<ImageView>(id)
        button.setOnTouchListener(
            View.OnTouchListener { v, evt ->
                when (evt.action) {
                    MotionEvent.ACTION_DOWN -> {
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
    fun analogStick(innerAnalogId: Int, outerAnalogId: Int, onMove: (relativeX: Float, relativeY: Float) -> Unit, onRelease: () -> Unit, onPressure: () -> Unit) {
        val analog = findViewById<ImageView>(innerAnalogId)
        val analogOuter = findViewById<ImageView>(outerAnalogId)

        var analogStartCoords: FloatArray? = null
        var startCoords: FloatArray? = null

        var startTime: Long = 0
        var endTime: Long = 0
        var trigger = true

        analog.setOnTouchListener(
                View.OnTouchListener { v, evt ->
            when (evt.action) {
                MotionEvent.ACTION_DOWN -> {
                    analogStartCoords = floatArrayOf(analog.x, analog.y)
                    startCoords = floatArrayOf(evt.rawX, evt.rawY)

                    startTime = System.nanoTime()
                    trigger = true
                }
                MotionEvent.ACTION_MOVE -> {
                    if (evt.pressure > 1.3)
                        onPressure()

                    val evtX = evt.rawX
                    val evtY = evt.rawY

                    /*
                    if (evtX > 0 || evtY > 0)
                        trigger = false
                    */

                    analog.x = evtX - analog.width / 2
                    analog.y = evtY - analog.height / 2

                    val relativeX = startCoords!![0] - evtX
                    val relativeY = startCoords!![1] - evtY

                    onMove(relativeX, relativeY)
                }
                MotionEvent.ACTION_UP -> {
                    analog.x = analogStartCoords!![0]
                    analog.y = analogStartCoords!![1]

                    /*
                    endTime = System.nanoTime()
                    val durationMs = (endTime - startTime) / 1000000
                    Log.v("TIME", durationMs.toString())
                    if (durationMs <= 100)
                        onPressure()
                    */

                    onRelease()
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

    fun pressButton(buttonCode: Int) {
        xboxStream?.pressButton(buttonCode)
    }

    fun depressButton(buttonCode: Int) {
        xboxStream?.depressButton(buttonCode)
    }
}

private enum class Trigger {
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