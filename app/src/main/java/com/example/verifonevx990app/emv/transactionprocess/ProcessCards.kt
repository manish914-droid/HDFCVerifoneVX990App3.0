package com.example.verifonevx990app.emv.transactionprocess

import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.os.*
import android.text.TextUtils
import android.util.Log
import com.example.verifonevx990app.R
import com.example.verifonevx990app.bankemi.EMISchemeAndOfferActivity
import com.example.verifonevx990app.bankemi.GenericEMISchemeAndOffer
import com.example.verifonevx990app.brandemi.BrandEMIDataModal
import com.example.verifonevx990app.crosssell.FlexiPayReqSentServerAndParseData
import com.example.verifonevx990app.main.DetectCardType
import com.example.verifonevx990app.main.DetectError
import com.example.verifonevx990app.main.MainActivity
import com.example.verifonevx990app.main.PosEntryModeType
import com.example.verifonevx990app.realmtables.TerminalParameterTable
import com.example.verifonevx990app.utils.GenericReadCardData
import com.example.verifonevx990app.utils.Utility
import com.example.verifonevx990app.vxUtils.*
import com.example.verifonevx990app.vxUtils.ROCProviderV2.getEncryptedTrackData
import com.vfi.smartpos.deviceservice.aidl.CheckCardListener
import com.vfi.smartpos.deviceservice.aidl.IEMV
import com.vfi.smartpos.deviceservice.aidl.IssuerUpdateHandler
import com.vfi.smartpos.deviceservice.aidl.PinInputListener
import com.vfi.smartpos.deviceservice.constdefine.ConstCheckCardListener
import com.vfi.smartpos.deviceservice.constdefine.ConstIPBOC
import com.vfi.smartpos.deviceservice.constdefine.ConstIPinpad
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.jvm.Throws

class ProcessCard(private var issuerUpdateHandler: IssuerUpdateHandler?,var activity: Activity, var handler: Handler, var cardProcessedDataModal: CardProcessedDataModal, var brandEmiData:BrandEMIDataModal?,var transactionCallback: (CardProcessedDataModal) -> Unit) {
    var transactionalAmt = cardProcessedDataModal.getTransactionAmount() ?: 0
    var otherAmt = cardProcessedDataModal.getOtherAmount() ?: 0

    init {
        detectCard()
    }

    // Detecting the card type ie(emv,cls,mag...)
    private fun detectCard(fallbackType: Int = 0) {
        //Toast to show for the use card case:-
        //VFService.showToast("start check card\nUse you card please")
        var iemv: IEMV? = VFService.vfIEMV

        try {
            //Below checkCard process is happening with the help of IEMV AIDL Interface:-
            iemv?.checkCard(getCardOptionBundle(cardProcessedDataModal), 30, object : CheckCardListener.Stub() {

                //This Override Function will only execute in case of Mag stripe card type:-
                override fun onCardSwiped(track: Bundle) {
                    // Process Swipe card with or without PIN .
                    fun processSwipeCardWithPINorWithoutPIN(ispin: Boolean, cardProcessedDataModal: CardProcessedDataModal) {
                        if (ispin) {
                            val param = Bundle()
                            val globleparam = Bundle()
                            val panBlock: String? = cardProcessedDataModal.getPanNumberData()
                            val pinLimit = byteArrayOf(4, 5, 6)
                            param.putByteArray(
                                ConstIPinpad.startPinInput.param.KEY_pinLimit_ByteArray,
                                pinLimit
                            )
                            param.putInt(ConstIPinpad.startPinInput.param.KEY_timeout_int, 20)
                            param.putBoolean(
                                ConstIPinpad.startPinInput.param.KEY_isOnline_boolean,
                                ispin
                            )
                            param.putString(
                                ConstIPinpad.startPinInput.param.KEY_pan_String,
                                panBlock
                            )
                            param.putString(
                                ConstIPinpad.startPinInput.param.KEY_promptString_String,
                                "Enter PIN"
                            )
                            param.putInt(
                                ConstIPinpad.startPinInput.param.KEY_desType_int,
                                ConstIPinpad.startPinInput.param.Value_desType_3DES
                            )


                            VFService.vfPinPad?.startPinInput(
                                2, param, globleparam,
                                object : PinInputListener.Stub() {
                                    override fun onInput(len: Int, key: Int) {
                                        Log.d("Data", "PinPad onInput, len:$len, key:$key")
                                    }

                                    @Throws(RemoteException::class)
                                    override fun onConfirm(data: ByteArray, isNonePin: Boolean) {
                                        Log.d("Data", "PinPad onConfirm")
                                        Log.d(
                                            "SWIPEPIN",
                                            "PinPad hex encrypted data ---> " + Utility.byte2HexStr(
                                                data
                                            )
                                        )

                                        cardProcessedDataModal.setGeneratePinBlock(
                                            Utility.byte2HexStr(
                                                data
                                            )
                                        )

                                        if (cardProcessedDataModal.getFallbackType() == EFallbackCode.EMV_fallback.fallBackCode)
                                            cardProcessedDataModal.setPosEntryMode(PosEntryModeType.EMV_POS_ENTRY_FALL_MAGPIN.posEntry.toString())
                                        else
                                            cardProcessedDataModal.setPosEntryMode(PosEntryModeType.POS_ENTRY_SWIPED_NO4DBC_PIN.posEntry.toString())

                                        cardProcessedDataModal.setApplicationPanSequenceValue("00")
                                        transactionCallback(cardProcessedDataModal)

                                    }

                                    @Throws(RemoteException::class)
                                    override fun onCancel() {
                                        Log.d("Data", "PinPad onCancel")
                                        GlobalScope.launch(Dispatchers.Main) {
                                            (activity as VFTransactionActivity).declinedTransaction()
                                        }
                                    }

                                    @Throws(RemoteException::class)
                                    override fun onError(errorCode: Int) {
                                        Log.d("Data", "PinPad onError, code:$errorCode")
                                        GlobalScope.launch(Dispatchers.Main) {
                                            (activity as VFTransactionActivity).declinedTransaction()
                                        }
                                    }
                                })
                        } else {
                            if (cardProcessedDataModal.getFallbackType() == EFallbackCode.EMV_fallback.fallBackCode)
                                cardProcessedDataModal.setPosEntryMode(PosEntryModeType.EMV_POS_ENTRY_FALL_MAGNOPIN.posEntry.toString())
                            else
                                cardProcessedDataModal.setPosEntryMode(PosEntryModeType.POS_ENTRY_SWIPED_NO4DBC.posEntry.toString())
                            cardProcessedDataModal.setApplicationPanSequenceValue("00")
                            transactionCallback(cardProcessedDataModal)
                        }
                    }

                    //Reading Swipe Card data
                    try {
                        iemv?.stopCheckCard()
                        println("Mag is calling")
                        cardProcessedDataModal.setTypeOfTxnFlag("1")
                        if (fallbackType != EFallbackCode.Swipe_fallback.fallBackCode) {
                            Log.d(MainActivity.TAG, "onCardSwiped ...")
                            VFService.vfBeeper?.startBeep(200)
                            val pan =
                                track.getString(ConstCheckCardListener.onCardSwiped.track.KEY_PAN_String)
                            val track1 =
                                track.getString(ConstCheckCardListener.onCardSwiped.track.KEY_TRACK1_String)
                            val track2 =
                                track.getString(ConstCheckCardListener.onCardSwiped.track.KEY_TRACK2_String)
                            val track3 =
                                track.getString(ConstCheckCardListener.onCardSwiped.track.KEY_TRACK3_String)
                            val serviceCode =
                                track.getString(ConstCheckCardListener.onCardSwiped.track.KEY_SERVICE_CODE_String)

                            val currDate = getCurrentDateforMag()
                            val validDate =
                                track.getString(ConstCheckCardListener.onCardSwiped.track.KEY_EXPIRED_DATE_String)

                            if (currDate.compareTo(validDate!!) <= 0) {
                                println("Correct Date")

                                Log.d(MainActivity.TAG, "onCardSwiped ...1")
                                val bytes: ByteArray = ROCProviderV2.hexStr2Byte(track2)
                                Log.d(
                                    MainActivity.TAG,
                                    "Track2:" + track2 + " (" + ROCProviderV2.byte2HexStr(bytes) + ")"
                                )

                                Utility.getCardHolderName(cardProcessedDataModal, track1, '^', '^')

                                var bIsKeyExist: Boolean? = VFService.getPinPadData()
                                if (!bIsKeyExist!!) {
                                    Log.e(MainActivity.TAG, "no key exist type: 12, @: 1")
                                }
                                val enctypted: ByteArray? =
                                    VFService.getDupkt(
                                        1,
                                        1,
                                        1,
                                        bytes,
                                        byteArrayOf(0, 0, 0, 0, 0, 0, 0, 0)
                                    )
                                if (null == enctypted) {
                                    Log.e(MainActivity.TAG, "NO DUKPT Encrypted got")
                                } else {
                                    Log.d(
                                        MainActivity.TAG,
                                        "DUKPT:" + ROCProviderV2.byte2HexStr(enctypted)
                                    )
                                }
                                bIsKeyExist = VFService.getPinPadData()
                                if (!bIsKeyExist!!) {
                                    Log.e(MainActivity.TAG, "no key exist type: 12, @: 1")
                                }

                                Log.d(MainActivity.TAG, "onCardSwiped ...3")

                                //Stubbing Card Processed Data:-
                                cardProcessedDataModal.setReadCardType(DetectCardType.MAG_CARD_TYPE)
                                if (track2 != null) {
                                    cardProcessedDataModal.setTrack2Data(
                                        getEncryptedTrackData(
                                            track2,
                                            cardProcessedDataModal
                                        ).toString()
                                    )
                                }
                                if (track1 != null) {
                                    cardProcessedDataModal.setTrack1Data(track1)
                                }
                                if (track3 != null) {
                                    cardProcessedDataModal.setTrack3Data(track3)
                                }
                                if (pan != null) {
                                    cardProcessedDataModal.setPanNumberData(pan)
                                    cardProcessedDataModal.getPanNumberData()?.let {
                                        logger(
                                            "SWIPE_PAN",
                                            it, "e"
                                        )
                                    }
                                    //  cardProcessedDataModal.setPanNumberData("6789878786")
                                    if (!cardProcessedDataModal.getPanNumberData()
                                            ?.let { cardLuhnCheck(it) }!!) {
                                        onError(
                                            DetectError.IncorrectPAN.errorCode,
                                            "Invalid Card Number"
                                        )
                                    } else {
                                        if (serviceCode != null) {
                                            cardProcessedDataModal.setServiceCodeData(serviceCode)
                                        }
                                        val sc = cardProcessedDataModal.getServiceCodeData()
                                        // val sc: String? = ""
                                        var scFirstByte: Char? = null
                                        var scLastbyte: Char? = null
                                        if (null != sc) {
                                            scFirstByte = sc.first()
                                            scLastbyte = sc.last()

                                        }
                                        //Checking the card has a PIN or WITHOUTPIN
                                        val isPin =
                                            scLastbyte == '0' || scLastbyte == '3' || scLastbyte == '5' || scLastbyte == '6' || scLastbyte == '7'
                                        //Here we are bypassing the pin condition for test case ANSI_MAG_001.
                                        //  isPin = false
                                        if (isPin) {
                                            cardProcessedDataModal.setIsOnline(1)
                                            cardProcessedDataModal.setPinEntryFlag("1")
                                        } else {
                                            //0 for no pin
                                            cardProcessedDataModal.setIsOnline(0)
                                            cardProcessedDataModal.setPinEntryFlag("0")
                                        }
                                        if (cardProcessedDataModal.getFallbackType() != EFallbackCode.EMV_fallback.fallBackCode) {
                                            //Checking Fallback
                                            if (scFirstByte == '2' || scFirstByte == '6') {
                                                onError(EFallbackCode.Swipe_fallback.fallBackCode, "FallBack")
                                            } else {
                                                //region================Condition Check and ProcessSwipeCardWithPinOrWithoutPin:-
                                                when (cardProcessedDataModal.getTransType()) {
                                                    TransactionType.SALE.type -> processSwipeCardWithPINorWithoutPIN(isPin, cardProcessedDataModal)
                                                    TransactionType.EMI_SALE.type,
                                                    TransactionType.BRAND_EMI.type,
                                                    TransactionType.TEST_EMI.type -> {
                                                        //region==========Implementing Scheme and Offer:-
                                                        cardProcessedDataModal.setEmiTransactionAmount(transactionalAmt)
                                                        if (!TextUtils.isEmpty(cardProcessedDataModal.getPanNumberData())) {
                                                            GlobalScope.launch(Dispatchers.Main) { (activity as VFTransactionActivity).showProgress();iemv?.stopCheckCard() }
                                                            GenericEMISchemeAndOffer(activity, cardProcessedDataModal, cardProcessedDataModal.getPanNumberData() ?: "", transactionalAmt,brandEmiData) { bankEMISchemeAndTAndCData, hostResponseCodeAndMessage ->
                                                                GlobalScope.launch(Dispatchers.Main) {
                                                                    if (hostResponseCodeAndMessage.first) {
                                                                        (activity as VFTransactionActivity).startActivityForResult(
                                                                            Intent(activity, EMISchemeAndOfferActivity::class.java).apply {
                                                                                putParcelableArrayListExtra("emiSchemeDataList", bankEMISchemeAndTAndCData.first as ArrayList<out Parcelable>)
                                                                                putParcelableArrayListExtra("emiTAndCDataList", bankEMISchemeAndTAndCData.second as ArrayList<out Parcelable>)
                                                                                putExtra("cardProcessedData", cardProcessedDataModal)
                                                                                putExtra("transactionType",cardProcessedDataModal.getTransType()) // for Transaction Type
                                                                            },
                                                                            EIntentRequest.BankEMISchemeOffer.code
                                                                        )
                                                                        (activity as VFTransactionActivity).hideProgress()
                                                                    } else {
                                                                        (activity as VFTransactionActivity).hideProgress()
                                                                        (activity as VFTransactionActivity).alertBoxWithAction(null, null, activity.getString(R.string.error), hostResponseCodeAndMessage.second, false, activity.getString(R.string.positive_button_ok), { (activity as VFTransactionActivity).declinedTransaction() },
                                                                            {})
                                                                    }

                                                                }
                                                            }
                                                        }
                                                        //endregion
                                                    }
                                                    else -> processSwipeCardWithPINorWithoutPIN(
                                                        isPin, cardProcessedDataModal
                                                    )
                                                }
                                                //endregion
                                            }
                                        } else {
                                            //region================Condition Check and ProcessSwipeCardWithPinOrWithoutPin:-
                                            when (cardProcessedDataModal.getTransType()) {
                                                TransactionType.SALE.type -> processSwipeCardWithPINorWithoutPIN(isPin, cardProcessedDataModal)
                                                TransactionType.EMI_SALE.type,
                                                TransactionType.BRAND_EMI.type,
                                                TransactionType.TEST_EMI.type -> {
                                                    cardProcessedDataModal.setEmiTransactionAmount(transactionalAmt)
                                                    //region==========Implementing Scheme and Offer:-
                                                    if (!TextUtils.isEmpty(cardProcessedDataModal.getPanNumberData())) {
                                                        GlobalScope.launch(Dispatchers.Main) { (activity as VFTransactionActivity).showProgress();iemv?.stopCheckCard() }
                                                        GenericEMISchemeAndOffer(activity, cardProcessedDataModal, cardProcessedDataModal.getPanNumberData() ?: "", transactionalAmt,brandEmiData) { bankEMISchemeAndTAndCData, hostResponseCodeAndMessage ->
                                                            GlobalScope.launch(Dispatchers.Main) {
                                                                if (hostResponseCodeAndMessage.first) {
                                                                    (activity as VFTransactionActivity).startActivityForResult(
                                                                        Intent(activity, EMISchemeAndOfferActivity::class.java).apply {
                                                                            putParcelableArrayListExtra("emiSchemeDataList", bankEMISchemeAndTAndCData.first as ArrayList<out Parcelable>)
                                                                            putParcelableArrayListExtra("emiTAndCDataList", bankEMISchemeAndTAndCData.second as ArrayList<out Parcelable>)
                                                                            putExtra("cardProcessedData", cardProcessedDataModal)
                                                                            putExtra("transactionType",cardProcessedDataModal.getTransType())
                                                                        },
                                                                        EIntentRequest.BankEMISchemeOffer.code
                                                                    )
                                                                    (activity as VFTransactionActivity).hideProgress()
                                                                } else {
                                                                    (activity as VFTransactionActivity).hideProgress()
                                                                    (activity as VFTransactionActivity).alertBoxWithAction(
                                                                        null,
                                                                        null,
                                                                        activity.getString(R.string.error),
                                                                        hostResponseCodeAndMessage.second,
                                                                        false,
                                                                        activity.getString(R.string.positive_button_ok),
                                                                        {
                                                                            (activity as VFTransactionActivity).declinedTransaction()
                                                                        },
                                                                        {})
                                                                }

                                                            }
                                                        }
                                                    }
                                                    //endregion
                                                }
                                                else -> processSwipeCardWithPINorWithoutPIN(
                                                    isPin, cardProcessedDataModal
                                                )
                                            }
                                            //endregion
                                        }
                                    }
                                    //  }
                                } else {
                                    onError(
                                        fallbackType,
                                        "Try other option + ${EFallbackCode.Swipe_fallback.fallBackCode}"
                                    )
                                }
                            } else {
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
                                // println("Incorrect Date")
                            }


                        }
                    } catch (ex: NoSuchElementException) {
                        ex.printStackTrace()
                        VFService.showToast("Please use your card again now")
                        detectCard(fallbackType)
                    } catch (ex: DeadObjectException) {
                        ex.printStackTrace()
                        println("Process card error22" + ex.message)
                        Handler(Looper.getMainLooper()).postDelayed({
                            GlobalScope.launch {
                                VFService.connectToVFService(VerifoneApp.appContext)
                                delay(100)
                                iemv = VFService.vfIEMV
                                delay(100)
                                detectCard(0)
                            }
                        }, 200)
                    } catch (ex: RemoteException) {
                        ex.printStackTrace()
                        println("Process card error23" + ex.message)
                        Handler(Looper.getMainLooper()).postDelayed({
                            GlobalScope.launch {
                                VFService.connectToVFService(VerifoneApp.appContext)
                                delay(100)
                                iemv = VFService.vfIEMV
                                delay(100)
                                detectCard(0)
                            }
                        }, 200)
                    } catch (ex: Exception) {
                        ex.printStackTrace()
                        println("Process card error24" + ex.message)
                        Handler(Looper.getMainLooper()).postDelayed({
                            GlobalScope.launch {
                                VFService.connectToVFService(VerifoneApp.appContext)
                                delay(100)
                                iemv = VFService.vfIEMV
                                delay(100)
                                detectCard(0)
                            }
                        }, 200)
                    }
                }

                //This Override Function will only execute in case of EMV/Insert card type:-
                override fun onCardPowerUp() {
                    try {
                        println("Contact is calling")
                        iemv?.stopCheckCard()
                        iemv?.abortEMV()
                        cardProcessedDataModal.setTypeOfTxnFlag("2")
                        cardProcessedDataModal.setReadCardType(DetectCardType.EMV_CARD_TYPE)

                        VFService.vfBeeper?.startBeep(200)
                        println("TransactionAmount is calling" + transactionalAmt.toString() + "Handler is" + handler)
                        when (cardProcessedDataModal.getTransType()) {
                            TransactionType.EMI_SALE.type, TransactionType.BRAND_EMI.type, TransactionType.TEST_EMI.type -> {
                                //region=========This Field is use only in case of BankEMI Field58 Transaction Amount:-
                                cardProcessedDataModal.setEmiTransactionAmount(transactionalAmt)
                                //endregion
                                iemv?.startEMV(ConstIPBOC.startEMV.processType.full_process, Bundle(), GenericReadCardData(activity, iemv) { cardBinValue ->
                                        /*iemv?.stopCheckCard()
                                        iemv?.abortEMV()*/
                                        Log.e("Card Bin Emi", cardBinValue)
                                        if (!TextUtils.isEmpty(cardBinValue)) {
                                            GlobalScope.launch(Dispatchers.Main) {
                                                (activity as VFTransactionActivity).showProgress();/*iemv?.stopCheckCard()*/
                                            }
                                            GenericEMISchemeAndOffer(activity, cardProcessedDataModal, cardBinValue, transactionalAmt,brandEmiData) { bankEMISchemeAndTAndCData, hostResponseCodeAndMessage ->
                                                GlobalScope.launch(Dispatchers.Main) {
                                                    if (hostResponseCodeAndMessage.first) {
                                                        (activity as VFTransactionActivity).startActivityForResult(
                                                            Intent(activity, EMISchemeAndOfferActivity::class.java).apply {
                                                                putParcelableArrayListExtra("emiSchemeDataList", bankEMISchemeAndTAndCData.first as ArrayList<out Parcelable>)
                                                                putParcelableArrayListExtra("emiTAndCDataList", bankEMISchemeAndTAndCData.second as ArrayList<out Parcelable>)
                                                                putExtra("cardProcessedData", cardProcessedDataModal)
                                                                putExtra("transactionType",cardProcessedDataModal.getTransType())
                                                            },
                                                            EIntentRequest.BankEMISchemeOffer.code)
                                                        (activity as VFTransactionActivity).hideProgress()
                                                    } else {
                                                        (activity as VFTransactionActivity).hideProgress()
                                                        (activity as VFTransactionActivity).alertBoxWithAction(null, null, activity.getString(R.string.error), hostResponseCodeAndMessage.second, false, activity.getString(R.string.positive_button_ok),
                                                            {
                                                                (activity as VFTransactionActivity).declinedTransaction()
                                                            },
                                                            {})
                                                    }
                                                }
                                            }
                                        }
                                    })
                            }

                            TransactionType.FLEXI_PAY.type -> {
                                //region=========This Field is use only in case of flexipay Field58 Transaction Amount:-
                                cardProcessedDataModal.setFlexiPayTransactionAmount(transactionalAmt)
                                //endregion
                                iemv?.startEMV(
                                    ConstIPBOC.startEMV.processType.full_process,
                                    Bundle(),
                                    GenericReadCardData(activity, iemv) { cardBinValue ->
                                        /*iemv?.stopCheckCard()
                                        iemv?.abortEMV()*/
                                        val panEncypted = getEncryptedPan(cardBinValue)
                                        Log.e(
                                            "FLEXI PAY",
                                            "CARD BIN ------->> $cardBinValue   ========= ENCHRypted  --->  $panEncypted"
                                        )
                                        //  cardProcessedDataModal.setTrack2Data(Utility.byte2HexStr(encryptedTrack2ByteArray))
                                        if (!TextUtils.isEmpty(panEncypted)) {
                                            /*GlobalScope.launch(Dispatchers.Main) { (activity as VFTransactionActivity).showProgress();iemv?.stopCheckCard() }*/
                                            FlexiPayReqSentServerAndParseData(
                                                panEncypted,
                                                activity,
                                                cardProcessedDataModal,
                                                transactionalAmt.toString()
                                            ) { flexiPayData, isSucess, hostMsg ->
                                                GlobalScope.launch(Dispatchers.Main) {
                                                    if (isSucess) {
                                                        (activity as VFTransactionActivity).startActivityForResult(
                                                            Intent(activity, EMISchemeAndOfferActivity::class.java).apply {
                                                                putParcelableArrayListExtra("flexiPayData", flexiPayData as ArrayList<out Parcelable>)
                                                                putExtra("cardProcessedData", cardProcessedDataModal)
                                                            },
                                                            EIntentRequest.FlexiPaySchemeOffer.code
                                                        )
                                                        (activity as VFTransactionActivity).hideProgress()
                                                    } else {
                                                        (activity as VFTransactionActivity).hideProgress()
                                                        (activity as VFTransactionActivity).alertBoxWithAction(null, null, activity.getString(R.string.error), hostMsg, false, activity.getString(R.string.positive_button_ok), {
                                                                (activity as VFTransactionActivity).declinedTransaction()
                                                            },
                                                            {})
                                                    }

                                                }
                                            }
                                        }
                                    })
                            }

                            TransactionType.SALE.type -> {
                                // Checking Insta Emi Available or not
                                var hasInstaEmi = false
                                val tpt = TerminalParameterTable.selectFromSchemeTable()
                                var limitAmt = 0f
                                if (tpt?.surChargeValue?.isNotEmpty()!!) {
                                    limitAmt = try {
                                        tpt.surChargeValue.toFloat() / 100
                                    } catch (ex: Exception) {
                                        0f
                                    }
                                }
                                if (tpt.surcharge.isNotEmpty()) {
                                    hasInstaEmi = try {
                                        tpt.surcharge == "1"
                                    } catch (ex: Exception) {
                                        false
                                    }
                                }
                                // Condition executes, If insta EMI is Available on card
                                if (limitAmt <= transactionalAmt && hasInstaEmi) {
                                    //region=========This Field is use only in case of BankEMI Field58 Transaction Amount:-
                                    cardProcessedDataModal.setEmiTransactionAmount(transactionalAmt)
                                    //endregion
                                    iemv?.startEMV(ConstIPBOC.startEMV.processType.full_process,
                                        Bundle(),
                                        GenericReadCardData(activity, iemv) { cardBinValue ->
                                            if (!TextUtils.isEmpty(cardBinValue)) {
                                                GlobalScope.launch(Dispatchers.Main) { (activity as VFTransactionActivity).showProgress()
                                                    iemv?.stopCheckCard()
                                                    (activity as VFTransactionActivity).hideProgress()
                                                }
                                                // Below Code is executed if insta
                                                GenericEMISchemeAndOffer(activity, cardProcessedDataModal, cardBinValue, transactionalAmt,brandEmiData) { bankEMISchemeAndTAndCData, hostResponseCodeAndMessageAndHasEMI ->
                                                    if (hostResponseCodeAndMessageAndHasEMI.first) {
                                                        // Checking Insta Emi
                                                        if (hostResponseCodeAndMessageAndHasEMI.third) {
                                                            (activity as VFTransactionActivity).showEMISaleDialog(cardProcessedDataModal) {
                                                                if (it.getTransType() == TransactionType.EMI_SALE.type) {

                                                                    (activity as VFTransactionActivity).startActivityForResult(
                                                                        Intent(activity, EMISchemeAndOfferActivity::class.java).apply {
                                                                            putParcelableArrayListExtra("emiSchemeDataList", bankEMISchemeAndTAndCData.first as ArrayList<out Parcelable>)
                                                                            putParcelableArrayListExtra("emiTAndCDataList", bankEMISchemeAndTAndCData.second as ArrayList<out Parcelable>)
                                                                            putExtra("cardProcessedData", it)
                                                                            putExtra("transactionType", it.getTransType())
                                                                        },
                                                                        EIntentRequest.BankEMISchemeOffer.code
                                                                    )
                                                                    (activity as VFTransactionActivity).hideProgress()

                                                                } else {
                                                                    (activity as VFTransactionActivity).hideProgress()
                                                                    DoEmv(issuerUpdateHandler,activity, handler, cardProcessedDataModal, ConstIPBOC.startEMV.intent.VALUE_cardType_smart_card) { cardProcessedDataModal ->
                                                                        transactionCallback(cardProcessedDataModal)
                                                                    }
                                                                }

                                                            }

                                                        }

                                                    } else {
                                                        DoEmv(issuerUpdateHandler, activity, handler, cardProcessedDataModal, ConstIPBOC.startEMV.intent.VALUE_cardType_smart_card) { cardProcessedDataModal ->
                                                            transactionCallback(cardProcessedDataModal)
                                                        }
                                                    }
                                                }
                                            }
                                        })
                                } else {
                                    //Condition executes when  No EMI Available instantly(Directly Normal Sale)
                                    DoEmv(issuerUpdateHandler, activity, handler, cardProcessedDataModal, ConstIPBOC.startEMV.intent.VALUE_cardType_smart_card) { cardProcessedDataModal ->
                                        transactionCallback(cardProcessedDataModal)
                                    }
                                }

                            }

                            else -> {
                                DoEmv(issuerUpdateHandler, activity, handler, cardProcessedDataModal, ConstIPBOC.startEMV.intent.VALUE_cardType_smart_card) { cardProcessedDataModal ->
                                    transactionCallback(cardProcessedDataModal)
                                }
                            }
                        }
                    } catch (ex: DeadObjectException) {
                        ex.printStackTrace()
                        println("Process card error11" + ex.message)
                        Handler(Looper.getMainLooper()).postDelayed({
                            GlobalScope.launch {
                                VFService.connectToVFService(VerifoneApp.appContext)
                                delay(100)
                                iemv = VFService.vfIEMV
                                delay(100)
                                detectCard(0)
                            }
                        }, 200)
                    } catch (ex: RemoteException) {
                        ex.printStackTrace()
                        println("Process card error12" + ex.message)
                        Handler(Looper.getMainLooper()).postDelayed(Runnable {
                            GlobalScope.launch {
                                VFService.connectToVFService(VerifoneApp.appContext)
                                delay(100)
                                iemv = VFService.vfIEMV
                                delay(100)
                                detectCard(0)
                            }
                        }, 200)
                    } catch (ex: Exception) {
                        ex.printStackTrace()
                        println("Process card error13" + ex.message)
                        val builder = AlertDialog.Builder(activity)
                        object : Thread() {
                            override fun run() {
                                Looper.prepare()
                                builder.setTitle("Alert...!!")
                                builder.setCancelable(false)
                                builder.setMessage(
                                    "Service stopped , " +
                                            "Please reinitiate the transaction."
                                )
                                    .setCancelable(false)
                                    .setPositiveButton("Start") { _, _ ->
                                        forceStart(activity)
                                    }
                                    .setNeutralButton("Cancel") { dialog, _ ->
                                        dialog.dismiss()
                                        (activity).finishAffinity()
                                    }
                                val alert: AlertDialog? = builder.create()
                                alert?.show()
                                Looper.loop()
                            }
                        }.start()
                    }
                }

                //This Override Function will only execute in case of CLS card type:-
                override fun onCardActivate() {
                    try {
                        println("Contactless is calling")
                        if(null != cardProcessedDataModal.getDoubeTap() && !(cardProcessedDataModal.getDoubeTap() == true)){
                            iemv?.stopCheckCard()
                            iemv?.abortEMV()

                        }
                        cardProcessedDataModal.setTypeOfTxnFlag("3")
                        cardProcessedDataModal.setReadCardType(DetectCardType.CONTACT_LESS_CARD_TYPE)
                        VFService.vfBeeper?.startBeep(200)
                        println("Transactionamount is calling" + transactionalAmt.toString() + "Handler is" + handler)

                        if(null != cardProcessedDataModal.getDoubeTap() && !(cardProcessedDataModal.getDoubeTap() == true)){
                            //    VFService.showToast("Going in if cond")
                            DoEmv(issuerUpdateHandler,activity, handler, cardProcessedDataModal, ConstIPBOC.startEMV.intent.VALUE_cardType_contactless) { cardProcessedDataModal ->
                                transactionCallback(cardProcessedDataModal)
                            }
                        }
                        else {
                            iemv?.setIssuerUpdateScript()
                            println("Issuer update script called")
                            transactionCallback(cardProcessedDataModal)

                        }

                    } catch (ex: DeadObjectException) {
                        ex.printStackTrace()
                        println("Process card error14" + ex.message)
                        Handler(Looper.getMainLooper()).postDelayed(Runnable {
                            GlobalScope.launch {
                                VFService.connectToVFService(VerifoneApp.appContext)
                                delay(100)
                                iemv = VFService.vfIEMV
                                delay(100)
                                detectCard(0)
                            }
                        }, 200)
                    } catch (ex: RemoteException) {
                        ex.printStackTrace()
                        println("Process card error15" + ex.message)
                        Handler(Looper.getMainLooper()).postDelayed(Runnable {
                            GlobalScope.launch {
                                VFService.connectToVFService(VerifoneApp.appContext)
                                delay(100)
                                iemv = VFService.vfIEMV
                                delay(100)
                                detectCard(0)
                            }
                        }, 200)
                    } catch (ex: Exception) {
                        ex.printStackTrace()
                        println("Process card error16" + ex.message)
                        val builder = AlertDialog.Builder(activity)
                        object : Thread() {
                            override fun run() {
                                Looper.prepare()
                                builder.setTitle("Alert...!!")
                                builder.setCancelable(false)
                                builder.setMessage(
                                    "Service stopped , " +
                                            "Please reinitiate the transaction."
                                )
                                    .setCancelable(false)
                                    .setPositiveButton("Start") { _, _ ->
                                        forceStart(activity)
                                    }
                                    .setNeutralButton("Cancel") { dialog, _ ->
                                        dialog.dismiss()
                                        (activity).finishAffinity()
                                    }
                                val alert: AlertDialog? = builder.create()
                                alert?.show()
                                Looper.loop()
                            }
                        }.start()
                    }

                }

                //This Override method will call When Aidl Callback gives us TimeOut:-
                override fun onTimeout() {
                    //To resolve timeout issue
                    cardProcessedDataModal.setReadCardType(DetectCardType.CARD_ERROR_TYPE)

                      if(!TextUtils.isEmpty(AppPreference.getString(AppPreference.doubletap))) {
                                println("Going in rupay double tap timeout")
                           AppPreference.saveString(AppPreference.doubletaptimeout, AppPreference.doubletaptimeout)
                          (activity as VFTransactionActivity).processDoubleTapTimeout()
                        }
                    transactionCallback(cardProcessedDataModal)
                    VFService.showToast("timeout")
                }

                //This Override method will call when something went wrong or any kind of exception happen in case of card detecting:-
                override fun onError(error: Int, message: String) {
                    //  cardProcessedDataModal.setReadCardType(DetectCardType.CARD_ERROR_TYPE)
                  //  VFService.showToast("error:$error, msg:$message")
                    try {
                        when (error) {
                            EFallbackCode.Swipe_fallback.fallBackCode -> {
                                iemv?.stopCheckCard()
                                (activity as VFTransactionActivity).handleEMVFallbackFromError(activity.getString(R.string.fallback), activity.getString(R.string.please_use_another_option), false) {
                                    cardProcessedDataModal.setFallbackType(EFallbackCode.Swipe_fallback.fallBackCode)
                                    detectCard(error)
                                }
                            }
                            CardErrorCode.CTLS_ERROR_CODE.errorCode -> {
                                VFService.showToast("PLEASE USE ONE CARD ONLY")
                                activity.finish()
                            }
                            CardErrorCode.EMV_FALLBACK_ERROR_CODE.errorCode -> {
                                //EMV Fallback case when we insert card from other side then chip side:-
                                iemv?.stopCheckCard()
                                (activity as VFTransactionActivity).handleEMVFallbackFromError(
                                    activity.getString(
                                        R.string.fallback
                                    ), activity.getString(R.string.please_use_another_option), false
                                ) {
                                    cardProcessedDataModal.setFallbackType(EFallbackCode.EMV_fallback.fallBackCode)
                                    detectCard(error)
                                }
                            }

                            DetectError.IncorrectPAN.errorCode -> {
                                activity.runOnUiThread {
                                    (activity as BaseActivity).alertBoxWithAction(
                                        null,
                                        null,
                                        activity.getString(R.string.card_error),
                                        message,
                                        false,
                                        activity.getString(R.string.key_ok), {
                                            (activity as VFTransactionActivity).declinedTransaction()
                                        }, {

                                        }
                                    )
                                }

                            }
                        }
                    } catch (ex: DeadObjectException) {
                        ex.printStackTrace()
                        println("Process card error17" + ex.message)
                        Handler(Looper.getMainLooper()).postDelayed(Runnable {
                            GlobalScope.launch {
                                VFService.connectToVFService(VerifoneApp.appContext)
                                delay(100)
                                iemv = VFService.vfIEMV
                                delay(100)
                                detectCard(0)
                            }
                        }, 200)

                    } catch (ex: RemoteException) {
                        ex.printStackTrace()
                        println("Process card error18" + ex.message)
                        Handler(Looper.getMainLooper()).postDelayed(Runnable {
                            GlobalScope.launch {
                                VFService.connectToVFService(VerifoneApp.appContext)
                                delay(100)
                                iemv = VFService.vfIEMV
                                delay(100)
                                detectCard(0)
                            }
                        }, 200)
                    }
                }

            })
        } catch (ex: DeadObjectException) {
            ex.printStackTrace()
            println("Process card error19" + ex.message)
            Handler(Looper.getMainLooper()).postDelayed({
                GlobalScope.launch {
                    VFService.connectToVFService(VerifoneApp.appContext)
                    delay(100)
                    iemv = VFService.vfIEMV
                    delay(100)
                    detectCard(0)
                }
            }, 200)

        } catch (ex: RemoteException) {
            ex.printStackTrace()
            println("Process card error20" + ex.message)
            Handler(Looper.getMainLooper()).postDelayed({
                GlobalScope.launch {
                    VFService.connectToVFService(VerifoneApp.appContext)
                    delay(100)
                    iemv = VFService.vfIEMV
                    delay(100)
                    detectCard(0)
                }
            }, 200)
        } catch (ex: Exception) {
            ex.printStackTrace()
            println("Process card error21" + ex.message)
            Handler(Looper.getMainLooper()).postDelayed({
                GlobalScope.launch {
                    VFService.connectToVFService(VerifoneApp.appContext)
                    delay(100)
                    iemv = VFService.vfIEMV
                    delay(100)
                    detectCard(0)
                }
            }, 200)
        }
    }


}