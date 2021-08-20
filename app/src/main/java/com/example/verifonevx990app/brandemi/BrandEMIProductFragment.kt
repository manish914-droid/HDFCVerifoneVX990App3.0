package com.example.verifonevx990app.brandemi

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
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.verifonevx990app.R
import com.example.verifonevx990app.databinding.FragmentBrandEmiProductBinding
import com.example.verifonevx990app.databinding.ItemBrandEmiProductBinding
import com.example.verifonevx990app.init.DashboardFragment
import com.example.verifonevx990app.main.*
import com.example.verifonevx990app.realmtables.EDashboardItem
import com.example.verifonevx990app.transactions.NewInputAmountFragment
import com.example.verifonevx990app.vxUtils.*
import com.google.gson.Gson
import kotlinx.coroutines.*
import kotlinx.parcelize.Parcelize

class BrandEMIProductFragment : Fragment() {
    private var binding: FragmentBrandEmiProductBinding? = null
    private var iDialog: IDialog? = null
    private var isSubCategoryItemPresent: Boolean = false
    private var iFrReq: IFragmentRequest? = null
    private val brandEmiProductDataList by lazy { mutableListOf<BrandEMIProductDataModal>() }
    private val brandEmiSearchedProductDataList by lazy { mutableListOf<BrandEMIProductDataModal>() }
    private val action by lazy { arguments?.getSerializable("type") ?: "" }
    private var brandEMIDataModal: BrandEMIDataModal? = null
    private var selectedProductUpdatedPosition = -1
    private var field57RequestData: String? = null
    private var moreDataFlag = "0"
    private var totalRecord: String? = "0"
    private var perPageRecord: String? = "0"
    private var searchedProductName: String? = null
    private val handler = Handler(Looper.getMainLooper())
    private var runnable: Runnable? = null
    private var delayTime: Long = 0L
    private val brandEMIProductAdapter by lazy {
        BrandEMIProductAdapter(brandEmiProductDataList, ::onProductSelected)
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        if (context is IFragmentRequest) iFrReq = context
        if (context is IDialog) iDialog = context
    }

    override fun onDetach() {
        super.onDetach()
        iDialog = null
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        binding = FragmentBrandEmiProductBinding.inflate(layoutInflater, container, false)
        return binding?.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        isSubCategoryItemPresent = arguments?.getBoolean("isSubCategoryItemPresent") ?: false
        //(activity as MainActivity).showBottomNavigationBar(isShow = false)
        brandEMIDataModal = arguments?.getSerializable("modal") as BrandEMIDataModal
        if (action as EDashboardItem == EDashboardItem.BRAND_EMI_CATALOGUE) {
            binding?.subHeaderView?.subHeaderText?.text = getString(R.string.brandEmiCatalogue)
            binding?.subHeaderView?.headerImage?.setImageResource(R.drawable.ic_brand_emi_catalogue)
            binding?.subHeaderView?.headerHome?.visibility= View.VISIBLE
            binding?.subHeaderView?.headerHome?.setOnClickListener { (activity as MainActivity).transactFragment(DashboardFragment()) }
        } else {
            binding?.subHeaderView?.subHeaderText?.text = getString(R.string.brandEmi)
            binding?.subHeaderView?.headerImage?.setImageResource(R.drawable.ic_brand_emi_sub_header_logo)
        }
     //   delayTime = timeOutTime()
        binding?.subHeaderView?.backImageButton?.setOnClickListener {
            if (isSubCategoryItemPresent) {
                parentFragmentManager.popBackStackImmediate()
            }
            else {
               // parentFragmentManager.popBackStackImmediate()
               parentFragmentManager.popBackStack()
                parentFragmentManager.popBackStack()

            }
        }

        //Below we are assigning initial request value of Field57 in BrandEMIMaster Data Host Hit:-
        field57RequestData = "${EMIRequestType.BRAND_EMI_Product.requestType}^0^${brandEMIDataModal?.brandID}^${brandEMIDataModal?.categoryID}"
        Log.d("57Data:-", field57RequestData.toString())

        //Initial SetUp of RecyclerView List with Empty Data , After Fetching Data from Host we will notify List:-
        setUpRecyclerView()
        brandEmiProductDataList.clear()
        fetchBrandEMIProductDataFromHost()

        //OnClick Event of Floating Action Button:-
        binding?.brandEmiProductFloatingButton?.visibility = View.GONE

        Log.d("BrandDataModal:- ", Gson().toJson(brandEMIDataModal))


        //region=================Search Button to Fetch Searched Product From Host on Click event:-
        ////Initially on searching of product we were not showing the products category with requestType 3.But now with request type 11,product category name is coming
        binding?.searchButton?.setOnClickListener {
            hideSoftKeyboard(requireActivity())
            binding?.emptyViewPlaceholder?.visibility = View.INVISIBLE
            searchedProductName = binding?.productSearchET?.text?.toString() ?: ""
            totalRecord = "0"
            brandEmiSearchedProductDataList.clear()
            //Initially on searching of product we were not showing the products category with requestType 3.But now with request type 11,product category name is coming
            field57RequestData = "${EMIRequestType.BRAND_EMI_Product_WithCategory.requestType}^$totalRecord^${brandEMIDataModal?.brandID}^^$searchedProductName"
            Log.d("57Data:-", field57RequestData.toString())
            fetchBrandEMIProductDataFromHost(isSearchedDataCall = true)
        }
        //endregion

        //region================Product Search EditText TextChangeListener event:-
        binding?.productSearchET?.addTextChangedListener(object : TextWatcher {
            override fun onTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {}
            override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {}
            override fun afterTextChanged(p0: Editable?) {
                if (TextUtils.isEmpty(p0.toString())) {
                    binding?.emptyViewPlaceholder?.visibility = View.INVISIBLE
                    brandEMIProductAdapter.refreshAdapterList(brandEmiProductDataList)
                    binding?.brandEmiProductRV?.smoothScrollToPosition(0)
                    hideSoftKeyboard(requireActivity())
                }
            }
        })
        //endregion
    }

    //region========================Navigate Product Page To Input Amount Fragment:-
    private fun navigateToInputAmountFragment(brandEmiProductDataList: MutableList<BrandEMIProductDataModal>) {
        if (checkInternetConnection()) {
            //region===================Saving Selected ProductID and ProductName in BrandEMIDataModal:-
            if (selectedProductUpdatedPosition > -1) {
                brandEMIDataModal?.productID =
                    (brandEmiProductDataList?.get(selectedProductUpdatedPosition)?.productID)
                brandEMIDataModal?.productName =
                    (brandEmiProductDataList[selectedProductUpdatedPosition].productName)
                brandEMIDataModal?.validationTypeName =
                    (brandEmiProductDataList[selectedProductUpdatedPosition].validationTypeName)
                brandEMIDataModal?.isRequired =
                    (brandEmiProductDataList[selectedProductUpdatedPosition].isRequired)
                brandEMIDataModal?.inputDataType =
                    (brandEmiProductDataList[selectedProductUpdatedPosition].inputDataType)
                brandEMIDataModal?.minLength =
                    (brandEmiProductDataList[selectedProductUpdatedPosition].minLength)
                brandEMIDataModal?.maxLength =
                    (brandEmiProductDataList[selectedProductUpdatedPosition].maxLength)

                    brandEMIDataModal?.producatDesc =
                        (brandEmiProductDataList[selectedProductUpdatedPosition].producatDesc)

                brandEMIDataModal?.productMinAmount =
                    ((brandEmiProductDataList[selectedProductUpdatedPosition].productMinAmount).toDouble()
                        .div(100).toString())
                brandEMIDataModal?.productMaxAmount =
                    ((brandEmiProductDataList[selectedProductUpdatedPosition].productMaxAmount).toDouble()
                        .div(100).toString())
                if (!brandEmiProductDataList[selectedProductUpdatedPosition].productCategoryName.isNullOrEmpty()) {
                    brandEMIDataModal?.categoryName =
                        (brandEmiProductDataList[selectedProductUpdatedPosition].productCategoryName)
                }
            }
            //endregion
            // add for checking brandemidatamodel is not blank====== add by sandeep(18-08-2021)
            if (brandEMIDataModal != null) {
                (activity as MainActivity).transactFragment(NewInputAmountFragment().apply {
                    arguments = Bundle().apply {
                        putSerializable("type", action)
                        putSerializable("modal", brandEMIDataModal)
                        putString(MainActivity.INPUT_SUB_HEADING, SubHeaderTitle.Brand_EMI.title)
                    }
                })
            }else{
                VFService.showToast("connection is slow ,please select product again")
            }
        }
        else {
            VFService.showToast(getString(R.string.no_internet_available_please_check_your_internet))
        }
    }
    //endregion

    //region===========================SetUp RecyclerView :-
    private fun setUpRecyclerView() {
        binding?.brandEmiProductRV?.apply {
            layoutManager = LinearLayoutManager(context)
            itemAnimator = DefaultItemAnimator()
            adapter = brandEMIProductAdapter
        }
    }
    //endregion

    //region===============================Hit Host to Fetch BrandEMIProduct Data:-
    private fun fetchBrandEMIProductDataFromHost(isSearchedDataCall: Boolean = false) {
        iDialog?.showProgress()
        var brandEMIProductISOData: IsoDataWriter? = null

        //region==============================Creating ISO Packet For BrandEMIMasterSubCategoryData Request:-
        runBlocking(Dispatchers.IO) {
            CreateBrandEMIPacket(field57RequestData) {
                brandEMIProductISOData = it
            }
        }
        //endregion

        //region==============================Host Hit To Fetch BrandEMIProduct Data:-
        GlobalScope.launch(Dispatchers.IO) {
            if (brandEMIProductISOData != null) {
                val byteArrayRequest = brandEMIProductISOData?.generateIsoByteRequest()
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
                        val brandEMIProductData =
                            responseIsoData.isoMap[57]?.parseRaw2String().toString()

                        when (responseCode) {
                            "00" -> {
                               // binding?.emptyViewPlaceholder?.visibility = View.INVISIBLE
                                ROCProviderV2.incrementFromResponse(
                                    ROCProviderV2.getRoc(AppPreference.getBankCode()).toString(), AppPreference.getBankCode())
                                GlobalScope.launch(Dispatchers.Main) {
                                    //Processing BrandEMIMasterSubCategoryData:-
                                    if (isSearchedDataCall) {

                                        stubbingBrandEMISearchedProductDataToList(brandEMIProductData, hostMsg,isSearchedDataCall)
                                    } else {

                                        stubbingBrandEMIProductDataToList(brandEMIProductData, hostMsg)
                                    }
                                }
                            }
                            "-1" -> {
                                GlobalScope.launch(Dispatchers.Main) {
                                    iDialog?.hideProgress()
                                   if(!isSearchedDataCall) {
                                       binding?.emptyViewPlaceholder?.visibility = View.VISIBLE
                                   }else{
                                       VFService.showToast(getString(R.string.no_data_found))
                                   }
                                   // parentFragmentManager.popBackStackImmediate()
                                    /*iDialog?.alertBoxWithAction(null, null,
                                        getString(R.string.info), "No Record Found",
                                        false, getString(R.string.positive_button_ok),
                                        {
                                            if (!TextUtils.isEmpty(brandEMIDataModal?.getCategoryID()))
                                                parentFragmentManager.popBackStack()
                                            else {
                                                parentFragmentManager.popBackStack()
                                                parentFragmentManager.popBackStack()
                                            }
                                        }, {})*/
                                }
                            }

                            else -> {
                                ROCProviderV2.incrementFromResponse(
                                    ROCProviderV2.getRoc(AppPreference.getBankCode()).toString(),
                                    AppPreference.getBankCode()
                                )

                                iDialog?.hideProgress()
                               // parentFragmentManager.popBackStackImmediate()
                            }
                        }
                    } else {
                        /// for handling internet issue
                        ROCProviderV2.incrementFromResponse(
                            ROCProviderV2.getRoc(AppPreference.getBankCode()).toString(),
                            AppPreference.getBankCode()
                        )
                        lifecycleScope.launch(Dispatchers.Main) {
                            iDialog?.hideProgress()
                          //  parentFragmentManager.popBackStackImmediate()
                            iDialog?.alertBoxWithAction(null, null,
                                getString(R.string.error), result,
                                false, getString(R.string.positive_button_ok),
                                {  (activity as MainActivity).transactFragment(DashboardFragment())}, {})
                        }
                    }
                }, {})
            }
        }
        //endregion
    }
    //endregion

    //region=================================Stubbing BrandEMI Product Data and Display in List:-
    private fun stubbingBrandEMIProductDataToList(brandEMIProductData: String, hostMsg: String) {
        GlobalScope.launch(Dispatchers.Main) {
            if (!TextUtils.isEmpty(brandEMIProductData)) {
                val dataList = parseDataListWithSplitter("|", brandEMIProductData)
                if (dataList.isNotEmpty()) {
                    moreDataFlag = dataList[0]
                    perPageRecord = dataList[1]
                    totalRecord = (totalRecord?.toInt()?.plus(perPageRecord?.toInt() ?: 0)).toString()
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
                            brandEmiProductDataList.add(
                                BrandEMIProductDataModal(
                                    splitData[0], splitData[1],
                                    splitData[2], splitData[3],
                                    splitData[4], splitData[5],
                                    splitData[6], splitData[7],
                                    splitData[8], splitData[9],
                                    splitData[10],"","subCat")
                            )
                        }
                    }

                    if (brandEmiProductDataList.isNotEmpty()) {
                        binding?.emptyViewPlaceholder?.visibility = View.INVISIBLE
                        brandEMIProductAdapter.refreshAdapterList(brandEmiProductDataList)
                    }

                    //Refresh Field57 request value for Pagination if More Record Flag is True:-
                    if (moreDataFlag == "1") {
                        field57RequestData =
                            "${EMIRequestType.BRAND_EMI_Product.requestType}^$totalRecord^${brandEMIDataModal?.brandID}^${brandEMIDataModal?.categoryID}"
                        fetchBrandEMIProductDataFromHost()
                        Log.d("FullDataList:- ", brandEmiProductDataList.toString())
                    } else {
                        iDialog?.hideProgress()
                        Log.d("Full Product Data:- ", Gson().toJson(brandEmiProductDataList))
                    }
                }
            } else {
                GlobalScope.launch(Dispatchers.Main) {
                    iDialog?.hideProgress()
                }
            }
        }
    }
    //endregion

    //region=================================Stubbing BrandEMI Searched Product Data and Display in List:-
    private fun stubbingBrandEMISearchedProductDataToList(brandEMIProductData: String, hostMsg: String, isSearchedDataCall: Boolean) {
        GlobalScope.launch(Dispatchers.Main) {
            if (!TextUtils.isEmpty(brandEMIProductData)) {
                val dataList = parseDataListWithSplitter("|", brandEMIProductData)
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
                            brandEmiSearchedProductDataList.add(
                                BrandEMIProductDataModal(
                                    splitData[0], splitData[1],
                                    splitData[2], splitData[3],
                                    splitData[4], splitData[5],
                                    splitData[6], splitData[7],
                                    splitData[8], splitData[9],
                                    splitData[10],splitData[11],
                                    "search"
                                )
                            )
                        }
                    }

                    if (brandEmiSearchedProductDataList.isNotEmpty()) {
                        binding?.emptyViewPlaceholder?.visibility = View.INVISIBLE
                        brandEMIProductAdapter.refreshAdapterList(brandEmiSearchedProductDataList)
                    }

                  /*  withContext(Dispatchers.Main) {
                        cancelTimeOut()
                    }*/

                    //Refresh Field57 request value for Pagination if More Record Flag is True:-
                    if (moreDataFlag == "1") {
                        field57RequestData = "${EMIRequestType.BRAND_EMI_Product_WithCategory.requestType}^$totalRecord^${brandEMIDataModal?.brandID}^^$searchedProductName"
                        fetchBrandEMIProductDataFromHost(isSearchedDataCall = true)
                        Log.d("SearchedFullDataList:- ", brandEmiSearchedProductDataList.toString())
                    } else {
                        iDialog?.hideProgress()
                        Log.d(
                            "Searched Full Data:- ",
                            Gson().toJson(brandEmiSearchedProductDataList)
                        )
                    }
                }
            } else {
                GlobalScope.launch(Dispatchers.Main) {
                    iDialog?.hideProgress()
                }
            }
        }
    }
    //endregion

    //region==========================Perform Click Event on Product Item Click:-
    private fun onProductSelected(position: Int,dataList: MutableList<BrandEMIProductDataModal>) {
        try {
            Log.d("Product Position:- ", position.toString())
            selectedProductUpdatedPosition = position
            if (selectedProductUpdatedPosition > -1)
                navigateToInputAmountFragment(dataList)
            else
                VFService.showToast(getString(R.string.please_select_product))
        } catch (ex: IndexOutOfBoundsException) {
            ex.printStackTrace()
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
}

internal class BrandEMIProductAdapter(
    private var dataList: MutableList<BrandEMIProductDataModal>?,
    private val onProductSelect: (Int,MutableList<BrandEMIProductDataModal>) -> Unit
) :
    RecyclerView.Adapter<BrandEMIProductAdapter.BrandEMIProductViewHolder>() {
    private var index = -1
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BrandEMIProductViewHolder {
        val binding: ItemBrandEmiProductBinding = ItemBrandEmiProductBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return BrandEMIProductViewHolder(binding)
    }

    override fun getItemCount(): Int {
        return dataList?.size ?: 0
    }

    override fun onBindViewHolder(holder: BrandEMIProductViewHolder, position: Int) {
        // holder.binding.tvProductIdValue.text = dataList?.get(position)?.productID ?: ""
        holder.binding.tvProductName.text = dataList?.get(position)?.productName ?: ""
        //holder.binding.tvSkuCodeValue.text = dataList?.get(position)?.skuCode ?: ""
        // holder.binding.tvProductMinAmountValue.text = dataList?.get(position)?.productMinAmount ?: ""
        // holder.binding.tvProductMaxAmountValue.text = dataList?.get(position)?.productMaxAmount ?: ""


        /*  //region==========================Checked Particular Row of RecyclerView Logic:-
          if (index === position) {
              holder.binding.cardView.strokeColor = Color.parseColor("#683992")
              holder.binding.productCheckIv.visibility = View.VISIBLE
              onProductSelect(position)
          } else {
              holder.binding.cardView.strokeColor = Color.parseColor("#FFFFFF")
              holder.binding.productCheckIv.visibility = View.GONE
          }
          //endregion*/
    }

    inner class BrandEMIProductViewHolder(val binding: ItemBrandEmiProductBinding) :
        RecyclerView.ViewHolder(binding.root) {
        init {
            binding.brandEmiProductLv.setOnClickListener {
                onProductSelect(absoluteAdapterPosition,dataList ?: ArrayList())
            }
        }
    }

    //region==========================Below Method is used to refresh Adapter New Data and Also
    fun refreshAdapterList(refreshList: MutableList<BrandEMIProductDataModal>) {
        this.dataList = refreshList
        notifyDataSetChanged()
    }
    //endregion
}

//region=============================Brand EMI Master Category Data Modal==========================
@Parcelize
data class BrandEMIProductDataModal(
    var productID: String,
    var productName: String,
    var skuCode: String,
    var productMinAmount: String,
    var productMaxAmount: String,
    var amountRangeValidationFlag: String,
    var validationTypeName: String,
    var isRequired: String,
    var inputDataType: String,
    var minLength: String,
    var maxLength: String,
    var productCategoryName: String,
    var producatDesc:String,
) : Parcelable
//endregion
//Initially on searching of product we were not showing the products category with requestType 3.But now with request type 11,product category name is coming