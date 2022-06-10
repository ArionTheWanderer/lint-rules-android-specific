package com.android.example.block

import android.os.PowerManager

class BlockTest(val wlFieldParam: PowerManager.WakeLock) {

    init {
        wlFieldParam.acquire()
        wlFieldParam.release()
    }

    fun funWithBody() {
        wlFieldParam.acquire()
    }

    fun funWithNoBody() = wlFieldParam.acquire()

}