package com.example.verifonevx990app.emiCatalogue

import android.content.Context
import android.net.Uri
import android.os.Bundle
import android.os.Parcelable
import android.provider.MediaStore
import android.text.TextUtils
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.DefaultItemAnimator
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.verifonevx990app.R
import com.example.verifonevx990app.brandemi.CreateBrandEMIPacket
import com.example.verifonevx990app.databinding.FragmentEmiIssuerListBinding
import com.example.verifonevx990app.databinding.ItemEmiIssuerListBinding
import com.example.verifonevx990app.databinding.ItemEmiIssuerTenureBinding
import com.example.verifonevx990app.main.EMIRequestType
import com.example.verifonevx990app.main.MainActivity
import com.example.verifonevx990app.main.SplitterTypes
import com.example.verifonevx990app.realmtables.BrandEMIDataTable
import com.example.verifonevx990app.realmtables.IssuerParameterTable
import com.example.verifonevx990app.vxUtils.*
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import kotlinx.parcelize.Parcelize
import java.io.Serializable
import java.util.*

/**
This is a EMI Issuer List Page here we are fetching All Issuer Data with including including Issuer Name , Issuer ID and SchemeID and
Displaying on UI:-
================Written By Ajay Thakur on 11th June 2021====================
 */
class EMIIssuerList : Fragment() {
    private var binding: FragmentEmiIssuerListBinding? = null
    private var allIssuers: ArrayList<IssuerParameterTable> = arrayListOf()
    private var allIssuerBankList: MutableList<IssuerBankModal> = mutableListOf()
    private var allIssuerTenureList: MutableList<TenureBankModal> = mutableListOf()
    private val enquiryAmtStr by lazy { arguments?.getString("enquiryAmt") ?: "0" }
    private val mobileNumber by lazy { arguments?.getString("mobileNumber") ?: "" }
    private var emiCatalogueImageList: MutableMap<String, Uri>? = null
    private var mobileNumberOnOff: Boolean = false
    private val action by lazy { arguments?.getSerializable("type") ?: "" }
    private val enquiryAmount by lazy { ((enquiryAmtStr.toFloat()) * 100).toLong() }
    private val issuerListAdapter by lazy {
        IssuerListAdapter(
            temporaryAllIssuerList,
            emiCatalogueImageList
        )
    }
    private val issuerTenureListAdapter by lazy {
        IssuerTenureListAdapter(
            temporaryAllTenureList,
            ::onTenureSelectedEvent
        )
    }
    private var temporaryAllIssuerList = mutableListOf<IssuerBankModal>()
    private var temporaryAllTenureList = mutableListOf<TenureBankModal>()
    private var refreshedBanksByTenure = mutableListOf<IssuerBankModal>()
    private var firstClick = true
    private var iDialog: IDialog? = null
    private var brandEmiData: BrandEMIDataTable? = null
    private var moreDataFlag = "0"
    private var totalRecord: String? = "0"
    private var perPageRecord: String? = "0"
    private var compareActionName: String? = null
    private var field57RequestData = ""
    private var selectedTenure: String? = null
    private var brandEMISelectedData: BrandEMIDataTable? = null

    override fun onAttach(context: Context) {
        super.onAttach(context)
        if (context is IDialog) iDialog = context
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        binding = FragmentEmiIssuerListBinding.inflate(inflater, container, false)
        return binding?.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        mobileNumberOnOff = !TextUtils.isEmpty(mobileNumber)
        emiCatalogueImageList = arguments?.getSerializable("imagesData") as MutableMap<String, Uri>
        setUpRecyclerViews()

        if (action == UiAction.BRAND_EMI_CATALOGUE) {
            binding?.subHeaderView?.subHeaderText?.text = getString(R.string.brandEmiCatalogue)
            binding?.subHeaderView?.headerImage?.setImageResource(R.drawable.ic_brand_emi_catalogue)
            brandEMISelectedData = runBlocking(Dispatchers.IO) { BrandEMIDataTable.getAllEMIData() }
            field57RequestData =
                "${EMIRequestType.EMI_CATALOGUE_ACCESS_CODE.requestType}^$totalRecord^${brandEMISelectedData?.brandID}" +
                        "^${brandEMISelectedData?.productID}^^^$enquiryAmount"
        } else {
            binding?.subHeaderView?.subHeaderText?.text = getString(R.string.bankEmiCatalogue)
            binding?.subHeaderView?.headerImage?.setImageResource(R.drawable.ic_bank_emi)
            field57RequestData =
                if (AppPreference.getLongData(AppPreference.ENQUIRY_AMOUNT_FOR_EMI_CATALOGUE) != 0L)
                    "${EMIRequestType.EMI_CATALOGUE_ACCESS_CODE.requestType}^$totalRecord^1^^^^${
                        AppPreference.getLongData(AppPreference.ENQUIRY_AMOUNT_FOR_EMI_CATALOGUE)
                    }"
                else
                    "${EMIRequestType.EMI_CATALOGUE_ACCESS_CODE.requestType}^$totalRecord^1^^^^$enquiryAmount"
        }

        binding?.subHeaderView?.backImageButton?.setOnClickListener { parentFragmentManager.popBackStackImmediate() }

        //Request All Scheme Data from Host:-
        Log.d("CompareActioneName:- ", compareActionName ?: "")
        getAllBanksTenureAndSchemeData()

        binding?.headingText?.text = getString(R.string.calculate_and_compare_emi_offers)

        //region=================Select All CheckBox OnClick Event:-
        binding?.selectAllBankCheckButton?.setOnClickListener {
            if (binding?.selectAllBankCheckButton?.isChecked == true) {
                binding?.selectAllBankCheckButton?.text = getString(R.string.unselect_all_banks)
                iDialog?.showProgress()
                issuerListAdapter.selectAllIssuerBank(
                    true,
                    CompareActionType.COMPARE_BY_TENURE.compareType
                )
                iDialog?.hideProgress()
            } else {
                binding?.selectAllBankCheckButton?.text = getString(R.string.select_all_banks)
                iDialog?.showProgress()
                issuerListAdapter.selectAllIssuerBank(
                    false,
                    CompareActionType.COMPARE_BY_TENURE.compareType
                )
                iDialog?.hideProgress()
            }
        }
        //endregion

        //region==============OnClick event of Compare By Tenure CardView:-
        binding?.compareByTenure?.setOnClickListener {
            compareByTenureSelectEventMethod()
        }
        //endregion

        // region==============OnClick event of Compare By Bank CardView:-
        binding?.compareByBank?.setOnClickListener {
            compareByBankSelectEventMethod()
        }
        //endregion

        //region===============Proceed EMI Catalogue button onClick Event:-
        binding?.proceedEMICatalogue?.setOnClickListener { proceedToCompareFragmentScreen() }
        //endregion
    }

    //region=================SetUp Tenure and Banks Recyclerview:-
    private fun setUpRecyclerViews() {
        //region==========Binding Tenure RecyclerView:-
        binding?.tenureRV?.apply {
            layoutManager = GridLayoutManager(activity, 3)
            itemAnimator = DefaultItemAnimator()
            adapter = issuerTenureListAdapter
        }
        //endregion

        //region==========Binding Issuer Bank RecyclerView SetUp:-
        binding?.issuerRV?.apply {
            layoutManager = GridLayoutManager(activity, 3)
            itemAnimator = DefaultItemAnimator()
            adapter = issuerListAdapter
        }
        //endregion
    }
    //endregion


    //region=========================Get All Banks Tenure and Scheme Data From Host:-
    private fun getAllBanksTenureAndSchemeData() {
        iDialog?.showProgress()
        var emiISOData: IsoDataWriter? = null

        //region==============================Creating ISO Packet For BrandEMIMasterSubCategoryData Request:-
        runBlocking(Dispatchers.IO) {
            CreateBrandEMIPacket(field57RequestData) {
                emiISOData = it
            }
        }
        //endregion

        //region==============================Host Hit To Fetch BrandEMIProduct Data:-
        lifecycleScope.launch(Dispatchers.IO) {
            if (emiISOData != null) {
                val byteArrayRequest = emiISOData?.generateIsoByteRequest()
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
                        val emiIssuerData = responseIsoData.isoMap[57]?.parseRaw2String().toString()
                        when (responseCode) {
                            "00" -> {
                                ROCProviderV2.incrementFromResponse(
                                    ROCProviderV2.getRoc(AppPreference.getBankCode()).toString(),
                                    AppPreference.getBankCode()
                                )
                                parseAndStubbingBankEMICatalogueDataToList(emiIssuerData)
                            }
                            "-1" -> {
                                lifecycleScope.launch(Dispatchers.Main) {
                                    iDialog?.hideProgress()
                                    iDialog?.alertBoxWithAction(null, null,
                                        getString(R.string.info), "No record found",
                                        false, getString(R.string.positive_button_ok),
                                        {
                                            parentFragmentManager.popBackStackImmediate()
                                        }, {})
                                }
                            }
                            else -> {
                                ROCProviderV2.incrementFromResponse(
                                    ROCProviderV2.getRoc(AppPreference.getBankCode()).toString(),
                                    AppPreference.getBankCode()
                                )
                            }
                        }
                    } else {
                        ROCProviderV2.incrementFromResponse(
                            ROCProviderV2.getRoc(AppPreference.getBankCode()).toString(),
                            AppPreference.getBankCode()
                        )
                        lifecycleScope.launch(Dispatchers.Main) {
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
    }
    //endregion

    //region============================Proceed To Compare Fragment Screen:-
    private fun proceedToCompareFragmentScreen() {
        //region===========Condition to Check When Compare By Bank is Selected and Single Bank is Selected:-
        when (compareActionName) {
            CompareActionType.COMPARE_BY_BANK.compareType -> {
                val selectedIssuerNameData =
                    temporaryAllIssuerList.filter { it.isIssuerSelected == true }
                if (selectedIssuerNameData.isNotEmpty()) {
                    val selectedIssuerNameFullData =
                        allIssuerBankList.filter { it.issuerSchemeID == selectedIssuerNameData[0].issuerSchemeID }
                    if (selectedIssuerNameFullData.isNotEmpty()) {
                        (activity as MainActivity).transactFragment(EMICompareFragment().apply {
                            arguments = Bundle().apply {
                                putString("compareActionName", compareActionName)
                                putSerializable("type", action)
                                putParcelableArrayList(
                                    "dataModal",
                                    selectedIssuerNameFullData as java.util.ArrayList<out Parcelable>
                                )
                            }
                        })
                    } else {
                        VFService.showToast(getString(R.string.please_select_one_issuer_bank))
                    }
                } else {
                    VFService.showToast(getString(R.string.please_select_one_issuer_bank))
                }
            }
            CompareActionType.COMPARE_BY_TENURE.compareType -> {
                val selectedIssuerNameData = refreshedBanksByTenure.filter {
                    it.isIssuerSelected == true
                }
                val selectedIssuerNameData2 =
                    temporaryAllIssuerList.filter { it.isIssuerSelected == true }
                val tenureWiseSelectedIssuerFullData = mutableListOf<IssuerBankModal>()
                for (value in selectedIssuerNameData) {
                    tenureWiseSelectedIssuerFullData.addAll(allIssuerBankList.filter {
                      it.issuerSchemeID == value.issuerSchemeID && it.issuerBankTenure == selectedTenure

                    })

                }

                if (tenureWiseSelectedIssuerFullData.isNotEmpty()) {
                    (activity as MainActivity).transactFragment(EMICompareFragment().apply {
                        arguments = Bundle().apply {
                            putString("compareActionName", compareActionName)
                            putSerializable("type", action)
                            putParcelableArrayList(
                                "dataModal",
                                tenureWiseSelectedIssuerFullData as java.util.ArrayList<out Parcelable>
                            )
                        }
                    })
                } else if (!selectedIssuerNameData2.isNotEmpty() && selectedTenure?.isNotEmpty() == true) {
                    VFService.showToast(getString(R.string.please_select_one_issuer_bank))
                }
                else {
                    VFService.showToast(getString(R.string.please_select_tenure))
                }
                Log.d("TWSIFD:- ", tenureWiseSelectedIssuerFullData.toString())
            }
        }
        //endregion
    }
    //endregion

    //region===================Compare By Bank Select Event:-
    private fun compareByTenureSelectEventMethod() {
        showEditTextSelected(null, binding?.compareByTenureCV, requireContext())
        showEditTextUnSelected(null, binding?.compareByBankCV, requireContext())
        iDialog?.showProgress()
        binding?.tenureHeadingText?.visibility = View.VISIBLE
        binding?.tenureHeadingText?.text = getString(R.string.select_tenure)
        binding?.tenureRV?.visibility = View.VISIBLE
        binding?.issuerRV?.visibility = View.GONE
        binding?.selectBankHeadingText?.visibility = View.GONE
        binding?.selectBankHeadingText?.text = getString(R.string.select_banks_to_compare)
        binding?.selectAllBankCheckButton?.visibility = View.GONE
        issuerListAdapter.selectAllIssuerBank(
            false,
            CompareActionType.COMPARE_BY_TENURE.compareType
        )
        issuerListAdapter.unCheckAllIssuerBankRadioButton()
        compareActionName = CompareActionType.COMPARE_BY_TENURE.compareType
        iDialog?.hideProgress()
    }
    //endregion

    //region======================Compare By Tenure Select Event Method:-
    private fun compareByBankSelectEventMethod() {
        showEditTextSelected(null, binding?.compareByBankCV, requireContext())
        showEditTextUnSelected(null, binding?.compareByTenureCV, requireContext())
        iDialog?.showProgress()
        binding?.tenureHeadingText?.visibility = View.GONE
        binding?.tenureRV?.visibility = View.GONE
        binding?.selectBankHeadingText?.visibility = View.VISIBLE
        binding?.selectBankHeadingText?.text = getString(R.string.select_bank_to_compare_tenure)
        binding?.selectAllBankCheckButton?.visibility = View.GONE
        runBlocking {
            temporaryAllIssuerList = allIssuerBankList.distinctBy { it.issuerID }.toMutableList()
        }
        issuerListAdapter.refreshBankList(temporaryAllIssuerList)
        issuerListAdapter.selectAllIssuerBank(false, CompareActionType.COMPARE_BY_BANK.compareType)
        issuerTenureListAdapter.unCheckAllTenureRadioButton()
        binding?.issuerRV?.visibility = View.VISIBLE
        compareActionName = CompareActionType.COMPARE_BY_BANK.compareType
        iDialog?.hideProgress()
    }
    //endregion

    //region===================OnTenureSelectedEvent:-
    private fun onTenureSelectedEvent(position: Int) {
        if (position > -1) {
            selectedTenure = temporaryAllTenureList[position].bankTenure ?: ""
            iDialog?.showProgress()
            binding?.selectAllBankCheckButton?.isChecked = false
            Log.d("GsonResponse:- ", Gson().toJson(allIssuerBankList))
            refreshedBanksByTenure = allIssuerBankList.filter {
                it.issuerBankTenure == temporaryAllTenureList[position].bankTenure
            } as MutableList<IssuerBankModal>
            if (refreshedBanksByTenure.isNotEmpty()) {
                refreshIssuerBankOnTenureSelection(refreshedBanksByTenure)
                issuerListAdapter.selectAllIssuerBank(
                    false,
                    CompareActionType.COMPARE_BY_TENURE.compareType
                )
            } else {
                binding?.selectBankHeadingText?.visibility = View.GONE
                binding?.selectAllBankCheckButton?.visibility = View.GONE
                binding?.issuerRV?.visibility = View.GONE
            }
            iDialog?.hideProgress()
        }
    }
    //endregion

    //region===================Refresh Issuer Bank List on Tenure Selection in RecyclerView:-
    private fun refreshIssuerBankOnTenureSelection(refreshBankList: MutableList<IssuerBankModal>) {
        issuerListAdapter.refreshBankList(refreshBankList)
        if (refreshBankList.size == 1) {
            binding?.selectBankHeadingText?.text = getString(R.string.select_bank)
            binding?.selectAllBankCheckButton?.visibility = View.GONE
        } else {
            binding?.selectBankHeadingText?.text = getString(R.string.select_banks_to_compare)
            binding?.selectAllBankCheckButton?.visibility = View.VISIBLE
        }
        binding?.selectBankHeadingText?.visibility = View.VISIBLE
        binding?.issuerRV?.visibility = View.VISIBLE
        iDialog?.hideProgress()
    }
    //endregion

    //region=====================UnSelect Select All CheckBox:-
    private fun selectAllUncheck(isChecked: Boolean) {
        if (!isChecked)
            binding?.selectAllBankCheckButton?.isChecked = false
    }
    //endregion

    override fun onStop() {
        super.onStop()
        compareActionName = null
        allIssuerBankList.clear()
        allIssuerTenureList.clear()
        temporaryAllIssuerList.clear()
        temporaryAllTenureList.clear()
        refreshedBanksByTenure.clear()
        totalRecord = "0"
        moreDataFlag = "0"
        totalRecord = "0"
        perPageRecord = "0"
        selectedTenure = null
    }

    override fun onDetach() {
        super.onDetach()
        iDialog = null
    }

    //region=================Parse and Stubbing BankEMI Data To List:-
    private fun parseAndStubbingBankEMICatalogueDataToList(bankEMICatalogueHostResponseData: String) {
        if (!TextUtils.isEmpty(bankEMICatalogueHostResponseData)) {
            val parsingDataWithVerticalLineSeparator =
                parseDataListWithSplitter("|", bankEMICatalogueHostResponseData)
            if (parsingDataWithVerticalLineSeparator.isNotEmpty()) {
                moreDataFlag = parsingDataWithVerticalLineSeparator[0]
                perPageRecord = parsingDataWithVerticalLineSeparator[1]
                totalRecord = (totalRecord?.toInt()?.plus(perPageRecord?.toInt() ?: 0)).toString()
                //Store DataList in Temporary List and remove first 2 index values to get sublist from 2nd index till dataList size
                // and iterate further on record data only:-
                var tempDataList = mutableListOf<String>()
                tempDataList = parsingDataWithVerticalLineSeparator.subList(
                    2,
                    parsingDataWithVerticalLineSeparator.size
                )

                //region=========================Stub Data in AllIssuerList:-
                for (i in tempDataList.indices) {
                    if (!TextUtils.isEmpty(tempDataList[i])) {
                        val splitData = parseDataListWithSplitter(
                            SplitterTypes.CARET.splitter,
                            tempDataList[i]
                        )
                        allIssuerBankList.add(
                            IssuerBankModal(
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
                                splitData[20], splitData[21],
                                splitData[22], splitData[23]
                            )
                        )
                    }
                }
                //endregion

                //region========================Stub Data in AllTenureList:-
                if (allIssuerBankList.isNotEmpty()) {
                    val dataLength = allIssuerBankList.size
                    for (i in 0 until dataLength)
                        allIssuerTenureList.add(
                            TenureBankModal(
                                allIssuerBankList[i].issuerBankTenure,
                                isTenureSelected = false
                            )
                        )
                }
                //endregion

                Log.d("AllIssuerList:- ", allIssuerBankList.toString())
                Log.d("AllTenureList:- ", allIssuerTenureList.toString())

                if (allIssuerBankList.isNotEmpty() && allIssuerTenureList.isNotEmpty()) {
                    lifecycleScope.launch(Dispatchers.IO) {
                        AppPreference.setLongData(
                            AppPreference.ENQUIRY_AMOUNT_FOR_EMI_CATALOGUE,
                            enquiryAmount
                        )
                        temporaryAllIssuerList =
                            allIssuerBankList.distinctBy { it.issuerID }.toMutableList()
                        temporaryAllTenureList =
                            allIssuerTenureList.distinctBy { it.bankTenure }.toMutableList()
                        withContext(Dispatchers.Main) {
                            compareActionName = CompareActionType.COMPARE_BY_BANK.compareType
                            issuerListAdapter.refreshBankList(temporaryAllIssuerList)
                            issuerTenureListAdapter.refreshTenureList(temporaryAllTenureList)
                            Log.d("DistinctIssuer:-", temporaryAllIssuerList.toString())
                            Log.d("DistinctTenure:-", temporaryAllIssuerList.toString())

                            //region===============Below Code will Execute EveryTime Merchant Came to This Screen:-
                            compareByBankSelectEventMethod()
                            iDialog?.hideProgress()
                            //endregion
                        }
                    }
                } else
                    iDialog?.hideProgress()
            } else
                iDialog?.hideProgress()
        } else
            iDialog?.hideProgress()
        //endregion
    }
    //endregion
}

//region===============Below adapter is used to show the All Issuer Bank lists available:-
class IssuerListAdapter(
    var issuerList: MutableList<IssuerBankModal>,
    var emiCatalogueImagesMap: MutableMap<String, Uri>?
) :
    RecyclerView.Adapter<IssuerListAdapter.IssuerListViewHolder>() {

    var compareActionName: String? = null
    var index = -1

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): IssuerListViewHolder {
        val itemBinding = ItemEmiIssuerListBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return IssuerListViewHolder(itemBinding)
    }

    override fun onBindViewHolder(holder: IssuerListViewHolder, position: Int) {
        val modal = issuerList[position]

        //If Condition to check whether we have got EMI catalogue Data from File which we save after Settlement Success, Image Data from Host:-
        if (emiCatalogueImagesMap?.isNotEmpty() == true && emiCatalogueImagesMap?.containsKey(modal.issuerID) == true) {
            val imageUri: Uri? = emiCatalogueImagesMap?.get(modal.issuerID)
            val bitmap =
                MediaStore.Images.Media.getBitmap(VerifoneApp.appContext.contentResolver, imageUri)
            if (bitmap != null) {
                holder.viewBinding.issuerBankLogo.setImageBitmap(bitmap)
            }
        }
        //Else Condition to Read Images From Drawable and Show:-
        else {
            val resource: Int? = when (modal.issuerBankName?.toLowerCase(Locale.ROOT)?.trim()) {
                "hdfc bank cc" -> R.drawable.hdfc_issuer_icon
                "hdfc bank dc" -> R.drawable.hdfc_dc_issuer_icon
                "sbi card" -> R.drawable.sbi_issuer_icon
                "citi" -> R.drawable.citi_issuer_icon
                "icici" -> R.drawable.icici_issuer_icon
                "yes" -> R.drawable.yes_issuer_icon
                "kotak" -> R.drawable.kotak_issuer_icon
                "rbl" -> R.drawable.rbl_issuer_icon
                "scb" -> R.drawable.scb_issuer_icon
                "axis" -> R.drawable.axis_issuer_icon
                "indusind" -> R.drawable.indusind_issuer_icon
                else -> null
            }
            if (resource != null) {
                holder.viewBinding.issuerBankLogo.setImageResource(resource)
            }
        }

        //region===============Below Code will only execute in case of Multiple Selection:-
        if (compareActionName == CompareActionType.COMPARE_BY_TENURE.compareType) {
            if (modal.isIssuerSelected == true) {
                holder.viewBinding.issuerCheckedIV.visibility = View.VISIBLE
            } else {
                holder.viewBinding.issuerCheckedIV.visibility = View.GONE
            }
        }
        //endregion

        holder.viewBinding.issuerBankLogo.setOnClickListener {
            if (compareActionName == CompareActionType.COMPARE_BY_TENURE.compareType) {
                modal.isIssuerSelected = !modal.isIssuerSelected!!
                holder.viewBinding.issuerCheckedIV.visibility = View.VISIBLE
            } else {
                index = position
            }
            notifyDataSetChanged()
        }

        //region=================Below Code will execute in case of Single Issuer Bank Selection:-
        if (compareActionName == CompareActionType.COMPARE_BY_BANK.compareType) {
            if (index == position) {
                modal.isIssuerSelected = true
                holder.viewBinding.issuerCheckedIV.visibility = View.VISIBLE
            } else {
                modal.isIssuerSelected = false
                holder.viewBinding.issuerCheckedIV.visibility = View.GONE
            }
        }
        //endregion
    }

    override fun getItemCount(): Int = issuerList.size

    inner class IssuerListViewHolder(var viewBinding: ItemEmiIssuerListBinding) :
        RecyclerView.ViewHolder(viewBinding.root)


    //region==================Refresh Bank List Data on UI:-
    fun refreshBankList(refreshBankList: MutableList<IssuerBankModal>) {
        this.issuerList = refreshBankList
        notifyDataSetChanged()
    }
    //endregion

    //region===============Select All Issuer Bank:-
    fun selectAllIssuerBank(isAllStatus: Boolean, compareAction: String) {
        val dataSize = issuerList.size
        this.compareActionName = compareAction
        for (i in 0 until dataSize) {
            when (isAllStatus) {
                true -> issuerList[i].isIssuerSelected = true
                false -> issuerList[i].isIssuerSelected = false
            }
        }
        notifyDataSetChanged()
    }
    //endregion

    //region==================Uncheck All Tenure RadioButtons:-
    fun unCheckAllIssuerBankRadioButton() {
        index = -1
        notifyDataSetChanged()
    }
    //endregion
}
//endregion

//region===============Below adapter is used to show the All Tenure for Issuer Bank lists available:-
class IssuerTenureListAdapter(
    var issuerTenureList: MutableList<TenureBankModal>,
    var cb: (Int) -> Unit
) :
    RecyclerView.Adapter<IssuerTenureListAdapter.IssuerTenureListViewHolder>() {

    var index = -1
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): IssuerTenureListViewHolder {
        val itemBinding = ItemEmiIssuerTenureBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return IssuerTenureListViewHolder(itemBinding)
    }

    override fun onBindViewHolder(holder: IssuerTenureListViewHolder, position: Int) {
        val modal = issuerTenureList[position]
        val tenureData = "${modal.bankTenure} Months"
        holder.viewBinding.tenureRadioButton.text = tenureData

        //region===============Below Code will only execute in case of Single Radio Button Selection:-
        holder.viewBinding.tenureRadioButton.isChecked = index == position
        //endregion
    }

    override fun getItemCount(): Int = issuerTenureList.size

    inner class IssuerTenureListViewHolder(var viewBinding: ItemEmiIssuerTenureBinding) :
        RecyclerView.ViewHolder(viewBinding.root) {
        init {
            viewBinding.tenureRadioButton.setOnClickListener {
                cb(absoluteAdapterPosition)
                index = absoluteAdapterPosition
                notifyDataSetChanged()
            }
        }
    }

    //region==================Uncheck All Tenure RadioButtons:-
    fun unCheckAllTenureRadioButton() {
        index = -1
        notifyDataSetChanged()
    }
    //endregion

    //region===================Refresh EMI Tenure List:-
    fun refreshTenureList(refreshTenureList: MutableList<TenureBankModal>) {
        this.issuerTenureList = refreshTenureList
        notifyDataSetChanged()
    }
    //endregion
}
//endregion

//region====================Data Modal Class:-
@Parcelize
data class IssuerBankModal(
    var issuerBankTenure: String,
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
    var cashBackCalculatedValue: String,
    var issuerID: String,
    var issuerBankName: String?,
    var issuerSchemeID: String?,
    var isIssuerSelected: Boolean? = false
) : Parcelable

data class TenureBankModal(val bankTenure: String?, var isTenureSelected: Boolean? = false) :
    Serializable
//endregion

//region ENUM:-
enum class CompareActionType(val compareType: String) {
    COMPARE_BY_TENURE("compare_by_tenure"),
    COMPARE_BY_BANK("compare_by_bank")
}
//endregion