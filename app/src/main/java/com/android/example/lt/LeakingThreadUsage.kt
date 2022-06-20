package com.android.example.lt

import android.app.Activity
import android.os.Bundle

class LeakingThreadUsage : Activity() {
    private lateinit var threadField: Thread
    private lateinit var threadField2: Thread
    private lateinit var threadField3: Thread

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        threadField = Thread()
        threadField3 = Thread()
        threadField.start()
        threadField2.start()
        threadField3.start()
    }

    override fun onDestroy() {
        super.onDestroy()
        threadField3.interrupt()
    }

    override fun onLowMemory() {
        super.onLowMemory()
        threadField3.interrupt()
        // TODO clean caches or unnecessary resources
    }

}
