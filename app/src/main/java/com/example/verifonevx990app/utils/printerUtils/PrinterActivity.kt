package com.example.verifonevx990app.utils.printerUtils

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.res.AssetManager
import android.os.*
import android.util.Log
import androidx.appcompat.app.AlertDialog
import com.example.verifonevx990app.BuildConfig
import com.example.verifonevx990app.R
import com.example.verifonevx990app.realmtables.BatchFileDataTable
import com.example.verifonevx990app.realmtables.EmiSchemeTable
import com.example.verifonevx990app.realmtables.IssuerParameterTable
import com.example.verifonevx990app.realmtables.TerminalParameterTable
import com.example.verifonevx990app.vxUtils.*
import com.vfi.smartpos.deviceservice.aidl.IPrinter
import com.vfi.smartpos.deviceservice.aidl.PrinterConfig
import com.vfi.smartpos.deviceservice.aidl.PrinterListener
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import java.io.IOException
import java.io.InputStream
import java.io.Serializable
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.*

class PrinterActivity : BaseActivity() {
    var printer: IPrinter? = null
    private var signatureMsg: String? = null
    private var pinVerifyMsg: String? = null
    private val disclaimerEmiOpen = "~emi~"
    private val disclaimerEmiClose = "~!emi~"
    private val disclaimerIssuerOpen = "~iss~"
    private val disclaimerIssuerClose = "~!iss~"

    private var footerText = arrayOf("*Thanks you Visit Again*", "POWERED BY")

    private val printingData by lazy {
        intent.getParcelableExtra("printingData") as? BatchFileDataTable
    }

    private val printingType by lazy {
        intent.getSerializableExtra("printingType") as EPrinting
    }

    private val textFormatBundle by lazy { Bundle() }

    // bundle formate for AddTextInLine
    private val textInLineFormatBundle by lazy { Bundle() }

    private var isProgressShown = false
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_printer)
        //  print_progress.visibility = View.VISIBLE
        //   isProgressShown = true
        showProgress(".....Printing....")
        initPrinter()

        checkPrintingType(printingType)

    }

    private fun initPrinter() {
        GlobalScope.launch {
            VFService.connectToVFService(VerifoneApp.appContext)

        }

        try {
            printer = VFService.vfPrinter
            if (printer?.status == 0) {
                initializeFontFiles()
                printer?.cleanCache()
                logger("PrintInit->", "Called Printing", "e")
                logger("PrintUtil->", "Printer Status --->  ${printer?.status}", "e")
                val terminalData = TerminalParameterTable.selectFromSchemeTable()
                //  isTipAllowed = terminalData?.tipProcessing == "1"
            } else {
                //   throw Exception()
                logger("PrintUtil", "Error in printer status --->  ${printer?.status}", "e")
            }
        } catch (ex: DeadObjectException) {
            ex.printStackTrace()
            logger("PrintUtil", "DEAD OBJECT EXCEPTION", "e")
            failureImpl(
                this,
                "Printer Service stopped.",
                "Please take charge slip from the Report menu."
            )
        } catch (e: RemoteException) {
            e.printStackTrace()
            logger("PrintUtil", "REMOTE EXCEPTION", "e")
            failureImpl(
                this,
                "Printer Service stopped.",
                "Please take charge slip from the Report menu."
            )
        } catch (ex: Exception) {
            ex.printStackTrace()
            logger("PrintUtil", "EXCEPTION", "e")
            failureImpl(
                this,
                "Printer Service stopped.",
                "Please take charge slip from the Report menu."
            )
        } finally {
            //   VFService.connectToVFService(VerifoneApp.appContext)
        }

    }

    private fun checkPrintingType(printingType: EPrinting) {
        when (printingType) {
            EPrinting.CANCEL_RECEIPT -> {
                printReversalReceipt {
                    VFService.showToast(it)
                    // print_progress.visibility = View.GONE
                    //  isProgressShown = false
                    onBackPressed()
                }
            }
            EPrinting.START_EMI_SALE_PRINTING -> {
                printEMISale(
                    EPrintCopyType.MERCHANT
                ) {
                    val intent = Intent().apply {
                        putExtra("printStatus", it)
                    }
                    setResult(Activity.RESULT_OK, intent)
                    finish()
                }


            }
            else -> {

            }
        }
    }

    //region ------------Printing Receipt Methods---------------
    private fun printReversalReceipt(callback: (String) -> Unit) {
        val isoW = AppPreference.getReversal()

        if (isoW != null) {
            try {
                setLogoAndHeader()

                val cal = Calendar.getInstance()
                cal.timeInMillis = isoW.timeStamp
                val yr = cal.get(Calendar.YEAR).toString()
                val of12 = isoW.isoMap[12]?.rawData ?: ""
                val of13 = isoW.isoMap[13]?.rawData ?: ""

                val d = of13 + yr
                val roc = isoW.isoMap[11]?.rawData ?: ""
                val tid = isoW.isoMap[41]?.parseRaw2String() ?: ""
                val mid = isoW.isoMap[42]?.parseRaw2String() ?: ""
                val batch = isoW.isoMap[60]?.parseRaw2String() ?: ""

                var amountStr = isoW.isoMap[4]?.rawData ?: "0"
                val amt = amountStr.toFloat() / 100
                amountStr = "%.2f".format(amt)

                val date = "${d.substring(0, 2)}/${d.substring(2, 4)}/${d.substring(4, d.length)}"
                val time =
                    "${of12.substring(0, 2)}:${of12.substring(2, 4)}:${
                        of12.substring(
                            4,
                            of12.length
                        )
                    }"
                alignLeftRightText(textInLineFormatBundle, "DATE : ${date}", "TIME : ${time}")
                alignLeftRightText(textInLineFormatBundle, "MID : ${mid}", "TID : ${tid}")
                alignLeftRightText(
                    textInLineFormatBundle,
                    "BATCH NO  : ${batch}",
                    "ROC : ${invoiceWithPadding(roc)}"
                )

                centerText(textFormatBundle, "TRANSACTION FAILED")


                val card = isoW.additionalData["pan"] ?: ""
                if (card.isNotEmpty())
                    alignLeftRightText(
                        textInLineFormatBundle,
                        "CARD NO : ${card}",
                        "cardtype"
                    )//chip,swipe,cls


                var tvr = isoW.additionalData["tvr"] ?: ""
                var tsi = isoW.additionalData["tsi"] ?: ""
                var aid = isoW.additionalData["aid"] ?: ""
                printer?.addText(textFormatBundle, "--------------------------------")

                if (tsi.isNotEmpty() && tvr.isNotEmpty()) {
                    alignLeftRightText(textInLineFormatBundle, "TVR : ${tvr}", "TSI : ${tsi}")
                }


                if (aid.isNotEmpty()) {
                    aid = "AID : $aid"
                    alignLeftRightText(textInLineFormatBundle, aid, "")
                }

                printSeperator(textFormatBundle)
                centerText(textFormatBundle, "TOTAL AMOUNT : RS $amountStr")
                printSeperator(textFormatBundle)

                centerText(
                    textFormatBundle,
                    "Please contact your card issuer for reversal of debit if any."
                )
                centerText(textFormatBundle, "POWERED BY")
                printLogo("BH.bmp")

                centerText(textFormatBundle, "APP VER : ${BuildConfig.VERSION_NAME}")


                printer?.feedLine(4)

                // start print here
                printer?.startPrint(object : PrinterListener.Stub() {
                    override fun onFinish() {
                        Log.e("CANCEL RECEIPT", "SUCESS__")
                        callback("Printing Success")
                    }

                    override fun onError(error: Int) {
                        Log.e("CANCEL RECEIPT", "FAIL__")
                        callback("Error in Printing")
                    }
                })

            } catch (ex: DeadObjectException) {
                ex.printStackTrace()
                failureImpl(
                    this,
                    "Printer Service stopped.",
                    "Please take chargeslip from the Report menu."
                )
            } catch (e: RemoteException) {
                e.printStackTrace()
                failureImpl(
                    this,
                    "Printer Service stopped.",
                    "Please take chargeslip from the Report menu."
                )
            } catch (ex: Exception) {
                ex.printStackTrace()
                failureImpl(
                    this,
                    "Printer Service stopped.",
                    "Please take chargeslip from the Report menu."
                )
            }
        } else {
            callback("No Reversal Available")
        }
    }

    fun printEMISale(
        copyType: EPrintCopyType,
        printerCallback: (Boolean) -> Unit
    ) {
        try {
            printingData?.let { hasPin(it) }
            setLogoAndHeader()
            printingData?.let { printTransDatetime(it) }
            //===========================
            alignLeftRightText(
                textInLineFormatBundle,
                "MID : ${printingData?.mid}",
                "TID : ${printingData?.tid}"
            )
            alignLeftRightText(
                textInLineFormatBundle,
                "BATCH NO : ${printingData?.batchNumber}",
                "ROC : ${printingData?.let { invoiceWithPadding(it.roc) }}"
            )
            alignLeftRightText(
                textInLineFormatBundle,
                "INVOICE : ${printingData?.let { invoiceWithPadding(it.invoiceNumber) }}",
                ""
            )
            // printer?.addText(textFormatBundle, printingData.getTransactionType())
            printingData?.let { centerText(textFormatBundle, it.getTransactionType(), true) }
            alignLeftRightText(
                textInLineFormatBundle,
                "CARD TYPE : ${printingData?.cardType}",
                "EXP : XX/XX"
            )
            printingData?.let {
                alignLeftRightText(
                    textInLineFormatBundle, "CARD NO : ${
                        printingData?.let {
                            getMaskedPan(
                                TerminalParameterTable.selectFromSchemeTable(),
                                it.cardNumber
                            )
                        }
                    }", it.operationType
                )
            }


            alignLeftRightText(
                textInLineFormatBundle,
                "AUTH CODE : ${printingData?.authCode?.trim()}",
                "RRN : ${printingData?.referenceNumber}"
            )

            if (printingData?.operationType ?: "" != "Mag") {
                //Condition nee to be here before inflating below tvr and tsi?
                if (printingData?.operationType ?: "" == "Chip") {
                    alignLeftRightText(
                        textInLineFormatBundle,
                        "TVR : ${printingData?.tvr}",
                        "TSI : ${printingData?.tsi}"
                    )
                }
                if (printingData?.aid?.isNotBlank() == true && printingData!!.tc.isNotBlank()) {
                    alignLeftRightText(
                        textInLineFormatBundle,
                        "AID : ${printingData!!.aid}",
                        ""
                    )
                    alignLeftRightText(textInLineFormatBundle, "TC : ${printingData!!.tc}", "")
                }
            }

            printSeperator(textFormatBundle)

            //  val txnAmount=(((printingData.transactionAmt).toFloat())%100)
            val txnAmount = "%.2f".format((printingData?.transactionalAmmount?.toFloat())?.div(100))
            alignLeftRightText(textInLineFormatBundle, "TXN AMOUNT", "Rs $txnAmount", ":")
            printingData?.let {
                alignLeftRightText(
                    textInLineFormatBundle,
                    "CARD ISSUER",
                    it.cardType,
                    ":"
                )
            }

            val rateOfInterest = "%.2f".format((printingData?.roi?.toFloat())?.div(100)) + " %"
            alignLeftRightText(textInLineFormatBundle, "ROI (p.a)", rateOfInterest, ":")
            alignLeftRightText(
                textInLineFormatBundle,
                "TENURE",
                (printingData?.tenure ?: "") + " months",
                ":"
            )

            val loanAmount = "%.2f".format((printingData?.loanAmt?.toFloat()?.div(100)))
            alignLeftRightText(textInLineFormatBundle, "LOAN AMOUNT", "Rs $loanAmount", ":")

            val monthlyEMI = "%.2f".format((printingData?.monthlyEmi?.toFloat()?.div(100)))
            alignLeftRightText(textInLineFormatBundle, "MONTHLY EMI", "Rs $monthlyEMI", ":")
            alignLeftRightText(
                textInLineFormatBundle,
                "TOTAL INTEREST",
                "Rs " + (printingData?.totalInterest ?: ""),
                ":"
            )
            val netPay = "%.2f".format(printingData?.netPay?.toFloat()?.div(100))


            alignLeftRightText(textInLineFormatBundle, "TOTAL Amt(With int)", "Rs $netPay", ":")

            printSeperator(textFormatBundle)
            printer?.setLineSpace(1)
            alignLeftRightText(textInLineFormatBundle, "CUSTOMER CONSENT FOR EMI", "")
            printer?.setLineSpace(1)
            val est = EmiSchemeTable.selectFromEmiSchemeTable()
                .first { it.emiSchemeId == printingData?.emiSchemeId ?: "" }

            val disclaimer = est.disclaimer

            val emiDis = disclaimer.substring(
                disclaimer.indexOf(disclaimerEmiOpen),
                disclaimer.indexOf(disclaimerEmiClose)
            )
            val issDis = disclaimer.substring(
                disclaimer.indexOf(disclaimerIssuerOpen),
                disclaimer.indexOf(disclaimerIssuerClose)
            )

            val emiDisArr = emiDis.split("#")
            val issDisArr = issDis.split("#")
            if (emiDisArr.size > 1) {
                for (i in 1 until emiDisArr.size) {
                    alignLeftRightText(textInLineFormatBundle, "#${emiDisArr[i]}", "")
                    /*  sb.append("#.");sb.append(emiDisArr[i])
                      sb.appendln()*/
                }
            }

            printSeperator(textFormatBundle)

            centerText(textFormatBundle, "BASE AMOUNT  :     RS  $txnAmount")

            centerText(textInLineFormatBundle, pinVerifyMsg.toString())
            centerText(textInLineFormatBundle, signatureMsg.toString())
            printingData?.let { centerText(textInLineFormatBundle, it.cardHolderName) }
            centerText(textInLineFormatBundle, copyType.pName)


            val ipt =
                IssuerParameterTable.selectFromIssuerParameterTable(AppPreference.WALLET_ISSUER_ID)
            printer?.addText(textInLineFormatBundle, ipt?.volletIssuerDisclammer)
            printer?.addText(textInLineFormatBundle, footerText[0])
            printer?.addText(textInLineFormatBundle, footerText[1])

            printLogo("BH.bmp")

            printer?.addText(textInLineFormatBundle, "App Version : ${BuildConfig.VERSION_NAME}")

            printSeperator(textFormatBundle)
            if (issDisArr.size > 1) {
                for (i in 1 until issDisArr.size) {
                    alignLeftRightText(textInLineFormatBundle, "#.${issDisArr[i]}", "")
                    /*  sb.append("#.");sb.append(issDisArr[i])
                      sb.appendln()*/
                }
            }

            printer?.addText(textFormatBundle, "---------X-----------X----------")
            printer?.feedLine(4)


            // start print here and callback of printer:-
            printer?.startPrint(object : PrinterListener.Stub() {
                override fun onFinish() {
                    val msg = Message()
                    msg.data.putString("msg", "print finished")
                    VFService.showToast("Printing Successfully")

                    when (copyType) {
                        EPrintCopyType.MERCHANT -> {
                            Handler(Looper.getMainLooper()).postDelayed({
                                if (printingData?.transactionType ?: 0 == TransactionType.EMI_SALE.type)
                                    showMerchantAlertBox(
                                        true
                                    ) { dialogCB ->
                                        printerCallback(dialogCB)
                                    }
                            }, 100)

                        }
                        EPrintCopyType.CUSTOMER -> {
                            VFService.showToast("Customer Transaction Slip Printed Successfully")
                            printerCallback(true)
                        }
                        EPrintCopyType.DUPLICATE -> {
                            VFService.showToast("Printing Successful")
                            printerCallback(true)

                        }

                    }
                }

                override fun onError(error: Int) {
                    if (error == 240) {
                        VFService.showToast("Printing roll not available..")
                        printerCallback(false)
                    } else {
                        VFService.showToast("Printer Error------> $error")
                        printerCallback(false)
                    }
                }
            })
            //====================
        } catch (ex: DeadObjectException) {
            ex.printStackTrace()
            failureImpl(
                this as Activity,
                "Printer Service stopped.",
                "Please take chargeslip from the Report menu."
            )
        } catch (e: RemoteException) {
            e.printStackTrace()
            failureImpl(
                this as Activity,
                "Printer Service stopped.",
                "Please take chargeslip from the Report menu."
            )
        } catch (ex: Exception) {
            ex.printStackTrace()
            failureImpl(
                this as Activity,
                "Printer Service stopped.",
                "Please take chargeslip from the Report menu."
            )
        }
    }
    //endregion -------------------------


    override fun onDestroy() {
        hideProgress()
        super.onDestroy()
    }

    override fun onEvents(event: VxEvent) {

    }

    //region ----------Helper Methods for Printing ------------
    private fun setLogoAndHeader() {
        try {
            val tpt = TerminalParameterTable.selectFromSchemeTable()
            val headers = arrayListOf<String>()
            tpt?.receiptHeaderOne?.let { headers.add(it) }
            tpt?.receiptHeaderTwo?.let { headers.add(it) }
            tpt?.receiptHeaderThree?.let { headers.add(it) }

            setHeaderWithLogo(textFormatBundle, HDFC_LOGO, headers)
        } catch (ex: DeadObjectException) {
            throw ex
        } catch (ex: RemoteException) {
            throw ex
        } catch (ex: Exception) {
            throw ex
        }
    }

    private fun alignLeftRightText(
        fmtAddTextInLine: Bundle,
        leftText: String,
        rightText: String,
        middleText: String = ""
    ) {
        try {
            val mode = if (middleText == "") {
                1
            } else {
                0
            }

            fmtAddTextInLine.putInt(
                PrinterConfig.addTextInLine.FontSize.BundleName,
                PrinterConfig.addTextInLine.FontSize.NORMAL_24_24
            )
            fmtAddTextInLine.putString(
                PrinterConfig.addTextInLine.GlobalFont.BundleName,
                PrinterFonts.path + PrinterFonts.FONT_AGENCYR
            )

            printer?.addTextInLine(
                fmtAddTextInLine, leftText,
                middleText, rightText, mode
            )
        } catch (ex: DeadObjectException) {
            throw ex
        } catch (ex: RemoteException) {
            throw ex
        } catch (ex: Exception) {
            throw ex
        }
        // PrinterConfig.addTextInLine.mode.Devide_flexible
    }

    private fun setHeaderWithLogo(
        format: Bundle,
        img: String,
        headers: ArrayList<String>,
        context: Context? = null
    ) {
        printLogo(img)
        centerText(format, headers[0])
        centerText(format, headers[1])
        centerText(format, headers[2])
    }

    private fun printSeperator(format: Bundle) {
        try {
            // Seperator
            format.putInt(
                PrinterConfig.addText.FontSize.BundleName,
                PrinterConfig.addText.FontSize.NORMAL_24_24
            )
            format.putInt(
                PrinterConfig.addText.Alignment.BundleName,
                PrinterConfig.addText.Alignment.CENTER
            )

            printer?.addText(format, "--------------------------------")
        } catch (ex: DeadObjectException) {
            throw ex
        } catch (ex: RemoteException) {
            throw ex
        } catch (ex: Exception) {
            throw ex
        }


    }

    private fun centerText(format: Bundle, text: String, bold: Boolean = false) {
        if (!bold) {
            format.putInt(
                PrinterConfig.addText.FontSize.BundleName,
                PrinterConfig.addText.FontSize.NORMAL_24_24
            )
            format.putInt(
                PrinterConfig.addText.Alignment.BundleName,
                PrinterConfig.addText.Alignment.CENTER
            )
            printer?.addText(format, text)

        } else {
            format.putInt(
                PrinterConfig.addText.FontSize.BundleName,
                PrinterConfig.addText.FontSize.NORMAL_DH_24_48_IN_BOLD
            )
            format.putInt(
                PrinterConfig.addText.Alignment.BundleName,
                PrinterConfig.addText.Alignment.CENTER
            )
            printer?.addText(format, text)
        }

    }

    private fun printLogo(logo: String) {
        // image
        val buffer: ByteArray?
        try {
            //
            val am: AssetManager = VerifoneApp.appContext.assets
            val ips: InputStream = am.open(logo)
            // get the size
            val size = ips.available()
            // crete the array of byte
            buffer = ByteArray(size)
            ips.read(buffer)
            // close the stream
            ips.close()
        } catch (e: IOException) {
            // Should never happen!
            throw RuntimeException(e)
        } catch (ex: Exception) {
            throw ex
        }
        try {
            val fmtImage = Bundle()
            fmtImage.putInt("offset", 0)
            fmtImage.putInt("width", 384) // bigger then actual, will print the actual
            fmtImage.putInt("height", 255) // bigger then actual, will print the actual//128 default
            //    logger("PS_IMG", (printer?.status).toString(), "e")
            printer?.addImage(fmtImage, buffer)
        } catch (ex: DeadObjectException) {
            throw  ex
        } catch (ex: RemoteException) {
            throw  ex
        } catch (ex: Exception) {
            throw  ex
        }
    }

    private fun hasPin(printerReceiptData: BatchFileDataTable) {
        signatureMsg = if (printerReceiptData.isPinverified) {
            "SIGNATURE NOT REQUIRED"
        } else {
            "SIGN............."
        }
        pinVerifyMsg = if (printerReceiptData.isPinverified) {
            "PIN VERIFIED OK"
        } else {
            ""
        }
    }

    private fun printTransType(format: Bundle, transType: Int) {
        try {
            format.putInt(
                PrinterConfig.addText.FontSize.BundleName,
                PrinterConfig.addText.FontSize.NORMAL_DH_24_48_IN_BOLD
            )
            format.putInt(
                PrinterConfig.addText.Alignment.BundleName,
                PrinterConfig.addText.Alignment.CENTER
            )
            printer?.addText(format, getNameByTransactionType(transType))
        } catch (ex: DeadObjectException) {
            throw ex
        } catch (ex: RemoteException) {
            throw ex
        } catch (ex: Exception) {
            throw ex
        }

    }


    private fun getNameByTransactionType(transactionType: Int): String {
        var tTyp = ""
        for (e in TransactionType.values()) {
            if (e.ordinal == transactionType) {
                tTyp = e.txnTitle
                break
            }
        }
        return tTyp
    }

    private fun printTransDatetime(printerReceiptData: BatchFileDataTable) {
        try {
            textInLineFormatBundle.putInt(
                PrinterConfig.addTextInLine.FontSize.BundleName,
                PrinterConfig.addTextInLine.FontSize.NORMAL_24_24
            )
            textInLineFormatBundle.putString(
                PrinterConfig.addTextInLine.GlobalFont.BundleName,
                PrinterFonts.path + PrinterFonts.FONT_AGENCYR
            )
            val time = printerReceiptData.time
            val timeFormat = SimpleDateFormat("HHmmss", Locale.getDefault())
            val timeFormat2 = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
            var formattedTime = ""
            try {
                val t1 = timeFormat.parse(time)
                formattedTime = timeFormat2.format(t1 ?: Date())
                Log.e("Time", formattedTime)
            } catch (e: ParseException) {
                e.printStackTrace()
            }
            printer?.addTextInLine(
                textInLineFormatBundle, "DATE : ${printerReceiptData.transactionDate}",
                "", "TIME : $formattedTime", 0
            )
        } catch (ex: DeadObjectException) {
            throw ex
        } catch (ex: RemoteException) {
            throw ex
        } catch (ex: Exception) {
            throw ex
        }
    }

    //Method to show Merchant Copy Alert Box used for printing process:-
    fun showMerchantAlertBox(isEMI: Boolean = false, dialogCB: (Boolean) -> Unit) {
        alertBoxWithAction(
            getString(R.string.print_customer_copy),
            getString(R.string.do_you_want_to_print_customer_copy),
            true, getString(R.string.positive_button_yes), { status ->
                //On OK PRESS CALLBACK
                if (status) {
                    if (isEMI) {
                        printEMISale(
                            EPrintCopyType.CUSTOMER
                        ) { customerCopyPrintSuccess ->
                            if (customerCopyPrintSuccess) {
                                VFService.showToast(getString(R.string.customer_copy_print_success))
                                dialogCB(true)
                            }
                        }
                    } else {
                        /* printerUtil.startPrinting(
                            batchData,
                            EPrintCopyType.CUSTOMER,
                            this
                        ) { customerCopyPrintSuccess ->
                            if (!customerCopyPrintSuccess) {
                                VFService.showToast(getString(R.string.customer_copy_print_success))
                                dialogCB(false)
                            }
                        }*/
                    }
                }
            }, {
                //ON CANCEL BUTTON PRESS
                dialogCB(true)
            })
    }

    fun alertBoxWithAction(
        title: String, msg: String, showCancelButton: Boolean,
        positiveButtonText: String, alertCallback: (Boolean) -> Unit,
        cancelButtonCallback: (Boolean) -> Unit
    ) {
        val builder = AlertDialog.Builder(this)
        builder.setTitle(title)
        builder.setMessage(msg)
            .setCancelable(false)
            .setPositiveButton(positiveButtonText) { dialog, _ ->
                dialog.dismiss()
                alertCallback(true)
            }

        if (showCancelButton) {
            builder.setNegativeButton("No") { dialog, _ ->
                dialog.cancel()
                cancelButtonCallback(true)
                /* startActivity(Intent(this, MainActivity::class.java).apply {
                     flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_CLEAR_TASK
                 })*/
            }
        }
        val alert: AlertDialog = builder.create()
        alert.show()

    }


//endregion -------------------------------------------
}

enum class EPrinting(val value: Int, val title: String) : Serializable {
    CANCEL_RECEIPT(1, "Print Cancel Receipt"),
    START_SALE_PRINTING(2, "Printing Sale Receipt"),
    START_EMI_SALE_PRINTING(3, "Printing EMI Sale Receipt")

}

typealias EmiPrintingCallback = (Boolean) -> Unit