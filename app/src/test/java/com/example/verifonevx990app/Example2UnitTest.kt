package com.example.verifonevx990app

import org.junit.Test

class Example2UnitTest {
    @Test
    fun main2() {
        val str = "LUCKY"
        val one: Any = 10

        if (one is Int) {
            print("INT")
            print(5 + one)
        }
        if (one is String) {
            print("INT")
        }

    }

}


