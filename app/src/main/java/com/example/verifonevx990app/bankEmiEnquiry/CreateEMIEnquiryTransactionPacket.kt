package com.example.verifonevx990app.bankEmiEnquiry

import com.example.verifonevx990app.realmtables.TerminalParameterTable
import com.example.verifonevx990app.vxUtils.*

class CreateEMIEnquiryTransactionPacket(var dF57: String) {


    //Below method is used to create Transaction Packet in all cases:-
    init {
        createTransactionPacket()
    }

    fun createTransactionPacket(): IsoDataWriter = IsoDataWriter().apply {
        val tpt = TerminalParameterTable.selectFromSchemeTable()

        // MTI for enquiry is same as BAnk EMI i.e -->0800
        mti = Mti.BANKI_EMI.mti

        addField(3, ProcessingCode.EMI_ENQUIRY.code)
        // addField(4, data["amount"] ?: "")

        // adding ROC (11) time(12) and date(13)
        addField(11, ROCProviderV2.getRoc(AppPreference.getBankCode()).toString())


        addField(24, Nii.DEFAULT.nii)

        addFieldByHex(41, tpt?.terminalId ?: "0")

        addFieldByHex(57, dF57)

        //adding Field 61
        addFieldByHex(61, KeyExchanger.getF61())

        //adding Field 63
        val deviceSerial = addPad(AppPreference.getString("serialNumber"), " ", 15, false)
        val bankCode = AppPreference.getBankCode()
        val f63 = "$deviceSerial$bankCode"
        addFieldByHex(63, f63)

    }

}
