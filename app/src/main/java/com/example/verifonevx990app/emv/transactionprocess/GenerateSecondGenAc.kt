package com.example.verifonevx990app.emv.transactionprocess

import android.os.Bundle
import android.os.RemoteException
import android.text.TextUtils
import android.util.Log
import com.example.verifonevx990app.main.DetectCardType
import com.example.verifonevx990app.main.MainActivity
import com.example.verifonevx990app.vxUtils.VFService
import com.example.verifonevx990app.vxUtils.logger
import com.vfi.smartpos.deviceservice.aidl.IEMV
import com.vfi.smartpos.deviceservice.aidl.OnlineResultHandler
import com.vfi.smartpos.deviceservice.constdefine.ConstIPBOC
import com.vfi.smartpos.deviceservice.constdefine.ConstOnlineResultHandler

class SecondGenAcOnNetworkError(
    var result: String, private var cardProcessedDataModal: CardProcessedDataModal?,
    var networkErrorSecondGenCB: (Boolean) -> Unit
) {
    val vfIEMV: IEMV? by lazy { VFService.vfIEMV }

    init {
        generateSecondGenAcForNetworkErrorCase(result)
    }

    private fun generateSecondGenAcForNetworkErrorCase(result: String) {
        //Here Second GenAC performed in Every Network Failure cases or Time out case:-
        Log.d("Failure Data:- ", result)
        when (cardProcessedDataModal?.getReadCardType()) {
            DetectCardType.MAG_CARD_TYPE -> {
                networkErrorSecondGenCB(false)
            }
            DetectCardType.EMV_CARD_TYPE -> {
                //Test case 15 Unable to go Online
                //here 2nd genearte AC
                val onlineResult = Bundle()
                onlineResult.putBoolean(
                    ConstIPBOC.inputOnlineResult.onlineResult.KEY_isOnline_boolean,
                    true
                )
                onlineResult.putString(
                    ConstIPBOC.inputOnlineResult.onlineResult.KEY_respCode_String,
                    "Z3"
                )  //here will go 5A33
                onlineResult.putString(
                    ConstIPBOC.inputOnlineResult.onlineResult.KEY_authCode_String,
                    "00"
                )
                onlineResult.putString(
                    ConstIPBOC.inputOnlineResult.onlineResult.KEY_field55_String,
                    "8A" + "02" + "5A33"
                )
                //println("Hex string to byte is ---> " + "8A" + "02" + "Z3")

                vfIEMV?.inputOnlineResult(onlineResult, object : OnlineResultHandler.Stub() {
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
                        //VFService.showToast(str)
                        //println("TC and Reversal data is$str")

                        // if(result==0) {

                        val aidArray = arrayOf("0x9F06")
                        val aidData = vfIEMV?.getAppTLVList(aidArray)
                        //println("Aid Data is ----> $aidData")

                        val tvrArray = arrayOf("0x95")
                        val tvrData = vfIEMV?.getAppTLVList(tvrArray)
                        //println("TVR Data is ----> $tvrData")


                        val tsiArray = arrayOf("0x9B")
                        val tsiData = vfIEMV?.getAppTLVList(tsiArray)
                        //println("TSI Data is ----> $tsiData")

                        //Here we are Adding AID , TVR and TSI Data in Triple<String , String , String> to return values:-
                        if (!TextUtils.isEmpty(aidData) && !TextUtils.isEmpty(tvrData) && !TextUtils.isEmpty(
                                tsiData
                            )
                        )
                        //   printData = Triple(aidData?:"" , tvrData?:"" , tsiData?:"")
                        //   }
                            when (result) {
                                ConstOnlineResultHandler.onProccessResult.result.TC -> {
                                    //VFService.showToast("TC")
                                    //   tc = true
                                }

                                ConstOnlineResultHandler.onProccessResult.result.Online_AAC -> {//VFService.showToast("Online_AAC")
                                }

                                else -> {
                                    //VFService.showToast("error, code:$result")
                                }
                            }
                        networkErrorSecondGenCB(true)
                    }
                })
            }
            DetectCardType.CONTACT_LESS_CARD_TYPE -> {
                networkErrorSecondGenCB(false)
            }
            DetectCardType.CONTACT_LESS_CARD_WITH_MAG_TYPE -> {
                networkErrorSecondGenCB(false)
            }
            else -> logger(
                "CARD_ERROR:- ",
                cardProcessedDataModal?.getReadCardType().toString(),
                "e"
            )
        }
    }
}