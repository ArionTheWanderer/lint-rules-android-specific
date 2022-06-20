package com.android.example.mim

class MyDataStructure(val first: String = "first", val second: Int = 2) {
    fun firstFun() {}
    fun secondFun() {}
}

open class SuperTest(val first: String, var second: Int) {

    val protectedInt = MyDataStructure() to MyDataStructure("custom", 0)


    open fun firstFun() {
        println(first)
    }

    fun expressionBodyFunRef() = secondFun()

    fun expressionBodyString() = ""

    open fun secondFun(): Int {
        second += 1
        return second
    }

    fun expressionBodyInt() = first
}