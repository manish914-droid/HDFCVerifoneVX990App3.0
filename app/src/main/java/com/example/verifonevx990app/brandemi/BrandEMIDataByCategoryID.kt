package com.example.verifonevx990app.brandemi

import android.content.Context
import android.os.Bundle
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
import com.example.verifonevx990app.databinding.FragmentBrandEmiDataByCategoryIdBinding
import com.example.verifonevx990app.databinding.ItemBrandEmiSubCategoryItemBinding
import com.example.verifonevx990app.init.DashboardFragment
import com.example.verifonevx990app.main.MainActivity
import com.example.verifonevx990app.realmtables.EDashboardItem
import com.example.verifonevx990app.vxUtils.IDialog
import com.example.verifonevx990app.vxUtils.VFService
import com.example.verifonevx990app.vxUtils.checkInternetConnection
import com.example.verifonevx990app.vxUtils.hideSoftKeyboard
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.*

/**
This is a Brand EMI Sub-Category Hierarchy Data By CategoryID Fragment
Here we are Fetching Brand EMI Sub-Category ID based Data From Previous Sub-Category DataList:-
================Written By Ajay Thakur on 9th March 2021====================
 */
class BrandEMIDataByCategoryID : Fragment() {
    private var iDialog: IDialog? = null
    private var subCategoryData: MutableList<BrandEMIMasterSubCategoryDataModal>? = null
    private var displayFilteredList: MutableList<BrandEMIMasterSubCategoryDataModal>? =
        mutableListOf()
    private var displayAllDataList: MutableList<BrandEMIMasterSubCategoryDataModal>? =
        mutableListOf()
    private var selectedCategoryID: String? = null
    private val action by lazy { arguments?.getSerializable("type") ?: "" }
    private val isSubCategoryItem by lazy { arguments?.getBoolean("isSubCategoryItemPresent") }
    private var binding: FragmentBrandEmiDataByCategoryIdBinding? = null
    private var brandEMIDataModal: BrandEMIDataModal? = null
    private val brandEMISubCategoryByIDAdapter by lazy {
        BrandEMISubCategoryByIDAdapter(displayFilteredList, ::onItemClick)
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
        binding = FragmentBrandEmiDataByCategoryIdBinding.inflate(layoutInflater, container, false)
        return binding?.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
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
        binding?.subHeaderView?.backImageButton?.setOnClickListener { parentFragmentManager.popBackStackImmediate() }
        brandEMIDataModal = arguments?.getSerializable("modal") as BrandEMIDataModal
        subCategoryData = arguments?.getParcelableArrayList("subCategoryData")
        Log.d("subCategoryData:- ", brandEMIDataModal.toString())


        //Method to Filter child category data from SubCategory Data According to Selected Category and display on UI:-
        setUpRecyclerView()
        fetchChildCategoryAndDisplay()

        //region================Search EditText TextChangeListener event:-
        binding?.categoryByIDSearchET?.addTextChangedListener(object : TextWatcher {
            override fun onTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {}
            override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {}
            override fun afterTextChanged(p0: Editable?) {
                if (TextUtils.isEmpty(p0.toString())) {
                    binding?.emptyViewPlaceholder?.visibility = View.INVISIBLE
                    displayFilteredList=displayAllDataList
                    brandEMISubCategoryByIDAdapter.refreshAdapterList(displayAllDataList)
                    binding?.brandCategoryByIDRecyclerView?.smoothScrollToPosition(0)
                    hideSoftKeyboard(requireActivity())
                }
            }
        })
        //endregion

        //region=================Search Button onClick event:-
        binding?.searchButton?.setOnClickListener {
            if (!TextUtils.isEmpty(binding?.categoryByIDSearchET?.text?.toString())) {
                iDialog?.showProgress()
                getSearchedCategoryByID(binding?.categoryByIDSearchET?.text?.trim().toString())
                hideSoftKeyboard(requireActivity())
            } else
                VFService.showToast(getString(R.string.please_enter_brand_name_to_search))
        }
        //endregion
    }

    //region======================Get Searched Category By ID Data:-
    private fun getSearchedCategoryByID(searchText: String?) {
        val searchedDataList = mutableListOf<BrandEMIMasterSubCategoryDataModal>()
        lifecycleScope.launch(Dispatchers.Default) {
            if (!TextUtils.isEmpty(searchText) && displayFilteredList?.isNotEmpty() == true) {
                val length = displayFilteredList?.size ?: 0
                for (i in 0 until length) {
                    val subCategoryData = displayFilteredList?.get(i)
                    //check whether sub category name contains letter which is inserted in search box:-
                    if (subCategoryData?.categoryName?.toLowerCase(Locale.ROOT)?.trim()
                            ?.contains(searchText?.toLowerCase(Locale.ROOT)?.trim()!!) == true
                    )
                        searchedDataList.add(
                            BrandEMIMasterSubCategoryDataModal(
                                subCategoryData.brandID, subCategoryData.categoryID,
                                subCategoryData.parentCategoryID, subCategoryData.categoryName
                            )
                        )
                }
                withContext(Dispatchers.Main) {
                    displayFilteredList = searchedDataList
                    if(searchedDataList.size>0) {
                        brandEMISubCategoryByIDAdapter.refreshAdapterList(displayFilteredList)
                        iDialog?.hideProgress()
                    }else{
                        brandEMISubCategoryByIDAdapter.refreshAdapterList(displayFilteredList)
                        iDialog?.hideProgress()
                        binding?.emptyViewPlaceholder?.visibility = View.VISIBLE
                    }
                }
            } else
                withContext(Dispatchers.Main) {
                    iDialog?.hideProgress()
                }
        }
    }
    //endregion

    //region====================================Fetch Child Category Data and Display:-
    private fun fetchChildCategoryAndDisplay() {
        lifecycleScope.launch(Dispatchers.IO) {
            if (subCategoryData != null) {
                displayFilteredList =
                    subCategoryData?.filter { brandEMIDataModal?.categoryID == it.parentCategoryID }
                            as MutableList<BrandEMIMasterSubCategoryDataModal>?
                Log.d("ChildCategoryData:- ", displayFilteredList.toString())
                if (displayFilteredList?.isNotEmpty() == true) {
                    withContext(Dispatchers.Main) {
                        displayAllDataList=displayFilteredList
                        brandEMISubCategoryByIDAdapter.refreshAdapterList(displayFilteredList)
                    }
                } else if (displayAllDataList?.isNotEmpty() == true) {
                    withContext(Dispatchers.Main) {

                        brandEMISubCategoryByIDAdapter.refreshAdapterList(displayAllDataList)
                    }
                }else {
                    withContext(Dispatchers.Main) {
                        (activity as MainActivity).transactFragment(BrandEMIProductFragment().apply {
                            arguments = Bundle().apply {
                                putBoolean("isSubCategoryItemPresent", isSubCategoryItem ?: false)
                                putSerializable("modal", brandEMIDataModal)
                                putSerializable("type", action)
                            }
                        })
                    }
                }
            } else {
                withContext(Dispatchers.Main) {
                    VFService.showToast("No Data Found")
                }
            }
        }
    }
//endregion

    //region===========================SetUp RecyclerView :-
    private fun setUpRecyclerView() {
        binding?.brandCategoryByIDRecyclerView?.apply {
            layoutManager = LinearLayoutManager(context)
            itemAnimator = DefaultItemAnimator()
            adapter = brandEMISubCategoryByIDAdapter
        }
    }
    //endregion

    //region=======================Adapter onItemClick Event to Detect SubCategory Item Click:-
    private fun onItemClick(position: Int) {
        try {
            Log.d("ItemClick:- ", position.toString())
            if (subCategoryData?.isNotEmpty() == true) {
                //Below we are getting selected sub category id and filter in dataList whether it has some child data ot not:-
                val newList =
                    subCategoryData?.filter { displayFilteredList?.get(position)?.categoryID == it.parentCategoryID }
                            as MutableList<BrandEMIMasterSubCategoryDataModal>?
                if (newList?.isNotEmpty() == true) {
                    displayFilteredList?.clear()
                    displayFilteredList = newList
                    Log.d("NewFilteredList:- ", displayFilteredList.toString())
                    brandEMISubCategoryByIDAdapter.refreshAdapterList(displayFilteredList!!)
                } else {
                    navigateToProductPage(position)
                }
            }
        } catch (ex: IndexOutOfBoundsException) {
            ex.printStackTrace()
        }
    }
//endregion

    //region===================================Navigate Controller To Product Page:-
    private fun navigateToProductPage(position: Int) {
        if (checkInternetConnection()) {
            //region Adding ChildSubCategoryID and Name:-
            brandEMIDataModal?.categoryID=(
                displayFilteredList?.get(position)?.categoryID ?: ""
            )
            brandEMIDataModal?.childSubCategoryID=(
                displayFilteredList?.get(position)?.parentCategoryID ?: ""
            )
            brandEMIDataModal?.childSubCategoryName=(
                displayFilteredList?.get(position)?.categoryName ?: ""
            )
            //endregion
            (activity as MainActivity).transactFragment(BrandEMIProductFragment().apply {
                arguments = Bundle().apply {
                    putBoolean("isSubCategoryItemPresent", true)
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

internal class BrandEMISubCategoryByIDAdapter(
    private var dataList: MutableList<BrandEMIMasterSubCategoryDataModal>?,
    private val onItemClick: (Int) -> Unit
) :
    RecyclerView.Adapter<BrandEMISubCategoryByIDAdapter.BrandEMIMasterSubCategoryByIDViewHolder>() {

    private val categoryByIDDataList: MutableList<BrandEMIMasterSubCategoryDataModal> =
        mutableListOf()

    init {
        if (dataList?.isNotEmpty() == true)
            categoryByIDDataList.addAll(dataList!!)
    }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): BrandEMIMasterSubCategoryByIDViewHolder {
        val binding: ItemBrandEmiSubCategoryItemBinding =
            ItemBrandEmiSubCategoryItemBinding.inflate(
                LayoutInflater.from(parent.context),
                parent, false
            )
        return BrandEMIMasterSubCategoryByIDViewHolder(binding)
    }

    override fun getItemCount(): Int {
        return categoryByIDDataList.size
    }

    override fun onBindViewHolder(holder: BrandEMIMasterSubCategoryByIDViewHolder, p1: Int) {
        holder.binding.tvBrandSubCategoryByIdName.text = categoryByIDDataList[p1].categoryName
    }

    inner class BrandEMIMasterSubCategoryByIDViewHolder(val binding: ItemBrandEmiSubCategoryItemBinding) :
        RecyclerView.ViewHolder(binding.root) {
        init {
            binding.brandEmiSubCategoryByIdLv.setOnClickListener {
                onItemClick(
                    absoluteAdapterPosition
                )
            }
        }
    }

    //region===========================Refresh Current BrandEMIDataByCategoryID DataList according to Selected Category Data:-
    fun refreshAdapterList(refreshList: MutableList<BrandEMIMasterSubCategoryDataModal>?) {
        val diffUtilCallBack = BrandCategoryByIDDiffUtil(this.categoryByIDDataList, refreshList)
        val diffResult = DiffUtil.calculateDiff(diffUtilCallBack)
        this.categoryByIDDataList.clear()
        if (refreshList != null) {
            this.categoryByIDDataList.addAll(refreshList)
        }
        diffResult.dispatchUpdatesTo(this)
    }
    //endregion
}