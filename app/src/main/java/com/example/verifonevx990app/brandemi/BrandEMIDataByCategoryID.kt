package com.example.verifonevx990app.brandemi

import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.DefaultItemAnimator
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.verifonevx990app.R
import com.example.verifonevx990app.databinding.FragmentBrandEmiDataByCategoryIdBinding
import com.example.verifonevx990app.databinding.ItemBrandEmiSubCategoryItemBinding
import com.example.verifonevx990app.main.MainActivity
import com.example.verifonevx990app.vxUtils.IDialog
import com.example.verifonevx990app.vxUtils.VFService
import com.example.verifonevx990app.vxUtils.checkInternetConnection

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
        binding?.subHeaderView?.subHeaderText?.text = getString(R.string.brand_emi_sub_category)
        binding?.subHeaderView?.backImageButton?.setOnClickListener { parentFragmentManager.popBackStackImmediate() }
      //  //(activity as MainActivity).showBottomNavigationBar(isShow = false)
        brandEMIDataModal = arguments?.getSerializable("modal") as BrandEMIDataModal
        subCategoryData = arguments?.getParcelableArrayList("subCategoryData")

        //Method to Filter child category data from SubCategory Data According to Selected Category and display on UI:-
        fetchChildCategoryAndDisplay()
    }

    //region====================================Fetch Child Category Data and Display:-
    private fun fetchChildCategoryAndDisplay() {
        if (subCategoryData != null) {
            displayFilteredList =
                subCategoryData?.filter { brandEMIDataModal?.getCategoryID() == it.parentCategoryID }
                        as MutableList<BrandEMIMasterSubCategoryDataModal>?
            Log.d("ChildCategoryData:- ", subCategoryData.toString())
            if (displayFilteredList?.isNotEmpty() == true)
                setUpRecyclerView()
            else
            //TODO Here we need to go for Products and Inflate on Next Page
                iDialog?.alertBoxWithAction(
                    null, null,
                    getString(R.string.info),
                    getString(R.string.product_page_implemented_soon),
                    true, getString(R.string.positive_button_ok), {}, {}
                )
        }
    }
//endregion

    //region===========================SetUp RecyclerView :-
    private fun setUpRecyclerView() {
        binding?.recyclerView?.apply {
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
            brandEMIDataModal?.setChildSubCategoryID(
                displayFilteredList?.get(position)?.categoryID ?: ""
            )
            brandEMIDataModal?.setChildSubCategoryName(
                displayFilteredList?.get(position)?.categoryName ?: ""
            )
            //endregion
            (activity as MainActivity).transactFragment(BrandEMIProductFragment().apply {
                arguments = Bundle().apply {
                    putBoolean("isSubCategoryItemPresent", isSubCategoryItem ?: false)
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
    RecyclerView.Adapter<BrandEMISubCategoryByIDAdapter.BrandEMIMasterSubCategoryViewHolder>() {

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): BrandEMIMasterSubCategoryViewHolder {
        val binding: ItemBrandEmiSubCategoryItemBinding =
            ItemBrandEmiSubCategoryItemBinding.inflate(
                LayoutInflater.from(parent.context),
                parent, false
            )
        return BrandEMIMasterSubCategoryViewHolder(binding)
    }

    override fun getItemCount(): Int {
        return dataList?.size ?: 0
    }

    override fun onBindViewHolder(holder: BrandEMIMasterSubCategoryViewHolder, p1: Int) {
        holder.binding.tvBrandSubCategoryItemName.text = dataList?.get(p1)?.categoryName ?: ""
    }

    inner class BrandEMIMasterSubCategoryViewHolder(val binding: ItemBrandEmiSubCategoryItemBinding) :
        RecyclerView.ViewHolder(binding.root) {
        init {
            binding.brandEmiMasterSubCategoryItemParent.setOnClickListener {
                onItemClick(
                    adapterPosition
                )
            }
        }
    }

    //region===========================Refresh Current BrandEMIDataByCategory DataList according to Selected Category Data:-
    fun refreshAdapterList(refreshList: MutableList<BrandEMIMasterSubCategoryDataModal>) {
        this.dataList = refreshList
        notifyDataSetChanged()
    }
    //endregion
}