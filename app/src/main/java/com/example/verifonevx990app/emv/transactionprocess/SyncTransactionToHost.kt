package com.example.verifonevx990app.emv.transactionprocess

import android.os.DeadObjectException
import android.os.RemoteException
import android.text.TextUtils
import android.util.Log
import com.example.verifonevx990app.main.ConnectionError
import com.example.verifonevx990app.main.DetectCardType
import com.example.verifonevx990app.main.PrefConstant
import com.example.verifonevx990app.vxUtils.*
import com.example.verifonevx990app.vxUtils.AppPreference.GENERIC_REVERSAL_KEY
import com.example.verifonevx990app.vxUtils.AppPreference.clearReversal
import com.google.gson.Gson
import com.vfi.smartpos.deviceservice.aidl.IEMV
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

class SyncTransactionToHost(
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
        when (cardProcessedDataModal?.getTransType()) {
            TransactionType.PRE_AUTH.type -> {
                transactionISOData?.mti = Mti.PRE_AUTH_MTI.mti
            }
            else -> {
                transactionISOData?.mti = Mti.DEFAULT_MTI.mti
            }
        }
        //Setting ROC again because if reversal send first and then transaction packet goes to host the ROC is similar in that case because we are creating Trans packet at initial stage
        transactionISOData?.addField(
            11,
            ROCProviderV2.getRoc(AppPreference.getBankCode()).toString()
        )
        val transactionISOByteArray = transactionISOData?.generateIsoByteRequest()
        if (transactionISOData != null) {
            logger("Transaction REQUEST PACKET --->>", transactionISOData.isoMap, "e")
        }
        if (cardProcessedDataModal?.getReadCardType() != DetectCardType.EMV_CARD_TYPE) {
            val reversalPacket = Gson().toJson(transactionISOData)
            AppPreference.saveString(GENERIC_REVERSAL_KEY, reversalPacket)
            //  transactionISOByteArray?.byteArr2HexStr()?.let { logger("PACKET-->", it) }
            //  val oo=90/0
        }

        if (transactionISOByteArray != null) {
            HitServer.hitServersale(transactionISOByteArray, { result, success, readtimeout ->
                //Save Server Hit Status in Preference , To Restrict Init and KeyExchange from Terminal:-
                AppPreference.saveBoolean(PrefConstant.SERVER_HIT_STATUS.keyName.toString(), true)
                try {
                    //println("Result is$success")
                    if (success) {
                        //Below we are incrementing previous ROC (Because ROC will always be incremented whenever Server Hit is performed:-
                        ROCProviderV2.incrementFromResponse(
                            ROCProviderV2.getRoc(AppPreference.getBankCode()).toString(),
                            AppPreference.getBankCode()
                        )
                        Log.d("Success Data:- ", result)
                        //if(!result.isNullOrBlank())
                        if (!TextUtils.isEmpty(result)) {
                            val value = readtimeout.toIntOrNull()
                            if (null != value) {
                                when (value) {
                                    500 -> {
                                        ConnectionError.ReadTimeout.errorCode
                                        when (cardProcessedDataModal?.getReadCardType()) {
                                            DetectCardType.MAG_CARD_TYPE, DetectCardType.CONTACT_LESS_CARD_TYPE,
                                            DetectCardType.CONTACT_LESS_CARD_WITH_MAG_TYPE -> {
                                            }
                                            DetectCardType.EMV_CARD_TYPE -> {
                                                val reversalPacket =
                                                    Gson().toJson(transactionISOData)
                                                AppPreference.saveString(
                                                    GENERIC_REVERSAL_KEY,
                                                    reversalPacket
                                                )
                                            }

                                            else -> {
                                            }
                                        }
                                        syncTransactionCallback(false, "", result, null)
                                    }
                                }
                            } else {

                                try {
                                    val responseIsoData: IsoDataReader = readIso(result, false)
                                } catch (ex: Exception) {
                                    ex.printStackTrace()
                                    //   syncTransactionCallback(false, "", result, null)
                                }

                                //   println("Number format problem")
                                val responseIsoData: IsoDataReader =
                                    readIso(result.toString(), false)
                                logger("Transaction RESPONSE ", "---", "e")
                                logger("Transaction RESPONSE --->>", responseIsoData.isoMap, "e")
                                Log.e(
                                    "Success 39-->  ", responseIsoData.isoMap[39]?.parseRaw2String()
                                        .toString() + "---->" + responseIsoData.isoMap[58]?.parseRaw2String()
                                        .toString()
                                )
                                successResponseCode =
                                    (responseIsoData.isoMap[39]?.parseRaw2String().toString())
                                val authCode =
                                    (responseIsoData.isoMap[38]?.parseRaw2String().toString())
                                cardProcessedDataModal?.setAuthCode(authCode.trim())
                                //Here we are getting RRN Number :-
                                val rrnNumber = responseIsoData.isoMap[37]?.rawData ?: ""
                                cardProcessedDataModal?.setRetrievalReferenceNumber(rrnNumber)

                                val acqRefereal =
                                    responseIsoData.isoMap[31]?.parseRaw2String().toString()
                                cardProcessedDataModal?.setAcqReferalNumber(acqRefereal)
                                logger(
                                    "ACQREFERAL",
                                    cardProcessedDataModal?.getAcqReferalNumber().toString(),
                                    "e"
                                )

                                val encrptedPan =
                                    responseIsoData.isoMap[57]?.parseRaw2String().toString()
                                cardProcessedDataModal?.setEncryptedPan(encrptedPan)

                                val f55 = responseIsoData.isoMap[55]?.rawData
                                if (f55 != null)
                                    cardProcessedDataModal?.setTC(tcDataFromField55(responseIsoData))

                                if (successResponseCode == "00") {

                                    AppPreference.saveBoolean(
                                        AppPreference.ONLINE_EMV_DECLINED,
                                        false
                                    )
                                    //   VFService.showToast("Transaction Success")

                                    when (cardProcessedDataModal?.getReadCardType()) {

                                        DetectCardType.MAG_CARD_TYPE, DetectCardType.CONTACT_LESS_CARD_TYPE,
                                        DetectCardType.CONTACT_LESS_CARD_WITH_MAG_TYPE,
                                        DetectCardType.MANUAL_ENTRY_TYPE -> {
                                            clearReversal()
                                            syncTransactionCallback(
                                                true,
                                                successResponseCode.toString(),
                                                result,
                                                null
                                            )
                                        }
                                        DetectCardType.EMV_CARD_TYPE -> {
                                            if (TextUtils.isEmpty(
                                                    AppPreference.getString(
                                                        GENERIC_REVERSAL_KEY
                                                    )
                                                )
                                            ) {
                                                if (cardProcessedDataModal?.getTransType() != TransactionType.REFUND.type &&
                                                    cardProcessedDataModal?.getTransType() != TransactionType.EMI_SALE.type &&
                                                    cardProcessedDataModal?.getTransType() != TransactionType.BRAND_EMI.type &&
                                                    cardProcessedDataModal?.getTransType() != TransactionType.BRAND_EMI_BY_ACCESS_CODE.type &&
                                                    cardProcessedDataModal?.getTransType() != TransactionType.SALE.type &&
                                                    cardProcessedDataModal?.getTransType() != TransactionType.PRE_AUTH.type &&
                                                    cardProcessedDataModal?.getTransType() != TransactionType.SALE_WITH_CASH.type
                                                ) {
                                                    CompleteSecondGenAc(
                                                        responseIsoData,
                                                        transactionISOData
                                                    ) { printExtraData ->
                                                        syncTransactionCallback(
                                                            true,
                                                            successResponseCode.toString(),
                                                            result,
                                                            printExtraData
                                                        )
                                                    }
                                                } else {
                                                    clearReversal()
                                                    syncTransactionCallback(
                                                        true,
                                                        successResponseCode.toString(),
                                                        result,
                                                        null
                                                    )

                                                }


                                            } else {
                                                clearReversal()
                                                syncTransactionCallback(
                                                    true,
                                                    successResponseCode.toString(),
                                                    result,
                                                    null
                                                )
                                            }
                                        }

                                        else -> logger(
                                            "CARD_ERROR:- ",
                                            cardProcessedDataModal?.getReadCardType().toString(),
                                            "e"
                                        )
                                    }
                                    //remove emi case

                                } else {
                                    //here 2nd Gen Ac in case of Failure
                                    //here reversal will also be there
                                    when (cardProcessedDataModal?.getReadCardType()) {
                                        DetectCardType.MAG_CARD_TYPE, DetectCardType.CONTACT_LESS_CARD_TYPE,
                                        DetectCardType.CONTACT_LESS_CARD_WITH_MAG_TYPE,
                                        DetectCardType.MANUAL_ENTRY_TYPE -> {
                                            clearReversal()
                                            syncTransactionCallback(
                                                true,
                                                successResponseCode.toString(),
                                                result,
                                                null
                                            )
                                        }
                                        DetectCardType.EMV_CARD_TYPE -> {
                                            clearReversal()
                                            if (cardProcessedDataModal?.getTransType() != TransactionType.REFUND.type &&
                                                cardProcessedDataModal?.getTransType() != TransactionType.EMI_SALE.type &&
                                                cardProcessedDataModal?.getTransType() != TransactionType.BRAND_EMI.type &&
                                                cardProcessedDataModal?.getTransType() != TransactionType.BRAND_EMI_BY_ACCESS_CODE.type &&
                                                cardProcessedDataModal?.getTransType() != TransactionType.SALE.type
                                            ) {
                                                CompleteSecondGenAc(responseIsoData) { printExtraData ->
                                                    syncTransactionCallback(
                                                        true,
                                                        successResponseCode.toString(),
                                                        result,
                                                        printExtraData
                                                    )
                                                }
                                            } else {
                                                syncTransactionCallback(
                                                    true,
                                                    successResponseCode.toString(),
                                                    result,
                                                    null
                                                )
                                            }

                                        }
                                        else -> logger(
                                            "CARD_ERROR:- ",
                                            cardProcessedDataModal?.getReadCardType().toString(),
                                            "e"
                                        )
                                    }

                                }
                            }
                        } else {
                            syncTransactionCallback(false, "", "", null)
                        }

                    } else {
                        val value = readtimeout.toIntOrNull()
                        if (null != value) {
                            when (value) {
                                504 -> {
                                    AppPreference.saveBoolean(
                                        PrefConstant.SERVER_HIT_STATUS.keyName.toString(),
                                        false
                                    )
                                    ConnectionError.NetworkError.errorCode
                                    //Clear reversal
                                    clearReversal()
                                    //Below we are incrementing previous ROC (Because ROC will always be incremented whenever Server Hit is performed:-
                                    ROCProviderV2.incrementFromResponse(
                                        ROCProviderV2.getRoc(AppPreference.getBankCode())
                                            .toString(), AppPreference.getBankCode()
                                    )
                                    SecondGenAcOnNetworkError(
                                        result.toString(),
                                        cardProcessedDataModal
                                    ) { secondGenAcErrorStatus ->
                                        if (secondGenAcErrorStatus) {
                                            syncTransactionCallback(
                                                false,
                                                successResponseCode.toString(),
                                                result,
                                                null
                                            )
                                        } else {
                                            syncTransactionCallback(
                                                false,
                                                ConnectionError.NetworkError.errorCode.toString(),
                                                result,
                                                null
                                            )
                                        }
                                    }
                                }

                                else -> {

                                    //Clear reversal
                                    clearReversal()
                                    //Below we are incrementing previous ROC (Because ROC will always be incremented whenever Server Hit is performed:-
                                    ROCProviderV2.incrementFromResponse(
                                        ROCProviderV2.getRoc(
                                            AppPreference.getBankCode()
                                        ).toString(), AppPreference.getBankCode()
                                    )
                                    syncTransactionCallback(
                                        false,
                                        ConnectionError.ConnectionTimeout.errorCode.toString(),
                                        result,
                                        null
                                    )
                                    Log.d("Failure Data:- ", result)
                                }
                            }
                        } else {
                            //Clear reversal
                            clearReversal()
                            //Below we are incrementing previous ROC (Because ROC will always be incremented whenever Server Hit is performed:-
                            ROCProviderV2.incrementFromResponse(
                                ROCProviderV2.getRoc(AppPreference.getBankCode()).toString(),
                                AppPreference.getBankCode()
                            )
                            syncTransactionCallback(
                                false,
                                ConnectionError.ConnectionTimeout.errorCode.toString(),
                                result,
                                null
                            )
                            Log.d("Failure Data:- ", result)
                        }
                    }
                } catch (ex: DeadObjectException) {
                    // throw RuntimeException(ex)
                    ex.printStackTrace()
                } catch (ex: RemoteException) {
                    //  throw RuntimeException(ex)
                    ex.printStackTrace()
                } catch (ex: Exception) {
                    //throw RuntimeException(ex)
                    ex.printStackTrace()
                }

            }, {
                //backToCalled(it, false, true)
            })
        }
    }
}