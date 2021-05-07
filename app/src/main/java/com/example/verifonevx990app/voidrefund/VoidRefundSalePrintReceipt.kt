package com.example.verifonevx990app.voidrefund

import android.content.Context
import android.content.res.AssetManager
import android.os.Bundle
import android.os.DeadObjectException
import android.os.RemoteException
import android.util.Log
import com.example.verifonevx990app.BuildConfig
import com.example.verifonevx990app.realmtables.BatchFileDataTable
import com.example.verifonevx990app.realmtables.IssuerParameterTable
import com.example.verifonevx990app.realmtables.TerminalParameterTable
import com.example.verifonevx990app.utils.printerUtils.EPrintCopyType
import com.example.verifonevx990app.utils.printerUtils.PrintUtil
import com.example.verifonevx990app.utils.printerUtils.PrinterFonts
import com.example.verifonevx990app.vxUtils.*
import com.vfi.smartpos.deviceservice.aidl.IPrinter
import com.vfi.smartpos.deviceservice.aidl.PrinterConfig
import com.vfi.smartpos.deviceservice.aidl.PrinterListener
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import java.io.IOException
import java.io.InputStream
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.*

class VoidRefundSalePrintReceipt {
    var printer: IPrinter? = null
    private var footerText = arrayOf("*Thank You Visit Again*", "POWERED BY")

    init {
        printer = VFService.vfPrinter
        initializeFontFiles()
    }

    fun startPrintingVoidRefund(
        printerReceiptData: BatchFileDataTable,
        transactionType: Int,
        copyType: EPrintCopyType,
        context: Context?,
        printerCallback: (Boolean, Boolean) -> Unit
    ) {
        val signatureMsg = if (printerReceiptData.isPinverified) {
            "SIGNATURE NOT REQUIRED"
        } else {
            "SIGN ..............................................."
        }

        val pinVerifyMsg = if (printerReceiptData.isPinverified) {
            "PIN VERIFIED OK"
        } else {
            ""
        }
        try {
            // bundle format for addText
            val format = Bundle()

            // bundle formate for AddTextInLine
            val fmtAddTextInLine = Bundle()

            printLogo("hdfc_print_logo.bmp")

            format.putInt(
                PrinterConfig.addText.FontSize.BundleName,
                PrinterConfig.addText.FontSize.NORMAL_24_24
            )
            format.putInt(
                PrinterConfig.addText.Alignment.BundleName,
                PrinterConfig.addText.Alignment.CENTER
            )
            printer?.addText(format, printerReceiptData.merchantName) // header1


            format.putInt(
                PrinterConfig.addText.FontSize.BundleName,
                PrinterConfig.addText.FontSize.NORMAL_24_24
            )
            format.putInt(
                PrinterConfig.addText.Alignment.BundleName,
                PrinterConfig.addText.Alignment.CENTER
            )
            printer?.addText(format, printerReceiptData.merchantAddress1) //header2


            format.putInt(
                PrinterConfig.addText.FontSize.BundleName,
                PrinterConfig.addText.FontSize.NORMAL_24_24
            )
            format.putInt(
                PrinterConfig.addText.Alignment.BundleName,
                PrinterConfig.addText.Alignment.CENTER
            )
            printer?.addText(format, printerReceiptData.merchantAddress2) //header3


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
            } catch (e: ParseException) {
                e.printStackTrace()
            }


            printer?.addTextInLine(
                fmtAddTextInLine, "DATE : ${printerReceiptData.transactionDate}",
                "", "TIME : $formattedTime", 0
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
                "MID : ${printerReceiptData.mid}",
                "",
                "TID : ${printerReceiptData.tid}",
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
                "BATCH NO : ${printerReceiptData.batchNumber}",
                "",
                "ROC : ${
                    invoiceWithPadding(ROCProviderV2.getRoc(AppPreference.getBankCode()).toString())
                }",
                PrinterConfig.addTextInLine.mode.Devide_flexible
            )
            printer?.addTextInLine(
                fmtAddTextInLine,
                "INVOICE : ${invoiceWithPadding(printerReceiptData.invoiceNumber)}",
                "",
                "",
                PrinterConfig.addTextInLine.mode.Devide_flexible
            )

            printTransType(Bundle(), transactionType)

            fmtAddTextInLine.putInt(
                PrinterConfig.addTextInLine.FontSize.BundleName,
                PrinterConfig.addTextInLine.FontSize.NORMAL_24_24
            )
            fmtAddTextInLine.putString(
                PrinterConfig.addTextInLine.GlobalFont.BundleName,
                PrinterFonts.path + PrinterFonts.FONT_AGENCYR
            )

            format.putInt(
                PrinterConfig.addText.FontSize.BundleName,
                PrinterConfig.addText.FontSize.NORMAL_DH_24_48_IN_BOLD
            )
            format.putInt(
                PrinterConfig.addText.Alignment.BundleName,
                PrinterConfig.addText.Alignment.CENTER
            )
            //printer?.addText(format, printerReceiptData.getTransactionType())

            fmtAddTextInLine.putInt(
                PrinterConfig.addTextInLine.FontSize.BundleName,
                PrinterConfig.addTextInLine.FontSize.NORMAL_24_24
            )
            fmtAddTextInLine.putString(
                PrinterConfig.addTextInLine.GlobalFont.BundleName,
                PrinterFonts.path + PrinterFonts.FONT_AGENCYR
            )

            printer?.addTextInLine(
                fmtAddTextInLine,
                "CARD TYPE : ${printerReceiptData.cardType}",
                "",
                "EXP : XX/XX",
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
                "CARD NO : ${
                    getMaskedPan(
                        TerminalParameterTable.selectFromSchemeTable(),
                        printerReceiptData.cardNumber
                    )
                }",
                "",
                printerReceiptData.operationType,
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
            if (printerReceiptData.authCode == "null") {
                printer?.addTextInLine(
                    fmtAddTextInLine,
                    "RRN : ${printerReceiptData.referenceNumber}",
                    "",
                    "",
                    PrinterConfig.addTextInLine.mode.Devide_flexible
                )
            } else {
                printer?.addTextInLine(
                    fmtAddTextInLine,
                    "AUTH CODE : ${printerReceiptData.authCode.trim()}",
                    "",
                    "RRN : ${printerReceiptData.referenceNumber}",
                    PrinterConfig.addTextInLine.mode.Devide_flexible
                )
            }

            /*   printer?.addTextInLine(
                   fmtAddTextInLine,
                   "AID : ${printerReceiptData.aid}",
                   "",
                   "",
                   PrinterConfig.addTextInLine.mode.Devide_flexible
               )*/

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
                    printer?.addTextInLine(
                        fmtAddTextInLine,
                        "TVR : ${printerReceiptData.tvr}",
                        "",
                        "TSI : ${printerReceiptData.tsi}",
                        PrinterConfig.addTextInLine.mode.Devide_flexible
                    )
                }

                //   printer.addTextInLine( fmtAddTextInLine, "L & R", "", "Divide Equally", 0);
                //   printer.addTextInLine( fmtAddTextInLine, "L & R", "", "Divide Equally", 0);
                if (!printerReceiptData.aid.isBlank() && !printerReceiptData.tc.isBlank()) {
                    fmtAddTextInLine.putInt(
                        PrinterConfig.addTextInLine.FontSize.BundleName,
                        PrinterConfig.addTextInLine.FontSize.NORMAL_24_24
                    )
                    fmtAddTextInLine.putString(
                        PrinterConfig.addTextInLine.GlobalFont.BundleName,
                        PrinterFonts.path + PrinterFonts.FONT_AGENCYR
                    )

                    printer?.addTextInLine(
                        fmtAddTextInLine,
                        "AID : ${printerReceiptData.aid}",
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
                    printer?.addTextInLine(
                        fmtAddTextInLine,
                        "TC : ${printerReceiptData.tc}",
                        "",
                        "",
                        PrinterConfig.addTextInLine.mode.Devide_flexible
                    )
                }

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
            val baseAmount = "%.2f".format(printerReceiptData.transactionalAmmount.toDouble() / 100)
            printer?.addTextInLine(
                fmtAddTextInLine,
                "BASE AMOUNT  :    Rs  $baseAmount",
                "",
                "",
                PrinterConfig.addTextInLine.mode.Devide_flexible
            )


            //  val totalAmount = "%.2f".format(printerReceiptData.totalAmmount.toFloat() / 100)
            var tipAndTransAmount = 0.0

            if (printerReceiptData.transactionType == TransactionType.TIP_SALE.type) {
                tipAndTransAmount = (printerReceiptData.totalAmmount.toDouble()) / 100

            } else {
                tipAndTransAmount = baseAmount.toDouble()
            }


            printer?.addTextInLine(
                fmtAddTextInLine,
                "TOTAL AMOUNT :       Rs  $baseAmount",
                "",
                "",
                PrinterConfig.addTextInLine.mode.Devide_flexible
            )
            //    centerText(format, "TOTAL AMOUNT :    Rs  $baseAmount")
            printSeperator(format)

            format.putInt(
                PrinterConfig.addText.FontSize.BundleName,
                PrinterConfig.addText.FontSize.NORMAL_24_24
            )
            format.putInt(
                PrinterConfig.addText.Alignment.BundleName,
                PrinterConfig.addText.Alignment.CENTER
            )
            if (printerReceiptData.isPinverified) {
                //  printer?.addText(format, pinVerifyMsg)
                centerText(format, pinVerifyMsg)
                centerText(format, signatureMsg)
            } else {
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
            //   printer?.addText(format, ipt?.volletIssuerDisclammer)
            printer?.addText(format, copyType.pName)
            printer?.addText(format, footerText[0])
            printer?.addText(format, footerText[1])




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

            printer?.addText(format, "---------X-----------X----------")
            printer?.feedLine(4)

            // start print here and callback of printer:-
            printer?.startPrint(object : PrinterListener.Stub() {
                override fun onFinish() {
                    when (copyType) {
                        EPrintCopyType.MERCHANT -> {
                            GlobalScope.launch(Dispatchers.Main) {
                                (context as BaseActivity).showMerchantAlertBox2(
                                    PrintUtil(context),
                                    BatchFileDataTable()
                                ) { dialogCB ->
                                    if (dialogCB) {
                                        Log.e("VOID OFF REFUND RECEIPT", "SUCCESS.....")
                                        printerCallback(true, true)
                                    } else {
                                        printerCallback(false, false)
                                    }
                                }
                            }
                        }
                        EPrintCopyType.CUSTOMER -> {
                            printerCallback(true, true)
                        }
                        EPrintCopyType.DUPLICATE -> {
                            printerCallback(true, true)
                        }
                    }

                }

                override fun onError(error: Int) {
                    Log.e("VOID OFF REFUND RECEIPT", "FAIL.....")
                    printerCallback(false, false)
                }


            })


        } catch (e: RemoteException) {
            e.printStackTrace()
        } catch (ex: Exception) {
            ex.printStackTrace()
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
        }
        val fmtImage = Bundle()
        fmtImage.putInt("offset", 0)
        fmtImage.putInt("width", 384) // bigger then actual, will print the actual
        fmtImage.putInt(
            "height",
            255
        ) // bigger then actual, will print the actual//128 default
        printer?.addImage(fmtImage, buffer)
    }

    private fun printSeperator(format: Bundle) {
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

    }

    private fun centerText(format: Bundle, text: String) {

        format.putInt(
            PrinterConfig.addText.FontSize.BundleName,
            PrinterConfig.addText.FontSize.NORMAL_24_24
        )
        format.putInt(
            PrinterConfig.addText.Alignment.BundleName,
            PrinterConfig.addText.Alignment.CENTER
        )
        printer?.addText(format, text)
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
}