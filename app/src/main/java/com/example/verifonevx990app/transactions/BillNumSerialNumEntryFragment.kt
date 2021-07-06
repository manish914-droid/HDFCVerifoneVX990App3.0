package com.example.verifonevx990app.transactions

import android.os.Bundle
import android.text.InputFilter
import android.text.InputFilter.LengthFilter
import android.text.InputType
import android.text.TextUtils
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.example.verifonevx990app.R
import com.example.verifonevx990app.brandemi.BrandEMIDataModal
import com.example.verifonevx990app.databinding.FragmentBillNumSerialNumEntryBinding
import com.example.verifonevx990app.main.MainActivity
import com.example.verifonevx990app.realmtables.EDashboardItem
import com.example.verifonevx990app.vxUtils.UiAction
import com.example.verifonevx990app.vxUtils.VFService
import com.example.verifonevx990app.vxUtils.showEditTextSelected
import com.example.verifonevx990app.vxUtils.showEditTextUnSelected
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext


class BillNumSerialNumEntryFragment : Fragment() {

    private var binding: FragmentBillNumSerialNumEntryBinding? = null

    /* putSerializable("uiAction", uiAction)
            putString("mobileNum", mobNumber)
            putString("amt", amt)
            putBoolean("isBillRequire", isBillNumRequiredForBankEmi)
            putBoolean("isSerialNumRequired", false)*/
    val uiAction: UiAction by lazy {
        arguments?.getSerializable("uiAction") as UiAction
    }

    val mobileNumber: String by lazy {
        arguments?.getString("mobileNum") as String
    }
    val txnAmount: String by lazy {
        arguments?.getString("amt") as String
    }
    val testEmiType: String by lazy {
        arguments?.getString("testEmiType") as String
    }
    val isBillRequire: Boolean by lazy {
        arguments?.getBoolean("isBillRequire") as Boolean
    }
    val isSerialIEMIRequire: Boolean by lazy {
        arguments?.getBoolean("isSerialImeiNumRequired") as Boolean
    }
    val brandValidation: BrandEmiBillSerialMobileValidationModel by lazy {
        arguments?.getSerializable("brandValidation") as BrandEmiBillSerialMobileValidationModel
    }
    private val brandEMIDataModal1: BrandEMIDataModal by lazy {
        (arguments?.getSerializable("brandEMIDataModal") ?: BrandEMIDataModal()) as BrandEMIDataModal
    }

    private val brandEMIDataModal: BrandEMIDataModal by lazy {
        (arguments?.getSerializable("brandEMIDataModal") ?: BrandEMIDataModal() )as BrandEMIDataModal
    }

    private val transType: EDashboardItem by lazy {
        arguments?.getSerializable("transType") as EDashboardItem
    }


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        binding = FragmentBillNumSerialNumEntryBinding.inflate(inflater, container, false)
        return binding?.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding?.subHeaderView?.subHeaderText?.text = uiAction.title

        binding?.subHeaderView?.backImageButton?.setOnClickListener {
            parentFragmentManager.popBackStackImmediate()

        }

        if (isBillRequire) {
            binding?.billnoCrdView?.visibility = View.VISIBLE
        } else {
            binding?.billnoCrdView?.visibility = View.GONE
        }
        if (brandEMIDataModal.getIsRequired() == "1" || brandEMIDataModal.getIsRequired() == "2") {
            if (brandValidation.isSerialNumReq) {
                binding?.serialNumEt?.filters = arrayOf<InputFilter>(
                    InputFilter.LengthFilter(
                        brandEMIDataModal.getMaxLength()?.toInt() ?: 20
                    )
                )
                binding?.serialNumEt?.hint = "Enter serial number"
            } else if (brandValidation.isImeiNumReq) {


                binding?.serialNumEt?.filters = arrayOf<InputFilter>(InputFilter.LengthFilter(brandEMIDataModal.getMaxLength()?.toInt() ?: 16))

                binding?.serialNumEt?.hint = "Enter IMEI number"
            }
            binding?.serialNoCrdView?.visibility = View.VISIBLE
        } else {
            binding?.serialNoCrdView?.visibility = View.GONE
        }
        binding?.billNumEt?.setOnClickListener {
            showEditTextSelected(binding?.billNumEt, binding?.billnoCrdView, requireContext())
            showEditTextUnSelected(binding?.serialNumEt, binding?.serialNoCrdView, requireContext())
        }
        binding?.serialNumEt?.setOnClickListener {
            showEditTextSelected(binding?.serialNumEt, binding?.serialNoCrdView, requireContext())
            showEditTextUnSelected(binding?.billNumEt, binding?.billnoCrdView, requireContext())
        }

        when (brandEMIDataModal.getInputDataType()) {
            "0" -> {
                binding?.serialNumEt?.inputType = InputType.TYPE_CLASS_NUMBER

            }
            "1", "2" -> {
                binding?.serialNumEt?.inputType = InputType.TYPE_CLASS_TEXT

            }
        }
        binding?.proceedBtn?.setOnClickListener {
            if (uiAction == UiAction.BANK_EMI || uiAction == UiAction.TEST_EMI) {
                val pair = Pair(txnAmount, testEmiType)
                val triple = Triple(mobileNumber, binding?.billNumEt?.text.toString().trim(), true)
                (activity as MainActivity).onFragmentRequest(uiAction, pair, triple)

            } else if (uiAction == UiAction.BRAND_EMI) {
                navigateToTransaction()

            }
        }


    }

    private fun navigateToTransaction() {

        if (brandValidation.isBillNumMandatory || brandValidation.isBillNumReq) {
            if (brandValidation.isBillNumMandatory) {
                if (TextUtils.isEmpty(binding?.billNumEt?.text.toString().trim())) {
                    context?.getString(R.string.enter_valid_bill_number)?.let { it1 ->
                        VFService.showToast(it1)
                    }
                    return
                }
            }
        }
        if (brandEMIDataModal.getIsRequired() == "1" || brandEMIDataModal.getIsRequired() == "2") {
            if (brandEMIDataModal.getIsRequired() == "1") {
                if (TextUtils.isEmpty(binding?.serialNumEt?.text.toString().trim())) {
                    context?.getString(R.string.enterValid_serial_iemei_no)?.let { it1 ->
                        VFService.showToast(it1)
                    }
                    return
                }
            }

        }

        lifecycleScope.launch(Dispatchers.IO) {
            saveBrandEMIDataToDB( binding?.billNumEt?.text.toString().trim(),  binding?.billNumEt?.text.toString().trim(), brandEMIDataModal, transType)
            withContext(Dispatchers.Main) {
                (activity as MainActivity).onFragmentRequest(
                    uiAction,
                    Pair(txnAmount, "0"),
                    Triple(mobileNumber, binding?.billNumEt?.text.toString().trim(), true)
                )
            }
        }

    }


}