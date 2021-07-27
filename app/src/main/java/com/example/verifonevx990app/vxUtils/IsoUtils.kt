package com.example.verifonevx990app.vxUtils

const val MAX_AMOUNT: Long = 99999999  // in paisa
const val MIN_AMOUNT: Long = 100       // in paisa

enum class ESaleType(val posEntryValue: Int) {
    //insert with pin
    EMV_POS_ENTRY_PIN(553),
    EMV_POS_ENTRY_NO_PIN(552),

    //off line pin
    EMV_POS_ENTRY_OFFLINE_PIN(554),

    //used for fall back
    EMV_POS_ENTRY_FALL_MAGPIN(623),
    EMV_POS_ENTRY_FALL_MAGNOPIN(620),
    EMV_POS_ENTRY_FALL_4DBCPIN(663),
    EMV_POS_ENTRY_FALL_4DBCNOPIN(660),

    ///swipe with cvv and pin
    POS_ENTRY_SWIPED_4DBC(563),

    //swipe with out cvv  with out pin
    POS_ENTRY_SWIPED_NO4DBC(523),

    //swipe pin with out cvv
    POS_ENTRY_SWIPED_NO4DBC_PIN(524),

    //Manual with cvv
    POS_ENTRY_MANUAL_4DBC(573),

    //Manual without cvv
    POS_ENTRY_MANUAL_NO4DBC(513),

    //contact less swipe data with out pin
    CTLS_MSD_POS_ENTRY_CODE(921),

    //contact less with  swipe data and pin
    CTLS_MSD_POS_WITH_PIN(923),

    // contact less  insert data with out pin
    CTLS_EMV_POS_ENTRY_CODE(911),

    // contact less insert data with pin
    CTLS_EMV_POS_WITH_PIN(913)

}

fun getEmvTransactionType(transactionType: TransactionType): Int = when (transactionType) {
    TransactionType.SALE, TransactionType.EMI_SALE -> 0x00
    else -> 0x00
}

fun getTransactionType(uiAction: UiAction): TransactionType {
    return when (uiAction) {
        UiAction.START_SALE -> TransactionType.SALE
        UiAction.INIT -> TransactionType.INIT
        UiAction.KEY_EXCHANGE -> TransactionType.KEY_EXCHANGE
        UiAction.PRE_AUTH -> TransactionType.PRE_AUTH
        UiAction.REFUND -> TransactionType.REFUND
        UiAction.BANK_EMI -> TransactionType.EMI_SALE
        else -> TransactionType.NONE
    }
}

enum class TransactionType(
    val type: Int,
    val processingCode: ProcessingCode = ProcessingCode.NONE,
    val txnTitle: String = "Not Defined",
    val settlementSequence: Int = 0
) {
    //   sale, emi sale ,sale with cash, cash only,auth comp,and tip transaction type will be included.

    NONE(0),
    KEY_EXCHANGE(1, ProcessingCode.KEY_EXCHANGE),
    INIT(2, ProcessingCode.INIT),
    LOGON(3, ProcessingCode.KEY_EXCHANGE),
    SALE(4, ProcessingCode.SALE, "SALE", 1),
    VOID(5, ProcessingCode.VOID, "VOID"),
    SETTLEMENT(6, ProcessingCode.SETTLEMENT),
    APP_UPDATE(7),
    BALANCE_ENQUIRY(8),
    REFUND(9, ProcessingCode.REFUND, "REFUND"),
    VOID_REFUND(10, ProcessingCode.VOID_REFUND, "VOID OF REFUND"),
    SALE_WITH_CASH(11, ProcessingCode.SALE_WITH_CASH, "SALE-CASH", 3),
    CASH(12),
    BATCH_UPLOAD(13),
    PRE_AUTH(14, ProcessingCode.PRE_AUTH, "PRE-AUTH"),

    SALE_WITH_TIP(15),
    ADJUSTMENT(16),
    REVERSAL(17, ProcessingCode.REFUND),
    EMI_SALE(18, ProcessingCode.SALE, "EMI SALE", 2),
    EMI(19),
    SALE_COMPLETION(20),
    TIP_ADJUSTMENT(21),

    OFF_SALE(22),
    CASH_AT_POS(23, ProcessingCode.CASH_AT_POS, "CASH ONLY", 4),
    BATCH_SETTLEMENT(24),

    PRE_AUTH_COMPLETE(25, ProcessingCode.PRE_SALE_COMPLETE, "AUTH-COMP", 5),
    VOID_PREAUTH(26, ProcessingCode.VOID_PREAUTH, "VOID PRE-AUTH"),

    TIP_SALE(27, ProcessingCode.TIP_SALE, "TIP ADJUST", 6),
    PENDING_PREAUTH(28, ProcessingCode.PENDING_PREAUTH, "PRE AUTH TXN"),
    OFFLINE_SALE(29, ProcessingCode.OFFLINE_SALE, "OFFLINE SALE"),
    VOID_OFFLINE_SALE(30, ProcessingCode.VOID_OFFLINE_SALE, "VOID OFFLINE SALE"),
    EMI_MASTER_DATA(31, ProcessingCode.BANK_EMI, "BRAND EMI"),
    EMI_ENQUIRY(32, ProcessingCode.BANK_EMI, "EMI ENQUIRY"),
    BRAND_EMI(33, ProcessingCode.BRAND_EMI, "EMI SALE"),
    TEST_EMI(34, ProcessingCode.BRAND_EMI, "TEST EMI"),
    BRAND_EMI_BY_ACCESS_CODE(35, ProcessingCode.BRAND_EMI, "EMI SALE"),
    FLEXI_PAY(36, ProcessingCode.FLEXI_PAY, "FLEXI PAY"),
}


enum class Mti(val mti: String) {
    MTI_INIT("0800"),
    MTI_LOGON("0800"),
    PRE_AUTH_MTI("0100"),
    PRE_AUTH_COMPLETE_MTI("0220"),  //also used in tipsale
    SETTLEMENT_MTI("0500"),
    DEFAULT_MTI("0200"),
    REVERSAL("0400"),
    APP_UPDATE_MTI("0800"),
    CROSS_SELL_MTI("0800"),
    BANK_EMI("0800"),
    EIGHT_HUNDRED_MTI("0800"),
    BANKI_EMI("0800"),
}

enum class ProcessingCode(val code: String) {
    NONE("-1"),
    KEY_EXCHANGE("960300"),
    KEY_EXCHANGE_RESPONSE("960301"),
    INIT("960200"),
    INIT_MORE("960201"),
    APP_UPDATE("960100"),
    APP_UPDATE_CONTINUE("960101"),
    APP_UPDATE_CONFIRMATION("960111"),
    SALE("920001"),

    VOID("940001"),
    REFUND("941001"),
    PRE_SALE_COMPLETE("925001"),//or ---> PRE Auth Complete
    SETTLEMENT("970001"),
    FORCE_SETTLEMENT("970002"),
    ZERO_SETTLEMENT("970003"),
    VOID_REFUND("942001"),
    PENDING_PREAUTH("931000"),
    VOID_PREAUTH("940000"),

    CHARGE_SLIP_START("960210"),
    CHARGE_SLIP_CONTINUE("960211"),
    CHARGE_SLIP_HEADER_FOOTER("960209"),
    GCC("920099"),

    OFFLINE_SALE("920011"),
    VOID_OFFLINE_SALE("940011"),
    TIP_SALE("924001"),
    CASH_AT_POS("923001"),
    SALE_WITH_CASH("922001"),
    PRE_AUTH("920000"),
    CROSS_SELL("982001"),
    BANK_EMI("982002"),
    EMI_ENQUIRY("982002"),
    BRAND_EMI("982002"),

    // Merchant Promo......
    INITIALIZE_PROMOTION("980100"),
    ADD_CUSTOMER("980200"),
    REDEEM_PROMO_INITIATE_OTP("980300"),
    REDEEM_PROMO_WITH_OTP("980400"),
    REDEEM_PROMO_WITHOUT_OTP("980500"),
    SEND_PROMO("980600"),
    ON_PAYMENT_PROMO("980600"),
    PROMO_REPORT("980700"),
    FLEXI_PAY("982002"),


}

enum class Nii(val nii: String) {
    DEFAULT("0091"),
    SOURCE("0001"),
    SMS_PAY("0411"),
    HDFC_DEFAULT("0002"),
    BANK_EMI("0028"),
    BRAND_EMI_MASTER("0028")
}

enum class ConnectionType(val code: String) {
    PSTN("1"), ETHERNET("2"), GPRS("3")
}

enum class ECardSaleType(val posEntryValue: Int) {
    EMV_POS_ENTRY_PIN(553),
    EMV_POS_ENTRY_NO_PIN(552),
    EMV_POS_ENTRY_OFFLINE_PIN(554),
    EMV_POS_ENTRY_FALL_MAGPIN(623),
    EMV_POS_ENTRY_FALL_MAGNOPIN(620),
    EMV_POS_ENTRY_FALL_4DBCPIN(663),
    EMV_POS_ENTRY_FALL_4DBCNOPIN(660),
    POS_ENTRY_SWIPED_4DBC(563),
    POS_ENTRY_SWIPED_NO4DBC(523),
    POS_ENTRY_SWIPED_NO4DBC_PIN(524),
    POS_ENTRY_MANUAL_4DBC(573),
    POS_ENTRY_MANUAL_NO4DBC(513),
    CTLS_MSD_POS_ENTRY_CODE(921),
    CTLS_MSD_POS_WITH_PIN(923),
    CTLS_EMV_POS_ENTRY_CODE(911),
    CTLS_EMV_POS_WITH_PIN(913),

    OFFLINE_SALE(513)

}

val Field55 = arrayOf(
    "9F26",
    "9F10",
    "9F37",
    "9F36",
    "95",
    "9A",
    "9C",
    "9F02",
    "5F2A",
    "9F1A",
    "82",
    "5F34",
    "9F27",
    "9F33",
    "9F34",
    "9F35",
    "9F03",
    "9F47"
)