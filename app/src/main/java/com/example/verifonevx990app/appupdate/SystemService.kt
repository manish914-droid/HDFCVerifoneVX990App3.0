package com.example.verifonevx990app.appupdate

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.IBinder
import android.os.RemoteException
import android.util.Log
import com.vfi.smartpos.system_service.aidl.ISystemManager
import com.vfi.smartpos.system_service.aidl.settings.ISettingsManager

object SystemService {

    private const val TAG = "SystemService"
    private const val ACTION_X9SERVICE = "com.vfi.smartpos.system_service"
    private const val PACKAGE = "com.vfi.smartpos.system_service"
    private const val CLASSNAME = "com.vfi.smartpos.system_service.SystemService"

    var systemManager: ISystemManager? = null
    private var settingsManager: ISettingsManager? = null

    //region=====================================Bind System Service=========================
    fun connectSystemService(context: Context) {
        val intent = Intent().apply {
            action = ACTION_X9SERVICE
            setClassName(PACKAGE, CLASSNAME)
        }

        val isSucc = context.bindService(intent, conn, Context.BIND_AUTO_CREATE)
        if (!isSucc) {
            Log.i("TAG", "deviceService connect fail!")
        } else {
            Log.i("TAG", "deviceService connect success")

        }
    }
    //endregion

    //region============================UnBind System Service:-
    fun disconnectSystemService(context: Context) = context.unbindService(conn)
    //endregion

    var conn: ServiceConnection = object : ServiceConnection {
        override fun onServiceConnected(componentName: ComponentName, iBinder: IBinder) {
            Log.d(TAG, "system service bind success")

            systemManager = ISystemManager.Stub.asInterface(iBinder)
            try {
                run {
                    {

                    }
                }
                settingsManager = systemManager?.settingsManager
            } catch (e: RemoteException) {
                e.printStackTrace()
            }
        }

        override fun onServiceDisconnected(componentName: ComponentName) {
            Log.d(TAG, "system service disconnected.")
            systemManager = null
        }
    }
//==================================To End system service=========================
}

//Performance , Scalability , Optimization in point of Android
//WebSockets , MPAndroidChart
//Android , Kotlin , Java Questions
//Android Testing