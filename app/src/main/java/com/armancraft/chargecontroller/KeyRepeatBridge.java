package com.armancraft.chargecontroller;

import android.os.Handler;
import android.os.Looper;

public class KeyRepeatBridge {

    private static final Handler handler =
            new Handler(Looper.getMainLooper());

    private static final long REPEAT_DELAY = 50;

    private static final boolean[] holding =
            new boolean[256];

    public static void start(int keyCode) {

        if (keyCode < 0 || keyCode >= holding.length) {
            return;
        }

        if (holding[keyCode]) {
            return;
        }

        holding[keyCode] = true;

        repeatKey(keyCode);
    }

    public static void stop(int keyCode) {

        if (keyCode < 0 || keyCode >= holding.length) {
            return;
        }

        holding[keyCode] = false;
    }

    private static void repeatKey(int keyCode) {

        if (keyCode < 0 || keyCode >= holding.length) {
            return;
        }

        if (!holding[keyCode]) {
            return;
        }

        GameInputIME.sendKey(keyCode);

        handler.postDelayed(
                () -> repeatKey(keyCode),
                REPEAT_DELAY
        );
    }
}
