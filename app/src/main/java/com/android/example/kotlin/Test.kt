package com.android.example.kotlin

class Test(val firstTest: String, var secondTest: Int): SuperTest(firstTest, secondTest) {
    var third: Double = 1.0

    fun dotFun(superTest: SuperTest) {
        superTest.protectedInt.first.first.length
        superTest.protectedInt.second.second.minus(2)
    }

    constructor(firstTest: String, secondTest: Int, third: Double) : this(firstTest, secondTest) {
        this.third = third
    }

    override fun firstFun() {
        this.third += 1.0
        super.firstFun()
    }

    override fun secondFun(): Int {
        val superReturned = super.secondFun()
        return superReturned + 2
    }

    fun thirdFun(param: String): String {
        return param + ""
    }
}
