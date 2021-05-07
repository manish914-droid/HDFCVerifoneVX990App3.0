package com.example.verifonevx990app.preAuth

import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.LinearLayout
import androidx.fragment.app.Fragment
import com.example.verifonevx990app.R
import com.example.verifonevx990app.databinding.FragmentVoidPreAuthBinding
import com.example.verifonevx990app.emv.transactionprocess.CardProcessedDataModal
import com.example.verifonevx990app.main.MainActivity
import com.example.verifonevx990app.vxUtils.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class VoidPreAuthFragment : Fragment() {

    private val title: String by lazy { arguments?.getString(MainActivity.INPUT_SUB_HEADING) ?: "" }

    private val cardProcessedData: CardProcessedDataModal by lazy { CardProcessedDataModal() }
    private val authData: AuthCompletionData by lazy { AuthCompletionData() }
    private var binding: FragmentVoidPreAuthBinding? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        binding = FragmentVoidPreAuthBinding.inflate(layoutInflater, container, false)
        return binding?.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding?.subHeaderView?.subHeaderText?.text = title

        binding?.subHeaderView?.backImageButton?.setOnClickListener {
            parentFragmentManager.popBackStackImmediate()
        }
        binding?.authVoidBtn?.setOnClickListener {

            authData.authBatchNo = binding?.batchVoidEt?.text.toString()
            authData.authRoc = binding?.rocVoidEt?.text.toString()
            if (authData.authBatchNo.isNullOrBlank()) {
                VFService.showToast("Invalid BatchNo")
                return@setOnClickListener
            } else if (authData.authRoc.isNullOrBlank()) {
                VFService.showToast("Invalid ROC")
                return@setOnClickListener
            } else {
                // voidAuthDataCreation(authData)
                confirmationDialog(authData)
            }
        }

    }

    private fun confirmationDialog(authData: AuthCompletionData) {
        val dialog = Dialog(requireActivity())
        // dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialog.setCancelable(false)
        dialog.setContentView(R.layout.auth_complete_confirm_dialog)
        dialog.findViewById<LinearLayout>(R.id.tid_ll)?.visibility = View.GONE
        dialog.findViewById<LinearLayout>(R.id.amount_ll)?.visibility = View.GONE
        dialog.findViewById<BHTextView>(R.id.roc_auth)?.text =
            authData.authRoc?.let { invoiceWithPadding(it) }
        dialog.findViewById<BHTextView>(R.id.batchno_auth)?.text =
            authData.authBatchNo?.let { invoiceWithPadding(it) }

        dialog.findViewById<BHButton>(R.id.cancel_btnn)?.setOnClickListener {
            dialog.dismiss()
        }
        dialog.findViewById<BHButton>(R.id.ok_btnn)?.setOnClickListener {
            voidAuthDataCreation(authData)
            dialog.dismiss()
        }
        dialog.window?.attributes?.windowAnimations = R.style.DialogAnimation
        val window = dialog.window
        window?.setLayout(
            ViewGroup.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.WRAP_CONTENT
        )
        dialog.show()
    }


    private fun voidAuthDataCreation(authCompletionData: AuthCompletionData) {
        val transactionalAmount = 0L //authCompletionData.authAmt?.replace(".", "")?.toLong() ?: 0L
        cardProcessedData.apply {
            setTransactionAmount(transactionalAmount)
            setTransType(TransactionType.VOID_PREAUTH.type)
            setProcessingCode(ProcessingCode.VOID_PREAUTH.code)
            setAuthBatch(authCompletionData.authBatchNo.toString())
            setAuthRoc(authCompletionData.authRoc.toString())
            //  setAuthTid(authCompletionData.authTid.toString())
        }
        val transactionISO = CreateAuthPacket().createPreAuthCompleteAndVoidPreauthISOPacket(
            authCompletionData,
            cardProcessedData
        )
        //Here we are Saving Date , Time and TimeStamp in CardProcessedDataModal:-
        try {
            val date2: Long = Calendar.getInstance().timeInMillis
            val timeFormater = SimpleDateFormat("HHmmss", Locale.getDefault())
            cardProcessedData.setTime(timeFormater.format(date2))
            val dateFormater = SimpleDateFormat("MMdd", Locale.getDefault())
            cardProcessedData.setDate(dateFormater.format(date2))
            cardProcessedData.setTimeStamp(date2.toString())
        } catch (ex: Exception) {
            ex.printStackTrace()
        }
        logger("Transaction REQUEST PACKET --->>", transactionISO.isoMap, "e")
        val voidPreAuthInvoiceNumber = transactionISO.isoMap[62]?.rawData
        //  runOnUiThread { showProgress(getString(R.string.sale_data_sync)) }
        GlobalScope.launch(Dispatchers.IO) {
            activity?.let {
                SyncAuthTransToHost(it as BaseActivity).checkReversalPerformAuthTransaction(
                    transactionISO, cardProcessedData
                )
            }
        }
    }
}