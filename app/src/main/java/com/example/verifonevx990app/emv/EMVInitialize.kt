package com.example.verifonevx990app.emv

import android.os.Bundle
import android.os.RemoteException
import android.util.Log
import android.util.SparseArray
import com.example.verifonevx990app.main.MainActivity
import com.example.verifonevx990app.main.TransType
import com.example.verifonevx990app.realmtables.TerminalParameterTable
import com.example.verifonevx990app.utils.IsoPackageWriter
import com.example.verifonevx990app.vxUtils.ROCProviderV2
import com.example.verifonevx990app.vxUtils.VFService
import com.example.verifonevx990app.vxUtils.VFService.showToast
import com.vfi.smartpos.deviceservice.aidl.CheckCardListener
import com.vfi.smartpos.deviceservice.aidl.IEMV
import com.vfi.smartpos.deviceservice.constdefine.ConstCheckCardListener
import com.vfi.smartpos.deviceservice.constdefine.ConstIPBOC

// Commenting (Unused file By luckysingh)
object EMVInitialize {

    var savedPan = "8880197100005603384"
    var data8583: SparseArray<String>? = null
    /* var terminalID = "01020304"
     var merchantName = "X990 EMV Demo"
     var merchantID = "ABCDE0123456789"*/

    private var terminalParameterTable: TerminalParameterTable? = null
    private var isoPackageWriter: IsoPackageWriter? = null

    private var transactionalAmmount: Long = 0L

    fun doBalance(
        iemv: IEMV?,
        terminalParameterTable: TerminalParameterTable?,
        isoPackageWriter: IsoPackageWriter,
        transactionamount: Long
    ) {

        this.isoPackageWriter = isoPackageWriter
        this.terminalParameterTable = terminalParameterTable
        transactionalAmmount = transactionamount

        val data8583_u_balance = SparseArray<String>()
        data8583_u_balance.put(0, "0200")
        data8583_u_balance.put(2, savedPan)
        data8583_u_balance.put(3, "310000")
        data8583_u_balance.put(11, "010203")
        data8583_u_balance.put(14, "9912")
        data8583_u_balance.put(22, "020") // mag
        data8583_u_balance.put(25, "00")
        data8583_u_balance.put(35, "") // track 2
        data8583_u_balance.put(49, "156") // RMB
        data8583_u_balance.put(60, "01654321")
        data8583 = data8583_u_balance
        doTransaction(TransType.T_BANLANCE, iemv, transactionalAmmount)

    }

    private fun doTransaction(transType: TransType, iemv: IEMV?, transactionalAmmount: Long) {
        if (transType == TransType.M_SIGNIN) {
            // management, no card need
            // start onlineRequest
            Thread(VFEmv.onlineRequest).start()
        } else {
            // set some fields
            //    data8583!!.put(41, terminalID)
            //    data8583!!.put(42, merchantID)

            // do search card and online request
            doSearchCard(transType, iemv, transactionalAmmount)

        }
    }


    private fun doSearchCard(transType: TransType?, iemv: IEMV?, transactionalAmmount: Long) {
        showToast("start check card\nUse you card please")
        val cardOption = Bundle()
        cardOption.putBoolean(
            ConstIPBOC.checkCard.cardOption.KEY_Contactless_boolean,
            ConstIPBOC.checkCard.cardOption.VALUE_supported
        )
        cardOption.putBoolean(
            ConstIPBOC.checkCard.cardOption.KEY_SmartCard_boolean,
            ConstIPBOC.checkCard.cardOption.VALUE_supported
        )
        cardOption.putBoolean(
            ConstIPBOC.checkCard.cardOption.KEY_MagneticCard_boolean,
            ConstIPBOC.checkCard.cardOption.VALUE_supported
        )

        try {
            iemv?.checkCard(cardOption, 30, object : CheckCardListener.Stub() {

                @Throws(RemoteException::class)
                override fun onCardSwiped(track: Bundle) {
                    Log.d(MainActivity.TAG, "onCardSwiped ...")
                    //                            iemv.stopCheckCard();
                    //                            iemv.abortPBOC();
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
                    Log.d(MainActivity.TAG, "onCardSwiped ...1")
                    val bytes: ByteArray = ROCProviderV2.hexStr2Byte(track2)
                    Log.d(
                        MainActivity.TAG,
                        "Track2:" + track2 + " (" + ROCProviderV2.byte2HexStr(bytes) + ")"
                    )


                    var bIsKeyExist: Boolean? = VFService.getPinPadData()
                    if (!bIsKeyExist!!) {
                        Log.e(MainActivity.TAG, "no key exist type: 12, @: 1")
                    }
                    val enctypted: ByteArray? =
                        VFService.getDupkt(1, 1, 1, bytes, byteArrayOf(0, 0, 0, 0, 0, 0, 0, 0))
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
                    if (null != track3) {
                        data8583!!.put(ISO8583u.F_Track_3_Data_36, track3)
                    }
                    val validDate =
                        track.getString(ConstCheckCardListener.onCardSwiped.track.KEY_EXPIRED_DATE_String)
                    if (null != validDate) {
                        data8583!!.put(ISO8583u.F_DateOfExpired_14, validDate)
                    }
                    Log.d(MainActivity.TAG, "onCardSwiped ...3")

                    val swipeRelatedData = DataForSwipe().apply {
                        this.panNumber = pan
                        this.track1 = track1
                        this.track2 = track2
                        this.track3 = track3
                        this.serviceCode = serviceCode

                    }

                    VFEmv.setTrackDataForSwipe(swipeRelatedData) {
                        if (it)
                            VFEmv.onlineMagRequest.run()
                        else
                            Log.e("ERROR", "ERROR IN TRACK SETTING")
                    }


                    // showToast("response:" + Arrays.toString(VFEmv.isoResponse?.getField(ISO8583u.F_ResponseCode_39)))
                    /*  transType?.let {
                                  doEMV(ConstIPBOC.startEMV.intent.VALUE_cardType_smart_card,
                                      it, iemv)
                              }*/


                }

                @Throws(RemoteException::class)
                override fun onCardPowerUp() {
                    iemv.stopCheckCard()
                    iemv.abortEMV()
                    VFService.vfBeeper?.startBeep(200)
                    if (transType != null) {
                        doEMV(
                            ConstIPBOC.startEMV.intent.VALUE_cardType_smart_card,
                            transType,
                            iemv,
                            transactionalAmmount
                        )

                    }

                }

                @Throws(RemoteException::class)
                override fun onCardActivate() {
                    iemv.stopCheckCard()
                    iemv.abortEMV()
                    VFService.vfBeeper?.startBeep(200)
                    if (transType != null) {
                        doEMV(
                            ConstIPBOC.startEMV.intent.VALUE_cardType_contactless,
                            transType,
                            iemv,
                            transactionalAmmount
                        )

                    }
                }

                @Throws(RemoteException::class)
                override fun onTimeout() {
                    showToast("timeout")
                }

                @Throws(RemoteException::class)
                override fun onError(error: Int, message: String) {
                    showToast("error:$error, msg:$message")
                }
            }
            )
        } catch (e: RemoteException) {
            e.printStackTrace()
        }
    }

    fun doEMV(
        type: Int,
        transType: TransType,
        iemv: IEMV?,
        transactionalAmmount: Long
    ) {
        //
        Log.i(MainActivity.TAG, "start EMV demo")
        val emvIntent = Bundle()
        emvIntent.putInt(ConstIPBOC.startEMV.intent.KEY_cardType_int, type)
        emvIntent.putLong(ConstIPBOC.startEMV.intent.KEY_authAmount_long, transactionalAmmount)
        emvIntent.putString(
            ConstIPBOC.startEMV.intent.KEY_merchantName_String,
            terminalParameterTable?.receiptHeaderTwo
        )
        emvIntent.putString(
            ConstIPBOC.startEMV.intent.KEY_merchantId_String,
            terminalParameterTable?.merchantId
        ) // 010001020270123
        emvIntent.putString(
            ConstIPBOC.startEMV.intent.KEY_terminalId_String,
            terminalParameterTable?.terminalId
        ) // 00000001
        emvIntent.putBoolean(
            ConstIPBOC.startEMV.intent.KEY_isSupportQ_boolean,
            ConstIPBOC.startEMV.intent.VALUE_supported
        )
        //        emvIntent.putBoolean(ConstIPBOC.startEMV.intent.KEY_isSupportQ_boolean, ConstIPBOC.startEMV.intent.VALUE_unsupported);
        emvIntent.putBoolean(
            ConstIPBOC.startEMV.intent.KEY_isSupportSM_boolean,
            ConstIPBOC.startEMV.intent.VALUE_unsupported
        )
        emvIntent.putBoolean(
            ConstIPBOC.startEMV.intent.KEY_isQPBOCForceOnline_boolean,
            ConstIPBOC.startEMV.intent.VALUE_unforced
        )
        emvIntent.putBoolean("isForceOffline", false)
        if (type == ConstIPBOC.startEMV.intent.VALUE_cardType_contactless) {   // todo, check here
            emvIntent.putByte(
                ConstIPBOC.startEMV.intent.KEY_transProcessCode_byte, 0x00.toByte()
            )
        }
        emvIntent.putBoolean("isSupportPBOCFirst", false)
        val KEY_transProcessCode_byte = "transProcessCode"
        val KEY_transCurrCode_String = "transCurrCode"
        val KEY_otherAmount_String = "otherAmount"
        val KEY_authAmount_long = "authAmount"
        val KEY_isSupportQ_boolean = "isSupportQ"
        emvIntent.putString(KEY_transCurrCode_String, "0356")
        emvIntent.putString(KEY_otherAmount_String, "0")

        try {
            iemv?.startEMV(
                ConstIPBOC.startEMV.processType.full_process,
                emvIntent,
                VFEmv.emvHandler
            )
        } catch (e: RemoteException) {
            e.printStackTrace()
        }

    }

}


class DataForSwipe {
    var track1: String? = ""
    var track2: String? = ""
    var track3: String? = ""
    var panNumber: String? = ""
    var serviceCode: String? = ""
}