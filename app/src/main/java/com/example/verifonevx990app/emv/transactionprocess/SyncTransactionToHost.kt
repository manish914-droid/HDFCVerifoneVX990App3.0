package com.example.verifonevx990app.emv.transactionprocess

import android.os.Bundle
import android.os.DeadObjectException
import android.os.RemoteException
import android.text.TextUtils
import android.util.Log
import com.example.verifonevx990app.main.*
import com.example.verifonevx990app.utils.Utility
import com.example.verifonevx990app.vxUtils.*
import com.example.verifonevx990app.vxUtils.AppPreference.GENERIC_REVERSAL_KEY
import com.example.verifonevx990app.vxUtils.AppPreference.clearReversal
import com.google.gson.Gson
import com.vfi.smartpos.deviceservice.aidl.IEMV
import com.vfi.smartpos.deviceservice.aidl.OnlineResultHandler
import com.vfi.smartpos.deviceservice.constdefine.ConstIPBOC
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch


class SyncTransactionToHost(var transactionISOByteArray: IsoDataWriter?,
                            var cardProcessedDataModal: CardProcessedDataModal? = null,
                            var syncTransactionCallback: (Boolean, String, String?, Triple<String, String, String>?,String?,String?) -> Unit) {

    private val iemv: IEMV? by lazy { VFService.vfIEMV }
    private var successResponseCode: String? = null
    private var secondTap: String? = null

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
                                        syncTransactionCallback(false, "", result, null,null,null)
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
                                            if(CardAid.Rupay.aid.equals(cardProcessedDataModal?.getAID())) {

                                              /*  doubleTap(responseIsoData, transactionISOData, successResponseCode){it ->
                                                    secondTap = it
                                                }*/

                                                val ta91 = 0x91
                                                val ta8A = 0x8A
                                                //  val field55 = "91109836BE3880804000FFFE000000000001"
                                               // val field55 = responseIsoData.isoMap[55]?.rawData ?: "91109836BE3880804000FFFE000000000001"
                                                val field55 = responseIsoData.isoMap[55]?.rawData ?: ""
                                                //   VFService.showToast(field55)
                                                println("Filed55 value is --> $field55")

                                                val f55Hash = HashMap<Int, String>()
                                                tlvParser(field55, f55Hash)

                                                val tagDatatag91 = f55Hash[ta91] ?: ""
                                                println("91 value is --> $tagDatatag91")

                                                f55Hash.clear()

                                                var i = 0
                                                var j = 1
                                                while (i < tagDatatag91.length - 1) {
                                                    val c = "" + tagDatatag91[i] + tagDatatag91[i + 1]
                                                    f55Hash.put(j, c)
                                                    println("91 value with pair is" + c)
                                                    j += 1
                                                    i += 2
                                                }

                                                f55Hash.forEach { (key, value) -> println("$key = $value") }

                                                if (f55Hash.isNotEmpty() && (f55Hash.get(7) == "40" || f55Hash.get(7) == "80")) {
                                                    //  VFService.showToast("Double Tap")
                                                    AppPreference.saveString(AppPreference.doubletap, "doubletap")
                                                    secondTap = "doubletap"

                                                    transactionISOData.apply {
                                                        additionalData["F39reversaldoubletap"] = "E1"
                                                    }

                                                    val reversalPacket = Gson().toJson(transactionISOData)
                                                    AppPreference.saveString(GENERIC_REVERSAL_KEY, reversalPacket)

                                                    println("Element at key $7 : ${f55Hash.get(7)}")


                                                    val mba = ArrayList<Byte>()
                                                    val mba1 = ArrayList<Byte>()
                                                    try {
                                                        if (tagDatatag91.isNotEmpty()) {
                                                            val ba = tagDatatag91.hexStr2ByteArr()
                                                            mba.addAll(ba.asList())
                                                            mba1.addAll(ba.asList())
                                                            //

                                                            //rtn = EMVCallback.EMVSetTLVData(ta.toShort(), mba.toByteArray(), mba.size)
                                                            logger("Data:- ", "On setting ${Integer.toHexString(ta91)} tag status = $", "e")
                                                        }
                                                    } catch (ex: Exception) {
                                                        logger("Exception:- ", ex.message ?: "")
                                                    }

                                                    val tagData8a = f55Hash[ta8A] ?: "00"
                                                    try {
                                                        if (tagData8a.isNotEmpty()) {

                                                            val byteArr = tagData8a.toByteArray()
                                                            var hexvalue = Utility.byte2HexStr(byteArr)
                                                            println("3030 hex value is --->" + hexvalue)
                                                            println("3030 hex to string is --->" + hexString2String(hexvalue))

                                                            val ba = tagData8a.hexStr2ByteArr()

                                                            var strba = ba.byteArr2HexStr()

                                                            // rtn = EMVCallback.EMVSetTLVData(ta.toShort(), ba, ba.size)
                                                            logger(VFTransactionActivity.TAG, "On setting ${Integer.toHexString(ta8A)} tag status = $", "e")
                                                        }
                                                    } catch (ex: Exception) {
                                                        logger(VFTransactionActivity.TAG, ex.message ?: "", "e")
                                                    }

                                                    val onlineResult = Bundle()
                                                    onlineResult.putBoolean(ConstIPBOC.inputOnlineResult.onlineResult.KEY_isOnline_boolean, true)

                                                    if (null != successResponseCode && successResponseCode.toString().isNotEmpty() && hexString2String(successResponseCode.toString()).equals("00")) {
                                                        onlineResult.putString(ConstIPBOC.inputOnlineResult.onlineResult.KEY_respCode_String, "00")  //tagData8a
                                                    } else {
                                                        onlineResult.putString(ConstIPBOC.inputOnlineResult.onlineResult.KEY_respCode_String, tagData8a)
                                                    }
                                                    onlineResult.putString(ConstIPBOC.inputOnlineResult.onlineResult.KEY_authCode_String, "00")

                                                    if (field55 != null && field55.isNotEmpty()) {

                                                        val byteArr = tagData8a.toByteArray()
                                                        var hexvalue = Utility.byte2HexStr(byteArr)

                                                        onlineResult.putString(ConstIPBOC.inputOnlineResult.onlineResult.KEY_field55_String, field55 + Integer.toHexString(ta8A) + "02" + hexvalue)
                                                        //At least 0A length for 91
                                                        println("Field55 value inside ---> " + field55 + Integer.toHexString(ta8A) + "02" + hexvalue)

                                                    } else {
                                                        onlineResult.putString(ConstIPBOC.inputOnlineResult.onlineResult.KEY_field55_String, "")
                                                    }


                                                    iemv?.inputOnlineResult(onlineResult, object : OnlineResultHandler.Stub() {

                                                        override fun onProccessResult(result: Int, data: Bundle) {
                                                            Log.i(MainActivity.TAG, "onProccessResult callback:")

                                                        }
                                                    })


                                                }

                                                else{
                                                    clearReversal()
                                                }


                                                syncTransactionCallback(true, successResponseCode.toString(), result, null,null,secondTap)

                                            }
                                            else{
                                                clearReversal()
                                                syncTransactionCallback(true, successResponseCode.toString(), result, null,null,secondTap)
                                            }

                                        }
                                        DetectCardType.EMV_CARD_TYPE -> {
                                            if (TextUtils.isEmpty(
                                                            AppPreference.getString(GENERIC_REVERSAL_KEY))) {
                                                if (cardProcessedDataModal?.getTransType() != TransactionType.REFUND.type &&
                                                        cardProcessedDataModal?.getTransType() != TransactionType.EMI_SALE.type &&
                                                        cardProcessedDataModal?.getTransType() != TransactionType.BRAND_EMI.type &&
                                                        cardProcessedDataModal?.getTransType() != TransactionType.BRAND_EMI_BY_ACCESS_CODE.type &&
                                                        cardProcessedDataModal?.getTransType() != TransactionType.SALE.type &&
                                                        cardProcessedDataModal?.getTransType() != TransactionType.PRE_AUTH.type &&
                                                        cardProcessedDataModal?.getTransType() != TransactionType.SALE_WITH_CASH.type &&
                                                        cardProcessedDataModal?.getTransType() != TransactionType.CASH_AT_POS.type &&
                                                        cardProcessedDataModal?.getTransType() != TransactionType.TEST_EMI.type
                                                ) {
                                                    CompleteSecondGenAc(cardProcessedDataModal, responseIsoData, transactionISOData) { printExtraData, de55 ->
                                                        syncTransactionCallback(true, successResponseCode.toString(), result, printExtraData, de55, null)
                                                    }
                                                } else {
                                                    clearReversal()
                                                    syncTransactionCallback(true, successResponseCode.toString(), result, null, null, null)

                                                }


                                            } else {
                                                clearReversal()
                                                syncTransactionCallback(true, successResponseCode.toString(), result, null, null, null)
                                            }
                                        }

                                        else -> logger("CARD_ERROR:- ", cardProcessedDataModal?.getReadCardType().toString(), "e")
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
                                            syncTransactionCallback(true, successResponseCode.toString(), result, null, null, secondTap)
                                        }
                                        DetectCardType.EMV_CARD_TYPE -> {
                                            clearReversal()
                                            if (cardProcessedDataModal?.getTransType() != TransactionType.REFUND.type &&
                                                    cardProcessedDataModal?.getTransType() != TransactionType.EMI_SALE.type &&
                                                    cardProcessedDataModal?.getTransType() != TransactionType.BRAND_EMI.type &&
                                                    cardProcessedDataModal?.getTransType() != TransactionType.BRAND_EMI_BY_ACCESS_CODE.type &&
                                                    cardProcessedDataModal?.getTransType() != TransactionType.TEST_EMI.type &&
                                                    cardProcessedDataModal?.getTransType() != TransactionType.SALE.type) {
                                                CompleteSecondGenAc(cardProcessedDataModal, responseIsoData) { printExtraData, de55 ->
                                                    syncTransactionCallback(true, successResponseCode.toString(), result, printExtraData, de55, null)
                                                }
                                            } else {
                                                syncTransactionCallback(true, successResponseCode.toString(), result, null, null, null)
                                            }

                                        }
                                        else -> logger("CARD_ERROR:- ", cardProcessedDataModal?.getReadCardType().toString(), "e")
                                    }

                                }
                            }
                        } else {
                            syncTransactionCallback(false, "", "", null, null, null)
                        }

                    } else {
                        val value = readtimeout.toIntOrNull()
                        if (null != value) {
                            when (value) {
                                504 -> {
                                    AppPreference.saveBoolean(PrefConstant.SERVER_HIT_STATUS.keyName.toString(), false)
                                    ConnectionError.NetworkError.errorCode
                                    //Clear reversal
                                    clearReversal()
                                    //Below we are incrementing previous ROC (Because ROC will always be incremented whenever Server Hit is performed:-
                                    ROCProviderV2.incrementFromResponse(ROCProviderV2.getRoc(AppPreference.getBankCode()).toString(), AppPreference.getBankCode())
                                    SecondGenAcOnNetworkError(result.toString(), cardProcessedDataModal) { secondGenAcErrorStatus ->
                                        if (secondGenAcErrorStatus) {
                                            syncTransactionCallback(false, successResponseCode.toString(), result, null, null, null)
                                        } else {
                                            syncTransactionCallback(false, ConnectionError.NetworkError.errorCode.toString(), result, null, null, null)
                                        }
                                    }
                                }

                                else -> {

                                    //Clear reversal
                                    clearReversal()
                                    //Below we are incrementing previous ROC (Because ROC will always be incremented whenever Server Hit is performed:-
                                    ROCProviderV2.incrementFromResponse(ROCProviderV2.getRoc(AppPreference.getBankCode()).toString(), AppPreference.getBankCode())
                                    syncTransactionCallback(false, ConnectionError.ConnectionTimeout.errorCode.toString(), result, null, null, null)
                                    Log.d("Failure Data:- ", result)
                                }
                            }
                        } else {
                            //Clear reversal
                            clearReversal()
                            //Below we are incrementing previous ROC (Because ROC will always be incremented whenever Server Hit is performed:-
                            ROCProviderV2.incrementFromResponse(ROCProviderV2.getRoc(AppPreference.getBankCode()).toString(), AppPreference.getBankCode())
                            syncTransactionCallback(false, ConnectionError.ConnectionTimeout.errorCode.toString(), result, null, null, null)
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
