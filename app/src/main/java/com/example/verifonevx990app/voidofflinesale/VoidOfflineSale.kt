package com.example.verifonevx990app.voidofflinesale

import android.app.Dialog
import android.content.Intent
import android.os.Bundle
import android.text.TextUtils
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.ImageView
import androidx.fragment.app.Fragment
import com.example.verifonevx990app.R
import com.example.verifonevx990app.databinding.FragmentVoidOfflineSaleBinding
import com.example.verifonevx990app.emv.transactionprocess.SyncReversalToHost
import com.example.verifonevx990app.main.MainActivity
import com.example.verifonevx990app.realmtables.BatchFileDataTable
import com.example.verifonevx990app.utils.printerUtils.EPrintCopyType
import com.example.verifonevx990app.vxUtils.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

class VoidOfflineSale : Fragment() {
    private val title: String by lazy { arguments?.getString(MainActivity.INPUT_SUB_HEADING) ?: "" }
    private var backImageButton: ImageView? = null
    private var invoiceNumberET: BHEditText? = null
    private var voidOfflineSaleBT: BHButton? = null
    private var binding: FragmentVoidOfflineSaleBinding? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentVoidOfflineSaleBinding.inflate(layoutInflater, container, false)
        return binding?.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding?.subHeaderView?.subHeaderText?.text = title
        logger("ConnectionAddress:- ", VFService.getIpPort().toString(), "d")

        invoiceNumberET = view.findViewById(R.id.invoiceNumberET)
        voidOfflineSaleBT = view.findViewById(R.id.voidOfflineSaleBT)
        backImageButton = view.findViewById(R.id.back_image_button)
        backImageButton?.setOnClickListener { parentFragmentManager.popBackStackImmediate() }

        //OnClick of VoidOfflineSale Button to find the matching result of the entered Invoice Number:-
        voidOfflineSaleBT?.setOnClickListener {
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
        val offlineSaleData = BatchFileDataTable.selectOfflineSaleDataByInvoice(
            invoiceNumberET?.text.toString().trim()
        )
        if (offlineSaleData != null)
            voidOfflineConfirmationDialog(offlineSaleData)
        else
            VFService.showToast(getString(R.string.no_data_found))
    }

    //Below method is used to show confirmation pop up for Void Offline Sale:-
    private fun voidOfflineConfirmationDialog(voidOfflineBatchData: BatchFileDataTable) {
        val dialog = Dialog(requireActivity())
        dialog.setCancelable(false)
        dialog.setContentView(R.layout.void_offline_confirmation_dialog_view)

        dialog.window?.attributes?.windowAnimations = R.style.DialogAnimation
        val window = dialog.window
        window?.setLayout(
            ViewGroup.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.WRAP_CONTENT
        )

        dialog.findViewById<BHTextView>(R.id.dateET)?.text = voidOfflineBatchData.printDate
        dialog.findViewById<BHTextView>(R.id.timeET)?.text = voidOfflineBatchData.time
        dialog.findViewById<BHTextView>(R.id.tidET)?.text = voidOfflineBatchData.tid
        dialog.findViewById<BHTextView>(R.id.invoiceET)?.text =
            invoiceWithPadding(voidOfflineBatchData.invoiceNumber)
        dialog.findViewById<BHTextView>(R.id.amountTV)?.text = voidOfflineBatchData.totalAmmount

        dialog.findViewById<BHButton>(R.id.cancel_btnn).setOnClickListener {
            dialog.dismiss()
        }
        dialog.findViewById<BHButton>(R.id.ok_btnn).setOnClickListener {
            dialog.dismiss()
            if (!TextUtils.isEmpty(AppPreference.getString(AppPreference.GENERIC_REVERSAL_KEY))) {
                (activity as MainActivity).showProgress(getString(R.string.reversal_data_sync))
                SyncReversalToHost(AppPreference.getReversal()) { syncStatus, transactionMsg ->
                    (activity as MainActivity).hideProgress()
                    if (syncStatus) {
                        GlobalScope.launch(Dispatchers.Main) {
                            syncOfflineSale(voidOfflineBatchData)
                        }
                    } else {
                        GlobalScope.launch(Dispatchers.Main) {
                            VFService.showToast("Reversal MSG:-------> $transactionMsg")
                        }
                    }
                }
            } else {
                syncOfflineSale(voidOfflineBatchData)
            }

        }
        dialog.show()
    }

    //Below method is used to Sync VoidOfflineSale:-
    private fun syncOfflineSale(voidOfflineBatchData: BatchFileDataTable) {
        if (voidOfflineBatchData.isOfflineSale) {
            (activity as MainActivity).showProgress(getString(R.string.sale_data_sync))
        }
        SyncVoidOfflineSale(voidOfflineBatchData) { voidOfflineCB ->
            if (voidOfflineCB) {
                GlobalScope.launch(Dispatchers.Main) {
                    (activity as MainActivity).hideProgress()
                    txnSuccessToast((activity as MainActivity))
                    BatchFileDataTable.updateOfflineSaleTransactionType(voidOfflineBatchData.invoiceNumber)
                    if (VFService.vfPrinter?.status == 0) {
                        VoidOfflineSalePrintReceipt().voidOfflineSalePrint(
                            activity,
                            voidOfflineBatchData,
                            TransactionType.VOID_OFFLINE_SALE.type,
                            EPrintCopyType.MERCHANT
                        ) { voidPrinterCB, voidPrinterCBC ->
                            if (voidPrinterCB) {
                                VoidOfflineSalePrintReceipt().voidOfflineSalePrint(
                                    activity,
                                    voidOfflineBatchData,
                                    TransactionType.VOID_OFFLINE_SALE.type,
                                    EPrintCopyType.CUSTOMER
                                ) { voidPrinterCB, voidPrinterCBC ->
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
                                // VFService.showToast(getString(R.string.something_went_wrong))
                            }
                        }
                    } else {
                        ROCProviderV2.incrementFromResponse(
                            ROCProviderV2.getRoc(AppPreference.getBankCode()).toString(),
                            AppPreference.getBankCode()
                        )
                        (activity as MainActivity).hideProgress()
                        VFService.showToast(getString(R.string.printer_error))
                        startActivity(Intent(activity, MainActivity::class.java).apply {
                            flags =
                                Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_CLEAR_TASK
                        })
                    }
                }
            } else {
                ROCProviderV2.incrementFromResponse(
                    ROCProviderV2.getRoc(AppPreference.getBankCode()).toString(),
                    AppPreference.getBankCode()
                )

                GlobalScope.launch(Dispatchers.Main) {
                    (activity as MainActivity).hideProgress()
                    VFService.showToast(getString(R.string.fail_to_upload_void_offline_sale))
                }
            }
        }
    }
}