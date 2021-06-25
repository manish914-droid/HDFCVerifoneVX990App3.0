package com.example.verifonevx990app.digiPOS.pendingTxn

import android.app.Dialog
import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.Button
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.example.customneumorphic.NeumorphButton
import com.example.verifonevx990app.R
import com.example.verifonevx990app.databinding.FragmentSearchTxnBinding
import com.example.verifonevx990app.digiPOS.EDigiPosPaymentStatus
import com.example.verifonevx990app.digiPOS.EnumDigiPosProcess
import com.example.verifonevx990app.digiPOS.EnumDigiPosProcessingCode
import com.example.verifonevx990app.digiPOS.LOG_TAG
import com.example.verifonevx990app.realmtables.DigiPosDataTable
import com.example.verifonevx990app.utils.printerUtils.EPrintCopyType
import com.example.verifonevx990app.utils.printerUtils.PrintUtil
import com.example.verifonevx990app.vxUtils.*
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.*

class SearchTxnFragment : Fragment() {
    var binding: FragmentSearchTxnBinding? = null
    var pendingFragListner: IPendingTxnListner? = null
    override fun onAttach(context: Context) {
        super.onAttach(context)
        if (context is IPendingTxnListner) {
            pendingFragListner = context
        }

    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        binding = FragmentSearchTxnBinding.inflate(layoutInflater, container, false)
        return binding?.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding?.searchTxnBtn?.setOnClickListener {
            validateAndHitServer()
          //  (parentFragment as PendingTxnFragment).getTxnStatus()
        }
    }

    private fun validateAndHitServer() {
        val txn_id_String = binding?.txnIdSearchET?.text.toString()
        if (txn_id_String.length > 2) {
          //  (context as childFragmentManager).getTxnStatus()
            checkTxnStatus(txn_id_String)
        } else
            VFService.showToast("NO Txn ID")
    }

    fun checkTxnStatus(txnId: String){
        lifecycleScope.launch(Dispatchers.IO) {
            withContext(Dispatchers.Main) {
                (activity as BaseActivity).showProgress()
            }
            val req57 = EnumDigiPosProcess.GET_STATUS.code + "^" + txnId + "^"

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
                        tabledata.txnStatus=statusRespDataList[5]

                        val dpObj = Gson().toJson(tabledata)
                        logger("SEARCH STATUS", "--->      $dpObj ")
                        Log.e("F56->>", responsef57)
                        lifecycleScope.launch(Dispatchers.Main){
                            txnStatusDialog(tabledata)
                        }

                    }
                    else {
                        lifecycleScope.launch(Dispatchers.Main) {
                            (activity as BaseActivity).alertBoxWithAction(null,
                                null,
                                getString(R.string.failed),
                                responseMsg,
                                false,
                                getString(R.string.positive_button_ok),
                                { alertPositiveCallback ->
                                    if (alertPositiveCallback) {
                                        parentFragmentManager.popBackStack()
                                    }
                                },
                                {})
                        }
                    }

                } catch (ex: Exception) {
                    ex.printStackTrace()
                    logger(LOG_TAG.DIGIPOS.tag, "Somethig wrong... in response data field 57")
                }
            }
        }
    }


    private fun txnStatusDialog(digiData: DigiPosDataTable) {
        val dialog = Dialog(requireActivity())
        dialog.setCancelable(false)
        dialog.setContentView(R.layout.get_trans_status_dilog)
        dialog.window?.attributes?.windowAnimations = R.style.DialogAnimation
        val window = dialog.window
        window?.setLayout(
            ViewGroup.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.WRAP_CONTENT
        )
        var status=""
        if(digiData.txnStatus==EDigiPosPaymentStatus.Approved.desciption){
            dialog.findViewById<Button>(R.id.print_btnn).visibility=View.VISIBLE
        }else{
            dialog.findViewById<Button>(R.id.print_btnn).visibility=View.GONE
        }
        status=digiData.txnStatus
        val transactionName = "Status :  $status"
        dialog.findViewById<NeumorphButton>(R.id.transType)?.text = transactionName
        dialog.findViewById<BHTextView>(R.id.mobileET)?.text = digiData.customerMobileNumber
        dialog.findViewById<BHTextView>(R.id.modeET)?.text = digiData.paymentMode
        dialog.findViewById<BHTextView>(R.id.amtET)?.text = digiData.amount
        dialog.findViewById<BHTextView>(R.id.mtxnIdEt)?.text = digiData.mTxnId
        dialog.findViewById<BHTextView>(R.id.txnIDET)?.text = digiData.partnerTxnId
        dialog.findViewById<Button>(R.id.okBtn).setOnClickListener {
            dialog.dismiss()
            parentFragmentManager.popBackStack()

        }
        dialog.findViewById<Button>(R.id.print_btnn).setOnClickListener {
            dialog.dismiss()
            PrintUtil(context).printSMSUPIChagreSlip(
                digiData,
                EPrintCopyType.DUPLICATE,
                context
            ) { alertCB, printingFail ->
                //context.hideProgress()
                if (!alertCB) {
                    dialog.dismiss()
                   parentFragmentManager.popBackStack()

                }
            }
        }
        dialog.show()
        window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
    }




}

