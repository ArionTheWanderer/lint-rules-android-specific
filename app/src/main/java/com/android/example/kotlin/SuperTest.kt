package com.android.example.kotlin

import java.io.*;
import java.lang.*;

class MyDataStructure(val first: String = "first", val second: Int = 2)

open class SuperTest(val first: String, var second: Int) {

    val protectedInt = MyDataStructure() to MyDataStructure("custom", 0)

    fun expressionBodyFunRef() = secondFun()

    fun expressionBodyString() = ""

    open fun secondFun(): Int {
        second += 1
        return second
    }

    open fun firstFun() {}


    fun expressionBodyInt() = ""

    fun blockBodyEmpty() {}
    
}