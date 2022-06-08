package com.android.example.kotlin

import android.os.PowerManager
import java.lang.*;

class MyDataStructure(val first: String = "first", val second: Int = 2) {
    fun acquire() {}
    fun release() {}
}

open class SuperTest(val first: String, var second: Int, val wlFieldParam: PowerManager.WakeLock) {

    val protectedInt = MyDataStructure() to MyDataStructure("custom", 0)

    open fun firstFun() {
        println("qwerty")
//        val qwe = MyDataStructure().first.length

    }

    fun expressionBodyFunRef() = secondFun()

    fun expressionBodyString() = ""

    open fun secondFun(): Int {
        second += 1
        return second
    }

    fun expressionBodyInt() = first
}