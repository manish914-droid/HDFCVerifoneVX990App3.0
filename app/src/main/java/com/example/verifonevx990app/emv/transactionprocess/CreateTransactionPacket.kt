package com.example.verifonevx990app.emv.transactionprocess

import android.text.TextUtils
import android.util.Log
import com.example.verifonevx990app.R
import com.example.verifonevx990app.bankemi.BankEMIDataModal
import com.example.verifonevx990app.bankemi.BankEMIIssuerTAndCDataModal
import com.example.verifonevx990app.brandemi.BrandEMIDataModal
import com.example.verifonevx990app.brandemibyaccesscode.BrandEMIAccessDataModal
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
    private var bankEmiTandCData: BankEMIIssuerTAndCDataModal? = null, private var brandEMIByAccessCodeDataModel: BrandEMIAccessDataModal?=null, private var brandEMIData: BrandEMIDataModal?=null
) :
    ITransactionPacketExchange {
    private var indicator: String? = null
  //  private var brandEMIData: brandEMIData? = null
  //  private var brandEMIByAccessCodeData: BrandEMIAccessDataModalTable? = null

    //Below method is used to create Transaction Packet in all cases:-
    init {
        createTransactionPacket()
    }

    override fun createTransactionPacket(): IsoDataWriter = IsoDataWriter().apply {
        //Condition To Check TransactionType == BrandEMIByAccessCode if it is then fetch its value from DB:-
        /*if (cardProcessedData.getTransType() == TransactionType.BRAND_EMI_BY_ACCESS_CODE.type) {
            brandEMIByAccessCodeData =
                runBlocking(Dispatchers.IO) { BrandEMIAccessDataModalTable.getBrandEMIByAccessCodeData() }
        }*/


       /* if (cardProcessedData.getTransType() == TransactionType.BRAND_EMI.type) {
           // todo same
          //  brandEMIData = runBlocking(Dispatchers.IO) { brandEMIData.getAllEMIData() }
        }*/
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
                    addPad(brandEMIByAccessCodeDataModel?.transactionAmount ?: "", "0", 12, true)
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
                            "${bankEmiTandCData?.issuerID}," +
                            "${bankEmiTandCData?.emiSchemeID},1,0,${cardProcessedData.getEmiTransactionAmount()}," +
                            "${bankEmiSchemeData?.discountAmount},${bankEmiSchemeData?.loanAmount},${bankEmiSchemeData?.tenure}," +
                            "${bankEmiSchemeData?.tenureInterestRate},${bankEmiSchemeData?.emiAmount},${bankEmiSchemeData?.cashBackAmount}," +
                            "${bankEmiSchemeData?.netPay},${cardProcessedData.getMobileBillExtraData()?.second ?: ""}," +
                            ",,${cardProcessedData.getMobileBillExtraData()?.first ?: ""},,0,${bankEmiSchemeData?.processingFee},${bankEmiSchemeData?.processingRate}," +
                            "${bankEmiSchemeData?.totalProcessingFee},,"
/*0|60|5|00,60832632,52,82,1,0,5666,224,5442,6,1400,944,0,5664,23,,,,8920868887,,0,0,0,0,,*/

                                    /*
                                    netpay
                                    bill invoice no-                                                ,
                                    serial number -                                                              ,
                                    customer name -                                                                         ,
                                    mobile no-                                                                                     ,
                                    email id -                                                                                        ,
                                    insta emi -                                                                                      0,
                                    processing fee-                                                              0,
                                    processing rate -                                                           0,
                                    processing amount total-                            0,
                                    emi code -                                           */
                    // mobile before mobile  --> bill,emie,phonenum
                }
/*0|46|1|00,460133,54,135,25,586,650000,0,635960,3,1300,216596,14040,635748,12,8,,8287305603,,0,0,0,0,,*/
                TransactionType.BRAND_EMI.type -> {
                    var imeiOrSerialNo:String?=null
                    if(brandEMIData?.imeiORserailNum !="" ){
                        imeiOrSerialNo=brandEMIData?.imeiORserailNum
                    }

                    indicator = "$cardIndFirst|$firstTwoDigitFoCard|$cdtIndex|$accSellection," +
                            "${cardProcessedData.getPanNumberData()?.substring(0, 8)}," +
                            "${bankEmiTandCData?.issuerID},${bankEmiTandCData?.emiSchemeID},${brandEMIData?.brandID}," +
                            "${brandEMIData?.productID},${cardProcessedData.getEmiTransactionAmount()}," +
                            "${bankEmiSchemeData?.discountAmount},${bankEmiSchemeData?.loanAmount},${bankEmiSchemeData?.tenure}," +
                            "${bankEmiSchemeData?.tenureInterestRate},${bankEmiSchemeData?.emiAmount},${bankEmiSchemeData?.cashBackAmount}," +
                            "${bankEmiSchemeData?.netPay},${cardProcessedData.getMobileBillExtraData()?.second ?: ""}," +
                            "${imeiOrSerialNo ?: ""},,${cardProcessedData.getMobileBillExtraData()?.first ?: ""},,0,${bankEmiSchemeData?.processingFee},${bankEmiSchemeData?.processingRate}," +
                            "${bankEmiSchemeData?.totalProcessingFee},,"
                }
/*                0|43|1|00,438628,54,142,11,2358,1000000,0,1000000,3,1300,340581,0,1041743,,abcdxyz,,,,0,0,200.0,20000,42942319,
                  0|60|5|00,60832632,52,144,11,2356,800000,18320,781680,3,1400,266663,0,815623,,12qw3e,,,,0,0,200.0,15634,52429840,*/
                TransactionType.BRAND_EMI_BY_ACCESS_CODE.type -> {
                    indicator = "$cardIndFirst|$firstTwoDigitFoCard|$cdtIndex|$accSellection," +
                            "${cardProcessedData.getPanNumberData()?.substring(0, 8)}," +
                            "${brandEMIByAccessCodeDataModel?.issuerID},${brandEMIByAccessCodeDataModel?.emiSchemeID},${brandEMIByAccessCodeDataModel?.brandID}," +
                            "${brandEMIByAccessCodeDataModel?.productID},${brandEMIByAccessCodeDataModel?.transactionAmount}," +
                            "${brandEMIByAccessCodeDataModel?.discountAmount},${brandEMIByAccessCodeDataModel?.loanAmount},${brandEMIByAccessCodeDataModel?.tenure}," +
                            "${brandEMIByAccessCodeDataModel?.interestAmount},${brandEMIByAccessCodeDataModel?.emiAmount},${brandEMIByAccessCodeDataModel?.cashBackAmount}," +
                            "${brandEMIByAccessCodeDataModel?.netPayAmount},${cardProcessedData.getMobileBillExtraData()?.second ?: ""}," +
                            "${brandEMIByAccessCodeDataModel?.productSerialCode ?: ""},,${cardProcessedData.getMobileBillExtraData()?.first ?: ""},,0,${brandEMIByAccessCodeDataModel?.processingFee},${brandEMIByAccessCodeDataModel?.processingFeeRate}," +
                            "${brandEMIByAccessCodeDataModel?.totalProcessingFee},${brandEMIByAccessCodeDataModel?.emiCode},"
                }

                else -> {
                    indicator = if( cardProcessedData.getTransType()==TransactionType.TEST_EMI.type ){
                            logger("TEST OPTION",cardProcessedData.testEmiOption,"e")
                        "$cardIndFirst|$firstTwoDigitFoCard|$cdtIndex|$accSellection|${cardProcessedData.testEmiOption}"
                    }else
                        "$cardIndFirst|$firstTwoDigitFoCard|$cdtIndex|$accSellection"
                }
            }

            Log.d("SALE Indicator:- ", indicator.toString())
            additionalData["indicatorF58"] = indicator ?: ""
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
            val pcNumbers = addPad(AppPreference.getString(AppPreference.PC_NUMBER_KEY), "0", 9)+addPad(AppPreference.getString(AppPreference.PC_NUMBER_KEY_2), "0", 9)
            val data = ConnectionType.GPRS.code + addPad(
                AppPreference.getString("deviceModel"),
                " ",
                6,
                false
            ) + addPad(VerifoneApp.appContext.getString(R.string.app_name), " ", 10, false) +
                    version + pcNumbers
            /* val customerID = HexStringConverter.addPreFixer(
                 issuerParameterTable?.customerIdentifierFiledType,
                 2
             )*/
            val customerID =
                issuerParameterTable?.customerIdentifierFiledType?.let { addPad(it, "0", 2) } ?: 0

          //  val walletIssuerID = issuerParameterTable?.issuerId?.let { addPad(it, "0", 2) } ?: 0

            val walletIssuerID = if (cardProcessedData.getTransType() == TransactionType.EMI_SALE.type || cardProcessedData.getTransType() == TransactionType.BRAND_EMI.type) {
                bankEmiTandCData?.issuerID?.let { addPad(it, "0", 2) } ?: 0
            }
            else if( cardProcessedData.getTransType() == TransactionType.BRAND_EMI_BY_ACCESS_CODE.type){
                brandEMIByAccessCodeDataModel?.issuerID?.let { addPad(it, "0", 2) } ?: 0
            }
            else {
                issuerParameterTable?.issuerId?.let { addPad(it, "0", 2) } ?: 0
            }


          // old way
         //   val walletIssuerID = issuerParameterTable?.issuerId?.let { addPad(it, "0", 2) } ?: 0

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