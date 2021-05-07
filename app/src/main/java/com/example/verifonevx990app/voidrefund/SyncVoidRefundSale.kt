package com.example.verifonevx990app.voidrefund

import android.util.Log
import com.example.verifonevx990app.vxUtils.*
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

class SyncVoidRefundSale(
    private var voidRefundSaleIso: IsoDataWriter,
    var voidRefundCB: (Boolean, String) -> Unit
) {

    init {
        GlobalScope.launch(Dispatchers.IO) {
            sendVoidRefundSaleToHost(voidRefundSaleIso)
        }
    }

    private suspend fun sendVoidRefundSaleToHost(voidRefundSaleIso: IsoDataWriter) {
        var isBool: Boolean = false
        //Reversal Save for Void Transaction
        val reversalPacket = Gson().toJson(voidRefundSaleIso)
        AppPreference.saveString(AppPreference.GENERIC_REVERSAL_KEY, reversalPacket)

        val voidRefundByteArray = voidRefundSaleIso.generateIsoByteRequest()
        logger("PACKET-->", voidRefundByteArray.byteArr2HexStr())

        HitServer.hitServer(voidRefundByteArray, { result, success ->
            if (success) {
                val responseIsoData: IsoDataReader = readIso(result, false)
                logger("Transaction RESPONSE ", "---", "e")
                logger("Transaction RESPONSE --->>", responseIsoData.isoMap, "e")
                Log.e(
                    "Success 39-->  ",
                    responseIsoData.isoMap[39]?.parseRaw2String().toString() + "---->" +
                            responseIsoData.isoMap[58]?.parseRaw2String().toString()
                )
                val successResponseCode = (responseIsoData.isoMap[39]?.parseRaw2String().toString())
                val settlementCheckCode = (responseIsoData.isoMap[39]?.parseRaw2String().toString())

                voidRefundCB(true, result)


            } else {
                AppPreference.clearReversal()
                voidRefundCB(false, "")
            }
        }, {
            //backToCalled(it, false, true)
        })
    }
}