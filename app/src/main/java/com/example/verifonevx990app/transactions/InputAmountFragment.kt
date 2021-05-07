package com.example.verifonevx990app.transactions


import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.example.verifonevx990app.R
import com.example.verifonevx990app.databinding.FragmentInputAmountBinding
import com.example.verifonevx990app.init.getEditorActionListener
import com.example.verifonevx990app.main.IFragmentRequest
import com.example.verifonevx990app.main.MainActivity
import com.example.verifonevx990app.main.SubHeaderTitle
import com.example.verifonevx990app.realmtables.BatchFileDataTable
import com.example.verifonevx990app.realmtables.EDashboardItem
import com.example.verifonevx990app.realmtables.TerminalParameterTable
import com.example.verifonevx990app.vxUtils.*
import com.example.verifonevx990app.vxUtils.ROCProviderV2.refreshToolbarLogos

class InputAmountFragment : Fragment() {

    private var iDailog: IDialog? = null

    companion object {
        private val TAG = InputAmountFragment::class.java.simpleName
    }

    private val transactionType by lazy { arguments?.getSerializable("type") as EDashboardItem }

    private var iFrReq: IFragmentRequest? = null
    private val title: String by lazy { arguments?.getString(MainActivity.INPUT_SUB_HEADING) ?: "" }
    private var inputAmountEditText: AmountEditText? = null
    private var proceedToPaybutton: Button? = null
    private var back_image_button: ImageView? = null
    private var sub_header_text: TextView? = null
    private var binding: FragmentInputAmountBinding? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentInputAmountBinding.inflate(layoutInflater, container, false)
        return binding?.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val data = BatchFileDataTable.selectBatchData()
        activity?.let { refreshToolbarLogos(it) }
        if (transactionType == EDashboardItem.SALE_WITH_CASH) {
            binding?.cashAmountLl?.visibility = View.VISIBLE
        } else {
            binding?.cashAmountLl?.visibility = View.GONE
        }

        inputAmountEditText = view.findViewById(R.id.iaf_amount_et)
        proceedToPaybutton = view.findViewById(R.id.fia_pay_btn)
        back_image_button = view.findViewById(R.id.back_image_button)
        sub_header_text = view.findViewById(R.id.sub_header_text)
        back_image_button?.setOnClickListener {
            parentFragmentManager.popBackStackImmediate()
        }
        sub_header_text?.text = title
        context?.let { inputAmountEditText?.let { it1 -> toggleSoftKeyboard(it1, it) } }
        transactionType.title.let { VxEvent.ChangeTitle(it) }.let {
            iDailog?.onEvents(
                it
            )
        }
        inputAmountEditText?.requestFocus()
        //Input Amount EditText onKeyboardDone Click Event:-
        inputAmountEditText?.setOnEditorActionListener(getEditorActionListener {
            sendStartSale(it.text.toString(), Triple("", "", true))
        })
        val tpt = TerminalParameterTable.selectFromSchemeTable()
        proceedToPaybutton?.setOnClickListener {
            try {
                if (title == SubHeaderTitle.SALE_SUBHEADER_VALUE.title) {
                    if (tpt?.reservedValues?.substring(0, 1) == "1")
                        showMobileBillDialog(activity, TransactionType.SALE.type) { extraPairData ->
                            sendStartSale(inputAmountEditText?.text.toString(), extraPairData)
                        } else
                        sendStartSale(inputAmountEditText?.text.toString(), Triple("", "", true))
                } else if (title == SubHeaderTitle.BANK_EMI.title && tpt?.reservedValues?.substring(
                        1,
                        2
                    ) == "1" && tpt.reservedValues.substring(2, 3) == "1"
                ) {
                    showMobileBillDialog(activity, TransactionType.EMI_SALE.type) { extraPairData ->
                        sendStartSale(inputAmountEditText?.text.toString(), extraPairData)
                    }
                } else if (title == SubHeaderTitle.BANK_EMI.title && tpt?.reservedValues?.substring(
                        1,
                        2
                    ) == "1"
                ) {
                    showMobileBillDialog(activity, TransactionType.EMI_SALE.type) { extraPairData ->
                        sendStartSale(inputAmountEditText?.text.toString(), extraPairData)
                    }
                } else if (title == SubHeaderTitle.BANK_EMI.title && tpt?.reservedValues?.substring(
                        2,
                        3
                    ) == "1"
                ) {
                    showMobileBillDialog(activity, TransactionType.EMI_SALE.type) { extraPairData ->
                        sendStartSale(inputAmountEditText?.text.toString(), extraPairData)
                    }
                } else
                    sendStartSale(inputAmountEditText?.text.toString(), Triple("", "", true))
            } catch (ex: java.lang.Exception) {
                ex.printStackTrace()
            }

        }
        if (transactionType == EDashboardItem.EMI_ENQUIRY) {
            proceedToPaybutton?.text = getString(R.string.proceed)
        }
    }


    override fun onAttach(context: Context) {
        super.onAttach(context)
        if (context is IFragmentRequest) {
            iFrReq = context
        }

        if (context is IDialog) iDailog = context

    }

    override fun onDetach() {
        super.onDetach()
        iFrReq = null
        iDailog = null
    }

    override fun onDestroyView() {
        super.onDestroyView()
        activity?.let { inputAmountEditText?.let { it1 -> toggleSoftKeyboard(it1, it) } }
    }

    /**
     * First Check for validation and if all validation found ok
     * then proceed
     * */
    private fun sendStartSale(amt: String, extraPairData: Triple<String, String, Boolean>) {
        try {
            val am = amt.toDouble()
            if (am >= 1) {
                activity?.let { hideSoftKeyboard(it) }
                when (transactionType) {
                    EDashboardItem.SALE -> iFrReq?.onFragmentRequest(
                        UiAction.START_SALE,
                        amt,
                        extraPairData
                    )
                    EDashboardItem.PREAUTH -> iFrReq?.onFragmentRequest(UiAction.PRE_AUTH, amt)
                    EDashboardItem.REFUND -> iFrReq?.onFragmentRequest(UiAction.REFUND, amt)
                    EDashboardItem.BANK_EMI -> iFrReq?.onFragmentRequest(
                        UiAction.BANK_EMI,
                        amt,
                        extraPairData
                    )
                    EDashboardItem.EMI_ENQUIRY -> {
                        // val formatAmt = "%.2f".format(amt)
                        iDailog?.onEvents(VxEvent.Emi(amt.toDouble(), transactionType))
                    }//(activity as MainActivity).showToast("TO BE IMPLEMENTED")
                    EDashboardItem.CASH_ADVANCE -> iFrReq?.onFragmentRequest(
                        UiAction.CASH_AT_POS,
                        amt
                    )
                    EDashboardItem.SALE_WITH_CASH -> {
                        val cashAmt = binding?.iafCashAmountEt?.text.toString()
                        if (!cashAmt.isBlank()) {
                            val cash = cashAmt.toDouble()
                            if (cash >= 1) {
                                val amtArr = arrayListOf<String>()
                                amtArr.add(0, amt)
                                amtArr.add(1, cashAmt)
                                iFrReq?.onFragmentRequest(UiAction.SALE_WITH_CASH, amtArr)
                            } else {
                                Toast.makeText(
                                    context,
                                    getString(R.string.minimum_amt_msg),
                                    Toast.LENGTH_LONG
                                ).show()
                            }
                        }
                    }
                    else -> (activity as MainActivity).showToast("TO BE IMPLEMENTED")
                }
            } else {
                Toast.makeText(context, getString(R.string.minimum_amt_msg), Toast.LENGTH_LONG)
                    .show()
            }
        } catch (ex: Exception) {
            ex.printStackTrace()
            Toast.makeText(context, getString(R.string.invalid_amt_msg), Toast.LENGTH_LONG).show()
        }
    }

}

enum class EPinRequire { NO_PIN, SHOULD_PIN, MUST_PIN }

enum class EDeviceType(val atmName: String = "") {
    NONE("None"), MS("Magnetic Stripe Mode"), IC("Chip Mode"), RF(
        "Contactless Mode"
    ),
    REMOVE
}
