package com.example.verifonevx990app.disputetransaction

import com.example.verifonevx990app.R
import com.example.verifonevx990app.realmtables.BatchFileDataTable
import com.example.verifonevx990app.realmtables.TerminalParameterTable
import com.example.verifonevx990app.vxUtils.*

class CreateSettlementPacket(
    private var settlementProcessingCode: String? = null,
    private var batchList: MutableList<BatchFileDataTable>
) :
    ISettlementPacketExchange {

    override fun createSettlementISOPacket(): IWriter = IsoDataWriter().apply {
        val tpt = TerminalParameterTable.selectFromSchemeTable()
        if (tpt != null) {
            mti = Mti.SETTLEMENT_MTI.mti

            //Processing Code:-
            addField(3, settlementProcessingCode ?: ProcessingCode.SETTLEMENT.code)

            //ROC will not go in case of AMEX on all PORT but for HDFC it was mandatory:-
            // Sending ROC in case of HDFC ........
            addField(11, ROCProviderV2.getRoc(AppPreference.getBankCode()).toString())

            //adding nii
            addField(24, Nii.DEFAULT.nii)

            //adding tid
            addFieldByHex(41, tpt.terminalId)

            //adding mid
            addFieldByHex(42, tpt.merchantId)

            //adding field 48
            addFieldByHex(48, Field48ResponseTimestamp.getF48Data())

            //Batch Number
            addFieldByHex(60, addPad(tpt.batchNumber, "0", 6, true))

            //adding field 61
            addFieldByHex(
                61,
                addPad(
                    AppPreference.getString("serialNumber"),
                    " ",
                    15,
                    false
                ) + AppPreference.getBankCode()
            )

            val version = addPad(getAppVersionNameAndRevisionID(), "0", 15, false)
            val pcNumber = addPad(AppPreference.getString(AppPreference.PC_NUMBER_KEY), "0", 9)
            //adding field 62
            addFieldByHex(
                62, ConnectionType.GPRS.code + addPad(
                    AppPreference.getString("deviceModel"), " ", 6, false
                ) +
                        addPad(
                            VerifoneApp.appContext.getString(R.string.app_name),
                            " ",
                            10,
                            false
                        ) +
                        version + pcNumber + addPad("0", "0", 9)
            )
            //adding field 63
            var saleCount = 0
            var saleAmount = 0L

            var refundCount = 0
            var refundAmount = "0"

            //SEQUENCE-------> sale, emi sale ,sale with cash, cash only,auth comp,and tip transaction type will be included.
            //Manipulating Data based on condition for Field 63:-
            if (batchList.size > 0) {
                for (i in 0 until batchList.size) {
                    when (batchList[i].transactionType) {
                        TransactionType.SALE.type -> {
                            saleCount = saleCount.plus(1)
                            saleAmount = saleAmount.plus(batchList[i].transactionalAmmount.toLong())
                        }
                        TransactionType.EMI_SALE.type -> {
                            saleCount = saleCount.plus(1)
                            saleAmount = saleAmount.plus(batchList[i].emiTransactionAmount.toLong())
                        }
                        TransactionType.BRAND_EMI.type -> {
                            saleCount = saleCount.plus(1)
                            saleAmount = saleAmount.plus(batchList[i].emiTransactionAmount.toLong())
                        }
                        TransactionType.BRAND_EMI_BY_ACCESS_CODE.type -> {
                            saleCount = saleCount.plus(1)
                            saleAmount = saleAmount.plus(batchList[i].transactionalAmmount.toLong())
                        }
                        TransactionType.SALE_WITH_CASH.type -> {
                            saleCount = saleCount.plus(1)
                            saleAmount = saleAmount.plus(batchList[i].transactionalAmmount.toLong())
                        }
                        TransactionType.CASH_AT_POS.type -> {
                            saleCount = saleCount.plus(1)
                            saleAmount = saleAmount.plus(batchList[i].transactionalAmmount.toLong())
                        }
                        TransactionType.PRE_AUTH_COMPLETE.type -> {
                            saleCount = saleCount.plus(1)
                            saleAmount = saleAmount.plus(batchList[i].transactionalAmmount.toLong())
                        }
                        TransactionType.TIP_SALE.type -> {
                            saleCount = saleCount.plus(1)
                            saleAmount = saleAmount.plus(batchList[i].totalAmmount.toLong())
                        }
                        TransactionType.TEST_EMI.type -> {
                            saleCount = saleCount.plus(1)
                            saleAmount = saleAmount.plus(100.toLong())
                        }
                        TransactionType.REFUND.type -> {
                            refundCount = refundCount.plus(1)
                            refundAmount =
                                refundAmount.plus(batchList[i].transactionalAmmount.toLong())
                        }
                    }
                }

                val sCount = addPad(saleCount, "0", 3, true)
                val sAmount = addPad(saleAmount.toString(), "0", 12, true)

                val rCount = addPad(refundCount, "0", 3, true)
                val rAmount = addPad(refundAmount, "0", 12, true)

                //   sale, emi sale ,sale with cash, cash only,auth comp,and tip transaction


                addFieldByHex(
                    63,
                    addPad(
                        sCount + sAmount + rCount + rAmount,
                        "0",
                        90,
                        toLeft = false
                    )
                )
            } else {
                addFieldByHex(63, addPad(0, "0", 90, toLeft = false))
            }
        }
        logger("SETTLEMENT REQ PACKET -->", this.isoMap, "e")

    }
}