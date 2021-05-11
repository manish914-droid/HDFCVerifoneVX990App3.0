package com.example.verifonevx990app.brandemibyaccesscode

import android.app.Dialog
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Parcelable
import android.text.TextUtils
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.LinearLayout
import androidx.fragment.app.Fragment
import com.example.verifonevx990app.R
import com.example.verifonevx990app.brandemi.CreateBrandEMIPacket
import com.example.verifonevx990app.databinding.BrandEmiByAccessCodeViewBinding
import com.example.verifonevx990app.emv.transactionprocess.VFTransactionActivity
import com.example.verifonevx990app.main.EMIRequestType
import com.example.verifonevx990app.main.SplitterTypes
import com.example.verifonevx990app.realmtables.BrandEMIAccessDataModalTable
import com.example.verifonevx990app.vxUtils.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.parcelize.Parcelize

class BrandEMIByAccessCodeFragment : Fragment() {
    private val action by lazy { arguments?.getSerializable("type") ?: "" }
    private var iDialog: IDialog? = null
    private var binding: BrandEmiByAccessCodeViewBinding? = null
    private var field57Request: String? = null
    private var totalRecord: String = "0"
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
                        val brandEmiAccessCodeData =
                            responseIsoData.isoMap[57]?.parseRaw2String().toString()

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
                                splitData[30], splitData[31],
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
                WindowManager.LayoutParams.WRAP_CONTENT
            )

            dialog.findViewById<BHButton>(R.id.cancelButton).setOnClickListener {
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
            val discountPercentageLL = dialog.findViewById<LinearLayout>(R.id.discountPercentageLL)
            val discountAmountLL = dialog.findViewById<LinearLayout>(R.id.discountAmountLL)
            val cashBackPercentageLL = dialog.findViewById<LinearLayout>(R.id.cashBackPercentageLL)
            val cashBackAmountLL = dialog.findViewById<LinearLayout>(R.id.cashBackAmountLL)

            productName.text = brandEMIAccessData.productName
            categoryName.text = brandEMIAccessData.productCategoryName
            issuerName.text = brandEMIAccessData.issuerName
            val tenureMonths = "${brandEMIAccessData.tenure} Months"
            tenureTime.text = tenureMonths
            transactionAmountET.text =
                "%.2f".format(brandEMIAccessData.transactionAmount.toFloat() / 100)
            if (!TextUtils.isEmpty(brandEMIAccessData.discountCalculatedValue)) {
                discountPercentageET.text = brandEMIAccessData.discountCalculatedValue
                discountPercentageLL.visibility = View.VISIBLE
            }
            if (!TextUtils.isEmpty(brandEMIAccessData.discountAmount) && brandEMIAccessData.discountAmount != "0") {
                discountAmountET.text = brandEMIAccessData.discountAmount
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
            emiAmountET.text = "%.2f".format(brandEMIAccessData.emiAmount.toFloat() / 100)
            netPayAmountET.text = "%.2f".format(brandEMIAccessData.netPayAmount.toFloat() / 100)



            dialog.findViewById<BHButton>(R.id.submitButton).setOnClickListener {
                dialog.dismiss()
                if (checkInternetConnection()) {
                    GlobalScope.launch(Dispatchers.IO) {
                        saveDataInDB(brandEMIAccessData)
                        activity?.startActivity(
                            Intent(
                                requireActivity(),
                                VFTransactionActivity::class.java
                            ).apply {
                                putExtra("amt", brandEMIAccessData.transactionAmount)
                                putExtra("type", TransactionType.BRAND_EMI_BY_ACCESS_CODE.type)
                                putExtra("proc_code", ProcessingCode.SALE.code)
                                putExtra("mobileNumber", "")
                                putExtra("billNumber", "")
                            })
                    }
                } else {
                    VFService.showToast(getString(R.string.no_internet_available_please_check_your_internet))
                }
            }

            dialog.show()
        }
    }
    //endregion

    //region======================Saving BrandEMI By Access Code Host Response Data in DB:-
    private fun saveDataInDB(brandEMIAccessData: BrandEMIAccessDataModal) {
        val modal = BrandEMIAccessDataModalTable()
        runBlocking(Dispatchers.IO) { BrandEMIAccessDataModalTable.clear() }

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
        runBlocking(Dispatchers.IO) { BrandEMIAccessDataModalTable.performOperation(modal) }
    }
    //endregion
}

//region=============================Brand EMI Master Category Data Modal==========================
@Parcelize
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
) : Parcelable
//endregion