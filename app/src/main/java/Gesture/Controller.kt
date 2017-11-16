package Gesture

import android.content.Context
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.View

import java.util.ArrayList

abstract class Controller(context: Context) : View(context), GestureDetector.OnGestureListener, GestureDetector.OnDoubleTapListener {
    private val gesture: GestureDetector = GestureDetector(context, this)

    private val saved: IntArray = IntArray(2)
    private val sourceList: MutableList<InputSource>

    private var touchPoints: Int = 0
    private var touchDown: Boolean = false

    internal var doubleTapIncrement = 0

    protected val savedX: Int
        @Synchronized get() = this.saved[0]

    protected val savedY: Int
        @Synchronized get() = this.saved[1]

    init {

        sourceList = ArrayList()
        sourceList.add(InputSource())

        touchPoints = 0
    }

    fun singleTap() {

    }

    override fun onSingleTapConfirmed(event: MotionEvent): Boolean {
        singleTap()
        return true
    }

    override fun onDoubleTap(motionEvent: MotionEvent): Boolean {
        return true
    }

    override fun onDoubleTapEvent(event: MotionEvent): Boolean {
        val action = event.action
        when (action and MotionEvent.ACTION_MASK) {
            MotionEvent.ACTION_DOWN -> {
                setMouseDown()
                doubleTapIncrement = 0
            }

            MotionEvent.ACTION_MOVE -> doubleTapIncrement++

            MotionEvent.ACTION_UP -> {
                setMouseUp()
                if (doubleTapIncrement < 5) {
                    singleTap()
                }
                doubleTapIncrement = 0
            }
        }
        return true
    }

    override fun onDown(motionEvent: MotionEvent): Boolean {
        return true
    }

    override fun onShowPress(motionEvent: MotionEvent) {}

    override fun onSingleTapUp(motionEvent: MotionEvent): Boolean {
        return true
    }

    override fun onScroll(
            motionEvent: MotionEvent, motionEvent2: MotionEvent, v: Float, v2: Float): Boolean {
        return false
    }

    override fun onLongPress(motionEvent: MotionEvent) {
        val action = motionEvent.action
        when (action and MotionEvent.ACTION_MASK) {
            MotionEvent.ACTION_DOWN -> if (doubleTapIncrement == 0) {
                // Long hold
            }
        }
    }

    override fun onFling(
            motionEvent: MotionEvent, motionEvent2: MotionEvent, v: Float, v2: Float): Boolean {
        return true
    }

    protected fun movePointer(event: MotionEvent): Boolean {
        when (event.action and MotionEvent.ACTION_MASK) {
            MotionEvent.ACTION_DOWN -> {
                setSaved(event.getX(0).toInt(), event.getY(0).toInt())
                touchDown = true

                if (ControllerToggles.getPreviousCommand() == MotionEvent.ACTION_HOVER_EXIT) {
                    setMouseDown()
                }
            }

            MotionEvent.ACTION_HOVER_ENTER -> {
                // Standard Movement
                setSaved(event.getX(0).toInt(), event.getY(0).toInt())
                touchDown = true

                setMouseUp()
            }

            MotionEvent.ACTION_POINTER_DOWN // Multitouch
            -> {
                sourceList.add(InputSource())
                touchPoints = sourceList.size - 1

                if (event.getX(touchPoints) <= 4 && event.getY(touchPoints) <= 4) {
                    getSource(touchPoints).setCoordinates(
                            event.getX(touchPoints).toInt(),
                            event.getY(touchPoints).toInt())
                }
            }

            MotionEvent.ACTION_POINTER_UP -> if (touchPoints > 0) {
                sourceList.removeAt(touchPoints) // Remove last pointer
                touchPoints = touchPoints - 1

                touchDown = false
            }

            MotionEvent.ACTION_MOVE, MotionEvent.ACTION_HOVER_MOVE -> if (touchDown)
            // Make sure there is touch input is down
            {
                if (touchPoints == 0) {
                    // Moves the pointer
                    val pack = ShortArray(2)
                    pack[0] = (getSource(0).x - savedX).toShort()
                    pack[1] = (getSource(0).y - savedY).toShort()

                    // Send coords

                } else if (touchPoints == 1) {

                } else if (touchPoints == 2) {

                }
            }
        }
        ControllerToggles.setPreviousCommand(event.action)
        return this.gesture.onTouchEvent(event)
    }

    protected fun getSource(index: Int): InputSource {
        return this.sourceList[index]
    }

    private fun getDistance(point1: Int, point2: Int): Int {
        return if (point1 >= point2) {
            point1 - point2
        } else if (point2 >= point1) {
            point2 - point1
        } else {
            0
        }
    }

    private fun setSaved(x: Int, y: Int) {
        this.saved[0] = x
        this.saved[1] = y
    }

    private fun setMouseDown() {
        // Left mouse is down
    }

    private fun setMouseUp() {
        // Left mouse is up
    }
}
