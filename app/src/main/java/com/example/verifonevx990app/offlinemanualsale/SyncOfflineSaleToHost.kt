package com.example.verifonevx990app.offlinemanualsale

import android.content.Context
import android.text.TextUtils
import android.util.Log
import com.example.verifonevx990app.realmtables.BatchFileDataTable
import com.example.verifonevx990app.vxUtils.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

class SyncOfflineSaleToHost(
    var context: Context,
    var autoSettleCode: String,
    var offlineSyncCB: (Int, String) -> Unit
) {
    private var successList: MutableList<Boolean> = mutableListOf()
    private var validationMsg: String? = null

    init {
        if (autoSettleCode == "1") {
            //Below code will run in case we get Auto Settle Code from Host Response:-
            GlobalScope.launch(Dispatchers.IO) {
                val batchData = BatchFileDataTable.selectOfflineSaleBatchData()
                if (batchData.size > 0) {
                    for (i in 0 until batchData.size) {
                        val isoData =
                            CreateOfflineSalePacket(batchData[i]).createOfflineSalePacket()
                        val isoByteArray = isoData.generateIsoByteRequest()
                        //     withContext(Dispatchers.IO) {
                        sendOfflineSaleToHost(isoByteArray) { offlineSuccess, serverValidationMsg ->
                            if (!TextUtils.isEmpty(serverValidationMsg))
                                validationMsg = serverValidationMsg
                            if (offlineSuccess) {
                                BatchFileDataTable.updateOfflineSaleStatus(batchData[i].invoiceNumber)
                                VFService.showToast("Offline Sale of Invoice: ${batchData[i].invoiceNumber} Uploaded Successfully")
                                successList.add(true)
                            } else {
                                VFService.showToast("Offline Sale of Invoice: ${batchData[i].invoiceNumber} Upload Failed")
                                successList.add(false)
                            }
                        }
                        //  }
                    }
                    if (successList.contains(false)) {
                        offlineSyncCB(2, validationMsg ?: "")
                    } else {
                        offlineSyncCB(1, validationMsg ?: "")
                    }
                } else {
                    offlineSyncCB(1, validationMsg ?: "")
                }
            }
        } else {
            GlobalScope.launch(Dispatchers.IO) {
                val batchData = BatchFileDataTable.selectOfflineSaleBatchData()
                if (batchData.size > 0) {
                    for (i in 0 until batchData.size) {
                        val isoData =
                            CreateOfflineSalePacket(batchData[i]).createOfflineSalePacket()
                        val isoByteArray = isoData.generateIsoByteRequest()
                        sendOfflineSaleToHost(isoByteArray) { offlineSuccess, serverValidationMsg ->
                            if (!TextUtils.isEmpty(serverValidationMsg))
                                validationMsg = serverValidationMsg
                            if (offlineSuccess) {
                                BatchFileDataTable.updateOfflineSaleStatus(batchData[i].invoiceNumber)
                                VFService.showToast("Offline Sale of Invoice: ${batchData[i].invoiceNumber} Uploaded Successfully")
                                successList.add(true)
                            } else {
                                VFService.showToast("Offline Sale of Invoice: ${batchData[i].invoiceNumber} Upload Failed")
                                successList.add(false)
                            }
                        }
                    }
                    if (successList.contains(false)) {
                        offlineSyncCB(2, validationMsg ?: "")
                    } else {
                        offlineSyncCB(1, validationMsg ?: "")
                    }
                } else {
                    offlineSyncCB(1, validationMsg ?: "")
                }
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

                var serverValidationMsg: String? = null
                val successResponseCode = (responseIsoData.isoMap[39]?.parseRaw2String().toString())
                serverValidationMsg = if (responseIsoData.isoMap[58] != null) {
                    (responseIsoData.isoMap[58]?.parseRaw2String().toString())
                } else {
                    ""
                }
                isBool = successResponseCode == "00"
                offlineSaleCB(isBool, serverValidationMsg)
            } else {
                isBool = false
                offlineSaleCB(isBool, result)
            }
        }, {
            //backToCalled(it, false, true)
        })
    }
}