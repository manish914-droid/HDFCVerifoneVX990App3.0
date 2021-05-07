package com.example.verifonevx990app.emv

import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.os.RemoteException
import android.text.TextUtils
import android.util.Log
import android.util.SparseArray
import com.example.verifonevx990app.BuildConfig
import com.example.verifonevx990app.R
import com.example.verifonevx990app.customui.CustomToast
import com.example.verifonevx990app.database.*
import com.example.verifonevx990app.main.MainActivity
import com.example.verifonevx990app.main.PrefConstant
import com.example.verifonevx990app.realmtables.*
import com.example.verifonevx990app.transactions.TransactionActivity
import com.example.verifonevx990app.utils.*
import com.example.verifonevx990app.utils.PaxUtils.bcd2Str
import com.example.verifonevx990app.utils.printerUtils.EPrintCopyType
import com.example.verifonevx990app.utils.printerUtils.PrintUtil
import com.example.verifonevx990app.vxUtils.*
import com.example.verifonevx990app.vxUtils.AppPreference.GENERIC_REVERSAL_KEY
import com.example.verifonevx990app.vxUtils.AppPreference.clearReversal
import com.example.verifonevx990app.vxUtils.ConnectionTimeStamps
import com.example.verifonevx990app.vxUtils.ROCProviderV2.byte2HexStr
import com.example.verifonevx990app.vxUtils.VFService.showToast
import com.google.gson.Gson
import com.vfi.smartpos.deviceservice.aidl.*
import com.vfi.smartpos.deviceservice.constdefine.ConstIPBOC
import com.vfi.smartpos.deviceservice.constdefine.ConstOnlineResultHandler
import com.vfi.smartpos.deviceservice.constdefine.ConstPBOCHandler
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import java.nio.charset.StandardCharsets
import java.text.SimpleDateFormat
import java.util.*

//Todo  Unused Class... Check And delete it
object VFEmv : ISO8583(), OnXmlDataParsed {


    val TAG = VFEmv::class.java.simpleName
    var isNoPin: Boolean? = true


    var iemv: IEMV? = null
    var vfPinpad: IPinpad? = null
    var savedPan = ""
    var data8583_i: SparseArray<String>? = null
    var hostIP: String? = null
    var hostPort = 0
    var isoResponse: ISO8583u? = null

    /**
     * field-value map to save iso data
     */
    var data8583: SparseArray<String>? = null
    var tagOfF55: SparseArray<String>? = null
    var savedPinblock: ByteArray? = null
    var emvHandler: EMVHandler? = null
    var pinInputListener: PinInputListener? = null
    var pinKey_WorkKey = "B0BCE9315C0AA31E5E6667A037DE0AC4B0BCE9315C0AA31E"
    var macKey = ""
    var mainKey_MasterKey = "758F0CD0C866348099109BAF9EADFA6E"

    var mainKeyId = 1
    var workKeyId = 2
    var settlementReversalStatus = false
    var voidReversalStatus = false
    var settlementByteArrayValue: ByteArray? = null


    private var packageWriterModel: PackageWriterModel? = null
    private var mIsoWriter: IsoPackageWriter? = null
    private var mXmlModel: HashMap<String, XmlFieldModel>? = null
    private var transactionType: Int = 0
    private var compositeDisposable: CompositeDisposable? = CompositeDisposable()
    private var isReversal: Boolean = false
    private var context: Context? = null
    private var cardDataTable: CardDataTable? = null
    private var terminalParameterTable: TerminalParameterTable? = null
    private var terminalCommunicationTable: TerminalCommunicationTable? = null
    var issuerParameterTable: IssuerParameterTable? = null
    private var isoPackageWriterForReversal: IsoPackageWriter? = null

    private var batchFileDataTable: BatchFileDataTable? = null
    private var transactionalAmmount: Long = 0L
    private var cashBackAmount: Long = 0L

    fun initializeEMVVoid(
        vfIEMV: IEMV?,
        vfPinPad: IPinpad?,
        isoPackageWriter: IsoPackageWriter,
        XmlModel: HashMap<String, XmlFieldModel>?,
        packageWriterModel: PackageWriterModel?,
        transactionActivity: Context,
        termParamTable: TerminalParameterTable?,
        terminalCommunicationTable: TerminalCommunicationTable?,
        issuerParamTable: IssuerParameterTable?,
        mcardDataTable: CardDataTable?,
        batchFileDataTable: BatchFileDataTable?,
        transactionalAmmount: Long,
        cashBackAmount: Long,
        transactiontype: Int
    ) {
        iemv = vfIEMV
        vfPinpad = vfPinPad
        mIsoWriter = isoPackageWriter
        mXmlModel = XmlModel
        this.packageWriterModel = packageWriterModel
        context = transactionActivity
        this.cardDataTable = cardDataTable
        terminalParameterTable = termParamTable
        this.terminalCommunicationTable = terminalCommunicationTable
        cardDataTable = mcardDataTable
        issuerParameterTable = issuerParamTable
        this.batchFileDataTable = batchFileDataTable
        this.transactionalAmmount = transactionalAmmount
        this.cashBackAmount = cashBackAmount
        data8583 = SparseArray<String>()
        transactionType = transactiontype

        this.createNormalPackage(mIsoWriter, transactionalAmmount, cashBackAmount, mXmlModel)

    }


    fun initializeEMV(
        vfIEMV: IEMV?, vfPinPad: IPinpad?,
        isoPackageWriter: IsoPackageWriter,
        XmlModel: HashMap<String, XmlFieldModel>?,
        packageWriterModel: PackageWriterModel?,
        transactionActivity: TransactionActivity,
        termParamTable: TerminalParameterTable?,
        terminalCommunicationTable: TerminalCommunicationTable?,
        issuerParamTable: IssuerParameterTable?,
        mcardDataTable: CardDataTable?,
        transactionalAmmount: Long,
        cashBackAmount: Long,
        transactiontype: Int
    ) {
        iemv = vfIEMV
        vfPinpad = vfPinPad
        mIsoWriter = isoPackageWriter
        mXmlModel = XmlModel
        this.packageWriterModel = packageWriterModel
        context = transactionActivity
        this.cardDataTable = cardDataTable
        terminalParameterTable = termParamTable
        this.terminalCommunicationTable = terminalCommunicationTable
        cardDataTable = mcardDataTable
        issuerParameterTable = issuerParamTable
        transactionType = transactiontype
        this.transactionalAmmount = transactionalAmmount
        this.cashBackAmount = cashBackAmount

        data8583 = SparseArray<String>()





        emvHandler = object : EMVHandler.Stub() {
            @Throws(RemoteException::class)
            override fun onRequestAmount() {
            }

            override fun onSelectApplication(appList: MutableList<Bundle>?) {
                if (appList != null) {
                    for (aidBundle in appList) {
                        val aidName = aidBundle.getString("aidName")
                        val aid = aidBundle.getString("aid")
                        val aidLabel = aidBundle.getString("aidLabel")
                        Log.i(
                            MainActivity.TAG, "AID Name=$aidName | AID Label=$aidLabel | AID=$aid"
                        )
                    }
                }
                //showToast("onSelectApplication..." + (appList?.get(0) ?: ""))
                iemv?.importAppSelection(0)
            }

            /**
             * \brief confirm the card info
             *
             * show the card info and import the confirm result
             * \code{.java}
             * \endcode
             *
             */
            @Throws(RemoteException::class)
            override fun onConfirmCardInfo(info: Bundle) {
                Log.d(MainActivity.TAG, "onConfirmCardInfo...")
                savedPan = info.getString(ConstPBOCHandler.onConfirmCardInfo.info.KEY_PAN_String)
                    .toString()
                var result = """
                    onConfirmCardInfo callback, 
                    PAN:$savedPan
                    TRACK2:${info.getString(ConstPBOCHandler.onConfirmCardInfo.info.KEY_TRACK2_String)}
                    CARD_SN:${info.getString(ConstPBOCHandler.onConfirmCardInfo.info.KEY_CARD_SN_String)}
                    SERVICE_CODE:${info.getString(ConstPBOCHandler.onConfirmCardInfo.info.KEY_SERVICE_CODE_String)}
                    EXPIRED_DATE:${info.getString(ConstPBOCHandler.onConfirmCardInfo.info.KEY_EXPIRED_DATE_String)}
                    """.trimIndent()
                val tlv: ByteArray? = iemv?.getCardData("9F51")
                result += """9F51:${byte2HexStr(tlv)}""".trimIndent()

                //println("Saved Pan number is$savedPan")
                //      mIsoWriter?.panNumber = savedPan


                val track2 =
                    info.getString(ConstPBOCHandler.onConfirmCardInfo.info.KEY_TRACK2_String)
                var track22: String? = null
                if (null != track2) {
                    val a = track2.indexOf('D')
                    if (a > 0) {
                        track22 = track2.substring(0, a)
                    } else {
                        val a = track2.indexOf('=')

                        if (a > 0) {
                            track22 = track2.substring(0, a)
                        }
                    }
                    try {
                        mIsoWriter?.panNumber = track22!!
                    } catch (ex: Exception) {
                        ex.printStackTrace()
                    }


                    cardDataTable = track22?.let { CardDataTable.selectFromCardDataTable(it) }

                    if (null != track2) {
                        val track21 = "35|" + track2.replace("D", "=").replace("F", "")
                        //println("Track 2 data is$track21")

                        val byteArray = track21.toByteArray(StandardCharsets.ISO_8859_1)
                        val encryptedbyteArrrays: ByteArray? =
                            vfPinPad?.encryptTrackData(0, 2, byteArray)

                        //println("Track 2 with encyption is --->" + Utility.byte2HexStr(encryptedbyteArrrays))

                        mIsoWriter?.track2Data = Utility.byte2HexStr(encryptedbyteArrrays)
                        mIsoWriter?.tid = termParamTable!!.terminalId
                        mIsoWriter?.mid = termParamTable.merchantId


                        data8583?.put(ISO8583u.F_Track_2_Data_35, track21)
                    }
                    //showToast("onConfirmCardInfo:$result")
                    iemv?.importCardConfirmResult(ConstIPBOC.importCardConfirmResult.pass.allowed)
                }
            }

            /**
             * \brief show the pin pad
             *
             * \code{.java}
             * \endcode
             *
             */
            @Throws(RemoteException::class)
            override fun onRequestInputPIN(isOnlinePin: Boolean, retryTimes: Int) {
                //showToast("onRequestInputPIN isOnlinePin:$isOnlinePin")
                isNoPin = isOnlinePin
                if (isNoPin as Boolean && isOnlinePin) {
                    TransactionTypeValues.saleType =
                        TransactionTypeValues.SALETYPE.EMV_POS_ENTRY_PIN
                } else {
                    TransactionTypeValues.saleType =
                        TransactionTypeValues.SALETYPE.EMV_POS_ENTRY_OFFLINE_PIN
                }
                //Here we are inflating PinPad on App UI:-
                //    VFService.openPinPad(isoPackageWriter,isOnlinePin)

            }

            @Throws(RemoteException::class)
            override fun onConfirmCertInfo(certType: String, certInfo: String) {
                //showToast("onConfirmCertInfo, type:$certType,info:$certInfo")
                iemv?.importCertConfirmResult(ConstIPBOC.importCertConfirmResult.option.CONFIRM)
            }

            @Throws(RemoteException::class)
            override fun onRequestOnlineProcess(aaResult: Bundle) {
                Log.d(MainActivity.TAG, "onRequestOnlineProcess...")
                if (isNoPin as Boolean) {
                    TransactionTypeValues.saleType =
                        TransactionTypeValues.SALETYPE.EMV_POS_ENTRY_NO_PIN
                }

                val result =
                    aaResult.getInt(ConstPBOCHandler.onRequestOnlineProcess.aaResult.KEY_RESULT_int)
                val signature =
                    aaResult.getBoolean(ConstPBOCHandler.onRequestOnlineProcess.aaResult.KEY_SIGNATURE_boolean)
                //showToast("onRequestOnlineProcess result=$result signal=$signature")
                when (result) {
                    ConstPBOCHandler.onRequestOnlineProcess.aaResult.VALUE_RESULT_AARESULT_ARQC, ConstPBOCHandler.onRequestOnlineProcess.aaResult.VALUE_RESULT_QPBOC_ARQC -> aaResult.getString(
                        ConstPBOCHandler.onRequestOnlineProcess.aaResult.KEY_ARQC_DATA_String
                    )
                    /*?.let {
                        showToast(
                            it
                        )
                    }*/
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
                    0x9F03,
                    0x9F33,
                    0x9F74,
                    0x9F24
                )
                var count = 0
                for (tag in tagList) {
                    tlv = iemv?.getCardData(Integer.toHexString(tag).toUpperCase(Locale.ROOT))
                    if (null != tlv && tlv.size > 0) {
                        Log.d(MainActivity.TAG, Utility.byte2HexStr(tlv))
                        val length = Integer.toHexString(tlv.size)
                        count = count + Integer.valueOf(length)
                        tagOfF55!!.put(tag, Utility.byte2HexStr(tlv)) // build up the field 55
                    } else {
                        Log.e(
                            MainActivity.TAG,
                            "getCardData:" + Integer.toHexString(tag) + ", fails"
                        )
                    }
                }
                //println("Total length of tag55 is$count")

                // set the pin block
                //            data8583?.put(ISO8583u.F_PINData_52, byte2HexStr(savedPinblock))
                Log.d(MainActivity.TAG, "start online request")
                onlineRequest.run()
                Log.d(MainActivity.TAG, "online request finished")
                // import the online result


            }

            @Throws(RemoteException::class)
            override fun onTransactionResult(result: Int, data: Bundle) {
                Log.d(MainActivity.TAG, "onTransactionResult")
                Log.d("FallbackCode:- ", result.toString())
                val msg = data.getString("ERROR")
                //showToast("onTransactionResult result = $result,msg = $msg")
                onlineRequest.run()
                when (result) {
                    ConstPBOCHandler.onTransactionResult.result.EMV_CARD_BIN_CHECK_FAIL -> {
                        // read card fail
                        //showToast("read card fail")
                        return
                    }
                    ConstPBOCHandler.onTransactionResult.result.EMV_MULTI_CARD_ERROR -> {
                        // multi-cards found
                        data.getString(ConstPBOCHandler.onTransactionResult.data.KEY_ERROR_String)
                        /* ?.let {
                             showToast(
                                 it
                             )
                         }*/
                        return
                    }
                }
            }
        }
    }

    fun initializePinInputListener() {
        pinInputListener = object : PinInputListener.Stub() {
            @Throws(RemoteException::class)
            override fun onInput(len: Int, key: Int) {
                Log.d(MainActivity.TAG, "PinPad onInput, len:$len, key:$key")
            }

            @Throws(RemoteException::class)
            override fun onConfirm(data: ByteArray, isNonePin: Boolean) {
                Log.d(MainActivity.TAG, "PinPad onConfirm")
                iemv?.importPin(1, data)
                savedPinblock = data
            }

            @Throws(RemoteException::class)
            override fun onCancel() {
                Log.d(MainActivity.TAG, "PinPad onCancel")
            }

            @Throws(RemoteException::class)
            override fun onError(errorCode: Int) {
                Log.d(MainActivity.TAG, "PinPad onError, code:$errorCode")
            }
        }
    }

    fun setTrackDataForSwipe(data: DataForSwipe, success: (Boolean) -> Unit) {
        Log.d(MainActivity.TAG, "Setting track data...")
        savedPan = data.panNumber.toString()


        //println("Saved Pan number is$savedPan")
        //      mIsoWriter?.panNumber = savedPan


        val track2 = data.track2
        var track22: String? = null
        if (null != track2) {
            val a = track2.indexOf('D')
            if (a > 0) {
                track22 = track2.substring(0, a)
            }

            mIsoWriter?.panNumber = data.panNumber.toString()

            cardDataTable = CardDataTable.selectFromCardDataTable(track2)

            if (null != track2) {
                val track21 = "35|" + track2.replace("D", "=").replace("F", "")
                //println("Track 2 data is$track21")

                val byteArray = track21.toByteArray(StandardCharsets.ISO_8859_1)
                val encryptedbyteArrrays: ByteArray? =
                    vfPinpad?.encryptTrackData(0, 2, byteArray)

                /*println(
                    "Track 2 with encyption is --->" + Utility.byte2HexStr(
                        encryptedbyteArrrays
                    )
                )*/

                mIsoWriter?.track2Data = Utility.byte2HexStr(encryptedbyteArrrays)
                mIsoWriter?.tid = terminalParameterTable?.terminalId.toString()
                mIsoWriter?.mid = terminalParameterTable?.merchantId

                success(true)
            }
        }
    }

    var onlineRequest = Runnable {
        val iso8583u = ISO8583u()
        if (tagOfF55 != null) {
            for (i in 0 until tagOfF55!!.size()) {
                val tag = tagOfF55!!.keyAt(i)
                val value = tagOfF55!!.valueAt(i)

                val indexedValue: Boolean = tag == 24372
                if (indexedValue) {
                    val applicationPanSequenceNumber = tagOfF55!!.valueAt(i)
                    if (applicationPanSequenceNumber != null) {
                        mIsoWriter?.applicationPanSequenceNumber = "" + applicationPanSequenceNumber
                    }
                }

                if (value.isNotEmpty()) {
                    val tmp: ByteArray? = iso8583u.appendF55(tag, value)
                    if (tmp == null) {
                        Log.e(
                            MainActivity.TAG,
                            "error of tag:" + Integer.toHexString(tag) + ", value:" + value
                        )
                    } else {
                        Log.d(
                            MainActivity.TAG,
                            "append F55 tag:" + Integer.toHexString(tag) + ", value:" + byte2HexStr(
                                tmp
                            )
                        )
                    }
                }
            }
            tagOfF55 = null
        }
        /*   val packet: ByteArray? =
               data8583?.let { iso8583u.makePacket(it, ISO8583.PACKET_TYPE.PACKET_TYPE_HEXLEN_BUF) }
           val field55: String? = fieldhasmap.get(55)
           mIsoWriter?.filed55Data = field55!!*/

        val f55 = getField55()
        mIsoWriter?.filed55Data = f55
        //   println("field55 data is ----> $f55")
        this.createNormalPackage(mIsoWriter, transactionalAmmount, cashBackAmount, mXmlModel)

    }

    var onlineMagRequest = Runnable {
        val iso8583u = ISO8583u()
        if (tagOfF55 != null) {
            for (i in 0 until tagOfF55!!.size()) {
                val tag = tagOfF55!!.keyAt(i)
                val value = tagOfF55!!.valueAt(i)

                val indexedValue: Boolean = tag == 24372
                if (indexedValue) {
                    val applicationPanSequenceNumber = tagOfF55!!.valueAt(i)
                    if (applicationPanSequenceNumber != null) {
                        mIsoWriter?.applicationPanSequenceNumber = "" + applicationPanSequenceNumber
                    }
                }

                if (value.isNotEmpty()) {
                    val tmp: ByteArray? = iso8583u.appendF55(tag, value)
                    if (tmp == null) {
                        Log.e(
                            MainActivity.TAG,
                            "error of tag:" + Integer.toHexString(tag) + ", value:" + value
                        )
                    } else {
                        Log.d(
                            MainActivity.TAG,
                            "append F55 tag:" + Integer.toHexString(tag) + ", value:" + byte2HexStr(
                                tmp
                            )
                        )
                    }
                }
            }
            tagOfF55 = null
        }

        mIsoWriter?.applicationPanSequenceNumber = "00"
        this.createNormalPackage(mIsoWriter, transactionalAmmount, cashBackAmount, mXmlModel)

    }


    val mField55 = intArrayOf(
        0x9F26,
        0x9F10,
        0x9F37,
        0x9F36,
        0x95,
        0x9A,
        0x9C,
        0x9F02,
        0x5F2A,
        0x9F1A,
        0x82,
        0x5F34,
        0x9F27,
        0x9F33,
        0x9F34,
        0x9F35,
        0x9F03,
        0x9F47
    )

    fun getField55(isAmex: Boolean = true): String {
        val sb = StringBuilder()
        for (f in mField55) {
            f
            val v = iemv?.getCardData(Integer.toHexString(f).toUpperCase(Locale.ROOT))!!
            if (v != null) {
                sb.append(Integer.toHexString(f))
                var l = Integer.toHexString(v.size)
                if (l.length < 2) {
                    l = "0$l"
                }
                if (f == 0x9F10 && isAmex) {
                    val c = l + bcd2Str(v)
                    var le = Integer.toHexString(c.length / 2)
                    if (le.length < 2) {
                        le = "0$le"
                    }
                    sb.append(le)
                    sb.append(c)
                } else {
                    sb.append(l)
                    sb.append(bcd2Str(v))
                }
            }// end of if null value check
            else if (f == 0x9F03) {
                sb.append(Integer.toHexString(f))
                sb.append("06")
                sb.append("000000000000")
            }
        }// end of for loop
        return sb.toString().toUpperCase(Locale.ROOT)
    }

    private fun createNormalPackage(
        isoPackageWriter: IsoPackageWriter?,
        transactionalAmmount: Long,
        cashBackAmount: Long,
        xmlFieldModels: HashMap<String, XmlFieldModel>?
    ) {
        //AppPreference.getNewInstance().saveString(REVERSAL_DATA, null, context)
        isReversal = false
        val data: IsoPackageWriter? = when (transactionType) {
            TransactionTypeValues.VOID -> createVoidPackage(isoPackageWriter)
            else -> createPackage(isoPackageWriter, transactionalAmmount, cashBackAmount)
        }

        if (data != null) {
            mIsoWriter = data
            if (!TextUtils.isEmpty(AppPreference.getString(GENERIC_REVERSAL_KEY))) {
                this.packageWriterModel = checkReversal()
                isoPackageWriterForReversal = IsoPackageWriter(this.context, this)
            } else {
                sendingToServer(data, xmlFieldModels)
            }
        }
    }

    private fun sendingToServer(
        isoPackageWriter: IsoPackageWriter,
        xmlFieldModels: HashMap<String, XmlFieldModel>?
    ) {
        val disposable =
            isoPackageWriter.observerToCreatePackage(transactionType, xmlFieldModels!!).subscribeOn(
                Schedulers.io()
            )
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({ stringBuilder ->
                    callToServer(stringBuilder.toString())
                }) { throwable ->
                    throwable.printStackTrace()
                }
        compositeDisposable!!.add(disposable)
    }

    private fun callToServer(packageData: String) {
        CustomToast.printAppLog(" observerToCreatePackage $packageData")
        val dd = PackageSender(
            VerifoneApp.appContext,
            packageData,
            object : ServerResponce, ResponseMessage {
                override fun onSucees(responce: ByteArray?) {
                    CustomToast.printAppLog(HexStringConverter.hexDump(responce!!))

                    try {
                        val isoPackageReader = ISOPackageReader(this)
                        isoPackageReader.readResponseData(responce, transactionType)
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }

                override fun onResponseTimeOut() {

                }

                override fun saveReversal() {
                    /* if (AppPreference.getString(ABATCH_KEY) == "0")
                         AppPreference.saveString(ABATCH_KEY, "1")*/

                    if (mIsoWriter!!.transactionType != TransactionTypeValues.SETTLEMENT &&
                        mIsoWriter!!.transactionType != TransactionTypeValues.VOID && !isReversal
                    ) {
                        saveReversalFile(null, null)
                    }
                }

                override fun onNoInternet() {

                }

                override fun onConnectionFailed() {

                }

                override fun onError(throwable: String?) {

                }

                override fun onSuccessMessage(data: ISOPackageReader?) {

                    GlobalScope.launch {

                        val d = async {
                            //region setting f48 identifier to Connection Time stamp object
                            val d = (data?.field48Str ?: "").split("~")
                            if (d.isNotEmpty()) {
                                ConnectionTimeStamps.identifier = d[0]
                                ConnectionTimeStamps.saveToTerminalParamTable()
                            }

                            //endregion
                        }
                        if (transactionType == TransactionTypeValues.SETTLEMENT) {
                            AppPreference.setIntData(AppPreference.BLOCK_COUNTER, 0)
                        }

                        /*
                        setPritingData(isoPackageWriter: IsoPackageWriter?, isoPackageReader: ISOPackageReader,
                       terminalParameterTable: TerminalParameterTable?, issuerParameterTable: IssuerParameterTable?,
                       t: CardDataTable?, isPinVerify: Boolean): BatchFileDataTable
                         */

                        if (!TextUtils.isEmpty(AppPreference.getString(GENERIC_REVERSAL_KEY)) && isReversal) {
                            isReversal = false
                            if (data?.resCode == "00") {
                                clearReversal()
                                //Below If Condition is for Settlement after Reversal Success:-
                                if (settlementReversalStatus)
                                    (context as MainActivity).settleBatch(settlementByteArrayValue)
                                else
                                //Below else Condition is for Transaction Sale after Reversal Success:-
                                    sendingToServer(mIsoWriter!!, mXmlModel)

                            } else {
                                val intent = Intent(context, MainActivity::class.java)
                                intent.flags =
                                    Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_CLEAR_TASK
                                context?.startActivity(intent)
                            }

                        } else {
                            clearReversal()
                            if (data?.resCode != null && data.resCode == "00" && !isReversal) {
                                //   AppPreference.saveString(REVERSAL_DATA, "", this@Track2Encription.context) // Do not clear reversal here, clear reversal after printing
                                if (terminalParameterTable != null && terminalParameterTable!!.invoiceNumber.isNotEmpty()) {
                                    val invoice =
                                        Integer.parseInt(terminalParameterTable!!.invoiceNumber) + 1
                                    terminalParameterTable!!.invoiceNumber =
                                        HexStringConverter.addPreFixer(invoice.toString(), 6)
                                    d.await()
                                    saveTerminalData()
                                }

                                // import the online result


                                //  onlineResult.putString(ConstIPBOC.inputOnlineResult.onlineResult.KEY_field55_String, Utility.byte2HexStr(data!!.field55))


                                //println("field 55 in response is ---> " + Utility.byte2HexStr(data.field55))

                                var string = hexString2String(Utility.byte2HexStr(data.field55))

                                val a = if (data.reservedIso != null) data.reservedIso else ""
                                val f55Hash = HashMap<Int, String>()
                                tlvParser(a, f55Hash)
                                var ta = 0x91
                                var ba: ByteArray? = null
                                val tagDatatag91 = f55Hash[ta] ?: ""
                                try {
                                    if (tagDatatag91.isNotEmpty()) {
                                        ba = tagDatatag91.hexStr2ByteArr()
                                        val mba = ArrayList<Byte>()
                                        mba.addAll(ba.asList())
                                        //println("91 data is --->" + Utility.byte2HexStr(ba))
                                        /*      val str = "00"
                                              val byteArr = str.toByteArray()
                                              var hexvalue = Utility.byte2HexStr(byteArr)
                                              println("3030 hex value is --->"+hexvalue)
                                              println("3030 hex to string is --->"+ hexString2String(hexvalue))
                                              println("91 and 3030 data is --->"+ Utility.byte2HexStr(ba) + hexvalue)
                                              onlineResult.putString(ConstIPBOC.inputOnlineResult.onlineResult.KEY_field55_String,"8A0230303"+"910A"+Utility.byte2HexStr(ba)+"3030")
                                              println("91 value ---> "+"8A0230303"+"910A"+Utility.byte2HexStr(ba)+"3030")
                                              onlineResult.putString(ConstIPBOC.inputOnlineResult.onlineResult.KEY_field55_String, "9108"+ Utility.byte2HexStr(ba))
                                              println("8A nad 91 value is --->"+  "9108"+ Utility.byte2HexStr(ba))*/
                                    }
                                } catch (ex: Exception) {
                                    ex.printStackTrace()
                                }

                                val onlineResult = Bundle()
                                onlineResult.putBoolean(
                                    ConstIPBOC.inputOnlineResult.onlineResult.KEY_isOnline_boolean,
                                    true
                                )
                                onlineResult.putString(
                                    ConstIPBOC.inputOnlineResult.onlineResult.KEY_respCode_String,
                                    "00"
                                ) //3030
                                onlineResult.putString(
                                    ConstIPBOC.inputOnlineResult.onlineResult.KEY_authCode_String,
                                    "00" /*data!!.autthCode*/
                                )
                                onlineResult.putString(
                                    ConstIPBOC.inputOnlineResult.onlineResult.KEY_field55_String,
                                    "9A08" + Utility.byte2HexStr(ba)
                                )

                                //println("91 value is ---> " + "9108" + Utility.byte2HexStr(ba))

                                iemv?.inputOnlineResult(
                                    onlineResult,
                                    object : OnlineResultHandler.Stub() {
                                        @Throws(RemoteException::class)
                                        override fun onProccessResult(result: Int, data: Bundle) {
                                            Log.i(MainActivity.TAG, "onProccessResult callback:")
                                            val str = """
                                                  RESULT:$result
                                                        TC_DATA:
                                             """.trimIndent() + data.getString(
                                                ConstOnlineResultHandler.onProccessResult.data.KEY_TC_DATA_String,
                                                "not defined"
                                            ) +
                                                    "\nSCRIPT_DATA:" + data.getString(
                                                ConstOnlineResultHandler.onProccessResult.data.KEY_SCRIPT_DATA_String,
                                                "not defined"
                                            ) +
                                                    "\nREVERSAL_DATA:" + data.getString(
                                                ConstOnlineResultHandler.onProccessResult.data.KEY_REVERSAL_DATA_String,
                                                "not defined"
                                            )

                                            //println("TC and Reversal data is$str")

                                            if (result == 0) {

                                                val strlist = arrayOf("0x9F06")
                                                val strs = iemv!!.getAppTLVList(strlist)
                                                //println("Aid Data is ----> $strs")

                                                val strlist1 = arrayOf("0x95")
                                                val strs1 = iemv!!.getAppTLVList(strlist1)
                                                //println("TVR Data is ----> $strs1")


                                                val strlist2 = arrayOf("0x9B")
                                                val strs2 = iemv!!.getAppTLVList(strlist2)
                                                //println("TSI Data is ----> $strs2")

                                            }


                                            when (result) {
                                                ConstOnlineResultHandler.onProccessResult.result.TC -> {
                                                    //showToast("TC")
                                                }

                                                ConstOnlineResultHandler.onProccessResult.result.Online_AAC -> {
                                                    //showToast("Online_AAC")
                                                }


                                                else -> {
                                                    //showToast("error, code:$result")
                                                }
                                            }
                                        }
                                    })


                                //After Success of Sale Transaction Here we are giving command to print transaction receipt and save data to BatchFileData Table:-
                                printAndSaveBatchFileData(
                                    mIsoWriter, data, terminalParameterTable,
                                    issuerParameterTable, cardDataTable, true, context
                                )


                                GlobalScope.launch(Dispatchers.Main) {
                                    showToast("Sale Transaction Success!!!!")
                                }

                            }
                        }
                        d.await()
                    }
                }

                override fun onResponseError(data: ISOPackageReader?) {
                    //
                }

            },
            transactionType
        )

        dd.sendServer()
    }

    private fun saveReversalFile(isSecondgen: String?, field39: String?) {
        /*  if (isSecondgen != null) {
              if (terminalParameterTable != null && !terminalParameterTable!!.invoiceNumber.isEmpty() && terminalParameterTable!!.invoiceNumber.toInt() > 1) {
                  val invoice = Integer.parseInt(terminalParameterTable!!.invoiceNumber) - 1
                  terminalParameterTable!!.invoiceNumber = HexStringConverter.addPreFixer(invoice.toString(), 6)
                  saveTerminalData()
              }
              packageWriterModel!!.field39 = field39
              packageWriterModel!!.field55 = isSecondgen
          }*/
        if (packageWriterModel != null) {
            val gson = Gson()
            val reversalData = gson.toJson(packageWriterModel)
            CustomToast.printAppLog(" RES $reversalData")
            AppPreference.saveString(GENERIC_REVERSAL_KEY, reversalData)
        }
    }

    fun printAndSaveBatchFileData(
        isoPackageWriter: IsoPackageWriter?, isoPackageReader: ISOPackageReader,
        terminalParameterTable: TerminalParameterTable?,
        issuerParameterTable: IssuerParameterTable?, t: CardDataTable?,
        isPinVerify: Boolean, context: Context?
    ): BatchFileDataTable {

        //Auto Increment Invoice Number in BatchFileData Table:-
        var invoiceIncrementValue = 0
        if (AppPreference.getIntData(PrefConstant.SALE_INVOICE_INCREMENT.keyName.toString()) == 0) {
            invoiceIncrementValue = isoPackageWriter?.invoiceNumber?.toInt()!!
            AppPreference.setIntData(
                PrefConstant.SALE_INVOICE_INCREMENT.keyName.toString(),
                invoiceIncrementValue
            )
        } else {
            invoiceIncrementValue =
                AppPreference.getIntData(PrefConstant.SALE_INVOICE_INCREMENT.keyName.toString()) + 1
            AppPreference.setIntData(
                PrefConstant.SALE_INVOICE_INCREMENT.keyName.toString(),
                invoiceIncrementValue
            )
        }

        //Below we are saving all data in BatchFileDataTable and saving in DB:-
        val printerReceiptData = BatchFileDataTable()
        printerReceiptData.serialNumber = isoPackageWriter?.serialNumber ?: ""
        printerReceiptData.sourceNII = isoPackageWriter?.sourceNII ?: ""
        printerReceiptData.destinationNII = isoPackageWriter?.destinationNII ?: ""
        printerReceiptData.mti = isoPackageWriter?.mti ?: ""
        printerReceiptData.transactionType = isoPackageWriter?.transactionType ?: 0
        printerReceiptData.transactionalAmmount = isoPackageWriter?.transactionalAmmount ?: ""
        printerReceiptData.nii = isoPackageWriter?.nii ?: ""
        printerReceiptData.applicationPanSequenceNumber =
            isoPackageWriter?.applicationPanSequenceNumber ?: ""
        printerReceiptData.merchantName = terminalParameterTable?.receiptHeaderOne!!
        printerReceiptData.panMask = terminalParameterTable.panMask
        printerReceiptData.panMaskConfig = terminalParameterTable.panMaskConfig
        printerReceiptData.panMaskFormate = terminalParameterTable.panMaskFormate
        printerReceiptData.merchantAddress1 = terminalParameterTable.receiptHeaderTwo
        printerReceiptData.merchantAddress2 = terminalParameterTable.receiptHeaderThree
        printerReceiptData.timeStamp = isoPackageWriter?.timeStamp ?: 0L
        printerReceiptData.transactionDate = dateFormater(isoPackageWriter?.timeStamp ?: 0L)
        printerReceiptData.transactionTime = timeFormater(isoPackageWriter?.time?.toLong() ?: 0L)
        printerReceiptData.time = isoPackageWriter?.time ?: ""
        printerReceiptData.date = isoPackageWriter?.date ?: ""
        printerReceiptData.mid = isoPackageWriter?.mid ?: ""

        printerReceiptData.posEntryValue = isoPackageWriter?.posEntryValue ?: ""

        printerReceiptData.batchNumber = isoPackageWriter?.batchNumber ?: ""
        printerReceiptData.roc = isoPackageWriter?.roc ?: ""
        printerReceiptData.invoiceNumber = invoiceIncrementValue.toString()

        printerReceiptData.track2Data =
            if (isoPackageWriter!!.transactionType != TransactionTypeValues.PRE_AUTH_COMPLETE) {
                isoPackageWriter.track2Data
            } else {
                isoPackageReader.field57
            }

        printerReceiptData.terminalSerialNumber = isoPackageWriter.terminalSerialNumber
        printerReceiptData.bankCode = isoPackageWriter.bankCode
        printerReceiptData.customerId = isoPackageWriter.customerId
        printerReceiptData.walletIssuerId = isoPackageWriter.walletIssuerId
        printerReceiptData.connectionType = isoPackageWriter.connectionType
        printerReceiptData.modelName = isoPackageWriter.modelName
        printerReceiptData.appName = isoPackageWriter.appName
        printerReceiptData.appVersion = isoPackageWriter.appVersion
        printerReceiptData.pcNumber = isoPackageWriter.pcNumber
        printerReceiptData.operationType = isoPackageWriter.operationType
        printerReceiptData.transationName =
            TransactionTypeValues.getTransactionStringType(isoPackageWriter.transactionType)
        printerReceiptData.cardType = t?.cardLabel ?: ""
        printerReceiptData.isPinverified = isPinVerify /*isoPackageWriter.isPinVerified()*/

        printerReceiptData.cardNumber =
            if (isoPackageWriter.transactionType != TransactionTypeValues.PRE_AUTH_COMPLETE) {
                isoPackageWriter.panNumber
            } else {
                printerReceiptData.track2Data
            }

        printerReceiptData.expiry = isoPackageWriter.expiryDate
        printerReceiptData.cardHolderName = isoPackageWriter.cardHolderName
        printerReceiptData.indicator = isoPackageWriter.indicator
        printerReceiptData.field55Data = isoPackageWriter.filed55Data

        printerReceiptData.baseAmmount =
            MoneyUtil.fen2yuan(isoPackageWriter.transactionalAmmount?.toLong() ?: 0L).toString()

        if (isoPackageWriter.cashBackAmount.isNotEmpty() && isoPackageWriter.cashBackAmount.isNotEmpty() && isoPackageWriter.cashBackAmount != "0") {
            printerReceiptData.cashBackAmount =
                MoneyUtil.fen2yuan(isoPackageWriter.cashBackAmount.toLong()).toString()
            if (isoPackageWriter.transactionType != TransactionTypeValues.CASH_AT_POS)
                printerReceiptData.totalAmmount = MoneyUtil.fen2yuan(
                    isoPackageWriter.transactionalAmmount?.toLong()
                        ?: 0L + isoPackageWriter.cashBackAmount.toLong()
                ).toString()
            else
                printerReceiptData.totalAmmount =
                    MoneyUtil.fen2yuan(isoPackageWriter.transactionalAmmount?.toLong() ?: 0L)
                        .toString()
        } else
            printerReceiptData.totalAmmount =
                MoneyUtil.fen2yuan(isoPackageWriter.transactionalAmmount?.toLong() ?: 0L).toString()

        printerReceiptData.referenceNumber = isoPackageReader.retrievalReferenceNumber
        printerReceiptData.authCode = isoPackageReader.autthCode ?: ""
        printerReceiptData.responseCode = isoPackageReader.reasionCode ?: ""

        printerReceiptData.invoiceNumber = invoiceIncrementValue.toString()
        printerReceiptData.terminalSerialNumber = isoPackageWriter.terminalSerialNumber
        printerReceiptData.tid = isoPackageWriter.tid
        printerReceiptData.discaimerMessage = issuerParameterTable!!.volletIssuerDisclammer
        printerReceiptData.isTimeOut = false

        //
        printerReceiptData.f48IdentifierWithTS = ConnectionTimeStamps.getFormattedStamp()
        //
        if (isoPackageWriter.transactionType != TransactionTypeValues.PRE_AUTH) {  // printerReceiptData will not be saved in Batch if transaction is pre auth
            //Here we are saving printerReceiptData in BatchFileData Table:-
            saveTableInDB(printerReceiptData)
            PrintUtil(context).startPrinting(
                printerReceiptData,
                EPrintCopyType.MERCHANT,
                context
            ) { printCB, printingFail ->

            }
        }

        return printerReceiptData

    }

    private fun createPackage(
        isoPackageWriter: IsoPackageWriter?,
        transactionalAmmount: Long,
        cashBackAmount: Long
    ): IsoPackageWriter? {
        try {
            packageWriterModel = PackageWriterModel()
            isoPackageWriter!!.transactionType = 4
            packageWriterModel!!.transactionType = isoPackageWriter.transactionType
            //tpdu
            isoPackageWriter.setSerialNumber(60)
            packageWriterModel!!.serialNumber = isoPackageWriter.serialNumber
            isoPackageWriter.setSourceNII(91)
            packageWriterModel!!.sourceNII = isoPackageWriter.sourceNII
            isoPackageWriter.setDestinationNII(1)
            packageWriterModel!!.destinationNII = isoPackageWriter.destinationNII

            when (transactionType) {
                TransactionTypeValues.PRE_AUTH -> isoPackageWriter.mti =
                    TransactionTypeValues.PRE_AUTH_MTI
                TransactionTypeValues.PRE_AUTH_COMPLETE -> isoPackageWriter.mti =
                    TransactionTypeValues.AUTH_COMPLETE_MTI
                else -> isoPackageWriter.mti =
                    TransactionTypeValues.DEFAULT_MTI // For sale , cashback, refund
            }
            packageWriterModel!!.mti = isoPackageWriter.mti
            //end of tpdu//
            //processing code
            isoPackageWriter.processingCode =
                TransactionTypeValues.getProcCode(transactionType)//"920001"
            packageWriterModel!!.processingCode = isoPackageWriter.processingCode
            isoPackageWriter.setFieldValues("3", isoPackageWriter.getProccingCode()!!)//3 byte

            //transation ammount
            isoPackageWriter.transactionalAmmount =
                (transactionalAmmount + cashBackAmount).toString()
            packageWriterModel!!.transactionalAmmount = isoPackageWriter.transactionalAmmount
            isoPackageWriter.setFieldValues(
                "4",
                isoPackageWriter.transactionalAmmount ?: ""
            )//6 byte

            //if(terminalParameterTable!!.stan==null)
            if (terminalParameterTable != null && !terminalParameterTable?.stan.isNullOrEmpty() && !terminalParameterTable?.stan.isNullOrEmpty()) {
                val stan = Integer.parseInt(terminalParameterTable!!.stan) + 1
                terminalParameterTable!!.stan = HexStringConverter.addPreFixer(stan.toString(), 6)
                saveTerminalData()
            }

            ROCProvider.increment()
            val rocincement = ROCProvider.getRoc()

            isoPackageWriter.roc = HexStringConverter.addPreFixer(
                rocincement.toString(),
                6
            )///HexStringConverter.addPreFixer(AppPreference.getNewInstance().getStanNumber(AppPreference.STAN_KEY, context).toString(), 6)//
            CustomToast.printAppLog(" ROC " + isoPackageWriter.roc)
            packageWriterModel!!.roc = isoPackageWriter.roc
            isoPackageWriter.setFieldValues(
                "11",
                isoPackageWriter.roc ?: ""
            )////"" + AppPreference.getNewInstance().getStanNumber(STAN_KEY, context)

            try {

                val date: Long = Calendar.getInstance().timeInMillis

                //if (ret == 0) {
                val timeFormater = SimpleDateFormat("HHmmss", Locale.getDefault())
                isoPackageWriter.time = timeFormater.format(date)
                isoPackageWriter.setFieldValues("12", isoPackageWriter.time ?: "")
                val dateFormater = SimpleDateFormat("MMdd", Locale.getDefault())
                //date
                isoPackageWriter.date = dateFormater.format(date)
                isoPackageWriter.timeStamp = date
                packageWriterModel!!.time = isoPackageWriter.time
                packageWriterModel!!.date = isoPackageWriter.date
                packageWriterModel!!.timeStamp = isoPackageWriter.timeStamp
                isoPackageWriter.setFieldValues("13", isoPackageWriter.date ?: "")

                //552 request without pin
                //553 request with online pin
                //554 request with offline pin

                if (transactionType != TransactionTypeValues.PRE_AUTH_COMPLETE) {
                    isoPackageWriter.posEntryValue =
                        "523" //(mag without pin)    //"552"(EMV without pin)
                    /*TransactionTypeValues.getPosValue().toString()*/  //911 in case of Contacless without pin // 523 //(mag without pin)
                    packageWriterModel!!.posEntryValue = isoPackageWriter.posEntryValue
                    isoPackageWriter.setFieldValues("22", isoPackageWriter.posEntryValue ?: "")
                }

                if (isoPackageWriter.applicationPanSequenceNumber!!.isNotEmpty()) {
                    isoPackageWriter.setFieldValues(
                        "23",
                        isoPackageWriter.applicationPanSequenceNumber ?: ""
                    )
                    packageWriterModel!!.applicationPanSequenceNumber =
                        isoPackageWriter.applicationPanSequenceNumber
                }
                //nii
                isoPackageWriter.nii =
                    HexStringConverter.addPreFixer(terminalCommunicationTable!!.nii, 4)//"0091"
                packageWriterModel!!.nii = isoPackageWriter.nii
                isoPackageWriter.setFieldValues("24", isoPackageWriter.nii ?: "")
//tid
                isoPackageWriter.tid = HexStringConverter.addPreFixer(
                    terminalParameterTable!!.terminalId,
                    8
                )//"10600003"//
                isoPackageWriter.setFieldValues("41", isoPackageWriter.tid)//20200002
                packageWriterModel!!.tid = isoPackageWriter.tid
                //50071135221
                isoPackageWriter.mid = HexStringConverter.addPreFixer(
                    terminalParameterTable!!.merchantId,
                    15
                )//"000000010610002"//630000000000010
                packageWriterModel!!.mid = isoPackageWriter.mid
                isoPackageWriter.setFieldValues("42", isoPackageWriter.mid ?: "")

                val f48 = ConnectionTimeStamps.getStamp()

                isoPackageWriter.field48 = f48
                isoPackageWriter.setFieldValues("48", isoPackageWriter.field48)
                packageWriterModel!!.feld48 = isoPackageWriter.field48

                if (isoPackageWriter.genratedPinBlock.isNotEmpty()) {

                    isoPackageWriter.setFieldValues("52", isoPackageWriter.genratedPinBlock)
                }

                if (cashBackAmount != 0L)
                    isoPackageWriter.cashBackAmount = cashBackAmount.toString()
                ///used in cash at pos
                if (isoPackageWriter.transactionType == TransactionTypeValues.CASH_AT_POS) {
                    isoPackageWriter.cashBackAmount = isoPackageWriter.transactionalAmmount ?: ""
                    packageWriterModel!!.cashBackAmount = isoPackageWriter.cashBackAmount
                    isoPackageWriter.setFieldValues("54", isoPackageWriter.cashBackAmount)
                } else if (isoPackageWriter.transactionType == TransactionTypeValues.SALE_WITH_CASH) {
                    packageWriterModel!!.cashBackAmount = isoPackageWriter.cashBackAmount
                    isoPackageWriter.setFieldValues("54", isoPackageWriter.cashBackAmount)
                }
                if (isoPackageWriter.filed55Data.isNotEmpty()) {
                    isoPackageWriter.setFieldValues(
                        "55",
                        isoPackageWriter.filed55Data
                    )//"9f26082865ffb05b57e5209f10080105a000030400009f37042cd04e9d9f36020160950502000480009a031804129c01009f02060000000245005f2a0203569f1a020356820258005f3401019f2701809f3303e0f0c89f34034203009f3501229f0306000000000000"// genratedPinBlock!!.toString(StandardCharsets.ISO_8859_1))

                }

                if (isoPackageWriter.transactionType == TransactionTypeValues.VOID || isoPackageWriter.transactionType == TransactionTypeValues.REVERSAL
                    || isoPackageWriter.transactionType == TransactionTypeValues.SALE_COMPLETION || isoPackageWriter.transactionType == TransactionTypeValues.SALE_WITH_TIP
                    || isoPackageWriter.transactionType == TransactionTypeValues.TIP_ADJUSTMENT
                ) {
                    //old stan+date time in yymmddHHmmss
                    //isoPackageWriter.setFieldValues("56", )//need to set here
                }


                //region=====Setting for field 56 for pre auth completion
                if (transactionType == TransactionTypeValues.PRE_AUTH_COMPLETE) {
                    isoPackageWriter.setFieldValues("56", isoPackageWriter.field56)
                    packageWriterModel?.field56 = isoPackageWriter.field56
                }
                //endregion

                if (isoPackageWriter.track2Data.isNotEmpty()) {
                    isoPackageWriter.cardHolderName = "AMEX"
                    isoPackageWriter.setFieldValues(
                        "57",
                        isoPackageWriter.track2Data
                    )//HexStringConverter.hexToString("ed81f715e4cbd8ec1746301713d165c71f8693208469468b1ecc3e752e87154c93c8f550ec516a56")
                    packageWriterModel!!.track2Data = isoPackageWriter.track2Data
                }
//                    isoPackageWriter.operationType = "MAG STRIPE"
                packageWriterModel!!.operationType = isoPackageWriter.operationType

                if (transactionType != TransactionTypeValues.PRE_AUTH_COMPLETE) {  // field 58 should be ignored in case of pre auth complete
                    isoPackageWriter.cardIndFirst = "0"
                    isoPackageWriter.firstTwoDigitFoCard =
                        isoPackageWriter.panNumber.substring(0, 2)
                    isoPackageWriter.cdtIndex = cardDataTable?.cardTableIndex ?: ""///"1"//
                    isoPackageWriter.accSellection = HexStringConverter.addPreFixer(
                        AppPreference.getString(AppPreference.ACC_SEL_KEY), 2
                    )//cardDataTable.getA//"00"

                    isoPackageWriter.indicator =
                        isoPackageWriter.cardIndFirst + "|" + isoPackageWriter.firstTwoDigitFoCard + "|" + isoPackageWriter.cdtIndex + "|" + isoPackageWriter.accSellection//used for visa// used for ruppay//"0|54|2|00"
                    packageWriterModel!!.indicator = isoPackageWriter.indicator

                    isoPackageWriter.run {
                        setFieldValues(
                            "58",
                            if (field58.isNotEmpty()) field58 else indicator
                        )//card indicator
                        packageWriterModel?.field58 = field58
                    }

                }



                isoPackageWriter.batchNumber = HexStringConverter.addPreFixer(
                    terminalParameterTable!!.batchNumber,
                    6
                )//"000003"
                packageWriterModel!!.batchNumber = isoPackageWriter.batchNumber

                if (isoPackageWriter.isGccAccepted) {
                    "${isoPackageWriter.batchNumber}||1"
                } else isoPackageWriter.batchNumber?.let {
                    isoPackageWriter.setFieldValues(
                        "60",
                        it
                    )
                }//terminalParameterTable!!.batchNumber!!


                isoPackageWriter.terminalSerialNumber = StringUtil.rightPadding(
                    " ",
                    15,
                    HexStringConverter.getSubString(AppPreference.getString("serialNumber"), 15)
                )
                isoPackageWriter.bankCode =
                    AppPreference.BANKCODE//HexStringConverter.addPreFixer(AppPreference.getNewInstance().getStringData(AppPreference.BANK_CODE_KEY, context), 2)//"03"
                isoPackageWriter.customerId = HexStringConverter.addPreFixer(
                    issuerParameterTable!!.customerIdentifierFiledType,
                    2
                )//00
                isoPackageWriter.walletIssuerId =
                    HexStringConverter.addPreFixer(issuerParameterTable!!.issuerId, 2)//50
                isoPackageWriter.connectionType = "3"//1 PSTN ,2 ETH,3 GPRS
                isoPackageWriter.modelName = StringUtil.rightPadding(" ", 6, Build.MODEL) //"VX675 "
                isoPackageWriter.appName = StringUtil.rightPadding(
                    " ",
                    10,
                    context?.getString(R.string.app_name_2)
                )///Build.//"BonusHub  "
                val pInfo = context?.packageManager?.getPackageInfo(context?.packageName ?: "", 0)
                val version = pInfo?.versionName
                val buildDate = Date(BuildConfig.TIMESTAMP)
                val format = SimpleDateFormat("yyMMdd", Locale.getDefault())
                val formatedDate = format.format(buildDate)
                isoPackageWriter.appVersion = "$version.$formatedDate"//"01.01.10"
                isoPackageWriter.pcNumber = HexStringConverter.addPreFixer(
                    AppPreference.getString(AppPreference.PC_NUMBER_KEY),
                    9
                )//1010000128822
                isoPackageWriter.pcNumber2 = HexStringConverter.addPreFixer(
                    AppPreference.getString(AppPreference.PC_NUMBER_KEY_2),
                    9
                )//1010000128822


                packageWriterModel!!.terminalSerialNumber = isoPackageWriter.terminalSerialNumber
                packageWriterModel!!.bankCode = isoPackageWriter.bankCode
                packageWriterModel!!.customerId = isoPackageWriter.customerId
                packageWriterModel!!.walletIssuerId = isoPackageWriter.walletIssuerId
                packageWriterModel!!.connectionType = isoPackageWriter.connectionType
                packageWriterModel!!.modelName = isoPackageWriter.modelName
                packageWriterModel!!.appName = isoPackageWriter.appName
                packageWriterModel!!.appVersion =
                    isoPackageWriter.appVersion // 01.01.35.200611000102685
                //       packageWriterModel!!.appVersion = "01.01.35"
                packageWriterModel!!.pcNumber = isoPackageWriter.pcNumber////261323569//
                packageWriterModel!!.pcNumber2 = isoPackageWriter.pcNumber2

                //  isoPackageWriter.setFieldValues("61", "V1E0242199      0700533VX675 BonusHub  01.01.35.200529000102474000102470")

                isoPackageWriter.setFieldValues(
                    "61",
                    isoPackageWriter.terminalSerialNumber + isoPackageWriter.bankCode + isoPackageWriter.customerId + isoPackageWriter.walletIssuerId + isoPackageWriter.connectionType + isoPackageWriter.modelName + isoPackageWriter.appName + isoPackageWriter.appVersion + isoPackageWriter.pcNumber + isoPackageWriter.pcNumber2
                )//"0300503VX675 BonusHub  01.01.10.161001000000000")///isoPackageWriter.terminalSerialNumber + isoPackageWriter.bankCode + isoPackageWriter.customerId + isoPackageWriter.walletIssuerId + isoPackageWriter.connectionType + isoPackageWriter.modelName + isoPackageWriter.appName + isoPackageWriter.appVersion + isoPackageWriter.pcNumber)////isoPackageWriter.terminalSerialNumber + isoPackageWriter.bankCode + isoPackageWriter.customerId + isoPackageWriter.connectionType + isoPackageWriter.modelName + isoPackageWriter.appName + isoPackageWriter.appVersion + isoPackageWriter.pcNumber
                isoPackageWriter.invoiceNumber = HexStringConverter.addPreFixer("000001", 6)
                packageWriterModel!!.invoiceNumber = isoPackageWriter.invoiceNumber
                CustomToast.printAppLog("   invoiceNumber " + isoPackageWriter.invoiceNumber)
                isoPackageWriter.setFieldValues(
                    "62",
                    isoPackageWriter.invoiceNumber ?: ""
                )//INVOICE NO//
            } catch (e: RemoteException) {
                e.printStackTrace()
            } catch (e: Exception) {
                e.printStackTrace()
            }

        } catch (e: Exception) {
            e.printStackTrace()
            //  paxLog("e", TAG, e.message ?: e.stackTrace.toString())
        }
        return isoPackageWriter
    }

    private fun createVoidPackage(isoPackageWriter: IsoPackageWriter?): IsoPackageWriter? {
        try {
            packageWriterModel = PackageWriterModel()
            isoPackageWriter?.transactionType = transactionType
            packageWriterModel?.transactionType = isoPackageWriter?.transactionType!!
            //tpdu
            //isoPackageWriter.setSerialNumber(60)
            isoPackageWriter.serialNumber = batchFileDataTable!!.serialNumber
            packageWriterModel!!.serialNumber = batchFileDataTable!!.serialNumber
            //isoPackageWriter.setSourceNII(91)
            isoPackageWriter.sourceNII = batchFileDataTable!!.sourceNII
            packageWriterModel!!.sourceNII = batchFileDataTable!!.sourceNII
            //isoPackageWriter.setDestinationNII(1)
            isoPackageWriter.destinationNII = batchFileDataTable!!.destinationNII
            packageWriterModel!!.destinationNII = batchFileDataTable!!.destinationNII
            isoPackageWriter.mti = batchFileDataTable!!.mti//"0200"
            packageWriterModel!!.mti = isoPackageWriter.mti
            //end of tpdu//
            //processing code
            isoPackageWriter.processingCode =
                TransactionTypeValues.getProcCode(transactionType)//"920001"
            packageWriterModel!!.processingCode = isoPackageWriter.getProccingCode()
            isoPackageWriter.setFieldValues("3", isoPackageWriter.getProccingCode()!!)//3 byte

            //transation ammount
            isoPackageWriter.transactionalAmmount =
                (transactionalAmmount + cashBackAmount).toString()//batchFileDataTable!!.totalAmmount
            packageWriterModel!!.transactionalAmmount = isoPackageWriter.transactionalAmmount
            isoPackageWriter.setFieldValues("4", isoPackageWriter.transactionalAmmount!!)//6 byte

            if (terminalParameterTable != null && !terminalParameterTable!!.stan.isEmpty()) {
                val stan = Integer.parseInt(terminalParameterTable!!.stan) + 1
                terminalParameterTable!!.stan = HexStringConverter.addPreFixer(stan.toString(), 6)
                saveTerminalData()
            }
            isoPackageWriter.roc = HexStringConverter.addPreFixer(
                terminalParameterTable!!.stan,
                6
            )//HexStringConverter.addPreFixer(AppPreference.getNewInstance().getStanNumber(AppPreference.STAN_KEY, context).toString(), 6)//
            CustomToast.printAppLog(isoPackageWriter.roc)
            packageWriterModel!!.roc = isoPackageWriter.roc
            CustomToast.printAppLog(isoPackageWriter.roc)
            isoPackageWriter.setFieldValues(
                "11",
                isoPackageWriter.roc!!
            )////"" + AppPreference.getNewInstance().getStanNumber(STAN_KEY, context)
            try {
                val date: Long = Calendar.getInstance().timeInMillis

                //if (ret == 0) {
                val timeFormater = SimpleDateFormat("HHmmss", Locale.getDefault())
                isoPackageWriter.time = timeFormater.format(date)
                isoPackageWriter.setFieldValues("12", isoPackageWriter.time!!)
                val dateFormater = SimpleDateFormat("MMdd", Locale.getDefault())
                //date
                isoPackageWriter.date = dateFormater.format(date)
                isoPackageWriter.timeStamp = date
                packageWriterModel!!.time = isoPackageWriter.time
                packageWriterModel!!.date = isoPackageWriter.date
                packageWriterModel!!.timeStamp = isoPackageWriter.timeStamp
                isoPackageWriter.setFieldValues("13", isoPackageWriter.date!!)
                isoPackageWriter.posEntryValue =
                    batchFileDataTable!!.posEntryValue//TransactionTypeValues.getPosValue().toString()
                packageWriterModel!!.posEntryValue = isoPackageWriter.posEntryValue
                isoPackageWriter.setFieldValues("22", isoPackageWriter.posEntryValue!!)

                isoPackageWriter.nii =
                    HexStringConverter.addPreFixer(batchFileDataTable!!.nii, 4)//"0091"
                packageWriterModel!!.nii = isoPackageWriter.nii
                isoPackageWriter.setFieldValues("24", isoPackageWriter.nii!!)
                //tid
                isoPackageWriter.tid =
                    HexStringConverter.addPreFixer(batchFileDataTable!!.tid, 8)//"10600003"//
                packageWriterModel!!.tid = isoPackageWriter.tid
                isoPackageWriter.setFieldValues("41", isoPackageWriter.tid)//20200002

                isoPackageWriter.mid = HexStringConverter.addPreFixer(
                    batchFileDataTable!!.mid,
                    15
                )//"000000010610002"//630000000000010
                packageWriterModel!!.mid = isoPackageWriter.mid
                isoPackageWriter.setFieldValues("42", isoPackageWriter.mid!!)

                if (!batchFileDataTable!!.field55Data.isEmpty()) {
                    isoPackageWriter.filed55Data = batchFileDataTable!!.field55Data
                    packageWriterModel!!.field55 = isoPackageWriter.filed55Data
                    isoPackageWriter.setFieldValues("55", isoPackageWriter.filed55Data)
                }

                if (isoPackageWriter.transactionType == TransactionTypeValues.VOID || isoPackageWriter.transactionType == TransactionTypeValues.REVERSAL || isoPackageWriter.transactionType == TransactionTypeValues.SALE_COMPLETION || isoPackageWriter.transactionType == TransactionTypeValues.SALE_WITH_TIP || isoPackageWriter.transactionType == TransactionTypeValues.TIP_ADJUSTMENT) {
                    //old stan+date time in yymmddHHmmss
                    val formater = SimpleDateFormat("yyMMddHHmmss", Locale.getDefault())
                    val formatedDate = formater.format(batchFileDataTable!!.timeStamp)
                    isoPackageWriter.field56 = batchFileDataTable!!.roc + formatedDate
                    isoPackageWriter.setFieldValues(
                        "56",
                        isoPackageWriter.field56
                    )//need to set here
                    packageWriterModel!!.field56 = isoPackageWriter.field56
                }

                if (batchFileDataTable?.track2Data != null && batchFileDataTable!!.track2Data.isNotEmpty() && HexStringConverter.hexToString(
                        batchFileDataTable!!.track2Data
                    )
                    != null && !batchFileDataTable!!.track2Data.isEmpty()
                ) {
                    isoPackageWriter.track2Data = batchFileDataTable!!.track2Data
                    isoPackageWriter.operationType = batchFileDataTable!!.operationType
                    isoPackageWriter.setFieldValues("57", isoPackageWriter.track2Data)
                    packageWriterModel!!.track2Data = isoPackageWriter.track2Data
                }
                isoPackageWriter.indicator = batchFileDataTable!!.indicator
                packageWriterModel!!.indicator = isoPackageWriter.indicator
                isoPackageWriter.setFieldValues("58", isoPackageWriter.indicator)
                isoPackageWriter.batchNumber =
                    HexStringConverter.addPreFixer(batchFileDataTable!!.batchNumber, 6)//"000003"
                packageWriterModel!!.batchNumber = isoPackageWriter.batchNumber
                isoPackageWriter.setFieldValues("60", isoPackageWriter.batchNumber!!)

                isoPackageWriter.terminalSerialNumber = batchFileDataTable!!.terminalSerialNumber
                isoPackageWriter.bankCode = batchFileDataTable!!.bankCode
                isoPackageWriter.customerId = batchFileDataTable!!.customerId
                isoPackageWriter.walletIssuerId = batchFileDataTable!!.walletIssuerId
                isoPackageWriter.connectionType = batchFileDataTable!!.connectionType
                isoPackageWriter.modelName = batchFileDataTable!!.modelName
                isoPackageWriter.appName = batchFileDataTable!!.appName

                isoPackageWriter.appVersion = batchFileDataTable!!.appVersion
                isoPackageWriter.pcNumber = batchFileDataTable!!.pcNumber
                packageWriterModel!!.terminalSerialNumber = isoPackageWriter.terminalSerialNumber
                packageWriterModel!!.bankCode = isoPackageWriter.bankCode
                packageWriterModel!!.customerId = isoPackageWriter.customerId
                packageWriterModel!!.walletIssuerId = isoPackageWriter.walletIssuerId
                packageWriterModel!!.connectionType = isoPackageWriter.connectionType
                packageWriterModel!!.modelName = isoPackageWriter.modelName
                packageWriterModel!!.appName = isoPackageWriter.appName
                packageWriterModel!!.appVersion = isoPackageWriter.appVersion
                packageWriterModel!!.pcNumber = isoPackageWriter.pcNumber
                isoPackageWriter.setFieldValues(
                    "61",
                    isoPackageWriter.terminalSerialNumber + isoPackageWriter.bankCode + isoPackageWriter.customerId + isoPackageWriter.walletIssuerId + isoPackageWriter.connectionType + isoPackageWriter.modelName + isoPackageWriter.appName + isoPackageWriter.appVersion + isoPackageWriter.pcNumber
                )
                isoPackageWriter.invoiceNumber =
                    invoiceWithPadding(batchFileDataTable!!.invoiceNumber)
                packageWriterModel!!.invoiceNumber = isoPackageWriter.invoiceNumber
                CustomToast.printAppLog(isoPackageWriter.invoiceNumber)
                isoPackageWriter.setFieldValues("62", isoPackageWriter.invoiceNumber!!)
            } catch (e: RemoteException) {
                e.printStackTrace()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return isoPackageWriter

    }


    //Below method is used to save TerminalData Table in Database:-
    private fun saveTerminalData() {
        paxLog("i", TAG, "new invoice no before saving = ${terminalParameterTable?.invoiceNumber}")
        terminalParameterTable?.let { saveTableInDB(it) }
    }

    //Below method is used to create settlement package:-
    fun createSettlementPackage(
        isoPackageWriter: IsoPackageWriter?,
        batchList: MutableList<BatchFileDataTable>
    ): IsoPackageWriter? {
        try {
            isoPackageWriter!!.setSerialNumber(60)
            isoPackageWriter.setSourceNII(91)
            isoPackageWriter.setDestinationNII(1)
            isoPackageWriter.mti = "0500"
            isoPackageWriter.setFieldValues("3", "970002")
            isoPackageWriter.setFieldValues("24", "0091")
            isoPackageWriter.setFieldValues("41", batchList[0].tid)
            isoPackageWriter.setFieldValues("42", batchList[0].mid)
            isoPackageWriter.setFieldValues("60", batchList[0].batchNumber)
            isoPackageWriter.terminalSerialNumber = StringUtil.rightPadding(
                " ",
                15,
                HexStringConverter.getSubString(isoPackageWriter.terminalSerialNumber, 15)
            )
            if (!TextUtils.isEmpty(AppPreference.getString(AppPreference.BANK_CODE_KEY)))
                isoPackageWriter.bankCode = HexStringConverter.addPreFixer(
                    AppPreference.getString(AppPreference.BANK_CODE_KEY),
                    2
                )//"03"
            else
                isoPackageWriter.bankCode = AppPreference.BANKCODE
            isoPackageWriter.customerId = "00"
            isoPackageWriter.walletIssuerId = AppPreference.WALLET_ISSUER_ID
            isoPackageWriter.connectionType = "3"//1 PSTN ,2 ETH,3 GPRS
            isoPackageWriter.modelName = StringUtil.rightPadding(" ", 6, Build.MODEL) //"VX675 "
            isoPackageWriter.appName = StringUtil.rightPadding(
                " ",
                10,
                context!!.getString(R.string.app_name)
            )///Build.//"BonusHub  "
            isoPackageWriter.appVersion =
                addPad(getAppVersionNameAndRevisionID(), "0", 15, false)//"01.01.10"
            if (TextUtils.isEmpty(AppPreference.getString(AppPreference.PC_NUMBER_KEY)))
                isoPackageWriter.pcNumber = HexStringConverter.addPreFixer("0", 9)
            else
                isoPackageWriter.pcNumber = HexStringConverter.addPreFixer(
                    AppPreference.getString(AppPreference.PC_NUMBER_KEY),
                    9
                )
            isoPackageWriter.setFieldValues(
                "61",
                addPad(
                    VerifoneApp.getDeviceSerialNo(),
                    " ",
                    15,
                    false
                ) + AppPreference.getBankCode()
            )

            isoPackageWriter.setFieldValues(
                "62",
                ConnectionType.GPRS.code + addPad(
                    AppPreference.getString("deviceModel"),
                    "*",
                    6,
                    false
                ) +
                        addPad(
                            VerifoneApp.appContext.getString(R.string.app_name),
                            " ",
                            10,
                            false
                        ) +
                        addPad(getAppVersionNameAndRevisionID(), "0", 15, false) +
                        addPad("0", "0", 9)
            )//"3VX675 BonusHub  01.01.01.161001000000000")//isoPackageWriter.terminalSerialNumber+isoPackageWriter.bankCode+isoPackageWriter.customerId+isoPackageWriter.walletIssuerId+isoPackageWriter.connectionType+isoPackageWriter.modelName+isoPackageWriter.appName+isoPackageWriter.appVersion+isoPackageWriter.pcNumber
            isoPackageWriter.setFieldValues("63", "1")
        } catch (e: Exception) {
            e.printStackTrace()

        }
        return isoPackageWriter

    }


    private fun createReversalPackage(
        packageWriterModel: PackageWriterModel?,
        isoPackageWriter: IsoPackageWriter
    ): IsoPackageWriter {
        try {
            if (packageWriterModel != null) {
                //tpdu
                isoPackageWriter.serialNumber = packageWriterModel.serialNumber
                isoPackageWriter.sourceNII = packageWriterModel.sourceNII
                isoPackageWriter.destinationNII = packageWriterModel.destinationNII
                isoPackageWriter.mti = TransactionTypeValues.Rev_Res_MTI//"0400"
                //end of tpdu//
                //processing code
                isoPackageWriter.processingCode = packageWriterModel.processingCode
                isoPackageWriter.setFieldValues("3", isoPackageWriter.getProccingCode()!!)//3 byte
                //transation ammount
                isoPackageWriter.transactionalAmmount = packageWriterModel.transactionalAmmount
                isoPackageWriter.setFieldValues(
                    "4",
                    isoPackageWriter.transactionalAmmount!!
                )//6 byte
                isoPackageWriter.roc = packageWriterModel.roc
                CustomToast.printAppLog(isoPackageWriter.roc)
                isoPackageWriter.setFieldValues("11", isoPackageWriter.roc!!)
                isoPackageWriter.time = packageWriterModel.time
                isoPackageWriter.setFieldValues("12", isoPackageWriter.time!!)
                //date
                isoPackageWriter.date = packageWriterModel.date
                isoPackageWriter.timeStamp = packageWriterModel.timeStamp
                isoPackageWriter.setFieldValues("13", isoPackageWriter.date!!)
                isoPackageWriter.posEntryValue = packageWriterModel.posEntryValue
                isoPackageWriter.setFieldValues("22", isoPackageWriter.posEntryValue!!)
                if (packageWriterModel.applicationPanSequenceNumber != null) {
                    isoPackageWriter.applicationPanSequenceNumber =
                        packageWriterModel.applicationPanSequenceNumber
                    isoPackageWriter.setFieldValues(
                        "23",
                        isoPackageWriter.applicationPanSequenceNumber!!
                    )
                }
                //nii
                isoPackageWriter.nii = packageWriterModel.nii
                isoPackageWriter.setFieldValues("24", isoPackageWriter.nii!!)
                if (packageWriterModel.field39 != null && packageWriterModel.field39.isNotEmpty()) {
                    isoPackageWriter.field39 = packageWriterModel.field39
                    isoPackageWriter.setFieldValues("39", isoPackageWriter.field39)
                }
                //tid
                isoPackageWriter.tid = packageWriterModel.tid
                isoPackageWriter.setFieldValues("41", isoPackageWriter.tid)//20200002//11100112
                isoPackageWriter.mid = packageWriterModel.mid
                isoPackageWriter.setFieldValues("42", isoPackageWriter.mid!!)
                if (packageWriterModel.field55 != null && packageWriterModel.field55.isNotEmpty()) {
                    isoPackageWriter.filed55Data = packageWriterModel.field55
                    isoPackageWriter.setFieldValues("55", isoPackageWriter.filed55Data)
                }
                val formatter = SimpleDateFormat("yyMMddHHmmss", Locale.getDefault())
                val dateTime = formatter.format(packageWriterModel.timeStamp)
                isoPackageWriter.setFieldValues("56", packageWriterModel.roc + dateTime)
                if (packageWriterModel.track2Data != null && !packageWriterModel.track2Data.isEmpty()) {
                    isoPackageWriter.track2Data = packageWriterModel.track2Data
                    isoPackageWriter.setFieldValues("57", isoPackageWriter.track2Data)
                }

                isoPackageWriter.indicator = packageWriterModel.indicator
                isoPackageWriter.field58 = packageWriterModel.field58
                isoPackageWriter.setFieldValues(
                    "58",
                    if (isoPackageWriter.field58.isNotEmpty()) isoPackageWriter.field58 else isoPackageWriter.indicator
                )

                isoPackageWriter.batchNumber = packageWriterModel.batchNumber
                isoPackageWriter.setFieldValues("60", isoPackageWriter.batchNumber!!)
                isoPackageWriter.terminalSerialNumber = packageWriterModel.terminalSerialNumber
                isoPackageWriter.bankCode = packageWriterModel.bankCode
                isoPackageWriter.customerId = packageWriterModel.customerId
                isoPackageWriter.walletIssuerId = packageWriterModel.walletIssuerId
                isoPackageWriter.connectionType = packageWriterModel.connectionType
                isoPackageWriter.modelName = packageWriterModel.modelName
                isoPackageWriter.appName = packageWriterModel.appName
                isoPackageWriter.appVersion = packageWriterModel.appVersion
                isoPackageWriter.pcNumber = packageWriterModel.pcNumber
                isoPackageWriter.setFieldValues(
                    "61",
                    isoPackageWriter.terminalSerialNumber + isoPackageWriter.bankCode + isoPackageWriter.customerId + isoPackageWriter.walletIssuerId + isoPackageWriter.connectionType + isoPackageWriter.modelName + isoPackageWriter.appName + isoPackageWriter.appVersion + isoPackageWriter.pcNumber
                )//"380986977      0300503VX675 BonusHub  01.01.10.161010000128822")//
                isoPackageWriter.transactionType =
                    TransactionTypeValues.getTransactionType(TransactionTypeValues.TransactionType.REVERSAL)
                isoPackageWriter.invoiceNumber = packageWriterModel.invoiceNumber
                CustomToast.printAppLog(isoPackageWriter.invoiceNumber)
                isoPackageWriter.setFieldValues(
                    "62",
                    isoPackageWriter.invoiceNumber!!
                )//INVOICE NO//
                isReversal = true
            }
        } catch (e: Exception) {
            e.printStackTrace()

        }
        return isoPackageWriter
    }

    override fun onXmlSuccess(xmlFieldModels: HashMap<String, XmlFieldModel>?) {
        val data = createReversalPackage(packageWriterModel, isoPackageWriterForReversal!!)
        sendingToServer(data, xmlFieldModels)
    }

    override fun onXmlError(message: String?) {

        Log.e("XML_ERROR", "---------VFEmv---------")
    }

    //Below method is a wrapper method on checkReversal method for the void case:-
    fun checkReversalForVoid(isVoidReversal: Boolean = false) {
        this.voidReversalStatus = isVoidReversal
        this.packageWriterModel = checkReversal()
        isoPackageWriterForReversal = IsoPackageWriter(this.context, this)
    }

    //Below method is a wrapper method on checkReversal method for the settlement case:-
    fun checkReversalForSettlement(
        isSettlementReversal: Boolean = false,
        settlementByteArray: ByteArray
    ) {
        this.settlementReversalStatus = isSettlementReversal
        this.settlementByteArrayValue = settlementByteArray
        this.packageWriterModel = checkReversal()
        isoPackageWriterForReversal = IsoPackageWriter(this.context, this)
    }

    //Below method is to check Reversal:-
    private fun checkReversal(): PackageWriterModel? {
        val gson = Gson()
        var reversalData = AppPreference.getString(GENERIC_REVERSAL_KEY)
        if (!TextUtils.isEmpty(reversalData))
            reversalData = "{\"data\":$reversalData}"
        return gson.fromJson(reversalData, ReversalRes::class.java)?.data
    }

}