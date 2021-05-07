package com.example.verifonevx990app.utils

import android.app.Activity
import android.app.Dialog
import android.content.Intent
import android.os.*
import android.util.Log
import android.view.Window
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.verifonevx990app.R
import com.example.verifonevx990app.emv.transactionprocess.MultiSelectionAppAdapter
import com.example.verifonevx990app.emv.transactionprocess.VFTransactionActivity
import com.example.verifonevx990app.main.DetectError
import com.example.verifonevx990app.main.MainActivity
import com.example.verifonevx990app.vxUtils.*
import com.vfi.smartpos.deviceservice.aidl.EMVHandler
import com.vfi.smartpos.deviceservice.aidl.IEMV
import com.vfi.smartpos.deviceservice.constdefine.ConstPBOCHandler
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class GenericReadCardData(
    var activity: Activity,
    var iemv: IEMV?,
    var readCardCallback: (String) -> Unit
) : EMVHandler.Stub() {
    private var TAG = GenericReadCardData::class.java.name

    override fun onRequestOnlineProcess(aaResult: Bundle?) {
        Log.d(MainActivity.TAG, "onRequestOnlineProcess...")
    }

    override fun onSelectApplication(appList: MutableList<Bundle>?) {
        try {
            //In case of Card Having Multiple Application Selection:-
            if (appList != null) {
                inflateAppsDialog(appList) { multiAppPosition ->
                    iemv?.importAppSelection(multiAppPosition)
                }
            }
        } catch (ex: DeadObjectException) {
            ex.printStackTrace()
            Handler(Looper.getMainLooper()).postDelayed({
                GlobalScope.launch {
                    VFService.connectToVFService(VerifoneApp.appContext)
                    delay(200)
                    //  iemv = VFService.vfIEMV
                    delay(100)
                    (activity as VFTransactionActivity).doProcessCard()
                }
            }, 200)
            println("VfEmvHandler error in onSelectApplication" + ex.message)
        } catch (ex: RemoteException) {
            ex.printStackTrace()
            Handler(Looper.getMainLooper()).postDelayed(Runnable {
                GlobalScope.launch {
                    VFService.connectToVFService(VerifoneApp.appContext)
                    delay(200)
                    //  iemv = VFService.vfIEMV
                    delay(100)
                    (activity as VFTransactionActivity).doProcessCard()
                }
            }, 200)
            println("VfEmvHandler error in onSelectApplication" + ex.message)
        } catch (ex: Exception) {
            ex.printStackTrace()
            println("VfEmvHandler error in onSelectApplication" + ex.message)
        }

        /*  VFService.showToast("onSelectApplication..." + (appList?.get(0) ?: ""))
          iemv?.importAppSelection(1)*/
    }

    override fun onConfirmCertInfo(certType: String?, certInfo: String?) {}

    override fun onRequestAmount() {
        Log.d(TAG, "Request Amount.......")
    }

    override fun onConfirmCardInfo(info: Bundle?) {
        //Reading Card Bin Value
        val track2 = info?.getString(ConstPBOCHandler.onConfirmCardInfo.info.KEY_TRACK2_String)
        var cardBinValue: String? = null
        if (null != track2) {
            var a = track2.indexOf('D')
            if (a > 0) {
                cardBinValue = track2.substring(0, a)
            } else {
                a = track2.indexOf('=')

                if (a > 0) {
                    cardBinValue = track2.substring(0, a)
                }
            }
        }

        //Check For Card is of Valid or Not
        if (!cardLuhnCheck(cardBinValue ?: "")) {
            val bun = Bundle()
            bun.putString("ERROR", "Invalid Card Number")
            onTransactionResult(DetectError.IncorrectPAN.errorCode, bun)
        }

        readCardCallback(cardBinValue ?: "")

    }

    override fun onTransactionResult(result: Int, data: Bundle?) {
        Log.d("FalbckCode GenricReader", result.toString())
    }

    override fun onRequestInputPIN(isOnlinePin: Boolean, retryTimes: Int) {
        println("Invalid pin$retryTimes")
    }

    //App Selection Rv
    private fun inflateAppsDialog(appList: MutableList<Bundle>, multiAppCB: (Int) -> Unit) {
        activity.runOnUiThread {
            var appSelectedPosition = 1
            val dialog = Dialog(activity)
            dialog.apply {
                requestWindowFeature(Window.FEATURE_NO_TITLE)
                setCancelable(false)
                setContentView(R.layout.multiapp_selection_layout)
                /*val body = dialog.findViewById(R.id.body) as TextView
                    body.text = title*/
                this.findViewById<RecyclerView>(R.id.apps_Rv)?.apply {
                    // set a LinearLayoutManager to handle Android
                    // RecyclerView behavior
                    layoutManager = LinearLayoutManager(activity)
                    // set the custom adapter to the RecyclerView
                    adapter = MultiSelectionAppAdapter(appList, dialog) {
                        appSelectedPosition = it
                    }
                }

                this.findViewById<BHButton>(R.id.cancel_btnn)?.setOnClickListener {
                    logger("cancel_Btn", "$appSelectedPosition  ", "e")
                    dismiss()
                    iemv?.stopCheckCard()
                    multiAppCB(0)
                    val intent = Intent(activity, MainActivity::class.java)
                    intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    activity.startActivity(intent)
                }

                this.findViewById<BHButton>(R.id.ok_btnn)?.setOnClickListener {
                    // iemv?.importAppSelection(appSelectedPosition)
                    logger("OkBtn", "$appSelectedPosition  ", "e")
                    try {
                        multiAppCB(appSelectedPosition)
                        dialog.dismiss()
                    } catch (ex: java.lang.Exception) {
                        ex.printStackTrace()
                        dialog.dismiss()
                        (activity as VFTransactionActivity).alertBoxWithAction(
                            null,
                            null,
                            activity.getString(R.string.app_selection_failed),
                            activity.getString(R.string.please_reinitiate_transaction),
                            false,
                            activity.getString(R.string.positive_button_ok),
                            {
                                activity.startActivity(
                                    Intent(
                                        activity,
                                        MainActivity::class.java
                                    ).apply {
                                        flags =
                                            Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_CLEAR_TASK
                                    })
                            },
                            {})
                    }
                }
            }.show()
        }
    }
}