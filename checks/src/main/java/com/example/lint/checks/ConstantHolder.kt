package com.example.lint.checks

import com.android.tools.lint.detector.api.Category

object ConstantHolder {
    @JvmField
    val ANDROID_QUALITY_SMELLS = Category.create("AQS", 100)
}
