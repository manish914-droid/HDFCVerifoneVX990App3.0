package com.example.verifonevx990app.appupdate

import com.example.verifonevx990app.R
import com.example.verifonevx990app.realmtables.TerminalParameterTable
import com.example.verifonevx990app.vxUtils.*

class SendAppUpdateConfirmationPacket : IAppUpdateConfirmationPacketExchange {

    init {
        createAppUpdateConfirmationPacket()
    }

    override fun createAppUpdateConfirmationPacket(): IsoDataWriter = IsoDataWriter().apply {
        val terminalData = TerminalParameterTable.selectFromSchemeTable()
        if (terminalData != null) {
            mti = Mti.APP_UPDATE_MTI.mti

            //Processing Code Field 3
            addField(3, ProcessingCode.APP_UPDATE_CONFIRMATION.code)

            //STAN(ROC) Field 11
            addField(11, ROCProviderV2.getRoc(AppPreference.getBankCode()).toString())

            //NII Field 24
            addField(24, Nii.DEFAULT.nii)

            //TID Field 41
            addFieldByHex(41, terminalData.terminalId)

            //Connection Time Stamps Field 48
            addFieldByHex(48, Field48ResponseTimestamp.getF48Data())

            //adding Field 61
            val version = addPad(getAppVersionNameAndRevisionID(), "0", 15, false)
            val pcNumber = addPad(AppPreference.getString(AppPreference.PC_NUMBER_KEY), "0", 9)
            val data = ConnectionType.GPRS.code + addPad(
                AppPreference.getString("deviceModel"), " ", 6, false
            ) +
                    addPad(VerifoneApp.appContext.getString(R.string.app_name), " ", 10, false) +
                    version + addPad("0", "0", 9) + pcNumber

            //Adding Field 61:-
            addFieldByHex(61, data)

            //adding Field 63
            val deviceSerial = addPad(AppPreference.getString("serialNumber"), " ", 15, false)
            val bankCode = AppPreference.getBankCode()
            val f63 = "$deviceSerial$bankCode"
            addFieldByHex(63, f63)

        }
    }
}