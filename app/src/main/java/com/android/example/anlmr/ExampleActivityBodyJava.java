package com.android.example.anlmr;

import android.app.Activity;
import android.os.Bundle;
import android.support.annotation.Nullable;

import java.io.Serializable;

public class ExampleActivityBodyJava extends Activity implements Serializable {
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }
}
