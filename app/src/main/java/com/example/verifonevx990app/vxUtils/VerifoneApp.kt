package com.example.verifonevx990app.vxUtils


import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.app.Application
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.ActivityInfo
import android.content.pm.PackageManager
import android.net.ConnectivityManager
import android.net.Network
import android.os.Build
import android.os.Bundle
import android.os.StrictMode
import android.telephony.PhoneStateListener
import android.telephony.SignalStrength
import android.telephony.TelephonyManager
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import androidx.annotation.RequiresApi
import androidx.multidex.MultiDex
import com.example.verifonevx990app.appupdate.SystemService
import com.example.verifonevx990app.main.MainActivity
import com.example.verifonevx990app.realmtables.TerminalParameterTable
import io.realm.Realm
import io.realm.RealmConfiguration
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

class VerifoneApp : Application() {

    companion object {
        @JvmStatic
        lateinit var appContext: Context
        private val TAG = VerifoneApp::class.java.simpleName

        var internetConnection = false
            private set

        private lateinit var vxAppContext: VerifoneApp

        fun getPaxContext(): VerifoneApp = vxAppContext
        fun getDeviceModel(): String {

            return "Device_model"
        }

        fun getDeviceSerialNo(): String {
            return "Deviceserialnumber"
        }

        var activeActivity: String? = null

        var networkStrength = ""
            private set

        var batteryStrength = ""
            private set

        var imeiNo = ""
            private set

        var simNo = ""
            private set

        var operatorName = ""
            private set
    }

    private val mBatteryReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            val level = intent?.getIntExtra("level", 0) ?: 0
            batteryStrength = if (level != 0) level.toString() else ""
        }
    }

    private val mConnectionReceiver by lazy {
        Connectivity {
            internetConnection = it
        }
    }

    private val mTelephonyManager: TelephonyManager by lazy { getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager }

    override fun attachBaseContext(base: Context?) {
        super.attachBaseContext(base)
        MultiDex.install(this)
    }

    @RequiresApi(Build.VERSION_CODES.M)
    @SuppressLint("HardwareIds")
    override fun onCreate() {
        super.onCreate()

        appContext = this
        vxAppContext = this
        MultiDex.install(this)
        VFService.connectToVFService(appContext)
        SystemService.connectSystemService(appContext)
        val builder: StrictMode.VmPolicy.Builder = StrictMode.VmPolicy.Builder()
        StrictMode.setVmPolicy(builder.build())


        registerActivityLifecycleCallbacks(object : ActivityLifecycleCallbacks {
            override fun onActivityPaused(p0: Activity) {
            }

            override fun onActivityResumed(p0: Activity) {
            }

            override fun onActivityStarted(p0: Activity) {
                if (p0 != null) {

                    val name = p0::class.java.simpleName
                    if (name == MainActivity::class.java.simpleName) {
                        activeActivity = name
                    }
                }
            }

            override fun onActivityDestroyed(p0: Activity) {

            }

            override fun onActivitySaveInstanceState(p0: Activity, p1: Bundle) {
            }

            override fun onActivityStopped(p0: Activity) {
                if (p0 != null) {
                    val name = p0::class.java.simpleName
                    if (name == MainActivity::class.java.simpleName) activeActivity = null
                }
            }

            override fun onActivityCreated(p0: Activity, p1: Bundle?) {
                p0.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT

            }
        })


        registerReceiver(mConnectionReceiver, IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION))
        registerReceiver(mBatteryReceiver, IntentFilter(Intent.ACTION_BATTERY_CHANGED))

        initRealm()
        GlobalScope.launch {

            //Below method is used to create an Alarm Service for AutoSettlement Process in App:-
            //setAutoSettlement()

            // setting for printer density
            val tpt = TerminalParameterTable.selectFromSchemeTable()
            if (tpt != null) {
                val darkness = if (tpt.printingImpact.isNotEmpty()) tpt.printingImpact else "0"
                //setPrintDarkness(darkness.toInt())
            }

            //EmvImplementation.isAidAdded = false

            /*  if (Build.VERSION.SDK_INT > Build.VERSION_CODES.O) {
                  if (checkSelfPermission(Manifest.permission.READ_PHONE_STATE) == PackageManager.PERMISSION_GRANTED) {
                      imeiNo = mTelephonyManager.imei
                  }
              } else {
                  if (checkSelfPermission(Manifest.permission.READ_PHONE_STATE) == PackageManager.PERMISSION_GRANTED) {
                      imeiNo = mTelephonyManager.deviceId
                  }
              }*/

        }

        UserProvider.refresh()
        setNetworkStrength()

    }

    /**
     * This function sets signal strength listener and set value into networkStrength
     * as per decible value
     * */
    private fun setNetworkStrength() {
        val nl = object : PhoneStateListener() {
            @RequiresApi(Build.VERSION_CODES.M)
            @SuppressLint("HardwareIds")
            override fun onSignalStrengthsChanged(signalStrength: SignalStrength?) {
                super.onSignalStrengthsChanged(signalStrength)
                if (signalStrength != null) {
                    var ss = signalStrength.gsmSignalStrength
                    ss = (2 * ss) - 113
                    networkStrength = getSignal(ss)
                }
                operatorName = mTelephonyManager.networkOperatorName
                if (checkSelfPermission(Manifest.permission.READ_PHONE_STATE) == PackageManager.PERMISSION_GRANTED) {
                    simNo = mTelephonyManager.simSerialNumber ?: ""
                }
            }
        }
        mTelephonyManager.listen(nl, PhoneStateListener.LISTEN_SIGNAL_STRENGTHS)
    }

    private fun initRealm() {
        val chars = "16CharacterLongPasswordKey4Realm".toCharArray()
        val key = ByteArray(chars.size * 2)
        for (i in chars.indices) {
            key[i * 2] = (chars[i].toInt() shr 8).toByte()
            key[i * 2 + 1] = chars[i].toByte()
        }

        /* realmConfiguration */
        Realm.init(this)
        Realm.setDefaultConfiguration(
            RealmConfiguration.Builder()
                .name("verifoneMpos.db")
                .allowWritesOnUiThread(true)
                .encryptionKey(key)
                .deleteRealmIfMigrationNeeded()
                .build()
        )
    }


    override fun onTerminate() {
        unregisterReceiver(mConnectionReceiver)
        unregisterReceiver(mBatteryReceiver)
        super.onTerminate()
    }


}

//Internet Check Class:-
internal class Connectivity() : BroadcastReceiver() {

    lateinit var onConnectionChange: (Boolean) -> Unit

    constructor(onConnectionChange: (Boolean) -> Unit) : this() {
        this.onConnectionChange = onConnectionChange
    }

    override fun onReceive(p0: Context?, p1: Intent?) {
        if (p0 != null) {
            checkConnectivity(p0)
        }
    }

    private fun checkConnectivity(p0: Context) {
        val connMgr = p0.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            connMgr.registerDefaultNetworkCallback(object : ConnectivityManager.NetworkCallback() {
                override fun onAvailable(network: Network) {
                    super.onAvailable(network)
                    if (network != null) {
                        val info = connMgr.getNetworkInfo(network)
                        if (info != null) {
                            onConnectionChange(info.isConnected)
                        }
                    } else onConnectionChange(false)
                }

                override fun onUnavailable() {
                    super.onUnavailable()
                    onConnectionChange(false)
                }

            })
        } else {
            val info = connMgr.activeNetworkInfo
            if (info != null) {
                onConnectionChange(info.isConnected)
            } else {
                onConnectionChange(false)
            }
        }

    }

}

//Below Method is used to open the Soft System Keyboard of Android Device:-
fun toggleSoftKeyboard(et: EditText, context: Context) {
    try {
        (context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager).apply {
            toggleSoftInputFromWindow(et.windowToken, InputMethodManager.SHOW_FORCED, 0)
        }
    } catch (ex: Exception) {
    }
}

//Below Method is used to close the Soft System Keyboard of Android Device:-
fun hideSoftKeyboard(activity: Activity) {
    try {
        val ims = activity.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        var view = activity.currentFocus
        if (view == null) {
            view = View(activity)
        }
        ims.hideSoftInputFromWindow(view.windowToken, 0)
    } catch (ex: Exception) {
    }
}

/*Commented by Ajay Thakur , Because not used in Verifone MPOS App*/
/*fun attachAnimationView(v: View, msg: String = "") {

    val webVW = v.findViewById<WebView>(R.id.nd_wv)
    webVW.loadUrl("file:///android_asset/rabbit.html")

    val msgTV = v.findViewById<TextView>(R.id.nd_tv)

    if (msg.isNotEmpty()) {
        msgTV.text = msg
    }

    val animation = AnimationUtils.loadAnimation(v.context, R.anim.text_scale_up)

    animation.setAnimationListener(object : Animation.AnimationListener {
        override fun onAnimationRepeat(animation: Animation?) {

        }

        override fun onAnimationEnd(animation: Animation?) {
            webVW.visibility = View.GONE
        }

        override fun onAnimationStart(animation: Animation?) {
        }

    })

    msgTV.startAnimation(animation)

}*/

internal fun getSignal(value: Int): String {
    return when {
        value >= -50 -> "100"
        value >= -60 && value < -50 -> "80"
        value >= -70 && value < -60 -> "60"
        value >= -80 && value < -70 -> "40"
        value >= -90 && value < -80 -> "20"
        else -> ""
    }
}