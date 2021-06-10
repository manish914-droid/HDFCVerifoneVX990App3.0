package com.example.verifonevx990app.digiPOS

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.example.verifonevx990app.R
import com.example.verifonevx990app.databinding.DigiPosTxnListDetailPageBinding
import com.example.verifonevx990app.main.MainActivity
import com.example.verifonevx990app.vxUtils.IDialog
import com.example.verifonevx990app.vxUtils.getDigiPosStatus
import com.example.verifonevx990app.vxUtils.logger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.*

class DigiPosTXNListDetailPage : Fragment() {
    private var iDialog: IDialog? = null
    private var binding: DigiPosTxnListDetailPageBinding? = null
    private var detailPageData: DigiPosTxnModal? = null

    override fun onAttach(context: Context) {
        super.onAttach(context)
        if (context is IDialog) iDialog = context
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = DigiPosTxnListDetailPageBinding.inflate(layoutInflater, container, false)
        return binding?.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        detailPageData = arguments?.getParcelable("data")

        binding?.subHeaderView?.subHeaderText?.text = getString(R.string.txn_detail_page)
        binding?.subHeaderView?.backImageButton?.setOnClickListener {
            startActivity(Intent(activity, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_CLEAR_TASK
            })
        }

        if (detailPageData?.status?.toLowerCase(Locale.ROOT).equals("success", true)) {
            binding?.transactionIV?.setImageResource(R.drawable.circle_with_tick_mark_green)
            val message = "Transaction ${detailPageData?.status}"
            binding?.transactionMessageTV?.text = message
            binding?.printButton?.text = getString(R.string.print)
        } else {
            binding?.transactionIV?.setImageResource(R.drawable.ic_exclaimation_mark_circle_error)
            binding?.printButton?.text = getString(R.string.getStatus)
            val message = "Transaction ${detailPageData?.status}"
            binding?.transactionMessageTV?.text = message
        }

        val amountData = "\u20B9${detailPageData?.amount}"
        binding?.transactionAmountTV?.text = amountData
        binding?.transactionDateTime?.text = detailPageData?.transactionTime
        binding?.paymentModeTV?.text = detailPageData?.paymentMode
        binding?.mobileNumberTV?.text = detailPageData?.customerMobileNumber
        binding?.ptxnTV?.text = detailPageData?.partnerTXNID
        binding?.mtxnTV?.text = detailPageData?.mTXNID
        binding?.txnStatusTV?.text = detailPageData?.status

        //OnClick event of Bottom Button:-
        binding?.printButton?.setOnClickListener {
            if (detailPageData?.status.equals("success", true)) {
                //TODO Print Receipt:-
            } else {
                getTransactionStatus()
            }
        }
    }

    //region=========================Get Transaction Status:-
    private fun getTransactionStatus() {
        iDialog?.showProgress()
        lifecycleScope.launch(Dispatchers.IO) {
            val req57 =
                EnumDigiPosProcess.GET_STATUS.code + "^" + detailPageData?.partnerTXNID + "^" + detailPageData?.partnerTXNID + "^"
            Log.d("Field57:- ", req57)
            getDigiPosStatus(
                req57,
                EnumDigiPosProcessingCode.DIGIPOSPROCODE.code,
                false
            ) { isSuccess, responseMsg, responsef57, fullResponse ->
                try {
                    if (isSuccess) {
                        val statusRespDataList = responsef57.split("^")
                        val status = statusRespDataList[1]
                        iDialog?.hideProgress()
                        lifecycleScope.launch(Dispatchers.Main) {
                            if (status.toLowerCase(Locale.ROOT).equals("success", true)) {
                                binding?.transactionIV?.setImageResource(R.drawable.circle_with_tick_mark_green)
                                val message = "Transaction ${detailPageData?.status}"
                                binding?.transactionMessageTV?.text = message
                            }
                        }

                    } else {
                        lifecycleScope.launch(Dispatchers.Main) {
                            iDialog?.hideProgress()
                            iDialog?.alertBoxWithAction(null, null,
                                getString(R.string.error), responseMsg,
                                false, getString(R.string.positive_button_ok),
                                {}, {})
                        }
                    }
                } catch (ex: Exception) {
                    iDialog?.hideProgress()
                    ex.printStackTrace()
                    logger(LOG_TAG.DIGIPOS.tag, "Somethig wrong... in response data field 57")
                }
            }
        }
    }
    //endregion

    override fun onDetach() {
        super.onDetach()
        iDialog = null
    }
}