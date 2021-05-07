package com.example.verifonevx990app.utils


import android.view.View
import android.widget.EditText
import android.widget.TextView
import com.example.verifonevx990app.vxUtils.VFService

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext


class KeyboardModel {

    var view: View? = null
    var callback: ((String) -> Unit)? = null

    fun onKeyClicked(str: String) {
        GlobalScope.launch(Dispatchers.IO) {
            try {
                VFService.vfBeeper?.startBeep(100)
            } catch (ex: java.lang.Exception) {
                ex.printStackTrace()
            }
            try {
                if (view != null) {
                    when (view) {
                        is EditText -> {
                            val et = view as EditText

                            when (str) {
                                "c" -> {  // c stands for clr
                                    setEt(et, "0.00")//et.setText("0.00")
                                }
                                "o" -> {  // o stands for ok
                                    sendCallback(et.text.toString())
                                    //  setEt(et, "0.00")//et.setText("0.00")
                                }
                                "d" -> {  // d stands for delete
                                    var s: String = et.text.toString()
                                    s = try {
                                        getFormattedAmount(
                                            s.subSequence(
                                                0,
                                                s.lastIndex
                                            ) as String
                                        )
                                    } catch (ex: Exception) {
                                        "0.00"
                                    }
                                    setEt(et, s)//et.setText(s)
                                }
                                else -> {  // concatenate the num
                                    var s = "${et.text}$str"
                                    s = getFormattedAmount(s)
                                    setEt(et, s)//et.setText(s)
                                }
                            }
                        }
                    }
                }

            } catch (ex: Exception) {
                ex.printStackTrace()
            }
        }

    }

    private fun sendCallback(str: String) {
        if (callback != null) {
            callback?.invoke(str)
        }
    }

    private suspend fun setEt(et: EditText, value: String) =
        withContext(Dispatchers.Main) { et.setText(value) }

    private suspend fun setTv(tv: TextView, value: String) =
        withContext(Dispatchers.Main) { tv.text = value }

}

fun getFormattedAmount(str: String): String = try {
    val s = str.replace(".", "")
    var f = s.toDouble()
    f = if (f <= 99999999) f / 100 else (f / 10).toInt().toDouble() / 100
    "%.2f".format(f)
} catch (ex: Exception) {
    "0.00"
}
