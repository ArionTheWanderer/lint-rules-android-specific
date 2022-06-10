package com.android.example.aids

class IdsTest {
    val hashmap = mapOf<Int, Any>()

    fun ids(param: MutableMap<Int, IdsClass>): Map<Int, Any> {
        val localVar = hashMapOf<Int, IdsClass>()
        return hashmap
    }

    data class IdsClass(val string: String = "")

//    fun ids2(param: MutableMap<Int, Any>): Map<Int, Any> {
//        val localVar = hashMapOf<Int, Object>()
//        return param
//    }
}
