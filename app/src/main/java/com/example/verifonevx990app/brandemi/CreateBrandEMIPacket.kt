package com.example.verifonevx990app.brandemi

import com.example.verifonevx990app.realmtables.TerminalParameterTable
import com.example.verifonevx990app.vxUtils.*

class CreateBrandEMIPacket(private var field57RequestData: String?, private var cb: (IsoDataWriter) -> Unit) {
    init {
        cb(createBrandEMIMasterDataPacket())
    }

    //region ==================Creating Brand EMI Related ISO packet
    private fun createBrandEMIMasterDataPacket(): IsoDataWriter =
        IsoDataWriter().apply {
            val terminalData = TerminalParameterTable.selectFromSchemeTable()
            if (terminalData != null) {
                mti = Mti.EIGHT_HUNDRED_MTI.mti

                //Processing Code Field 3
                addField(3, ProcessingCode.BRAND_EMI.code)

                //STAN(ROC) Field 11
                addField(11, ROCProviderV2.getRoc(AppPreference.getBankCode()).toString())

                //NII Field 24
                addField(24, Nii.BRAND_EMI_MASTER.nii)

                //TID Field 41
                addFieldByHex(41, terminalData.terminalId)

                //adding field 57
                addFieldByHex(57, field57RequestData ?: "1^0")

                //adding Field 61
                addFieldByHex(61, KeyExchanger.getF61())

                //adding field 63
                val deviceSerial = addPad(AppPreference.getString("serialNumber"), " ", 15, false)
                val bankCode = AppPreference.getBankCode()
                val f63 = "$deviceSerial$bankCode"
                addFieldByHex(63, f63)
            }
        }
}
//endregion