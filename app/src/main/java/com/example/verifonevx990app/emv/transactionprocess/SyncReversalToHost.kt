package com.example.verifonevx990app.emv.transactionprocess

import android.text.TextUtils
import android.util.Log
import com.example.verifonevx990app.vxUtils.*
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

class SyncReversalToHost(
    private var transactionISOData: IsoDataWriter?,
    var syncTransactionCallback: (Boolean, String) -> Unit
) {

    private var successResponseCode: String? = null
    private var transMsg: String? = null

    init {

        GlobalScope.launch(Dispatchers.IO) {
            syncReversal()
        }
    }

    private suspend fun syncReversal() {

        if (!TextUtils.isEmpty(AppPreference.getString(AppPreference.GENERIC_REVERSAL_KEY))) {
            //In case of reversal add current date time , add field 56 data (Contains data for reversal) and change the MTI as Reversal Mti
            transactionISOData?.mti = Mti.REVERSAL.mti
            transactionISOData?.additionalData?.get("F56reversal")
                ?.let { transactionISOData?.addFieldByHex(56, it) }
            transactionISOData?.let { addIsoDateTime(it) }
        }
        val transactionISOByteArray = transactionISOData?.generateIsoByteRequest()
        if (transactionISOData != null) {
            transactionISOData?.isoMap?.let { logger("Transaction REQUEST PACKET --->>", it, "e") }
        }
        val reversalPacket = Gson().toJson(transactionISOData)
        AppPreference.saveString(AppPreference.GENERIC_REVERSAL_KEY, reversalPacket)
        transactionISOByteArray?.byteArr2HexStr()?.let { logger("PACKET-->", it) }
        // throw CreateReversal()

        if (transactionISOByteArray != null) {
            HitServer.hitServer(transactionISOByteArray, { result, success ->
                if (success) {
                    //Reversal save To Preference code here.............
                    //Below we are incrementing previous ROC (Because ROC will always be incremented whenever Server Hit is performed:-
                    ROCProviderV2.incrementFromResponse(
                        ROCProviderV2.getRoc(AppPreference.getBankCode()).toString(),
                        AppPreference.getBankCode()
                    )
                    Log.d("Success Data:- ", result)
                    val responseIsoData: IsoDataReader = readIso(result, false)
                    logger("Transaction RESPONSE ", "---", "e")
                    logger("Transaction RESPONSE --->>", responseIsoData.isoMap, "e")
                    Log.e(
                        "Success 39-->  ",
                        responseIsoData.isoMap[39]?.parseRaw2String().toString() + "---->" +
                                responseIsoData.isoMap[58]?.parseRaw2String().toString()
                    )
                    successResponseCode = (responseIsoData.isoMap[39]?.parseRaw2String().toString())
                    transMsg = responseIsoData.isoMap[58]?.parseRaw2String().toString()
                    if (successResponseCode == "00") {
                        syncTransactionCallback(true, transMsg.toString())
                    } else {
                        syncTransactionCallback(false, transMsg.toString())
                    }
                } else {
                    //Below we are incrementing previous ROC (Because ROC will always be incremented whenever Server Hit is performed:-
                    ROCProviderV2.incrementFromResponse(
                        ROCProviderV2.getRoc(AppPreference.getBankCode()).toString(),
                        AppPreference.getBankCode()
                    )
                    syncTransactionCallback(
                        false,
                        "Uploading reversal fail...."
                    )
                }
            }, {
                //backToCalled(it, false, true)
            })
        }
    }

}