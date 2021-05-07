package com.example.verifonevx990app.brandemi

import android.text.TextUtils
import android.util.Log
import com.example.verifonevx990app.vxUtils.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

class GenericBrandTAndC(
    private var brandRequestType: String,
    private var brandTAndCCallBack: (Pair<MutableList<String>, Boolean>, Pair<String, String>) -> Unit
) {

    //region====================Brand Terms And Conditions Variables:-
    private var brandField57Data = "$brandRequestType^0"
    private var brandTAndCMoreDataFlag: String? = null
    private var brandTAndCPerPageRecord: Int = 0
    private var brandTAndCTotalRecord: Int = 0
    private var brandTAndCRecordData: String? = null
    private var brandTermsAndConditionsDataList = mutableListOf<String>()
    //endregion

    init {
        fetchBrandTAndC()
    }

    //region=============================Fetch Brand Terms and Conditions:-
    private fun fetchBrandTAndC() {
        var brandTAndCISOData: IsoDataWriter? = null
        //region==============================Creating ISO Packet For BrandEMIMasterData Request:-
        runBlocking(Dispatchers.IO) {
            CreateBrandEMIPacket(brandField57Data) {
                brandTAndCISOData = it
            }
        }
        //endregion

        //region==============================Host Hit To Fetch BrandEMIMaster Data:-
        GlobalScope.launch(Dispatchers.IO) {
            if (brandTAndCISOData != null) {
                val byteArrayRequest = brandTAndCISOData?.generateIsoByteRequest()
                HitServer.hitServer(byteArrayRequest!!, { result, success ->
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
                        val brandTAndCData =
                            responseIsoData.isoMap[57]?.parseRaw2String().toString()

                        if (responseCode == "00") {
                            ROCProviderV2.incrementFromResponse(
                                ROCProviderV2.getRoc(AppPreference.getBankCode()).toString(),
                                AppPreference.getBankCode()
                            )
                            GlobalScope.launch(Dispatchers.Main) {
                                //Processing BrandEMIMasterData:-
                                stubbingBrandTAndCData(brandTAndCData, responseCode, hostMsg)
                            }
                        } else {
                            ROCProviderV2.incrementFromResponse(
                                ROCProviderV2.getRoc(AppPreference.getBankCode()).toString(),
                                AppPreference.getBankCode()
                            )
                            brandTAndCCallBack(
                                Pair(mutableListOf(), false),
                                Pair(responseCode, hostMsg)
                            )

                        }
                    } else {
                        ROCProviderV2.incrementFromResponse(
                            ROCProviderV2.getRoc(AppPreference.getBankCode()).toString(),
                            AppPreference.getBankCode()
                        )
                        brandTAndCCallBack(Pair(mutableListOf(), false), Pair("", result))
                    }
                }, {})
            }
        }
        //endregion
    }
    //endregion

    //region===========================Below method is used to Stubbing brand terms and conditions data:-
    private fun stubbingBrandTAndCData(brandTAndC: String, responseCode: String, hostMsg: String) {
        GlobalScope.launch(Dispatchers.Main) {
            if (!TextUtils.isEmpty(brandTAndC)) {
                val dataList = parseDataListWithSplitter("|", brandTAndC)
                if (dataList.isNotEmpty()) {
                    brandTAndCMoreDataFlag = dataList[0]
                    brandTAndCPerPageRecord = dataList[1].toInt()
                    brandTAndCTotalRecord += brandTAndCPerPageRecord
                    brandTAndCRecordData = dataList[2]

                    //Store DataList in Temporary List and remove first 2 index values to get sublist from 3th index till dataList size
                    // and iterate further on record data only:-
                    var tempDataList = mutableListOf<String>()
                    tempDataList = dataList.subList(2, dataList.size - 1)
                    for (i in tempDataList.indices) {
                        Log.d("IssuerTAndC:- ", tempDataList[i])
                        brandTermsAndConditionsDataList.add(tempDataList[i])
                    }
                    if (brandTAndCMoreDataFlag == "1") {
                        brandField57Data = "$brandRequestType^$brandTAndCTotalRecord"
                        fetchBrandTAndC()
                    } else {
                        brandTAndCCallBack(
                            Pair(brandTermsAndConditionsDataList, true),
                            Pair(responseCode, hostMsg)
                        )
                    }
                }
            }
        }
    }
    //endregion
}