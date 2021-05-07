package com.example.verifonevx990app.transactions


import com.example.verifonevx990app.realmtables.BatchFileDataTable
import java.io.Serializable
import java.util.regex.Matcher
import java.util.regex.Pattern

open class Track3 : Serializable {
    var rawT3Data = ""
        set(value) {
            if (value.isNotEmpty()) field = value
        }

    var discretionaryData = ""
        set(value) {
            if (value.isNotEmpty()) field = value
        }
}

open class Track2 : Track3(), Serializable {
    var label = ""
        set(value) {
            if (value.isNotEmpty()) {
                field = value
            }
        }


    var pan = ""
        set(value) {
            if (value.isNotEmpty()) {
                field = value
            }
        }

    var expDate = ""
        set(value) {
            if (value.isNotEmpty()) {
                field = value
            }
        }

    var serviceCode = ""
        set(value) {
            if (value.isNotEmpty()) {
                field = value
            }
        }

    var rawData = ""
        set(value) {
            if (value.isNotEmpty()) {
                field = value
            }
        }

    fun updateTrack3(track3: Track3) {
        rawT3Data = track3.rawT3Data
        discretionaryData = track3.discretionaryData
    }

}

open class Track1 : Track2(), Serializable {
    var name = ""
    var formatCode = ""

    fun updateTrack2(track2: Track2) {
        pan = track2.pan
        expDate = track2.expDate
        serviceCode = track2.serviceCode
        rawData = track2.rawData
    }

    fun updateTrack1(track1: Track1) {
        name = track1.name
        formatCode = track1.formatCode
        updateTrack2(track1)
        updateTrack3(track1)
    }

}

fun getTrack1(data: String): Track1? {
    val track1FormatBPattern =
        Pattern.compile("(%?([A-Z])([0-9]{1,19})\\^([^\\^]{2,26})\\^([0-9]{4}|\\^)([0-9]{3}|\\^)?([^\\?]+)?\\??)([\t\n\r ]{0,2})(.*)")
    val t1Data = Track1().apply {
        rawData = data
    }
    val matcher =
        track1FormatBPattern.matcher(data.replace("\\u0000".toRegex(), "").trim { it <= ' ' })
    return if (matcher.matches()) t1Data.apply {
        formatCode = getGroup(matcher, 2)
        pan = getGroup(matcher, 3)
        name = getGroup(matcher, 4)
        expDate = getGroup(matcher, 5)
        serviceCode = getGroup(matcher, 6)
        discretionaryData = getGroup(matcher, 7)
    } else null

}

fun getTrack2(data: String): Track2? {
    val track2Pattern = Pattern.compile("([\\d]{1,19}+)=([\\d]{0,4}|\\^)([\\d]{0,3}|\\^)(.*)")
    val matcher = track2Pattern.matcher(data.replace("\\u0000".toRegex(), "").trim { it <= ' ' })
    val t2data = Track2().apply {
        rawData = data
    }
    return if (matcher.matches()) t2data.apply {
        pan = (getGroup(matcher, 1))
        expDate = (getGroup(matcher, 2))
        serviceCode = (getGroup(matcher, 3))
        discretionaryData = (getGroup(matcher, 4))
    } else null
}

fun getTrack3(data: String): Track3? {
    val track3Pattern = Pattern.compile(".*?[\t\n\r ]{0,2}(\\+(.*)\\?)")
    val t3data = Track3()
    val matcher = track3Pattern.matcher(data.replace("\\u0000".toRegex(), "").trim { it <= ' ' })
    return if (matcher.matches()) t3data.apply {
        rawT3Data = getGroup(matcher, 1)
        discretionaryData = getGroup(matcher, 2)
    } else null
}

private fun getGroup(matcher: Matcher, group: Int): String {
    val groupCount = matcher.groupCount()
    return if (groupCount > group - 1) {
        matcher.group(group)
    } else {
        ""
    }
}


class MagState : Serializable {
    var isMagSearchActive = false
    var isReaderOpen = false

    var isReadComplete = false

    var pollResult = -1

}

class EmvState : Serializable {

    var isFallback = false

    var tvr = byteArrayOf()
    var tsi = byteArrayOf()
    var expiry = byteArrayOf()
    var ecBalance = byteArrayOf()
    var csn = byteArrayOf()

    var aid = byteArrayOf()
    var tc = byteArrayOf()

    var prompOfflineAuth = false

    var batchData = BatchFileDataTable()

    var isTransactionDone = false

    var transactionRespCode = -1

    var currentErrorMsg = ""


    var isOnlineSuccess = false

    var isAutoSettle = false


    fun clear() {
        tvr = byteArrayOf()
        tsi = byteArrayOf()
        expiry = byteArrayOf()
        ecBalance = byteArrayOf()
        csn = byteArrayOf()
        aid = byteArrayOf()
        tc = byteArrayOf()
        prompOfflineAuth = false
        batchData = BatchFileDataTable()
        isOnlineSuccess = false
        isAutoSettle = false
    }

}