package com.example.verifonevx990app.bankEmiEnquiry

import android.text.TextUtils
import android.util.Log
import com.example.verifonevx990app.R
import com.example.verifonevx990app.emv.transactionprocess.SyncReversalToHost
import com.example.verifonevx990app.main.MainActivity
import com.example.verifonevx990app.vxUtils.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class SyncEmiEnquiryToHost(
    var context: MainActivity,
    var syncTransactionCallback: (Boolean, IsoDataReader?, String?) -> Unit
) {
    private var successResponseCode: String? = null
    private var transMsg: String? = null

    suspend fun synToHost(isoDataPacket: IsoDataWriter) {
        // Sync Reversal to host
        if (!TextUtils.isEmpty(AppPreference.getString(AppPreference.GENERIC_REVERSAL_KEY))) {
            withContext(Dispatchers.Main) {
                context.hideProgress()
                context.getString(R.string.reversal_data_sync).let { context.showProgress(it) }
            }
            SyncReversalToHost(AppPreference.getReversal()) { isSyncToHost, transMsg ->
                context.hideProgress()
                if (isSyncToHost) {
                    AppPreference.clearReversal()
                    GlobalScope.launch(Dispatchers.IO) {
                        synToHost(isoDataPacket)
                    }
                } else {
                    GlobalScope.launch(Dispatchers.Main) {
                        VFService.showToast(transMsg)
                        //Uploading Reversal Fails
                    }
                }
            }
        } else {
            logger("EMI Enquiry PACKET --->>", isoDataPacket.isoMap, "e")
            val isoByteArray = isoDataPacket.generateIsoByteRequest()
            HitServer.hitServer(isoByteArray, { result, success ->
                if (success) {
                    //Below we are incrementing previous ROC (Because ROC will always be incremented whenever Server Hit is performed:-
                    ROCProviderV2.incrementFromResponse(
                        ROCProviderV2.getRoc(AppPreference.getBankCode()).toString(),
                        AppPreference.getBankCode()
                    )
                    Log.d("Success Data:- ", result)
                    val responseIsoData: IsoDataReader = readIso(result, false)
                    logger("Transaction RESPONSE --->>", responseIsoData.isoMap, "e")
                    successResponseCode = (responseIsoData.isoMap[39]?.parseRaw2String().toString())
                    transMsg = responseIsoData.isoMap[58]?.parseRaw2String().toString()
                    if (successResponseCode == "00") {
                        syncTransactionCallback(true, responseIsoData, transMsg)
                    } else {
                        syncTransactionCallback(false, responseIsoData, transMsg)
                    }
                } else {
                    //Below we are incrementing previous ROC (Because ROC will always be incremented whenever Server Hit is performed:-
                    ROCProviderV2.incrementFromResponse(
                        ROCProviderV2.getRoc(AppPreference.getBankCode()).toString(),
                        AppPreference.getBankCode()
                    )
                    syncTransactionCallback(false, null, result)
                }
            }, {
                //backToCalled(it, false, true)
            })


        }


    }
}