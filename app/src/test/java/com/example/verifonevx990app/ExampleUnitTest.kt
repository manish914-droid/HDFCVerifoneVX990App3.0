package com.example.verifonevx990app

import org.junit.Test

/**
 * Example local unit test, which will execute on the development machine (host).
 *
 * See [testing documentation](http://d.android.com/tools/testing).
 **/
class ExampleUnitTest {
    val cardNo = "374245001751016"

    fun cardLuhnCheck() {
        val nDigits = cardNo.length
        var nSum = 0
        var isSecond = false
        for (i in nDigits - 1 downTo 0) {
            var d = cardNo[i] - '0'
            if (isSecond) d *= 2
            // We add two digits to handle
            // cases that make two digits
            // after doubling
            nSum += d / 10
            nSum += d % 10
            isSecond = !isSecond
        }
        nSum % 10 == 0
    }

    @Test
    fun main() {
        val st1 = "aa|bb||||cc"
        val ll = st1.split("|")
        ll.forEach {
            if (it == "")
                println("fuck")
            else
                println(it)
        }
    }
}