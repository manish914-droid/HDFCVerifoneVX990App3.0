package com.example.verifonevx990app.emiCatalogue

import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.verifonevx990app.R
import com.example.verifonevx990app.databinding.EmiCompareFragmentBinding
import com.example.verifonevx990app.databinding.ItemEmiCompareViewBinding
import com.example.verifonevx990app.vxUtils.IDialog
import com.example.verifonevx990app.vxUtils.UiAction
import com.example.verifonevx990app.vxUtils.divideAmountBy100
import com.google.gson.Gson
import java.util.*

class EMICompareFragment : Fragment() {

    private var iDialog: IDialog? = null
    private var dataList: MutableList<IssuerBankModal> = mutableListOf()
    private var binding: EmiCompareFragmentBinding? = null
    private val compareActionName by lazy { arguments?.getString("compareActionName") ?: "" }
    private val emiCompareAdapter by lazy {
        EMICompareAdapter(
            compareActionName,
            dataList,
            ::onItemDeleteClick
        )
    }
    private val action by lazy { arguments?.getSerializable("type") ?: "" }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        if (context is IDialog) iDialog = context
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = EmiCompareFragmentBinding.inflate(layoutInflater, container, false)
        return binding?.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        dataList =
            arguments?.getParcelableArrayList<IssuerBankModal>("dataModal") as MutableList<IssuerBankModal>
        Log.d("Data:- ", Gson().toJson(dataList))

        if (action == UiAction.BRAND_EMI_CATALOGUE) {
            binding?.subHeaderView?.subHeaderText?.text = getString(R.string.brandEmiCatalogue)
            binding?.subHeaderView?.headerImage?.setImageResource(R.drawable.ic_brand_emi_catalogue)
        } else {
            binding?.subHeaderView?.subHeaderText?.text = getString(R.string.bankEmiCatalogue)
            binding?.subHeaderView?.headerImage?.setImageResource(R.drawable.ic_bank_emi)
        }


        //region=============Show/Hide Compare By Tenure and Compare By Bank Buttons:-
        when (compareActionName) {
            CompareActionType.COMPARE_BY_BANK.compareType -> {
                binding?.compareByBankCV?.visibility = View.VISIBLE
                binding?.compareByTenureCV?.visibility = View.GONE
                binding?.issuerBankIcon?.visibility = View.VISIBLE
                binding?.tenureText?.visibility = View.GONE

                val resource: Int? =
                    when (dataList[0].issuerBankName?.toLowerCase(Locale.ROOT)?.trim()) {
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
                    binding?.issuerBankIcon?.setImageResource(resource)
                }
            }

            CompareActionType.COMPARE_BY_TENURE.compareType -> {
                binding?.compareByBankCV?.visibility = View.GONE
                binding?.compareByTenureCV?.visibility = View.VISIBLE
                binding?.issuerBankIcon?.visibility = View.GONE
                binding?.tenureText?.visibility = View.VISIBLE

                if (dataList.isNotEmpty()) {
                    val tenureData = "${dataList[0].issuerBankTenure} Months"
                    binding?.tenureText?.text = tenureData
                }
            }
        }
        //endregion

        binding?.subHeaderView?.backImageButton?.setOnClickListener { parentFragmentManager.popBackStackImmediate() }

        //region============Setting Up RecyclerView:-
        binding?.compareRV?.apply {
            layoutManager =
                LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
            if (dataList.size > 1) {
                addItemDecoration(
                    DividerItemDecoration(
                        requireContext(),
                        DividerItemDecoration.HORIZONTAL
                    )
                )
            }
            adapter = emiCompareAdapter
        }
        //endregion
    }

    //region=================Method To Delete RecyclerView Cell on click of Delete Icon:-
    private fun onItemDeleteClick(position: Int) {
        if (position > -1) {
            Log.d("PositionClicked:- ", position.toString())
            dataList.removeAt(position)
            emiCompareAdapter.refreshAdapterList(dataList)

            if (dataList.isEmpty())
                parentFragmentManager.popBackStackImmediate()
        }
    }
    //endregion

    override fun onDetach() {
        super.onDetach()
        iDialog = null
    }
}

//region===============Below adapter is used to show the All Issuer Bank lists available:-
class EMICompareAdapter(
    var compareActionName: String,
    var dataList: MutableList<IssuerBankModal>,
    var cb: (Int) -> Unit
) :
    RecyclerView.Adapter<EMICompareAdapter.EMICompareViewHolder>() {

    private var compareDataList = mutableListOf<IssuerBankModal>()

    init {
        if (dataList.isNotEmpty())
            compareDataList.addAll(dataList)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): EMICompareViewHolder {
        val itemBinding =
            ItemEmiCompareViewBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return EMICompareViewHolder(itemBinding)
    }

    override fun onBindViewHolder(holder: EMICompareViewHolder, position: Int) {
        val modal = compareDataList[position]
        when (compareActionName) {
            CompareActionType.COMPARE_BY_BANK.compareType -> {
                val tenureData = "${modal.issuerBankTenure} Months"
                holder.viewBinding.topHeaderBT.text = tenureData
            }
            CompareActionType.COMPARE_BY_TENURE.compareType -> {
                holder.viewBinding.topHeaderBT.text = modal.issuerBankName
            }
        }

        holder.viewBinding.transactionAmountTV.text =
            divideAmountBy100(modal.transactionAmount.toInt()).toString()
        holder.viewBinding.discountAmount.text =
            divideAmountBy100(modal.discountAmount.toInt()).toString()
        holder.viewBinding.loanAmount.text = divideAmountBy100(modal.loanAmount.toInt()).toString()
        holder.viewBinding.roi.text = divideAmountBy100(modal.tenureInterestRate.toInt()).toString()
        holder.viewBinding.emiAmount.text = divideAmountBy100(modal.emiAmount.toInt()).toString()
        holder.viewBinding.totalWithInterest.text =
            divideAmountBy100(modal.netPay.toInt()).toString()
        holder.viewBinding.cashbackAmount.text =
            divideAmountBy100(modal.cashBackAmount.toInt()).toString()
        holder.viewBinding.netCost.text = divideAmountBy100(modal.netPay.toInt()).toString()
        holder.viewBinding.additionalOffer.text = ""
    }

    override fun getItemCount(): Int = compareDataList.size

    inner class EMICompareViewHolder(var viewBinding: ItemEmiCompareViewBinding) :
        RecyclerView.ViewHolder(viewBinding.root) {
        init {
            viewBinding.issuerDeleteIV.setOnClickListener { cb(absoluteAdapterPosition) }
        }
    }

    //region==========================Below Method is used to refresh Adapter New Data after Delete Cell:-
    fun refreshAdapterList(refreshList: MutableList<IssuerBankModal>) {
        val diffUtilCallBack = EMICompareDiffUtil(this.compareDataList, refreshList)
        val diffResult = DiffUtil.calculateDiff(diffUtilCallBack)
        this.compareDataList.clear()
        this.compareDataList.addAll(refreshList)
        diffResult.dispatchUpdatesTo(this)
    }
    //endregion
}
//endregion