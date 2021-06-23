package com.example.verifonevx990app.preAuth

import android.content.Intent
import android.text.TextUtils
import android.util.Log
import com.example.verifonevx990app.R
import com.example.verifonevx990app.emv.transactionprocess.CardProcessedDataModal
import com.example.verifonevx990app.emv.transactionprocess.StubBatchData
import com.example.verifonevx990app.emv.transactionprocess.SyncReversalToHost
import com.example.verifonevx990app.main.MainActivity
import com.example.verifonevx990app.offlinemanualsale.SyncOfflineSaleToHost
import com.example.verifonevx990app.realmtables.BatchFileDataTable
import com.example.verifonevx990app.utils.printerUtils.EPrintCopyType
import com.example.verifonevx990app.utils.printerUtils.PrintUtil
import com.example.verifonevx990app.utils.printerUtils.checkForPrintReversalReceipt
import com.example.verifonevx990app.vxUtils.*
import com.google.gson.Gson
import kotlinx.coroutines.*

class SyncAuthTransToHost(activityContext: BaseActivity) {

    private var activityContext: BaseActivity? = null

    init {
        this.activityContext = activityContext
    }

    //
    suspend fun checkReversalPerformAuthTransaction(
        transactionISOByteArray: IsoDataWriter,
        cardProcessedDataModal: CardProcessedDataModal, cb: (Boolean, String) -> Unit
    ) {
        // //Sending Transaction Data Packet to Host:-(In case of no reversal)
        logger("coroutine 4", Thread.currentThread().name, "e")
        if (TextUtils.isEmpty(AppPreference.getString(AppPreference.GENERIC_REVERSAL_KEY))) {
            withContext(Dispatchers.Main) {
                activityContext?.getString(R.string.sale_data_sync)?.let {
                    activityContext?.showProgress(
                        it
                    )
                }
            }
            logger("coroutine 5", Thread.currentThread().name, "e")
            syncAuthTransactionToHost(
                transactionISOByteArray,
                cardProcessedDataModal
            ) { syncStatus, responseCode, transactionMsg ->
                //withactivityContext(Dispatchers.Main){
                activityContext?.hideProgress()
                //}
                if (syncStatus && responseCode == "00") {
                    GlobalScope.launch(Dispatchers.Main) {
                        activityContext?.let { txnSuccessToast(it) }
                    }
                    //Below we are saving batch data and print the receipt of transaction:-
                    val responseIsoData: IsoDataReader = readIso(transactionMsg, false)
                    val autoSettlementCheck =
                        responseIsoData.isoMap[60]?.parseRaw2String().toString()
                    AppPreference.clearReversal()

                    val encyPan = responseIsoData.isoMap[57]?.parseRaw2String().toString()
                    cardProcessedDataModal.setTrack2Data(encyPan)
                    //    cardProcessedDataModal.setPanNumberData(encyPan)
                    responseIsoData.isoMap[4]?.rawData?.toLong()?.let {
                        cardProcessedDataModal.setTransactionAmount(
                            it
                        )
                    }
                    StubBatchData(
                        cardProcessedDataModal.getTransType(),
                        cardProcessedDataModal,
                        null, autoSettlementCheck
                    ) { stubbedData ->

                        activityContext?.let {
                            printAndSaveAuthTransToBatchDataInDB(
                                stubbedData, autoSettlementCheck,
                                it
                            ) { isSuccess, msg ->
                                cb(true, msg)

                            }
                        }

                    }
                } else if (syncStatus && responseCode != "00") {
                    AppPreference.clearReversal()
                    val responseIsoData: IsoDataReader = readIso(transactionMsg, false)
                    val autoSettlementCheck =
                        responseIsoData.isoMap[60]?.parseRaw2String().toString()
                    //--------------
                    GlobalScope.launch(Dispatchers.Main) {
                        activityContext?.getString(R.string.error_hint)?.let {
                            activityContext?.alertBoxWithAction(null,
                                null,
                                it,
                                responseIsoData.isoMap[58]?.parseRaw2String().toString(),
                                false,
                                activityContext!!.getString(R.string.positive_button_ok),
                                { alertPositiveCallback ->
                                    if (alertPositiveCallback) {
                                        if (!TextUtils.isEmpty(autoSettlementCheck))
                                            syncOfflineSaleAndAskAutoSettlement(
                                                autoSettlementCheck.substring(
                                                    0,
                                                    1
                                                ), activityContext!!
                                            ) { isucc, msg ->
                                                cb(
                                                    false,
                                                    responseIsoData.isoMap[58]?.parseRaw2String()
                                                        .toString()
                                                )
                                            }
                                        else {
                                            // activityContext?.startActivity(Intent((activityContext as BaseActivity), MainActivity::class.java).apply { flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_CLEAR_TASK })
                                            cb(
                                                false,
                                                responseIsoData.isoMap[58]?.parseRaw2String()
                                                    .toString()
                                            )

                                        }
                                    }
                                },
                                {})
                        }
                    }
                } else {
                    //    VFService.showToast(transactionMsg)
                    activityContext?.hideProgress()
                    checkForPrintReversalReceipt(activityContext,"") {
                        (activityContext as BaseActivity).hideProgress()
                        GlobalScope.launch(Dispatchers.Main) {
                            (activityContext as BaseActivity).alertBoxWithAction(
                                null,
                                null,
                                activityContext!!.getString(R.string.declined),
                                activityContext!!.getString(R.string.transaction_delined_msg),
                                false,
                                activityContext!!.getString(R.string.positive_button_ok),
                                { alertPositiveCallback ->
                                    if (alertPositiveCallback)
                                        cb(
                                            false,
                                            activityContext!!.getString(R.string.transaction_delined_msg)
                                        )
                                    //  declinedTransaction()
                                },
                                {})
                        }
                    }

                }
            }
        }
        //Sending Reversal Data Packet to Host:-(In Case of reversal)
        else {
            if (!TextUtils.isEmpty(AppPreference.getString(AppPreference.GENERIC_REVERSAL_KEY))) {
                withContext(Dispatchers.Main) {
                    activityContext?.showProgress("Reversal Data Sync...")
                }
                SyncReversalToHost(
                    AppPreference.getReversal()
                ) { syncStatus, transactionMsg ->
                    activityContext?.hideProgress()
                    if (syncStatus) {
                        AppPreference.clearReversal()
                        GlobalScope.launch(Dispatchers.IO) {
                            checkReversalPerformAuthTransaction(
                                transactionISOByteArray,
                                cardProcessedDataModal
                            ) { bool, str ->
                                cb(bool, str)
                            }
                        }
                    } else {
                        GlobalScope.launch(Dispatchers.Main) {
                            VFService.showToast(transactionMsg)
                            cb(false, transactionMsg)
                            GlobalScope.launch(Dispatchers.Main) {
                                (activityContext as BaseActivity).alertBoxWithAction(
                                    null,
                                    null,
                                    activityContext!!.getString(R.string.reversal),
                                    activityContext!!.getString(R.string.reversal_upload_fail),
                                    false,
                                    activityContext!!.getString(R.string.positive_button_ok),
                                    { alertPositiveCallback ->

                                        if (alertPositiveCallback)
                                            cb(
                                                false,
                                                activityContext!!.getString(R.string.transaction_delined_msg)
                                            )
                                        // declinedTransaction()
                                    },
                                    {})
                            }


                        }
                    }
                }
            }
        }
    }

    private var successResponseCode: String? = null

    private suspend fun syncAuthTransactionToHost(
        transISODataWriter: IsoDataWriter?, cardProcessedDataModal: CardProcessedDataModal?,
        syncAuthTransactionCallback: (Boolean, String, String) -> Unit
    ) {
        if (!TextUtils.isEmpty(AppPreference.getString(AppPreference.GENERIC_REVERSAL_KEY))) {
            //In case of reversal add current date time , add field 56 data (Contains data for reversal) and change the MTI as Reversal Mti
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
            //In Case of no reversal
            transISODataWriter?.mti = Mti.PRE_AUTH_COMPLETE_MTI.mti
        }
        val transactionISOByteArray = transISODataWriter?.generateIsoByteRequest()
        val reversalPacket = Gson().toJson(transISODataWriter)
        AppPreference.saveString(AppPreference.GENERIC_REVERSAL_KEY, reversalPacket)
        /*   transISODataWriter?.isoMap?.let { logger("After Save", it, "e") }
           throw CreateReversal()*/

        if (transactionISOByteArray != null) {
            HitServer.hitServersale(transactionISOByteArray, { result, success, readtimeout ->
                try {
                    if (success) {
                        //Below we are incrementing previous ROC (Because ROC will always be incremented whenever Server Hit is performed:-
                        ROCProviderV2.incrementFromResponse(
                            ROCProviderV2.getRoc(AppPreference.getBankCode()).toString(),
                            AppPreference.getBankCode()
                        )

                        try {
                            val responseIsoData: IsoDataReader = readIso(result, false)
                        } catch (ex: Exception) {
                            ex.printStackTrace()
                            //   syncTransactionCallback(false, "", result, null)
                        }

                        Log.d("Success Data:- ", result)
                        val responseIsoData: IsoDataReader = readIso(result, false)
                        logger("Transaction RESPONSE --->>", responseIsoData.isoMap, "e")
                        Log.e(
                            "Success 39-->  ",
                            responseIsoData.isoMap[39]?.parseRaw2String().toString() + "---->" +
                                    responseIsoData.isoMap[58]?.parseRaw2String().toString()
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

                        syncAuthTransactionCallback(true, successResponseCode.toString(), result)


                    } else {
                        AppPreference.clearReversal()
                        //Below we are incrementing previous ROC (Because ROC will always be incremented whenever Server Hit is performed:-
                        ROCProviderV2.incrementFromResponse(
                            ROCProviderV2.getRoc(AppPreference.getBankCode()).toString(),
                            AppPreference.getBankCode()
                        )
                        syncAuthTransactionCallback(
                            false,
                            successResponseCode.toString(),
                            result
                        )
                        Log.d("Failure Data:- ", result)
                    }
                } catch (ex: Exception) {
                    ex.printStackTrace()
                    syncAuthTransactionCallback(
                        false,
                        "Something went wrong",
                        result
                    )
                    Log.e("EXCEPTION", "Something went wrong")
                }
            }, {
                //backToCalled(it, false, true)
            })
        }
    }


    //Below method is used to save (Tip adjust on a sale and change sale type to tip type data) in batch file data table and print the receipt of it:-
    private fun printAndSaveAuthTransToBatchDataInDB(
        stubbedData: BatchFileDataTable,
        autoSettlementCheck: String,
        activityContext: BaseActivity, cb: (Boolean, String) -> Unit
    ) {
        //Here we are saving printerReceiptData in BatchFileData Table:-
        saveTableInDB(stubbedData)
        PrintUtil(activityContext).printAuthCompleteChargeSlip(
            stubbedData,
            EPrintCopyType.MERCHANT,
            activityContext
        ) { printCB ->
            if (!printCB) {
                //Here we are Syncing Offline Sale if we have any in Batch Table and also Check Sale Response has Auto Settlement enabled or not:-
                //If Auto Settlement Enabled Show Pop Up and User has choice whether he/she wants to settle or not:-
                if (!TextUtils.isEmpty(autoSettlementCheck))
                    GlobalScope.launch(Dispatchers.Main) {
                        syncOfflineSaleAndAskAutoSettlement(
                            autoSettlementCheck.substring(0, 1),
                            activityContext
                        ) { isSuccess, msg ->
                            cb(isSuccess, msg)

                        }
                    }
            }
        }
        //  }
    }

    //Below method is used to handle Transaction Declined case:-
    private fun declinedTransaction() {
        //   finish()
        activityContext?.startActivity(Intent(activityContext, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_CLEAR_TASK
        })
    }

    //Below method is used to Sync Offline Sale and Ask for Auto Settlement:-
    private fun syncOfflineSaleAndAskAutoSettlement(
        autoSettleCode: String,
        activityContext: BaseActivity, cb: (Boolean, String) -> Unit
    ) {
        val offlineSaleData = BatchFileDataTable.selectOfflineSaleBatchData()
        // In case of offline transaction available
        if (offlineSaleData.size > 0) {
            (activityContext).showProgress(activityContext.getString(R.string.please_wait_offline_sale_sync))
            SyncOfflineSaleToHost(
                activityContext,
                autoSettleCode
            ) { offlineSaleStatus, validationMsg ->
                if (offlineSaleStatus == 1)
                    GlobalScope.launch(Dispatchers.Main) {
                        activityContext.hideProgress()
                        delay(1000)
                        if (autoSettleCode == "1") {
                            activityContext.alertBoxWithAction(
                                null, null,
                                activityContext.getString(R.string.batch_settle),
                                activityContext.getString(R.string.do_you_want_to_settle_batch),
                                true, activityContext.getString(R.string.positive_button_yes), {
                                    activityContext.startActivity(
                                        Intent(
                                            activityContext,
                                            MainActivity::class.java
                                        ).apply {
                                            putExtra("appUpdateFromSale", true)
                                            flags =
                                                Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_CLEAR_TASK
                                        })
                                }, {
                                    /* activityContext.startActivity(
                                         Intent(
                                             activityContext,
                                             MainActivity::class.java
                                         ).apply {
                                             flags =
                                                 Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_CLEAR_TASK
                                         })*/
                                    cb(
                                        true,
                                        "Not wanted to auto batch update after offline trans sync"
                                    )
                                })
                        } else {
                            /* activityContext.startActivity(
                                 Intent(activityContext, MainActivity::class.java).apply {
                                     flags =
                                         Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_CLEAR_TASK
                                 })*/
                            cb(true, "offline trans sync  (No autoupdate available)")
                        }
                    }
                else
                    GlobalScope.launch(Dispatchers.Main) {
                        activityContext.hideProgress()
                        //VFService.showToast(validationMsg)
                        activityContext.alertBoxWithAction(null, null,
                            activityContext.getString(R.string.offline_sale_uploading),
                            activityContext.getString(R.string.fail) + validationMsg,
                            false, activityContext.getString(R.string.positive_button_ok), {
                                /* activityContext.startActivity(
                                     Intent(activityContext, MainActivity::class.java).apply {
                                         flags =
                                             Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_CLEAR_TASK
                                     })*/
                                cb(true, "Offline sale upload fail ")
                            }, {

                            })
                    }
            }
        } else {
            GlobalScope.launch(Dispatchers.Main) {
                if (autoSettleCode == "1") {
                    activityContext.alertBoxWithAction(null, null,
                        activityContext.getString(R.string.batch_settle),
                        activityContext.getString(R.string.do_you_want_to_settle_batch),
                        true, activityContext.getString(R.string.positive_button_yes), {
                            activityContext.startActivity(
                                Intent((activityContext), MainActivity::class.java).apply {
                                    putExtra("appUpdateFromSale", true)
                                    flags =
                                        Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_CLEAR_TASK
                                })
                        }, {
                            /* activityContext.startActivity(
                                 Intent((activityContext), MainActivity::class.java).apply {
                                     flags =
                                         Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_CLEAR_TASK
                                 })*/
                            cb(true, "Not wanted to auto batch update after preauth done ")
                        })
                } else {
                    /* GlobalScope.launch(Dispatchers.Main) {
                         activityContext.startActivity(
                             Intent(activityContext, MainActivity::class.java).apply {
                                 flags =
                                     Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_CLEAR_TASK
                             })
                     }*/
                    cb(true, "No auto batch settle update needed, Preauth done")
                }
            }
        }
    }

}