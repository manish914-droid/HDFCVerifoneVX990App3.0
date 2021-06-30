package com.example.verifonevx990app.utils


import android.view.View
import android.widget.EditText
import android.widget.TextView
import com.example.verifonevx990app.realmtables.TerminalParameterTable
import com.example.verifonevx990app.vxUtils.VFService

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext


class KeyboardModel {

    var view: View? = null
    var callback: ((String) -> Unit)? = null
    var isInutSimpleDigit = false

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
                                    setEt(et, "")//et.setText("0.00")
                                }
                                "o" -> {  // o stands for ok
                                    sendCallback(et.text.toString())
                                    //  setEt(et, "0.00")//et.setText("0.00")
                                }
                                "d" -> {  // d stands for delete
                                    var s: String = et.text.toString()
                                    s = try {
                                        var sst = ""
                                        sst = if (isInutSimpleDigit) {
                                            s.subSequence(0, s.lastIndex) as String
                                        } else {
                                            getFormattedAmount(
                                                s.subSequence(
                                                    0,
                                                    s.lastIndex
                                                ) as String
                                            )
                                        }
                                        if (sst == "0.00") {
                                            ""
                                        } else {
                                            sst
                                        }
                                    } catch (ex: Exception) {
                                        ""
                                    }
                                    setEt(et, s)//et.setText(s)
                                }
                                else -> {  // concatenate the num
                                    var s = "${et.text}$str"
                                    s = if (isInutSimpleDigit) {
                                        "${et.text}$str"
                                    } else {
                                        getFormattedAmount(s)
                                    }
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
    /*val tpt=TerminalParameterTable.selectFromSchemeTable()?.maxAmtEntryDigits
    val s = str.replace(".", "")
    var f = s.toDouble()
    f = if (f <= 99999999) f / 100 else (f / 10).toInt().toDouble() / 100
    "%.2f".format(f)*/

    val fl = str.replace(".", "").toLong()
     "%.2f".format(fl.toDouble() / 100)
} catch (ex: Exception) {
    "0.00"
}
/*
"%.0f".format(f)
printf("dexp: %f\n", f);
val fl = text.toString().replace(".", "").toLong()
                val tx = "%.2f".format(fl.toDouble() / 100)

                max amt ipt

                */
