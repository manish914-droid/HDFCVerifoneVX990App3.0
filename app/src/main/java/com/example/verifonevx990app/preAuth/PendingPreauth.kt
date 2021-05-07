package com.example.verifonevx990app.preAuth

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.text.TextUtils
import android.util.Log
import com.example.verifonevx990app.R
import com.example.verifonevx990app.emv.transactionprocess.CardProcessedDataModal
import com.example.verifonevx990app.emv.transactionprocess.SyncReversalToHost
import com.example.verifonevx990app.main.MainActivity
import com.example.verifonevx990app.main.PrefConstant
import com.example.verifonevx990app.offlinemanualsale.SyncOfflineSaleToHost
import com.example.verifonevx990app.realmtables.BatchFileDataTable
import com.example.verifonevx990app.vxUtils.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.Serializable
import java.text.SimpleDateFormat
import java.util.*

class PendingPreauth(var context: Context) {

    private var successResponseCode: String? = null
    private val cardProcessedData: CardProcessedDataModal by lazy { CardProcessedDataModal() }
    var counter = 0
    val pendingPreauthList = mutableListOf<PendingPreauthData>()


    private fun doPendingPreAuth(counter: Int) {
        val transactionalAmount = 0L //authCompletionData.authAmt?.replace(".", "")?.toLong() ?: 0L
        cardProcessedData.apply {
            setTransactionAmount(transactionalAmount)
            setTransType(TransactionType.PENDING_PREAUTH.type)
            setProcessingCode(ProcessingCode.PENDING_PREAUTH.code)
        }
        val transactionISO =
            CreateAuthPacket().createPendingPreAuthISOPacket(cardProcessedData, counter)
        //Here we are Saving Date , Time and TimeStamp in CardProcessedDataModal:-
        try {
            val date2: Long = Calendar.getInstance().timeInMillis
            val timeFormater = SimpleDateFormat("HHmmss", Locale.getDefault())
            cardProcessedData.setTime(timeFormater.format(date2))
            val dateFormater = SimpleDateFormat("MMdd", Locale.getDefault())
            cardProcessedData.setDate(dateFormater.format(date2))
            cardProcessedData.setTimeStamp(date2.toString())
        } catch (ex: Exception) {
            ex.printStackTrace()
        }
        logger("Transaction REQUEST PACKET --->>", transactionISO.isoMap, "e")
        //  runOnUiThread { showProgress(getString(R.string.sale_data_sync)) }
        GlobalScope.launch(Dispatchers.IO) {
            checkReversalPerformPendingPreAuthTransaction(
                transactionISO,
                cardProcessedData
            )
        }
    }

    fun confirmationAlert(
        title: String, msg: String, showCancelButton: Boolean = true,
        positiveButtonText: String = "YES"
    ) {
        val builder = androidx.appcompat.app.AlertDialog.Builder(context)
        builder.setTitle(title)
        builder.setMessage(msg)
            .setCancelable(false)
            .setPositiveButton(positiveButtonText) { dialog, _ ->
                dialog.dismiss()
                doPendingPreAuth(counter)
            }

        if (showCancelButton) {
            builder.setNegativeButton("No") { dialog, _ ->
                dialog.cancel()

            }
        }
        val alert: androidx.appcompat.app.AlertDialog = builder.create()
        alert.show()
    }

    private suspend fun checkReversalPerformPendingPreAuthTransaction(
        transactionISOByteArray: IsoDataWriter,
        cardProcessedDataModal: CardProcessedDataModal
    ) {

        //Sending Transaction Data Packet to Host:-(In case of no reversal)
        if (TextUtils.isEmpty(AppPreference.getString(AppPreference.GENERIC_REVERSAL_KEY))) {
            withContext(Dispatchers.Main) {
                (context as BaseActivity).showProgress("Getting Pending Pre-Auth From Server")
            }
            syncPreAuthTransactionToHost(
                transactionISOByteArray,
                cardProcessedDataModal,
                false
            ) { syncStatus, responseCode, transactionMsg ->
                GlobalScope.launch(Dispatchers.Main) {
                    (context as BaseActivity).hideProgress()
                }
                if (syncStatus && responseCode == "00") {
                    //  AppPreference.clearReversal()
                    val resIso = readIso(transactionMsg, false)
                    logger("RESP DATA..>", transactionMsg)
                    logger("PendingPre RES -->", resIso.isoMap, "e")
                    val autoSettlementCheck =
                        resIso.isoMap[60]?.parseRaw2String().toString()
                    val f62 = resIso.isoMap[62]?.parseRaw2String() ?: ""
                    val f62Arr = f62.split("|")
                    if (f62Arr.size >= 2) {
                        for (e in 2..(f62Arr.lastIndex)) {
                            if (f62Arr[e].isNotEmpty()) {
                                val ip = PendingPreauthData()
                                ip.pendingPreauthDataParser(f62Arr[e])
                                pendingPreauthList.add(ip)
                            }
                        }
                        if (f62Arr[0] == "1") {
                            counter += f62Arr[1].toInt()
                            //Again Request for pending pre auth transaction with next counter
                            doPendingPreAuth(counter)
                        } else {
                            logger("Pending Preauth", "Finish")
                            //--
                            (context as BaseActivity).transactFragment(
                                PendingPreAuthFragment()
                                    .apply {
                                        arguments = Bundle().apply {
                                            putSerializable(
                                                "PreAuthData",
                                                pendingPreauthList as ArrayList<PendingPreauthData>
                                            )
                                            putSerializable(
                                                "CardProcessData",
                                                cardProcessedDataModal
                                            )
                                        }
                                    })

                            //--
                            /*PrintUtil(context).printPendingPreauth(
                                cardProcessedDataModal,
                                context,
                                pendingPreauthList
                            ) { printCB ->
                                if (!printCB) {
                                    //Here we are Syncing Offline Sale if we have any in Batch Table and also Check Sale Response has Auto Settlement enabled or not:-
                                    //If Auto Settlement Enabled Show Pop Up and User has choice whether he/she wants to settle or not:-
                                    if (!TextUtils.isEmpty(autoSettlementCheck))
                                        syncOfflineSaleAndAskAutoSettlement(
                                            autoSettlementCheck.substring(
                                                0,
                                                1
                                            ), context as BaseActivity
                                        )
                                }

                            }*/
                        }
                    } else {
                        GlobalScope.launch(Dispatchers.Main) {
                            (context as BaseActivity).getInfoDialog(
                                "Info",
                                "No more Pending Pre-auth available"
                            ) {}
                        }
                    }

                } else if (syncStatus && responseCode != "00") {
                    //  AppPreference.clearReversal()
                    val resIso = readIso(transactionMsg, false)
                    logger("RESP DATA..>", transactionMsg)
                    logger("PendingPre RES -->", resIso.isoMap, "e")
                    val autoSettlementCheck =
                        resIso.isoMap[60]?.parseRaw2String().toString()
                    //---
                    GlobalScope.launch(Dispatchers.Main) {
                        context.getString(R.string.error_hint).let {
                            (context as BaseActivity).alertBoxWithAction(null,
                                null,
                                it,
                                resIso.isoMap[58]?.parseRaw2String().toString(),
                                false,
                                context.getString(R.string.positive_button_ok),
                                { alertPositiveCallback ->
                                    if (alertPositiveCallback) {
                                        if (!TextUtils.isEmpty(autoSettlementCheck))
                                            syncOfflineSaleAndAskAutoSettlement(
                                                autoSettlementCheck.substring(
                                                    0,
                                                    1
                                                ), context as BaseActivity
                                            )
                                    }
                                },
                                {})
                        }
                    }
                } else {
                    //   AppPreference.clearReversal()
                    GlobalScope.launch(Dispatchers.Main) {
                        (context as BaseActivity).hideProgress()
                        (context as BaseActivity).alertBoxWithAction(null,
                            null,
                            (context as BaseActivity).getString(R.string.connection_failed),
                            (context as BaseActivity).getString(R.string.pending_preauthdetails),
                            false,
                            (context as BaseActivity).getString(R.string.positive_button_ok),
                            { alertPositiveCallback ->
                                if (alertPositiveCallback)
                                    declinedTransaction()
                            },
                            {})

                        //    VFService.showToast(transactionMsg)
                    }
                }
            }
        }
        //Sending Reversal Data Packet to Host:-(In Case of reversal)
        else {
            if (!TextUtils.isEmpty(AppPreference.getString(AppPreference.GENERIC_REVERSAL_KEY))) {
                withContext(Dispatchers.Main) {
                    (context as BaseActivity).showProgress((context as MainActivity).getString(R.string.reversal_data_sync))
                }
                SyncReversalToHost(AppPreference.getReversal()) { isSyncToHost, transMsg ->
                    (context as BaseActivity).hideProgress()
                    if (isSyncToHost) {
                        AppPreference.clearReversal()
                        GlobalScope.launch(Dispatchers.IO) {
                            checkReversalPerformPendingPreAuthTransaction(
                                transactionISOByteArray,
                                cardProcessedDataModal
                            )
                        }
                    } else {
                        GlobalScope.launch(Dispatchers.Main) {
                            //  VFService.showToast(transMsg)
                        }
                    }
                }
            }
        }
    }

    //Below method is used to handle Transaction Declined case:-
    fun declinedTransaction() {
        context.startActivity(Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_CLEAR_TASK
        })
    }

    private suspend fun syncPreAuthTransactionToHost(
        transISODataWriter: IsoDataWriter?,
        cardProcessedDataModal: CardProcessedDataModal?,
        isReversal: Boolean,
        syncAuthTransactionCallback: (Boolean, String, String) -> Unit
    ) {
        if (!TextUtils.isEmpty(AppPreference.getString(AppPreference.GENERIC_REVERSAL_KEY))) {
            transISODataWriter?.mti = Mti.REVERSAL.mti
            transISODataWriter?.additionalData?.get("F56reversal")?.let {
                transISODataWriter.addFieldByHex(
                    56,
                    it
                )
            }
            if (transISODataWriter != null) {
                addIsoDateTime(transISODataWriter)
            }
        } else {
            transISODataWriter?.mti = Mti.PRE_AUTH_MTI.mti
        }
        val transactionISOByteArray = transISODataWriter?.generateIsoByteRequest()
        //  val reversalPacket = Gson().toJson(transISODataWriter)
        // AppPreference.saveString(AppPreference.GENERIC_REVERSAL_KEY, reversalPacket)
        if (transactionISOByteArray != null) {
            HitServer.hitServer(transactionISOByteArray, { result, success ->
                //Save Server Hit Status in Preference:-
                AppPreference.saveBoolean(PrefConstant.SERVER_HIT_STATUS.keyName.toString(), true)
                try {
                    if (success) {
                        //Below we are incrementing previous ROC (Because ROC will always be incremented whenever Server Hit is performed:-
                        ROCProviderV2.incrementFromResponse(
                            ROCProviderV2.getRoc(AppPreference.getBankCode()).toString(),
                            AppPreference.getBankCode()
                        )
                        Log.d("Success Data:- ", result)
                        val responseIsoData: IsoDataReader = readIso(result, false)
                        logger("Transaction RESPONSE --->>", responseIsoData.isoMap, "e")
                        Log.e(
                            "Success 39-->  ",
                            responseIsoData.isoMap[39]?.parseRaw2String()
                                .toString() + "---->" + responseIsoData.isoMap[58]?.parseRaw2String()
                                .toString()
                        )
                        successResponseCode =
                            (responseIsoData.isoMap[39]?.parseRaw2String().toString())

                        val authCode = (responseIsoData.isoMap[38]?.parseRaw2String().toString())
                        cardProcessedDataModal?.setAuthCode(authCode.trim())
                        //Here we are getting RRN Number :-
                        val rrnNumber = responseIsoData.isoMap[37]?.rawData ?: ""
                        cardProcessedDataModal?.setRetrievalReferenceNumber(rrnNumber)
                        val encrptedPan = responseIsoData.isoMap[57]?.parseRaw2String().toString()
                        cardProcessedDataModal?.setEncryptedPan(encrptedPan)
                        Log.e("ENCRYPT_PAN", "---->    $encrptedPan")


                        var responseAmount = responseIsoData.isoMap[4]?.rawData ?: "0"
                        responseAmount = responseAmount.toLong().toString()
                        cardProcessedDataModal?.setAmountInResponse(responseAmount)
                        Log.e("TransAmountF4", "---->    $responseAmount")

                        if (successResponseCode == "00") {
                            //   VFService.showToast("Auth-Complete Success")
                            //   AppPreference.clearReversal()
                            syncAuthTransactionCallback(
                                true,
                                successResponseCode.toString(),
                                result
                            )

                        } else {
                            //   AppPreference.clearReversal()
                            syncAuthTransactionCallback(
                                true,
                                successResponseCode.toString(),
                                result
                            )
                            //  VFService.showToast("Transaction Fail Error Code = ${responseIsoData.isoMap[39]?.parseRaw2String().toString()}")
                        }
                    } else {
                        //Below we are incrementing previous ROC (Because ROC will always be incremented whenever Server Hit is performed:-
                        ROCProviderV2.incrementFromResponse(
                            ROCProviderV2.getRoc(AppPreference.getBankCode()).toString(),
                            AppPreference.getBankCode()
                        )
                        /* if (!isReversal) {
                         AppPreference.clearReversal()
                     }*/
                        syncAuthTransactionCallback(false, successResponseCode.toString(), result)
                        Log.d("Failure Data:- ", result)
                    }
                } catch (ex: Exception) {
                    ex.printStackTrace()
                }
            }, {
                //backToCalled(it, false, true)
            })
        }
    }


    //Below method is used to Sync Offline Sale and Ask for Auto Settlement:-
    private fun syncOfflineSaleAndAskAutoSettlement(autoSettleCode: String, context: BaseActivity) {
        val offlineSaleData = BatchFileDataTable.selectOfflineSaleBatchData()
        if (offlineSaleData.size > 0) {
            context.runOnUiThread {
                context.getString(R.string.please_wait_offline_sale_sync).let {
                    context.showProgress(
                        it
                    )
                }
            }
            SyncOfflineSaleToHost(context, autoSettleCode) { offlineSaleStatus, validationMsg ->
                if (offlineSaleStatus == 1)
                    context.runOnUiThread {
                        context.hideProgress()
                        if (autoSettleCode == "1") {
                            context.alertBoxWithAction(
                                null, null,
                                context.getString(R.string.batch_settle),
                                context.getString(R.string.do_you_want_to_settle_batch),
                                true, context.getString(R.string.positive_button_yes), {
                                    context.startActivity(
                                        Intent(
                                            context,
                                            MainActivity::class.java
                                        ).apply {
                                            putExtra("appUpdateFromSale", true)
                                            flags =
                                                Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_CLEAR_TASK
                                        })
                                }, {
                                    context.startActivity(
                                        Intent(
                                            context,
                                            MainActivity::class.java
                                        ).apply {
                                            flags =
                                                Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_CLEAR_TASK
                                        })
                                })
                        } else {
                            context.startActivity(
                                Intent(context, MainActivity::class.java).apply {
                                    flags =
                                        Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_CLEAR_TASK
                                })
                        }
                    }
                else
                    context.runOnUiThread {
                        VFService.showToast(validationMsg)
                        context.startActivity(
                            Intent(
                                context,
                                MainActivity::class.java
                            ).apply {
                                flags =
                                    Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_CLEAR_TASK
                            })
                    }
            }
        } else {
            context.runOnUiThread {
                if (autoSettleCode == "1") {

                    context.alertBoxWithAction(null, null,
                        context.getString(R.string.batch_settle),
                        context.getString(R.string.do_you_want_to_settle_batch),
                        true, context.getString(R.string.positive_button_yes), {
                            context.startActivity(
                                Intent(context, MainActivity::class.java).apply {
                                    putExtra("appUpdateFromSale", true)
                                    flags =
                                        Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_CLEAR_TASK
                                })
                        }, {
                            context.startActivity(
                                Intent(context, MainActivity::class.java).apply {
                                    flags =
                                        Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_CLEAR_TASK
                                })
                        })
                } else {
                    context.startActivity(
                        Intent(context, MainActivity::class.java).apply {
                            flags =
                                Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_CLEAR_TASK
                        })
                }
            }
        }
    }


}


// Used in Pending preauth for data creation which comes in response of Pending preauth at Field62
class PendingPreauthData : Serializable {
    var batch = 0
    var roc = 0
    var pan = ""
    var amount = 0f
    var date = ""
    var time = ""

    override fun toString(): String {
        return "batch = $batch\nroc = $roc\npan = $pan\namount = $amount\ndate = $date\ntime = $time"
    }

    fun pendingPreauthDataParser(str: String) {
        try {
            val li = str.split("~")
            batch = li[0].toInt()
            roc = li[1].toInt()
            pan = li[2]
            val d = li[3]
            date = d.substring(0, 8)
            date =
                "${date.substring(6, date.length)}/${date.substring(4, 6)}/${date.substring(0, 4)}"
            time = d.substring(8, d.length)
            time = "${time.substring(0, 2)}:${time.substring(2, 4)}:${time.substring(4, 6)}"
            amount = li[4].toFloat() / 100
        } catch (ex: Exception) {
            ex.printStackTrace()
        }
    }

}

class CreateReversal : java.lang.Exception("Reversal Generated SuccessFully")

