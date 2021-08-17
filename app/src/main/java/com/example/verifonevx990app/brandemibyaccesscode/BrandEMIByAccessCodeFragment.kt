package com.example.verifonevx990app.brandemibyaccesscode

import android.app.Dialog
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.os.Parcelable
import android.text.TextUtils
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.Button
import android.widget.LinearLayout
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.example.customneumorphic.NeumorphCardView
import com.example.verifonevx990app.R
import com.example.verifonevx990app.bankemi.GenericEMIIssuerTAndC
import com.example.verifonevx990app.brandemi.CreateBrandEMIPacket
import com.example.verifonevx990app.brandemi.GenericBrandTAndC
import com.example.verifonevx990app.databinding.BrandEmiByAccessCodeViewBinding
import com.example.verifonevx990app.emv.transactionprocess.VFTransactionActivity
import com.example.verifonevx990app.main.EMIRequestType
import com.example.verifonevx990app.main.MainActivity
import com.example.verifonevx990app.main.SplitterTypes
import com.example.verifonevx990app.realmtables.*
import com.example.verifonevx990app.transactions.setMaxLength
import com.example.verifonevx990app.vxUtils.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.parcelize.Parcelize
import java.io.Serializable

class BrandEMIByAccessCodeFragment : Fragment() {
    private val action by lazy { arguments?.getSerializable("type")as EDashboardItem }
    private var iDialog: IDialog? = null
    private var binding: BrandEmiByAccessCodeViewBinding? = null
    private var field57Request: String? = null
    private var totalRecord: String = "0"
    private var isDataMatch = false

    private val brandEmiAccessCodeList: MutableList<BrandEMIAccessDataModal> = mutableListOf()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = BrandEmiByAccessCodeViewBinding.inflate(layoutInflater, container, false)
        return binding?.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        //(activity as MainActivity).showBottomNavigationBar(isShow = false)
        binding?.subHeaderView?.subHeaderText?.text = getString(R.string.brand_emi_by_access_code)
        binding?.subHeaderView?.headerImage?.setImageResource(action.res)
        binding?.subHeaderView?.backImageButton?.setOnClickListener { parentFragmentManager.popBackStackImmediate() }

        binding?.brandEmiAccessCodeBT?.setOnClickListener {
            if (!TextUtils.isEmpty(binding?.accessCodeET?.text?.toString()))
                fetchBrandEmiDataByAccessCode(binding?.accessCodeET?.text?.toString())
            else
                VFService.showToast(getString(R.string.please_enter_access_code))
        }
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        if (context is IDialog) iDialog = context
    }

    override fun onDetach() {
        super.onDetach()
        iDialog = null
    }

    //region===========================Fetching Brand EMI Data by Access Code:-
    private fun fetchBrandEmiDataByAccessCode(accessCode: String?) {
        field57Request = "${EMIRequestType.BRAND_EMI_BY_ACCESS_CODE.requestType}^0^$accessCode"
        Log.d("Request:- ", field57Request.toString())
        iDialog?.showProgress()
        var brandEMIProcessData: IsoDataWriter? = null
        //region==============================Creating ISO Packet For BrandEMIMasterSubCategoryData Request:-
        runBlocking(Dispatchers.IO) {
            CreateBrandEMIPacket(field57Request) {
                brandEMIProcessData = it
            }
        }

        //region==============================Host Hit To Fetch BrandEMIAccessCode Data:-
        GlobalScope.launch(Dispatchers.IO) {
            if (brandEMIProcessData != null) {
                val byteArrayRequest = brandEMIProcessData?.generateIsoByteRequest()
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
                        val brandEmiAccessCodeData = responseIsoData.isoMap[57]?.parseRaw2String().toString()

                        when (responseCode) {
                            "00" -> {
                                ROCProviderV2.incrementFromResponse(
                                    ROCProviderV2.getRoc(
                                        AppPreference.getBankCode()
                                    ).toString(), AppPreference.getBankCode()
                                )
                                GlobalScope.launch(Dispatchers.Main) {
                                    //Processing BrandEMIByAccessCodeData:-
                                    stubbingBrandEMIAccessCodeDataToList(
                                        brandEmiAccessCodeData,
                                        hostMsg
                                    )
                                }
                            }
                            "-1" -> {
                                GlobalScope.launch(Dispatchers.Main) {
                                    iDialog?.hideProgress()
                                    iDialog?.alertBoxWithAction(null, null,
                                        getString(R.string.info), hostMsg,
                                        false, getString(R.string.positive_button_ok),
                                        {
                                            parentFragmentManager.popBackStack()
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
                        GlobalScope.launch(Dispatchers.Main) {
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
        //endregion
    }
    //endregion

    //region=================================Stubbing BrandEMI Access Code Data and Display in List:-
    private fun stubbingBrandEMIAccessCodeDataToList(brandEMIAccessData: String, hostMsg: String) {
        GlobalScope.launch(Dispatchers.Main) {
            if (!TextUtils.isEmpty(brandEMIAccessData)) {
                val dataList = parseDataListWithSplitter("|", brandEMIAccessData)
                if (dataList.isNotEmpty()) {
                    // and iterate further on record data only:-
                    var tempDataList = mutableListOf<String>()
                    tempDataList = dataList.subList(2, dataList.size)
                    if (!TextUtils.isEmpty(tempDataList[0])) {
                        val splitData = parseDataListWithSplitter(
                            SplitterTypes.CARET.splitter, tempDataList[0]
                        )

                        brandEmiAccessCodeList.add(
                            BrandEMIAccessDataModal(
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
                                splitData[22], splitData[23],
                                splitData[24], splitData[25],
                                splitData[26], splitData[27],
                                splitData[28], splitData[29],
                                splitData[30], splitData[31],splitData[32],splitData[33],splitData[34],splitData[35],splitData[36],splitData[37]
                            )
                        )

                    }
                    //Show Confirmation BrandEMI ByAccessCode Data Dialog:-
                    iDialog?.hideProgress()


                    if (brandEmiAccessCodeList.isNotEmpty())
                        showConfirmationDataDialog(brandEmiAccessCodeList[0])
                }
            } else {
                GlobalScope.launch(Dispatchers.Main) {
                    iDialog?.hideProgress()
                }
            }
        }
    }
    //endregion

    //region========================BrandEMIBy Access Code Confirmation Data Dialog:-
    private fun showConfirmationDataDialog(brandEMIAccessData: BrandEMIAccessDataModal) {
        GlobalScope.launch(Dispatchers.Main) {
            val dialog = Dialog(requireActivity())
            dialog.setCancelable(false)
            dialog.setContentView(R.layout.brand_emi_by_access_code_dialog_view)

            dialog.window?.attributes?.windowAnimations = R.style.DialogAnimation
            val window = dialog.window
            window?.setLayout(
                ViewGroup.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.WRAP_CONTENT)
            dialog.findViewById<Button>(R.id.cancelButton).setOnClickListener {
                brandEmiAccessCodeList.clear()
                dialog.dismiss()
            }
            val productName = dialog.findViewById<BHTextView>(R.id.productNameET)
            val categoryName = dialog.findViewById<BHTextView>(R.id.categoryNameET)
            val tenureTime = dialog.findViewById<BHTextView>(R.id.tenureET)
            val transactionAmountET = dialog.findViewById<BHTextView>(R.id.transactionAmountET)
            val discountPercentageET = dialog.findViewById<BHTextView>(R.id.discountPercentageET)
            val discountAmountET = dialog.findViewById<BHTextView>(R.id.discountAmountET)
            val cashBackPercentageET = dialog.findViewById<BHTextView>(R.id.cashBackPercentageET)
            val cashBackAmountET = dialog.findViewById<BHTextView>(R.id.cashBackAmountET)
            val emiAmountET = dialog.findViewById<BHTextView>(R.id.emiAmountET)
            val netPayAmountET = dialog.findViewById<BHTextView>(R.id.netPayAmountET)
            val issuerName = dialog.findViewById<BHTextView>(R.id.issuerName)
            val brandNameTV = dialog.findViewById<BHTextView>(R.id.productBrandNameET)
            val discountPercentageLL = dialog.findViewById<LinearLayout>(R.id.discountPercentageLL)
            val discountAmountLL = dialog.findViewById<LinearLayout>(R.id.discountAmountLL)
            val cashBackPercentageLL = dialog.findViewById<LinearLayout>(R.id.cashBackPercentageLL)
            val cashBackAmountLL = dialog.findViewById<LinearLayout>(R.id.cashBackAmountLL)
            val billNoCrdView = dialog.findViewById<NeumorphCardView>(R.id.billno_crd_view)
            val billNoet = dialog.findViewById<BHEditText>(R.id.billNum_et)
            val rupeeSymbol= activity?.getString(R.string.rupees_symbol)

            if (brandEMIAccessData.brandReservField[2]=='1' || brandEMIAccessData.brandReservField[2]=='2' ) {
                billNoCrdView?.visibility = View.VISIBLE
                showEditTextSelected(billNoet, billNoCrdView, requireContext())
                billNoet?.setMaxLength( 16)
            } else {
                billNoCrdView?.visibility = View.GONE
            }
            productName.text = brandEMIAccessData.productName
            categoryName.text = brandEMIAccessData.productCategoryName
            issuerName.text = brandEMIAccessData.issuerName
            val tenureMonths = "${brandEMIAccessData.tenure} Months"
            tenureTime.text = tenureMonths
            val txnAmt= rupeeSymbol+"%.2f".format(brandEMIAccessData.transactionAmount.toFloat() / 100)
            transactionAmountET.text =txnAmt
            brandNameTV.text=brandEMIAccessData.brandName
            if (!TextUtils.isEmpty(brandEMIAccessData.discountCalculatedValue)) {
                discountPercentageET.text = brandEMIAccessData.discountCalculatedValue
                discountPercentageLL.visibility = View.VISIBLE
            }
            if (!TextUtils.isEmpty(brandEMIAccessData.discountAmount) && brandEMIAccessData.discountAmount != "0") {
               val disAmount= rupeeSymbol+"%.2f".format(brandEMIAccessData.discountAmount.toFloat() / 100)
                discountAmountET.text = disAmount
                discountAmountLL.visibility = View.VISIBLE
            }
            if (!TextUtils.isEmpty(brandEMIAccessData.cashBackCalculatedValue)) {
                cashBackPercentageET.text = brandEMIAccessData.cashBackCalculatedValue
                cashBackPercentageLL.visibility = View.VISIBLE
            }
            if (!TextUtils.isEmpty(brandEMIAccessData.cashBackAmount) && brandEMIAccessData.cashBackAmount != "0") {
                cashBackAmountET.text = brandEMIAccessData.cashBackAmount
                cashBackAmountLL.visibility = View.VISIBLE
            }
            val emiAmt=rupeeSymbol+"%.2f".format(brandEMIAccessData.emiAmount.toFloat() / 100)
                val netPayAmt=rupeeSymbol+ "%.2f".format(brandEMIAccessData.netPayAmount.toFloat() / 100)
            emiAmountET.text = emiAmt
            netPayAmountET.text =netPayAmt
            dialog.findViewById<Button>(R.id.submitButton).setOnClickListener {
                if(brandEMIAccessData.brandReservField[2]=='2' && billNoet.text.isNullOrBlank()){
                   VFService.showToast("Enter bill number")
                   return@setOnClickListener
               }
                val issuerTAndCData = runBlocking(Dispatchers.IO) { IssuerTAndCTable.getAllIssuerTAndCData() }
                val brandTAndCData = runBlocking(Dispatchers.IO) { BrandTAndCTable.getAllBrandTAndCData() }
                iDialog?.showProgress()
                if (issuerTAndCData?.isEmpty() == true || brandTAndCData.isEmpty() || !matchHostAndDBData(brandEMIAccessData)) {
                    getIssuerTAndCData { issuerTCDataSaved ->
                        if (issuerTCDataSaved) {
                            getBrandTAndCData { brandTCDataSaved ->
                                if (brandTCDataSaved) {
                                    saveBrandMasterTimeStampsData("","",brandEMIAccessData.issuerTimeStamp,brandEMIAccessData.brandTimeStamp) {
                                        lifecycleScope.launch(Dispatchers.Main) {
                                            iDialog?.hideProgress()
                                            navigateToVFTransactionActivity(
                                                brandEMIAccessData,
                                                billNoet.text.toString()
                                            )
                                        }
                                    }

                                } else {
                                    lifecycleScope.launch(Dispatchers.Main) {
                                        iDialog?.hideProgress()
                                        showSomethingWrongPopUp()
                                    }
                                }
                            }
                        } else {
                            lifecycleScope.launch(Dispatchers.Main) {
                                iDialog?.hideProgress()
                                showSomethingWrongPopUp()
                            }
                        }
                    }
                } else {
                    GlobalScope.launch(Dispatchers.Main) {
                        iDialog?.hideProgress()
                        navigateToVFTransactionActivity(brandEMIAccessData,billNoet.text.toString())
                    }
                }

                dialog.dismiss()

            }
            dialog.show()
            window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        }
    }
    //endregion

    //region======================Navigate Fragment to VFTransactionActivity:-
    private fun navigateToVFTransactionActivity(brandEMIAccessData: BrandEMIAccessDataModal,billNoet: String){
        if (checkInternetConnection()) {
            GlobalScope.launch(Dispatchers.IO) {
                // Saving brandEmiAccessData (Brand Data)
                //   saveDataInDB(brandEMIAccessData)
                activity?.startActivity(
                    Intent(
                        requireActivity(),
                        VFTransactionActivity::class.java
                    ).apply {
                        putExtra("amt", brandEMIAccessData.transactionAmount)
                        putExtra("type", TransactionType.BRAND_EMI_BY_ACCESS_CODE.type)
                        putExtra("proc_code", ProcessingCode.SALE.code)
                        putExtra("mobileNumber", brandEMIAccessData.mobileNo)
                        putExtra("billNumber", billNoet)
                        putExtra("uiAction", UiAction.BANK_EMI_BY_ACCESS_CODE)
                        brandEMIAccessData.billNumberInvoiceNo=billNoet
                        putExtra("brandEMIAccessData", brandEMIAccessData)

                    })
            }
        }
        else {
            VFService.showToast(getString(R.string.no_internet_available_please_check_your_internet))
        }
    }
    //region=======================Check Whether we got Updated Data from Host or to use Previous BrandEMIMaster Store Data:-
    private fun matchHostAndDBData(brandEMIAccessData: BrandEMIAccessDataModal): Boolean {
        val timeStampsData = runBlocking(Dispatchers.IO) { BrandEMIMasterTimeStamps.getAllBrandEMIMasterDataTimeStamps() }

        if (!TextUtils.isEmpty(brandEMIAccessData.brandTimeStamp) && !TextUtils.isEmpty(brandEMIAccessData.issuerTimeStamp)) {
            isDataMatch = brandEMIAccessData.issuerTimeStamp == timeStampsData[0].issuerTAndCTimeStamp &&
                    brandEMIAccessData.brandTimeStamp == timeStampsData[0].brandTAndCTimeStamp
        }
        return isDataMatch
    }
    //endregion
    //region======================Saving BrandEMI By Access Code Host Response Data in DB:-

    //endregion

    //region==================Get Issuer TAndC Data:-
    private fun getIssuerTAndCData(cb: (Boolean) -> Unit) {
        if (checkInternetConnection()) {
            Log.d("Bank EMI Clicked:- ", "Clicked")
            GenericEMIIssuerTAndC { issuerTermsAndConditionData, issuerHostResponseCodeAndMsg ->
                val issuerTAndCData = issuerTermsAndConditionData.first
                val responseBool = issuerTermsAndConditionData.second
                if (issuerTAndCData.isNotEmpty() && responseBool) {
                    //region================Insert IssuerTAndC and Brand TAndC in DB:-
                    //Issuer TAndC Inserting:-
                    for (i in 0 until issuerTAndCData.size) {
                        val issuerModel = IssuerTAndCTable()
                        if (!TextUtils.isEmpty(issuerTAndCData[i])) {
                            val splitData = parseDataListWithSplitter(
                                SplitterTypes.CARET.splitter,
                                issuerTAndCData[i]
                            )

                            if (splitData.size > 2) {
                                issuerModel.issuerId = splitData[0]
                                issuerModel.headerTAndC = splitData[1]
                                issuerModel.footerTAndC = splitData[2]
                            } else {
                                issuerModel.issuerId = splitData[0]
                                issuerModel.headerTAndC = splitData[1]
                            }

                            runBlocking(Dispatchers.IO) {
                                IssuerTAndCTable.performOperation(issuerModel)
                            }
                        }
                    }
                    cb(true)
                } else
                    cb(false)
            }
        } else {
            cb(false)
            VFService.showToast(getString(R.string.no_internet_available_please_check_your_internet))
        }
    }
    //endregion

    //region==========================Get Brand TAndC Data:-
    private fun getBrandTAndCData(cb: (Boolean) -> Unit) {
        GenericBrandTAndC(EMIRequestType.BRAND_T_AND_C.requestType) { brandTAndCData, hostResponseData ->
            if (brandTAndCData.first.isNotEmpty()) {
                for (i in 0 until brandTAndCData.first.size) {
                    val brandModel = BrandTAndCTable()
                    if (!TextUtils.isEmpty(brandTAndCData.first[i])) {
                        val splitData = parseDataListWithSplitter(
                            SplitterTypes.CARET.splitter,
                            brandTAndCData.first[i]
                        )
                        brandModel.brandId = splitData[0]
                        brandModel.brandTAndC = splitData[1]
                        runBlocking(Dispatchers.IO) {
                            BrandTAndCTable.performOperation(brandModel)
                        }
                    }
                }
                cb(true)
            } else
                cb(false)
        }
    }
    //endregion

    //region==============Save Brand Master Data TimeStamps:-
    private fun saveBrandMasterTimeStampsData(brandTimeStamp:String,brandCategoryUpdatedTimeStamp:String, issuerTAndCTimeStamp:String,brandTAndCTimeStamp:String,cb: (Boolean) -> Unit) {
        runBlocking(Dispatchers.IO) { BrandEMIMasterTimeStamps.clear() }
        val model = BrandEMIMasterTimeStamps()
        model.brandTimeStamp = brandTimeStamp ?: ""
        model.brandCategoryUpdatedTimeStamp = brandCategoryUpdatedTimeStamp ?: ""
        model.issuerTAndCTimeStamp = issuerTAndCTimeStamp ?: ""
        model.brandTAndCTimeStamp = brandTAndCTimeStamp ?: ""
        runBlocking(Dispatchers.IO) { BrandEMIMasterTimeStamps.performOperation(model) }
        cb(true)
    }
    //endregion

    //region=====================TAndC not Loaded Properly Pop-Up:-
    private fun showSomethingWrongPopUp() {
        (activity as MainActivity).alertBoxWithAction(null,
            null,
            getString(R.string.error),
            getString(R.string.issuer_brand_tandc_loading_error),
            false,
            getString(R.string.positive_button_ok),
            {},
            {})
    }
    //endregion
}

//region=============================Brand EMI Master Category Data Modal==========================

data class BrandEMIAccessDataModal(
    var emiCode: String,
    var bankID: String,
    var bankTID: String,
    var issuerID: String,
    var tenure: String,
    var brandID: String,
    var productID: String,
    var emiSchemeID: String,
    var transactionAmount: String,
    var discountAmount: String,
    var loanAmount: String,
    var interestAmount: String,
    var emiAmount: String,
    var cashBackAmount: String,
    var netPayAmount: String,
    var processingFee: String,
    var processingFeeRate: String,
    var totalProcessingFee: String,
    var brandName: String,
    var issuerName: String,
    var productName: String,
    var productCode: String,
    var productModal: String,
    var productCategoryName: String,
    var productSerialCode: String,
    var skuCode: String,
    var totalInterest: String,
    var schemeTAndC: String,
    var schemeTenureTAndC: String,
    var schemeDBDTAndC: String,
    var discountCalculatedValue: String,
    var cashBackCalculatedValue: String,
    var orignalTxnAmt:String,
    var mobileNo:String,
    var brandReservField:String,
    var productBaseCat:String,
    var issuerTimeStamp:String,
    var brandTimeStamp:String
) : Serializable{
   var billNumberInvoiceNo:String=""
}
//endregion

// Saving Brand EMI By access code data in database
 fun saveBrandEMIbyCodeDataInDB(brandEMIAccessData: BrandEMIAccessDataModal?, hostInvoice:String) {
    val modal = BrandEMIAccessDataModalTable()
   // runBlocking(Dispatchers.IO) { BrandEMIAccessDataModalTable.clear() }
if(brandEMIAccessData!=null) {
    //Saving BrandEMI By AccessCode Data in DB:-
    modal.emiCode = brandEMIAccessData.emiCode
    modal.bankID = brandEMIAccessData.bankID
    modal.bankTID = brandEMIAccessData.bankTID
    modal.issuerID = brandEMIAccessData.issuerID
    modal.tenure = brandEMIAccessData.tenure
    modal.brandID = brandEMIAccessData.brandID
    modal.productID = brandEMIAccessData.productID
    modal.emiSchemeID = brandEMIAccessData.emiSchemeID
    modal.transactionAmount = brandEMIAccessData.transactionAmount
    modal.discountAmount = brandEMIAccessData.discountAmount
    modal.loanAmount = brandEMIAccessData.loanAmount
    modal.interestAmount = brandEMIAccessData.interestAmount
    modal.emiAmount = brandEMIAccessData.emiAmount
    modal.cashBackAmount = brandEMIAccessData.cashBackAmount
    modal.netPayAmount = brandEMIAccessData.netPayAmount
    modal.processingFee = brandEMIAccessData.processingFee
    modal.processingFeeRate = brandEMIAccessData.processingFeeRate
    modal.totalProcessingFee = brandEMIAccessData.totalProcessingFee
    modal.brandName = brandEMIAccessData.brandName
    modal.issuerName = brandEMIAccessData.issuerName
    modal.productName = brandEMIAccessData.productName
    modal.productCode = brandEMIAccessData.productCode
    modal.productModal = brandEMIAccessData.productModal
    modal.productCategoryName = brandEMIAccessData.productCategoryName
    modal.productSerialCode = brandEMIAccessData.productSerialCode
    modal.skuCode = brandEMIAccessData.skuCode
    modal.totalInterest = brandEMIAccessData.totalInterest
    modal.schemeTAndC = brandEMIAccessData.schemeTAndC
    modal.schemeTenureTAndC = brandEMIAccessData.schemeTenureTAndC
    modal.schemeDBDTAndC = brandEMIAccessData.schemeDBDTAndC
    modal.discountCalculatedValue = brandEMIAccessData.discountCalculatedValue
    modal.cashBackCalculatedValue = brandEMIAccessData.cashBackCalculatedValue
    modal.orignalTxnAmt=brandEMIAccessData.orignalTxnAmt
    modal.mobileNo=brandEMIAccessData.mobileNo
    modal.brandReservField=brandEMIAccessData.brandReservField
    modal.issuerTimeStamp=brandEMIAccessData.issuerTimeStamp
    modal.brandTimeStamp=brandEMIAccessData.brandTimeStamp
    modal.productBaseCat=brandEMIAccessData.productBaseCat

/* below invoice is used to define the relation between
 Emi data and Brand Emi data in "BRAND EMI BY ACCESS CODE" module.*/
    modal.hostInvoice = hostInvoice

    runBlocking(Dispatchers.IO) { BrandEMIAccessDataModalTable.performOperation(modal) }
}
}