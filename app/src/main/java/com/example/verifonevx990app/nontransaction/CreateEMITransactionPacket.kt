package com.example.verifonevx990app.nontransaction

import android.text.TextUtils
import com.example.verifonevx990app.R
import com.example.verifonevx990app.emv.transactionprocess.CardProcessedDataModal
import com.example.verifonevx990app.main.DetectCardType
import com.example.verifonevx990app.realmtables.CardDataTable
import com.example.verifonevx990app.realmtables.IssuerParameterTable
import com.example.verifonevx990app.realmtables.TerminalParameterTable
import com.example.verifonevx990app.transactions.EmiCustomerDetails
import com.example.verifonevx990app.utils.HexStringConverter
import com.example.verifonevx990app.vxUtils.*
import java.text.SimpleDateFormat
import java.util.*

class CreateEMITransactionPacket(
    private var cardProcessedData: CardProcessedDataModal,
    var emiCustomerDetails: EmiCustomerDetails?
) : ITransactionPacketExchange {


    //Below method is used to create Transaction Packet in all cases:-
    init {
        createTransactionPacket()
    }

    override fun createTransactionPacket(): IsoDataWriter = IsoDataWriter().apply {
        //     val batchFileDataTable = BatchFileDataTable.selectBatchData()
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
            addField(4, addPad(cardProcessedData.getTransactionAmount().toString(), "0", 12, true))

            //STAN(ROC) Field 11
            addField(11, ROCProviderV2.getRoc(AppPreference.getBankCode()).toString())

            //Date and Time Field 12 & 13
            addIsoDateTime(this)

            //println("Pos entry mode is --->"+cardProcessedData.getPosEntryMode().toString())
            //Pos Entry Mode Field 22
            //    if(null !=cardProcessedData.getPosEntryMode().toString() && cardProcessedData.getPosEntryMode().toString().isNotEmpty())
            addField(22, cardProcessedData.getPosEntryMode().toString())

            //Pan Sequence Number Field 23
            addField(
                23,
                addPad(cardProcessedData.getApplicationPanSequenceValue().toString(), "0", 6, true)
            )

            //NII Field 24
            addField(24, Nii.DEFAULT.nii)

            //TID Field 41
            addFieldByHex(41, terminalData.terminalId)

            //MID Field 42
            addFieldByHex(42, terminalData.merchantId)

            //Connection Time Stamps Field 48
            addFieldByHex(48, Field48ResponseTimestamp.getF48Data())

            //Field 52 in case of Pin
            if (!(TextUtils.isEmpty(cardProcessedData.getGeneratePinBlock())))
                addField(52, cardProcessedData.getGeneratePinBlock().toString())

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

            //     0|37|4|00,374245,53,49,01,0,3201,,00000003201,3,1200,00000001088,,00000003264,,,,,,0,

            var result = getField58(emiCustomerDetails)

            //  val indicator = "$cardIndFirst|$firstTwoDigitFoCard|$cdtIndex|$accSellection"//used for visa// used for ruppay//"0|54|2|00"
            val indicator = "$cardIndFirst|$firstTwoDigitFoCard|$cdtIndex|$accSellection$result"
            //println("Indicator value is "+indicator)

            AppPreference.saveString(terminalData.invoiceNumber, indicator)

            addFieldByHex(58, indicator)

            //Batch Number
            addFieldByHex(60, addPad(terminalData.batchNumber, "0", 6, true))

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
            val customerID = HexStringConverter.addPreFixer(
                issuerParameterTable?.customerIdentifierFiledType,
                2
            )

            val walletIssuerID = HexStringConverter.addPreFixer(issuerParameterTable?.issuerId, 2)
            addFieldByHex(
                61, addPad(
                    AppPreference.getString("serialNumber"), " ", 15, false
                ) + AppPreference.getBankCode() + customerID + walletIssuerID + data
            )

            //adding field 62
            addFieldByHex(62, terminalData.invoiceNumber)

            //Here we are Saving Date , Time and TimeStamp in CardProcessedDataModal:-
            var year = "Year"
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
        }
    }

    private fun getField58(emiDetails: EmiCustomerDetails?): String {
        var bankorInstaEMI = cardProcessedData.getEmiType()
        var result = ""
        if (emiDetails != null) {
            result =
                ",${emiDetails.emiBin},${emiDetails.issuerId},${emiDetails.emiSchemeId},${emiDetails.brandId},${emiDetails.productId}," +
                        "${emiDetails.transactionAmt},${emiDetails.cashDiscountAmt},${
                            addPad(
                                emiDetails.loanAmt, "0", 11
                            )
                        },${emiDetails.tenure},${emiDetails.roi}," +
                        "${
                            addPad(
                                emiDetails.monthlyEmi, "0", 11
                            )
                        },${emiDetails.cashback},${
                            addPad(
                                emiDetails.netPay, "0", 11
                            )
                        }," +
                        "${cardProcessedData.getMobileBillExtraData()?.second},${emiDetails.serialNo},${emiDetails.customerName},${cardProcessedData.getMobileBillExtraData()?.first},${emiDetails.email},${bankorInstaEMI},"
        }
        return result
    }
}