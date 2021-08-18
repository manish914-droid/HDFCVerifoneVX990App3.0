package com.example.verifonevx990app.utils.printerUtils

/**
 * Created by Lucky Singh.
 * Modifications needed as per requirements...
 */

import android.app.Activity
import android.content.Context
import android.content.res.AssetManager
import android.os.*
import android.text.TextUtils
import android.util.Log
import com.example.verifonevx990app.BuildConfig
import com.example.verifonevx990app.R
import com.example.verifonevx990app.bankemi.BankEMIDataModal
import com.example.verifonevx990app.crosssell.CrossSellReportWithType
import com.example.verifonevx990app.crosssell.ReportDownloadedModel
import com.example.verifonevx990app.crosssell.TotalCrossellRep
import com.example.verifonevx990app.digiPOS.EDigiPosPaymentStatus
import com.example.verifonevx990app.emv.transactionprocess.CardProcessedDataModal
import com.example.verifonevx990app.emv.transactionprocess.VFTransactionActivity
import com.example.verifonevx990app.main.DetectCardType
import com.example.verifonevx990app.main.MainActivity
import com.example.verifonevx990app.main.SplitterTypes
import com.example.verifonevx990app.preAuth.PendingPreauthData
import com.example.verifonevx990app.realmtables.*
import com.example.verifonevx990app.transactions.EBenefitCalculation
import com.example.verifonevx990app.transactions.IssuerDataModel
import com.example.verifonevx990app.transactions.TenureDataModel
import com.example.verifonevx990app.utils.MoneyUtil
import com.example.verifonevx990app.utils.printerUtils.PrinterFonts.initialize
import com.example.verifonevx990app.vxUtils.*
import com.vfi.smartpos.deviceservice.aidl.IPrinter
import com.vfi.smartpos.deviceservice.aidl.PrinterConfig
import com.vfi.smartpos.deviceservice.aidl.PrinterListener
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import java.io.IOException
import java.io.InputStream
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.*

const val HDFC_LOGO = "hdfc_print_logo.bmp"
const val AMEX_LOGO = "amex_print.bmp"
const val DIGI_SMART_HUB_LOGO = "smart_hub.bmp"
private const val disclaimerEmiOpen = "~emi~"
private const val disclaimerEmiClose = "~!emi~"
private const val disclaimerIssuerOpen = "~iss~"
private const val disclaimerIssuerClose = "~!iss~"
private const val bankEMIFooterTAndCSeparator = "~!emi~~brd~~!brd~~iss~"

class PrintUtil(context: Context?) {
    var printer: IPrinter? = null
    private var isTipAllowed = false
    private var contexT: Context? = null

    init {
        this.contexT = context
        try {
            printer = VFService.vfPrinter
            if (printer?.status == 0) {
                initializeFontFiles()
                printer?.cleanCache()
                logger("PrintInit->", "Called Printing", "e")
                logger("PrintUtil->", "Printer Status --->  ${printer?.status}", "e")
                val terminalData = TerminalParameterTable.selectFromSchemeTable()
                isTipAllowed = terminalData?.tipProcessing == "1"
            } else {
                //   throw Exception()
                logger("PrintUtil", "Error in printer status --->  ${printer?.status}", "e")
            }
        } catch (ex: DeadObjectException) {
            ex.printStackTrace()
            logger("PrintUtil", "DEAD OBJECT EXCEPTION", "e")
            //  VFService.showToast(".... TRY AGAIN ....")

            failureImpl(
                contexT as Activity,
                "Printer Service stopped.",
                "Please take charge slip from the Report menu."
            )

        } catch (e: RemoteException) {
            e.printStackTrace()
            logger("PrintUtil", "REMOTE EXCEPTION", "e")
            failureImpl(
                contexT as Activity,
                "Printer Service stopped.",
                "Please take charge slip from the Report menu."
            )
        } catch (ex: Exception) {
            ex.printStackTrace()
            logger("PrintUtil", "EXCEPTION", "e")
            failureImpl(
                contexT as Activity,
                "Printer Service stopped.",
                "Please take charge slip from the Report menu."
            )
        } finally {

        }

    }

    // bundle format for addText
    // bundle format for addText
    private lateinit var signatureMsg: String
    private lateinit var pinVerifyMsg: String
    private var _issuerName: String? = null
    private var _issuerNameString = "ISSUER"

    private val textFormatBundle by lazy { Bundle() }

    // bundle formate for AddTextInLine
    private val textInLineFormatBundle by lazy { Bundle() }

    private var footerText = arrayOf("*Thank You Visit Again*", "POWERED BY")
    /*var copyType = EPrintCopyType.MERCHANT*/

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


    // Printing Sale Charge slip....
    fun startPrinting(
        printerReceiptData: BatchFileDataTable,
        copyType: EPrintCopyType,
        context: Context?,
        printerCallback: (Boolean, Int) -> Unit
    ) {
        //  printer=null
        try {
            //  logger("PS_START", (printer?.status).toString(), "e")
            val signatureMsg = if (printerReceiptData.isPinverified) {
                "SIGNATURE NOT REQUIRED"
            } else {
                "SIGN ..................."
            }

            val pinVerifyMsg = if (printerReceiptData.isPinverified) {
                "PIN VERIFIED OK"
            } else {
                ""
            }

            //Changes By manish Kumar
            //If in Respnse field 60 data comes Auto settle flag | Bank id| Issuer id | MID | TID | Batch No | Stan | Invoice | Card Type
            // then show response data otherwise show data available in database
            //From mid to hostMID (coming from field 60)
            //From tid to hostTID (coming from field 60)
            //From batchNumber to hostBatchNumber (coming from field 60)
            //From roc to hostRoc (coming from field 60)
            //From invoiceNumber to hostInvoice (coming from field 60)
            //From cardType to hostCardType (coming from field 60)

            val hostMID = if (printerReceiptData.hostMID.isNotBlank()) {
                printerReceiptData.hostMID
            } else {
                printerReceiptData.mid
            }

            val hostTID = if (printerReceiptData.hostTID.isNotBlank()) {
                printerReceiptData.hostTID
            } else {
                printerReceiptData.tid
            }

            val hostBatchNumber = if (printerReceiptData.hostBatchNumber.isNotBlank()) {
                printerReceiptData.hostBatchNumber
            } else {
                printerReceiptData.batchNumber
            }

            val hostRoc = if (printerReceiptData.hostRoc.isNotBlank()) {
                printerReceiptData.hostRoc
            } else {
                printerReceiptData.roc
            }
            val hostInvoice = if (printerReceiptData.hostInvoice.isNotBlank()) {
                printerReceiptData.hostInvoice
            } else {
                printerReceiptData.invoiceNumber
            }
            val hostCardType = if (printerReceiptData.hostCardType.isNotBlank()) {
                printerReceiptData.hostCardType
            } else {
                printerReceiptData.cardType
            }


            // bundle format for addText
            val format = Bundle()
            // bundle formate for AddTextInLine
            val fmtAddTextInLine = Bundle()

            //   printLogo("hdfc_print_logo.bmp")
            setLogoAndHeader()

            fmtAddTextInLine.putInt(
                PrinterConfig.addTextInLine.FontSize.BundleName,
                PrinterConfig.addTextInLine.FontSize.NORMAL_24_24
            )
            fmtAddTextInLine.putString(
                PrinterConfig.addTextInLine.GlobalFont.BundleName,
                PrinterFonts.path + PrinterFonts.FONT_AGENCYR
            )
            //   printer.addTextInLine( fmtAddTextInLine, "L & R", "", "Divide Equally", 0);
            //   printer.addTextInLine( fmtAddTextInLine, "L & R", "", "Divide Equally", 0);
            val formatterdate = SimpleDateFormat("yyMMdd", Locale.getDefault())
            val formattertime = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
            //val date = formatterdate.parse(printerReceiptData.transactionDate)
            //    val time = formattertime.parse(printerReceiptData.time)

            val time = printerReceiptData.time
            val timeFormat = SimpleDateFormat("HHmmss", Locale.getDefault())
            val timeFormat2 = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
            var formattedTime = ""
            try {
                val t1 = timeFormat.parse(time)
                formattedTime = timeFormat2.format(t1)
                Log.e("Time", formattedTime)
                //   logger("PS_DateTime", (printer?.status).toString(), "e")
                printer?.addTextInLine(
                    fmtAddTextInLine,
                    "DATE:${printerReceiptData.transactionDate}",
                    "",
                    "TIME:$formattedTime",
                    0
                )
            } catch (e: ParseException) {
                e.printStackTrace()
            }
            fmtAddTextInLine.putInt(
                PrinterConfig.addTextInLine.FontSize.BundleName,
                PrinterConfig.addTextInLine.FontSize.NORMAL_24_24
            )
            fmtAddTextInLine.putString(
                PrinterConfig.addTextInLine.GlobalFont.BundleName,
                PrinterFonts.path + PrinterFonts.FONT_AGENCYR
            )
            //   printer.addTextInLine( fmtAddTextInLine, "L & R", "", "Divide Equally", 0);
            //   printer.addTextInLine( fmtAddTextInLine, "L & R", "", "Divide Equally", 0);
            //  logger("PS_MID_TID", (printer?.status).toString(), "e")
            printer?.addTextInLine(
                fmtAddTextInLine,
                "MID:${hostMID}",
                "",
                "TID:${hostTID}",
                PrinterConfig.addTextInLine.mode.Devide_flexible
            )


            fmtAddTextInLine.putInt(
                PrinterConfig.addTextInLine.FontSize.BundleName,
                PrinterConfig.addTextInLine.FontSize.NORMAL_24_24
            )
            fmtAddTextInLine.putString(
                PrinterConfig.addTextInLine.GlobalFont.BundleName,
                PrinterFonts.path + PrinterFonts.FONT_AGENCYR
            )
            //   printer.addTextInLine( fmtAddTextInLine, "L & R", "", "Divide Equally", 0);
            //   printer.addTextInLine( fmtAddTextInLine, "L & R", "", "Divide Equally", 0);

            logger("PS_Bno_ROC", (printer?.status).toString(), "e")
            printer?.addTextInLine(
                fmtAddTextInLine,
                "BATCH NO:${addPad(hostBatchNumber, "0", 6)}",
                "",
                "ROC:${invoiceWithPadding(hostRoc)}",
                PrinterConfig.addTextInLine.mode.Devide_flexible
            )

            fmtAddTextInLine.putInt(
                PrinterConfig.addTextInLine.FontSize.BundleName,
                PrinterConfig.addTextInLine.FontSize.NORMAL_24_24
            )
            fmtAddTextInLine.putString(
                PrinterConfig.addTextInLine.GlobalFont.BundleName,
                PrinterFonts.path + PrinterFonts.FONT_AGENCYR
            )
            //   printer.addTextInLine( fmtAddTextInLine, "L & R", "", "Divide Equally", 0);
            //   printer.addTextInLine( fmtAddTextInLine, "L & R", "", "Divide Equally", 0);
            logger("PS_invoice", (printer?.status).toString(), "e")
            printer?.addTextInLine(
                fmtAddTextInLine,
                "INVOICE:${invoiceWithPadding(hostInvoice)}",
                "",
                "",
                PrinterConfig.addTextInLine.mode.Devide_flexible
            )

            printTransType(format, printerReceiptData.transactionType)

            fmtAddTextInLine.putInt(
                PrinterConfig.addTextInLine.FontSize.BundleName,
                PrinterConfig.addTextInLine.FontSize.NORMAL_24_24
            )
            fmtAddTextInLine.putString(
                PrinterConfig.addTextInLine.GlobalFont.BundleName,
                PrinterFonts.path + PrinterFonts.FONT_AGENCYR
            )

            logger("PS_ct_exp", (printer?.status).toString(), "e")

            printer?.addTextInLine(
                fmtAddTextInLine,
                "CARD TYPE:${hostCardType}",
                "",
                "EXP:XX/XX",
                PrinterConfig.addTextInLine.mode.Devide_flexible
            )

            fmtAddTextInLine.putInt(
                PrinterConfig.addTextInLine.FontSize.BundleName,
                PrinterConfig.addTextInLine.FontSize.NORMAL_24_24
            )
            fmtAddTextInLine.putString(
                PrinterConfig.addTextInLine.GlobalFont.BundleName,
                PrinterFonts.path + PrinterFonts.FONT_AGENCYR
            )
            //   printer.addTextInLine( fmtAddTextInLine, "L & R", "", "Divide Equally", 0);
            //   printer.addTextInLine( fmtAddTextInLine, "L & R", "", "Divide Equally", 0);
            logger("PS_ct_no", (printer?.status).toString(), "e")

            printer?.addTextInLine(
                fmtAddTextInLine,
                "CARD NO:${printerReceiptData.cardNumber}",
                "",
                printerReceiptData.operationType,
                PrinterConfig.addTextInLine.mode.Devide_flexible
            )

            if (printerReceiptData.merchantMobileNumber.isNotBlank())
                printer?.addTextInLine(
                    fmtAddTextInLine,
                    "MOBILE NO:${printerReceiptData.merchantMobileNumber}",
                    "",
                    "",
                    PrinterConfig.addTextInLine.mode.Devide_flexible
                )

            fmtAddTextInLine.putInt(
                PrinterConfig.addTextInLine.FontSize.BundleName,
                PrinterConfig.addTextInLine.FontSize.NORMAL_24_24
            )
            fmtAddTextInLine.putString(
                PrinterConfig.addTextInLine.GlobalFont.BundleName,
                PrinterFonts.path + PrinterFonts.FONT_AGENCYR
            )
            //   printer.addTextInLine( fmtAddTextInLine, "L & R", "", "Divide Equally", 0);
            //   printer.addTextInLine( fmtAddTextInLine, "L & R", "", "Divide Equally", 0);
            logger("PS_auth_rrn", (printer?.status).toString(), "e")

            if (printerReceiptData.authCode == "null") {
                printer?.addTextInLine(
                    fmtAddTextInLine,
                    "RRN:${printerReceiptData.referenceNumber}",
                    "",
                    "",
                    PrinterConfig.addTextInLine.mode.Devide_flexible
                )
            } else {
                printer?.addTextInLine(
                    fmtAddTextInLine,
                    "AUTH CODE:${printerReceiptData.authCode.trim()}",
                    "",
                    "RRN:${printerReceiptData.referenceNumber}",
                    PrinterConfig.addTextInLine.mode.Devide_flexible
                )
            }

            if (printerReceiptData.operationType != "Mag") {
                fmtAddTextInLine.putInt(
                    PrinterConfig.addTextInLine.FontSize.BundleName,
                    PrinterConfig.addTextInLine.FontSize.NORMAL_24_24
                )
                fmtAddTextInLine.putString(
                    PrinterConfig.addTextInLine.GlobalFont.BundleName,
                    PrinterFonts.path + PrinterFonts.FONT_AGENCYR
                )
                //   printer.addTextInLine( fmtAddTextInLine, "L & R", "", "Divide Equally", 0);
                //   printer.addTextInLine( fmtAddTextInLine, "L & R", "", "Divide Equally", 0);

                //Condition nee to be here before inflating below tvr and tsi?

                if (printerReceiptData.operationType == "Chip") {
                    logger("PS_tvr_tsi", (printer?.status).toString(), "e")
                    printer?.addTextInLine(
                        fmtAddTextInLine,
                        "TVR:${printerReceiptData.tvr}",
                        "",
                        "TSI:${printerReceiptData.tsi}",
                        PrinterConfig.addTextInLine.mode.Devide_flexible
                    )
                }

                //   printer.addTextInLine( fmtAddTextInLine, "L & R", "", "Divide Equally", 0);
                //   printer.addTextInLine( fmtAddTextInLine, "L & R", "", "Divide Equally", 0);
                if (!printerReceiptData.aid.isBlank()) {
                    fmtAddTextInLine.putInt(
                        PrinterConfig.addTextInLine.FontSize.BundleName,
                        PrinterConfig.addTextInLine.FontSize.NORMAL_24_24
                    )
                    fmtAddTextInLine.putString(
                        PrinterConfig.addTextInLine.GlobalFont.BundleName,
                        PrinterFonts.path + PrinterFonts.FONT_AGENCYR
                    )
                    logger("PS_aid", (printer?.status).toString(), "e")
                    printer?.addTextInLine(
                        fmtAddTextInLine,
                        "AID:${printerReceiptData.aid}",
                        "",
                        "",
                        PrinterConfig.addTextInLine.mode.Devide_flexible
                    )

                    fmtAddTextInLine.putInt(
                        PrinterConfig.addTextInLine.FontSize.BundleName,
                        PrinterConfig.addTextInLine.FontSize.NORMAL_24_24
                    )
                    fmtAddTextInLine.putString(
                        PrinterConfig.addTextInLine.GlobalFont.BundleName,
                        PrinterFonts.path + PrinterFonts.FONT_AGENCYR
                    )
                }

                //   printer.addTextInLine( fmtAddTextInLine, "L & R", "", "Divide Equally", 0);
                //   printer.addTextInLine( fmtAddTextInLine, "L & R", "", "Divide Equally", 0);
                logger("PS_Tc", (printer?.status).toString(), "e")
                if (!printerReceiptData.tc.isBlank()) {
                    printer?.addTextInLine(
                        fmtAddTextInLine,
                        "TC:${printerReceiptData.tc}",
                        "",
                        "",
                        PrinterConfig.addTextInLine.mode.Devide_flexible
                    )
                }

            }

            // region =======Setting amount on Sale charge slip ==============
            printSeperator(format)
            fmtAddTextInLine.putInt(
                PrinterConfig.addTextInLine.FontSize.BundleName,
                PrinterConfig.addTextInLine.FontSize.NORMAL_24_24
            )
            fmtAddTextInLine.putString(
                PrinterConfig.addTextInLine.GlobalFont.BundleName,
                PrinterFonts.path + PrinterFonts.FONT_AGENCYR
            )
            var saleAmount = "%.2f".format(printerReceiptData.baseAmmount.toDouble())


            val totalAmount = "%.2f".format(printerReceiptData.totalAmmount.toDouble())
            val cashAmount = if (printerReceiptData.cashBackAmount.isEmpty()) {
                "%.2f".format("0".toDouble())
            } else {
                "%.2f".format(printerReceiptData.cashBackAmount.toDouble())
            }
            val tipAmount =
                if (printerReceiptData.tipAmmount.isEmpty()) {
                    "%.2f".format("0".toDouble())
                } else {
                    "%.2f".format(printerReceiptData.tipAmmount.toDouble())
                }

            if (isTipAllowed && tipAmount.toDouble() > 0) {
                if (printerReceiptData.transactionType != TransactionType.TIP_SALE.type) {
                    saleAmount = "%.2f".format((saleAmount.toDouble() - tipAmount.toDouble()))
                }
            }
            val tpt = TerminalParameterTable.selectFromSchemeTable()
            when (printerReceiptData.transactionType) {
                TransactionType.SALE_WITH_CASH.type -> {
                    printer?.addTextInLine(
                        fmtAddTextInLine,
                        "SALE AMOUNT   :    ${getCurrencySymbol(tpt)}  ${
                            MoneyUtil.fen2yuan(
                                saleAmount.toDouble().toLong()
                            )
                        }",
                        "",
                        "",
                        PrinterConfig.addTextInLine.mode.Devide_flexible
                    )
                    printer?.addTextInLine(
                        fmtAddTextInLine,
                        "CASH AMOUNT   :    ${getCurrencySymbol(tpt)}  ${
                            MoneyUtil.fen2yuan(
                                cashAmount.toDouble().toLong()
                            )
                        }",
                        "",
                        "",
                        PrinterConfig.addTextInLine.mode.Devide_flexible
                    )
                    printer?.addTextInLine(
                        fmtAddTextInLine,
                        "TOTAL AMOUNT  :    ${getCurrencySymbol(tpt)}  ${
                            MoneyUtil.fen2yuan(
                                totalAmount.toDouble().toLong()
                            )
                        }",
                        "",
                        "",
                        PrinterConfig.addTextInLine.mode.Devide_flexible
                    )
                }
                TransactionType.SALE.type -> {
                    printer?.addTextInLine(
                        fmtAddTextInLine,
                        "SALE AMOUNT   :    ${getCurrencySymbol(tpt)}  ${
                            MoneyUtil.fen2yuan(
                                saleAmount.toDouble().toLong()
                            )
                        }",
                        "",
                        "",
                        PrinterConfig.addTextInLine.mode.Devide_flexible
                    )
                    if (isTipAllowed && printerReceiptData.tipAmmount.toDouble() <= 0) {
                        printer?.addTextInLine(
                            fmtAddTextInLine,
                            "TIP AMOUNT    :    .............",
                            "",
                            "",
                            PrinterConfig.addTextInLine.mode.Devide_flexible
                        )
                    } else if (isTipAllowed && tipAmount.toDouble() > 0) {
                        printer?.addTextInLine(
                            fmtAddTextInLine,
                            "TIP AMOUNT    :    ${getCurrencySymbol(tpt)}  ${
                                MoneyUtil.fen2yuan(
                                    tipAmount.toDouble().toLong()
                                )
                            }",
                            "",
                            "",
                            PrinterConfig.addTextInLine.mode.Devide_flexible
                        )
                    }
                    printer?.addTextInLine(
                        fmtAddTextInLine,
                        "TOTAL AMOUNT  :    ${getCurrencySymbol(tpt)}  ${
                            MoneyUtil.fen2yuan(
                                totalAmount.toDouble().toLong()
                            )
                        }",
                        "",
                        "",
                        PrinterConfig.addTextInLine.mode.Devide_flexible
                    )
                }
                TransactionType.TIP_SALE.type -> {
                    saleAmount = "%.2f".format((saleAmount.toDouble() - tipAmount.toDouble()))

                    printer?.addTextInLine(
                        fmtAddTextInLine, "SALE AMOUNT   :    ${getCurrencySymbol(tpt)}  ${
                            MoneyUtil.fen2yuan(saleAmount.toDouble().toLong())
                        }", "", "", PrinterConfig.addTextInLine.mode.Devide_flexible
                    )
                    printer?.addTextInLine(
                        fmtAddTextInLine,
                        "TIP AMOUNT    :    ${getCurrencySymbol(tpt)}  ${
                            MoneyUtil.fen2yuan(
                                tipAmount.toDouble().toLong()
                            )
                        }",
                        "",
                        "",
                        PrinterConfig.addTextInLine.mode.Devide_flexible
                    )
                    printer?.addTextInLine(
                        fmtAddTextInLine,
                        "TOTAL AMOUNT  :    ${getCurrencySymbol(tpt)}  ${
                            MoneyUtil.fen2yuan(
                                totalAmount.toDouble().toLong()
                            )
                        }",
                        "",
                        "",
                        PrinterConfig.addTextInLine.mode.Devide_flexible
                    )
                }
                else -> {
                    printer?.addTextInLine(
                        fmtAddTextInLine, "BASE AMOUNT   :    ${getCurrencySymbol(tpt)}  ${
                            MoneyUtil.fen2yuan(totalAmount.toDouble().toLong())
                        }", "", "", PrinterConfig.addTextInLine.mode.Devide_flexible
                    )
                    printer?.addTextInLine(
                        fmtAddTextInLine,
                        "TOTAL AMOUNT  :    ${getCurrencySymbol(tpt)}  ${
                            MoneyUtil.fen2yuan(
                                totalAmount.toDouble().toLong()
                            )
                        }",
                        "",
                        "",
                        PrinterConfig.addTextInLine.mode.Devide_flexible
                    )
                }
            }

            printSeperator(format)

            // endregion=======Setting amount on Sale charge slip ==============

            format.putInt(
                PrinterConfig.addText.FontSize.BundleName,
                PrinterConfig.addText.FontSize.NORMAL_24_24
            )
            format.putInt(
                PrinterConfig.addText.Alignment.BundleName,
                PrinterConfig.addText.Alignment.CENTER
            )
            if ((printerReceiptData.operationType == DetectCardType.CONTACT_LESS_CARD_TYPE.cardTypeName || printerReceiptData.operationType == DetectCardType.CONTACT_LESS_CARD_WITH_MAG_TYPE.cardTypeName) && (printerReceiptData.nocvm) && !printerReceiptData.isPinverified) {
                centerText(format, printerReceiptData.ctlsCaption)
            } else if (printerReceiptData.isPinverified && !(printerReceiptData.nocvm)) {
                //  printer?.addText(format, pinVerifyMsg)
                centerText(format, pinVerifyMsg)
                centerText(format, signatureMsg)
            } else if (!(printerReceiptData.nocvm)) {
                // -------(Remove in New VFservice 3.0)  printer?.feedLine(2)
                alignLeftRightText(format, pinVerifyMsg, "", "")
                alignLeftRightText(format, signatureMsg, "", "")
                // -------(Remove in New VFservice 3.0)  printer?.feedLine(2)
                // printer?.addText(format, pinVerifyMsg)
                //  printer?.addText(format, signatureMsg)
            }

            centerText(format, printerReceiptData.cardHolderName)
            //  printer?.addText(format, printerReceiptData.cardHolderName)


            val ipt =
                IssuerParameterTable.selectFromIssuerParameterTable(AppPreference.WALLET_ISSUER_ID)
            val chunks: List<String>? = ipt?.volletIssuerDisclammer?.let { chunkTnC(it) }
            if (chunks != null) {
                for (st in chunks) {
                    logger("TNC", st, "e")
                    //  printer?.addText(format,st)
                    alignLeftRightText(format, st, "", "")
                }
            }
            format.putInt(
                PrinterConfig.addText.FontSize.BundleName,
                0
            )
            //   printer?.addText(format, ipt?.volletIssuerDisclammer)
            printer?.addText(format, copyType.pName)
            printer?.addText(format, footerText[0])
            printer?.addText(format, footerText[1])

            printLogo("BH.bmp")

            format.putInt(
                PrinterConfig.addText.FontSize.BundleName,
                0
            )
            format.putInt(
                PrinterConfig.addText.Alignment.BundleName,
                PrinterConfig.addText.Alignment.CENTER
            )
            printer?.addText(format, "App Version : ${BuildConfig.VERSION_NAME}")
            ////    printer?.addText(format, "---------X-----------X----------")
            printer?.feedLine(4)

            // start print here
            printer?.startPrint(
                IPrintListener(
                    this,
                    context,
                    copyType,
                    printerReceiptData,
                    printerCallback
                )
            )
        } catch (ex: DeadObjectException) {
            ex.printStackTrace()
            failureImpl(
                context as Activity,
                "Printer Service stopped.",
                "Please take chargeslip from the Report menu."
            )
        } catch (e: RemoteException) {
            e.printStackTrace()
            failureImpl(
                context as Activity,
                "Printer Service stopped.",
                "Please take chargeslip from the Report menu."
            )
        } catch (ex: Exception) {
            ex.printStackTrace()
            failureImpl(
                context as Activity,
                "Printer Service stopped.",
                "Please take chargeslip from the Report menu."
            )
        } finally {
            //   VFService.connectToVFService(VerifoneApp.appContext)
        }
    }

    fun printDetailReport(
        batch: List<BatchFileDataTable>,
        context: Context?,
        printCB: (Boolean) -> Unit
    ) {
        try {
            val pp = printer?.status
            Log.e("Printer Status", pp.toString())
            if (pp == 0) {
                //-----------------------------------------------
                setLogoAndHeader()
                //  ------------------------------------------
                val appVersion = BuildConfig.VERSION_NAME

                val td = System.currentTimeMillis()
                val formatdate = SimpleDateFormat("dd/MM/yyyy", Locale.ENGLISH)
                val formattime = SimpleDateFormat("HH:mm:ss", Locale.ENGLISH)

                val date = formatdate.format(td)
                val time = formattime.format(td)
                val tpt = TerminalParameterTable.selectFromSchemeTable()

                alignLeftRightText(textInLineFormatBundle, "DATE : $date", "TIME : $time")
                alignLeftRightText(
                    textInLineFormatBundle,
                    "MID : ${tpt?.merchantId}",
                    "TID : ${tpt?.terminalId}"
                )

                alignLeftRightText(textInLineFormatBundle, "BATCH NO : ${tpt?.batchNumber}", "")

                centerText(textFormatBundle, "DETAIL REPORT", true)

                if (batch.isEmpty()) {
                    alignLeftRightText(textInLineFormatBundle, "Total Transaction", "0")
                } else {
                    alignLeftRightText(textInLineFormatBundle, "TRANS-TYPE", "AMOUNT")
                    alignLeftRightText(textInLineFormatBundle, "ISSUER", "PAN/CID")
                    alignLeftRightText(textInLineFormatBundle, "DATE-TIME", "INVOICE")
                    printSeperator(textFormatBundle)
                    val totalMap = mutableMapOf<Int, SummeryTotalType>()
                    val deformatter = SimpleDateFormat("yyMMdd HHmmss", Locale.ENGLISH)
                    for (b in batch) {
                        //  || b.transactionType == TransactionType.VOID_PREAUTH.type
                        if (b.transactionType == TransactionType.PRE_AUTH.type) continue  // Do not add pre auth transactions

                        if (totalMap.containsKey(b.transactionType)) {
                            val x = totalMap[b.transactionType]
                            if (x != null) {
                                x.count += 1
                                x.total += b.transactionalAmmount.toLong()
                            }
                        } else {
                            totalMap[b.transactionType] =
                                SummeryTotalType(1, b.transactionalAmmount.toLong())
                        }
                        val transAmount = "%.2f".format(b.transactionalAmmount.toDouble() / 100)
                        alignLeftRightText(
                            textInLineFormatBundle,
                            transactionType2Name(b.transactionType),
                            transAmount
                        )
                        if (b.transactionType == TransactionType.VOID_PREAUTH.type) {
                            alignLeftRightText(
                                textInLineFormatBundle,
                                b.cardType,
                                panMasking(b.encryptPan, "0000********0000")
                            )
                        } else {
                            alignLeftRightText(
                                textInLineFormatBundle,
                                b.cardType,
                                panMasking(b.cardNumber, "0000********0000")
                            )
                        }
                        if (b.transactionType == TransactionType.OFFLINE_SALE.type || b.transactionType == TransactionType.VOID_OFFLINE_SALE.type) {
                            try {
                                val dat = "${b.printDate} - ${b.time}"
                                alignLeftRightText(
                                    textInLineFormatBundle,
                                    dat,
                                    invoiceWithPadding(b.invoiceNumber)
                                )
                            } catch (ex: Exception) {
                                ex.printStackTrace()
                            }

                        } else {
                            val timee = b.time
                            val timeFormat = SimpleDateFormat("HHmmss", Locale.getDefault())
                            val timeFormat2 = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
                            var formattedTime = ""
                            try {
                                val t1 = timeFormat.parse(timee)
                                formattedTime = timeFormat2.format(t1)
                                Log.e("Time", formattedTime)
                                val dat = "${b.transactionDate} - $formattedTime"
                                alignLeftRightText(
                                    textInLineFormatBundle,
                                    dat,
                                    invoiceWithPadding(b.invoiceNumber)
                                )
                                //alignLeftRightText(textInLineFormatBundle," "," ")
                            } catch (ex: Exception) {
                                ex.printStackTrace()
                            }
                        }

                        printSeperator(textFormatBundle)
                    }
                    printSeperator(textFormatBundle)
                    textFormatBundle.putInt(
                        PrinterConfig.addText.FontSize.BundleName,
                        0
                    )
                    centerText(textFormatBundle, "***TOTAL TRANSACTIONS***")

                    val sortedMap = totalMap.toSortedMap(compareByDescending { it })
                    /* for ((k, v) in sortedMap) {
                         alignLeftRightText(
                             textInLineFormatBundle,
                             "${transactionType2Name(k)} = ${v.count}",
                             "Rs %.2f".format(v.total.toDouble() / 100)
                         )
                     }*/

                    for ((k, m) in sortedMap) {
                        /* alignLeftRightText(
                             textInLineFormatBundle,
                             "${transactionType2Name(k).toUpperCase(Locale.ROOT)}${"     =" + m.count}",
                             "Rs.     ${"%.2f".format(((m.total).toDouble() / 100))}"

                         )*/

                        alignLeftRightText(
                            textInLineFormatBundle,
                            transactionType2Name(k).toUpperCase(Locale.ROOT),
                            "Rs.     ${"%.2f".format(((m.total).toDouble() / 100))}",
                            "  =  " + m.count

                        )

                    }

                }
                printSeperator(textFormatBundle)
                printer?.addText(textFormatBundle, "--------------------------------")
                centerText(textFormatBundle, "App Version :$appVersion")
                //// centerText(textFormatBundle, "---------X-----------X----------")
                printer?.feedLine(4)
                // start print here
                printer?.startPrint(object : PrinterListener.Stub() {
                    override fun onFinish() {
                        Log.e("DEATIL REPORT", "SUCESS__")
                        printCB(true)
                    }

                    override fun onError(error: Int) {
                        Log.e("DEATIL REPORT", "FAIL__")
                        printCB(false)
                    }


                })
            }
        } catch (ex: DeadObjectException) {
            ex.printStackTrace()
            failureImpl(
                context as Activity,
                "Printer Service stopped.",
                "Please take chargeslip from the Report menu."
            )
        } catch (e: RemoteException) {
            e.printStackTrace()
            failureImpl(
                context as Activity,
                "Printer Service stopped.",
                "Please take chargeslip from the Report menu."
            )
        } catch (ex: Exception) {
            ex.printStackTrace()
            failureImpl(
                context as Activity,
                "Printer Service stopped.",
                "Please take chargeslip from the Report menu."
            )
        } finally {
            //   VFService.connectToVFService(VerifoneApp.appContext)
        }
    }

    fun printDetailReportupdate(
        batch: MutableList<BatchFileDataTable>,
        context: Context?,
        printCB: (Boolean) -> Unit
    ) {
        try {
            val pp = printer?.status
            Log.e("Printer Status", pp.toString())
            if (pp == 0) {

                val appVersion = BuildConfig.VERSION_NAME
                val tpt = TerminalParameterTable.selectFromSchemeTable()

                batch.sortBy { it.hostTID }

                if (batch.isEmpty()) {
                    // alignLeftRightText(textInLineFormatBundle, "MID : ${tpt?.merchantId}", "TID : ${tpt?.terminalId}")
                } else {
                    //-----------------------------------------------
                    setLogoAndHeader()
                    //  ------------------------------------------
                    val td = System.currentTimeMillis()
                    val formatdate = SimpleDateFormat("dd/MM/yyyy", Locale.ENGLISH)
                    val formattime = SimpleDateFormat("HH:mm:ss", Locale.ENGLISH)

                    val date = formatdate.format(td)
                    val time = formattime.format(td)


                    alignLeftRightText(textInLineFormatBundle, "DATE:$date", "TIME:$time")

                    centerText(textFormatBundle, "DETAIL REPORT", true)
                    alignLeftRightText(
                        textInLineFormatBundle,
                        "MID:${batch[0].hostMID}",
                        "TID:${batch[0].hostTID}"
                    )
                    alignLeftRightText(
                        textInLineFormatBundle,
                        "BATCH NO:${batch[0].hostBatchNumber}",
                        ""
                    )
                    printSeperator(textFormatBundle)
                }

                if (batch.isEmpty()) {
                    // alignLeftRightText(textInLineFormatBundle, "Total Transaction", "0")
                } else {
                    alignLeftRightText(textInLineFormatBundle, "TRANS-TYPE", "AMOUNT")
                    alignLeftRightText(textInLineFormatBundle, "ISSUER", "PAN/CID")
                    alignLeftRightText(textInLineFormatBundle, "DATE-TIME", "INVOICE")
                    printSeperator(textFormatBundle)

                    val totalMap = mutableMapOf<Int, SummeryTotalType>()
                    val deformatter = SimpleDateFormat("yyMMdd HHmmss", Locale.ENGLISH)

                    var frequency = 0
                    var count = 0
                    var lastfrequecny = 0
                    var hasfrequency = false
                    var updatedindex = 0
                    var iteration = 0
                    val frequencylist = mutableListOf<String>()
                    val tidlist = mutableListOf<String>()

                    for (item in batch) {
                        tidlist.add(item.hostTID)
                    }
                    for (item in tidlist.distinct()) {
                        println(
                            "Frequency of item" + item + ": " + Collections.frequency(
                                tidlist,
                                item
                            )
                        )
                        frequencylist.add("" + Collections.frequency(tidlist, item))
                    }

                    iteration = tidlist.distinct().size - 1

                    for (b in batch) {
                        //  || b.transactionType == TransactionType.VOID_PREAUTH.type
                        if (b.transactionType == TransactionType.PRE_AUTH.type) continue  // Do not add pre auth transactions

                        if (b.transactionType == TransactionType.EMI_SALE.type || b.transactionType == TransactionType.BRAND_EMI.type || b.transactionType == TransactionType.BRAND_EMI_BY_ACCESS_CODE.type) {
                            b.transactionType = TransactionType.EMI_SALE.type
                        }

                        if (b.transactionType == TransactionType.TEST_EMI.type) {
                            b.transactionType = TransactionType.SALE.type
                        }

                        count++
                        if (updatedindex <= frequencylist.size - 1)
                            frequency = frequencylist[updatedindex].toInt() + lastfrequecny


                        if (totalMap.containsKey(b.transactionType)) {
                            val x = totalMap[b.transactionType]
                            if (x != null) {
                                x.count += 1
                                x.total += b.transactionalAmmount.toLong()
                            }
                        } else {
                            totalMap[b.transactionType] =
                                SummeryTotalType(1, b.transactionalAmmount.toLong())
                        }
                        val transAmount = "%.2f".format(b.transactionalAmmount.toDouble() / 100)
                        alignLeftRightText(
                            textInLineFormatBundle,
                            transactionType2Name(b.transactionType),
                            transAmount
                        )
                        if (b.transactionType == TransactionType.VOID_PREAUTH.type) {
                            alignLeftRightText(
                                textInLineFormatBundle,
                                b.cardType,
                                panMasking(b.encryptPan, "0000********0000")
                            )
                        } else {
                            alignLeftRightText(
                                textInLineFormatBundle, b.cardType,
                                panMasking(b.cardNumber, "0000********0000")
                            )
                        }
                        if (b.transactionType == TransactionType.OFFLINE_SALE.type || b.transactionType == TransactionType.VOID_OFFLINE_SALE.type) {
                            try {
                                val dat = "${b.printDate} - ${b.time}"
                                alignLeftRightText(
                                    textInLineFormatBundle,
                                    dat,
                                    invoiceWithPadding(b.hostInvoice)
                                )
                            } catch (ex: Exception) {
                                ex.printStackTrace()
                            }

                        } else {
                            val timee = b.time
                            val timeFormat = SimpleDateFormat("HHmmss", Locale.getDefault())
                            val timeFormat2 = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
                            var formattedTime = ""
                            try {
                                val t1 = timeFormat.parse(timee)
                                formattedTime = timeFormat2.format(t1)
                                Log.e("Time", formattedTime)
                                val dat = "${b.transactionDate} - $formattedTime"
                                alignLeftRightText(
                                    textInLineFormatBundle,
                                    dat,
                                    invoiceWithPadding(b.hostInvoice)
                                )
                                //alignLeftRightText(textInLineFormatBundle," "," ")
                            } catch (ex: Exception) {
                                ex.printStackTrace()
                            }
                        }

                        printSeperator(textFormatBundle)
                        if (frequency == count) {
                            lastfrequecny = frequency
                            hasfrequency = true
                            updatedindex++
                        } else {
                            hasfrequency = false
                        }
                        if (hasfrequency) {
                            // printSeperator(textFormatBundle)
                            centerText(textFormatBundle, "***TOTAL TRANSACTIONS***")
                            val sortedMap = totalMap.toSortedMap(compareByDescending { it })
                            /* for ((k, v) in sortedMap) {
                             alignLeftRightText(
                                 textInLineFormatBundle,
                                 "${transactionType2Name(k)} = ${v.count}",
                                 "Rs %.2f".format(v.total.toDouble() / 100)
                             )
                         }*/

                            for ((k, m) in sortedMap) {
                                /* alignLeftRightText(
                                 textInLineFormatBundle,
                                 "${transactionType2Name(k).toUpperCase(Locale.ROOT)}${"     =" + m.count}",
                                 "Rs.     ${"%.2f".format(((m.total).toDouble() / 100))}"

                             )*/

                                alignLeftRightText(
                                    textInLineFormatBundle,
                                    transactionType2Name(k).toUpperCase(Locale.ROOT),
                                    "%.2f".format(((m.total).toDouble() / 100)),
                                    "=" + m.count+" ${getCurrencySymbol(tpt)}"

                                )

                            }

                            if (iteration > 0) {
                                printSeperator(textFormatBundle)
                                alignLeftRightText(
                                    textInLineFormatBundle,
                                    "MID:${batch[frequency].hostMID}",
                                    "TID:${batch[frequency].hostTID}"
                                )
                                alignLeftRightText(
                                    textInLineFormatBundle,
                                    "BATCH NO:${batch[frequency].hostBatchNumber}",
                                    ""
                                )
                                printSeperator(textFormatBundle)
                                iteration--
                            }

                            totalMap.clear()
                        }
                    }

                }
                // region === Below code is execute when digi txns are available on POS
                val digiPosDataList =
                    DigiPosDataTable.selectDigiPosDataAccordingToTxnStatus(EDigiPosPaymentStatus.Approved.desciption) as ArrayList<DigiPosDataTable>

                if (digiPosDataList.isNotEmpty()) {
                    printSeperator(textFormatBundle)
                    // centerText(textFormatBundle, "---------X-----------X----------")
                    centerText(textFormatBundle, "Digi Pos Detail Report", true)
                    tpt?.terminalId?.let { centerText(textFormatBundle, "TID : $it") }
                    printSeperator(textFormatBundle)
                    // Txn description
                    alignLeftRightText(textInLineFormatBundle, "MODE", "AMOUNT(INR)")
                    alignLeftRightText(textInLineFormatBundle, "PartnetTxnId", "DATE-TIME")
                    alignLeftRightText(textInLineFormatBundle, "mTxnId", "pgwTxnId")
                    printSeperator(textFormatBundle)
                    //Txn Detail
                    for (digiPosData in digiPosDataList) {
                        alignLeftRightText(
                            textInLineFormatBundle,
                            digiPosData.paymentMode,
                            digiPosData.amount
                        )
                        alignLeftRightText(
                            textInLineFormatBundle,
                            digiPosData.partnerTxnId,
                            digiPosData.txnDate + "  " + digiPosData.txnTime
                        )
                        alignLeftRightText(
                            textInLineFormatBundle,
                            digiPosData.mTxnId,
                            digiPosData.pgwTxnId
                        )
                        printer?.addText(textFormatBundle, "--------------------------------")
                    }
                    //   DigiPosDataTable.deletAllRecordAccToTxnStatus(EDigiPosPaymentStatus.Approved.desciption)
                }
                //endregion
                if (batch.isNotEmpty()) {
                    printer?.addText(textFormatBundle, "--------------------------------")
                    centerText(textFormatBundle, "Bonushub")
                    centerText(textFormatBundle, "App Version :$appVersion")
                    //  centerText(textFormatBundle, "---------X-----------X----------")
                    printer?.feedLine(4)
                }
                // start print here
                printer?.startPrint(object : PrinterListener.Stub() {
                    override fun onFinish() {
                        Log.e("DEATIL REPORT", "SUCESS__")
                        printCB(true)
                    }

                    override fun onError(error: Int) {
                        Log.e("DEATIL REPORT", "FAIL__")
                        printCB(false)
                    }


                })
            }
        } catch (ex: DeadObjectException) {
            ex.printStackTrace()
            failureImpl(
                context as Activity,
                "Printer Service stopped.",
                "Please take chargeslip from the Report menu."
            )
        } catch (e: RemoteException) {
            e.printStackTrace()
            failureImpl(
                context as Activity,
                "Printer Service stopped.",
                "Please take chargeslip from the Report menu."
            )
        } catch (ex: Exception) {
            ex.printStackTrace()
            failureImpl(
                context as Activity,
                "Printer Service stopped.",
                "Please take chargeslip from the Report menu."
            )
        } finally {
            //   VFService.connectToVFService(VerifoneApp.appContext)
        }
    }


    fun printReversal(context: Context?, field60Data: String, callback: (String) -> Unit) {
        val isoW = AppPreference.getReversal()

        if (isoW != null) {
            var hostBankID: String? = null
            var hostIssuerID: String? = null
            var hostMID: String? = null
            var hostTID: String? = null
            var hostBatchNumber: String? = null
            var hostRoc: String? = null
            var hostInvoice: String? = null
            var hostCardType: String? = null
            try {

                val f60DataList = field60Data.split('|')
                //   Auto settle flag | Bank id| Issuer id | MID | TID | Batch No | Stan | Invoice | Card Type
                // 0|1|51|000000041501002|41501369|000150|260|000260|RUPAY|
                try {

                    hostBankID = f60DataList[1]
                    hostIssuerID = f60DataList[2]
                    hostMID = f60DataList[3]
                    hostTID = f60DataList[4]
                    hostBatchNumber = f60DataList[5]
                    hostRoc = f60DataList[6]
                    hostInvoice = f60DataList[7]
                    hostCardType = f60DataList[8]

                    println(
                        "Server MID and TID and batchumber and roc and cardType is" +
                                "MID -> " + hostMID + "TID -> " + hostTID + "Batchnumber -> " + hostBatchNumber + "ROC ->" + hostRoc + "CardType -> " + hostCardType
                    )

                    //  batchFileData
                } catch (ex: Exception) {
                    ex.printStackTrace()
                    //  batchFileData
                }

                //Changes By manish Kumar
                //If in Respnse field 60 data comes Auto settle flag | Bank id| Issuer id | MID | TID | Batch No | Stan | Invoice | Card Type
                // then show response data otherwise show data available in database
                //From mid to hostMID (coming from field 60)
                //From tid to hostTID (coming from field 60)
                //From batchNumber to hostBatchNumber (coming from field 60)
                //From roc to hostRoc (coming from field 60)
                //From invoiceNumber to hostInvoice (coming from field 60)
                //From cardType to hostCardType (coming from field 60)

                val roc = isoW.isoMap[11]?.rawData ?: ""
                val tid = isoW.isoMap[41]?.parseRaw2String() ?: ""
                val mid = isoW.isoMap[42]?.parseRaw2String() ?: ""
                val batchdata = isoW.isoMap[60]?.parseRaw2String() ?: ""
                val batch = batchdata.split("|")[0]
                val cardType = isoW.additionalData["cardType"] ?: ""

                val hostMID = if (hostMID?.isNotBlank() == true) {
                    hostMID
                } else {
                    mid
                }
                val hostTID = if (hostTID?.isNotBlank() == true) {
                    hostTID
                } else {
                    tid
                }
                val hostBatchNumber = if (hostBatchNumber?.isNotBlank() == true) {
                    hostBatchNumber
                } else {
                    batch
                }
                val hostRoc = if (hostRoc?.isNotBlank() == true) {
                    hostRoc
                } else {
                    roc
                }
                val hostCardType = if (hostCardType?.isNotBlank() == true) {
                    hostCardType
                } else {
                    cardType
                }


                setLogoAndHeader()

                val cal = Calendar.getInstance()
                cal.timeInMillis = isoW.timeStamp
                val yr = cal.get(Calendar.YEAR).toString()
                val of12 = isoW.isoMap[12]?.rawData ?: ""
                val of13 = isoW.isoMap[13]?.rawData ?: ""

                val d = of13 + yr


                var amountStr = isoW.isoMap[4]?.rawData ?: "0"
                val amt = amountStr.toFloat() / 100
                amountStr = "%.2f".format(amt)

                val date = "${d.substring(2, 4)}/${d.substring(0, 2)}/${d.substring(4, d.length)}"
                val time =
                    "${of12.substring(0, 2)}:${of12.substring(2, 4)}:${
                        of12.substring(
                            4,
                            of12.length
                        )
                    }"
                alignLeftRightText(textInLineFormatBundle, "DATE:${date}", "TIME:${time}")
                alignLeftRightText(textInLineFormatBundle, "MID:${hostMID}", "TID:${hostTID}")
                alignLeftRightText(
                    textInLineFormatBundle,
                    "BATCH NO:${hostBatchNumber}",
                    "ROC:${invoiceWithPadding(hostRoc)}"
                )

                centerText(textFormatBundle, "TRANSACTION FAILED")


                val card = isoW.additionalData["pan"] ?: ""
                if (card.isNotEmpty())
                    alignLeftRightText(
                        textInLineFormatBundle,
                        "CARD NO:$card",
                        hostCardType
                    )//chip,swipe,cls


                val tvr = isoW.additionalData["tvr"] ?: ""
                val tsi = isoW.additionalData["tsi"] ?: ""
                var aid = isoW.additionalData["aid"] ?: ""

                printer?.addText(textFormatBundle, "--------------------------------")

                if (tsi.isNotEmpty() && tvr.isNotEmpty()) {
                    alignLeftRightText(textInLineFormatBundle, "TVR:${tvr}", "TSI:${tsi}")
                }


                if (aid.isNotEmpty()) {
                    aid = "AID:$aid"
                    alignLeftRightText(textInLineFormatBundle, aid, "")
                }

                printSeperator(textFormatBundle)
                centerText(textFormatBundle, "TOTAL AMOUNT : ${getCurrencySymbol(TerminalParameterTable.selectFromSchemeTable())} $amountStr")
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
                context?.getString(R.string.something_went_wrong)?.let { callback(it) }
                ex.printStackTrace()
                failureImpl(
                    context as Activity,
                    "Printer Service stopped.",
                    "Please take chargeslip from the Report menu."
                )
            } catch (e: RemoteException) {
                context?.getString(R.string.something_went_wrong)?.let { callback(it) }
                e.printStackTrace()
                failureImpl(
                    context as Activity,
                    "Printer Service stopped.",
                    "Please take chargeslip from the Report menu."
                )
            } catch (ex: Exception) {
                context?.getString(R.string.something_went_wrong)?.let { callback(it) }
                ex.printStackTrace()
                failureImpl(
                    context as Activity,
                    "Printer Service stopped.",
                    "Please take chargeslip from the Report menu."
                )
            }
        } else {
            callback("No Cancel Receipt Found")
        }
    }

    fun printSMSUPIChagreSlip(
        digiPosData: DigiPosDataTable,
        copyType: EPrintCopyType,
        context: Context?,
        printerCallback: (Boolean, Int) -> Unit
    ) {
        //  printer=null
        try {
            val terminalData = TerminalParameterTable.selectFromSchemeTable()
            val format = Bundle()
            // bundle formate for AddTextInLine
            val fmtAddTextInLine = Bundle()

            //   printLogo("smart_hub.bmp")
            setLogoAndHeader(DIGI_SMART_HUB_LOGO)
            /* format.putInt(
                 PrinterConfig.addText.FontSize.BundleName,
                 PrinterConfig.addText.FontSize.NORMAL_24_24
             )
             format.putInt(
                 PrinterConfig.addText.Alignment.BundleName,
                 PrinterConfig.addText.Alignment.CENTER
             )
             //  logger("PS_H1", (printer?.status).toString(), "e")
             printer?.addText(format, terminalData?.receiptHeaderOne) // header1
             //   logger("PS_H2", (printer?.status).toString(), "e")
             printer?.addText(format, terminalData?.receiptHeaderTwo) //header2
             //   logger("PS_H3", (printer?.status).toString(), "e")
             printer?.addText(format, terminalData?.receiptHeaderThree) //header3*/
            printSeperator(format)

            fmtAddTextInLine.putInt(
                PrinterConfig.addTextInLine.FontSize.BundleName,
                PrinterConfig.addTextInLine.FontSize.NORMAL_24_24
            )
            fmtAddTextInLine.putString(
                PrinterConfig.addTextInLine.GlobalFont.BundleName,
                PrinterFonts.path + PrinterFonts.FONT_AGENCYR)

            printer?.addTextInLine(
                fmtAddTextInLine,
                "DATE:${digiPosData.txnDate}",
                "",
                "TIME:${digiPosData.txnTime}",
                PrinterConfig.addTextInLine.mode.Devide_flexible
            )
            printer?.addTextInLine(
                fmtAddTextInLine,
                "TID:${terminalData?.terminalId}",
                "",
                "",
                PrinterConfig.addTextInLine.mode.Devide_flexible
            )

            printer?.addTextInLine(
                fmtAddTextInLine,
                "Partner Txn Id:${digiPosData.partnerTxnId}",
                "",
                "",
                PrinterConfig.addTextInLine.mode.Devide_flexible
            )

            printer?.addTextInLine(
                fmtAddTextInLine,
                "mTxnId:${digiPosData.mTxnId}",
                "",
                "",
                PrinterConfig.addTextInLine.mode.Devide_flexible
            )

            printer?.addTextInLine(
                fmtAddTextInLine,
                "PgwTxnId:${digiPosData.pgwTxnId}",
                "",
                "",
                PrinterConfig.addTextInLine.mode.Devide_flexible
            )


            printSeperator(format)

            val str = "Txn Status:${digiPosData.txnStatus}"
            centerText(fmtAddTextInLine, str, true)
            centerText(fmtAddTextInLine, "Txn Amount :  ${getCurrencySymbol(TerminalParameterTable.selectFromSchemeTable())} ${digiPosData.amount}", true)

            printSeperator(format)


            printer?.addTextInLine(
                fmtAddTextInLine,
                "Mob:${digiPosData.customerMobileNumber}",
                "",
                "Mode:${digiPosData.paymentMode}",
                PrinterConfig.addTextInLine.mode.Devide_flexible
            )

            printer?.addText(format, copyType.pName)
            printer?.addText(format, footerText[0])
            printer?.addText(format, footerText[1])

            printLogo("BH.bmp")

            format.putInt(
                PrinterConfig.addText.FontSize.BundleName,
              0
            )
            format.putInt(
                PrinterConfig.addText.Alignment.BundleName,
                PrinterConfig.addText.Alignment.CENTER
            )
            printer?.addText(format, "App Version:${BuildConfig.VERSION_NAME}")

            //
            //   printer?.addText(format, "---------X-----------X----------")
            printer?.feedLine(4)

            // start print here
            printer?.startPrint(
                ISmSUpiPrintListener(
                    this,
                    context,
                    copyType,
                    digiPosData,
                    printerCallback
                )
            )
        } catch (ex: DeadObjectException) {
            ex.printStackTrace()
            failureImpl(
                context as Activity,
                "Printer Service stopped.",
                "Please take charge slip from the Report menu."
            )
        } catch (e: RemoteException) {
            e.printStackTrace()
            failureImpl(
                context as Activity,
                "Printer Service stopped.",
                "Please take charge slip from the Report menu."
            )
        } catch (ex: Exception) {
            ex.printStackTrace()
            failureImpl(
                context as Activity,
                "Printer Service stopped.",
                "Please take charge slip from the Report menu."
            )
        } finally {
            //   VFService.connectToVFService(VerifoneApp.appContext)
        }
    }


    private fun setHeaderWithLogo(
        format: Bundle,
        img: String?,
        headers: ArrayList<String>,
        context: Context? = null
    ) {
        if (img != null)
            printLogo(img)
        centerText(format, headers[0].trim())
        centerText(format, headers[1].trim())
        centerText(format, headers[2].trim())
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

    private fun customSpaceLine(format: Bundle, text: String) {

        format.putInt(
            PrinterConfig.addText.FontSize.BundleName,
            0
        )
        format.putInt(
            PrinterConfig.addText.Alignment.BundleName,
            PrinterConfig.addText.Alignment.CENTER
        )
        printer?.addText(format, text)

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
        //   PrinterConfig.addTextInLine.mode.Devide_flexible
    }

    fun printSettlementReportupdate(
        context: Context?,
        batch: MutableList<BatchFileDataTable>,
        isSettlementSuccess: Boolean = false,
        isLastSummary: Boolean = false,
        callBack: (Boolean) -> Unit
    ) {
        //  val format = Bundle()
        //   val fmtAddTextInLine = Bundle()

//below if condition is for zero settlement
        if (batch.size <= 0) {
            try {
                // centerText(textFormatBundle, "SETTLEMENT SUCCESSFUL")

                val tpt = TerminalParameterTable.selectFromSchemeTable()
                /*   tpt?.receiptHeaderOne?.let { centerText(textInLineFormatBundle, it) }
                   tpt?.receiptHeaderTwo?.let { centerText(textInLineFormatBundle, it) }
                   tpt?.receiptHeaderThree?.let { centerText(textInLineFormatBundle, it) }
   */
                setLogoAndHeader()

                val td = System.currentTimeMillis()
                val formatdate = SimpleDateFormat("dd/MM/yyyy", Locale.ENGLISH)
                val formattime = SimpleDateFormat("HH:mm:ss", Locale.ENGLISH)

                val date = formatdate.format(td)
                val time = formattime.format(td)

                alignLeftRightText(textInLineFormatBundle, "DATE:$date", "TIME:$time")
                if (isLastSummary) {
                    centerText(textFormatBundle, "LAST SUMMARY REPORT")
                } else {
                    centerText(textFormatBundle, "SUMMARY REPORT")
                }

                alignLeftRightText(
                    textInLineFormatBundle,
                    "TID:${tpt?.terminalId}",
                    "MID:${tpt?.merchantId}"
                )
                alignLeftRightText(textInLineFormatBundle, "BATCH NO:${tpt?.batchNumber}", "")
                printSeperator(textFormatBundle)
                alignLeftRightText(textInLineFormatBundle, "TOTAL TXN = 0", "${getCurrencySymbol(tpt)}  0.00")

                centerText(textFormatBundle, "ZERO SETTLEMENT SUCCESSFUL")
                centerText(textFormatBundle, "BonusHub")
                centerText(textFormatBundle, "App Version : ${BuildConfig.VERSION_NAME}")
                printer?.feedLine(4)

                printer?.startPrint(object : PrinterListener.Stub() {
                    override fun onFinish() {
                        callBack(true)
                        Log.e("Settle_RECEIPT", "SUCESS__")
                    }

                    override fun onError(error: Int) {
                        callBack(false)
                        Log.e("Settle_RECEIPT", "FAIL__")
                    }


                })
            } catch (ex: DeadObjectException) {
                ex.printStackTrace()
                failureImpl(
                    context as Activity,
                    "Printer Service stopped.",
                    "Please take chargeslip from the Report menu."
                )
            } catch (e: RemoteException) {
                e.printStackTrace()
                failureImpl(
                    context as Activity,
                    "Printer Service stopped.",
                    "Please take chargeslip from the Report menu."
                )
            } catch (ex: Exception) {
                ex.printStackTrace()
                failureImpl(
                    context as Activity,
                    "Printer Service stopped.",
                    "Please take chargeslip from the Report menu."
                )
            }
        }
        ////below if condition is for settlement(Other than zero settlement)
        else {
            try {
                val map = mutableMapOf<String, MutableMap<Int, SummeryModel>>()
                val map1 = mutableMapOf<String, MutableMap<Int, SummeryModel>>()
                val tpt = TerminalParameterTable.selectFromSchemeTable()

                setLogoAndHeader()

                val td = System.currentTimeMillis()
                val formatdate = SimpleDateFormat("dd/MM/yyyy", Locale.ENGLISH)
                val formattime = SimpleDateFormat("HH:mm:ss", Locale.ENGLISH)

                val date = formatdate.format(td)
                val time = formattime.format(td)

                alignLeftRightText(textInLineFormatBundle, "DATE : $date", "TIME : $time")

                //  alignLeftRightText(fmtAddTextInLine,"DATE : ${batch.date}","TIME : ${batch.time}")
                /*   alignLeftRightText(textInLineFormatBundle, "MID : ${batch[0].mid}", "TID : ${batch[0].tid}")
                   alignLeftRightText(textInLineFormatBundle, "BATCH NO  : ${batch[0].batchNumber}", "")*/

                if (isLastSummary) {
                    centerText(textFormatBundle, "LAST SUMMARY REPORT")
                } else {
                    centerText(textFormatBundle, "SUMMARY REPORT")
                }

                batch.sortBy { it.hostTID }

                var tempTid = batch[0].hostTID

                val list = mutableListOf<String>()
                val frequencylist = mutableListOf<String>()

                for (it in batch) {  // Do not count preauth transaction
// || it.transactionType == TransactionType.VOID_PREAUTH.type
                    if (it.transactionType == TransactionType.PRE_AUTH.type) continue

                    if (it.transactionType == TransactionType.EMI_SALE.type || it.transactionType == TransactionType.BRAND_EMI.type || it.transactionType == TransactionType.BRAND_EMI_BY_ACCESS_CODE.type) {
                        it.transactionType = TransactionType.EMI_SALE.type
                        it.cardType = it.issuerName
                    }

                    if (it.transactionType == TransactionType.TEST_EMI.type) {
                        it.issuerName = "Test Issuer"
                        it.cardType = "Test Issuer"
                        it.transactionType = TransactionType.SALE.type

                    }

                    val transAmt = try {
                        it.transactionalAmmount.toLong()
                    } catch (ex: Exception) {
                        0L
                    }


                    if (tempTid == it.hostTID) {
                        _issuerName = it.cardType
                        if (map.containsKey(it.hostTID + it.hostMID + it.hostBatchNumber + it.cardType)) {
                            _issuerName = it.cardType

                            val ma =
                                map[it.hostTID + it.hostMID + it.hostBatchNumber + it.cardType] as MutableMap<Int, SummeryModel>
                            if (ma.containsKey(it.transactionType)) {
                                val m = ma[it.transactionType] as SummeryModel
                                m.count += 1
                                m.total += transAmt
                            } else {
                                val sm = SummeryModel(
                                    transactionType2Name(it.transactionType),
                                    1,
                                    transAmt,
                                    it.hostTID
                                )
                                ma[it.transactionType] = sm
                            }
                        } else {
                            val hm = HashMap<Int, SummeryModel>().apply {
                                this[it.transactionType] = SummeryModel(
                                    transactionType2Name(it.transactionType),
                                    1,
                                    transAmt,
                                    it.hostTID
                                )
                            }
                            map[it.hostTID + it.hostMID + it.hostBatchNumber + it.cardType] = hm
                            list.add(it.hostTID)
                        }
                    } else {
                        tempTid = it.hostTID
                        _issuerName = it.cardType
                        val hm = HashMap<Int, SummeryModel>().apply {
                            this[it.transactionType] = SummeryModel(
                                transactionType2Name(it.transactionType),
                                1,
                                transAmt,
                                it.hostTID
                            )
                        }
                        map[it.hostTID + it.hostMID + it.hostBatchNumber + it.cardType] = hm
                        list.add(it.hostTID)
                    }

                }

                for (item in list.distinct()) {
                    println("Frequency of item" + item + ": " + Collections.frequency(list, item))
                    frequencylist.add("" + Collections.frequency(list, item))
                }


                val totalMap = mutableMapOf<Int, SummeryTotalType>()


                var ietration = list.distinct().size
                var curentIndex = 0
                var frequency = 0
                var count = 0
                var lastfrequecny = 0
                var hasfrequency = false
                var updatedindex = 0

                for ((key, _map) in map.onEachIndexed { index, entry -> curentIndex = index }) {

                    count++
                    if (updatedindex <= frequencylist.size - 1)
                        frequency = frequencylist.get(updatedindex).toInt() + lastfrequecny

                    if (key.isNotBlank()) {

                        var hostTid = if (key.isNotBlank() && key.length >= 8) {
                            key.subSequence(0, 8).toString()
                        } else {
                            ""
                        }
                        var hostMid = if (key.isNotBlank() && key.length >= 23) {
                            key.subSequence(8, 23).toString()
                        } else {
                            ""
                        }
                        var hostBatchNumber = if (key.isNotBlank() && key.length >= 29) {
                            key.subSequence(23, 29).toString()
                        } else {
                            ""
                        }
                        var cardIssuer = if (key.isNotBlank() && key.length >= 30) {
                            key.subSequence(29, key.length).toString()
                        } else {
                            ""
                        }


                        if (ietration > 0) {
                            printSeperator(textFormatBundle)
                            alignLeftRightText(
                                textInLineFormatBundle,
                                "MID:${hostMid}",
                                "TID:${hostTid}"
                            )
                            alignLeftRightText(
                                textInLineFormatBundle,
                                "BATCH NO:${hostBatchNumber}",
                                ""
                            )
                            ietration--
                        }
                        if (cardIssuer.isNullOrEmpty()) {
                            cardIssuer = _issuerName.toString()
                            _issuerNameString = "CARD ISSUER"

                        }

                        printSeperator(textFormatBundle)
                        alignLeftRightText(
                            textInLineFormatBundle,
                            _issuerNameString,
                            "",
                            cardIssuer.toUpperCase(Locale.ROOT)
                        )
                        // if(ind==0){
                        alignLeftRightText(textInLineFormatBundle, "TXN TYPE", "TOTAL", "COUNT")
                        //   ind=1
                        //  }
                    }
                    for ((k, m) in _map) {
                        val amt = "%.2f".format(((m.total).toDouble() / 100))
                        if (k == TransactionType.PRE_AUTH_COMPLETE.type || k == TransactionType.VOID_PREAUTH.type) {
                            // need Not to show
                        } else {
                            alignLeftRightText(
                                textInLineFormatBundle,
                                m.type.toUpperCase(Locale.ROOT),
                                amt,
                                "${m.count} ${getCurrencySymbol(tpt)}"
                            )
                        }

                        if (totalMap.containsKey(k)) {
                            val x = totalMap[k]
                            if (x != null) {
                                x.count += m.count
                                x.total += m.total
                            }
                        } else {
                            totalMap[k] = SummeryTotalType(m.count, m.total)
                        }

                    }

                    if (frequency == count) {
                        lastfrequecny = frequency
                        hasfrequency = true
                        updatedindex++
                    } else {
                        hasfrequency = false
                    }
                    if (hasfrequency) {
                        printSeperator(textFormatBundle)
                        textFormatBundle.putInt(
                            PrinterConfig.addText.FontSize.BundleName,
                            0
                        )
                        centerText(textFormatBundle, "***TOTAL TRANSACTION***")
                        val sortedMap = totalMap.toSortedMap(compareByDescending { it })
                        for ((k, m) in sortedMap) {
                            /* alignLeftRightText(
                                 textInLineFormatBundle,
                                 "${transactionType2Name(k).toUpperCase(Locale.ROOT)}${"     =" + m.count}",
                                 "Rs.     ${"%.2f".format(((m.total).toDouble() / 100))}"

                             )*/

                            alignLeftRightText(
                                textInLineFormatBundle,
                                transactionType2Name(k).toUpperCase(Locale.ROOT),
                                "%.2f".format(((m.total).toDouble() / 100)),
                                "  = " + m.count +" "+getCurrencySymbol(tpt)
                            )

                        }

                        totalMap.clear()
                    }
                    //  sb.appendln()
                }

                //    sb.appendln(getChar(LENGTH, '='))

                printSeperator(textFormatBundle)
                if (isSettlementSuccess) {
                    centerText(textInLineFormatBundle, "SETTLEMENT SUCCESSFUL")
                }
                // Below code is used for Digi POS Settlement report
                if (!isLastSummary) {
                    val digiPosDataList =
                        DigiPosDataTable.selectDigiPosDataAccordingToTxnStatus(EDigiPosPaymentStatus.Approved.desciption)
                    val requiredTxnhm = hashMapOf<String, ArrayList<DigiPosDataTable>>()
                    if (digiPosDataList.isNotEmpty()) {
                        for (i in digiPosDataList) {
                            val digiData = arrayListOf<DigiPosDataTable>()
                            for (j in digiPosDataList) {
                                if (i.paymentMode == j.paymentMode) {
                                    digiData.add(j)
                                    requiredTxnhm[i.paymentMode] = digiData
                                }
                            }
                        }

                        ///  centerText(textFormatBundle, "---------X-----------X----------")
                        centerText(textFormatBundle, "Digi Pos Summary Report", true)
                        tpt?.terminalId?.let { centerText(textFormatBundle, "TID : $it") }
                        printSeperator(textFormatBundle)
                        // Txn description
                        alignLeftRightText(textInLineFormatBundle, "TXN TYPE", "TOTAL", "COUNT")
                        printSeperator(textFormatBundle)
                        var totalAmount = 0.0f
                        var totalCount = 0
                        for ((k, v) in requiredTxnhm) {
                            val txnType = k
                            val txnCount = v.size
                            var txnTotalAmount = 0.0f
                            for (value in v) {
                                txnTotalAmount += (value.amount.toFloat())
                                totalAmount += (value.amount.toFloat())
                                totalCount++
                            }
                            alignLeftRightText(
                                textInLineFormatBundle,
                                txnType,
                                "%.2f".format(txnTotalAmount),
                                txnCount.toString() + getCurrencySymbol(tpt)
                            )
                        }
                        printSeperator(textFormatBundle)
                        alignLeftRightText(
                            textInLineFormatBundle,
                            "Total TXNs",
                            "%.2f".format(totalAmount),
                            totalCount.toString() + getCurrencySymbol(tpt)
                        )
                        printSeperator(textFormatBundle)
                    }
                }

                centerText(textFormatBundle, "Bonushub")
                centerText(textFormatBundle, "App Version:${BuildConfig.VERSION_NAME}")

                ///  centerText(textFormatBundle, "---------X-----------X----------")
                printer?.feedLine(4)


                // start print here
                printer?.startPrint(object : PrinterListener.Stub() {
                    override fun onFinish() {
                        if (isSettlementSuccess) {
                            DigiPosDataTable.deletAllRecordAccToTxnStatus(EDigiPosPaymentStatus.Approved.desciption)
                        }
                        callBack(true)
                        Log.e("Settle_RECEIPT", "SUCESS__")
                    }

                    override fun onError(error: Int) {
                        if (isSettlementSuccess) {
                            DigiPosDataTable.deletAllRecordAccToTxnStatus(EDigiPosPaymentStatus.Approved.desciption)
                        }
                        callBack(false)
                        Log.e("Settle_RECEIPT", "FAIL__")
                    }


                })
            } catch (ex: DeadObjectException) {
                ex.printStackTrace()
                failureImpl(
                    context as Activity,
                    "Printer Service stopped.",
                    "Please take chargeslip from the Report menu."
                )
            } catch (e: RemoteException) {
                e.printStackTrace()
                failureImpl(
                    context as Activity,
                    "Printer Service stopped.",
                    "Please take chargeslip from the Report menu."
                )
            } catch (ex: Exception) {
                ex.printStackTrace()
                failureImpl(
                    context as Activity,
                    "Printer Service stopped.",
                    "Please take chargeslip from the Report menu."
                )
            }
        }
    }


    fun printAuthCompleteChargeSlip(
        printerReceiptData: BatchFileDataTable,
        copyType: EPrintCopyType,
        context: Context?,
        printerCallback: (Boolean) -> Unit
    ) {
        val signatureMsg = "SIGN ..................."

        try {

            //Changes By manish Kumar
            //If in Respnse field 60 data comes Auto settle flag | Bank id| Issuer id | MID | TID | Batch No | Stan | Invoice | Card Type
            // then show response data otherwise show data available in database
            //From mid to hostMID (coming from field 60)
            //From tid to hostTID (coming from field 60)
            //From batchNumber to hostBatchNumber (coming from field 60)
            //From roc to hostRoc (coming from field 60)
            //From invoiceNumber to hostInvoice (coming from field 60)
            //From cardType to hostCardType (coming from field 60)

            val hostMID = if (printerReceiptData.hostMID.isNotBlank()) {
                printerReceiptData.hostMID
            } else {
                printerReceiptData.mid
            }

            val hostTID = if (printerReceiptData.hostTID.isNotBlank()) {
                printerReceiptData.hostTID
            } else {
                printerReceiptData.tid
            }

            val hostBatchNumber = if (printerReceiptData.hostBatchNumber.isNotBlank()) {
                printerReceiptData.hostBatchNumber
            } else {
                printerReceiptData.batchNumber
            }

            val hostRoc = if (printerReceiptData.hostRoc.isNotBlank()) {
                printerReceiptData.hostRoc
            } else {
                printerReceiptData.roc
            }
            val hostInvoice = if (printerReceiptData.hostInvoice.isNotBlank()) {
                printerReceiptData.hostInvoice
            } else {
                printerReceiptData.invoiceNumber
            }
            var hostCardType = if (printerReceiptData.hostCardType.isNotBlank()) {
                printerReceiptData.hostCardType
            } else {
                printerReceiptData.cardType
            }


            // bundle format for addText
            val format = Bundle()

            // bundle formate for AddTextInLine
            val fmtAddTextInLine = Bundle()
            setLogoAndHeader()


            fmtAddTextInLine.putInt(
                PrinterConfig.addTextInLine.FontSize.BundleName,
                PrinterConfig.addTextInLine.FontSize.NORMAL_24_24
            )
            fmtAddTextInLine.putString(
                PrinterConfig.addTextInLine.GlobalFont.BundleName,
                PrinterFonts.path + PrinterFonts.FONT_AGENCYR
            )

            val time = printerReceiptData.time
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


            printer?.addTextInLine(
                fmtAddTextInLine,
                "DATE:${printerReceiptData.transactionDate}",
                "",
                "TIME:$formattedTime",
                0
            )


            fmtAddTextInLine.putInt(
                PrinterConfig.addTextInLine.FontSize.BundleName,
                PrinterConfig.addTextInLine.FontSize.NORMAL_24_24
            )
            fmtAddTextInLine.putString(
                PrinterConfig.addTextInLine.GlobalFont.BundleName,
                PrinterFonts.path + PrinterFonts.FONT_AGENCYR
            )
            //   printer.addTextInLine( fmtAddTextInLine, "L & R", "", "Divide Equally", 0);
            //   printer.addTextInLine( fmtAddTextInLine, "L & R", "", "Divide Equally", 0);
            printer?.addTextInLine(
                fmtAddTextInLine,
                "MID:${hostMID}",
                "",
                "TID:${hostTID}",
                PrinterConfig.addTextInLine.mode.Devide_flexible
            )


            fmtAddTextInLine.putInt(
                PrinterConfig.addTextInLine.FontSize.BundleName,
                PrinterConfig.addTextInLine.FontSize.NORMAL_24_24
            )
            fmtAddTextInLine.putString(
                PrinterConfig.addTextInLine.GlobalFont.BundleName,
                PrinterFonts.path + PrinterFonts.FONT_AGENCYR
            )
            //   printer.addTextInLine( fmtAddTextInLine, "L & R", "", "Divide Equally", 0);
            //   printer.addTextInLine( fmtAddTextInLine, "L & R", "", "Divide Equally", 0);
            printer?.addTextInLine(
                fmtAddTextInLine,
                "BATCH NO:${hostBatchNumber}",
                "",
                "ROC:${invoiceWithPadding(hostRoc)}",
                PrinterConfig.addTextInLine.mode.Devide_flexible
            )

            fmtAddTextInLine.putInt(
                PrinterConfig.addTextInLine.FontSize.BundleName,
                PrinterConfig.addTextInLine.FontSize.NORMAL_24_24
            )
            fmtAddTextInLine.putString(
                PrinterConfig.addTextInLine.GlobalFont.BundleName,
                PrinterFonts.path + PrinterFonts.FONT_AGENCYR
            )
            //   printer.addTextInLine( fmtAddTextInLine, "L & R", "", "Divide Equally", 0);
            //   printer.addTextInLine( fmtAddTextInLine, "L & R", "", "Divide Equally", 0);
            printer?.addTextInLine(
                fmtAddTextInLine,
                "INVOICE:${invoiceWithPadding(hostInvoice)}",
                "",
                "",
                PrinterConfig.addTextInLine.mode.Devide_flexible
            )
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
            centerText(fmtAddTextInLine, "ENTERED DETAILS")

            if (printerReceiptData.transactionType == TransactionType.PRE_AUTH_COMPLETE.type)
                alignLeftRightText(fmtAddTextInLine, "TID:${printerReceiptData.authTID}", "")

            alignLeftRightText(
                fmtAddTextInLine,
                "BATCH NO:${invoiceWithPadding(printerReceiptData.authBatchNO)}",
                "ROC:${invoiceWithPadding(printerReceiptData.authROC)}"
            )

            printer?.addText(format, "--------------------------------")

            /*   format.putInt(
                   PrinterConfig.addText.FontSize.BundleName,
                   PrinterConfig.addText.FontSize.NORMAL_DH_24_48_IN_BOLD
               )
               format.putInt(
                   PrinterConfig.addText.Alignment.BundleName,
                   PrinterConfig.addText.Alignment.CENTER
               )
               printer?.addText(format, printerReceiptData.getTransactionType())*/
            printTransType(format, printerReceiptData.transactionType)

            fmtAddTextInLine.putInt(
                PrinterConfig.addTextInLine.FontSize.BundleName,
                PrinterConfig.addTextInLine.FontSize.NORMAL_24_24
            )
            fmtAddTextInLine.putString(
                PrinterConfig.addTextInLine.GlobalFont.BundleName,
                PrinterFonts.path + PrinterFonts.FONT_AGENCYR
            )
            //   printer.addTextInLine( fmtAddTextInLine, "L & R", "", "Divide Equally", 0);
            //   printer.addTextInLine( fmtAddTextInLine, "L & R", "", "Divide Equally", 0);
            printer?.addTextInLine(
                fmtAddTextInLine,
                "CARD NO:${printerReceiptData.encryptPan}",
                "",
                "",
                PrinterConfig.addTextInLine.mode.Devide_flexible
            )

            //   printer.addTextInLine( fmtAddTextInLine, "L & R", "", "Divide Equally", 0);
            //   printer.addTextInLine( fmtAddTextInLine, "L & R", "", "Divide Equally", 0);

            if (printerReceiptData.authCode == "null") {
                printer?.addTextInLine(
                    fmtAddTextInLine,
                    "RRN:${printerReceiptData.referenceNumber}",
                    "",
                    "",
                    PrinterConfig.addTextInLine.mode.Devide_flexible
                )
            } else {
                printer?.addTextInLine(
                    fmtAddTextInLine,
                    "AUTH CODE:${printerReceiptData.authCode.trim()}",
                    "",
                    "RRN:${printerReceiptData.referenceNumber}",
                    PrinterConfig.addTextInLine.mode.Devide_flexible
                )
            }

            printSeperator(format)

            fmtAddTextInLine.putInt(
                PrinterConfig.addTextInLine.FontSize.BundleName,
                PrinterConfig.addTextInLine.FontSize.NORMAL_24_24
            )
            fmtAddTextInLine.putString(
                PrinterConfig.addTextInLine.GlobalFont.BundleName,
                PrinterFonts.path + PrinterFonts.FONT_AGENCYR
            )
            //   printer.addTextInLine( fmtAddTextInLine, "L & R", "", "Divide Equally", 0);
            //   printer.addTextInLine( fmtAddTextInLine, "L & R", "", "Divide Equally", 0);
            var baseAmount = "00"
            when (printerReceiptData.transactionType) {
                TransactionType.PRE_AUTH_COMPLETE.type -> {
                    baseAmount =
                        "%.2f".format(printerReceiptData.transactionalAmmount.toDouble() / 100)
                }
                TransactionType.VOID_PREAUTH.type -> {
                    baseAmount =
                        "%.2f".format(printerReceiptData.amountInResponse.toDouble() / 100)
                }
            }
            val tpt=TerminalParameterTable.selectFromSchemeTable()
            printer?.addTextInLine(
                fmtAddTextInLine,
                "BASE AMOUNT  :    ${getCurrencySymbol(tpt)}    $baseAmount",
                "",
                "",
                PrinterConfig.addTextInLine.mode.Devide_flexible
            )

            fmtAddTextInLine.putInt(
                PrinterConfig.addTextInLine.FontSize.BundleName,
                PrinterConfig.addTextInLine.FontSize.NORMAL_24_24
            )
            fmtAddTextInLine.putString(
                PrinterConfig.addTextInLine.GlobalFont.BundleName,
                PrinterFonts.path + PrinterFonts.FONT_AGENCYR
            )
            //   printer.addTextInLine( fmtAddTextInLine, "L & R", "", "Divide Equally", 0);
            //   printer.addTextInLine( fmtAddTextInLine, "L & R", "", "Divide Equally", 0);
            val totalAmount = "%.2f".format(printerReceiptData.totalAmmount.toDouble() / 100)
            printer?.addTextInLine(
                fmtAddTextInLine,
                "TOTAL AMOUNT :    ${getCurrencySymbol(tpt)}    $baseAmount",
                "",
                "",
                PrinterConfig.addTextInLine.mode.Devide_flexible
            )
            printSeperator(format)

            format.putInt(
                PrinterConfig.addText.FontSize.BundleName,
                PrinterConfig.addText.FontSize.NORMAL_24_24
            )
            format.putInt(
                PrinterConfig.addText.Alignment.BundleName,
                PrinterConfig.addText.Alignment.CENTER
            )

            format.putInt(
                PrinterConfig.addTextInLine.FontSize.BundleName,
                PrinterConfig.addText.FontSize.NORMAL_24_24
            )
            // -------(Remove in New VFservice 3.0)  printer?.feedLine(2)
            alignLeftRightText(format, signatureMsg, "", "")
            // -------(Remove in New VFservice 3.0)  printer?.feedLine(2)
            val ipt =
                IssuerParameterTable.selectFromIssuerParameterTable(AppPreference.WALLET_ISSUER_ID)

            val chunks: List<String>? = ipt?.volletIssuerDisclammer?.let { chunkTnC(it) }
            if (chunks != null) {
                for (st in chunks) {
                    logger("TNC", st, "e")
                    alignLeftRightText(format, st, "", "")
                }
            }
            //   printer?.addText(format, ipt?.volletIssuerDisclammer)
            format.putInt(
                PrinterConfig.addText.FontSize.BundleName,
                0
            )
            printer?.addText(format, copyType.pName)
            printer?.addText(format, footerText[0])
            printer?.addText(format, footerText[1])



            printLogo("BH.bmp")

            format.putInt(PrinterConfig.addText.FontSize.BundleName, 0)
            format.putInt(
                PrinterConfig.addText.Alignment.BundleName,
                PrinterConfig.addText.Alignment.CENTER
            )
            printer?.addText(format, "App Version:${BuildConfig.VERSION_NAME}")

            ///  printer?.addText(format, "---------X-----------X----------")
            printer?.feedLine(4)

            // start print here
            printer?.startPrint(
                IAuthCompletePrintListener(
                    this,
                    context,
                    copyType,
                    printerReceiptData,
                    printerCallback
                )
            )


        } catch (ex: DeadObjectException) {
            ex.printStackTrace()
            failureImpl(
                context as Activity,
                "Printer Service stopped.",
                "Please take chargeslip from the Report menu."
            )
        } catch (e: RemoteException) {
            e.printStackTrace()
            failureImpl(
                context as Activity,
                "Printer Service stopped.",
                "Please take chargeslip from the Report menu."
            )
        } catch (ex: Exception) {
            ex.printStackTrace()
            failureImpl(
                context as Activity,
                "Printer Service stopped.",
                "Please take chargeslip from the Report menu."
            )
        } finally {
            //   VFService.connectToVFService(VerifoneApp.appContext)
        }
    }

    fun printTenure(
        context: Context?,
        issuerDataModelList: ArrayList<IssuerDataModel>,
        amt: Float
    ) {
        try {
            //    var tenure= arrayListOf<TenureDataModel>()
            setLogoAndHeader()
            val terminalData = TerminalParameterTable.selectFromSchemeTable()
            val dateTime: Long = Calendar.getInstance().timeInMillis
            val time: String = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(dateTime)
            val date: String = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(dateTime)
            val year: String = SimpleDateFormat("yy", Locale.getDefault()).format(dateTime)
            logger("AUTH YEAR->", year, "e")
            alignLeftRightText(textInLineFormatBundle, "DATE:$date", "TIME:$time")
            alignLeftRightText(
                textInLineFormatBundle,
                "MID:${terminalData?.merchantId}",
                "TID:${terminalData?.terminalId}"
            )
            alignLeftRightText(
                textInLineFormatBundle,
                "BATCH NO:${terminalData?.batchNumber}",
                ""
            )
            textFormatBundle.putInt(
                PrinterConfig.addText.FontSize.BundleName,
                PrinterConfig.addText.FontSize.LARGE_DH_32_64_IN_BOLD
            )
            textFormatBundle.putInt(
                PrinterConfig.addText.Alignment.BundleName,
                PrinterConfig.addText.Alignment.CENTER
            )
            printer?.addText(textFormatBundle, "EMI ENQUIRY")
            //  centerText(textFormatBundle, "EMI CATALOGUE", true)
            fun printt(tenureData: TenureDataModel) {

                val rateOfInterest = "%.2f".format(tenureData.roi.toFloat() / 100) + " %"
                centerText(
                    textInLineFormatBundle,
                    "Tenure:${tenureData.tenure} Month INTEREST RATE:$rateOfInterest"
                )

                val procFee =
                    ((tenureData.proccesingFee.toFloat() / 100) + ((tenureData.processingRate.toFloat() / 100) * ((amt - (tenureData.emiAmount?.discount
                        ?: 0f))) / 100))

                var procCodePrint = ""
                procCodePrint = if (procFee <= 0f) {
                    "%.2f".format(procFee) + " %"
                } else {
                    "%.2f".format(procFee)
                }
                val tpt=TerminalParameterTable.selectFromSchemeTable()
                alignLeftRightText(
                    textInLineFormatBundle,
                    "PROCESSING FEE",
                    procCodePrint,
                    " :  ${getCurrencySymbol(tpt)}  "
                )
                val amtToPrint = "%.2f".format(amt)
                alignLeftRightText(
                    textInLineFormatBundle,
                    "AMOUNT",
                    amtToPrint,
                    "  :  ${getCurrencySymbol(tpt)}  "
                )
                val loanAmtToPrint = "%.2f".format(tenureData.emiAmount?.principleAmt)
                alignLeftRightText(
                    textInLineFormatBundle,
                    "LOAN AMOUNT",
                    loanAmtToPrint,
                    "  :  ${getCurrencySymbol(tpt)}  "
                )

                val monthlyemitoPrint = "%.2f".format(tenureData.emiAmount?.monthlyEmi)
                alignLeftRightText(
                    textInLineFormatBundle,
                    "MONTHLY EMI",
                    monthlyemitoPrint,
                    "  :  ${getCurrencySymbol(tpt)}  "
                )

                //format two decimal places //loanintrest
                val toi = "%.2f".format(tenureData.emiAmount?.totalInterest)
                alignLeftRightText(
                    textInLineFormatBundle,
                    "TOTAL INTEREST",
                    toi,
                    "  :  ${getCurrencySymbol(tpt)}  "
                )


//total payment

                val tp = totalPaymentforTenure(amt, tenureData)
                val tpf = "%.2f".format(tp)
                alignLeftRightText(
                    textInLineFormatBundle,
                    "TOTAL PAYMENT",
                    tpf,
                    "  :  ${getCurrencySymbol(tpt)}  "
                )
                if (tenureData.emiAmount?.benefitCalc == EBenefitCalculation.FIXED_VALUE) {
                    tenureData.emiAmount?.cashBackpercent = tenureData.emiAmount?.cashBack!!
                }

                if (tenureData.emiAmount?.cashBackpercent!! > 0f) {
                    var percentSign = ""
                    if (tenureData.emiAmount?.benefitCalc == EBenefitCalculation.PERCENTAGE_VALUE)
                        percentSign = " %"
                    val cashBackPercentToPrint =
                        "%.2f".format(tenureData.emiAmount?.cashBackpercent)
                    alignLeftRightText(
                        textInLineFormatBundle,
                        "CASHBACK",
                        cashBackPercentToPrint + percentSign,
                        "  :  ${getCurrencySymbol(tpt)}  "
                    )

                }
                if (tenureData.emiAmount?.cashBack!! > 0f) {
                    val cashBacktoPrint = "%.2f".format(tenureData.emiAmount?.cashBack)
                    alignLeftRightText(
                        textInLineFormatBundle,
                        "TOTAL CASHBACK",
                        cashBacktoPrint,
                        "  :  ${getCurrencySymbol(tpt)}  "
                    )
                }
            }

            var schemesToPrint = ArrayList<TenureDataModel>()
            for (index in issuerDataModelList.indices) {
                centerText(
                    textFormatBundle,
                    "BANK NAME : ${issuerDataModelList.size}.${issuerDataModelList[index].issuerName}"
                )

                for (schemeIndex in issuerDataModelList[index].schemeDataModel.indices) {
                    centerText(textFormatBundle, "SCHEME : ${schemeIndex + 1}")

                    val schemesTenureData =
                        issuerDataModelList[index].schemeDataModel[schemeIndex].tenureDataModel.toCollection(
                            ArrayList()
                        )
                    //Added by Lucky
                    schemesToPrint = arrayListOf<TenureDataModel>()
                    for (ss in schemesTenureData) {
                        if (ss.isChecked) {
                            schemesToPrint.add(ss)
                        }
                    }
                    for (tenure in schemesToPrint) {
                        // attach the EMI Calculation with each tenure
                        printSeperator(textFormatBundle)
                        printt(tenure)

                    }

                }
            }

            printSeperator(textFormatBundle)
            printer?.feedLine(1)

            printer?.addText(textFormatBundle, footerText[1])

            printLogo("BH.bmp")
            printer?.addText(textFormatBundle, "App Version : ${BuildConfig.VERSION_NAME}")
            /// printer?.addText(textFormatBundle, "---------X-----------X----------")
            printer?.feedLine(4)

            //  if (schemesToPrint.size > 0) {
            printer?.startPrint(object : PrinterListener.Stub() {
                override fun onFinish() {
                    logger("Print", "Success")
                }

                override fun onError(error: Int) {
                    logger("Print", "Fail")
                }
            })
            //      } else {
            //    VFService.showToast("Select tenure")
            //    printer?.cleanCache()
            //    return

            //    }

        } catch (ex: DeadObjectException) {
            ex.printStackTrace()
            failureImpl(
                context as Activity,
                "Printer Service stopped.",
                "Please take chargeslip from the Report menu."
            )
        } catch (e: RemoteException) {
            e.printStackTrace()
            failureImpl(
                context as Activity,
                "Printer Service stopped.",
                "Please take chargeslip from the Report menu."
            )
        } catch (ex: Exception) {
            ex.printStackTrace()
            failureImpl(
                context as Activity,
                "Printer Service stopped.",
                "Please take chargeslip from the Report menu."
            )
        }

    }


    fun printEMIEnquiry(
        context: Context?,
        issuerDataModelList: ArrayList<BankEMIDataModal>,
        amt: String, bankName: String, printerCallback: (Boolean, String) -> Unit
    ) {
        try {
            //    var tenure= arrayListOf<TenureDataModel>()
            //  val brandData = runBlocking { BrandEMIDataTable.getAllEMIData() }
            setLogoAndHeader()
            val terminalData = TerminalParameterTable.selectFromSchemeTable()
            val dateTime: Long = Calendar.getInstance().timeInMillis
            val time: String = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(dateTime)
            val date: String = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(dateTime)
            val year: String = SimpleDateFormat("yy", Locale.getDefault()).format(dateTime)
            logger("AUTH YEAR->", year, "e")
            alignLeftRightText(textInLineFormatBundle, "DATE : $date", "TIME : $time")
            alignLeftRightText(
                textInLineFormatBundle,
                "MID : ${terminalData?.merchantId}",
                "TID : ${terminalData?.terminalId}"
            )
            alignLeftRightText(
                textInLineFormatBundle,
                "BATCH NO : ${terminalData?.batchNumber}",
                ""
            )
            textFormatBundle.putInt(
                PrinterConfig.addText.FontSize.BundleName,
                PrinterConfig.addText.FontSize.LARGE_DH_32_64_IN_BOLD
            )
            textFormatBundle.putInt(
                PrinterConfig.addText.Alignment.BundleName,
                PrinterConfig.addText.Alignment.CENTER
            )
            printer?.addText(textFormatBundle, "EMI CATALOGUE")
            printer?.addText(textFormatBundle, "BANK NAME   :  $bankName")
            //  centerText(textFormatBundle, "EMI CATALOGUE", true)
            printSeperator(textFormatBundle)
            //region=====================Printing Merchant Brand Purchase Details:-
            /* if (printerReceiptData.transactionType == TransactionType.B.type) {
                 centerText(textFormatBundle, "-----**Product Details**-----", true)
                 alignLeftRightText(
                     textInLineFormatBundle,
                     "Merch/Mfr Name",
                     brandData.brandName,
                     ":"
                 )
                 alignLeftRightText(
                     textInLineFormatBundle,
                     "Product Category",
                     brandData.categoryName,
                     ":"
                 )
                 alignLeftRightText(
                     textInLineFormatBundle,
                     "Product",
                     brandData.productName,
                     ":"
                 )
                 if (!TextUtils.isEmpty(brandData.imeiNumber)) {
                     alignLeftRightText(
                         textInLineFormatBundle,
                         "Product IMEI/Serial No.",
                         brandData.imeiNumber,
                         ":"
                     )
                 }
             }*/
            //endregion
            fun printt(modelData: BankEMIDataModal) {

                alignLeftRightText(
                    textInLineFormatBundle,
                    "AMOUNT",
                    amt,
                    " :  "
                )
                //    holder.transactionAmount.text =enquiryAmt
                val tenureDuration = "${modelData.tenure} Months"
                val tenureHeadingDuration = "${modelData.tenure} Months Scheme"
                var roi = divideAmountBy100(modelData.tenureInterestRate.toInt()).toString()
                var loanamt = divideAmountBy100(modelData.loanAmount.toInt()).toString()
                roi = "%.2f".format(roi.toDouble()) + " %"
                loanamt = "%.2f".format(loanamt.toDouble())
                alignLeftRightText(
                    textInLineFormatBundle,
                    "ROI (p.a.)",
                    roi,
                    " :  "
                )
                alignLeftRightText(
                    textInLineFormatBundle,
                    "TENURE",
                    tenureDuration,
                    " :  "
                )

                if (!TextUtils.isEmpty(modelData.cashBackAmount) && modelData.cashBackAmount != "0") {
                    alignLeftRightText(
                        textInLineFormatBundle,
                        "TOTAL CASHBACK AMOUNT",
                        "%.2f".format(
                            divideAmountBy100(modelData.cashBackAmount.toInt()).toString()
                                .toDouble()
                        ),
                        " :  INR"
                    )
                }

                if (!TextUtils.isEmpty(modelData.discountAmount) && modelData.discountAmount != "0") {
                    alignLeftRightText(
                        textInLineFormatBundle,
                        "TOTAL DISCOUNT AMOUNT",
                        "%.2f".format(
                            divideAmountBy100(modelData.discountAmount.toInt()).toString()
                                .toDouble()
                        ),
                        " :  INR"
                    )
                }

                alignLeftRightText(
                    textInLineFormatBundle,
                    "LOAN AMOUNT",
                    loanamt,
                    " :  INR"
                )
                alignLeftRightText(
                    textInLineFormatBundle,
                    "MONTHLY EMI",
                    divideAmountBy100(modelData.emiAmount.toInt()).toString(),
                    " :  INR"
                )
                alignLeftRightText(
                    textInLineFormatBundle,
                    "TOTAL INTEREST",
                    divideAmountBy100(modelData.totalInterestPay.toInt()).toString(),
                    " :  INR"
                )
                alignLeftRightText(
                    textInLineFormatBundle,
                    "TOTAL Amt(with Int)",
                    divideAmountBy100(modelData.totalEmiPay.toInt()).toString(),
                    " :  INR"
                )
            }

            for (data in issuerDataModelList) {
                printSeperator(textFormatBundle)
                printt(data)
            }

            printSeperator(textFormatBundle)
            printer?.feedLine(1)

            printer?.addText(textFormatBundle, footerText[1])

            printLogo("BH.bmp")
            printer?.addText(textFormatBundle, "App Version : ${BuildConfig.VERSION_NAME}")
            ///printer?.addText(textFormatBundle, "---------X-----------X----------")
            printer?.feedLine(4)

            //  if (schemesToPrint.size > 0) {
            printer?.startPrint(object : PrinterListener.Stub() {
                override fun onFinish() {
                    logger("Print", "Success")
                    printerCallback(true, "SUCCESS")
                }

                override fun onError(error: Int) {
                    logger("Print", "Fail")
                    printerCallback(false, "Printing Error")
                }
            })
            //      } else {
            //    VFService.showToast("Select tenure")
            //    printer?.cleanCache()
            //    return

            //    }

        } catch (ex: DeadObjectException) {
            ex.printStackTrace()
            failureImpl(
                context as Activity,
                "Printer Service stopped.",
                "Please take chargeslip from the Report menu."
            )
        } catch (e: RemoteException) {
            e.printStackTrace()
            failureImpl(
                context as Activity,
                "Printer Service stopped.",
                "Please take chargeslip from the Report menu."
            )
        } catch (ex: Exception) {
            ex.printStackTrace()
            failureImpl(
                context as Activity,
                "Printer Service stopped.",
                "Please take chargeslip from the Report menu."
            )
        }

    }

    private fun totalPaymentforTenure(amount: Float, tenure: TenureDataModel): Float {
        /*return amount + tenure.proccesingFee.toFloat() - (tenure.emiAmount?.cashBack
            ?: 0f) - (tenure.emiAmount?.discount ?: 0f) + (tenure.emiAmount?.totalInterest ?: 0f)*/

        return amount + tenure.proccesingFee.toFloat() / 100 + ((tenure.processingRate.toFloat() / 100) * (amount - (tenure.emiAmount?.discount
            ?: 0f))) / 100 - (tenure.emiAmount?.cashBack
            ?: 0f) - (tenure.emiAmount?.discount ?: 0f) + (tenure.emiAmount?.totalInterest ?: 0f)
    }

    //region=======================Method to Print BankEMI ChargeSlip:-
    fun printEMISale(
        printerReceiptData: BatchFileDataTable,
        copyType: EPrintCopyType,
        context: Context?,
        printerCallback: (Boolean, Int) -> Unit
    ) {
        var currencySymbol: String? = "Rs"
        var brandEmiData: BrandEMIDataTable? = null
        try {
            val tpt = runBlocking(Dispatchers.IO) { TerminalParameterTable.selectFromSchemeTable() }

            //Changes By manish Kumar
            //If in Respnse field 60 data comes Auto settle flag | Bank id| Issuer id | MID | TID | Batch No | Stan | Invoice | Card Type
            // then show response data otherwise show data available in database
            //From issuerId to hostIssuerID (coming from field 60)
            //From mid to hostMID (coming from field 60)
            //From tid to hostTID (coming from field 60)
            //From batchNumber to hostBatchNumber (coming from field 60)
            //From roc to hostRoc (coming from field 60)
            //From invoiceNumber to hostInvoice (coming from field 60)
            //From cardType to hostRoc (coming from field 60)

            val hostIssuerId = if (printerReceiptData.hostIssuerID.isNotBlank()) {
                printerReceiptData.hostIssuerID
            } else {
                printerReceiptData.issuerId
            }
            Log.e("hostIssuerId", "-->" + hostIssuerId)
            // Log.e("issuerId","-->"+issuerId)

            val hostMID = if (printerReceiptData.hostMID.isNotBlank()) {
                printerReceiptData.hostMID
            } else {
                printerReceiptData.mid
            }
            val hostTID = if (printerReceiptData.hostTID.isNotBlank()) {
                printerReceiptData.hostTID
            } else {
                printerReceiptData.tid
            }
            val hostBatchNumber = if (printerReceiptData.hostBatchNumber.isNotBlank()) {
                printerReceiptData.hostBatchNumber
            } else {
                printerReceiptData.batchNumber
            }
            val hostRoc = if (printerReceiptData.hostRoc.isNotBlank()) {
                printerReceiptData.hostRoc
            } else {
                printerReceiptData.roc
            }

            val hostInvoice = if (printerReceiptData.hostInvoice.isNotBlank()) {
                printerReceiptData.hostInvoice
            } else {
                printerReceiptData.invoiceNumber
            }

            val hostCardType = if (printerReceiptData.hostCardType.isNotBlank()) {
                printerReceiptData.hostCardType
            } else {
                printerReceiptData.cardType
            }

            val issuerTAndCData = runBlocking(Dispatchers.IO) {
                IssuerTAndCTable.selectIssuerTAndCDataByID(hostIssuerId)
            }

            if (printerReceiptData.transactionType == TransactionType.BRAND_EMI.type) {
                brandEmiData = runBlocking(Dispatchers.IO) {
                    BrandEMIDataTable.getBrandEMIDataByInvoice(hostInvoice)
                }
            }

            if (!TextUtils.isEmpty(tpt?.currencySymbol)) {
                currencySymbol = tpt?.currencySymbol
                Log.d("TPTCurrencySymbol:- ", currencySymbol ?: "")
            }

            val signatureMsg = if (printerReceiptData.isPinverified) {
                "SIGNATURE NOT REQUIRED"
            } else {
                "SIGN ..................."
            }

            val pinVerifyMsg = if (printerReceiptData.isPinverified) {
                "PIN VERIFIED OK"
            } else {
                ""
            }
            printer?.cleanCache()

            /* textFormatBundle.putInt(
               PrinterConfig.addTextInLine.FontSize.BundleName,
               PrinterConfig.addTextInLine.FontSize.NORMAL_24_24
           )
            textFormatBundle.putString(
               PrinterConfig.addTextInLine.GlobalFont.BundleName,
               PrinterFonts.path + PrinterFonts.FONT_AGENCYR
           )*/
            val centerTextBundle = Bundle()
            val seperatorLineBundle = Bundle()

            hasPin(printerReceiptData)
            setLogoAndHeader()
            printTransDatetime(printerReceiptData)


            //===========================
            alignLeftRightText(
                textInLineFormatBundle, "MID:${hostMID}", "TID:${hostTID}"
            )

            alignLeftRightText(
                textInLineFormatBundle,
                "BATCH NO:${hostBatchNumber}",
                "ROC:${invoiceWithPadding(hostRoc)}"
            )
            var mBillno = ""
            if (printerReceiptData.merchantBillNumber.isNotBlank() && printerReceiptData.merchantBillNumber != "0") {
                mBillno = "M.BILL NO:" + printerReceiptData.merchantBillNumber
            }


            if (printerReceiptData.merchantBillNumber.isNotBlank() && printerReceiptData.merchantBillNumber != "0") {
                alignLeftRightText(
                    textInLineFormatBundle,
                    "INVOICE:${invoiceWithPadding(hostInvoice)}",
                    mBillno
                )
            } else {
                alignLeftRightText(
                    textInLineFormatBundle,
                    "INVOICE:${invoiceWithPadding(hostInvoice)}", ""
                )
            }
            // printer?.addText(textFormatBundle, printerReceiptData.getTransactionType())
            centerText(centerTextBundle, printerReceiptData.getTransactionType(), true)
            alignLeftRightText(
                textInLineFormatBundle,
                "CARD NO:${printerReceiptData.cardNumber}",
                printerReceiptData.operationType
            )

            alignLeftRightText(
                textInLineFormatBundle,
                "CARD TYPE:${hostCardType}",
                "EXP: XX/XX"
            )

            if (printerReceiptData.merchantMobileNumber.isNotBlank())
                alignLeftRightText(
                    textInLineFormatBundle,
                    "MOBILE NO:${printerReceiptData.merchantMobileNumber}",
                    ""
                )

            alignLeftRightText(
                textInLineFormatBundle,
                "AUTH CODE:${printerReceiptData.authCode.trim()}",
                "RRN:${printerReceiptData.referenceNumber}"
            )

            if (printerReceiptData.operationType != "Mag") {
                //Condition nee to be here before inflating below tvr and tsi?
                if (printerReceiptData.operationType == "Chip") {
                    alignLeftRightText(
                        textInLineFormatBundle,
                        "TVR:${printerReceiptData.tvr}",
                        "TSI:${printerReceiptData.tsi}"
                    )
                }
                if (printerReceiptData.aid.isNotBlank() && printerReceiptData.tc.isNotBlank()) {
                    alignLeftRightText(
                        textInLineFormatBundle,
                        "AID:${printerReceiptData.aid}",
                        ""
                    )
                    alignLeftRightText(textInLineFormatBundle, "TC: ${printerReceiptData.tc}", "")
                }
            }

            printSeperator(seperatorLineBundle)
            /*   textInLineFormatBundle.putFloat("scale_w",1f)
               textInLineFormatBundle.putFloat("scale_h",1.1f)*/
            if (!TextUtils.isEmpty(printerReceiptData.emiTransactionAmount)) {
                var emiTxnAmount =
                    "%.2f".format(printerReceiptData.emiTransactionAmount.toFloat() / 100)
                val authTxnAmount = "%.2f".format(printerReceiptData.transactionAmt.toFloat() / 100)
                if (printerReceiptData.transactionType == TransactionType.BRAND_EMI_BY_ACCESS_CODE.type && printerReceiptData.issuerId == "64") {
                    emiTxnAmount =
                        "%.2f".format(printerReceiptData.orignalTxnAmt.toFloat() / 100)
                }
                /*  alignLeftRightText(
                      textInLineFormatBundle,
                      "TXN AMOUNT",
                      emiTxnAmount,
                      ":$currencySymbol"
                  )*/
                printer?.addText(
                    textInLineFormatBundle,
                    formatTextLMR("TXN AMOUNT", ":$currencySymbol", emiTxnAmount, 18)
                )
                if (printerReceiptData.transactionType == TransactionType.TEST_EMI.type) {
                    /*  alignLeftRightText(
                          textInLineFormatBundle,
                          "AUTH AMOUNT",
                          "1.00",
                          ":$currencySymbol"
                      )*/
                    printer?.addText(
                        textInLineFormatBundle,
                        formatTextLMR("AUTH AMOUNT", ":$currencySymbol", "1.00", 18)
                    )
                } else {
                    /* alignLeftRightText(
                         textInLineFormatBundle,
                         "AUTH AMOUNT",
                         authTxnAmount,
                         ":$currencySymbol"
                     )*/
                    printer?.addText(
                        textInLineFormatBundle,
                        formatTextLMR("AUTH AMOUNT", ":$currencySymbol", authTxnAmount, 18)
                    )
                }

            }

            if (printerReceiptData.transactionType == TransactionType.TEST_EMI.type) {
                /* alignLeftRightText(
                     textInLineFormatBundle,
                     "CARD ISSUER",
                     "TEST ISSUER",
                     ":"
                 )*/
                printer?.addText(
                    textInLineFormatBundle,
                    formatTextLMR("CARD ISSUER", ":", "TEST ISSUER", 18)
                )
            } else {
                /*  alignLeftRightText(
                      textInLineFormatBundle,
                      "CARD ISSUER",
                      printerReceiptData.issuerName,
                      ":   "
                  )*/
                printer?.addText(
                    textInLineFormatBundle,
                    formatTextLMR("CARD ISSUER", ":", printerReceiptData.issuerName, 18)
                )
            }


            if (!TextUtils.isEmpty(printerReceiptData.roi)) {
                val rateOfInterest = "%.2f".format(printerReceiptData.roi.toFloat() / 100) + " %"
                //   alignLeftRightText(textInLineFormatBundle, "ROI(p.a)", rateOfInterest, ":   ")

                printer?.addText(
                    textInLineFormatBundle,
                    formatTextLMR("ROI(pa)", ":", rateOfInterest, 18)
                )


            }


            /*  alignLeftRightText(
                  textInLineFormatBundle,
                  "TENURE",
                  "${printerReceiptData.tenure} Months",
                  ":       "
              )*/
            printer?.addText(
                textInLineFormatBundle,
                formatTextLMR("TENURE", ":", "${printerReceiptData.tenure} Months", 18)
            )

            //region===============Processing Fee Changes And Showing On ChargeSlip:-
            if (!TextUtils.isEmpty(printerReceiptData.processingFee)) {
                if ((printerReceiptData.processingFee) != "0") {
                    val procFee = "%.2f".format(printerReceiptData.processingFee.toFloat() / 100)

                    /*   alignLeftRightText(
                           textInLineFormatBundle,
                           "PROC-FEE AMOUNT",
                           procFee,
                           ":$currencySymbol "
                       )*/
                    printer?.addText(
                        textInLineFormatBundle,
                        formatTextLMR("PROC-FEE AMOUNT", ":$currencySymbol", procFee, 18)
                    )
                }
            }
            if (!TextUtils.isEmpty(printerReceiptData.processingFeeRate)) {
                if ((printerReceiptData.processingFeeRate) != "0") {

                    val procFeeAmount =
                        "%.2f".format(printerReceiptData.processingFeeRate.toFloat() / 100) + " %"

                    /*alignLeftRightText(
                        textInLineFormatBundle,
                        "PROC-FEE",
                        procFeeAmount,
                        ":$currencySymbol"
                    )*/
                    printer?.addText(
                        textInLineFormatBundle,
                        formatTextLMR("PROC-FEE", ":$currencySymbol", procFeeAmount, 18)
                    )

                }
            }
            if (!TextUtils.isEmpty(printerReceiptData.totalProcessingFee)) {
                if (!(printerReceiptData.totalProcessingFee).equals("0")) {
                    val totalProcFeeAmount =
                        "%.2f".format(printerReceiptData.totalProcessingFee.toFloat() / 100)
                    /* alignLeftRightText(
                         textInLineFormatBundle,
                         "T-PROC-FEE AMOUNT",
                         totalProcFeeAmount,
                         ":$currencySymbol"
                     )*/
                    printer?.addText(
                        textInLineFormatBundle,
                        formatTextLMR(
                            "T-PROC-FEE AMOUNT",
                            ":$currencySymbol",
                            totalProcFeeAmount,
                            18
                        )
                    )
                }
            }
            //endregion

            var cashBackPercentHeadingText = ""
            var cashBackAmountHeadingText = ""
            var islongTextHeading = true
            when (printerReceiptData.issuerId) {
                "51" -> {
                    cashBackPercentHeadingText = "Mfg/Merch Payback"
                    cashBackAmountHeadingText = "Mfg/Merch-"
                    //  cashBackAmountHeadingText = "Mfg/Merch Payback Amt"
                }
                "64" -> {
                    cashBackPercentHeadingText = "Mfg/Merch Payback"
                    cashBackAmountHeadingText = "Mfg/Merch-"
                    //  cashBackAmountHeadingText = "Mfg/Merch Payback Amt"
                }
                "52" -> {
                    cashBackPercentHeadingText = "Mfg/Merch Cashback"
                    cashBackAmountHeadingText = "Mfg/Merch-"
                    //   cashBackAmountHeadingText = "Mfg/Merch Cashback Amt"
                }
                "55" -> {
                    cashBackPercentHeadingText = "Merch/Mfr Cashback"
                    cashBackAmountHeadingText = "Merch/Mfr-"
                    //  cashBackAmountHeadingText = "Merch/Mfr Cashback Amt"
                }
                else -> {
                    islongTextHeading = false
                    cashBackPercentHeadingText = "CASH BACK"
                    cashBackAmountHeadingText = "TOTAL CASH BACK"
                }
            }
            var nextLineAppendStr = ""
            when (printerReceiptData.issuerId) {
                "51", "64" -> {
                    nextLineAppendStr = "Payback Amt"
                }
                "52", "55" -> {
                    nextLineAppendStr = "Cashback Amt"
                }

            }
            //region=============CashBack CalculatedValue====================
            if (!TextUtils.isEmpty(printerReceiptData.cashBackCalculatedValue)) {
                if (islongTextHeading) {
                    printer?.addText(
                        textInLineFormatBundle,
                        formatTextLMR(
                            cashBackPercentHeadingText,
                            ":$currencySymbol",
                            printerReceiptData.cashBackCalculatedValue,
                            20
                        )
                    )
                } else {
                    /* alignLeftRightText(
                         textInLineFormatBundle,
                         cashBackPercentHeadingText,
                         printerReceiptData.cashBackCalculatedValue, ":$currencySymbol"
                     )*/
                    printer?.addText(
                        textInLineFormatBundle,
                        formatTextLMR(
                            cashBackPercentHeadingText,
                            ":$currencySymbol",
                            printerReceiptData.cashBackCalculatedValue,
                            18
                        )
                    )
                }
            }

            //endregion


            if (!TextUtils.isEmpty(printerReceiptData.cashback) && printerReceiptData.cashback != "0") {
                val cashBackAmount = "%.2f".format(printerReceiptData.cashback.toFloat() / 100)

                if (islongTextHeading) {
                    printer?.addText(
                        textInLineFormatBundle,
                        formatTextLMR(cashBackAmountHeadingText, "", "", 18)
                    )


                    printer?.addText(
                        textInLineFormatBundle,
                        formatTextLMR(nextLineAppendStr, ":$currencySymbol", cashBackAmount, 18)
                    )
                } else {
                    /* alignLeftRightText(
                         textInLineFormatBundle,
                         cashBackAmountHeadingText,
                         cashBackAmount,
                         ":$currencySymbol"
                     )*/
                    printer?.addText(
                        textInLineFormatBundle,
                        formatTextLMR(
                            cashBackAmountHeadingText,
                            ":$currencySymbol",
                            cashBackAmountHeadingText,
                            18
                        )
                    )
                }
            }
            //endregion

            var discountPercentHeadingText = ""
            var discountAmountHeadingText = ""
            islongTextHeading = true
            when (printerReceiptData.issuerId) {
                "51" -> {
                    discountPercentHeadingText = "Mfg/Merch Payback"
                    discountAmountHeadingText = "Mfg/Merch-"
                    //  discountAmountHeadingText = "Mfg/Merch Payback Amt"
                }
                "64" -> {
                    discountPercentHeadingText = "Mfg/Merch Payback"
                    discountAmountHeadingText = "Mfg/Merch-"
                    // discountAmountHeadingText = "Mfg/Merch Payback Amt"
                }
                "52" -> {
                    discountPercentHeadingText = "Mfg/Merch Cashback"
                    discountAmountHeadingText = "Mfg/Merch-"
                    //  discountAmountHeadingText = "Mfg/Merch Cashback Amt"
                }

                "55" -> {
                    discountPercentHeadingText = "Merch/Mfr Cashback"
                    discountAmountHeadingText = "Merch/Mfr"
                    //  discountAmountHeadingText = "Merch/Mfr Cashback Amt"
                }

                else -> {
                    islongTextHeading = false
                    discountPercentHeadingText = "DISCOUNT"
                    discountAmountHeadingText = "TOTAL DISCOUNT"
                }
            }

            if (!TextUtils.isEmpty(printerReceiptData.discountCalculatedValue)) {
                if (islongTextHeading) {
                    printer?.addText(
                        textInLineFormatBundle,
                        formatTextLMR(
                            cashBackPercentHeadingText,
                            ":$currencySymbol",
                            printerReceiptData.discountCalculatedValue,
                            18
                        )
                    )
                } else {
                    /* alignLeftRightText(
                         textInLineFormatBundle,
                         discountPercentHeadingText,
                         printerReceiptData.discountCalculatedValue,": $currencySymbol"
                     )*/
                    printer?.addText(
                        textInLineFormatBundle,
                        formatTextLMR(
                            discountPercentHeadingText,
                            ":$currencySymbol",
                            printerReceiptData.discountCalculatedValue,
                            18
                        )
                    )
                }
            }

            if (!TextUtils.isEmpty(printerReceiptData.cashDiscountAmt) && printerReceiptData.cashDiscountAmt != "0") {
                val discAmount = "%.2f".format(printerReceiptData.cashDiscountAmt.toFloat() / 100)

                if (islongTextHeading) {
                    printer?.addText(
                        textInLineFormatBundle,
                        formatTextLMR(discountAmountHeadingText, "", "", 18)
                    )

                    printer?.addText(
                        textInLineFormatBundle,
                        formatTextLMR(nextLineAppendStr, ":$currencySymbol", discAmount, 18)
                    )
                } else {
                    /*  alignLeftRightText(
                          textInLineFormatBundle,
                          discountAmountHeadingText,
                          discAmount,
                          ":$currencySymbol"
                      )*/
                    printer?.addText(
                        textInLineFormatBundle,
                        formatTextLMR(discountAmountHeadingText, ":$currencySymbol", discAmount, 18)
                    )

                }
            }

            if (!TextUtils.isEmpty(printerReceiptData.loanAmt)) {
                val loanAmount = "%.2f".format(printerReceiptData.loanAmt.toFloat() / 100)
                /* alignLeftRightText(
                     textInLineFormatBundle,
                     "LOAN AMOUNT",
                     loanAmount,
                     ":$currencySymbol"
                 )*/
                printer?.addText(
                    textInLineFormatBundle,
                    formatTextLMR("LOAN AMOUNT", ":$currencySymbol", loanAmount, 18)
                )
            }

            if (!TextUtils.isEmpty(printerReceiptData.monthlyEmi)) {
                val monthlyEmi = "%.2f".format(printerReceiptData.monthlyEmi.toFloat() / 100)
                /* alignLeftRightText(
                     textInLineFormatBundle,
                     "MONTHLY EMI",
                     monthlyEmi,
                     ":$currencySymbol"
                 )*/
                printer?.addText(
                    textInLineFormatBundle,
                    formatTextLMR("MONTHLY EMI", ":$currencySymbol", monthlyEmi, 18)
                )
            }

            if (!TextUtils.isEmpty(printerReceiptData.totalInterest)) {
                val totalInterest = "%.2f".format(printerReceiptData.totalInterest.toFloat() / 100)
                /*alignLeftRightText(
                    textInLineFormatBundle,
                    "TOTAL INTEREST",
                    totalInterest,
                    ":$currencySymbol"
                )*/
                printer?.addText(
                    textInLineFormatBundle,
                    formatTextLMR("TOTAL INTEREST", ":$currencySymbol", totalInterest, 18)
                )
            }

            var totalAmountHeadingText = ""

            // below is the old technique used in old font
            /* totalAmountHeadingText = when (printerReceiptData.issuerId) {
                 "52" -> "TOTAL AMOUNT(incl Int)"
                 "55" -> "TOTAL EFFECTIVE PAYOUT"
                 else -> "TOTAL Amt(With Int)"
             } */
            //  With new font
            totalAmountHeadingText = when (printerReceiptData.issuerId) {
                "52" -> "TOTAL AMOUNT-"
                "55" -> "TOTAL EFFECTIVE-"
                else -> "TOTAL Amt"
            }


            if (!TextUtils.isEmpty(printerReceiptData.totalInterest)) {

                if (printerReceiptData.transactionType == TransactionType.TEST_EMI.type) {

                    val loanAmt = "%.2f".format(printerReceiptData.loanAmt.toFloat() / 100)
                    val totalInterest =
                        "%.2f".format(printerReceiptData.totalInterest.toFloat() / 100)
                    val totalAmt = loanAmt.toDouble().plus(totalInterest.toDouble())
                    /*alignLeftRightText(
                        textInLineFormatBundle,
                        totalAmountHeadingText,
                        totalAmt.toString(),
                        ":$currencySymbol"
                    )*/
                    when (printerReceiptData.issuerId) {
                        "52" -> {
                            printer?.addText(
                                textInLineFormatBundle,
                                formatTextLMR(totalAmountHeadingText, "", "", 18)
                            )
                            printer?.addText(
                                textInLineFormatBundle,
                                formatTextLMR(
                                    "(incl Int)",
                                    ":$currencySymbol",
                                    totalAmt.toString(),
                                    18
                                )
                            )


                        }
                        "55" -> {
                            printer?.addText(
                                textInLineFormatBundle,
                                formatTextLMR(totalAmountHeadingText, "", "", 18)
                            )
                            printer?.addText(
                                textInLineFormatBundle,
                                formatTextLMR("PAYOUT", ":$currencySymbol", totalAmt.toString(), 18)
                            )
                        }
                        else -> {
                            printer?.addText(
                                textInLineFormatBundle,
                                formatTextLMR(totalAmountHeadingText, "", "", 18)
                            )
                            printer?.addText(
                                textInLineFormatBundle,
                                formatTextLMR(
                                    "(With Int)",
                                    ":$currencySymbol",
                                    totalAmt.toString(),
                                    18
                                )
                            )
                        }

                    }

                } else {
                    val f_totalAmt = "%.2f".format(printerReceiptData.netPay.toFloat() / 100)
                    /*alignLeftRightText(
                        textInLineFormatBundle,
                        totalAmountHeadingText,
                        f_totalAmt.toString(),
                        ":$currencySymbol"
                    )*/

                    when (printerReceiptData.issuerId) {
                        "52" -> {
                            printer?.addText(
                                textInLineFormatBundle,
                                formatTextLMR(totalAmountHeadingText, "", "", 18)
                            )
                            printer?.addText(
                                textInLineFormatBundle,
                                formatTextLMR(
                                    "(incl Int)",
                                    ":$currencySymbol",
                                    f_totalAmt.toString(),
                                    18
                                )
                            )


                        }
                        "55" -> {
                            printer?.addText(
                                textInLineFormatBundle,
                                formatTextLMR(totalAmountHeadingText, "", "", 18)
                            )
                            printer?.addText(
                                textInLineFormatBundle,
                                formatTextLMR(
                                    "PAYOUT",
                                    ":$currencySymbol",
                                    f_totalAmt.toString(),
                                    18
                                )
                            )
                        }
                        else -> {
                            printer?.addText(
                                textInLineFormatBundle,
                                formatTextLMR(totalAmountHeadingText, "", "", 18)
                            )
                            printer?.addText(
                                textInLineFormatBundle,
                                formatTextLMR(
                                    "(With Int)",
                                    ":$currencySymbol",
                                    f_totalAmt.toString(),
                                    18
                                )
                            )
                        }

                    }
                }
            }
            printSeperator(seperatorLineBundle)

            centerText(centerTextBundle, "CUSTOMER CONSENT FOR EMI", true)
            //region=======================Issuer Header Terms and Condition=================
            val issuerHeaderTAndC: List<String>
            val testTnc =
                "#.I have been offered the choice of normal as well as EMI for this purchase and I have chosen EMI.#.I have fully understood and accept the terms of EMI scheme and applicable charges mentioned in this charge-slip.#.EMI conversion subject to Banks discretion and by take minimum * working days.#.GST extra on the interest amount.#.For the first EMI, the interest will be calculated from the loan booking date till the payment due date.#.Convenience fee of Rs --.-- + GST will be applicable on EMI transactions."
            issuerHeaderTAndC =
                if (printerReceiptData.transactionType == TransactionType.TEST_EMI.type) {
                    testTnc.split(SplitterTypes.POUND.splitter)
                } else {
                    issuerTAndCData.headerTAndC.split(SplitterTypes.POUND.splitter)
                }

            if (issuerHeaderTAndC.size > 1) {
                for (i in 1 until issuerHeaderTAndC.size) {
                    if (!TextUtils.isEmpty(issuerHeaderTAndC[i])) {
                        val limit = 46
                        if (!(issuerHeaderTAndC[i].isBlank())) {
                            val emiTnc = "#" + issuerHeaderTAndC[i]
                            val chunks: List<String> = chunkTnC(emiTnc, limit)
                            for (st in chunks) {
                                logger("issuerHeaderTAndC", st, "e")
                                alignLeftRightText(textInLineFormatBundle, st, "")
                            }
                        }
                    }
                }
            }
            //endregion

            printer?.feedLine(1)

            //region=====================BRAND TAndC===============
            val brandId = if(printerReceiptData.transactionType == TransactionType.BRAND_EMI.type){
                brandEmiData?.brandID?:"0"
            }else{
                val productData =
                    BrandEMIAccessDataModalTable.getBrandEMIAccessCodeDataByInvoice(hostInvoice)
                productData?.brandID?:""
            }
            val brandTnc =
                BrandTAndCTable.getBrandTncBybrandId(brandId?:"0")?:""
            val chunk: List<String> = chunkTnC(brandTnc)
            for (st in chunk) {
                logger("Brand Tnc", st, "e")
                alignLeftRightText(
                    textInLineFormatBundle,
                    st.replace(bankEMIFooterTAndCSeparator, "")
                        .replace(disclaimerIssuerClose, ""),
                    ""
                )
            }
            //endregion



            //region=====================SCHEME TAndC===============
            val emiCustomerConsent =
                printerReceiptData.bankEmiTAndC.split(SplitterTypes.POUND.splitter)
            if (emiCustomerConsent.size > 1) {
                for (i in emiCustomerConsent.indices) {
                    val limit = 48
                    if (!(emiCustomerConsent[i].isNullOrBlank())) {
                        val emiTnc = "#" + emiCustomerConsent[i]
                        val chunks: List<String> = chunkTnC(emiTnc, limit)
                        for (st in chunks) {
                            logger("emiCustomerConsent", st, "e")
                            alignLeftRightText(
                                textInLineFormatBundle,
                                st.replace(bankEMIFooterTAndCSeparator, "")
                                    .replace(disclaimerIssuerClose, ""),
                                ""
                            )
                        }
                    }

                }
            }
            //endregion


            //region=====================Printing Merchant Brand Purchase Details:-
            if (printerReceiptData.transactionType == TransactionType.BRAND_EMI.type) {
                //region====================Printing DBD Wise TAndC Brand EMI==================
                if (copyType == EPrintCopyType.MERCHANT && (brandEmiData?.brandReservedValues?.get(3) == '1')) {
                    if (!TextUtils.isEmpty(printerReceiptData.tenureWiseDBDTAndC)) {
                        val tenureWiseTAndC: List<String> =
                            chunkTnC(printerReceiptData.tenureWiseDBDTAndC)
                        for (st in tenureWiseTAndC) {
                            logger("tenureWiseDBDTAndC", st, "e")
                            alignLeftRightText(textFormatBundle, st, "", "")
                        }
                    }
                }
                //endregion

                printSeperator(seperatorLineBundle)
                centerText(centerTextBundle, "-----**Product Details**-----", true)
                if (brandEmiData != null) {

                    printer?.addText(
                        textInLineFormatBundle,
                        formatTextLMR("Merch/Mfr Name", ":", brandEmiData.brandName, 14)
                    )

                    printer?.addText(
                        textInLineFormatBundle,
                        formatTextLMR("Prod Category", ":", brandEmiData.categoryName, 14)
                    )
                    if (brandEmiData.producatDesc == "subCat") {

                        printer?.addText(
                            textInLineFormatBundle,
                            formatTextLMR("Prod desc", ":", brandEmiData.childSubCategoryName, 14)
                        )
                    }

                    printer?.addText(
                        textInLineFormatBundle,
                        formatTextLMR("Prod", ":", brandEmiData.productName, 10)
                    )


                    if (!TextUtils.isEmpty(brandEmiData.imeiNumber)) {

                        printer?.addText(
                            textInLineFormatBundle,
                            formatTextLMR(
                                "Prod ${brandEmiData.validationTypeName}",
                                ":",
                                brandEmiData.imeiNumber,
                                14
                            )
                        )
                    }
                    if (!TextUtils.isEmpty(printerReceiptData.merchantMobileNumber)) {
                        when (brandEmiData.brandReservedValues.substring(1, 2)) {
                            "1" -> {
                                // MASK PRINT
                                val maskedMob = panMasking(
                                    printerReceiptData.merchantMobileNumber,
                                    "000****000"
                                )

                                printer?.addText(
                                    textInLineFormatBundle,
                                    formatTextLMR("Mobile No", ":", maskedMob, 14)
                                )

                            }
//PLAIN PRINT
                            "2" -> {

                                printer?.addText(
                                    textInLineFormatBundle,
                                    formatTextLMR(
                                        "Mobile No",
                                        ":",
                                        printerReceiptData.merchantMobileNumber,
                                        14
                                    )
                                )
                            }
                            else -> {
                                // NO PRINT
                            }
                        }
                    }
                }
            }
            else if (printerReceiptData.transactionType == TransactionType.BRAND_EMI_BY_ACCESS_CODE.type) {
                val productData =
                    BrandEMIAccessDataModalTable.getBrandEMIAccessCodeDataByInvoice(hostInvoice)
                //region====================Printing DBD Wise TAndC Brand EMI By code==================
                 if (copyType == EPrintCopyType.MERCHANT && (productData?.brandReservField?.get(3) == '1')) {
                    if (!TextUtils.isEmpty(printerReceiptData.tenureWiseDBDTAndC)) {
                        val tenureWiseTAndC: List<String> =
                            chunkTnC(printerReceiptData.tenureWiseDBDTAndC)
                        for (st in tenureWiseTAndC) {
                            logger("dbdTNC_By Code", st, "e")
                            alignLeftRightText(textFormatBundle, st, "", "")
                        }
                    }
                }
                //endregion

                printSeperator(seperatorLineBundle)
                centerText(centerTextBundle, "-----**Product Details**-----", true)
                if (productData != null) {
                    printer?.addText(
                        textInLineFormatBundle, formatTextLMR("Merch/Mfr Name",":",productData.brandName,14)
                    )
                    printer?.addText(
                        textInLineFormatBundle,formatTextLMR("Prod Category", ":", productData.productBaseCat,14)
                    )
                    printer?.addText(
                        textInLineFormatBundle,
                        formatTextLMR("Prod desc", ":", productData.productCategoryName, 14)
                    )
                    printer?.addText(
                        textInLineFormatBundle,
                        formatTextLMR("Prod", ":", productData.productName, 10)
                    )

                    printer?.addText(
                        textInLineFormatBundle, formatTextLMR("Prod Iemei No",":"  ,productData.productSerialCode,14)
                    )

                    when (productData.brandReservField.substring(1, 2)) {
                        "1" -> {
                            // MASK PRINT
                            val maskedMob = panMasking(
                                productData.mobileNo,
                                "000****000"
                            )
                            printer?.addText(
                                textInLineFormatBundle,
                                formatTextLMR(
                                    "Mobile No",
                                    ":",
                                    maskedMob,
                                    14
                                )
                            )

                        }
//PLAIN PRINT
                        "2" -> {
                            printer?.addText(
                                textInLineFormatBundle,
                                formatTextLMR(
                                    "Mobile No ",
                                    ":",
                                    productData.mobileNo,
                                    14
                                )
                            )
                        }
                        else -> {
                            // NO PRINT
                        }
                    }


                }
            }
            //endregion


            //region====================Printing Tenure TAndC==================
            if (!TextUtils.isEmpty(printerReceiptData.tenureTAndC)) {
                printSeperator(seperatorLineBundle)
                val tenureTAndC: List<String> = chunkTnC(printerReceiptData.tenureTAndC)
                for (st in tenureTAndC) {
                    logger("TNC", st, "e")
                    alignLeftRightText(textFormatBundle, st, "", "")
                }
            }
            //endregion

            printSeperator(seperatorLineBundle)
            printer?.feedLine(1)
            if (!TextUtils.isEmpty(printerReceiptData.emiTransactionAmount)) {
                val baseAmount = "%.2f".format(printerReceiptData.transactionAmt.toFloat() / 100)

                if (printerReceiptData.transactionType == TransactionType.TEST_EMI.type) {
                    centerText(
                        centerTextBundle,
                        "BASE AMOUNT  :     $currencySymbol  1.00",
                        true
                    )
                } else {
                    centerText(
                        centerTextBundle,
                        "BASE AMOUNT  :     $currencySymbol  $baseAmount",
                        true
                    )
                }
            }
            customSpaceLine(textFormatBundle, "  ")
            if (printerReceiptData.isPinverified) {
                centerText(centerTextBundle, pinVerifyMsg)
                centerText(centerTextBundle, signatureMsg)
                centerText(centerTextBundle, printerReceiptData.cardHolderName)

            } else {
                alignLeftRightText(textFormatBundle, pinVerifyMsg, "", "")
                alignLeftRightText(textFormatBundle, signatureMsg, "", "")
            }

            val ipt =
                IssuerParameterTable.selectFromIssuerParameterTable(AppPreference.WALLET_ISSUER_ID)
            val chunks: List<String>? = ipt?.volletIssuerDisclammer?.let { chunkTnC(it) }
            if (chunks != null) {
                for (st in chunks) {
                    logger("TNC", st, "e")
                    alignLeftRightText(textFormatBundle, st, "", "")
                }
            }
//val bunn=Bundle()

            textFormatBundle.putInt(
                PrinterConfig.addText.FontSize.BundleName,
                0
            )
            textFormatBundle.putInt(
                PrinterConfig.addText.Alignment.BundleName,
                PrinterConfig.addText.Alignment.CENTER
            )

            printer?.addText(textFormatBundle, copyType.pName)
            printer?.addText(textFormatBundle, footerText[0])
            printer?.addText(textFormatBundle, footerText[1])

            printLogo("BH.bmp")

            /* textFormatBundle.putInt(
                 PrinterConfig.addText.FontSize.BundleName,
                 PrinterConfig.addText.FontSize.NORMAL_24_24
             )
             textFormatBundle.putInt(
                 PrinterConfig.addText.Alignment.BundleName,
                 PrinterConfig.addText.Alignment.CENTER
             )*/
            printer?.addText(textFormatBundle, "App Version : ${BuildConfig.VERSION_NAME}")


            //printSeperator(textFormatBundle)


            //region=======================Issuer Footer Terms and Condition=================
            if (!TextUtils.isEmpty(issuerTAndCData.footerTAndC)) {
                printSeperator(seperatorLineBundle)
                printer?.feedLine(1)
                val issuerFooterTAndC =
                    issuerTAndCData.footerTAndC.split(SplitterTypes.POUND.splitter)
                if (issuerFooterTAndC.size > 1) {
                    for (i in 1 until issuerFooterTAndC.size) {
                        if (!TextUtils.isEmpty(issuerFooterTAndC[i])) {
                            val limit = 48
                            val emiTnc = "#" + issuerFooterTAndC[i]
                            val chunks: List<String> = chunkTnC(emiTnc, limit)
                            for (st in chunks) {
                                logger("TNC", st, "e")
                                alignLeftRightText(textInLineFormatBundle, st, "")
                            }
                        }
                    }
                } else {
                    alignLeftRightText(
                        textInLineFormatBundle,
                        "# ${issuerTAndCData.footerTAndC}",
                        ""
                    )
                }
            }
            //endregion

            printer?.feedLine(4)

            // start print here and callback of printer:-
            printer?.startPrint(object : PrinterListener.Stub() {
                override fun onFinish() {
                    val msg = Message()
                    msg.data.putString("msg", "print finished")
                    //VFService.showToast("Printing Successfully")
                    when (copyType) {
                        EPrintCopyType.MERCHANT -> {
                            GlobalScope.launch(Dispatchers.Main) {
                                if (printerReceiptData.transactionType == TransactionType.EMI_SALE.type ||
                                    printerReceiptData.transactionType == TransactionType.BRAND_EMI.type ||
                                    printerReceiptData.transactionType == TransactionType.BRAND_EMI_BY_ACCESS_CODE.type ||
                                    printerReceiptData.transactionType == TransactionType.TEST_EMI.type
                                )
                                    (context as VFTransactionActivity).showMerchantAlertBox(
                                        this@PrintUtil,
                                        printerReceiptData,
                                        true
                                    ) { dialogCB ->
                                        printerCallback(dialogCB, 1)
                                    }
                            }

                        }
                        EPrintCopyType.CUSTOMER -> {
                            //VFService.showToast("Customer Transaction Slip Printed Successfully")
                            printerCallback(false, 1)
                        }
                        EPrintCopyType.DUPLICATE -> {
                            //  VFService.showToast("Success")
                            printerCallback(true, 1)
                        }
                    }
                }

                override fun onError(error: Int) {
                    if (error == 240) {
                        //VFService.showToast("Printing roll not available..")
                        printerCallback(false, 0)
                    } else {
                        //VFService.showToast("Printer Error------> $error")
                        printerCallback(false, 0)
                    }
                }
            })
            //====================
        } catch (ex: DeadObjectException) {
            ex.printStackTrace()
            failureImpl(
                context as Activity,
                "Printer Service stopped.",
                "Please take ChargeSlip from the Report menu."
            )
        } catch (e: RemoteException) {
            e.printStackTrace()
            failureImpl(
                context as Activity,
                "Printer Service stopped.",
                "Please take ChargeSlip from the Report menu."
            )
        } catch (ex: Exception) {
            ex.printStackTrace()
            failureImpl(
                context as Activity,
                "Printer Service stopped.",
                "Please take ChargeSlip from the Report menu."
            )
        }
    }

    //endregion

    fun printPendingPreauth(
        cardProcessedDataModal: CardProcessedDataModal,
        context: Context?,
        pendingPreauthData: MutableList<PendingPreauthData>,
        printerCallback: (Boolean) -> Unit
    ) {
        try {
            val format = Bundle()
            val fmtAddTextInLine = Bundle()
            val tpt = TerminalParameterTable.selectFromSchemeTable()
            val headers = arrayListOf<String>()
            /* tpt?.receiptHeaderOne?.let { headers.add(it) }
             tpt?.receiptHeaderTwo?.let { headers.add(it) }
             tpt?.receiptHeaderThree?.let { headers.add(it) }
             setHeaderWithLogo(format, "hdfc_print_logo.bmp", headers, context)*/

            setLogoAndHeader()

            val time = cardProcessedDataModal.getTime()
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

            alignLeftRightText(
                fmtAddTextInLine,
                "DATE:${dateFormater(cardProcessedDataModal.getTimeStamp()?.toLong() ?: 0L)}",
                "TIME:$formattedTime"
            )
            alignLeftRightText(
                fmtAddTextInLine,
                "MID:${tpt?.merchantId}",
                "TID:${tpt?.terminalId}"
            )
            printTransType(format, cardProcessedDataModal.getTransType())
            printSeperator(format)

            for (data in pendingPreauthData) {
                printPendingPreauthSingleRecord(fmtAddTextInLine, data)
                printSeperator(format)
            }
            printer?.addText(format, footerText[1])
            bHLogoWithAppVersion(format)

            printer?.startPrint(object : PrinterListener.Stub() {
                override fun onFinish() {
                    logger("PRINTING", "Printing Success in Pending PreAuth")
                    //VFService.showToast("Customer Transaction Slip Printed Successfully")
                    printerCallback(true)
                }

                override fun onError(error: Int) {
                    logger("PRINTING", "Printing Fail in Pending PreAuth")
                    if (error == 240)
                    //VFService.showToast("Printing roll not available..")
                    else
                    //VFService.showToast("Printer Error------> $error")
                        printerCallback(false)
                }
            })

        } catch (ex: DeadObjectException) {
            ex.printStackTrace()
            failureImpl(
                context as Activity,
                "Printer Service stopped.",
                "Please take chargeslip from the Report menu."
            )
        } catch (e: RemoteException) {
            e.printStackTrace()
            failureImpl(
                context as Activity,
                "Printer Service stopped.",
                "Please take chargeslip from the Report menu."
            )
        } catch (ex: Exception) {
            ex.printStackTrace()
            failureImpl(
                context as Activity,
                "Printer Service stopped.",
                "Please take chargeslip from the Report menu."
            )
        }
    }


    fun printCrossSellReport(reportDataList: ArrayList<ReportDownloadedModel>) {
        printSeperator(Bundle())
        centerText(Bundle(), "CROSS SELL REPORT", true)
        printSeperator(Bundle())
        alignLeftRightText(Bundle(), "Product", "4 Digit Mobile No")
        alignLeftRightText(Bundle(), "Txn Ref No", "Status")
        alignLeftRightText(Bundle(), "Date & Time", "")
        printSeperator(Bundle())


        val rr = arrayListOf<TotalCrossellRep>()
        val testArrayL = arrayListOf<HashMap<String, String>>()
        for (data in reportDataList) {
            val reqMap = hashMapOf<String, String>()
            var reqType = ""
            val totalR = TotalCrossellRep()
            if (data.requestTypeId == "1") {
                reqType = "Insta Loan"
                reqMap[data.requestTypeId] = data.requestStatus
                totalR.reqStatus = data.requestStatus
                totalR.reqType = data.requestTypeId
            }
            if (data.requestTypeId == "3") {
                reqType = "Jumbo Loan"
                reqMap[data.requestTypeId] = data.requestStatus
                totalR.reqStatus = data.requestStatus
                totalR.reqType = data.requestTypeId
            }
            if (data.requestTypeId == "5") {
                reqType = "Credit Limit Increase"
                reqMap[data.requestTypeId] = data.requestStatus
                totalR.reqStatus = data.requestStatus
                totalR.reqType = data.requestTypeId
            }
            if (data.requestTypeId == "9") {
                reqType = "HDFC Credit Card"
                reqMap[data.requestTypeId] = data.requestStatus
                totalR.reqStatus = data.requestStatus
                totalR.reqType = data.requestTypeId
            }
            rr.add(totalR)
            testArrayL.add(reqMap)
            alignLeftRightText(Bundle(), reqType, data.mobile)
            var status = ""
            if (data.requestStatus == "1") {
                status = "Request send to customer"
            }
            if (data.requestStatus == "2") {
                status = "Request submitted to bank"
            }
            alignLeftRightText(Bundle(), data.transactionRefNo, status)
            alignLeftRightText(Bundle(), data.requestDate, "")
            printSeperator(Bundle())
        }
        val requestTypeList = arrayListOf<CrossSellReportWithType>()
        val obj1 = CrossSellReportWithType()
        obj1.type = "1"
        obj1.typeName = "Insta Loan"

        val obj2 = CrossSellReportWithType()
        obj2.type = "3"
        obj2.typeName = "Jumbo Loan"

        val obj3 = CrossSellReportWithType()
        obj3.type = "5"
        obj3.typeName = "Credit Limit Increase"

        val obj4 = CrossSellReportWithType()
        obj4.type = "9"
        obj4.typeName = "HDFC Credit Card"

        requestTypeList.add(obj1)
        requestTypeList.add(obj2)
        requestTypeList.add(obj3)
        requestTypeList.add(obj4)

        printer?.setLineSpace(2)
        alignLeftRightText(Bundle(), "Product", "S.C", "S.B")
        for (i in 0 until requestTypeList.size) {
            val list1 = rr.filter { it.reqType == requestTypeList[i].type && it.reqStatus == "1" }
            val list2 = rr.filter { it.reqType == requestTypeList[i].type && it.reqStatus == "2" }
            println("${requestTypeList[i].typeName}--> ${list1.size} ,  ${list2.size}")
            alignLeftRightText(
                Bundle(),
                requestTypeList[i].typeName,
                list1.size.toString(),
                list2.size.toString()
            )
        }

        printSeperator(Bundle())

        centerText(textFormatBundle, "BonusHub")
        centerText(textFormatBundle, "App Version:${BuildConfig.VERSION_NAME}")
        printer?.feedLine(4)
        printer?.startPrint(object : PrinterListener.Stub() {
            override fun onFinish() {
                //  val msg = Message()
                VFService.showToast("Printing Successfully")
            }

            override fun onError(error: Int) {
                if (error == 240) {
                    VFService.showToast("Printing roll not available..")
                    //   printerCallback(false, 0)
                } else {
                    VFService.showToast("Printer Error------> $error")
                    //   printerCallback(false, 0)
                }
            }
        })
    }


    private fun getNameByTransactionType(transactionType: Int): String {
        var tTyp = ""
        for (e in TransactionType.values()) {
            if (e.type == transactionType) {
                tTyp = e.txnTitle
                break
            }
        }
        return tTyp
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

    private fun printPendingPreauthSingleRecord(
        format: Bundle,
        pendingPreauthData: PendingPreauthData
    ) {
        try {
            alignLeftRightText(
                format,
                "BATCH NO:${invoiceWithPadding(pendingPreauthData.batch.toString())}",
                "ROC:${invoiceWithPadding(pendingPreauthData.roc.toString())}"
            )

            alignLeftRightText(
                format,
                "PAN:${pendingPreauthData.pan}",
                "AMT:${"%.2f".format(pendingPreauthData.amount)}"
            )
            alignLeftRightText(
                format,
                "DATE:${pendingPreauthData.date}",
                "TIME:${pendingPreauthData.time}"
            )
        } catch (ex: DeadObjectException) {
            throw ex
        } catch (ex: RemoteException) {
            throw ex
        } catch (ex: Exception) {
            throw ex
        }

    }

    private fun printDigiPosReport(callBack: (Boolean) -> Unit) {
        val digiPosDataList =
            DigiPosDataTable.selectDigiPosDataAccordingToTxnStatus(EDigiPosPaymentStatus.Approved.desciption) as ArrayList<DigiPosDataTable>
        if (digiPosDataList.size == 0) {
            Log.e("UPLOAD DIGI", " ----------------------->  NO SUCCESS TXN FOUND")
            // cb(true)
            return
        }
        centerText(textInLineFormatBundle, "Digi POS Report")
        centerText(
            textInLineFormatBundle,
            "TID : ${TerminalParameterTable.selectAll()[0].terminalId}"
        )
        printSeperator(textFormatBundle)
        alignLeftRightText(textInLineFormatBundle, "TXN TYPE", "TOTAL", "COUNT")
        printSeperator(textFormatBundle)
        // txn

        printSeperator(textFormatBundle)

        // start print here
        printer?.startPrint(object : PrinterListener.Stub() {
            override fun onFinish() {
                callBack(true)
                Log.e("Settle_RECEIPT", "SUCESS__")
            }

            override fun onError(error: Int) {
                callBack(false)
                Log.e("Settle_RECEIPT", "FAIL__")
            }


        })
    }

    private fun bHLogoWithAppVersion(format: Bundle) {

        try {

            printLogo("BH.bmp")

            format.putInt(
                PrinterConfig.addText.FontSize.BundleName,
                PrinterConfig.addText.FontSize.NORMAL_24_24
            )
            format.putInt(
                PrinterConfig.addText.Alignment.BundleName,
                PrinterConfig.addText.Alignment.CENTER
            )
            printer?.addText(format, "App Version : ${BuildConfig.VERSION_NAME}")

            // printer?.addText(format, "---------X-----------X----------")
            printer?.feedLine(4)
        } catch (ex: DeadObjectException) {
            throw ex
        } catch (ex: RemoteException) {
            throw ex
        } catch (ex: Exception) {
            throw ex
        }
    }

    private fun hasPin(printerReceiptData: BatchFileDataTable) {
        signatureMsg = if (printerReceiptData.isPinverified) {
            "SIGNATURE NOT REQUIRED"
        } else {
            "SIGN ..................."
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
                PrinterConfig.addText.FontSize.LARGE_DH_32_64_IN_BOLD
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

    private fun setLogoAndHeader(logo: String? = HDFC_LOGO) {
        try {
            val tpt = TerminalParameterTable.selectFromSchemeTable()
            val hdfcTpt = HdfcTpt.selectAllHDFCTPTData()[0]
            val headers = arrayListOf<String>()
            if (hdfcTpt.defaultMerchantName.isBlank()) {
                tpt?.receiptHeaderOne?.let { headers.add(it.trim()) }
            } else {
                headers.add(hdfcTpt.defaultMerchantName.trim())
            }
            if (hdfcTpt.receiptL2.isBlank()) {
                tpt?.receiptHeaderTwo?.let { headers.add(it.trim()) }
            } else {
                headers.add(hdfcTpt.receiptL2.trim())
            }
            if (hdfcTpt.receiptL3.isBlank()) {
                tpt?.receiptHeaderThree?.let { headers.add(it.trim()) }
            } else {
                headers.add(hdfcTpt.receiptL3)
            }
            setHeaderWithLogo(Bundle(), logo, headers)
        } catch (ex: DeadObjectException) {
            throw ex
        } catch (ex: RemoteException) {
            throw ex
        } catch (ex: Exception) {
            throw ex
        }
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
                textInLineFormatBundle, "DATE:${printerReceiptData.transactionDate}",
                "", "TIME:$formattedTime", 0
            )
        } catch (ex: DeadObjectException) {
            throw ex
        } catch (ex: RemoteException) {
            throw ex
        } catch (ex: Exception) {
            throw ex
        }
    }
}

fun getCurrencySymbol(tpt: TerminalParameterTable?): String {
    return if (!TextUtils.isEmpty(tpt?.currencySymbol)) {
        tpt?.currencySymbol ?: "Rs"
    } else {
        "Rs"
    }


}

fun checkForPrintReversalReceipt(
    context: Context?,
    field60Data: String,
    callback: (String) -> Unit
) {
    if (!TextUtils.isEmpty(AppPreference.getString(AppPreference.GENERIC_REVERSAL_KEY))) {
        val tpt = TerminalParameterTable.selectFromSchemeTable()
        tpt?.cancledTransactionReceiptPrint?.let { logger("CancelPrinting", it, "e") }
        if (tpt?.cancledTransactionReceiptPrint == "01") {
            PrintUtil(context).printReversal(context, field60Data) {
                callback(it)
            }
        } else {
            callback("")
        }
    } else {
        callback("")
    }
}

internal data class SummeryModel(
    val type: String,
    var count: Int = 0,
    var total: Long = 0,
    var hostTid: String
)

internal data class SummeryTotalType(var count: Int = 0, var total: Long = 0)
internal open class IPrintListener(
    var printerUtil: PrintUtil,
    var context: Context?,
    var copyType: EPrintCopyType,
    var batch: BatchFileDataTable,
    var isSuccess: (Boolean, Int) -> Unit
) : PrinterListener.Stub() {
    @Throws(RemoteException::class)
    override fun onError(error: Int) {
        if (error == 240)
        //VFService.showToast("Printing roll not available..")
            isSuccess(true, 0)
        else
        //VFService.showToast("Printer Error------> $error")
            isSuccess(false, 0)
    }

    @Throws(RemoteException::class)
    override fun onFinish() {
        val msg = Message()
        msg.data.putString("msg", "print finished")
        // VFService.showToast("Printing Successfully")
        when (copyType) {
            EPrintCopyType.MERCHANT -> {
                GlobalScope.launch(Dispatchers.Main) {
                    if (batch.transactionType == TransactionType.TIP_SALE.type || batch.transactionType == TransactionType.VOID.type || batch.transactionType == TransactionType.VOID_EMI.type) {
                        (context as BaseActivity).showMerchantAlertBoxForTipSale(
                            printerUtil,
                            batch
                        ) { dialogCB ->
                            isSuccess(dialogCB, 1)
                        }
                    } else {
                        (context as VFTransactionActivity).showMerchantAlertBox(
                            printerUtil,
                            batch
                        ) { dialogCB ->
                            isSuccess(dialogCB, 1)
                        }
                    }
                }

            }
            EPrintCopyType.CUSTOMER -> {
                //VFService.showToast("Customer Transaction Slip Printed Successfully")
                isSuccess(false, 1)
            }
            EPrintCopyType.DUPLICATE -> {
                isSuccess(true, 1)
            }
        }
    }
}


internal open class ISmSUpiPrintListener(
    var printerUtil: PrintUtil,
    var context: Context?,
    var copyType: EPrintCopyType,
    var digiPosData: DigiPosDataTable,
    var isSuccess: (Boolean, Int) -> Unit
) : PrinterListener.Stub() {
    @Throws(RemoteException::class)
    override fun onError(error: Int) {
        if (error == 240)
        //VFService.showToast("Printing roll not available..")
            isSuccess(true, 0)
        else
        //VFService.showToast("Printer Error------> $error")
            isSuccess(false, 0)
    }

    @Throws(RemoteException::class)
    override fun onFinish() {
        val msg = Message()
        msg.data.putString("msg", "print finished")
        // VFService.showToast("Printing Successfully")
        when (copyType) {
            EPrintCopyType.MERCHANT -> {
                GlobalScope.launch(Dispatchers.Main) {

                    (context as BaseActivity).showMerchantAlertBoxSMSUpiPay(
                        printerUtil,
                        digiPosData
                    ) { dialogCB ->
                        isSuccess(dialogCB, 1)
                    }

                }

            }
            EPrintCopyType.CUSTOMER -> {
                //VFService.showToast("Customer Transaction Slip Printed Successfully")
                isSuccess(false, 1)
            }
            EPrintCopyType.DUPLICATE -> {
                isSuccess(true, 1)
            }
        }
    }
}

internal open class IAuthCompletePrintListener(
    var printerUtil: PrintUtil,
    var context: Context?,
    var copyType: EPrintCopyType,
    var batch: BatchFileDataTable,
    var isSuccess: (Boolean) -> Unit
) : PrinterListener.Stub() {
    @Throws(RemoteException::class)
    override fun onError(error: Int) {

        if (error == 240) {

            VFService.showToast("Printing roll not available..")
            isSuccess(true)
        } else {
            VFService.showToast("Printer Error------> $error")
            isSuccess(false)
        }
    }

    @Throws(RemoteException::class)
    override fun onFinish() {
        val msg = Message()
        msg.data.putString("msg", "print finished")
        //VFService.showToast("Printing Successfully")
        when (copyType) {
            EPrintCopyType.MERCHANT -> {
                var toastMsg = ""
                when (batch.transactionType) {
                    TransactionType.PRE_AUTH_COMPLETE.type -> {
                        toastMsg = context?.getString(R.string.comp_preauth_success).toString()

                    }
                    TransactionType.VOID_PREAUTH.type -> {
                        toastMsg = context?.getString(R.string.void_preauth_success).toString()
                    }
                }
                GlobalScope.launch(Dispatchers.Main) {
                    /*  val toast = Toast.makeText(
                          context,
                          toastMsg,
                          Toast.LENGTH_LONG
                      )
                      toast.setGravity(Gravity.CENTER_VERTICAL or Gravity.CENTER_HORIZONTAL, 0, 0)
                      toast.show()
                      delay(4000)*/
                    (context as BaseActivity).showMerchantAlertBox1(
                        printerUtil,
                        batch
                    ) { dialogCB ->
                        isSuccess(dialogCB)
                    }
                }
            }
            EPrintCopyType.CUSTOMER -> {
                /*  context?.startActivity(Intent(context, MainActivity::class.java).apply {
                      flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_CLEAR_TASK
                  })*/
                isSuccess(false)
            }
            EPrintCopyType.DUPLICATE -> {
                isSuccess(true)
            }
        }

    }
}

fun initializeFontFiles() = initialize(VerifoneApp.appContext.assets)
internal open class ISettlementPrintListener(
    var context: Context?,
    var settlementByteArray: ByteArray
) : PrinterListener.Stub() {
    @Throws(RemoteException::class)
    override fun onError(error: Int) {
        Log.d("Failure:- ", "Settlement Print Failure Result.....")
    }

    @Throws(RemoteException::class)
    override fun onFinish() {
        Log.d("Success:- ", "Settlement Print Success Result.....")
        Handler(Looper.getMainLooper()).postDelayed({
            GlobalScope.launch {
                (context as MainActivity).settleBatch(settlementByteArray)
            }
        }, 400)

    }
}

fun formatTextLMR(leftTxt: String, middleText: String, rightTxt: String, totalLen: Int): String {
//val tLength=18
    val padded = addPad(leftTxt, " ", totalLen, false)
    return "$padded$middleText $rightTxt"

}

enum class EPrintCopyType(val pName: String) {
    MERCHANT("**MERCHANT COPY**"), CUSTOMER("**CUSTOMER COPY**"), DUPLICATE("**DUPLICATE COPY**");
}
