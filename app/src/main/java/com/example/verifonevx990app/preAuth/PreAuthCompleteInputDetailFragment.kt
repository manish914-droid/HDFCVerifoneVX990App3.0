package com.example.verifonevx990app.preAuth

import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import androidx.fragment.app.Fragment
import com.example.verifonevx990app.R
import com.example.verifonevx990app.databinding.FragmentPreAuthCompleteDetailBinding
import com.example.verifonevx990app.emv.transactionprocess.CardProcessedDataModal
import com.example.verifonevx990app.main.MainActivity
import com.example.verifonevx990app.vxUtils.*
import com.example.verifonevx990app.vxUtils.VFService.showToast
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*


class PreAuthCompleteInputDetailFragment : Fragment() {

    private val title: String by lazy { arguments?.getString(MainActivity.INPUT_SUB_HEADING) ?: "" }

    private val cardProcessedData: CardProcessedDataModal by lazy { CardProcessedDataModal() }

    private val authData: AuthCompletionData by lazy { AuthCompletionData() }
    private var binding: FragmentPreAuthCompleteDetailBinding? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        binding = FragmentPreAuthCompleteDetailBinding.inflate(layoutInflater, container, false)
        return binding?.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding?.subHeaderView?.subHeaderText?.text = title
        binding?.tidEt?.isFocusableInTouchMode = true
        binding?.tidEt?.requestFocus()
        binding?.subHeaderView?.backImageButton?.setOnClickListener {
            parentFragmentManager.popBackStackImmediate()
        }
        binding?.authCompleteBtn?.setOnClickListener {
            authData.authTid = binding?.tidEt?.text.toString()//""
            authData.authAmt = binding?.amountEt?.text.toString()
            authData.authBatchNo = binding?.batchEt?.text.toString()
            authData.authRoc = binding?.rocEt?.text.toString()
            if (authData.authTid.isNullOrBlank() || authData.authTid!!.length < 8) {
                showToast("Invalid TID")
                return@setOnClickListener
            } else if (authData.authBatchNo.isNullOrBlank()) {
                showToast("Invalid BatchNo")
                return@setOnClickListener
            } else if (authData.authRoc.isNullOrBlank()) {
                showToast("Invalid ROC")
                return@setOnClickListener
            } else if (authData.authAmt.isNullOrBlank() || authData.authAmt!!.toDouble() < 1) {
                showToast("Invalid Amount")
                return@setOnClickListener
            } else {
                authCompleteConfirmDialog(authData)
            }
        }
    }

    private fun authCompleteConfirmDialog(authCompletionData: AuthCompletionData) {
        val dialog = Dialog(requireActivity())
        // dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialog.setCancelable(false)
        dialog.setContentView(R.layout.auth_complete_confirm_dialog)

        val amountWithRsSymbol =
            "${getString(R.string.rupees_symbol)} ${authCompletionData.authAmt}"

        dialog.findViewById<BHTextView>(R.id.amt_auth)?.text = amountWithRsSymbol
        dialog.findViewById<BHTextView>(R.id.roc_auth)?.text =
            authCompletionData.authRoc?.let { invoiceWithPadding(it) }
        dialog.findViewById<BHTextView>(R.id.tid_auth)?.text = authCompletionData.authTid
        dialog.findViewById<BHTextView>(R.id.batchno_auth)?.text =
            authCompletionData.authBatchNo?.let { invoiceWithPadding(it) }

        dialog.findViewById<BHButton>(R.id.cancel_btnn)?.setOnClickListener {
            dialog.dismiss()
            //  doProcessCard()
        }
        dialog.findViewById<BHButton>(R.id.ok_btnn)?.setOnClickListener {
            confirmCompletePreAuth(authCompletionData)
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

    private fun confirmCompletePreAuth(authCompletionData: AuthCompletionData) {

        val transactionalAmount = authCompletionData.authAmt?.replace(".", "")?.toLong() ?: 0L
        cardProcessedData.apply {
            setTransactionAmount(transactionalAmount)
            setTransType(TransactionType.PRE_AUTH_COMPLETE.type)
            setProcessingCode(ProcessingCode.PRE_SALE_COMPLETE.code)
            setAuthBatch(authCompletionData.authBatchNo.toString())
            setAuthRoc(authCompletionData.authRoc.toString())
            setAuthTid(authCompletionData.authTid.toString())
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
        //    logger("Transaction REQUEST PACKET --->>", transactionISO.isoMap, "e")
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

class AuthCompletionData {
    var authTid: String? = ""
    var authBatchNo: String? = ""
    var authRoc: String? = ""
    var authAmt: String? = ""
}