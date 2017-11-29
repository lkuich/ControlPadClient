package lkuich.controlhubclient

import android.content.pm.ActivityInfo
import android.os.Bundle
import android.support.design.widget.Snackbar
import android.support.v7.app.AppCompatActivity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.ImageView
import android.widget.RelativeLayout

import kotlinx.android.synthetic.main.activity_customize_layout.*

class ControlLayout(val name: String, val controls: MutableList<ElementPosition>)

class ElementPosition(val elm: RelativeLayout, private val actionUp: (elm: View, rawX: Float, rawY: Float) -> Unit) {
    var x: Float = elm.x
    var y: Float = elm.y

    init {
        enableMovement()
    }

    fun enableMovement(elms: RelativeLayout? = null) {
        val target = if (elms == null) elm else elms
        target.setOnTouchListener(
                View.OnTouchListener { v, evt ->
                    when (evt.action) {
                        MotionEvent.ACTION_DOWN -> {
                        }
                        MotionEvent.ACTION_MOVE -> {
                            target.x = evt.rawX - target.width / 2
                            target.y = evt.rawY - target.height / 2
                        }
                        MotionEvent.ACTION_UP -> {
                            actionUp(target, evt.rawX, evt.rawY)
                        }
                    }
                    return@OnTouchListener true
                })
    }

    fun setPos(x: Float, y: Float) {
        this.x = x - elm.width / 2
        this.y = y - elm.height / 2
    }

    fun move(elms: RelativeLayout) {
        elms.x = x - elms.width / 2
        elms.y = y - elms.height / 2
    }
}

class CustomizeLayoutActivity : AppCompatActivity() {

    private var app : ControlHubApplication? = null

    fun onElmUp(elm: View, rawX: Float, rawY: Float) {
        // Save layout
        app!!.getInstance()!!.layouts
                .first { e -> e.name == "custom" }
                .controls.first { control -> control.elm.id == elm.id }.setPos(rawX , rawY)

    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_canvas)

        app = applicationContext as ControlHubApplication
        if (app!!.getInstance()!!.layouts.isEmpty()) {
            app!!.getInstance()!!.layouts.add(ControlLayout("custom", mutableListOf<ElementPosition>(
                    ElementPosition(findViewById(R.id.left_directional_pad), { elm, rawX, rawY -> onElmUp(elm, rawX, rawY) }),
                    ElementPosition(findViewById(R.id.right_directional_pad), { elm, rawX, rawY -> onElmUp(elm, rawX, rawY) }),
                    ElementPosition(findViewById(R.id.buttons), { elm, rawX, rawY -> onElmUp(elm, rawX, rawY) }),
                    ElementPosition(findViewById(R.id.dpad), { elm, rawX, rawY -> onElmUp(elm, rawX, rawY) })
            )))
        } else {
            // Set controls
            app!!.getInstance()!!.layouts.first { controlLayout -> controlLayout.name == "custom" }.controls.forEach { control ->
                val view = findViewById<RelativeLayout>(control.elm.id)
                control.enableMovement(view)
                control.move(view)
            }
        }

        fullscreen()
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
}
