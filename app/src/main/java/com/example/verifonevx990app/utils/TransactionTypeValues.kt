package com.example.verifonevx990app.utils


class TransactionTypeValues {
    class TotalTrans {
        var transName: String = ""
        var count: Int = 0
        var amount: String = ""
    }

    enum class TransactionType {
        KEY_EXCHANGE,
        INIT,
        LOGON,
        SALE,
        VOID,
        SETTLEMENT,
        APP_UPDATE,
        BALANCE_ENQUIRY,
        REFUND,
        VOID_REFUND,
        SALE_WITH_CASH,
        CASH,
        BATCH_UPLOAD,
        PRE_AUTH,
        SALE_WITH_TIP,
        ADJUSTMENT,
        REVERSAL,
        EMI_SALE,
        EMI,
        SALE_COMPLETION,
        TIP_ADJUSTMENT,
        OFF_SALE,
        CASH_AT_POS,
        BATCH_SETTELEMENT,
        PRE_AUTH_COMPLETE
    }

    enum class SALETYPE {
        EMV_POS_ENTRY_PIN,
        EMV_POS_ENTRY_NO_PIN,
        EMV_POS_ENTRY_OFFLINE_PIN,
        EMV_POS_ENTRY_FALL_MAGPIN,
        EMV_POS_ENTRY_FALL_MAGNOPIN,
        EMV_POS_ENTRY_FALL_4DBCPIN,
        EMV_POS_ENTRY_FALL_4DBCNOPIN,
        POS_ENTRY_SWIPED_4DBC,
        POS_ENTRY_SWIPED_NO4DBC,
        POS_ENTRY_SWIPED_NO4DBC_PIN,
        POS_ENTRY_MANUAL_4DBC,
        POS_ENTRY_MANUAL_NO4DBC,
        CTLS_MSD_POS_ENTRY_CODE,
        CTLS_MSD_POS_WITH_PIN,
        CTLS_EMV_POS_ENTRY_CODE,
        CTLS_EMV_POS_WITH_PIN
    }

    enum class CARD_RAEDING_TYPE {
        IS_EMV,
        IS_MAG,
        IS_FALL_BACK

    }

    companion object {
        //processing code
        const val INIT_Proc_Code = "960200"
        val INIT_Res_Proc_Code = "960201"
        const val Appl_Update_Proc_Code = "960100"
        val Appl_Update_Res_Proc_Code = "960101"
        const val PreSale_Proc_Code = "920000"
        val PreSale_OTP_Proc_Code = "910011"
        const val Sale_Proc_Code = "920001"
        const val SALE_WITH_CASH_PROC = "922001"
        val Sale_QR_Proc_Code = "920002"
        val SalePIN_Proc_Code = "920002"
        val SaleToken_Proc_Code = "920004"
        val SaleCard_Proc_Code = "920005"
        val Presale_Push_Proc_Code = "920006"
        val Salepush_Proc_Code = "920007"
        val Settle_Proc_Code = "970001"
        const val Settle_Force_Proc_Code = "970002"
        const val Zero_Settlement = "970003"
        val Settle_AftBU_Proc_Code = "970000"
        val Prebalan_Proc_Code = "930000"
        val Prebalan_OTP_Proc_Code = "930100"
        val Balan_Proc_Code = "930001"
        val PreREload_Proc_Code = "950000"
        val PreREload_OTP_Proc_Code = "950100"
        val REload_Proc_Code = "950001"
        val KEY_EXCH_Proc_Code = "960300"
        val KEY_EXCH_Res_Proc_Code = "960301"
        val PKDK_EXCH_Proc_Code = "960301"
        val PKDK_EXCH_RES_Proc_Code = "960302"
        val PKDK_EXCH_PCode_after_Sett = "960400"
        val PreVoid_Proc_Code = "940000"
        val Void_Proc_Code = "940001"
        const val CASH_AT_POS_PROC = "923001"

        //MTI
        const val Setle_Req_MTI = "0500"
        const val Setle_Res_MTI = "0510"
        const val Rev_Res_MTI = "0400"
        const val Reload_Req_MTI = "0260"
        const val Reload_Res_MTI = "0270"
        const val DEFAULT_MTI = "0200"
        const val PRE_AUTH_MTI = "0100"
        const val AUTH_COMPLETE_MTI = "0220"

        const val REFUND_PROCESSING_CODE = "941001"

        const val PRE_AUTH_PROCESSING_CODE = PreSale_Proc_Code
        const val AUTH_COMPLETE_PROCESSING_CODE = "925001"


        var cardReadingType: CARD_RAEDING_TYPE? = CARD_RAEDING_TYPE.IS_EMV

        //insert with pin
        val EMV_POS_ENTRY_PIN =
            553   //Test Number          1.1.1      MSG-001 Chip Sale, Bit 22 Validation
        val EMV_POS_ENTRY_NO_PIN = 552

        //off line pin
        val EMV_POS_ENTRY_OFFLINE_PIN = 554

        //used for fall back
        val EMV_POS_ENTRY_FALL_MAGPIN = 623
        val EMV_POS_ENTRY_FALL_MAGNOPIN = 620
        val EMV_POS_ENTRY_FALL_4DBCPIN = 663
        val EMV_POS_ENTRY_FALL_4DBCNOPIN = 660


        ///swipe with cvv and pin
        val POS_ENTRY_SWIPED_4DBC = 563

        //swipe with out cvv  with out pin
        val POS_ENTRY_SWIPED_NO4DBC = 523

        //swipe pin with out cvv
        val POS_ENTRY_SWIPED_NO4DBC_PIN = 524

        //Manual with cvv
        val POS_ENTRY_MANUAL_4DBC = 573

        //manual with out cvv
        val POS_ENTRY_MANUAL_NO4DBC = 513

        //contact less swipe data with out pin
        val CTLS_MSD_POS_ENTRY_CODE = 921

        //contact less with  swipe data and pin
        val CTLS_MSD_POS_WITH_PIN = 923

        // contact less  insert data with out pin
        val CTLS_EMV_POS_ENTRY_CODE = 911

        // contact less insert data with pin
        val CTLS_EMV_POS_WITH_PIN = 913

        var saleType: SALETYPE? = null

        fun getPosValue(): Int {
            if (saleType == null) {
                return 0
            }
            when (saleType) {
                SALETYPE.EMV_POS_ENTRY_PIN -> {
                    return EMV_POS_ENTRY_PIN
                }

                SALETYPE.EMV_POS_ENTRY_NO_PIN -> {
                    return EMV_POS_ENTRY_NO_PIN
                }
                SALETYPE.EMV_POS_ENTRY_OFFLINE_PIN -> {
                    return EMV_POS_ENTRY_OFFLINE_PIN
                }
                SALETYPE.EMV_POS_ENTRY_FALL_MAGPIN -> {
                    return EMV_POS_ENTRY_FALL_MAGPIN
                }
                SALETYPE.EMV_POS_ENTRY_FALL_MAGNOPIN -> {
                    return EMV_POS_ENTRY_FALL_MAGNOPIN
                }
                SALETYPE.EMV_POS_ENTRY_FALL_4DBCPIN -> {
                    return EMV_POS_ENTRY_FALL_4DBCPIN
                }
                SALETYPE.EMV_POS_ENTRY_FALL_4DBCNOPIN -> {
                    return EMV_POS_ENTRY_FALL_4DBCNOPIN
                }
                SALETYPE.POS_ENTRY_SWIPED_4DBC -> {
                    return POS_ENTRY_SWIPED_4DBC
                }

                SALETYPE.POS_ENTRY_SWIPED_NO4DBC -> {
                    return POS_ENTRY_SWIPED_NO4DBC
                }
                SALETYPE.POS_ENTRY_SWIPED_NO4DBC_PIN -> {
                    return POS_ENTRY_SWIPED_NO4DBC_PIN
                }
                SALETYPE.POS_ENTRY_MANUAL_4DBC -> {
                    return POS_ENTRY_MANUAL_4DBC
                }
                SALETYPE.POS_ENTRY_MANUAL_NO4DBC -> {
                    return POS_ENTRY_MANUAL_NO4DBC
                }
                SALETYPE.CTLS_MSD_POS_ENTRY_CODE -> {
                    return CTLS_MSD_POS_ENTRY_CODE
                }
                SALETYPE.CTLS_MSD_POS_WITH_PIN -> {
                    return CTLS_MSD_POS_WITH_PIN
                }
                SALETYPE.CTLS_EMV_POS_ENTRY_CODE -> {
                    return CTLS_EMV_POS_ENTRY_CODE
                }
                SALETYPE.CTLS_EMV_POS_WITH_PIN -> {
                    return CTLS_EMV_POS_WITH_PIN
                }


            }
            return 0
        }

        ///transaction type
        val KEY_EXCHANGE = 1
        const val INIT = 2
        const val LOGON = 3
        const val SALE = 4
        const val VOID = 5
        const val SETTLEMENT = 6
        const val APP_UPDATE = 7
        const val BALANCE_ENQUIRY = 8
        const val REFUND = 9
        const val VOID_REFUND = 10
        const val SALE_WITH_CASH = 11
        const val CASH = 12
        const val BATCH_UPLOAD = 13
        const val PRE_AUTH = 14

        const val SALE_WITH_TIP = 15
        const val ADJUSTMENT = 16
        const val REVERSAL = 17
        const val EMI_SALE = 18
        const val EMI = 19
        const val SALE_COMPLETION = 20
        const val TIP_ADJUSTMENT = 21

        const val OFF_SALE = 22
        const val CASH_AT_POS = 23
        const val BATCH_SETTELEMENT = 24

        const val PRE_AUTH_COMPLETE = 26
        //var transactionType: TransactionType? = TransactionType.SALE

        fun getTransactionType(transactionType: TransactionType?): Int {

            return when (transactionType) {

                TransactionType.KEY_EXCHANGE -> KEY_EXCHANGE

                TransactionType.INIT -> INIT

                TransactionType.LOGON -> LOGON

                TransactionType.SALE -> SALE

                TransactionType.VOID -> VOID

                TransactionType.SETTLEMENT -> SETTLEMENT

                TransactionType.APP_UPDATE -> APP_UPDATE

                TransactionType.BALANCE_ENQUIRY -> BALANCE_ENQUIRY

                TransactionType.REFUND -> REFUND

                TransactionType.VOID_REFUND -> VOID_REFUND

                TransactionType.SALE_WITH_CASH -> SALE_WITH_CASH

                TransactionType.CASH -> CASH

                TransactionType.BATCH_UPLOAD -> BATCH_UPLOAD

                TransactionType.PRE_AUTH -> PRE_AUTH

                TransactionType.SALE_WITH_TIP -> SALE_WITH_TIP

                TransactionType.ADJUSTMENT -> ADJUSTMENT

                TransactionType.REVERSAL -> REVERSAL

                TransactionType.EMI_SALE -> EMI_SALE

                TransactionType.EMI -> EMI

                TransactionType.SALE_COMPLETION -> SALE_COMPLETION

                TransactionType.TIP_ADJUSTMENT -> TIP_ADJUSTMENT

                TransactionType.CASH_AT_POS -> CASH_AT_POS

                TransactionType.BATCH_SETTELEMENT -> BATCH_SETTELEMENT

                TransactionType.PRE_AUTH_COMPLETE -> PRE_AUTH_COMPLETE

                else -> 0
            }

        }

        fun getTransactionStringType(transactionType: Int): String {
            return when (transactionType) {
                KEY_EXCHANGE -> "KEY-EXCHANGE"
                INIT -> "INIT"
                LOGON -> "LOGON"
                SALE -> "SALE"
                SALE_WITH_CASH -> "SALE-WITH-CASH"
                CASH_AT_POS -> "CASH"
                VOID -> "VOID"
                SETTLEMENT -> "SETTLEMENT"
                APP_UPDATE -> "APP-UPDATE"
                BALANCE_ENQUIRY -> "BALANCE_ENQUIRY"

                REFUND -> "REFUND"

                VOID_REFUND -> "VOID-REFUND"

                BATCH_UPLOAD -> "BATCH-UPLOAD"
                PRE_AUTH -> "PRE_AUTH"
                SALE_WITH_TIP -> "SALE-WITH-TIP"
                ADJUSTMENT -> "ADJUSTMENT"
                REVERSAL -> "REVERSAL"
                EMI_SALE -> "EMI-SALE"
                EMI -> "EMI"
                SALE_COMPLETION -> "SALE-COMPLETION"
                TIP_ADJUSTMENT -> "TIP-ADJUSTMENT"

                PRE_AUTH_COMPLETE -> "AUTH COMPLETE"
                else -> ""

            }

        }

        fun getTotalTransactionType(transactionType: Int): String {

            when (transactionType) {
                KEY_EXCHANGE -> {
                    return "TOTAL KEY-EXCHANGE"
                }

                INIT -> {
                    return "TOTAL INIT"
                }
                LOGON -> {
                    return "TOTAL LOGON"
                }
                SALE -> {
                    return "TOTAL SALE"
                }
                VOID -> {
                    return "TOTAL VOID"
                }
                SETTLEMENT -> {
                    return "TOTAL SETTLEMENT"
                }
                APP_UPDATE -> {
                    return "TOTAL APP-UPDATE"
                }
                BALANCE_ENQUIRY -> {
                    return "TOTAL BALANCE_ENQUIRY"
                }

                REFUND -> {
                    return "TOTAL REFUND"
                }
                VOID_REFUND -> {
                    return "TOTAL VOID-REFUND"
                }
                SALE_WITH_CASH -> {
                    return "TOTAL SALE-WITH-CASH"
                }
                CASH_AT_POS -> {
                    return "TOTAL CASH"
                }
                BATCH_UPLOAD -> {
                    return "TOTAL BATCH-UPLOAD"
                }
                PRE_AUTH -> {
                    return "TOTAL PRE_AUTH"
                }
                SALE_WITH_TIP -> {
                    return "TOTAL SALE-WITH-TIP"
                }
                ADJUSTMENT -> {
                    return "TOTAL ADJUSTMENT"
                }
                REVERSAL -> {
                    return "TOTAL REVERSAL"
                }
                EMI_SALE -> {
                    return "TOTAL EMI-SALE"
                }
                EMI -> {
                    return "TOTAL EMI"
                }
                SALE_COMPLETION -> {
                    return "TOTAL SALE-COMPLETION"
                }
                TIP_ADJUSTMENT -> {
                    return "TOTAL TIP-ADJUSTMENT"
                }


            }
            return ""
        }

        fun getTransactionForSummaryType(transactionType: Int): String {
//,,,,VOID REFUND,TIP ADJUST,TIP,CASH SALE,PREAUTH,SALE COMP//SALE,CASH
            when (transactionType) {
                KEY_EXCHANGE -> {
                    return "TOTAL KEY-EXCHANGE"
                }
                OFF_SALE -> {
                    return "TOTAL OFF SALE"
                }
                INIT -> {
                    return "TOTAL INIT"
                }
                LOGON -> {
                    return "TOTAL LOGON"
                }
                SALE -> {
                    return "SALE"
                }
                VOID -> {
                    return "VOID"
                }
                SETTLEMENT -> {
                    return "TOTAL SETTLEMENT"
                }
                APP_UPDATE -> {
                    return "TOTAL APP-UPDATE"
                }
                BALANCE_ENQUIRY -> {
                    return "TOTAL BALANCE_ENQUIRY"
                }

                REFUND -> {
                    return "REFUND"
                }
                VOID_REFUND -> {
                    return "TOTAL VOID-REFUND"
                }
                SALE_WITH_CASH -> {
                    return "TOTAL SALE-WITH-CASH"
                }
                CASH_AT_POS -> {
                    return "TOTAL CASH"
                }
                BATCH_UPLOAD -> {
                    return "TOTAL BATCH-UPLOAD"
                }
                PRE_AUTH -> {
                    return "TOTAL PRE_AUTH"
                }
                SALE_WITH_TIP -> {
                    return "TOTAL SALE-WITH-TIP"
                }
                ADJUSTMENT -> {
                    return "TOTAL ADJUSTMENT"
                }
                REVERSAL -> {
                    return "TOTAL REVERSAL"
                }
                EMI_SALE -> {
                    return "TOTAL EMI-SALE"
                }
                EMI -> {
                    return "TOTAL EMI"
                }
                SALE_COMPLETION -> {
                    return "TOTAL SALE-COMPLETION"
                }
                TIP_ADJUSTMENT -> {
                    return "TOTAL TIP-ADJUSTMENT"
                }


            }
            return ""
        }

        fun getTransactionForSummaryTypeByName(transactionType: String): Int {
            // {"SALE","CASH","SALE-WITH-CASH", "VOID",   "REFUND"}
            when (transactionType) {
                "SALE" -> {
                    return SALE
                }
                "CASH" -> {
                    return CASH_AT_POS
                }
                "SALE-WITH-CASH" -> {
                    return SALE_WITH_CASH
                }
                "VOID" -> {
                    return VOID
                }
                "REFUND" -> {
                    return REFUND
                }


            }
            return 0
        }


        fun getProcCode(transactionType: Int): String {
//,,,,VOID REFUND,TIP ADJUST,TIP,CASH SALE,PREAUTH,SALE COMP
            return when (transactionType) {
                INIT -> INIT_Proc_Code
                SALE -> Sale_Proc_Code
                VOID -> Void_Proc_Code
                APP_UPDATE -> Appl_Update_Proc_Code
                SETTLEMENT -> Zero_Settlement
                CASH_AT_POS -> CASH_AT_POS_PROC
                SALE_WITH_CASH -> SALE_WITH_CASH_PROC
                REFUND -> REFUND_PROCESSING_CODE
                PRE_AUTH -> PRE_AUTH_PROCESSING_CODE
                PRE_AUTH_COMPLETE -> AUTH_COMPLETE_PROCESSING_CODE
                else -> ""
            }

        }

    }


}
