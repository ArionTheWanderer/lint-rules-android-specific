package com.android.example

import android.content.Context
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.os.PowerManager
import com.android.example.R

class MainActivity : AppCompatActivity() {

    lateinit var wl1: PowerManager.WakeLock
    lateinit var wl2: PowerManager.WakeLock

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        val pm = applicationContext.getSystemService(Context.POWER_SERVICE) as PowerManager
        val wl: PowerManager.WakeLock = pm.newWakeLock(
            PowerManager.SCREEN_DIM_WAKE_LOCK or PowerManager.ON_AFTER_RELEASE, "myapp:mywltag"
        )
        wl1 = pm.newWakeLock(
            PowerManager.SCREEN_DIM_WAKE_LOCK or PowerManager.ON_AFTER_RELEASE, "myapp:mywltag"
        )
        wl2 = pm.newWakeLock(
            PowerManager.SCREEN_DIM_WAKE_LOCK or PowerManager.ON_AFTER_RELEASE, "myapp:mywltag"
        )
        wl.acquire()
        println()
        wl.release()
    }

    override fun onResume() {
        super.onResume()
        wl1.acquire()
    }

    override fun onDestroy() {
        super.onDestroy()
        wl1.release()
        wl2.release()
    }
}