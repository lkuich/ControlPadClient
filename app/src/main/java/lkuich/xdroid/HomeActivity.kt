package lkuich.xdroid

import android.app.AlertDialog
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.view.LayoutInflater
import android.widget.ImageView
import android.widget.TextView
import android.content.Intent
import android.content.pm.ActivityInfo
import java.io.IOException
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import android.os.StrictMode
import android.os.AsyncTask
import android.widget.Button
import android.widget.EditText
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentStatePagerAdapter
import androidx.viewpager.widget.PagerAdapter
import androidx.viewpager.widget.ViewPager
import lkuich.xdroid.R

class BroadcastReceiver(private val postExecute: (result: String) -> Unit): AsyncTask<Void, Void, String>() {
    private val PORT = 58385

    override fun doInBackground(vararg params: Void?): String {
        return startReceiving()
    }

    override fun onPostExecute(result: String?) {
        super.onPostExecute(result)
        postExecute(result!!)
    }

    fun sendBroadcast(ip: String, messageStr: String) {
        // Hack Prevent crash (sending should be done using an async task)
        val policy = StrictMode.ThreadPolicy.Builder().permitAll().build()
        StrictMode.setThreadPolicy(policy)

        val socket = DatagramSocket()
        socket.broadcast = true

        val inetAddress = InetAddress.getByName(ip)
        val sendData = messageStr.toByteArray()
        val sendPacket = DatagramPacket(sendData, sendData.size, inetAddress, PORT)

        try {
            socket.send(sendPacket)
        } catch (e: IOException) {
        }
        socket.close()
    }

    fun startReceiving(): String {
        val socket = DatagramSocket(PORT, InetAddress.getByName("0.0.0.0"))
        socket.broadcast = true

        val receiveData = ByteArray(1024)
        try {
            val receivePacket = DatagramPacket(receiveData, receiveData.size)
            socket.receive(receivePacket)

            val recievedIp: String = String(receivePacket.data).trim({ it <= ' ' })
            sendBroadcast(recievedIp, "recieved")
            socket.close()

            return recievedIp
        } catch (e: IOException) {
        }
        return ""
    }
}

class KeepAliveThread(private val notAlive: () -> Unit): AsyncTask<Void, Void, String>() {
    private var aliveTime = 10

    fun reset() {
        aliveTime = 10
    }

    override fun doInBackground(vararg p0: Void?): String {
        while (true) {
            Thread.sleep(1000)
            if (aliveTime <= 0)
                notAlive()
            else
                aliveTime--
        }
    }
}

class HomeActivity : FragmentActivity() {
    private val NUM_PAGES = 2
    private var mPager: ViewPager? = null
    private var mPagerAdapter: PagerAdapter? = null
    private var selectedConfig = 0
    var app: ControlHubApplication? = null

    companion object {
        val IP = "com.lkuich.xdroid.IP"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home)
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT

        app = applicationContext as ControlHubApplication
        app?.getInstance()?.homeLoaded = true

        // Instantiate a ViewPager and a PagerAdapter.
        mPager = findViewById<ViewPager>(R.id.inputType)
        mPagerAdapter = ScreenSlidePagerAdapter(supportFragmentManager)
        mPager!!.adapter = mPagerAdapter
        mPager!!.setOnPageChangeListener(object : ViewPager.SimpleOnPageChangeListener() {
            override fun onPageSelected(position: Int) {
                invalidateOptionsMenu()
                selectedConfig = position
            }
        })

        // Customize layout button
        findViewById<Button>(R.id.btnConfigure).setOnClickListener({
            val intent = Intent(applicationContext, CustomizeLayoutActivity::class.java)
            startActivity(intent)
        })

        // TODO: Not in use
        val ka = KeepAliveThread({
            // Disable buttons
            runOnUiThread {
                val btn = findViewById<Button>(R.id.btnConnect)
                // btn.isEnabled = false
                (findViewById<TextView>(R.id.btnConnect)).text = "Looking for server..."
            }
        })

        val btn = findViewById<Button>(R.id.btnConnect)
        // Start broadcasting
        BroadcastReceiver { result ->
            // ka.reset()

            // Now we can activate the button
            runOnUiThread {
                btn.setOnClickListener {
                    val activity = if (selectedConfig == 0) XboxActivity::class.java else StandardInputActivity::class.java
                    val intent = Intent(applicationContext, activity)
                    intent.putExtra(IP, result)

                    // Log the last IP
                    setLastIp(result) // Log in server

                    startActivity(intent)
                }

                // (findViewById<ProgressBar>(R.id.progressSpinner)).visibility = View.INVISIBLE
                (findViewById<TextView>(R.id.btnConnect)).text = "Connect ($result)"
            }
        }.execute()

        //TODO: Remove
        btn.setOnClickListener {
            val dialogContext = layoutInflater.inflate(R.layout.connect, null)
            val editText = dialogContext.findViewById<EditText>(R.id.connect_ip)
            editText.setText(app?.getInstance()!!.lastIp)

            val builder = AlertDialog.Builder(this)
            builder.setView(dialogContext)
            builder.setCancelable(true)
            builder.setTitle("Manually Connect")
            builder.setPositiveButton("Connect") { _, _ ->
                val ip = editText.text.toString()

                val activity = if (selectedConfig == 0) XboxActivity::class.java else StandardInputActivity::class.java
                val intent = Intent(applicationContext, activity)
                intent.putExtra(IP, ip)
                setLastIp(ip) // Log in server

                startActivity(intent)
            }

            builder.create().show()
        }

        // ka.execute()
    }

    fun setLastIp(ip: String) {
        app?.getInstance()!!.lastIp = ip
        app?.getInstance()!!.database?.child("lastIp")?.setValue(app?.getInstance()!!.lastIp)
    }

    override fun onBackPressed() {
        if (mPager!!.currentItem == 0) {
            // If the user is currently looking at the first step, allow the system to handle the
            // Back button. This calls finish() on this activity and pops the back stack.
            // super.onBackPressed()
        } else {
            // Otherwise, select the previous step.
            mPager!!.currentItem = mPager!!.currentItem - 1
        }
    }

    /**
     * A simple pager adapter that represents 5 ScreenSlidePageFragment objects, in
     * sequence.
     */
    private inner class ScreenSlidePagerAdapter(fm: FragmentManager) : FragmentStatePagerAdapter(fm) {

        override fun getItem(position: Int): Fragment {
            return ScreenSlidePageFragment.create(position)
        }

        override fun getCount(): Int {
            return NUM_PAGES
        }
    }
}


class ScreenSlidePageFragment : Fragment() {
    var pageNumber: Int = 0
        private set

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        pageNumber = arguments!!.getInt(ARG_PAGE)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        // Inflate the layout containing a title and body text.
        val rootView = inflater.inflate(R.layout.control_thumbnail, container, false) as ViewGroup

        var drawable = R.drawable.ic_xbox
        var title = ""
        var description = ""
        // Set the title view to show the page number.
        when (pageNumber + 1) {
            1 -> {
                drawable = R.drawable.ic_xbox
                title = "Xbox 360"
                description = getString(R.string.xbox_desc)
            }
            2 -> {
                drawable = R.drawable.ic_windows_10
                title = "Mouse & Keyboard"
                description = getString(R.string.standard_desc)
            }
            /*
            3 -> {
                drawable = R.drawable.ic_console
                description = "Your system will treat this configuration as direct keyboard and mouse input. If your game does not support Standard Keyboard and Mouse input, use this configuration. These are usually older games like Half Life."
            }
            */
        }

        rootView.findViewById<ImageView>(R.id.thumbnailImage).setImageResource(drawable)
        rootView.findViewById<TextView>(R.id.thumbnailTitle).text = title
        rootView.findViewById<TextView>(R.id.thumbnailDescription).text = description

        return rootView
    }

    companion object {
        /**
         * The argument key for the page number this fragment represents.
         */
        val ARG_PAGE = "page"

        /**
         * Factory method for this fragment class. Constructs a new fragment for the given page number.
         */
        fun create(pageNumber: Int): ScreenSlidePageFragment {
            val fragment = ScreenSlidePageFragment()
            val args = Bundle()
            args.putInt(ARG_PAGE, pageNumber)
            fragment.arguments = args
            return fragment
        }
    }
}