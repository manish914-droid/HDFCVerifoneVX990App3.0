package com.example.verifonevx990app.voidofflinesale

import android.util.Log
import com.example.verifonevx990app.offlinemanualsale.CreateOfflineSalePacket
import com.example.verifonevx990app.realmtables.BatchFileDataTable
import com.example.verifonevx990app.vxUtils.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

class SyncVoidOfflineSale(
    private var voidOfflineData: BatchFileDataTable,
    var voidOfflineSyncCB: (Boolean) -> Unit
) {

    init {
        GlobalScope.launch(Dispatchers.IO) {
            //Here we are checking that if the offline sale is uploaded or not
            //if uploaded then void sale will directly uploaded to host
            //else first offline sale uploaded then void sale uploaded to host:-
            val offlineIsoData = CreateOfflineSalePacket(voidOfflineData).createOfflineSalePacket()
            val offlineIsoByteArray = offlineIsoData.generateIsoByteRequest()
            if (voidOfflineData.isOfflineSale) {
                sendOfflineSaleToHost(offlineIsoByteArray) { offlineUploadCB ->
                    if (offlineUploadCB) {
                        GlobalScope.launch(Dispatchers.IO) {
                            val voidIsoData =
                                CreateVoidOfflinePacket(voidOfflineData).createVoidOfflineSalePacket()
                            val voidIsoByteArray = voidIsoData.generateIsoByteRequest()
                            VFService.showToast("Void Offline Sale Uploaded Successfully")
                            BatchFileDataTable.updateOfflineSaleStatus(voidOfflineData.invoiceNumber)
                            sendVoidOfflineSaleToHost(voidIsoByteArray)
                        }
                    } else {
                        GlobalScope.launch(Dispatchers.Main) {
                            VFService.showToast("Offline Sale Upload Fails")
                            voidOfflineSyncCB(false)
                        }
                    }
                }
            } else {
                val voidIsoData =
                    CreateVoidOfflinePacket(voidOfflineData).createVoidOfflineSalePacket()
                val voidIsoByteArray = voidIsoData.generateIsoByteRequest()
                sendVoidOfflineSaleToHost(voidIsoByteArray)
            }
        }
    }

    private suspend fun sendVoidOfflineSaleToHost(offlineSaleIso: ByteArray) {
        var isBool: Boolean = false
        HitServer.hitServer(offlineSaleIso, { result, success ->
            if (success) {
                //Below we are incrementing previous ROC (Because ROC will always be incremented whenever Server Hit is performed:-
                /* ROCProviderV2.incrementFromResponse(
                     ROCProviderV2.getRoc(AppPreference.getBankCode()).toString(),
                     AppPreference.getBankCode()
                 )*/

                val responseIsoData: IsoDataReader = readIso(result, false)
                logger("Transaction RESPONSE ", "---", "e")
                logger("Transaction RESPONSE --->>", responseIsoData.isoMap, "e")
                Log.e(
                    "Success 39-->  ",
                    responseIsoData.isoMap[39]?.parseRaw2String().toString() + "---->" +
                            responseIsoData.isoMap[58]?.parseRaw2String().toString()
                )
                val successResponseCode = (responseIsoData.isoMap[39]?.parseRaw2String().toString())
                isBool = successResponseCode == "00"
                voidOfflineSyncCB(isBool)
            } else {
                ROCProviderV2.incrementFromResponse(
                    ROCProviderV2.getRoc(AppPreference.getBankCode()).toString(),
                    AppPreference.getBankCode()
                )
                isBool = false
                voidOfflineSyncCB(isBool)
            }
        }, {
            //backToCalled(it, false, true)
        })
    }

    //Below method is to upload offline sale to host:-
    private suspend fun sendOfflineSaleToHost(
        offlineSaleIso: ByteArray,
        offlineSaleCB: (Boolean) -> Unit
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
                isBool = successResponseCode == "00"
                offlineSaleCB(isBool)
            } else {
                ROCProviderV2.incrementFromResponse(
                    ROCProviderV2.getRoc(AppPreference.getBankCode()).toString(),
                    AppPreference.getBankCode()
                )

                isBool = false
                offlineSaleCB(isBool)
            }
        }, {
            //backToCalled(it, false, true)
        })
    }
}