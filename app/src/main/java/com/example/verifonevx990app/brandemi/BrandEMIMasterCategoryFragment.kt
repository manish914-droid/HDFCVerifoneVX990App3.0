package com.example.verifonevx990app.brandemi

import android.content.Context
import android.os.Bundle
import android.text.TextUtils
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.DefaultItemAnimator
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.verifonevx990app.R
import com.example.verifonevx990app.bankemi.GenericEMIIssuerTAndC
import com.example.verifonevx990app.databinding.BrandEmiMasterCategoryFragmentBinding
import com.example.verifonevx990app.databinding.ItemBrandEmiFooterBinding
import com.example.verifonevx990app.databinding.ItemBrandEmiMasterBinding
import com.example.verifonevx990app.main.EMIRequestType
import com.example.verifonevx990app.main.MainActivity
import com.example.verifonevx990app.main.SplitterTypes
import com.example.verifonevx990app.realmtables.BrandEMIMasterTimeStamps
import com.example.verifonevx990app.realmtables.BrandTAndCTable
import com.example.verifonevx990app.realmtables.IssuerTAndCTable
import com.example.verifonevx990app.vxUtils.*
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

/**
This is a Brand EMI Master Category Data Fragment
Here we are Fetching Master Category Data From Host and Displaying on UI:-
================Written By Ajay Thakur on 8th March 2021====================
 */
class BrandEMIMasterCategoryFragment : Fragment() {
    private var binding: BrandEmiMasterCategoryFragmentBinding? = null
    private var iDialog: IDialog? = null
    private val brandEmiMasterDataList by lazy { mutableListOf<BrandEMIMasterDataModal>() }
    private val action by lazy { arguments?.getSerializable("type") ?: "" }
    private var field57RequestData: String? = null
    private var moreDataFlag = "0"
    private var totalRecord: String? = null
    private var brandTimeStamp: String? = null
    private var brandCategoryUpdatedTimeStamp: String? = null
    private var issuerTAndCTimeStamp: String? = null
    private var brandTAndCTimeStamp: String? = null
    private var empty_view_placeholder: ImageView? = null
    private val brandEMIDataModel by lazy { BrandEMIDataModal() }
    private val brandEMIMasterCategoryAdapter by lazy {
        BrandEMIMasterCategoryAdapter(
            brandEmiMasterDataList,
            moreDataFlag,
            ::onItemClick
        )
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        if (context is IDialog) iDialog = context
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = BrandEmiMasterCategoryFragmentBinding.inflate(layoutInflater, container, false)
        return binding?.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding?.subHeaderView?.subHeaderText?.text = getString(R.string.brand_emi_master)
        binding?.subHeaderView?.backImageButton?.setOnClickListener { parentFragmentManager.popBackStackImmediate() }
        //(activity as MainActivity).showBottomNavigationBar(isShow = false)
        empty_view_placeholder = view.findViewById(R.id.empty_view_placeholder)

        //Below we are assigning initial request value of Field57 in BrandEMIMaster Data Host Hit:-
        field57RequestData = "${EMIRequestType.BRAND_DATA.requestType}^0"

        //Initial SetUp of RecyclerView List with Empty Data , After Fetching Data from Host we will notify List:-
        setUpRecyclerView()
        brandEmiMasterDataList.clear()

        val issuerTAndCData = runBlocking(Dispatchers.IO) {
            IssuerTAndCTable.getAllIssuerTAndCData()
        }
        Log.d("IssuerTC:- ", Gson().toJson(issuerTAndCData))

        //Method to Fetch BrandEMIMasterData:-
        fetchBrandEMIMasterDataFromHost()
    }

    //region===============================Hit Host to Fetch BrandEMIMaster Data:-
    private fun fetchBrandEMIMasterDataFromHost() {
        iDialog?.showProgress()
        var brandEMIMasterISOData: IsoDataWriter? = null
        //region==============================Creating ISO Packet For BrandEMIMasterData Request:-
        runBlocking(Dispatchers.IO) {
            CreateBrandEMIPacket(field57RequestData) {
                brandEMIMasterISOData = it
            }
        }
        //endregion

        //region==============================Host Hit To Fetch BrandEMIMaster Data:-
        GlobalScope.launch(Dispatchers.IO) {
            if (brandEMIMasterISOData != null) {
                val byteArrayRequest = brandEMIMasterISOData?.generateIsoByteRequest()
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
                        val brandEMIMasterData =
                            responseIsoData.isoMap[57]?.parseRaw2String().toString()

                        if (responseCode == "00") {
                            ROCProviderV2.incrementFromResponse(
                                ROCProviderV2.getRoc(AppPreference.getBankCode()).toString(),
                                AppPreference.getBankCode()
                            )
                            GlobalScope.launch(Dispatchers.Main) {
                                //Processing BrandEMIMasterData:-
                                stubbingBrandEMIMasterDataToList(brandEMIMasterData, hostMsg)
                                iDialog?.hideProgress()
                            }
                        } else {
                            ROCProviderV2.incrementFromResponse(
                                ROCProviderV2.getRoc(AppPreference.getBankCode()).toString(),
                                AppPreference.getBankCode()
                            )
                            GlobalScope.launch(Dispatchers.Main) {
                                iDialog?.hideProgress()
                                iDialog?.alertBoxWithAction(null, null,
                                    getString(R.string.error), hostMsg,
                                    false, getString(R.string.positive_button_ok),
                                    { parentFragmentManager.popBackStackImmediate() }, {})
                            }
                        }
                    } else {
                        ROCProviderV2.incrementFromResponse(
                            ROCProviderV2.getRoc(AppPreference.getBankCode()).toString(),
                            AppPreference.getBankCode()
                        )
                        GlobalScope.launch(Dispatchers.Main) {
                            iDialog?.hideProgress()
                            iDialog?.alertBoxWithAction(null, null,
                                getString(R.string.error), result,
                                false, getString(R.string.positive_button_ok),
                                { parentFragmentManager.popBackStackImmediate() }, {})
                        }
                    }
                }, {})
            }
        }
        //endregion
    }
    //endregion

    //region=================================Stubbing BrandEMI Master Data and Display in List:-
    private fun stubbingBrandEMIMasterDataToList(brandEMIMasterData: String, hostMsg: String) {
        GlobalScope.launch(Dispatchers.Main) {
            if (!TextUtils.isEmpty(brandEMIMasterData)) {
                val dataList = parseDataListWithSplitter("|", brandEMIMasterData)
                if (dataList.isNotEmpty()) {
                    moreDataFlag = dataList[0]
                    totalRecord = dataList[1]
                    brandTimeStamp = dataList[2]
                    brandCategoryUpdatedTimeStamp = dataList[3]
                    issuerTAndCTimeStamp = dataList[4]
                    brandTAndCTimeStamp = dataList[5]

                    //Store DataList in Temporary List and remove first 5 index values to get sublist from 5th index till dataList size
                    // and iterate further on record data only:-
                    var tempDataList = mutableListOf<String>()
                    tempDataList = dataList.subList(6, dataList.size)
                    for (i in tempDataList.indices) {
                        if (!TextUtils.isEmpty(tempDataList[i]))
                            brandEmiMasterDataList.add(BrandEMIMasterDataModal(tempDataList[i]))
                    }
                    //Notify RecyclerView DataList on UI:-
                    brandEMIMasterCategoryAdapter.refreshAdapterList(
                        brandEmiMasterDataList,
                        moreDataFlag
                    )

                    //Refresh Field57 request value for Pagination if More Record Flag is True:-
                    if (moreDataFlag == "1") {
                        field57RequestData = "${EMIRequestType.BRAND_DATA.requestType}^$totalRecord"
                    }
                }
            } else {
                GlobalScope.launch(Dispatchers.Main) {
                    iDialog?.hideProgress()
                    iDialog?.alertBoxWithAction(null, null,
                        getString(R.string.error), hostMsg,
                        false, getString(R.string.positive_button_ok),
                        {}, {})
                }
            }
        }

    }
    //endregion

    //region===========================SetUp RecyclerView :-
    private fun setUpRecyclerView() {
        binding?.brandEmiMasterRV?.apply {
            layoutManager = LinearLayoutManager(context)
            itemAnimator = DefaultItemAnimator()
            adapter = brandEMIMasterCategoryAdapter
        }
    }
    //endregion

    //region=============================Load More Button CallBack Function:-
    private fun onItemClick(position: Int) {
        try {
            if (brandEmiMasterDataList.size == position) {
                Log.d("LoadMore Position:- ", position.toString())
                fetchBrandEMIMasterDataFromHost()
            } else {
                Log.d("Navigate To Category:- ", position.toString())
                val issuerTAndCData =
                    runBlocking(Dispatchers.IO) { IssuerTAndCTable.getAllIssuerTAndCData() }
                val brandTAndCData =
                    runBlocking(Dispatchers.IO) { BrandTAndCTable.getAllBrandTAndCData() }
                iDialog?.showProgress()
                if (issuerTAndCData.isEmpty() || brandTAndCData.isEmpty() || !matchHostAndDBData()) {
                    getIssuerTAndCData { issuerTCDataSaved ->
                        if (issuerTCDataSaved) {
                            getBrandTAndCData { brandTCDataSaved ->
                                if (brandTCDataSaved) {
                                    saveBrandMasterTimeStampsData { brandMasterDataTimeStampsSaved ->
                                        GlobalScope.launch(Dispatchers.Main) {
                                            iDialog?.hideProgress()
                                            navigateToBrandEMISubCategoryFragment(position)
                                        }
                                    }
                                } else {
                                    GlobalScope.launch(Dispatchers.Main) {
                                        iDialog?.hideProgress()
                                        showSomethingWrongPopUp()
                                    }
                                }
                            }
                        } else {
                            GlobalScope.launch(Dispatchers.Main) {
                                iDialog?.hideProgress()
                                showSomethingWrongPopUp()
                            }
                        }
                    }
                } else {
                    GlobalScope.launch(Dispatchers.Main) {
                        iDialog?.hideProgress()
                        navigateToBrandEMISubCategoryFragment(position)
                    }
                }
            }
        } catch (ex: IndexOutOfBoundsException) {
            ex.printStackTrace()
        }
    }
    //endregion

    //region=====================TAndC not Loaded Properly Pop-Up:-
    private fun showSomethingWrongPopUp() {
        (activity as MainActivity).alertBoxWithAction(null,
            null,
            getString(R.string.error),
            getString(R.string.issuer_brand_tandc_loading_error),
            false,
            getString(R.string.positive_button_ok),
            {},
            {})
    }
    //endregion

    //region==================Get Issuer TAndC Data:-
    private fun getIssuerTAndCData(cb: (Boolean) -> Unit) {
        if (checkInternetConnection()) {
            Log.d("Bank EMI Clicked:- ", "Clicked")
            GenericEMIIssuerTAndC { issuerTermsAndConditionData, issuerHostResponseCodeAndMsg ->
                val issuerTAndCData = issuerTermsAndConditionData.first
                val responseBool = issuerTermsAndConditionData.second
                if (issuerTAndCData.isNotEmpty() && responseBool) {
                    //region================Insert IssuerTAndC and Brand TAndC in DB:-
                    //Issuer TAndC Inserting:-
                    for (i in 0 until issuerTAndCData.size) {
                        val issuerModel = IssuerTAndCTable()
                        if (!TextUtils.isEmpty(issuerTAndCData[i])) {
                            val splitData = parseDataListWithSplitter(
                                SplitterTypes.CARET.splitter,
                                issuerTAndCData[i]
                            )

                            if (splitData.size > 2) {
                                issuerModel.issuerId = splitData[0]
                                issuerModel.headerTAndC = splitData[1]
                                issuerModel.footerTAndC = splitData[2]
                            } else {
                                issuerModel.issuerId = splitData[0]
                                issuerModel.headerTAndC = splitData[1]
                            }

                            runBlocking(Dispatchers.IO) {
                                IssuerTAndCTable.performOperation(issuerModel)
                            }
                        }
                    }
                    cb(true)
                } else
                    cb(false)
            }
        } else {
            VFService.showToast(getString(R.string.no_internet_available_please_check_your_internet))
        }
    }
    //endregion

    //region==========================Get Brand TAndC Data:-
    private fun getBrandTAndCData(cb: (Boolean) -> Unit) {
        GenericBrandTAndC(EMIRequestType.BRAND_T_AND_C.requestType) { brandTAndCData, hostResponseData ->
            if (brandTAndCData.first.isNotEmpty()) {
                for (i in 0 until brandTAndCData.first.size) {
                    val brandModel = BrandTAndCTable()
                    if (!TextUtils.isEmpty(brandTAndCData.first[i])) {
                        val splitData = parseDataListWithSplitter(
                            SplitterTypes.CARET.splitter,
                            brandTAndCData.first[i]
                        )
                        brandModel.brandId = splitData[0]
                        brandModel.brandTAndC = splitData[1]
                        runBlocking(Dispatchers.IO) {
                            BrandTAndCTable.performOperation(brandModel)
                        }
                    }
                }
                cb(true)
            } else
                cb(false)
        }
    }
    //endregion

    //region==============Save Brand Master Data TimeStamps:-
    private fun saveBrandMasterTimeStampsData(cb: (Boolean) -> Unit) {
        runBlocking(Dispatchers.IO) { BrandEMIMasterTimeStamps.clear() }
        val model = BrandEMIMasterTimeStamps()
        model.brandTimeStamp = brandTimeStamp ?: ""
        model.brandCategoryUpdatedTimeStamp = brandCategoryUpdatedTimeStamp ?: ""
        model.issuerTAndCTimeStamp = issuerTAndCTimeStamp ?: ""
        model.brandTAndCTimeStamp = brandTAndCTimeStamp ?: ""
        runBlocking(Dispatchers.IO) { BrandEMIMasterTimeStamps.performOperation(model) }
        cb(true)
    }
    //endregion

    //region===================================Navigate Fragment to BrandEMISubCategory Fragment:-
    private fun navigateToBrandEMISubCategoryFragment(position: Int) {
        if (checkInternetConnection()) {
            val brandData = parseDataListWithSplitter(
                SplitterTypes.CARET.splitter,
                brandEmiMasterDataList[position].recordData
            )
            //region=========Adding BrandID , BrandName and Brand ReservedValues in BrandEMIDataModal:-
            brandEMIDataModel.setBrandID(brandData[0])
            brandEMIDataModel.setBrandName(brandData[1])
            brandEMIDataModel.setBrandReservedValue(brandData[2])
            //endregion
            (activity as MainActivity).transactFragment(BrandEMISubCategoryFragment().apply {
                arguments = Bundle().apply {
                    putString("categoryUpdatedTimeStamp", brandCategoryUpdatedTimeStamp)
                    putString("brandTimeStamp", brandTimeStamp)
                    putSerializable("type", action)
                    putSerializable("modal", brandEMIDataModel)
                }
            })
        } else {
            VFService.showToast(getString(R.string.no_internet_available_please_check_your_internet))
        }
    }
    //endregion

    //region=======================Check Whether we got Updated Data from Host or to use Previous BrandEMIMaster Store Data:-
    private fun matchHostAndDBData(): Boolean {
        val timeStampsData =
            runBlocking(Dispatchers.IO) { BrandEMIMasterTimeStamps.getAllBrandEMIMasterDataTimeStamps() }
        var isDataMatch = false

        if (!TextUtils.isEmpty(timeStampsData[0].brandTAndCTimeStamp) &&
            !TextUtils.isEmpty(timeStampsData[0].issuerTAndCTimeStamp)
        ) {
            isDataMatch = issuerTAndCTimeStamp == timeStampsData[0].issuerTAndCTimeStamp &&
                    brandTAndCTimeStamp == timeStampsData[0].brandTAndCTimeStamp
        }
        return isDataMatch
    }
    //endregion

    override fun onDetach() {
        super.onDetach()
        iDialog = null
    }
}

internal class BrandEMIMasterCategoryAdapter(
    private var dataList: MutableList<BrandEMIMasterDataModal>?,
    private var moreDataAvailableToLoad: String?,
    private val onItemClick: (Int) -> Unit
) :
    RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    //region========================Below Variables are for Inserting Footer View after last item in recyclerview items:-
    private val TYPE_FOOTER = 1
    private val TYPE_ITEM = 2
    //endregion

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            TYPE_ITEM -> {
                val binding: ItemBrandEmiMasterBinding = ItemBrandEmiMasterBinding.inflate(
                    LayoutInflater.from(parent.context), parent, false
                )
                BrandEMIMasterViewHolder(binding)
            }
            TYPE_FOOTER -> {
                val binding: ItemBrandEmiFooterBinding = ItemBrandEmiFooterBinding.inflate(
                    LayoutInflater.from(parent.context), parent, false
                )
                FooterViewHolder(binding)
            }
            else -> null!!
        }
    }

    override fun getItemCount(): Int {
        return dataList?.size?.plus(1) ?: 0
    }

    override fun getItemViewType(position: Int): Int {
        if (position == dataList?.size) {
            return TYPE_FOOTER
        }
        return TYPE_ITEM
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, p1: Int) {
        if (holder is BrandEMIMasterViewHolder) {
            //region==============Parse BrandEMIMaster Record Data and set Brand Name to TextView:-
            /*
            Below parseDataWithSplitter gives following data:-
            0 index -> Brand ID
            1 index -> Brand Name
            2 index -> Mobile Number Capture Flag
            3 index -> Bill Invoice Capture Flag
             */
            if (!TextUtils.isEmpty(dataList?.get(p1)?.recordData)) {
                val brandData = parseDataListWithSplitter(
                    SplitterTypes.CARET.splitter,
                    dataList?.get(p1)?.recordData ?: ""
                )
                holder.binding.tvBrandMasterName.text = brandData[1]
            }
            //endregion
        } else if (holder is FooterViewHolder) {
            if (moreDataAvailableToLoad == "1")
                holder.binding.loadMoreBT.visibility = View.VISIBLE
            else
                holder.binding.loadMoreBT.visibility = View.GONE
        }
    }

    inner class BrandEMIMasterViewHolder(val binding: ItemBrandEmiMasterBinding) :
        RecyclerView.ViewHolder(binding.root) {
        init {
            binding.brandEmiMasterParent.setOnClickListener { onItemClick(adapterPosition) }
        }
    }

    inner class FooterViewHolder(val binding: ItemBrandEmiFooterBinding) :
        RecyclerView.ViewHolder(binding.root) {
        init {
            binding.loadMoreBT.setOnClickListener { onItemClick(adapterPosition) }
        }
    }

    //region==========================Below Method is used to refresh Adapter New Data and Also
    // MoreDataAvailable Variable to check whether we need to show Footer Load More Button or not:-
    fun refreshAdapterList(
        refreshList: MutableList<BrandEMIMasterDataModal>,
        moreDataFlagRefreshValue: String
    ) {
        this.dataList = refreshList
        this.moreDataAvailableToLoad = moreDataFlagRefreshValue
        notifyDataSetChanged()
    }
    //endregion
}

//region=============================Brand EMI Master Category Data Modal==========================
data class BrandEMIMasterDataModal(var recordData: String)
//endregion