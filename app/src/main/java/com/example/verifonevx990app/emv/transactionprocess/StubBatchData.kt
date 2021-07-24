package com.example.verifonevx990app.emv.transactionprocess

import com.example.verifonevx990app.BuildConfig
import com.example.verifonevx990app.R
import com.example.verifonevx990app.bankemi.BankEMIDataModal
import com.example.verifonevx990app.bankemi.BankEMIIssuerTAndCDataModal
import com.example.verifonevx990app.brandemibyaccesscode.BrandEMIAccessDataModal
import com.example.verifonevx990app.main.DetectCardType
import com.example.verifonevx990app.realmtables.*
import com.example.verifonevx990app.utils.TransactionTypeValues
import com.example.verifonevx990app.vxUtils.*
import com.google.gson.Gson
import com.vfi.smartpos.deviceservice.aidl.IEMV
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.*

class StubBatchData(private var de55: String?,var transactionType: Int, var cardProcessedDataModal: CardProcessedDataModal, private var printExtraData: Triple<String, String, String>?, private val field60Data: String, batchStubCallback: (BatchFileDataTable) -> Unit) {

    var vfIEMV: IEMV? = null

    init {
        vfIEMV = VFService.vfIEMV
        batchStubCallback(stubbingData())
    }

    //Below method is used to save Batch Data in BatchFileDataTable in DB and print the Transaction Slip:-
    private fun stubbingData(): BatchFileDataTable {
        val terminalData = TerminalParameterTable.selectFromSchemeTable()
        val issuerParameterTable =
            IssuerParameterTable.selectFromIssuerParameterTable(AppPreference.WALLET_ISSUER_ID)
        val cardDataTable = CardDataTable.selectFromCardDataTable(
            cardProcessedDataModal.getPanNumberData().toString()
        )
        val batchFileData = BatchFileDataTable()

        //Below we are saving Transaction related CardProcessedDataModal Data in BatchFileDataTable object to save in DB:-
        batchFileData.serialNumber = AppPreference.getString("serialNumber")
        batchFileData.sourceNII = Nii.SOURCE.nii
        batchFileData.destinationNII = Nii.DEFAULT.nii
        batchFileData.mti = Mti.DEFAULT_MTI.mti
        batchFileData.transactionType = transactionType

        batchFileData.emiTransactionAmount =
            (cardProcessedDataModal.getEmiTransactionAmount() ?: 0L).toString()
        batchFileData.nii = Nii.DEFAULT.nii
        batchFileData.applicationPanSequenceNumber =
            cardProcessedDataModal.getApplicationPanSequenceValue() ?: ""
        batchFileData.merchantName = terminalData?.receiptHeaderOne ?: ""
        batchFileData.panMask = terminalData?.panMask ?: ""
        batchFileData.panMaskConfig = terminalData?.panMaskConfig ?: ""
        batchFileData.panMaskFormate = terminalData?.panMaskFormate ?: ""
        batchFileData.merchantAddress1 = terminalData?.receiptHeaderTwo ?: ""
        batchFileData.merchantAddress2 = terminalData?.receiptHeaderThree ?: ""
        batchFileData.timeStamp = cardProcessedDataModal.getTimeStamp()?.toLong() ?: 0L
        batchFileData.transactionDate =
            dateFormater(cardProcessedDataModal.getTimeStamp()?.toLong() ?: 0L)
        batchFileData.transactionTime =
            timeFormater(cardProcessedDataModal.getTime()?.toLong() ?: 0L)
        batchFileData.time = cardProcessedDataModal.getTime() ?: ""
        batchFileData.date = cardProcessedDataModal.getDate() ?: ""
        batchFileData.mid = terminalData?.merchantId ?: ""
        batchFileData.posEntryValue = cardProcessedDataModal.getPosEntryMode() ?: ""
        batchFileData.batchNumber = invoiceWithPadding(terminalData?.batchNumber ?: "")
        val roc = ROCProviderV2.getRoc(AppPreference.getBankCode()) - 1
        batchFileData.roc =
            roc.toString()//ROCProviderV2.getRoc(AppPreference.getBankCode()).toString()
        //      batchFileData.invoiceNumber = invoiceIncrementValue.toString()

        batchFileData.track2Data =
            if (transactionType != TransactionTypeValues.PRE_AUTH_COMPLETE) {
                cardProcessedDataModal.getTrack2Data() ?: ""
            } else {
                ""//isoPackageReader.field57 (Need to Check by Ajay)
            }

        batchFileData.terminalSerialNumber = AppPreference.getString("serialNumber")
        batchFileData.bankCode = AppPreference.getBankCode()
        batchFileData.customerId = issuerParameterTable?.customerIdentifierFiledType ?: ""
        batchFileData.walletIssuerId = AppPreference.WALLET_ISSUER_ID
        batchFileData.connectionType = ConnectionType.GPRS.code
        batchFileData.modelName = AppPreference.getString("deviceModel")
        batchFileData.appName = VerifoneApp.appContext.getString(R.string.app_name)
        val buildDate: String =
            SimpleDateFormat("yyMMdd", Locale.getDefault()).format(Date(BuildConfig.TIMESTAMP))
        batchFileData.appVersion = addPad(getAppVersionNameAndRevisionID(), "0", 15, false)
        batchFileData.pcNumber = AppPreference.getString(AppPreference.PC_NUMBER_KEY)
        //batchFileData.operationType = isoPackageWriter.operationType(Need to Discuss by Ajay)
        batchFileData.transationName =
            TransactionTypeValues.getTransactionStringType(transactionType)
        when (cardProcessedDataModal.getReadCardType()) {
            DetectCardType.MAG_CARD_TYPE -> {
                batchFileData.cardType = cardDataTable?.cardLabel ?: ""
            }
            else -> {
                batchFileData.cardType = cardProcessedDataModal.getcardLabel() ?: ""
            }
        }

        batchFileData.isPinverified = true

        batchFileData.nocvm = cardProcessedDataModal.getNoCVM() ?: false
        batchFileData.ctlsCaption= terminalData?.ctlsCaption?:""

        //Saving card number in mask form because we don't save the pan number in Plain text.
        batchFileData.cardNumber =
            if (transactionType != TransactionType.PRE_AUTH_COMPLETE.type) {
                getMaskedPan(
                    TerminalParameterTable.selectFromSchemeTable(),
                    cardProcessedDataModal.getPanNumberData() ?: ""
                )
            } else {
                getMaskedPan(
                    TerminalParameterTable.selectFromSchemeTable(),
                    cardProcessedDataModal.getTrack2Data() ?: ""
                )
            }

        batchFileData.de55 = de55 ?: "" //for Rupay

        //batchFileData.detectedCardType=cardProcessedDataModal.getReadCardType()?:DetectCardType.CARD_ERROR_TYPE
        batchFileData.operationType =
            cardProcessedDataModal.getReadCardType()?.cardTypeName.toString()
        //batchFileData.expiry = isoPackageWriter.expiryDate (Need to Discuss by Ajay)
        if (AppPreference.getBankCode() == "07")
            batchFileData.cardHolderName = cardProcessedDataModal.getCardHolderName() ?: "Amex"
        else
            batchFileData.cardHolderName = cardProcessedDataModal.getCardHolderName()
                ?: VerifoneApp.appContext.getString(R.string.hdfc)
        //batchFileData.indicator = isoPackageWriter.indicator (Need to Discuss by Ajay)
        batchFileData.field55Data = cardProcessedDataModal.getFiled55() ?: ""


        // setting base amount
        // ( getOtherAmount is not zero in CAsh at pos And sale with Cash type other then this it will be zero)

        var baseAmount = 0L
        var cashAmount = 0L
        var totalAmount = 0L
        var saleWithTipAmount = 0L
        when (cardProcessedDataModal.getTransType()) {
            TransactionType.SALE_WITH_CASH.type -> {
                baseAmount = cardProcessedDataModal.getSaleAmount() ?: 0L
                cashAmount = cardProcessedDataModal.getOtherAmount() ?: 0L
                totalAmount = cardProcessedDataModal.getTransactionAmount() ?: 0L
                batchFileData.baseAmmount = (baseAmount).toString()
                batchFileData.cashBackAmount = (cashAmount).toString()
                batchFileData.totalAmmount = (totalAmount).toString()
                // this is used in settlement and iso packet amount
                batchFileData.transactionalAmmount =
                    cardProcessedDataModal.getTransactionAmount().toString()
            }
            else -> {
                baseAmount = cardProcessedDataModal.getTransactionAmount() ?: 0L
                totalAmount = cardProcessedDataModal.getTransactionAmount() ?: 0L
                saleWithTipAmount = cardProcessedDataModal.getTipAmount() ?: 0L
                batchFileData.baseAmmount = (baseAmount).toString()
                batchFileData.tipAmmount = (saleWithTipAmount).toString()
                batchFileData.cashBackAmount = (cashAmount).toString()
                batchFileData.totalAmmount = (totalAmount).toString()
                // this is used in settlement and iso packet amount
                batchFileData.transactionalAmmount =
                    cardProcessedDataModal.getTransactionAmount().toString()
            }
        }

        batchFileData.authCode = cardProcessedDataModal.getAuthCode() ?: ""
        //   batchFileData.invoiceNumber = invoiceIncrementValue.toString()
        batchFileData.tid = terminalData?.terminalId ?: ""
        batchFileData.discaimerMessage = issuerParameterTable?.volletIssuerDisclammer ?: ""
        batchFileData.isTimeOut = false

        batchFileData.f48IdentifierWithTS = ConnectionTimeStamps.getFormattedStamp()

        //Setting AID , TVR and TSI into BatchFileDataTable here:-

        when (cardProcessedDataModal.getReadCardType()) {

            DetectCardType.EMV_CARD_TYPE -> {
                batchFileData.tvr = printExtraData?.first ?: ""
                batchFileData.aid = printExtraData?.second ?: ""
                batchFileData.tsi = printExtraData?.third ?: ""
            }

            DetectCardType.CONTACT_LESS_CARD_TYPE -> {
                /*   val aidArray = arrayOf("0x9F06")
               val aidData = vfIEMV?.getAppTLVList(aidArray)*/
                var aidData = cardProcessedDataModal.getAIDPrint() ?: ""
                //println("Aid Data is ----> $aidData")
                //val formattedAid = aidData?.subSequence(6, aidData.length)
                batchFileData.aid = cardProcessedDataModal.getAIDPrint() ?: ""
            }
            else -> {
            }
        }

        batchFileData.isPinverified =
            cardProcessedDataModal.getIsOnline() == 1 || cardProcessedDataModal.getIsOnline() == 2

        batchFileData.referenceNumber =
            hexString2String(cardProcessedDataModal.getRetrievalReferenceNumber() ?: "")
        batchFileData.tc = cardProcessedDataModal.getTC() ?: ""
        //  batchFileData.track2Data = cardProcessedDataModal.getTrack2Data() ?: "0000000"

        batchFileData.authBatchNO = cardProcessedDataModal.getAuthBatch().toString()
        batchFileData.authROC = cardProcessedDataModal.getAuthRoc().toString()
        batchFileData.authTID = cardProcessedDataModal.getAuthTid().toString()
        batchFileData.encryptPan = cardProcessedDataModal.getEncryptedPan() ?: ""
        batchFileData.amountInResponse = cardProcessedDataModal.getAmountInResponse().toString()
        batchFileData.aqrRefNo = cardProcessedDataModal.getAcqReferalNumber().toString()


        val cardIndFirst = "0"
        val firstTwoDigitFoCard = cardProcessedDataModal.getPanNumberData()?.substring(0, 2)
        //  val cardDataTable = CardDataTable.selectFromCardDataTable(cardProcessedData.getTrack2Data()!!)
        val cdtIndex = cardDataTable?.cardTableIndex ?: ""
        val accSellection =
            addPad(
                AppPreference.getString(AppPreference.ACC_SEL_KEY),
                "0",
                2
            ) //cardDataTable.getA//"00"

        val mIndicator =
            "$cardIndFirst|$firstTwoDigitFoCard|$cdtIndex|$accSellection"//used for visa// used for ruppay//"0|54|2|00"
        batchFileData.indicator = mIndicator

        batchFileData.indicator=cardProcessedDataModal.indicatorF58


        var innvoice = terminalData?.invoiceNumber?.toInt()
        if (innvoice != null) {
            innvoice += 1
        }

        batchFileData.invoiceNumber = terminalData?.invoiceNumber.toString()

        /*  terminalData?.invoiceNumber = innvoice?.let { addPad(it, "0", 6, true) }.toString()

          TerminalParameterTable.performOperation(terminalData!!) {
              logger("Invoice", terminalData.invoiceNumber + "  update")
          }
          */
        TerminalParameterTable.updateTerminalDataInvoiceNumber(terminalData?.invoiceNumber.toString())

        //Here we are putting Refund Transaction Status in Batch Table:-
        if (cardProcessedDataModal.getProcessingCode() == ProcessingCode.REFUND.code) {
            batchFileData.isRefundSale = true
        }

        val calender = Calendar.getInstance()
        val currentYearData = calender.get(Calendar.YEAR)
        batchFileData.currentYear = currentYearData.toString().substring(2, 4)

        //Mobile Number and Bill Number Save in BatchTable here:-
        batchFileData.merchantMobileNumber =
            cardProcessedDataModal.getMobileBillExtraData()?.first ?: ""
        batchFileData.merchantBillNumber =
            cardProcessedDataModal.getMobileBillExtraData()?.second ?: ""

        if (batchFileData.transactionType != TransactionType.PRE_AUTH.type) {
            val lastSuccessReceiptData = Gson().toJson(batchFileData)
            AppPreference.saveString(AppPreference.LAST_SUCCESS_RECEIPT_KEY, lastSuccessReceiptData)
        }
        // region =======saving extra fields for printing and for reports i.e ROC,INVOICE,TID ,MID,etc..

        val f60DataList = field60Data.split('|')
        //   Auto settle flag | Bank id| Issuer id | MID | TID | Batch No | Stan | Invoice | Card Type
//0|1|51|000000041501002|41501369|000150|260|000260|RUPAY|
        return try {
            batchFileData.hostBankID = f60DataList[1]
            batchFileData.hostIssuerID = f60DataList[2]
            batchFileData.hostMID = f60DataList[3]
            batchFileData.hostTID = f60DataList[4]
            batchFileData.hostBatchNumber = f60DataList[5]
            batchFileData.hostRoc = f60DataList[6]
            batchFileData.hostInvoice = f60DataList[7]
            batchFileData.hostCardType = f60DataList[8]
            if (batchFileData.transactionType != TransactionType.PRE_AUTH.type) {
                val lastSuccessReceiptData = Gson().toJson(batchFileData)
                AppPreference.saveString(AppPreference.LAST_SUCCESS_RECEIPT_KEY, lastSuccessReceiptData)
            }

            batchFileData
        } catch (ex: Exception) {
            ex.printStackTrace()
            batchFileData
        }
        //endregion============
    }
}

// Here We are stubbing emi data into batch record and save it in BatchFile.
fun stubEMI(
    batchData: BatchFileDataTable,
    emiCustomerDetails: BankEMIDataModal?,
    emiIssuerTAndCData: BankEMIIssuerTAndCDataModal?, brandEMIByAccessCodeData: BrandEMIAccessDataModal?,
    batchStubCallback: (BatchFileDataTable) -> Unit
) {
    GlobalScope.launch(Dispatchers.IO) {
        //For emi find the details from EMI
        if (batchData.transactionType == TransactionType.BRAND_EMI_BY_ACCESS_CODE.type) {

           // val brandEMIByAccessCodeData = BrandEMIAccessDataModalTable.getBrandEMIByAccessCodeData()
               if(brandEMIByAccessCodeData!=null) {
                   withContext(Dispatchers.Main) {
                       batchData.tenure = brandEMIByAccessCodeData.tenure
                       batchData.issuerId = brandEMIByAccessCodeData.issuerID
                       batchData.emiSchemeId = brandEMIByAccessCodeData.emiSchemeID
                       batchData.issuerName = brandEMIByAccessCodeData.issuerName
                       batchData.bankEmiTAndC = brandEMIByAccessCodeData.schemeTAndC
                       batchData.tenureTAndC = brandEMIByAccessCodeData.schemeTenureTAndC
                       batchData.tenureWiseDBDTAndC = brandEMIByAccessCodeData.schemeDBDTAndC
                       batchData.discountCalculatedValue =
                           brandEMIByAccessCodeData.discountCalculatedValue
                       batchData.cashBackCalculatedValue =
                           brandEMIByAccessCodeData.cashBackCalculatedValue
                       batchData.transactionAmt = brandEMIByAccessCodeData.transactionAmount
                       batchData.cashDiscountAmt = brandEMIByAccessCodeData.discountAmount
                       batchData.loanAmt = brandEMIByAccessCodeData.loanAmount
                       batchData.roi = brandEMIByAccessCodeData.interestAmount
                       batchData.monthlyEmi = brandEMIByAccessCodeData.emiAmount
                       batchData.cashback = brandEMIByAccessCodeData.cashBackAmount
                       batchData.netPay = brandEMIByAccessCodeData.netPayAmount
                       batchData.processingFee = brandEMIByAccessCodeData.processingFee
                       batchData.totalInterest = brandEMIByAccessCodeData.totalInterest
                       batchData.emiTransactionAmount = brandEMIByAccessCodeData.transactionAmount
                       batchData.transactionalAmmount = brandEMIByAccessCodeData.transactionAmount
                       batchData.baseAmmount = brandEMIByAccessCodeData.transactionAmount
                       batchData.totalAmmount = brandEMIByAccessCodeData.transactionAmount

                   }
               }

        } else {
            batchData.tenure = emiCustomerDetails?.tenure.toString()
            batchData.issuerId = emiIssuerTAndCData?.issuerID.toString()
            batchData.emiSchemeId = emiIssuerTAndCData?.emiSchemeID.toString()
            batchData.issuerName = emiIssuerTAndCData?.issuerName.toString()
            batchData.bankEmiTAndC = emiIssuerTAndCData?.schemeTAndC.toString()
            batchData.tenureTAndC = emiCustomerDetails?.tenureTAndC.toString()
            batchData.tenureWiseDBDTAndC = emiCustomerDetails?.tenureWiseDBDTAndC.toString()
            batchData.discountCalculatedValue =
                emiCustomerDetails?.discountCalculatedValue.toString()
            batchData.cashBackCalculatedValue =
                emiCustomerDetails?.cashBackCalculatedValue.toString()
            batchData.transactionAmt = emiCustomerDetails?.transactionAmount.toString()
            batchData.cashDiscountAmt = emiCustomerDetails?.discountAmount.toString()
            batchData.loanAmt = emiCustomerDetails?.loanAmount.toString()
            batchData.roi = emiCustomerDetails?.tenureInterestRate.toString()
            batchData.monthlyEmi = emiCustomerDetails?.emiAmount.toString()
            batchData.cashback = emiCustomerDetails?.cashBackAmount.toString()
            batchData.netPay = emiCustomerDetails?.netPay.toString()
            batchData.processingFee = emiCustomerDetails?.processingFee.toString()
            batchData.processingFeeAmount = emiCustomerDetails?.processingRate.toString()
            batchData.totalProcessingFee = emiCustomerDetails?.totalProcessingFee.toString()
            batchData.totalInterest = emiCustomerDetails?.totalInterestPay.toString()
        }

        val lastSuccessReceiptData = Gson().toJson(batchData)
        logger("STUBBED ", lastSuccessReceiptData, "e")
        AppPreference.saveString(AppPreference.LAST_SUCCESS_RECEIPT_KEY, lastSuccessReceiptData)
        batchStubCallback(batchData)
    }
}