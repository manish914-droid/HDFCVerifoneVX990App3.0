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
import com.example.verifonevx990app.main.MainActivity
import com.example.verifonevx990app.main.SplitterTypes
import com.example.verifonevx990app.realmtables.BrandEMIDataTable
import com.example.verifonevx990app.realmtables.IssuerParameterTable
import com.example.verifonevx990app.vxUtils.IDialog
import com.example.verifonevx990app.vxUtils.UiAction
import com.example.verifonevx990app.vxUtils.VFService
import com.example.verifonevx990app.vxUtils.parseDataListWithSplitter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

class EMIIssuerList : Fragment() {
    private var binding: FragmentEmiIssuerListBinding? = null
    private var allIssuers: ArrayList<IssuerParameterTable> = arrayListOf()
    private val enquiryAmtStr by lazy { arguments?.getString("enquiryAmt") ?: "0" }
    private val mobileNumber by lazy { arguments?.getString("mobileNumber") ?: "" }
    private var mobileNumberOnOff: Boolean = false
    private val action by lazy { arguments?.getSerializable("type") ?: "" }
    private val enquiryAmount by lazy { ((enquiryAmtStr.toFloat()) * 100).toLong() }
    private val issuerListAdapter by lazy { IssuerListAdapter(allIssuers) }
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
            binding?.subHeaderView?.headerImage?.setImageResource(R.drawable.ic_brand_emi_sub_header_logo)
        } else {
            binding?.subHeaderView?.subHeaderText?.text = getString(R.string.bankEmiCatalogue)
            binding?.subHeaderView?.headerImage?.setImageResource(R.drawable.ic_bank_emi)
        }

        binding?.subHeaderView?.backImageButton?.setOnClickListener { parentFragmentManager.popBackStackImmediate() }

        val ipt = IssuerParameterTable.selectFromIssuerParameterTableOnConditionBase()
        allIssuers = ipt as ArrayList<IssuerParameterTable>

        //region===========Getting Brand , Category , Sub-Category and Product Data from Brand Table:-
        brandEmiData = runBlocking(Dispatchers.IO) { BrandEMIDataTable.getAllEMIData() }
        //endregion

        //region=============Show/Hide Select All Button on basis of mobileNumberOnOff:-
        binding?.selectAllCV?.visibility = View.VISIBLE
        binding?.headingText?.text = getString(R.string.please_select_one_or_multiple_issuer_banks)
        //endregion


        //region==========RecyclerView SetUp:-
        binding?.issuerRV?.apply {
            layoutManager = GridLayoutManager(activity, 2)
            adapter = issuerListAdapter
        }
        //endregion

        //region=================Select All OnClick Event:-
        binding?.selectAll?.setOnClickListener {
            if (firstClick) {
                iDialog?.showProgress()
                binding?.selectAllIV?.visibility = View.VISIBLE
                issuerListAdapter.selectAllIssuerBank(true)
                firstClick = false
                iDialog?.hideProgress()
            } else {
                iDialog?.showProgress()
                binding?.selectAllIV?.visibility = View.GONE
                issuerListAdapter.selectAllIssuerBank(false)
                firstClick = true
                iDialog?.hideProgress()
            }
        }
        //endregion

        //region===============Proceed EMI Catalogue button onClick Event:-
        binding?.proceedEMICatalogue?.setOnClickListener {
            val dataLength = allIssuers.size
            var selectedIssuerIDS = ""
            for (i in 0 until dataLength) {
                if (allIssuers[i].isIssuerSelected) {
                    selectedIssuerIDS = "$selectedIssuerIDS,${allIssuers[i].issuerId}"
                    bankNameList.add(allIssuers[i].issuerName)
                }
            }
            Log.d("IssuerSelected:- ", selectedIssuerIDS)
            proceedEMICatalogueWithIssuer(selectedIssuerIDS.substring(1, selectedIssuerIDS.length))
        }
        //endregion

    }

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

// Below adapter is used to show the Issuer lists available in issuer parameter table.
class IssuerListAdapter(var issuerList: ArrayList<IssuerParameterTable>) :
    RecyclerView.Adapter<IssuerListAdapter.IssuerListViewHolder>() {

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
        holder.viewBinding.issuerName.text = modal.issuerName

        //region===============Below Code will only execute in case of Multiple Selection:-
            if (modal.isIssuerSelected) {
                holder.viewBinding.issuerCheckedIV.visibility = View.VISIBLE
            } else {
                holder.viewBinding.issuerCheckedIV.visibility = View.GONE
            }
        //endregion

        holder.viewBinding.issuerNameCV.setOnClickListener {
                modal.isIssuerSelected = !modal.isIssuerSelected
                holder.viewBinding.issuerCheckedIV.visibility = View.VISIBLE
            notifyDataSetChanged()
        }
    }

    override fun getItemCount(): Int = issuerList.size

    inner class IssuerListViewHolder(var viewBinding: ItemEmiIssuerListBinding) :
        RecyclerView.ViewHolder(viewBinding.root)

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
}