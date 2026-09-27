package com.armancraft.chargecontroller;

import android.inputmethodservice.InputMethodService;
import android.view.KeyEvent;

public class GameInputIME extends InputMethodService {

    public static void sendKey(int keyCode) {
        GameInputIME ime = currentInstance;

        if (ime == null) {
            return;
        }

        ime.sendKeyInternal(keyCode);
    }

    private static GameInputIME currentInstance;

    @Override
    public void onCreate() {
        super.onCreate();
        currentInstance = this;
    }

    @Override
    public void onDestroy() {
        if (currentInstance == this) {
            currentInstance = null;
        }
        super.onDestroy();
    }

    private void sendKeyInternal(int keyCode) {
        if (getCurrentInputConnection() != null) {
            sendDownUpKeyEvents(keyCode);
        }
    }
}
