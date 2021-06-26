package com.example.verifonevx990app.digiPOS.pendingTxn

import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.example.verifonevx990app.R
import com.example.verifonevx990app.databinding.DigiPosTxnListDetailPageBinding
import com.example.verifonevx990app.digiPOS.*
import com.example.verifonevx990app.realmtables.DigiPosDataTable
import com.example.verifonevx990app.utils.printerUtils.EPrintCopyType
import com.example.verifonevx990app.utils.printerUtils.PrintUtil
import com.example.verifonevx990app.vxUtils.IDialog
import com.example.verifonevx990app.vxUtils.VFService
import com.example.verifonevx990app.vxUtils.getDigiPosStatus
import com.example.verifonevx990app.vxUtils.logger
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.*


class PendingDetailFragment : Fragment() {
    private var iDialog: IDialog? = null
    private var binding: DigiPosTxnListDetailPageBinding? = null
    private var detailPageData: DigiPosDataTable? = null
    private var dataToPrintAfterSuccess: DigiPosDataTable? = null
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
            parentFragmentManager.popBackStack()
        }

        if (detailPageData?.txnStatus?.toLowerCase(Locale.ROOT).equals("success", true)) {
            binding?.printButton?.text = getString(R.string.print)
            binding?.transactionIV?.setImageResource(R.drawable.circle_with_tick_mark_green)
            val message = "Transaction ${detailPageData?.txnStatus}"
            binding?.transactionMessageTV?.text = message
            binding?.printButton?.text = getString(R.string.print)
        } else {
            binding?.printButton?.text = getString(R.string.getStatus)
            binding?.transactionIV?.setImageResource(R.drawable.ic_exclaimation_mark_circle_error)
            binding?.printButton?.text = getString(R.string.getStatus)
            val message = "Transaction ${detailPageData?.txnStatus}"
            binding?.transactionMessageTV?.text = message
        }

        val amountData = "\u20B9${detailPageData?.amount}"
        binding?.transactionAmountTV?.text = amountData
        binding?.transactionDateTime?.text = detailPageData?.displayFormatedDate
        binding?.paymentModeTV?.text = detailPageData?.paymentMode
        binding?.mobileNumberTV?.text = detailPageData?.customerMobileNumber
        binding?.ptxnTV?.text = detailPageData?.partnerTxnId
        binding?.mtxnTV?.text = detailPageData?.mTxnId
        binding?.txnStatusTV?.text = detailPageData?.txnStatus

        //OnClick event of Bottom Button:-
        binding?.printButton?.setOnClickListener {
            if (binding?.printButton?.text.toString() == getString(R.string.print)) {
                dataToPrintAfterSuccess?.let { it1 ->
                    PrintUtil(context).printSMSUPIChagreSlip(
                        it1,
                        EPrintCopyType.DUPLICATE,
                        context
                    ) { alertCB, printingFail ->
                        //context.hideProgress()
                        if (!alertCB) {
                            parentFragmentManager.popBackStack()

                        }
                    }
                }

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
                EnumDigiPosProcess.GET_STATUS.code + "^" + detailPageData?.partnerTxnId + "^" + detailPageData?.partnerTxnId + "^"
            Log.d("Field57:- ", req57)
            getDigiPosStatus(
                req57,
                EnumDigiPosProcessingCode.DIGIPOSPROCODE.code,
                false
            ) { isSuccess, responseMsg, responsef57, fullResponse ->
                try {
                    if (isSuccess) {
                        //  val statusRespDataList = responsef57.split("^")
                        //   val status = statusRespDataList[5]
                        iDialog?.hideProgress()
                        lifecycleScope.launch(Dispatchers.Main) {

                            val statusRespDataList =
                                responsef57.split("^")
                            val tabledata =
                                DigiPosDataTable()
                            tabledata.requestType =
                                statusRespDataList[0].toInt()
                            //  tabledata.partnerTxnId = statusRespDataList[1]
                            tabledata.status =
                                statusRespDataList[1]
                            tabledata.statusMsg =
                                statusRespDataList[2]
                            tabledata.statusCode =
                                statusRespDataList[3]
                            tabledata.mTxnId =
                                statusRespDataList[4]
                            tabledata.partnerTxnId =
                                statusRespDataList[6]
                            tabledata.transactionTimeStamp = statusRespDataList[7]
                            tabledata.displayFormatedDate =
                                getDateInDisplayFormatDigipos(statusRespDataList[7])
                            val dateTime =
                                statusRespDataList[7].split(
                                    " "
                                )
                            tabledata.txnDate = dateTime[0]
                            tabledata.txnTime = dateTime[1]
                            tabledata.amount =
                                statusRespDataList[8]
                            tabledata.paymentMode =
                                statusRespDataList[9]
                            tabledata.customerMobileNumber =
                                statusRespDataList[10]
                            tabledata.description =
                                statusRespDataList[11]
                            tabledata.pgwTxnId =
                                statusRespDataList[12]
                            when (statusRespDataList[5]) {
                                EDigiPosPaymentStatus.Pending.desciption -> {
                                    tabledata.txnStatus = statusRespDataList[5]
                                    VFService.showToast(getString(R.string.txn_status_still_pending))
                                }

                                EDigiPosPaymentStatus.Approved.desciption -> {
                                    tabledata.txnStatus = statusRespDataList[5]
                                    binding?.transactionIV?.setImageResource(R.drawable.circle_with_tick_mark_green)
                                    val message = "Transaction ${tabledata.txnStatus}"
                                    binding?.transactionMessageTV?.text = message
                                    binding?.txnStatusTV?.text = tabledata.txnStatus
                                    binding?.printButton?.text = getString(R.string.print)
                                    dataToPrintAfterSuccess = tabledata
                                }

                                else -> {
                                    tabledata.txnStatus =
                                        statusRespDataList[5]
                                    VFService.showToast(statusRespDataList[5])
                                    DigiPosDataTable.deletRecord(tabledata.partnerTxnId)
                                }
                            }
                            DigiPosDataTable.insertOrUpdateDigiposData(tabledata)
                            val dp = DigiPosDataTable.selectAllDigiPosData()
                            val dpObj = Gson().toJson(dp)
                            logger(LOG_TAG.DIGIPOS.tag, "--->      $dpObj ")
                            Log.e("F56->>", responsef57)

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