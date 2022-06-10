package com.android.example.block;

import android.os.PowerManager;

public class BlockTestJava {

    PowerManager.WakeLock wlFieldParam;

    BlockTestJava(PowerManager.WakeLock wakeLock) {
        wlFieldParam = wakeLock;
    }

    public void funWithBody() {
        wlFieldParam.acquire();
        wlFieldParam.release();
    }
}
