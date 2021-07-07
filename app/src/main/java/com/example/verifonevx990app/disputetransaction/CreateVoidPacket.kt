package com.example.verifonevx990app.disputetransaction

import com.example.verifonevx990app.R
import com.example.verifonevx990app.main.CardAid
import com.example.verifonevx990app.realmtables.BatchFileDataTable
import com.example.verifonevx990app.realmtables.IssuerParameterTable
import com.example.verifonevx990app.utils.HexStringConverter
import com.example.verifonevx990app.vxUtils.*
import java.text.SimpleDateFormat
import java.util.*

class CreateVoidPacket(val batch: BatchFileDataTable) : IVoidExchange {

    override fun createVoidISOPacket(): IsoDataWriter = IsoDataWriter().apply {
        // packing data
        mti = Mti.DEFAULT_MTI.mti

        if (batch.transactionType == TransactionType.REFUND.type) {
            addField(3, ProcessingCode.VOID_REFUND.code)
        } else {
            addField(3, ProcessingCode.VOID.code)
        }


        addField(4, batch.transactionalAmmount)

        addField(11, ROCProviderV2.getRoc(AppPreference.getBankCode()).toString())

        addIsoDateTime(this)

        addField(22, batch.posEntryValue)
        addField(24, Nii.DEFAULT.nii)

        if (null != batch.aqrRefNo && !batch.aqrRefNo.isNullOrBlank())
            addFieldByHex(
                31,
                batch.aqrRefNo
            )  // going in case of Amex for visa and master check if to send or not

        addFieldByHex(41, batch.tid)
        addFieldByHex(42, batch.mid)
        addFieldByHex(48, Field48ResponseTimestamp.getF48Data())

        if (batch.transactionType == TransactionType.TIP_SALE.type)
            addFieldByHex(54, addPad(batch.tipAmmount, "0", 12))

        var  aidstr = if(batch.aid.isNotBlank()) { batch.aid?.subSequence(0,10).toString() } else { batch.aid = ""}

        println("DE55 value in void is"+"${batch.field55Data}${batch.de55}")
        if(batch.operationType == "Chip" && (CardAid.Rupay.aid == aidstr || CardAid.Diners.aid == aidstr || CardAid.Jcb.aid == aidstr)){

            if(batch.field55Data.isNotBlank() && batch.de55.isNotBlank()) {
                println("DE55 value in void is"+"${batch.field55Data}${batch.de55}")
                addField(55, "${batch.field55Data}${batch.de55}")
            }
            else if(batch.field55Data.isNotBlank()) {
                println("DE55 value in void is"+"${batch.field55Data}")
                addField(55, "${batch.field55Data}")
            }
        }




        //Transaction's ROC, transactionDate, transaction Time
        val f56 = "${batch.roc}${batch.transactionDate}${batch.transactionTime}"

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

        // old data
     //   addFieldByHex(56, addPad("${batch.roc}", "0", 6, true) + "${formatedDate}")


        addField(57, batch.track2Data)
        if (batch.transactionType != TransactionType.EMI_SALE.type)
            addFieldByHex(58, batch.indicator)
        else
            addFieldByHex(58, AppPreference.getString(batch.invoiceNumber))

        addFieldByHex(60, batch.batchNumber)

        //adding field 61
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
        val customerID =
            HexStringConverter.addPreFixer(issuerParameterTable?.customerIdentifierFiledType, 2)

        val walletIssuerID = HexStringConverter.addPreFixer(issuerParameterTable?.issuerId, 2)
        addFieldByHex(
            61,
            addPad(
                AppPreference.getString("serialNumber"),
                " ",
                15,
                false
            ) + AppPreference.getBankCode() + customerID + walletIssuerID + data
        )

        //   addFieldByHex(61, batch.getField61())

        addFieldByHex(62, batch.invoiceNumber)
        var year: String = "Year"
        try {
            val date: Long = Calendar.getInstance().timeInMillis
            year = SimpleDateFormat("yy", Locale.getDefault()).format(date)
        } catch (ex: Exception) {
            ex.printStackTrace()
        }
        //  saving field 56 if reversal generated for this trans then in next trans we send this field in reversal
        val f56Roc =
            addPad(ROCProviderV2.getRoc(AppPreference.getBankCode()).toString(), "0", 6)
        val f56Date = this.isoMap[13]?.rawData
        val f56Time = this.isoMap[12]?.rawData
        additionalData["F56reversal"] =
            f56Roc + year + f56Date + f56Time

    }
}