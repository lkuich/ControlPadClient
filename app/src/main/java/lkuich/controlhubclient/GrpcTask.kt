package lkuich.controlhubclient

import android.os.AsyncTask
import io.grpc.ManagedChannel
import io.grpc.ManagedChannelBuilder
import io.grpc.stub.StreamObserver
import service.RobotGrpc
import service.Services
import java.io.PrintWriter
import java.io.StringWriter
import java.util.concurrent.TimeUnit


abstract class GrpcStream {
    abstract fun onResponseNext(response: Services.Response)
    abstract fun onResponseError(t: Throwable)
    abstract fun onResponseCompleted()

    protected var responseObserver: StreamObserver<Services.Response>? = null
    protected var xboxPressButtonRequestObserver: StreamObserver<Services.XboxButton>? = null
    protected var xboxDepressButtonRequestObserver: StreamObserver<Services.XboxButton>? = null
    protected var xboxLeftThumbAxisRequestObserver: StreamObserver<Services.XboxThumbAxis>? = null
    protected var xboxRightThumbAxisRequestObserver: StreamObserver<Services.XboxThumbAxis>? = null
    protected var xboxLeftTriggerRequestObserver: StreamObserver<Services.XboxTrigger>? = null
    protected var xboxRightTriggerRequestObserver: StreamObserver<Services.XboxTrigger>? = null

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