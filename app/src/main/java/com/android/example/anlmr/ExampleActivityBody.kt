package com.android.example.anlmr

import android.app.Activity
import android.os.Bundle
import java.io.Serializable

class ExampleActivityBody : Activity(), Serializable {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun onDestroy() {
        super.onDestroy()
    }
}
