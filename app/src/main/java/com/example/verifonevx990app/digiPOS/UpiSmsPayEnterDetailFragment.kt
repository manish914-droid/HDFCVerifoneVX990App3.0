package com.example.verifonevx990app.digiPOS

import android.content.Context
import android.os.Bundle
import android.text.TextUtils
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.EditText
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.example.customneumorphic.NeumorphCardView
import com.example.verifonevx990app.R
import com.example.verifonevx990app.databinding.FragmentUpiEnterDetailBinding
import com.example.verifonevx990app.realmtables.DigiPosDataTable
import com.example.verifonevx990app.realmtables.EDashboardItem
import com.example.verifonevx990app.utils.printerUtils.EPrintCopyType
import com.example.verifonevx990app.utils.printerUtils.PrintUtil
import com.example.verifonevx990app.vxUtils.*
import com.google.gson.Gson
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
        activity?.window?.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE)
        when (transactionType) {
            EDashboardItem.SMS_PAY -> {
                binding?.mobilenoEt?.hint=getString(R.string.enter_mobile_number)
            }
            EDashboardItem.UPI -> {
                binding?.mobilenoEt?.hint=getString(R.string.enter_mobile_number_optional)

            }
            else -> {

            }
        }
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
            transactionType == EDashboardItem.UPI && !TextUtils.isEmpty(binding?.mobilenoEt?.text.toString())   ->  context?.getString(R.string.enter_valid_mobile_number)
                ?.let { VFService.showToast(it) }

            transactionType == EDashboardItem.SMS_PAY &&  binding?.mobilenoEt?.text.toString().length !in 10..13 -> {
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
                                    val respDataList = responsef57.split("^")
                                    if (respDataList[4] == EnumDigiPosTerminalStatusCode.TerminalStatusCodeS101.code) {
                                        (activity as BaseActivity).alertBoxWithAction(
                                            null,
                                            null,
                                            getString(R.string.sms_upi_pay),
                                            getString(R.string.upi_payment_link_sent),
                                            true,
                                            getString(R.string.positive_button_yes),
                                            { status ->
                                                if (status) {
                                                    lifecycleScope.launch(Dispatchers.IO) {
                                                        withContext(Dispatchers.Main) {
                                                            (activity as BaseActivity).showProgress()
                                                        }

                                                        val req57 = EnumDigiPosProcess.GET_STATUS.code + "^" + respDataList[1] + "^"

                                                        getDigiPosStatus(req57, EnumDigiPosProcessingCode.DIGIPOSPROCODE.code, false) { isSuccess, responseMsg, responsef57, fullResponse ->
                                                            try {
                                                                (activity as BaseActivity).hideProgress()
                                                                if (isSuccess) {
                                                                    val statusRespDataList = responsef57.split("^")


                                                                    val tabledata = DigiPosDataTable()
                                                                    tabledata.requestType = statusRespDataList[0].toInt()
                                                                    //  tabledata.partnerTxnId = statusRespDataList[1]
                                                                    tabledata.status = statusRespDataList[1]
                                                                    tabledata.statusMsg = statusRespDataList[2]
                                                                    tabledata.statusCode = statusRespDataList[3]
                                                                    tabledata.mTxnId = statusRespDataList[4]
                                                                    tabledata.partnerTxnId = statusRespDataList[6]
                                                                    tabledata.transactionTimeStamp = statusRespDataList[7]
                                                                    val dateTime = statusRespDataList[7].split(" ")
                                                                    tabledata.txnDate = dateTime[0]
                                                                    tabledata.txnTime = dateTime[1]
                                                                    tabledata.amount = statusRespDataList[8]
                                                                    tabledata.paymentMode = statusRespDataList[9]
                                                                    tabledata.customerMobileNumber = statusRespDataList[10]
                                                                    tabledata.description = statusRespDataList[11]
                                                                    tabledata.pgwTxnId = statusRespDataList[12]
                                                                    when (statusRespDataList[5]) {
                                                                        EDigiPosPaymentStatus.Pending.desciption -> {
                                                                            tabledata.txnStatus =
                                                                                EDigiPosPaymentStatus.Pending.code
                                                                        }
                                                                        EDigiPosPaymentStatus.Failed.desciption -> {
                                                                            tabledata.txnStatus =
                                                                                EDigiPosPaymentStatus.Failed.code
                                                                        }
                                                                        EDigiPosPaymentStatus.Approved.desciption -> {
                                                                            tabledata.txnStatus =
                                                                                EDigiPosPaymentStatus.Approved.code
                                                                            txnSuccessToast(activity as Context)
                                                                            PrintUtil(context).printSMSUPIChagreSlip(
                                                                                tabledata,
                                                                                EPrintCopyType.MERCHANT,
                                                                                context
                                                                            ) { alertCB, printingFail ->
                                                                                //context.hideProgress()
                                                                                if (!alertCB) {
                                                                                    parentFragmentManager.popBackStack()

                                                                                }
                                                                            }
                                                                        }
                                                                    }
                                                                    DigiPosDataTable.insertOrUpdateDigiposData(tabledata)
                                                                    val dp = DigiPosDataTable.selectAllDigiPosData()
                                                                    val dpObj = Gson().toJson(dp)
                                                                    logger(LOG_TAG.DIGIPOS.tag, "--->      $dpObj ")
                                                                    Log.e("F56->>", responsef57)
                                                                } else {
                                                                    // todo for not success

                                                                }

                                                            } catch (ex: java.lang.Exception) {
                                                                ex.printStackTrace()
                                                                logger(
                                                                    LOG_TAG.DIGIPOS.tag,
                                                                    "Somethig wrong... in response data field 57"
                                                                )
                                                            }
                                                        }
                                                    }
                                                }
                                            },
                                            {
                                                val tabledata = DigiPosDataTable()
                                                val reqList = field57.split("^")
                                                tabledata.amount = reqList[1]
                                                tabledata.description = reqList[2]
                                                tabledata.customerMobileNumber = reqList[3]
                                                val respDataList = responsef57.split("^")
                                                tabledata.requestType = respDataList[0].toInt()
                                                tabledata.partnerTxnId = respDataList[1]
                                                tabledata.status = respDataList[2]
                                                tabledata.statusMsg = respDataList[3]
                                                tabledata.statusCode = respDataList[4]
                                                tabledata.mTxnId = respDataList[5]
                                                val txnstatus = respDataList[6]
                                                when (txnstatus) {
                                                    EDigiPosPaymentStatus.Pending.desciption -> {
                                                        tabledata.txnStatus =
                                                            EDigiPosPaymentStatus.Pending.code
                                                    }
                                                    EDigiPosPaymentStatus.Failed.desciption -> {
                                                        tabledata.txnStatus =
                                                            EDigiPosPaymentStatus.Failed.code
                                                    }
                                                    EDigiPosPaymentStatus.Approved.desciption -> {
                                                        tabledata.txnStatus =
                                                            EDigiPosPaymentStatus.Approved.code
                                                    }
                                                }
                                                DigiPosDataTable.insertOrUpdateDigiposData(tabledata)
                                                val dp = DigiPosDataTable.selectAllDigiPosData()
                                                val dpObj = Gson().toJson(dp)
                                                logger(LOG_TAG.DIGIPOS.tag, "--->      $dpObj ")
                                            })
                                    }else{
                                        // todo received other than S101

                                    }
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

                    }
                 catch (ex: Exception) {
                    ex.printStackTrace()
                    logger(LOG_TAG.DIGIPOS.tag, "Something wrong... in response data field 57")
                }
            }
        }
    }
}