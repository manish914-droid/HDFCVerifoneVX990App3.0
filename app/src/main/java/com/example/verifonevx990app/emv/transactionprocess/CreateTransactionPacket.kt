package com.example.verifonevx990app.emv.transactionprocess

import android.text.TextUtils
import android.util.Log
import com.example.verifonevx990app.R
import com.example.verifonevx990app.bankemi.BankEMIDataModal
import com.example.verifonevx990app.bankemi.BankEMIIssuerTAndCDataModal
import com.example.verifonevx990app.main.DetectCardType
import com.example.verifonevx990app.realmtables.*
import com.example.verifonevx990app.vxUtils.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import java.text.SimpleDateFormat
import java.util.*

class CreateTransactionPacket(
    private var cardProcessedData: CardProcessedDataModal,
    private var bankEmiSchemeData: BankEMIDataModal? = null,
    private var bankEmiTandCData: BankEMIIssuerTAndCDataModal? = null
) :
    ITransactionPacketExchange {

    private var indicator: String? = null
    private var brandEMIDataTable: BrandEMIDataTable? = null
    private var brandEMIByAccessCodeData: BrandEMIAccessDataModalTable? = null

    //Below method is used to create Transaction Packet in all cases:-
    init {
        createTransactionPacket()
    }

    override fun createTransactionPacket(): IsoDataWriter = IsoDataWriter().apply {
        //Condition To Check TransactionType == BrandEMIByAccessCode if it is then fetch its value from DB:-
        if (cardProcessedData.getTransType() == TransactionType.BRAND_EMI_BY_ACCESS_CODE.type) {
            brandEMIByAccessCodeData =
                runBlocking(Dispatchers.IO) { BrandEMIAccessDataModalTable.getBrandEMIByAccessCodeData() }
        }


        if (cardProcessedData.getTransType() == TransactionType.BRAND_EMI.type) {
            brandEMIDataTable = runBlocking(Dispatchers.IO) { BrandEMIDataTable.getAllEMIData() }
        }
        val terminalData = TerminalParameterTable.selectFromSchemeTable()
        if (terminalData != null) {
            logger("PINREQUIRED--->  ", cardProcessedData.getIsOnline().toString(), "e")
            mti =
                if (!TextUtils.isEmpty(AppPreference.getString(AppPreference.GENERIC_REVERSAL_KEY))) {
                    Mti.REVERSAL.mti
                } else {
                    when (cardProcessedData.getTransType()) {
                        TransactionType.PRE_AUTH.type -> Mti.PRE_AUTH_MTI.mti
                        else -> Mti.DEFAULT_MTI.mti
                    }
                }

            //Processing Code Field 3
            addField(3, cardProcessedData.getProcessingCode().toString())

            //Transaction Amount Field
            //val formattedTransAmount = "%.2f".format(cardProcessedData.getTransactionAmount()?.toDouble()).replace(".", "")
            if (cardProcessedData.getTransType() == TransactionType.BRAND_EMI_BY_ACCESS_CODE.type) {
                addField(
                    4,
                    addPad(brandEMIByAccessCodeData?.transactionAmount ?: "", "0", 12, true)
                )
            } else {
                addField(
                    4,
                    addPad(cardProcessedData.getTransactionAmount().toString(), "0", 12, true)
                )
            }

            //STAN(ROC) Field 11
            addField(11, ROCProviderV2.getRoc(AppPreference.getBankCode()).toString())

            //Date and Time Field 12 & 13
            addIsoDateTime(this)

            //println("Pos entry mode is --->" + cardProcessedData.getPosEntryMode().toString())
            //Pos Entry Mode Field 22
            //    if(null !=cardProcessedData.getPosEntryMode().toString() && cardProcessedData.getPosEntryMode().toString().isNotEmpty())
            addField(22, cardProcessedData.getPosEntryMode().toString())

            //Pan Sequence Number Field 23
            if (null != cardProcessedData.getApplicationPanSequenceValue())
                addFieldByHex(
                    23,
                    addPad(
                        cardProcessedData.getApplicationPanSequenceValue().toString(),
                        "0",
                        3,
                        true
                    )
                )
            else {
                addFieldByHex(23, addPad("00", "0", 3, true))
            }

            //NII Field 24
            addField(24, Nii.DEFAULT.nii)

            //TID Field 41
            addFieldByHex(41, terminalData.terminalId)

            //MID Field 42
            addFieldByHex(42, terminalData.merchantId)

            //addFieldByHex(48, Field48ResponseTimestamp.getF48Data())
            //Connection Time Stamps Field 48
            addFieldByHex(48, Field48ResponseTimestamp.getF48Data())

            //Field 52 in case of Pin
            if (!(TextUtils.isEmpty(cardProcessedData.getGeneratePinBlock())) && cardProcessedData.getPinByPass() == 0)
                addField(52, cardProcessedData.getGeneratePinBlock().toString())

            //Field 54 in case od sale with cash AND Cash at POS.
            when (cardProcessedData.getTransType()) {
                TransactionType.CASH_AT_POS.type, TransactionType.SALE_WITH_CASH.type ->
                    addFieldByHex(
                        54,
                        addPad(cardProcessedData.getOtherAmount().toString(), "0", 12, true)
                    )

                /* TransactionType.SALE.type->{
                     if(cardProcessedData?.getSaleTipAmount()?:0L >0L){
                         addFieldByHex(
                             54,
                             addPad(cardProcessedData?.getSaleTipAmount().toString(), "0", 12, true)
                         )
                     }
                 }*/
                else -> {
                }
            }


            //Field 55
            when (cardProcessedData.getReadCardType()) {
                DetectCardType.EMV_CARD_TYPE, DetectCardType.CONTACT_LESS_CARD_TYPE -> addField(
                    55, cardProcessedData.getFiled55().toString()
                )
                else -> {
                }
            }

            //Below Field57 is Common for Cases Like CTLS + CTLSMAG + EMV + MAG:-
            addField(57, cardProcessedData.getTrack2Data().toString())

            //Indicator Data Field 58
            val cardIndFirst = "0"
            val firstTwoDigitFoCard = cardProcessedData.getPanNumberData()?.substring(0, 2)
            val cardDataTable = CardDataTable.selectFromCardDataTable(
                cardProcessedData.getPanNumberData().toString()
            )
            //  val cardDataTable = CardDataTable.selectFromCardDataTable(cardProcessedData.getTrack2Data()!!)
            val cdtIndex = cardDataTable?.cardTableIndex ?: ""
            val accSellection =
                addPad(
                    AppPreference.getString(AppPreference.ACC_SEL_KEY),
                    "0",
                    2
                ) //cardDataTable.getA//"00"

            //region===============Check If Transaction Type is EMI_SALE , Brand_EMI or Other then Field would be appended with Bank EMI Scheme Offer Values:-
            when (cardProcessedData.getTransType()) {
                TransactionType.EMI_SALE.type -> {
                    indicator = "$cardIndFirst|$firstTwoDigitFoCard|$cdtIndex|$accSellection," +
                            "${cardProcessedData.getPanNumberData()?.substring(0, 8)}," +
                            "${bankEmiTandCData?.issuerID},${bankEmiTandCData?.emiSchemeID},1,0,${cardProcessedData.getEmiTransactionAmount()}," +
                            "${bankEmiSchemeData?.discountAmount},${bankEmiSchemeData?.loanAmount},${bankEmiSchemeData?.tenure}," +
                            "${bankEmiSchemeData?.tenureInterestRate},${bankEmiSchemeData?.emiAmount},${bankEmiSchemeData?.cashBackAmount}," +
                            "${bankEmiSchemeData?.netPay},${cardProcessedData.getMobileBillExtraData()?.first ?: cardProcessedData.getMobileBillExtraData()?.second ?: ""}," +
                            ",,,,0,${bankEmiSchemeData?.processingFee},${bankEmiSchemeData?.processingRate}," +
                            "${bankEmiSchemeData?.totalProcessingFee},,"
                }

                TransactionType.BRAND_EMI.type -> {
                    indicator = "$cardIndFirst|$firstTwoDigitFoCard|$cdtIndex|$accSellection," +
                            "${cardProcessedData.getPanNumberData()?.substring(0, 8)}," +
                            "${bankEmiTandCData?.issuerID},${bankEmiTandCData?.emiSchemeID},${brandEMIDataTable?.brandID}," +
                            "${brandEMIDataTable?.productID},${cardProcessedData.getEmiTransactionAmount()}," +
                            "${bankEmiSchemeData?.discountAmount},${bankEmiSchemeData?.loanAmount},${bankEmiSchemeData?.tenure}," +
                            "${bankEmiSchemeData?.tenureInterestRate},${bankEmiSchemeData?.emiAmount},${bankEmiSchemeData?.cashBackAmount}," +
                            "${bankEmiSchemeData?.netPay},${cardProcessedData.getMobileBillExtraData()?.first ?: cardProcessedData.getMobileBillExtraData()?.second ?: ""}," +
                            "${brandEMIDataTable?.imeiNumber ?: brandEMIDataTable?.serialNumber ?: ""},,,,0,${bankEmiSchemeData?.processingFee},${bankEmiSchemeData?.processingRate}," +
                            "${bankEmiSchemeData?.totalProcessingFee},,"
                }

                TransactionType.BRAND_EMI_BY_ACCESS_CODE.type -> {
                    indicator = "$cardIndFirst|$firstTwoDigitFoCard|$cdtIndex|$accSellection," +
                            "${cardProcessedData.getPanNumberData()?.substring(0, 8)}," +
                            "${brandEMIByAccessCodeData?.issuerID},${brandEMIByAccessCodeData?.emiSchemeID},${brandEMIByAccessCodeData?.brandID}," +
                            "${brandEMIByAccessCodeData?.productID},${brandEMIByAccessCodeData?.transactionAmount}," +
                            "${brandEMIByAccessCodeData?.discountAmount},${brandEMIByAccessCodeData?.loanAmount},${brandEMIByAccessCodeData?.tenure}," +
                            "${brandEMIByAccessCodeData?.interestAmount},${brandEMIByAccessCodeData?.emiAmount},${brandEMIByAccessCodeData?.cashBackAmount}," +
                            "${brandEMIByAccessCodeData?.netPayAmount},${cardProcessedData.getMobileBillExtraData()?.first ?: cardProcessedData.getMobileBillExtraData()?.second ?: ""}," +
                            "${/*brandEMIByAccessCodeData?.imeiNumber ?: */brandEMIByAccessCodeData?.productSerialCode ?: ""},,,,0,${brandEMIByAccessCodeData?.processingFee},${brandEMIByAccessCodeData?.processingFeeRate}," +
                            "${brandEMIByAccessCodeData?.totalProcessingFee},${brandEMIByAccessCodeData?.emiCode},"
                }

                else -> {
                    indicator = "$cardIndFirst|$firstTwoDigitFoCard|$cdtIndex|$accSellection"
                }
            }

            Log.d("SALE Indicator:- ", indicator.toString())

            //Adding Field 58
            addFieldByHex(58, indicator ?: "")

            //Adding Field 60 value on basis of Condition Whether it consist Mobile Number Data , Bill Number Data or not:-
            val gcc = "0"
            var field60: String? = null
            var batchNumber: String? = null
            when {
                !TextUtils.isEmpty(cardProcessedData.getMobileBillExtraData()?.first) -> {
                    batchNumber = addPad(terminalData.batchNumber, "0", 6, true)
                    val mobileNumber = cardProcessedData.getMobileBillExtraData()?.first
                    field60 = "$batchNumber|$mobileNumber|$gcc"
                    addFieldByHex(60, field60)
                }

                else -> {
                    batchNumber = addPad(terminalData.batchNumber, "0", 6, true)
                    field60 = "$batchNumber||$gcc"
                    addFieldByHex(60, field60)
                }
            }

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
            ) + addPad(VerifoneApp.appContext.getString(R.string.app_name), " ", 10, false) +
                    version + pcNumber + addPad("0", "0", 9)
            /* val customerID = HexStringConverter.addPreFixer(
                 issuerParameterTable?.customerIdentifierFiledType,
                 2
             )*/
            val customerID =
                issuerParameterTable?.customerIdentifierFiledType?.let { addPad(it, "0", 2) } ?: 0

            //  val walletIssuerID = HexStringConverter.addPreFixer(issuerParameterTable?.issuerId, 2)
            val walletIssuerID = issuerParameterTable?.issuerId?.let { addPad(it, "0", 2) } ?: 0
            addFieldByHex(
                61, addPad(
                    AppPreference.getString("serialNumber"), " ", 15, false
                ) + AppPreference.getBankCode() + customerID + walletIssuerID + data
            )

            //adding field 62
            addFieldByHex(62, terminalData.invoiceNumber)

            //Here we are Saving Date , Time and TimeStamp in CardProcessedDataModal:-
            var year: String = "Year"
            try {
                val date: Long = Calendar.getInstance().timeInMillis
                val timeFormater = SimpleDateFormat("HHmmss", Locale.getDefault())
                cardProcessedData.setTime(timeFormater.format(date))
                val dateFormater = SimpleDateFormat("MMdd", Locale.getDefault())
                cardProcessedData.setDate(dateFormater.format(date))
                cardProcessedData.setTimeStamp(date.toString())
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

            additionalData["pan"] = getMaskedPan(
                TerminalParameterTable.selectFromSchemeTable(),
                cardProcessedData.getPanNumberData() ?: ""
            )

            additionalData["cardType"] =
                cardProcessedData.getReadCardType()?.cardTypeName.toString()
            /* if (!TextUtils.isEmpty(AppPreference.getString(AppPreference.GENERIC_REVERSAL_KEY))) {
                 val f56Reversal = year + cardProcessedData.getDate() + cardProcessedData.getTime()
                 //year + date + time
                 addFieldByHex(56, f56Reversal)

             }*/
        }
    }
}