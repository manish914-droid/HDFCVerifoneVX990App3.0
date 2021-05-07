package com.example.verifonevx990app.emv.transactionprocess

import android.util.Log
import com.example.verifonevx990app.vxUtils.*
import com.vfi.smartpos.deviceservice.aidl.IEMV
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

class SyncEmiEnquiryTransactionToHost(
    var transactionISOByteArray: IsoDataWriter?,
    var cardProcessedDataModal: CardProcessedDataModal? = null,
    var syncTransactionCallback: (Boolean, String, String?, Triple<String, String, String>?) -> Unit
) {
    private val iemv: IEMV? by lazy { VFService.vfIEMV }
    private var successResponseCode: String? = null

    init {
        GlobalScope.launch(Dispatchers.IO) {
            sendTransactionPacketToHost(transactionISOByteArray)
        }
    }

    //Below method is used to sync Transaction Packet Data to host:-
    private suspend fun sendTransactionPacketToHost(transactionISOData: IsoDataWriter?) {
        val transactionISOByteArray = transactionISOData?.generateIsoByteRequest()
        if (transactionISOData != null) {
            logger("Transaction REQUEST PACKET --->>", transactionISOData.isoMap, "e")
        }


        if (transactionISOByteArray != null) {
            HitServer.hitServersale(transactionISOByteArray, { result, success, readtimeout ->
                try {
                    if (success) {
                        //Below we are incrementing previous ROC (Because ROC will always be incremented whenever Server Hit is performed:-
                        ROCProviderV2.incrementFromResponse(
                            ROCProviderV2.getRoc(AppPreference.getBankCode()).toString(),
                            AppPreference.getBankCode()
                        )

                        Log.d("Success Data:- ", result)
                        val responseIsoData: IsoDataReader = readIso(result.toString(), false)
                        logger("Transaction RESPONSE ", "---", "e")
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

                        val acqRefereal = responseIsoData.isoMap[31]?.parseRaw2String().toString()
                        cardProcessedDataModal?.setAcqReferalNumber(acqRefereal)
                        logger(
                            "ACQREFERAL",
                            cardProcessedDataModal?.getAcqReferalNumber().toString(),
                            "e"
                        )

                        val encrptedPan = responseIsoData.isoMap[57]?.parseRaw2String().toString()
                        cardProcessedDataModal?.setEncryptedPan(encrptedPan)

                        val f55 = responseIsoData.isoMap[55]?.rawData
                        if (f55 != null)
                            cardProcessedDataModal?.setTC(tcDataFromField55(responseIsoData))

                        if (successResponseCode == "00") {
                            //  clearReversal()
                            syncTransactionCallback(
                                true,
                                successResponseCode.toString(),
                                result,
                                null
                            )

                        } else {
                            //    clearReversal()
                            syncTransactionCallback(
                                true,
                                successResponseCode.toString(),
                                result,
                                null
                            )
                            VFService.showToast(
                                "Transaction Fail Error Code = ${
                                    responseIsoData.isoMap[39]?.parseRaw2String()
                                        .toString()
                                }"
                            )

                        }
                    } else {
                        ROCProviderV2.incrementFromResponse(
                            ROCProviderV2.getRoc(AppPreference.getBankCode()).toString(),
                            AppPreference.getBankCode()
                        )
                        syncTransactionCallback(false, successResponseCode.toString(), result, null)
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
}