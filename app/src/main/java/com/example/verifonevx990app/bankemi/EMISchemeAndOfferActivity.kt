package com.example.verifonevx990app.bankemi

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.DefaultItemAnimator
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.verifonevx990app.R
import com.example.verifonevx990app.databinding.EmiSchemeOfferViewBinding
import com.example.verifonevx990app.databinding.ItemEmiSchemeOfferBinding
import com.example.verifonevx990app.emv.transactionprocess.CardProcessedDataModal
import com.example.verifonevx990app.main.MainActivity
import com.example.verifonevx990app.realmtables.BrandEMIMasterTimeStamps
import com.example.verifonevx990app.vxUtils.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class EMISchemeAndOfferActivity : BaseActivity() {
    private var binding: EmiSchemeOfferViewBinding? = null
    private var bankEMIDataModal: BankEMIDataModal? = null
    private var emiSchemeOfferDataList: MutableList<BankEMIDataModal>? = null
    private var emiTAndCDataList: MutableList<BankEMIIssuerTAndCDataModal>? = null
    private var cardProcessedDataModal: CardProcessedDataModal? = null
    private var selectedSchemeUpdatedPosition = -1
   // private var brandEMIData: BrandEMIDataTable? = null
   /* private var transactionType by lazy { intent?.getIntExtra("transactionType", -1947) }*/
   private var transactionType = -1
    private val emiSchemeAndOfferAdapter: EMISchemeAndOfferAdapter by lazy {
        EMISchemeAndOfferAdapter(
            transactionType,
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
        lifecycleScope.launch(Dispatchers.IO) {
            val timeStampsData = BrandEMIMasterTimeStamps.getAllBrandEMIMasterDataTimeStamps()
            if (timeStampsData.isNotEmpty()) {
                if (emiTAndCDataList?.get(0)?.updateIssuerTAndCTimeStamp ?: "0" != timeStampsData[0].issuerTAndCTimeStamp) {
                    fetchAndSaveIssuerTCData(
                        emiTAndCDataList?.get(0)?.updateIssuerTAndCTimeStamp ?: "0"
                    )
                } else
                    Log.d("SameTimeStamps:- ", "IssuerTAndCData")
            } else {
                fetchAndSaveIssuerTCData(
                    emiTAndCDataList?.get(0)?.updateIssuerTAndCTimeStamp ?: "0"
                )
            }
        }
        //endregion

        /*region====================Checking Condition whether Previous Transaction Flow Comes from Brand EMI:-
        if(true)-------> Fetch Selected Brand EMI Data for IMEI and Other Validations if bits are on
        */
        /*if (cardProcessedDataModal?.getTransType() == TransactionType.BRAND_EMI.type) {
            brandEMIData = runBlocking(Dispatchers.IO) { BrandEMIDataTable.getAllEMIData() }
        }*/

        //region======================Getting Parcelable Data List of Emi Scheme&Offer , Emi TAndC and CardProcessedData:-
        emiSchemeOfferDataList = intent?.getParcelableArrayListExtra("emiSchemeDataList")
        emiTAndCDataList = intent?.getParcelableArrayListExtra("emiTAndCDataList")
        cardProcessedDataModal = intent?.getSerializableExtra("cardProcessedData") as CardProcessedDataModal?
        transactionType        = intent?.getIntExtra("transactionType",-1) ?: -1
        //endregion

        if(transactionType == TransactionType.TEST_EMI.type){
            emiSchemeOfferDataList?.clear()
            calculateEmi()
        }


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

    private fun calculateEmi() {
        var monthlyEmi3 =   cardProcessedDataModal?.getTransactionAmount()?.toFloat()?.let { emi_calculator(it,11.0f,3.0f) }

        var totalInterestPay3 = cardProcessedDataModal?.getTransactionAmount()?.toDouble()?.let { (monthlyEmi3?.times(3.0))?.minus(it) }


        bankEMIDataModal = BankEMIDataModal("3", "1100",
            "","",cardProcessedDataModal?.getTransactionAmount().toString(), "","","",
            cardProcessedDataModal?.getTransactionAmount().toString(),
            monthlyEmi3.toString(),"","", "","",totalInterestPay3.toString(),"","",
            "","","","")

        emiSchemeOfferDataList?.add(bankEMIDataModal as BankEMIDataModal)

        var monthlyEmi6 =   cardProcessedDataModal?.getTransactionAmount()?.toFloat()?.let { emi_calculator(it,12.0f,6.0f) }

        var totalInterestPay6 = cardProcessedDataModal?.getTransactionAmount()?.toDouble()?.let { (monthlyEmi6?.times(6.0))?.minus(it) }

        bankEMIDataModal = BankEMIDataModal("6", "1200",
            "","",cardProcessedDataModal?.getTransactionAmount().toString(), "","","",
            cardProcessedDataModal?.getTransactionAmount().toString(),
            monthlyEmi6.toString(),"","", "","",totalInterestPay6.toString(),"","",
            "","","","")

        emiSchemeOfferDataList?.add(bankEMIDataModal as BankEMIDataModal)

        var monthlyEmi9 =   cardProcessedDataModal?.getTransactionAmount()?.toFloat()?.let { emi_calculator(it,13.0f,9.0f) }

        var totalInterestPay9 = cardProcessedDataModal?.getTransactionAmount()?.toDouble()?.let { (monthlyEmi9?.times(9.0))?.minus(it) }


        bankEMIDataModal = BankEMIDataModal("9", "1300",
            "","",cardProcessedDataModal?.getTransactionAmount().toString(), "","","",
            cardProcessedDataModal?.getTransactionAmount().toString(),
            monthlyEmi9.toString(),"","", "","",totalInterestPay9.toString(),"","",
            "","","","")

        emiSchemeOfferDataList?.add(bankEMIDataModal as BankEMIDataModal)

        var monthlyEmi12 =   cardProcessedDataModal?.getTransactionAmount()?.toFloat()?.let { emi_calculator(it,14.0f,12.0f) }

        var totalInterestPay12 = cardProcessedDataModal?.getTransactionAmount()?.toDouble()?.let { (monthlyEmi12?.times(12.0))?.minus(it) }


        bankEMIDataModal = BankEMIDataModal("12", "1400",
            "","",cardProcessedDataModal?.getTransactionAmount().toString(), "","","",
            cardProcessedDataModal?.getTransactionAmount().toString(),
            monthlyEmi12.toString(),"","", "","",totalInterestPay12.toString(),"","",
            "","","","")

        emiSchemeOfferDataList?.add(bankEMIDataModal as BankEMIDataModal)
    }

    // Function to calculate EMI
    fun emi_calculator(principalamount: Float, rate: Float, time: Float): Double {
        var r = rate
        var t = time
        val emi: Float
        r = r / (12 * 100) // one month interest
        t = t  // one month period
        emi = (principalamount * r * Math.pow((1 + r).toDouble(), t.toDouble()).toFloat()
                / (Math.pow((1 + r).toDouble(), t.toDouble()) - 1).toFloat())

        return emi.toDouble()
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

internal class EMISchemeAndOfferAdapter(private val transactionType: Int,private val emiSchemeDataList: MutableList<BankEMIDataModal>?, private var schemeSelectCB: (Int) -> Unit) : RecyclerView.Adapter<EMISchemeAndOfferAdapter.EMISchemeOfferHolder>() {

    private var index = -1

    override fun onCreateViewHolder(p0: ViewGroup, p1: Int): EMISchemeOfferHolder {
        val inflater: ItemEmiSchemeOfferBinding =
            ItemEmiSchemeOfferBinding.inflate(LayoutInflater.from(p0.context), p0, false)
        return EMISchemeOfferHolder(inflater)
    }

    override fun getItemCount(): Int {
        return emiSchemeDataList?.size ?: 0
    }

    @SuppressLint("SetTextI18n")
    override fun onBindViewHolder(holder: EMISchemeOfferHolder, position: Int) {
        val modelData = emiSchemeDataList?.get(position)
        if (modelData != null) {
           // (((modelData.transactionAmount).toDouble()).div(100)).toString()
            "%.2f".format((((modelData.transactionAmount).toDouble()).div(100)).toString().toDouble())

            holder.binding.tvTransactionAmount.text = "\u20B9 " +  "%.2f".format((((modelData.transactionAmount).toDouble()).div(100)).toString().toDouble())
            holder.binding.tvLoanAmount.text = "\u20B9 " +   "%.2f".format((((modelData.loanAmount).toDouble()).div(100)).toString().toDouble())
            holder.binding.tvEmiAmount.text = "\u20B9 " +   "%.2f".format((((modelData.emiAmount).toDouble()).div(100)).toString().toDouble())
            val tenureDuration = "${modelData.tenure} Months"
            val tenureHeadingDuration = "${modelData.tenure} Months Scheme"
            holder.binding.tvTenure.text = tenureDuration
            holder.binding.tenureHeadingTv.text = tenureHeadingDuration

            //If Discount Amount Available show this else if CashBack Amount show that:-
            if (!modelData.discountAmount.isNullOrEmpty() && modelData.discountAmount.toInt() != 0) {

                holder.binding.tvDiscountAmount.text = "\u20B9 " + "%.2f".format((((modelData.discountAmount).toDouble()).div(100)).toString().toDouble())
                holder.binding.discountLL.visibility = View.VISIBLE
                holder.binding.cashBackLL.visibility = View.GONE
            }
            if (!modelData.cashBackAmount.isNullOrEmpty() && modelData.cashBackAmount.toInt() != 0) {
                holder.binding.tvCashbackAmount.text = "\u20B9 " + divideAmountBy100(modelData.cashBackAmount.toInt()).toString()
                holder.binding.cashBackLL.visibility = View.VISIBLE
                holder.binding.discountLL.visibility = View.GONE
            }


            if(transactionType != TransactionType.TEST_EMI.type) {

                holder.binding.toatalemipayLL.visibility = View.VISIBLE
                holder.binding.tvTotalInterestPay.text = "\u20B9 " +  "%.2f".format((((modelData.tenureInterestRate).toDouble()).div(100)).toString().toDouble())
                holder.binding.tvTotalEmiPay.text = "\u20B9 " + "%.2f".format((((modelData.totalEmiPay).toDouble()).div(100)).toString().toDouble())
                val roi=  "%.2f".format((((modelData.tenureInterestRate).toDouble()).div(100)).toString().toDouble())
                holder.binding.tvRoi.text = "$roi %"

            }
            else {
                holder.binding.toatalemipayLL.visibility = View.GONE
                holder.binding.tvInterestRate.text =  ""+divideAmountBy100(modelData.tenureInterestRate.toInt()).toString() +" %"
                holder.binding.tvTotalInterestPay.text = "\u20B9 " + "%.2f".format((((modelData.totalInterestPay).toDouble()).div(100)).toString().toDouble())
                val roi=  "%.2f".format((((modelData.tenureInterestRate).toDouble()).div(100)).toString().toDouble())
                holder.binding.tvRoi.text = "$roi %"

            }
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

