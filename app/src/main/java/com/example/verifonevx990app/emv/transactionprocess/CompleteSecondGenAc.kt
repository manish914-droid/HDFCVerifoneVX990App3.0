package com.example.verifonevx990app.emv.transactionprocess

import android.os.Bundle
import android.os.DeadObjectException
import android.os.RemoteException
import android.text.TextUtils
import android.util.Log
import com.example.verifonevx990app.main.CardAid
import com.example.verifonevx990app.main.HostResponseCode
import com.example.verifonevx990app.main.MainActivity
import com.example.verifonevx990app.utils.Utility
import com.example.verifonevx990app.vxUtils.*
import com.example.verifonevx990app.vxUtils.AppPreference.GENERIC_REVERSAL_KEY
import com.example.verifonevx990app.vxUtils.AppPreference.ONLINE_EMV_DECLINED
import com.google.gson.Gson
import com.vfi.smartpos.deviceservice.aidl.IEMV
import com.vfi.smartpos.deviceservice.aidl.OnlineResultHandler
import com.vfi.smartpos.deviceservice.constdefine.ConstIPBOC
import com.vfi.smartpos.deviceservice.constdefine.ConstOnlineResultHandler
import java.text.SimpleDateFormat
import java.util.*
import kotlin.collections.ArrayList
import kotlin.collections.HashMap

class CompleteSecondGenAc(var cardProcessedDataModal: CardProcessedDataModal?,
                          var data: IsoDataReader, var isoData: IsoDataWriter? = null,
                          var printExtraDataSB: (Triple<String, String, String>?,String?) -> Unit) {
    val iemv: IEMV? by lazy { VFService.vfIEMV }

    init {
        performSecondGenAc(cardProcessedDataModal,data)
    }

    //Below method is used to complete second gen ac in case of EMV Card Type:-
    private fun performSecondGenAc(cardProcessedDataModal: CardProcessedDataModal?,data: IsoDataReader) {

        var field56data: String? = null
        var aidstr = cardProcessedDataModal?.getAID() ?: ""

        var  finalaidstr = if(aidstr.isNotBlank()) { aidstr.subSequence(0,10).toString() } else { aidstr = ""}

        try {
            var  cardStatus =  VFService.vfsmartReader?.checkCardStatus()
            VFService.showToast("Card status is"+VFService.vfsmartReader?.checkCardStatus())
        }
        catch (ex: DeadObjectException){
            ex.printStackTrace()
        }
        catch (ex: RemoteException){
            ex.printStackTrace()
        }
        catch (ex: Exception){
            ex.printStackTrace()
        }

        var printData: Triple<String, String, String>? = null
        var de55: String?= null
        var tc = false
        val authCode = data.isoMap[38]?.parseRaw2ByteArr() ?: byteArrayOf()
        if (authCode.isNotEmpty()) {
            val ac = authCode.byteArr2Str().replace(" ", "").str2ByteArr()
        }
        val responseCode = (data.isoMap[39]?.parseRaw2String().toString())
        val field55 = data.isoMap[55]?.rawData ?: ""//910A16F462F8DCDBD7400012720F860D84240000088417ADCFE4D04B81
        //  910A55A52CC220D48AEC0014722C9F180430303030860E84DA00CB090767BED29D791A7B70861384DA00C80E0000000000009039CED44D2F36E5
        println("Filed55 value is --> $field55")//910ADE930EAD11D6F1720014
        //  VFService.showToast("Field 55 value is"+field55)
        val field60Data = data.isoMap[60]?.parseRaw2String().toString()
        val f60DataList = field60Data.split('|')
        //   Auto settle flag | Bank id| Issuer id | MID | TID | Batch No | Stan | Invoice | Card Type
//0|1|51|000000041501002|41501369|000150|260|000260|RUPAY|
        try {

            var hostBankID = f60DataList[1]
            var hostIssuerID = f60DataList[2]
            var hostMID = f60DataList[3]
            val hostTID = f60DataList[4]
            val hostBatchNumber = f60DataList[5]
            val hostRoc = f60DataList[6]
            val hostInvoice = f60DataList[7]
            var hostCardType = f60DataList[8]

            val dateTime: Long = Calendar.getInstance().timeInMillis
            val formatedDate = SimpleDateFormat("yyMMddHHmmss", Locale.getDefault()).format(dateTime)

            field56data = "${hostTID}${hostBatchNumber}${hostRoc}${formatedDate}${""}${hostInvoice}"

        } catch (ex: Exception) {
            ex.printStackTrace()
            // batchFileData
        }



        val f55Hash = HashMap<Int, String>()
        tlvParser(field55, f55Hash)
        val ta8A = 0x8A
        val ta91 = 0x91
        val resCode = data.isoMap[39]?.rawData ?: "05"
        //  VFService.showToast("Response code is"+responseCode)
        val tagData8a = f55Hash[ta8A] ?: responseCode
        try {
            if (tagData8a.isNotEmpty()) {
                val ba = tagData8a.hexStr2ByteArr()
                // rtn = EMVCallback.EMVSetTLVData(ta.toShort(), ba, ba.size)
                logger(VFTransactionActivity.TAG, "On setting ${Integer.toHexString(ta8A)} tag status = $", "e")
            }
        } catch (ex: Exception) {
            logger(VFTransactionActivity.TAG, ex.message ?: "", "e")
        }

        val tagDatatag91 = f55Hash[ta91] ?: ""
        //  mDevCltr.mEmvState.tc = tagDatatag91.hexStr2ByteArr()
        val mba = ArrayList<Byte>()
        val mba1 = ArrayList<Byte>()
        try {
            if (tagDatatag91.isNotEmpty()) {
                val ba = tagDatatag91.hexStr2ByteArr()
                mba.addAll(ba.asList())
                mba1.addAll(ba.asList())
                //
                mba.addAll(tagData8a.str2ByteArr().asList())

                //rtn = EMVCallback.EMVSetTLVData(ta.toShort(), mba.toByteArray(), mba.size)
                logger("Data:- ", "On setting ${Integer.toHexString(ta91)} tag status = $", "e")
            }
        } catch (ex: Exception) {
            logger("Exception:- ", ex.message ?: "")
        }

        var f71 = f55Hash[0x71] ?: ""
        var f72 = f55Hash[0x72] ?: ""

        try {
            val script71 = if (f71.isNotEmpty()) {
                var lenStr = Integer.toHexString(f71.length / 2)
                lenStr = addPad(lenStr, "0", 2)

                f71= "${Integer.toHexString(0x71)}$lenStr$f71"
                f71.hexStr2ByteArr()
                //     rtn = EMVCallback.EMVSetTLVData(ta.toShort(), ba, ba.size)
                logger("Exception:- ", "On setting ${Integer.toHexString(0x71)} tag status = $")
            }
            else byteArrayOf()
        } catch (ex: Exception) {
            logger("Exception:- ", ex.message ?: "")
        }

        val script72 = if (f72.isNotEmpty()) {
            var lenStr = Integer.toHexString(f72.length / 2)
            lenStr = addPad(lenStr, "0", 2)

            f72 = "${Integer.toHexString(0x72)}$lenStr$f72"
            logger("Field72:- ", "72 = $f72")
            f72.hexStr2ByteArr()
        } else byteArrayOf()

        //   val finalRet = EMVCallback.EMVCompleteTrans(resResult, script, script.size, acType)

        try {
            val onlineResult = Bundle()
            onlineResult.putBoolean(ConstIPBOC.inputOnlineResult.onlineResult.KEY_isOnline_boolean, true)
            if (null != resCode && resCode.isNotEmpty() && hexString2String(resCode).equals("00")) {
                onlineResult.putString(ConstIPBOC.inputOnlineResult.onlineResult.KEY_respCode_String, "00")  //tagData8a
            } else {
                onlineResult.putString(ConstIPBOC.inputOnlineResult.onlineResult.KEY_respCode_String, tagData8a)
            }
            onlineResult.putString(ConstIPBOC.inputOnlineResult.onlineResult.KEY_authCode_String, "00")

            if (CardAid.Master.aid == finalaidstr) {

                onlineResult.putString(ConstIPBOC.inputOnlineResult.onlineResult.KEY_field55_String, field55)
                //At least 0A length for 91
                VFService.showToast("Field55 value in Master ---> " + field55)
                println("Field55 value in Master ---> " + field55)
            } else {

                if (field55 != null && field55.isNotEmpty() && mba1.size == 8) {
                    onlineResult.putString(ConstIPBOC.inputOnlineResult.onlineResult.KEY_field55_String, Integer.toHexString(ta91) + "0A" + Utility.byte2HexStr(mba.toByteArray()) + f71 + f72)
                    //At least 0A length for 91
                    //   VFService.showToast("Field55 value in first"+ Integer.toHexString(ta91) + "0A" + Utility.byte2HexStr(mba.toByteArray()) + f71 + f72)
                    println("Field55 value inside ---> " + Integer.toHexString(ta91) + "0A" + Utility.byte2HexStr(mba.toByteArray()) + f71 + f72)
                }
                else if (field55 != null && field55.isNotEmpty() && (mba1.size <= 8 || mba1.size < 10)) {
                    onlineResult.putString(ConstIPBOC.inputOnlineResult.onlineResult.KEY_field55_String, Integer.toHexString(ta91) + "0A" + Utility.byte2HexStr(mba.toByteArray()) + f71 + f72)
                    //At least 0A length for 91
                    //  VFService.showToast("Field55 value inside ---> " + Integer.toHexString(ta91) + "0A" + Utility.byte2HexStr(mba.toByteArray()) + f71 + f72)
                    println("Field55 value inside ---> " + Integer.toHexString(ta91) + "0A" + Utility.byte2HexStr(mba.toByteArray()) + f71 + f72)
                }
                else if (field55 != null && field55.isNotEmpty() && mba1.size >= 10) {

                    println("Field55 value outsides" + field55)
                    //  VFService.showToast("Field55 value outsides"+field55)
                    onlineResult.putString(ConstIPBOC.inputOnlineResult.onlineResult.KEY_field55_String, field55)


                    //At least 0A length for 91
                    println("Field55 value inside ---> " + field55)
                } else {
                    onlineResult.putString(ConstIPBOC.inputOnlineResult.onlineResult.KEY_field55_String, "")
                }

             }

            /*println(
                "Field55 value outside --> " + Integer.toHexString(ta91) + "0A" + Utility.byte2HexStr(
                    mba.toByteArray()
                ) + f72
            )*/

            iemv?.inputOnlineResult(onlineResult, object : OnlineResultHandler.Stub() {

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

                    //    VFService.showToast("TC and Reversal data is$str")
                    println("TC and Reversal data is$str")

                    val aidArray = arrayOf("0x9F06")
                    val aidData = iemv?.getAppTLVList(aidArray)
                    println("Aid Data is ----> $aidData")

                    val tvrArray = arrayOf("0x95")
                    val tvrData = iemv?.getAppTLVList(tvrArray)
                    println("TVR Data is ----> $tvrData")


                    val tsiArray = arrayOf("0x9B")
                    val tsiData = iemv?.getAppTLVList(tsiArray)
                    println("TSI Data is ----> $tsiData")


                    val tcvalue = arrayOf("0x9F26")
                    val tcData = iemv?.getAppTLVList(tcvalue) ?: ""
                    // println("TC Data is ----> tcvalue")

                    val issuerscriptresult = arrayOf("0xDF31")// Issuer script data Tag
                    var issuerscriptdata = iemv?.getAppTLVList(issuerscriptresult)

                    if(null != issuerscriptdata && issuerscriptdata?.isNotBlank() == true) {

                        var issuerscriptvalue = issuerscriptdata?.subSequence(4, issuerscriptdata.length).toString()
                        issuerscriptvalue = "9F5B" + issuerscriptvalue

                        issuerscriptdata = issuerscriptvalue
                    }

                    //    VFService.showToast("Issuer script data is" +issuerscriptdata)

                    println("Issuerscript Data value is ----> $issuerscriptdata")


                    val tcHash = HashMap<Int, String>()
                    tlvParser(tcData, tcHash)

                    tcHash.forEach { (key, value) -> cardProcessedDataModal?.setTC(value) }

                    val subsequenceData = getValueOfTVRAndAID(tvrData ?: "", aidData ?: "", tsiData ?: "")



                    //Here we are Adding AID , TVR and TSI Data in Triple<String , String , String> to return values:-
                    if (!TextUtils.isEmpty(aidData) && !TextUtils.isEmpty(tvrData) && !TextUtils.isEmpty(tsiData))
                        printData = Triple(subsequenceData.first, subsequenceData.second, subsequenceData.third)

                    println("Second Gen Ac value is "+result)

                    when (result) {
                        ConstOnlineResultHandler.onProccessResult.result.TC -> {
                            //VFService.showToast("TC")

                            if (responseCode == HostResponseCode.SUCCESS.code) {

                                if (null != issuerscriptdata && issuerscriptdata?.isNotBlank() == true) {
                                    de55 = issuerscriptdata
                                    println("Issuer script data in if" + de55)
                                }
                            }
                            tc = true
                        }

                        ConstOnlineResultHandler.onProccessResult.result.Online_AAC -> {
                            //VFService.showToast("Online_AAC")
                            //Here we need to Generate Reversal:-
                            if (responseCode == HostResponseCode.SUCCESS.code) {
                                Log.d("Response:- ", "Response Success and Terminal Declined with Save Reversal")

                                //Reversal save To Preference code here.............
                                isoData?.mti = "0400"

                                println("Field56 data in reversal in secondgen ac"+field56data)
                                isoData?.apply {
                                    additionalData["F56reversal"] = field56data ?: ""
                                }

                                var aidstr = cardProcessedDataModal?.getAID()
                                if(CardAid.Rupay.aid == aidstr || CardAid.Diners.aid == aidstr || CardAid.Jcb.aid == aidstr) {

                                    if(VFService.vfsmartReader?.checkCardStatus() == 0x00.toByte()){
                                        isoData?.apply {
                                            additionalData["F39reversal"] = "E2"
                                        }
                                    }
                                    else{
                                        isoData?.apply {
                                            additionalData["F39reversal"] = "E1"
                                        }
                                    }

                                }
                                else if(null != issuerscriptdata && issuerscriptdata?.isNotBlank() == true){
                                    de55 = issuerscriptdata
                                    isoData?.apply {
                                        additionalData["DE55reversal"] = de55?:""
                                    }
                                }

                                val reversalPacket = Gson().toJson(isoData)
                                AppPreference.saveString(GENERIC_REVERSAL_KEY, reversalPacket)

                                AppPreference.saveBoolean(ONLINE_EMV_DECLINED, true)
                            } else {
                                //clearReversal()
                                Log.d("Response:- ", "Response Failure and Terminal Declined with No Save Reversal")
                                AppPreference.saveBoolean(ONLINE_EMV_DECLINED, false)
                            }
                        }

                        ConstOnlineResultHandler.onProccessResult.result.TERMINATE -> {
                            //Reversal save To Preference code here.............
                            if (responseCode == HostResponseCode.SUCCESS.code) {
                                Log.d("Response:- ", "Response Success and Terminal Declined with Save Reversal")
                                //Reversal save To Preference code here.............
                                isoData?.mti = "0400"

                                println("Field56 data in reversal in secondgen ac"+field56data)
                                isoData?.apply {
                                    additionalData["F56reversal"] = field56data ?: ""
                                }
                                val reversalPacket = Gson().toJson(isoData)
                                AppPreference.saveString(GENERIC_REVERSAL_KEY, reversalPacket)
                                AppPreference.saveBoolean(ONLINE_EMV_DECLINED, true)
                            } else {
                                //clearReversal()
                                Log.d("Response:- ", "Response Failure and Terminal Declined with No Save Reversal")
                                AppPreference.saveBoolean(ONLINE_EMV_DECLINED, false)
                            }
                        }

                        ConstOnlineResultHandler.onProccessResult.result.ERROR -> {
                            if (responseCode == HostResponseCode.SUCCESS.code) {
                                Log.d("Response:- ", "Response Success and Terminal Declined with Save Reversal")
                                //Reversal save To Preference code here.............
                                isoData?.mti = "0400"

                                println("Field56 data in reversal in secondgen ac in error"+field56data)
                                isoData?.apply {
                                    additionalData["F56reversal"] = field56data ?: ""
                                }

                                val reversalPacket = Gson().toJson(isoData)
                                AppPreference.saveString(GENERIC_REVERSAL_KEY, reversalPacket)
                                AppPreference.saveBoolean(ONLINE_EMV_DECLINED, true)

                            } else {
                                //clearReversal()
                                Log.d("Response:- ", "Response Failure and Terminal Declined with No Save Reversal")
                                AppPreference.saveBoolean(ONLINE_EMV_DECLINED, false)
                            }
                        }

                        else -> {

                            //VFService.showToast("error, code:$result")
                        }
                    }
                }
            }
            )
        } catch (ex: java.lang.Exception) {
            ex.printStackTrace()
            //println("Exception is" + ex.printStackTrace())
        }
        if (tc)
            printExtraDataSB(printData,de55)
        else {
            printData = Triple("", "", "")
            printExtraDataSB(printData,de55)
        }
    }
}