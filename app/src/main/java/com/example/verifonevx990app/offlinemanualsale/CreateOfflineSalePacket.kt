package com.example.verifonevx990app.offlinemanualsale

import com.example.verifonevx990app.R
import com.example.verifonevx990app.main.PosEntryModeType
import com.example.verifonevx990app.realmtables.BatchFileDataTable
import com.example.verifonevx990app.realmtables.IssuerParameterTable
import com.example.verifonevx990app.realmtables.TerminalParameterTable
import com.example.verifonevx990app.utils.HexStringConverter
import com.example.verifonevx990app.vxUtils.*

class CreateOfflineSalePacket(private var offlineBatchTableData: BatchFileDataTable) :
    IOfflineSalePacketExchange {

    init {
        createOfflineSalePacket()
    }

    override fun createOfflineSalePacket(): IsoDataWriter = IsoDataWriter().apply {
        val terminalData = TerminalParameterTable.selectFromSchemeTable()
        if (terminalData != null) {
            mti = Mti.DEFAULT_MTI.mti

            //Processing Code Field 3
            addField(3, ProcessingCode.OFFLINE_SALE.code)

            //Transaction Amount Field 4
            addField(4, addPad(offlineBatchTableData.transactionalAmmount, "0", 12, true))

            //STAN(ROC) Field 11
            addField(11, offlineBatchTableData.roc)

            //Date and Time Field 12 & 13
            addIsoDateTime(this)

            //POS Entry Code Field 22
            addField(22, PosEntryModeType.OFFLINE_SALE_POS_ENTRY_CODE.posEntry.toString())

            //NII Field 24
            addField(24, Nii.DEFAULT.nii)

            //TID Field 41
            addFieldByHex(41, terminalData.terminalId)

            //MID Field 42
            addFieldByHex(42, terminalData.merchantId)

            //Connection Time Stamps Field 48
            addFieldByHex(48, Field48ResponseTimestamp.getF48Data())

            //adding Field 57 for Offline Sale
            addField(57, offlineBatchTableData.track2Data)

            //adding Field 58 Indicator Data
            addFieldByHex(58, offlineBatchTableData.indicator)

            //Batch Number Field 60
            addFieldByHex(60, addPad(terminalData.batchNumber, "0", 6, true))

            //adding Field 61
            val issuerParameterTable =
                IssuerParameterTable.selectFromIssuerParameterTable(AppPreference.WALLET_ISSUER_ID)
            val version = addPad(getAppVersionNameAndRevisionID(), "0", 15, false)
            val pcNumber = addPad(AppPreference.getString(AppPreference.PC_NUMBER_KEY), "0", 9)
            val data = ConnectionType.GPRS.code + addPad(
                AppPreference.getString("deviceModel"),
                " ",
                6,
                false
            ) +
                    addPad(VerifoneApp.appContext.getString(R.string.app_name), " ", 10, false) +
                    version + pcNumber + addPad("0", "0", 9)
            val customerID = HexStringConverter.addPreFixer(
                issuerParameterTable?.customerIdentifierFiledType,
                2
            )

            //adding Field 61
            val walletIssuerID = HexStringConverter.addPreFixer(issuerParameterTable?.issuerId, 2)
            addFieldByHex(
                61, addPad(
                    AppPreference.getString("serialNumber"), " ", 15, false
                ) + AppPreference.getBankCode() + customerID + walletIssuerID + data
            )

            //adding Field 62
            addFieldByHex(62, offlineBatchTableData.invoiceNumber)

        }
    }
}