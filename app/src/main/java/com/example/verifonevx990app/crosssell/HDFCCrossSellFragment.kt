package com.example.verifonevx990app.crosssell

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.fragment.app.Fragment
import com.example.customneumorphic.NeumorphButton
import com.example.verifonevx990app.R
import com.example.verifonevx990app.databinding.CrossSellViewBinding

import com.example.verifonevx990app.main.MainActivity
import com.example.verifonevx990app.main.SubHeaderTitle
import com.example.verifonevx990app.realmtables.EDashboardItem
import com.example.verifonevx990app.transactions.NewInputAmountFragment
import com.example.verifonevx990app.vxUtils.BHButton
import com.example.verifonevx990app.vxUtils.VFService
import com.example.verifonevx990app.vxUtils.checkInternetConnection

class HDFCCrossSellFragment : Fragment(R.layout.cross_sell_view) {
    private val title: String by lazy { arguments?.getString(MainActivity.INPUT_SUB_HEADING) ?: "" }
    private val reservedValue: String by lazy {
        arguments?.getString(MainActivity.RESERVED_VALUE) ?: ""
    }
    private var backImageButton: ImageView? = null
    private var hdfcCreditCard: NeumorphButton? = null
    private var instaLoan: NeumorphButton? = null
    private var jumboLoan: NeumorphButton? = null
    private var creditLimitIncrease: NeumorphButton? = null
    private var reports: NeumorphButton? = null
    private var flexiPayBtn: NeumorphButton? = null

    private var crossSellViewBinding: CrossSellViewBinding? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        crossSellViewBinding = CrossSellViewBinding.inflate(inflater, container, false)
        return crossSellViewBinding?.root
    }


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)


        crossSellViewBinding?.subHeaderView?.subHeaderText?.text = title
        crossSellViewBinding?.subHeaderView?.headerImage?.setImageResource(CrossSellOptions.HDFC_CREDIT_CARD.res)

        //region Mapping All Layout Views START=======
        hdfcCreditCard = view.findViewById(R.id.hdfcCreditCard)
        instaLoan = view.findViewById(R.id.instaLoan)
        jumboLoan = view.findViewById(R.id.jumboLoan)
        creditLimitIncrease = view.findViewById(R.id.creditLimitIncrease)
        reports = view.findViewById(R.id.reports)
        backImageButton = view.findViewById(R.id.back_image_button)
        flexiPayBtn = view.findViewById(R.id.flexipay)
        //endregion

        //region HIDE/SHOW Buttons on condition based======
        showHideCrossSellOptions(reservedValue)
        Log.d("RESERVE_VALUES:- ", reservedValue)
        //endregion

        //region Attaching Click Events START==========
        backImageButton?.setOnClickListener { parentFragmentManager.popBackStackImmediate() }

        flexiPayBtn?.setOnClickListener {
            if (checkInternetConnection()) {
                (activity as MainActivity).inflateInputFragment(
                    NewInputAmountFragment(),
                    SubHeaderTitle.Flexi_PAY.title,
                    EDashboardItem.FLEXI_PAY
                )
            } else {
                VFService.showToast(getString(R.string.no_internet_available_please_check_your_internet))
            }

        }
        hdfcCreditCard?.setOnClickListener {
            inflateCrossSellProcessFragment(
                CrossSellOptions.HDFC_CREDIT_CARD.heading,
                CrossSellOptions.HDFC_CREDIT_CARD.code,
                CrossSellRequestType.HDFC_CREDIT_CARD_VERIFY_CARD_DETAILS_REQUEST_TYPE.requestTypeCode
            )
        }

        instaLoan?.setOnClickListener {
            inflateCrossSellProcessFragment(
                CrossSellOptions.INSTA_LOAN.heading,
                CrossSellOptions.INSTA_LOAN.code,
                CrossSellRequestType.INSTA_LOAN_VERIFY_CARD_DETAILS_REQUEST_TYPE.requestTypeCode
            )
        }

        jumboLoan?.setOnClickListener {
            inflateCrossSellProcessFragment(
                CrossSellOptions.JUMBO_LOAN.heading,
                CrossSellOptions.JUMBO_LOAN.code,
                CrossSellRequestType.JUMBO_LOAN_VERIFY_CARD_DETAILS_REQUEST_TYPE.requestTypeCode
            )
        }

        creditLimitIncrease?.setOnClickListener {
            inflateCrossSellProcessFragment(
                CrossSellOptions.CREDIT_LIMIT_INCREASE.heading,
                CrossSellOptions.CREDIT_LIMIT_INCREASE.code,
                CrossSellRequestType.CREDIT_LIMIT_INCREASE_VERIFY_CARD_DETAILS_REQUEST_TYPE.requestTypeCode
            )
        }

        reports?.setOnClickListener {
            inflateCrossSellProcessFragment(
                CrossSellOptions.REPORTS.heading,
                CrossSellOptions.REPORTS.code,
                CrossSellRequestType.SENT_REPORT_ON_MAIL_OR_SMS.requestTypeCode
            )
        }
        //endregion
    }

    //region Below method is used to show/hide cross sell options on tpt reserved value based======
    private fun showHideCrossSellOptions(values: String) {
        if (values[13].toString().toInt() == 1) showHideButtons(creditLimitIncrease)
        if (values[14].toString().toInt() == 1) showHideButtons(jumboLoan)
        if (values[15].toString().toInt() == 1) showHideButtons(instaLoan)
        if (values[16].toString().toInt() == 1) showHideButtons(hdfcCreditCard)
    }
    //endregion

    //region ShowButtons Visibility in Layout=========
    private fun showHideButtons(button: NeumorphButton?) {
        button?.visibility = View.VISIBLE
    }
    //endregion

    //region OnClick Navigate to Fragment===========
    private fun inflateCrossSellProcessFragment(heading: String, type: Int, requestType: Int) {
        (activity as MainActivity).transactFragment(InitiateCrossSellProcessFragment().apply {
            arguments = Bundle().apply {
                putInt(MainActivity.CROSS_SELL_OPTIONS, type)
                putInt(MainActivity.CROSS_SELL_REQUEST_TYPE, requestType)
                putString(MainActivity.CROSS_SELL_PROCESS_TYPE_HEADING, heading)
            }
        }, isBackStackAdded = true)
    }
    //endregion
}


//ENUMS
//Below enum class is used to detect Cross Sell List Options:-
enum class CrossSellOptions(val heading: String, val code: Int, val res: Int) {
    FLEXI_PAY("Flexi Pay", 12, R.drawable.ic_flexi_pay),
    CREDIT_LIMIT_INCREASE("Credit Limit Increase", 13, R.drawable.ic_credit_limt),
    JUMBO_LOAN("Jumbo Loan", 14, R.drawable.ic_jumo_loan),
    INSTA_LOAN("Insta Loan", 15 ,R.drawable.ic_insta_loan,),
    HDFC_CREDIT_CARD("HDFC Credit Card", 16, R.drawable.ic_hdfc_cc),
    REPORTS("Report", 17, R.drawable.ic_cs_report)
}

//Below enum class is used to identify cross sell packet request type:-
enum class CrossSellRequestType(val requestTypeCode: Int, val requestName: String = "NOT DEFINED") {
    INSTA_LOAN_VERIFY_CARD_DETAILS_REQUEST_TYPE(1, "Insta Loan"),
    INSTA_LOAN_OTP_VERIFY_REQUEST_TYPE(2),
    JUMBO_LOAN_VERIFY_CARD_DETAILS_REQUEST_TYPE(3, "Jumbo Loan"),
    JUMBO_LOAN_OTP_VERIFY_REQUEST_TYPE(4),
    CREDIT_LIMIT_INCREASE_VERIFY_CARD_DETAILS_REQUEST_TYPE(5, "Credit Limit Increase"),
    CREDIT_LIMIT_INCREASE_OTP_VERIFY_REQUEST_TYPE(6),
    CARD_UPGRADE_VERIFY_CARD_DETAILS_REQUEST_TYPE(7),
    CARD_UPGRADE_OTP_VERIFY_REQUEST_TYPE(8),
    HDFC_CREDIT_CARD_VERIFY_CARD_DETAILS_REQUEST_TYPE(9, "HDFC Credit Card"),
    HDFC_CREDIT_CARD_OTP_VERIFY_REQUEST_TYPE(10),
    DOWNLOAD_AND_PRINT_MONTHLY_REPORT_ON_POS(11),
    SENT_REPORT_ON_MAIL_OR_SMS(12),
}