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

abstract class BaseCanvasActivity: AppCompatActivity() {
    var app: ControlHubApplication? = null

    abstract fun onCreate()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_canvas)
        fullscreen()

        app = applicationContext as ControlHubApplication

        if (app?.getInstance()!!.layouts.isEmpty()) {
            // Load the cached layouts
            app?.getInstance()!!.layouts.clear()
            app?.getInstance()!!.cachedLayouts.forEach {
                // Iterate through cached layout
                val elements = mutableListOf<ElementPosition>()
                it.controls.forEach { control ->
                    // Go through each control
                    val element = findViewById<RelativeLayout>(control.id.toInt()) // get elm
                    val ctrl = ElementPosition(element, { elm, rawX, rawY -> onElmUp(elm, rawX, rawY) })
                    ctrl.setPos(control.x.toFloat(), control.y.toFloat())
                    ctrl.move(element)

                    elements.add(ctrl)
                }
                app?.getInstance()!!.layouts.add(ControlLayout(it.name, elements))
            }
        }

        onCreate()
    }

    fun onElmUp(elm: View, rawX: Float, rawY: Float) {
        // Save layout
        val selectedLayout = app?.getInstance()?.selectedLayout
        app?.getInstance()?.layouts!!
                .first { e -> e.name == selectedLayout }
                .controls.first { control -> control.elm.id == elm.id }.setPos(rawX , rawY)

        // Save it to DB
        app?.getInstance()?.firebaseLayouts!!.children.forEach { layout ->
            val name = layout.child("name").value.toString()
            val correctLayout = name == app?.getInstance()?.selectedLayout
            if (correctLayout) {
                layout.child("controls").children.forEach { config ->
                    if (config.child("id").value.toString().toInt() == elm.id) {
                        val controls = app?.getInstance()?.database?.child("layouts")?.child(layout.key)?.child("controls")?.child(config.key)!!
                        controls.child("x")?.setValue(rawX.toString())
                        controls.child("y")?.setValue(rawY.toString())
                    }
                }
            }
        }
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

class CustomizeLayoutActivity : BaseCanvasActivity() {

    private var layouts: Array<String> = arrayOf()
    override fun onCreate() {
        layouts = app!!.getInstance()!!.layoutNames
        val selectedLayout = app!!.getInstance()!!.selectedLayout

        // Set controls
        if (!app?.getInstance()!!.layouts.isEmpty()) {
            app?.getInstance()!!.layouts.first { controlLayout -> controlLayout.name == selectedLayout }.controls.forEach { control ->
                val view = findViewById<RelativeLayout>(control.elm.id)
                control.enableMovement(view)
                control.move(view)
            }
        } else {
            // TODO: No layouts were loaded, this is a problem
            val i =0
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
    }

    private var mDrawerLayout: DrawerLayout? = null
    private var mDrawerList: ListView? = null

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
}
