package com.example.verifonevx990app.transactions

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.text.InputFilter
import android.text.TextUtils
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.Animation
import android.view.animation.AnimationUtils
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.example.customneumorphic.ShapeType
import com.example.verifonevx990app.R
import com.example.verifonevx990app.brandemi.BrandEMIDataModal
import com.example.verifonevx990app.databinding.FragmentNewInputAmountBinding
import com.example.verifonevx990app.main.IFragmentRequest
import com.example.verifonevx990app.main.MainActivity
import com.example.verifonevx990app.realmtables.BrandEMIDataTable
import com.example.verifonevx990app.realmtables.EDashboardItem
import com.example.verifonevx990app.realmtables.HdfcCdt
import com.example.verifonevx990app.realmtables.TerminalParameterTable
import com.example.verifonevx990app.utils.KeyboardModel
import com.example.verifonevx990app.vxUtils.*
import com.google.gson.Gson
import kotlinx.coroutines.*
import java.util.*

class NewInputAmountFragment : Fragment() {

    val tpt = TerminalParameterTable.selectFromSchemeTable()

    private val keyModelSaleAmount: KeyboardModel by lazy {
        KeyboardModel()
    }
    private val keyModelCashAmount: KeyboardModel by lazy {
        KeyboardModel()
    }
    private val keyModelMobNumber: KeyboardModel by lazy {
        KeyboardModel()
    }
    var inputInSaleAmount = false
    var inputInCashAmount = false
    var inputInMobilenumber = false
    private lateinit var transactionType: EDashboardItem
    private  var testEmiTxnType: String?=null
    private var iFrReq: IFragmentRequest? = null
    private var subHeaderText: TextView? = null
    private var subHeaderImage: ImageView? = null
    private var subHeaderBackButton: ImageView? = null

    /// private var navController: NavController? = null
    private var cashAmount: EditText? = null
    private var iDialog: IDialog? = null
    private var binding: FragmentNewInputAmountBinding? = null
    private var brandEMIDataModal: BrandEMIDataModal? = null

    private var animShow: Animation? = null
    private var animHide: Animation? = null

    override fun onAttach(activity: Activity) {
        super.onAttach(activity)
        if (activity is MainActivity) {
            activity.hidePoweredByFooter()
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        binding = FragmentNewInputAmountBinding.inflate(inflater, container, false)
        return binding?.root
    }


    private fun initAnimation() {
        animShow = AnimationUtils.loadAnimation(activity, R.anim.view_show)
        animHide = AnimationUtils.loadAnimation(activity, R.anim.view_hide)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        transactionType = arguments?.getSerializable("type") as EDashboardItem
        testEmiTxnType = (arguments?.getSerializable("TestEmiOption")?:"") as String?
        isMobileNumberEntryOnsale { isMobileNeeded, isMobilenumberMandatory ->
            if (isMobileNeeded) {
                binding?.mobNoCrdView?.visibility = View.VISIBLE
            } else {
                binding?.mobNoCrdView?.visibility = View.GONE
            }
        }

        val hdfcTPTData = getHDFCTptData()
        //todo change below
        val hdfcCDTData = HdfcCdt.selectAllHDFCCDTData() ///getHDFCDtData()
        Log.d("HDFCTPTData:- ", hdfcTPTData.toString())
        Log.d("HDFCCDTData:- ", hdfcCDTData.toString())
        initAnimation()

        brandEMIDataModal = arguments?.getSerializable("modal") as? BrandEMIDataModal
        Log.d("Brand EMI Trans Data:- ", Gson().toJson(brandEMIDataModal))

        binding?.mainKeyBoard?.root?.visibility = View.VISIBLE
        binding?.mainKeyBoard?.root?.startAnimation(animShow)

        cashAmount = view.findViewById(R.id.cashAmount)
        ///  navController = Navigation.findNavController(view)
        subHeaderImage = view.findViewById(R.id.header_Image)
        subHeaderImage?.visibility = View.VISIBLE
        subHeaderImage?.setImageResource(transactionType.res)
        when (transactionType) {
            EDashboardItem.SALE_WITH_CASH -> {
                //  binding?.enterCashAmountTv?.visibility = View.VISIBLE
                binding?.cashAmtCrdView?.visibility = View.VISIBLE
                cashAmount?.hint = VerifoneApp.appContext.getString(R.string.cash_amount)
                //   binding?.enterCashAmountTv?.text = VerifoneApp.appContext.getString(R.string.cash_amount)

            }
            EDashboardItem.SALE -> {
                if (checkHDFCTPTFieldsBitOnOff(TransactionType.TIP_SALE)) {
                    //   binding?.enterCashAmountTv?.visibility = View.VISIBLE
                    binding?.cashAmtCrdView?.visibility = View.VISIBLE
                    cashAmount?.hint = VerifoneApp.appContext.getString(R.string.enter_tip_amount)
                    //    binding?.enterCashAmountTv?.text = VerifoneApp.appContext.getString(R.string.enter_tip_amount)

                } else {
                    cashAmount?.visibility = View.GONE
                    binding?.cashAmtCrdView?.visibility = View.GONE
                    //  binding?.enterCashAmountTv?.visibility = View.GONE

                }
            }

            EDashboardItem.DYNAMIC_QR->{
                binding?.mobNoCrdView?.visibility=View.VISIBLE
                binding?.descrCrdView?.visibility=View.VISIBLE
            }
            else -> {
                cashAmount?.visibility = View.GONE
                binding?.cashAmtCrdView?.visibility = View.GONE
                //   binding?.enterCashAmountTv?.visibility = View.GONE
            }
        }

        if (transactionType == EDashboardItem.BRAND_EMI) {
            binding?.subHeaderView?.headerImage?.setImageResource(R.drawable.ic_brand_emi_sub_header_logo)
        }

        if (transactionType == EDashboardItem.BRAND_EMI_CATALOGUE && tpt?.reservedValues?.substring(
                10,
                11
            ) == "0" ||
            transactionType == EDashboardItem.BANK_EMI_CATALOGUE && tpt?.reservedValues?.substring(
                6,
                7
            ) == "0"
        ) {
            binding?.mobNoCrdView?.visibility = View.GONE
        }

        subHeaderText = view.findViewById(R.id.sub_header_text)
        subHeaderBackButton = view.findViewById(R.id.back_image_button)
        setTxnTypeMsg(transactionType.title)
        subHeaderBackButton?.setOnClickListener {
            parentFragmentManager.popBackStackImmediate()
        }
        keyModelSaleAmount.view = binding?.saleAmount
        keyModelSaleAmount.callback = ::onOKClicked
        inputInSaleAmount = true
        inputInCashAmount = false
        inputInMobilenumber = false
        // binding?.saleAmount?.setBackgroundResource(R.drawable.et_bg_selected)
        binding?.saleAmtCrdView?.setShapeType(ShapeType.BASIN)

        if (hdfcTPTData != null) {
            binding?.saleAmount?.filters = arrayOf<InputFilter>(
                InputFilter.LengthFilter(
                    hdfcTPTData.transAmountDigit.toInt()
                )
            )
        }

        binding?.saleAmount?.setOnClickListener {
            keyModelSaleAmount.view = it
            keyModelSaleAmount.callback = ::onOKClicked
            inputInSaleAmount = true
            inputInCashAmount = false
            inputInMobilenumber = false

            binding?.saleAmtCrdView?.setShapeType(ShapeType.BASIN)
            binding?.cashAmtCrdView?.setShapeType(ShapeType.FLAT)
            binding?.mobNoCrdView?.setShapeType(ShapeType.FLAT)
        }

        cashAmount?.setOnClickListener {
            keyModelCashAmount.view = it
            keyModelCashAmount.callback = ::onOKClicked
            inputInSaleAmount = false
            inputInCashAmount = true
            inputInMobilenumber = false

            binding?.saleAmtCrdView?.setShapeType(ShapeType.FLAT)
            binding?.cashAmtCrdView?.setShapeType(ShapeType.BASIN)
            binding?.mobNoCrdView?.setShapeType(ShapeType.FLAT)
        }

        binding?.mobNumbr?.setOnClickListener {
            keyModelMobNumber.view = it
            keyModelMobNumber.callback = ::onOKClicked
            keyModelMobNumber.isInutSimpleDigit = true
            inputInSaleAmount = false
            inputInCashAmount = false
            inputInMobilenumber = true

            binding?.saleAmtCrdView?.setShapeType(ShapeType.FLAT)
            binding?.cashAmtCrdView?.setShapeType(ShapeType.FLAT)
            binding?.mobNoCrdView?.setShapeType(ShapeType.BASIN)
        }

        onSetKeyBoardButtonClick()

    }

    //region============================by sac - to set txn type on amount entry screen:-
    private fun setTxnTypeMsg(strText: String): Unit? {
        return subHeaderText?.setText(strText.toUpperCase(Locale.ROOT))
    }
    //endregion

    override fun onAttach(context: Context) {
        super.onAttach(context)
        if (context is IFragmentRequest) {
            iFrReq = context
        }
        if (context is IDialog) iDialog = context
    }

    override fun onDetach() {
        super.onDetach()
        iFrReq = null
        iDialog = null
        (activity as MainActivity).showPoweredByFooter()
    }



    private fun onOKClicked(amt: String) {
        Log.e("SALE", "OK CLICKED  ${binding?.saleAmount?.text.toString()}")
        Log.e("CASh", "OK CLICKED  ${cashAmount?.text}")
        Log.e("AMT", "OK CLICKED  $amt")
        try {
            (binding?.saleAmount?.text.toString()).toDouble()
        } catch (ex: Exception) {
            ex.printStackTrace()
            VFService.showToast("Sale Amount should be greater than Rs 1")
            return
        }
        val cashAmtStr = (cashAmount?.text.toString())
        var cashAmt = 0.toDouble()
        if (cashAmtStr != "") {
            cashAmt = (cashAmount?.text.toString()).toDouble()
        }
        val saleAmountStr = binding?.saleAmount?.text.toString()
        var saleAmount = 0.toDouble()
        if (saleAmountStr != "") {
            saleAmount = (binding?.saleAmount?.text.toString()).toDouble()
        }

        if (saleAmount < 1) {
            VFService.showToast("Amount should be greater than Rs 1")
            return
        } else if (transactionType == EDashboardItem.SALE_WITH_CASH && (cashAmt < 1)) {
            VFService.showToast("Cash Amount should be greater than Rs 1")
            return
        } else {
            when (transactionType) {
                EDashboardItem.SALE -> {
                    val saleAmt = saleAmount.toString().trim().toFloat()
                    val saleTipAmt = cashAmt.toString().trim().toFloat()
                    val trnsAmt = saleAmt + saleTipAmt
                    if (saleTipAmt > 0) {
                        when {
                            !TextUtils.isEmpty(binding?.mobNumbr?.text.toString()) -> if (binding?.mobNumbr?.text.toString().length in 10..13) {
                                val extraPairData =
                                    Triple(binding?.mobNumbr?.text.toString(), "", third = true)
                                validateTIP(trnsAmt, saleAmt, extraPairData)
                            } else
                                context?.getString(R.string.enter_valid_mobile_number)
                                    ?.let { VFService.showToast(it) }

                            TextUtils.isEmpty(binding?.mobNumbr?.text.toString()) -> {
                                val extraPairData = Triple("", "", third = true)
                                validateTIP(trnsAmt, saleAmt, extraPairData)
                            }
                        }


                    } else {
                        /* if (tpt?.reservedValues?.substring(0, 1) == "1")
                             showMobileBillDialog(activity, TransactionType.SALE.type) { extraPairData ->
                                 iFrReq?.onFragmentRequest(UiAction.START_SALE, Pair(trnsAmt.toString().trim(), cashAmt.toString().trim()), extraPairData)
                             } else
                             iFrReq?.onFragmentRequest(
                                 UiAction.START_SALE,
                                 Pair(trnsAmt.toString().trim(), cashAmt.toString().trim())
                             )*/

                        isMobileNumberEntryOnsale { isMobileNeeded, isMobilenumberMandatory ->
                            if (isMobileNeeded) {
                                when {
                                    !TextUtils.isEmpty(binding?.mobNumbr?.text.toString()) -> if (binding?.mobNumbr?.text.toString().length in 10..13) {
                                        val extraPairData = Triple(
                                            binding?.mobNumbr?.text.toString(),
                                            "",
                                            third = true
                                        )
                                        iFrReq?.onFragmentRequest(
                                            UiAction.START_SALE,
                                            Pair(
                                                trnsAmt.toString().trim(),
                                                cashAmt.toString().trim()
                                            ),
                                            extraPairData
                                        )
                                    } else
                                        context?.getString(R.string.enter_valid_mobile_number)
                                            ?.let { VFService.showToast(it) }

                                    TextUtils.isEmpty(binding?.mobNumbr?.text.toString()) -> {
                                        iFrReq?.onFragmentRequest(
                                            UiAction.START_SALE,
                                            Pair(
                                                trnsAmt.toString().trim(),
                                                cashAmt.toString().trim()
                                            )
                                        )
                                    }
                                }
                            } else {
                                iFrReq?.onFragmentRequest(
                                    UiAction.START_SALE,
                                    Pair(trnsAmt.toString().trim(), cashAmt.toString().trim())
                                )
                            }
                        }
                    }
                }
                EDashboardItem.BANK_EMI, EDashboardItem.TEST_EMI -> {
                    var uiAction = UiAction.BANK_EMI
                    if (transactionType == EDashboardItem.TEST_EMI) {
                        uiAction = UiAction.TEST_EMI
                    }
                    if (tpt?.reservedValues?.substring(1, 2) == "1" && tpt.reservedValues.substring(2, 3) == "1") {
                        showMobileBillDialog(activity, TransactionType.EMI_SALE.type) { extraPairData ->
                            if (extraPairData.third) {
                                iFrReq?.onFragmentRequest(
                                    uiAction,
                                    Pair(saleAmount.toString().trim(), testEmiTxnType),
                                    extraPairData
                                )
                            } else {
                                startActivity(
                                    Intent(
                                        requireActivity(),
                                        MainActivity::class.java
                                    ).apply {
                                        flags =
                                            Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_CLEAR_TASK
                                    })
                            }
                        }
                    }
                    else if (tpt?.reservedValues?.substring(1, 2) == "1") {
                        showMobileBillDialog(
                            activity,
                            TransactionType.EMI_SALE.type
                        ) { extraPairData ->
                            if (extraPairData.third) {
                                iFrReq?.onFragmentRequest(
                                    uiAction,
                                    Pair(saleAmount.toString().trim(), testEmiTxnType),
                                    extraPairData
                                )
                            } else {
                                startActivity(
                                    Intent(
                                        requireActivity(),
                                        MainActivity::class.java
                                    ).apply {
                                        flags =
                                            Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_CLEAR_TASK
                                    })
                            }
                        }
                    }
                    else if (tpt?.reservedValues?.substring(2, 3) == "1") {
                        showMobileBillDialog(
                            activity,
                            TransactionType.EMI_SALE.type
                        ) { extraPairData ->
                            if (extraPairData.third) {
                                iFrReq?.onFragmentRequest(
                                    uiAction,
                                    Pair(saleAmount.toString().trim(), testEmiTxnType),
                                    extraPairData
                                )
                            } else {
                                startActivity(
                                    Intent(
                                        requireActivity(),
                                        MainActivity::class.java
                                    ).apply {
                                        flags =
                                            Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_CLEAR_TASK
                                    })
                            }
                        }
                    }
                    else {
                        iFrReq?.onFragmentRequest(
                            uiAction,
                            Pair(saleAmount.toString().trim(), testEmiTxnType),
                            Triple("", "", true)
                        )
                    }
                }

                EDashboardItem.BRAND_EMI -> {
                    val checkSaleAmount = saleAmount.toString().trim().toDouble()
                    if (checkSaleAmount >= brandEMIDataModal?.getProductMinAmount()?.toDouble() ?: 0.0 && checkSaleAmount <= brandEMIDataModal?.getProductMaxAmount()?.toDouble() ?: 0.0) {
                        enableDisableMobileAndInvoiceField()
                    } else {
                        VFService.showToast("Entered Amount Should be in Product Min & Max Amount Range")
                    }
                }

                EDashboardItem.CASH_ADVANCE -> {
                    iFrReq?.onFragmentRequest(
                        UiAction.CASH_AT_POS,
                        Pair(
                            saleAmount.toString().trim(),
                            saleAmount.toString().trim()
                        )
                    )
                }
                EDashboardItem.SALE_WITH_CASH -> {
                    iFrReq?.onFragmentRequest(
                        UiAction.SALE_WITH_CASH,
                        Pair(
                            saleAmount.toString().trim(),
                            cashAmt.toString().trim()
                        )
                    )
                }
                EDashboardItem.REFUND -> {
                    iFrReq?.onFragmentRequest(
                        UiAction.REFUND,
                        Pair(saleAmount.toString().trim(), "0")
                    )
                }
                EDashboardItem.PREAUTH -> {
                    iFrReq?.onFragmentRequest(
                        UiAction.PRE_AUTH,
                        Pair(saleAmount.toString().trim(), "0")
                    )
                }
                EDashboardItem.EMI_ENQUIRY -> {
                    if (TerminalParameterTable.selectFromSchemeTable()?.bankEnquiryMobNumberEntry == true) {
                        showMobileBillDialog(activity, TransactionType.EMI_ENQUIRY.type) {
                            //  sendStartSale(inputAmountEditText?.text.toString(), extraPairData)
                            iFrReq?.onFragmentRequest(
                                UiAction.EMI_ENQUIRY,
                                Pair(saleAmount.toString().trim(), "0"), it
                            )
                        }
                    } else {
                        iFrReq?.onFragmentRequest(
                            UiAction.EMI_ENQUIRY,
                            Pair(saleAmount.toString().trim(), "0")
                        )
                    }
                }
                EDashboardItem.FLEXI_PAY -> {
                    iFrReq?.onFragmentRequest(
                        UiAction.FLEXI_PAY,
                        Pair(saleAmount.toString().trim(), "0")
                    )
                }
                EDashboardItem.BRAND_EMI_CATALOGUE -> {
                    val checkSaleAmount = binding?.saleAmount?.text.toString().trim().toDouble()
                    if (checkSaleAmount >= brandEMIDataModal?.getProductMinAmount()
                            ?.toDouble() ?: 0.0
                        && checkSaleAmount <= brandEMIDataModal?.getProductMaxAmount()
                            ?.toDouble() ?: 0.0
                    ) {
                        GlobalScope.launch(Dispatchers.IO) {
                            saveBrandEMIDataToDB("", "")
                            withContext(Dispatchers.Main) {
                                if (tpt?.reservedValues?.substring(10, 11) == "1") {
                                    when {
                                        TextUtils.isEmpty(
                                            saleAmount.toString().trim()
                                        ) -> VFService.showToast("Enter Sale Amount")
                                     //   TextUtils.isEmpty(binding?.mobNumbr?.text?.toString()?.trim()) -> VFService.showToast("Enter Mobile Number")
                                        else -> iFrReq?.onFragmentRequest(
                                            UiAction.BRAND_EMI_CATALOGUE,
                                            Pair(
                                                saleAmount.toString().trim(),
                                                cashAmt.toString().trim()
                                            )
                                        )
                                    }
                                } else {
                                    when {
                                        TextUtils.isEmpty(
                                            saleAmount.toString().trim()
                                        ) -> VFService.showToast("Enter Sale Amount")
                                        else -> iFrReq?.onFragmentRequest(
                                            UiAction.BRAND_EMI_CATALOGUE,
                                            Pair(
                                                saleAmount.toString().trim(),
                                                cashAmt.toString().trim()
                                            )
                                        )
                                    }
                                }
                            }
                        }
                    } else {
                        VFService.showToast("Entered Amount Should be in Product Min & Max Amount Range")
                    }
                }
                EDashboardItem.BANK_EMI_CATALOGUE -> {
                    if (tpt?.reservedValues?.substring(10, 11) == "1") {
                        when {
                            TextUtils.isEmpty(saleAmount.toString().trim()) -> VFService.showToast("Enter Sale Amount")
                          //  TextUtils.isEmpty(binding?.mobNumbr?.text?.toString()?.trim()) -> VFService.showToast("Enter Mobile Number")
                            else -> iFrReq?.onFragmentRequest(
                                UiAction.BANK_EMI_CATALOGUE,
                                Pair(saleAmount.toString().trim(), cashAmt.toString().trim())
                            )
                        }
                    } else {
                        when {
                            TextUtils.isEmpty(
                                saleAmount.toString().trim()
                            ) -> VFService.showToast("Enter Sale Amount")
                            else -> iFrReq?.onFragmentRequest(
                                UiAction.BANK_EMI_CATALOGUE,
                                Pair(
                                    saleAmount.toString().trim(),
                                    cashAmt.toString().trim()
                                )
                            )
                        }
                    }
                }

                EDashboardItem.DYNAMIC_QR->{
                    if( binding?.mobNumbr?.text.toString().length !in 10..13 ){
                        context?.getString(R.string.enter_valid_mobile_number)?.let { VFService.showToast(it) }
                    }else{
                        val extraPairData = Triple(
                            binding?.mobNumbr?.text.toString(),
                            binding?.descriptionEt?.text.toString(),
                            third = true
                        )
                        iFrReq?.onFragmentRequest(
                            UiAction.DYNAMIC_QR,
                            Pair(
                                saleAmount.toString().trim(),
                               "0"
                            ),extraPairData
                        )
                    }

                }

                else -> {
                }
            }
        }
    }

    //region===================Enable/Disable Mobile And Invoice Field For Brand EMI:-
    private fun enableDisableMobileAndInvoiceField() {
        if (brandEMIDataModal != null) {
            if (brandEMIDataModal?.getBrandReservedValue()?.substring(0, 1) == "1" ||
                brandEMIDataModal?.getBrandReservedValue()?.substring(0, 1) == "2" ||
                brandEMIDataModal?.getBrandReservedValue()?.substring(2, 3) == "1" ||
                brandEMIDataModal?.getBrandReservedValue()?.substring(2, 3) == "2"
            ) {
                showMobileBillDialog(
                    activity,
                    TransactionType.BRAND_EMI.type,
                    brandEMIDataModal
                ) { extraPairData ->
                    GlobalScope.launch(Dispatchers.Main) {
                        if (extraPairData.third) {
                            if (isShowIMEISerialDialog()) {
                                showIMEISerialDialog(activity, brandEMIDataModal) { cbData ->
                                    if (cbData.third) {
                                        GlobalScope.launch(Dispatchers.IO) {
                                            saveBrandEMIDataToDB(cbData.first, cbData.second)
                                            withContext(Dispatchers.Main) {
                                                iFrReq?.onFragmentRequest(
                                                    UiAction.BRAND_EMI,
                                                    Pair(
                                                        binding?.saleAmount?.text.toString().trim(),
                                                        "0"
                                                    ),
                                                    extraPairData
                                                )
                                            }
                                        }
                                    } else {
                                        startActivity(
                                            Intent(
                                                requireActivity(),
                                                MainActivity::class.java
                                            ).apply {
                                                flags =
                                                    Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_CLEAR_TASK
                                            })
                                    }
                                }
                            } else {
                                GlobalScope.launch(Dispatchers.IO) {
                                    saveBrandEMIDataToDB("", "")
                                    withContext(Dispatchers.Main) {
                                        iFrReq?.onFragmentRequest(
                                            UiAction.BRAND_EMI,
                                            Pair(binding?.saleAmount?.text.toString().trim(), "0"),
                                            extraPairData
                                        )
                                    }
                                }
                            }
                        } else {
                            startActivity(
                                Intent(
                                    requireActivity(),
                                    MainActivity::class.java
                                ).apply {
                                    flags =
                                        Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_CLEAR_TASK
                                })
                        }
                    }
                }
            } else {
                GlobalScope.launch(Dispatchers.Main) {
                    if (isShowIMEISerialDialog()) {
                        showIMEISerialDialog(activity, brandEMIDataModal) { cbData ->
                            if (cbData.third) {
                                GlobalScope.launch(Dispatchers.IO) {
                                    saveBrandEMIDataToDB(cbData.first, cbData.second)
                                    withContext(Dispatchers.Main) {
                                        iFrReq?.onFragmentRequest(
                                            UiAction.BRAND_EMI,
                                            Pair(binding?.saleAmount?.text.toString().trim(), "0"),
                                            Triple("", "", true)
                                        )
                                    }
                                }
                            } else {
                                startActivity(
                                    Intent(
                                        requireActivity(),
                                        MainActivity::class.java
                                    ).apply {
                                        flags =
                                            Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_CLEAR_TASK
                                    })
                            }
                        }
                    } else {
                        GlobalScope.launch(Dispatchers.IO) {
                            saveBrandEMIDataToDB("", "")
                            withContext(Dispatchers.Main) {
                                iFrReq?.onFragmentRequest(
                                    UiAction.BRAND_EMI,
                                    Pair(binding?.saleAmount?.text.toString().trim(), "0"),
                                    Triple("", "", true)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
    //endregion

    //region=====================Condition to check Whether we need to show IMEI/Serial Dialog or not:-
    private fun isShowIMEISerialDialog(): Boolean {
        return brandEMIDataModal?.getValidationTypeName() == "IMEI" ||
                brandEMIDataModal?.getValidationTypeName() == "imei" ||
                brandEMIDataModal?.getValidationTypeName() == "Serial Number" ||
                brandEMIDataModal?.getValidationTypeName() == "serial number"
    }
    //endregion

    //region======================Saving BrandEMI Data To DB:-
    private fun saveBrandEMIDataToDB(imeiNumber: String?, serialNumber: String?) {
        val modal = BrandEMIDataTable()
        runBlocking(Dispatchers.IO) { BrandEMIDataTable.clear() }

        //Stubbing Data to BrandEMIDataTable:-
        modal.brandID = brandEMIDataModal?.getBrandID() ?: ""
        modal.brandName = brandEMIDataModal?.getBrandName() ?: ""
        modal.brandReservedValues = brandEMIDataModal?.getBrandReservedValue() ?: ""
        modal.categoryID = brandEMIDataModal?.getCategoryID() ?: ""
        modal.categoryName = brandEMIDataModal?.getCategoryName() ?: ""
        modal.productID = brandEMIDataModal?.getProductID() ?: ""
        modal.productName = brandEMIDataModal?.getProductName() ?: ""
        modal.childSubCategoryID = brandEMIDataModal?.getChildSubCategoryID() ?: ""
        modal.childSubCategoryName = brandEMIDataModal?.getChildSubCategoryName() ?: ""
        modal.validationTypeName = brandEMIDataModal?.getValidationTypeName() ?: ""
        modal.isRequired = brandEMIDataModal?.getIsRequired() ?: ""
        modal.inputDataType = brandEMIDataModal?.getInputDataType() ?: ""
        modal.imeiNumber = imeiNumber ?: ""
        modal.serialNumber = serialNumber ?: ""
        modal.emiType = transactionType.title
        runBlocking(Dispatchers.IO) { BrandEMIDataTable.performOperation(modal) }
    }
    //endregion

    private fun validateTIP(
        totalTransAmount: Float,
        saleAmt: Float,
        extraPair: Triple<String, String, Boolean>
    ) {
        val tpt = TerminalParameterTable.selectFromSchemeTable()
        if (tpt != null) {
            val tipAmount = try {
                cashAmount?.text.toString().toFloat()
            } catch (ex: Exception) {
                0f
            }
            val maxTipPercent =
                if (tpt.maxTipPercent.isEmpty()) 0f else (tpt.maxTipPercent.toFloat()).div(
                    100
                )
            val maxTipLimit =
                if (tpt.maxTipLimit.isEmpty()) 0f else (tpt.maxTipLimit.toFloat()).div(
                    100
                )
            if (maxTipLimit != 0f) { // flat tip check is applied
                if (tipAmount <= maxTipLimit) {
                    // iDialog?.showProgress()
                    GlobalScope.launch {

                        iFrReq?.onFragmentRequest(
                            UiAction.START_SALE,
                            Pair(
                                totalTransAmount.toString().trim(),
                                cashAmount?.text.toString().trim()
                            ), extraPair
                        )
                    }
                } else {
                    val msg =
                        "Maximum tip allowed on this terminal is \u20B9 ${
                            "%.2f".format(
                                maxTipLimit
                            )
                        }."
                    GlobalScope.launch(Dispatchers.Main) {
                        iDialog?.getInfoDialog("Tip Sale Error", msg) {}
                    }
                }
            } else { // percent tip check is applied
                val saleAmount = saleAmt
                val maxAmountTip = (maxTipPercent / 100) * saleAmount
                val formatMaxTipAmount = "%.2f".format(maxAmountTip)
                if (tipAmount <= maxAmountTip) {
                    //   iDialog?.showProgress()
                    GlobalScope.launch {

                        iFrReq?.onFragmentRequest(
                            UiAction.START_SALE,
                            Pair(
                                totalTransAmount.toString().trim(),
                                cashAmount?.text.toString().trim()
                            ), extraPair
                        )
                    }
                } else {
                    //    val tipAmt = saleAmt * per / 100
                    val msg = "Tip limit for this transaction is \n \u20B9 ${
                        "%.2f".format(
                            formatMaxTipAmount.toDouble()
                        )
                    }"
                    /* "Maximum ${"%.2f".format(
                         maxTipPercent.toDouble()
                     )}% tip allowed on this terminal.\nTip limit for this transaction is \u20B9 ${"%.2f".format(
                         formatMaxTipAmount.toDouble()
                     )}"*/
                    GlobalScope.launch(Dispatchers.Main) {
                        iDialog?.getInfoDialog("Tip Sale Error", msg) {}
                    }
                }
            }
        } else {
            VFService.showToast("TPT not fount")
        }
    }


    // fun for checking mobile number on sale
    /*
    first Boolean--> Is mobile number field needed
    second Boolean --> Is mobile number mandatory
     */
    private fun isMobileNumberEntryOnsale(cb: (Boolean, Boolean) -> Unit) {
        if (transactionType == EDashboardItem.SALE && tpt?.reservedValues?.substring(0, 1) == "1")
            cb(true, false)
        else
            cb(false, false)
    }

    private fun onSetKeyBoardButtonClick() {
        binding?.mainKeyBoard?.key0?.setOnClickListener {
            when {
                inputInSaleAmount -> {
                    keyModelSaleAmount.onKeyClicked("0")
                }
                inputInMobilenumber -> {
                    keyModelMobNumber.onKeyClicked("0")
                }
                else -> {
                    keyModelCashAmount.onKeyClicked("0")
                }
            }
        }
        binding?.mainKeyBoard?.key00?.setOnClickListener {
            when {
                inputInSaleAmount -> {
                    keyModelSaleAmount.onKeyClicked("00")
                }
                inputInMobilenumber -> {
                    keyModelMobNumber.onKeyClicked("00")
                }
                else -> {
                    keyModelCashAmount.onKeyClicked("00")
                }
            }
        }
        binding?.mainKeyBoard?.key000?.setOnClickListener {
            when {
                inputInSaleAmount -> {
                    keyModelSaleAmount.onKeyClicked("000")
                }
                inputInMobilenumber -> {
                    keyModelMobNumber.onKeyClicked("000")
                }
                else -> {
                    keyModelCashAmount.onKeyClicked("000")
                }
            }
        }
        binding?.mainKeyBoard?.key1?.setOnClickListener {
            when {
                inputInSaleAmount -> {
                    keyModelSaleAmount.onKeyClicked("1")
                }
                inputInMobilenumber -> {
                    keyModelMobNumber.onKeyClicked("1")
                }
                else -> {
                    keyModelCashAmount.onKeyClicked("1")
                }
            }
        }
        binding?.mainKeyBoard?.key2?.setOnClickListener {
            when {
                inputInSaleAmount -> {
                    Log.e("SALE", "KEY 2")
                    keyModelSaleAmount.onKeyClicked("2")
                }
                inputInMobilenumber -> {
                    keyModelMobNumber.onKeyClicked("2")
                }
                else -> {
                    Log.e("CASH", "KEY 2")
                    keyModelCashAmount.onKeyClicked("2")
                }
            }
        }
        binding?.mainKeyBoard?.key3?.setOnClickListener {
            when {
                inputInSaleAmount -> {
                    keyModelSaleAmount.onKeyClicked("3")
                }
                inputInMobilenumber -> {
                    keyModelMobNumber.onKeyClicked("3")
                }
                else -> {
                    keyModelCashAmount.onKeyClicked("3")
                }
            }
        }
        binding?.mainKeyBoard?.key4?.setOnClickListener {
            when {
                inputInSaleAmount -> {
                    keyModelSaleAmount.onKeyClicked("4")
                }
                inputInMobilenumber -> {
                    keyModelMobNumber.onKeyClicked("4")
                }
                else -> {
                    keyModelCashAmount.onKeyClicked("4")
                }
            }
        }
        binding?.mainKeyBoard?.key5?.setOnClickListener {
            when {
                inputInSaleAmount -> {
                    keyModelSaleAmount.onKeyClicked("5")
                }
                inputInMobilenumber -> {
                    keyModelMobNumber.onKeyClicked("5")
                }
                else -> {
                    keyModelCashAmount.onKeyClicked("5")
                }
            }
        }
        binding?.mainKeyBoard?.key6?.setOnClickListener {
            when {
                inputInSaleAmount -> {
                    keyModelSaleAmount.onKeyClicked("6")
                }
                inputInMobilenumber -> {
                    keyModelMobNumber.onKeyClicked("6")
                }
                else -> {
                    keyModelCashAmount.onKeyClicked("6")
                }
            }
        }
        binding?.mainKeyBoard?.key7?.setOnClickListener {
            when {
                inputInSaleAmount -> {
                    keyModelSaleAmount.onKeyClicked("7")
                }
                inputInMobilenumber -> {
                    keyModelMobNumber.onKeyClicked("7")
                }
                else -> {
                    keyModelCashAmount.onKeyClicked("7")
                }
            }
        }
        binding?.mainKeyBoard?.key8?.setOnClickListener {
            when {
                inputInSaleAmount -> {
                    keyModelSaleAmount.onKeyClicked("8")
                }
                inputInMobilenumber -> {
                    keyModelMobNumber.onKeyClicked("8")
                }
                else -> {
                    keyModelCashAmount.onKeyClicked("8")
                }
            }
        }
        binding?.mainKeyBoard?.key9?.setOnClickListener {
            when {
                inputInSaleAmount -> {
                    keyModelSaleAmount.onKeyClicked("9")
                }
                inputInMobilenumber -> {
                    keyModelMobNumber.onKeyClicked("9")
                }
                else -> {
                    keyModelCashAmount.onKeyClicked("9")
                }
            }
        }
        binding?.mainKeyBoard?.keyClr?.setOnClickListener {
            when {
                inputInSaleAmount -> {
                    keyModelSaleAmount.onKeyClicked("c")
                }
                inputInMobilenumber -> {
                    keyModelMobNumber.onKeyClicked("c")
                }
                else -> {
                    keyModelCashAmount.onKeyClicked("c")
                }
            }
        }
        binding?.mainKeyBoard?.keyDelete?.setOnClickListener {
            when {
                inputInSaleAmount -> {
                    keyModelSaleAmount.onKeyClicked("d")
                }
                inputInMobilenumber -> {
                    keyModelMobNumber.onKeyClicked("d")
                }
                else -> {
                    keyModelCashAmount.onKeyClicked("d")
                }
            }
        }
        binding?.mainKeyBoard?.keyOK?.setOnClickListener {
            when {
                inputInSaleAmount -> {
                    keyModelSaleAmount.onKeyClicked("o")
                }
                inputInMobilenumber -> {
                    keyModelMobNumber.onKeyClicked("o")
                }
                else -> {
                    keyModelCashAmount.onKeyClicked("o")
                }
            }
        }

    }

}