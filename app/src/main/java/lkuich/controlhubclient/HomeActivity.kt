package lkuich.controlhubclient

import android.app.Activity
import android.content.Context
import android.os.Bundle
import android.support.v4.app.Fragment
import android.support.v4.app.FragmentActivity
import android.support.v4.app.FragmentManager
import android.view.View
import android.support.v4.view.ViewPager
import android.view.ViewGroup
import android.view.LayoutInflater
import android.support.v4.app.FragmentStatePagerAdapter
import android.support.v4.view.PagerAdapter
import android.widget.ImageView
import android.widget.TextView
import android.content.Intent
import android.content.IntentFilter
import android.net.ConnectivityManager
import android.net.NetworkInfo
import android.net.wifi.WifiManager
import java.io.IOException
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import android.os.StrictMode
import android.os.AsyncTask
import android.support.annotation.NonNull
import android.util.Log
import android.widget.Button
import android.widget.ProgressBar
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.tasks.Task
import com.google.firebase.auth.*
import com.google.firebase.database.*


class BroadcastReceiver(val postExecute: (result: String) -> Unit): AsyncTask<Void, Void, String>() {
    val PORT = 58384

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
    private val NUM_PAGES = 2
    private var mPager: ViewPager? = null
    private var mPagerAdapter: PagerAdapter? = null
    private var selectedConfig = 0

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
                invalidateOptionsMenu()
                selectedConfig = position
            }
        })

        // Customize layout button
        findViewById<Button>(R.id.btnConfigure).setOnClickListener({
            val intent = Intent(applicationContext, CustomizeLayoutActivity::class.java)
            startActivity(intent)
        })

        // Start broadcasting
        BroadcastReceiver({ result ->
            // Now we can activate the button
            runOnUiThread {
                val btn = findViewById<Button>(R.id.btnConnect)
                btn.isEnabled = true
                btn.setOnClickListener({
                    val activity = if (selectedConfig == 0) XboxActivity::class.java else CanvasActivity::class.java
                    val intent = Intent(applicationContext, activity)
                    intent.putExtra(IP, result)
                    startActivity(intent)
                })

                (findViewById<ProgressBar>(R.id.progressSpinner)).visibility = View.INVISIBLE
                (findViewById<TextView>(R.id.txtServerStatus)).text = "Found server (" + result + ")"
            }
        }).execute()
    }

    fun isWifiConnected(): Boolean {
        val wifi = applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
        val networkInfo: NetworkInfo
        if (wifi.isWifiEnabled) {
            val connectivityManager = applicationContext.getSystemService(
                    Context.CONNECTIVITY_SERVICE) as ConnectivityManager
            networkInfo = connectivityManager.getNetworkInfo(ConnectivityManager.TYPE_WIFI)
        } else
            return false

        return networkInfo.isConnected
    }

    fun isUsbConnected(): Boolean {
        val intent = applicationContext.registerReceiver(null, IntentFilter(
                "android.hardware.usb.action.USB_STATE")
        )
        return intent != null && intent.extras!!.getBoolean("connected")
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