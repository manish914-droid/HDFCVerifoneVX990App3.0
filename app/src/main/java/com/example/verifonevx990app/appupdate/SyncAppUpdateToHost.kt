package com.example.verifonevx990app.appupdate

import android.util.Log
import com.example.verifonevx990app.vxUtils.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

class SyncAppUpdateToHost(private var appUpdateISOPacket: ByteArray, var appUpdateCB: (String, String, String) -> Unit) {
    private var successResponseCode: String? = null
    private var responseProcessingCode: String? = null
    private var responseField60Value: String? = null
    private var appUpdateProcessingCode = ProcessingCode.APP_UPDATE.code

    init {
        GlobalScope.launch(Dispatchers.IO) {
            sendAppUpdateToHost(appUpdateISOPacket)
        }
    }

    private suspend fun sendAppUpdateToHost(appUpdateISOPacket: ByteArray) {

        HitServer.hitServer(appUpdateISOPacket, { result, success ->
            if (success) {
                //Below we are incrementing previous ROC (Because ROC will always be incremented whenever Server Hit is performed:-
                ROCProviderV2.incrementFromResponse(
                    ROCProviderV2.getRoc(AppPreference.getBankCode()).toString(),
                    AppPreference.getBankCode()
                )

                val responseIsoData: IsoDataReader = readIso(result, false)
                logger("Transaction RESPONSE ", "---", "e")
                logger("Transaction RESPONSE --->>", responseIsoData.isoMap, "e")
                Log.e(
                    "Success 39-->  ",
                    responseIsoData.isoMap[39]?.parseRaw2String().toString() + "---->" +
                            responseIsoData.isoMap[58]?.parseRaw2String().toString()
                )
                successResponseCode = (responseIsoData.isoMap[39]?.parseRaw2String().toString())
                responseProcessingCode = (responseIsoData.isoMap[3]?.rawData.toString())
                responseField60Value = (responseIsoData.isoMap[60]?.rawData.toString())
                appUpdateCB(
                    successResponseCode.toString(),
                    responseProcessingCode.toString(),
                    responseField60Value.toString()
                )
            } else {
                ROCProviderV2.incrementFromResponse(ROCProviderV2.getRoc(AppPreference.getBankCode()).toString(), AppPreference.getBankCode())
                appUpdateCB("", result, "")
            }
        }, {
            //backToCalled(it, false, true)
        },isAppUpdate = true)
    }
}