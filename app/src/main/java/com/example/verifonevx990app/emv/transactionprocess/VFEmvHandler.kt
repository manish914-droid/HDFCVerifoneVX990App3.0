package com.example.verifonevx990app.emv.transactionprocess

import android.app.Activity
import android.app.Dialog
import android.content.Intent
import android.os.*
import android.util.Log
import android.util.SparseArray
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.widget.RadioButton
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.verifonevx990app.R
import com.example.verifonevx990app.main.DetectCardType
import com.example.verifonevx990app.main.DetectError
import com.example.verifonevx990app.main.MainActivity
import com.example.verifonevx990app.main.PosEntryModeType
import com.example.verifonevx990app.utils.Utility
import com.example.verifonevx990app.vxUtils.*
import com.example.verifonevx990app.vxUtils.ROCProviderV2.getField55
import com.vfi.smartpos.deviceservice.aidl.EMVHandler
import com.vfi.smartpos.deviceservice.aidl.IEMV
import com.vfi.smartpos.deviceservice.constdefine.ConstIPBOC
import com.vfi.smartpos.deviceservice.constdefine.ConstPBOCHandler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.nio.charset.StandardCharsets
import java.util.*

class VFEmvHandler(
    var activity: Activity,
    var handler: Handler,
    var iemv: IEMV?,
    var cardProcessedDataModal: CardProcessedDataModal,
    var vfEmvHandlerCallback: (CardProcessedDataModal) -> Unit
) : EMVHandler.Stub() {
    private var TAG = VFEmvHandler::class.java.name
    private var maxPin: Int = 0
    private var SuccessPin: Int = 1
    private var retryTimess: Int = 0
    private var savedPan: String? = null
    private var tagOfF55: SparseArray<String>? = null


    override fun onRequestOnlineProcess(aaResult: Bundle?) {
        Log.d(MainActivity.TAG, "onRequestOnlineProcess...")
        //Setting Pos entry mode for those who did not got in Request Input Pin

        when (cardProcessedDataModal.getReadCardType()) {
            DetectCardType.EMV_CARD_TYPE -> {
                when (cardProcessedDataModal.getIsOnline()) {
                    0 -> cardProcessedDataModal.setPosEntryMode(PosEntryModeType.EMV_POS_ENTRY_NO_PIN.posEntry.toString())
                }
            }
            DetectCardType.CONTACT_LESS_CARD_TYPE -> {
                when (cardProcessedDataModal.getIsOnline()) {
                    0 -> cardProcessedDataModal.setPosEntryMode(PosEntryModeType.CTLS_EMV_POS_ENTRY_CODE.posEntry.toString())
                }
            }
            DetectCardType.CONTACT_LESS_CARD_WITH_MAG_TYPE -> {
                when (cardProcessedDataModal.getIsOnline()) {
                    0 -> cardProcessedDataModal.setPosEntryMode(PosEntryModeType.CTLS_MSD_POS_ENTRY_CODE.posEntry.toString())
                }
            }

            DetectCardType.MAG_CARD_TYPE -> cardProcessedDataModal.setPosEntryMode(PosEntryModeType.CTLS_MSD_POS_ENTRY_CODE.posEntry.toString())

            else -> {
            }
        }

        val result =
            aaResult?.getInt(ConstPBOCHandler.onRequestOnlineProcess.aaResult.KEY_RESULT_int)
        val signature =
            aaResult?.getBoolean(ConstPBOCHandler.onRequestOnlineProcess.aaResult.KEY_SIGNATURE_boolean)
        //   VFService.showToast("onRequestOnlineProcess result=$result signal=$signature")
        when (result) {
            ConstPBOCHandler.onRequestOnlineProcess.aaResult.VALUE_RESULT_AARESULT_ARQC, ConstPBOCHandler.onRequestOnlineProcess.aaResult.VALUE_RESULT_QPBOC_ARQC ->
                aaResult.getString(ConstPBOCHandler.onRequestOnlineProcess.aaResult.KEY_ARQC_DATA_String)
                    ?.let {
                        //VFService.showToast(it)
                        //println("ARQC data is -> " + it)
                    }
            ConstPBOCHandler.onRequestOnlineProcess.aaResult.VALUE_RESULT_PAYPASS_EMV_ARQC -> {
            }
        }

        var tlv: ByteArray?
        tagOfF55 = SparseArray()
        val tagList = intArrayOf(
            0x9F26,
            0x9F27,
            0x9F10,
            0x9F37,
            0x9F36,
            0x95,
            0x9A,
            0x9C,
            0x9F02,
            0x5F2A,
            0x5F34,
            0x82,
            0x9F1A,
            0x84,
            0x9F03,
            0x9F33,
            0x9F74
        )
        var count = 0
        try {
            for (tag in tagList) {
                tlv = iemv?.getCardData(Integer.toHexString(tag).toUpperCase(Locale.ROOT))
                if (null != tlv && tlv.isNotEmpty()) {
                    Log.e(
                        "TLV F55 REQ--",
                        "TAG--> " + Integer.toHexString(tag) + ", VALUE-->" + Utility.byte2HexStr(
                            tlv
                        )
                    )
                    val length = Integer.toHexString(tlv.size)
                    count += Integer.valueOf(length)
                    tagOfF55?.put(tag, Utility.byte2HexStr(tlv)) // build up the field 55

                    if (null != tag && "84".equals(Integer.toHexString(tag))) {
                        //println("Aid value with Tag is ---> "+Integer.toHexString(tag) + Utility.byte2HexStr(tlv))
                        cardProcessedDataModal.setAID(Utility.byte2HexStr(tlv))

                    }
                } else {
                    //  tagOfF55?.put(tag,"00")
                    Log.e(MainActivity.TAG, "getCardData:" + Integer.toHexString(tag) + ", fails")
                }
            }
            //println("Total length of tag55 is$count")
        } catch (ex: Exception) {
            Log.d("Exception during ARQC", "" + ex.printStackTrace())
        }
        Log.d(MainActivity.TAG, "start online request")
        processField55Data()
        Log.d(MainActivity.TAG, "online request finished")
        processField57Data()

    }

    //1
    override fun onSelectApplication(appList: MutableList<Bundle>?) {
        try {
            if (appList != null) {
                /* for (aidBundle in appList) {
                     val aidName = aidBundle.getString("aidName")
                     val aid = aidBundle.getString("aid")
                     val aidLabel = aidBundle.getString("aidLabel")
                     Log.i(TAG, "AID Name=$aidName | AID Label=$aidLabel | AID=$aid")
                 }*/
                inflateAppsDialog(appList) { multiAppPosition ->
                    iemv?.importAppSelection(multiAppPosition)
                }
            }
        } catch (ex: DeadObjectException) {
            ex.printStackTrace()
            Handler(Looper.getMainLooper()).postDelayed(Runnable {
                GlobalScope.launch {
                    VFService.connectToVFService(VerifoneApp.appContext)
                    delay(200)
                    //  iemv = VFService.vfIEMV
                    delay(100)
                    (activity as VFTransactionActivity).doProcessCard()
                }
            }, 200)
            println("VfEmvHandler error in onSelectApplication" + ex.message)
        } catch (ex: RemoteException) {
            ex.printStackTrace()
            Handler(Looper.getMainLooper()).postDelayed(Runnable {
                GlobalScope.launch {
                    VFService.connectToVFService(VerifoneApp.appContext)
                    delay(200)
                    //  iemv = VFService.vfIEMV
                    delay(100)
                    (activity as VFTransactionActivity).doProcessCard()
                }
            }, 200)
            println("VfEmvHandler error in onSelectApplication" + ex.message)
        } catch (ex: Exception) {
            ex.printStackTrace()
            println("VfEmvHandler error in onSelectApplication" + ex.message)
        }

        /*  VFService.showToast("onSelectApplication..." + (appList?.get(0) ?: ""))
          iemv?.importAppSelection(1)*/
    }

    override fun onConfirmCertInfo(certType: String?, certInfo: String?) {
        //VFService.showToast("onConfirmCertInfo, type:$certType,info:$certInfo")
        try {
            iemv?.importCertConfirmResult(ConstIPBOC.importCertConfirmResult.option.CONFIRM)
            logger(
                "onConfirmCertInfo",
                "certInfo--->" + certInfo.toString() + "certType---> " + certType.toString(),
                "e"
            )
        } catch (ex: DeadObjectException) {
            ex.printStackTrace()
            Handler(Looper.getMainLooper()).postDelayed(Runnable {
                GlobalScope.launch {
                    VFService.connectToVFService(VerifoneApp.appContext)
                    delay(200)
                    //  iemv = VFService.vfIEMV
                    delay(100)
                    (activity as VFTransactionActivity).doProcessCard()
                }
            }, 200)
            println("VfEmvHandler error in onSelectApplication" + ex.message)
        } catch (ex: RemoteException) {
            ex.printStackTrace()
            Handler(Looper.getMainLooper()).postDelayed(Runnable {
                GlobalScope.launch {
                    VFService.connectToVFService(VerifoneApp.appContext)
                    delay(200)
                    //  iemv = VFService.vfIEMV
                    delay(100)
                    (activity as VFTransactionActivity).doProcessCard()
                }
            }, 200)
            println("VfEmvHandler error in onSelectApplication" + ex.message)
        } catch (ex: Exception) {
            ex.printStackTrace()
            println("VfEmvHandler error in onConfirmCertInfo" + ex.message)
        }
    }

    override fun onRequestAmount() {
        Log.d(TAG, "Request Amount.......")
    }

    //2
    override fun onConfirmCardInfo(info: Bundle?) {
        Log.d(MainActivity.TAG, "onConfirmCardInfo...")
        savedPan =
            info?.getString(ConstPBOCHandler.onConfirmCardInfo.info.KEY_PAN_String).toString()

        //Below Result is Inserted Card Info:-
        var result = """
                    onConfirmCardInfo callback, 
                    PAN:${savedPan}
                    TRACK2:${info?.getString(ConstPBOCHandler.onConfirmCardInfo.info.KEY_TRACK2_String)}
                    CARD_SN:${info?.getString(ConstPBOCHandler.onConfirmCardInfo.info.KEY_CARD_SN_String)}
                    SERVICE_CODE:${info?.getString(ConstPBOCHandler.onConfirmCardInfo.info.KEY_SERVICE_CODE_String)}
                    EXPIRED_DATE:${info?.getString(ConstPBOCHandler.onConfirmCardInfo.info.KEY_EXPIRED_DATE_String)}
                      CARD_TYPE:${info?.getInt(ConstPBOCHandler.onConfirmCardInfo.info.KEY_CARD_TYPE_String)}
                    """.trimIndent()

        val tlvcardTypeLabel = iemv?.getCardData("50")   // card Type TAG
        if (null != tlvcardTypeLabel && !(tlvcardTypeLabel.isEmpty())) {
            var cardType = Utility.byte2HexStr(tlvcardTypeLabel)
            cardProcessedDataModal.setcardLabel(hexString2String(cardType))
            println("Card  Type ---> " + hexString2String(cardType))
        }

        val tlv = iemv?.getCardData("5F20")   // CardHolder Name TAG
        if (null != tlv && !(tlv.isEmpty())) {
            var cardholderName = Utility.byte2HexStr(tlv)
            var removespace = hexString2String(cardholderName)
            var finalstr = removespace.trimEnd()
            cardProcessedDataModal.setCardHolderName(finalstr)
            println("Card Holder Name ---> " + finalstr)
        }
        val tlvapplabel = iemv?.getCardData("9F12")   // application label  TAG
        if (null != tlvapplabel && !(tlvapplabel.isEmpty())) {
            var applicationlabel = Utility.byte2HexStr(tlvapplabel)
            var removespace = hexString2String(applicationlabel)
            var finalstr = removespace.trimEnd()
            cardProcessedDataModal.setApplicationLabel(finalstr)
            println("Application label ---> " + finalstr)
        }

        val tlvcardissuer = iemv?.getCardData("5F28")   // card issuer country code  TAG
        if (null != tlvcardissuer && !(tlvcardissuer.isEmpty())) {
            var cardissuercountrycode = Utility.byte2HexStr(tlvcardissuer)
            cardProcessedDataModal.setCardIssuerCountryCode(cardissuercountrycode)
            println("Card issuer country code ---> " + cardissuercountrycode)
        }

        //region========================Scheme AID==============
        val tlvaid = iemv?.getCardData("84")   // card issuer country code  TAG
        if (null != tlvaid && !(tlvaid.isEmpty())) {
            var aid = Utility.byte2HexStr(tlvaid)

            var aidstr = aid.subSequence(0, 10).toString()

            cardProcessedDataModal.setAID(aidstr)
            println("Aid  code ---> " + aidstr)
        }
        //endregion

        cardProcessedDataModal.setPinEntryFlag("0")

        //println("Card Type is ---> " + info?.getInt(ConstPBOCHandler.onConfirmCardInfo.info.KEY_CARD_TYPE_String))

        if (info?.getInt(ConstPBOCHandler.onConfirmCardInfo.info.KEY_CARD_TYPE_String) == 0) {
            //For EMV //card Type 0
        } else if (info?.getInt(ConstPBOCHandler.onConfirmCardInfo.info.KEY_CARD_TYPE_String) == 1) {
            //For Magnetic card type will be 1
            //     println("Card Type is ---> "+info?.getInt(ConstPBOCHandler.onConfirmCardInfo.info.KEY_CARD_TYPE_String))
            cardProcessedDataModal.setReadCardType(DetectCardType.CONTACT_LESS_CARD_WITH_MAG_TYPE)


        } else {
            //For Others

        }
        //println("Saved Pan number is${savedPan}")

        val track2 = info?.getString(ConstPBOCHandler.onConfirmCardInfo.info.KEY_TRACK2_String)
        var track22: String? = null
        if (null != track2) {
            var a = track2.indexOf('D')
            if (a > 0) {
                track22 = track2.substring(0, a)
            } else {
                a = track2.indexOf('=')

                if (a > 0) {
                    track22 = track2.substring(0, a)
                }
            }

            try {
                if (track22 != null) {
                    cardProcessedDataModal.setPanNumberData(track22)
                    //  cardProcessedDataModal.setPanNumberData("8909878")
                    cardProcessedDataModal.getPanNumberData()?.let {
                        logger("CTLS_EMV", it, "e")
                    }
                }

                //Here we make a callback for first time card read in case of bank emi:-
                //  if (isFirstBankEMICardRead)
                //    vfEmvHandlerCallback(cardProcessedDataModal)

                if (!cardProcessedDataModal.getPanNumberData()?.let { cardLuhnCheck(it) }!!) {
                    val bun = Bundle()
                    bun.putString("ERROR", "Invalid Card Number")
                    onTransactionResult(DetectError.IncorrectPAN.errorCode, bun)
                } else {
                    cardProcessedDataModal.setTrack1Data(track2)
                    //   var track21 = "35,36|" + track2.replace("D", "=").replace("F", "")

                    //    VFService.showToast("onConfirmCardInfo:$result")
                    //  checkEmiInstaEmi(cardProcessedDataModal)
                    if (cardProcessedDataModal.getTransType() == TransactionType.SALE.type) {
                        (activity as VFTransactionActivity).checkEmiInstaEmi(cardProcessedDataModal) {
                            if (cardProcessedDataModal.getTransType() == TransactionType.EMI_SALE.type) {
                                //  iemv?.importCardConfirmResult(ConstIPBOC.importCardConfirmResult.pass.allowed)
                            } else if (cardProcessedDataModal.getTransType() == TransactionType.SALE.type) {
                                iemv?.importCardConfirmResult(ConstIPBOC.importCardConfirmResult.pass.allowed)
                            }
                        }
                    } else if (cardProcessedDataModal.getTransType() == TransactionType.EMI_SALE.type ||
                        cardProcessedDataModal.getTransType() == TransactionType.BRAND_EMI.type ||
                        cardProcessedDataModal.getTransType() == TransactionType.BRAND_EMI_BY_ACCESS_CODE.type
                    ) {
                        (activity as VFTransactionActivity).checkEmiInstaEmi(cardProcessedDataModal) {
                            iemv?.importCardConfirmResult(ConstIPBOC.importCardConfirmResult.pass.allowed)
                        }
                    } else {
                        iemv?.importCardConfirmResult(ConstIPBOC.importCardConfirmResult.pass.allowed)
                    }
                }
            } catch (ex: DeadObjectException) {
                ex.printStackTrace()
                Handler(Looper.getMainLooper()).postDelayed(Runnable {
                    GlobalScope.launch {
                        VFService.connectToVFService(VerifoneApp.appContext)
                        delay(200)
                        //  iemv = VFService.vfIEMV
                        delay(100)
                        (activity as VFTransactionActivity).doProcessCard()
                    }
                }, 200)
                println("VfEmvHandler error in onSelectApplication" + ex.message)
            } catch (ex: RemoteException) {
                ex.printStackTrace()
                Handler(Looper.getMainLooper()).postDelayed(Runnable {
                    GlobalScope.launch {
                        VFService.connectToVFService(VerifoneApp.appContext)
                        delay(200)
                        //  iemv = VFService.vfIEMV
                        delay(100)
                        (activity as VFTransactionActivity).doProcessCard()
                    }
                }, 200)
                println("VfEmvHandler error in onSelectApplication" + ex.message)
            } catch (ex: Exception) {
                ex.printStackTrace()
                println("VfEmvHandler error in onConfirmCardinfo" + ex.message)
            }


        }
    }

    //3(In ERROR CASE)
    override fun onTransactionResult(result: Int, data: Bundle?) {
        Log.d("FallbackCode:- ", result.toString())

        //202 refuse on qPBOC

        /*//Case for CTLS Card Type Transaction:-
        if (result == 29) {
            VFService.showToast(data?.getString("ERROR").toString())
            return
        }*/

        val msg = data?.getString("ERROR")
        when {
            DetectError.SeePhone.errorCode == result -> {
                (activity as VFTransactionActivity).handleEMVFallbackFromError(
                    activity.getString(R.string.contactless_seephone_error),
                    activity.getString(R.string.please_use_another_card_for_transaction),
                    false
                ) { alertCBBool ->
                    if (alertCBBool)
                        try {
                            (activity as VFTransactionActivity).doProcessCard()
                        } catch (ex: Exception) {
                            ex.printStackTrace()
                        }
                }
            }
            DetectError.ReadCardFail.errorCode == result -> {
                (activity as VFTransactionActivity).handleEMVFallbackFromError(
                    activity.getString(R.string.alert), activity.getString(R.string.read_card_fail),
                    false
                ) { alertCBBool ->
                    if (alertCBBool)
                        try {
                            (activity as VFTransactionActivity).declinedTransaction()
                        } catch (ex: Exception) {
                            ex.printStackTrace()
                        }
                }
            }
            DetectError.Muticarderror.errorCode == result -> {
                (activity as VFTransactionActivity).handleEMVFallbackFromError(
                    activity.getString(R.string.alert),
                    activity.getString(R.string.multi_card_error),
                    false
                ) { alertCBBool ->
                    if (alertCBBool)
                        try {
                            (activity as VFTransactionActivity).declinedTransaction()
                        } catch (ex: Exception) {
                            ex.printStackTrace()
                        }
                }
            }
            DetectError.NoCoOwnedApp.errorCode == result -> {
                (activity as VFTransactionActivity).handleEMVFallbackFromError(
                    activity.getString(R.string.alert),
                    activity.getString(R.string.please_use_another_card_for_transaction),
                    false
                ) { alertCBBool ->
                    if (alertCBBool)
                        try {
                            cardProcessedDataModal.setFallbackType(EFallbackCode.EMV_fallback.fallBackCode)
                            (activity as VFTransactionActivity).doProcessCard()
                        } catch (ex: Exception) {
                            ex.printStackTrace()
                        }
                }
            }
            DetectError.NeedContact.errorCode == result -> {
                (activity as VFTransactionActivity).handleEMVFallbackFromError(
                    activity.getString(R.string.card_read_error),
                    activity.getString(R.string.reinitiate_trans),
                    false
                ) { alertCBBool ->
                    if (alertCBBool)
                        try {
                            (activity as VFTransactionActivity).declinedTransaction()
                        } catch (ex: Exception) {
                            ex.printStackTrace()
                        }
                }
            }
            DetectError.EMVFallBack.errorCode == result -> {
                (activity as VFTransactionActivity).handleEMVFallbackFromError(
                    activity.getString(R.string.emv_fallback),
                    activity.getString(R.string.please_use_another_card_for_transaction),
                    false
                ) { alertCBBool ->
                    if (alertCBBool)
                        try {
                            (activity as VFTransactionActivity).doProcessCard()
                        } catch (ex: Exception) {
                            ex.printStackTrace()
                        }
                }
            }
            DetectError.DynamicLimit.errorCode == result -> {
                (activity as VFTransactionActivity).handleEMVFallbackFromError(
                    activity.getString(R.string.card_read_error),
                    activity.getString(R.string.dynamicLimit_error),
                    false
                ) { alertCBBool ->
                    if (alertCBBool)
                        try {
                            (activity as VFTransactionActivity).declinedTransaction()
                        } catch (ex: Exception) {
                            ex.printStackTrace()
                        }
                }
            }
            DetectError.IncorrectPAN.errorCode == result -> {
                (activity as VFTransactionActivity).handleEMVFallbackFromError(
                    activity.getString(R.string.alert), msg.toString(),
                    false
                ) { alertCBBool ->
                    if (alertCBBool)
                        try {
                            (activity as VFTransactionActivity).declinedTransaction()
                        } catch (ex: Exception) {
                            ex.printStackTrace()
                        }
                }
                logger("onTransactionResult", "IncorrectPAN", "e")
            }
            DetectError.CTLS_CARD_READ_FAILED_ERROR.errorCode == result -> {
                (activity as VFTransactionActivity).handleEMVFallbackFromError(
                    activity.getString(R.string.read_failed),
                    activity.getString(R.string.card_read_failed),
                    false
                ) { alertCBBool ->
                    if (alertCBBool)
                        try {
                            (activity as VFTransactionActivity).declinedTransaction()
                        } catch (ex: Exception) {
                            ex.printStackTrace()
                        }
                }
                logger("onTransactionResult", "IncorrectPAN", "e")
            }
            DetectError.OtherErrorTransactionterminated.errorCode == result -> {
                (activity as VFTransactionActivity).handleEMVFallbackFromError(
                    activity.getString(R.string.timeOut),
                    activity.getString(R.string.transaction_terminated),
                    false
                ) { alertCBBool ->
                    if (alertCBBool)
                        try {
                            (activity as VFTransactionActivity).declinedTransaction()
                        } catch (ex: Exception) {
                            ex.printStackTrace()
                        }
                }
                logger("onTransactionResult", "IncorrectPAN", "e")
            }
            else -> {
                (activity as VFTransactionActivity).handleEMVFallbackFromError(
                    activity.getString(R.string.alert), msg.toString(),
                    false
                ) { alertCBBool ->
                    if (alertCBBool)
                        try {
                            (activity as VFTransactionActivity).declinedTransaction()
                        } catch (ex: Exception) {
                            ex.printStackTrace()
                        }
                }
            }
        }


        /*   when (cardProcessedDataModal.getReadCardType()) {
               DetectCardType.MAG_CARD_TYPE -> {

               }
               DetectCardType.EMV_CARD_TYPE -> {
                   if (result == EFallbackCode.EMV_fallback.fallBackCode) {
                       (activity as VFTransactionActivity).handleEMVFallbackFromError(
                           activity.getString(R.string.fallback),
                           activity.getString(R.string.please_use_another_option), false
                       ) { alertCBBool ->
                           if (alertCBBool) {
                               try {
                                   cardProcessedDataModal.setFallbackType(EFallbackCode.EMV_fallback.fallBackCode)
                                   vfEmvHandlerCallback(cardProcessedDao
               DetectCardType.CONTACT_LESS_CARD_TYPE -> {

               }
               DetectCardType.CONTACT_LESS_CARD_WITH_MAG_TYPE -> {
                   when {
                       //Test case 20
                       DetectError.SeePhone.errorCode == result -> {
                           (activity as VFTransactionActivity).handleEMVFallbackFromError(
                               activity.getString(R.string.contactless_seephone_error),
                               activity.getString(R.string.please_use_another_card_for_transaction),
                               false
                           ) { alertCBBool ->
                               if (alertCBBool)
                                   try {
                                       (activity as VFTransactionActivity).doProcessCard()
                                   } catch (ex: Exception) {
                                       ex.printStackTrace()
                                   }
                           }
                       }
                       DetectError.RefuseTrans.errorCode == result -> {
                           //Implement
                       }

                   }

               }
               else -> {
               }
           }*/

        /*     Log.d(MainActivity.TAG, "onTransactionResult")
             val msg = data?.getString("ERROR")
             //VFService.showToast("onTransactionResult result = $result,msg = $msg")

             println("Errror msg in transaction result is$msg")

             when (result) {
                 ConstPBOCHandler.onTransactionResult.result.EMV_CARD_BIN_CHECK_FAIL -> {
                     // read card fail
                     //VFService.showToast("read card fail")
                     return
                 }
                 ConstPBOCHandler.onTransactionResult.result.EMV_MULTI_CARD_ERROR -> {
                     // multi-cards found
                     data?.getString(ConstPBOCHandler.onTransactionResult.data.KEY_ERROR_String)
                         ?.let {
                             //VFService.showToast(it)
                         }
                     return
                 }
             }*/
    }

    //3,2,1 --->
    override fun onRequestInputPIN(isOnlinePin: Boolean, retryTimes: Int) {
        //    VFService.showToast("onRequestInputPIN isOnlinePin:$isOnlinePin")
        println("Invalid pin" + retryTimes)
        cardProcessedDataModal.setPinEntryFlag("1")
        retryTimess = retryTimes
        if (isOnlinePin) {
            cardProcessedDataModal.setPinEntryFlag("1")
            //For Online Pin
            //Here we are inflating PinPad on App UI:-
            cardProcessedDataModal.setIsOnline(1)
            VFService.openPinPad(cardProcessedDataModal, activity)
        } else {
            cardProcessedDataModal.setPinEntryFlag("2")
            //For Offline Pin
            //Here we are inflating PinPad on App UI:-
            cardProcessedDataModal.setRetryTimes(retryTimes)
            cardProcessedDataModal.setIsOnline(2) //
            if (retryTimess >= 3) {
                retryTimess = 3
                maxPin = 3
            } else {
                retryTimess = retryTimes
            }
            if (maxPin == 3) {
                when (retryTimess) {
                    3 -> {

                        VFService.openPinPad(cardProcessedDataModal, activity)
                    }
                    2 -> {
                        GlobalScope.launch(Dispatchers.Main) {
                            (activity as BaseActivity).alertBoxWithAction(
                                null, null, "Invalid PIN",
                                "Wrong PIN please try again", false, "OK", { alertCallback ->
                                    if (alertCallback) {
                                        VFService.openPinPad(cardProcessedDataModal, activity)
                                    }

                                }) { alertCallback ->
                                if (alertCallback) {

                                    VFService.openPinPad(cardProcessedDataModal, activity)
                                }
                            }
                        }
                    }
                    1 -> {
                        GlobalScope.launch(Dispatchers.Main) {
                            (activity as BaseActivity).alertBoxWithAction(
                                null, null, "Invalid PIN",
                                "This is your last attempt", false, "OK", { alertCallback ->
                                    if (alertCallback) {
                                        VFService.openPinPad(cardProcessedDataModal, activity)
                                    }
                                }) { alertCallback ->
                                if (alertCallback) {
                                    VFService.openPinPad(cardProcessedDataModal, activity)
                                }
                            }
                        }
                    }
                }
            } else if (retryTimess == 2) {
                when (retryTimess) {
                    2 -> {
                        VFService.openPinPad(cardProcessedDataModal, activity)
                        /*      GlobalScope.launch(Dispatchers.Main) {
                                  (activity as BaseActivity).alertBoxWithAction(
                                      null, null, "Invalid PIN",
                                      "Wrong PIN please try again", false, "OK", {}) { alertCallback ->
                                      if (alertCallback) {

                                          VFService.openPinPad(cardProcessedDataModal, activity)
                                      }
                                  }
                              }*/
                    }
                    1 -> {
                        GlobalScope.launch(Dispatchers.Main) {
                            (activity as BaseActivity).alertBoxWithAction(
                                null,
                                null,
                                "Invalid PIN",
                                "This is your last attempt",
                                false,
                                "OK",
                                { alertCallback ->
                                    if (alertCallback) {
                                        VFService.openPinPad(cardProcessedDataModal, activity)
                                    }

                                }) { alertCallback ->
                                if (alertCallback) {
                                    VFService.openPinPad(cardProcessedDataModal, activity)
                                }
                            }
                        }
                    }
                }
            } else {
                when (retryTimess) {
                    1 -> {
                        GlobalScope.launch(Dispatchers.Main) {
                            (activity as BaseActivity).alertBoxWithAction(
                                null, null, "Invalid PIN",
                                "This is your last attempt", false, "OK", { alertCallback ->
                                    if (alertCallback) {
                                        VFService.openPinPad(cardProcessedDataModal, activity)
                                    }
                                }) { alertCallback ->
                                if (alertCallback) {
                                    VFService.openPinPad(cardProcessedDataModal, activity)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    private fun processField57Data() {

        var dash = "~"
        var cardHolderName =
            if (isNullOrEmpty(cardProcessedDataModal.getCardHolderName())) "~" else cardProcessedDataModal.getCardHolderName() + dash
        var applicationlabel =
            if (isNullOrEmpty(cardProcessedDataModal.getApplicationLabel())) "~" else cardProcessedDataModal.getApplicationLabel() + dash
        var cardissuercountrycode =
            if (isNullOrEmpty(cardProcessedDataModal.getCardIssuerCountryCode())) "~" else cardProcessedDataModal.getCardIssuerCountryCode() + dash

        var track21 = "35,36|${
            cardProcessedDataModal.getTrack1Data()?.replace("D", "=")?.replace("F", "")
        }" + "|" + cardHolderName + applicationlabel + cardissuercountrycode +
                cardProcessedDataModal.getTypeOfTxnFlag() + "~" + cardProcessedDataModal.getPinEntryFlag()

        println(
            "Field 57 before encryption is -> 35,36|${
                cardProcessedDataModal.getTrack1Data()?.replace("D", "=")?.replace("F", "")
            }" + "|" + cardHolderName + applicationlabel + cardissuercountrycode +
                    cardProcessedDataModal.getTypeOfTxnFlag() + "~" + cardProcessedDataModal.getPinEntryFlag()
        )

        val DIGIT_8 = 8

        val mod = track21.length % DIGIT_8
        if (mod != 0) {
            track21 = getEncryptedField57DataForVisa(track21.length, track21)
        }
        val byteArray = track21.toByteArray(StandardCharsets.ISO_8859_1)
        val encryptedTrack2ByteArray: ByteArray? =
            VFService.vfPinPad?.encryptTrackData(0, 2, byteArray)
        /*println(  "Track 2 with encyption is --->" + Utility.byte2HexStr(encryptedTrack2ByteArray ))*/
        cardProcessedDataModal.setTrack2Data(Utility.byte2HexStr(encryptedTrack2ByteArray))

    }

    //Below method is used to get Field55:-
    private fun processField55Data() {
        GlobalScope.launch(Dispatchers.IO) {
            if (tagOfF55 != null) {
                for (i in 0 until tagOfF55!!.size()) {
                    val tag = tagOfF55?.keyAt(i)
                    val value = tagOfF55?.valueAt(i)

                    val indexedValue: Boolean = tag == 24372
                    if (indexedValue) {
                        val applicationPanSequenceNumber = tagOfF55?.valueAt(i)
                        if (applicationPanSequenceNumber != null) {
                            cardProcessedDataModal.setApplicationPanSequenceValue("" + applicationPanSequenceNumber)
                        }
                    }

                }

                tagOfF55 = null

                when (cardProcessedDataModal.getReadCardType()) {
                    DetectCardType.EMV_CARD_TYPE -> {
                        val f55 = getField55(false, cardProcessedDataModal)
                        cardProcessedDataModal.setField55(f55)
                        //println("Field 55 is -> " + f55)
                        vfEmvHandlerCallback(cardProcessedDataModal)
                    }
                    DetectCardType.CONTACT_LESS_CARD_TYPE -> {
                        val f55 = getField55(cardProcessedDataModal = cardProcessedDataModal)
                        cardProcessedDataModal.setField55(f55)
                        //println("Field 55 is -> " + f55)
                        vfEmvHandlerCallback(cardProcessedDataModal)
                    }
                    DetectCardType.CONTACT_LESS_CARD_WITH_MAG_TYPE -> {
                        val f55 = getField55(cardProcessedDataModal = cardProcessedDataModal)
                        cardProcessedDataModal.setField55(f55)
                        //println("Field 55 is -> " + f55)
                        vfEmvHandlerCallback(cardProcessedDataModal)
                    }
                    DetectCardType.MAG_CARD_TYPE -> {
                        val f55 = ""
                        cardProcessedDataModal.setField55("")
                        //println("Field 55 is -> " + "")
                        vfEmvHandlerCallback(cardProcessedDataModal)
                    }

                }

                //VFEmv.mIsoWriter?.filed55Data = f55
                //   println("field55 data is ----> $f55")
                /* this.createNormalPackage(
                     VFEmv.mIsoWriter,
                     VFEmv.transactionalAmmount,
                     VFEmv.cashBackAmount,
                     VFEmv.mXmlModel
                 )*/

            }

        }
    }

    //App Selection Rv
    private fun inflateAppsDialog(appList: MutableList<Bundle>, multiAppCB: (Int) -> Unit) {
        activity.runOnUiThread {
            var appSelectedPosition = 1
            val dialog = Dialog(activity)
            dialog.apply {
                requestWindowFeature(Window.FEATURE_NO_TITLE)
                setCancelable(false)
                setContentView(R.layout.multiapp_selection_layout)
                /*val body = dialog.findViewById(R.id.body) as TextView
                    body.text = title*/
                this.findViewById<RecyclerView>(R.id.apps_Rv)?.apply {
                    // set a LinearLayoutManager to handle Android
                    // RecyclerView behavior
                    layoutManager = LinearLayoutManager(activity)
                    // set the custom adapter to the RecyclerView
                    adapter = MultiSelectionAppAdapter(appList, dialog) {
                        appSelectedPosition = it
                    }
                }

                this.findViewById<BHButton>(R.id.cancel_btnn)?.setOnClickListener {
                    logger("cancel_Btn", "$appSelectedPosition  ", "e")
                    dismiss()
                    iemv?.stopCheckCard()
                    multiAppCB(0)
                    val intent = Intent(activity, MainActivity::class.java)
                    intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    activity.startActivity(intent)
                }

                this.findViewById<BHButton>(R.id.ok_btnn)?.setOnClickListener {
                    // iemv?.importAppSelection(appSelectedPosition)
                    logger("OkBtn", "$appSelectedPosition  ", "e")
                    try {
                        multiAppCB(appSelectedPosition)
                        dialog.dismiss()
                    } catch (ex: java.lang.Exception) {
                        ex.printStackTrace()
                        dialog.dismiss()
                        (activity as VFTransactionActivity).alertBoxWithAction(
                            null,
                            null,
                            activity.getString(R.string.app_selection_failed),
                            activity.getString(R.string.please_reinitiate_transaction),
                            false,
                            activity.getString(R.string.positive_button_ok),
                            {
                                activity.startActivity(
                                    Intent(
                                        activity,
                                        MainActivity::class.java
                                    ).apply {
                                        flags =
                                            Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_CLEAR_TASK
                                    })
                            },
                            {})
                    }
                }
            }.show()
        }
    }
}


open class MultiSelectionAppAdapter(
    var appList: MutableList<Bundle>,
    var dialog: Dialog,
    var updatePosition: (Int) -> Unit
) :
    RecyclerView.Adapter<MultiSelectionAppViewHolder>() {
    private var lastSelectedPosition = 0
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MultiSelectionAppViewHolder {
        return MultiSelectionAppViewHolder(
            LayoutInflater.from(parent.context)
                .inflate(R.layout.item_multiapp_selection_layout, parent, false)
        )
    }

    override fun getItemCount(): Int = appList.size

    override fun onBindViewHolder(holder: MultiSelectionAppViewHolder, position: Int) {


        val aidBundle = appList[position]
        val aidName = aidBundle.getString("aidName")
        val aid = aidBundle.getString("aid")
        val aidLabel = aidBundle.getString("aidLabel")
        Log.i("TAG", "AID Name=$aidName | AID Label=$aidLabel | AID=$aid")
        val appRB = holder.view.findViewById<RadioButton>(R.id.app_Rb)
        appRB.text = aidLabel

        //since only one radio button is allowed to be selected,
        // this condition un-checks previous selections
        appRB.isChecked = lastSelectedPosition == position

        appRB.setOnClickListener {
            //VFService.showToast("onSelectApplication..." + (appList[position]))

            lastSelectedPosition = position
            updatePosition(position + 1)
            notifyDataSetChanged()
        }
    }
}

class MultiSelectionAppViewHolder(val view: View) : RecyclerView.ViewHolder(view)

