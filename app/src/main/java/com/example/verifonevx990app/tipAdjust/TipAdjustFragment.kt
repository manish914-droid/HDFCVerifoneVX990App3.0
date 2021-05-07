package com.example.verifonevx990app.tipAdjust

import android.content.Intent
import android.os.Bundle
import android.text.TextUtils
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.example.verifonevx990app.R
import com.example.verifonevx990app.databinding.FragmentTipAdjustBinding
import com.example.verifonevx990app.emv.transactionprocess.SyncReversalToHost
import com.example.verifonevx990app.main.MainActivity
import com.example.verifonevx990app.offlinemanualsale.SyncOfflineSaleToHost
import com.example.verifonevx990app.realmtables.BatchFileDataTable
import com.example.verifonevx990app.realmtables.IssuerParameterTable
import com.example.verifonevx990app.realmtables.TerminalParameterTable
import com.example.verifonevx990app.utils.HexStringConverter
import com.example.verifonevx990app.utils.printerUtils.EPrintCopyType
import com.example.verifonevx990app.utils.printerUtils.PrintUtil
import com.example.verifonevx990app.utils.printerUtils.checkForPrintReversalReceipt
import com.example.verifonevx990app.vxUtils.*
import com.google.gson.Gson
import kotlinx.coroutines.*
import java.text.SimpleDateFormat
import java.util.*


class TipAdjustFragment : Fragment() {
    private var successResponseCode: String? = null
    private val title: String by lazy { arguments?.getString(MainActivity.INPUT_SUB_HEADING) ?: "" }

    /*  private val cardProcessedData: CardProcessedDataModal by lazy { CardProcessedDataModal() }
      private val authData: AuthCompletionData by lazy { AuthCompletionData() }*/

    private val tpt by lazy { TerminalParameterTable.selectFromSchemeTable() }
    private var binding: FragmentTipAdjustBinding? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        binding = FragmentTipAdjustBinding.inflate(layoutInflater, container, false)
        return binding?.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding?.subHeaderView?.subHeaderText?.text = title
        binding?.tipOnInvoiceEt?.isFocusableInTouchMode = true
        binding?.tipOnInvoiceEt?.requestFocus()
        binding?.subHeaderView?.backImageButton?.setOnClickListener {
            parentFragmentManager.popBackStackImmediate()
        }
        binding?.authVoidBtn?.setOnClickListener {
            validate()
        }

    }


    private fun validate() {
        if (tpt != null) {
            val invoice = binding?.tipOnInvoiceEt?.text.toString()

            //  val amm=  tip_amount_et.text?.toString()?.replace(".", "")?.toLong() ?: 0L
            val amount = try {
                binding?.tipAmountEt?.text.toString().toFloat()
            } catch (ex: Exception) {
                0f
            }
            val batch = BatchFileDataTable.selectAnyReceipts(invoice)
            if (batch != null && amount != 0f && batch.transactionType == TransactionType.SALE.type) {
                if (batch.tipAmmount.toLong() > 0L) {
                    val msg = "Tip is already adjusted"
                    VFService.showToast(msg)
                    return
                }

                val maxTipPercent =
                    if (tpt?.maxTipPercent?.isEmpty() == true) 0f else (tpt?.maxTipPercent?.toFloat())?.div(
                        100
                    )
                        ?: 0f
                val maxTipLimit =
                    if (tpt?.maxTipLimit?.isEmpty() == true) 0f else (tpt?.maxTipLimit?.toFloat())?.div(
                        100
                    )
                        ?: 0f

                if (maxTipLimit != 0f) { // flat tip check is applied
                    if (amount <= maxTipLimit) {
                        (activity as BaseActivity).showProgress()
                        GlobalScope.launch { createSendISOtoServer(amount, batch) }
                    } else {
                        val msg = "Maximum tip allowed on this terminal is \u20B9 ${
                            "%.2f".format(
                                maxTipLimit
                            )
                        }."
                        (activity as BaseActivity).getInfoDialog("Tip Sale Error", msg) {}
                    }
                } else { // percent tip check is applied
                    val saleAmt = batch.transactionalAmmount.toFloat() / 100
                    //   val per = amount * 100 / saleAmt
                    //   var perc=maxTipPercent * saleAmt
                    // Converting percent to decimal:
                    val maxAmountTip = (maxTipPercent / 100) * saleAmt
                    val formatMaxTipAmount = "%.2f".format(maxAmountTip)
                    if (amount <= maxAmountTip) {
                        (activity as BaseActivity).showProgress()
                        GlobalScope.launch { createSendISOtoServer(amount, batch) }
                    } else {
                        //    val tipAmt = saleAmt * per / 100
                        val msg =
                            "Maximum ${
                                "%.2f".format(
                                    maxTipPercent.toDouble()
                                )
                            }% tip allowed on this terminal.\nTip limit for this transaction is \u20B9 ${
                                "%.2f".format(
                                    formatMaxTipAmount.toDouble()
                                )
                            }"
                        (activity as BaseActivity).getInfoDialog("Tip Sale Error", msg) {}
                    }
                }

            } else if (batch == null) {
                binding?.tipOnInvoiceEt?.error = "No Invoice Found."
            } else if (amount == 0f) {
                binding?.tipAmountEt?.error = "Enter Amount."
            } else if (batch.transactionType != TransactionType.SALE.ordinal) {
                val msg = "Tip sale is not valid for ${batch.getTransactionType()}"
                VFService.showToast(msg)
            }
        }
    }

    private suspend fun checkReversalPerformTipAdjustTransaction(
        transactionISOByteArray: IsoDataWriter,
        batch: BatchFileDataTable,
        tipAmt: Float,
        context: BaseActivity
    ) {
        if (TextUtils.isEmpty(AppPreference.getString(AppPreference.GENERIC_REVERSAL_KEY))) {
            withContext(Dispatchers.Main) {
                context.hideProgress()
                context.showProgress(getString(R.string.sale_data_sync))
            }
            syncTipAdjustTransactionPacketToHost(transactionISOByteArray) { syncStatus, responseCode, transactionMsg ->
                //withContext(Dispatchers.Main){
                context.hideProgress()
                //}

                if (syncStatus && responseCode == "00") {
                    AppPreference.clearReversal()
                    GlobalScope.launch(Dispatchers.Main) { txnSuccessToast(context) }

                    val responseIsoData: IsoDataReader = readIso(transactionMsg, false)
                    val autoSettlementCheck =
                        responseIsoData.isoMap[60]?.parseRaw2String().toString()
                    //Below we are saving batch data and print the receipt of transaction:-
                    val resp = readIso(transactionMsg, false)
                    val tip = (tipAmt * 100).toLong()
                    /* batch.tipAmmount = tip.toString()
                     batch.totalAmmount = (batch.transactionalAmmount.toLong() + tip).toString()
                     batch.transactionalAmmount = batch.totalAmmount
 */
                    batch.baseAmmount = (batch.transactionalAmmount.toString())
                    batch.tipAmmount = (tip).toString()
                    batch.totalAmmount = (batch.transactionalAmmount.toLong() + tip).toString()
                    batch.baseAmmount = (batch.transactionalAmmount.toLong() + tip).toString()
                    batch.transactionalAmmount =
                        (batch.transactionalAmmount.toLong() + tip).toString()

                    //     batch.aqrRefNo = resp.isoMap[31]?.parseRaw2String() ?: ""
                    /*RRN -->37
                    Auth code--> 38
                    */
                    //Here we are only saving new referencenumber and other details are as well
                    batch.referenceNumber =
                        (resp.isoMap[37]?.parseRaw2String() ?: "").replace(" ", "")

                    batch.authCode = (responseIsoData.isoMap[38]?.parseRaw2String().toString())

                    batch.roc = ROCProviderV2.getRoc(AppPreference.getBankCode()).toString()
                    ROCProviderV2.incrementFromResponse(
                        ROCProviderV2.getRoc(AppPreference.getBankCode()).toString(),
                        AppPreference.getBankCode()
                    )
                    batch.transactionType = TransactionType.TIP_SALE.type
                    BatchFileDataTable.performOperation(batch)

// Saving for last success report
                    val lastSuccessReceiptData = Gson().toJson(batch)
                    AppPreference.saveString(
                        AppPreference.LAST_SUCCESS_RECEIPT_KEY,
                        lastSuccessReceiptData
                    )
                    /* GlobalScope.launch(Dispatchers.Main){
                         context.showProgress("Printing")
                     }*/
                    PrintUtil(context).startPrinting(
                        batch, EPrintCopyType.MERCHANT,
                        context
                    ) { alertCB, printingFail ->
                        context.hideProgress()
                        if (!alertCB) {
                            if (!TextUtils.isEmpty(autoSettlementCheck)) {
                                context.runOnUiThread {
                                    syncOfflineSaleAndAskAutoSettlement(
                                        autoSettlementCheck.substring(
                                            0,
                                            1
                                        )
                                    )
                                }
                            }
                            ////Here SyncOffline Code and AutoSettlement Check Code implemented
                        }
                    }

                } else if (syncStatus && responseCode != "00") {
                    val responseIsoData: IsoDataReader = readIso(transactionMsg, false)
                    val autoSettlementCheck =
                        responseIsoData.isoMap[60]?.parseRaw2String().toString()
                    AppPreference.clearReversal()
                    if (!TextUtils.isEmpty(autoSettlementCheck)) {
                        context.runOnUiThread {
                            syncOfflineSaleAndAskAutoSettlement(autoSettlementCheck.substring(0, 1))
                        }
                    }
                    ROCProviderV2.incrementFromResponse(
                        ROCProviderV2.getRoc(AppPreference.getBankCode()).toString(),
                        AppPreference.getBankCode()
                    )
                    GlobalScope.launch(Dispatchers.Main) {
                        // VFService.showToast("$responseCode ------> $transactionMsg")
                    }
                } else {
                    VFService.showToast(transactionMsg)
                    ROCProviderV2.incrementFromResponse(
                        ROCProviderV2.getRoc(AppPreference.getBankCode()).toString(),
                        AppPreference.getBankCode()
                    )
                    GlobalScope.launch(Dispatchers.Main) {
                        context.alertBoxWithAction(
                            null,
                            null,
                            context.getString(R.string.declined),
                            context.getString(R.string.transaction_delined_msg),
                            false,
                            context.getString(R.string.positive_button_ok),
                            { alertPositiveCallback ->
                                if (alertPositiveCallback)
                                    checkForPrintReversalReceipt(activity) {
                                        declinedTransaction()
                                    }
                            },
                            {})
                    }
                }
            }
        }
        //Sending Reversal Data Packet to Host:-(In Case of reversal)
        else {
            if (!TextUtils.isEmpty(AppPreference.getString(AppPreference.GENERIC_REVERSAL_KEY))) {
                withContext(Dispatchers.Main) {
                    context.hideProgress()
                    activity?.getString(R.string.reversal_data_sync)
                        ?.let { context.showProgress(it) }
                }
                SyncReversalToHost(AppPreference.getReversal()) { isSyncToHost, transMsg ->
                    context.hideProgress()
                    if (isSyncToHost) {
                        AppPreference.clearReversal()
                        GlobalScope.launch(Dispatchers.IO) {
                            checkReversalPerformTipAdjustTransaction(
                                transactionISOByteArray, batch, tipAmt, context
                            )
                        }
                    } else {
                        GlobalScope.launch(Dispatchers.Main) {
                            // VFService.showToast(transMsg)
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


    //Below method is used to handle Transaction Declined case:-
    private fun declinedTransaction() {
        startActivity(Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_CLEAR_TASK
        })
    }

    private suspend fun createSendISOtoServer(tipAmt: Float, batch: BatchFileDataTable) {
        val tipAdjustISO = createTipAdjustISO(tipAmt, batch)
        // logger("Transaction REQUEST PACKET --->>", tipAdjustISO.isoMap, "e")
        checkReversalPerformTipAdjustTransaction(
            tipAdjustISO,
            batch,
            tipAmt,
            activity as BaseActivity
        )
    }

    //Below method is used to sync Transaction Packet Data to host:-
    private suspend fun syncTipAdjustTransactionPacketToHost(
        transactionISOData: IsoDataWriter?,
        syncTipAdjustCallback: (Boolean, String, String) -> Unit
    ) {
        //In case of reversal
        if (!TextUtils.isEmpty(AppPreference.getString(AppPreference.GENERIC_REVERSAL_KEY))) {
            if (transactionISOData != null) {
                transactionISOData.mti = Mti.REVERSAL.mti

                transactionISOData.additionalData["F56reversal"]?.let {
                    transactionISOData.addFieldByHex(
                        56,
                        it
                    )
                }
                addIsoDateTime(transactionISOData)
            }
        } else {
            transactionISOData?.mti = Mti.PRE_AUTH_COMPLETE_MTI.mti  //used in tip sale
        }
        val transactionISOByteArray = transactionISOData?.generateIsoByteRequest()
        if (transactionISOData != null) {
            logger("Transaction REQUEST PACKET --->>", transactionISOData.isoMap, "e")
        }
        val reversalPacket = Gson().toJson(transactionISOData)
        AppPreference.saveString(AppPreference.GENERIC_REVERSAL_KEY, reversalPacket)
//throw CreateReversal()
        if (transactionISOByteArray != null) {
            HitServer.hitServersale(transactionISOByteArray, { result, success, readtimeout ->
                try {

                    if (success) {
                        //Reversal save To Preference code here.............
                        //Below we are incrementing previous ROC (Because ROC will always be incremented whenever Server Hit is performed:-


                        Log.d("Success Data:- ", result)
                        val responseIsoData: IsoDataReader = readIso(result, false)
                        logger("Transaction RESPONSE ", "---", "e")
                        logger("Transaction RESPONSE --->>", responseIsoData.isoMap, "e")
                        Log.e(
                            "Success 39-->  ",
                            responseIsoData.isoMap[39]?.parseRaw2String().toString() + "---->" +
                                    responseIsoData.isoMap[58]?.parseRaw2String().toString()
                        )
                        successResponseCode =
                            (responseIsoData.isoMap[39]?.parseRaw2String().toString())

                        if (successResponseCode == "00") {
                            //     VFService.showToast("Transaction Success")
                            //   AppPreference.clearReversal()
                            syncTipAdjustCallback(true, successResponseCode.toString(), result)

                        } else {
                            // AppPreference.clearReversal()
                            syncTipAdjustCallback(true, successResponseCode.toString(), result)
                            //   VFService.showToast("Transaction Fail Error Code = ${responseIsoData.isoMap[39]?.parseRaw2String().toString()}")
                        }
                    } else {
                        //Below we are incrementing previous ROC (Because ROC will always be incremented whenever Server Hit is performed:-
                        AppPreference.clearReversal() // Thi line added by Manish Kumar have to check correct or not
                        syncTipAdjustCallback(false, successResponseCode.toString(), result)

                        Log.d("Failure Data:- ", result)
                    }
                } catch (ex: Exception) {
                    ex.printStackTrace()
                }
            }, {
                //backToCalled(it, false, true)
            })
        }
    }

}

fun createTipAdjustISO(tipAmt: Float, batch: BatchFileDataTable): IsoDataWriter =
    IsoDataWriter().apply {
        var amtStr = ""
        try {
            var amt = batch.transactionalAmmount.toFloat() / 100
            amt += tipAmt
            amtStr = "%.2f".format(amt)
            amtStr = amtStr.replace(".", "")
            amtStr = addPad(amtStr, "0", 12)
        } catch (ex: Exception) {
            ex.printStackTrace()

        }


        // mti = Mti.PRE_AUTH_COMPLETE_MTI.mti
        addField(3, ProcessingCode.TIP_SALE.code)
        addField(4, amtStr)

        //STAN(ROC) Field 11
        addField(11, ROCProviderV2.getRoc(AppPreference.getBankCode()).toString())

        //Date and Time Field 12 & 13
        addIsoDateTime(this)

        addField(22, batch.posEntryValue)
        addField(24, Nii.DEFAULT.nii)

        val f31 = batch.aqrRefNo
        if (f31.isNotEmpty()) {
            addFieldByHex(31, f31)
        }

        addFieldByHex(41, batch.tid)
        addFieldByHex(42, batch.mid)

        addFieldByHex(48, Field48ResponseTimestamp.getF48Data())

        var f54 = "%.2f".format(tipAmt)
        f54 = f54.replace(".", "")
        f54 = addPad(f54, "0", 12) + "2"
        addFieldByHex(54, f54)

        logger("TransDate", batch.transactionDate)
        logger("TransTime", batch.transactionTime)
        logger("Time", batch.time)
        logger("Date", batch.date)
        logger("TimeStamp", batch.timeStamp.toString())

        val rocF56 = addPad(batch.roc, "0", 6)
        val batchF56 = addPad(batch.batchNumber, "0", 6)
        val tidF56 = batch.tid
        //   batch.timeStamp.toString()
        val timeStamp: Long = batch.timeStamp//Calendar.getInstance().timeInMillis
        val timeFormatter = SimpleDateFormat("HHmmss", Locale.getDefault())

        logger("Testtime", timeFormatter.format(timeStamp))
        val dateFormatter = SimpleDateFormat("MMdd", Locale.getDefault())
        val previousTransTime = timeFormatter.format(timeStamp)
        val previousTransDate = dateFormatter.format(timeStamp)
        logger("Testdate", dateFormatter.format(timeStamp))
        logger("Miliii", timeStamp.toString())
        val previousTransYear: String =
            SimpleDateFormat("yy", Locale.getDefault()).format(timeStamp)

        logger("yy", previousTransYear)

        val f56 =
            "${tidF56}$batchF56$rocF56$previousTransYear${previousTransDate}${previousTransTime}${batch.authCode}"

        addFieldByHex(56, f56)
        additionalData["F56reversal"] = f56
        addField(57, batch.track2Data)

        //Indicator Data Field 58

        addFieldByHex(58, batch.indicator)

        val batchNm = batch.batchNumber
        addFieldByHex(60, batchNm)

        //adding field 61
        val issuerParameterTable =
            IssuerParameterTable.selectFromIssuerParameterTable(AppPreference.WALLET_ISSUER_ID)
        val version = addPad(getAppVersionNameAndRevisionID(), "0", 15, false)
        val pcNumber = addPad(AppPreference.getString(AppPreference.PC_NUMBER_KEY), "0", 9)
        val data = ConnectionType.GPRS.code + addPad(
            AppPreference.getString("deviceModel"),
            "*",
            6,
            false
        ) +
                addPad(VerifoneApp.appContext.getString(R.string.app_name), " ", 10, false) +
                version + pcNumber + addPad("0", "0", 9)
        val customerID = HexStringConverter.addPreFixer(
            issuerParameterTable?.customerIdentifierFiledType,
            2
        )

        val walletIssuerID = HexStringConverter.addPreFixer(issuerParameterTable?.issuerId, 2)

        addFieldByHex(
            61, addPad(
                AppPreference.getString("serialNumber"), " ", 15, false
            ) + AppPreference.getBankCode() + customerID + walletIssuerID + data
        )

        val invoiceNm = batch.invoiceNumber
        addFieldByHex(62, addPad(invoiceNm, "0", 6, true))


        var year: String = "Year"
        var monthDate: String = "MMdd"
        var hms: String = "HHmmss"
        try {
            val date: Long = Calendar.getInstance().timeInMillis
            val timeFormater = SimpleDateFormat("HHmmss", Locale.getDefault())
            hms = timeFormater.format(date)
            val dateFormater = SimpleDateFormat("MMdd", Locale.getDefault())
            monthDate = dateFormater.format(date)
            //  cardProcessedData.setTimeStamp(date.toString())
            year = SimpleDateFormat("yy", Locale.getDefault()).format(date)
        } catch (ex: Exception) {
            ex.printStackTrace()
        }

    }
