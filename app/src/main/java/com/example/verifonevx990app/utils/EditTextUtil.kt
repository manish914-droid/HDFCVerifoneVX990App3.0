package com.example.verifonevx990app.utils

import android.text.Editable
import android.text.TextWatcher
import android.view.Gravity
import android.widget.EditText

object EditTextUtil {

     fun editTextCursor(edittext : EditText){
        edittext.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(
                s: CharSequence,
                start: Int,
                count: Int,
                after: Int
            ) {
            }

            override fun onTextChanged(
                s: CharSequence,
                start: Int,
                before: Int,
                count: Int
            ) {
            }

            override fun afterTextChanged(s: Editable) {
                if (s.toString().isNotEmpty()) {
                    edittext.gravity = Gravity.CENTER
                } else {
                    edittext.gravity = Gravity.START
                }
            }
        })
    }
}
