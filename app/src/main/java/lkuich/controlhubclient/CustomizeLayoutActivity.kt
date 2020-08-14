package lkuich.controlhubclient

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.content.pm.ActivityInfo
import android.os.Bundle
import android.widget.*
import android.widget.RelativeLayout
import android.content.Context
import android.view.*
import com.github.amlcurran.showcaseview.OnShowcaseEventListener
import com.github.amlcurran.showcaseview.ShowcaseView
import com.github.amlcurran.showcaseview.targets.ViewTarget
import android.graphics.Point
import android.os.Vibrator
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.MotionEventCompat
import androidx.drawerlayout.widget.DrawerLayout

class ElementPosition(val elm: RelativeLayout, var keys: MutableList<String>, private val actionUp: (elm: View, rawX: Float, rawY: Float) -> Unit, private val onLongClick: () -> Unit) {
    var x: Float = elm.x
    var y: Float = elm.y

    private val vibrator: Vibrator = (elm.context.getSystemService(Context.VIBRATOR_SERVICE)) as Vibrator
    lateinit var onTap: () -> Unit

    @SuppressLint("ClickableViewAccessibility")
    fun enableMovement(elms: RelativeLayout? = null) {
        val target = elms ?: elm
        target.setOnClickListener { onTap() }
        target.setOnLongClickListener(View.OnLongClickListener { lng ->
            vibrator.vibrate(500)
            onLongClick()

            target.setOnTouchListener(
                View.OnTouchListener { _, evt ->
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

    fun setKeys(vararg key: String) {
        keys.clear()
        key.forEach { this.keys.add(it) }
    }

    fun move(elms: RelativeLayout) {
        elms.x = x
        elms.y = y
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
                    val rootView = findViewById<RelativeLayout>(R.id.mainContent)

                    // Go through each control
                    val element = rootView.findViewWithTag<RelativeLayout>(control.tag) // get elm
                    val ctrl = ElementPosition(element, control.key, { elm, rawX, rawY -> onElmUp(elm, rawX, rawY) }, { fullscreen() })
                    ctrl.setPos(control.x, control.y)

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
                .controls.first { control -> control.elm.tag == elm.tag }.setPos(rawX , rawY)

        // Save it to DB
        app?.getInstance()?.firebaseLayouts!!.children.forEach { layout ->
            val name = layout.child("name").value.toString()
            val correctLayout = name == app?.getInstance()?.selectedLayout
            if (correctLayout) {
                layout.child("controls").children.forEach { config ->
                    if (config.child("tag").value.toString() == elm.tag) {
                        val controls = app?.getInstance()?.
                            database?.
                            child("layouts")?.
                            child(layout.key!!)?.
                            child("controls")?.
                            child(config.key!!)!!

                        controls.child("x").setValue(rawX .toString())
                        controls.child("y").setValue(rawY.toString())
                    }
                }
            }
        }
    }

    fun singleKeyMap(key: String): Int = when (key) {
        "a" -> JKeyEvent.VK_A; "b" ->  JKeyEvent.VK_B
        "c" -> JKeyEvent.VK_C; "d" -> JKeyEvent.VK_D
        "e" -> JKeyEvent.VK_E; "f" -> JKeyEvent.VK_F
        "g" -> JKeyEvent.VK_G; "h" -> JKeyEvent.VK_H
        "i" -> JKeyEvent.VK_I; "j" -> JKeyEvent.VK_J
        "k" -> JKeyEvent.VK_K; "l" -> JKeyEvent.VK_L
        "m" -> JKeyEvent.VK_M; "n" -> JKeyEvent.VK_N
        "o" -> JKeyEvent.VK_O; "p" -> JKeyEvent.VK_P
        "q" -> JKeyEvent.VK_Q; "r" -> JKeyEvent.VK_R
        "s" -> JKeyEvent.VK_S; "t" -> JKeyEvent.VK_T
        "u" -> JKeyEvent.VK_U; "v" -> JKeyEvent.VK_V
        "w" -> JKeyEvent.VK_W; "x" -> JKeyEvent.VK_X
        "y" -> JKeyEvent.VK_Y; "z" -> JKeyEvent.VK_Z
        "1" -> JKeyEvent.VK_1; "2" -> JKeyEvent.VK_2
        "3" -> JKeyEvent.VK_3; "4" -> JKeyEvent.VK_4
        "5" -> JKeyEvent.VK_5; "6" -> JKeyEvent.VK_6
        "7" -> JKeyEvent.VK_7; "8" -> JKeyEvent.VK_8
        "9" -> JKeyEvent.VK_9; "0" -> JKeyEvent.VK_0
        "left" -> JKeyEvent.VK_LEFT; "right" -> JKeyEvent.VK_RIGHT
        "up" -> JKeyEvent.VK_UP; "down" -> JKeyEvent.VK_DOWN
        "shift" -> JKeyEvent.VK_SHIFT
        "ctrl" -> JKeyEvent.VK_CONTROL
        "alt" -> JKeyEvent.VK_ALT
        "esc" -> JKeyEvent.VK_ESCAPE
        "tab" -> JKeyEvent.VK_TAB
        else -> JKeyEvent.VK_UNDEFINED
    }

    fun parseKey(k: String): Int {
        if (k.isEmpty())
            throw Exception("Key cannot be empty")
        val key = k.toLowerCase()

        val split = key.split(',')
        when (split.size) {
            1 -> { // There's no comma, no side spec
                // No split, just treat as single char
                return singleKeyMap(key)
            }
            2 -> {
                val side = split[0]
                val value = split[1]

                if (side == "left") {
                    when (value) {
                        "ctrl" -> return JKeyEvent.VK_CONTROL_LEFT
                        "alt" -> return JKeyEvent.VK_ALT_LEFT
                        "click" -> return JKeyEvent.LBUTTON
                    }
                } else { // Right
                    when (value) {
                        "ctrl" -> return JKeyEvent.VK_CONTROL_RIGHT
                        "alt" -> return JKeyEvent.VK_ALT_RIGHT
                        "click" -> return JKeyEvent.RBUTTON
                    }
                }
            }
        }
        return JKeyEvent.VK_UNDEFINED
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
            control.onTap = { buttonMapDialog(control.elm.tag as String) }

            if (app?.getInstance()!!.firstRun) {
                val dimensions = getDimensions()

                val x = dimensions.x * (control.x / 100)
                val y = dimensions.y * (control.y / 100)
                control.setPos(x, y)

                // Update DB
                onElmUp(control.elm, x, y)
            }
            control.move(view)
        }

        val drawerItems = resources.getStringArray(R.array.config_options)
        mDrawerLayout = findViewById(R.id.drawer_layout)
        mDrawerList = findViewById<ListView>(R.id.left_drawer)
        mDrawerList?.adapter = ArrayAdapter<String>(this, R.layout.drawer_list_item, drawerItems)
        mDrawerList?.setOnItemClickListener { _: AdapterView<*>, _: View, position: Int, _: Long ->
            when(position) {
                0 -> { // Select config
                    layoutSelection()
                }
                1 -> {
                    resetLayout()
                }
                2 -> { // Done
                    finish()
                }
            }
        }

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
        when (MotionEventCompat.getActionMasked(event)) {
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

    fun buttonMapDialog(tag: String) {
        val activity = this@CustomizeLayoutActivity
        val builder = AlertDialog.Builder(activity)
        val inflater = activity.layoutInflater
        val selectedControl = app?.layouts!!.first { it.name == app?.selectedLayout }.controls.first { it.elm.tag == tag }
        val keys = selectedControl.keys

        var mapLayout = 0
        when (tag) {
            getString(R.string.left_shoulder_tag) -> mapLayout = R.layout.left_bumper_map
            getString(R.string.right_shoulder_tag) -> mapLayout = R.layout.right_bumper_map
            getString(R.string.left_directional_pad_tag) -> mapLayout = R.layout.analog_stick_map
            getString(R.string.right_directional_pad_tag) -> mapLayout = R.layout.analog_stick_map
            getString(R.string.buttons_tag) -> mapLayout = R.layout.buttons_map
            else -> return
        }
        val context = inflater.inflate(mapLayout, null)
        builder.setView(context)

        when (tag) {
            getString(R.string.left_shoulder_tag) -> {
                context.findViewById<EditText>(R.id.left_bumper_map).setText(keys[0])
                context.findViewById<EditText>(R.id.left_trigger_map).setText(keys[1])
            }
            getString(R.string.right_shoulder_tag) -> {
                context.findViewById<EditText>(R.id.right_bumper_map).setText(keys[0])
                context.findViewById<EditText>(R.id.right_trigger_map).setText(keys[1])
            }
            getString(R.string.left_directional_pad_tag), getString(R.string.right_directional_pad_tag) -> {
                context.findViewById<EditText>(R.id.thumbstick_key).setText(keys[0])
                context.findViewById<CheckBox>(R.id.pressure_click).isChecked = keys[1].toBoolean()
            }
            getString(R.string.buttons_tag) -> {
                context.findViewById<EditText>(R.id.a_map).setText(keys[0])
                context.findViewById<EditText>(R.id.b_map).setText(keys[1])
                context.findViewById<EditText>(R.id.y_map).setText(keys[2])
                context.findViewById<EditText>(R.id.x_map).setText(keys[3])
            }
        }

        builder.setCancelable(true)
        builder.setTitle("Button mapping")
        builder.setPositiveButton("OK", null)
        builder.setNeutralButton("Cancel") { _, _->
            mDrawerLayout?.closeDrawers()
            fullscreen()
        }

        // Build and show
        val dialog = builder.create()
        dialog.setOnShowListener {
            val button = (dialog as AlertDialog).getButton(AlertDialog.BUTTON_POSITIVE)
            button.setOnClickListener {
                when (tag) {
                    getString(R.string.left_shoulder_tag) -> {
                        selectedControl.setKeys(
                                context.findViewById<EditText>(R.id.left_bumper_map).text.toString(),
                                context.findViewById<EditText>(R.id.left_trigger_map).text.toString()
                        )
                    }
                    getString(R.string.right_shoulder_tag) -> {
                        selectedControl.setKeys(
                                context.findViewById<EditText>(R.id.right_bumper_map).text.toString(),
                                context.findViewById<EditText>(R.id.right_trigger_map).text.toString()
                        )
                    }
                    getString(R.string.left_directional_pad_tag), getString(R.string.right_directional_pad_tag) -> {
                        selectedControl.setKeys(
                                context.findViewById<EditText>(R.id.thumbstick_key).text.toString(),
                                context.findViewById<CheckBox>(R.id.pressure_click).isChecked.toString()
                        )
                    }
                    getString(R.string.buttons_tag) -> {
                        selectedControl.setKeys(
                                context.findViewById<EditText>(R.id.a_map).text.toString(),
                                context.findViewById<EditText>(R.id.b_map).text.toString(),
                                context.findViewById<EditText>(R.id.y_map).text.toString(),
                                context.findViewById<EditText>(R.id.x_map).text.toString()
                        )
                    }
                }

                val keyErrors = mutableListOf<String>()
                selectedControl.keys.forEach {
                    if (parseKey(it) == JKeyEvent.VK_UNDEFINED && (it != "true" && it != "false"))
                        keyErrors.add(it)
                }
                if (keyErrors.isEmpty()) {
                    // Save it to DB
                    app?.getInstance()?.firebaseLayouts!!.children.forEach { layout ->
                        val name = layout.child("name").value.toString()
                        val correctLayout = name == app?.getInstance()?.selectedLayout
                        if (correctLayout) {
                            layout.child("controls").children.forEach { config ->
                                if (config.child("tag").value.toString() == tag) { // Get selected element
                                    val controls = app?.getInstance()?.database?.child("layouts")?.child(layout.key!!)?.child("controls")?.child(config.key!!)!!
                                    controls.child("key").setValue(selectedControl.keys)
                                }
                            }
                        }
                    }

                    dialog.dismiss()

                    mDrawerLayout?.closeDrawers()
                    fullscreen()
                } else {
                    val elm = context.findViewById<TextView>(R.id.txt_map_error)
                    elm.text = ""
                    keyErrors.forEach {
                        elm.append("- $it is invalid\n")
                    }
                }
            }
        }
        dialog.show()
    }

    private fun getDimensions(): Point {
        val size = Point()
        windowManager.defaultDisplay.getRealSize(size)
        return size
    }

    private fun resetLayout() {
        val builder: AlertDialog.Builder = AlertDialog.Builder(this)
        builder.setTitle("Warning")
            .setMessage("Are you sure you would like to reset " + app?.getInstance()!!.selectedLayout + "?")
            .setPositiveButton(android.R.string.yes, { _, _ ->
                app!!.getInstance()!!.layouts.first { controlLayout -> controlLayout.name == app!!.getInstance()!!.selectedLayout }.controls.forEach { control ->
                    val ctrl = app!!.getInstance()!!.defaultControls.first { it.tag == control.elm.tag }

                    val dimensions = getDimensions()

                    val x = dimensions.x * (ctrl.x / 100)
                    val y = dimensions.y * (ctrl.y / 100)

                    control.setPos(x, y)
                    control.elm.x = x
                    control.elm.y = y
                    onElmUp(control.elm, x, y)
                }

                mDrawerLayout?.closeDrawers()
                fullscreen()
            })
            .setNeutralButton(android.R.string.no, { _, _ ->
                mDrawerLayout?.closeDrawers()
                fullscreen()
            })
            .show()
    }

    fun layoutSelection() {
        val builder = AlertDialog.Builder(this@CustomizeLayoutActivity)
        var currentItem = layouts[0]
        builder.setSingleChoiceItems(layouts, layouts.indexOf(app!!.getInstance()?.selectedLayout)) { dialog, index ->
            currentItem = layouts[index]
        }

        builder.setCancelable(true)
        builder.setTitle("Select layout")
        builder.setPositiveButton("OK") { _, _ ->
            app!!.getInstance()!!.selectedLayout = currentItem
            app!!.getInstance()!!.layouts.first { controlLayout -> controlLayout.name == app!!.getInstance()!!.selectedLayout }.controls.forEach { control ->
                val view = findViewById<RelativeLayout>(control.elm.id)
                control.enableMovement(view)
                control.move(view)
            }

            mDrawerLayout?.closeDrawers()
            fullscreen()
        }
        builder.setNeutralButton("Cancel") { _, _ ->
            mDrawerLayout?.closeDrawers()
            fullscreen()
        }

        // Build and show
        builder.create().show()
    }
}
