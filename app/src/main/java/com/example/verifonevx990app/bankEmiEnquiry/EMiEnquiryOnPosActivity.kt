package com.example.verifonevx990app.bankEmiEnquiry

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.verifonevx990app.R
import com.example.verifonevx990app.bankemi.BankEMIDataModal
import com.example.verifonevx990app.databinding.EmiOnPosViewBinding
import com.example.verifonevx990app.databinding.ItemEmiEnquiryOnPosBinding
import com.example.verifonevx990app.main.MainActivity
import com.example.verifonevx990app.utils.printerUtils.PrintUtil
import com.example.verifonevx990app.vxUtils.IDialog
import com.example.verifonevx990app.vxUtils.UiAction
import com.example.verifonevx990app.vxUtils.VFService
import com.example.verifonevx990app.vxUtils.divideAmountBy100

class EMiEnquiryOnPosActivity : Fragment() {
    var viewBinding: EmiOnPosViewBinding? = null
    private var emiEnquiryData: ArrayList<BankEMIDataModal> = arrayListOf()
    private val action by lazy { arguments?.getSerializable("type") ?: "" }
    private var iDialog: IDialog? = null

    //enquiryAmt
    private val enquiryAmt by lazy { arguments?.getString("enquiryAmt") ?: "0" }
    private val bankName by lazy { arguments?.getString("bankName") ?: "UNKNOWN" }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        if (context is IDialog) iDialog = context
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        viewBinding = EmiOnPosViewBinding.inflate(layoutInflater, container, false)
        return viewBinding?.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        emiEnquiryData = arguments?.getParcelableArrayList("bankEnquiryData") ?: arrayListOf()
        viewBinding?.subHeaderView?.backImageButton?.setOnClickListener { navigateToDashboard() }
        viewBinding?.cancelButton?.setOnClickListener { navigateToDashboard() }

        if (action == UiAction.BRAND_EMI_CATALOGUE) {
            viewBinding?.subHeaderView?.subHeaderText?.text = getString(R.string.brandEmiCatalogue)
            viewBinding?.subHeaderView?.headerImage?.setImageResource(R.drawable.ic_brand_emi_sub_header_logo)
        } else {
            viewBinding?.subHeaderView?.subHeaderText?.text = getString(R.string.bankEmiCatalogue)
            viewBinding?.subHeaderView?.headerImage?.setImageResource(R.drawable.ic_bank_emi)
        }

        //region==============Print OnClick Event:-
        viewBinding?.printButton?.setOnClickListener {
            iDialog?.showProgress("Printing...")
            PrintUtil(requireContext()).printEMIEnquiry(
                requireContext(),
                emiEnquiryData,
                enquiryAmt,
                bankName
            ) { isSuccess, msg ->
                if (isSuccess) {
                    iDialog?.hideProgress()
                    navigateToDashboard()
                } else {
                    VFService.showToast(msg)
                    iDialog?.hideProgress()
                }
            }
        }
        viewBinding?.issuerRV?.layoutManager = LinearLayoutManager(requireContext())
        viewBinding?.issuerRV?.adapter =
            EMiEnquiryOnPosAdapter(emiEnquiryData, enquiryAmt)
    }

    //region=============Navigate To Main Screen:-
    private fun navigateToDashboard() =
        requireActivity().startActivity(Intent(requireContext(), MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_CLEAR_TASK
        })
    //endregion

    override fun onDetach() {
        super.onDetach()
        iDialog = null
    }
}

class EMiEnquiryOnPosAdapter(
    private val emiSchemeDataList: MutableList<BankEMIDataModal>?,
    private val enquiryAmt: String
) :
    RecyclerView.Adapter<EMiEnquiryOnPosAdapter.EMiEnquiryOnPosViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): EMiEnquiryOnPosViewHolder {
        val itemBinding =
            ItemEmiEnquiryOnPosBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return EMiEnquiryOnPosViewHolder(itemBinding)
    }

    override fun onBindViewHolder(holder: EMiEnquiryOnPosViewHolder, position: Int) {
        val modelData = emiSchemeDataList?.get(position)
        if (modelData != null) {
            holder.view.tvTransactionAmount.text = enquiryAmt
            val tenureDuration = "${modelData.tenure} Months"
            val tenureHeadingDuration = "${modelData.tenure} Months Scheme"
            var roi = divideAmountBy100(modelData.tenureInterestRate.toInt()).toString()
            var loanamt = divideAmountBy100(modelData.loanAmount.toInt()).toString()
            roi = "%.2f".format(roi.toDouble()) + " %"
            loanamt = "%.2f".format(loanamt.toDouble())

            holder.view.tvTotalRoi.text = roi
            holder.view.tvTenure.text = tenureDuration
            holder.view.tenureHeadingTv.text = tenureHeadingDuration
            holder.view.tvLoanAmount.text = loanamt
            holder.view.tvEmiAmount.text = divideAmountBy100(modelData.emiAmount.toInt()).toString()

            //If Discount Amount Available show this else if CashBack Amount show that:-
            if (modelData.discountAmount.toInt() != 0) {
                holder.view.tvDiscountAmount.text =
                    divideAmountBy100(modelData.discountAmount.toInt()).toString()
                holder.view.discountLL.visibility = View.VISIBLE
                holder.view.cashBackLL.visibility = View.GONE
            }
            if (modelData.cashBackAmount.toInt() != 0) {
                holder.view.tvCashbackAmount.text =
                    divideAmountBy100(modelData.cashBackAmount.toInt()).toString()
                holder.view.cashBackLL.visibility = View.VISIBLE
                holder.view.discountLL.visibility = View.GONE
            }

            holder.view.tvTotalEmiPay.text =
                divideAmountBy100(modelData.totalEmiPay.toInt()).toString()
            holder.view.tvTotalItrst.text =
                divideAmountBy100(modelData.totalInterestPay.toInt()).toString()
        }

    }

    override fun getItemCount(): Int = emiSchemeDataList?.size ?: 0

    class EMiEnquiryOnPosViewHolder(var view: ItemEmiEnquiryOnPosBinding) :
        RecyclerView.ViewHolder(view.root)
}