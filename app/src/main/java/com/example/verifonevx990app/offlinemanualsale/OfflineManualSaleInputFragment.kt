package com.example.verifonevx990app.offlinemanualsale

import android.content.Intent
import android.os.Bundle
import android.text.TextUtils
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.fragment.app.Fragment
import com.example.verifonevx990app.R
import com.example.verifonevx990app.databinding.FragmentOfflineManualSaleBinding
import com.example.verifonevx990app.main.MainActivity
import com.example.verifonevx990app.main.PrefConstant
import com.example.verifonevx990app.realmtables.CardDataTable
import com.example.verifonevx990app.realmtables.IssuerParameterTable
import com.example.verifonevx990app.realmtables.TerminalParameterTable
import com.example.verifonevx990app.utils.printerUtils.EPrintCopyType
import com.example.verifonevx990app.vxUtils.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import java.util.*

class OfflineManualSaleInputFragment : Fragment() {
    private val title: String by lazy { arguments?.getString(MainActivity.INPUT_SUB_HEADING) ?: "" }
    private var backImageButton: ImageView? = null
    private var expiryDateET: BHTextInputEditText? = null
    private var approvalCodeET: BHTextInputEditText? = null
    private var cardNumberET: BHTextInputEditText? = null
    private var amountET: AmountEditText? = null
    private var offlineSaleSaveBT: BHButton? = null
    private var terminalParameterTable: TerminalParameterTable? = null
    private var cardDataTable: CardDataTable? = null
    private val calender: Calendar by lazy { Calendar.getInstance() }
    private var offlineTransLimit: Double? = 0.0
    private var binding: FragmentOfflineManualSaleBinding? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentOfflineManualSaleBinding.inflate(layoutInflater, container, false)
        return binding?.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding?.subHeaderView?.subHeaderText?.text = title
        logger("ConnectionAddress:- ", VFService.getIpPort().toString(), "d")
        terminalParameterTable = TerminalParameterTable.selectFromSchemeTable()
        cardDataTable = CardDataTable.selectFirstCardTableData()
        offlineTransLimit =
            IssuerParameterTable.selectFromIssuerParameterTable(AppPreference.WALLET_ISSUER_ID)?.transactionAmountLimit?.toDouble()
                ?.div(100)


        amountET = view.findViewById(R.id.amountET)
        cardNumberET = view.findViewById(R.id.cardNumberET)
        approvalCodeET = view.findViewById(R.id.approvalCodeET)
        backImageButton = view.findViewById(R.id.back_image_button)
        backImageButton?.setOnClickListener { fragmentManager?.popBackStackImmediate() }

        //Expiry Date viewBind & TextWatcher to Format Expiry Date i.e, 03/22 etc:-
        expiryDateET = view.findViewById(R.id.expiryDateET)
        expiryDateET?.isFocusable = false
        expiryDateET?.setOnClickListener {
            activity?.let { it1 ->
                openDatePicker(
                    view.findViewById(R.id.expiryDateET),
                    it1
                )
            }
        }

        //Offline Sale Save Button viewBind & onClick:-
        offlineSaleSaveBT = view.findViewById(R.id.offlineSaleSaveBT)
        offlineSaleSaveBT?.setOnClickListener {
            when {
                amountET?.text.toString() == "0.00" -> VFService.showToast(getString(R.string.amount_should_not_be_empty))

                //Offline Check
                amountET?.text.toString()
                    .toDouble() > offlineTransLimit ?: 0.0 -> VFService.showToast(getString(R.string.transaction_limit_exceeds))


                TextUtils.isEmpty(cardNumberET?.text.toString()) -> VFService.showToast(getString(R.string.card_number_should_not_be_empty))
                !validateCardMinMaxLength(cardNumberET?.text.toString()) -> VFService.showToast(
                    getString(R.string.invalid_card_length)
                )
                !validateCardPanLowAndHighRange(cardNumberET?.text.toString()) -> VFService.showToast(
                    getString(R.string.invalid_card_number_range)
                )
                !cardLuhnCheck(cardNumberET?.text.toString()) -> activity?.getString(R.string.card_number_not_valid_as_per_luhn_check)
                    ?.let { it1 ->
                        VFService.showToast(it1)
                    }
                TextUtils.isEmpty(expiryDateET?.text.toString()) -> VFService.showToast(getString(R.string.expiry_date_is_not_valid))
                TextUtils.isEmpty(approvalCodeET?.text.toString()) -> VFService.showToast(
                    getString(R.string.approval_code_should_not_be_empty)
                )
                approvalCodeET?.text.toString().length < 6 -> VFService.showToast(getString(R.string.approval_code_must_be_a_valid_6digit_number))
                else -> {
                    offlineSaleSaveBT?.isEnabled = false
                    (activity as MainActivity).showProgress(getString(R.string.offline_sale))
                    offlineSaleSaveBT?.isEnabled = true
                    saveOfflineSaleData()
                }
            }
        }
    }


    //Below method is used to validate card number length from Terminal Parameter Table:-
    private fun validateCardMinMaxLength(cardNumber: String): Boolean {
        return (!TextUtils.isEmpty(cardNumber) && cardNumber.length.toString() >= terminalParameterTable?.minOfflineSalePanLen.toString()
                && cardNumber.length.toString() <= terminalParameterTable?.maxOfflineSalePanLen.toString())
    }

    //Below method is used to validate card number range from CardDataTable between PanLow & PanHigh:-
    private fun validateCardPanLowAndHighRange(cardNumber: String): Boolean {
        return cardNumber >= cardDataTable?.panLow.toString() && cardNumber <= cardDataTable?.panHi.toString()
    }

    //Below method is used to save offline sale data in batch table:-
    private fun saveOfflineSaleData() {
        val printer = VFService.vfPrinter
        if (printer?.status != 0) {
            (activity as MainActivity).hideProgress()
            (activity as MainActivity).alertBoxWithAction(null,
                null,
                getString(R.string.printing_roll_error),
                getString(R.string.offline_sale_not_be_proceed_without_printing_roll),
                false,
                getString(R.string.positive_button_ok),
                {

                },
                {})
        } else {
            val track2Data = getEncryptedField57DataForOfflineSale(
                cardNumberET?.text.toString().trim(),
                expiryDateET?.text.toString().substring(0, 2) + expiryDateET?.text.toString()
                    .substring(3, 5),
                approvalCodeET?.text.toString().trim()
            )

            logger("OfflineTrack2Data", track2Data, "d")
            val terminalData = TerminalParameterTable.selectFromSchemeTable()
            StubOfflineBatchData(
                amountET?.text.toString().replace(".", "").toLong(),
                track2Data,
                cardNumberET?.text.toString().trim(),
                approvalCodeET?.text.toString().trim()
            ) { batchData ->
                //Save Server Hit Status in Preference , To Restrict Init and KeyExchange from Terminal:-
                AppPreference.saveBoolean(PrefConstant.SERVER_HIT_STATUS.keyName.toString(), true)
                activity?.let {
                    OfflineSalePrintReceipt().offlineSalePrint(
                        batchData,
                        EPrintCopyType.MERCHANT,
                        it
                    ) { printCB, printingFail ->
                        GlobalScope.launch(Dispatchers.Main) {
                            (activity as MainActivity).hideProgress()
                            if (!printCB) {
                                TerminalParameterTable.updateTerminalDataInvoiceNumber(terminalData?.invoiceNumber.toString())
                                //Below we are incrementing previous ROC (Because ROC will always be incremented whenever Server Hit is performed:-
                                ROCProviderV2.incrementFromResponse(
                                    ROCProviderV2.getRoc(
                                        AppPreference.getBankCode()
                                    ).toString(), AppPreference.getBankCode()
                                )
                                if (printingFail == 0) {
                                    (activity as MainActivity).alertBoxWithAction(null,
                                        null,
                                        getString(R.string.printer_error),
                                        getString(R.string.printing_roll_empty_msg),
                                        false,
                                        getString(R.string.positive_button_ok),
                                        {
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
                                } else {
                                    startActivity(Intent(activity, MainActivity::class.java).apply {
                                        flags =
                                            Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_CLEAR_TASK
                                    })
                                }
                            } else
                                VFService.showToast(getString(R.string.something_went_wrong))
                        }
                    }
                }
            }
        }
    }
}