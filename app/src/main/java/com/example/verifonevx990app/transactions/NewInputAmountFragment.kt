
package com.example.verifonevx990app.transactions

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.text.InputFilter
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
    private val keyModelSaleAmount: KeyboardModel by lazy {
        KeyboardModel()
    }
    private val keyModelCashAmount: KeyboardModel by lazy {
        KeyboardModel()
    }
    var inputInSaleAmount = false
    var inputInCashAmount = false
    private lateinit var transactionType: EDashboardItem
    private var iFrReq: IFragmentRequest? = null
    private var subHeaderText: TextView? = null
    private var subHeaderImage: ImageView? = null
    private var subHeaderBackButton: ImageView? = null

    /// private var navController: NavController? = null
    private var cashAmount: EditText? = null
    private var iDialog: IDialog? = null
    private var binding: FragmentNewInputAmountBinding? = null
    private var brandEMIDataModal: BrandEMIDataModal? = null


    override fun onCreateView(
            inflater: LayoutInflater,
            container: ViewGroup?,
            savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        binding = FragmentNewInputAmountBinding.inflate(inflater, container, false)
        return binding?.root
    }

    private var animShow: Animation? = null
    private var animHide: Animation? = null
    private fun initAnimation() {
        animShow = AnimationUtils.loadAnimation(activity, R.anim.view_show)
        animHide = AnimationUtils.loadAnimation(activity, R.anim.view_hide)
    }
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        ///   (activity as NavigationActivity).showBottomNavigationBar(isShow = false)
        //    (activity as AppCompatActivity?)?.supportActionBar?.hide()
        //    (activity as MainActivity).isAppbarVisibleOrGone(false)
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
        transactionType = arguments?.getSerializable("type") as EDashboardItem
        subHeaderImage = view.findViewById(R.id.header_Image)
        subHeaderImage?.visibility = View.VISIBLE
        subHeaderImage?.setImageResource(transactionType.res)
        if (transactionType == EDashboardItem.SALE_WITH_CASH) {
            //  binding?.enterCashAmountTv?.visibility = View.VISIBLE
            cashAmount?.visibility = View.VISIBLE
            cashAmount?.hint = VerifoneApp.appContext.getString(R.string.cash_amount)
            //   binding?.enterCashAmountTv?.text = VerifoneApp.appContext.getString(R.string.cash_amount)

        } else if (transactionType == EDashboardItem.SALE) {
            if (checkHDFCTPTFieldsBitOnOff(TransactionType.TIP_SALE)) {
                //   binding?.enterCashAmountTv?.visibility = View.VISIBLE
                cashAmount?.visibility = View.VISIBLE
                cashAmount?.hint = VerifoneApp.appContext.getString(R.string.enter_tip_amount)
                //    binding?.enterCashAmountTv?.text = VerifoneApp.appContext.getString(R.string.enter_tip_amount)

            } else {
                cashAmount?.visibility = View.GONE
                //  binding?.enterCashAmountTv?.visibility = View.GONE

            }
        } else {
            cashAmount?.visibility = View.GONE
            //   binding?.enterCashAmountTv?.visibility = View.GONE
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
        // binding?.saleAmount?.setBackgroundResource(R.drawable.et_bg_selected)
        binding?.saleAmtCrdView?.setShapeType(ShapeType.BASIN)

        if (hdfcTPTData != null) {
            binding?.saleAmount?.filters = arrayOf<InputFilter>(
                    InputFilter.LengthFilter(
                            hdfcTPTData.transAmountDigit.toInt()
                    )
            )
        }
        /*  binding?.enterSaleAmtTv?.setTextColor(
              ContextCompat.getColor(
                  VerifoneApp.appContext,
                  R.color.colorPrimary
              )
          )*/
        binding?.saleAmount?.setOnClickListener {
            keyModelSaleAmount.view = it
            keyModelSaleAmount.callback = ::onOKClicked
            inputInSaleAmount = true
            inputInCashAmount = false
            //    it.setBackgroundResource(R.drawable.et_bg_selected)
            //  cashAmount?.setBackgroundResource(R.drawable.et_bg_un)
            /* binding?.enterSaleAmtTv?.setTextColor(
                 ContextCompat.getColor(
                     VerifoneApp.appContext,
                     R.color.colorPrimary
                 )
             )
             binding?.enterCashAmountTv?.setTextColor(
                 ContextCompat.getColor(
                     VerifoneApp.appContext,
                     R.color.black
                 )
             )*/
            binding?.saleAmtCrdView?.setShapeType(ShapeType.BASIN)
            binding?.cashAmtCrdView?.setShapeType(ShapeType.FLAT)
        }
        cashAmount?.setOnClickListener {
            keyModelCashAmount.view = it
            keyModelCashAmount.callback = ::onOKClicked
            inputInSaleAmount = false
            inputInCashAmount = true
            //   it.setBackgroundResource(R.drawable.et_bg_selected)
            // binding?.saleAmount?.setBackgroundResource(R.drawable.et_bg_un)
            /* binding?.enterSaleAmtTv?.setTextColor(
                 ContextCompat.getColor(
                     VerifoneApp.appContext,
                     R.color.colorPrimary
                 )
             )
             binding?.enterSaleAmtTv?.setTextColor(
                 ContextCompat.getColor(
                     VerifoneApp.appContext,
                     R.color.black
                 )
             )*/
            binding?.saleAmtCrdView?.setShapeType(ShapeType.FLAT)
            binding?.cashAmtCrdView?.setShapeType(ShapeType.BASIN)
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
    }

    private fun onSetKeyBoardButtonClick() {
        binding?.mainKeyBoard?.key0?.setOnClickListener {
            if (inputInSaleAmount) {
                keyModelSaleAmount.onKeyClicked("0")
            } else {
                keyModelCashAmount.onKeyClicked("0")
            }
        }
        binding?.mainKeyBoard?.key00?.setOnClickListener {
            if (inputInSaleAmount) {
                keyModelSaleAmount.onKeyClicked("00")
            } else {
                keyModelCashAmount.onKeyClicked("00")
            }
        }
        binding?.mainKeyBoard?.key000?.setOnClickListener {
            if (inputInSaleAmount) {
                keyModelSaleAmount.onKeyClicked("000")
            } else {
                keyModelCashAmount.onKeyClicked("000")
            }
        }
        binding?.mainKeyBoard?.key1?.setOnClickListener {
            if (inputInSaleAmount) {
                keyModelSaleAmount.onKeyClicked("1")
            } else {
                keyModelCashAmount.onKeyClicked("1")
            }
        }
        binding?.mainKeyBoard?.key2?.setOnClickListener {
            if (inputInSaleAmount) {
                Log.e("SALE", "KEY 2")
                keyModelSaleAmount.onKeyClicked("2")
            } else {
                Log.e("CASH", "KEY 2")
                keyModelCashAmount.onKeyClicked("2")
            }
        }
        binding?.mainKeyBoard?.key3?.setOnClickListener {
            if (inputInSaleAmount) {
                keyModelSaleAmount.onKeyClicked("3")
            } else {
                keyModelCashAmount.onKeyClicked("3")
            }
        }
        binding?.mainKeyBoard?.key4?.setOnClickListener {
            if (inputInSaleAmount) {
                keyModelSaleAmount.onKeyClicked("4")
            } else {
                keyModelCashAmount.onKeyClicked("4")
            }
        }
        binding?.mainKeyBoard?.key5?.setOnClickListener {
            if (inputInSaleAmount) {
                keyModelSaleAmount.onKeyClicked("5")
            } else {
                keyModelCashAmount.onKeyClicked("5")
            }
        }
        binding?.mainKeyBoard?.key6?.setOnClickListener {
            if (inputInSaleAmount) {
                keyModelSaleAmount.onKeyClicked("6")
            } else {
                keyModelCashAmount.onKeyClicked("6")
            }
        }
        binding?.mainKeyBoard?.key7?.setOnClickListener {
            if (inputInSaleAmount) {
                keyModelSaleAmount.onKeyClicked("7")
            } else {
                keyModelCashAmount.onKeyClicked("7")
            }
        }
        binding?.mainKeyBoard?.key8?.setOnClickListener {
            if (inputInSaleAmount) {
                keyModelSaleAmount.onKeyClicked("8")
            } else {
                keyModelCashAmount.onKeyClicked("8")
            }
        }
        binding?.mainKeyBoard?.key9?.setOnClickListener {
            if (inputInSaleAmount) {
                keyModelSaleAmount.onKeyClicked("9")
            } else {
                keyModelCashAmount.onKeyClicked("9")
            }
        }
        binding?.mainKeyBoard?.keyClr?.setOnClickListener {
            if (inputInSaleAmount) {
                keyModelSaleAmount.onKeyClicked("c")
            } else {
                keyModelCashAmount.onKeyClicked("c")
            }
        }
        binding?.mainKeyBoard?.keyDelete?.setOnClickListener {
            if (inputInSaleAmount) {
                keyModelSaleAmount.onKeyClicked("d")
            } else {
                keyModelCashAmount.onKeyClicked("d")
            }
        }
        binding?.mainKeyBoard?.keyOK?.setOnClickListener {
            if (inputInSaleAmount) {
                keyModelSaleAmount.onKeyClicked("o")
            } else {
                keyModelCashAmount.onKeyClicked("o")
            }
        }

    }

    private fun onOKClicked(amt: String) {
        Log.e("SALE", "OK CLICKED  ${binding?.saleAmount?.text.toString()}")
        Log.e("CASh", "OK CLICKED  ${cashAmount?.text}")
        Log.e("AMT", "OK CLICKED  $amt")
        if ((binding?.saleAmount?.text.toString()).toDouble() < 1) {
            VFService.showToast("Sale Amount should be greater than Rs 1")
            return
        } else if (transactionType == EDashboardItem.SALE_WITH_CASH && (cashAmount?.text.toString()).toDouble() < 1) {
            VFService.showToast("Cash Amount should be greater than Rs 1")
            return
        } else {
            when (transactionType) {
                EDashboardItem.SALE -> {
                    val saleAmt = binding?.saleAmount?.text.toString().trim().toFloat()
                    val saleTipAmt = cashAmount?.text.toString().trim().toFloat()
                    val trnsAmt = saleAmt + saleTipAmt
                    if (saleTipAmt > 0) {
                        validateTIP(trnsAmt, saleAmt)
                    } else {
                        if (tpt?.reservedValues?.substring(0, 1) == "1")
                            showMobileBillDialog(
                                    activity,
                                    TransactionType.SALE.type
                            ) { extraPairData ->
                                iFrReq?.onFragmentRequest(
                                        UiAction.START_SALE,
                                        Pair(
                                                trnsAmt.toString().trim(),
                                                cashAmount?.text.toString().trim()
                                        ), extraPairData
                                )
                            } else
                            iFrReq?.onFragmentRequest(
                                    UiAction.START_SALE,
                                    Pair(trnsAmt.toString().trim(), cashAmount?.text.toString().trim())
                            )
                    }
                }
                EDashboardItem.BANK_EMI, EDashboardItem.TEST_EMI -> {
                    var uiAction = UiAction.BANK_EMI
                    if (transactionType == EDashboardItem.TEST_EMI) {
                        uiAction = UiAction.TEST_EMI
                    }
                    if (tpt?.reservedValues?.substring(1, 2) == "1" && tpt.reservedValues.substring(
                                    2,
                                    3
                            ) == "1"
                    ) {
                        showMobileBillDialog(
                                activity,
                                TransactionType.EMI_SALE.type
                        ) { extraPairData ->
                            if (extraPairData.third) {
                                iFrReq?.onFragmentRequest(
                                        uiAction,
                                        Pair(binding?.saleAmount?.text.toString().trim(), "0"),
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
                    } else if (tpt?.reservedValues?.substring(1, 2) == "1") {
                        showMobileBillDialog(
                                activity,
                                TransactionType.EMI_SALE.type
                        ) { extraPairData ->
                            if (extraPairData.third) {
                                iFrReq?.onFragmentRequest(
                                        uiAction,
                                        Pair(binding?.saleAmount?.text.toString().trim(), "0"),
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
                    } else if (tpt?.reservedValues?.substring(2, 3) == "1") {
                        showMobileBillDialog(
                                activity,
                                TransactionType.EMI_SALE.type
                        ) { extraPairData ->
                            if (extraPairData.third) {
                                iFrReq?.onFragmentRequest(
                                        uiAction,
                                        Pair(binding?.saleAmount?.text.toString().trim(), "0"),
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
                    } else {
                        iFrReq?.onFragmentRequest(
                                uiAction,
                                Pair(binding?.saleAmount?.text.toString().trim(), "0"),
                                Triple("", "", true)
                        )
                    }
                }

                EDashboardItem.BRAND_EMI -> {
                    if (binding?.saleAmount?.text.toString()
                                    .trim() >= brandEMIDataModal?.getProductMinAmount() ?: "0"
                            && binding?.saleAmount?.text.toString()
                                    .trim() <= brandEMIDataModal?.getProductMaxAmount() ?: "0"
                    ) {
                        enableDisableMobileAndInvoiceField()
                    } else {
                        VFService.showToast("Entered Amount Should be in Product Min & Max Amount Range")
                    }
                }

                EDashboardItem.CASH_ADVANCE -> {
                    iFrReq?.onFragmentRequest(
                            UiAction.CASH_AT_POS,
                            Pair(
                                    binding?.saleAmount?.text.toString().trim(),
                                    binding?.saleAmount?.text.toString().trim()
                            )
                    )
                }
                EDashboardItem.SALE_WITH_CASH -> {
                    iFrReq?.onFragmentRequest(
                            UiAction.SALE_WITH_CASH,
                            Pair(
                                    binding?.saleAmount?.text.toString().trim(),
                                    cashAmount?.text.toString().trim()
                            )
                    )
                }
                EDashboardItem.REFUND -> {
                    iFrReq?.onFragmentRequest(
                            UiAction.REFUND,
                            Pair(binding?.saleAmount?.text.toString().trim(), "0")
                    )
                }
                EDashboardItem.PREAUTH -> {
                    iFrReq?.onFragmentRequest(
                            UiAction.PRE_AUTH,
                            Pair(binding?.saleAmount?.text.toString().trim(), "0")
                    )
                }
                EDashboardItem.EMI_ENQUIRY -> {

                    if (TerminalParameterTable.selectFromSchemeTable()?.bankEnquiryMobNumberEntry == true) {
                        showMobileBillDialog(activity, TransactionType.EMI_ENQUIRY.type) {
                            //  sendStartSale(inputAmountEditText?.text.toString(), extraPairData)
                            iFrReq?.onFragmentRequest(
                                    UiAction.EMI_ENQUIRY,
                                    Pair(binding?.saleAmount?.text.toString().trim(), "0"), it
                            )
                        }
                    } else {
                        iFrReq?.onFragmentRequest(
                                UiAction.EMI_ENQUIRY,
                                Pair(binding?.saleAmount?.text.toString().trim(), "0")
                        )
                    }
                }
                EDashboardItem.FLEXI_PAY -> {
                    iFrReq?.onFragmentRequest(
                            UiAction.FLEXI_PAY,
                            Pair(binding?.saleAmount?.text.toString().trim(), "0")
                    )
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
        runBlocking(Dispatchers.IO) { BrandEMIDataTable.performOperation(modal) }
    }
    //endregion

    private fun validateTIP(totalTransAmount: Float, saleAmt: Float) {
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
                                )
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
                                )
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

    val tpt = TerminalParameterTable.selectFromSchemeTable()


}