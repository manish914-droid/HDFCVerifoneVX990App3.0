package com.example.verifonevx990app.offlinemanualsale

import android.util.Log
import com.example.verifonevx990app.realmtables.BatchFileDataTable
import com.example.verifonevx990app.vxUtils.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

class SyncOfflineSaleSettlementToHost(var offlineSyncCB: (Boolean, String) -> Unit) {
    private var responseMsg: String? = null
    private var successList: MutableList<Boolean> = mutableListOf()

    init {
        GlobalScope.launch(Dispatchers.IO) {
            val batchData = BatchFileDataTable.selectOfflineSaleSettleBatchData()
            if (batchData.size > 0) {
                for (i in 0 until batchData.size) {
                    val isoData = CreateOfflineSalePacket(batchData[i]).createOfflineSalePacket()
                    val isoByteArray = isoData.generateIsoByteRequest()
                    sendOfflineSaleToHost(isoByteArray) { offlineSuccess, responseValidationMsg ->
                        responseMsg = responseValidationMsg
                        if (offlineSuccess) {
                            BatchFileDataTable.updateOfflineSaleStatus(batchData[i].invoiceNumber)
                            VFService.showToast("Offline Sale of Invoice: ${batchData[i].invoiceNumber} Uploaded Successfully")
                            successList.add(true)
                        } else {
                            VFService.showToast("Offline Sale of Invoice: ${batchData[i].invoiceNumber} Uploading Failed")
                            successList.add(false)
                        }
                    }
                }
                if (successList.contains(false)) {
                    offlineSyncCB(false, responseMsg ?: "")
                } else {
                    offlineSyncCB(true, responseMsg ?: "")
                }
            } else {
                offlineSyncCB(true, "")
            }
        }
    }

    private suspend fun sendOfflineSaleToHost(
        offlineSaleIso: ByteArray,
        offlineSaleCB: (Boolean, String) -> Unit
    ) {
        var isBool: Boolean = false
        HitServer.hitServer(offlineSaleIso, { result, success ->
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
                val successResponseCode = (responseIsoData.isoMap[39]?.parseRaw2String().toString())
                val responseValidationMsg =
                    (responseIsoData.isoMap[58]?.parseRaw2String().toString())
                isBool = successResponseCode == "00"
                offlineSaleCB(isBool, responseValidationMsg)
            } else {
                isBool = false
                offlineSaleCB(isBool, "")
            }
        }, {
            //backToCalled(it, false, true)
        })
    }
}