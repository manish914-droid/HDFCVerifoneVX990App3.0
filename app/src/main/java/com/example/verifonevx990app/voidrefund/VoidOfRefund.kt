package com.example.verifonevx990app.voidrefund

import android.app.Dialog
import android.content.Intent
import android.os.Bundle
import android.text.TextUtils
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.Button
import android.widget.ImageView
import androidx.fragment.app.Fragment
import com.example.verifonevx990app.R
import com.example.verifonevx990app.databinding.FragmentVoidRefundViewBinding
import com.example.verifonevx990app.emv.transactionprocess.SyncReversalToHost
import com.example.verifonevx990app.main.MainActivity
import com.example.verifonevx990app.offlinemanualsale.SyncOfflineSaleToHost
import com.example.verifonevx990app.realmtables.BatchFileDataTable
import com.example.verifonevx990app.utils.printerUtils.EPrintCopyType
import com.example.verifonevx990app.utils.printerUtils.checkForPrintReversalReceipt
import com.example.verifonevx990app.vxUtils.*
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.*

class VoidOfRefund : Fragment() {
    private val title: String by lazy { arguments?.getString(MainActivity.INPUT_SUB_HEADING) ?: "" }
    private var backImageButton: ImageView? = null
    private var invoiceNumberET: BHEditText? = null
    private var voidRefundBT: BHButton? = null
    private var binding: FragmentVoidRefundViewBinding? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentVoidRefundViewBinding.inflate(layoutInflater, container, false)
        return binding?.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding?.subHeaderView?.subHeaderText?.text = title
        logger("ConnectionAddress:- ", VFService.getIpPort().toString(), "d")

        invoiceNumberET = view.findViewById(R.id.invoiceNumberET)
        voidRefundBT = view.findViewById(R.id.voidRefundBT)
        backImageButton = view.findViewById(R.id.back_image_button)
        backImageButton?.setOnClickListener { parentFragmentManager.popBackStackImmediate() }

        //OnClick of VoidOfflineSale Button to find the matching result of the entered Invoice Number:-
        voidRefundBT?.setOnClickListener {
            when {
                TextUtils.isEmpty(invoiceNumberET?.text.toString().trim()) -> VFService.showToast(
                    getString(R.string.invoice_number_should_not_be_empty)
                )
                else -> showConfirmation()
            }
        }
    }

    //Below method is execute only if Invoice Number Field is not empty:-
    private fun showConfirmation() {
        val voidRefundData = BatchFileDataTable.selectVoidRefundSaleDataByInvoice(
            invoiceWithPadding(invoiceNumberET?.text.toString().trim())
        )
        if (voidRefundData != null)
            voidRefundConfirmationDialog(voidRefundData)
        else
            VFService.showToast(getString(R.string.no_data_found))
    }

    //Below method is used to show confirmation pop up for Void Offline Sale:-
    private fun voidRefundConfirmationDialog(voidRefundBatchData: BatchFileDataTable) {
        val dialog = Dialog(requireActivity())
        dialog.setCancelable(false)
        dialog.setContentView(R.layout.void_offline_confirmation_dialog_view)

        dialog.window?.attributes?.windowAnimations = R.style.DialogAnimation
        val window = dialog.window
        window?.setLayout(
            ViewGroup.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.WRAP_CONTENT
        )

        dialog.findViewById<BHTextView>(R.id.dateET)?.text = voidRefundBatchData.transactionDate

        val time = voidRefundBatchData.time
        val timeFormat = SimpleDateFormat("HHmmss", Locale.getDefault())
        val timeFormat2 = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
        var formattedTime = ""
        try {
            val t1 = timeFormat.parse(time)
            formattedTime = timeFormat2.format(t1)
            Log.e("Time", formattedTime)
        } catch (e: ParseException) {
            e.printStackTrace()
        }

        dialog.findViewById<BHTextView>(R.id.timeET)?.text = formattedTime
        dialog.findViewById<BHTextView>(R.id.tidET)?.text = voidRefundBatchData.tid
        dialog.findViewById<BHTextView>(R.id.invoiceET)?.text =
            invoiceWithPadding(voidRefundBatchData.invoiceNumber)
        dialog.findViewById<BHTextView>(R.id.amountTV)?.text = voidRefundBatchData.totalAmmount

        dialog.findViewById<Button>(R.id.cancel_btnn).setOnClickListener {
            dialog.dismiss()
        }
        dialog.findViewById<Button>(R.id.ok_btnn).setOnClickListener {
            dialog.dismiss()
            if (!TextUtils.isEmpty(AppPreference.getString(AppPreference.GENERIC_REVERSAL_KEY))) {
                activity?.runOnUiThread { (activity as MainActivity).showProgress(getString(R.string.reversal_data_sync)) }
                SyncReversalToHost(AppPreference.getReversal()) { isSyncToHost, transMsg ->
                    (activity as MainActivity).runOnUiThread {
                        (activity as MainActivity).hideProgress()
                        if (isSyncToHost) {
                            AppPreference.clearReversal()
                            val voidRefundISOData =
                                CreateVoidRefundPacket(voidRefundBatchData).createVoidRefundTransactionPacket()
                            //  val voidRefundByteArray = voidRefundISOData.generateIsoByteRequest()
                            sendVoidOfRefundToServer(voidRefundISOData, voidRefundBatchData)
                        } else {
                            GlobalScope.launch(Dispatchers.Main) {
                                // VFService.showToast(transMsg)
                            }
                        }
                    }
                }
            } else {
                val voidRefundISOData =
                    CreateVoidRefundPacket(voidRefundBatchData).createVoidRefundTransactionPacket()
                // val voidRefundByteArray = voidRefundISOData.generateIsoByteRequest()
                sendVoidOfRefundToServer(voidRefundISOData, voidRefundBatchData)
            }
        }
        dialog.show()

    }

    //Below method is used to send Void Of Refund to Host:-
    private fun sendVoidOfRefundToServer(
        voidRefundISOData: IsoDataWriter,
        voidRefundBatchData: BatchFileDataTable
    ) {
        (activity as MainActivity).showProgress(getString(R.string.sale_data_sync))
        SyncVoidRefundSale(voidRefundISOData) { voidRefundCB, isoResultData ->
            GlobalScope.launch(Dispatchers.Main) {
                if (voidRefundCB) {
                    val responseIsoData: IsoDataReader = readIso(isoResultData, false)
                    val settlementCheckCode = responseIsoData.isoMap[60]?.rawData.toString()
                    val responseCode = responseIsoData.isoMap[39]?.parseRaw2String().toString()
                    if (voidRefundCB && responseCode == "00") {
                        (activity as MainActivity).hideProgress()
                        BatchFileDataTable.updateVoidRefundStatus(voidRefundBatchData.invoiceNumber)
                        // Saving for Last Success Receipt
                        voidRefundBatchData.transactionType = TransactionType.VOID_REFUND.type
                        val lastSuccessReceiptData = Gson().toJson(voidRefundBatchData)
                        AppPreference.saveString(
                            AppPreference.LAST_SUCCESS_RECEIPT_KEY,
                            lastSuccessReceiptData
                        )

                        GlobalScope.launch(Dispatchers.Main) {
                            txnSuccessToast((activity as MainActivity))
                        }
                        AppPreference.clearReversal()
                        if (VFService.vfPrinter?.status == 0) {
                            VoidRefundSalePrintReceipt().startPrintingVoidRefund(
                                voidRefundBatchData,
                                TransactionType.VOID_REFUND.type,
                                EPrintCopyType.MERCHANT,
                                activity as BaseActivity
                            ) { printCB, printCBC ->
                                if (printCB) {
                                    VoidRefundSalePrintReceipt().startPrintingVoidRefund(
                                        voidRefundBatchData,
                                        TransactionType.VOID_REFUND.type,
                                        EPrintCopyType.CUSTOMER,
                                        activity as BaseActivity
                                    ) { printCB, printCBC_ ->
                                        if (printCBC_) {
                                            if (!TextUtils.isEmpty(settlementCheckCode))
                                                GlobalScope.launch(Dispatchers.Main) {
                                                    syncOfflineSaleAndAskAutoSettlement(
                                                        settlementCheckCode
                                                    )
                                                }

                                        } else {
                                            if (!TextUtils.isEmpty(settlementCheckCode))
                                                GlobalScope.launch(Dispatchers.Main) {
                                                    syncOfflineSaleAndAskAutoSettlement(
                                                        settlementCheckCode
                                                    )
                                                }
                                        }
                                    }
                                } else {
                                    ROCProviderV2.incrementFromResponse(
                                        ROCProviderV2.getRoc(
                                            AppPreference.getBankCode()
                                        ).toString(), AppPreference.getBankCode()
                                    )
                                    (activity as MainActivity).hideProgress()
                                    startActivity(Intent(activity, MainActivity::class.java).apply {
                                        flags =
                                            Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_CLEAR_TASK
                                    })
                                }

                            }
                        } else {
                            (activity as MainActivity).hideProgress()
                            ROCProviderV2.incrementFromResponse(
                                ROCProviderV2.getRoc(AppPreference.getBankCode()).toString(),
                                AppPreference.getBankCode()
                            )
                            VFService.showToast(getString(R.string.printer_error))
                        }

                    } else if (voidRefundCB && responseCode != "00") {
                        ROCProviderV2.incrementFromResponse(
                            ROCProviderV2.getRoc(AppPreference.getBankCode()).toString(),
                            AppPreference.getBankCode()
                        )
                        AppPreference.clearReversal()
                        if (!TextUtils.isEmpty(settlementCheckCode)) {
                            syncOfflineSaleAndAskAutoSettlement(settlementCheckCode)
                        }
                    } else {
                        ROCProviderV2.incrementFromResponse(
                            ROCProviderV2.getRoc(AppPreference.getBankCode()).toString(),
                            AppPreference.getBankCode()
                        )
                        checkForPrintReversalReceipt(activity) { VFService.showToast(getString(R.string.fail_to_upload_void_refund_sale)) }
                    }
                } else {
                    ROCProviderV2.incrementFromResponse(
                        ROCProviderV2.getRoc(AppPreference.getBankCode()).toString(),
                        AppPreference.getBankCode()
                    )
                    (activity as MainActivity).hideProgress()
                    if (!TextUtils.isEmpty(AppPreference.getString(AppPreference.GENERIC_REVERSAL_KEY))) {
                        checkForPrintReversalReceipt(activity) {
                            VFService.showToast(getString(R.string.fail_to_upload_void_refund_sale))
                        }
                    }
                }
            }

        }
    }

    //Below method is used to Sync Offline Sale and Ask for Auto Settlement:-
    private fun syncOfflineSaleAndAskAutoSettlement(autoSettleCode: String) {
        val offlineSaleData = BatchFileDataTable.selectOfflineSaleBatchData()
        if (offlineSaleData.size > 0) {
            (activity as BaseActivity).showProgress(getString(R.string.please_wait_offline_sale_sync))
            SyncOfflineSaleToHost(
                activity as BaseActivity,
                autoSettleCode
            ) { offlineSaleStatus, validationMsg ->
                if (offlineSaleStatus == 1)
                    GlobalScope.launch(Dispatchers.Main) {
                        (activity as BaseActivity).hideProgress()
                        delay(1000)
                        if (autoSettleCode == "1") {
                            (activity as BaseActivity).alertBoxWithAction(
                                null, null,
                                getString(R.string.batch_settle),
                                getString(R.string.do_you_want_to_settle_batch),
                                true, getString(R.string.positive_button_yes), {
                                    startActivity(
                                        Intent(
                                            (activity as BaseActivity),
                                            MainActivity::class.java
                                        ).apply {
                                            putExtra("appUpdateFromSale", true)
                                            flags =
                                                Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_CLEAR_TASK
                                        })
                                }, {
                                    startActivity(
                                        Intent(
                                            (activity as BaseActivity),
                                            MainActivity::class.java
                                        ).apply {
                                            flags =
                                                Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_CLEAR_TASK
                                        })
                                })
                        } else {
                            startActivity(
                                Intent((activity as BaseActivity), MainActivity::class.java).apply {
                                    flags =
                                        Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_CLEAR_TASK
                                })
                        }
                    }
                else
                    GlobalScope.launch(Dispatchers.Main) {
                        (activity as BaseActivity).hideProgress()
                        //VFService.showToast(validationMsg)
                        (activity as BaseActivity).alertBoxWithAction(null, null,
                            getString(R.string.offline_sale_uploading),
                            getString(R.string.fail) + validationMsg,
                            false, getString(R.string.positive_button_ok), {
                                startActivity(
                                    Intent(
                                        (activity as BaseActivity),
                                        MainActivity::class.java
                                    ).apply {
                                        flags =
                                            Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_CLEAR_TASK
                                    })
                            }, {

                            })


                    }
            }
        } else {
            GlobalScope.launch(Dispatchers.Main) {
                if (autoSettleCode == "1") {
                    (activity as BaseActivity).alertBoxWithAction(null, null,
                        getString(R.string.batch_settle),
                        getString(R.string.do_you_want_to_settle_batch),
                        true, getString(R.string.positive_button_yes), {
                            startActivity(
                                Intent((activity as BaseActivity), MainActivity::class.java).apply {
                                    putExtra("appUpdateFromSale", true)
                                    flags =
                                        Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_CLEAR_TASK
                                })
                        }, {
                            startActivity(
                                Intent((activity as BaseActivity), MainActivity::class.java).apply {
                                    flags =
                                        Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_CLEAR_TASK
                                })
                        })
                } else {
                    GlobalScope.launch(Dispatchers.Main) {
                        startActivity(
                            Intent((activity as BaseActivity), MainActivity::class.java).apply {
                                flags =
                                    Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_CLEAR_TASK
                            })
                    }
                }
            }
        }
    }
}