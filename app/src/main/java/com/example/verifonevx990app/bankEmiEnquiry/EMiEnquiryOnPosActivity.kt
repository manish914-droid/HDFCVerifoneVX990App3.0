package com.example.verifonevx990app.bankEmiEnquiry

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.verifonevx990app.R
import com.example.verifonevx990app.bankemi.BankEMIDataModal
import com.example.verifonevx990app.databinding.FragmentIssuerListBinding
import com.example.verifonevx990app.main.MainActivity
import com.example.verifonevx990app.utils.printerUtils.PrintUtil
import com.example.verifonevx990app.vxUtils.BaseActivity
import com.example.verifonevx990app.vxUtils.VFService
import com.example.verifonevx990app.vxUtils.VxEvent
import com.example.verifonevx990app.vxUtils.divideAmountBy100
import com.google.android.material.card.MaterialCardView

class EMiEnquiryOnPosActivity : BaseActivity() {
    var viewBinding: FragmentIssuerListBinding? = null
    private var emiEnquiryData: ArrayList<BankEMIDataModal> = arrayListOf()

    //enquiryAmt
    private val enquiryAmt by lazy {
        intent?.getStringExtra("enquiryAmt") ?: "0"
    }
    private val bankName by lazy {
        intent?.getStringExtra("bankName") ?: "UNKNOWN"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewBinding = FragmentIssuerListBinding.inflate(layoutInflater)
        setContentView(viewBinding?.root)
        emiEnquiryData = intent?.getParcelableArrayListExtra("bankEnquiryData") ?: arrayListOf()

        viewBinding?.doEnquiryBtn?.text = "PRINT"
        viewBinding?.doEnquiryBtn?.setIconResource(R.drawable.ic_baseline_print_24)
        viewBinding?.doEnquiryBtn?.setOnClickListener {
            showProgress("Printing...")
            PrintUtil(this).printEMIEnquiry(
                this,
                emiEnquiryData,
                enquiryAmt,
                bankName
            ) { isSuccess, msg ->
                if (isSuccess) {
                    hideProgress()
                    finish()
                    startActivity(
                        Intent(this, MainActivity::class.java).apply {
                            flags =
                                Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_CLEAR_TASK
                        })
                } else {
                    VFService.showToast(msg)
                    hideProgress()

                }
            }
        }
        viewBinding?.issuerRV?.layoutManager = LinearLayoutManager(this)
        viewBinding?.issuerRV?.adapter =
            EMiEnquiryOnPosAdapter(emiEnquiryData, enquiryAmt)
    }

    override fun onEvents(event: VxEvent) {
        // Need to override because this method is present IDialog Interface and  BaseActivity extends IDialog
    }
}

class EMiEnquiryOnPosAdapter(
    private val emiSchemeDataList: MutableList<BankEMIDataModal>?,
    private val enquiryAmt: String
) :
    RecyclerView.Adapter<EMiEnquiryOnPosAdapter.EMiEnquiryOnPosViewHolder>() {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): EMiEnquiryOnPosViewHolder {
        val inflater =
            LayoutInflater.from(parent.context)
                .inflate(R.layout.item_emi_enquiry_on_pos, parent, false)
        return EMiEnquiryOnPosViewHolder(inflater)
    }

    override fun onBindViewHolder(holder: EMiEnquiryOnPosViewHolder, position: Int) {
        val modelData = emiSchemeDataList?.get(position)
        if (modelData != null) {
            holder.transactionAmount.text = enquiryAmt
            val tenureDuration = "${modelData.tenure} Months"
            val tenureHeadingDuration = "${modelData.tenure} Months Scheme"
            var roi = divideAmountBy100(modelData.tenureInterestRate.toInt()).toString()
            var loanamt = divideAmountBy100(modelData.loanAmount.toInt()).toString()
            roi = "%.2f".format(roi.toDouble()) + " %"
            loanamt = "%.2f".format(loanamt.toDouble())

            holder.roi.text = roi
            holder.tenure.text = tenureDuration
            holder.tenureHeadingTV.text = tenureHeadingDuration
            holder.loanAmount.text = loanamt
            holder.emiAmount.text = divideAmountBy100(modelData.emiAmount.toInt()).toString()

            //If Discount Amount Available show this else if CashBack Amount show that:-
            if (modelData.discountAmount.toInt() != 0) {
                holder.discountAmount.text =
                    divideAmountBy100(modelData.discountAmount.toInt()).toString()
                holder.discountLinearLayout.visibility = View.VISIBLE
                holder.cashBackLinearLayout.visibility = View.GONE
            }
            if (modelData.cashBackAmount.toInt() != 0) {
                holder.cashBackAmount.text =
                    divideAmountBy100(modelData.cashBackAmount.toInt()).toString()
                holder.cashBackLinearLayout.visibility = View.VISIBLE
                holder.discountLinearLayout.visibility = View.GONE
            }

            holder.totalEmiPay.text = divideAmountBy100(modelData.totalEmiPay.toInt()).toString()
            holder.totalIntrstRate.text =
                divideAmountBy100(modelData.totalInterestPay.toInt()).toString()
        }

    }

    override fun getItemCount(): Int = emiSchemeDataList?.size ?: 0

    class EMiEnquiryOnPosViewHolder(var view: View) : RecyclerView.ViewHolder(view) {
        val transactionAmount = view.findViewById<TextView>(R.id.tv_transaction_amount)
        val tenure = view.findViewById<TextView>(R.id.tv_tenure)
        val emiAmount = view.findViewById<TextView>(R.id.tv_emi_amount)
        val loanAmount = view.findViewById<TextView>(R.id.tv_loan_amount)
        val discountAmount = view.findViewById<TextView>(R.id.tv_discount_amount)
        val cashBackAmount = view.findViewById<TextView>(R.id.tv_cashback_amount)
        val roi = view.findViewById<TextView>(R.id.tv_total_roi)
        val totalEmiPay = view.findViewById<TextView>(R.id.tv_total_emi_pay)
        val tenureHeadingTV = view.findViewById<TextView>(R.id.tenure_heading_tv)
        val totalIntrstRate = view.findViewById<TextView>(R.id.tv_total_itrst)
        val parentEmiLayout = view.findViewById<LinearLayout>(R.id.parent_emi_view_ll)
        val discountLinearLayout = view.findViewById<LinearLayout>(R.id.discountLL)
        val cashBackLinearLayout = view.findViewById<LinearLayout>(R.id.cashBackLL)
        val cardView = view.findViewById<MaterialCardView>(R.id.cardView)
        val schemeCheckIV = view.findViewById<ImageView>(R.id.scheme_check_iv)
    }


}