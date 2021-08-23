package com.example.verifonevx990app.crosssell

import android.app.Dialog
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.os.CountDownTimer
import android.text.TextUtils
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.view.animation.Animation
import android.view.animation.AnimationUtils
import android.widget.*
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.example.customneumorphic.NeumorphCardView
import com.example.verifonevx990app.R
import com.example.verifonevx990app.databinding.EnterOtpDialogBinding
import com.example.verifonevx990app.databinding.FragmentInitiateCrossSellProcessBinding
import com.example.verifonevx990app.main.MainActivity

import com.example.verifonevx990app.realmtables.EDashboardItem
import com.example.verifonevx990app.utils.KeyboardModel
import com.example.verifonevx990app.utils.printerUtils.PrintUtil
import com.example.verifonevx990app.vxUtils.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit


class InitiateCrossSellProcessFragment : Fragment(R.layout.fragment_initiate_cross_sell_process) {

    private val fragTitle: String by lazy {
        arguments?.getString(MainActivity.CROSS_SELL_PROCESS_TYPE_HEADING) ?: ""
    }
    private val CROSS_SELL_OPTIONS: Int by lazy {
        arguments?.getInt(MainActivity.CROSS_SELL_OPTIONS) ?: -1947
    }
    private val CROSS_SELL_REQUEST_TYPE: Int by lazy {
        arguments?.getInt(MainActivity.CROSS_SELL_REQUEST_TYPE) ?: -1947
    }
    private var crossSellMobileCardView: ConstraintLayout? = null
    private var reportLL: LinearLayout? = null
    private var lastFourCardDigitTIL: NeumorphCardView? = null
    private var mobileNoTIL: BHTextInputLayout? = null

    private var mCountDown: CountDownTimer? = null
    private var otpExpireTime: Long = 120000
    private var field57Data: String? = null
    private var transactionReferenceNumber: String? = null
    private var animShow: Animation? = null
    private var animHide: Animation? = null
    var dataCounter = 0
    var hasMoreData = false
    var downloadedReportDataList = arrayListOf<ReportDownloadedModel>()
    private val keyModelMobNumber: KeyboardModel by lazy {
        KeyboardModel()
    }
    var inputInMobilenumber = false
    private var binding: FragmentInitiateCrossSellProcessBinding? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentInitiateCrossSellProcessBinding.inflate(inflater, container, false)
        return binding?.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding?.subHeaderView?.backImageButton?.setOnClickListener { parentFragmentManager.popBackStackImmediate() }
        binding?.subHeaderView?.subHeaderText?.text = fragTitle
        crossSellMobileCardView = view.findViewById(R.id.crossSellMobileCardView)
        reportLL = view.findViewById(R.id.reportLL)
        lastFourCardDigitTIL = view.findViewById(R.id.lastFourCardDigitET_crd_view)
        mobileNoTIL = view.findViewById(R.id.mobileNoTIL)
        Log.d("RequestID:- ", CROSS_SELL_REQUEST_TYPE.toString())
        Log.d("OptionID:- ", CROSS_SELL_OPTIONS.toString())

        //Hide/Show Views on condition based:-
        when (CROSS_SELL_OPTIONS) {
            CrossSellOptions.HDFC_CREDIT_CARD.code -> {
                crossSellMobileCardView?.visibility = View.VISIBLE
                lastFourCardDigitTIL?.visibility = View.GONE
                binding?.subHeaderView?.headerImage?.setImageResource(CrossSellOptions.HDFC_CREDIT_CARD.res)
            }
            CrossSellOptions.INSTA_LOAN.code -> {
                crossSellMobileCardView?.visibility = View.VISIBLE
                lastFourCardDigitTIL?.visibility = View.VISIBLE
                binding?.subHeaderView?.headerImage?.setImageResource(CrossSellOptions.INSTA_LOAN.res)
            }
            CrossSellOptions.JUMBO_LOAN.code -> {
                crossSellMobileCardView?.visibility = View.VISIBLE
                lastFourCardDigitTIL?.visibility = View.VISIBLE
                binding?.subHeaderView?.headerImage?.setImageResource(CrossSellOptions.JUMBO_LOAN.res)
            }
            CrossSellOptions.CREDIT_LIMIT_INCREASE.code -> {
                crossSellMobileCardView?.visibility = View.VISIBLE
                lastFourCardDigitTIL?.visibility = View.VISIBLE
                binding?.subHeaderView?.headerImage?.setImageResource(CrossSellOptions.CREDIT_LIMIT_INCREASE.res)
            }
            CrossSellOptions.REPORTS.code -> {
                reportLL?.visibility = View.VISIBLE
                crossSellMobileCardView?.visibility = View.GONE
                binding?.subHeaderView?.headerImage?.setImageResource(CrossSellOptions.REPORTS.res)
            }
        }

        // 2= last month
        // 1= current month
        setOnClickListeners()
    }

    private fun hdfcCreditCardValidation(): Boolean {
        return binding?.mobileNoET?.text?.length == 10 || binding?.mobileNoET?.text?.length == 13


    }
    private fun setOnClickListeners() {

        binding?.reportLastMonthBTN?.setOnClickListener { sendMailPrintChooserDialog("2") }
        binding?.reportCurrentMonthBTN?.setOnClickListener { sendMailPrintChooserDialog("1") }
        //mobile number editText
        binding?.mobileNoET?.setOnClickListener {
            showEditTextUnSelectedWithFocus(binding?.mobileNoET, binding?.mobileNoETCrdView, requireContext())
            val hm = hashMapOf<NeumorphCardView?, EditText?>()
            hm[binding?.lastFourCardDigitETCrdView] = binding?.lastFourCardDigitET

            showOtherEditTextUnSelected(hm, requireContext())
            keyModelMobNumber.view = it
            keyModelMobNumber.callback = ::onOKClicked
            keyModelMobNumber.isInutSimpleDigit = true
            inputInMobilenumber = true

        }
        binding?.lastFourCardDigitET?.setOnClickListener {
            showEditTextUnSelectedWithFocus(binding?.lastFourCardDigitET, binding?.lastFourCardDigitETCrdView, requireContext())
            val hm = hashMapOf<NeumorphCardView?, EditText?>()
            hm[binding?.mobileNoETCrdView] = binding?.mobileNoET
            showOtherEditTextUnSelected(hm, requireContext())
            keyModelMobNumber.view = it
            keyModelMobNumber.callback = ::onOKClicked
            keyModelMobNumber.isInutSimpleDigit = true
            inputInMobilenumber = true

        }
        onSetKeyBoardButtonClick()
        initAnimation()
        binding?.mainKeyBoard?.root?.visibility = View.VISIBLE
        binding?.mainKeyBoard?.root?.startAnimation(animShow)
    }
    //Below method is used to show enter OTP dialog:-
    private fun enterOTPDialog() {
        val dialog = androidx.appcompat.app.AlertDialog.Builder(requireActivity()).create()
        dialog.setCancelable(false)
        val bindingg = EnterOtpDialogBinding.inflate(LayoutInflater.from(activity))
        dialog.setView(bindingg.root)
        mCountDown = getCountDownTimer(otpExpireTime, bindingg, dialog)
        mCountDown?.start()
        dialog.window?.attributes?.windowAnimations = R.style.DialogAnimation
        val window = dialog.window
        window?.setLayout(
            ViewGroup.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.WRAP_CONTENT
        )

        val otpET = bindingg.otpET
        val closeDialogIMG = bindingg.closeDialogIMG
        val otpSubmitBTN = bindingg.otpSubmitBTN

        closeDialogIMG.setOnClickListener {
            mCountDown?.cancel()
            mCountDown = null
            dialog.dismiss()
            startActivity(Intent(activity, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_CLEAR_TASK
            })
        }

        otpET.addTextChangedListener(OnTextChange {
            otpSubmitBTN.isEnabled = it.length == 4 || it.length == 6
        })


        dialog.show()
    }


    //Below method is used to show enter OTP dialog:-
    private fun sendMailPrintChooserDialog(whichMonth: String) {
        var selectedOption = 1
        val dialog = activity?.let { Dialog(it) }
        dialog?.setCancelable(false)
        dialog?.setContentView(R.layout.print_mail_chooser_view)

        dialog?.window?.attributes?.windowAnimations = R.style.DialogAnimation
        val window = dialog?.window
        window?.setLayout(
            ViewGroup.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.WRAP_CONTENT
        )
        val radioGroup = dialog?.findViewById<RadioGroup>(R.id.rg)
        val sentToEmail = dialog?.findViewById<RadioButton>(R.id.sentToEmail)
        val printOnPos = dialog?.findViewById<RadioButton>(R.id.printOnPos)
        val closeDialogChooserIMG = dialog?.findViewById<ImageView>(R.id.closeDialogChooserIMG)
        val otpSubmitBTN = dialog?.findViewById<Button>(R.id.otpSubmitBTN)

        closeDialogChooserIMG?.setOnClickListener { dialog.dismiss() }

        radioGroup?.setOnCheckedChangeListener { group, checkedId ->
            // This will get the radiobutton that has changed in its check state
            val checkedRadioButton =
                group.findViewById<View>(checkedId) as RadioButton

            when (checkedRadioButton) {
                sentToEmail -> {
                    selectedOption = 1
                }
                printOnPos -> {
                    selectedOption = 2
                }
            }
        }

        otpSubmitBTN?.setOnClickListener {
            //Send report to mail
            if (selectedOption == 1) {
                field57Data = "${CROSS_SELL_REQUEST_TYPE}~${whichMonth}"
                (activity as MainActivity).showProgress()
                SyncCrossSellToHost(field57Data ?: "") { mailReportCB, responseMsg, _ ->
                    GlobalScope.launch(Dispatchers.Main) {
                        (activity as MainActivity).hideProgress()
                        if (mailReportCB) {
                            dialog.dismiss()
                            (activity as MainActivity).alertBoxWithAction(
                                null,
                                null,
                                getString(R.string.success_message),
                                getString(R.string.success_transaction),
                                false,
                                getString(R.string.positive_button_ok),
                                { alertPositiveCallback ->
                                    if (alertPositiveCallback)
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
                            dialog.dismiss()
                            (activity as MainActivity).alertBoxWithAction(null,
                                null,
                                getString(R.string.reports),
                                responseMsg,
                                false,
                                getString(R.string.positive_button_ok),
                                { alertPositiveCallback ->
                                    if (alertPositiveCallback)
                                        navigateToDashboard()
                                },
                                {})
                        }
                    }
                }

            }

            // Download report on POS
            if (selectedOption == 2) {
                if (dialog != null) {
                    downloadReportOnPos(whichMonth, dialog)
                }
            }
        }
        window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        dialog?.show()

    }

    private fun downloadReportOnPos(whichMonth: String, dialog: Dialog) {
        field57Data = "${CROSS_SELL_REQUEST_TYPE}~${whichMonth}~${dataCounter}"
        (activity as MainActivity).showProgress()
        SyncCrossSellToHost(field57Data ?: "") { _, responseMsg, _ ->
            GlobalScope.launch(Dispatchers.Main) {
                (activity as MainActivity).hideProgress()
                val reportData =
                    "0|5|20200824173312289769~3~8287305603~24-08-2020~1|~9~9555274574~24-08-2020~2|~5~~~1|~9~9555274574~24-08-2020~1|~9~9555274574~24-08-2020~2|~9~9555274574~24-08-2020~1|~9~9555274574~24-08-2020~1|"
                val mailReportCB = true
                if (mailReportCB) {
                    //   dialog.dismiss()
                    val moreData = reportData.substring(0, 1)
                    val counterToUpdate = reportData.substring(2, 3)
                    val totalReportRecord = reportData.substring(4)
                    hasMoreData = moreData != "0"
                    dataCounter += counterToUpdate.toInt()
                    val totalRecordinResponse = totalReportRecord.split("|")

                    for (record in totalRecordinResponse) {
                        if (record != "") {
                            val recordValues = record.split("~")
                            //  for (r in recordValues) {
                            val singleRecord = ReportDownloadedModel()
                            if (record.first() == '~') {
                                singleRecord.transactionRefNo = ""
                            } else {
                                singleRecord.transactionRefNo = recordValues[0]
                            }
                            if (record.last() == '~') {
                                singleRecord.requestStatus = ""
                            } else {
                                singleRecord.requestStatus = recordValues[4]
                            }
                            singleRecord.requestTypeId = recordValues[1]
                            singleRecord.mobile = recordValues[2]
                            singleRecord.requestDate = recordValues[3]
                            downloadedReportDataList.add(singleRecord)
                            //   }

                        }

                    }

                    if (hasMoreData) {
                        downloadReportOnPos(whichMonth, dialog)
                    } else {
                        dialog.dismiss()
                        (activity as MainActivity).alertBoxWithAction(null,
                            null,
                            getString(R.string.reports),
                            responseMsg,
                            false,
                            getString(R.string.positive_button_ok),
                            { alertPositiveCallback ->
                                if (alertPositiveCallback)
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
                        PrintUtil(activity).printCrossSellReport(downloadedReportDataList)
                        //Printing Receipt

                    }
                } else {
                    dialog.dismiss()
                    (activity as MainActivity).alertBoxWithAction(null,
                        null,
                        getString(R.string.failed),
                        getString(R.string.transaction_failed_msg),
                        false,
                        getString(R.string.positive_button_ok),
                        { alertPositiveCallback ->
                            if (alertPositiveCallback)
                                navigateToDashboard()
                        },
                        {})
                }
            }
        }
    }

    private fun getCountDownTimer(
        secondsInLong: Long, dialogBinding: EnterOtpDialogBinding,
        dialog: Dialog
    ): CountDownTimer {
        return object : CountDownTimer(secondsInLong, 1000) {
            val expTime = dialogBinding.expireTime
            override fun onTick(millisUntilFinished: Long) {
                if (millisUntilFinished < 20000) {

                    expTime.setTextColor(Color.parseColor("#FF0000"))
                }
                val text = "" + String.format(
                    "%02d : %02d",
                    TimeUnit.MILLISECONDS.toMinutes(millisUntilFinished),
                    TimeUnit.MILLISECONDS.toSeconds(millisUntilFinished) -
                            TimeUnit.MINUTES.toSeconds(
                                TimeUnit.MILLISECONDS.toMinutes(
                                    millisUntilFinished
                                )
                            )
                )
                expTime.text = text
            }

            override fun onFinish() {
                // resendLL.visibility = View.VISIBLE
                expTime.visibility = View.GONE
                expTime.setTextColor(Color.parseColor("#3a61d3"))
                dialog.dismiss()
                GlobalScope.launch(Dispatchers.Main) {
                    //  VFService.showToast(transMsg)
                    try {
                        VFService.vfBeeper?.startBeep(200)
                        (activity as MainActivity).alertBoxWithAction(null,
                            null,
                            getString(R.string.otp_time_out),
                            getString(R.string.transaction_delined_msg),
                            false,
                            getString(R.string.positive_button_ok),
                            { alertPositiveCallback ->
                                if (alertPositiveCallback)
                                    navigateToDashboard()
                            },
                            {})
                    } catch (ex: Exception) {
                        ex.printStackTrace()
                        VFService.showToast(getString(R.string.otp_time_out))
                        startActivity(Intent(activity, MainActivity::class.java).apply {
                            flags =
                                Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_CLEAR_TASK
                        })
                    }
                }
            }
        }
    }

    //Below method is used to navigate current fragment to DashBoardFragment:-
    fun navigateToDashboard() {
        startActivity(Intent(activity, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_CLEAR_TASK
        })
    }
    private fun initAnimation() {
        animShow = AnimationUtils.loadAnimation(activity, R.anim.view_show)
        animHide = AnimationUtils.loadAnimation(activity, R.anim.view_hide)
    }
    private fun onOKClicked(mobile: String) {
        when (CROSS_SELL_OPTIONS) {
            CrossSellOptions.HDFC_CREDIT_CARD.code -> {
                if (hdfcCreditCardValidation()) {
                    field57Data =
                        "$CROSS_SELL_REQUEST_TYPE~~${binding?.mobileNoET?.text?.trim()}~~~"
                    lifecycleScope.launch(Dispatchers.Main) {
                        (activity as MainActivity).showProgress()
                    }
                    SyncCrossSellToHost(
                        field57Data ?: ""
                    ) { crossSellCB, responseMsg, reportData ->
                        GlobalScope.launch(Dispatchers.Main) {
                            (activity as MainActivity).hideProgress()
                            if (!TextUtils.isEmpty(reportData)) {
                                val data = reportData.split("~")
                                transactionReferenceNumber = data[3]
                            }
                            if (crossSellCB) {
                                VFService.showToast(responseMsg)
                                enterOTPDialog()
                            } else
                                VFService.showToast(responseMsg)
                        }
                    }
                } else
                    VFService.showToast(getString(R.string.enter_valid_mobile_number))
            }

            else -> {
                when {
                    binding?.lastFourCardDigitET?.text?.length != 4 -> VFService.showToast(
                        getString(R.string.enter_valid_last_four_digit_of_card)
                    )
                    binding?.mobileNoET?.text?.length !in 10..13 -> VFService.showToast(
                        getString(R.string.enter_valid_mobile_number)
                    )
                    else -> {
                        field57Data =
                            "$CROSS_SELL_REQUEST_TYPE~${binding?.lastFourCardDigitET?.text?.trim()}~${binding?.mobileNoET?.text?.trim()}~~~"
                        (activity as MainActivity).showProgress()
                        SyncCrossSellToHost(field57Data.toString()) { crossSellCB, responseMsg, reportData ->
                            GlobalScope.launch(Dispatchers.Main) {
                                (activity as MainActivity).hideProgress()
                                if (!TextUtils.isEmpty(reportData)) {
                                    val data = reportData.split("~")
                                    transactionReferenceNumber = data[3]
                                }
                                if (crossSellCB) {
                                    VFService.showToast(responseMsg)
                                    enterOTPDialog()
                                } else
                                    VFService.showToast(responseMsg)
                            }
                        }
                    }
                }
            }
        }
    }

    private fun onSetKeyBoardButtonClick() {
        binding?.mainKeyBoard?.key0?.setOnClickListener {
            when {

                inputInMobilenumber -> {
                    keyModelMobNumber.onKeyClicked("0")
                }

            }
        }
        binding?.mainKeyBoard?.key00?.setOnClickListener {
            when {

                inputInMobilenumber -> {
                    keyModelMobNumber.onKeyClicked("00")
                }

            }
        }
        binding?.mainKeyBoard?.key000?.setOnClickListener {
            when {

                inputInMobilenumber -> {
                    keyModelMobNumber.onKeyClicked("000")
                }

            }
        }
        binding?.mainKeyBoard?.key1?.setOnClickListener {
            when {

                inputInMobilenumber -> {
                    keyModelMobNumber.onKeyClicked("1")
                }

            }
        }
        binding?.mainKeyBoard?.key2?.setOnClickListener {
            when {

                inputInMobilenumber -> {
                    keyModelMobNumber.onKeyClicked("2")
                }

            }
        }
        binding?.mainKeyBoard?.key3?.setOnClickListener {
            when {

                inputInMobilenumber -> {
                    keyModelMobNumber.onKeyClicked("3")
                }

            }
        }
        binding?.mainKeyBoard?.key4?.setOnClickListener {
            when {

                inputInMobilenumber -> {
                    keyModelMobNumber.onKeyClicked("4")
                }

            }
        }
        binding?.mainKeyBoard?.key5?.setOnClickListener {
            when {

                inputInMobilenumber -> {
                    keyModelMobNumber.onKeyClicked("5")
                }

            }
        }
        binding?.mainKeyBoard?.key6?.setOnClickListener {
            when {

                inputInMobilenumber -> {
                    keyModelMobNumber.onKeyClicked("6")
                }

            }
        }
        binding?.mainKeyBoard?.key7?.setOnClickListener {
            when {

                inputInMobilenumber -> {
                    keyModelMobNumber.onKeyClicked("7")
                }

            }
        }
        binding?.mainKeyBoard?.key8?.setOnClickListener {
            when {

                inputInMobilenumber -> {
                    keyModelMobNumber.onKeyClicked("8")
                }

            }
        }
        binding?.mainKeyBoard?.key9?.setOnClickListener {
            when {

                inputInMobilenumber -> {
                    keyModelMobNumber.onKeyClicked("9")
                }

            }
        }
        binding?.mainKeyBoard?.keyClr?.setOnClickListener {
            when {

                inputInMobilenumber -> {
                    keyModelMobNumber.onKeyClicked("c")
                }

            }
        }
        binding?.mainKeyBoard?.keyDelete?.setOnClickListener {
            when {

                inputInMobilenumber -> {
                    keyModelMobNumber.onKeyClicked("d")
                }

            }
        }
        binding?.mainKeyBoard?.keyOK?.setOnClickListener {
            when {

                inputInMobilenumber -> {
                    keyModelMobNumber.onKeyClicked("o")
                }

            }
        }

    }
}

class ReportDownloadedModel {
    var transactionRefNo = ""
    var requestTypeId = ""
    var mobile = ""
    var requestDate = ""
    var requestStatus = ""

}

class TotalCrossellRep {
    var reqType = ""
    var reqStatus = ""
}


class CrossSellReportWithType {
    var type = ""
    var typeName = ""


}
