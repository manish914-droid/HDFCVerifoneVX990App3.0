package com.example.verifonevx990app.bankemi

import android.app.Activity
import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DefaultItemAnimator
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.verifonevx990app.R
import com.example.verifonevx990app.databinding.EmiSchemeOfferViewBinding
import com.example.verifonevx990app.databinding.ItemEmiSchemeOfferBinding
import com.example.verifonevx990app.emv.transactionprocess.CardProcessedDataModal
import com.example.verifonevx990app.main.MainActivity
import com.example.verifonevx990app.realmtables.BrandEMIDataTable
import com.example.verifonevx990app.vxUtils.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking

class EMISchemeAndOfferActivity : BaseActivity() {
    private var binding: EmiSchemeOfferViewBinding? = null
    private var emiSchemeOfferDataList: MutableList<BankEMIDataModal>? = null
    private var emiTAndCDataList: MutableList<BankEMIIssuerTAndCDataModal>? = null
    private var cardProcessedDataModal: CardProcessedDataModal? = null
    private var selectedSchemeUpdatedPosition = -1
    private var brandEMIData: BrandEMIDataTable? = null
    private val transactionType by lazy { intent?.getIntExtra("transactionType", -1947) }
    private val emiSchemeAndOfferAdapter: EMISchemeAndOfferAdapter by lazy {
        EMISchemeAndOfferAdapter(
            emiSchemeOfferDataList,
            ::onSchemeClickEvent
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = EmiSchemeOfferViewBinding.inflate(layoutInflater)
        setContentView(binding?.root)
        binding?.toolbarTxn?.mainToolbarStart?.setBackgroundResource(R.drawable.ic_back_arrow_white)
        ROCProviderV2.refreshToolbarLogos(this)
        binding?.toolbarTxn?.mainToolbarStart?.setOnClickListener {
            navigateControlBackToTransaction(
                isTransactionContinue = false
            )
        }
        showProgress()

        //region==============Below Code will only execute in case of Insta EMI sale to fetch IssuerTAndC Data:-
        if (transactionType == TransactionType.EMI_SALE.type) runBlocking(Dispatchers.IO) { fetchAndSaveIssuerTCData() }
        //endregion

        /*region====================Checking Condition whether Previous Transaction Flow Comes from Brand EMI:-
        if(true)-------> Fetch Selected Brand EMI Data for IMEI and Other Validations if bits are on
        */
        if (cardProcessedDataModal?.getTransType() == TransactionType.BRAND_EMI.type) {
            brandEMIData = runBlocking(Dispatchers.IO) { BrandEMIDataTable.getAllEMIData() }
        }

        //region======================Getting Parcelable Data List of Emi Scheme&Offer , Emi TAndC and CardProcessedData:-
        emiSchemeOfferDataList = intent?.getParcelableArrayListExtra("emiSchemeDataList")
        emiTAndCDataList = intent?.getParcelableArrayListExtra("emiTAndCDataList")
        cardProcessedDataModal =
            intent?.getSerializableExtra("cardProcessedData") as CardProcessedDataModal?
        //endregion

        setUpRecyclerView()

        //region======================Proceed TXN Floating Button OnClick Event:-
        binding?.emiSchemeFloatingButton?.setOnClickListener {
            if (selectedSchemeUpdatedPosition != -1)
                navigateControlBackToTransaction(isTransactionContinue = true)
            else
                VFService.showToast(getString(R.string.please_select_scheme))
        }
        //endregion
    }

    //region==========================onClickEvent==================================================
    private fun onSchemeClickEvent(position: Int) {
        Log.d("Position:- ", emiSchemeOfferDataList?.get(position).toString())
        selectedSchemeUpdatedPosition = position
    }
    //endregion

    override fun onBackPressed() = navigateControlBackToTransaction(isTransactionContinue = false)

    //region=========================SetUp RecyclerView Data:-
    private fun setUpRecyclerView() {
        if (emiSchemeOfferDataList != null) {
            binding?.emiSchemeOfferRV?.apply {
                layoutManager = LinearLayoutManager(context)
                itemAnimator = DefaultItemAnimator()
                adapter = emiSchemeAndOfferAdapter
            }
            hideProgress()
        }
        hideProgress()
    }
    //endregion


    //region=========================control back to VFTransactionActivity==========================
    private fun navigateControlBackToTransaction(isTransactionContinue: Boolean) {
        if (isTransactionContinue) {
            //Show IMEI Capture Dialog if bit on:-
            val intent = Intent().apply {
                putExtra("cardProcessedData", cardProcessedDataModal)
                putExtra(
                    "emiSchemeDataList",
                    emiSchemeOfferDataList?.get(selectedSchemeUpdatedPosition)
                )
                putExtra("emiTAndCDataList", emiTAndCDataList?.get(0))
            }
            setResult(Activity.RESULT_OK, intent)
            finish()
        } else {
            //Below method to Navigate merchant to MainActivity:-
            startActivity(Intent(this, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_CLEAR_TASK
            })
        }
    }
    //endregion

    override fun onEvents(event: VxEvent) {}
}

internal class EMISchemeAndOfferAdapter(
    private val emiSchemeDataList: MutableList<BankEMIDataModal>?,
    private var schemeSelectCB: (Int) -> Unit
) : RecyclerView.Adapter<EMISchemeAndOfferAdapter.EMISchemeOfferHolder>() {

    private var index = -1

    override fun onCreateViewHolder(p0: ViewGroup, p1: Int): EMISchemeOfferHolder {
        val inflater: ItemEmiSchemeOfferBinding =
            ItemEmiSchemeOfferBinding.inflate(LayoutInflater.from(p0.context), p0, false)
        return EMISchemeOfferHolder(inflater)
    }

    override fun getItemCount(): Int {
        return emiSchemeDataList?.size ?: 0
    }

    override fun onBindViewHolder(holder: EMISchemeOfferHolder, position: Int) {
        val modelData = emiSchemeDataList?.get(position)
        if (modelData != null) {
            holder.binding.tvTransactionAmount.text =
                divideAmountBy100(modelData.transactionAmount.toInt()).toString()
            val tenureDuration = "${modelData.tenure} Months"
            val tenureHeadingDuration = "${modelData.tenure} Months Scheme"
            holder.binding.tvTenure.text = tenureDuration
            holder.binding.tenureHeadingTv.text = tenureHeadingDuration
            holder.binding.tvLoanAmount.text =
                divideAmountBy100(modelData.loanAmount.toInt()).toString()
            holder.binding.tvEmiAmount.text =
                divideAmountBy100(modelData.emiAmount.toInt()).toString()

            //If Discount Amount Available show this else if CashBack Amount show that:-
            if (modelData.discountAmount.toInt() != 0) {
                holder.binding.tvDiscountAmount.text =
                    divideAmountBy100(modelData.discountAmount.toInt()).toString()
                holder.binding.discountLL.visibility = View.VISIBLE
                holder.binding.cashBackLL.visibility = View.GONE
            }
            if (modelData.cashBackAmount.toInt() != 0) {
                holder.binding.tvCashbackAmount.text =
                    divideAmountBy100(modelData.cashBackAmount.toInt()).toString()
                holder.binding.cashBackLL.visibility = View.VISIBLE
                holder.binding.discountLL.visibility = View.GONE
            }
            holder.binding.tvTotalInterestPay.text =
                divideAmountBy100(modelData.tenureInterestRate.toInt()).toString()
            holder.binding.tvTotalEmiPay.text =
                divideAmountBy100(modelData.totalEmiPay.toInt()).toString()
        }

        holder.binding.parentEmiViewLl.setOnClickListener {
            index = position
            notifyDataSetChanged()
        }

        //region==========================Checked Particular Row of RecyclerView Logic:-
        if (index == position) {
            holder.binding.cardView.setStrokeColor(ColorStateList.valueOf(Color.parseColor("#13E113")))
            holder.binding.schemeCheckIv.visibility = View.VISIBLE
            schemeSelectCB(position)
        } else {
            holder.binding.cardView.setStrokeColor(ColorStateList.valueOf(Color.parseColor("#FFFFFF")))
            holder.binding.schemeCheckIv.visibility = View.GONE
        }
        //endregion
    }


    inner class EMISchemeOfferHolder(val binding: ItemEmiSchemeOfferBinding) :
        RecyclerView.ViewHolder(binding.root)
}

