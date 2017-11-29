package lkuich.controlhubclient

import android.content.DialogInterface
import android.content.pm.ActivityInfo
import android.os.Bundle
import android.support.design.widget.Snackbar
import android.support.v4.view.GravityCompat
import android.support.v4.widget.DrawerLayout
import android.support.v7.app.AlertDialog
import android.support.v7.app.AppCompatActivity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.*
import android.widget.Toast
import android.widget.TextView
import android.widget.RelativeLayout
import android.app.Activity
import android.support.v4.view.MotionEventCompat
import java.util.*


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
        this.x = x// - elm.width / 2
        this.y = y// - elm.height / 2
    }

    fun move(elms: RelativeLayout) {
        elms.x = x - elms.width / 2
        elms.y = y - elms.height / 2
    }
}

class CustomizeLayoutActivity : AppCompatActivity() {

    private var mDrawerLayout: DrawerLayout? = null
    private var mDrawerList: ListView? = null

    private var app : ControlHubApplication? = null

    fun onElmUp(elm: View, rawX: Float, rawY: Float) {
        // Save layout
        val selectedLayout = app!!.getInstance()!!.selectedLayout
        app!!.getInstance()!!.layouts
                .first { e -> e.name == selectedLayout }
                .controls.first { control -> control.elm.id == elm.id }.setPos(rawX , rawY)

    }

    override fun onTouchEvent(event: MotionEvent?): Boolean {
        val action = MotionEventCompat.getActionMasked(event)
        when (action) {
            MotionEvent.ACTION_DOWN -> {
            }
            MotionEvent.ACTION_MOVE -> {
                fullscreen()
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

    private val layouts = arrayOf("Default", "Custom 1", "Custom 2")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_canvas)

        app = applicationContext as ControlHubApplication
        val selectedLayout = app!!.getInstance()!!.selectedLayout
        if (app!!.getInstance()!!.layouts.isEmpty()) {
            layouts.forEach { layout ->
                app!!.getInstance()!!.layouts.add(ControlLayout(layout, mutableListOf<ElementPosition>(
                        ElementPosition(findViewById(R.id.left_directional_pad), { elm, rawX, rawY -> onElmUp(elm, rawX, rawY) }),
                        ElementPosition(findViewById(R.id.right_directional_pad), { elm, rawX, rawY -> onElmUp(elm, rawX, rawY) }),
                        ElementPosition(findViewById(R.id.buttons), { elm, rawX, rawY -> onElmUp(elm, rawX, rawY) }),
                        ElementPosition(findViewById(R.id.dpad), { elm, rawX, rawY -> onElmUp(elm, rawX, rawY) }),
                        ElementPosition(findViewById(R.id.left_shoulder), { elm, rawX, rawY -> onElmUp(elm, rawX, rawY) }),
                        ElementPosition(findViewById(R.id.right_shoulder), { elm, rawX, rawY -> onElmUp(elm, rawX, rawY) })
                )))
            }
        } else {
            // Set controls
            app!!.getInstance()!!.layouts.first { controlLayout -> controlLayout.name == selectedLayout }.controls.forEach { control ->
                val view = findViewById<RelativeLayout>(control.elm.id)
                control.enableMovement(view)
                control.move(view)
            }
        }

        val drawerItems = resources.getStringArray(R.array.config_options)
        mDrawerLayout = findViewById(R.id.drawer_layout)
        mDrawerList = findViewById(R.id.left_drawer)
        mDrawerList?.adapter = ArrayAdapter<String>(this, R.layout.drawer_list_item, drawerItems)
        mDrawerList?.setOnItemClickListener({ _: AdapterView<*>, _: View, position: Int, _: Long ->
            when(position) {
                0 -> { // Select config
                    layoutSelection()
                }
                1 -> { // Sync
                    // Toggle sync
                }
                2 -> { // Done
                    finish()
                }
            }
        })

        fullscreen()
    }

    fun layoutSelection() {
        // Build an AlertDialog
        val builder = AlertDialog.Builder(this@CustomizeLayoutActivity)
        var currentItem = layouts[0]
        builder.setSingleChoiceItems(layouts, layouts.indexOf(app!!.getInstance()?.selectedLayout)) { dialog, index ->
            currentItem = layouts[index]
        }

        builder.setCancelable(true)
        builder.setTitle("Select layout")
        builder.setPositiveButton("OK") { dialog, index ->
            app!!.getInstance()!!.selectedLayout = currentItem
            app!!.getInstance()!!.layouts.first { controlLayout -> controlLayout.name == app!!.getInstance()!!.selectedLayout }.controls.forEach { control ->
                val view = findViewById<RelativeLayout>(control.elm.id)
                control.enableMovement(view)
                control.move(view)
            }

            mDrawerLayout?.closeDrawers()
            fullscreen()
        }
        builder.setNeutralButton("Cancel") { dialog, which ->

        }

        // Build and show
        builder.create().show()
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
