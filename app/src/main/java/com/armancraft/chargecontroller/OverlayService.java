private void sendKeyAttempt(String key) {

    int keyCode;

    switch (key) {
        case "W":
            keyCode = android.view.KeyEvent.KEYCODE_W;
            break;

        case "A":
            keyCode = android.view.KeyEvent.KEYCODE_A;
            break;

        case "S":
            keyCode = android.view.KeyEvent.KEYCODE_S;
            break;

        case "D":
            keyCode = android.view.KeyEvent.KEYCODE_D;
            break;

        default:
            return;
    }

    boolean sent = GameInputIME.sendKey(keyCode);

    if (!sent) {
        android.widget.Toast.makeText(
                this,
                "No active keyboard connection",
                android.widget.Toast.LENGTH_SHORT
        ).show();
    }
}
