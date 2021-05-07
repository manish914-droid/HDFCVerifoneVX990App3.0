package com.example.verifonevx990app.crosssell

import android.content.Context
import android.os.Parcelable
import android.util.Log
import com.example.verifonevx990app.emv.transactionprocess.CardProcessedDataModal
import com.example.verifonevx990app.realmtables.TerminalParameterTable
import com.example.verifonevx990app.vxUtils.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.parcelize.Parcelize

class FlexiPayReqSentServerAndParseData(
    var encryptedPan: String,
    var context: Context,
    var cardProcessedDataModal: CardProcessedDataModal,
    var transactionAmount: String, var callBack: (ArrayList<FlexiPayData>, Boolean, String) -> Unit
) {

    private var flexiPay57DataList: ArrayList<FlexiPayData> = arrayListOf()
    private var moreDataFlag = "0"
    private var totalRecord = "0"

    //9^0^^200000
    private var transAmountInPaisa = "0"
    private var accessCode = "9"
    private var field57Request: String? = null
    private var isBool = false

    init {
        transAmountInPaisa = transactionAmount
        field57Request = "${accessCode}^${moreDataFlag}^^${transAmountInPaisa}"
        GlobalScope.launch(Dispatchers.IO) {
            fetchingFlexiPayData()
        }
    }

    //region========================Fetching BankEMI Details From Host========================
    private suspend fun fetchingFlexiPayData() {
        GlobalScope.launch(Dispatchers.IO) {
            Log.d("Field57Request:- ", field57Request ?: "")
            val flexiPayISOPacket = createFlexiPayPacket()
            logger("FLEXI PAY REQ ", "---", "e")
            logger("FLEXI PAY REQ Data--->>", flexiPayISOPacket.isoMap, "e")
            HitServer.hitServer(flexiPayISOPacket.generateIsoByteRequest(), { result, success ->
                if (success) {
                    ROCProviderV2.incrementFromResponse(
                        ROCProviderV2.getRoc(AppPreference.getBankCode()).toString(),
                        AppPreference.getBankCode()
                    )

                    val responseIsoData: IsoDataReader = readIso(result, false)
                    logger("FLEXI PAY RES ", "---", "e")
                    logger("FLEXI PAY Data--->>", responseIsoData.isoMap, "e")
                    Log.e(
                        "Success 39-->  ",
                        responseIsoData.isoMap[39]?.parseRaw2String()
                            .toString() + "---->" + responseIsoData.isoMap[58]?.parseRaw2String()
                            .toString()
                    )

                    val flexiPayHostResponseData =
                        responseIsoData.isoMap[57]?.parseRaw2String().toString()
                    val hostMsg = responseIsoData.isoMap[58]?.parseRaw2String().toString()
                    val successResponseCode =
                        responseIsoData.isoMap[39]?.parseRaw2String().toString()
                    isBool = successResponseCode == "00"
                    if (isBool) {
                        parseFlexiPayData(flexiPayHostResponseData, hostMsg)
                        //  parseAndStubbingBankEMIDataToList(flexiPayHostResponseData, hostMsg)
                    } else {
                        callBack(flexiPay57DataList, isBool, hostMsg)
                        val stt =
                            "0|4|BHFP001^15^0^800000^100^790795^2800^800000^0^9205^15 DAYS NO COST^15 DAYS NO COST^# I have been offered the choice of normal as well as EMI for this purchase and I have chosen EMI# I have fully understood and accept the terms of EMI scheme and applicable charges mentioned in the charge-slip# EMI conversion subject to Banks discretion and may take minimum 4 working days and the transaction amount will be disbursed to the merchant Customer should maintain sufficient fund in his SB a/c linked to this debit card.# If loan is taken on the 1st for 15 days, the same will be due on 16th, on 16th, customer?s account will be auto debited for the loan amount at no extra cost.# If the customer has taken tenure of 30 days, with a disbursement on January 10th & due date of Feb 9th. Customer will be charged interest + principal after 30 days from loan booking i.e. on Feb 9th (30 days including date of disbursement) similar to the case for 60 & 90 days tenure# I understand that the decision to grant EMI facility is at the sole discretion of HDFC Bank.# I hereby give my consent to auto debit my account linked to the debit card used for this transaction against the EMI facility granted by HDFC Bank till loan closure.# I hereby agree for HDFC to debit my linked account for Rs 1 in addition to the EMI booking and other charge thereon as applicable.# Additional charge are as applicable  - late payment charges - Rs 550 + GST ; Auto debit return charge - 2% +GST on EMI Amt , Min. of Rs. 550/-; Pre closure charges - 3% + GST ; GST :- 18% as per Govt. instruction. # I hereby agree to abide by the detailed T&C s published on www.hdfcbank.com at all times. # I understand that this EMI facility is between me and HDFC Bank basis my relationship / account held and the merchant / manufacturer will not be held responsible for the same.^# In case of any dispute on the charge slip content same need to be raised within 30 days.# All disputes, differences and /or claims arising in respect of the loan shall be referred to the arbitration of a sole arbitrator to be nominated by the bank, which arbitration shall be governed by the Arbitration and conciliation Act, 1996 .The award including interim award /s of the sole arbitrator shall be final and binding on all parties concerned. Subject to the foregoing, the courts and the tribunals at Mumbai, India will have exclusive jurisdiction. Product information|BHFP002^30^0^800000^100^800000^2800^818667^18411^18411^30 DAYS Interest Payble Rs.184  per Month^30 DAYS^^|BHFP003^60^0^800000^100^800000^2800^414054^18411^36822^60 DAYS Interest Payble Rs.184  per Month^60 DAYS^^|BHFP004^90^0^800000^100^800000^2800^279207^18411^55233^90 DAYS Interest Payble Rs.184  per Month^90 DAYS^^|"
                        parseFlexiPayData(stt, hostMsg)

                    }
                } else {
                    ROCProviderV2.incrementFromResponse(
                        ROCProviderV2.getRoc(AppPreference.getBankCode()).toString(),
                        AppPreference.getBankCode()
                    )
                    isBool = false

                    callBack(flexiPay57DataList, success, result)
                }
            }, {})
        }
    }
    //endregion

    //region=========================BankEMI ISO Request Packet===============================
    private suspend fun createFlexiPayPacket(): IsoDataWriter = IsoDataWriter().apply {
        val terminalData = TerminalParameterTable.selectFromSchemeTable()
        if (terminalData != null) {
            mti = Mti.BANK_EMI.mti

            //Processing Code Field 3
            addField(3, ProcessingCode.BANK_EMI.code)

            //STAN(ROC) Field 11
            addField(11, ROCProviderV2.getRoc(AppPreference.getBankCode()).toString())

            //NII Field 24
            addField(24, Nii.BANK_EMI.nii)

            //TID Field 41
            addFieldByHex(41, terminalData.terminalId)

            //adding Field 56
            addField56(56, encryptedPan)

            //adding Field 57
            addFieldByHex(57, field57Request ?: "")

            //adding Field 61
            addFieldByHex(61, KeyExchanger.getF61())
            /*val issuerParameterTable = IssuerParameterTable.selectFromIssuerParameterTable(AppPreference.WALLET_ISSUER_ID)
            issuerParameterTable?.let { getField61(it, terminalData.tidBankCode, Mti.BANK_EMI.mti) }
                ?.let { addFieldByHex(61, it) }*/

            //adding Field 63
            val deviceSerial = addPad(AppPreference.getString("serialNumber"), " ", 15, false)
            val bankCode = AppPreference.getBankCode()
            val f63 = "$deviceSerial$bankCode"
            addFieldByHex(63, f63)
        }
    }
    //endregion

    private fun parseFlexiPayData(data: String, hostMsg: String) {
        val flexiPayDataList = data.split("|", limit = 3)
        moreDataFlag = flexiPayDataList[0]
        totalRecord = flexiPayDataList[1]

        val flexiPayData = flexiPayDataList[2]
        val flexiPayTotalData57List = flexiPayData.split("|")

        for (fpd in flexiPayTotalData57List) {
            if (fpd != "") {
                val recordData = fpd.split("^")
                var tnCsHeader = recordData[12]
                var tnCsFooter = recordData[13]
                tnCsHeader = if (tnCsHeader == "") {
                    flexiPayTotalData57List[0].split("^")[12]
                } else {
                    recordData[12]
                }
                tnCsFooter = if (tnCsFooter == "") {
                    flexiPayTotalData57List[0].split("^")[13]
                } else {
                    recordData[13]
                }

                flexiPay57DataList.add(
                    FlexiPayData(
                        recordData[0],
                        recordData[1],
                        recordData[2],
                        recordData[3],
                        recordData[4],
                        recordData[5],
                        recordData[6],
                        recordData[7],
                        recordData[8],
                        recordData[9],
                        recordData[10],
                        recordData[11],
                        tnCsHeader,
                        tnCsFooter
                    )
                )

            }
        }
        println(flexiPay57DataList.toString())
        callBack(flexiPay57DataList, true, hostMsg)

    }


}

@Parcelize
data class FlexiPayData(
    var schemeCode: String,
    var tenureInDays: String,
    var effectiveRate: String,
    var originalTransactionAmount: String,
    var baseTransactionAmt: String,
    var loanAmount: String,
    var tenureROI: String,
    var eMIamountPerMonth: String,
    var interestPerMonth: String,
    var totalInterest: String,
    var tenureMessage: String,
    var tenureMenu: String,
    var tnCsHeader: String,
    var tnCsFooter: String
) : Parcelable