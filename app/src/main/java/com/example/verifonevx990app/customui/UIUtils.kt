package com.example.verifonevx990app.customui


import android.app.Dialog
import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.view.LayoutInflater
import android.widget.TextView
import com.example.verifonevx990app.R

object ProgressDialog {

    fun progressDialog(context: Context?, message: String?): Dialog? {
        val dialog = context?.let { Dialog(it) }
        val inflate = LayoutInflater.from(context).inflate(R.layout.progress_dialog, null)
        dialog?.setContentView(inflate)
        val messageTV: TextView? = dialog?.findViewById(R.id.messageTV)
        messageTV?.text = message
        dialog?.setCancelable(false)
        dialog?.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        return dialog
    }
}