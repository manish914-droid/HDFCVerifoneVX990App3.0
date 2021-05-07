package com.example.verifonevx990app.brandemi

import android.content.Context
import android.os.Bundle
import android.os.Parcelable
import android.text.TextUtils
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.DefaultItemAnimator
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.verifonevx990app.R
import com.example.verifonevx990app.databinding.FragmentBrandEmiSubCategoryBinding
import com.example.verifonevx990app.databinding.ItemBrandEmiSubCategoryBinding
import com.example.verifonevx990app.main.EMIRequestType
import com.example.verifonevx990app.main.MainActivity
import com.example.verifonevx990app.main.SplitterTypes
import com.example.verifonevx990app.vxUtils.*
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.parcelize.Parcelize
import java.util.*

/**
This is a Brand EMI Master Sub-Category Data Fragment
Here we are Fetching Master Sub-Category Data From Host on the basis of Brand Master Category Selection from Previous
Fragment Page and Displaying on UI:-
================Written By Ajay Thakur on 9th March 2021====================
 */
class BrandEMISubCategoryFragment : Fragment() {
    private var binding: FragmentBrandEmiSubCategoryBinding? = null
    private var iDialog: IDialog? = null
    private var brandEmiMasterSubCategoryDataList =
        mutableListOf<BrandEMIMasterSubCategoryDataModal>()
    private val action by lazy { arguments?.getSerializable("type") ?: "" }
    private var brandEMIDataModal: BrandEMIDataModal? = null
    private var field57RequestData: String? = null
    private var moreDataFlag = "0"
    private var totalRecord: String? = "0"
    private var perPageRecord: String? = "0"
    private val brandEMIMasterSubCategoryAdapter by lazy {
        BrandEMIMasterSubCategoryAdapter(
            brandEmiMasterSubCategoryDataList,
            ::onCategoryItemClick
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
        // Inflate the layout for this fragment
        binding = FragmentBrandEmiSubCategoryBinding.inflate(layoutInflater, container, false)
        return binding?.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding?.subHeaderView?.subHeaderText?.text = getString(R.string.brand_emi_sub_category)
        binding?.subHeaderView?.backImageButton?.setOnClickListener { parentFragmentManager.popBackStackImmediate() }
        (activity as MainActivity).showBottomNavigationBar(isShow = false)
        brandEMIDataModal = arguments?.getSerializable("modal") as BrandEMIDataModal
        Log.d("BrandID:- ", brandEMIDataModal?.getBrandID() ?: "")

        //Below we are assigning initial request value of Field57 in BrandEMIMaster Data Host Hit:-
        field57RequestData =
            "${EMIRequestType.BRAND_SUB_CATEGORY.requestType}^0^${brandEMIDataModal?.getBrandID()}"

        //Initial SetUp of RecyclerView List with Empty Data , After Fetching Data from Host we will notify List:-
        setUpRecyclerView()
        brandEmiMasterSubCategoryDataList.clear()
        fetchBrandEMIMasterSubCategoryDataFromHost()
    }

    //region===============================Hit Host to Fetch BrandEMIMasterSubCategory Data:-
    private fun fetchBrandEMIMasterSubCategoryDataFromHost() {
        iDialog?.showProgress()
        Log.d("BrandEMISubCategory:- ", field57RequestData.toString())
        var brandEMIMasterSubCategoryISOData: IsoDataWriter? = null
        //region==============================Creating ISO Packet For BrandEMIMasterSubCategoryData Request:-
        runBlocking(Dispatchers.IO) {
            CreateBrandEMIPacket(field57RequestData) {
                brandEMIMasterSubCategoryISOData = it
            }
        }
        //endregion

        //region==============================Host Hit To Fetch BrandEMIMasterSubCategory Data:-
        GlobalScope.launch(Dispatchers.IO) {
            if (brandEMIMasterSubCategoryISOData != null) {
                val byteArrayRequest = brandEMIMasterSubCategoryISOData?.generateIsoByteRequest()
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
                        val brandEMIMasterSubCategoryData =
                            responseIsoData.isoMap[57]?.parseRaw2String().toString()

                        if (responseCode == "00") {
                            ROCProviderV2.incrementFromResponse(
                                ROCProviderV2.getRoc(AppPreference.getBankCode()).toString(),
                                AppPreference.getBankCode()
                            )
                            GlobalScope.launch(Dispatchers.Main) {
                                //Processing BrandEMIMasterSubCategoryData:-
                                stubbingBrandEMIMasterSubCategoryDataToList(
                                    brandEMIMasterSubCategoryData,
                                    hostMsg
                                )
                            }
                        } else {
                            ROCProviderV2.incrementFromResponse(
                                ROCProviderV2.getRoc(AppPreference.getBankCode()).toString(),
                                AppPreference.getBankCode()
                            )
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

    //region=================================Stubbing BrandEMI Master SubCategory Data and Display in List:-
    private fun stubbingBrandEMIMasterSubCategoryDataToList(
        brandEMIMasterSubCategoryData: String,
        hostMsg: String
    ) {
        GlobalScope.launch(Dispatchers.Main) {
            if (!TextUtils.isEmpty(brandEMIMasterSubCategoryData)) {
                val dataList = parseDataListWithSplitter("|", brandEMIMasterSubCategoryData)
                if (dataList.isNotEmpty()) {
                    moreDataFlag = dataList[0]
                    perPageRecord = dataList[1]
                    totalRecord =
                        (totalRecord?.toInt()?.plus(perPageRecord?.toInt() ?: 0)).toString()
                    //Store DataList in Temporary List and remove first 2 index values to get sublist from 2nd index till dataList size
                    // and iterate further on record data only:-
                    var tempDataList = mutableListOf<String>()
                    tempDataList = dataList.subList(2, dataList.size)
                    for (i in tempDataList.indices) {
                        //Below we are splitting Data from tempDataList to extract brandID , categoryID , parentCategoryID , categoryName:-
                        if (!TextUtils.isEmpty(tempDataList[i])) {
                            val splitData = parseDataListWithSplitter(
                                SplitterTypes.CARET.splitter,
                                tempDataList[i]
                            )
                            brandEmiMasterSubCategoryDataList.add(
                                BrandEMIMasterSubCategoryDataModal(
                                    splitData[0], splitData[1],
                                    splitData[2], splitData[3]
                                )
                            )
                        }
                    }

                    //Notify RecyclerView DataList on UI with Category Data that has ParentCategoryID == 0 && BrandID = selected brandID :-
                    val totalDataList = brandEmiMasterSubCategoryDataList
                    Log.d("TotalDataList:- ", Gson().toJson(totalDataList))
                    brandEmiMasterSubCategoryDataList = brandEmiMasterSubCategoryDataList.filter {
                        it.parentCategoryID == "0" && it.brandID == brandEMIDataModal?.getBrandID()
                    }
                            as MutableList<BrandEMIMasterSubCategoryDataModal>
                    if (brandEmiMasterSubCategoryDataList.isNotEmpty()) {
                        brandEMIMasterSubCategoryAdapter.refreshAdapterList(
                            brandEmiMasterSubCategoryDataList
                        )
                    }

                    //Refresh Field57 request value for Pagination if More Record Flag is True:-
                    if (moreDataFlag == "1") {
                        field57RequestData =
                            "${EMIRequestType.BRAND_SUB_CATEGORY.requestType}^$totalRecord^${
                                brandEMIDataModal?.getBrandID()
                            }"
                        fetchBrandEMIMasterSubCategoryDataFromHost()
                        Log.d("FullDataList:- ", brandEmiMasterSubCategoryDataList.toString())
                    } else {
                        iDialog?.hideProgress()
                        if (brandEmiMasterSubCategoryDataList.isEmpty()) {
                            navigateToProductPage(isSubCategoryItem = false, -1)
                        }
                    }
                }
            } else {
                GlobalScope.launch(Dispatchers.Main) {
                    iDialog?.hideProgress()
                    /* iDialog?.alertBoxWithAction(null, null,
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
        binding?.brandEmiMasterSubCategoryRV?.apply {
            layoutManager = LinearLayoutManager(context)
            itemAnimator = DefaultItemAnimator()
            adapter = brandEMIMasterSubCategoryAdapter
        }
    }
    //endregion

    //region==========================Perform Click Event on Category Item Click:-
    private fun onCategoryItemClick(position: Int) {
        try {
            Log.d("CategoryName:- ", brandEmiMasterSubCategoryDataList[position].categoryName)
            val childFilteredList =
                brandEmiMasterSubCategoryDataList.filter { brandEmiMasterSubCategoryDataList[position].categoryID == it.parentCategoryID }
                        as MutableList<BrandEMIMasterSubCategoryDataModal>?
            Log.d("Data:- ", Gson().toJson(brandEmiMasterSubCategoryDataList))
            if (position > -1 && childFilteredList?.isNotEmpty() == true) {
                navigateToBrandEMIDataByCategoryIDPage(position, true)
            } else
                navigateToProductPage(isSubCategoryItem = true, position)
        } catch (ex: IndexOutOfBoundsException) {
            ex.printStackTrace()
        }
    }
    //endregion

    //region==============================Navigate to BrandEMIDataByCategoryID Page:-
    private fun navigateToBrandEMIDataByCategoryIDPage(
        position: Int,
        isSubCategoryItem: Boolean = false
    ) {
        if (checkInternetConnection()) {
            //region==========Adding BrandEMISubCategoryID , CategoryName in brandEMIDataModal:-
            if (isSubCategoryItem) {
                brandEMIDataModal?.setCategoryID(brandEmiMasterSubCategoryDataList[position].categoryID)
                brandEMIDataModal?.setCategoryName(brandEmiMasterSubCategoryDataList[position].categoryName)
            }
            //endregion

            (activity as MainActivity).transactFragment(BrandEMIDataByCategoryID().apply {
                arguments = Bundle().apply {
                    putSerializable("modal", brandEMIDataModal)
                    putBoolean("isSubCategoryItemPresent", isSubCategoryItem)
                    putParcelableArrayList(
                        "subCategoryData",
                        brandEmiMasterSubCategoryDataList as ArrayList<out Parcelable>
                    )
                    putSerializable("type", action)
                }
            })
        } else {
            VFService.showToast(getString(R.string.no_internet_available_please_check_your_internet))
        }
    }
    //endregion

    //region===================================Navigate Controller To Product Page:-
    private fun navigateToProductPage(isSubCategoryItem: Boolean = false, position: Int) {
        if (checkInternetConnection()) {
            //region===================Adding CategoryID , CategoryName:-
            if (isSubCategoryItem && position > -1) {
                brandEMIDataModal?.setCategoryID(brandEmiMasterSubCategoryDataList[position].categoryID)
                brandEMIDataModal?.setCategoryName(brandEmiMasterSubCategoryDataList[position].categoryName)
            } else {
                brandEMIDataModal?.setCategoryID("")
                brandEMIDataModal?.setCategoryName("")
            }
            //endregion
            (activity as MainActivity).transactFragment(BrandEMIProductFragment().apply {
                arguments = Bundle().apply {
                    putBoolean("isSubCategoryItemPresent", isSubCategoryItem)
                    putSerializable("modal", brandEMIDataModal)
                    putSerializable("type", action)
                }
            })
        } else {
            VFService.showToast(getString(R.string.no_internet_available_please_check_your_internet))
        }
    }
    //endregion

    override fun onDetach() {
        super.onDetach()
        iDialog = null
    }
}

internal class BrandEMIMasterSubCategoryAdapter(
    private var dataList: MutableList<BrandEMIMasterSubCategoryDataModal>?,
    private val onCategoryItemClick: (Int) -> Unit
) :
    RecyclerView.Adapter<BrandEMIMasterSubCategoryAdapter.BrandEMIMasterSubCategoryViewHolder>() {

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): BrandEMIMasterSubCategoryViewHolder {
        val binding: ItemBrandEmiSubCategoryBinding = ItemBrandEmiSubCategoryBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return BrandEMIMasterSubCategoryViewHolder(binding)
    }

    override fun getItemCount(): Int {
        return dataList?.size ?: 0
    }

    override fun onBindViewHolder(holder: BrandEMIMasterSubCategoryViewHolder, p1: Int) {
        holder.binding.tvBrandSubCategoryName.text = dataList?.get(p1)?.categoryName ?: ""
    }

    inner class BrandEMIMasterSubCategoryViewHolder(val binding: ItemBrandEmiSubCategoryBinding) :
        RecyclerView.ViewHolder(binding.root) {
        init {
            binding.brandEmiMasterSubCategoryParent.setOnClickListener {
                onCategoryItemClick(
                    adapterPosition
                )
            }
        }
    }

    //region==========================Below Method is used to refresh Adapter New Data and Also
    fun refreshAdapterList(refreshList: MutableList<BrandEMIMasterSubCategoryDataModal>) {
        this.dataList = refreshList
        notifyDataSetChanged()
    }
    //endregion
}

//region=============================Brand EMI Master Category Data Modal==========================
@Parcelize
data class BrandEMIMasterSubCategoryDataModal(
    var brandID: String,
    var categoryID: String,
    var parentCategoryID: String,
    var categoryName: String
) : Parcelable
//endregion