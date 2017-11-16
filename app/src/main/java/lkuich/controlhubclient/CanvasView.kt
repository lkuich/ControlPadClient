package lkuich.controlhubclient

import Gesture.Controller
import Gesture.TouchEvents
import android.content.Context
import android.graphics.*

class CanvasView {
// class CanvasView(c: Context) : Controller(c), TouchEvents {
/*
    private var mCanvas: Canvas? = null
    private var drawBitmap: Bitmap? = null

    private val drawPath: Path
    private val drawPaint: Paint

    private val backgroundColor = Color.rgb(25, 25, 25)
    private var mX: Float = 0.toFloat()
    private var mY: Float = 0.toFloat()

    init {
        drawPath = Path()
        drawPaint = Paint(Paint.DITHER_FLAG)
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)

        drawBitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        mCanvas = Canvas(drawBitmap!!)
        mCanvas?.drawColor(backgroundColor)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        canvas.drawBitmap(drawBitmap!!, 0f, 0f, drawPaint)
    }

    override fun touchStart(x: Float, y: Float) {
        mX = x
        mY = y
    }

    override fun touchMove(x: Float, y: Float) {
        val dx = Math.abs(x - mX)
        val dy = Math.abs(y - mY)
        val TOUCH_TOLERANCE = 4f
        if (dx >= TOUCH_TOLERANCE || dy >= TOUCH_TOLERANCE) {
            mX = x
            mY = y
        }
    }

    override fun touchUp() {
    }
*/
}