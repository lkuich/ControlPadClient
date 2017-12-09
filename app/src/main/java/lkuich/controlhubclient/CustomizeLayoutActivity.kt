package lkuich.controlhubclient

import android.content.pm.ActivityInfo
import android.os.Bundle
import android.support.v4.widget.DrawerLayout
import android.support.v7.app.AlertDialog
import android.support.v7.app.AppCompatActivity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.*
import android.widget.RelativeLayout
import android.content.Context
import android.os.Vibrator
import android.support.v4.view.MotionEventCompat
import android.view.Gravity
import com.github.amlcurran.showcaseview.OnShowcaseEventListener
import com.github.amlcurran.showcaseview.ShowcaseView
import com.github.amlcurran.showcaseview.targets.ViewTarget

class ElementPosition(val elm: RelativeLayout, var keys: MutableList<Int>, private val actionUp: (elm: View, rawX: Float, rawY: Float) -> Unit) {
    var x: Float = elm.x
    var y: Float = elm.y

    private val vibrator: Vibrator = (elm.context.getSystemService(Context.VIBRATOR_SERVICE)) as Vibrator
    lateinit var onTap: () -> Unit

    fun enableMovement(elms: RelativeLayout? = null) {
        val target = elms ?: elm
        target.setOnClickListener({
            onTap()
        })
        target.setOnLongClickListener(View.OnLongClickListener { lng ->
            vibrator.vibrate(500)
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
                                actionUp(target, target.x, target.y)
                                target.setOnTouchListener(null)
                            }
                        }
                        return@OnTouchListener true
                    })

            return@OnLongClickListener true
        })

    }

    fun setPos(x: Float, y: Float) {
        this.x = x // - elm.width / 2
        this.y = y // - elm.height / 2
    }

    fun move(elms: RelativeLayout) {
        elms.x = x
        elms.y = y
    }

    /*fun move(elms: RelativeLayout) {
        elms.x = x - elms.width / 2
        elms.y = y - elms.height / 2
    }*/
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
                    val ctrl = ElementPosition(element, control.key, { elm, rawX, rawY -> onElmUp(elm, rawX, rawY) })
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
                        controls.child("x")?.setValue(rawX .toString())
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
        app?.getInstance()!!.layouts.first { controlLayout -> controlLayout.name == selectedLayout }.controls.forEach { control ->
            val view = findViewById<RelativeLayout>(control.elm.id)
            control.enableMovement(view)
            control.onTap = { buttonMapDialog(control.elm.id ) }
            control.move(view)
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
                1 -> { // Done
                    finish()
                }
            }
        })

        if (app?.getInstance()!!.firstRun) {
            showTutorial()

            // Set to not first run
            app?.getInstance()!!.firstRun = false
            app?.getInstance()!!.database?.child("firstRun")?.setValue(app?.getInstance()!!.firstRun)
        }
    }

    private fun showTutorial() {
        val activity = this
        val target = ViewTarget(R.id.left_directional_pad, this)
        ShowcaseView.Builder(this, true)
                .setTarget(target)
                .setContentTitle("Controls")
                .setContentText("Touch and hold to move, tap to map keys")
                .hideOnTouchOutside()
                .setShowcaseEventListener(object : OnShowcaseEventListener {
                    override fun onShowcaseViewShow(showcaseView: ShowcaseView?) { }

                    override fun onShowcaseViewHide(showcaseView: ShowcaseView?) { }

                    override fun onShowcaseViewDidHide(showcaseView: ShowcaseView?) {
                        // Show menu
                        mDrawerLayout?.openDrawer(Gravity.START, true)
                        val menuTarget = ViewTarget(mDrawerList?.getChildAt(0))
                        ShowcaseView.Builder(activity, true)
                                .setTarget(menuTarget)
                                .setContentTitle("Menu")
                                .setContentText("Swipe from the left to access other controls")
                                .hideOnTouchOutside()
                                .setShowcaseEventListener(object : OnShowcaseEventListener {
                                    override fun onShowcaseViewShow(showcaseView: ShowcaseView?) { }

                                    override fun onShowcaseViewHide(showcaseView: ShowcaseView?) { }

                                    override fun onShowcaseViewDidHide(showcaseView: ShowcaseView?) {
                                        mDrawerLayout?.closeDrawers()
                                    }

                                    override fun onShowcaseViewTouchBlocked(motionEvent: MotionEvent?) { }
                                })
                                .build()
                    }

                    override fun onShowcaseViewTouchBlocked(motionEvent: MotionEvent?) { }

                })
                .build()
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

    fun buttonMapDialog(id: Int) {
        val activity = this@CustomizeLayoutActivity
        val builder = AlertDialog.Builder(activity)
        val inflater = activity.layoutInflater
        val keys = app?.layouts!!.first { it.name == app?.selectedLayout }.controls.first { it.elm.id == id }.keys

        var mapLayout = 0
        when (id) {
            R.id.left_shoulder -> mapLayout = R.layout.left_bumper_map
            R.id.right_shoulder -> mapLayout = R.layout.right_bumper_map
            R.id.left_directional_pad -> mapLayout = R.layout.single_button_map
            R.id.right_directional_pad -> mapLayout = R.layout.single_button_map
            R.id.buttons -> mapLayout = R.layout.buttons_map
            else -> return
        }
        val context = inflater.inflate(mapLayout, null)
        builder.setView(context)

        when (id) {
            R.id.left_shoulder -> {
                context.findViewById<EditText>(R.id.left_bumper_map).setText(keys[0].toString())
                context.findViewById<EditText>(R.id.left_trigger_map).setText(keys[1].toString())
            }
            R.id.right_shoulder -> {
                context.findViewById<EditText>(R.id.right_bumper_map).setText(keys[0].toString())
                context.findViewById<EditText>(R.id.right_trigger_map).setText(keys[1].toString())
            }
            R.id.left_directional_pad -> {
                context.findViewById<EditText>(R.id.thumbstick_key).setText(keys[0].toString())
            }
            R.id.right_directional_pad -> {
                context.findViewById<EditText>(R.id.thumbstick_key).setText(keys[0].toString())
            }
            R.id.buttons -> {
                context.findViewById<EditText>(R.id.a_map).setText(keys[0].toString())
                context.findViewById<EditText>(R.id.b_map).setText(keys[1].toString())
                context.findViewById<EditText>(R.id.y_map).setText(keys[2].toString())
                context.findViewById<EditText>(R.id.x_map).setText(keys[3].toString())
            }
        }

        builder.setCancelable(true)
        builder.setTitle("Button mapping")
        builder.setPositiveButton("OK") { dialog, index ->
            // Map characters to values and save to firebase

            mDrawerLayout?.closeDrawers()
            fullscreen()
        }
        builder.setNeutralButton("Cancel") { dialog, which ->
            mDrawerLayout?.closeDrawers()
            fullscreen()
        }

        // Build and show
        builder.create().show()
    }

    fun layoutSelection() {
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
            mDrawerLayout?.closeDrawers()
            fullscreen()
        }

        // Build and show
        builder.create().show()
    }
}
