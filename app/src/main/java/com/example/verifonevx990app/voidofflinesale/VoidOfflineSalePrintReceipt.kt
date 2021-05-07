package com.example.verifonevx990app.voidofflinesale

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
import java.text.SimpleDateFormat
import java.util.*

class VoidOfflineSalePrintReceipt {
    var printer: IPrinter? = null

    init {
        printer = VFService.vfPrinter
        initializeFontFiles()
    }

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

    private fun setHeaderWithLogo(
        format: Bundle, img: String,
        headers: ArrayList<String>, context: Context? = null
    ) {
        printLogo(img)
        centerText(format, headers[0])
        centerText(format, headers[1])
        centerText(format, headers[2])
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

    //Below method is used to print Offline Sale Receipt:-
    fun voidOfflineSalePrint(
        context: Context?,
        batch: BatchFileDataTable,
        transactionType: Int,
        copyType: EPrintCopyType,
        printerCB: (Boolean, Boolean) -> Unit
    ) {
        val format = Bundle()
        val fmtAddTextInLine = Bundle()
        val tpt = TerminalParameterTable.selectFromSchemeTable()
        val headers = arrayListOf<String>()
        tpt?.receiptHeaderOne?.let { headers.add(it) }
        tpt?.receiptHeaderTwo?.let { headers.add(it) }
        tpt?.receiptHeaderThree?.let { headers.add(it) }

        setHeaderWithLogo(format, "hdfc_print_logo.bmp", headers, context)
        val formattime = SimpleDateFormat("HH:mm:ss", Locale.ENGLISH)
        val date: Long = Calendar.getInstance().timeInMillis
        val timeFormater = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
        val timeStampFormattted = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())


        //Upper Body Print Code:-
        alignLeftRightText(
            fmtAddTextInLine,
            "DATE : ${timeFormater.format(date)}",
            "TIME : ${timeStampFormattted.format(date)}"
        )
        alignLeftRightText(fmtAddTextInLine, "MID : ${batch.mid}", "TID : ${batch.tid}")
        alignLeftRightText(
            fmtAddTextInLine,
            "BATCH NO  : ${batch.batchNumber}",
            "ROC : ${
                invoiceWithPadding(ROCProviderV2.getRoc(AppPreference.getBankCode()).toString())
            }"
        )

        alignLeftRightText(
            fmtAddTextInLine,
            "INVOICE  : ${invoiceWithPadding(batch.invoiceNumber)}",
            ""
        )

        //centerText(format, "VOID OFF SALE")
        printTransType(Bundle(), transactionType)

        //Middle Body Print Code:-
        alignLeftRightText(
            fmtAddTextInLine,
            "CARD TYPE : ${batch.cardType}",
            "EXP : XX/XX"
        )

        alignLeftRightText(
            fmtAddTextInLine,
            "CARD NO : ${
                getMaskedPan(
                    TerminalParameterTable.selectFromSchemeTable(),
                    batch.cardNumber
                )
            }",
            ":Man"
        )

        alignLeftRightText(
            fmtAddTextInLine,
            "AUTH CODE : ${batch.authCode.trim()}",
            ""
        )

        printer?.addText(format, "--------------------------------")

        //Lower Body Print Code:-
        alignLeftRightText(
            fmtAddTextInLine,
            "OFF SALE AMOUNT :    Rs ${batch.totalAmmount}",
            ""
        )

        alignLeftRightText(
            fmtAddTextInLine,
            "TOTAL AMOUNT :    Rs ${batch.totalAmmount}",
            ""
        )

        printer?.addText(format, "--------------------------------")


        format.putInt(
            PrinterConfig.addText.FontSize.BundleName,
            PrinterConfig.addText.FontSize.NORMAL_24_24
        )
        format.putInt(
            PrinterConfig.addText.Alignment.BundleName,
            PrinterConfig.addText.Alignment.CENTER
        )
        val signatureMsg = "SIGN ..............................................."
        // printer?.addText(format, signatureMsg)
        // -------(Remove in New VFservice 3.0)  printer?.feedLine(2)
        alignLeftRightText(format, signatureMsg, "", "")
        // -------(Remove in New VFservice 3.0)  printer?.feedLine(2)
        //Agreement Body Code:-
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
        centerText(fmtAddTextInLine, copyType.pName)
        printer?.addText(fmtAddTextInLine, footerText[0])
        printer?.addText(fmtAddTextInLine, footerText[1])

        printLogo("BH.bmp")


        printer?.addText(fmtAddTextInLine, "App Version : ${BuildConfig.VERSION_NAME}")

        printer?.addText(fmtAddTextInLine, "---------X-----------X----------")
        printer?.feedLine(4)

        // start print here and callback of printer:-
        printer?.startPrint(object : PrinterListener.Stub() {
            override fun onFinish() {
                Log.e("VOID OFF SALE RECEIPT", "SUCCESS.....")
                when (copyType) {
                    EPrintCopyType.MERCHANT -> {
                        GlobalScope.launch(Dispatchers.Main) {
                            (context as BaseActivity).showMerchantAlertBox2(
                                PrintUtil(context),
                                BatchFileDataTable()
                            ) { dialogCB ->
                                if (dialogCB) {
                                    Log.e("VOID OFF Sale RECEIPT", "SUCCESS.....")
                                    printerCB(true, true)
                                } else {
                                    printerCB(false, false)
                                }
                            }
                        }
                    }
                    EPrintCopyType.CUSTOMER -> {
                        printerCB(true, true)
                    }
                }

                //  printerCB(true)
            }

            override fun onError(error: Int) {
                Log.e("VOID OFF SALE RECEIPT", "FAIL.....")
                printerCB(false, false)
            }


        })

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
            if (e.ordinal == transactionType) {
                tTyp = e.txnTitle
                break
            }
        }
        return tTyp
    }
}