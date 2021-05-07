package com.example.verifonevx990app.main

import android.app.AlarmManager
import android.app.PendingIntent
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.*
import com.example.verifonevx990app.realmtables.TerminalParameterTable
import com.example.verifonevx990app.vxUtils.AppPreference
import com.example.verifonevx990app.vxUtils.VerifoneApp
import com.example.verifonevx990app.vxUtils.VxEvent
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import java.util.*

/*
This AutoSettlement File is Not Used in Project , Please Ignore it!!
 */
suspend fun setAutoSettlement() {
    try {
        val tpt = TerminalParameterTable.selectFromSchemeTable()

        if (tpt != null) {
            val alMag =
                VerifoneApp.appContext.getSystemService(Context.ALARM_SERVICE) as AlarmManager

            val alarmIntent: PendingIntent
            alarmIntent =
                Intent(VerifoneApp.appContext, AutoSettleReceiver::class.java).let { intent ->
                    intent.action = "com.bonushub.vxPos.force_settle"
                    intent.addCategory("android.intent.category.DEFAULT")
                    PendingIntent.getBroadcast(
                        VerifoneApp.appContext,
                        0,
                        intent,
                        PendingIntent.FLAG_UPDATE_CURRENT
                    )
                }

            if (tpt.forceSettle == "1" && tpt.forceSettleTime.isNotEmpty()) {

                val hrsStr = tpt.forceSettleTime.substring(0, 2)
                val minStr = tpt.forceSettleTime.substring(2, 4)

                val cal = Calendar.getInstance().apply {
                    timeInMillis = System.currentTimeMillis()
                    set(Calendar.HOUR_OF_DAY, hrsStr.toInt())
                    set(Calendar.MINUTE, minStr.toInt())
                }

                if (cal.timeInMillis < System.currentTimeMillis()) {
                    cal.add(Calendar.DATE, 1)
                }

                alMag.set(AlarmManager.RTC_WAKEUP, cal.timeInMillis, alarmIntent)

            } else {
                AppPreference.setAutoSettle(false)
                alMag.cancel(alarmIntent)
            }
        }
    } catch (ex: Exception) {
        ex.printStackTrace()
    }
}


class AutoSettleReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context?, intent: Intent?) {
        if (intent?.action == "com.bonushub.vxPos.force_settle") {
            context?.startService(Intent(context, AutoSettleService::class.java))
        }
    }
}


//Don't forget to set auto settlement false after starting settlement

class AutoSettleService : Service() {

    private var mLooper: Looper? = null
    private var mHandler: AutoSettleHandler? = null


    override fun onBind(intent: Intent?): IBinder? {
        return null
    }


    override fun onCreate() {
        super.onCreate()

        HandlerThread("AutoSettleService", Process.THREAD_PRIORITY_BACKGROUND).run {
            start()
            mLooper = this.looper
            mHandler = AutoSettleHandler(mLooper as Looper)
        }

    }


    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        mHandler?.obtainMessage()?.also { msg ->
            msg.arg1 = 1
            mHandler?.sendMessage(msg)
        }
        return START_STICKY
    }


    inner class AutoSettleHandler(looper: Looper) : Handler(looper) {

        override fun handleMessage(msg: Message) {
            when (msg.arg1) {
                1 ->
                    GlobalScope.launch {

                        AppPreference.setAutoSettle(true)
                        while (AppPreference.getAutoSettle()) {
                            if (VerifoneApp.activeActivity == MainActivity::class.java.simpleName) {

                                EventSender.fireEvent(VxEvent.ForceSettle)
                                try {
                                    Thread.sleep(10 * 1000)
                                } catch (ex: InterruptedException) {
                                }

                            } else {
                                try {
                                    Thread.sleep(10 * 1000)
                                } catch (ex: InterruptedException) {
                                }
                            }

                        }

                        obtainMessage().also { msg ->
                            msg.arg1 = 0
                            sendMessage(msg)
                        }
                    }
            }
        }
    }
}


object EventSender {

    private val list = mutableListOf<EventSenderCallback>()

    fun registerEvent(esv: EventSenderCallback) {
        list.add(esv)
    }

    fun unregisterEvent(esv: EventSenderCallback) {
        for (e in list.indices) {
            if (list[e] == esv) {
                list.removeAt(e)
                break
            }
        }
    }

    fun fireEvent(event: VxEvent) {
        for (e in list) {
            e(event)
        }
    }

}


typealias EventSenderCallback = (VxEvent) -> Unit
