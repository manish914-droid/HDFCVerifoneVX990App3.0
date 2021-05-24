package com.example.verifonevx990app.vxUtils

import android.app.Activity
import android.app.Dialog
import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.text.InputFilter
import android.text.InputFilter.LengthFilter
import android.text.InputType
import android.view.ViewGroup
import android.view.Window
import android.view.WindowManager
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import com.example.verifonevx990app.R
import com.example.verifonevx990app.init.getEditorActionListener
import com.example.verifonevx990app.realmtables.TerminalParameterTable
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.async
import kotlinx.coroutines.launch


private fun getPasswordDialog(
    title: String,
    _hint: String,
    activity: Activity,
    callback: (Boolean) -> Unit,
    isSuperAdmin: Boolean
) {
    GlobalScope.launch {
        try {
            var password = -1

            val task = async {
                val tpt = TerminalParameterTable.selectFromSchemeTable()
                password = try {
                    (if (isSuperAdmin) tpt?.superAdminPassword ?: "" else tpt?.adminPassword
                        ?: "").toInt()
                } catch (ex: Exception) {
                    -1
                }
                // password=  123456
            }

            launch(Dispatchers.Main) {
                Dialog(activity).apply {
                    requestWindowFeature(Window.FEATURE_NO_TITLE)
                    setContentView(R.layout.item_get_invoice_no)
                    setCancelable(false)
                    val window = window
                    window?.setLayout(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        WindowManager.LayoutParams.WRAP_CONTENT
                    )
                    val enterPasswordET = this.findViewById<BHEditText>(R.id.invoice_no_et)
                    if (isSuperAdmin)
                        enterPasswordET.filters = arrayOf<InputFilter>(LengthFilter(6))
                    else
                        enterPasswordET.filters = arrayOf<InputFilter>(LengthFilter(4))

                    enterPasswordET.apply {
                        setOnFocusChangeListener { _, _ -> error = null }
                        hint = _hint
                    }
                    val okBtn = findViewById<Button>(R.id.invoice_ok_btn)

                    enterPasswordET.setOnEditorActionListener(getEditorActionListener { okBtn.performClick() })
                    findViewById<TextView>(R.id.title_tv).text = title
                    findViewById<Button>(R.id.invoice_cancel_btn).setOnClickListener {
                        dismiss()
                        callback(false)
                    }

                    okBtn.setOnClickListener {
                        val p2 = try {
                            enterPasswordET.text.toString().toInt()
                        } catch (ex: Exception) {
                            0
                        }
                        if (p2 == password) {
                            dismiss()
                            callback(true)
                        } else {
                            enterPasswordET.error = activity.getString(R.string.invalid_password)
                        }
                    }
                    task.await()
                    window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
                }.show()
            }
        } catch (ex: NullPointerException) {
            callback(true)
        }
    }

}

fun verifyAdminPasswordDialog(activity: Activity, callback: (Boolean) -> Unit) = getPasswordDialog(
    activity.getString(R.string.admin_password),
    activity.getString(R.string.enter_admin_password), activity, callback, false
)

fun verifySuperAdminPasswordDialog(activity: Activity, callback: (Boolean) -> Unit) =
    getPasswordDialog(
        activity.getString(R.string.super_admin),
        activity.getString(R.string.enter_super_password), activity, callback, true
    )


fun getInputDialog(
    context: Context,
    title: String,
    _text: String,
    isNumeric: Boolean = false,
    callback: (String) -> Unit
) {
    Dialog(context).apply {
        requestWindowFeature(Window.FEATURE_NO_TITLE)
        setContentView(R.layout.item_get_invoice_no)
        setCancelable(false)
        val window = window
        window?.setLayout(
            ViewGroup.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.WRAP_CONTENT
        )
        val invoiceET = findViewById<EditText>(R.id.invoice_no_et)
        val okbtn = findViewById<Button>(R.id.invoice_ok_btn)
        if (_text == TerminalParameterTable.selectFromSchemeTable()?.terminalId.toString()) {
            invoiceET.filters = arrayOf<InputFilter>(LengthFilter(8))
        } else {
            invoiceET.filters = arrayOf<InputFilter>(LengthFilter(14))
        }
        invoiceET.apply {
            setText(_text)
            inputType = if (isNumeric) InputType.TYPE_CLASS_NUMBER else InputType.TYPE_CLASS_TEXT
            setOnEditorActionListener(getEditorActionListener { okbtn.performClick() })
        }

        findViewById<TextView>(R.id.title_tv).text = title
        findViewById<Button>(R.id.invoice_cancel_btn).setOnClickListener {
            dismiss()
        }
        okbtn.setOnClickListener {
            dismiss()
            callback(invoiceET.text.toString())
        }
        window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
    }.show()
}