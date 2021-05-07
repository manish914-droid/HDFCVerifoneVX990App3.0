@file:JvmName("PaxLog")

package com.example.verifonevx990app.utils


import android.util.Log
import com.example.verifonevx990app.BuildConfig


fun paxLog(type: String, tag: String, msg: String) {
    if (BuildConfig.DEBUG) {
        when (type) {
            "e" -> Log.e(tag, msg)
            "i" -> Log.i(tag, msg)
            "d" -> Log.d(tag, msg)
            "w" -> Log.w(tag, msg)
            "v" -> Log.v(tag, msg)
            else -> Log.v(tag, msg)

        }
    }
}