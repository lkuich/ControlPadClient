package lkuich.controlhubclient

import android.content.pm.ActivityInfo
import android.graphics.Rect
import android.os.AsyncTask
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.util.Log
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.ImageView
import io.grpc.ManagedChannel
import io.grpc.ManagedChannelBuilder
import io.grpc.stub.StreamObserver
import service.XboxButtonsGrpc
import service.Services
import java.io.PrintWriter
import java.io.StringWriter
import java.util.concurrent.TimeUnit
import android.opengl.ETC1.getHeight
import android.opengl.ETC1.getWidth
import service.XboxLeftThumbAxisGrpc
import service.XboxRightThumbAxisGrpc


class XboxActivity : AppCompatActivity() {
    private var xboxButtonStream: XboxButtonStream? = null
    private var xboxLeftAxisStream: XboxLeftAxisStream? = null
    private var xboxRightAxisStream: XboxRightAxisStream? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_canvas)
        // supportActionBar?.setDisplayHomeAsUpEnabled(true)
        fullscreen()

        xboxButtonStream = XboxButtonStream(createXboxButtonsStub())
        xboxLeftAxisStream = XboxLeftAxisStream(createXboxLeftThumbAxisStub())
        xboxRightAxisStream = XboxRightAxisStream(createXboxRightThumbAxisStub())

        analogStick(R.id.left_analog_inner, R.id.left_analog_outer, { x, y ->
            xboxLeftAxisStream?.leftThumbAxis(x.toShort(), y.toShort())
        })

        analogStick(R.id.right_analog_inner, R.id.right_analog_outer, { x, y ->
            xboxRightAxisStream?.rightThumbAxis(x.toShort(), y.toShort())
        })

        button(R.id.a_button, 0x1000)
        button(R.id.b_button, 0x2000)
        button(R.id.x_button, 0x4000)
        button(R.id.y_button, 0x8000)
    }

    fun createXboxButtonsStub(): XboxButtonsGrpc.XboxButtonsStub {
        val host: String = getString(R.string.grpc_ip)
        val port: Int = 50051

        val channel: ManagedChannel = ManagedChannelBuilder.forAddress(host, port).usePlaintext(true).build()
        return XboxButtonsGrpc.newStub(channel)
    }

    fun createXboxLeftThumbAxisStub(): XboxLeftThumbAxisGrpc.XboxLeftThumbAxisStub {
        val host: String = getString(R.string.grpc_ip)
        val port: Int = 50051

        val channel: ManagedChannel = ManagedChannelBuilder.forAddress(host, port).usePlaintext(true).build()
        return XboxLeftThumbAxisGrpc.newStub(channel)
    }

    fun createXboxRightThumbAxisStub(): XboxRightThumbAxisGrpc.XboxRightThumbAxisStub {
        val host: String = getString(R.string.grpc_ip)
        val port: Int = 50051

        val channel: ManagedChannel = ManagedChannelBuilder.forAddress(host, port).usePlaintext(true).build()
        return XboxRightThumbAxisGrpc.newStub(channel)
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
    fun analogStick(innerAnalogId: Int, outerAnalogId: Int, onMove: (relativeX: Float, relativeY: Float) -> Unit) {
        val analog = findViewById<ImageView>(innerAnalogId)
        val analogOuter = findViewById<ImageView>(outerAnalogId)

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

                    onMove(0f, 0f)
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
        xboxButtonStream?.pressButton(buttonCode)
    }

    fun depressButton(buttonCode: Int) {
        xboxButtonStream?.depressButton(buttonCode)
    }
}

private class XboxButtonStream(stub: XboxButtonsGrpc.XboxButtonsStub) : GrpcStream() {
    var lastSent: Int = 0

    init {
        xboxPressButtonRequestObserver = stub.pressXboxButton(responseObserver)
        xboxDepressButtonRequestObserver = stub.depressXboxButton(responseObserver)
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

private class XboxRightAxisStream(stub: XboxRightThumbAxisGrpc.XboxRightThumbAxisStub) : GrpcStream() {
    init {
        xboxRightThumbAxisRequestObserver = stub.xboxRightThumbAxis(responseObserver)
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

private class XboxLeftAxisStream(stub: XboxLeftThumbAxisGrpc.XboxLeftThumbAxisStub) : GrpcStream() {
    init {
        xboxLeftThumbAxisRequestObserver = stub.xboxLeftThumbAxis(responseObserver)
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