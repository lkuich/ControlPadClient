package lkuich.controlhubclient

import android.os.Bundle
import android.support.v4.app.Fragment
import android.support.v4.app.FragmentActivity
import android.support.v4.app.FragmentManager
import android.view.View
import android.support.v4.view.ViewPager
import android.support.v4.app.FragmentPagerAdapter
import android.view.ViewGroup
import android.view.LayoutInflater
import android.support.v4.app.FragmentStatePagerAdapter
import android.support.v4.view.PagerAdapter
import android.widget.ImageView
import android.widget.TextView
import android.R.menu
import android.view.Menu

import android.content.ContentValues.TAG
import android.support.v4.content.LocalBroadcastManager
import android.content.Intent
import android.provider.SyncStateContract
import android.system.Os.socket
import java.io.IOException
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import android.content.ContentValues.TAG
import android.content.Context
import android.os.StrictMode
import android.net.DhcpInfo
import android.content.Context.WIFI_SERVICE
import android.net.wifi.WifiManager
import android.os.AsyncTask
import android.widget.Button


class BroadcastReceiver: AsyncTask<Void, Void, String>() {
    val PORT = 58384

    override fun doInBackground(vararg params: Void?): String {
        return startReceiving()
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

            return recievedIp
        } catch (e: IOException) {
        }
        return ""
    }
}

class HomeActivity : FragmentActivity() {

    /**
     * The number of pages (wizard steps) to show in this demo.
     */
    private val NUM_PAGES = 2

    /**
     * The pager widget, which handles animation and allows swiping horizontally to access previous
     * and next wizard steps.
     */
    private var mPager: ViewPager? = null

    /**
     * The pager adapter, which provides the pages to the view pager widget.
     */
    private var mPagerAdapter: PagerAdapter? = null

    companion object {
        val IP = "com.lkuich.controlhubclient.IP"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home)

        // Instantiate a ViewPager and a PagerAdapter.
        mPager = findViewById<ViewPager>(R.id.inputType)
        mPagerAdapter = ScreenSlidePagerAdapter(supportFragmentManager)
        mPager!!.adapter = mPagerAdapter
        mPager!!.setOnPageChangeListener(object : ViewPager.SimpleOnPageChangeListener() {
            override fun onPageSelected(position: Int) {
                // When changing pages, reset the action bar actions since they are dependent
                // on which page is currently active. An alternative approach is to have each
                // fragment expose actions itself (rather than the activity exposing actions),
                // but for simplicity, the activity provides the actions in this sample.
                invalidateOptionsMenu()
            }
        })

        // Start broadcasting
        Thread().run {
            val ip = BroadcastReceiver().execute().get()

            // Now we can activate the button
            runOnUiThread {
                val btn = findViewById<Button>(R.id.btnConnect)
                btn.isEnabled = true
                btn.setOnClickListener({
                    val intent = Intent(applicationContext, XboxActivity::class.java)
                    intent.putExtra(IP, ip)
                    startActivity(intent)
                })
            }
        }
    }

    override fun onBackPressed() {
        if (mPager!!.currentItem == 0) {
            // If the user is currently looking at the first step, allow the system to handle the
            // Back button. This calls finish() on this activity and pops the back stack.
            super.onBackPressed()
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

    /**
     * The fragment's page number, which is set to the argument value for [.ARG_PAGE].
     */
    /**
     * Returns the page number represented by this fragment object.
     */
    var pageNumber: Int = 0
        private set

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        pageNumber = arguments.getInt(ARG_PAGE)
    }

    override fun onCreateView(inflater: LayoutInflater?, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        // Inflate the layout containing a title and body text.
        val rootView = inflater!!.inflate(R.layout.control_thumbnail, container, false) as ViewGroup

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