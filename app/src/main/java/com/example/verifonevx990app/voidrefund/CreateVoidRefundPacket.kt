package com.example.verifonevx990app.voidrefund

import com.example.verifonevx990app.R
import com.example.verifonevx990app.realmtables.BatchFileDataTable
import com.example.verifonevx990app.realmtables.IssuerParameterTable
import com.example.verifonevx990app.utils.HexStringConverter
import com.example.verifonevx990app.vxUtils.*
import java.text.SimpleDateFormat
import java.util.*

class CreateVoidRefundPacket(private var batch: BatchFileDataTable) :
    IVoidRefundTransactionPacketExchange {


    //Below method is used to create Transaction Packet of Void Refund:-
    init {
        createVoidRefundTransactionPacket()
    }

    override fun createVoidRefundTransactionPacket(): IsoDataWriter = IsoDataWriter().apply {
        mti = Mti.DEFAULT_MTI.mti

        addField(3, ProcessingCode.VOID_REFUND.code)
        addField(4, batch.transactionalAmmount)

        // adding ROC (11) time(12) and date(13)
        addField(11, ROCProviderV2.getRoc(AppPreference.getBankCode()).toString())

        addIsoDateTime(this)

        addField(22, batch.posEntryValue)

        addField(24, Nii.DEFAULT.nii)

        addFieldByHex(31, batch.aqrRefNo)

        addFieldByHex(41, batch.tid)

        addFieldByHex(42, batch.mid)

        addFieldByHex(48, Field48ResponseTimestamp.getF48Data())

        val formater = SimpleDateFormat("yyMMddHHmmss", Locale.getDefault())
        val formatedDate = formater.format(batch.timeStamp)

        //send 56 field in request packet as –
        //TID           – 8 length
        //BATCHNO       - 6 length
        //STAN          - 6 length
        //TXN_DATE_TIME – 12 length
        //AUTH_CODE     - 6 length
        //INVOICE       - 6 length

        //Changes By manish Kumar
        //If in Respnse field 60 data comes Auto settle flag | Bank id| Issuer id | MID | TID | Batch No | Stan | Invoice | Card Type
        // then show response data otherwise show data available in database
        //From mid to hostMID (coming from field 60)
        //From tid to hostTID (coming from field 60)
        //From batchNumber to hostBatchNumber (coming from field 60)
        //From roc to hostRoc (coming from field 60)
        //From invoiceNumber to hostInvoice (coming from field 60)
        //From cardType to hostCardType (coming from field 60)

        var hostTID = if (batch.hostTID.isNotBlank()) { batch.hostTID } else { batch.tid }
        var hostBatchNumber = if (batch.hostBatchNumber.isNotBlank()) { batch.hostBatchNumber } else { addPad("${batch.batchNumber}", "0", 6, true) }
        var hostRoc = if (batch.hostRoc.isNotBlank()) { batch.hostRoc } else { addPad("${batch.roc}", "0", 6, true) }
        var hostInvoice = if (batch.hostInvoice.isNotBlank()) { batch.hostInvoice } else { addPad("${batch.invoiceNumber}", "0", 6, true) }

        if(hostTID.isNotBlank() && hostBatchNumber.isNotBlank() && hostRoc.isNotBlank() && hostInvoice.isNotBlank()) {

            addFieldByHex(56, "${hostTID}${hostBatchNumber}${hostRoc}${formatedDate}${batch.authCode}${hostInvoice}")
            println("Field 56 data is" + "${hostTID}${hostBatchNumber}${hostRoc}${formatedDate}${batch.authCode}${hostInvoice}")
        }


         //old data
        //Transaction's ROC, transactionDate, transaction Time
        val f56 = "${invoiceWithPadding(ROCProviderV2.getRoc(AppPreference.getBankCode()).toString())}${batch.currentYear}${batch.date}${batch.time}"

        addFieldByHex(56, f56)

        addField(57, batch.track2Data)

        addFieldByHex(58, batch.indicator)

        addFieldByHex(60, batch.batchNumber)

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

        addFieldByHex(62, batch.invoiceNumber)

        //  saving field 56 if reversal generated for this trans then in next trans we send this field in reversal
        val f56Roc =
            addPad(ROCProviderV2.getRoc(AppPreference.getBankCode()).toString(), "0", 6)
        val f56Date = this.isoMap[13]?.rawData
        val f56Time = this.isoMap[12]?.rawData
        val cal = Calendar.getInstance()
        val curYear = cal.get(Calendar.YEAR)
        val requireYearDigit = curYear.toString().substring(2, 4)
        additionalData["F56reversal"] =
            f56Roc + requireYearDigit + f56Date + f56Time


    }
}