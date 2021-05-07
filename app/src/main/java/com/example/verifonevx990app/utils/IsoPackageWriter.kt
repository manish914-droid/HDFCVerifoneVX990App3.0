package com.example.verifonevx990app.utils


import android.content.Context
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import com.example.verifonevx990app.database.*
import com.example.verifonevx990app.utils.HexStringConverter.*
import com.example.verifonevx990app.vxUtils.readFile
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers
import java.io.InputStream
import java.io.Serializable
import java.io.UnsupportedEncodingException
import java.util.*

@RequiresApi(api = Build.VERSION_CODES.KITKAT)
open class IsoPackageWriter() : Serializable {
    private var context: Context? = null
    private var onXmlDataParsed: OnXmlDataParsed? = null

    var isGccAccepted = false // check to be add in sale package.

    //Bit map in 0's and 1's format
    constructor(context: Context?, onXmlDataParsed: OnXmlDataParsed?) : this() {
        this.context = context
        this.onXmlDataParsed = onXmlDataParsed

        val inputStream = readFile()
        if (inputStream != null) {
            parseXmlData(inputStream)
        }
    }

    constructor(context: Context?, onXmlDataParsed: OnXmlDataParsed?, pcNo: Int) : this(
        context,
        onXmlDataParsed
    ) {
        pcNumber = pcNo.toString()
    }

    constructor(context: Context?) : this() {
        this.context = context
    }

    private var compositeDisposable: CompositeDisposable? = CompositeDisposable()
    var transactionalAmmount: String? = null

    private val bitMapCraeter = ByteArray(64)

    //mti
    var serialNumber: String? = null

    //getter for MTI
    //setter for MTI
    var mti: String? = null
        @Throws(UnsupportedEncodingException::class)
        set(mti) = if (!HexStringConverter.checkFieldByteSize(mti, 2)) {
            field = HexStringConverter.byteShiftOperation(mti, 2)
        } else {
            field = mti
        }
    /*Header data*/
    //getter for serial number
    //setter for serial number

    //getter for source NII
    var sourceNII: String? = null

    //getter for Destonation NII
    var destinationNII: String? = null

    /*end of header data*/
    //to store no of active data
    val isoPackageWriters = ArrayList<IsoFiledLength>()

    //contains XML data field values
    // private HashMap<String, XmlFieldModel> xmlFieldModels;
    private var length: Long = 0
    var processingCode: String = ""


    //setter for source NII
    @Throws(UnsupportedEncodingException::class)
    fun setSourceNII(sourceNII: Int) {
        this.sourceNII = intToHex(sourceNII)
        if (!checkFieldByteSize(this.sourceNII, 2)) {
            this.sourceNII = byteShiftOperation(intToHex(sourceNII), 2)
        } else {
            this.sourceNII = intToHex(sourceNII)
        }

    }


    //setter for destination NII
    @Throws(UnsupportedEncodingException::class)
    fun setDestinationNII(destinationNII: Int) {
        this.destinationNII = intToHex(destinationNII)
        if (!checkFieldByteSize(this.destinationNII, 2)) {
            this.destinationNII = byteShiftOperation(intToHex(destinationNII), 2)
        } else {
            this.destinationNII = intToHex(destinationNII)
        }
    }

    fun parseXmlData(inputStream: InputStream) {
        val disposable = getObservableXmlList(inputStream).subscribeOn(Schedulers.newThread())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe({ retvalue1 ->
                val xmlFieldModels = retvalue1.isoList
                if (xmlFieldModels != null && xmlFieldModels.size > 0)
                    if (onXmlDataParsed != null) {
                        onXmlDataParsed!!.onXmlSuccess(xmlFieldModels)
                    } else
                        onXmlDataParsed?.onXmlError("XML Parsing Error")

            }) { throwable -> throwable.printStackTrace() }
//
        compositeDisposable!!.add(disposable)

    }

    private fun getObservableXmlList(inputStream: InputStream): Observable<XMLHelper> {
        return Observable.create { emitter ->
            try {

                val helper = XMLHelper(inputStream)
                helper.get()
                emitter.onNext(helper)
                emitter.onComplete()
            } catch (e: Exception) {
                emitter.onError(e)
            }
        }

    }

    //set the fields number and values
    fun setFieldValues(fieldNumber: String, fieldValues: String) {
        val isoFiledLength = IsoFiledLength()
        isoFiledLength.fieldNumber = fieldNumber
        isoFiledLength.fieldValues = fieldValues
        isoPackageWriters.add(isoFiledLength)


    }


    //observer to create package
    fun observerToCreatePackage(
        transactionType: Int,
        xmlFieldModels: HashMap<String, XmlFieldModel>
    ): Observable<StringBuilder> {
        return Observable.create { emitter ->
            try {
                emitter.onNext(createPackageData(transactionType, xmlFieldModels))
                emitter.onComplete()
            } catch (e: Exception) {
                emitter.onError(e)
            }
        }
    }


    //creates the package
    private fun createPackageData(
        transactionType: Int,
        xmlFieldModels: HashMap<String, XmlFieldModel>
    ): StringBuilder {
        val transactionHandling = TransactionHandling()
        for (isoFiledLength in isoPackageWriters) {
            val xmlFieldModel = xmlFieldModels[isoFiledLength.fieldNumber]
            //enabling bitmap
            if (xmlFieldModel != null) {
                bitMapCraeter[Integer.parseInt(xmlFieldModel.id) - 1] = 1
            }
            transactionHandling.fieldNumber(
                Integer.parseInt(xmlFieldModel!!.id),
                isoFiledLength,
                transactionType,
                true
            )
        }
        length = transactionHandling.length
        setHraderANDMTIBITMAPBasedOnTransactionType(transactionHandling.stringBuilder)
        return transactionHandling.stringBuilder
    }

    ///set Header based of Transation Type
    @Throws(UnsupportedEncodingException::class)
    private fun setHraderANDMTIBITMAPBasedOnTransactionType(stringBuilder: StringBuilder) {
        //when (transactionType) {
        stringBuilder.insert(
            0,
            byteShiftOperation(HexStringConverter.binaryToString(bitMapCraeter), 8)
        )
        length += 8
        //String prefiBitMti=addPreFixer(getMti(),8);
        stringBuilder.insert(0, mti)
        length += 2
        stringBuilder.insert(0, destinationNII)
        stringBuilder.insert(0, sourceNII)
        stringBuilder.insert(0, serialNumber)
        length += 5
        //if (!checkFieldByteSize(String.valueOf(length), 2)) {
        val prefixedValue: String = addPreFixer(length.toString(), 4)
        stringBuilder.insert(0, byteShiftOperation(prefixedValue, 2))
        Log.e(" package data ", "" + stringBuilder)

    }


    fun getProccingCode(): String? {
        return processingCode
    }

    var tid: String = ""
    var mid: String? = null
    var batchNumber: String? = null
    var roc: String? = null
    var invoiceNumber: String? = null
    var panNumber: String = ""
    var time: String? = null
    var date: String? = null
    var expiryDate: String = ""
    var cardHolderName: String = ""
    var timeStamp: Long = 0
    var genratedPinBlock: String = ""//=ByteArray(8)
    var filed55Data: String = ""
    var track2Data: String = ""
    var transactionType: Int = 0
    var applicationPanSequenceNumber: String? = null
    var nii: String? = null
    var indicator: String = ""
    var bankCode: String = ""
    var customerId: String = ""
    var walletIssuerId: String = ""
    var connectionType: String = ""
    var modelName: String = ""
    var appName: String = ""
    var appVersion: String = ""
    var pcNumber: String = ""
    var pcNumber2: String = ""
    var posEntryValue: String? = null
    var terminalSerialNumber: String = ""

    var field58 = ""

    fun setSerialNumber(i: Int) {
        //this.serialNumber = intToHex(i)
        if (!checkFieldByteSize(i.toString(), 1)) {
            this.serialNumber = byteShiftOperation(i.toString(), 1)
        } else {
            this.serialNumber = i.toString()
        }
    }

    private var isPinVerified: Boolean = false
    fun setPinVerified(isPinVerified: Boolean) {
        this.isPinVerified = isPinVerified

    }

    fun isPinVerified(): Boolean {
        return isPinVerified
    }

    //   var trackOneData: TrackOneData? = null
    //  var trackTwoData: TrackTwoData? = null
    var cardIndFirst: String = ""
    var firstTwoDigitFoCard: String = ""
    var cdtIndex: String = ""
    var accSellection: String = ""
    var field56: String = ""
    var cashBackAmount: String = ""
    var operationType: String = ""
    var field39: String = ""
    var field48: String = ""
}


