package com.armancraft.chargecontroller;

import android.inputmethodservice.InputMethodService;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.InputConnection;

public class GameInputIME extends InputMethodService {

    private static GameInputIME instance;

    @Override
    public void onCreate() {
        super.onCreate();
        instance = this;
    }

    @Override
    public void onDestroy() {
        if (instance == this) {
            instance = null;
        }
        super.onDestroy();
    }

    @Override
    public View onCreateInputView() {
        View view = new View(this);
        view.setVisibility(View.INVISIBLE);
        return view;
    }

    public static boolean sendKey(int keyCode) {
        if (instance == null) {
            return false;
        }

        InputConnection connection =
                instance.getCurrentInputConnection();

        if (connection == null) {
            return false;
        }

        long now = System.currentTimeMillis();

        KeyEvent down = new KeyEvent(
                now,
                now,
                KeyEvent.ACTION_DOWN,
                keyCode,
                0,
                0,
                KeyEvent.VIRTUAL_KEYBOARD,
                0,
                KeyEvent.FLAG_SOFT_KEYBOARD
        );

        KeyEvent up = new KeyEvent(
                now,
                now,
                KeyEvent.ACTION_UP,
                keyCode,
                0,
                0,
                KeyEvent.VIRTUAL_KEYBOARD,
                0,
                KeyEvent.FLAG_SOFT_KEYBOARD
        );

        boolean downResult =
                connection.sendKeyEvent(down);

        boolean upResult =
                connection.sendKeyEvent(up);

        return downResult || upResult;
    }
}
