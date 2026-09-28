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

    public static boolean keyDown(int keyCode) {

        if (instance == null) {
            return false;
        }

        InputConnection connection =
                instance.getCurrentInputConnection();

        if (connection == null) {
            return false;
        }

        long now = System.currentTimeMillis();

        KeyEvent event = new KeyEvent(
                now,
                now,
                KeyEvent.ACTION_DOWN,
                keyCode,
                0
        );

        return connection.sendKeyEvent(event);
    }

    public static boolean keyUp(int keyCode) {

        if (instance == null) {
            return false;
        }

        InputConnection connection =
                instance.getCurrentInputConnection();

        if (connection == null) {
            return false;
        }

        long now = System.currentTimeMillis();

        KeyEvent event = new KeyEvent(
                now,
                now,
                KeyEvent.ACTION_UP,
                keyCode,
                0
        );

        return connection.sendKeyEvent(event);
    }

    public static boolean sendKey(int keyCode) {
        boolean down = keyDown(keyCode);
        boolean up = keyUp(keyCode);
        return down || up;
    }
}
