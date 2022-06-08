package com.android.example.aigs

class IgsTest(private var first: String, var second: Int) {
    fun voidFun() {
       second += 1
    }

    fun unitFun(): Unit {
        second += 1
        first = "sf"
    }

    fun stringFun(): String = first
}