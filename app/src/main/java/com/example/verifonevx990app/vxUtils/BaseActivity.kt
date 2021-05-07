package com.example.verifonevx990app.vxUtils


import android.app.Activity
import android.app.Dialog
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.TextUtils
import android.view.Gravity
import android.view.View
import android.view.Window
import android.view.WindowManager
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.example.verifonevx990app.R
import com.example.verifonevx990app.main.MainActivity
import com.example.verifonevx990app.offlinemanualsale.OfflineSalePrintReceipt
import com.example.verifonevx990app.realmtables.BatchFileDataTable
import com.example.verifonevx990app.realmtables.EDashboardItem
import com.example.verifonevx990app.transactions.TenureDataModel
import com.example.verifonevx990app.utils.printerUtils.EPrintCopyType
import com.example.verifonevx990app.utils.printerUtils.PrintUtil


abstract class BaseActivity : AppCompatActivity(), IDialog {
    private lateinit var progressDialog: Dialog
    lateinit var progressTitleMsg: TextView


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        //Thread.setDefaultUncaughtExceptionHandler(UnCaughtException(this@BaseActivity))
        setProgressDialog()
    }

    override fun showToast(msg: String) {
        Toast(VerifoneApp.appContext).apply {
            setGravity(Gravity.NO_GRAVITY, 0, 0)
            duration = Toast.LENGTH_LONG
            val vi = (layoutInflater.inflate(R.layout.custom_toast, null) as TextView)
            vi.text = msg
            view = vi

        }.show()
    }

    private fun setProgressDialog() {
        progressDialog = Dialog(this).apply {
            requestWindowFeature(Window.FEATURE_NO_TITLE)
            setContentView(R.layout.item_progress_dialog)
            setCancelable(false)
        }
        progressTitleMsg = progressDialog.findViewById(R.id.msg_et)
    }

    override fun showProgress(progressMsg: String) {
        if (null != progressDialog && !progressDialog.isShowing && !(this as Activity).isFinishing) {
            progressTitleMsg.text = progressMsg
            progressDialog.show()
        }
    }


    override fun hideProgress() {
        if (null != progressDialog && progressDialog.isShowing && !(this as Activity).isFinishing)
            progressDialog.dismiss()
    }

    override fun setProgressTitle(title: String) {
        progressTitleMsg.text = title
    }


    override fun getInfoDialog(title: String, msg: String, acceptCb: () -> Unit) {
        val dialog = Dialog(this)
        dialog.apply {
            requestWindowFeature(Window.FEATURE_NO_TITLE)
            setContentView(R.layout.msg_dialog)
            setCancelable(false)

            findViewById<TextView>(R.id.msg_dialog_title).text = title
            findViewById<TextView>(R.id.msg_dialog_msg).text = msg

            with(findViewById<View>(R.id.msg_dialog_ok)) {
                setOnClickListener {
                    dismiss()
                    acceptCb()
                }
            }


            findViewById<View>(R.id.msg_dialog_cancel).visibility = View.INVISIBLE
        }.show()

    }

    override fun getMsgDialog(
        title: String,
        msg: String,
        positiveTxt: String,
        negativeTxt: String,
        positiveAction: () -> Unit,
        negativeAction: () -> Unit,
        isCancellable: Boolean
    ) {

        val dialog = Dialog(this)
        dialog.apply {
            requestWindowFeature(Window.FEATURE_NO_TITLE)
            setContentView(R.layout.msg_dialog)
            setCancelable(isCancellable)

            findViewById<TextView>(R.id.msg_dialog_title).text = title
            findViewById<TextView>(R.id.msg_dialog_msg).text = msg

            with(findViewById<Button>(R.id.msg_dialog_ok)) {
                text = positiveTxt
                setOnClickListener {
                    dismiss()
                    positiveAction()
                }
            }


            findViewById<Button>(R.id.msg_dialog_cancel).apply {
                text = negativeTxt
                setOnClickListener {
                    dismiss()
                    negativeAction()
                }
            }
        }.show()

    }

    override fun alertBoxWithAction(
        printUtils: PrintUtil?, batchData: BatchFileDataTable?,
        title: String, msg: String, showCancelButton: Boolean,
        positiveButtonText: String, alertCallback: (Boolean) -> Unit,
        cancelButtonCallback: (Boolean) -> Unit
    ) {

        val builder = androidx.appcompat.app.AlertDialog.Builder(this)
        builder.setTitle(title)
        builder.setMessage(msg)
            .setCancelable(false)
            .setPositiveButton(positiveButtonText) { dialog, _ ->
                dialog.dismiss()
                alertCallback(true)
            }


        //Below condition check is to show Cancel Button in Alert Dialog on condition base:-
        if (showCancelButton) {
            builder.setNegativeButton("No") { dialog, _ ->
                dialog.cancel()
                cancelButtonCallback(true)
            }
        }
        val alert: androidx.appcompat.app.AlertDialog = builder.create()
        //Below Handler will execute to auto cancel Alert Dialog Pop-Up when positiveButtonText isEmpty:-
        if (TextUtils.isEmpty(positiveButtonText)) {
            Handler(Looper.getMainLooper()).postDelayed({
                alert.dismiss()
                alert.cancel()
                startActivity(Intent(this, MainActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_CLEAR_TASK
                })
            }, 2000)
        }

        try {
            if (!alert.isShowing) {
                alert.show()
            }
        } catch (ex: WindowManager.BadTokenException) {
            ex.printStackTrace()
        } catch (ex: Exception) {
            ex.printStackTrace()
        }
    }

    open fun transactFragment(fragment: Fragment, isBackStackAdded: Boolean = false): Boolean {
        val trans = supportFragmentManager.beginTransaction().apply {
            replace(R.id.ma_fl, fragment, fragment::class.java.simpleName)
            addToBackStack("")
        }
        if (isBackStackAdded) trans.addToBackStack(null)
        return trans.commitAllowingStateLoss() >= 0
    }

    fun showMerchantAlertBox2(
        printerUtil: PrintUtil,
        batchData: BatchFileDataTable,
        dialogCB: (Boolean) -> Unit
    ) {
        alertBoxWithAction(
            printerUtil, batchData, getString(R.string.print_customer_copy),
            getString(R.string.do_you_want_to_print_customer_copy),
            true, getString(R.string.positive_button_yes), { status ->
                if (status) {
                    dialogCB(true)
                }
            }, {
                dialogCB(false)
            })
    }

    fun showMerchantAlertBox1(
        printerUtil: PrintUtil,
        batchData: BatchFileDataTable,
        dialogCB: (Boolean) -> Unit
    ) {
        alertBoxWithAction(
            printerUtil, batchData, getString(R.string.print_customer_copy),
            getString(R.string.do_you_want_to_print_customer_copy),
            true, getString(R.string.positive_button_yes), { status ->
                if (status) {
                    printerUtil.printAuthCompleteChargeSlip(
                        batchData,
                        EPrintCopyType.CUSTOMER,
                        this
                    ) { customerCopyPrintSuccess ->

                        if (!customerCopyPrintSuccess) {
                            VFService.showToast(getString(R.string.customer_copy_print_success))
                            dialogCB(false)
                        }

                    }
                }
            }, {
                /* startActivity(
                     Intent(this, MainActivity::class.java).apply {
                         flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_CLEAR_TASK
                     })*/
                dialogCB(false)
            })
    }

    fun showMerchantAlertBoxForTipSale(
        printerUtil: PrintUtil,
        batchData: BatchFileDataTable,
        dialogCB: (Boolean) -> Unit
    ) {
        alertBoxWithAction(
            printerUtil, batchData, getString(R.string.print_customer_copy),
            getString(R.string.do_you_want_to_print_customer_copy),
            true, getString(R.string.positive_button_yes), { status ->
                if (status) {
                    printerUtil.startPrinting(
                        batchData,
                        EPrintCopyType.CUSTOMER,
                        this
                    ) { customerCopyPrintSuccess, printingFail ->
                        if (customerCopyPrintSuccess) {
                            VFService.showToast(getString(R.string.customer_copy_print_success))
                        }
                        dialogCB(false)
                    }
                }
            }, {
                dialogCB(false)
            })
    }

    fun showMerchantAlertBoxOfflineSale(
        batchData: BatchFileDataTable,
        dialogCB: (Boolean) -> Unit
    ) {
        alertBoxWithAction(
            null, batchData, getString(R.string.print_customer_copy),
            getString(R.string.do_you_want_to_print_customer_copy),
            true, getString(R.string.positive_button_yes), { status ->
                if (status) {
                    OfflineSalePrintReceipt().offlineSalePrint(
                        batchData, EPrintCopyType.CUSTOMER,
                        this
                    ) { printCB, printingFail ->
                        dialogCB(false)

                    }
                }
            }, {
                /* startActivity(
                     Intent(this, MainActivity::class.java).apply {
                         flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_CLEAR_TASK
                     })*/
                dialogCB(false)
            })
    }


}

interface IDialog {
    /* fun getMsgDialog(
         title: String, msg: String, positiveTxt: String, negativeTxt: String
         , positiveAction: () -> Unit, negativeAction: () -> Unit, isCancellable: Boolean = false
     )

     fun getInfoDialog(title: String, msg: String, acceptCb: () -> Unit)

     fun setProgressTitle(title: String)*/
    fun getMsgDialog(
        title: String,
        msg: String,
        positiveTxt: String,
        negativeTxt: String,
        positiveAction: () -> Unit,
        negativeAction: () -> Unit,
        isCancellable: Boolean = false
    )

    fun setProgressTitle(title: String)
    fun onEvents(event: VxEvent)

    fun showToast(msg: String)
    fun showProgress(progressMsg: String = "Please Wait....")
    fun hideProgress()
    fun getInfoDialog(title: String, msg: String, acceptCb: () -> Unit)
    fun alertBoxWithAction(
        printUtils: PrintUtil? = null, batchData: BatchFileDataTable? = null,
        title: String, msg: String, showCancelButton: Boolean, positiveButtonText: String,
        alertCallback: (Boolean) -> Unit, cancelButtonCallback: (Boolean) -> Unit
    )

}

sealed class VxEvent {
    data class ChangeTitle(val titleName: String) : VxEvent()
    data class ReplaceFragment(val fragment: Fragment) : VxEvent()
    object AutoSettle : VxEvent()
    data class PayEmi(
        val amount: Float,
        val issuerId: String,
        val tenure: TenureDataModel
    ) :
        VxEvent()

    data class Emi(val amt: Double, val type: EDashboardItem) : VxEvent()
    object ForceSettle : VxEvent()
    object Home : VxEvent()
    object InitTerminal : VxEvent()
    object AppUpdate : VxEvent()
    object DownloadTMKForHDFC : VxEvent()
}


enum class EIntentRequest(val code: Int) {
    TRANSACTION(100),
    EMI_ENQUIRY(101),
    GALLERY(2000),
    PRINTINGRECEIPT(102),
    BankEMISchemeOffer(106),
    FlexiPaySchemeOffer(107),
}