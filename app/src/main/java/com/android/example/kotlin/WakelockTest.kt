package com.android.example.kotlin

import android.os.PowerManager

class Wakelock {
    fun acquire() {}
    fun release() {}
}

open class WakelockTest(val wlFieldParam: PowerManager.WakeLock) {

    val wlField = Wakelock()

    fun wlParamFun(wlParam: PowerManager.WakeLock) {
        wlParam.acquire()
    }

    fun wlParamFunRelease(wlParam: PowerManager.WakeLock) {
        wlParam.acquire(10*60*1000L /*10 minutes*/)
        wlParam.release()
    }

    fun wlLocalFun() {
        val wlLocal = Wakelock()
        wlLocal.acquire()
    }

    fun wlLocalReleaseFun() {
        val wlLocal = Wakelock()
        wlLocal.acquire()
        wlLocal.release()
    }

    fun wlFieldFun() {
        wlField.acquire()
    }

    fun wlFieldReleaseFun() {
        wlField.acquire()
        wlField.release()
    }

    fun wlFieldParamFun() {
        wlFieldParam.acquire()
    }

    fun wlFieldParamReleaseFun() {
        wlFieldParam.acquire()
        wlFieldParam.release()
    }
}
