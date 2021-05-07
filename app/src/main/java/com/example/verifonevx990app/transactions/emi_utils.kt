package com.example.verifonevx990app.transactions

import android.os.Parcel
import android.os.Parcelable
import android.view.View
import com.example.verifonevx990app.realmtables.*
import com.example.verifonevx990app.vxUtils.getCurrentDate


interface IBenefitTable {
    fun getBenifitTable(schemeId: String, tenure: String): BenifitSlabTable?

    /*  fun getEmiSchemeTable(schemeId: String): ArrayList<EmiSchemeTable>*/
    fun getEmiSchemeTable(schemeId: String): EmiSchemeTable?
}

abstract class OnDoubleTapListener : View.OnClickListener {

    companion object {
        private const val DELTA_DOUBLE_TIME = 200
    }

    var lastTime = 0L
    override fun onClick(p0: View?) {
        val clickTime = System.currentTimeMillis()
        if (clickTime - lastTime < DELTA_DOUBLE_TIME) {
            doubleTap(p0)
            lastTime = 0
        } else {
            singleTap(p0)
        }
        lastTime = clickTime
    }

    abstract fun singleTap(p0: View?)
    abstract fun doubleTap(p0: View?)
}

class IssuerDataModel {
    var actionId: String = ""
    var tableId: String = ""
    var isActive: String = ""
    var issuerId: String = ""

    var issuerName: String = ""

    var schemeDataModel = ArrayList<SchemeDataModel>()


    fun set(issuerParam: IssuerParameterTable) {
        issuerName = issuerParam.issuerName
        issuerId = issuerParam.issuerId
    }

}

enum class EmiViewType { SCHEME, TENURE }

open class EmiView(val type: EmiViewType)


class SchemeDataModel() : Parcelable, EmiView(EmiViewType.SCHEME) {
    var tenureDataModel = Array(0) { TenureDataModel() }

    var actionId: String = ""
    var tableId: String = ""
    var isActive: String = ""

    var schemeId: String = ""
    var issuerId: String = ""
    var schemeName: String = ""
    var isContainerVisible: Boolean = true

    private constructor(parcel: Parcel) : this() {

        tenureDataModel =
            parcel.readParcelableArray(TenureDataModel::class.java.classLoader) as Array<TenureDataModel>

        actionId = parcel.readString().toString()
        tableId = parcel.readString().toString()
        isActive = parcel.readString().toString()
        schemeId = parcel.readString().toString()
        issuerId = parcel.readString().toString()
        schemeName = parcel.readString().toString()
        isContainerVisible = parcel.readByte() != 0.toByte()
    }

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeParcelableArray(tenureDataModel, 0)
        parcel.writeString(actionId)
        parcel.writeString(tableId)
        parcel.writeString(isActive)
        parcel.writeString(schemeId)
        parcel.writeString(issuerId)
        parcel.writeString(schemeName)
        parcel.writeByte(if (isContainerVisible) 1 else 0)
    }

    override fun describeContents(): Int {
        return 0
    }

    companion object CREATOR : Parcelable.Creator<SchemeDataModel> {
        override fun createFromParcel(parcel: Parcel): SchemeDataModel {
            return SchemeDataModel(parcel)
        }

        override fun newArray(size: Int): Array<SchemeDataModel?> {
            return Array(size) { SchemeDataModel() }
        }
    }

    fun set(schemeTable: SchemeTable) {
        actionId = schemeTable.actionId
        isActive = schemeTable.isActive
        issuerId = schemeTable.issuerId
        schemeId = schemeTable.schemeId
        schemeName = schemeTable.schemeName
        tableId = schemeTable.tableId
    }

}


class TenureDataModel() : EmiView(EmiViewType.TENURE), Parcelable, Comparable<TenureDataModel> {

    override fun compareTo(other: TenureDataModel): Int = try {
        when {
            other.tenure.toInt() > tenure.toInt() -> -1
            other.tenure.toInt() < tenure.toInt() -> 1
            else -> 0
        }
    } catch (ex: Exception) {
        0
    }

    var actionId: String = ""
    var tableId: String = ""
    var isActive: String = ""

    var emiTenureId: String = ""
    var schemeId: String = ""
    var tenure: String = ""
    var roi: String = ""
    var proccesingFee: String = ""
    var isChecked: Boolean = false
    var emiBinValue = ""
    var emiSchemeId = ""
    var effecativeRate = ""
    var processingRate = ""
    var emiAmount: EmiAmounts? = null

    private constructor(parcel: Parcel) : this() {
        actionId = parcel.readString().toString()
        tableId = parcel.readString().toString()
        isActive = parcel.readString().toString()
        emiTenureId = parcel.readString().toString()
        schemeId = parcel.readString().toString()
        tenure = parcel.readString().toString()
        roi = parcel.readString().toString()
        proccesingFee = parcel.readString().toString()
        emiBinValue = parcel.readString().toString()
        effecativeRate = parcel.readString().toString()
        processingRate = parcel.readString().toString()
        isChecked = parcel.readByte() != 0.toByte()
    }

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeString(actionId)
        parcel.writeString(tableId)
        parcel.writeString(isActive)
        parcel.writeString(emiTenureId)
        parcel.writeString(schemeId)
        parcel.writeString(tenure)
        parcel.writeString(roi)
        parcel.writeString(proccesingFee)
        parcel.writeString(emiBinValue)
        parcel.writeString(effecativeRate)
        parcel.writeString(processingRate)
        parcel.writeByte(if (isChecked) 1 else 0)
    }

    override fun describeContents(): Int {
        return 0
    }

    companion object CREATOR : Parcelable.Creator<TenureDataModel> {
        override fun createFromParcel(parcel: Parcel): TenureDataModel {
            return TenureDataModel(parcel)
        }

        override fun newArray(size: Int): Array<TenureDataModel?> {
            return Array(size) { TenureDataModel() }
        }
    }

    fun set(tenureTable: TenureTable) {
        actionId = tenureTable.actionId
        tableId = tenureTable.tableId
        isActive = tenureTable.isActive

        emiTenureId = tenureTable.emiTenureId
        schemeId = tenureTable.schemeId
        tenure = tenureTable.tenure

        roi = tenureTable.roi
        proccesingFee = tenureTable.proccesingFee
        effecativeRate = tenureTable.effecativeRate
        processingRate = tenureTable.processingRate
    }
}


enum class EAccountType(val code: String) {
    DEFAULT("00"), SAVING("10"), CREDIT("30"), CHEQUE("20"), UNIVERSAL(
        "40"
    )
}

open class EmiCustomerDetails() : EmiTransactionDetail() {

    var accountType = EAccountType.DEFAULT.code
    var merchantBillNo = ""
    var serialNo = ""
    var customerName = ""
    var phoneNo = ""
    var email = ""


    protected constructor(parcel: Parcel) : this() {
        readParcel(parcel)
    }

    override fun readParcel(parcel: Parcel) {
        super.readParcel(parcel)
        accountType = parcel.readString().toString()
        merchantBillNo = parcel.readString().toString()
        serialNo = parcel.readString().toString()
        customerName = parcel.readString().toString()
        phoneNo = parcel.readString().toString()
        email = parcel.readString().toString()
    }

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        super.writeToParcel(parcel, flags)
        parcel.writeString(accountType)
        parcel.writeString(merchantBillNo)
        parcel.writeString(serialNo)
        parcel.writeString(customerName)
        parcel.writeString(phoneNo)
        parcel.writeString(email)
    }

    companion object CREATOR : Parcelable.Creator<EmiCustomerDetails> {
        override fun createFromParcel(parcel: Parcel): EmiCustomerDetails {
            return EmiCustomerDetails(parcel)
        }

        override fun newArray(size: Int): Array<EmiCustomerDetails?> {
            return Array(size) { EmiCustomerDetails() }
        }
    }


}

open class EmiTransactionDetail() : EmiBrandDetail() {


    var emiBin = ""
    var issuerId = ""
    var emiSchemeId = ""
    var transactionAmt = ""
    var cashDiscountAmt = ""
    var loanAmt = ""
    var tenure = ""
    var roi = ""
    var monthlyEmi = ""
    var cashback = ""
    var netPay = ""
    var processingFee = ""
    var totalInterest = ""
    var cashBackPercent = ""
    var isCashBackInPercent = false

    protected constructor(parcel: Parcel) : this() {
        readParcel(parcel)
    }

    override fun writeToParcel(parcel: Parcel, flags: Int) {
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
        parcel.writeString(processingFee)
        parcel.writeString(totalInterest)
        parcel.writeString(cashBackPercent)
        parcel.writeByte(if (isCashBackInPercent) 1 else 0)

    }

    override fun readParcel(parcel: Parcel) {
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
        processingFee = parcel.readString().toString()
        totalInterest = parcel.readString().toString()
        cashBackPercent = parcel.readString().toString()
        isCashBackInPercent = parcel.readByte() != 0.toByte()
    }

    override fun describeContents(): Int {
        return 0
    }

    companion object CREATOR : Parcelable.Creator<EmiTransactionDetail> {
        override fun createFromParcel(parcel: Parcel): EmiTransactionDetail {
            return EmiTransactionDetail(parcel)
        }

        override fun newArray(size: Int): Array<EmiTransactionDetail?> {
            return Array(size) { EmiTransactionDetail() }
        }
    }
}

open class EmiBrandDetail() : Parcelable {
    var brandId = "01"
    var productId = "0"

    protected constructor(parcel: Parcel) : this() {
        readParcel(parcel)
    }

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeString(brandId)
        parcel.writeString(productId)
    }

    open fun readParcel(parcel: Parcel) {
        brandId = parcel.readString().toString()
        productId = parcel.readString().toString()
    }

    override fun describeContents(): Int {
        return 0
    }

    companion object CREATOR : Parcelable.Creator<EmiBrandDetail> {
        override fun createFromParcel(parcel: Parcel): EmiBrandDetail {
            return EmiBrandDetail(parcel)
        }

        override fun newArray(size: Int): Array<EmiBrandDetail?> {
            return Array(size) { EmiBrandDetail() }
        }
    }


}

fun flatEmi(amount: Long, rate: Float, nom: Int): Float =
    (amount.toFloat() / (1200 * nom)) * (1200 + (rate * nom))

fun reducingBalanceEmi(amount: Float, rate: Float, nom: Int): Float {
    val periodInterest = rate.toDouble() / 1200
    val p1 = amount.toDouble() * periodInterest
    val f = Math.pow((1 + periodInterest), nom.toDouble())
    val p2 = f
    val p3 = f - 1
    var total = p1 * p2 / p3
    return total.toFloat()
    //  return p1 * p2 / p3
}


fun attachEmiAmountWithValidation(
    scheme: SchemeDataModel,
    tenureDataModel: TenureDataModel,
    amount: Float,
    benefitGetter: IBenefitTable?
) {
    // val emiSchemes = benefitGetter?.getEmiSchemeTable(scheme.schemeId)
    //  val emiSchemess = emiSchemes?.forEach{
    //           emiScheme ->
    val emiScheme = benefitGetter?.getEmiSchemeTable(scheme.schemeId)
    if (emiScheme != null) {
        //YYMMDD
        emiScheme.startDate
        emiScheme.endDate
        tenureDataModel.emiSchemeId = emiScheme.emiSchemeId
        var benefitSlabTable: BenifitSlabTable? = null

        val getCalculation: (str: String) -> EBenefitCalculation = {
            benefitSlabTable =
                benefitGetter.getBenifitTable(emiScheme.emiSchemeId, tenureDataModel.tenure)
            when (it) {
                "7302" -> EBenefitCalculation.FIXED_VALUE
                "7303" -> EBenefitCalculation.PERCENTAGE_VALUE
                "7301" -> EBenefitCalculation.TOTAL_CALCULATED_INTEREST
                else -> {
                    if (benefitSlabTable?.ruleId == "7401") {
                        //for fixed value 7401
                        EBenefitCalculation.FIXED_VALUE
                    } else {
                        //7402 for Percentage
                        EBenefitCalculation.PERCENTAGE_VALUE
                    }
                }
            }
        }

        val getCalcValue: (str: String) -> Float = {
            when (it) {
                "7304" -> {
                    benefitSlabTable =
                        benefitGetter.getBenifitTable(emiScheme.emiSchemeId, tenureDataModel.tenure)
                    //   if(null !=benefitSlabTable)
                    if (amount <= (benefitSlabTable?.maxValue?.toFloat()
                            ?: 0f) / 100 && amount >= (benefitSlabTable?.minValue?.toFloat()
                            ?: 0f) / 100
                    ) {
                        when (benefitSlabTable?.ruleId) {
                            "7401" -> benefitSlabTable?.fixedValue?.toFloat() ?: 0f
                            else -> benefitSlabTable?.percentageValue?.toFloat() ?: 0f
                        }
                    } else 0f
                }
                "7302" -> emiScheme.fixedValue.toFloat()
                else -> emiScheme.percantageValue.toFloat()
            }
        }

        /*val getCalcValue: (str: String) -> Float = {
            when (it) {
                "7304" -> {
                var   benefitSlabTables = benefitGetter.getBenifitTable(emiScheme.emiSchemeId, tenureDataModel.tenure)
               var      benefitSlabTable = benefitSlabTables.forEach {
                   it
               }

                    if (amount < (benefitSlabTable?.maxValue?.toFloat()
                            ?: 0f) / 100 && amount > (benefitSlabTable?.minValue?.toFloat()
                            ?: 0f) / 100) {
                        when (it) {
                            "7401" -> benefitSlabTable?.fixedValue?.toFloat() ?: 0f
                            else -> benefitSlabTable?.percentageValue?.toFloat() ?: 0f
                        }
                    } else 0f
                }
                "7302" -> emiScheme.fixedValue.toFloat()
                else -> emiScheme.percantageValue.toFloat()
            }
        }*/

        if (amount <= emiScheme.maxValue.toDouble() / 100 && amount >= emiScheme.minValue.toDouble() / 100) {
            val roi = tenureDataModel.roi.toFloat() / 100
            val currDt = getCurrentDate()
            if (currDt.compareTo(emiScheme.startDate) >= 0 && currDt.compareTo(emiScheme.endDate) <= 0) {
                val emiAmount = when (emiScheme.benifitModelId) {
                    "7202" -> {
                        EmiAmounts(
                            amount,
                            roi,
                            tenureDataModel.tenure.toInt(),
                            EBenefitModel.CASH_BACK,
                            emiScheme.maxAmount.toFloat()
                        ).apply {
                            // Cash back
                            addCashBack(
                                getCalculation(emiScheme.benifitCalculationRefundId),
                                getCalcValue(emiScheme.benifitCalculationRefundId),
                                tenureDataModel
                            )
                        }
                    }
                    "7203" -> {
                        EmiAmounts(
                            amount,
                            roi,
                            tenureDataModel.tenure.toInt(),
                            EBenefitModel.DISCOUNT,
                            emiScheme.maxAmount.toFloat()
                        ).apply {
                            // Discount
                            addDiscount(
                                getCalculation(emiScheme.benifitCalculationRefundId),
                                getCalcValue(emiScheme.benifitCalculationRefundId),
                                tenureDataModel,
                                emiScheme
                            )
                        }
                    }
                    //CIB 7201
                    else -> EmiAmounts(
                        amount,
                        roi,
                        tenureDataModel.tenure.toInt(),
                        maxAmount = emiScheme.maxAmount.toFloat()
                    )  // Customer total interest bareable

                }

                tenureDataModel.emiAmount = emiAmount
            } else {
                //println("Expired data ")
            }
        }
    }
    //  }
}


enum class EBenefitCalculation(val type: Byte) {
    TOTAL_CALCULATED_INTEREST(1), FIXED_VALUE(2), PERCENTAGE_VALUE(
        3
    )
}

enum class EBenefitModel(val type: Byte) { CUSTOMER_INTEREST_BEAR(1), CASH_BACK(2), DISCOUNT(3) }

class EmiAmounts(
    val principleAmt: Float,
    val roi: Float = 0f,
    noOfMonth: Int = 0,
    val benefitModel: EBenefitModel = EBenefitModel.CUSTOMER_INTEREST_BEAR,
    private val maxAmount: Float = 0f
) {
    var monthlyEmi = 0f
    var totalInterest = 0f
    var cashBack = 0f
    var cashBackpercent = 0f
    var discount = 0f
    var discountpercent = 0f

    var benefitCalc = EBenefitCalculation.PERCENTAGE_VALUE

    init {
        monthlyEmi = reducingBalanceEmi(principleAmt, roi, noOfMonth).round()
        totalInterest = monthlyEmi * noOfMonth - principleAmt
    }

    var isTotalcalculatednterest = false
    fun addDiscount(
        type: EBenefitCalculation,
        value: Float = 0f,
        tenureDataModel: TenureDataModel,
        emiScheme: EmiSchemeTable
    ) {
        if (benefitModel == EBenefitModel.DISCOUNT) {
            benefitCalc = type
            discount = when (type) {
                EBenefitCalculation.PERCENTAGE_VALUE -> {
                    discountpercent = (value / 100)
                    principleAmt * (value / 100) / 100
                }

                EBenefitCalculation.FIXED_VALUE -> value / 100

                EBenefitCalculation.TOTAL_CALCULATED_INTEREST -> {
                    isTotalcalculatednterest = true
                    totalInterest
                    (principleAmt * (tenureDataModel.effecativeRate.toFloat() / 100)) / 100
                }
            }
            //discount is Multiplation of transactionAmt * effectiveRate
            if (isTotalcalculatednterest) {
                isTotalcalculatednterest = false
                discount = (principleAmt * (tenureDataModel.effecativeRate.toFloat() / 100)) / 100
            } else {
                isTotalcalculatednterest = false
                discount = checkWithMaxAmtDiscount(discount, tenureDataModel)
            }

        }
    }

    private fun checkWithMaxAmtDiscount(amt: Float, tenureDataModel: TenureDataModel): Float =
        if (amt <= maxAmount / 100) {
            amt
            recaclulateEmi(amt, tenureDataModel)
        } else {
            maxAmount / 100
            recaclulateEmi(maxAmount / 100, tenureDataModel)
        }

    private fun recaclulateEmi(value: Float, tenureDataModel: TenureDataModel): Float {
        monthlyEmi =
            reducingBalanceEmi(principleAmt - value, roi, tenureDataModel.tenure.toInt()).round()
        totalInterest = monthlyEmi * tenureDataModel.tenure.toInt() - (principleAmt - value)

        return value
    }


    private fun checkWithMaxAmtCashBack(amt: Float): Float = if (amt <= maxAmount / 100) {
        amt
    } else {
        maxAmount / 100
    }

    var isTotalcalculatednterestcash = false
    fun addCashBack(
        type: EBenefitCalculation,
        value: Float = 0f,
        tenureDataModel: TenureDataModel
    ) {
        if (benefitModel == EBenefitModel.CASH_BACK) {
            benefitCalc = type
            cashBack = when (type) {
                EBenefitCalculation.PERCENTAGE_VALUE -> {
                    cashBackpercent = (value / 100)
                    principleAmt * (value / 100) / 100
                }

                EBenefitCalculation.FIXED_VALUE -> value / 100

                EBenefitCalculation.TOTAL_CALCULATED_INTEREST -> {
                    isTotalcalculatednterestcash = true
                    totalInterest
                    (principleAmt * (tenureDataModel.effecativeRate.toFloat() / 100)) / 100
                }


            }
            //cashBack is Multiplation of transactionAmt * effectiveRate
            if (isTotalcalculatednterestcash) {
                isTotalcalculatednterestcash = false
                cashBack = (principleAmt * (tenureDataModel.effecativeRate.toFloat() / 100)) / 100
            } else {
                isTotalcalculatednterestcash = false
                cashBack = checkWithMaxAmtCashBack(cashBack)
            }
        }
    }

    fun getMonthEmi(): String = "%d".format((monthlyEmi * 100).toInt())
    fun getRoi(): String = "%d".format((roi * 100).toInt())
    fun getCashback(): String = if (cashBack > 0) {
        "%d".format((cashBack * 100).toInt())
    } else ""

    fun getDiscount(): String = if (discount > 0) {
        "%d".format((discount * 100).toInt())
    } else ""

    fun getTransactionAmt(): String = "%d".format((principleAmt * 100).toInt())


}

fun Float.toFormatString(): String = "%.2f".format(this.round())


fun Float.round(): Float {
    var f = kotlin.math.round(this * 100)
    f /= 100
    return f
}

fun Double.toFormatString(): String = this.toFloat().toFormatString()