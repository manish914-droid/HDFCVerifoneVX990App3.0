package com.example.verifonevx990app.bankemi

import android.content.Context
import android.os.Parcelable
import android.text.TextUtils
import android.util.Log
import com.example.verifonevx990app.brandemi.BrandEMIDataModal
import com.example.verifonevx990app.emv.transactionprocess.CardProcessedDataModal
import com.example.verifonevx990app.main.SplitterTypes
import com.example.verifonevx990app.realmtables.BrandEMIDataTable
import com.example.verifonevx990app.realmtables.TerminalParameterTable
import com.example.verifonevx990app.vxUtils.*
import com.example.verifonevx990app.vxUtils.KeyExchanger.Companion.getF61
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.parcelize.Parcelize

// In triple last Boolean is for checking that card has a emi facility on sale time
class GenericEMISchemeAndOffer(
    var context: Context, var cardProcessedDataModal: CardProcessedDataModal,
    var cardBinValue: String, var transactionAmount: Long,var brandEmiData:BrandEMIDataModal?,
    var callback: (Pair<MutableList<BankEMIDataModal>, MutableList<BankEMIIssuerTAndCDataModal>>, Triple<Boolean, String, Boolean>) -> Unit
) {
    private var field57Request: String? = null
    private var bankEMIRequestCode = "4"
    private var moreDataFlag = "0"
    private var totalRecord: String? = "0"
    private var perPageRecord: String? = "0"
    private var isBool = false
    private var bankEMISchemesDataList: MutableList<BankEMIDataModal> = mutableListOf()
    private var bankEMIIssuerTAndCList: MutableList<BankEMIIssuerTAndCDataModal> = mutableListOf()
  //  private var brandEMIDataTable: BrandEMIDataTable? = null

    init {
        bankEMISchemesDataList.clear()
        bankEMIIssuerTAndCList.clear()
        field57Request =
            if (cardProcessedDataModal.getTransType() == TransactionType.BRAND_EMI.type) {

                "$bankEMIRequestCode^0^${brandEmiData?.brandID}^${brandEmiData?.productID}^^${
                    cardBinValue.substring(0, 8)
                }^$transactionAmount"
            } else {
                //9^0^^200000
                "$bankEMIRequestCode^0^1^0^^${cardBinValue.substring(0, 8)}^$transactionAmount"
            }
        GlobalScope.launch(Dispatchers.IO) {
            fetchBankEMIDetailsFromHost()
        }
    }

    //region========================Fetching BankEMI Details From Host========================
    private suspend fun fetchBankEMIDetailsFromHost() {
        GlobalScope.launch(Dispatchers.IO) {
            Log.d("Field57Request:- ", field57Request ?: "")
            val bankEMIISOPacket = createBankEMIPacket()
            HitServer.hitServer(bankEMIISOPacket.generateIsoByteRequest(), { result, success ->
                if (success) {
                    val responseIsoData: IsoDataReader = readIso(result, false)
                    logger("Bank EMI RESPONSE ", "---", "e")
                    logger("Bank EMI Data--->>", responseIsoData.isoMap, "e")
                    Log.e(
                        "Success 39-->  ",
                        responseIsoData.isoMap[39]?.parseRaw2String()
                            .toString() + "---->" + responseIsoData.isoMap[58]?.parseRaw2String()
                            .toString()
                    )

                    val bankEMIHostResponseData =
                        responseIsoData.isoMap[57]?.parseRaw2String().toString()
                    val hostMsg = responseIsoData.isoMap[58]?.parseRaw2String().toString()
                    val successResponseCode =
                        responseIsoData.isoMap[39]?.parseRaw2String().toString()
                    isBool = successResponseCode == "00"
                    if (isBool) {
                        parseAndStubbingBankEMIDataToList(bankEMIHostResponseData, hostMsg)
                    } else {
                        callback(
                            Pair(bankEMISchemesDataList, bankEMIIssuerTAndCList),
                            Triple(isBool, hostMsg, false)
                        )
                    }
                } else {
                    ROCProviderV2.incrementFromResponse(
                        ROCProviderV2.getRoc(AppPreference.getBankCode()).toString(),
                        AppPreference.getBankCode()
                    )
                    isBool = false
                    callback(
                        Pair(bankEMISchemesDataList, bankEMIIssuerTAndCList),
                        Triple(isBool, "", false)
                    )
                }
            }, {})
        }
    }
    //endregion

    //region=========================BankEMI ISO Request Packet===============================
    private suspend fun createBankEMIPacket(): IsoDataWriter = IsoDataWriter().apply {
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

            //adding Field 57
            addFieldByHex(57, field57Request ?: "")

            //adding Field 61
            addFieldByHex(61, getF61())
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

    //region=================Parse and Stubbing BankEMI Data To List:-
    private fun parseAndStubbingBankEMIDataToList(
        bankEMIHostResponseData: String,
        hostMsg: String
    ) {
        GlobalScope.launch(Dispatchers.Main) {
            if (!TextUtils.isEmpty(bankEMIHostResponseData)) {
                val parsingDataWithCurlyBrace =
                    parseDataListWithSplitter("}", bankEMIHostResponseData)
                val parsingDataWithVerticalLineSeparator =
                    parseDataListWithSplitter("|", parsingDataWithCurlyBrace[0])
                if (parsingDataWithVerticalLineSeparator.isNotEmpty()) {
                    moreDataFlag = parsingDataWithVerticalLineSeparator[0]
                    perPageRecord = parsingDataWithVerticalLineSeparator[1]
                    totalRecord =
                        (totalRecord?.toInt()?.plus(perPageRecord?.toInt() ?: 0)).toString()
                    //Store DataList in Temporary List and remove first 2 index values to get sublist from 2nd index till dataList size
                    // and iterate further on record data only:-
                    var tempDataList = mutableListOf<String>()
                    tempDataList = parsingDataWithVerticalLineSeparator.subList(
                        2,
                        parsingDataWithVerticalLineSeparator.size
                    )

                    for (i in tempDataList.indices) {
                        if (!TextUtils.isEmpty(tempDataList[i])) {
                            val splitData = parseDataListWithSplitter(
                                SplitterTypes.CARET.splitter,
                                tempDataList[i]
                            )
                            bankEMISchemesDataList.add(
                                BankEMIDataModal(
                                    splitData[0], splitData[1],
                                    splitData[2], splitData[3],
                                    splitData[4], splitData[5],
                                    splitData[6], splitData[7],
                                    splitData[8], splitData[9],
                                    splitData[10], splitData[11],
                                    splitData[12], splitData[13],
                                    splitData[14], splitData[15],
                                    splitData[16], splitData[17],
                                    splitData[18], splitData[19],
                                    splitData[20]
                                )
                            )
                        }
                    }

                    //Parsing and Stubbing BankEMI TAndC Data:-
                    val issuerRelatedData = parseDataListWithSplitter(
                        SplitterTypes.VERTICAL_LINE.splitter,
                        parsingDataWithCurlyBrace[1]
                    )
                    if (!TextUtils.isEmpty(issuerRelatedData[0])) {
                        val issuerTAndCData = parseDataListWithSplitter(
                            SplitterTypes.CARET.splitter,
                            issuerRelatedData[0]
                        )

                        Log.d("IssuerSchemeID", issuerTAndCData[0])
                        Log.d("IssuerID", issuerTAndCData[1])
                        Log.d("IssuerName", issuerTAndCData[2])
                        if (issuerTAndCData.size > 3) {
                            bankEMIIssuerTAndCList.add(
                                BankEMIIssuerTAndCDataModal(
                                    issuerTAndCData[0],
                                    issuerTAndCData[1],
                                    issuerTAndCData[2],
                                    issuerTAndCData[3],
                                    issuerRelatedData[1]
                                )
                            )
                        } else {
                            bankEMIIssuerTAndCList.add(
                                BankEMIIssuerTAndCDataModal(
                                    issuerTAndCData[0],
                                    issuerTAndCData[1],
                                    issuerTAndCData[2],
                                    "",
                                    issuerRelatedData[1]
                                )
                            )
                        }
                    }


                    //Refresh Field57 request value for Pagination if More Record Flag is True:-
                    if (moreDataFlag == "1") {
                        field57Request =
                            if (cardProcessedDataModal.getTransType() == TransactionType.BRAND_EMI.type) {
                                "$bankEMIRequestCode^$totalRecord^${brandEmiData?.brandID}^${brandEmiData?.productID}^^${
                                    cardBinValue.substring(
                                        0,
                                        8
                                    )
                                }^$transactionAmount"
                            } else {
                                "$bankEMIRequestCode^$totalRecord^1^0^^${
                                    cardBinValue.substring(
                                        0,
                                        8
                                    )
                                }^$transactionAmount"
                            }
                        fetchBankEMIDetailsFromHost()
                    } else {
                        Log.d("Total BankEMI Data:- ", bankEMISchemesDataList.toString())
                        Log.d("Total BankEMI TAndC:- ", parsingDataWithCurlyBrace[1])
                        ROCProviderV2.incrementFromResponse(
                            ROCProviderV2.getRoc(AppPreference.getBankCode()).toString(),
                            AppPreference.getBankCode()
                        )
                        callback(
                            Pair(bankEMISchemesDataList, bankEMIIssuerTAndCList),
                            Triple(isBool, "", true)
                        )
                    }
                }
            }
        }
        //endregion
    }
}

//region==================Data Modal For BankEMI Data:-
@Parcelize
data class BankEMIDataModal(
    var tenure: String,
    var tenureInterestRate: String,
    var effectiveRate: String,
    var discountModel: String,
    var transactionAmount: String = "0",
    var discountAmount: String = "0",
    var discountFixedValue: String,
    var discountPercentage: String,
    var loanAmount: String = "0",
    var emiAmount: String,
    var totalEmiPay: String,
    var processingFee: String,
    var processingRate: String,
    var totalProcessingFee: String,
    var totalInterestPay: String = "0",
    val cashBackAmount: String = "0",
    var netPay: String,
    var tenureTAndC: String,
    var tenureWiseDBDTAndC: String,
    var discountCalculatedValue: String,
    var cashBackCalculatedValue: String
) : Parcelable
//endregion

//region==================Data Modal For BankEMI Issuer TAndC Data:-
@Parcelize
data class BankEMIIssuerTAndCDataModal(
    var emiSchemeID: String,
    var issuerID: String,
    var issuerName: String,
    var schemeTAndC: String,
    var updateIssuerTAndCTimeStamp: String
) : Parcelable
//endregion