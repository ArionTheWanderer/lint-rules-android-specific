package com.android.example.lt

import android.app.Activity

class LeakingThreadUsage : Activity() {
    private lateinit var threadField: Thread
    private lateinit var threadField2: Thread
    private lateinit var threadField3: Thread
    private val threadField4: Thread = Thread()

    override fun onDestroy() {
        super.onDestroy()
        threadField = Thread()
        threadField3 = Thread()
        threadField.start()
        threadField2.start()
        threadField3.start()
        threadField4.start()
    }

    override fun onLowMemory() {
        super.onLowMemory()
        threadField3.interrupt()
        // TODO clean caches or unnecessary resources
    }

}
