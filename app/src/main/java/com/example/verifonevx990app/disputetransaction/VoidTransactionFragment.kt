package com.example.verifonevx990app.disputetransaction

import android.app.Activity
import android.app.Dialog
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.text.TextUtils
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.Button
import android.widget.ImageView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.example.customneumorphic.NeumorphButton
import com.example.verifonevx990app.R
import com.example.verifonevx990app.databinding.FragmentVoidRefundViewBinding
import com.example.verifonevx990app.digiPOS.EnumDigiPosProcess
import com.example.verifonevx990app.digiPOS.syncTxnCallBackToHost
import com.example.verifonevx990app.emv.transactionprocess.CardProcessedDataModal
import com.example.verifonevx990app.emv.transactionprocess.SyncReversalToHost
import com.example.verifonevx990app.emv.transactionprocess.SyncVoidTransactionToHost
import com.example.verifonevx990app.main.MainActivity
import com.example.verifonevx990app.offlinemanualsale.SyncOfflineSaleToHost
import com.example.verifonevx990app.realmtables.BatchFileDataTable
import com.example.verifonevx990app.realmtables.TerminalParameterTable
import com.example.verifonevx990app.realmtables.TxnCallBackRequestTable
import com.example.verifonevx990app.utils.MoneyUtil
import com.example.verifonevx990app.utils.printerUtils.EPrintCopyType
import com.example.verifonevx990app.utils.printerUtils.PrintUtil
import com.example.verifonevx990app.utils.printerUtils.checkForPrintReversalReceipt
import com.example.verifonevx990app.vxUtils.*
import com.google.gson.Gson
import kotlinx.coroutines.*
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.*


class VoidTransactionFragment : Fragment() {
    private val title: String by lazy { arguments?.getString(MainActivity.INPUT_SUB_HEADING) ?: "" }
    private var backImageButton: ImageView? = null
    private var invoiceNumberET: BHEditText? = null
    private var voidRefundBT: NeumorphButton? = null
    private var binding: FragmentVoidRefundViewBinding? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        binding = FragmentVoidRefundViewBinding.inflate(inflater, container, false)
        return binding?.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding?.subHeaderView?.subHeaderText?.text = title
        logger("ConnectionAddress:- ", VFService.getIpPort().toString(), "d")
        invoiceNumberET = view.findViewById(R.id.invoiceNumberET)
        voidRefundBT = view.findViewById(R.id.voidRefundBT)
        backImageButton = view.findViewById(R.id.back_image_button)
        backImageButton?.setOnClickListener { fragmentManager?.popBackStackImmediate() }
        voidRefundBT?.text = getString(R.string.search_trans)

        /*  if (AppPreference.getIntData(PrefConstant.VOID_ROC_INCREMENT.keyName.toString()) == 0) {
              AppPreference.setIntData(PrefConstant.VOID_ROC_INCREMENT.keyName.toString(), 0)
          }*/
        //OnClick of VoidOfflineSale Button to find the matching result of the entered Invoice Number:-
        voidRefundBT?.setOnClickListener {
            when {
                TextUtils.isEmpty(invoiceNumberET?.text.toString().trim()) -> VFService.showToast(
                    getString(R.string.invoice_number_should_not_be_empty)
                )
                else -> {
                    if (invoiceNumberET?.text.isNullOrBlank()) {
                        VFService.showToast("Enter Invoice")
                    } else {
                        voidRefundBT?.isEnabled = false
                        showConfirmation(invoiceNumberET?.text.toString())
                    }
                }
            }
        }
    }

    //Below method is execute only if Invoice Number Field is not empty:-
    private fun showConfirmation(invoice: String) {
        var voidData: BatchFileDataTable? = null
        GlobalScope.launch {
            val bat =
                BatchFileDataTable.selectVoidTransSaleDataByInvoice(invoiceWithPadding(invoice))
            try {
                voidData = bat.first { it?.hostInvoice?.toLong() == invoice.toLong() }
            } catch (ex: Exception) {
                ex.printStackTrace()
            }
            if (voidData != null) {
                val tpt=TerminalParameterTable.selectFromSchemeTable()
                if(voidData?.transactionType==TransactionType.REFUND.type && tpt?.voidRefund!="1"){
                    VFService.showToast(getString(R.string.void_refund_not_allowed))
                }else{
                    withContext(Dispatchers.Main) {
                        voidTransConfirmationDialog(voidData?: BatchFileDataTable())
                    }
                }
            }
            else {
                withContext(Dispatchers.Main) {
                    voidRefundBT?.isEnabled = true
                }
                VFService.showToast(getString(R.string.no_data_found))
            }
        }
    }

    //Below method is used to show confirmation pop up for Void Offline Sale:-
    private fun voidTransConfirmationDialog(voidData: BatchFileDataTable) {
        val dialog = Dialog(requireActivity())
        dialog.setCancelable(false)
        dialog.setContentView(R.layout.void_offline_confirmation_dialog_view)

        dialog.window?.attributes?.windowAnimations = R.style.DialogAnimation
        val window = dialog.window
        window?.setLayout(
            ViewGroup.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.WRAP_CONTENT
        )
        val transactionName = "VOID " + getTransactionNameByTransType(voidData.transactionType)
        dialog.findViewById<NeumorphButton>(R.id.transType)?.text = transactionName
        dialog.findViewById<BHTextView>(R.id.dateET)?.text = voidData.transactionDate

        val time = voidData.time
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
        dialog.findViewById<BHTextView>(R.id.tidET)?.text = voidData.tid
        dialog.findViewById<BHTextView>(R.id.invoiceET)?.text =
            invoiceWithPadding(voidData.hostInvoice)
        val amt = voidData.totalAmmount.toFloat() / 100f
        /* if (voidData.tipAmmount.toLong() <=0L) {
             dialog.findViewById<BHTextView>(R.id.amountTV)?.text = voidData.totalAmmount
         } else {*/
        //   amt /= 100
        dialog.findViewById<BHTextView>(R.id.amountTV)?.text = "%.2f".format(amt)
        //  }
        dialog.findViewById<Button>(R.id.cancel_btnn).setOnClickListener {
            voidRefundBT?.isEnabled = true
            dialog.dismiss()
        }
        dialog.findViewById<Button>(R.id.ok_btnn).setOnClickListener {
            dialog.dismiss()

            // todo One More Confirmation dialog
            onContinueClicked(voidData)
        }
        dialog.show()
        window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
    }

    private fun onContinueClicked(voidData: BatchFileDataTable) {
        //Sync Reversal
        if (!TextUtils.isEmpty(AppPreference.getString(AppPreference.GENERIC_REVERSAL_KEY))) {
            activity?.runOnUiThread { (activity as MainActivity).showProgress(getString(R.string.reversal_data_sync)) }
            SyncReversalToHost(AppPreference.getReversal()) { isSyncToHost, transMsg ->
                (activity as MainActivity).hideProgress()
                if (isSyncToHost) {
                    AppPreference.clearReversal()
                    onContinueClicked(voidData)
                } else {
                    activity?.runOnUiThread {
                        // VFService.showToast(transMsg)
                    }
                }
            }
        } else {
            //Sync Main Transaction(VOID transaction)
            if (TextUtils.isEmpty(AppPreference.getString(AppPreference.GENERIC_REVERSAL_KEY))) {
                GlobalScope.launch {
                    delay(1000)
                    //**** Creating void packet and send to server ****
                    VoidHelper(
                        activity as MainActivity, voidData
                    ) { code, respnosedatareader, msg ->
                        GlobalScope.launch(Dispatchers.Main) {
                            voidRefundBT?.isEnabled = true
                            when (code) {
                                0 -> {
                                    if (msg.isNotEmpty()) Toast.makeText(
                                        activity as Context,
                                        respnosedatareader?.isoMap?.get(58)?.parseRaw2String()
                                            .toString(),
                                        Toast.LENGTH_SHORT
                                    ).show()
                                    ROCProviderV2.incrementFromResponse(
                                        ROCProviderV2.getRoc(
                                            AppPreference.getBankCode()
                                        ).toString(), AppPreference.getBankCode()
                                    )

                                    GlobalScope.launch(Dispatchers.Main) {
                                        val autoSettlementCheck =
                                            respnosedatareader?.isoMap?.get(60)?.parseRaw2String()
                                                .toString()
                                        if (!TextUtils.isEmpty(autoSettlementCheck))
                                            syncOfflineSaleAndAskAutoSettlement(
                                                autoSettlementCheck.substring(
                                                    0,
                                                    1
                                                )
                                            )
                                        else {
                                            startActivity(
                                                Intent(
                                                    (activity as BaseActivity),
                                                    MainActivity::class.java
                                                ).apply {
                                                    flags =
                                                        Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_CLEAR_TASK
                                                })
                                        }
                                    }

                                }
                                //  Success case
                                1 -> {
                                    //   println("Index and batchListsize is" + index + " and " + " batch " + (batchList.size - 1))
                                    activity?.runOnUiThread {
                                        txnSuccessToast(activity as MainActivity)
                                    }

                                    if (respnosedatareader != null) {
                                        // respnosedatareader.isoMap[62]?.parseRaw2String()?.let {
                                        /*  deleteBatchTableDataInDBWithInvoiceNumber(
                                                                        it
                                                                    )*/
                                        val autoSettlementCheck =
                                            respnosedatareader.isoMap.get(60)?.parseRaw2String()
                                                .toString()

                                        val f60DataList = autoSettlementCheck.split('|')
                                        //   Auto settle flag | Bank id| Issuer id | MID | TID | Batch No | Stan | Invoice | Card Type
                                        //0|1|51|000000041501002|41501369|000150|260|000260|RUPAY|
                                        val date: Long = Calendar.getInstance().timeInMillis
                                        val timeFormater = SimpleDateFormat("HHmmss", Locale.getDefault())
                                        voidData.time=(timeFormater.format(date))
                                        val dateFormater = SimpleDateFormat("MMdd", Locale.getDefault())
                                        voidData.date=(dateFormater.format(date))





                                        try {

                                            voidData.hostBankID = f60DataList[1]
                                            voidData.hostIssuerID = f60DataList[2]
                                            voidData.hostMID = f60DataList[3]
                                            voidData.hostTID = f60DataList[4]
                                            voidData.hostBatchNumber = f60DataList[5]
                                            voidData.hostRoc = f60DataList[6]
                                            voidData.hostInvoice = f60DataList[7]
                                            voidData.hostCardType = f60DataList[8]

                                            // batchFileData
                                        } catch (ex: Exception) {
                                            ex.printStackTrace()
                                            //batchFileData
                                        }


                                        if (voidData.transactionType == TransactionType.REFUND.type) {
                                            voidData.transactionType =
                                                TransactionType.VOID_REFUND.type
                                        }
                                        else if(voidData.transactionType == TransactionType.EMI_SALE.type||voidData.transactionType == TransactionType.BRAND_EMI_BY_ACCESS_CODE.type||voidData.transactionType == TransactionType.BRAND_EMI.type){

                                            voidData.transactionType = TransactionType.VOID_EMI.type
                                        }
                                        else {
                                            voidData.transactionType = TransactionType.VOID.type
                                        }
                                        //   voidData.isRefundSale=false
                                        //   (activity as MainActivity).showProgress()
                                        //    BatchFileDataTable.updateVoidRefundStatus(voidData.invoiceNumber)
                                        //     voidData.aqrRefNo = respnosedatareader.isoMap[31]?.parseRaw2String() ?: ""
                                        voidData.referenceNumber =
                                            (respnosedatareader.isoMap[37]?.parseRaw2String()
                                                ?: "").replace(" ", "")
                                        voidData.roc =
                                            ROCProviderV2.getRoc(AppPreference.getBankCode())
                                                .toString()
                                        ROCProviderV2.incrementFromResponse(
                                            ROCProviderV2.getRoc(AppPreference.getBankCode())
                                                .toString(),
                                            AppPreference.getBankCode()
                                        )

                                        BatchFileDataTable.performOperation(voidData)

                                        // Saving for Last Success Receipt
                                        val lastSuccessReceiptData = Gson().toJson(voidData)
                                        AppPreference.saveString(
                                            AppPreference.LAST_SUCCESS_RECEIPT_KEY,
                                            lastSuccessReceiptData
                                        )

                                        PrintUtil(activity).startPrinting(
                                            voidData,
                                            EPrintCopyType.MERCHANT,
                                            activity as BaseActivity
                                        ) { printCB, printingFail ->
                                            if (!printCB) {

                                                lifecycleScope.launch(Dispatchers.IO) {
                                                    withContext(Dispatchers.Main) {
                                                        (activity as MainActivity).showProgress(
                                                            getString(
                                                                R.string.txn_syn
                                                            )
                                                        )
                                                    }
                                                    val amount = MoneyUtil.fen2yuan(
                                                        voidData.totalAmmount.toDouble().toLong()
                                                    )
                                                    val txnCbReqData = TxnCallBackRequestTable()
                                                    txnCbReqData.reqtype = EnumDigiPosProcess.TRANSACTION_CALL_BACK.code
                                                    txnCbReqData.tid = voidData.hostTID
                                                    txnCbReqData.batchnum = voidData.hostBatchNumber
                                                    txnCbReqData.roc = voidData.hostRoc
                                                    txnCbReqData.amount = amount
                                                    TxnCallBackRequestTable.insertOrUpdateTxnCallBackData(txnCbReqData)
                                                    syncTxnCallBackToHost {
                                                        Log.e("TXN CB ", "SYNCED TO SERVER  --> $it")
                                                        (activity as MainActivity).hideProgress()
                                                    }
                                                    Log.e("VOID LAST", "COMMENT ******")

                                                    //Here we are Syncing Offline Sale if we have any in Batch Table and also Check Sale Response has Auto Settlement enabled or not:-
                                                    //If Auto Settlement Enabled Show Pop Up and User has choice whether he/she wants to settle or not:-

                                                    if (!TextUtils.isEmpty(autoSettlementCheck)) {
                                                        withContext(Dispatchers.Main) {
                                                            syncOfflineSaleAndAskAutoSettlement(
                                                                autoSettlementCheck.substring(0, 1)
                                                            )
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                                2 -> {
                                    checkForPrintReversalReceipt(activity, "") {
                                    }
                                }
                            }
                        }
                    }.start()

                }
            }
        }
    }

    internal class VoidHelper(
        val context: Activity,
        val batch: BatchFileDataTable,
        private val callback: (Int, IsoDataReader?, String) -> Unit
    ) {
        companion object

        val TAG = VoidHelper::class.java.simpleName
        fun start() {
            GlobalScope.launch {
                val transactionISO = CreateVoidPacket(batch).createVoidISOPacket()
                //logger1("Transaction REQUEST PACKET --->>", transactionISO.generateIsoByteRequest(), "e")
                (context as MainActivity).runOnUiThread {
                    (context).showProgress(
                        (context).getString(
                            R.string.sale_data_sync
                        )
                    )

                }
                GlobalScope.launch(Dispatchers.Main) {
                    sendVoidTransToHost(transactionISO)
                }
            }
        }

        private fun sendVoidTransToHost(transactionISOByteArray: IsoDataWriter) {

            if (TextUtils.isEmpty(AppPreference.getString(AppPreference.GENERIC_REVERSAL_KEY))) {
                //  (context as MainActivity).showProgress((context).getString(R.string.please_wait_offline_sale_sync))
                SyncVoidTransactionToHost(
                    transactionISOByteArray,
                    cardProcessedDataModal = CardProcessedDataModal()
                ) { syncStatus, responseCode, transactionMsg, printExtraData ->
                    (context as MainActivity).hideProgress()
                    if (syncStatus) {
                        if (syncStatus && responseCode == "00") {
                            try {
                                val responseIsoData: IsoDataReader =
                                    readIso(transactionMsg.toString(), false)
                                batch.isVoid = true
                                //   batch.isChecked = false
                                AppPreference.clearReversal()
                                callback(
                                    1,
                                    responseIsoData,
                                    responseIsoData.isoMap[39]?.parseRaw2String().toString()
                                )
                            } catch (ex: Exception) {
                                ex.printStackTrace()
                                callback(1, null, "")
                            }
                        } else if (syncStatus && responseCode != "00") {
                            GlobalScope.launch(Dispatchers.Main) {
                                //      VFService.showToast("$responseCode ------> $transactionMsg")
                                try {
                                    val responseIsoData: IsoDataReader =
                                        readIso(transactionMsg.toString(), false)
                                    callback(
                                        0,
                                        responseIsoData,
                                        responseIsoData.isoMap[39]?.parseRaw2String().toString()
                                    )
                                    AppPreference.clearReversal()
                                } catch (ex: Exception) {
                                    ex.printStackTrace()
                                    callback(1, null, "")
                                }
                            }
                        } else {
                            (context).runOnUiThread {
                                (context).hideProgress()
                                callback(2, null, "")
                            }
                        }
                    } else {
                        (context).runOnUiThread {
                            (context).hideProgress()
                            callback(2, null, "")
                        }
                        //  val responseIsoData: IsoDataReader = readIso(transactionMsg, false)
                        //   callback(0, IsoDataReader(), "")
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

    override fun onDetach() {
        super.onDetach()
        hideSoftKeyboard(requireActivity())
    }
}
