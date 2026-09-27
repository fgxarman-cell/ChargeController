package com.armancraft.chargecontroller;

import android.inputmethodservice.InputMethodService;
import android.view.KeyEvent;
import android.view.View;
import android.widget.TextView;

public class GameInputIME extends InputMethodService {

    private static GameInputIME currentInstance;

    public static void sendKey(int keyCode) {
        if (currentInstance != null) {
            currentInstance.sendKeyInternal(keyCode);
        }
    }

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

    @Override
    public View onCreateInputView() {
        TextView view = new TextView(this);

        view.setText("Charge Controller");
        view.setTextSize(1);
        view.setVisibility(View.INVISIBLE);

        return view;
    }

    private void sendKeyInternal(int keyCode) {

        if (getCurrentInputConnection() == null) {
            return;
        }

        sendDownUpKeyEvents(keyCode);
    }
}
