package com.example.verifonevx990app.merchantPromo

import android.app.Activity
import android.app.Dialog
import android.content.Context
import android.graphics.Color
import android.os.Bundle
import android.os.CountDownTimer
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.view.inputmethod.InputMethodManager
import android.widget.RadioButton
import androidx.fragment.app.Fragment
import com.example.verifonevx990app.R
import com.example.verifonevx990app.databinding.EnterOtpDialogBinding
import com.example.verifonevx990app.databinding.FragmentInitiatePromoBinding
import com.example.verifonevx990app.main.MainActivity
import com.example.verifonevx990app.realmtables.TerminalParameterTable
import com.example.verifonevx990app.vxUtils.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import java.io.Serializable
import java.util.concurrent.TimeUnit


class InitiatePromoFragment : Fragment(R.layout.fragment_initiate_promo) {

    private var initiateViewBinding: FragmentInitiatePromoBinding? = null
    private var title: String? = null
    private var promoType: Int? = null
    private var gender = 'M'
    private var mobileNumber: String? = null

    //  var addAndSendPromo=false
    private var promoId: String? = null

    private var mCountDown: CountDownTimer? = null
    private var otpExpireTime: Long = 120000

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        initiateViewBinding = FragmentInitiatePromoBinding.inflate(inflater, container, false)
        return initiateViewBinding?.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        mobileNumber = arguments.let { it?.getString("mobNo") }
        promoId = arguments?.getString("promoId") ?: "0"
        //    addAndSendPromo= arguments?.getBoolean("addAndSend") == true
        initiateViewBinding?.mobileNoET?.setText(mobileNumber ?: "") //= "mobileNumber"
        title = arguments?.getString(MainActivity.INPUT_SUB_HEADING)
        promoType = arguments?.getInt("promoType", -1947)
        initiateViewBinding?.subHeaderView?.backImageButton?.setOnClickListener { parentFragmentManager.popBackStackImmediate() }
        initiateViewBinding?.subHeaderView?.subHeaderText?.text = title
        initiateViewBinding?.okBTN?.text = title

        initiateViewBinding?.rg?.setOnCheckedChangeListener { group, checkedId ->
            // This will get the radiobutton that has changed in its check state
            val checkedRadioButton =
                group.findViewById<View>(checkedId) as RadioButton

            when (checkedRadioButton) {
                initiateViewBinding?.maleRB -> {
                    gender = 'M'
                }
                initiateViewBinding?.femaleRB -> {
                    gender = 'F'
                }
            }
        }

        // Init UI According to type of Promotion option...
        when (promoType) {
            1 -> {
                initiateViewBinding?.genderLL?.visibility = View.GONE
                initiateViewBinding?.ageTIL?.visibility = View.GONE
            }
            2 -> {
                initiateViewBinding?.genderLL?.visibility = View.GONE
                initiateViewBinding?.ageTIL?.visibility = View.GONE
                initiateViewBinding?.promoCodeTIL?.visibility = View.GONE
            }
            3 -> {
                initiateViewBinding?.promoCodeTIL?.visibility = View.GONE
            }

        }

        initiateViewBinding?.okBTN?.setOnClickListener {
            hideSoftKeyboard(requireActivity())
            when (promoType) {
                1 -> {
                    // Redeem promo , mobile , promo
                    if (validatePhoneNumber()) {
                        if (validatePromoCode()) {
                            VFService.showToast("CORRECT---")
                            (activity as BaseActivity).showProgress()
                            GlobalScope.launch(Dispatchers.IO) {

                                val tpt = TerminalParameterTable.selectFromSchemeTable()
                                if (tpt != null) {
                                    val field57Data = "${tpt.promoVersionNo}|${
                                        initiateViewBinding?.mobileNoET?.text.toString().trim()
                                    }|${initiateViewBinding?.promoCodeET?.text?.toString()?.trim()}"
                                    getPromotionData(
                                        field57Data,
                                        ProcessingCode.REDEEM_PROMO_WITHOUT_OTP.code,
                                        tpt
                                    ) { isSuccess, responseMsg, responsef57, fullResponse ->
                                        (activity as BaseActivity).hideProgress()
                                        if (isSuccess) {
                                            val responseIsoData: IsoDataReader =
                                                readIso(fullResponse, false)
                                            val successResponseCode =
                                                responseIsoData.isoMap[39]?.parseRaw2String()
                                                    .toString()
                                            when (successResponseCode) {
                                                "00" -> {
                                                    GlobalScope.launch(Dispatchers.Main) {
                                                        (activity as BaseActivity).alertBoxWithAction(
                                                            null,
                                                            null,
                                                            getString(R.string.success_message),
                                                            responseMsg,
                                                            false,
                                                            getString(R.string.positive_button_ok),
                                                            {
                                                                Log.e(
                                                                    "REDDEM PROMO",
                                                                    "CUSTOMER REDEEM PROMOTION, SUCCESSFULLY"
                                                                )

                                                                // if 00 means it is a without otp redumption
                                                                //  getPromFromServerAndShow()
                                                            },
                                                            {
                                                                // for no button
                                                            })
                                                    }
                                                }

                                                "05" -> {
                                                    // todo ask otp
                                                    // now 980400

                                                    GlobalScope.launch(Dispatchers.Main) {
//newDia()
                                                        enterOTPDialog()
                                                    }
                                                }

                                                else -> {
                                                    GlobalScope.launch(Dispatchers.Main) {
                                                        Log.e(
                                                            "ADD PROMO",
                                                            "ERROR  Else part"
                                                        )
                                                        (activity as BaseActivity).alertBoxWithAction(
                                                            null,
                                                            null,
                                                            getString(R.string.fail),
                                                            responseMsg,
                                                            false,
                                                            getString(R.string.positive_button_ok),
                                                            {
                                                                parentFragmentManager.popBackStackImmediate()
                                                                //    getPromFromServerAndShow()
                                                            },
                                                            {
                                                                // for no button
                                                                // parentFragmentManager.popBackStackImmediate()
                                                            })

                                                    }


                                                }
                                            }
                                        } else {

                                            VFService.showToast(responseMsg)
                                        }
                                    }
                                }
                            }


                        } else {
                            VFService.showToast(getString(R.string.invalid_promo))
                        }
                    } else {
                        VFService.showToast(getString(R.string.enter_valid_mobile_number))
                    }

                }
                2 -> {
                    // Send Promo  mobile --> list of promo
                    if (validatePhoneNumber()) {
                        VFService.showToast("CORRECT")
                        // if 06 new customer then enter age gender ......then add it
                        // send proc -- 980600....

                        getPromFromServerAndShow()


                    } else {
                        VFService.showToast(getString(R.string.enter_valid_mobile_number))
                    }
                }
                3 -> {
                    //Add Customer  mobile  ,age,gender,
                    if (validatePhoneNumber()) {
                        if (validateAge()) {
                            (activity as BaseActivity).showProgress()
                            VFService.showToast("CORRECT+++")
                            Log.e("GENDER", gender.toString())
                            GlobalScope.launch(Dispatchers.IO) {
                                val tpt = TerminalParameterTable.selectFromSchemeTable()
                                if (tpt != null) {

                                    var field57Data = ""
                                    var procCode = ""
                                    if (promoId == "0") {
                                        // Simply Add
                                        field57Data = "${tpt.promoVersionNo}|${
                                            initiateViewBinding?.mobileNoET?.text.toString().trim()
                                        }|${
                                            initiateViewBinding?.ageET?.text?.toString()?.trim()
                                        }|$gender"
                                        procCode = ProcessingCode.ADD_CUSTOMER.code

                                    } else {
                                        // Add and Send promo
                                        field57Data = "${tpt.promoVersionNo}|${
                                            initiateViewBinding?.mobileNoET?.text.toString().trim()
                                        }|${
                                            initiateViewBinding?.ageET?.text?.toString()?.trim()
                                        }|$gender|${promoId}"
                                        procCode = ProcessingCode.SEND_PROMO.code
                                    }

                                    getPromotionData(
                                        field57Data,
                                        procCode,
                                        tpt
                                    )
                                    { isSuccess, responseMsg, responsef57, fullResponse ->
                                        (activity as BaseActivity).hideProgress()
                                        if (isSuccess) {
                                            val responseIsoData: IsoDataReader =
                                                readIso(fullResponse, false)
                                            val successResponseCode =
                                                responseIsoData.isoMap[39]?.parseRaw2String()
                                                    .toString()
                                            when (successResponseCode) {
                                                // "00" means customer sucessfully added
                                                "00" -> {
                                                    GlobalScope.launch(Dispatchers.Main) {
                                                        (activity as BaseActivity).alertBoxWithAction(
                                                            null,
                                                            null,
                                                            getString(R.string.success_message),
                                                            responseMsg,
                                                            false,
                                                            getString(R.string.positive_button_ok),
                                                            {
                                                                Log.e(
                                                                    "ADD PROMO",
                                                                    "CUSTOMER ADDED FOR PROMOTION, SUCCESSFULLY"
                                                                )
                                                                //  getPromFromServerAndShow()
                                                                parentFragmentManager.popBackStack()
                                                            },
                                                            {
                                                                // for no button
                                                            })
                                                    }
                                                }

                                                // 07 means customer already exists.
                                                "07" -> {
                                                    GlobalScope.launch(Dispatchers.Main) {
                                                        Log.e(
                                                            "ADD PROMO",
                                                            "ALREADY ADDED"
                                                        )
                                                        (activity as BaseActivity).alertBoxWithAction(
                                                            null,
                                                            null,
                                                            getString(R.string.success_message),
                                                            responseMsg,
                                                            false,
                                                            getString(R.string.positive_button_ok),
                                                            {
                                                                //    parentFragmentManager.popBackStackImmediate()
                                                                getPromFromServerAndShow()
                                                            },
                                                            {
                                                                // for no button
                                                                parentFragmentManager.popBackStackImmediate()
                                                            })

                                                    }

                                                }
                                                else -> {
                                                    GlobalScope.launch(Dispatchers.Main) {
                                                        Log.e(
                                                            "ADD PROMO",
                                                            "ERROR  Else part"
                                                        )
                                                        (activity as BaseActivity).alertBoxWithAction(
                                                            null,
                                                            null,
                                                            getString(R.string.fail),
                                                            responseMsg,
                                                            false,
                                                            getString(R.string.positive_button_ok),
                                                            {
                                                                parentFragmentManager.popBackStackImmediate()
                                                                //    getPromFromServerAndShow()
                                                            },
                                                            {
                                                                // for no button
                                                                // parentFragmentManager.popBackStackImmediate()
                                                            })

                                                    }


                                                }
                                            }
                                        } else {
                                            // todo show response msg in toast as an error
                                            VFService.showToast(responseMsg)
                                        }
                                    }
                                }
                            }


                        } else {
                            VFService.showToast(getString(R.string.invalid_age))
                        }

                    } else {
                        VFService.showToast(getString(R.string.enter_valid_mobile_number))
                    }

                }
            }
        }

    }


    fun hideSoftKeyboard(activity: Activity) {
        if (activity.currentFocus == null) {
            return
        }
        val inputMethodManager =
            activity.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        inputMethodManager.hideSoftInputFromWindow(activity.currentFocus?.windowToken, 0)
    }

    private fun getPromFromServerAndShow() {
        (activity as BaseActivity).showProgress()
        GlobalScope.launch(Dispatchers.IO) {
            val tpt = TerminalParameterTable.selectFromSchemeTable()
            if (tpt != null) {
                getPromotionData(
                    "000000000000",
                    ProcessingCode.INITIALIZE_PROMOTION.code, tpt
                ) { isSuccess, responseMsg, responsef57, fullResponse ->
                    (activity as BaseActivity).hideProgress()
                    if (isSuccess) {
                        val spliter = responsef57.split("|")
                        if (spliter[1] == "1") {
                            val terminalParameterTable =
                                TerminalParameterTable.selectFromSchemeTable()
                            terminalParameterTable?.isPromoAvailable = true
                            // CheckPromo....
                            if (terminalParameterTable?.reservedValues?.get(4).toString()
                                    .toInt() == 1 && terminalParameterTable?.isPromoAvailable == true
                            ) {
                                terminalParameterTable.hasPromo = "1"
                                TerminalParameterTable.performOperation(terminalParameterTable) {
                                    Log.i("TPT", "UPDATED with promo availability")
                                    TerminalParameterTable.updateMerchantPromoData(
                                        Triple(
                                            spliter[0],
                                            spliter[1] == "1",
                                            spliter[2] == "1"
                                        )
                                    )
                                }
                                val promoData = PromoData().parseAndReturnPromoData(spliter[3])
                                Log.e("PROMO_DATA", promoData.toString())
                                (activity as MainActivity).transactFragment(PromoListFragment().apply {
                                    arguments = Bundle().apply {
                                        putSerializable("promoList", promoData)
                                        putString(
                                            "mobNo",
                                            initiateViewBinding?.mobileNoET?.text.toString().trim()
                                        )
                                        putString(
                                            "age",
                                            initiateViewBinding?.ageET?.text.toString().trim()
                                        )
                                        putString("gender", gender.toString())
                                        putString("promoId", promoId)
                                        putInt("promoType", promoType ?: -1900)
                                    }
                                })

                            }
                        } else {
                            // todo promo not available
                        }
                    }
                }
            }
        }
        // endregion================
        (context as MainActivity).hideProgress()
    }

    private fun validatePhoneNumber(): Boolean {
        return initiateViewBinding?.mobileNoET?.text?.length in 10..13
    }

    private fun validateAge(): Boolean {
        return initiateViewBinding?.ageET?.text?.toString()?.length ?: 0 >= 2
    }

    private fun validatePromoCode(): Boolean {
        return initiateViewBinding?.promoCodeET?.text?.length ?: 0 == 6
    }

    private fun getCountDownTimer(
        secondsInLong: Long,
        dialogBinding: EnterOtpDialogBinding, dialog: Dialog
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
                // dialogBinding?.resendLL.visibility = View.VISIBLE
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
                                    VFService.showToast("Todo here")
                                // todo back here
                            },
                            {})
                    } catch (ex: Exception) {
                        ex.printStackTrace()
                        VFService.showToast(getString(R.string.otp_time_out))
                        //
                    }
                }
            }
        }
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

        val otpET = bindingg.otpET//dialog.findViewById<BHEditText>(R.id.otpET)
        val closeDialogIMG =
            bindingg.closeDialogIMG//dialog.findViewById<ImageView>(R.id.closeDialogIMG)
        val otpSubmitBTN = bindingg.otpSubmitBTN//dialog.findViewById<BHButton>(R.id.otpSubmitBTN)

        closeDialogIMG.setOnClickListener {
            mCountDown?.cancel()
            mCountDown = null
            dialog.dismiss()
            /*  startActivity(Intent(activity, MainActivity::class.java).apply {
                  flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_CLEAR_TASK
              })*/
        }

        otpET.addTextChangedListener(OnTextChange {
            otpSubmitBTN.isEnabled = it.length == 4 || it.length == 6
        })

        otpSubmitBTN.setOnClickListener {
            VFService.showToast("SUBMIT OTP HERE")
        }
        dialog.show()
    }
}


class PromoData : Serializable {
    var promoID = ""
    var promoName = ""
    var promoDescription = ""
    var startDate = ""
    var endDate = ""
    var isOTPRequired = ""
    var action = ""
    var isSelected = false

    fun parseAndReturnPromoData(pData: String): ArrayList<PromoData> {
        val promoList = arrayListOf<PromoData>()
        val allData = pData.split('|')

        for (data in allData) {
            val singleRecord = PromoData()
            val dataList = data.split(',')
            singleRecord.promoID = dataList[0]
            singleRecord.promoName = dataList[1]
            singleRecord.promoDescription = dataList[2]
            singleRecord.startDate = dataList[3]
            singleRecord.endDate = dataList[4]
            singleRecord.isOTPRequired = dataList[5]
            singleRecord.action = dataList[6]

            promoList.add(singleRecord)
        }

        //   just for test purpose
        /*  val singleRecord = PromoData()
          singleRecord.promoID = "dataList[0]"
          singleRecord.promoName = "OFFER 2 , This IS a Special OFFER For test"
          singleRecord.promoDescription = "dataList[2]"
          singleRecord.startDate = "dataList[3]"
          singleRecord.endDate = "dataList[4]"
          singleRecord.isOTPRequired = "dataList[5]"
          singleRecord.action = "dataList[6]"
          promoList.add(singleRecord)*/
        return promoList

    }


}