package com.example.verifonevx990app.transactions

import android.util.Log
import com.example.verifonevx990app.R
import com.example.verifonevx990app.realmtables.BatchFileDataTable
import com.example.verifonevx990app.realmtables.IssuerParameterTable
import com.example.verifonevx990app.realmtables.TerminalParameterTable
import com.example.verifonevx990app.utils.HexStringConverter
import com.example.verifonevx990app.vxUtils.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*


interface ITransaction {
    suspend fun createIso(): IWriter
    suspend fun
            readIsoResponse(data: String)
}

/**
 * First Parameter String is message ,
 * Second Parameter Boolean is to differentiate for Transactional success
 * Third Parameter Boolean is to differentiate for progress success
 * */
typealias OnProgress = ApiCallback

abstract class Transaction(val callback: OnProgress) : ITransaction {

    companion object {
        private val TAG = Transaction::class.java.simpleName
    }

    fun callCaller(msg: String, isResponse: Boolean, isProgress: Boolean) {
        logger(
            "Transaction",
            "message = $msg, isResponseType = $isResponse, isProgress = $isProgress"
        )
        GlobalScope.launch(Dispatchers.Main) {
            callback(msg, isResponse, isProgress)
        }
    }

}

interface IReversalHandler {
    suspend fun saveReversal()
    fun clearReversal()
}

class SaleTransaction(
    private val iso: IsoDataWriter,
    private val ibsr: IBonushubServerResponse,
    callback: OnProgress
) :
    Transaction(callback), IReversalHandler {
    lateinit var mTpt: TerminalParameterTable

    companion object {
        private val TAG = SaleTransaction::class.java.simpleName
    }

    override suspend fun saveReversal() {
        //   AppPreference.saveReversal(iso)
    }

    override fun clearReversal() {
        AppPreference.clearReversal()
    }

    //add date , time and roc , rest fields has been added in Transaction controller
    override suspend fun createIso(): IWriter = iso.apply {
        //adding stan (padding of stan is internally handled by iso)
        addField(11, ROCProviderV2.getRoc(mTpt.tidBankCode).toString())

        // adding time(12) and date(13)
        addIsoDateTime(this)

        addFieldByHex(48, ConnectionTimeStamps.getStamp() + ConnectionTimeStamps.getOtherInfo())

    }

    /**
     * @author mahesh.prajapati
     * Creating parse ISO packate from response
     *
     */
    override suspend fun readIsoResponse(data: String) {
        val iReader = readIso(data, false)

        val roc = iReader.isoMap[11]?.rawData
        if (roc != null) ROCProviderV2.incrementFromResponse(
            roc,
            mTpt.tidBankCode
        ) else ROCProviderV2.increment(mTpt.tidBankCode)

        val f48: String = iReader.isoMap[48]?.parseRaw2String() ?: ""
        if (f48.isNotEmpty()) ConnectionTimeStamps.saveStamp(f48)

        val resCode: String = iReader.isoMap[39]?.parseRaw2String() ?: ""

        var resMsg: String = iReader.isoMap[58]?.parseRaw2String() ?: ""
        resMsg =
            if (resCode == "00") VerifoneApp.appContext.getString(R.string.execute_success) else resMsg

        callCaller(resMsg, true, false)

        if (resCode == "00") {
            val f62 = iReader.isoMap[62]?.parseRaw2String() ?: mTpt.invoiceNumber
            var f62L = f62.toLong() + 1
            if (f62L > 999999) {
                f62L = 1
            }
            mTpt.invoiceNumber = f62L.toString()
            TerminalParameterTable.performOperation(mTpt) {
                logger(TAG, "Batch file updated")
            }
        }


        GlobalScope.launch {
            for ((e, v) in iReader.isoMap) {
                //println(" $e  == ${v.rawData}  ")
            }

            //println("==============================================================")

            for ((e, v) in iReader.isoMap) {
                //println(" $e  == ${v.parseRaw2String()}  ")
            }
        }

        if (resCode == "00") {
            ibsr.onTransactionSuccess(iReader, mTpt)
        } else {
            ibsr.onTransactionError(resMsg, iReader)
        }

    }

    /**
     * @author mahesh.prajapati
     * Start transection
     *
     */
    fun start() {
        GlobalScope.launch {
            checknPerformReversal(::callCaller) {
                GlobalScope.launch {
                    if (it) {
                        //region sale execution area
                        val data = createIso().generateIsoByteRequest()
                        Log.d("Bonushub Message:mkp ", data.byteArr2HexStr())
                        HitServer.hitServer(data, { msg, isSuccess ->
                            GlobalScope.launch {
                                if (isSuccess) {
                                    AppPreference.clearReversal()
                                    readIsoResponse(msg)
                                } else {
                                    callCaller(msg, isSuccess, false)
                                }
                            }
                        }, { resp ->
                            callCaller(resp, false, true)
                        })
                        //endregion
                    } else {
                        callback("Reversal sending error.\nTransaction declined!!!", false, false)
                    }
                }
            }
        }// end of global scope
    }

}


/**
 * if onResult Boolean is true the Iso read data else message for error.
 * */

suspend fun doSettlement(
    pc: OnProgress,
    onResult: suspend (Boolean, Any) -> Unit,
    batchList: List<BatchFileDataTable>? = null,
    tpt: TerminalParameterTable? = null
) {
    checknPerformReversal(pc) {
        GlobalScope.launch {
            if (it) {
                if (tpt != null) {
                    val iWriter = IsoDataWriter().apply {
                        mti = Mti.SETTLEMENT_MTI.mti
                        addField(3, TransactionType.SETTLEMENT.processingCode.code)
                        addField(11, ROCProviderV2.getRoc(tpt.tidBankCode).toString())
                        addField(24, Nii.DEFAULT.nii)

                        addFieldByHex(41, tpt.terminalId)
                        addFieldByHex(42, tpt.merchantId)
                        addFieldByHex(
                            48,
                            ConnectionTimeStamps.getStamp() + ConnectionTimeStamps.getOtherInfo()
                        )

                        addFieldByHex(60, addPad(tpt.batchNumber, "0", 6))
                        val f62 = KeyExchanger.getF61()
                        addFieldByHex(62, f62)

                        //region=========Adding f61============
                        val terminalSerialNumber =
                            addPad(VerifoneApp.getDeviceSerialNo(), " ", 15, false)

                        val bankCode = AppPreference.getBankCode()
                        val f61 = "$terminalSerialNumber$bankCode"
                        addFieldByHex(61, f61)
                        //endregion========

                        addFieldByHex(63, "1")
                    }

                    val byteData = iWriter.generateIsoByteRequest()

                    //region=============After Success Settlement==============
                    fun parser(isodata: String) {
                        GlobalScope.launch {
                            val iread = readIso(isodata, false)

                            val f39 = iread.isoMap[39]?.parseRaw2String()

                            if (f39 == "00") {

                                //region===========clear Batch List=========
                                BatchFileDataTable.clear()
                                //endregion

                                //region=========resetting stan, batch and invoice==========
                                var bn = tpt.batchNumber.toInt()
                                bn += 1
                                if (bn > 999999) {
                                    bn = 1
                                }
                                tpt.batchNumber = bn.toString()
                                tpt.stan = "1"
                                tpt.invoiceNumber = "1"

                                TerminalParameterTable.performOperation(tpt) {}

                                ROCProvider.resetRoc()

                                //endregion

                                //region======reset, init, keyexchange and stan========
                                //Todo rest init key-exchange part to be done
                                //Todo update date and time from f48
                                //endregion

                                onResult(true, iread)

                            } else {
                                val msg = iread.isoMap[58]?.parseRaw2String() ?: ""
                                onResult(false, msg)
                            }
                        }
                    }
                    //endregion

                    HitServer.hitServer(byteData,
                        { msg, isSuccess ->
                            if (isSuccess) {
                                parser(msg)
                            } else {
                                pc(msg, true, false)
                            }
                        },
                        {
                            pc(it, false, true)
                        })

                } else onResult(false, "Terminal Param Table Error!!!")
            } else {
                onResult(false, "Reversal error settlement declined!!!")
            }
        }
    }
}


fun checknPerformReversal(pc: OnProgress, resultCb: (Boolean) -> Unit) {
    GlobalScope.launch {
        logger("Reversal", "===============checknPerformReversal===================", "e")
        try {
            val preIsoDataWriter = AppPreference.getReversal()
            if (preIsoDataWriter != null) {
                var bankCode: String =
                    ""  // Bank Code will be reassigned when we will parse the field 61
                val isoData = IsoDataWriter().apply {
                    mti = Mti.REVERSAL.mti

                    //====Adding field 3====
                    val f3 = preIsoDataWriter.isoMap[3]
                    if (f3 != null)
                        addField(3, f3.rawData)

                    //====Adding field 4====
                    val f4 = preIsoDataWriter.isoMap[4]
                    if (f4 != null)
                        addField(4, f4.rawData)

                    //region===Adding field 12,13, 22, 23, 24, 41, 42, 48====


                    //12, 13
                    addIsoDateTime(this)

                    val f22 = preIsoDataWriter.isoMap[22]
                    if (f22 != null)
                        addField(22, f22.rawData)
//                    isoMap[22] = f22

                    val f23 = preIsoDataWriter.isoMap[23]
                    if (f23 != null)
                        addFieldByHex(23, f23.parseRaw2String())
//                    isoMap[23] = f23

                    val f24 = preIsoDataWriter.isoMap[24]
                    if (f24 != null)
                        addField(24, f24.rawData)
//                    isoMap[24] = f24

                    val f41 = preIsoDataWriter.isoMap[41]
                    if (f41 != null)
                        addFieldByHex(41, f41.parseRaw2String())
//                    isoMap[41] = f41

                    val f42 = preIsoDataWriter.isoMap[42]
                    if (f42 != null)
                        addFieldByHex(42, f42.parseRaw2String())
//                    isoMap[42] = f42

                    addFieldByHex(48, Field48ResponseTimestamp.getF48Data())

                    //endregion////


                    //region======Adding field 55, 56, 57, 58============
                    //F55
                    val f55 = preIsoDataWriter.isoMap[55]
                    if (f55 != null)
                        addFieldByHex(55, f55.parseRaw2String())

                    val cal = Calendar.getInstance()
                    cal.timeInMillis =
                        if (timeStamp != 0L) timeStamp else System.currentTimeMillis()
                    val yr = cal.get(Calendar.YEAR).toString().substring(2)
                    val of11 = preIsoDataWriter.isoMap[11]?.rawData ?: ""
                    val of12 = preIsoDataWriter.isoMap[12]?.rawData ?: ""
                    val of13 = preIsoDataWriter.isoMap[13]?.rawData ?: ""
                    val f56 = of11 + yr + of13 + of12
                    addFieldByHex(56, f56)

                    val f57 = preIsoDataWriter.isoMap[57]
                    if (f57 != null)
                        addFieldByHex(57, f57.parseRaw2String())

                    val f58 = preIsoDataWriter.isoMap[58]
                    if (f58 != null)
                        addFieldByHex(58, f58.parseRaw2String())

                    //endregion

                    //region=======Adding field 60, 61, 62========

                    val f60 = preIsoDataWriter.isoMap[60]
                    if (f60 != null)
                        addFieldByHex(60, f60.parseRaw2String())

                    val f61 = preIsoDataWriter.isoMap[61]
                    if (f61 != null) {
                        val f61Str = f61.parseRaw2String()
                        bankCode = f61Str.substring(15..16)
                        addFieldByHex(61, f61Str)
                    }

                    //Adding field 11 after knowing field 61
                    addField(11, ROCProviderV2.getRoc(bankCode).toString())

                    val f62 = preIsoDataWriter.isoMap[62]
                    if (f62 != null)
                        addFieldByHex(62, f62.parseRaw2String())

                    //endregion

                }

                val data = isoData.generateIsoByteRequest()
                HitServer.hitServer(data,
                    { msg, isSucc ->
                        GlobalScope.launch {
                            if (isSucc) {
                                val iReader = readIso(msg, false)

                                val roc = iReader.isoMap[11]?.rawData
                                if (roc != null) ROCProviderV2.incrementFromResponse(
                                    roc,
                                    bankCode
                                ) else ROCProviderV2.increment(bankCode)

                                val res = iReader.isoMap[39]?.parseRaw2String()
                                if (res == "00") {
                                    AppPreference.clearReversal()

                                    resultCb(true)

                                } else resultCb(false)

                            } else resultCb(false)
                        }
                    },
                    {
                        pc(it, false, true)
                    })

            } else {
                resultCb(true)
            }
        } catch (ex: Exception) {
            resultCb(false)
        }
    }

}

interface IBonushubServerResponse {
    fun onTransactionSuccess(iReader: IsoDataReader, tpt: TerminalParameterTable)
    fun onTransactionError(message: String, iReader: IsoDataReader)
}


class PayBySmsData(val amount: Long, val mobile: Long)

class PayBySms(
    val data: PayBySmsData,
    callback: (String/*Message*/, Boolean/*Transactional Success*/, Boolean/*Progress Success*/) -> Unit
) : Transaction(callback) {

    var tpt: TerminalParameterTable? = null
    var ipt: IssuerParameterTable? = null

    companion object {
        private val TAG = PayBySmsData::class.java.simpleName
    }
/*

    fun startTransaction() {
        GlobalScope.launch {

            suspend fun sp() {
                val iW = createIso()
                val data = iW.generateIsoByteRequest()
                HitServer.hitServer(data, { msg, isSuccess ->
                    if (isSuccess) {
                        launch { readIsoResponse(msg) }
                    } else callCaller(msg, isSuccess, false)
                }, { callCaller(it, false, true) })
            }

            if (tpt != null && ipt != null) {
                sp()
            } else {
                tpt = TerminalParameterTable.selectFromSchemeTable()
                ipt =
                    IssuerParameterTable.selectFromIssuerParameterTable(
                        AppPreference.getString(
                            AppPreference.CRDB_ISSUER_ID_KEY
                        )
                    )

                if (tpt == null || ipt == null) {
                    callCaller(
                        "No terminal param data or issuer param data not found",
                        false,
                        false
                    )
                } else sp()
            }
        }
    }

*/

    override suspend fun readIsoResponse(data: String) {
        val iso = readIso(data)
        logger(TAG, iso.isoMap)

        val resp = iso.isoMap[39]
        val msgIso = iso.isoMap[58]
        if (resp != null && resp.parseRaw2String() == "") {
            //todo sms pay query
            callCaller(msgIso?.parseRaw2String() ?: "", true, false)
        } else {
            callCaller(msgIso?.parseRaw2String() ?: "No Message in failure", false, false)
        }

    }


    override suspend fun createIso(): IWriter = IsoDataWriter().apply {
        mti = Mti.DEFAULT_MTI.mti
        // adding processing code and field 59 for public and private key
        addField(2, data.mobile.toString())
        addField(3, ProcessingCode.SALE.code)
        addField(4, data.amount.toString())
        //adding stan (padding of stan is internally handled by iso)
        addField(11, ROCProvider.getRoc().toString())

        // adding time(12) and date(13)
        addIsoDateTime(this)

        //adding nii
        addField(24, Nii.SMS_PAY.nii)


        addFieldByHex(52, addPad("", " ", 8))

        if (tpt != null) {
            //region=======adding tid and mid==========
            addFieldByHex(41, tpt?.terminalId ?: "")
            addFieldByHex(42, tpt?.merchantId ?: "")
            //endregion

            //region=========adding field 60===========
            addFieldByHex(60, tpt?.batchNumber ?: "")
            //endregion

            //region=========adding field 62===========
            addFieldByHex(62, tpt?.invoiceNumber ?: "")
            //endregion

        }

        //region=========adding field 61=============
        val f61 =
            getField61(
                ipt as IssuerParameterTable,
                (tpt as TerminalParameterTable).tidBankCode,
                Mti.DEFAULT_MTI.mti
            )
        addFieldByHex(61, f61)
        //endregion

    }

}


//region========== Field 61, Pan Mask, Field 57=================
@Synchronized
fun addField61ToBatch(ipt: IssuerParameterTable, bfdt: BatchFileDataTable) {
    bfdt.run {
        terminalSerialNumber = VerifoneApp.getDeviceSerialNo()//terminalSerialNumber
        bankCode = AppPreference.getBankCode()
        walletIssuerId = ipt.issuerId                             //walletId
        customerId = ipt.customerIdentifierFiledType//customerId
        appName =
            VerifoneApp.appContext.getString(R.string.app_name)  // Dont forget to addpadding in app name
        val mn = VerifoneApp.getDeviceModel()
        modelName = when {
            mn.length > 6 -> mn.substring(0, 6)
            mn.length < 6 -> addPad(mn, " ", 6, false)  // right padding of 6 byte for device model
            else -> mn
        }
        appVersion = addPad(getAppVersionNameAndRevisionID(), "0", 15, false)  //version
        connectionType = ConnectionType.GPRS.code  //connectionType
        AppPreference.getString(AppPreference.PC_NUMBER_KEY)//pccNo
    }
}

fun BatchFileDataTable.getField61(): String {
    val deviceModel = VerifoneApp.getDeviceModel()
    val version = addPad(getAppVersionNameAndRevisionID(), "0", 15, false)

    val apName = addPad(appName, " ", 10, false)

    val pccNo = addPad(AppPreference.getString(AppPreference.PC_NUMBER_KEY), "0", 9)
    return "$terminalSerialNumber$bankCode$customerId$walletIssuerId$connectionType$deviceModel$apName$version$pccNo"
}

/**
 * 0 = sale,
 * 1 = mcash back ,
 * 2 = refund, 3 = pre auth,
 * 4 = pre auth completion
 * */

@Synchronized
fun getMaskedPan(terminalParameterTable: TerminalParameterTable?, panNumber: String): String? {
    return panMasking(panNumber, terminalParameterTable?.panMaskFormate ?: "0000********0000")
}

fun panMasking(input: String, maskFormat: String): String {
    if (input.isNotEmpty()) {
        val maskCharArr = maskFormat.toCharArray()
        val inputArr = input.toCharArray()

        // get all stars index
        val li = arrayListOf<Int>()
        for (e in maskCharArr.indices) {
            if (maskFormat[e] == '*') {
                li.add(e)
            }
        }
        when {
            inputArr.size == maskCharArr.size -> for (e in li) {
                inputArr[e] = '*'
            }
            inputArr.size > maskCharArr.size -> {
                for (e in li.first()..(inputArr.lastIndex - li.last())) {
                    inputArr[e] = '*'
                }
            }
            else -> for (e in 4..(inputArr.lastIndex - 4)) {
                inputArr[e] = '*'
            }
        }
        val sb = StringBuilder()

        var index = 0
        while (index < inputArr.size) {
            var endIndex = index + 3
            if (endIndex > inputArr.lastIndex) {
                endIndex = inputArr.lastIndex
            }
            val tempCh = inputArr.slice(index..endIndex)
            sb.append(tempCh.toCharArray())
            sb.append(" ")
            index += 4
        }

        return sb.toString().substring(0, sb.lastIndex)
    } else return ""
}

fun getF57Bytes(f57: String): ByteArray? {

    val f57Arr = f57.str2ByteArr()
    logger("getF57Bytes", "====F57==>${f57Arr.byteArr2Str()}")
    val cardLenWithField = f57Arr.size//lenStr2[0] + 2
    val inLength: Int = if (cardLenWithField % 8 == 0) {
        cardLenWithField
    } else {
        cardLenWithField + (8 - (cardLenWithField % 8))
    }
    val f57En = ByteArray(inLength) { 48 }
    for (i in 0 until cardLenWithField) {
        f57En[i] = f57Arr[i]
    }


    return byteArrayOf() //TODO Here we will write the code by which we can get the TLE data (Track)  ====>>  byteArrayOf() is a dummy call

    /*DalHelper.dal.getPed(EPedType.INTERNAL)
        .calcDes(INDEX_TDK, f57En, EPedDesMode.ENCRYPT)?:null*/
}

//endregion


//region============Void Transactions===============
/**
 * in onResult, true and IsoReader will be params and false and String.
 * */
fun doVoidTransaction(
    batch: BatchFileDataTable, onProgress: OnProgress,
    onResult: (Boolean, Any) -> Unit, transactionType: TransactionType = TransactionType.VOID
) {

    //region=======Parsing Response===========
    fun parseResponse(response: String, bankCode: String) {
        val resp = readIso(response, false)

        val roc = resp.isoMap[11]?.rawData
        if (roc != null) ROCProviderV2.incrementFromResponse(
            roc,
            bankCode
        ) else ROCProviderV2.increment(bankCode)

        if (resp.isoMap[39]?.parseRaw2String() == "00") {
            onResult(true, batch)
        } else {
            val msg = resp.isoMap[58]?.parseRaw2String() ?: ""
            onResult(false, msg)
        }

    }
    //endregion=============

    checknPerformReversal(onProgress) { noReversal ->
        // check and perform Reversal will give callback in non UI Thread
        if (noReversal) {

            GlobalScope.launch {

                // packing data
                val idw = IsoDataWriter().apply {
                    mti = Mti.DEFAULT_MTI.mti

                    addField(3, transactionType.processingCode.code)
                    addField(4, batch.transactionalAmmount)


                    // adding ROC (11) time(12) and date(13)
                    addField(
                        11,
                        ROCProviderV2.getRoc(batch.bankCode)
                            .toString() /*ROCProvider.getRoc().toString()*/
                    )

                    addIsoDateTime(this)

                    addField(22, batch.posEntryValue)
                    addField(24, Nii.DEFAULT.nii)

                    /*addFieldByHex(
                        31,
                        batch.aqrRefNo
                    ) */ // going in case of Amex for visa and master check if to send or not

                    addFieldByHex(41, batch.tid)
                    addFieldByHex(42, batch.mid)
                    addFieldByHex(48, Field48ResponseTimestamp.getF48Data())

                    //Transaction's ROC, transactionDate, transaction Time
                    val f56 = "${batch.roc}${batch.transactionDate}${batch.transactionTime}"
                    addFieldByHex(56, f56)

                    addFieldByHex(57, batch.track2Data)
                    addFieldByHex(58, batch.indicator)

                    addFieldByHex(60, batch.batchNumber)

                    addFieldByHex(61, batch.getField61())

                    addFieldByHex(62, batch.invoiceNumber)

                }

                val byteData = idw.generateIsoByteRequest()

                HitServer.hitServer(byteData,
                    { msg, isSuccess ->
                        if (isSuccess) {
                            parseResponse(msg, batch.bankCode)
                        } else {
                            onProgress(
                                msg,
                                false,
                                false
                            ) // Progress false means it is response , and response false means no connection
                        }
                    },
                    {
                        onProgress(it, false, true)
                    })
            }

        } else {
            onResult(false, "Reversal error. Void sale declined!!!")
        }
    }

}

//endregion

//region===========EMI enquiry api for SMS==============
fun doEmiEnquiry(
    data: HashMap<String, String>,
    onProgress: OnProgress,
    onResult: (Boolean, Any) -> Unit
) {

    fun parseResponse(msg: String) {
        val iso = readIso(msg, false)

        val roc = iso.isoMap[11]?.rawData
        if (roc != null) ROCProvider.incrementFromResponse(roc) else ROCProvider.increment()


        if (iso.isoMap[39]?.parseRaw2String() == "00") {
            onResult(true, "")
        } else {
            val f58 = iso.isoMap[58]?.parseRaw2String() ?: ""
            onResult(false, f58)
        }
    }

    checknPerformReversal(onProgress) { noReversal ->
        if (noReversal) {
            GlobalScope.launch {
                val tpt = TerminalParameterTable.selectFromSchemeTable()
                val issuer = data["issuer"] ?: ""
                val ipt = IssuerParameterTable.selectFromIssuerParameterTable(issuer)
                if (tpt != null && ipt != null) {
                    val idw = IsoDataWriter()

                    with(idw) {
                        mti = Mti.PRE_AUTH_MTI.mti

                        addField(3, ProcessingCode.EMI_ENQUIRY.code)
                        addField(4, data["amount"] ?: "")

                        // adding ROC (11) time(12) and date(13)  ROCProviderV2.getRoc(AppPreference.getBankCode()).toString()
                        addField(11, ROCProviderV2.getRoc(AppPreference.getBankCode()).toString())
                        addIsoDateTime(this)

                        addField(24, Nii.DEFAULT.nii)

                        addFieldByHex(41, tpt.terminalId)
                        addFieldByHex(42, tpt.merchantId)

                        addFieldByHex(48, Field48ResponseTimestamp.getF48Data())

                        val f58 = "${data["mobile"]}|$issuer|"
                        addFieldByHex(58, f58)

                        addFieldByHex(60, addPad(tpt.batchNumber, "0", 6))


                        //adding field 61
                        val issuerParameterTable =
                            IssuerParameterTable.selectFromIssuerParameterTable(AppPreference.WALLET_ISSUER_ID)
                        val version = addPad(getAppVersionNameAndRevisionID(), "0", 15, false)
                        val pcNumber =
                            addPad(AppPreference.getString(AppPreference.PC_NUMBER_KEY), "0", 9)
                        val data = ConnectionType.GPRS.code + addPad(
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
                                version + pcNumber + addPad("0", "0", 9)
                        val customerID = HexStringConverter.addPreFixer(
                            issuerParameterTable?.customerIdentifierFiledType,
                            2
                        )

                        val walletIssuerID =
                            HexStringConverter.addPreFixer(issuerParameterTable?.issuerId, 2)
                        addFieldByHex(
                            61,
                            addPad(
                                AppPreference.getString("serialNumber"),
                                " ",
                                15,
                                false
                            ) + AppPreference.getBankCode() + customerID + walletIssuerID + data
                        )

                        //    val f61 = getField61(ipt)
                        // addFieldByHex(61, f61)

                        addFieldByHex(62, addPad(tpt.invoiceNumber, "0", 6))

                    }
                    val byteData = idw.generateIsoByteRequest()
                    logger("Request SMS", idw.isoMap)
                    HitServer.hitServer(byteData,
                        { msg, isSuccess ->
                            if (isSuccess) {
                                parseResponse(msg)
                            } else {
                                onProgress(msg, true, false)
                            }
                        },
                        {
                            onProgress(it, false, true)
                        })


                } else {
                    onResult(false, "TPT or IPT error. Init Again")
                }
            }
        } else {
            onResult(false, "Reversal error. Void sale declined!!!")
        }
    }
}
//endregion

/**
 * This function do not add following parameter, Add them after calling
 *=========================================================
 * Transaction Type, Transaction Amount, Base Amount, Total Amount,
 * POS Entry Value, Batch number Is Auto Settle,
 * Encripted Track 2 and Field 58 (Indicator),
 * PinVerified, Card number , Expiry, Card Holder Name, Operation Type
 *===========================================================
 * */
fun addIso2Batch(
    batch: BatchFileDataTable,
    ir: IsoDataReader,
    tpt: TerminalParameterTable,
    ipt: IssuerParameterTable
) {

    with(batch) {
        serialNumber = ir.srNo
        sourceNII = ir.srcNii
        destinationNII = ir.destNii
        mti = ir.mti

        nii = ir.isoMap[24]?.rawData ?: ""

        applicationPanSequenceNumber = ir.isoMap[23]?.rawData ?: ""

        merchantName = tpt.receiptHeaderTwo
        panMask = tpt.panMask
        panMaskConfig = tpt.panMaskConfig
        panMaskFormate = tpt.panMaskFormate

        merchantAddress1 = tpt.receiptHeaderThree
        merchantAddress2 = tpt.receiptHeaderThree


        val resTimeStamp = ir.isoMap[12]?.rawData ?: ""
        if (resTimeStamp.isNotEmpty()) {
            val formatter = SimpleDateFormat("yyMMddHHmmss", Locale.getDefault())
            val date = formatter.parse(resTimeStamp)
            timeStamp = date.time
            transactionDate = SimpleDateFormat("yyMMdd", Locale.getDefault()).format(Date())
            transactionTime = SimpleDateFormat("HHmmss", Locale.getDefault()).format(date)
            this.date = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date())
            time = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(date)
        }

        mid = ir.isoMap[42]?.parseRaw2String() ?: ""

        roc = ir.isoMap[11]?.rawData ?: ""
        invoiceNumber = ir.isoMap[62]?.parseRaw2String() ?: ""
        aqrRefNo = ir.isoMap[31]?.parseRaw2String() ?: ""
        responseCode = ir.isoMap[39]?.parseRaw2String() ?: ""

        referenceNumber = (ir.isoMap[37]?.parseRaw2String() ?: "").replace(" ", "")

        authCode = (ir.isoMap[38]?.parseRaw2String() ?: "").replace(" ", "")

        tid = ir.isoMap[41]?.parseRaw2String() ?: ""
        discaimerMessage = ipt.volletIssuerDisclammer

        message = ir.isoMap[59]?.parseRaw2String() ?: ""

        track2Data = ir.isoMap[57]?.parseRaw2String() ?: ""
        indicator = ir.isoMap[58]?.parseRaw2String() ?: ""

        addField61ToBatch(ipt, this)

        bankCode = AppPreference.getBankCode()


        hasPromo = ir.isoMap[63]?.parseRaw2String() == "1"

    }

}


/*


//region=========getting data for Pending preauth===============


fun doPendingPreauth(onResult: (Boolean, Any) -> Unit) {

    GlobalScope.launch {
        val tpt = TerminalParameterTable.selectFromSchemeTable()
        val issuer = AppPreference.getString(AppPreference.CRDB_ISSUER_ID_KEY)
        val ipt = IssuerParameterTable.selectFromIssuerParameterTable(issuer)

        if (tpt == null) {
            onResult(false, "Terminal parameter table error. Please initialize the terminal again")
        } else if (ipt == null) {
            onResult(
                false,
                "Issuer parameter table error. Please initialize the terminal again and set the issuer in env params."
            )
        } else {

            val mIso = IsoDataWriter()
            val list = mutableListOf<ItemPreauth>()
            var counter = 0
            with(mIso) {

                mti = Mti.PRE_AUTH_MTI.mti

                addField(3, ProcessingCode.PENDING_PREAUTH.code)
                addField(4, "00")
                // adding ROC (11) time(12) and date(13)
                addField(11, ROCProvider.getRoc().toString())
                addIsoDateTime(mIso)

                addField(24, Nii.DEFAULT.nii)

                addFieldByHex(41, tpt.terminalId)
                addFieldByHex(42, tpt.merchantId)

               addFieldByHex(48, Field48ResponseTimestamp.getF48Data())

                addFieldByHex(60, addPad(tpt.batchNumber, "0", 6))
                val f61 = getField61(ipt)
                addFieldByHex(61, f61)
                addFieldByHex(62, counter.toString())

            }

            val sc = ServerCommunicator()
            if (sc.open()) {
                while (true) {
                    val data = mIso.generateIsoByteRequest()

                    val resp = sc.sendData(data)

                    if (resp.isNotEmpty()) {

                        val resIso = readIso(resp, false)

                        //======== Adding ROC =======
                        val roc = resIso.isoMap[11]?.rawData
                        if (roc != null) ROCProvider.incrementFromResponse(roc) else ROCProvider.increment()

                        val f58 = resIso.isoMap[58]?.parseRaw2String() ?: ""
                        val f39 = resIso.isoMap[39]?.parseRaw2String() ?: ""
                        if (f39 == "00") {
                            val f62 = resIso.isoMap[62]?.parseRaw2String() ?: ""
                            val f62Arr = f62.split("|")
                            if (f62Arr.size >= 2) {
                                counter = f62Arr[1].toInt()

                                for (e in 2..(f62Arr.lastIndex)) {
                                    if (f62Arr[e].isNotEmpty()) {
                                        val ip = ItemPreauth()
                                        ip.parse(f62Arr[e])
                                        list.add(ip)
                                    }
                                }

                                if (f62Arr[0] != "0") {
                                    mIso.addFieldByHex(62, counter.toString())
                                } else {
                                    onResult(true, list)
                                    break
                                }
                            } else {
                                onResult(false, "F62 Response error")
                                break
                            }
                        } else {
                            onResult(false, f58)
                            break
                        }

                    } else {
                        onResult(false, "No response from server")
                        break
                    }
                }//end of while
                logger("Server communicator", "======Closing Communicator")
                sc.close()
            } else onResult(false, "Unable to connect!!!")

        }

    }

}

//endregion

//region===========Check GCC==================
*/
/**
 * iso will have  Field 4, Field 41, Field 42, Field 22, Field 57, Field 58, Field 60, Field 61, Field 62.
 * Field 3,11, 12, 13, 24  will be added in this function.
 * onResult(true, "") = no gcc
 * onResult(true, msgF60) = gcc
 * *//*

fun checkGcc(iso: IsoDataWriter, onProgress: OnProgress, onResult: (Boolean, Any) -> Unit) {

    checknPerformReversal(onProgress) {
        if (it) {
            GlobalScope.launch {
                val sc = ServerCommunicator()

                if (sc.open()) {

                    iso.apply {

                        mti = Mti.PRE_AUTH_MTI.mti

                        addField(3, ProcessingCode.GCC.code)

                        addField(11, ROCProvider.getRoc().toString())
                        //12, 13
                        addIsoDateTime(iso)

                        addField(24, Nii.DEFAULT.nii)

                    }

                    val data = iso.generateIsoByteRequest()
                    val resp = sc.sendData(data)

                    if (resp.isNotEmpty()) {
                        val isoReader = readIso(resp, false)

                        val roc = isoReader.isoMap[11]?.rawData
                        if (roc != null) ROCProvider.incrementFromResponse(roc) else ROCProvider.increment()

                        val f60 = isoReader.isoMap[60]?.parseRaw2String() ?: ""
                        try {
                            if (f60[7] == '1') {
                                val msg = f60.substring(8, f60.length)
                                onResult(true, msg)
                            } else {
                                onResult(true, "")
                            }
                        } catch (ex: Exception) {
                            onResult(true, "")
                        }

                    } else {
                        onResult(false, "Empty Response Error!!!")
                    }

                    sc.close()
                } else {
                    onResult(false, "Connection Error!!!")
                }

            }
        } else {
            onResult(false, "Reversal Error!!!")
        }
    }

}

//endregion


//region==================Offline Transactions=================


suspend fun doOfflineTransaction(
    data: HashMap<String, String>,
    tpt: TerminalParameterTable,
    onResult: (Boolean, Any) -> Unit
) {

    val ost = OfflineSaleTable().apply {
        batchNo = tpt.batchNumber.toInt()
        invoiceNo = tpt.invoiceNumber.toInt()
        roc = ROCProvider.getRoc()
        time = System.currentTimeMillis()

        tid = tpt.terminalId
        mid = tpt.merchantId

        amount = data["amt"]?.toLong() ?: 0

    }

    val pan = data["pan"] ?: ""
    val maskedPan = getMaskedPan(tpt, pan) ?: ""

    var expiry = data["expiry"] ?: ""
    val y = expiry.substring(2..3)
    val m = expiry.substring(0..1)

    expiry = "$y$m"

    val apprCode = data["apprCode"] ?: ""
    ost.apprCode = apprCode
    ost.maskedPan = maskedPan

    val f57 = "02,14,38|$pan|$expiry|$apprCode"

    val f57Encript = getF57Bytes(f57)

    if (f57Encript != null) {
        ost.encriptedF57 = f57Encript.byteArr2HexStr()

        ROCProvider.increment()
        val inv = tpt.invoiceNumber.toInt() + 1
        tpt.invoiceNumber = addPad(inv, "0", 6)


        val cdt = CardDataTable.selectFromCardDataTable(pan)
        if (cdt != null) {
            var str = AppPreference.getString(AppPreference.CRDB_ISSUER_ID_KEY)
            if (str.isEmpty()) {
                str = "53"
            }
            val ipt = IssuerParameterTable.selectFromIssuerParameterTable(str)
            if (ipt != null) {

                val cardIndFirst = "0"
                val firstTwoDigitFoCard = pan.substring(0, 2)
                val cdtIndex = cdt.cardTableIndex    ///"1"//
                val accSellection =
                    addPad(AppPreference.getString(AppPreference.ACC_SEL_KEY), "0", 2) //cardDataTable.getA//"00"

                val indicator =
                    "$cardIndFirst|$firstTwoDigitFoCard|$cdtIndex|$accSellection"//used for visa// used for ruppay//"0|54|2|00"

                ost.f58 = indicator
                val f61 = getField61(ipt)
                ost.f61 = f61

                TerminalParameterTable.performOperation(tpt) {}
                OfflineSaleTable.insertOrUpdate(ost)
                val resp = printOfflineSale(tpt, ost)
                onResult(
                    true,
                    if (resp) ost else "${VerifoneApp.appContext.getString(R.string.offline_success_transaction)}\n${VerifoneApp.appContext.getString(
                        R.string.printing_error
                    )}\n${VerifoneApp.appContext.getString(R.string.print_last_receipt)}"
                )
            } else {
                onResult(false, "ERROR in IPT Table!!!")
            }
        } else {
            onResult(false, "ERROR in CDT Table!!!")
        }

    } else {
        onResult(false, "F57 encrypting error!!!")
    }

}


suspend fun uploadOfflineSale(data: List<OfflineSaleTable> = listOf()): Boolean {

    var result = true

    val dataList = if (data.isEmpty()) {
        OfflineSaleTable.selectFromProductCategoryTable()
    } else data

    if (dataList.isEmpty()) return true

    val sc = ServerCommunicator()
    if (sc.open()) {
        val iDW = IsoDataWriter().apply {

            mti = Mti.DEFAULT_MTI.mti

            addField(3, ProcessingCode.OFFLINE_SALE.code)
            addField(22, ECardSaleType.OFFLINE_SALE.posEntryValue.toString())

            addField(24, Nii.DEFAULT.nii)

        }
        val timeFormatter = SimpleDateFormat("ddMM HHmmss")

        val tpt = TerminalParameterTable.selectFromSchemeTable()

        var iss = AppPreference.getString(AppPreference.CRDB_ISSUER_ID_KEY)
        if (iss.isEmpty()) {
            iss = "53"
        }

        val ipt = IssuerParameterTable.selectFromIssuerParameterTable(iss)

        for (each in dataList) {

            iDW.addField(4, each.amount.toString())

            val timeArr = timeFormatter.format(Date(each.time)).split(" ")

            with(iDW) {
                addField(11, each.roc.toString())
                addField(12, timeArr[1])
                addField(13, timeArr[0])

                addFieldByHex(41, each.tid)
                addFieldByHex(42, each.mid)

               addFieldByHex(48, Field48ResponseTimestamp.getF48Data())

                val f57Arr = each.encriptedF57.hexStr2ByteArr()

                addField(57, f57Arr, true)

                addFieldByHex(58, each.f58)
                addFieldByHex(60, addPad(each.batchNo, "0", 6))
                addFieldByHex(61, each.f61)
                addFieldByHex(62, addPad(each.invoiceNo, "0", 6))
            }

            val respStr = sc.sendData(iDW.generateIsoByteRequest())
            if (respStr.isNotEmpty()) {  // Parsing and adding data to batch

                val res = readIso(respStr, false)

                if (res.isoMap[39]?.parseRaw2String() == "00") {

                    val batch = BatchFileDataTable().apply {
                        cardNumber = each.maskedPan

                        time = timeArr[1]
                        date = timeArr[0]
                        expiry = "XX/XX"

                    }

                    batch.transactionType = TransactionType.OFFLINE_SALE.ordinal
                    val resAmt: String = res.isoMap[4]?.rawData ?: each.amount.toString()
                    batch.transactionalAmmount = resAmt.toLong().toString()

                    batch.tipAmmount = "0"
                    batch.totalAmmount = batch.transactionalAmmount

                    batch.posEntryValue = addPad(ECardSaleType.OFFLINE_SALE.posEntryValue, "0", 4)

                    if (tpt != null && ipt != null) {
                        addIso2Batch(batch, res, tpt, ipt)  // Field 61 will automatically added in addIso2BatchFunction
                    }

                    val fullBN = res.isoMap[60]?.parseRaw2String() ?: ""
                    batch.batchNumber = if (fullBN.length > 6) {
                        fullBN.takeLast(6)
                    } else {
                        fullBN
                    }

                    batch.track2Data = each.encriptedF57.hexStr2ByteArr().byteArr2Str()
                    batch.indicator = each.f58
                    batch.operationType = "MANUAL"

                    BatchFileDataTable.performOperation(batch)
                    OfflineSaleTable.delete(each)
                } else {
                    result = false
                }
            } else result = false

        }
    } else result = false


    return result

}


//endregion


//region=========Tip Sale Transaction================
*/
/**
 *
 * *//*

suspend fun doTipSale(tipAmt: Float, batch: BatchFileDataTable, onResult: (Boolean, Any) -> Unit) {
    try {
        var amt = batch.transactionalAmmount.toFloat() / 100
        amt += tipAmt
        var amtStr = "%.2f".format(amt)
        amtStr = amtStr.replace(".", "")
        amtStr = addPad(amtStr, "0", 12)

        val iDw = IsoDataWriter().apply {
            mti = Mti.PRE_AUTH_COMPLETE_MTI.mti
            addField(3, ProcessingCode.TIP_SALE.code)
            addField(4, amtStr)


            addField(22, batch.posEntryValue)
            addField(24, Nii.DEFAULT.nii)

            val f31 = batch.aqrRefNo
            if (f31.isNotEmpty()) {
                addFieldByHex(31, f31)
            }

            addFieldByHex(41, batch.tid)
            addFieldByHex(42, batch.mid)

           addFieldByHex(48, Field48ResponseTimestamp.getF48Data())

            var f54 = "%.2f".format(tipAmt)
            f54 = f54.replace(".", "")
            f54 = addPad(f54, "0", 12) + "2"
            addFieldByHex(54, f54)

            val b = addPad(batch.batchNumber, "0", 6)
            val r = addPad(batch.roc, "0", 6)
            val f56 = "${batch.tid}$b$r${batch.transactionDate}${batch.transactionTime}${batch.authCode}"
            addFieldByHex(56, f56)

            addFieldByHex(57, batch.track2Data)

            val f58 = batch.indicator
            addFieldByHex(58, f58)

            val batchNm = batch.batchNumber
            addFieldByHex(60, batchNm)

            addFieldByHex(61, batch.getField61())

            val invoiceNm = batch.invoiceNumber
            addFieldByHex(62, invoiceNm)

        }
        // Adding ROC and Date and Time
        var rocReq = ROCProvider.getRoc().toString()
        rocReq = addPad(rocReq, "0", 6)
        iDw.addField(11, rocReq)
        addIsoDateTime(iDw)


        val conn = ServerCommunicator()

        if (conn.open()) {
            val data = iDw.generateIsoByteRequest()
            val response = conn.sendData(data)
            val resp = readIso(response, false)

            val roc = resp.isoMap[11]?.rawData
            if (roc != null) ROCProvider.incrementFromResponse(roc) else ROCProvider.increment()

            if (resp.isoMap[39]?.parseRaw2String() == "00") {
                val tip = (tipAmt * 100).toLong()
                batch.tipAmmount = tip.toString()
                batch.totalAmmount = (batch.transactionalAmmount.toLong() + tip).toString()

                batch.aqrRefNo = resp.isoMap[31]?.parseRaw2String() ?: ""
                batch.referenceNumber = (resp.isoMap[37]?.parseRaw2String() ?: "").replace(" ", "")
                batch.authCode = (resp.isoMap[38]?.parseRaw2String() ?: "").replace(" ", "")
                batch.roc = rocReq

                batch.transactionType = TransactionType.TIP_SALE.ordinal
                BatchFileDataTable.performOperation(batch)

                onResult(true, batch)

            } else {
                val msg = resp.isoMap[58]?.parseRaw2String() ?: ""
                onResult(false, msg)
            }

        } else {
            val msg = VerifoneApp.appContext.getString(R.string.connection_error)
            onResult(false, msg)
        }

    } catch (ex: Exception) {
        val msg = ex.message ?: ""
        onResult(false, msg)
    }
}

//endregion*/
