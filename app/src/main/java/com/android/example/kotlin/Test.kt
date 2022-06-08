package com.android.example.kotlin

import android.os.PowerManager

fun dotFun(superTest: SuperTest): Int {
    return superTest.protectedInt.first.first.length
}

const val staticInt = 4

class Test(val firstTest: String, var secondTest: Int, val wlMine: PowerManager.WakeLock): SuperTest(firstTest, secondTest, wlMine) {
    var third: Double = 1.0

    constructor(firstTest: String, secondTest: Int, third: Double, wlMine: PowerManager.WakeLock) : this(firstTest, secondTest, wlMine) {
        this.third = third
    }

    override fun firstFun(): Unit {
        val qwe = MyDataStructure().first.length
        val qwe2 = MyDataStructure().first.first().inc()
        this.third += 1.0
        super.firstFun()
    }

    override fun secondFun(): Int {
        val obj = Object()
        val superReturned = super.secondFun()
        return superReturned + 2
    }

    fun thirdFun(param: String): String {
        return param + ""
    }

    companion object {
        const val staticCompanionInt = 5
    }
}