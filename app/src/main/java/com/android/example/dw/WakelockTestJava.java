package com.android.example.dw;

import android.content.Context;
import android.os.PowerManager;

public class WakelockTestJava {
    private Context context;
    private PowerManager.WakeLock wlField1;
    private PowerManager.WakeLock wlField2;

    public WakelockTestJava(PowerManager.WakeLock wlField1, PowerManager.WakeLock wlField2, Context context) {
        this.wlField1 = wlField1;
        this.wlField2 = wlField2;
        this.context = context;
    }

    public void wlParamFun(PowerManager.WakeLock wlParam) {
        wlParam.acquire();
    }

    public void wlParamFunRelease(PowerManager.WakeLock wlParam) {
        wlParam.acquire();
        wlParam.release();
    }

    public void wlLocalFun() {
//        Wakelock wlLocal = new Wakelock();
//        wlLocal.acquire(); //?

        PowerManager pm = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
        PowerManager.WakeLock wlLocal = pm.newWakeLock(
                PowerManager.SCREEN_DIM_WAKE_LOCK | PowerManager.ON_AFTER_RELEASE, "myapp:mywltag"
        );
        wlLocal.acquire();
    }


    public void wlLocalReleaseFun() {
//        Wakelock wlLocal = new Wakelock();
//        wlLocal.acquire();
//        wlLocal.release();

        PowerManager pm = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
        PowerManager.WakeLock wlLocal = pm.newWakeLock(
                PowerManager.SCREEN_DIM_WAKE_LOCK | PowerManager.ON_AFTER_RELEASE, "myapp:mywltag"
        );
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
        wlField2.acquire(); //?
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
