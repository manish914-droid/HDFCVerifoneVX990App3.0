package com.example.verifonevx990app.transactions

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.verifonevx990app.R
import com.example.verifonevx990app.database.OnXmlDataParsed
import com.example.verifonevx990app.database.XmlFieldModel
import com.example.verifonevx990app.databinding.ActivityTransactionBinding
import com.example.verifonevx990app.emv.VFEmv
import com.example.verifonevx990app.main.MainActivity
import com.example.verifonevx990app.realmtables.*
import com.example.verifonevx990app.utils.IsoPackageWriter
import com.example.verifonevx990app.utils.PackageWriterModel
import com.example.verifonevx990app.utils.printerUtils.EPrintCopyType
import com.example.verifonevx990app.utils.printerUtils.PrintUtil
import com.example.verifonevx990app.vxUtils.AppPreference
import com.example.verifonevx990app.vxUtils.BHTextView
import com.example.verifonevx990app.vxUtils.VFService
import java.util.*

// Commenting (Unused file By luckysingh)
class TransactionActivity : AppCompatActivity(), OnXmlDataParsed {
    private var mXmlModel: HashMap<String, XmlFieldModel>? = null
    private var transactionalAmount: Long = 0L
    private var otherTransAmount: Long = 0L
    private var isoPackageWriter: IsoPackageWriter? = null
    private var packageWriterModel: PackageWriterModel? = null
    private var context: Context? = null
    private val transactionAmountValue by lazy { intent.getStringExtra("amt") ?: "0" }
    private val otherTransAmountValue by lazy { intent.getStringExtra("otherAmount") ?: "0" }
    private var cardDataTable: CardDataTable? = null
    private var binding: ActivityTransactionBinding? = null


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityTransactionBinding.inflate(layoutInflater)
        setContentView(binding?.root)
        //   txnAmount= intent?.getStringExtra("amt")?.toFloat()?:0f//intent.getStringExtra("amt").toFloat()
        initUI()
        isoPackageWriter = IsoPackageWriter(this, this)
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun initUI() {
        //  binding?.paymentGif?.loadUrl("file:///android_asset/card_animation.html")
        //    binding?.paymentGif?.setOnTouchListener { _, event -> event.action == MotionEvent.ACTION_MOVE }
        val amountValue = "${getString(R.string.rupees_symbol)} $transactionAmountValue"
        findViewById<BHTextView>(R.id.base_amt_tv).text = amountValue

        transactionalAmount = transactionAmountValue.replace(".", "").toLong()
        otherTransAmount = otherTransAmountValue.replace(".", "").toLong()

    }

    override fun onXmlSuccess(xmlFieldModels: HashMap<String, XmlFieldModel>?) {
        mXmlModel = xmlFieldModels
        val terminalParameterTable = TerminalParameterTable.selectFromSchemeTable()
        val terminalCommunicationTable = TerminalCommunicationTable.selectFromSchemeTable()
        val issuerParamTable =
            IssuerParameterTable.selectFromIssuerParameterTable(AppPreference.WALLET_ISSUER_ID)


        VFEmv.initializeEMV(
            VFService.vfIEMV, VFService.vfPinPad,
            isoPackageWriter!!, mXmlModel,
            packageWriterModel, this@TransactionActivity,
            terminalParameterTable, terminalCommunicationTable, issuerParamTable, cardDataTable,
            transactionalAmount, 0L, 4
        )

        /*   EMVInitialize.doBalance(
               VFService.vfIEMV,
               terminalParameterTable,
               isoPackageWriter!!,
               transactionalAmount
           )
   */
    }

    override fun onXmlError(message: String?) {
        Toast.makeText(context, "isoWriter Error ----> $message", Toast.LENGTH_SHORT).show()
    }

    //Method to show Merchant Copy Alert Box:-
    fun showMerchantAlertBox(printerUtil: PrintUtil, batchData: BatchFileDataTable) {
        val builder = androidx.appcompat.app.AlertDialog.Builder(this)
        builder.setTitle("Receipt")
        builder.setMessage("Do you want to print customer copy")
            .setCancelable(false)
            .setPositiveButton("Yes") { dialog, _ ->
                dialog.dismiss()
                printerUtil.startPrinting(
                    batchData,
                    EPrintCopyType.CUSTOMER,
                    this
                ) { printCb, printingFail ->
                    //onSuccess or failure code here
                }
            }
            .setNegativeButton("No") { dialog, _ ->
                dialog.cancel()
                finish()
                startActivity(Intent(this, MainActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_CLEAR_TASK
                })
            }
        val alert: androidx.appcompat.app.AlertDialog = builder.create()
        alert.show()
    }
}

