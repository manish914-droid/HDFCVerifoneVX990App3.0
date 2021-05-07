package com.example.verifonevx990app.crosssell

import android.util.Log
import com.example.verifonevx990app.R
import com.example.verifonevx990app.realmtables.TerminalParameterTable
import com.example.verifonevx990app.vxUtils.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

class SyncCrossSellToHost(
    var field57Data: String,
    var crossSellSyncCB: (Boolean, String, String) -> Unit
) {

    init {
        val crossSellISOData = CreateCrossSellPackets(field57Data).createCrossSellPacket()
        val crossSellByteArray = crossSellISOData.generateIsoByteRequest()
        logger("Transaction REQ --->>", crossSellISOData.isoMap, "e")

        GlobalScope.launch(Dispatchers.IO) { sendCrossSellPacket(crossSellByteArray) }

    }

    private suspend fun sendCrossSellPacket(offlineSaleIso: ByteArray) {
        var isBool = false
        var responseMsg = ""
        var reportData = ""

        HitServer.hitServer(offlineSaleIso, { result, success ->
            if (success) {
                val responseIsoData: IsoDataReader = readIso(result, false)
                logger("Transaction RESPONSE ", "---", "e")
                logger("Transaction RESPONSE --->>", responseIsoData.isoMap, "e")
                Log.e(
                    "Success 39-->  ",
                    responseIsoData.isoMap[39]?.parseRaw2String().toString() + "---->" +
                            responseIsoData.isoMap[58]?.parseRaw2String().toString()
                )
                val successResponseCode = responseIsoData.isoMap[39]?.parseRaw2String().toString()
                if (responseIsoData.isoMap[57] != null) {
                    reportData = responseIsoData.isoMap[57]?.parseRaw2String().toString()
                }
                if (responseIsoData.isoMap[58] != null) {
                    responseMsg = responseIsoData.isoMap[58]?.parseRaw2String().toString()
                }
                isBool = successResponseCode == "00"
                crossSellSyncCB(isBool, responseMsg, reportData)
            } else {
                isBool = false
                crossSellSyncCB(isBool, responseMsg, reportData)
            }
        }, {})
    }
}

class CreateCrossSellPackets(var field57Data: String) : ICrossSellPacketExchange {

    init {
        createCrossSellPacket()
    }


    override fun createCrossSellPacket(): IsoDataWriter = IsoDataWriter().apply {
        val terminalData = TerminalParameterTable.selectFromSchemeTable()
        if (terminalData != null) {
            mti = Mti.CROSS_SELL_MTI.mti

            //Processing Code Field 3
            addField(3, ProcessingCode.CROSS_SELL.code)

            //STAN(ROC) Field 11
            addField(11, ROCProviderV2.getRoc(AppPreference.getBankCode()).toString())

            //NII Field 24
            addField(24, Nii.HDFC_DEFAULT.nii)

            //TID Field 41
            addFieldByHex(41, terminalData.terminalId)

            //Connection Time Stamps Field 48
            addFieldByHex(48, Field48ResponseTimestamp.getF48Data())

            //adding Field 57
            addFieldByHex(57, field57Data)

            //adding Field 61
            val version = addPad(getAppVersionNameAndRevisionID(), "0", 15, false)
            val pcNumber = addPad(AppPreference.getString(AppPreference.PC_NUMBER_KEY), "0", 9)
            val pcNumber2 = addPad(AppPreference.getString(AppPreference.PC_NUMBER_KEY_2), "0", 9)
            val data = ConnectionType.GPRS.code + addPad(
                AppPreference.getString("deviceModel"), " ", 6, false
            ) +
                    addPad(VerifoneApp.appContext.getString(R.string.app_name), " ", 10, false) +
                    version + pcNumber + pcNumber2
            val f61 = data

            //adding Field 61

            addFieldByHex(61, f61)

            //adding Field 63
            val deviceSerial = addPad(AppPreference.getString("serialNumber"), " ", 15, false)
            val bankCode = AppPreference.getBankCode()
            val f63 = "$deviceSerial$bankCode"
            addFieldByHex(63, f63)
        }
    }
}