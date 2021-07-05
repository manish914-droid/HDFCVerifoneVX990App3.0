package com.example.verifonevx990app.realmtables

import android.os.Parcel
import android.os.Parcelable
import com.example.verifonevx990app.R
import com.example.verifonevx990app.digiPOS.EDigiPosPaymentStatus
import com.example.verifonevx990app.transactions.EAccountType
import com.example.verifonevx990app.vxUtils.TransactionType
import com.example.verifonevx990app.vxUtils.addPad
import com.example.verifonevx990app.vxUtils.invoiceWithPadding
import io.realm.Realm
import io.realm.RealmObject
import io.realm.annotations.PrimaryKey
import io.realm.annotations.RealmClass
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.async
import kotlinx.coroutines.runBlocking
import java.io.Serializable
import java.text.SimpleDateFormat
import java.util.*

enum class EDashboardItem(
    val title: String,
    val res: Int,
    val rank: Int = 15,
    var childList: MutableList<EDashboardItem>? = null
) : Serializable {
    NONE("No Option Found", R.drawable.ic_home),
    SALE("Sale", R.drawable.sale_icon, 1),
    BANK_EMI("Bank EMI", R.drawable.emi_catalog_icon, 2),
    PREAUTH("Pre-Auth", R.drawable.pre_auth, 3),
    EMI_ENQUIRY("EMI Catalogue", R.drawable.emi_catalog_icon, 4),
    PREAUTH_COMPLETE("Pre-Auth Complete", R.drawable.ic_pre_auth_complete, 5),
    PENDING_PREAUTH("Pending Preauth", R.drawable.ic_pending_preauth, 6),
    OFFLINE_SALE("Offline Sale", R.drawable.sale_icon, 7),
    VOID_OFFLINE_SALE("Void Offline Sale", R.drawable.void_icon, 8),
    SALE_TIP("Tip Adjust", R.drawable.tip_adjust_icon, 9),
    VOID_PREAUTH("Void Preauth", R.drawable.void_icon, 10),
    REFUND("Refund", R.drawable.refund_icon, 11),
    VOID_REFUND("Void Refund", R.drawable.void_icon, 12),
    VOID_SALE("Void", R.drawable.void_icon, 13),
    CROSS_SELL("Cross Sell", R.drawable.cross_sell_icon, 14),
    SALE_WITH_CASH("Sale With Cash", R.drawable.sale_with_cash),
    CASH_ADVANCE("Cash Advance", R.drawable.ic_cash_at_pos_icon),
    BRAND_EMI("Brand EMI", R.drawable.brand_emi_icon),
    PENDING_OFFLINE_SALE("View Offline Sale", R.drawable.ic_pending_preauth),
    PRE_AUTH_CATAGORY("Pre-Auth", R.drawable.pre_auth, 9),
    MORE("View More", R.drawable.ic_arrow_down, 999),
    BONUS_PROMO("Bonus Promo", R.drawable.ic_cash_advance, 15),
    EMI_PRO("Brand EMI By Access Code", R.drawable.emi_catalog_icon, 16),
    EMI_CATALOGUE("EMI Catalogue", R.drawable.emi_catalog_icon, 17),
    BRAND_EMI_CATALOGUE("Brand EMI Catalogue", R.drawable.ic_sale, 18),
    BANK_EMI_CATALOGUE("Bank EMI Catalogue", R.drawable.ic_sale, 19),
    DIGI_POS("Digi POS", R.drawable.digipos_icon, 20),

    // just for handling the test emi not used in dashboard items
    TEST_EMI("Test Emi", R.drawable.ic_sale, 777),
    FLEXI_PAY("Flexi Pay", R.drawable.ic_cash_advance, 666),
    LESS("View Less", R.drawable.ic_arrow_up, 888),

    UPI("UPI COLLECT", R.drawable.upi_icon, 901),
    SMS_PAY("SMS PAY", R.drawable.sms_icon, 902),
    TXN_LIST("TXN LIST", R.drawable.sms_icon, 903),
    PENDING_TXN("Pending Txn", R.drawable.pending_txn, 903),
    STATIC_QR("Static QR", R.drawable.ic_qr_code, 904),
    DYNAMIC_QR("Dynamic QR", R.drawable.ic_qr_code, 905),
}

/**
 * use withRealm fun and write the query in lambda
 * withRealm is handling the thread and closing of realm object
 * */

@Synchronized
fun withRealm(realmCall: (Realm) -> Unit) = runBlocking {
    Realm.getDefaultInstance().let { re ->
        realmCall(re)
        re.close()
    }
}


@Synchronized
private fun getRealm(realmCall: (Realm) -> Unit) = GlobalScope.async {
    Realm.getDefaultInstance().run {
        realmCall(this)
        close()
    }

}


//region============Tables Mainly used in transaction(SALE, REFUND, VOID etc)==================

/**
 * Table for transaction batch data
 * */
@RealmClass
open class BatchFileDataTable() : RealmObject(), Parcelable {
    var authCode: String = ""
    var isChecked: Boolean = false
    var cashBackAmount: String = ""
    var panMaskFormate: String = ""
    var panMaskConfig: String = ""
    var panMask: String = ""
    var terminalSerialNumber: String = ""
    var responseCode: String = ""
    var tid: String = ""
    var mid: String = ""
    var batchNumber: String = ""
    var baseAmmount: String = ""
    var roc: String = ""

    @PrimaryKey
    var invoiceNumber: String = ""
    var panNumber: String = ""
    var time: String = ""
    var date: String = ""
    var printDate: String = ""
    var currentYear: String = ""
    var currentTime: String = ""
    var expiryDate: String = ""
    var cardHolderName: String = ""
    var timeStamp: Long = 0
    var genratedPinBlock: String = ""
    var field55Data: String = ""
    var track2Data: String = ""
    var transactionType: Int = 0
    var applicationPanSequenceNumber: String = ""
    var nii: String = ""
    var indicator: String = ""
    var bankCode: String = ""
    var customerId: String = ""
    var walletIssuerId: String = ""
    var connectionType: String = ""
    var modelName: String = ""
    var appName: String = ""
    var appVersion: String = ""
    var pcNumber: String = ""
    var posEntryValue: String = ""
    var transactionalAmmount: String = ""
    var mti: String = ""
    var serialNumber: String = ""
    var sourceNII: String = ""
    var destinationNII: String = ""
    var processingCode: String = ""
    var merchantName: String = ""
    var merchantAddress1: String = ""
    var merchantAddress2: String = ""
    var transactionDate: String = ""
    var transactionTime: String = ""
    var transationName: String = ""
    var cardType: String = ""
    var expiry: String = ""
    var cardNumber: String = ""

    //    var autthCode: String = ""
    var referenceNumber: String = ""
    var aid: String = ""
    var tc: String = ""
    var tipAmmount: String = ""
    var totalAmmount: String = ""
    var isPinverified: Boolean = false
    var nocvm: Boolean = false
    var discaimerMessage: String = ""
    var isMerchantCoppy = true
    var message: String = ""
    var isTimeOut: Boolean = false

    var operationType: String = ""

    var isVoid: Boolean = false

    var f48IdentifierWithTS: String = ""

    var tvr = ""
    var tsi = ""

    var aqrRefNo = ""

    var hasPromo = false

    var gccMsg = ""
    var isOfflineSale = false
    var cdtIndex = ""
    var isRefundSale = false

    // var emiData: RealmList<EmiCustomerDetails>? = null

    //EmiCustomerDetails
    var accountType = EAccountType.DEFAULT.code
    var merchantBillNo = ""
    var serialNo = ""
    var customerName = ""
    var phoneNo = ""
    var email = ""
    var emiTransactionAmount = ""

    //EMITRANSDETAIL
    var emiBin = ""
    var issuerId = ""
    var emiSchemeId = ""
    var transactionAmt = ""
    var cashDiscountAmt = ""
    var loanAmt = ""
    var tenure: String = ""
    var roi: String = ""
    var monthlyEmi = ""
    var cashback = ""
    var netPay = ""
    var processingFee = ""
    var totalInterest = ""
    var issuerName = ""
    var bankEmiTAndC = ""
    var tenureTAndC = ""
    var tenureWiseDBDTAndC = ""
    var discountCalculatedValue = ""
    var cashBackCalculatedValue = ""
    var processingFeeAmount = ""
    var totalProcessingFee = ""

    //EMI BrandDetail
    var brandId = "01"
    var productId = "0"

    //Status for Server Hit in Sale:-
    var isServerHit = false

    var merchantMobileNumber = ""
    var merchantBillNumber = ""

    //EMIAMOUNTS
    var cashBackPercent = ""
    var isCashBackInPercent = false

    fun getTransactionType(): String {
        var tTyp = ""
        for (e in TransactionType.values()) {
            if (e.ordinal == transactionType) {
                tTyp = e.txnTitle
                break
            }
        }
        return tTyp
    }

    var authROC = ""
    var authTID = ""
    var authBatchNO = ""
    var encryptPan = ""
    var amountInResponse = ""
    var isVoidPreAuth = false
    var isPreAuthComplete = false

    //Host Response Fields:-
    var hostAutoSettleFlag: String = ""

    var hostBankID: String = ""
    var hostIssuerID: String = ""
    var hostMID: String = ""
    var hostTID: String = ""
    var hostBatchNumber: String = ""
    var hostRoc: String = ""
    var hostInvoice: String = ""
    var hostCardType: String = ""
    var ctlsCaption:String=""


    private constructor(parcel: Parcel) : this() {
        authCode = parcel.readString().toString()
        isChecked = parcel.readByte() != 0.toByte()
        cashBackAmount = parcel.readString().toString()
        panMaskFormate = parcel.readString().toString()
        panMaskConfig = parcel.readString().toString()
        panMask = parcel.readString().toString()
        terminalSerialNumber = parcel.readString().toString()
        responseCode = parcel.readString().toString()
        tid = parcel.readString().toString()
        mid = parcel.readString().toString()
        batchNumber = parcel.readString().toString()
        roc = parcel.readString().toString()
        invoiceNumber = parcel.readString().toString()
        panNumber = parcel.readString().toString()
        time = parcel.readString().toString()
        date = parcel.readString().toString()
        expiryDate = parcel.readString().toString()
        cardHolderName = parcel.readString().toString()
        timeStamp = parcel.readLong()
        genratedPinBlock = parcel.readString().toString()
        field55Data = parcel.readString().toString()
        track2Data = parcel.readString().toString()
        transactionType = parcel.readInt()
        applicationPanSequenceNumber = parcel.readString().toString()
        nii = parcel.readString().toString()
        indicator = parcel.readString().toString()
        bankCode = parcel.readString().toString()
        customerId = parcel.readString().toString()
        walletIssuerId = parcel.readString().toString()
        connectionType = parcel.readString().toString()
        modelName = parcel.readString().toString()
        appName = parcel.readString().toString()
        appVersion = parcel.readString().toString()
        pcNumber = parcel.readString().toString()
        posEntryValue = parcel.readString().toString()
        transactionalAmmount = parcel.readString().toString()
        mti = parcel.readString().toString()
        serialNumber = parcel.readString().toString()
        sourceNII = parcel.readString().toString()
        destinationNII = parcel.readString().toString()
        processingCode = parcel.readString().toString()
        merchantName = parcel.readString().toString()
        merchantAddress1 = parcel.readString().toString()
        merchantAddress2 = parcel.readString().toString()
        transactionDate = parcel.readString().toString()
        transactionTime = parcel.readString().toString()
        transationName = parcel.readString().toString()
        cardType = parcel.readString().toString()
        expiry = parcel.readString().toString()
        cardNumber = parcel.readString().toString()

        referenceNumber = parcel.readString().toString()
        aid = parcel.readString().toString()
        tc = parcel.readString().toString()
        tipAmmount = parcel.readString().toString()
        totalAmmount = parcel.readString().toString()
        isPinverified = parcel.readByte() != 0.toByte()
        nocvm = parcel.readByte() != 0.toByte()
        discaimerMessage = parcel.readString().toString()
        isMerchantCoppy = parcel.readByte() != 0.toByte()
        message = parcel.readString().toString()
        isTimeOut = parcel.readByte() != 0.toByte()
        operationType = parcel.readString().toString()
        isVoid = parcel.readByte() != 0.toByte()
        f48IdentifierWithTS = parcel.readString().toString()
        tvr = parcel.readString().toString()
        tsi = parcel.readString().toString()

        aqrRefNo = parcel.readString().toString()
        cdtIndex = parcel.readString().toString()

        hasPromo = parcel.readByte() != 0.toByte()
        isOfflineSale = parcel.readByte() != 0.toByte()
        isRefundSale = parcel.readByte() != 0.toByte()
        gccMsg = parcel.readString().toString()


        //   accountType = parcel.readString().toString()
        merchantBillNo = parcel.readString().toString()
        serialNo = parcel.readString().toString()
        customerName = parcel.readString().toString()
        phoneNo = parcel.readString().toString()
        email = parcel.readString().toString()
        emiBin = parcel.readString().toString()
        issuerId = parcel.readString().toString()
        emiSchemeId = parcel.readString().toString()
        transactionAmt = parcel.readString().toString()
        cashDiscountAmt = parcel.readString().toString()
        loanAmt = parcel.readString().toString()
        tenure = parcel.readString().toString()
        roi = parcel.readString().toString()
        monthlyEmi = parcel.readString().toString()
        cashback = parcel.readString().toString()
        netPay = parcel.readString().toString()
        totalInterest = parcel.readString().toString()
        brandId = parcel.readString().toString()
        productId = parcel.readString().toString()
        processingFee = parcel.readString().toString()
        cashBackPercent = parcel.readString().toString()
        isCashBackInPercent = parcel.readByte() != 0.toByte()

        hostBankID = parcel.readString().toString()
        hostIssuerID = parcel.readString().toString()
        hostMID = parcel.readString().toString()
        hostTID = parcel.readString().toString()
        hostBatchNumber = parcel.readString().toString()
        hostRoc = parcel.readString().toString()
        hostInvoice = parcel.readString().toString()
        hostCardType = parcel.readString().toString()
        ctlsCaption = parcel.readString().toString()

    }

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeString(authCode)
        parcel.writeByte(if (isChecked) 1 else 0)
        parcel.writeString(cashBackAmount)
        parcel.writeString(panMaskFormate)
        parcel.writeString(panMaskConfig)
        parcel.writeString(panMask)
        parcel.writeString(terminalSerialNumber)
        parcel.writeString(responseCode)
        parcel.writeString(tid)
        parcel.writeString(mid)
        parcel.writeString(batchNumber)
        parcel.writeString(roc)
        parcel.writeString(invoiceNumber)
        parcel.writeString(panNumber)
        parcel.writeString(time)
        parcel.writeString(date)
        parcel.writeString(expiryDate)
        parcel.writeString(cardHolderName)
        parcel.writeLong(timeStamp)
        parcel.writeString(genratedPinBlock)
        parcel.writeString(field55Data)
        parcel.writeString(track2Data)
        parcel.writeInt(transactionType)
        parcel.writeString(applicationPanSequenceNumber)
        parcel.writeString(nii)
        parcel.writeString(indicator)
        parcel.writeString(bankCode)
        parcel.writeString(customerId)
        parcel.writeString(walletIssuerId)
        parcel.writeString(connectionType)
        parcel.writeString(modelName)
        parcel.writeString(appName)
        parcel.writeString(appVersion)
        parcel.writeString(pcNumber)
        parcel.writeString(posEntryValue)
        parcel.writeString(transactionalAmmount)
        parcel.writeString(mti)
        parcel.writeString(serialNumber)
        parcel.writeString(sourceNII)
        parcel.writeString(destinationNII)
        parcel.writeString(processingCode)
        parcel.writeString(merchantName)
        parcel.writeString(merchantAddress1)
        parcel.writeString(merchantAddress2)
        parcel.writeString(transactionDate)
        parcel.writeString(transactionTime)
        parcel.writeString(transationName)
        parcel.writeString(cardType)
        parcel.writeString(expiry)
        parcel.writeString(cardNumber)

        parcel.writeString(referenceNumber)
        parcel.writeString(aid)
        parcel.writeString(tc)
        parcel.writeString(tipAmmount)
        parcel.writeString(totalAmmount)
        parcel.writeByte(if (isPinverified) 1 else 0)
        parcel.writeByte(if (nocvm) 1 else 0)
        parcel.writeString(discaimerMessage)
        parcel.writeByte(if (isMerchantCoppy) 1 else 0)
        parcel.writeString(message)
        parcel.writeByte(if (isTimeOut) 1 else 0)
        parcel.writeString(operationType)
        parcel.writeByte(if (isVoid) 1 else 0)
        parcel.writeString(f48IdentifierWithTS)
        parcel.writeString(tvr)
        parcel.writeString(tsi)
        parcel.writeString(aqrRefNo)
        parcel.writeString(cdtIndex)
        parcel.writeByte(if (hasPromo) 1 else 0)
        parcel.writeByte(if (isOfflineSale) 1 else 0)
        parcel.writeByte(if (isRefundSale) 1 else 0)
        parcel.writeString(gccMsg)

        //   parcel.writeString(accountType)
        parcel.writeString(merchantBillNo)
        parcel.writeString(serialNo)
        parcel.writeString(customerName)
        parcel.writeString(phoneNo)
        parcel.writeString(email)

        parcel.writeString(emiBin)
        parcel.writeString(issuerId)
        parcel.writeString(emiSchemeId)
        parcel.writeString(transactionAmt)
        parcel.writeString(cashDiscountAmt)


        parcel.writeString(loanAmt)
        parcel.writeString(tenure)
        parcel.writeString(roi)
        parcel.writeString(monthlyEmi)
        parcel.writeString(cashback)
        parcel.writeString(netPay)
        parcel.writeString(totalInterest)
        parcel.writeString(brandId)
        parcel.writeString(productId)
        parcel.writeString(processingFee)
        parcel.writeString(cashBackPercent)
        parcel.writeByte(if (isCashBackInPercent) 1 else 0)

        parcel.writeString(hostBankID)
        parcel.writeString(hostIssuerID)
        parcel.writeString(hostMID)
        parcel.writeString(hostTID)
        parcel.writeString(hostBatchNumber)
        parcel.writeString(hostRoc)
        parcel.writeString(hostInvoice)
        parcel.writeString(hostCardType)
        parcel.writeString(ctlsCaption)

    }

    override fun describeContents(): Int {
        return 0
    }

    companion object {
        private val TAG: String = BatchFileDataTable::class.java.simpleName

        @JvmField
        val CREATOR = object : Parcelable.Creator<BatchFileDataTable> {
            override fun createFromParcel(parcel: Parcel): BatchFileDataTable {
                return BatchFileDataTable(
                        parcel
                )
            }

            override fun newArray(size: Int): Array<BatchFileDataTable> {
                return Array(size) { BatchFileDataTable() }
            }
        }

        fun performOperation(param: BatchFileDataTable) =
                withRealm {
                    it.executeTransaction { i ->
                        i.insertOrUpdate(param)
                    }
                }

        fun performOperation(param: BatchFileDataTable, callback: () -> Unit) =
                withRealm {
                    it.executeTransaction { i ->
                        i.insertOrUpdate(param)
                    }
                    callback()
                }

        fun selectBatchData(): MutableList<BatchFileDataTable> = runBlocking {
            var result = mutableListOf<BatchFileDataTable>()
            getRealm {
                val re = it.copyFromRealm(it.where(BatchFileDataTable::class.java).findAll())
                if (re != null) result = re

            }.await()
            result
        }

        fun selectAllVoidRefundBatchData(): MutableList<BatchFileDataTable> = runBlocking {
            var result = mutableListOf<BatchFileDataTable>()
            getRealm {
                val re = it.copyFromRealm(
                        it.where(BatchFileDataTable::class.java)
                                .equalTo("isRefundSale", true)
                                .findAll()
                )
                if (re != null) result = re

            }.await()
            result
        }

        fun selectOfflineSaleBatchData(): MutableList<BatchFileDataTable> = runBlocking {
            var result = mutableListOf<BatchFileDataTable>()
            getRealm {
                val re = it.copyFromRealm(
                        it.where(BatchFileDataTable::class.java)
                                .equalTo("isOfflineSale", true)
                                .findAll()
                )
                if (re != null) result = re

            }.await()
            result
        }

        fun selectRefundSaleBatchData(): MutableList<BatchFileDataTable> = runBlocking {
            var result = mutableListOf<BatchFileDataTable>()
            getRealm {
                val re = it.copyFromRealm(
                        it.where(BatchFileDataTable::class.java)
                                .equalTo("isRefundSale", true)
                                .findAll()
                )
                if (re != null) result = re

            }.await()
            result
        }

        fun updateOfflineSaleStatus(invoiceNumber: String) = runBlocking {
            getRealm {
                val invoice = addPad(invoiceNumber, "0", 6)
                val res = it.where(BatchFileDataTable::class.java)
                        .equalTo("invoiceNumber", invoice)
                        .equalTo("isOfflineSale", true)
                        .findFirst()
                it.beginTransaction()
                res?.isOfflineSale = false
                it.commitTransaction()
            }.await()
        }

        fun updateOfflineSaleTransactionType(invoiceNumber: String) = runBlocking {
            getRealm {
                val invoice = addPad(invoiceNumber, "0", 6)
                val res = it.where(BatchFileDataTable::class.java)
                        .equalTo("invoiceNumber", invoice)
                        .equalTo("transactionType", TransactionType.OFFLINE_SALE.type)
                        .findFirst()
                it.beginTransaction()
                res?.transactionType = TransactionType.VOID_OFFLINE_SALE.type
                it.commitTransaction()
            }.await()
        }

        fun selectOfflineSaleSettleBatchData(): MutableList<BatchFileDataTable> = runBlocking {
            var result = mutableListOf<BatchFileDataTable>()
            getRealm {
                val re = it.copyFromRealm(
                        it.where(BatchFileDataTable::class.java)
                                .equalTo("transactionType", TransactionType.OFFLINE_SALE.type)
                                .equalTo("isOfflineSale", true)
                                .findAll()
                )
                if (re != null) result = re

            }.await()
            result
        }

        fun selectOfflineSaleDataByInvoice(invoiceNumber: String): BatchFileDataTable? =
                runBlocking {
                    var batch: BatchFileDataTable? = null
                    val inv = invoiceWithPadding(invoiceNumber)
                    getRealm {
                        val tp = it.where(BatchFileDataTable::class.java)
                                .equalTo("invoiceNumber", inv)
                                .equalTo("transactionType", TransactionType.OFFLINE_SALE.type)
                                .findFirst()
                        if (tp != null) batch = it.copyFromRealm(tp)
                    }.await()
                    batch
                }

        fun selectVoidRefundSaleDataByInvoice(invoiceNumber: String): BatchFileDataTable? =
                runBlocking {
                    var batch: BatchFileDataTable? = null
                    getRealm {
                        val tp = it.where(BatchFileDataTable::class.java)
                                .equalTo("invoiceNumber", invoiceNumber)
                                .equalTo("isRefundSale", true)
                                .equalTo("transactionType", TransactionType.REFUND.type)
                                .findFirst()
                        if (tp != null) batch = it.copyFromRealm(tp) }.await()
                    batch
                }

        fun updateVoidRefundStatus(invoiceNumber: String) = runBlocking {
            getRealm {
                val tp = it.where(BatchFileDataTable::class.java)
                        .equalTo("invoiceNumber", invoiceNumber)
                        .equalTo("isRefundSale", true)
                        .findFirst()
                it.beginTransaction()
                tp?.isRefundSale = false
                tp?.transactionType = TransactionType.VOID_REFUND.type
                it.commitTransaction()
            }.await()
        }

        fun selectAllNonCanceledData(): List<BatchFileDataTable> = runBlocking {
            var result = listOf<BatchFileDataTable>()
            getRealm {
                val r = it.copyFromRealm(
                        it.where(BatchFileDataTable::class.java)
                                .equalTo(
                                        "transactionType",
                                        TransactionType.CASH_AT_POS.type
                                ).findAll()
                )
                if (r != null) result = r
            }.await()
            result
        }

        fun selectBatchDataLast(): BatchFileDataTable? = runBlocking {
            var batch: BatchFileDataTable? = null
            getRealm {
                val res =
                        it.where(BatchFileDataTable::class.java)
                                .findAll().last()
                if (res != null) {
                    batch = it.copyFromRealm(res)
                }
            }.await()
            batch
        }

        fun selectCancelReports(): BatchFileDataTable? = runBlocking {
            var batch: BatchFileDataTable? = null
            getRealm {
                val res =
                        it.where(BatchFileDataTable::class.java)
                                .equalTo("isTimeOut", true).findAll()
                                .last()
                if (res != null) batch = res
            }.await()
            batch
        }

        fun selectAnyReceipts(invoiceNumber: String): BatchFileDataTable? = runBlocking {
            var batch: BatchFileDataTable? = null
            getRealm {
                val inv = addPad(invoiceNumber, "0", 6)
                val res =
                        it.where(BatchFileDataTable::class.java)
                                .equalTo("hostInvoice", inv)
                                .findFirst()
                if (res != null) batch = it.copyFromRealm(res)
            }.await()
            batch
        }

        fun selectVoidTransSaleDataByInvoice(invoiceNumber: String): MutableList<BatchFileDataTable?> =
                runBlocking {
                    var batch: MutableList<BatchFileDataTable?> = mutableListOf()
                    getRealm {
                        val tp = it.where(BatchFileDataTable::class.java)
                                .equalTo("hostInvoice", invoiceNumber)
                                // .equalTo("isRefundSale", false)
                                .equalTo("transactionType", TransactionType.SALE.type)
                                .or()
                                .equalTo("transactionType", TransactionType.EMI_SALE.type)
                                .or()
                                .equalTo("transactionType", TransactionType.REFUND.type)
                                .or()
                                .equalTo("transactionType", TransactionType.SALE_WITH_CASH.type)
                                .or()
                                .equalTo("transactionType", TransactionType.CASH_AT_POS.type)
                                .or()
                                .equalTo("transactionType", TransactionType.TIP_SALE.type)
                                .or()
                                .equalTo("transactionType", TransactionType.TEST_EMI.type)
                                .or()
                                .equalTo("transactionType", TransactionType.BRAND_EMI.type)
                                .or()
                                .equalTo("transactionType", TransactionType.BRAND_EMI_BY_ACCESS_CODE.type)
                                .findAll()
                        if (tp != null) batch = it.copyFromRealm(tp)
                    }.await()
                    batch
                }


        fun clear() =
                withRealm {
                    it.executeTransaction { i ->
                        i.delete(
                                BatchFileDataTable::class.java
                        )
                    }
                }

    }// end of companion block/////


}


/**
 * Table for transaction batch data
 * */
@RealmClass
open class TempBatchFileDataTable() : RealmObject(), Parcelable {
    var authCode: String = ""
    var isChecked: Boolean = false
    var cashBackAmount: String = ""
    var panMaskFormate: String = ""
    var panMaskConfig: String = ""
    var panMask: String = ""
    var terminalSerialNumber: String = ""
    var responseCode: String = ""
    var tid: String = ""
    var mid: String = ""
    var batchNumber: String = ""
    var baseAmmount: String = ""
    var roc: String = ""

    @PrimaryKey
    var invoiceNumber: String = ""
    var panNumber: String = ""
    var time: String = ""
    var date: String = ""
    var expiryDate: String = ""
    var cardHolderName: String = ""
    var timeStamp: Long = 0
    var genratedPinBlock: String = ""
    var field55Data: String = ""
    var track2Data: String = ""
    var transactionType: Int = 0
    var applicationPanSequenceNumber: String = ""
    var nii: String = ""
    var indicator: String = ""
    var bankCode: String = ""
    var customerId: String = ""
    var walletIssuerId: String = ""
    var connectionType: String = ""
    var modelName: String = ""
    var appName: String = ""
    var appVersion: String = ""
    var pcNumber: String = ""
    var posEntryValue: String = ""
    var transactionalAmmount: String = ""
    var mti: String = ""
    var serialNumber: String = ""
    var sourceNII: String = ""
    var destinationNII: String = ""
    var processingCode: String = ""
    var merchantName: String = ""
    var merchantAddress1: String = ""
    var merchantAddress2: String = ""
    var transactionDate: String = ""
    var transactionTime: String = ""
    var transationName: String = ""
    var cardType: String = ""
    var expiry: String = ""
    var cardNumber: String = ""

    //    var autthCode: String = ""
    var referenceNumber: String = ""
    var aid: String = ""
    var tc: String = ""
    var tipAmmount: String = ""
    var totalAmmount: String = ""
    var isPinverified: Boolean = false
    var discaimerMessage: String = ""
    var isMerchantCoppy = true
    var message: String = ""
    var isTimeOut: Boolean = false

    var operationType: String = ""

    var isVoid: Boolean = false

    var f48IdentifierWithTS: String = ""

    var tvr = ""
    var tsi = ""

    var aqrRefNo = ""

    var hasPromo = false

    var gccMsg = ""

    fun getTransactionType(): String {
        var tTyp = ""
        for (e in TransactionType.values()) {
            if (e.ordinal == transactionType) {
                tTyp = e.name
                break
            }
        }
        return tTyp
    }

    private constructor(parcel: Parcel) : this() {

        authCode = parcel.readString().toString()
        isChecked = parcel.readByte() != 0.toByte()
        cashBackAmount = parcel.readString().toString()
        panMaskFormate = parcel.readString().toString()
        panMaskConfig = parcel.readString().toString()
        panMask = parcel.readString().toString()
        terminalSerialNumber = parcel.readString().toString()
        responseCode = parcel.readString().toString()
        tid = parcel.readString().toString()
        mid = parcel.readString().toString()
        batchNumber = parcel.readString().toString()
        roc = parcel.readString().toString()
        invoiceNumber = parcel.readString().toString()
        panNumber = parcel.readString().toString()
        time = parcel.readString().toString()
        date = parcel.readString().toString()
        expiryDate = parcel.readString().toString()
        cardHolderName = parcel.readString().toString()
        timeStamp = parcel.readLong()
        genratedPinBlock = parcel.readString().toString()
        field55Data = parcel.readString().toString()
        track2Data = parcel.readString().toString()
        transactionType = parcel.readInt()
        applicationPanSequenceNumber = parcel.readString().toString()
        nii = parcel.readString().toString()
        indicator = parcel.readString().toString()
        bankCode = parcel.readString().toString()
        customerId = parcel.readString().toString()
        walletIssuerId = parcel.readString().toString()
        connectionType = parcel.readString().toString()
        modelName = parcel.readString().toString()
        appName = parcel.readString().toString()
        appVersion = parcel.readString().toString()
        pcNumber = parcel.readString().toString()
        posEntryValue = parcel.readString().toString()
        transactionalAmmount = parcel.readString().toString()
        mti = parcel.readString().toString()
        serialNumber = parcel.readString().toString()
        sourceNII = parcel.readString().toString()
        destinationNII = parcel.readString().toString()
        processingCode = parcel.readString().toString()
        merchantName = parcel.readString().toString()
        merchantAddress1 = parcel.readString().toString()
        merchantAddress2 = parcel.readString().toString()
        transactionDate = parcel.readString().toString()
        transactionTime = parcel.readString().toString()
        transationName = parcel.readString().toString()
        cardType = parcel.readString().toString()
        expiry = parcel.readString().toString()
        cardNumber = parcel.readString().toString()

        referenceNumber = parcel.readString().toString()
        aid = parcel.readString().toString()
        tc = parcel.readString().toString()
        tipAmmount = parcel.readString().toString()
        totalAmmount = parcel.readString().toString()
        isPinverified = parcel.readByte() != 0.toByte()
        discaimerMessage = parcel.readString().toString()
        isMerchantCoppy = parcel.readByte() != 0.toByte()
        message = parcel.readString().toString()
        isTimeOut = parcel.readByte() != 0.toByte()
        operationType = parcel.readString().toString()
        isVoid = parcel.readByte() != 0.toByte()
        f48IdentifierWithTS = parcel.readString().toString()
        tvr = parcel.readString().toString()
        tsi = parcel.readString().toString()

        aqrRefNo = parcel.readString().toString()

        hasPromo = parcel.readByte() != 0.toByte()

        gccMsg = parcel.readString().toString()

    }

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeString(authCode)
        parcel.writeByte(if (isChecked) 1 else 0)
        parcel.writeString(cashBackAmount)
        parcel.writeString(panMaskFormate)
        parcel.writeString(panMaskConfig)
        parcel.writeString(panMask)
        parcel.writeString(terminalSerialNumber)
        parcel.writeString(responseCode)
        parcel.writeString(tid)
        parcel.writeString(mid)
        parcel.writeString(batchNumber)
        parcel.writeString(roc)
        parcel.writeString(invoiceNumber)
        parcel.writeString(panNumber)
        parcel.writeString(time)
        parcel.writeString(date)
        parcel.writeString(expiryDate)
        parcel.writeString(cardHolderName)
        parcel.writeLong(timeStamp)
        parcel.writeString(genratedPinBlock)
        parcel.writeString(field55Data)
        parcel.writeString(track2Data)
        parcel.writeInt(transactionType)
        parcel.writeString(applicationPanSequenceNumber)
        parcel.writeString(nii)
        parcel.writeString(indicator)
        parcel.writeString(bankCode)
        parcel.writeString(customerId)
        parcel.writeString(walletIssuerId)
        parcel.writeString(connectionType)
        parcel.writeString(modelName)
        parcel.writeString(appName)
        parcel.writeString(appVersion)
        parcel.writeString(pcNumber)
        parcel.writeString(posEntryValue)
        parcel.writeString(transactionalAmmount)
        parcel.writeString(mti)
        parcel.writeString(serialNumber)
        parcel.writeString(sourceNII)
        parcel.writeString(destinationNII)
        parcel.writeString(processingCode)
        parcel.writeString(merchantName)
        parcel.writeString(merchantAddress1)
        parcel.writeString(merchantAddress2)
        parcel.writeString(transactionDate)
        parcel.writeString(transactionTime)
        parcel.writeString(transationName)
        parcel.writeString(cardType)
        parcel.writeString(expiry)
        parcel.writeString(cardNumber)

        parcel.writeString(referenceNumber)
        parcel.writeString(aid)
        parcel.writeString(tc)
        parcel.writeString(tipAmmount)
        parcel.writeString(totalAmmount)
        parcel.writeByte(if (isPinverified) 1 else 0)
        parcel.writeString(discaimerMessage)
        parcel.writeByte(if (isMerchantCoppy) 1 else 0)
        parcel.writeString(message)
        parcel.writeByte(if (isTimeOut) 1 else 0)
        parcel.writeString(operationType)
        parcel.writeByte(if (isVoid) 1 else 0)
        parcel.writeString(f48IdentifierWithTS)
        parcel.writeString(tvr)
        parcel.writeString(tsi)
        parcel.writeString(aqrRefNo)

        parcel.writeByte(if (hasPromo) 1 else 0)

        parcel.writeString(gccMsg)

    }

    override fun describeContents(): Int {
        return 0
    }

    companion object {
        private val TAG: String = TempBatchFileDataTable::class.java.simpleName

        @JvmField
        val CREATOR = object : Parcelable.Creator<TempBatchFileDataTable> {
            override fun createFromParcel(parcel: Parcel): TempBatchFileDataTable {
                return TempBatchFileDataTable(
                        parcel
                )
            }

            override fun newArray(size: Int): Array<TempBatchFileDataTable> {
                return Array(size) { TempBatchFileDataTable() }
            }
        }

        fun performOperation(param: TempBatchFileDataTable) =
            withRealm {
                it.executeTransaction { i ->
                    i.insertOrUpdate(param)
                }
            }

        fun performOperation(param: TempBatchFileDataTable, callback: () -> Unit) =
            withRealm {
                it.executeTransaction { i ->
                    i.insertOrUpdate(param)
                }
                callback()
            }

        fun selectBatchData(): MutableList<TempBatchFileDataTable> = runBlocking {
            var result = mutableListOf<TempBatchFileDataTable>()
            getRealm {
                val re = it.copyFromRealm(it.where(TempBatchFileDataTable::class.java).findAll())
                if (re != null) result = re

            }.await()
            result
        }

        fun selectAllNonCanceledData(): List<TempBatchFileDataTable> = runBlocking {
            var result = listOf<TempBatchFileDataTable>()
            getRealm {
                val r = it.copyFromRealm(
                    it.where(TempBatchFileDataTable::class.java)
                        .equalTo(
                            "transactionType",
                            TransactionType.CASH_AT_POS.type
                        ).findAll()
                )
                if (r != null) result = r
            }.await()
            result
        }

        fun selectBatchDataLast(): TempBatchFileDataTable? = runBlocking {
            var batch: TempBatchFileDataTable? = null
            getRealm {
                val res =
                    it.where(TempBatchFileDataTable::class.java)
                        .findAll().last()
                if (res != null) {
                    batch = it.copyFromRealm(res)
                }
            }.await()
            batch
        }

        fun selectCancelReports(): TempBatchFileDataTable? = runBlocking {
            var batch: TempBatchFileDataTable? = null
            getRealm {
                val res =
                    it.where(TempBatchFileDataTable::class.java)
                        .equalTo("isTimeOut", true).findAll()
                        .last()
                if (res != null) batch = res
            }.await()
            batch
        }

        fun selectAnyReceipts(invoiceNumber: String): TempBatchFileDataTable? = runBlocking {
            var batch: TempBatchFileDataTable? = null
            getRealm {
                val inv = addPad(invoiceNumber, "0", 6)
                val res =
                    it.where(TempBatchFileDataTable::class.java)
                        .equalTo("invoiceNumber", inv)
                        .findFirst()
                if (res != null) batch = it.copyFromRealm(res)
            }.await()
            batch
        }

        fun clear() =
            withRealm {
                it.executeTransaction { i ->
                    i.delete(
                        TempBatchFileDataTable::class.java
                    )
                }
            }

    }// end of companion block/////


}

/**
 * Table for Issuer Parameter
 * type of transaction allowed in IssuerParameterTable
 * */
@RealmClass
open class IssuerParameterTable() : RealmObject(), Parcelable {
    @field:BHFieldParseIndex(0)
    var pcNo: String = ""

    @field:BHFieldParseIndex(1)
    var actionId: String = ""

    @field:BHFieldParseIndex(2)
    var tableId: String = ""

    @field:BHFieldParseIndex(3)
    var isActive: String = ""

    @PrimaryKey
    @field:BHFieldParseIndex(4)
    @field:BHFieldName("Issuer Id")
    var issuerId: String = ""

    @field:BHFieldParseIndex(5)
    @field:BHFieldName("Issuer Type Id")
    var issuerTypeId: String = ""

    @field:BHFieldName("Issuer Name")
    @field:BHFieldParseIndex(6)
    var issuerName: String = ""

    @field:BHFieldParseIndex(7)
    @field:BHFieldName("OTP Size")
    var otpSize: String = ""

    @field:BHFieldParseIndex(8)
    @field:BHFieldName("Token Size")
    var tokenSize: String = ""

    @field:BHFieldParseIndex(9)
    @field:BHFieldName("Sale Allowed")
    var saleAllowed: String = ""

    @field:BHFieldParseIndex(10)
    @field:BHFieldName("Void Sale Allowed")
    var voidSaleAllowed: String = ""

    @field:BHFieldParseIndex(11)
    var cashReloadAllowed: String = ""

    @field:BHFieldParseIndex(12)
    var voidCashReloadAllowed: String = ""

    @field:BHFieldParseIndex(13)
    var creditReloadAllowed: String = ""

    @field:BHFieldParseIndex(14)
    var voidCreaditReloadAllowed: String = ""

    @field:BHFieldParseIndex(15)
    var balanceEnquiry: String = ""

    @field:BHFieldParseIndex(16)
    var volletIssuerDisclamerLength: String = ""

    @field:BHFieldParseIndex(17)
    var volletIssuerDisclammer: String = ""

    @field:BHFieldParseIndex(18)
    @field:BHFieldName("Master Key")
    var volletIssuerMasterKey: String = ""

    @field:BHFieldParseIndex(19)
    @field:BHFieldName("Customer Id Type")
    var customerIdentifierFiledType: String = ""

    @field:BHFieldParseIndex(20)
    @field:BHFieldName("Customer Id Size")
    var customerIdentifierFieldSize: String = ""

    @field:BHFieldParseIndex(21)
    @field:BHFieldName("Customer Id Name")
    var customerIdentifierFieldName: String = ""

    @field:BHFieldParseIndex(22)
    @field:BHFieldName("Customer Id Masking")
    var idendifierMasking: String = ""

    @field:BHFieldParseIndex(23)
    @field:BHFieldName("Transaction Amount Limit")
    var transactionAmountLimit: String = ""

    @field:BHFieldParseIndex(24)
    @field:BHFieldName("Push Bill Allowed")
    var pushBillAllowed: String = ""


    @field:BHFieldParseIndex(25)
    @field:BHFieldName("Customer ReEnter Allowed")
    var reEnteredCustomerId: String = ""

    @field:BHFieldParseIndex(26)
    @field:BHFieldName("Reserved Value")
    var reservedForFutureUsed: String = ""

    var isIssuerSelected: Boolean = false

    private constructor(parcel: Parcel) : this() {
        pcNo = parcel.readString().toString()
        actionId = parcel.readString().toString()
        tableId = parcel.readString().toString()
        isActive = parcel.readString().toString()
        issuerId = parcel.readString().toString()
        issuerTypeId = parcel.readString().toString()
        issuerName = parcel.readString().toString()
        otpSize = parcel.readString().toString()
        tokenSize = parcel.readString().toString()
        saleAllowed = parcel.readString().toString()
        voidSaleAllowed = parcel.readString().toString()
        cashReloadAllowed = parcel.readString().toString()
        voidCashReloadAllowed = parcel.readString().toString()
        creditReloadAllowed = parcel.readString().toString()
        voidCreaditReloadAllowed = parcel.readString().toString()
        balanceEnquiry = parcel.readString().toString()
        volletIssuerDisclamerLength = parcel.readString().toString()
        volletIssuerDisclammer = parcel.readString().toString()
        volletIssuerMasterKey = parcel.readString().toString()
        customerIdentifierFiledType = parcel.readString().toString()
        customerIdentifierFieldSize = parcel.readString().toString()
        customerIdentifierFieldName = parcel.readString().toString()
        idendifierMasking = parcel.readString().toString()
        transactionAmountLimit = parcel.readString().toString()
        pushBillAllowed = parcel.readString().toString()
        reEnteredCustomerId = parcel.readString().toString()
        reservedForFutureUsed = parcel.readString().toString()
        isIssuerSelected = parcel.readByte() != 0.toByte()

    }

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeString(pcNo)
        parcel.writeString(actionId)
        parcel.writeString(tableId)
        parcel.writeString(isActive)
        parcel.writeString(issuerId)
        parcel.writeString(issuerTypeId)
        parcel.writeString(issuerName)
        parcel.writeString(otpSize)
        parcel.writeString(tokenSize)
        parcel.writeString(saleAllowed)
        parcel.writeString(voidSaleAllowed)
        parcel.writeString(cashReloadAllowed)
        parcel.writeString(voidCashReloadAllowed)
        parcel.writeString(creditReloadAllowed)
        parcel.writeString(voidCreaditReloadAllowed)
        parcel.writeString(balanceEnquiry)
        parcel.writeString(volletIssuerDisclamerLength)
        parcel.writeString(volletIssuerDisclammer)
        parcel.writeString(volletIssuerMasterKey)
        parcel.writeString(customerIdentifierFiledType)
        parcel.writeString(customerIdentifierFieldSize)
        parcel.writeString(customerIdentifierFieldName)
        parcel.writeString(idendifierMasking)
        parcel.writeString(transactionAmountLimit)
        parcel.writeString(pushBillAllowed)
        parcel.writeString(reEnteredCustomerId)
        parcel.writeString(reservedForFutureUsed)
        parcel.writeByte(if (isIssuerSelected) 1 else 0)
    }

    override fun describeContents(): Int {
        return 0
    }

    companion object {
        private val TAG: String = IssuerParameterTable::class.java.simpleName

        @JvmField
        val CREATOR = object : Parcelable.Creator<IssuerParameterTable> {
            override fun createFromParcel(parcel: Parcel): IssuerParameterTable {
                return IssuerParameterTable(
                        parcel
                )
            }

            override fun newArray(size: Int): Array<IssuerParameterTable> {
                return Array(size) { IssuerParameterTable() }
            }
        }

        fun performOperation(issuerParamTable: IssuerParameterTable, callback: () -> Unit) =
            withRealm {
                when {
                    issuerParamTable.actionId == "1" || issuerParamTable.actionId == "2" -> it.executeTransaction { i ->
                        i.insertOrUpdate(
                            issuerParamTable
                        )
                    }

                    issuerParamTable.actionId == "3" -> {
                        it.executeTransaction { i ->
                            val rows =
                                i.where(IssuerParameterTable::class.java)
                                    .equalTo("issuerId", issuerParamTable.issuerId)
                                    .findAll()
                            rows.deleteAllFromRealm()
                        }
                    }
                }
                callback()
            }


        fun selectFromIssuerParameterTable(): List<IssuerParameterTable> = runBlocking {
            var result = listOf<IssuerParameterTable>()
            getRealm {
                result = it.copyFromRealm(
                    it.where(IssuerParameterTable::class.java)
                        .findAll()
                )
            }.await()
            result
        }

        fun selectFromIssuerParameterTableOnConditionBase(): List<IssuerParameterTable> =
            runBlocking {
                var result = listOf<IssuerParameterTable>()
                getRealm {
                    result = it.copyFromRealm(
                        it.where(IssuerParameterTable::class.java)
                            .equalTo("isActive", "1")
                            .notEqualTo("issuerId", "50")
                            .findAll()
                    )
                }.await()
                result
            }

        fun selectFromIssuerParameterTableList(issuerId: String): List<IssuerParameterTable> =
            runBlocking {
                var result = listOf<IssuerParameterTable>()
                getRealm {
                    result = it.copyFromRealm(
                        it.where(IssuerParameterTable::class.java)
                            .equalTo(
                                "issuerId",
                                issuerId
                            ).findAll()
                    )
                }.await()
                result
            }


        fun selectFromIssuerParameterTable(issuerId: String): IssuerParameterTable? = runBlocking {
            var result: IssuerParameterTable? = null
            getRealm {
                val re =
                    it.where(IssuerParameterTable::class.java)
                        .equalTo("issuerId", issuerId)
                        .findFirst()
                if (re != null) result = it.copyFromRealm(re)
            }.await()
            result
        }

        fun clear() =
            withRealm {
                it.executeTransaction { i ->
                    i.delete(
                        IssuerParameterTable::class.java
                    )
                }
            }

    }  // end of companion object block

}

/**
 * Table for Terminal Communication
 * ip, dns, apn information are stored in this table
 * */
@RealmClass
open class TerminalCommunicationTable() : RealmObject(), Parcelable {
    @field:BHFieldParseIndex(0)
    var pcNo: String = ""

    @field:BHFieldParseIndex(1)
    var actionId: String = ""

    @field:BHFieldParseIndex(2)
    var tableId: String = ""

    @field:BHFieldParseIndex(3)
    var isActive: String = ""

    @PrimaryKey
    @field:BHFieldParseIndex(4)
    var recordId: String = ""

    @field:BHFieldParseIndex(5)
    var recordType: String = "" // what type of table it is if =1 txn com param ,   if =2 app update com param

    @field:BHFieldParseIndex(7)
    var epbxEnable: String = ""

    @field:BHFieldParseIndex(8)
    var nii: String = ""

    /*used for Dial*/
    @field:BHFieldParseIndex(9)
    var authorizationPrimaryPhoneNo1: String = ""

    @field:BHFieldParseIndex(10)
    var authorizationSecondaryPhone1: String = ""

    @field:BHFieldParseIndex(11)
    var primarySettlementPhone1: String = ""

    @field:BHFieldParseIndex(12)
    var secondarySettlementPhone2: String = ""

    @field:BHFieldParseIndex(13)
    var dialTimeOut: String = ""

    /*used for GPRS*/
    @field:BHFieldName("APN")
    @field:BHFieldParseIndex(14)
    var apn: String = ""


    @field:BHFieldName("APN User Name")
    @field:BHFieldParseIndex(15)
    var apnUserName: String = ""

    @field:BHFieldName("APN Password")
    @field:BHFieldParseIndex(16)
    var apnPassword: String = ""

    @field:BHFieldName("Host Primary IP")
    @field:BHFieldParseIndex(17)
    var hostPrimaryIp: String = ""

    @field:BHFieldName("Host Primary Port")
    @field:BHFieldParseIndex(18)
    var hostPrimaryPortNo: String = ""

    @field:BHFieldName("Host Secondary IP")
    @field:BHFieldParseIndex(19)
    var hostSecIp: String = ""

    @field:BHFieldName("Host Secondary Port")
    @field:BHFieldParseIndex(20)
    var hostSecPortNo: String = ""

    /*used for ethernet*/
    @field:BHFieldName("DNS Primary")
    @field:BHFieldParseIndex(21)
    var dnsPrimary: String = ""

    @field:BHFieldParseIndex(22)
    var primaryGateway: String = ""

    @field:BHFieldParseIndex(23)
    var primarySubnet: String = ""

    @field:BHFieldParseIndex(24)
    var hostEthPrimaryIp: String = ""

    @field:BHFieldParseIndex(25)
    var hostPrimaryEthPort: String = ""

    @field:BHFieldParseIndex(26)
    var dnsSecondry: String = ""

    @field:BHFieldParseIndex(27)
    var secondryGateway: String = ""

    @field:BHFieldParseIndex(28)
    var secondrySubnet: String = ""

    @field:BHFieldParseIndex(29)
    var hostEthSecondryIp: String = ""

    @field:BHFieldParseIndex(30)
    var hostSecondryEthPort: String = ""

    @field:BHFieldParseIndex(31)
    @field:BHFieldName("Connection Timeout")
    var connectTimeOut: String = ""

    @field:BHFieldParseIndex(32)
    @field:BHFieldName("Response Timeout")
    var responseTimeOut: String = ""

    @field:BHFieldParseIndex(33)
    var reserveValue: String = ""

    //region=====New Fields For HDFC Sim 2 detail ======
    @field:BHFieldParseIndex(34)
    @field:BHFieldName("APN 2")
    var apn2 = ""

    @field:BHFieldParseIndex(35)
    @field:BHFieldName("GPRS User 2")
    var gprsUser2 = ""

    @field:BHFieldParseIndex(36)
    @field:BHFieldName("GPRS Password 2")
    var gprsPassword2 = ""

    @field:BHFieldParseIndex(37)
    @field:BHFieldName("Host Primary IP 2")
    var hostPrimaryIp2 = ""

    @field:BHFieldParseIndex(38)
    @field:BHFieldName("Host Primary Port 2")
    var hostPrimaryPort2 = ""

    @field:BHFieldParseIndex(39)
    @field:BHFieldName("Host Secondary IP 2")
    var hostSecondaryIp2 = ""

    @field:BHFieldParseIndex(40)
    @field:BHFieldName("Host Secondary Port 2")
    var hostSecondaryPort2 = ""

    @field:BHFieldParseIndex(41)
    @field:BHFieldName("Bank Code")
    var bankCode = ""

    @field:BHFieldParseIndex(42)
    @field:BHFieldName("TID")
    var tid = ""

    //endregion


    private constructor(parcel: Parcel) : this() {
        pcNo = parcel.readString().toString()
        actionId = parcel.readString().toString()
        tableId = parcel.readString().toString()
        isActive = parcel.readString().toString()
        recordId = parcel.readString().toString()

        recordType = parcel.readString().toString()
        epbxEnable = parcel.readString().toString()
        nii = parcel.readString().toString()
        authorizationPrimaryPhoneNo1 = parcel.readString().toString()
        authorizationSecondaryPhone1 = parcel.readString().toString()

        primarySettlementPhone1 = parcel.readString().toString()
        secondarySettlementPhone2 = parcel.readString().toString()
        dialTimeOut = parcel.readString().toString()
        apn = parcel.readString().toString()
        apnUserName = parcel.readString().toString()

        apnPassword = parcel.readString().toString()
        hostPrimaryIp = parcel.readString().toString()
        hostPrimaryPortNo = parcel.readString().toString()
        hostSecIp = parcel.readString().toString()
        hostSecPortNo = parcel.readString().toString()

        dnsPrimary = parcel.readString().toString()
        primaryGateway = parcel.readString().toString()
        primarySubnet = parcel.readString().toString()
        hostEthPrimaryIp = parcel.readString().toString()
        hostPrimaryEthPort = parcel.readString().toString()

        dnsSecondry = parcel.readString().toString()
        secondryGateway = parcel.readString().toString()
        secondrySubnet = parcel.readString().toString()
        hostEthSecondryIp = parcel.readString().toString()
        hostSecondryEthPort = parcel.readString().toString()

        connectTimeOut = parcel.readString().toString()
        responseTimeOut = parcel.readString().toString()
        reserveValue = parcel.readString().toString()

        //region=====New Fields For HDFC Sim 2 detail ======
        apn2 = parcel.readString().toString()
        gprsUser2 = parcel.readString().toString()
        gprsPassword2 = parcel.readString().toString()
        hostPrimaryIp2 = parcel.readString().toString()
        hostPrimaryPort2 = parcel.readString().toString()
        hostSecondaryIp2 = parcel.readString().toString()
        hostSecondaryPort2 = parcel.readString().toString()
        bankCode = parcel.readString().toString()
        tid = parcel.readString().toString()
        //endregion

    }

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeString(pcNo)
        parcel.writeString(actionId)
        parcel.writeString(tableId)
        parcel.writeString(isActive)
        parcel.writeString(recordId)

        parcel.writeString(recordType)
        parcel.writeString(epbxEnable)
        parcel.writeString(nii)
        parcel.writeString(authorizationPrimaryPhoneNo1)
        parcel.writeString(authorizationSecondaryPhone1)

        parcel.writeString(primarySettlementPhone1)
        parcel.writeString(secondarySettlementPhone2)
        parcel.writeString(dialTimeOut)
        parcel.writeString(apn)
        parcel.writeString(apnUserName)

        parcel.writeString(apnPassword)
        parcel.writeString(hostPrimaryIp)
        parcel.writeString(hostPrimaryPortNo)
        parcel.writeString(hostSecIp)
        parcel.writeString(hostSecPortNo)

        parcel.writeString(dnsPrimary)
        parcel.writeString(primaryGateway)
        parcel.writeString(primarySubnet)
        parcel.writeString(hostEthPrimaryIp)
        parcel.writeString(hostPrimaryEthPort)

        parcel.writeString(dnsSecondry)
        parcel.writeString(secondryGateway)
        parcel.writeString(secondrySubnet)
        parcel.writeString(hostEthSecondryIp)
        parcel.writeString(hostSecondryEthPort)

        parcel.writeString(connectTimeOut)
        parcel.writeString(responseTimeOut)
        parcel.writeString(reserveValue)

        //region=====New Fields For HDFC Sim 2 detail ======
        parcel.writeString(apn2)
        parcel.writeString(gprsUser2)
        parcel.writeString(gprsPassword2)
        parcel.writeString(hostPrimaryIp2)
        parcel.writeString(hostPrimaryPort2)
        parcel.writeString(hostSecondaryIp2)
        parcel.writeString(hostSecondaryPort2)
        parcel.writeString(bankCode)
        parcel.writeString(tid)
        //endregion

    }

    override fun describeContents(): Int {
        return 0
    }

    companion object {

        private val TAG: String = TerminalCommunicationTable::class.java.simpleName

        @JvmField
        val CREATOR = object : Parcelable.Creator<TerminalCommunicationTable> {
            override fun createFromParcel(parcel: Parcel): TerminalCommunicationTable {
                return TerminalCommunicationTable(
                        parcel
                )
            }

            override fun newArray(size: Int): Array<TerminalCommunicationTable> {
                return Array(size) { TerminalCommunicationTable() }
            }
        }

        fun performOperation(tct: TerminalCommunicationTable, callback: () -> Unit) =
            withRealm {
                when (tct.actionId) {
                    "1", "2" -> it.executeTransaction { r -> r.insertOrUpdate(tct) }
                    "3" -> it.executeTransaction { r ->
                        r.where(TerminalCommunicationTable::class.java)
                            .equalTo("recordId", tct.recordId).findAll()
                            .deleteAllFromRealm()
                    }
                }
                callback()
            }


        fun selectFromSchemeTable(): TerminalCommunicationTable? = runBlocking {
            var tct: TerminalCommunicationTable? = null
            getRealm {
                val re = it.copyFromRealm(
                    it.where(TerminalCommunicationTable::class.java)
                        .findAll()
                )
                if (re.size > 0) tct = re[0]
            }.await()
            tct
        }

        fun clear() =
            withRealm {
                it.executeTransaction { r ->
                    r.delete(
                        TerminalCommunicationTable::class.java
                    )
                }
            }

    }  // End of companion object block


}

/**
 * Table for Terminal Parameter
 * terminal id, pan masking, passwords etc are in this table
 * */
@RealmClass
open class TerminalParameterTable() : RealmObject(), Parcelable {
    @field:BHFieldParseIndex(0)
    var pcNO: String = ""

    @field:BHFieldParseIndex(1)
    var actionId: String = ""

    @field:BHFieldParseIndex(2)
    @PrimaryKey
    var tableId: String = ""

    @field:BHFieldParseIndex(3)
    var isActive: String = ""

    @field:BHFieldParseIndex(4)
    @field: BHFieldName("Terminal Id")
    var terminalId: String = ""

    @field:BHFieldParseIndex(5)
    @field:BHFieldName("Merchant Id")
    var merchantId: String = ""

    @field:BHFieldParseIndex(6)
    @field:BHFieldName("Batch Number")
    var batchNumber: String = ""

    @field:BHFieldParseIndex(7)
    @field:BHFieldName("Invoice Number")
    var invoiceNumber: String = ""

    @field:BHFieldParseIndex(8)
    var receiptHeaderOne: String = ""

    @field:BHFieldParseIndex(9)
    var receiptHeaderTwo: String = ""

    @field:BHFieldParseIndex(10)
    var receiptHeaderThree: String = ""

    @field:BHFieldParseIndex(11)
    @field:BHFieldName("Print Receipt")
    var printReceipt: String = ""

    @field:BHFieldParseIndex(12)
    //@field:BHFieldName("Admin Password")
    var adminPassword: String = ""

    @field:BHFieldName("Manager Password")
    var managerPassword: String = ""
        get() {
            return if (adminPassword.length == 8) adminPassword.substring(0, 4) else adminPassword
        }

    @field:BHFieldParseIndex(13)
    var traningMode: String = ""

    @field:BHFieldParseIndex(14)
    var cancledTransactionReceiptPrint: String = ""

    @field:BHFieldParseIndex(15)
    //@field:BHFieldName("Super Admin Password")
    var superAdminPassword: String = ""

    @field:BHFieldParseIndex(16)
    var terminalDateTime: String = ""

    @field:BHFieldParseIndex(17)
    @field:BHFieldName("Currency Symbol")
    var currencySymbol: String = ""

    @field:BHFieldParseIndex(18)
    @field:BHFieldName("Tip Processing")
    @field:BHDashboardItem(EDashboardItem.SALE_TIP)
    var tipProcessing: String = ""

    @field:BHFieldParseIndex(19)
    @field:BHFieldName("Tip Percent")
    var tipPercent: String = ""

    @field:BHFieldParseIndex(20)
    @field:BHFieldName("Max Tip Percent")
    var maxTipPercent: String = ""

    @field:BHFieldParseIndex(21)
    @field:BHFieldName("Max Tip Limit")
    var maxTipLimit: String = ""

    @field:BHFieldParseIndex(22)
    @field:BHFieldName("Surcharge")
    var surcharge: String = ""

    @field:BHFieldParseIndex(23)
    @field:BHFieldName("Surcharge Type")
    var surchargeType: String = ""

    @field:BHFieldParseIndex(24)
    @field:BHFieldName("Surcharge Value")
    var surChargeValue: String = ""

    @field:BHFieldParseIndex(25)
    var maxSurchargeValue: String = ""

    @field:BHFieldParseIndex(26)
    @field:BHFieldName("Force Settle")
    var forceSettle: String = ""

    @field:BHFieldParseIndex(27)
    @field:BHFieldName("Force Settle Time")
    var forceSettleTime: String = ""

    @field:BHFieldParseIndex(28)
    @field:BHFieldName("Sale With Cash")
    @field:BHDashboardItem(EDashboardItem.SALE_WITH_CASH)
    var saleWithCash: String = ""

    @field:BHFieldParseIndex(29)
    @field:BHFieldName("Cash Advance")
    @field:BHDashboardItem(EDashboardItem.CASH_ADVANCE)
    var cashAdvance: String = ""

    @field:BHFieldParseIndex(30)
    @field:BHFieldName("Cash Advance Limit")
    var cashAdvanceMaxAmountLimit: String = ""

    //allowed or not masking 0 -> default masking, 1-> masking based on maskformate
    @field:BHFieldParseIndex(32)
    @field:BHFieldName("Pan Mask")
    var panMask: String = ""

    @field:BHFieldParseIndex(33)
    @field:BHFieldName("Pan Mash Format")
    var panMaskFormate: String = ""

    //on which coppy allowed masking 0->none,1->customer coppy, 2->merchant coppy,3->both
    @field:BHFieldParseIndex(34)
    var panMaskConfig: String = ""

    @field:BHFieldParseIndex(35)
    @field:BHFieldName("Sale")
    @field:BHDashboardItem(EDashboardItem.SALE)
    var sale: String = ""

    @field:BHFieldParseIndex(36)
    @field:BHFieldName("Void")
    @field:BHDashboardItem(EDashboardItem.VOID_SALE)
    var voidSale: String = ""

    @field:BHFieldParseIndex(37)
    @field:BHFieldName("Refund")
    @field:BHDashboardItem(EDashboardItem.REFUND)
    var refund: String = ""

    @field:BHFieldParseIndex(38)
    @field:BHFieldName("Void Refund")
    //  @field:BHDashboardItem(EDashboardItem.VOID_REFUND)
    var voidRefund: String = ""

    @field:BHFieldParseIndex(39)
    @field:BHFieldName("Pre Auth")
    @field:BHDashboardItem(EDashboardItem.PREAUTH, EDashboardItem.PREAUTH_COMPLETE)
    var preAuth: String = ""

    @field:BHFieldParseIndex(31)
    var maxAmtEntryDigits: String = ""

    @field:BHFieldParseIndex(40)
    @field:BHFieldName("Bank Emi")
    @field:BHDashboardItem(EDashboardItem.BANK_EMI)
    var bankEmi: String = ""

    @field:BHFieldParseIndex(41)
    @field:BHFieldName("Brand Emi")
    @field:BHDashboardItem(EDashboardItem.BRAND_EMI)
    var brandEmi: String = ""

    @field:BHFieldName("Brand Emi By Access Code")
    @field:BHDashboardItem(EDashboardItem.EMI_PRO)
    @field:BHFieldParseIndex(42)
    var emiPro: String = ""

    @field:BHFieldParseIndex(43)
    var wolletTransation: String = ""

    @field:BHFieldParseIndex(44)
    var qrTransaction: String = ""

    @field:BHFieldParseIndex(45)
    @field: BHFieldName("Manual Entry")
    var fManEntry = ""

    @field:BHFieldParseIndex(46)
    @field:BHDashboardItem(
        EDashboardItem.OFFLINE_SALE
    )
    // EDashboardItem.PENDING_OFFLINE_SALE
    @field:BHFieldName("Offline Sale")
    var fManOfflineSale = ""


    @field:BHFieldParseIndex(47)
    var reservedValues: String = ""

    @field:BHFieldName("roc")
    var stan: String = ""

    @field:BHFieldParseIndex(48)
    @field:BHFieldName("Void Preauth")
    @field:BHDashboardItem(EDashboardItem.VOID_PREAUTH)
    var fVoidPreauth = ""

    @field:BHFieldParseIndex(49)
    @field:BHFieldName("Void Offline Sale")
    @field:BHDashboardItem(EDashboardItem.VOID_OFFLINE_SALE)
    var fVoidOfflineSale = ""

    @field:BHFieldParseIndex(50)
    @field:BHFieldName("Pending Preauth")
    @field:BHDashboardItem(EDashboardItem.PENDING_PREAUTH)
    var fPendingPreauthTrans = ""

    @field:BHFieldParseIndex(51)
    var maxCtlsTransAmt = ""

    @field:BHFieldParseIndex(52)
    var minCtlsTransAmt = ""

    @field:BHFieldParseIndex(53)
    @field:BHFieldName("Offline Sale Min PAN")
    var minOfflineSalePanLen = ""

    @field:BHFieldParseIndex(54)
    @field:BHFieldName("Offline Sale Max PAN")
    var maxOfflineSalePanLen = ""

    @field:BHFieldParseIndex(55)
    var tlsFlag = ""

    @field:BHFieldParseIndex(56)
    @field:BHFieldName("Printing Impact")
    var printingImpact = ""

    @field:BHFieldParseIndex(57)
    var posHealthStatics = ""

    @field:BHFieldParseIndex(58)
    var fPushEndPointDetail = ""

    @field:BHFieldParseIndex(59)
    var fPushTimeStamp = ""

    //region=========New Fields for HDFC===========
    @field:BHFieldParseIndex(60)
    @field:BHFieldName("Tid Type")
    var tidType: String = ""  // if type is 1 main else child tid

    @field:BHFieldParseIndex(61)
    @field:BHFieldName("Tid Index")
    var tidIndex = ""   // sorting order of child tid

    @field:BHFieldParseIndex(62)
    @field:BHFieldName("Tid Bank Code")
    var tidBankCode = ""  // relation with bank

    @field:BHFieldParseIndex(63)
    @field:BHFieldName("Tid Name")
    var tidName = ""  // name of bank

    @field:BHFieldParseIndex(67)
    @field:BHFieldName("STAN")
    var roc = ""

    @field:BHFieldParseIndex(68)
    var ctlsCaption = ""

    @field:BHFieldParseIndex(69)
    var flexiPayMinAmountLimit = ""

    @field:BHFieldParseIndex(70)
    var flexiPayMaxAmountLimit = ""
    //---->
//68 ---ctls caption  ctls txn /
    // 69 flexipay min  ----------->
    // 70 flexipay max  ----------->

    @field:BHDashboardItem(EDashboardItem.EMI_ENQUIRY)
    var bankEnquiry: String = ""

    @field:BHDashboardItem(EDashboardItem.BONUS_PROMO)
    var hasPromo: String = ""

    @field:BHDashboardItem(EDashboardItem.DIGI_POS)
    var isDigiposActive: String = ""

    var isPromoAvailable = false
    var isPromoAvailableOnPayment = false
    var promoVersionNo: String = "000000000000"
    var bankEnquiryMobNumberEntry: Boolean = false

    // region =======
    // Digi POS Data
    var digiPosResponseType: String = ""
    var digiPosStatus: String = ""
    var digiPosStatusMessage: String = ""
    var digiPosStatusCode: String = ""
    var digiPosTerminalStatus: String = ""
    var digiPosBQRStatus: String = ""
    var digiPosUPIStatus: String = ""
    var digiPosSMSpayStatus: String = ""
    var digiPosStaticQrDownloadRequired: String = ""
    var digiPosCardCallBackRequired: String = ""

    //endregion


    private constructor(parcel: Parcel) : this() {
        pcNO = parcel.readString().toString()
        emiPro = parcel.readString().toString()
        actionId = parcel.readString().toString()
        tableId = parcel.readString().toString()
        isActive = parcel.readString().toString()
        terminalId = parcel.readString().toString()
        merchantId = parcel.readString().toString()
        batchNumber = parcel.readString().toString()
        invoiceNumber = parcel.readString().toString()
        receiptHeaderOne = parcel.readString().toString()
        receiptHeaderTwo = parcel.readString().toString()
        receiptHeaderThree = parcel.readString().toString()
        printReceipt = parcel.readString().toString()
        adminPassword = parcel.readString().toString()
        managerPassword = parcel.readString().toString()
        traningMode = parcel.readString().toString()
        cancledTransactionReceiptPrint = parcel.readString().toString()
        superAdminPassword = parcel.readString().toString()
        terminalDateTime = parcel.readString().toString()
        currencySymbol = parcel.readString().toString()
        tipProcessing = parcel.readString().toString()
        tipPercent = parcel.readString().toString()
        maxTipPercent = parcel.readString().toString()
        maxTipLimit = parcel.readString().toString()
        surcharge = parcel.readString().toString()
        surchargeType = parcel.readString().toString()
        surChargeValue = parcel.readString().toString()
        maxSurchargeValue = parcel.readString().toString()
        forceSettle = parcel.readString().toString()
        forceSettleTime = parcel.readString().toString()
        saleWithCash = parcel.readString().toString()
        cashAdvance = parcel.readString().toString()
        cashAdvanceMaxAmountLimit = parcel.readString().toString()
        panMask = parcel.readString().toString()
        panMaskFormate = parcel.readString().toString()
        panMaskConfig = parcel.readString().toString()
        sale = parcel.readString().toString()
        voidSale = parcel.readString().toString()
        refund = parcel.readString().toString()
        voidRefund = parcel.readString().toString()
        preAuth = parcel.readString().toString()
        maxAmtEntryDigits = parcel.readString().toString()
        bankEmi = parcel.readString().toString()
        brandEmi = parcel.readString().toString()
        wolletTransation = parcel.readString().toString()
        qrTransaction = parcel.readString().toString()

        fManEntry = parcel.readString().toString()
        fManOfflineSale = parcel.readString().toString()
        reservedValues = parcel.readString().toString()
        stan = parcel.readString().toString()

        fVoidPreauth = parcel.readString().toString()
        fVoidOfflineSale = parcel.readString().toString()

        fPendingPreauthTrans = parcel.readString().toString()
        maxCtlsTransAmt = parcel.readString().toString()
        minCtlsTransAmt = parcel.readString().toString()
        minOfflineSalePanLen = parcel.readString().toString()
        maxOfflineSalePanLen = parcel.readString().toString()

        tlsFlag = parcel.readString().toString()
        printingImpact = parcel.readString().toString()
        posHealthStatics = parcel.readString().toString()
        fPushEndPointDetail = parcel.readString().toString()
        fPushTimeStamp = parcel.readString().toString()

        //region=========New Fields for HDFC===========
        tidType = parcel.readString().toString()
        tidIndex = parcel.readString().toString()
        tidBankCode = parcel.readString().toString()
        tidName = parcel.readString().toString()
        roc = parcel.readString().toString()
        bankEnquiry = parcel.readString().toString()

        bankEnquiryMobNumberEntry = parcel.readByte() != 0.toByte()
        //endregion
        isPromoAvailable = parcel.readByte() != 0.toByte()
        isPromoAvailableOnPayment = parcel.readByte() != 0.toByte()
        promoVersionNo = parcel.readString().toString()


    }

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeString(pcNO)
        parcel.writeString(emiPro)
        parcel.writeString(actionId)
        parcel.writeString(tableId)
        parcel.writeString(isActive)
        parcel.writeString(terminalId)
        parcel.writeString(merchantId)
        parcel.writeString(batchNumber)
        parcel.writeString(invoiceNumber)
        parcel.writeString(receiptHeaderOne)
        parcel.writeString(receiptHeaderTwo)
        parcel.writeString(receiptHeaderThree)
        parcel.writeString(printReceipt)
        parcel.writeString(adminPassword)
        parcel.writeString(managerPassword)
        parcel.writeString(traningMode)
        parcel.writeString(cancledTransactionReceiptPrint)
        parcel.writeString(superAdminPassword)
        parcel.writeString(terminalDateTime)
        parcel.writeString(currencySymbol)
        parcel.writeString(tipProcessing)
        parcel.writeString(tipPercent)
        parcel.writeString(maxTipPercent)
        parcel.writeString(maxTipLimit)
        parcel.writeString(surcharge)
        parcel.writeString(surchargeType)
        parcel.writeString(surChargeValue)
        parcel.writeString(maxSurchargeValue)
        parcel.writeString(forceSettle)
        parcel.writeString(forceSettleTime)
        parcel.writeString(saleWithCash)
        parcel.writeString(cashAdvance)
        parcel.writeString(cashAdvanceMaxAmountLimit)
        parcel.writeString(panMask)
        parcel.writeString(panMaskFormate)
        parcel.writeString(panMaskConfig)
        parcel.writeString(sale)
        parcel.writeString(voidSale)
        parcel.writeString(refund)
        parcel.writeString(voidRefund)
        parcel.writeString(preAuth)
        parcel.writeString(maxAmtEntryDigits)
        parcel.writeString(bankEmi)
        parcel.writeString(brandEmi)
        parcel.writeString(wolletTransation)
        parcel.writeString(qrTransaction)

        parcel.writeString(fManEntry)
        parcel.writeString(fManOfflineSale)
        parcel.writeString(reservedValues)
        parcel.writeString(stan)

        parcel.writeString(fVoidPreauth)
        parcel.writeString(fVoidOfflineSale)

        parcel.writeString(fPendingPreauthTrans)
        parcel.writeString(maxCtlsTransAmt)
        parcel.writeString(minCtlsTransAmt)
        parcel.writeString(minOfflineSalePanLen)
        parcel.writeString(maxOfflineSalePanLen)

        parcel.writeString(tlsFlag)
        parcel.writeString(printingImpact)
        parcel.writeString(posHealthStatics)
        parcel.writeString(fPushEndPointDetail)
        parcel.writeString(fPushTimeStamp)

        //region=========New Fields for HDFC===========
        parcel.writeString(tidType)
        parcel.writeString(tidIndex)
        parcel.writeString(tidBankCode)
        parcel.writeString(tidName)
        parcel.writeString(roc)
        parcel.writeString(bankEnquiry)
        parcel.writeByte(if (bankEnquiryMobNumberEntry) 1 else 0)
        //endregion
        parcel.writeByte(if (isPromoAvailable) 1 else 0)
        parcel.writeByte(if (isPromoAvailableOnPayment) 1 else 0)
        parcel.writeString(promoVersionNo)

    }

    override fun describeContents(): Int {
        return 0
    }

    companion object {
        private val TAG: String = TerminalParameterTable::class.java.simpleName

        @JvmField
        val CREATOR = object : Parcelable.Creator<TerminalParameterTable> {
            override fun createFromParcel(parcel: Parcel): TerminalParameterTable {
                return TerminalParameterTable(
                    parcel
                )
            }

            override fun newArray(size: Int): Array<TerminalParameterTable> {
                return Array(size) { TerminalParameterTable() }
            }
        }

        fun performOperation(tpt: TerminalParameterTable, callback: () -> Unit) =
            withRealm {
                when (tpt.actionId) {
                    "1", "2" -> it.executeTransaction { r -> r.insertOrUpdate(tpt) }
                    "3" -> it.executeTransaction { r ->
                        r.where(TerminalParameterTable::class.java)
                            .equalTo("tableId", tpt.tableId)
                            .findAll()
                            .deleteAllFromRealm()
                    }
                }
                callback()
            }


        fun selectFromSchemeTable(): TerminalParameterTable? = runBlocking {
            var tpt: TerminalParameterTable? = null
            getRealm {
                val tp = it.where(TerminalParameterTable::class.java).findFirst()
                if (tp != null) tpt = it.copyFromRealm(tp)
            }.await()
            tpt
        }

        fun testTPT(): TerminalParameterTable? = runBlocking {
            var tct: TerminalParameterTable? = null
            getRealm {
                val re = it.copyFromRealm(
                    it.where(TerminalParameterTable::class.java)
                        .findAll()
                )
                if (re.size > 0) tct = re[0]
            }.await()
            tct
        }
        /*var tpt: TerminalParameterTable? = null
            getRealm {
                val re = it.copyFromRealm(
                    it.where(TerminalParameterTable::class.java)
                        .findAll()
                )
                if (re.size > 0) tct = re[0]
            }.await()
            tpt*/

        fun updateSaleBatchNumber(batchNumber: String) = runBlocking {
            getRealm {
                val tp = it.where(TerminalParameterTable::class.java).findFirst()
                it.beginTransaction()
                if (batchNumber.toInt() > 9999999) {
                    tp?.batchNumber = invoiceWithPadding(1.toString())
                } else {
                    tp?.batchNumber = invoiceWithPadding(batchNumber)
                }
                it.commitTransaction()
            }.await()
        }

        fun updateTerminalID(terminalID: String) = runBlocking {
            var tpt: TerminalParameterTable? = null
            getRealm {
                val tp = it.where(TerminalParameterTable::class.java).findFirst()
                it.beginTransaction()
                tp?.terminalId = terminalID
                it.commitTransaction()
            }.await()
        }

        fun updateTerminalDataInvoiceNumber(invoiceNumber: String) = runBlocking {
            //  var tpt: TerminalParameterTable? = null
            getRealm {
                val tp = it.where(TerminalParameterTable::class.java).findFirst()
                it.beginTransaction()
                if (invoiceNumber.toInt() > 999999) {
                    tp?.invoiceNumber = invoiceWithPadding(1.toString())
                } else {
                    tp?.invoiceNumber =
                        invoiceWithPadding((invoiceNumber.toInt().plus(1)).toString())
                }
                it.commitTransaction()
            }.await()
        }

        fun updateTerminalDataROCNumber(roc: Int) = runBlocking {
            getRealm {
                val tp = it.where(TerminalParameterTable::class.java).findFirst()
                it.beginTransaction()
                tp?.roc = invoiceWithPadding((roc).toString())
                it.commitTransaction()
            }.await()
        }

        fun select(bankCode: String): TerminalParameterTable? = runBlocking {
            var tpt: TerminalParameterTable? = null
            getRealm {
                var ltpt: List<TerminalParameterTable> =
                    listOf()
                val tp =
                    it.where(TerminalParameterTable::class.java)
                        .findAll()
                if (tp != null) ltpt = it.copyFromRealm(tp)
                for (e in ltpt) {
                    if (e.tidBankCode.toInt() == bankCode.toInt()) {
                        tpt = e
                        break
                    }
                }
            }.await()
            tpt
        }

        fun selectAll(): List<TerminalParameterTable> = runBlocking {
            var tpt: List<TerminalParameterTable> = listOf()
            getRealm {
                val tp =
                    it.where(TerminalParameterTable::class.java)
                        .findAll()
                if (tp != null) tpt = it.copyFromRealm(tp)
            }.await()
            tpt
        }

        //Below method is to get Sale Void Data from BatchFileDataTable:-
        fun getVoidSaleBatchData(): io.reactivex.Observable<BatchFileDataTable> {
            return io.reactivex.Observable.fromCallable { selectVoidBatchData() }
        }

        //Below method is to get Void Batch Data:-
        private fun selectVoidBatchData(): BatchFileDataTable? {
            val realm = Realm.getDefaultInstance()
            val voidBatchTable =
                realm.copyFromRealm(realm.where(BatchFileDataTable::class.java).findAll())
            realm.close()
            return if (voidBatchTable.size > 0)
                voidBatchTable[voidBatchTable.size - 1]
            else
                null
        }

        fun clear() =
            withRealm {
                it.executeTransaction { r ->
                    r.delete(
                        TerminalParameterTable::class.java
                    )
                }
            }

        fun updateMerchantPromoData(data: Triple<String, Boolean, Boolean>) = runBlocking {
            var tpt: TerminalParameterTable? = null
            getRealm {
                val tp = it.where(TerminalParameterTable::class.java).findFirst()
                it.beginTransaction()
                tp?.promoVersionNo = data.first
                tp?.isPromoAvailable = data.second
                tp?.isPromoAvailableOnPayment = data.third
                it.commitTransaction()
            }.await()
        }

    } // end of companion object block


}

/**
 * Table for Card Data Table
 * cardtype, max pan, floor limit, SALE, CASHBACK etc alloed or not are store in table
 * */
@RealmClass
open class CardDataTable() : RealmObject(), Parcelable {
    @field:BHFieldParseIndex(0)
    var pcNo: String = ""

    @field:BHFieldParseIndex(9)
    @field:BHFieldName("Maximum Pan Digits")
    var maxPanDigits: String = ""

    @field:BHFieldParseIndex(1)
    var actionId: String = ""

    @field:BHFieldParseIndex(2)
    var tableId: String = ""

    @field:BHFieldParseIndex(3)
    var isActive: String = ""

    @field:BHFieldParseIndex(4)
    @PrimaryKey
    @field:BHFieldName("Card Index")
    var cardTableIndex: String = ""

    @field:BHFieldParseIndex(5)
    @field:BHFieldName("Card Type")
    var cardType: String = ""

    @field:BHFieldParseIndex(6)
    var cardAbbrev: String = ""

    @field:BHFieldParseIndex(7)
    @field:BHFieldName("Card Label")
    var cardLabel: String = ""

    @field:BHFieldParseIndex(8)
    @field:BHFieldName("Minimum Pan Digits")
    var minPanDigits: String = ""

    @field:BHFieldParseIndex(10)
    @field:BHFieldName("Floor Limit")
    var floorLimit: String = "0"

    @field:BHFieldParseIndex(11)
    @field:BHFieldName("Pan Low")
    var panLow: String = ""

    @field:BHFieldParseIndex(12)
    @field:BHFieldName("Pan High")
    var panHi: String = ""

    @field:BHFieldParseIndex(13)
    @field:BHFieldName("Manual Entry")
    var manualEntry: String = ""

    @field:BHFieldParseIndex(14)
    var singleLine: String = ""

    @field:BHFieldParseIndex(15)
    @field:BHFieldName("Tip Adjust Allowed")
    var tipAdjustAllowed: String = ""

    @field:BHFieldParseIndex(16)
    @field:BHFieldName("Pre Auth Allowed")
    var preAuthAllowed: String = ""

    @field:BHFieldParseIndex(17)
    @field:BHFieldName("Sale With Cash Allowed")
    var saleWithCashAllowed: String = ""

    @field:BHFieldParseIndex(18)
    @field:BHFieldName("Cash Only Allowed")
    var cashOnlyAllowed: String = ""

    @field:BHFieldParseIndex(19)
    var cashAdvanceAllowed: String = ""

    @field:BHFieldParseIndex(20)
    @field:BHFieldName("Sale Allowed")
    var saleAllowed: String = ""

    @field:BHFieldParseIndex(21)
    @field:BHFieldName("Void Sale Allowed")
    var voidSaleAllowed: String = ""

    @field:BHFieldParseIndex(22)
    @field:BHFieldName("Refund Allowed")
    var refundAllowed: String = ""

    @field:BHFieldParseIndex(23)
    @field:BHFieldName("Void Refund Allowed")
    var voidRefundAllowed: String = ""

    @field:BHFieldParseIndex(24)
    @field:BHFieldName("Manual Offline Sale")
    var manOffSaleAllowed: String = ""

    @field:BHFieldParseIndex(25)
    @field:BHFieldName("Reverse Value")
    var reservedValued: String = ""


    //region==========New Field for HDFC========
    @field:BHFieldParseIndex(26)
    @field:BHFieldName("Bank Code")
    var bankCode = ""

    @field:BHFieldParseIndex(27)
    @field:BHFieldName("TID")
    var tid = ""

    @field:BHFieldParseIndex(28)
    @field:BHFieldName("Bank Issuer Id")
    var bankIssuerId = ""
    //endregion


    private constructor(parcel: Parcel) : this() {
        pcNo = parcel.readString().toString()
        manualEntry = parcel.readString().toString()
        maxPanDigits = parcel.readString().toString()
        actionId = parcel.readString().toString()
        tableId = parcel.readString().toString()

        isActive = parcel.readString().toString()
        cardTableIndex = parcel.readString().toString()
        cardType = parcel.readString().toString()
        cardAbbrev = parcel.readString().toString()
        cardLabel = parcel.readString().toString()

        minPanDigits = parcel.readString().toString()
        floorLimit = parcel.readString().toString()
        panLow = parcel.readString().toString()
        panHi = parcel.readString().toString()
        manualEntry = parcel.readString().toString()

        singleLine = parcel.readString().toString()
        tipAdjustAllowed = parcel.readString().toString()
        preAuthAllowed = parcel.readString().toString()
        saleWithCashAllowed = parcel.readString().toString()
        cashOnlyAllowed = parcel.readString().toString()

        cashAdvanceAllowed = parcel.readString().toString()
        saleAllowed = parcel.readString().toString()
        voidSaleAllowed = parcel.readString().toString()
        refundAllowed = parcel.readString().toString()
        voidRefundAllowed = parcel.readString().toString()

        manOffSaleAllowed = parcel.readString().toString()
        reservedValued = parcel.readString().toString()

        //region======New Field For HDFC========
        bankCode = parcel.readString().toString()
        tid = parcel.readString().toString()
        bankIssuerId = parcel.readString().toString()
        //endregion


    }

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeString(pcNo)
        parcel.writeString(manualEntry)
        parcel.writeString(maxPanDigits)
        parcel.writeString(actionId)
        parcel.writeString(tableId)

        parcel.writeString(isActive)
        parcel.writeString(cardTableIndex)
        parcel.writeString(cardType)
        parcel.writeString(cardAbbrev)
        parcel.writeString(cardLabel)

        parcel.writeString(minPanDigits)
        parcel.writeString(floorLimit)
        parcel.writeString(panLow)
        parcel.writeString(panHi)
        parcel.writeString(manualEntry)

        parcel.writeString(singleLine)
        parcel.writeString(tipAdjustAllowed)
        parcel.writeString(preAuthAllowed)
        parcel.writeString(saleWithCashAllowed)
        parcel.writeString(cashOnlyAllowed)

        parcel.writeString(cashAdvanceAllowed)
        parcel.writeString(saleAllowed)
        parcel.writeString(voidSaleAllowed)
        parcel.writeString(refundAllowed)
        parcel.writeString(voidRefundAllowed)

        parcel.writeString(manOffSaleAllowed)
        parcel.writeString(reservedValued)

        //region======New Field For HDFC========
        parcel.writeString(bankCode)
        parcel.writeString(tid)
        parcel.writeString(bankIssuerId)
        //endregion

    }

    override fun describeContents(): Int {
        return 0
    }

    companion object {
        private val TAG: String = CardDataTable::class.java.simpleName

        @JvmField
        val CREATOR = object : Parcelable.Creator<CardDataTable> {
            override fun createFromParcel(parcel: Parcel): CardDataTable {
                return CardDataTable(parcel)
            }

            override fun newArray(size: Int): Array<CardDataTable> {
                return Array(size) { CardDataTable() }
            }
        }

        fun performOperation(param: CardDataTable, callback: () -> Unit) =
            withRealm {
                when (param.actionId) {
                    "1", "2" -> it.executeTransaction { i -> i.insertOrUpdate(param) }
                    "3" -> it.executeTransaction { i ->
                        i.where(CardDataTable::class.java)
                            .equalTo("cardTableIndex", param.cardTableIndex).findAll()
                            .deleteAllFromRealm()
                    }
                }
                callback()
            }


        fun selecteAllCardsData(): List<CardDataTable> = runBlocking {
            var result = listOf<CardDataTable>()
            getRealm {
                result = it.copyFromRealm(
                    it.where(CardDataTable::class.java)
                        .findAll()
                )
            }.await()
            result
        }

        fun selectFirstCardTableData(): CardDataTable? = runBlocking {
            var tpt: CardDataTable? = null
            getRealm {
                val tp = it.where(CardDataTable::class.java).findFirst()
                if (tp != null) tpt = it.copyFromRealm(tp)
            }.await()
            tpt
        }

        fun selectFromCardDataTable(panNumber: String): CardDataTable? = runBlocking {
            if (panNumber.isEmpty()) return@runBlocking null
            var result: CardDataTable? = null
            getRealm {
                val cdtl = it.copyFromRealm(
                    it.where(CardDataTable::class.java)
                        .findAll()
                )
                for (each in cdtl) {
                    if (each.panLow.length >= 6 && each.panHi.length >= 6 && panNumber.length >= 6) {
                        val panLow = each.panLow.substring(0, 6).toLong()
                        val panHi = each.panHi.substring(0, 6).toLong()
                        val cuPan = panNumber.substring(0, 6).toLong()
                        if (cuPan in panLow..panHi) {
                            result = each
                            //break
                        }
                    }
                }
            }.await()
            result
        }

        fun clear() =
            withRealm {
                it.executeTransaction { i ->
                    i.delete(
                        CardDataTable::class.java
                    )
                }
            }

    }  // End of Companion object block


}

//endregion


//region==============Tables Mainly used in EMI============================

/**
 * Tenure table
 * Information of tenure, scheme id, rate of interest(roi) and processing fee
 * Tenure Table posEntryValue is mapped on behalf of scheme id
 * */
@RealmClass
open class TenureTable() : RealmObject(), Parcelable {
    @field:BHFieldParseIndex(0)
    var pcNo: String = ""

    @field:BHFieldParseIndex(1)
    var actionId: String = ""

    @field:BHFieldParseIndex(2)
    var tableId: String = ""

    @field:BHFieldParseIndex(3)
    var isActive: String = ""

    @PrimaryKey
    @field:BHFieldParseIndex(4)
    var emiTenureId: String = ""

    @field:BHFieldParseIndex(5)
    var schemeId: String = ""

    @field:BHFieldParseIndex(6)
    var tenure: String = ""

    @field:BHFieldParseIndex(7)
    var roi: String = ""

    @field:BHFieldParseIndex(8)
    var proccesingFee: String = ""

    @field:BHFieldParseIndex(9)
    var effecativeRate: String = ""

    @field:BHFieldParseIndex(10)
    var processingRate: String = ""

    constructor(parcel: Parcel) : this() {
        pcNo = parcel.readString().toString()
        actionId = parcel.readString().toString()
        tableId = parcel.readString().toString()
        isActive = parcel.readString().toString()
        emiTenureId = parcel.readString().toString()
        schemeId = parcel.readString().toString()
        tenure = parcel.readString().toString()
        roi = parcel.readString().toString()
        proccesingFee = parcel.readString().toString()
        effecativeRate = parcel.readString().toString()
        processingRate = parcel.readString().toString()
    }

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeString(pcNo)
        parcel.writeString(actionId)
        parcel.writeString(tableId)
        parcel.writeString(isActive)
        parcel.writeString(emiTenureId)
        parcel.writeString(schemeId)
        parcel.writeString(tenure)
        parcel.writeString(roi)
        parcel.writeString(proccesingFee)
        parcel.writeString(effecativeRate)
        parcel.writeString(processingRate)
    }

    override fun describeContents(): Int {
        return 0
    }

    companion object {
        private val TAG: String = TenureTable::class.java.simpleName

        @JvmField
        val CREATOR = object : Parcelable.Creator<TenureTable> {
            override fun createFromParcel(parcel: Parcel): TenureTable {
                return TenureTable(parcel)
            }

            override fun newArray(size: Int): Array<TenureTable> {
                return Array(size) { TenureTable() }
            }
        }

        fun performOperation(param: TenureTable, callback: () -> Unit) =
            withRealm {
                when (param.actionId) {
                    "1", "2" -> it.executeTransaction { i -> i.insertOrUpdate(param) }
                    "3" -> it.executeTransaction { i ->
                        i.where(TenureTable::class.java)
                            .equalTo("emiTenureId", param.emiTenureId)
                            .findAll()
                            .deleteAllFromRealm()
                    }
                }
                callback()
            }

        fun selectFromSchemeTable(): List<TenureTable> = runBlocking {
            var list = listOf<TenureTable>()
            getRealm {
                list = it.copyFromRealm(
                    it.where(TenureTable::class.java)
                        .findAll()
                )
            }.await()
            list
        }

        fun selectFromTenureTable(schemeId: String): ArrayList<TenureTable> = runBlocking {
            var result = arrayListOf<TenureTable>()
            getRealm {
                val re = it.copyFromRealm(
                    it.where(TenureTable::class.java)
                        .equalTo("schemeId", schemeId).equalTo(
                            "isActive",
                            "1"
                        ).findAll()
                )
                for (each in re) result.add(each)
            }.await()
            result
        }

        fun clear() = withRealm {
            it.executeTransaction { i ->
                i.delete(TenureTable::class.java)
            }
        }

    }  // end of companion object block


}

/**
 * Scheme Table
 * Information of scheme id, scheme name and issuer id
 * scheme tabel posEntryValue is mapped with issuer id along with emi bin table
 * */
@RealmClass
open class SchemeTable() : RealmObject(), Parcelable {
    @field:BHFieldParseIndex(0)
    var pcNo: String = ""

    @field:BHFieldParseIndex(1)
    var actionId: String = ""

    @field:BHFieldParseIndex(2)
    var tableId: String = ""

    @field:BHFieldParseIndex(3)
    var isActive: String = ""

    @field:BHFieldParseIndex(4)
    @PrimaryKey
    var schemeId: String = ""

    @field:BHFieldParseIndex(5)
    var issuerId: String = ""

    @field:BHFieldParseIndex(6)
    var schemeName: String = ""

    private constructor(parcel: Parcel) : this() {
        pcNo = parcel.readString().toString()
        actionId = parcel.readString().toString()
        tableId = parcel.readString().toString()
        isActive = parcel.readString().toString()
        schemeId = parcel.readString().toString()
        issuerId = parcel.readString().toString()
        schemeName = parcel.readString().toString()
    }

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeString(pcNo)
        parcel.writeString(actionId)
        parcel.writeString(tableId)
        parcel.writeString(isActive)
        parcel.writeString(schemeId)
        parcel.writeString(issuerId)
        parcel.writeString(schemeName)
    }

    override fun describeContents(): Int {
        return 0
    }

    companion object {
        private val TAG = SchemeTable::class.java.simpleName

        @JvmField
        val CREATOR = object : Parcelable.Creator<SchemeTable> {
            override fun createFromParcel(parcel: Parcel): SchemeTable {
                return SchemeTable(parcel)
            }

            override fun newArray(size: Int): Array<SchemeTable> {
                return Array(size) { SchemeTable() }
            }
        }

        fun performOperation(param: SchemeTable, callback: () -> Unit) =
            withRealm {
                when (param.actionId) {
                    "1", "2" -> it.executeTransaction { i -> i.insertOrUpdate(param) }
                    "3" -> it.executeTransaction { i ->
                        i.where(SchemeTable::class.java)
                            .equalTo("schemeId", param.schemeId).findAll()
                            .deleteAllFromRealm()
                    }
                }
                callback()
            }


        fun selectFromSchemeTable(): List<SchemeTable> = runBlocking {
            var result = listOf<SchemeTable>()
            getRealm {
                result = it.copyFromRealm(
                    it.where(SchemeTable::class.java)
                        .findAll()
                )
            }.await()
            result
        }

        fun selectFromSchemeTable(issuerId: String): ArrayList<SchemeTable> = runBlocking {
            val result = arrayListOf<SchemeTable>()
            getRealm {
                val res = it.copyFromRealm(
                    it.where(SchemeTable::class.java)
                        .equalTo("issuerId", issuerId).equalTo(
                            "isActive",
                            "1"
                        ).findAll()
                )
                for (each in res) result.add(each)

            }.await()
            result
        }

        fun clear() = withRealm {
            it.executeTransaction { i ->
                i.delete(SchemeTable::class.java)
            }
        }

    }  // end of companion object block

}

/**
 * Emi Bin Table
 * Information of binIndexId, IssuerId and EMIBinValue
 * emi bin table is mapped with Issuer Parameter Table and scheme table
 * */
@RealmClass
open class EmiBinTable() : RealmObject(), Parcelable {
    @field:BHFieldParseIndex(0)
    var pcNo: String = ""

    @field:BHFieldParseIndex(1)
    var actionId: String = ""

    @field:BHFieldParseIndex(2)
    var tableId: String = ""

    @field:BHFieldParseIndex(3)
    var isActive: String = ""

    @PrimaryKey
    @field:BHFieldParseIndex(4)
    var binIndexId: String = ""

    @field:BHFieldParseIndex(5)
    var issuerId: String = ""

    @field:BHFieldParseIndex(6)
    var binValue: String = ""

    private constructor(parcel: Parcel) : this() {

        pcNo = parcel.readString().toString()
        actionId = parcel.readString().toString()
        tableId = parcel.readString().toString()
        isActive = parcel.readString().toString()
        binIndexId = parcel.readString().toString()
        issuerId = parcel.readString().toString()
        binValue = parcel.readString().toString()
    }

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeString(pcNo)
        parcel.writeString(actionId)
        parcel.writeString(tableId)
        parcel.writeString(isActive)
        parcel.writeString(binIndexId)
        parcel.writeString(issuerId)
        parcel.writeString(binValue)
    }

    override fun describeContents(): Int {
        return 0
    }

    companion object {
        private val TAG = EmiBinTable::class.java.simpleName

        @JvmField
        val CREATOR = object : Parcelable.Creator<EmiBinTable> {
            override fun createFromParcel(parcel: Parcel): EmiBinTable {
                return EmiBinTable(parcel)
            }

            override fun newArray(size: Int): Array<EmiBinTable> {
                return Array(size) { EmiBinTable() }
            }
        }

        fun performOperation(param: EmiBinTable, callback: () -> Unit) =
            withRealm {
                when (param.actionId) {
                    "1", "2" -> it.executeTransaction { i -> i.insertOrUpdate(param) }
                    "3" -> it.executeTransaction { i ->
                        i.where(EmiBinTable::class.java)
                            .equalTo("binIndexId", param.binIndexId)
                            .findAll()
                            .deleteAllFromRealm()
                    }
                }
                callback()
            }

        fun selectFromEmiBinTable(): List<EmiBinTable> = runBlocking {
            var result = listOf<EmiBinTable>()
            getRealm {
                result = it.copyFromRealm(
                    it.where(EmiBinTable::class.java)
                        .findAll()
                )
            }.await()
            result
        }

        fun selectFromEmiBinTable(issuerId: String): EmiBinTable? = runBlocking {
            var result: EmiBinTable? = null
            getRealm {
                val ebtl =
                    it.where(EmiBinTable::class.java)
                        .equalTo("issuerId", issuerId).findFirst()
                if (ebtl != null) result = it.copyFromRealm(ebtl)

            }.await()
            result
        }

        fun clear() = withRealm {
            it.executeTransaction { i ->
                i.delete(EmiBinTable::class.java)
            }
        }

    }  // end of companion object block
}


/**
 * Emi Scheme Table
 * Information of emiSchemeId, SchemeId, BrandId, min and max Value of transaction
 * start and end date of benefit, type of benefit(FIXED, TOTAL, PERCENTAGE or PARAMETER SLAB WISE) etc
 * mapped to TENURE TABLE
 * */
@RealmClass
open class EmiSchemeTable() : RealmObject(), Parcelable {
    @field:BHFieldParseIndex(0)
    var pcNo: String = ""

    @field:BHFieldParseIndex(1)
    var actionId: String = ""

    @field:BHFieldParseIndex(2)
    var tableId: String = ""

    @field:BHFieldParseIndex(3)
    var isActive: String = ""

    @field:BHFieldParseIndex(4)
    @PrimaryKey
    var emiSchemeId: String = ""

    @field:BHFieldParseIndex(5)
    var brandId: String = ""

    @field:BHFieldParseIndex(6)
    var schemeId: String = ""

    @field:BHFieldParseIndex(7)
    var emiSchemeCodeName: String = ""

    @field:BHFieldParseIndex(8)
    var minValue: String = ""

    @field:BHFieldParseIndex(9)
    var maxValue: String = ""

    @field:BHFieldParseIndex(10)
    var startDate: String = ""

    @field:BHFieldParseIndex(11)
    var endDate: String = ""

    @field:BHFieldParseIndex(12)
    var benifitModelId: String = ""

    @field:BHFieldParseIndex(13)
    var benifitCalculationRefundId: String = ""

    @field:BHFieldParseIndex(14)
    var fixedValue: String = ""

    @field:BHFieldParseIndex(15)
    var percantageValue: String = ""

    @field:BHFieldParseIndex(16)
    var maxAmount: String = ""

    @field:BHFieldParseIndex(17)
    var disclaimer: String = ""

    private constructor(parcel: Parcel) : this() {
        pcNo = parcel.readString().toString()
        actionId = parcel.readString().toString()
        tableId = parcel.readString().toString()
        isActive = parcel.readString().toString()
        emiSchemeId = parcel.readString().toString()
        brandId = parcel.readString().toString()
        schemeId = parcel.readString().toString()
        emiSchemeCodeName = parcel.readString().toString()
        minValue = parcel.readString().toString()
        maxValue = parcel.readString().toString()
        startDate = parcel.readString().toString()
        endDate = parcel.readString().toString()
        benifitModelId = parcel.readString().toString()
        benifitCalculationRefundId = parcel.readString().toString()
        fixedValue = parcel.readString().toString()
        percantageValue = parcel.readString().toString()
        maxAmount = parcel.readString().toString()
        disclaimer = parcel.readString().toString()
    }

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeString(pcNo)
        parcel.writeString(actionId)
        parcel.writeString(tableId)
        parcel.writeString(isActive)
        parcel.writeString(emiSchemeId)
        parcel.writeString(brandId)
        parcel.writeString(schemeId)
        parcel.writeString(emiSchemeCodeName)
        parcel.writeString(minValue)
        parcel.writeString(maxValue)
        parcel.writeString(startDate)
        parcel.writeString(endDate)
        parcel.writeString(benifitModelId)
        parcel.writeString(benifitCalculationRefundId)
        parcel.writeString(fixedValue)
        parcel.writeString(percantageValue)
        parcel.writeString(maxAmount)
        parcel.writeString(disclaimer)
    }

    override fun describeContents(): Int {
        return 0
    }

    companion object {
        private val TAG = EmiSchemeTable::class.java.simpleName

        @JvmField
        val CREATOR = object : Parcelable.Creator<EmiSchemeTable> {
            override fun createFromParcel(parcel: Parcel): EmiSchemeTable {
                return EmiSchemeTable(parcel)
            }

            override fun newArray(size: Int): Array<EmiSchemeTable> {
                return Array(size) { EmiSchemeTable() }
            }
        }

        fun performOperation(param: EmiSchemeTable, callback: () -> Unit) =
            withRealm {
                when (param.actionId) {
                    "1", "2" -> it.executeTransaction { i -> i.insertOrUpdate(param) }
                    "3" -> it.executeTransaction { i ->
                        i.where(EmiSchemeTable::class.java)
                            .equalTo("emiSchemeId", param.emiSchemeId)
                            .findAll()
                    }
                }
                callback()
            }

        fun selectFromEmiSchemeTable(): List<EmiSchemeTable> = runBlocking {
            var result = listOf<EmiSchemeTable>()
            getRealm {
                result = it.copyFromRealm(
                    it.where(EmiSchemeTable::class.java)
                        .findAll()
                )
            }.await()
            result
        }

        fun clear() =
            withRealm {
                it.executeTransaction { i ->
                    i.delete(
                        EmiSchemeTable::class.java
                    )
                }
            }

    }  // End of companion object block


}

/**
 * Benifit Slab Table
 * (if EMI scheme table is refering for PARAMETER SLAB WISE)
 * information of start and end date of benefit, type of benefit(FIXED, TOTAL, PERCENTAGE ) etc
 * mapped to EMI SCHEME TABLE
 * */
@RealmClass
open class BenifitSlabTable() : RealmObject(), Parcelable {
    @field:BHFieldParseIndex(0)
    var pcNo: String = ""

    @field:BHFieldParseIndex(1)
    var actionId: String = ""

    @field:BHFieldParseIndex(2)
    var tableId: String = ""

    @field:BHFieldParseIndex(3)
    var isActive: String = ""

    @field:BHFieldParseIndex(4)
    @PrimaryKey
    var schemeSlabId: String = ""

    @field:BHFieldParseIndex(5)
    var emiSchemeId: String = ""

    @field:BHFieldParseIndex(6)
    var tenure: String = ""

    @field:BHFieldParseIndex(7)
    var minValue: String = ""

    @field:BHFieldParseIndex(8)
    var maxValue: String = ""

    @field:BHFieldParseIndex(9)
    var ruleId: String = ""

    @field:BHFieldParseIndex(10)
    var fixedValue: String = ""

    @field:BHFieldParseIndex(11)
    var percentageValue: String = ""

    private constructor(parcel: Parcel) : this() {
        pcNo = parcel.readString().toString()
        actionId = parcel.readString().toString()
        tableId = parcel.readString().toString()
        isActive = parcel.readString().toString()
        schemeSlabId = parcel.readString().toString()
        emiSchemeId = parcel.readString().toString()
        tenure = parcel.readString().toString()
        minValue = parcel.readString().toString()
        maxValue = parcel.readString().toString()
        ruleId = parcel.readString().toString()
        fixedValue = parcel.readString().toString()
        percentageValue = parcel.readString().toString()
    }

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeString(pcNo)
        parcel.writeString(actionId)
        parcel.writeString(tableId)
        parcel.writeString(isActive)
        parcel.writeString(schemeSlabId)
        parcel.writeString(emiSchemeId)
        parcel.writeString(tenure)
        parcel.writeString(minValue)
        parcel.writeString(maxValue)
        parcel.writeString(ruleId)
        parcel.writeString(fixedValue)
        parcel.writeString(percentageValue)
    }

    override fun describeContents(): Int {
        return 0
    }

    companion object {
        private val TAG: String = BenifitSlabTable::class.java.simpleName

        @JvmField
        val CREATOR = object : Parcelable.Creator<BenifitSlabTable> {
            override fun createFromParcel(parcel: Parcel): BenifitSlabTable {
                return BenifitSlabTable(
                    parcel
                )
            }

            override fun newArray(size: Int): Array<BenifitSlabTable> {
                return Array(size) { BenifitSlabTable() }
            }
        }

        fun performOperation(param: BenifitSlabTable, callback: () -> Unit) =
            withRealm {
                when (param.actionId) {
                    "1", "2" -> it.executeTransaction { i -> i.insertOrUpdate(param) }
                    "3" -> it.executeTransaction { i ->
                        i.where(BenifitSlabTable::class.java)
                            .equalTo("schemeSlabId", param.schemeSlabId).findAll()
                            .deleteAllFromRealm()
                    }
                }
                callback()
            }

        fun selectFromBenifitSlabTable(): List<BenifitSlabTable> = runBlocking {
            var result = listOf<BenifitSlabTable>()
            getRealm {
                result = it.copyFromRealm(
                        it.where(BenifitSlabTable::class.java)
                                .findAll()
                )
            }.await()
            result
        }

        fun clear() =
            withRealm {
                it.executeTransaction { i ->
                    i.delete(
                        BenifitSlabTable::class.java
                    )
                }
            }

    } // End of Companion object block

}

/**
 * Brand data table
 * information of brandId, BrandCodeName, Scheme Type etc
 * */
@RealmClass
open class BrandDataTable() : RealmObject(), Parcelable {
    @field:BHFieldParseIndex(0)
    var pcNo: String = ""

    @field:BHFieldParseIndex(1)
    var actionId: String = ""

    @field:BHFieldParseIndex(2)
    var tableId: String = ""

    @field:BHFieldParseIndex(3)
    var isActive: String = ""

    @field:BHFieldParseIndex(4)
    @PrimaryKey
    var brandId: String = ""

    @field:BHFieldParseIndex(5)
    var brandCodeName: String = ""

    @field:BHFieldParseIndex(6)
    var schemeType: String = ""

    @field:BHFieldParseIndex(7)
    var reserveValue: String = ""

    private constructor(parcel: Parcel) : this() {
        pcNo = parcel.readString().toString()
        actionId = parcel.readString().toString()
        tableId = parcel.readString().toString()
        isActive = parcel.readString().toString()
        brandId = parcel.readString().toString()
        brandCodeName = parcel.readString().toString()
        schemeType = parcel.readString().toString()
        reserveValue = parcel.readString().toString()
    }

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeString(pcNo)
        parcel.writeString(actionId)
        parcel.writeString(tableId)
        parcel.writeString(isActive)
        parcel.writeString(brandId)
        parcel.writeString(brandCodeName)
        parcel.writeString(schemeType)
        parcel.writeString(reserveValue)
    }

    override fun describeContents(): Int {
        return 0
    }

    companion object {
        private val TAG: String = BrandDataTable::class.java.simpleName

        @JvmField
        val CREATOR = object : Parcelable.Creator<BrandDataTable> {
            override fun createFromParcel(parcel: Parcel): BrandDataTable {
                return BrandDataTable(parcel)
            }

            override fun newArray(size: Int): Array<BrandDataTable?> {
                return Array(size) { BrandDataTable() }
            }
        }

        fun performOperation(param: BrandDataTable, callback: () -> Unit) =
            withRealm {
                when (param.actionId) {
                    "1", "2" -> it.executeTransaction { i -> i.insertOrUpdate(param) }
                    "3" -> it.executeTransaction { i ->
                        i.where(BrandDataTable::class.java)
                            .equalTo("brandId", param.brandId).findAll()
                            .deleteAllFromRealm()
                    }
                }
                callback()
            }

        fun selectFromBrandDataTable(): List<BrandDataTable> = runBlocking {
            var result = listOf<BrandDataTable>()
            getRealm {
                result = it.copyFromRealm(
                    it.where(BrandDataTable::class.java)
                        .findAll()
                )
            }.await()
            result
        }

        fun clear() =
            withRealm {
                it.executeTransaction { i ->
                    i.delete(
                        BrandDataTable::class.java
                    )
                }
            }

    }  // End of companion object block


}

/**
 * Emi Scheme Product Table
 * information of schemeProductId, schemeId, productId
 * */
@RealmClass
open class EmiSchemeProductTable() : RealmObject(), Parcelable {
    @field:BHFieldParseIndex(0)
    var pcNo: String = ""

    @field:BHFieldParseIndex(1)
    var actionId: String = ""

    @field:BHFieldParseIndex(2)
    var tableId: String = ""

    @field:BHFieldParseIndex(3)
    var isActive: String = ""

    @field:BHFieldParseIndex(4)
    @PrimaryKey
    var schemeProductId: String = ""

    @field:BHFieldParseIndex(5)
    var schemeId: String = ""

    @field:BHFieldParseIndex(6)
    var productId: String = ""

    private constructor(parcel: Parcel) : this() {
        pcNo = parcel.readString().toString()
        actionId = parcel.readString().toString()
        tableId = parcel.readString().toString()
        isActive = parcel.readString().toString()
        schemeProductId = parcel.readString().toString()
        schemeId = parcel.readString().toString()
        productId = parcel.readString().toString()
    }

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeString(pcNo)
        parcel.writeString(actionId)
        parcel.writeString(tableId)
        parcel.writeString(isActive)
        parcel.writeString(schemeProductId)
        parcel.writeString(schemeId)
        parcel.writeString(productId)
    }

    override fun describeContents(): Int {
        return 0
    }

    companion object {
        private val TAG: String = EmiSchemeProductTable::class.java.simpleName

        @JvmField
        val CREATOR = object : Parcelable.Creator<EmiSchemeProductTable> {
            override fun createFromParcel(parcel: Parcel): EmiSchemeProductTable {
                return EmiSchemeProductTable(
                    parcel
                )
            }

            override fun newArray(size: Int): Array<EmiSchemeProductTable> {
                return Array(size) { EmiSchemeProductTable() }
            }
        }

        fun performOperation(param: EmiSchemeProductTable, callback: () -> Unit) =
            withRealm {
                when (param.actionId) {
                    "1", "2" -> it.executeTransaction { i -> i.insertOrUpdate(param) }
                    "3" -> it.executeTransaction { i ->
                        i.where(EmiSchemeProductTable::class.java)
                            .equalTo("schemeProductId", param.schemeProductId).findAll()
                            .deleteAllFromRealm()
                    }
                }
                callback()
            }

        fun selectFromEmiSchemeProductTable(): List<EmiSchemeProductTable> = runBlocking {
            var result = listOf<EmiSchemeProductTable>()
            getRealm {
                result = it.copyFromRealm(
                    it.where(EmiSchemeProductTable::class.java)
                        .findAll()
                )
            }.await()
            result
        }

        fun clear() =
            withRealm {
                it.executeTransaction { i ->
                    i.delete(
                        EmiSchemeProductTable::class.java
                    )
                }
            }

    }  // End of companion object block
}

@RealmClass
open class EmiSchemeGroupTable() : RealmObject(), Parcelable {
    @field:BHFieldParseIndex(0)
    var pcNo: String = ""

    @field:BHFieldParseIndex(1)
    var actionId: String = ""

    @field:BHFieldParseIndex(2)
    var tableId: String = ""

    @field:BHFieldParseIndex(3)
    var isActive: String = ""

    @field:BHFieldParseIndex(4)
    @PrimaryKey
    var groupId: String = ""

    @field:BHFieldParseIndex(5)
    var emischemeIds: String = ""


    private constructor(parcel: Parcel) : this() {
        pcNo = parcel.readString().toString()
        actionId = parcel.readString().toString()
        tableId = parcel.readString().toString()
        isActive = parcel.readString().toString()
        groupId = parcel.readString().toString()
        emischemeIds = parcel.readString().toString()

    }

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeString(pcNo)
        parcel.writeString(actionId)
        parcel.writeString(tableId)
        parcel.writeString(isActive)
        parcel.writeString(groupId)
        parcel.writeString(emischemeIds)

    }

    override fun describeContents(): Int {
        return 0
    }

    companion object {
        private val TAG: String = EmiSchemeGroupTable::class.java.simpleName

        @JvmField
        val CREATOR = object : Parcelable.Creator<EmiSchemeGroupTable> {
            override fun createFromParcel(parcel: Parcel): EmiSchemeGroupTable {
                return EmiSchemeGroupTable(
                        parcel
                )
            }

            override fun newArray(size: Int): Array<EmiSchemeGroupTable> {
                return Array(size) { EmiSchemeGroupTable() }
            }
        }

        fun performOperation(param: EmiSchemeGroupTable, callback: () -> Unit) =
                withRealm {
                    it.executeTransaction { i -> i.insertOrUpdate(param) }
                    callback()
                }

        fun selectFromEmiSchemeGroupProductTable(): List<EmiSchemeGroupTable> = runBlocking {
            var result = listOf<EmiSchemeGroupTable>()
            getRealm {
                result = it.copyFromRealm(
                        it.where(EmiSchemeGroupTable::class.java)
                                .findAll()
                )
            }.await()
            result
        }

        fun clear() =
                withRealm {
                    it.executeTransaction { i ->
                        i.delete(
                                EmiSchemeGroupTable::class.java
                        )
                    }
                }

    }  // End of companion object block
}


/**
 * Product Table
 * information of min and max amount , downPaymentAmount. productId, categoryId
 * mapped to PRODUCT CATEGORY TABLE
 * */
@RealmClass
open class ProductTable() : RealmObject(), Parcelable {
    @field:BHFieldParseIndex(0)
    var pcNo: String = ""

    @field:BHFieldParseIndex(1)
    var actionId: String = ""

    @field:BHFieldParseIndex(2)
    var tableId: String = ""

    @field:BHFieldParseIndex(3)
    var isActive: String = ""

    @field:BHFieldParseIndex(4)
    @PrimaryKey
    var productId: String = ""

    @field:BHFieldParseIndex(5)
    var productCodeName: String = ""

    @field:BHFieldParseIndex(6)
    var categoryId: String = ""

    @field:BHFieldParseIndex(7)
    var serialCodeValidation: String = ""

    @field:BHFieldParseIndex(8)
    var minPrice: String = ""

    @field:BHFieldParseIndex(9)
    var maxPrice: String = ""

    @field:BHFieldParseIndex(10)
    var productMrp: String = ""

    @field:BHFieldParseIndex(11)
    var downPaymentAmount: String = ""

    private constructor(parcel: Parcel) : this() {
        pcNo = parcel.readString().toString()
        actionId = parcel.readString().toString()
        tableId = parcel.readString().toString()
        isActive = parcel.readString().toString()
        productId = parcel.readString().toString()
        productCodeName = parcel.readString().toString()
        categoryId = parcel.readString().toString()
        serialCodeValidation = parcel.readString().toString()
        minPrice = parcel.readString().toString()
        maxPrice = parcel.readString().toString()
        productMrp = parcel.readString().toString()
        downPaymentAmount = parcel.readString().toString()
    }

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeString(pcNo)
        parcel.writeString(actionId)
        parcel.writeString(tableId)
        parcel.writeString(isActive)
        parcel.writeString(productId)
        parcel.writeString(productCodeName)
        parcel.writeString(categoryId)
        parcel.writeString(serialCodeValidation)
        parcel.writeString(minPrice)
        parcel.writeString(maxPrice)
        parcel.writeString(productMrp)
        parcel.writeString(downPaymentAmount)
    }

    override fun describeContents(): Int {
        return 0
    }

    companion object {
        private val TAG: String = ProductTable::class.java.simpleName

        @JvmField
        val CREATOR = object : Parcelable.Creator<ProductTable> {
            override fun createFromParcel(parcel: Parcel): ProductTable {
                return ProductTable(parcel)
            }

            override fun newArray(size: Int): Array<ProductTable?> {
                return Array(size) { ProductTable() }
            }
        }

        fun performOperation(param: ProductTable, callback: () -> Unit) =
                withRealm {
                    when (param.actionId) {
                        "1", "2" -> it.executeTransaction { i -> i.insertOrUpdate(param) }
                        "3" -> it.executeTransaction { i ->
                            i.where(ProductTable::class.java)
                                    .equalTo("productId", param.productId)
                                    .findAll()
                                    .deleteAllFromRealm()
                        }
                    }
                    callback()
                }

        fun selectFromProductTable(): List<ProductTable> = runBlocking {
            var result = listOf<ProductTable>()
            getRealm {
                result = it.copyFromRealm(
                        it.where(ProductTable::class.java)
                                .findAll()
                )
            }.await()
            result
        }

        fun clear() =
                withRealm {
                    it.executeTransaction { i ->
                        i.delete(
                                ProductTable::class.java
                        )
                    }
                }

    }  // End of Companion Object block

}

/**
 * ProductCategoryTable
 * information of categoryId, brandId, categoryCodeName, parentCategoryId
 * mapped to BRAND TABLE
 * */
@RealmClass
open class ProductCategoryTable() : RealmObject(), Parcelable {
    @field:BHFieldParseIndex(0)
    var pcNo: String = ""

    @field:BHFieldParseIndex(1)
    var actionId: String = ""

    @field:BHFieldParseIndex(2)
    var tableId: String = ""

    @field:BHFieldParseIndex(3)
    var isActive: String = ""

    @field:BHFieldParseIndex(4)
    @PrimaryKey
    var categoryId: String = ""

    @field:BHFieldParseIndex(5)
    var brandId: String = ""

    @field:BHFieldParseIndex(6)
    var categoryCodeName: String = ""

    @field:BHFieldParseIndex(7)
    var parentCategoryId: String = ""

    private constructor(parcel: Parcel) : this() {
        pcNo = parcel.readString().toString()
        actionId = parcel.readString().toString()
        tableId = parcel.readString().toString()
        isActive = parcel.readString().toString()
        categoryId = parcel.readString().toString()
        brandId = parcel.readString().toString()
        categoryCodeName = parcel.readString().toString()
        parentCategoryId = parcel.readString().toString()
    }

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeString(pcNo)
        parcel.writeString(actionId)
        parcel.writeString(tableId)
        parcel.writeString(isActive)
        parcel.writeString(categoryId)
        parcel.writeString(brandId)
        parcel.writeString(categoryCodeName)
        parcel.writeString(parentCategoryId)
    }

    override fun describeContents(): Int {
        return 0
    }

    companion object {
        private val TAG = ProductCategoryTable::class.java.simpleName

        @JvmField
        val CREATOR = object : Parcelable.Creator<ProductCategoryTable> {
            override fun createFromParcel(parcel: Parcel): ProductCategoryTable {
                return ProductCategoryTable(
                        parcel
                )
            }

            override fun newArray(size: Int): Array<ProductCategoryTable> {
                return Array(size) { ProductCategoryTable() }
            }

            fun performOperation(param: ProductCategoryTable) =
                    withRealm {
                        when (param.actionId) {
                            "1", "2" -> it.insertOrUpdate(param)
                            "3" -> it.where(ProductCategoryTable::class.java)
                                    .equalTo(
                                            "categoryId",
                                            param.categoryId
                                    ).findAll().deleteAllFromRealm()
                        }
                    }

            fun selectFromBrandDataTable(): List<ProductCategoryTable> = runBlocking {
                var result = listOf<ProductCategoryTable>()
                getRealm {
                    result = it.copyFromRealm(
                            it.where(ProductCategoryTable::class.java)
                                    .findAll()
                    )
                }.await()
                result
            }

            fun clear() = withRealm {
                it.delete(ProductCategoryTable::class.java)
            }

        }

        fun performOperation(param: ProductCategoryTable, callback: () -> Unit) =
                withRealm {
                    when (param.actionId) {
                        "1", "2" -> it.executeTransaction { i -> i.insertOrUpdate(param) }
                        "3" -> it.executeTransaction { i ->
                            i.where(ProductCategoryTable::class.java)
                                    .equalTo("categoryId", param.categoryId).findAll()
                        }
                    }
                    callback()
                }

        fun selectFromProductCategoryTable(): List<ProductCategoryTable> = runBlocking {
            var result = listOf<ProductCategoryTable>()
            getRealm {
                result = it.copyFromRealm(
                        it.where(ProductCategoryTable::class.java)
                                .findAll()
                )
            }
            result
        }

        fun clear() =
                withRealm {
                    it.executeTransaction { i ->
                        i.delete(
                                ProductCategoryTable::class.java
                        )
                    }
                }

    }
}
//endregion

//region===============Issuer Terms and Condition Table for EMI:-
@RealmClass
open class IssuerTAndCTable() : RealmObject(), Parcelable {
    @PrimaryKey
    var issuerId: String = ""
    var headerTAndC: String = ""
    var footerTAndC: String = ""

    private constructor(parcel: Parcel) : this() {
        issuerId = parcel.readString().toString()
        headerTAndC = parcel.readString().toString()
        footerTAndC = parcel.readString().toString()
    }

    override fun writeToParcel(p0: Parcel?, p1: Int) {
        p0?.writeString(issuerId)
        p0?.writeString(headerTAndC)
        p0?.writeString(footerTAndC)
    }

    override fun describeContents(): Int {
        return 0
    }

    companion object {
        private val TAG = IssuerTAndCTable::class.java.simpleName

        @JvmField
        val CREATOR = object : Parcelable.Creator<IssuerTAndCTable> {
            override fun createFromParcel(parcel: Parcel): IssuerTAndCTable {
                return IssuerTAndCTable(parcel)
            }

            override fun newArray(size: Int): Array<IssuerTAndCTable> {
                return Array(size) { IssuerTAndCTable() }
            }
        }

        fun performOperation(param: IssuerTAndCTable) =
                withRealm { it.executeTransaction { i -> i.insertOrUpdate(param) } }

        //region====================Method to Get All IssuerTAndC Data================
        fun getAllIssuerTAndCData(): MutableList<IssuerTAndCTable>? = runBlocking {
            var result = mutableListOf<IssuerTAndCTable>()
            getRealm {
                val re = it.copyFromRealm(it.where(IssuerTAndCTable::class.java).findAll())
                if (re != null) result = re

            }.await()
            result
        }
        //endregion

        //region===================Method to Get All IssuerTAndC Data by Issuer ID================
        fun selectIssuerTAndCDataByID(issuerId: String): IssuerTAndCTable = runBlocking {
            var result = IssuerTAndCTable()
            getRealm {
                val re = it.copyFromRealm(
                        it.where(IssuerTAndCTable::class.java)
                                .equalTo("issuerId", issuerId)
                                .findFirst()
                )
                if (re != null) result = re

            }.await()
            result
        }
        //endregion

        fun clear() =
                withRealm {
                    it.executeTransaction { i ->
                        i.delete(
                                IssuerTAndCTable::class.java
                        )
                    }
                }
    }
}
//endregion

// region===============Brand Terms and Condition Table for EMI:-
@RealmClass
open class BrandTAndCTable() : RealmObject(), Parcelable {
    @PrimaryKey
    var brandId: String = ""
    var brandTAndC: String? = ""

    private constructor(parcel: Parcel) : this() {
        brandId = parcel.readString().toString()
        brandTAndC = parcel.readString().toString()
    }

    override fun writeToParcel(p0: Parcel?, p1: Int) {
        p0?.writeString(brandId)
        p0?.writeString(brandTAndC)
    }

    override fun describeContents(): Int {
        return 0
    }

    companion object {
        private val TAG = BrandTAndCTable::class.java.simpleName

        @JvmField
        val CREATOR = object : Parcelable.Creator<BrandTAndCTable> {
            override fun createFromParcel(parcel: Parcel): BrandTAndCTable {
                return BrandTAndCTable(parcel)
            }

            override fun newArray(size: Int): Array<BrandTAndCTable> {
                return Array(size) { BrandTAndCTable() }
            }
        }

        fun performOperation(param: BrandTAndCTable) =
                withRealm { it.executeTransaction { i -> i.insertOrUpdate(param) } }

        //region====================Method to Get All BrandTAndC Data================
        fun getAllBrandTAndCData(): MutableList<BrandTAndCTable> = runBlocking {
            var result = mutableListOf<BrandTAndCTable>()
            getRealm {
                val re = it.copyFromRealm(it.where(BrandTAndCTable::class.java).findAll())
                if (re != null) result = re

            }.await()
            result
        }
        //endregion

        fun clear() =
                withRealm {
                    it.executeTransaction { i ->
                        i.delete(
                                BrandTAndCTable::class.java
                        )
                    }
                }
    }
}
//endregion

// region===============Brand EMI Master Category TimeStamps Table:-
@RealmClass
open class BrandEMIMasterTimeStamps() : RealmObject(), Parcelable {
    @PrimaryKey
    var brandTimeStamp: String = ""
    var brandCategoryUpdatedTimeStamp: String = ""
    var issuerTAndCTimeStamp: String = ""
    var brandTAndCTimeStamp: String = ""

    private constructor(parcel: Parcel) : this() {
        brandTimeStamp = parcel.readString().toString()
        brandCategoryUpdatedTimeStamp = parcel.readString().toString()
        issuerTAndCTimeStamp = parcel.readString().toString()
        brandTAndCTimeStamp = parcel.readString().toString()
    }

    override fun writeToParcel(p0: Parcel?, p1: Int) {
        p0?.writeString(brandTimeStamp)
        p0?.writeString(brandCategoryUpdatedTimeStamp)
        p0?.writeString(issuerTAndCTimeStamp)
        p0?.writeString(brandTAndCTimeStamp)
    }

    override fun describeContents(): Int {
        return 0
    }

    companion object {
        private val TAG = BrandEMIMasterTimeStamps::class.java.simpleName

        @JvmField
        val CREATOR = object : Parcelable.Creator<BrandEMIMasterTimeStamps> {
            override fun createFromParcel(parcel: Parcel): BrandEMIMasterTimeStamps {
                return BrandEMIMasterTimeStamps(parcel)
            }

            override fun newArray(size: Int): Array<BrandEMIMasterTimeStamps> {
                return Array(size) { BrandEMIMasterTimeStamps() }
            }
        }

        fun performOperation(param: BrandEMIMasterTimeStamps) =
            withRealm { it.executeTransaction { i -> i.insertOrUpdate(param) } }

        //region====================Update IssuerTAndCTimeStamp Data:-
        fun updateIssuerTandCTimeStamp(issuerTAndCTimeStampData: String?) = runBlocking {
            getRealm {
                val res = it.where(BrandEMIMasterTimeStamps::class.java).findFirst()
                it.beginTransaction()
                res?.issuerTAndCTimeStamp = issuerTAndCTimeStampData ?: ""
                it.commitTransaction()
            }.await()
        }
        //endregion

        //region====================Method to Get All BrandTAndC Data================
        fun getAllBrandEMIMasterDataTimeStamps(): MutableList<BrandEMIMasterTimeStamps> =
            runBlocking {
                var result = mutableListOf<BrandEMIMasterTimeStamps>()
                getRealm {
                    val re =
                        it.copyFromRealm(it.where(BrandEMIMasterTimeStamps::class.java).findAll())
                    if (re != null) result = re

                }.await()
                result
            }
        //endregion

        fun clear() =
            withRealm {
                it.executeTransaction { i ->
                    i.delete(
                        BrandEMIMasterTimeStamps::class.java
                    )
                }
            }
    }
}
//endregion

// region===============Brand EMI Sub-Category Data Table:-
@RealmClass
open class BrandEMISubCategoryTable() : RealmObject(), Parcelable {
    var brandID: String = ""

    @PrimaryKey
    var categoryID: String = ""
    var parentCategoryID: String = ""
    var categoryName: String = ""

    private constructor(parcel: Parcel) : this() {
        brandID = parcel.readString().toString()
        categoryID = parcel.readString().toString()
        parentCategoryID = parcel.readString().toString()
        categoryName = parcel.readString().toString()
    }

    override fun writeToParcel(p0: Parcel?, p1: Int) {
        p0?.writeString(brandID)
        p0?.writeString(categoryID)
        p0?.writeString(parentCategoryID)
        p0?.writeString(categoryName)
    }

    override fun describeContents(): Int {
        return 0
    }

    companion object {
        private val TAG = BrandEMISubCategoryTable::class.java.simpleName

        @JvmField
        val CREATOR = object : Parcelable.Creator<BrandEMISubCategoryTable> {
            override fun createFromParcel(parcel: Parcel): BrandEMISubCategoryTable {
                return BrandEMISubCategoryTable(parcel)
            }

            override fun newArray(size: Int): Array<BrandEMISubCategoryTable> {
                return Array(size) { BrandEMISubCategoryTable() }
            }
        }

        fun performOperation(param: BrandEMISubCategoryTable) =
            withRealm { it.executeTransaction { i -> i.insertOrUpdate(param) } }

        //region====================Method to Get All Sub-Category Table Data================
        fun getAllSubCategoryTableData(): MutableList<BrandEMISubCategoryTable> =
            runBlocking {
                var result = mutableListOf<BrandEMISubCategoryTable>()
                getRealm {
                    val re =
                        it.copyFromRealm(it.where(BrandEMISubCategoryTable::class.java).findAll())
                    if (re != null) result = re

                }.await()
                result
            }
        //endregion

        // region====================Method to Get All Sub-Category Table Data================
        fun getAllSubCategoryTableDataByBrandID(brand_id: String): MutableList<BrandEMISubCategoryTable> =
            runBlocking {
                var result = mutableListOf<BrandEMISubCategoryTable>()
                getRealm {
                    val re =
                        it.copyFromRealm(
                            it.where(BrandEMISubCategoryTable::class.java)
                                .equalTo("brandID", brand_id)
                                .findAll()
                        )
                    if (re != null) result = re

                }.await()
                result
            }
        //endregion

        // region====================Method to Get All Sub-Category Table Data by Matching CategoryID with ParentCategoryID================
        fun getAllDataByMatchingCategoryIdWithParentCategoryID(categoryID: String): MutableList<BrandEMISubCategoryTable> =
            runBlocking {
                var result = mutableListOf<BrandEMISubCategoryTable>()
                getRealm {
                    val re = it.copyFromRealm(
                        it.where(BrandEMISubCategoryTable::class.java)
                            .equalTo("parentCategoryID", categoryID)
                            .findAll()
                    )
                    if (re != null) result = re

                }.await()
                result
            }
        //endregion

        suspend fun clear() =
            withRealm {
                it.executeTransaction { i ->
                        i.delete(
                                BrandEMISubCategoryTable::class.java
                        )
                    }
                }
    }
}
//endregion

//region===============Brand EMI Data Table:-
@RealmClass
open class BrandEMIDataTable() : RealmObject(), Parcelable {
    @PrimaryKey
    var brandID: String = ""
    var brandName: String = ""
    var brandReservedValues: String = ""
    var categoryID: String = ""
    var categoryName: String = ""
    var productID: String = ""
    var productName: String = ""
    var childSubCategoryID: String = ""
    var childSubCategoryName: String = ""
    var validationTypeName: String = ""
    var isRequired: String = ""
    var inputDataType: String = ""
    var imeiNumber: String = ""
    var serialNumber: String = ""
    var emiType: String = ""

    private constructor(parcel: Parcel) : this() {
        brandID = parcel.readString().toString()
        brandName = parcel.readString().toString()
        brandReservedValues = parcel.readString().toString()
        categoryID = parcel.readString().toString()
        categoryName = parcel.readString().toString()
        productID = parcel.readString().toString()
        productName = parcel.readString().toString()
        childSubCategoryID = parcel.readString().toString()
        childSubCategoryName = parcel.readString().toString()
        validationTypeName = parcel.readString().toString()
        isRequired = parcel.readString().toString()
        inputDataType = parcel.readString().toString()
        imeiNumber = parcel.readString().toString()
        serialNumber = parcel.readString().toString()
        emiType = parcel.readString().toString()
    }

    override fun writeToParcel(p0: Parcel?, p1: Int) {
        p0?.writeString(brandID)
        p0?.writeString(brandName)
        p0?.writeString(brandReservedValues)
        p0?.writeString(categoryID)
        p0?.writeString(categoryName)
        p0?.writeString(productID)
        p0?.writeString(productName)
        p0?.writeString(childSubCategoryID)
        p0?.writeString(childSubCategoryName)
        p0?.writeString(validationTypeName)
        p0?.writeString(isRequired)
        p0?.writeString(inputDataType)
        p0?.writeString(imeiNumber)
        p0?.writeString(serialNumber)
        p0?.writeString(emiType)
    }

    override fun describeContents(): Int {
        return 0
    }

    companion object {
        private val TAG = BrandEMIDataTable::class.java.simpleName

        @JvmField
        val CREATOR = object : Parcelable.Creator<BrandEMIDataTable> {
            override fun createFromParcel(parcel: Parcel): BrandEMIDataTable {
                return BrandEMIDataTable(parcel)
            }

            override fun newArray(size: Int): Array<BrandEMIDataTable> {
                return Array(size) { BrandEMIDataTable() }
            }
        }

        fun performOperation(param: BrandEMIDataTable) =
            withRealm { it.executeTransaction { i -> i.insertOrUpdate(param) } }

        //region====================Method to Get All IssuerTAndC Data================
        fun getAllEMIData(): BrandEMIDataTable = runBlocking {
            var result = BrandEMIDataTable()
            getRealm {
                val re = it.copyFromRealm(it.where(BrandEMIDataTable::class.java).findFirst())
                if (re != null) result = re

            }.await()
            result
        }
        //endregion

        //region===================Method to Clear BrandEMIData Table:-
        fun clear() =
            withRealm { it.executeTransaction { i -> i.delete(BrandEMIDataTable::class.java) } }
        //endregion

    }
}
//endregion

//region================Brand EMI By Access Code Table:-
open class BrandEMIAccessDataModalTable() : RealmObject(), Parcelable {
    @PrimaryKey
    var emiCode: String = ""
    var bankID: String = ""
    var bankTID: String = ""
    var issuerID: String = ""
    var tenure: String = ""
    var brandID: String = ""
    var productID: String = ""
    var emiSchemeID: String = ""
    var transactionAmount: String = ""
    var discountAmount: String = ""
    var loanAmount: String = ""
    var interestAmount: String = ""
    var emiAmount: String = ""
    var cashBackAmount: String = ""
    var netPayAmount: String = ""
    var processingFee: String = ""
    var processingFeeRate: String = ""
    var totalProcessingFee: String = ""
    var brandName: String = ""
    var issuerName: String = ""
    var productName: String = ""
    var productCode: String = ""
    var productModal: String = ""
    var productCategoryName: String = ""
    var productSerialCode: String = ""
    var skuCode: String = ""
    var totalInterest: String = ""
    var schemeTAndC: String = ""
    var schemeTenureTAndC: String = ""
    var schemeDBDTAndC: String = ""
    var discountCalculatedValue: String = ""
    var cashBackCalculatedValue: String = ""

    private constructor(parcel: Parcel) : this() {
        emiCode = parcel.readString().toString()
        bankID = parcel.readString().toString()
        bankTID = parcel.readString().toString()
        issuerID = parcel.readString().toString()
        tenure = parcel.readString().toString()
        brandID = parcel.readString().toString()
        productID = parcel.readString().toString()
        emiSchemeID = parcel.readString().toString()
        transactionAmount = parcel.readString().toString()
        discountAmount = parcel.readString().toString()
        loanAmount = parcel.readString().toString()
        interestAmount = parcel.readString().toString()
        emiAmount = parcel.readString().toString()
        cashBackAmount = parcel.readString().toString()
        netPayAmount = parcel.readString().toString()
        processingFee = parcel.readString().toString()
        processingFeeRate = parcel.readString().toString()
        totalProcessingFee = parcel.readString().toString()
        brandName = parcel.readString().toString()
        processingFeeRate = parcel.readString().toString()
        issuerName = parcel.readString().toString()
        productName = parcel.readString().toString()
        productCode = parcel.readString().toString()
        productModal = parcel.readString().toString()
        productCategoryName = parcel.readString().toString()
        productSerialCode = parcel.readString().toString()
        skuCode = parcel.readString().toString()
        totalInterest = parcel.readString().toString()
        schemeTAndC = parcel.readString().toString()
        schemeTenureTAndC = parcel.readString().toString()
        schemeDBDTAndC = parcel.readString().toString()
        discountCalculatedValue = parcel.readString().toString()
        cashBackCalculatedValue = parcel.readString().toString()
    }

    override fun writeToParcel(p0: Parcel?, p1: Int) {
        p0?.writeString(emiCode)
        p0?.writeString(bankID)
        p0?.writeString(bankTID)
        p0?.writeString(issuerID)
        p0?.writeString(tenure)
        p0?.writeString(brandID)
        p0?.writeString(productID)
        p0?.writeString(emiSchemeID)
        p0?.writeString(transactionAmount)
        p0?.writeString(discountAmount)
        p0?.writeString(loanAmount)
        p0?.writeString(interestAmount)
        p0?.writeString(emiAmount)
        p0?.writeString(cashBackAmount)
        p0?.writeString(netPayAmount)
        p0?.writeString(processingFee)
        p0?.writeString(processingFeeRate)
        p0?.writeString(totalProcessingFee)
        p0?.writeString(brandName)
        p0?.writeString(processingFeeRate)
        p0?.writeString(issuerName)
        p0?.writeString(productName)
        p0?.writeString(productCode)
        p0?.writeString(productModal)
        p0?.writeString(productCategoryName)
        p0?.writeString(productSerialCode)
        p0?.writeString(skuCode)
        p0?.writeString(totalInterest)
        p0?.writeString(schemeTAndC)
        p0?.writeString(schemeTenureTAndC)
        p0?.writeString(schemeDBDTAndC)
        p0?.writeString(discountCalculatedValue)
        p0?.writeString(cashBackCalculatedValue)
    }

    override fun describeContents(): Int {
        return 0
    }

    companion object {
        private val TAG = BrandEMIAccessDataModalTable::class.java.simpleName

        @JvmField
        val CREATOR = object : Parcelable.Creator<BrandEMIAccessDataModalTable> {
            override fun createFromParcel(parcel: Parcel): BrandEMIAccessDataModalTable {
                return BrandEMIAccessDataModalTable(parcel)
            }

            override fun newArray(size: Int): Array<BrandEMIAccessDataModalTable> {
                return Array(size) { BrandEMIAccessDataModalTable() }
            }
        }

        fun performOperation(param: BrandEMIAccessDataModalTable) =
            withRealm { it.executeTransaction { i -> i.insertOrUpdate(param) } }

        //region====================Method to Get BrandEMIByAccessCode Data================
        fun getBrandEMIByAccessCodeData(): BrandEMIAccessDataModalTable = runBlocking {
            var result = BrandEMIAccessDataModalTable()
            getRealm {
                val re =
                    it.copyFromRealm(it.where(BrandEMIAccessDataModalTable::class.java).findFirst())
                if (re != null) result = re

            }.await()
            result
        }
        //endregion

        //region===================Method to Clear BrandEMI By AccessCode Table:-
        fun clear() =
            withRealm { it.executeTransaction { i -> i.delete(BrandEMIAccessDataModalTable::class.java) } }
        //endregion
    }
}
//endregion


/**
 * Table for DigiPos
 * */
//region================DigiPosTable Table:-\
@RealmClass
open class DigiPosDataTable() : RealmObject(), Parcelable {
    // Digi POS Data
    var requestType: Int = 0
    var amount = ""
    var description = ""
    var vpa = ""
    var mTxnId = ""

    @PrimaryKey
    var partnerTxnId = ""
    var status = ""
    var statusMsg = ""
    var statusCode = ""
    var customerMobileNumber = ""
    var transactionTimeStamp = ""
    var txnStatus = EDigiPosPaymentStatus.Pending.desciption
    var paymentMode = ""
    var pgwTxnId = ""
    var txnDate = ""
    var txnTime = ""
    var displayFormatedDate = ""

    private constructor(parcel: Parcel) : this()

    override fun writeToParcel(p0: Parcel?, p1: Int) {

    }

    override fun describeContents(): Int {
        return 0
    }

    companion object {
        private val TAG: String = DigiPosDataTable::class.java.simpleName

        @JvmField
        val CREATOR = object : Parcelable.Creator<DigiPosDataTable> {
            override fun createFromParcel(parcel: Parcel): DigiPosDataTable {
                return DigiPosDataTable(
                    parcel
                )
            }

            override fun newArray(size: Int): Array<DigiPosDataTable> {
                return Array(size) { DigiPosDataTable() }
            }
        }

        fun insertOrUpdateDigiposData(param: DigiPosDataTable) =
            withRealm {
                it.executeTransaction { i ->
                    i.insertOrUpdate(param)
                }
            }

        fun insertOrUpdateDigiposDataWithCB(param: DigiPosDataTable, callback: () -> Unit) =
            withRealm {
                it.executeTransaction { i ->
                    i.insertOrUpdate(param)
                }
                callback()
            }

        fun selectAllDigiPosData(): MutableList<DigiPosDataTable> = runBlocking {
            var result = mutableListOf<DigiPosDataTable>()
            getRealm {
                val re = it.copyFromRealm(it.where(DigiPosDataTable::class.java).findAll())
                if (re != null) result = re

            }.await()
            result
        }

        fun selectDigiPosDataAccordingToTxnStatus(status: String): MutableList<DigiPosDataTable> =
            runBlocking {
                var result = mutableListOf<DigiPosDataTable>()
                getRealm {
                    val re = it.copyFromRealm(
                        it.where(DigiPosDataTable::class.java)
                            .equalTo("txnStatus", status)
                            .findAll()
                    )
                    if (re != null) result = re

                }.await()
                result
            }

        fun deletAllRecordAccToTxnStatus(txnStatus: String) =
            withRealm {
                it.executeTransaction { i ->
                    i.where(DigiPosDataTable::class.java)
                        .equalTo(
                            "txnStatus",
                            txnStatus
                        ).findAll()?.deleteAllFromRealm()
                }
            }

        fun deletRecord(partnerTxnId: String) =
            withRealm {
                it.executeTransaction { i ->
                    i.where(DigiPosDataTable::class.java)
                        .equalTo(
                            "partnerTxnId",
                            partnerTxnId
                        ).findAll()?.deleteAllFromRealm()
                }
            }

        fun clear() =
            withRealm {
                it.executeTransaction { i ->
                    i.delete(
                        DigiPosDataTable::class.java
                    )
                }
            }

    }// end of companion block/////


}
//endregion

@RealmClass
open class TxnCallBackRequestTable() : RealmObject(), Parcelable {
    @PrimaryKey
    var roc = ""
    var reqtype = ""
    var tid = ""
    var batchnum = ""
    var amount = ""


    private constructor(parcel: Parcel) : this()


    override fun describeContents(): Int {
        return 0
    }

    override fun writeToParcel(dest: Parcel?, flags: Int) {

    }

    companion object {
        private val TAG: String = TxnCallBackRequestTable::class.java.simpleName

        @JvmField
        val CREATOR = object : Parcelable.Creator<TxnCallBackRequestTable> {
            override fun createFromParcel(parcel: Parcel): TxnCallBackRequestTable {
                return TxnCallBackRequestTable(
                    parcel
                )
            }

            override fun newArray(size: Int): Array<TxnCallBackRequestTable> {
                return Array(size) { TxnCallBackRequestTable() }
            }
        }

        fun insertOrUpdateTxnCallBackData(param: TxnCallBackRequestTable) =
            withRealm {
                it.executeTransaction { i ->
                    i.insertOrUpdate(param)
                }
            }

        fun insertOrUpdateTxnCallBackDataWithCB(
            param: TxnCallBackRequestTable,
            callback: () -> Unit
        ) =
            withRealm {
                it.executeTransaction { i ->
                    i.insertOrUpdate(param)
                }
                callback()
            }

        fun selectAllTxnCallBackData(): MutableList<TxnCallBackRequestTable> = runBlocking {
            var result = mutableListOf<TxnCallBackRequestTable>()
            getRealm {
                val re = it.copyFromRealm(it.where(TxnCallBackRequestTable::class.java).findAll())
                if (re != null) result = re

            }.await()
            result
        }


        fun deletRecord(roc: String) =
            withRealm {
                it.executeTransaction { i ->
                    i.where(TxnCallBackRequestTable::class.java)
                        .equalTo(
                            "roc",
                            roc
                        ).findAll()?.deleteAllFromRealm()
                }
            }

        fun clear() =
            withRealm {
                it.executeTransaction { i ->
                    i.delete(
                        TxnCallBackRequestTable::class.java
                    )
                }
            }

    }
}


@RealmClass
open class OfflineSaleTable() : RealmObject(), Parcelable {
    var maskedPan = ""
    var amount: Long = 0
    var encriptedF57 = ""
    var apprCode = ""

    var roc: Int = 0
    var batchNo: Int = 0
    var invoiceNo: Int = 0
    var time: Long = 0

    var tid = ""
    var mid = ""

    var f58 = ""  // Indicator
    var f61 = ""  // all details


    private constructor(parcel: Parcel) : this() {
        maskedPan = parcel.readString().toString()
        amount = parcel.readLong()
        encriptedF57 = parcel.readString().toString()
        apprCode = parcel.readString().toString()

        roc = parcel.readInt()
        batchNo = parcel.readInt()
        invoiceNo = parcel.readInt()
        time = parcel.readLong()

        tid = parcel.readString().toString()
        mid = parcel.readString().toString()

        f58 = parcel.readString().toString()
        f61 = parcel.readString().toString()

    }

    override fun writeToParcel(dest: Parcel, flags: Int) {

        dest.writeString(maskedPan)
        dest.writeLong(amount)
        dest.writeString(encriptedF57)
        dest.writeString(apprCode)

        dest.writeInt(roc)
        dest.writeInt(batchNo)
        dest.writeInt(invoiceNo)
        dest.writeLong(time)

        dest.writeString(tid)
        dest.writeString(mid)

        dest.writeString(f58)
        dest.writeString(f61)

    }

    override fun describeContents(): Int = 0

    fun getTimeString(): String {
        date.time = time
        return formatter.format(
            date
        )
    }

    companion object {
        private val TAG = OfflineSaleTable::class.java.simpleName

        @JvmField
        val CREATOR = object : Parcelable.Creator<OfflineSaleTable> {
            override fun createFromParcel(source: Parcel): OfflineSaleTable {
                return OfflineSaleTable(
                    source
                )
            }

            override fun newArray(size: Int): Array<OfflineSaleTable> =
                Array(size) { OfflineSaleTable() }
        }

        fun insertOrUpdate(param: OfflineSaleTable) =
            withRealm {
                it.executeTransaction { i -> i.insertOrUpdate(param) }
            }

        fun selectFromProductCategoryTable(): List<OfflineSaleTable> = runBlocking {
            var result = listOf<OfflineSaleTable>()
            getRealm {
                result = it.copyFromRealm(
                    it.where(OfflineSaleTable::class.java)
                        .findAll()
                )
            }.await()
            result
        }

        fun delete(ost: OfflineSaleTable) {
            withRealm {
                it.executeTransaction { i ->
                    val result =
                        i.where(OfflineSaleTable::class.java)
                            .equalTo("invoiceNo", ost.invoiceNo)
                            .findFirst()
                    result?.deleteFromRealm()
                }
            }
        }

        fun clear() =
            withRealm {
                it.executeTransaction { i ->
                    i.delete(
                        OfflineSaleTable::class.java
                    )
                }
            }


        private val formatter = SimpleDateFormat("dd-MMM-yyyy\nhh:mm a")
        private val date = Date()
    }


}


suspend fun clearDatabase() {
    BatchFileDataTable.clear()
    IssuerParameterTable.clear()
    TerminalCommunicationTable.clear()
    TerminalParameterTable.clear()
    CardDataTable.clear()

    TenureTable.clear()
    SchemeTable.clear()
    EmiBinTable.clear()
    EmiSchemeTable.clear()
    BenifitSlabTable.clear()

    BrandDataTable.clear()
    EmiSchemeProductTable.clear()
    ProductTable.clear()
    ProductCategoryTable.clear()
}


@MustBeDocumented
@Target(AnnotationTarget.PROPERTY, AnnotationTarget.FIELD)
@Retention(AnnotationRetention.RUNTIME)
annotation class BHFieldName(val name: String, val isToShow: Boolean = true)

@MustBeDocumented
@Target(AnnotationTarget.PROPERTY, AnnotationTarget.FIELD)
@Retention(AnnotationRetention.RUNTIME)
annotation class BHFieldParseIndex(val index: Int)

@MustBeDocumented
@Target(AnnotationTarget.FIELD, AnnotationTarget.PROPERTY)
@Retention(AnnotationRetention.RUNTIME)
annotation class BHDashboardItem(
    val item: EDashboardItem,
    val childItem: EDashboardItem = EDashboardItem.NONE
)

//region========================push bill table for sms pay=======================
@RealmClass
open class SmsPushBill() : RealmObject(), Parcelable {
    @PrimaryKey
    var id = ""
    var amount = ""
    var mobile = ""
    var request = ""

    constructor(parcel: Parcel) : this() {
        id = parcel.readString().toString()
        amount = parcel.readString().toString()
        mobile = parcel.readString().toString()
        request = parcel.readString().toString()
    }

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeString(id)
        parcel.writeString(amount)
        parcel.writeString(mobile)
        parcel.writeString(request)
    }

    override fun describeContents(): Int {
        return 0
    }

    companion object {
        private val TAG: String = SmsPushBill::class.java.simpleName

        @JvmField
        val CREATOR = object : Parcelable.Creator<SmsPushBill> {
            override fun createFromParcel(parcel: Parcel): SmsPushBill {
                return SmsPushBill(parcel)
            }

            override fun newArray(size: Int): Array<SmsPushBill> {
                return Array(size) { SmsPushBill() }
            }
        }

        fun select(mobile: String): SmsPushBill? = runBlocking {
            var result: SmsPushBill? = null
            getRealm {
                val re =
                    it.where(SmsPushBill::class.java)
                        .equalTo("mobile", mobile).findFirst()
                if (re != null) result = it.copyFromRealm(re)
            }.await()
            result
        }

        fun select(): List<SmsPushBill> = runBlocking {
            var result = listOf<SmsPushBill>()
            getRealm {
                result = it.copyFromRealm(
                    it.where(SmsPushBill::class.java)
                        .findAll()
                )
            }.await()
            result
        }

        fun clear() = withRealm {
            it.executeTransaction { i ->
                i.delete(SmsPushBill::class.java)
            }
        }

        fun delete(mobile: String, callback: (Boolean) -> Unit) =
            withRealm {
                var result = false
                it.executeTransaction { i ->
                    result =
                        i.where(SmsPushBill::class.java)
                            .equalTo("mobile", mobile).findAll()
                            .deleteAllFromRealm()
                }
                callback(result)
            }

    }

}
//endregion


//region=================HDFC Tables=================
@RealmClass
open class HdfcTpt() : RealmObject(), Parcelable {
    @field:BHFieldParseIndex(0)
    var pcNo = ""

    @field:BHFieldParseIndex(1)
    var actionId = ""

    @field:BHFieldParseIndex(2)
    var tableId = ""

    @field:BHFieldParseIndex(3)
    var isActive = ""

    @field:BHFieldParseIndex(4)
    @PrimaryKey
    var recordId = ""

    @field:BHFieldParseIndex(5)
    var bankId = ""

    @field:BHFieldParseIndex(6)
    var bankTid = ""

    @field:BHFieldParseIndex(7)
    var dateTime = ""

    @field:BHFieldParseIndex(8)
    var adminPassword = ""

    @field:BHFieldParseIndex(9)  // bit oriented for
    var option1 = ""

    @field:BHFieldParseIndex(10)
    var option2 = ""

    @field:BHFieldParseIndex(11)
    var receiptL2 = "" // header2

    @field:BHFieldParseIndex(12)
    var receiptL3 = ""// header3

    @field:BHFieldParseIndex(13)
    var defaultMerchantName = "" // header 1

    @field:BHFieldParseIndex(14)
    var localTerminalOption = ""


    @field:BHFieldParseIndex(15)
    var helpDeskNumber = ""

    @field:BHFieldParseIndex(16)
    var transAmountDigit = ""

    @field:BHFieldParseIndex(17)
    var settleAmtDigit = ""

    @field:BHFieldParseIndex(18)
    var option3 = ""

    @field:BHFieldParseIndex(19)
    var option4 = ""

    override fun writeToParcel(dest: Parcel, flags: Int) {
        dest.writeString(pcNo)
        dest.writeString(actionId)
        dest.writeString(tableId)
        dest.writeString(isActive)
        dest.writeString(recordId)
        dest.writeString(bankId)
        dest.writeString(bankTid)
        dest.writeString(dateTime)
        dest.writeString(adminPassword)
        dest.writeString(option1)
        dest.writeString(option2)
        dest.writeString(receiptL2)
        dest.writeString(receiptL3)
        dest.writeString(defaultMerchantName)
        dest.writeString(localTerminalOption)

        dest.writeString(helpDeskNumber)
        dest.writeString(transAmountDigit)
        dest.writeString(settleAmtDigit)
        dest.writeString(option3)
        dest.writeString(option4)

    }

    private constructor(parcel: Parcel) : this() {
        pcNo = parcel.readString().toString()
        actionId = parcel.readString().toString()
        tableId = parcel.readString().toString()
        isActive = parcel.readString().toString()
        recordId = parcel.readString().toString()
        bankId = parcel.readString().toString()
        bankTid = parcel.readString().toString()
        dateTime = parcel.readString().toString()
        adminPassword = parcel.readString().toString()
        option1 = parcel.readString().toString()
        option2 = parcel.readString().toString()
        receiptL2 = parcel.readString().toString()
        receiptL3 = parcel.readString().toString()
        defaultMerchantName = parcel.readString().toString()
        localTerminalOption = parcel.readString().toString()

        helpDeskNumber = parcel.readString().toString()
        transAmountDigit = parcel.readString().toString()
        settleAmtDigit = parcel.readString().toString()
        option3 = parcel.readString().toString()
        option4 = parcel.readString().toString()

    }


    override fun describeContents(): Int = 0

    companion object {
        val TAG = HdfcTpt::class.java.simpleName

        @JvmField
        val CREATOR = object : Parcelable.Creator<HdfcTpt> {
            override fun createFromParcel(source: Parcel): HdfcTpt {
                return HdfcTpt(source)
            }

            override fun newArray(size: Int): Array<HdfcTpt> = Array(size) { HdfcTpt() }
        }

        fun performOperation(param: HdfcTpt, callback: () -> Unit) =
            withRealm {
                when (param.actionId) {
                    "1", "2" -> it.executeTransaction { i -> i.insertOrUpdate(param) }
                    "3" -> it.executeTransaction { i ->
                        i.where(HdfcTpt::class.java)
                            .equalTo("recordId", param.recordId).findAll()
                    }
                }
                callback()
            }

        fun selectAllHDFCTPTData(): List<HdfcTpt> = runBlocking {
            var result = listOf<HdfcTpt>()
            getRealm {
                result = it.copyFromRealm(
                    it.where(HdfcTpt::class.java)
                        .findAll()
                )
            }.await()
            result
        }

        fun delete(ost: HdfcTpt) {
            withRealm {
                it.executeTransaction { i ->
                    val result =
                        i.where(HdfcTpt::class.java)
                            .equalTo("bankId", ost.bankId)
                            .findFirst()
                    result?.deleteFromRealm()
                }
            }
        }

        fun clear() =
            withRealm {
                it.executeTransaction { i ->
                    i.delete(
                        OfflineSaleTable::class.java
                    )
                }
            }
    }

}


@RealmClass
open class HdfcCdt() : RealmObject(), Parcelable {

    @field:BHFieldParseIndex(0)
    var pcNo = ""

    @field:BHFieldParseIndex(1)
    var actionId = ""

    @field:BHFieldParseIndex(2)
    var tableId = ""

    @field:BHFieldParseIndex(3)
    var isActive = ""

    @field:BHFieldParseIndex(4)
    @PrimaryKey
    var recordId = ""

    @field:BHFieldParseIndex(5)
    var bankId = ""

    @field:BHFieldParseIndex(6)
    var bankTid = ""

    @field:BHFieldParseIndex(7)
    var cardRangeNumber = ""

    @field:BHFieldParseIndex(8)
    var panRangeLow = ""

    @field:BHFieldParseIndex(9)
    var panRangeHigh = ""

    @field:BHFieldParseIndex(10)
    var issuerNumber = ""

    @field:BHFieldParseIndex(11)
    var maxPanDigit = ""

    @field:BHFieldParseIndex(12)
    var minPanDigit = ""

    @field:BHFieldParseIndex(13)
    var floorLimit = ""

    @field:BHFieldParseIndex(14)
    var reauthMarginPercent = ""

    @field:BHFieldParseIndex(15)
    var defaultAccount = ""

    @field:BHFieldParseIndex(16)  // Bit oriented
    var option1 = ""

    @field:BHFieldParseIndex(17)
    var option2 = ""

    @field:BHFieldParseIndex(18)
    var option3 = ""

    @field:BHFieldParseIndex(19)
    var cardName = ""

    @field:BHFieldParseIndex(20)
    var cardLabel = ""

    @field:BHFieldParseIndex(21)
    var option4 = ""

    @field:BHFieldParseIndex(22)
    var issuerIndex = ""

    @field:BHFieldParseIndex(23)
    var issuerName = ""


    override fun writeToParcel(dest: Parcel, flags: Int) {

        dest.writeString(pcNo)
        dest.writeString(actionId)
        dest.writeString(tableId)
        dest.writeString(isActive)
        dest.writeString(recordId)
        dest.writeString(bankId)

        dest.writeString(bankTid)
        dest.writeString(cardRangeNumber)
        dest.writeString(panRangeLow)
        dest.writeString(panRangeHigh)
        dest.writeString(issuerNumber)

        dest.writeString(maxPanDigit)
        dest.writeString(minPanDigit)
        dest.writeString(floorLimit)
        dest.writeString(reauthMarginPercent)
        dest.writeString(defaultAccount)

        dest.writeString(option1)
        dest.writeString(option2)
        dest.writeString(option3)
        dest.writeString(cardName)
        dest.writeString(cardLabel)

        dest.writeString(option4)
        dest.writeString(issuerIndex)
        dest.writeString(issuerName)

    }

    override fun describeContents(): Int = 0

    private constructor(parcel: Parcel) : this() {

        pcNo = parcel.readString().toString()
        actionId = parcel.readString().toString()
        tableId = parcel.readString().toString()
        isActive = parcel.readString().toString()
        recordId = parcel.readString().toString()
        bankId = parcel.readString().toString()

        bankTid = parcel.readString().toString()
        cardRangeNumber = parcel.readString().toString()
        panRangeLow = parcel.readString().toString()
        panRangeHigh = parcel.readString().toString()
        issuerNumber = parcel.readString().toString()

        maxPanDigit = parcel.readString().toString()
        minPanDigit = parcel.readString().toString()
        floorLimit = parcel.readString().toString()
        reauthMarginPercent = parcel.readString().toString()
        defaultAccount = parcel.readString().toString()

        option1 = parcel.readString().toString()
        option2 = parcel.readString().toString()
        option3 = parcel.readString().toString()
        cardName = parcel.readString().toString()
        cardLabel = parcel.readString().toString()

        option4 = parcel.readString().toString()
        issuerIndex = parcel.readString().toString()
        issuerName = parcel.readString().toString()

    }

    companion object {

        val TAG = HdfcCdt::class.java.simpleName

        @JvmField
        val CREATOR = object : Parcelable.Creator<HdfcCdt> {
            override fun createFromParcel(source: Parcel): HdfcCdt =
                HdfcCdt(source)

            override fun newArray(size: Int): Array<HdfcCdt> = Array<HdfcCdt>(size) { HdfcCdt() }

        }

        fun performOperation(param: HdfcCdt, callback: () -> Unit) =
            withRealm {
                when (param.actionId) {
                    "1", "2" -> it.executeTransaction { i -> i.insertOrUpdate(param) }
                    "3" -> it.executeTransaction { i ->
                        i.where(HdfcCdt::class.java)
                            .equalTo("recordId", param.recordId).findAll()
                    }
                }
                callback()
            }

        fun selectAllHDFCCDTData(): List<HdfcCdt> = runBlocking {
            var result = listOf<HdfcCdt>()
            getRealm {
                result = it.copyFromRealm(
                    it.where(HdfcCdt::class.java)
                        .findAll()
                )
            }.await()
            result
        }

        fun delete(ost: HdfcCdt) {
            withRealm {
                it.executeTransaction { i ->
                    val result =
                        i.where(HdfcCdt::class.java)
                            .equalTo("bankId", ost.bankId)
                            .findFirst()
                    result?.deleteFromRealm()
                }
            }
        }

        fun clear() =
            withRealm {
                it.executeTransaction { i ->
                    i.delete(
                        HdfcCdt::class.java
                    )
                }
            }

    }

}
//endregion






