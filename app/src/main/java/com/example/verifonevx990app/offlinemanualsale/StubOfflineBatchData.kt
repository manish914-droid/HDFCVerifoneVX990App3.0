package com.example.verifonevx990app.offlinemanualsale

import com.example.verifonevx990app.R
import com.example.verifonevx990app.main.PosEntryModeType
import com.example.verifonevx990app.realmtables.BatchFileDataTable
import com.example.verifonevx990app.realmtables.CardDataTable
import com.example.verifonevx990app.realmtables.IssuerParameterTable
import com.example.verifonevx990app.realmtables.TerminalParameterTable
import com.example.verifonevx990app.utils.MoneyUtil
import com.example.verifonevx990app.utils.TransactionTypeValues
import com.example.verifonevx990app.vxUtils.*
import java.text.SimpleDateFormat
import java.util.*

class StubOfflineBatchData(
    private var transactionAmount: Long,
    private var field57Data: String,
    var cardNumber: String,
    var authCode: String,
    var batchStubCallback: (BatchFileDataTable) -> Unit
) {

    var date: Long? = null
    private var simpleTimeFormat: SimpleDateFormat? = null
    private var formatTime: String? = null
    private var formatDate: String? = null
    var timeStamp: String? = null

    init {
        batchStubCallback(saveOfflineSaleBatchData())
    }

    //Below method is used to save offline sale data in batch table:-
    private fun saveOfflineSaleBatchData(): BatchFileDataTable {
        val terminalData = TerminalParameterTable.selectFromSchemeTable()
        val issuerParameterTable =
            IssuerParameterTable.selectFromIssuerParameterTable(AppPreference.WALLET_ISSUER_ID)
        val cardDataTable = CardDataTable.selectFirstCardTableData()
        val batchFileData = BatchFileDataTable()

        //Auto Increment Invoice Number in BatchFileData Table:-
        batchFileData.serialNumber = AppPreference.getString("serialNumber")
        batchFileData.sourceNII = Nii.SOURCE.nii
        batchFileData.destinationNII = Nii.DEFAULT.nii
        batchFileData.mti = Mti.DEFAULT_MTI.mti
        batchFileData.transactionType = TransactionType.OFFLINE_SALE.type
        batchFileData.transactionalAmmount = transactionAmount.toString()
        batchFileData.nii = Nii.DEFAULT.nii
        batchFileData.merchantName = terminalData?.receiptHeaderOne ?: ""
        batchFileData.panMask = terminalData?.panMask ?: ""
        batchFileData.panMaskConfig = terminalData?.panMaskConfig ?: ""
        batchFileData.panMaskFormate = terminalData?.panMaskFormate ?: ""
        batchFileData.merchantAddress1 = terminalData?.receiptHeaderTwo ?: ""
        batchFileData.merchantAddress2 = terminalData?.receiptHeaderThree ?: ""

        batchFileData.time = formatTime ?: ""
        batchFileData.date = formatDate ?: ""
        batchFileData.mid = terminalData?.merchantId ?: ""
        batchFileData.posEntryValue =
            PosEntryModeType.OFFLINE_SALE_POS_ENTRY_CODE.posEntry.toString()
        batchFileData.batchNumber = terminalData?.batchNumber ?: ""

        batchFileData.roc = ROCProviderV2.getRoc(AppPreference.getBankCode()).toString()
        batchFileData.invoiceNumber = terminalData?.invoiceNumber.toString()
        batchFileData.track2Data = field57Data

        batchFileData.terminalSerialNumber = AppPreference.getString("serialNumber")
        batchFileData.bankCode = AppPreference.getBankCode()
        batchFileData.customerId = issuerParameterTable?.customerIdentifierFiledType ?: ""
        batchFileData.walletIssuerId = AppPreference.WALLET_ISSUER_ID
        batchFileData.connectionType = ConnectionType.GPRS.code
        batchFileData.modelName = AppPreference.getString("deviceModel")
        batchFileData.appName = VerifoneApp.appContext.getString(R.string.app_name)
        batchFileData.appVersion = addPad(getAppVersionNameAndRevisionID(), "0", 15, false)
        batchFileData.pcNumber = AppPreference.getString(AppPreference.PC_NUMBER_KEY)
        batchFileData.authCode = this.authCode
        //batchFileData.operationType = isoPackageWriter.operationType(Need to Discuss by Ajay)
        batchFileData.transationName =
            TransactionTypeValues.getTransactionStringType(TransactionType.OFFLINE_SALE.type)
        batchFileData.cardType = cardDataTable?.cardLabel ?: ""
        batchFileData.isPinverified = true
        if (AppPreference.getBankCode() == "07")
            batchFileData.cardHolderName = VerifoneApp.appContext.getString(R.string.amex)
        else
            batchFileData.cardHolderName = VerifoneApp.appContext.getString(R.string.hdfc)

        batchFileData.baseAmmount =
            MoneyUtil.fen2yuan(transactionAmount)
                .toString()
        val cashBackAmount = 0L
        if (cashBackAmount.toString().isNotEmpty() && cashBackAmount.toString() != "0") {
            batchFileData.cashBackAmount =
                MoneyUtil.fen2yuan(cashBackAmount).toString()
            if (TransactionType.OFFLINE_SALE.type != TransactionTypeValues.CASH_AT_POS)
                batchFileData.totalAmmount = MoneyUtil.fen2yuan(
                    transactionAmount
                ).toString()
            else
                batchFileData.totalAmmount =
                    MoneyUtil.fen2yuan(
                        transactionAmount
                    )
                        .toString()
        } else
            batchFileData.totalAmmount =
                MoneyUtil.fen2yuan(transactionAmount)
                    .toString()

        batchFileData.tid = terminalData?.terminalId ?: ""
        batchFileData.discaimerMessage = issuerParameterTable?.volletIssuerDisclammer ?: ""
        batchFileData.isTimeOut = false

        batchFileData.f48IdentifierWithTS = ConnectionTimeStamps.getFormattedStamp()
        batchFileData.cardNumber =
            getMaskedPan(TerminalParameterTable.selectFromSchemeTable(), cardNumber).toString()
        val cdtIndex = CardDataTable.selectFromCardDataTable(cardNumber)?.cardTableIndex
        val cardIndFirst = "0"
        val firstTwoDigitFoCard = cardNumber.substring(0, 2)
        val accSellection = addPad(AppPreference.getString(AppPreference.ACC_SEL_KEY), "0", 2)
        val indicatorData = "$cardIndFirst|$firstTwoDigitFoCard|$cdtIndex|$accSellection"
        batchFileData.indicator = indicatorData
        batchFileData.isOfflineSale = true

        try {
            val date: Long = Calendar.getInstance().timeInMillis
            val timeFormater = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
            val timeFormaterDisplay = SimpleDateFormat("HHmmss", Locale.getDefault())
            batchFileData.time = timeFormater.format(date)
            val dateFormater = SimpleDateFormat("MMdd", Locale.getDefault())
            val timeStampFormattted = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
            batchFileData.date = dateFormater.format(date)
            batchFileData.printDate = timeStampFormattted.format(date)
            val calender = Calendar.getInstance()
            val currentYearData = calender.get(Calendar.YEAR)
            val currentTimeData = timeFormaterDisplay.format(date)
            batchFileData.currentTime = currentTimeData
            batchFileData.currentYear = currentYearData.toString().substring(2, 4)
            batchFileData.timeStamp = date
            batchFileData.transactionDate = dateFormater(date)
            batchFileData.transactionTime = timeFormater(date)
        } catch (ex: Exception) {
            ex.printStackTrace()
        }

        saveTableInDB(batchFileData)
        return batchFileData
    }
}