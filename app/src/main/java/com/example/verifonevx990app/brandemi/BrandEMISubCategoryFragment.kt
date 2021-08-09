
package com.example.verifonevx990app.brandemi

import android.annotation.SuppressLint
import android.content.Context
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.Parcelable
import android.text.Editable
import android.text.TextUtils
import android.text.TextWatcher
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.DefaultItemAnimator
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.verifonevx990app.R
import com.example.verifonevx990app.databinding.FragmentBrandEmiSubCategoryBinding
import com.example.verifonevx990app.databinding.ItemBrandEmiSubCategoryBinding
import com.example.verifonevx990app.init.DashboardFragment
import com.example.verifonevx990app.main.EMIRequestType
import com.example.verifonevx990app.main.MainActivity
import com.example.verifonevx990app.main.SplitterTypes
import com.example.verifonevx990app.realmtables.BrandEMISubCategoryTable
import com.example.verifonevx990app.realmtables.EDashboardItem
import com.example.verifonevx990app.vxUtils.*
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
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
    private var brandEmiMasterSubCategoryDataListUpdate = mutableListOf<BrandEMIMasterSubCategoryDataModal>()
    private var brandEmiMasterSubCategoryDataList = mutableListOf<BrandEMIMasterSubCategoryDataModal>()
    private var brandEMIAllDataList = mutableListOf<BrandEMIMasterSubCategoryDataModal>()
    private val action by lazy { arguments?.getSerializable("type") ?: "" }
    private var brandEMIDataModal: BrandEMIDataModal? = null
    private var field57RequestData: String? = null
    private var moreDataFlag = "0"
    private var totalRecord: String? = "0"
    private var perPageRecord: String? = "0"
    private var brandIDFromPref: String? = null
    private val handler = Handler(Looper.getMainLooper())
    private var runnable: Runnable? = null
    private var delayTime: Long = 0L
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
        binding?.subHeaderView?.backImageButton?.setOnClickListener { parentFragmentManager.popBackStackImmediate() }
        if (action as EDashboardItem == EDashboardItem.BRAND_EMI_CATALOGUE) {
            binding?.subHeaderView?.subHeaderText?.text = getString(R.string.brandEmiCatalogue)
            binding?.subHeaderView?.headerImage?.setImageResource(R.drawable.ic_brand_emi_catalogue)
            binding?.subHeaderView?.headerHome?.visibility= View.VISIBLE
            binding?.subHeaderView?.headerHome?.setOnClickListener { (activity as MainActivity).transactFragment(
                DashboardFragment()
            ) }
        } else {
            binding?.subHeaderView?.subHeaderText?.text = getString(R.string.brandEmi)
            binding?.subHeaderView?.headerImage?.setImageResource(R.drawable.ic_brand_emi_sub_header_logo)
        }
        // delayTime = timeOutTime()
        //(activity as MainActivity).showBottomNavigationBar(isShow = false)
        brandEMIDataModal = arguments?.getSerializable("modal") as? BrandEMIDataModal
        Log.d("BrandID:- ", brandEMIDataModal?.brandID ?: "")

        //Save brandID in Shared Preference to use when user back from sub-category by id screen to sub-category screen for data load:-
        if (!TextUtils.isEmpty(AppPreference.getString(AppPreference.BrandID))) {
            brandIDFromPref = AppPreference.getString(AppPreference.BrandID)
        }

        //Initial SetUp of RecyclerView List with Empty Data , After Fetching Data from Host we will notify List:-
        setUpRecyclerView()
        AppPreference.saveString(AppPreference.BrandID, brandEMIDataModal?.brandID ?: "")
        brandEmiMasterSubCategoryDataList.clear()
        brandEMIAllDataList.clear()
        checkAndLoadDataFromSourceCondition()

        //region================Search EditText TextChangeListener event:-
        binding?.categorySearchET?.addTextChangedListener(object : TextWatcher {
            override fun onTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {}
            override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {}
            override fun afterTextChanged(p0: Editable?) {
                if (TextUtils.isEmpty(p0.toString())) {
                    binding?.emptyViewPlaceholder?.visibility = View.INVISIBLE
                    brandEmiMasterSubCategoryDataList = brandEmiMasterSubCategoryDataListUpdate
                    brandEMIMasterSubCategoryAdapter.refreshAdapterList(brandEmiMasterSubCategoryDataList)
                    binding?.brandEmiMasterSubCategoryRV?.smoothScrollToPosition(0)
                    hideSoftKeyboard(requireActivity())
                }
            }
        })
        //endregion

        //region=================Search Button onClick event:-
        binding?.searchButton?.setOnClickListener {
            if (!TextUtils.isEmpty(binding?.categorySearchET?.text?.toString())) {
                iDialog?.showProgress()
                getSearchedSubCategory(binding?.categorySearchET?.text?.trim()?.toString())
                hideSoftKeyboard(requireActivity())
            } else
                VFService.showToast(getString(R.string.please_enter_brand_name_to_search))
        }
        //endregion
    }

    //region===================Get Searched Results from Brand List:-
    private fun getSearchedSubCategory(searchText: String?) {
        val searchedDataList = mutableListOf<BrandEMIMasterSubCategoryDataModal>()
        lifecycleScope.launch(Dispatchers.Default) {
            if (!TextUtils.isEmpty(searchText)) {
                val loopLength = brandEMIAllDataList.size
                for (i in 0 until loopLength) {
                    val subCategoryData = brandEMIAllDataList[i]
                    //check whether sub category name contains letter which is inserted in search box:-
                    if (subCategoryData.categoryName.toLowerCase(Locale.ROOT).trim()
                            .contains(searchText?.toLowerCase(Locale.ROOT)?.trim()!!)
                    )
                        searchedDataList.add(
                            BrandEMIMasterSubCategoryDataModal(
                                subCategoryData.brandID, subCategoryData.categoryID,
                                subCategoryData.parentCategoryID, subCategoryData.categoryName
                            )
                        )
                }
                withContext(Dispatchers.Main) {
                    if(searchedDataList.size>0) {
                        brandEmiMasterSubCategoryDataListUpdate = brandEmiMasterSubCategoryDataList
                        brandEmiMasterSubCategoryDataList = searchedDataList
                        brandEMIMasterSubCategoryAdapter.refreshAdapterList(
                            brandEmiMasterSubCategoryDataList
                        )
                        iDialog?.hideProgress()
                    }else{
                        brandEmiMasterSubCategoryDataListUpdate = brandEmiMasterSubCategoryDataList
                        brandEMIMasterSubCategoryAdapter.refreshAdapterList(
                            searchedDataList
                        )
                        iDialog?.hideProgress()
                        binding?.emptyViewPlaceholder?.visibility = View.VISIBLE
                        // VFService.showToast(getString(R.string.no_data_found))
                    }

                }
            } else
                withContext(Dispatchers.Main) {
                    iDialog?.hideProgress()
                }
        }
    }
    //endregion

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
        lifecycleScope.launch(Dispatchers.IO) {
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
                            //Processing BrandEMIMasterSubCategoryData:-
                            stubbingBrandEMIMasterSubCategoryDataToList(
                                brandEMIMasterSubCategoryData,
                                hostMsg
                            )
                        } else {
                            ROCProviderV2.incrementFromResponse(
                                ROCProviderV2.getRoc(AppPreference.getBankCode()).toString(),
                                AppPreference.getBankCode()
                            )
                            lifecycleScope.launch(Dispatchers.Main) {
                                iDialog?.hideProgress()
                                // parentFragmentManager.popBackStackImmediate()
                            }
                        }
                    } else {
                        ROCProviderV2.incrementFromResponse(
                            ROCProviderV2.getRoc(AppPreference.getBankCode()).toString(),
                            AppPreference.getBankCode()
                        )
                        lifecycleScope.launch(Dispatchers.Main) {
                            iDialog?.hideProgress()
                            //  parentFragmentManager.popBackStackImmediate()
                            /*iDialog?.alertBoxWithAction(null, null,
                                getString(R.string.error), result,
                                false, getString(R.string.positive_button_ok),
                                { parentFragmentManager.popBackStackImmediate() }, {})*/
                        }
                    }
                }, {})
            } else {
                lifecycleScope.launch(Dispatchers.Main) {
                    iDialog?.hideProgress()
                    //    parentFragmentManager.popBackStackImmediate()
                    /*iDialog?.alertBoxWithAction(null, null,
                        getString(R.string.error), "Something went wrong",
                        false, getString(R.string.positive_button_ok),
                        { parentFragmentManager.popBackStackImmediate() }, {})*/
                }
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
        lifecycleScope.launch(Dispatchers.Default) {
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

                    //Refresh Field57 request value for Pagination if More Record Flag is True:-
                    if (moreDataFlag == "1") {
                        field57RequestData =
                            "${EMIRequestType.BRAND_SUB_CATEGORY.requestType}^$totalRecord^${brandEMIDataModal?.brandID ?: brandIDFromPref}"
                        fetchBrandEMIMasterSubCategoryDataFromHost()
                        Log.d("FullDataList:- ", brandEmiMasterSubCategoryDataList.toString())
                    } else {
                        withContext(Dispatchers.Main) {
                            if (brandEmiMasterSubCategoryDataList.isEmpty()) {
                                navigateToProductPage(isSubCategoryItem = false, -1)
                            } else {
                                withContext(Dispatchers.IO) {
                                    saveAllSubCategoryDataInDB(brandEmiMasterSubCategoryDataList)
                                }
                                Log.d(
                                    "Sub Category Data:- ",
                                    Gson().toJson(brandEmiMasterSubCategoryDataList)
                                )
                                brandEMIAllDataList = brandEmiMasterSubCategoryDataList

                                //region=====================Line added to resolve category only issue===================== By Manish
                                brandEmiMasterSubCategoryDataList =
                                    brandEmiMasterSubCategoryDataList.filter {
                                        it.brandID == brandEMIDataModal?.brandID && it.parentCategoryID == "0"
                                    } as MutableList<BrandEMIMasterSubCategoryDataModal>
                                //region=====================Line added to resolve category only issue end=====================

                                brandEMIMasterSubCategoryAdapter.refreshAdapterList(
                                    brandEmiMasterSubCategoryDataList
                                )
                            }
                            iDialog?.hideProgress()
                        }
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

    //region======================save all sub-category data in DB:-
    private suspend fun saveAllSubCategoryDataInDB(subCategoryDataList: MutableList<BrandEMIMasterSubCategoryDataModal>) {
        val modal = BrandEMISubCategoryTable()
        BrandEMISubCategoryTable.clear()
        for (value in subCategoryDataList) {
            modal.brandID = value.brandID
            modal.categoryID = value.categoryID
            modal.parentCategoryID = value.parentCategoryID
            modal.categoryName = value.categoryName
            BrandEMISubCategoryTable.performOperation(modal)
        }
    }
    //endregion

    //region=====================Condition to check whether sub-category data need to load from DB or Host based on Data Update TimeStamp:-
    private fun checkAndLoadDataFromSourceCondition() {
        val subCategoryDataFromDB = runBlocking {
            BrandEMISubCategoryTable.getAllSubCategoryTableDataByBrandID(brandEMIDataModal?.brandID ?: brandIDFromPref ?: "")
        }
        //Below we are assigning initial request value of Field57 in BrandEMIMaster Data Host Hit:-
        field57RequestData = "${EMIRequestType.BRAND_SUB_CATEGORY.requestType}^0^${brandEMIDataModal?.brandID?:brandIDFromPref}"
        if (brandEMIDataModal?.dataTimeStampChangedOrNot == true) {
            if (subCategoryDataFromDB.isNotEmpty()) {
                lifecycleScope.launch(Dispatchers.Default) {
                    for (value in subCategoryDataFromDB) {
                        brandEmiMasterSubCategoryDataList.add(
                            BrandEMIMasterSubCategoryDataModal(
                                value.brandID, value.categoryID,
                                value.parentCategoryID, value.categoryName)
                        )
                    }

                    withContext(Dispatchers.Main) {
                        brandEMIAllDataList = brandEmiMasterSubCategoryDataList
                        //region=====================Line added to resolve category only showing issue===================== By Manish
                        brandEmiMasterSubCategoryDataList =
                            brandEmiMasterSubCategoryDataList.filter {
                                it.brandID == brandEMIDataModal?.brandID && it.parentCategoryID == "0"
                            } as MutableList<BrandEMIMasterSubCategoryDataModal>
                        //region=====================Line added to resolve category only issue end=====================

                        brandEMIMasterSubCategoryAdapter.refreshAdapterList(
                            brandEmiMasterSubCategoryDataList
                        )
                    }
                }
            } else {
                //Data by Brand ID for Sub-Category is Not Found in Database Table , So we will show a Pop-UP:-
                lifecycleScope.launch(Dispatchers.Main) {
                    navigateToProductPage(isSubCategoryItem = false, -1)
                }
            }
        } else {
            fetchBrandEMIMasterSubCategoryDataFromHost()
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
    @SuppressLint("LongLogTag")
    private fun onCategoryItemClick(position: Int) {
        try {
            //  Log.d("CategoryName:- ", brandEmiMasterSubCategoryDataList[position].categoryName)
            Log.d("Category & Subcategory data- ", Gson().toJson(brandEMIAllDataList))
            val childFilteredList = brandEMIAllDataList.filter {
                brandEmiMasterSubCategoryDataList[position].categoryID == it.parentCategoryID
            }
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
        if (checkInternetConnection() && position > -1) {
            //region==========Adding BrandEMISubCategoryID , CategoryName in brandEMIDataModal:-
            if (isSubCategoryItem) {
                brandEMIDataModal?.categoryID=(brandEmiMasterSubCategoryDataList[position].categoryID)
                brandEMIDataModal?.categoryName=(brandEmiMasterSubCategoryDataList[position].categoryName)
            }
            //endregion
            binding?.categorySearchET?.setText("")
            (activity as MainActivity).transactFragment(BrandEMIDataByCategoryID().apply {
                arguments = Bundle().apply {
                    putSerializable("modal", brandEMIDataModal)
                    putBoolean("isSubCategoryItemPresent", isSubCategoryItem)
                    putParcelableArrayList(
                        "subCategoryData",
                        brandEMIAllDataList as ArrayList<out Parcelable>
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
                brandEMIDataModal?.categoryID=(brandEmiMasterSubCategoryDataList[position].categoryID)
                brandEMIDataModal?.categoryName=(brandEmiMasterSubCategoryDataList[position].categoryName)
            } else {
                brandEMIDataModal?.categoryID=("")
                brandEMIDataModal?.categoryName=("")
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

    //region==============================Start TimeOut Handler:-
    fun startTimeOut() {
        runnable = object : Runnable {
            override fun run() {
                Looper.prepare()
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

internal class BrandEMIMasterSubCategoryAdapter(
    private var dataList: MutableList<BrandEMIMasterSubCategoryDataModal>?,
    private val onCategoryItemClick: (Int) -> Unit
) :
    RecyclerView.Adapter<BrandEMIMasterSubCategoryAdapter.BrandEMIMasterSubCategoryViewHolder>() {

    private val subCategoryDataList: MutableList<BrandEMIMasterSubCategoryDataModal> =
        mutableListOf()

    init {
        this.subCategoryDataList.clear()
        if (dataList?.isNotEmpty() == true)
            subCategoryDataList.addAll(dataList!!)
    }

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
        return subCategoryDataList.size
    }

    override fun onBindViewHolder(holder: BrandEMIMasterSubCategoryViewHolder, p1: Int) {
        holder.binding.tvBrandSubCategoryName.text = subCategoryDataList[p1].categoryName
    }

    inner class BrandEMIMasterSubCategoryViewHolder(val binding: ItemBrandEmiSubCategoryBinding) :
        RecyclerView.ViewHolder(binding.root) {
        init {
            binding.brandEmiSubCategoryLv.setOnClickListener {
                onCategoryItemClick(
                    absoluteAdapterPosition
                )
            }
        }
    }

    //region==========================Below Method is used to refresh Adapter New Data and Also
    fun refreshAdapterList(refreshList: MutableList<BrandEMIMasterSubCategoryDataModal>) {
        val diffUtilCallBack = BrandSubCategoryUpdateDiffUtil(this.subCategoryDataList, refreshList)
        val diffResult = DiffUtil.calculateDiff(diffUtilCallBack)
        this.subCategoryDataList.clear()
        this.subCategoryDataList.addAll(refreshList)
        diffResult.dispatchUpdatesTo(this)
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

//when we start counting from 0:-
//10th Field of Reserved Field TPT -> BrandEMI Catalogue ON/OFF
//11th Field of Reserved Field TPT -> BrandEMI Catalogue Mobile Number ON/OFF
//6th Field of Reserved Field TPT -> BankEMI Catalogue ON/OFF
//7th Field of Reserved Field TPT -> BankEMI Catalogue Mobile Number ON/OFF