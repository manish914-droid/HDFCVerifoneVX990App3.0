package com.example.verifonevx990app.bankEmiEnquiry

import android.content.Intent
import android.os.Bundle
import android.os.Parcelable
import android.text.TextUtils
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.verifonevx990app.R
import com.example.verifonevx990app.bankemi.BankEMIDataModal
import com.example.verifonevx990app.databinding.FragmentIssuerListBinding
import com.example.verifonevx990app.databinding.ItemIssuerListViewholderBinding
import com.example.verifonevx990app.main.MainActivity
import com.example.verifonevx990app.main.SplitterTypes
import com.example.verifonevx990app.realmtables.IssuerParameterTable
import com.example.verifonevx990app.vxUtils.VFService
import com.example.verifonevx990app.vxUtils.parseDataListWithSplitter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

class IssuerListFragment : Fragment() {
    private var bindingView: FragmentIssuerListBinding? = null
    private var selectedIssuers: ArrayList<IssuerParameterTable> = arrayListOf()
    private var allIssuers: ArrayList<IssuerParameterTable> = arrayListOf()
    private val enquiryAmtStr by lazy { arguments?.getString("enquiryAmt") ?: "0" }
    private val mobileNumber by lazy { arguments?.getString("mobileNumber") ?: "" }
    private val isEnquiryOnMobile by lazy { mobileNumber != "" }
    private val enquiryAmount by lazy { ((enquiryAmtStr.toFloat()) * 100).toLong() }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        bindingView = FragmentIssuerListBinding.inflate(inflater, container, false)
        return bindingView?.root
    }

    private fun createSelectedIssuerList(position: Int, operation: EnumAddDeleteIssuer) {
        if (allIssuers.size > 0) {

            if (isEnquiryOnMobile) {
                when (operation) {
                    EnumAddDeleteIssuer.SELECTED -> {
                        selectedIssuers.add(allIssuers[position])
                    }
                    EnumAddDeleteIssuer.UNSELECTED -> {
                        selectedIssuers.remove(allIssuers[position])
                    }
                }
            } else {
                when (operation) {
                    EnumAddDeleteIssuer.SELECTED -> {
                        selectedIssuers.clear()
                        selectedIssuers.add(allIssuers[position])
                    }
                    EnumAddDeleteIssuer.UNSELECTED -> {
                        selectedIssuers.clear()
                    }
                }
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        bindingView?.emiSchemeTV?.visibility = View.VISIBLE
        bindingView?.emiSchemeTV?.text = getString(R.string.select_ssuer)

        val ipt = IssuerParameterTable.selectFromIssuerParameterTable()
        allIssuers = ipt as ArrayList<IssuerParameterTable>
        bindingView?.issuerRV?.layoutManager = LinearLayoutManager(activity)
        bindingView?.issuerRV?.adapter =
            IssuerListAdapter(allIssuers, isEnquiryOnMobile, ::createSelectedIssuerList)
        bindingView?.doEnquiryBtn?.setOnClickListener {
            if (selectedIssuers.size > 0) {
                (activity as MainActivity).showProgress()
                selectedIssuers.forEach {
                    Log.e(
                        "SELECT ISSUERS",
                        "Name --> " + it.issuerName + "  ID--> " + it.issuerId
                    )
                }
                Log.e("AMOUNT-->", enquiryAmtStr)
                var issuerIds = ""
                for (issuer in selectedIssuers) {
                    issuerIds = issuerIds + issuer.issuerId + ","
                }
                issuerIds = issuerIds.dropLast(1)
                val requestType = if (mobileNumber == "") "4" else "8"

                val dF57 = "$requestType^0^01^0^^^$enquiryAmount^$issuerIds^$mobileNumber"

                val isoPacket = CreateEMIEnquiryTransactionPacket(dF57).createTransactionPacket()
                GlobalScope.launch(Dispatchers.IO) {
                    SyncEmiEnquiryToHost(activity as MainActivity) { syncStatus, responseIsoReader, responseMsg ->
                        (activity as MainActivity).hideProgress()
                        if (syncStatus) {
                            if (isEnquiryOnMobile) {
                                GlobalScope.launch(Dispatchers.Main) {
                                    (activity as MainActivity).alertBoxWithAction(null,
                                        null,
                                        requireActivity().getString(R.string.success_message),
                                        requireActivity().getString(R.string.sms_sent_to_mob),
                                        false,
                                        getString(R.string.positive_button_ok),
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
                                        startActivity(
                                            Intent(
                                                activity,
                                                EMiEnquiryOnPosActivity::class.java
                                            ).apply {
                                                putParcelableArrayListExtra(
                                                    "bankEnquiryData",
                                                    bankEmiEnquiryData as java.util.ArrayList<out Parcelable>
                                                )
                                                putExtra("enquiryAmt", enquiryAmtStr)
                                                putExtra("bankName", selectedIssuers[0].issuerName)
                                            })

                                    }
                                } catch (ex: Exception) {
                                    // ex.printStackTrace()
                                    VFService.showToast("Some enquiry data missing")
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
    }

    private var moreDataFlag = "0"
    private var totalRecord: String? = "0"
    private var perPageRecord: String? = "0"
    private var bankEMISchemesDataList: MutableList<BankEMIDataModal> = mutableListOf()

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
                totalRecord =
                    (totalRecord?.toInt()?.plus(perPageRecord?.toInt() ?: 0)).toString()
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


}

// Below adapter is used to show the Issuer lists available in issuer parameter table.
class IssuerListAdapter(
    var issuerList: ArrayList<IssuerParameterTable>, var enquiryOnMobile: Boolean,
    var selectedListCb: (Int, EnumAddDeleteIssuer) -> Unit
) : RecyclerView.Adapter<IssuerListAdapter.IssuerListViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): IssuerListViewHolder {
        val itemBinding = ItemIssuerListViewholderBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return IssuerListViewHolder(itemBinding)
    }

    override fun onBindViewHolder(holder: IssuerListViewHolder, position: Int) {
        holder.viewBinding.issuerName.text = issuerList[position].issuerName
        holder.viewBinding.issuerCb.isChecked = issuerList[position].isIssuerSelected
    }

    override fun getItemCount(): Int = issuerList.size

    inner class IssuerListViewHolder(var viewBinding: ItemIssuerListViewholderBinding) :
        RecyclerView.ViewHolder(viewBinding.root) {
        init {
            viewBinding.issuerLayout.setOnClickListener {
                // In case of enquiry on Mobile(Multiple Selection)
                if (enquiryOnMobile) {
                    if (issuerList[adapterPosition].isIssuerSelected) {
                        issuerList[adapterPosition].isIssuerSelected = false
                        selectedListCb(adapterPosition, EnumAddDeleteIssuer.UNSELECTED)
                    } else {
                        issuerList[adapterPosition].isIssuerSelected = true
                        selectedListCb(adapterPosition, EnumAddDeleteIssuer.SELECTED)
                    }
                }
                // In case of enquiry on POS (Single Selection)
                else {
                    if (issuerList[adapterPosition].isIssuerSelected) {
                        issuerList[adapterPosition].isIssuerSelected = false
                        selectedListCb(adapterPosition, EnumAddDeleteIssuer.UNSELECTED)
                    } else {
                        issuerList.forEach { it.isIssuerSelected = false }
                        issuerList[adapterPosition].isIssuerSelected = true
                        selectedListCb(adapterPosition, EnumAddDeleteIssuer.SELECTED)
                    }
                }
                notifyDataSetChanged()
            }
        }
    }
}

// Enum for selection of Issuers handling
enum class EnumAddDeleteIssuer(var id: Int) {
    SELECTED(1),
    UNSELECTED(2)
}

//
class EMIEnquiryModel {
    var mobileNumber: String? = null
    var selectedIssuers: ArrayList<IssuerParameterTable>? = null
    var emiEnquiryAmount: String? = null

}


