package com.example.verifonevx990app.digiPOS

import android.os.Bundle
import android.text.TextUtils
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.example.customneumorphic.NeumorphCardView
import com.example.verifonevx990app.R
import com.example.verifonevx990app.databinding.FragmentUpiEnterDetailBinding
import com.example.verifonevx990app.digiPOS.mvvm.util.EnumDigiPosProcess
import com.example.verifonevx990app.digiPOS.mvvm.util.EnumDigiPosProcessingCode
import com.example.verifonevx990app.digiPOS.mvvm.util.LOG_TAG
import com.example.verifonevx990app.realmtables.EDashboardItem
import com.example.verifonevx990app.vxUtils.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.*


class UpiSmsPayEnterDetailFragment : Fragment() {
    private var binding: FragmentUpiEnterDetailBinding? = null
    private lateinit var transactionType: EDashboardItem

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentUpiEnterDetailBinding.inflate(layoutInflater, container, false)
        return binding?.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        transactionType = arguments?.getSerializable("type") as EDashboardItem
        binding?.subHeaderView?.headerImage?.visibility = View.VISIBLE
        binding?.subHeaderView?.headerImage?.setImageResource(transactionType.res)
        binding?.subHeaderView?.subHeaderText?.text = transactionType.title
        setOnClickListeners()
    }

    private fun setOnClickListeners() {
        binding?.subHeaderView?.backImageButton?.setOnClickListener {
            parentFragmentManager.popBackStackImmediate()
        }

        if (transactionType == EDashboardItem.SMS_PAY) {
            binding?.vpaCrdView?.visibility = View.GONE

        }

        binding?.amountEt?.setOnClickListener {
            showEditTextSelected(binding?.amountEt, binding?.enterAmountView, requireContext())
            val hm = hashMapOf<NeumorphCardView?, EditText?>()
            hm[binding?.vpaCrdView] = binding?.vpaEt
            hm[binding?.mobileCrdView] = binding?.mobilenoEt
            hm[binding?.descrCrdView] = binding?.descriptionEt
            showOtherEditTextUnSelected(hm, requireContext())
        }
        binding?.vpaEt?.setOnClickListener {
            showEditTextSelected(binding?.vpaEt, binding?.vpaCrdView, requireContext())
            val hm = hashMapOf<NeumorphCardView?, EditText?>()
            hm[binding?.mobileCrdView] = binding?.mobilenoEt
            hm[binding?.descrCrdView] = binding?.descriptionEt
            hm[binding?.enterAmountView] = binding?.amountEt
            showOtherEditTextUnSelected(hm, requireContext())
        }
        binding?.mobilenoEt?.setOnClickListener {
            showEditTextSelected(binding?.mobilenoEt, binding?.mobileCrdView, requireContext())
            val hm = hashMapOf<NeumorphCardView?, EditText?>()
            hm[binding?.vpaCrdView] = binding?.vpaEt
            hm[binding?.descrCrdView] = binding?.descriptionEt
            hm[binding?.enterAmountView] = binding?.amountEt
            showOtherEditTextUnSelected(hm, requireContext())
        }
        binding?.descriptionEt?.setOnClickListener {
            showEditTextSelected(binding?.descriptionEt, binding?.descrCrdView, requireContext())
            val hm = hashMapOf<NeumorphCardView?, EditText?>()
            hm[binding?.vpaCrdView] = binding?.vpaEt
            hm[binding?.mobileCrdView] = binding?.mobilenoEt
            hm[binding?.enterAmountView] = binding?.amountEt
            showOtherEditTextUnSelected(hm, requireContext())
        }
        binding?.proceedDgp?.setOnClickListener {
            validateAndSyncRequestToServer(binding?.amountEt?.text.toString())
        }
    }

    override fun onDetach() {
        super.onDetach()
        hideSoftKeyboard(requireActivity())
    }

    private fun validateAndSyncRequestToServer(amt: String) {
        if (amt == "") {
            VFService.showToast("Amount should be greater than 0 Rs")
            return
        }
        val formattedAmt = "%.2f".format(amt.toFloat())
        val amtValue = formattedAmt.toFloat()
        when {
            amtValue <= 0 -> {
                VFService.showToast("Amount should be greater than 0 Rs")
            }
/*
            TextUtils.isEmpty(binding?.mobilenoEt?.text.toString()) && transactionType == EDashboardItem.SMS_PAY  -> if (binding?.mobilenoEt?.text.toString().length in 10..13) {

               // validateTIP(trnsAmt, saleAmt, extraPairData)
            } else
                context?.getString(R.string.enter_valid_mobile_number)
                    ?.let { VFService.showToast(it) }
            */
            binding?.mobilenoEt?.text.toString().length !in 10..13 -> {
                context?.getString(R.string.enter_valid_mobile_number)
                    ?.let { VFService.showToast(it) }
            }
            TextUtils.isEmpty(binding?.vpaEt?.text.toString()) && transactionType == EDashboardItem.UPI -> {
                    VFService.showToast("Enter Valid VPA")
            }
            else -> {
                //   "5^1.08^^8287305603^064566935811302^:"
                val dNow = Date()
                val ft = SimpleDateFormat("yyMMddhhmmssMs", Locale.getDefault())
                val uniqueID: String = ft.format(dNow)
                println(uniqueID)
                var f56 = ""
                f56 = if (transactionType == EDashboardItem.UPI)
                    EnumDigiPosProcess.UPIDigiPOS.code + "^" + formattedAmt + "^" + binding?.descriptionEt?.text?.toString() + "^" + binding?.mobilenoEt?.text?.toString() + "^" + binding?.vpaEt?.text?.toString() + "^" + uniqueID
                else
                    EnumDigiPosProcess.SMS_PAYDigiPOS.code + "^" + formattedAmt + "^" + binding?.descriptionEt?.text?.toString() + "^" + binding?.mobilenoEt?.text?.toString() + "^" + uniqueID
                sendReceiveDataFromHost(f56)
            }
        }
    }

    private fun sendReceiveDataFromHost(field57: String) {
        lifecycleScope.launch(Dispatchers.IO) {
            withContext(Dispatchers.Main) {
                (activity as BaseActivity).showProgress()
            }
            getDigiPosStatus(
                field57,
                EnumDigiPosProcessingCode.DIGIPOSPROCODE.code,
                true
            ) { isSuccess, responseMsg, responsef57, fullResponse ->
                (activity as BaseActivity).hideProgress()
                try {
                    if (isSuccess) {
                        when (transactionType) {
                            EDashboardItem.UPI -> {
                                lifecycleScope.launch(Dispatchers.Main) {
                                    (activity as BaseActivity).alertBoxWithAction(
                                        null, null, getString(R.string.sms_upi_pay),
                                        getString(R.string.upi_payment_link_sent),
                                        true, getString(R.string.positive_button_yes), { status ->
                                            if (status) {
                                                logger("DigiPOS", "AGAIN HIT HERE", "e")
                                            }
                                        }, {
                                            logger("DigiPOS", "SAVE AS PENDING", "e")
                                        })
                                }

                            }
                            EDashboardItem.SMS_PAY -> {
                                lifecycleScope.launch(Dispatchers.Main) {
                                    (activity as BaseActivity).alertBoxWithAction(
                                        null, null, getString(R.string.sms_upi_pay),
                                        getString(R.string.upi_payment_link_sent),
                                        true, getString(R.string.positive_button_yes), { status ->
                                            if (status) {
                                                logger("DigiPOS", "AGAIN HIT HERE", "e")
                                            }
                                        }, {
                                            logger("DigiPOS", "SAVE AS PENDING", "e")
                                        })
                                }
                            }
                            else -> {
                                logger("DigiPOS", "Unknown trans", "e")
                            }
                        }

                        //1^Success^Success^S101^Active^Active^Active^Active^0^1
                        val responsF57List = responsef57.split("^")
                        Log.e("F56->>", responsef57)
                    } else {
                        VFService.showToast(responseMsg)
                    }
                } catch (ex: Exception) {
                    ex.printStackTrace()
                    logger(LOG_TAG.DIGIPOS.tag, "Something wrong... in response data field 57")
                }
            }
        }
    }
}