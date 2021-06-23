@file:JvmName("Converter")

package com.example.verifonevx990app.vxUtils

import android.text.TextUtils
import kotlin.experimental.and
import kotlin.experimental.or

/**
 * @author Digvijay Singh
 * @modified 8th Sept 2018
 * @usedFor BonusHub
 * */

/**
 * Converts hex String into ByteArray
 * hex string of two chars have max  int posEntryValue 255 and occupies 1 byte
 * posEntryValue varies from -128 to 127
 * */

fun String.hexStr2ByteArr(): ByteArray {
    val arr = ByteArray(this.length / 2)
    val ca: CharArray = this.toCharArray()

    var index = 0
    while (index < ca.size) {
        val a = hex2Int(ca[index])   // tenth
        val b = hex2Int(ca[index + 1]) //Once
        arr[index / 2] = ((a shl 4) + b).toByte()
        index += 2
    }

    return arr
}

/**
 * Converts byte (-128 to 127) into hex String with two chars
 * */
fun ByteArray.byteArr2HexStr(): String {
    val bu = StringBuilder()
    for (d in this) {
        val dI = d.toUnsigned()
        val be = dI shr 4 // calc for big endian
        val se = dI and 15 // calc for small endian (15 = 0000 1111)
        bu.append(intCharMap(be))
        bu.append(intCharMap(se))
    }

    return bu.toString()
}

fun ByteArray.byteArr2HexStr(len: Int): String {
    val ba = if (len < size) ByteArray(len) { this[it] } else this
    return ba.byteArr2HexStr()
}


private fun intCharMap(int: Int): Char {
    return when (int) {
        0 -> '0'
        1 -> '1'
        2 -> '2'
        3 -> '3'
        4 -> '4'
        5 -> '5'
        6 -> '6'
        7 -> '7'
        8 -> '8'
        9 -> '9'
        10 -> 'A'
        11 -> 'B'
        12 -> 'C'
        13 -> 'D'
        14 -> 'E'
        15 -> 'F'
        else -> '?'
    }
}

/**
 * Converts byte into unsigned int
 * */
fun Byte.toUnsigned(): Int {
    val x = this.toInt()
    return (x shl 24) ushr 24
}

private fun hex2Int(hex: Char): Int {
    return when (hex) {
        in 'a'..'z' -> (hex - 'a' + 10)
        in 'A'..'Z' -> (hex - 'A' + 10)
        in '0'..'9' -> (hex - '0')
        else -> 0
    }
}

fun hex2byte(hex: Char): Byte {
    return hex2Int(hex).toByte()
}

/**
 * Converts string into hex string
 * */
fun String.str2HexStr(): String = this.str2ByteArr().byteArr2HexStr()


/**
 * Converts string into byte array
 * */
fun String.str2ByteArr(): ByteArray {
    val cArray = this.toCharArray()
    val bA = ByteArray(cArray.size)
    for (index in cArray.indices) {
        bA[index] = cArray[index].toByte()
    }
    return bA
}

/**
 * Converts byteArray into String
 * */
fun ByteArray.byteArr2Str(): String {
    val builder = StringBuilder()
    for (each in this) {
        builder.append(each.toChar())
    }
    return builder.toString()
}

fun Int.toHexString(): String {
    var byteArr = ByteArray(4)
    for (index in 0..3) {
        byteArr[index] = ((this shl (8 * index)) shr 24).toByte()
    }

    byteArr = trimByteArray(byteArr)

    return byteArr.byteArr2HexStr()
}


fun hexString2Int(str: String): Int {
    val arr = str.hexStr2ByteArr()
    var result = 0
    for (e in arr.indices) {
        val r = arr[e].toUnsigned() shl (8 * (arr.size - e - 1))
        result = result or r
    }
    return result
}

fun hexString2String(str: String): String {
    return (str.hexStr2ByteArr()).byteArr2Str()
}


/**
 * trims the starting bits having posEntryValue 0
 * */
fun trimByteArray(arg: ByteArray): ByteArray {
    val arLi = mutableListOf<Byte>()
    arg.forEach { arLi.add(it) }
    fun helper() {
        if (arLi[0] == 0.toByte()) {
            arLi.removeAt(0)
            helper()
        }
    }
    helper()
    val b = ByteArray(arLi.size)
    for (i in b.indices) b[i] = arLi[i]
    return b
}


/**
 * Parses the tlv into and store into Hash map of Int and String
 * @param data is the input hex string data
 * @param map is output data where parsed data will get stored. Int will be tvl tag and String will be tlv parsed hex string posEntryValue.
 * */
fun tlvParser(data: String, map: HashMap<Int, String>) {
    val tagList = setOf(
        "8A",
        "89",
        "91",
        "71",
        "72",
        "9F26",
        "9F10",
        "9F37",
        "9F36",
        "95",
        "9A",
        "9C",
        "9B",
        "9F02",
        "5F2A",
        "9F1A",
        "82",
        "84",
        "5F34",
        "9F27",
        "9F33",
        "9F34",
        "9F35",
        "9F03",
        "9F47",
        "9F06",
        "9F22",
        "DF05",
        "DF06",
        "DF02",
        "DF03",
        "DF04"
    )
    var pointer = 0
    fun reader() {
        if (pointer < data.length) {
            var temp = data.substring(pointer, pointer + 2)
            if (tagList.contains(temp)) {
                pointer += 2
            } else {
                temp = data.substring(pointer, pointer + 4)
                pointer += 4
            }
            val lenStr = data.substring(pointer, pointer + 2)
            pointer += 2

            val len = Integer.decode("0x$lenStr") * 2
            val key = Integer.decode("0x$temp")

            if (len != 0) {
                val value = data.substring(pointer, pointer + len)
                pointer += len
                map[key] = value
            } else {
                map[key] = ""
            }
            reader()
        }
    }

    reader()
}

/**
 *@param input input data String which needs to modified
 * @param padChar padding Char to be added on left or right
 * @param totalLen maximum length of output. if input length is greater than total len same will be returned
 * @param toLeft extra char to be added on start or on end of String
 *  */
fun addPad(input: String, padChar: String, totalLen: Int, toLeft: Boolean = true): String {
    return if (input.length >= totalLen) {
        input
    } else {
        val sb = StringBuilder()
        val remaining = totalLen - input.length
        if (toLeft) {
            for (e in 1..remaining) {
                sb.append(padChar)
            }
            sb.append(input)
        } else {
            sb.append(input)
            for (e in 1..remaining) {
                sb.append(padChar)
            }
        }
        sb.toString()
    }
}

//Below method is used to get substring value of tvr and aid value:-
fun getValueOfTVRAndAID(tvr: String, aid: String, tsi: String): Triple<String, String, String> {
    var data: Triple<String, String, String>? = null
    return if (!TextUtils.isEmpty(tvr) && !TextUtils.isEmpty(aid)) {
        data = Triple(
            tvr.subSequence(4, tvr.length).toString(),
            aid.subSequence(6, aid.length).toString(),
            tsi.subSequence(4, tsi.length).toString()
        )
        data
    } else {
        data = Triple("", "", "")
        data
    }
}

//Below we are manipulating Field55 and get TC Data through TAG91 data of Field55:-
fun tcDataFromField55(data: IsoDataReader): String {
    val field55 = data.isoMap[55]?.rawData ?: ""
    //println("Filed55 value is --> $field55")
    val f55Hash = HashMap<Int, String>()
    tlvParser(field55, f55Hash)
    val tag8A = 0x8A
    val tag91 = 0x91   //71,72

    var tcData = f55Hash[tag91] ?: ""
    return if (!TextUtils.isEmpty(tcData)) {
        tcData
    } else {
        tcData = ""
        tcData
    }
}


/**
 * Overloading of addPad with Int type args
 * */
fun addPad(input: Int, padChar: String, totalLen: Int, toLeft: Boolean = true): String =
    addPad(input.toString(), padChar, totalLen, toLeft)

/**
 * @param amount is principle amount
 * @param rate is annual rate of interest for emi
 * @param nom is nom of months for emi
 * this function calculates the emi according to flat basis
 * */
fun flatEmi(amount: Long, rate: Float, nom: Int): Float =
    (amount.toFloat() / (1200 * nom)) * (1200 + (rate * nom))

/**
 * @param amount is principle amount
 * @param rate is annual rate of interest for emi
 * @param nom is nom of months for emi
 * this function calculates the emi according to reducing bases of emi
 * */
fun reducingBalanceEmi(amount: Float, rate: Float, nom: Int): Float {
    val periodInterest = rate / 1200
    val p1: Float = amount * periodInterest
    val f = Math.pow((1 + periodInterest).toDouble(), nom.toDouble()).toFloat()
    val p2: Float = f
    val p3: Float = f - 1
    return p1 * p2 / p3
}


/**
 * @param input input data for parameter
 * @param maxLength maximum length of character in line
 * this method splits the string in accordingly to max len of line
 * */


fun wrapText(input: String, maxLength: Int): String {
    val cIn = input.split(" ")
    val output = StringBuilder()
    var pointer = 0
    fun helper() {
        if (pointer < cIn.size - 1) {
            val start = pointer
            var len = 0
            try {
                while (len < maxLength) {
                    len += cIn[pointer].length + 1  // plus one is for extra space
                    pointer++
                }
            } catch (ex: Exception) {
            }
            pointer -= 2
            for (x in start..pointer) {
                output.append(cIn[x])
                if (x != pointer)
                    output.append(" ")
            }
            output.appendLine()
            pointer++
            helper()
        }
    }

    helper()
    return output.toString()
}


fun String.intStr2ByteArr(): ByteArray {
    val len = this.length / 2
    val result = ByteArray(len)
    for ((pointer, i) in (0 until len).withIndex()) {
        val x = this.substring(pointer * 2, (pointer + 1) * 2)
        result[i] = x.toByte()
    }
    return result
}


fun ByteArray.byteArr2IntStr(): String {
    val sb = StringBuilder()

    for (b in this) {
        sb.append(addPad(b.toInt(), "0", 2))
    }
    return sb.toString()
}


fun str2NibbleArr(numString: String, cvmLimitStatus: Boolean = false): ByteArray {
    var data = numString
    if (cvmLimitStatus) {
        data = "0${data}0"
    }
    val len = data.length / 2
    val result = ByteArray(len)
    var pointer = 0

    while (pointer < len) {
        val n = data.substring(pointer * 2, (pointer * 2) + 2)
        val high = (n[0].toInt() shl 4).toByte()
        val low = n[1].toByte() and 0xf
        result[pointer] = high or low
        pointer++
    }
    return result
}

fun String.str2NibbleArray(): ByteArray = str2NibbleArr(this)


fun addBytePad(src: ByteArray, len: Int, dest: Byte = 0, isLeft: Boolean = true): ByteArray {
    val diff = len - src.size
    if (diff > 0) {
        val list = mutableListOf<Byte>()
        if (isLeft) {
            for (e in 0 until diff) {
                list.add(dest)
            }
            list.addAll(src.toList())
        } else {
            list.addAll(src.toList())
            for (e in 0 until len) {
                list.add(dest)
            }
        }
        return list.toByteArray()
    } else {
        return src
    }


}


/**
 * Convert hex String to byte array  (Added by lucky)
 *
 */
fun String.decodeHexStringToByteArray(): ByteArray {
    require(length % 2 == 0) { "Must have an even length" }

    return chunked(2)
        .map { it.toInt(16).toByte() }
        .toByteArray()
}
