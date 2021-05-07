package com.example.verifonevx990app.bankemi

import android.text.TextUtils
import android.util.Log
import com.example.verifonevx990app.main.EMIRequestType
import com.example.verifonevx990app.realmtables.TerminalParameterTable
import com.example.verifonevx990app.vxUtils.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

class GenericEMIIssuerTAndC(var issuerTAndCCallBack: (Pair<MutableList<String>, Boolean>, Pair<String, String>) -> Unit) {

    //region====================Issuer Terms And Conditions Variables:-
    private var issuerField57Data = "${EMIRequestType.ISSUER_T_AND_C.requestType}^0"
    private var issuerTAndCMoreDataFlag: String? = null
    private var issuerTAndCPerPageRecord: Int = 0
    private var issuerTAndCTotalRecord: Int = 0
    private var issuerTAndCRecordData: String? = null
    private var issuerTermsAndConditionsDataList = mutableListOf<String>()
    //endregion

    init {
        GlobalScope.launch(Dispatchers.IO) {
            fetchIssuerTAndC()
        }
    }

    //region=============================Fetch Issuer Terms and Conditions:-
    private suspend fun fetchIssuerTAndC() {
        //region==============================Host Hit To Fetch BrandEMIMaster Data:-
        GlobalScope.launch(Dispatchers.IO) {
            val issuerTAndCISOData: IsoDataWriter = createIssuerTAndCRequestPacket()
            if (issuerTAndCISOData != null) {
                val byteArrayRequest = issuerTAndCISOData.generateIsoByteRequest()
                HitServer.hitServer(byteArrayRequest, { result, success ->
                    if (success && !TextUtils.isEmpty(result)) {
                        val responseIsoData: IsoDataReader = readIso(result, false)
                        logger("Transaction RESPONSE ", "---", "e")
                        logger("Transaction RESPONSE --->>", responseIsoData.isoMap, "e")
                        Log.e(
                            "Success 39-->  ", responseIsoData.isoMap[39]?.parseRaw2String()
                                .toString() + "---->" + responseIsoData.isoMap[58]?.parseRaw2String()
                                .toString()
                        )

                        val responseCode = responseIsoData.isoMap[39]?.parseRaw2String().toString()
                        val hostMsg = responseIsoData.isoMap[58]?.parseRaw2String().toString()
                        val issuerTAndCData =
                            responseIsoData.isoMap[57]?.parseRaw2String().toString()

                        if (responseCode == "00") {
                            GlobalScope.launch(Dispatchers.Main) {
                                //Processing BrandEMIMasterData:-
                                stubbingIssuerTAndCData(issuerTAndCData, responseCode, hostMsg)
                            }
                        } else {
                            ROCProviderV2.incrementFromResponse(
                                ROCProviderV2.getRoc(AppPreference.getBankCode()).toString(),
                                AppPreference.getBankCode()
                            )
                            issuerTAndCCallBack(
                                Pair(issuerTermsAndConditionsDataList, false),
                                Pair(responseCode, hostMsg)
                            )

                        }
                    } else {
                        ROCProviderV2.incrementFromResponse(
                            ROCProviderV2.getRoc(AppPreference.getBankCode()).toString(),
                            AppPreference.getBankCode()
                        )
                        issuerTAndCCallBack(
                            Pair(issuerTermsAndConditionsDataList, false),
                            Pair("", result)
                        )
                    }
                }, {})
            }
        }
        //endregion
    }
    //endregion

    //region=========================EMI IssuerTAndC ISO Request Packet===============================
    private suspend fun createIssuerTAndCRequestPacket(): IsoDataWriter = IsoDataWriter().apply {
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

            //adding field 57
            addFieldByHex(57, issuerField57Data)


            //adding Field 61
            addFieldByHex(61, KeyExchanger.getF61())

            //adding Field 63
            val deviceSerial = addPad(AppPreference.getString("serialNumber"), " ", 15, false)
            val bankCode = AppPreference.getBankCode()
            val f63 = "$deviceSerial$bankCode"
            addFieldByHex(63, f63)
        }
    }
    //endregion

    //region===========================Below method is used to Stubbing issuer terms and conditions data:-
    private fun stubbingIssuerTAndCData(
        issuerTAndC: String,
        responseCode: String,
        hostMsg: String
    ) {
        GlobalScope.launch(Dispatchers.Main) {
            if (!TextUtils.isEmpty(issuerTAndC)) {
                val dataList = parseDataListWithSplitter("|", issuerTAndC)
                if (dataList.isNotEmpty()) {
                    issuerTAndCMoreDataFlag = dataList[0]
                    issuerTAndCPerPageRecord = dataList[1].toInt()
                    issuerTAndCTotalRecord += issuerTAndCPerPageRecord
                    issuerTAndCRecordData = dataList[2]

                    //Store DataList in Temporary List and remove first 2 index values to get sublist from 3th index till dataList size
                    // and iterate further on record data only:-
                    var tempDataList = mutableListOf<String>()
                    tempDataList = dataList.subList(2, dataList.size - 1)
                    for (i in tempDataList.indices) {
                        Log.d("IssuerTAndC:- ", tempDataList[i])
                        issuerTermsAndConditionsDataList.add(tempDataList[i])
                    }
                    if (issuerTAndCMoreDataFlag == "1") {
                        issuerField57Data =
                            "${EMIRequestType.ISSUER_T_AND_C.requestType}^$issuerTAndCTotalRecord"
                        fetchIssuerTAndC()
                    } else {
                        issuerTAndCCallBack(
                            Pair(issuerTermsAndConditionsDataList, true),
                            Pair(responseCode, hostMsg)
                        )
                        //TODO Here we need to Save Data in DB and Callback======================
                    }
                }
            }
        }
    }
//endregion
}