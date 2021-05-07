package com.example.verifonevx990app.vxUtils

import android.app.Activity
import android.app.AlarmManager
import android.app.AlertDialog
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageInfo
import android.os.Build
import android.os.Environment
import android.os.Looper
import android.os.StatFs
import android.util.Log
import com.example.verifonevx990app.main.MainActivity
import java.io.PrintWriter
import java.io.StringWriter
import java.io.Writer
import java.lang.Thread.UncaughtExceptionHandler
import java.util.*


class UnCaughtException(private val context: Context) : UncaughtExceptionHandler {
    private val statFs: StatFs
        get() {
            val path = Environment.getDataDirectory()
            return StatFs(path.path)
        }

    private fun getAvailableInternalMemorySize(stat: StatFs): Long {
        val blockSize = stat.blockSizeLong
        val availableBlocks = stat.availableBlocksLong
        return availableBlocks * blockSize
    }

    private fun getTotalInternalMemorySize(stat: StatFs): Long {
        val blockSize = stat.blockSizeLong
        val totalBlocks = stat.availableBlocksLong
        return totalBlocks * blockSize
    }

    private fun addInformation(message: StringBuilder) {
        message.append("Locale: ").append(Locale.getDefault()).append('\n')
        try {
            val pm = context.packageManager
            val pi: PackageInfo
            pi = pm.getPackageInfo(context.packageName, 0)
            message.append("Version: ").append(pi.versionName).append('\n')
            message.append("Package: ").append(pi.packageName).append('\n')
        } catch (e: Exception) {
            Log.e("CustomExceptionHandler", "Error", e)
            message.append("Could not get Version information for ").append(
                context.packageName
            )
        }
        message.append("Phone Model: ").append(Build.MODEL).append('\n')
        message.append("Android Version: ").append(Build.VERSION.RELEASE).append('\n')
        message.append("Board: ").append(Build.BOARD).append('\n')
        message.append("Brand: ").append(Build.BRAND).append('\n')
        message.append("Device: ").append(Build.DEVICE).append('\n')
        message.append("Host: ").append(Build.HOST).append('\n')
        message.append("ID: ").append(Build.ID).append('\n')
        message.append("Model: ").append(Build.MODEL).append('\n')
        message.append("Product: ").append(Build.PRODUCT).append('\n')
        message.append("Type: ").append(Build.TYPE).append('\n')
        val stat = statFs
        message.append("Total Internal memory: ").append(getTotalInternalMemorySize(stat))
            .append('\n')
        message.append("Available Internal memory: ").append(getAvailableInternalMemorySize(stat))
            .append('\n')
    }

    override fun uncaughtException(t: Thread, e: Throwable) {
        try {
            val report = StringBuilder()
            val curDate = Date()
            report.append("Error Report collected on : ")
                .append(curDate.toString()).append('\n').append('\n')
            report.append("Informations :").append('\n')
            addInformation(report)
            report.append('\n').append('\n')
            report.append("Stack:\n")
            val result: Writer = StringWriter()
            val printWriter = PrintWriter(result)
            e.printStackTrace(printWriter)
            report.append(result.toString())
            printWriter.close()
            report.append('\n')
            report.append("****  End of current Report ***")
            Log.e(UnCaughtException::class.java.name, "Error while sendErrorMail$report")
            sendErrorMail(report)
        } catch (ignore: Throwable) {
            Log.e(
                UnCaughtException::class.java.name,
                "Error while sending error e-mail", ignore
            )
        }
    }

    private fun sendErrorMail(errorContent: StringBuilder?) {
        val builder = AlertDialog.Builder(context)
        object : Thread() {
            override fun run() {
                Looper.prepare()
                builder.setTitle("Alert...!!")
                builder.setCancelable(false)
                builder.setMessage(
                    "Sorry for your inconvenience.Verifone Pos app has been stopped working , " +
                            "Please Tap Start to start the application. THANK YOU !!"
                )
                    .setCancelable(false)
                    .setPositiveButton("Start") { _, _ ->
                        // sendCrashIntent(errorContent)
                        // (context as Activity).finishAffinity()
                        forceStart()
                    }
                    .setNeutralButton("Cancel") { dialog, _ ->
                        dialog.dismiss()
                        (context as Activity).finishAffinity()
                    }
                val alert: AlertDialog? = builder.create()
                alert?.show()
                Looper.loop()
            }
        }.start()
    }

    //Method to Handle Force start after App crashes:
    private fun forceStart() {
        (context as Activity).finish()
        val intent = Intent(context, MainActivity::class.java).apply {
            flags =
                Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_NEW_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            VerifoneApp.appContext.applicationContext,
            0,
            intent,
            PendingIntent.FLAG_ONE_SHOT
        )
        val alarmManager =
            VerifoneApp.appContext.applicationContext.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        alarmManager.set(AlarmManager.RTC, System.currentTimeMillis() + 100, pendingIntent)
    }

    //Method to send Crash Report via a Mail Intent:-
    private fun sendCrashIntent(errorContent: StringBuilder?) {
        val sendIntent = Intent(Intent.ACTION_SEND)
        val subject = "Bonusone Business Crash Report Logs"
        val body = StringBuilder("")
        body.append(errorContent).append('\n').append('\n')
        sendIntent.type = "message/rfc822"
        sendIntent.putExtra(Intent.EXTRA_EMAIL, aEmailList)
        sendIntent.putExtra(Intent.EXTRA_CC, aEmailCCList)
        sendIntent.putExtra(Intent.EXTRA_TEXT, body.toString())
        sendIntent.putExtra(Intent.EXTRA_SUBJECT, subject)
        sendIntent.type = "message/rfc822"
        context.startActivity(sendIntent)
    }


    companion object {
        private val aEmailList =
            arrayOf("bonushub.pe@gmail.com")
        private val aEmailCCList =
            arrayOf("manish.kumar1@bonushub.co.in")
    }

}