package com.android.example.anlmr

import android.app.Activity
import android.os.Bundle
import java.io.Serializable

class ExampleActivityOlm : Activity(), Serializable {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun onDestroy() {
        super.onDestroy()
    }

    override fun onLowMemory() {
        super.onLowMemory()
    }
}
