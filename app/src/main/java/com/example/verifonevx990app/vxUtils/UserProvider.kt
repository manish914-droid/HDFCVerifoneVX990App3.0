package com.example.verifonevx990app.vxUtils

import com.example.verifonevx990app.realmtables.TerminalParameterTable

object UserProvider {

    var tid = ""

    var mid = ""

    var name = ""


    fun refresh() {
        try {
            val tpt = TerminalParameterTable.selectFromSchemeTable()
            if (tpt != null) {
                tid = tpt.terminalId.toLong().toString()
                mid = tpt.merchantId.toLong().toString()
                name = tpt.receiptHeaderTwo
            }
        } catch (ex: Exception) {
        }
    }

}