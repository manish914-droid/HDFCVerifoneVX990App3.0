package com.example.verifonevx990app.emiCatalogue

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Parcelable
import android.text.TextUtils
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.verifonevx990app.R
import com.example.verifonevx990app.bankEmiEnquiry.CreateEMIEnquiryTransactionPacket
import com.example.verifonevx990app.bankEmiEnquiry.EMiEnquiryOnPosActivity
import com.example.verifonevx990app.bankEmiEnquiry.SyncEmiEnquiryToHost
import com.example.verifonevx990app.bankemi.BankEMIDataModal
import com.example.verifonevx990app.databinding.FragmentEmiIssuerListBinding
import com.example.verifonevx990app.databinding.ItemEmiIssuerListBinding
import com.example.verifonevx990app.databinding.ItemEmiIssuerTenureBinding
import com.example.verifonevx990app.main.MainActivity
import com.example.verifonevx990app.main.SplitterTypes
import com.example.verifonevx990app.realmtables.BrandEMIDataTable
import com.example.verifonevx990app.realmtables.IssuerParameterTable
import com.example.verifonevx990app.vxUtils.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

class EMIIssuerList : Fragment() {
    private var binding: FragmentEmiIssuerListBinding? = null
    private var allIssuers: ArrayList<IssuerParameterTable> = arrayListOf()
    private var allIssuerBankList: MutableList<IssuerBankModal> = mutableListOf()
    private var allIssuerTenureList: MutableList<TenureBankModal> = mutableListOf()
    private val enquiryAmtStr by lazy { arguments?.getString("enquiryAmt") ?: "0" }
    private val mobileNumber by lazy { arguments?.getString("mobileNumber") ?: "" }
    private var mobileNumberOnOff: Boolean = false
    private val action by lazy { arguments?.getSerializable("type") ?: "" }
    private val enquiryAmount by lazy { ((enquiryAmtStr.toFloat()) * 100).toLong() }
    private val issuerListAdapter by lazy {
        IssuerListAdapter(
            allIssuerBankList,
            ::selectAllUncheck
        )
    }
    private val issuerTenureListAdapter by lazy {
        IssuerTenureListAdapter(
            allIssuerTenureList,
            ::onTenureSelectedEvent
        )
    }
    private var firstClick = true
    private var iDialog: IDialog? = null
    private var brandEmiData: BrandEMIDataTable? = null
    private var moreDataFlag = "0"
    private var totalRecord: String? = "0"
    private var perPageRecord: String? = "0"
    private var bankNameList = mutableListOf<String>()
    private var bankEMISchemesDataList: MutableList<BankEMIDataModal> = mutableListOf()

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

        if (action == UiAction.BRAND_EMI_CATALOGUE) {
            binding?.subHeaderView?.subHeaderText?.text = getString(R.string.brandEmiCatalogue)
            binding?.subHeaderView?.headerImage?.setImageResource(R.drawable.ic_brand_emi_catalogue)
        } else {
            binding?.subHeaderView?.subHeaderText?.text = getString(R.string.bankEmiCatalogue)
            binding?.subHeaderView?.headerImage?.setImageResource(R.drawable.ic_bank_emi)
        }

        binding?.subHeaderView?.backImageButton?.setOnClickListener { parentFragmentManager.popBackStackImmediate() }

        //region============Stub Dummy Data in Tenure List and Issuer Bank List for Test use:-
        allIssuerBankList.add(
            IssuerBankModal(
                "HDFC DC",
                "3 MONTHS",
                "200",
                R.drawable.hdfc_dc_issuer_icon
            )
        )
        allIssuerBankList.add(
            IssuerBankModal(
                "AXIS",
                "3 MONTHS",
                "201",
                R.drawable.axis_issuer_icon
            )
        )
        allIssuerBankList.add(IssuerBankModal("SCB", "6 MONTHS", "202", R.drawable.scb_issuer_icon))
        allIssuerBankList.add(
            IssuerBankModal(
                "HDFC",
                "6 MONTHS",
                "203",
                R.drawable.hdfc_issuer_icon
            )
        )
        allIssuerBankList.add(
            IssuerBankModal(
                "ICICI",
                "9 MONTHS",
                "204",
                R.drawable.icici_issuer_icon
            )
        )
        allIssuerBankList.add(
            IssuerBankModal(
                "CITI",
                "9 MONTHS",
                "205",
                R.drawable.citi_issuer_icon
            )
        )
        allIssuerBankList.add(
            IssuerBankModal(
                "SBI Card",
                "12 MONTHS",
                "206",
                R.drawable.sbi_issuer_icon
            )
        )
        allIssuerBankList.add(
            IssuerBankModal(
                "Indusind",
                "12 MONTHS",
                "207",
                R.drawable.indusind_issuer_icon
            )
        )
        allIssuerBankList.add(
            IssuerBankModal(
                "KOTAK",
                "20 MONTHS",
                "208",
                R.drawable.kotak_issuer_icon
            )
        )

        allIssuerTenureList.add(TenureBankModal("3 MONTHS"))
        allIssuerTenureList.add(TenureBankModal("6 MONTHS"))
        allIssuerTenureList.add(TenureBankModal("9 MONTHS"))
        allIssuerTenureList.add(TenureBankModal("12 MONTHS"))
        allIssuerTenureList.add(TenureBankModal("15 MONTHS"))
        allIssuerTenureList.add(TenureBankModal("20 MONTHS"))
        allIssuerTenureList.add(TenureBankModal("24 MONTHS"))
        allIssuerTenureList.add(TenureBankModal("36 MONTHS"))
        allIssuerTenureList.add(TenureBankModal("NO EMI ONLY CASHBACK"))
        //endregion

        val ipt = IssuerParameterTable.selectFromIssuerParameterTableOnConditionBase()
        allIssuers = ipt as ArrayList<IssuerParameterTable>

        //region===========Getting Brand , Category , Sub-Category and Product Data from Brand Table:-
        brandEmiData = runBlocking(Dispatchers.IO) { BrandEMIDataTable.getAllEMIData() }
        //endregion

        //region=============Show/Hide Select All Button on basis of mobileNumberOnOff:-
        //binding?.selectAllCV?.visibility = View.VISIBLE
        binding?.headingText?.text = getString(R.string.calculate_and_compare_emi_offers)
        //endregion

        //region==========Binding Tenure RecyclerView:-
        binding?.tenureRV?.apply {
            layoutManager = GridLayoutManager(activity, 3)
            adapter = issuerTenureListAdapter
        }
        //endregion
        //region==========Binding Issuer Bank RecyclerView SetUp:-
        binding?.issuerRV?.apply {
            layoutManager = GridLayoutManager(activity, 3)
            adapter = issuerListAdapter
        }
        //endregion

        //region=================Select All CheckBox OnClick Event:-
        binding?.selectAllBankCheckButton?.setOnCheckedChangeListener { _, ischecked ->
            if (ischecked) {
                binding?.selectAllBankCheckButton?.text = getString(R.string.unselect_all_banks)
                iDialog?.showProgress()
                issuerListAdapter.selectAllIssuerBank(true)
                iDialog?.hideProgress()
            } else {
                binding?.selectAllBankCheckButton?.text = getString(R.string.select_all_banks)
                iDialog?.showProgress()
                issuerListAdapter.selectAllIssuerBank(false)
                iDialog?.hideProgress()
            }
        }
        //endregion

        //region==============OnClick event of Compare By Tenure CardView:-
        binding?.compareByTenure?.setOnClickListener {
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
            issuerListAdapter.refreshBankList(allIssuerBankList, isFirstFilter = true)
            issuerListAdapter.selectAllIssuerBank(false)
            issuerListAdapter.unCheckAllIssuerBankRadioButton()
            iDialog?.hideProgress()
        }
        //endregion

        // region==============OnClick event of Compare By Bank CardView:-
        binding?.compareByBank?.setOnClickListener {
            showEditTextSelected(null, binding?.compareByBankCV, requireContext())
            showEditTextUnSelected(null, binding?.compareByTenureCV, requireContext())
            iDialog?.showProgress()
            binding?.tenureHeadingText?.visibility = View.GONE
            binding?.tenureRV?.visibility = View.GONE
            binding?.selectBankHeadingText?.visibility = View.VISIBLE
            binding?.selectBankHeadingText?.text = getString(R.string.select_bank_to_compare_tenure)
            binding?.selectAllBankCheckButton?.visibility = View.GONE
            issuerListAdapter.refreshBankList(allIssuerBankList, isFirstFilter = false)
            issuerListAdapter.selectAllIssuerBank(false)
            issuerTenureListAdapter.unCheckAllTenureRadioButton()
            binding?.issuerRV?.visibility = View.VISIBLE
            iDialog?.hideProgress()
        }
        //endregion

        //region===============Proceed EMI Catalogue button onClick Event:-
        binding?.proceedEMICatalogue?.setOnClickListener {
            val dataLength = allIssuerBankList.size
            var selectedIssuerIDS = ""
            for (i in 0 until dataLength) {
                if (allIssuerBankList[i].isIssuerSelected == true) {
                    selectedIssuerIDS = "$selectedIssuerIDS,${allIssuerBankList[i].issuerID}"
                    bankNameList.add(allIssuerBankList[i].issuerBankName ?: "")
                }
            }
            Log.d("IssuerSelected:- ", selectedIssuerIDS)
            //proceedEMICatalogueWithIssuer(selectedIssuerIDS.substring(1, selectedIssuerIDS.length))
        }
        //endregion

    }

    //region===================OnTenureSelectedEvent:-
    private fun onTenureSelectedEvent(position: Int) {
        if (position > -1) {
            iDialog?.showProgress()
            binding?.selectAllBankCheckButton?.isChecked = false
            val refreshedBanks = allIssuerBankList.filter {
                it.issuerBankTenure == allIssuerTenureList[position].bankTenure
            } as MutableList<IssuerBankModal>
            if (refreshedBanks.isNotEmpty()) {
                refreshIssuerBankOnTenureSelection(refreshedBanks)
                issuerListAdapter.selectAllIssuerBank(false)
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
        issuerListAdapter.refreshBankList(refreshBankList, isFirstFilter = true)
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

    override fun onDetach() {
        super.onDetach()
        iDialog = null
    }

    //region==================Do EMI Enquiry:-
    private fun proceedEMICatalogueWithIssuer(selectedIssuerIDs: String) {
        val requestType = if (mobileNumberOnOff) "8" else "4"
        val skipRecordCount = "0"
        val brandID = if (!TextUtils.isEmpty(brandEmiData?.brandID)) brandEmiData?.brandID else "01"
        val productID =
            if (!TextUtils.isEmpty(brandEmiData?.productID)) brandEmiData?.productID else "0"
        val serialNumber = ""
        val imeiNumber = ""
        val emiAmount = enquiryAmount
        val capturedMobileNumber = mobileNumber
        if (allIssuers.size > 0) {
            (activity as MainActivity).showProgress()
            val dF57 =
                "$requestType^$skipRecordCount^$brandID^$productID^$serialNumber^$imeiNumber^$emiAmount^$selectedIssuerIDs^$capturedMobileNumber"
            Log.d("Field57:- ", dF57)
            val isoPacket =
                runBlocking(Dispatchers.IO) { CreateEMIEnquiryTransactionPacket(dF57).createTransactionPacket() }
            GlobalScope.launch(Dispatchers.IO) {
                SyncEmiEnquiryToHost(activity as MainActivity) { syncStatus, responseIsoReader, responseMsg ->
                    (activity as MainActivity).hideProgress()
                    if (syncStatus) {
                        if (mobileNumberOnOff) {
                            GlobalScope.launch(Dispatchers.Main) {
                                (activity as MainActivity).alertBoxWithAction(null,
                                    null,
                                    "",
                                    getString(R.string.sms_sent_to_mob),
                                    false,
                                    "",
                                    { alertPositiveCallback ->
                                        if (alertPositiveCallback)
                                            startActivity(
                                                Intent(
                                                    activity,
                                                    MainActivity::class.java
                                                ).apply {
                                                    flags =
                                                        Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_CLEAR_TASK
                                                })
                                    },
                                    {})
                            }
                        } else {
                            try {
                                parseAndStubbingBankEMIEnquiryDataToList(
                                    responseIsoReader?.isoMap?.get(
                                        57
                                    )?.parseRaw2String().toString()
                                ) { bankEmiEnquiryData ->
                                    (activity as MainActivity).transactFragment(
                                        EMiEnquiryOnPosActivity().apply {
                                            arguments = Bundle().apply {
                                                putParcelableArrayList(
                                                    "bankEnquiryData",
                                                    bankEmiEnquiryData as ArrayList<out Parcelable>
                                                )
                                                putString("enquiryAmt", enquiryAmtStr)
                                                putSerializable("type", action)
                                                if (bankNameList.size == 1)
                                                    putString("bankName", bankNameList[0])
                                            }
                                        })
                                }
                            } catch (ex: Exception) {
                                ex.printStackTrace()
                            }
                        }
                    } else {
                        VFService.showToast(responseMsg.toString())
                    }
                }.synToHost(isoPacket)
            }
        } else {
            VFService.showToast("Select an Issuer \nFor further process")
        }
    }
    //endregion

    //region=================Parse and Stubbing BankEMI Data To List:-
    private fun parseAndStubbingBankEMIEnquiryDataToList(
        bankEMIEnquiryHostResponseData: String, cb: (MutableList<BankEMIDataModal>) -> Unit
    ) {

        if (!TextUtils.isEmpty(bankEMIEnquiryHostResponseData)) {
            val parsingDataWithCurlyBrace =
                parseDataListWithSplitter("}", bankEMIEnquiryHostResponseData)
            val parsingDataWithVerticalLineSeparator =
                parseDataListWithSplitter("|", parsingDataWithCurlyBrace[0])
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

                for (i in tempDataList.indices) {
                    if (!TextUtils.isEmpty(tempDataList[i])) {
                        val splitData = parseDataListWithSplitter(
                            SplitterTypes.CARET.splitter,
                            tempDataList[i]
                        )
                        bankEMISchemesDataList.add(
                            BankEMIDataModal(
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
                                splitData[20]
                            )
                        )
                    }
                }
                Log.d("Total BankEMI Data:- ", bankEMISchemesDataList.toString())
                Log.d("Total BankEMI TAndC:- ", parsingDataWithCurlyBrace[1])
                cb(bankEMISchemesDataList)

            }
        }

        //endregion
    }
    //endregion
}

//region===============Below adapter is used to show the All Issuer Bank lists available:-
class IssuerListAdapter(var issuerList: MutableList<IssuerBankModal>, var cb: (Boolean) -> Unit) :
    RecyclerView.Adapter<IssuerListAdapter.IssuerListViewHolder>() {

    var isFirstFilter: Boolean = false
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
        holder.viewBinding.issuerBankLogo.setImageResource(modal.bankLogo)

        //region===============Below Code will only execute in case of Multiple Selection:-
        if (isFirstFilter) {
            if (modal.isIssuerSelected == true) {
                holder.viewBinding.issuerCheckedIV.visibility = View.VISIBLE
            } else {
                holder.viewBinding.issuerCheckedIV.visibility = View.GONE
            }
        }
        //endregion

        holder.viewBinding.issuerBankLogo.setOnClickListener {
            if (isFirstFilter) {
                modal.isIssuerSelected = !modal.isIssuerSelected!!
                holder.viewBinding.issuerCheckedIV.visibility = View.VISIBLE
                cb(modal.isIssuerSelected ?: false)
            } else {
                index = position
            }
            notifyDataSetChanged()
        }

        //region=================Below Code will execute in case of Single Issuer Bank Selection:-
        if (!isFirstFilter) {
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
    fun refreshBankList(refreshBankList: MutableList<IssuerBankModal>, isFirstFilter: Boolean) {
        this.issuerList = refreshBankList
        this.isFirstFilter = isFirstFilter
        notifyDataSetChanged()
    }
    //endregion

    //region===============Select All Issuer Bank:-
    fun selectAllIssuerBank(isAllStatus: Boolean) {
        val dataSize = issuerList.size
        for (i in 0 until dataSize) {
            when (isAllStatus) {
                true -> issuerList[i].isIssuerSelected = true
                else -> issuerList[i].isIssuerSelected = false
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
        holder.viewBinding.tenureRadioButton.text = modal.bankTenure

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
}
//endregion

//region====================Data Modal Class:-
data class IssuerBankModal(
    val issuerBankName: String?, val issuerBankTenure: String?,
    var issuerID: String, var bankLogo: Int, var isIssuerSelected: Boolean? = false
)

data class TenureBankModal(val bankTenure: String?, var isTenureSelected: Boolean? = false)
//endregion