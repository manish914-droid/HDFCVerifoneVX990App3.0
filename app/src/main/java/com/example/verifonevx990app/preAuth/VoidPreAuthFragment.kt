package com.example.verifonevx990app.preAuth

import android.app.Dialog
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.LinearLayout
import androidx.fragment.app.Fragment
import com.example.customneumorphic.NeumorphButton
import com.example.verifonevx990app.R
import com.example.verifonevx990app.databinding.FragmentVoidPreAuthBinding
import com.example.verifonevx990app.emv.transactionprocess.CardProcessedDataModal
import com.example.verifonevx990app.main.MainActivity
import com.example.verifonevx990app.realmtables.EDashboardItem
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
    private val action by lazy { arguments?.getSerializable("type") ?: "" }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        binding = FragmentVoidPreAuthBinding.inflate(layoutInflater, container, false)
        return binding?.root
    }

    override fun onDetach() {
        super.onDetach()
        hideSoftKeyboard(requireActivity())
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding?.subHeaderView?.subHeaderText?.text = title
      binding?.subHeaderView?.headerImage?.setImageResource((action as EDashboardItem).res)
        binding?.subHeaderView?.backImageButton?.setOnClickListener {
            parentFragmentManager.popBackStackImmediate()

        }

        //region================VOID ROC OnClick Event:-
        binding?.rocVoidEt?.setOnClickListener {
            showEditTextSelected(binding?.rocVoidEt, binding?.rocCrdView, requireContext())
            showEditTextUnSelected(binding?.batchVoidEt, binding?.batchCrdView, requireContext())
            Log.d("RocET:- ", "Clicked")
        }
        //endregion

        //region===============VOID BATCH OnClick Event:-
        binding?.batchVoidEt?.setOnClickListener {
            showEditTextSelected(binding?.batchVoidEt, binding?.batchCrdView, requireContext())
            showEditTextUnSelected(binding?.rocVoidEt, binding?.rocCrdView, requireContext())
            Log.d("BatchET:- ", "Clicked")
        }
        //endregion

        binding?.authVoidBtn?.setOnClickListener {

            authData.authBatchNo = binding?.batchVoidEt?.text.toString()
            authData.authRoc = binding?.rocVoidEt?.text.toString()


            when {
                authData.authBatchNo.isNullOrBlank() -> {
                    VFService.showToast("Enter batch Number")
                    return@setOnClickListener
                }
                authData.authRoc.isNullOrBlank() -> {
                    VFService.showToast("Enter ROC")
                    return@setOnClickListener
                }
                else -> {
                    // voidAuthDataCreation(authData)
                    confirmationDialog(authData)
                }
            }
        }
    }

    private fun confirmationDialog(authData: AuthCompletionData) {
        val dialog = Dialog(requireActivity())
        // dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialog.setCancelable(false)
        dialog.setContentView(R.layout.auth_complete_confirm_dialog)

        dialog.window?.attributes?.windowAnimations = R.style.DialogAnimation
        var window = dialog.window
        window?.setLayout(
            ViewGroup.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.WRAP_CONTENT
        )

        dialog.findViewById<LinearLayout>(R.id.tid_ll)?.visibility = View.GONE
        dialog.findViewById<LinearLayout>(R.id.amount_ll)?.visibility = View.GONE
        dialog.findViewById<BHTextView>(R.id.roc_auth)?.text =
            authData.authRoc?.let { invoiceWithPadding(it) }
        dialog.findViewById<BHTextView>(R.id.batchno_auth)?.text =
            authData.authBatchNo?.let { invoiceWithPadding(it) }

        dialog.findViewById<NeumorphButton>(R.id.cancel_btnn)?.setOnClickListener {
            dialog.dismiss()
        }
        dialog.findViewById<NeumorphButton>(R.id.ok_btnn)?.setOnClickListener {
            voidAuthDataCreation(authData)
            dialog.dismiss()
        }
        dialog.show()
        window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
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
                ) { isSuccess, msg ->
                  //  VFService.showToast("$msg----------->  $isSuccess")
                    logger("PREAUTHVOID", "Is success --->  $isSuccess  Message  --->  $msg")
                }
            }
        }
    }
}