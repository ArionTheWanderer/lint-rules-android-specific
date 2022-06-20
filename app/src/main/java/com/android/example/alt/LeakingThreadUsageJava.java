package com.android.example.alt;

import android.app.Activity;
import android.os.Bundle;
import android.support.annotation.Nullable;

public class LeakingThreadUsageJava extends Activity {
    private Thread threadField;
    private Thread threadField2;
    private Thread threadField3;
    private Thread threadField4 = new Thread();


    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        threadField = new Thread();
        threadField3 = new Thread();
        threadField.start();
        threadField2.start();
        threadField3.start();
        threadField4.start();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        threadField3.interrupt();
    }

    @Override
    public void onLowMemory() {
        super.onLowMemory();
        // TODO clean caches or unnecessary resources
    }
}
