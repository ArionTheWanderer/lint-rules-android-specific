package com.android.example.dw;

import android.os.PowerManager;

public class WakelockTestJava {
    private PowerManager.WakeLock wlField1;
    private Wakelock wlField2;

    public void wlParamFun(PowerManager.WakeLock wlParam) {
        wlParam.acquire();
    }

    public void wlParamFunRelease(PowerManager.WakeLock wlParam) {
        wlParam.acquire();
        wlParam.release();
    }

    public void wlLocalFun() {
        Wakelock wlLocal = new Wakelock();
        wlLocal.acquire();
    }


    public void wlLocalReleaseFun() {
        Wakelock wlLocal = new Wakelock();
        wlLocal.acquire();
        wlLocal.release();
    }

    public void wlFieldFun() {
        wlField1.acquire();
    }

    public void wlFieldReleaseFun() {
        wlField1.acquire();
        wlField1.release();
    }

    public void wlFieldParamFun() {
        wlField2.acquire();
    }

    public void wlFieldParamReleaseFun() {
        wlField2.acquire();
        wlField2.release();
    }

    static class Wakelock {
        public void acquire() {}
        public void release() {}
    }
}
