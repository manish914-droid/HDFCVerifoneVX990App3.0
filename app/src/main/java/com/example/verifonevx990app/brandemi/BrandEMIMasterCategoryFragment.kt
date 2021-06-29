package com.example.verifonevx990app.brandemi

import android.content.Context
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.Editable
import android.text.TextUtils
import android.text.TextWatcher
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.DefaultItemAnimator
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.verifonevx990app.R
import com.example.verifonevx990app.bankemi.GenericEMIIssuerTAndC
import com.example.verifonevx990app.databinding.BrandEmiMasterCategoryFragmentBinding
import com.example.verifonevx990app.databinding.ItemBrandEmiMasterBinding
import com.example.verifonevx990app.main.EMIRequestType
import com.example.verifonevx990app.main.MainActivity
import com.example.verifonevx990app.main.SplitterTypes
import com.example.verifonevx990app.realmtables.BrandEMIMasterTimeStamps
import com.example.verifonevx990app.realmtables.BrandTAndCTable
import com.example.verifonevx990app.realmtables.EDashboardItem
import com.example.verifonevx990app.realmtables.IssuerTAndCTable
import com.example.verifonevx990app.vxUtils.*
import com.google.gson.Gson
import kotlinx.coroutines.*
import java.util.*

/**
This is a Brand EMI Master Category Data Fragment
Here we are Fetching Master Category Data(Brand Data) From Host and Displaying on UI:-
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
    private var isDataMatch = false
    private val brandEMIDataModel by lazy { BrandEMIDataModal() }
    private val handler = Handler(Looper.getMainLooper())
    private var runnable: Runnable? = null
    private var delayTime: Long = 0L
    private val brandEMIMasterCategoryAdapter by lazy {
        BrandEMIMasterCategoryAdapter(
            brandEmiMasterDataList,
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
        if (action as EDashboardItem == EDashboardItem.BRAND_EMI_CATALOGUE) {
            binding?.subHeaderView?.subHeaderText?.text = getString(R.string.brandEmiCatalogue)
            binding?.subHeaderView?.headerImage?.setImageResource(R.drawable.ic_brand_emi_catalogue)
        } else {
            binding?.subHeaderView?.subHeaderText?.text = getString(R.string.brandEmi)
            binding?.subHeaderView?.headerImage?.setImageResource(R.drawable.ic_brand_emi_sub_header_logo)
        }
        binding?.subHeaderView?.backImageButton?.setOnClickListener {
            hideSoftKeyboard(requireActivity())
            parentFragmentManager.popBackStackImmediate()
        }
        delayTime = timeOutTime()
        //(activity as MainActivity).showBottomNavigationBar(isShow = false)
        empty_view_placeholder = view.findViewById(R.id.empty_view_placeholder)

        //Below we are assigning initial request value of Field57 in BrandEMIMaster Data Host Hit:-
        field57RequestData = "${EMIRequestType.BRAND_DATA.requestType}^0"

        //Initial SetUp of RecyclerView List with Empty Data , After Fetching Data from Host we will notify List:-
        setUpRecyclerView()
        brandEmiMasterDataList.clear()

        val issuerTAndCData =
            runBlocking(Dispatchers.IO) { IssuerTAndCTable.getAllIssuerTAndCData() }
        Log.d("IssuerTC:- ", Gson().toJson(issuerTAndCData))

        //Method to Fetch BrandEMIMasterData:-
        fetchBrandEMIMasterDataFromHost()

        //region================Search EditText TextChangeListener event:-
        binding?.brandSearchET?.addTextChangedListener(object : TextWatcher {
            override fun onTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {}
            override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {}
            override fun afterTextChanged(p0: Editable?) {
                if (TextUtils.isEmpty(p0.toString())) {
                    brandEMIMasterCategoryAdapter.refreshAdapterList(brandEmiMasterDataList)
                    binding?.brandEmiMasterRV?.smoothScrollToPosition(0)
                    hideSoftKeyboard(requireActivity())
                }
            }
        })
        //endregion

        //region=================Search Button onClick event:-
        binding?.searchButton?.setOnClickListener {
            if (!TextUtils.isEmpty(binding?.brandSearchET?.text?.toString())) {
                iDialog?.showProgress(getString(R.string.searchingBrand))
                getSearchedBrands(binding?.brandSearchET?.text?.trim()?.toString())
                hideSoftKeyboard(requireActivity())
            } else
                VFService.showToast(getString(R.string.please_enter_brand_name_to_search))
        }
        //endregion
    }

    //region===================Get Searched Results from Brand List:-
    private fun getSearchedBrands(searchText: String?) {
        val searchedDataList = mutableListOf<BrandEMIMasterDataModal>()
        lifecycleScope.launch(Dispatchers.Default) {
            if (!TextUtils.isEmpty(searchText)) {
                for (i in 0 until brandEmiMasterDataList.size) {
                    val brandData = brandEmiMasterDataList[i]
                    //check whether brand name contains letter which is inserted in search box:-
                    if (brandData.brandName.toLowerCase(Locale.ROOT).trim()
                            .contains(searchText?.toLowerCase(Locale.ROOT)?.trim()!!)
                    )
                        searchedDataList.add(
                            BrandEMIMasterDataModal(
                                brandData.brandID, brandData.brandName,
                                brandData.mobileNumberBillNumberFlag
                            )
                        )
                }
                withContext(Dispatchers.Main) {
                    brandEMIMasterCategoryAdapter.refreshAdapterList(searchedDataList)
                    iDialog?.hideProgress()
                }
            } else
                withContext(Dispatchers.Main) {
                    iDialog?.hideProgress()
                }
        }
    }
    //endregion

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

        startTimeOut()

        //region==============================Host Hit To Fetch BrandEMIMaster Data:-
        lifecycleScope.launch(Dispatchers.IO) {
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
                            //Processing BrandEMIMasterData:-
                            stubbingBrandEMIMasterDataToList(brandEMIMasterData, hostMsg)

                        } else {
                            ROCProviderV2.incrementFromResponse(
                                ROCProviderV2.getRoc(AppPreference.getBankCode()).toString(),
                                AppPreference.getBankCode()
                            )
                            lifecycleScope.launch(Dispatchers.Main) {
                                iDialog?.hideProgress()
                                parentFragmentManager.popBackStackImmediate()
                                /*iDialog?.alertBoxWithAction(null, null,
                                    getString(R.string.error), hostMsg,
                                    false, getString(R.string.positive_button_ok),
                                    { parentFragmentManager.popBackStackImmediate() }, {})*/
                            }
                        }
                    } else {
                        ROCProviderV2.incrementFromResponse(
                            ROCProviderV2.getRoc(AppPreference.getBankCode()).toString(),
                            AppPreference.getBankCode()
                        )
                        lifecycleScope.launch(Dispatchers.Main) {
                            iDialog?.hideProgress()
                            parentFragmentManager.popBackStackImmediate()
                            /*iDialog?.alertBoxWithAction(null, null,
                                getString(R.string.error), result,
                                false, getString(R.string.positive_button_ok),
                                { parentFragmentManager.popBackStackImmediate() }, {})*/
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
        lifecycleScope.launch(Dispatchers.Default) {
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
                        if (!TextUtils.isEmpty(tempDataList[i])) {
                            /* Below parseDataWithSplitter gives following data:-
                                 0 index -> Brand ID
                                 1 index -> Brand Name
                                 2 index -> Mobile Number Capture Flag / Bill Invoice Capture Flag
                               */
                            if (!TextUtils.isEmpty(tempDataList[i])) {
                                val brandData = parseDataListWithSplitter(
                                    SplitterTypes.CARET.splitter,
                                    tempDataList[i]
                                )
                                brandEmiMasterDataList.add(
                                    BrandEMIMasterDataModal(
                                        brandData[0],
                                        brandData[1],
                                        brandData[2]
                                    )
                                )
                            }
                        }
                    }
                    withContext(Dispatchers.Main) {
                        cancelTimeOut()
                    }
                    //Refresh Field57 request value for Pagination if More Record Flag is True:-
                    if (moreDataFlag == "1") {
                        field57RequestData = "${EMIRequestType.BRAND_DATA.requestType}^$totalRecord"
                        Log.d("Field57UpdateRequest:- ", field57RequestData.toString())
                        fetchBrandEMIMasterDataFromHost()
                    } else {
                        //Notify RecyclerView DataList on UI:-
                        withContext(Dispatchers.Main) {
                            iDialog?.hideProgress()
                            Log.d("Brands Data:- ", Gson().toJson(brandEmiMasterDataList))
                            brandEMIMasterCategoryAdapter.refreshAdapterList(
                                brandEmiMasterDataList
                            )
                        }
                    }
                } else {
                    withContext(Dispatchers.Main) {
                        iDialog?.hideProgress()
                        /*iDialog?.alertBoxWithAction(null, null,
                            getString(R.string.error), hostMsg,
                            false, getString(R.string.positive_button_ok),
                            {}, {})*/
                    }
                }
            } else {
                withContext(Dispatchers.Main) {
                    iDialog?.hideProgress()
                    /*iDialog?.alertBoxWithAction(null, null,
                        getString(R.string.error), hostMsg,
                        false, getString(R.string.positive_button_ok),
                        {}, {})*/
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

    //region=============================On Brand Item click CallBack Function:-
    private fun onItemClick(position: Int) {
        try {
            if (position > -1) {
                Log.d("Navigate To Category:- ", position.toString())
                val issuerTAndCData =
                    runBlocking(Dispatchers.IO) { IssuerTAndCTable.getAllIssuerTAndCData() }
                val brandTAndCData =
                    runBlocking(Dispatchers.IO) { BrandTAndCTable.getAllBrandTAndCData() }
                iDialog?.showProgress()
                if (issuerTAndCData?.isEmpty() == true || brandTAndCData.isEmpty() || !matchHostAndDBData()) {
                    getIssuerTAndCData { issuerTCDataSaved ->
                        if (issuerTCDataSaved) {
                            getBrandTAndCData { brandTCDataSaved ->
                                if (brandTCDataSaved) {
                                    saveBrandMasterTimeStampsData {
                                        lifecycleScope.launch(Dispatchers.Main) {
                                            iDialog?.hideProgress()
                                            navigateToBrandEMISubCategoryFragment(position)
                                        }
                                    }
                                } else {
                                    lifecycleScope.launch(Dispatchers.Main) {
                                        iDialog?.hideProgress()
                                        showSomethingWrongPopUp()
                                    }
                                }
                            }
                        } else {
                            lifecycleScope.launch(Dispatchers.Main) {
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
            } else
                VFService.showToast("Something went wrong")
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
            val modal = brandEmiMasterDataList[position]
            //region=========Adding BrandID , BrandName and Brand ReservedValues in BrandEMIDataModal:-
            brandEMIDataModel.setBrandID(modal.brandID)
            brandEMIDataModel.setBrandName(modal.brandName)
            brandEMIDataModel.setBrandReservedValue(modal.mobileNumberBillNumberFlag)
            brandEMIDataModel.setDataTimeStampChangedOrNot(isDataMatch)
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

        if (!TextUtils.isEmpty(timeStampsData[0].brandTAndCTimeStamp) &&
            !TextUtils.isEmpty(timeStampsData[0].issuerTAndCTimeStamp)
        ) {
            isDataMatch = issuerTAndCTimeStamp == timeStampsData[0].issuerTAndCTimeStamp &&
                    brandTAndCTimeStamp == timeStampsData[0].brandTAndCTimeStamp
        }
        return isDataMatch
    }
    //endregion

    //region==============================Start TimeOut Handler:-
    fun startTimeOut() {
        runnable = object : Runnable {
            override fun run() {
                try {
                    Log.d("TimeOut:- ", "Loading Data Failed....")
                    lifecycleScope.launch(Dispatchers.Main) {
                        iDialog?.hideProgress()
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                } finally {
                    //also call the same runnable to call it at regular interval
                    handler.postDelayed(this, delayTime)
                }
            }
        }
        handler.post(runnable as Runnable)
    }
    //endregion

    //region==============================Cancel TimeOut Handler:-
    fun cancelTimeOut() = runnable?.let { handler.removeCallbacks(it) }

    //endregion

    override fun onDetach() {
        super.onDetach()
        iDialog = null
    }
}

internal class BrandEMIMasterCategoryAdapter(
    private var dataList: MutableList<BrandEMIMasterDataModal>?,
    private val onItemClick: (Int) -> Unit
) :
    RecyclerView.Adapter<BrandEMIMasterCategoryAdapter.BrandEMIMasterViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BrandEMIMasterViewHolder {
        val binding: ItemBrandEmiMasterBinding =
            ItemBrandEmiMasterBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return BrandEMIMasterViewHolder(binding)
    }

    private val brandEmiDataList: MutableList<BrandEMIMasterDataModal> = mutableListOf()

    init {
        if (dataList?.isNotEmpty() == true)
            brandEmiDataList.addAll(dataList!!)
    }

    override fun getItemCount(): Int {
        return brandEmiDataList.size
    }

    override fun onBindViewHolder(holder: BrandEMIMasterViewHolder, p1: Int) {
        //region==============Parse BrandEMIMaster Record Data and set Brand Name to TextView:-
        /* Below parseDataWithSplitter gives following data:-
         0 index -> Brand ID
         1 index -> Brand Name
         2 index -> Mobile Number Capture Flag
         3 index -> Bill Invoice Capture Flag*/

        if (!TextUtils.isEmpty(brandEmiDataList[p1].brandName)) {
            holder.binding.tvBrandMasterName.text = brandEmiDataList[p1].brandName
        }
        //endregion
    }

    inner class BrandEMIMasterViewHolder(val binding: ItemBrandEmiMasterBinding) :
        RecyclerView.ViewHolder(binding.root) {
        init {
            binding.brandEmiMasterParent.setOnClickListener { onItemClick(absoluteAdapterPosition) }
        }
    }

    //region==========================Below Method is used to refresh Adapter New Data:-
    fun refreshAdapterList(refreshList: MutableList<BrandEMIMasterDataModal>) {
        val diffCallback = BrandMasterUpdateDiffUtil(this.brandEmiDataList, refreshList)
        val diffResult = DiffUtil.calculateDiff(diffCallback)
        this.brandEmiDataList.clear()
        this.brandEmiDataList.addAll(refreshList)
        diffResult.dispatchUpdatesTo(this)
    }
    //endregion
}

//region=============================Brand EMI Master Category Data Modal==========================
data class BrandEMIMasterDataModal(
    var brandID: String,
    var brandName: String,
    var mobileNumberBillNumberFlag: String
)
//endregion


