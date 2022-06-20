package com.android.example.ids

class IdsTest {
    val hashmap = mapOf<Int, Any>()

    fun ids(param: MutableMap<Int, IdsClass>): Map<Int, Any> {
        val localVar = hashMapOf<Int, IdsClass>()
        return hashmap
    }

    data class IdsClass(val string: String = "")
}
