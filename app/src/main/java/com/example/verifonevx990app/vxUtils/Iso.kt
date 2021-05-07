package com.example.verifonevx990app.vxUtils

/**
 * @author Ajay Thakur
 * @modified 17th June 2020
 * @usedFor BonusHub
 * */

import java.io.Serializable
import java.text.SimpleDateFormat
import java.util.*
import kotlin.experimental.and
import kotlin.experimental.inv
import kotlin.experimental.or

/**
 * Used to differentiate between fixed len and variable len types
 * BCD is for Binery Decimal code, length is fixed
 * BYTE is for simple byte array, length is fixed
 * LLVR Length is not fixed, its length is defined in BCD and rest data is in BYTE
 * */
enum class ISO_FIELD_TYPE { BCD, LLVR, BYTE }


/**
 * Abstration for ISO Field
 * @param fieldNo define field no very imp parameter
 * @param fieldName defines the name of the field, its use is for read while debugging
 * @param isFixedLen defines if the stored data is having BCD and LLVR
 * @param fieldType similar use of isFixedLen
 * */
abstract class AbstractIsoField(
    val fieldNo: Byte, val fieldName: String = "",
    val isFixedLen: Boolean = false, val fieldType: ISO_FIELD_TYPE = ISO_FIELD_TYPE.BCD
)


/**
 * @property len is the length of data for BCD type and
 * length of data length for LLVR type
 * default posEntryValue for len =2 for llvr type
 * if field is bcd make sure len param is definately passed
 * @property rawData it stores the iso data into form of hex string
 *
 * */
class IsoField private constructor(
    fieldNo: Byte, fieldName: String = "",
    isFixedLen: Boolean = false, fieldType: ISO_FIELD_TYPE = ISO_FIELD_TYPE.BCD
) : AbstractIsoField(fieldNo, fieldName, isFixedLen, fieldType) {
    var len: Int = 0
    var rawData: String = ""


    constructor(
        fieldNo: Byte, fieldName: String = "", isFixedLen: Boolean = false,
        fieldType: ISO_FIELD_TYPE = ISO_FIELD_TYPE.BCD, len: Int = 2
    ) : this(fieldNo, fieldName, isFixedLen, fieldType) {
        this.len = len
    }

    constructor() : this(0, "Not in use")

    companion object {
        fun getIsoField(fieldNo: Byte, isReq: Boolean = true): IsoField {
            return when (fieldNo) {
                2.toByte() -> IsoField(2, "PAN / Mobile", true, len = 8)
                3.toByte() -> IsoField(3, "Processing Code", true, len = 3)
                4.toByte() -> IsoField(4, "Transaction Amount", true, len = 6)
                5.toByte() -> IsoField()
                6.toByte() -> IsoField()
                7.toByte() -> IsoField(7, "Server Transmission DateA nd Time", true, len = 5)
                8.toByte() -> IsoField()
                9.toByte() -> IsoField()
                10.toByte() -> IsoField()

                11.toByte() -> IsoField(11, "STAN", true, len = 3)
                12.toByte() -> if (isReq) IsoField(
                    12,
                    "Local  Transaction Time",
                    true,
                    len = 3
                ) else IsoField(12, "Local  Transaction Date Time", true, len = 6)
                13.toByte() -> IsoField(13, "Local  Transaction Date", true, len = 2)
                14.toByte() -> IsoField(14, "Expiry Date", true, len = 2)
                15.toByte() -> IsoField(15, "Settlement Date", true, len = 2)
                16.toByte() -> IsoField()
                17.toByte() -> IsoField(17, "Effective Date", true, len = 2)
                18.toByte() -> IsoField()
                19.toByte() -> IsoField()
                20.toByte() -> IsoField()

                21.toByte() -> IsoField()
                22.toByte() -> IsoField(22, "POS Code", true, len = 2)
                23.toByte() -> IsoField(23, "Address/Application Sequence Number", true, len = 3)
                24.toByte() -> IsoField(24, "NII", true, len = 2)
                25.toByte() -> IsoField(25)
                26.toByte() -> IsoField(26)
                27.toByte() -> IsoField(27)
                28.toByte() -> IsoField(28)
                29.toByte() -> IsoField(29)
                30.toByte() -> IsoField(30, "Original Amount", true, len = 6)

                31.toByte() -> IsoField(
                    31,
                    "Acquirer Ref No",
                    fieldType = ISO_FIELD_TYPE.LLVR,
                    len = 1
                )
                32.toByte() -> IsoField(
                    32,
                    "Acquiring Institution Id Code",
                    fieldType = ISO_FIELD_TYPE.LLVR,
                    len = 1
                )
                33.toByte() -> IsoField(33)
                34.toByte() -> IsoField(34)
                35.toByte() -> IsoField(35, "Track2", fieldType = ISO_FIELD_TYPE.LLVR, len = 2)
                36.toByte() -> IsoField(36)
                37.toByte() -> IsoField(37, "Retrieval Reference Number", true, len = 12)
                38.toByte() -> IsoField(38, "Approval Code", true, len = 12)
                39.toByte() -> IsoField(39, "Response Code", true, len = 2)
                40.toByte() -> IsoField()

                41.toByte() -> IsoField(41, "TID", true, len = 8)
                42.toByte() -> IsoField(42, "MID", true, len = 15)
                43.toByte() -> IsoField(43)
                44.toByte() -> IsoField(
                    44,
                    "Additional Response Data",
                    fieldType = ISO_FIELD_TYPE.LLVR,
                    len = 2
                )
                45.toByte() -> IsoField(45, "Track1", fieldType = ISO_FIELD_TYPE.LLVR, len = 2)
                46.toByte() -> IsoField(46)
                47.toByte() -> IsoField(
                    47,
                    "User Id, Customer Id",
                    fieldType = ISO_FIELD_TYPE.LLVR,
                    len = 2
                )
                48.toByte() -> IsoField(
                    48,
                    "Connection code and date time stamp",
                    fieldType = ISO_FIELD_TYPE.LLVR,
                    len = 2
                )
                49.toByte() -> IsoField(49, "Transaction Currency Code", true, len = 2)
                50.toByte() -> IsoField(50)

                51.toByte() -> IsoField(51)
                52.toByte() -> IsoField(
                    52,
                    "Pin Block",
                    true,
                    len = 8,
                    fieldType = ISO_FIELD_TYPE.BYTE
                )
                53.toByte() -> IsoField(53, "CVV", fieldType = ISO_FIELD_TYPE.LLVR, len = 2)
                54.toByte() -> IsoField(
                    54,
                    "Additional Amount",
                    fieldType = ISO_FIELD_TYPE.LLVR,
                    len = 2
                )
                55.toByte() -> IsoField(55, "ICC Data", fieldType = ISO_FIELD_TYPE.LLVR, len = 2)
                56.toByte() -> IsoField(
                    56,
                    "Previous ROC, Date, Time in Reversal case",
                    fieldType = ISO_FIELD_TYPE.LLVR,
                    len = 1
                )
                57.toByte() -> IsoField(
                    57,
                    "Track2 Encripted",
                    fieldType = ISO_FIELD_TYPE.LLVR,
                    len = 2
                )
                58.toByte() -> IsoField(
                    58,
                    "Card Indicator and Response Message",
                    fieldType = ISO_FIELD_TYPE.LLVR,
                    len = 2
                )
                59.toByte() -> IsoField(
                    59,
                    "RSA Key in Request, Advice in response",
                    fieldType = ISO_FIELD_TYPE.LLVR,
                    len = 2
                )
                60.toByte() -> IsoField(60, "Batch No", fieldType = ISO_FIELD_TYPE.LLVR, len = 2)

                61.toByte() -> IsoField(
                    61,
                    "Bank Details",
                    fieldType = ISO_FIELD_TYPE.LLVR,
                    len = 2
                )
                62.toByte() -> IsoField(62, "Invoice No", fieldType = ISO_FIELD_TYPE.LLVR, len = 2)
                63.toByte() -> IsoField(
                    63,
                    "Promo Details",
                    fieldType = ISO_FIELD_TYPE.LLVR,
                    len = 2
                )

// below is used to add 56 field with different length
                //  51.toByte() -> IsoField(56, "Previous ROC, Date, Time in Reversal case", fieldType = ISO_FIELD_TYPE.LLVR, len = 2)

                else -> IsoField()
            }
        }
    }

    /**
     * Converts the raw string data into ascii string
     * */
    fun parseRaw2String(): String = rawData.hexStr2ByteArr().byteArr2Str()

    /**
     * Converts the raw string data into byte array
     * */
    fun parseRaw2ByteArr(): ByteArray = rawData.hexStr2ByteArr()
}

/**
 * @param data is Byte Array type expected to have 8 byte len as 64 bit is required
 * @return cA of  CharArray type having length 64, 1 = open bit and 0= closed bit
 * getBitMap calls the checkBit function which is closure and return func which takes bit index and checks
 * if bit is open it returns '1' else return '0'
 * */
fun getBitMap(data: ByteArray): CharArray {
    val cA = CharArray(64) { '0' }
    var index = 0
    for (byte in data) {
        val checker = checkBit(byte)
        for (bit in 0..7) {
            cA[index + 7 - bit] = checker(bit.toByte())
        }
        index += 8
    }
    return cA
}

/**
 * CheckBit is Closure which return function take Byte and return Char
 * @param data whose bits need to be checked.
 * friend function take bit number need to read checks with data and returns '1' and '0'
 * */
fun checkBit(data: Byte): (Byte) -> Char {
    val offBit: Byte = 0
    val bit0: Byte = 1
    val bit1: Byte = 2
    val bit2: Byte = 4
    val bit3: Byte = 8
    val bit4: Byte = 16
    val bit5: Byte = 32
    val bit6: Byte = 64
    val bit7: Byte = 128.toByte()
    fun friend(bit: Byte): Char {
        return when (bit) {
            0.toByte() -> if (data and bit0 != offBit) '1' else '0'
            1.toByte() -> if (data and bit1 != offBit) '1' else '0'
            2.toByte() -> if (data and bit2 != offBit) '1' else '0'
            3.toByte() -> if (data and bit3 != offBit) '1' else '0'
            4.toByte() -> if (data and bit4 != offBit) '1' else '0'
            5.toByte() -> if (data and bit5 != offBit) '1' else '0'
            6.toByte() -> if (data and bit6 != offBit) '1' else '0'
            7.toByte() -> if (data and bit7 != offBit) '1' else '0'
            else -> '0'
        }
    }
    return ::friend
}


/**
 * @param data is input parameter
 * @param ifh is out param,
 * parseBitmap function read the bitmap and set in activeField param of ifh
 * */
fun parseBitmap(data: ByteArray, ifh: IsoDataReader) {
    val activeBit = getBitMap(data)
    for (index in activeBit.indices) {
        if (activeBit[index] == '1') {
            ifh.activeField.add((index + 1).toByte())
        }
    }
}

/**
 * @param data is input parameter
 * @param ifh is out param,
 * parseBitmap function is overloaded to work with input type String
 * */
fun parseBitmap(data: String, ifh: IsoDataReader) {
    parseBitmap(data.hexStr2ByteArr(), ifh)
}

/**
 * @param cA CharArray type expected to have size of 64
 * createBitmap fun read the cA array and create a ByteArray with accordingly on and off bits
 * and then return String from the ByteArray
 * its user bitChanger Closure to modify the bits
 * */
fun createBitmap(cA: CharArray): String {
    val bA = ByteArray(cA.size / 8)
    val chg = bitChanger()
    for (index in bA.indices) {
        val ind = index * 8
        for (i in 0..7) {
            if (cA[ind + i] == '1') {
                bA[index] = chg(bA[index], (7 - i).toByte(), true)
            }
        }
    }
    return bA.byteArr2HexStr()
}

/**
 * @param ifh is used to create the CharArray of '0' and '1' using its activeField parameter
 * and then call its overloaded fun having accepting CharArray
 * */
fun createBitmap(ifh: IsoParent): String {
    val cA = CharArray(64) { '0' }
    for (each in ifh.activeField.sorted()) {
        cA[each.toInt() - 1] = '1'
    }
    return createBitmap(cA)
}


/**
 * bitChanger is Closure
 * @return function which accept Byte (data), Byte(bit) to read, Boolean (modification type) on or off
 * */
fun bitChanger(): (Byte, Byte, Boolean) -> Byte {
    val bit0: Byte = 1
    val bit1: Byte = 2
    val bit2: Byte = 4
    val bit3: Byte = 8
    val bit4: Byte = 16
    val bit5: Byte = 32
    val bit6: Byte = 64
    val bit7: Byte = 128.toByte()

    fun friend(data: Byte, bit: Byte, on: Boolean): Byte {
        return when (bit) {
            0.toByte() -> if (on) data or bit0 else data and bit0.inv()
            1.toByte() -> if (on) data or bit1 else data and bit1.inv()
            2.toByte() -> if (on) data or bit2 else data and bit2.inv()
            3.toByte() -> if (on) data or bit3 else data and bit3.inv()
            4.toByte() -> if (on) data or bit4 else data and bit4.inv()
            5.toByte() -> if (on) data or bit5 else data and bit5.inv()
            6.toByte() -> if (on) data or bit6 else data and bit6.inv()
            7.toByte() -> if (on) data or bit7 else data and bit7.inv()
            else -> data
        }
    }
    return ::friend
}


abstract class IsoParent : Serializable {
    val activeField = mutableSetOf<Byte>()
    val isoMap = HashMap<Byte, IsoField>()
    var mti = ""
}

interface IReader {
    fun parseFields(data: String, isReq: Boolean)
    fun parseFields(data: ByteArray, isReq: Boolean)
}

interface IWriter {
    fun addField(fieldNo: Byte, data: String)
    fun addFieldByHex(fieldNo: Byte, data: String)
    fun addField(fieldNo: Byte, data: ByteArray, isHexType: Boolean)

    //    fun generateIsoRequest(): String
    fun generateIsoByteRequest(): ByteArray

}

/**
 * @property activeField have set of Bytes numbers for active bits
 * @property isoMap is HashMap type, it hold the IsoField data at their field index, it get populate
 * after calling parseFields function.
 *
 * This class is  for reading iso response
 * */


class IsoDataReader : IsoParent(), IReader {

    var srcNii = ""
    var destNii = ""
    var srNo = ""

    /**
     * @param data respose type data, total len , address and mti should be removed before passing the arg
     * data should start from 8 byte bitmap sequence and last to the end.
     * parsing result get stored in isoMap property
     * */
    override fun parseFields(data: String, isReq: Boolean) {
        val bit = data.substring(0, 16)
        parseBitmap(bit, this)

        val isoData = data.substring(16)
        var pointer = 0

        for (bm in activeField) {
            val field = IsoField.getIsoField(bm, isReq)
            if (field.isFixedLen) {
                field.rawData = isoData.substring(pointer, pointer + (field.len * 2))
                pointer += field.len * 2
            } else {
                val l = isoData.substring(pointer, pointer + (field.len * 2)).toInt()
                pointer += field.len * 2
                field.rawData = isoData.substring(pointer, pointer + (l * 2))
                pointer += l * 2
            }
            isoMap[bm] = field
        }

    }

    /**
     * @param data respose type data, total len , address and mti should be removed before passing the arg
     * data should start from 8 byte bitmap sequence and last to the end.
     * parsing result get stored in isoMap property
     * */
    override fun parseFields(data: ByteArray, isReq: Boolean) {
        parseFields(data.byteArr2HexStr(), isReq)
    }


}


class IsoDataWriter : IsoParent(), IWriter, Serializable {

    companion object {
        val TAG = IsoDataWriter::class.java.simpleName
        val srcNii = 91
        val desNii = 1
        val serialNo = 60

        var address = "$serialNo${addPad(srcNii, "0", 4)}${addPad(desNii, "0", 4)}"
    }

    var timeStamp: Long = 0
    val additionalData = mutableMapOf<String, String>()

    override fun addField(fieldNo: Byte, data: String) {
        val iso = IsoField.getIsoField(fieldNo)
        iso.rawData = if (iso.isFixedLen) {
            if (data.length < iso.len * 2) addPad(data, "0", iso.len * 2)
            else data
        } else {
            data
        }
        activeField.add(fieldNo)
        isoMap[fieldNo] = iso
    }


    /**
     * This function will convert the string into hex string
     * */
    override fun addFieldByHex(fieldNo: Byte, data: String) {
        addField(fieldNo, data.str2ByteArr().byteArr2HexStr())
    }


    fun addField56(fieldNo: Byte, data: String) {
        val iso = IsoField(56, "Encipher pan", fieldType = ISO_FIELD_TYPE.LLVR, len = 2)
        iso.rawData = if (iso.isFixedLen) {
            if (data.length < iso.len * 2) addPad(data, "0", iso.len * 2)
            else data
        } else {
            data
        }
        activeField.add(fieldNo)
        isoMap[fieldNo] = iso
    }

    override fun addField(fieldNo: Byte, data: ByteArray, isHexType: Boolean) {
        if (isHexType)
            addField(fieldNo, data.byteArr2HexStr())
        else
            addField(fieldNo, data.byteArr2Str())
    }


    override fun generateIsoByteRequest(): ByteArray {
        val request = mutableListOf<Byte>()

        fun add(items: ByteArray) {
            for (e in items) request.add(e)
        }

        add(addPad(address, "0", 5 * 2).str2NibbleArray()) // address
        add(addPad(mti, "0", 2 * 2).str2NibbleArray())  // mti

        add(createBitmap(this).hexStr2ByteArr()) // Creating ISO8583 Bitmap

        val fields = activeField.sorted()
        for (each in fields) {
            val fie = isoMap[each]
            if (fie != null) {
                if (fie.isFixedLen) {
                    if (fie.fieldType == ISO_FIELD_TYPE.BYTE) {
                        add(fie.rawData.hexStr2ByteArr())
                    } else {
                        if (fie.fieldNo.toString() == "42") {
                            add(fie.rawData.hexStr2ByteArr())
                        } else
                            add(addPad(fie.rawData, "0", fie.len * 2).str2NibbleArray())
                    }
                } else {
                    add(addPad(fie.rawData.length / 2, "0", fie.len * 2).str2NibbleArray())
                    add(fie.rawData.hexStr2ByteArr())
                }
            }
        }

        val len = addPad(request.size, "0", 2 * 2).str2NibbleArray()

        request.add(0, len[1])
        request.add(0, len[0])

        //================ Assigning time stamp
        timeStamp = System.currentTimeMillis()

        return request.toByteArray()
    }

}


/**
 * @param data response data string
 * Read the data and return IsoDataReader
 * */
fun readIso(data: String, isRequest: Boolean = true): IsoDataReader = IsoDataReader().apply {
    // cut the 7 byte from the data, mti = 2 byte and address = 5 byte
    srNo = data.substring(0, 2)
    destNii = data.substring(2, 6)
    srcNii = data.substring(6, 10)

    mti = data.substring(10, 14)
    parseFields(data.substring(14), isRequest)
}

/**
 * It adds date and time field in ISODataWriter object.
 * */
fun addIsoDateTime(iWriter: IsoDataWriter) {
    val dateTime: Long = Calendar.getInstance().timeInMillis
    val time: String = SimpleDateFormat("HHmmss", Locale.getDefault()).format(dateTime)
    val date: String = SimpleDateFormat("MMdd", Locale.getDefault()).format(dateTime)

    with(iWriter) {
        addField(12, time)
        addField(13, date)
    }

}




