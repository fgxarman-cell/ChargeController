package com.armancraft.chargecontroller;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.PixelFormat;
import android.graphics.drawable.GradientDrawable;
import android.os.Build;
import android.os.IBinder;
import android.provider.Settings;
import android.util.Log;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.PopupMenu;
import android.widget.TextView;

import androidx.core.app.NotificationCompat;

public class OverlayService extends Service {

    private static final String TAG = "OverlayService";
    private static final String CHANNEL_ID = "charge_controller_overlay";
    private static final int NOTIFICATION_ID = 1001;

    private WindowManager windowManager;

    private View overlayView;
    private WindowManager.LayoutParams params;

    private View spaceView;
    private WindowManager.LayoutParams spaceParams;

    private View numberView;
    private WindowManager.LayoutParams numberParams;

    private boolean numberKeyboardVisible = false;

    private float initialX;
    private float initialY;
    private int initialTouchX;
    private int initialTouchY;

    private float spaceInitialX;
    private float spaceInitialY;
    private int spaceInitialTouchX;
    private int spaceInitialTouchY;

    private float numberInitialX;
    private float numberInitialY;
    private int numberInitialTouchX;
    private int numberInitialTouchY;

    @Override
    public void onCreate() {
        super.onCreate();

        windowManager = (WindowManager) getSystemService(WINDOW_SERVICE);

        createNotificationChannel();
        startForeground(NOTIFICATION_ID, buildNotification());

        showOverlay();
        showSpaceButton();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (overlayView == null) {
            showOverlay();
        }

        if (spaceView == null) {
            showSpaceButton();
        }

        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        removeOverlay();
        removeSpaceButton();
        removeNumberKeyboard();
        super.onDestroy();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    "Charge Controller Overlay",
                    NotificationManager.IMPORTANCE_LOW
            );

            channel.setDescription("Charge Controller overlay");

            NotificationManager manager =
                    getSystemService(NotificationManager.class);

            if (manager != null) {
                manager.createNotificationChannel(channel);
            }
        }
    }

    private Notification buildNotification() {
        Intent intent = new Intent(this, MainActivity.class);

        PendingIntent pendingIntent = PendingIntent.getActivity(
                this,
                0,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT
                        | PendingIntent.FLAG_IMMUTABLE
        );

        return new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("Charge Controller")
                .setContentText("Controller overlay active")
                .setSmallIcon(android.R.drawable.ic_menu_compass)
                .setContentIntent(pendingIntent)
                .setOngoing(true)
                .build();
    }

    private int getWindowType() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            return WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY;
        }

        return WindowManager.LayoutParams.TYPE_PHONE;
    }

    private WindowManager.LayoutParams createWindowParams(int gravity) {
        WindowManager.LayoutParams result =
                new WindowManager.LayoutParams(
                        WindowManager.LayoutParams.WRAP_CONTENT,
                        WindowManager.LayoutParams.WRAP_CONTENT,
                        getWindowType(),
                        WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                                | WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN
                                | WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
                        PixelFormat.TRANSLUCENT
                );

        result.gravity = gravity;
        return result;
    }

    private void showOverlay() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M
                && !Settings.canDrawOverlays(this)) {
            stopSelf();
            return;
        }

        if (overlayView != null) {
            return;
        }

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(8, 8, 8, 8);

        GradientDrawable background = new GradientDrawable();
        background.setColor(Color.argb(210, 30, 30, 30));
        background.setCornerRadius(18);
        background.setStroke(2, Color.argb(180, 100, 180, 255));
        root.setBackground(background);

        LinearLayout topRow = new LinearLayout(this);
        topRow.setOrientation(LinearLayout.HORIZONTAL);
        topRow.setGravity(Gravity.CENTER_VERTICAL);

        Button menuButton = new Button(this);
        menuButton.setText("☰");
        menuButton.setTextSize(14);
        menuButton.setTextColor(Color.WHITE);
        menuButton.setMinWidth(0);
        menuButton.setMinHeight(0);
        menuButton.setPadding(12, 4, 12, 4);
        menuButton.setBackgroundColor(Color.TRANSPARENT);
        menuButton.setOnClickListener(v -> showAppMenu(v));
        topRow.addView(menuButton);

        TextView title = new TextView(this);
        title.setText(" WASD ");
        title.setTextColor(Color.LTGRAY);
        title.setTextSize(11);
        title.setPadding(4, 8, 4, 0);
        topRow.addView(title);

        root.addView(topRow);

        Button buttonW = createKeyButton(
                "W",
                android.view.KeyEvent.KEYCODE_W
        );

        LinearLayout wRow = new LinearLayout(this);
        wRow.setGravity(Gravity.CENTER_HORIZONTAL);
        wRow.addView(buttonW);
        root.addView(wRow);

        LinearLayout asdRow = new LinearLayout(this);
        asdRow.setOrientation(LinearLayout.HORIZONTAL);
        asdRow.setGravity(Gravity.CENTER_HORIZONTAL);

        asdRow.addView(createKeyButton(
                "A",
                android.view.KeyEvent.KEYCODE_A
        ));

        asdRow.addView(createKeyButton(
                "S",
                android.view.KeyEvent.KEYCODE_S
        ));

        asdRow.addView(createKeyButton(
                "D",
                android.view.KeyEvent.KEYCODE_D
        ));

        root.addView(asdRow);

        TextView dragHint = new TextView(this);
        dragHint.setText("⠿ drag");
        dragHint.setTextColor(Color.GRAY);
        dragHint.setTextSize(10);
        dragHint.setGravity(Gravity.CENTER);
        root.addView(dragHint);

        root.setOnTouchListener((v, event) -> {
            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    initialX = params.x;
                    initialY = params.y;
                    initialTouchX = (int) event.getRawX();
                    initialTouchY = (int) event.getRawY();
                    return true;

                case MotionEvent.ACTION_MOVE:
                    params.x = (int) (
                            initialX + event.getRawX() - initialTouchX
                    );

                    params.y = (int) (
                            initialY + event.getRawY() - initialTouchY
                    );

                    try {
                        windowManager.updateViewLayout(overlayView, params);
                    } catch (Exception e) {
                        Log.e(TAG, "Failed to move WASD overlay", e);
                    }
                    return true;
            }

            return false;
        });

        params = createWindowParams(Gravity.BOTTOM | Gravity.START);
        params.x = 24;
        params.y = 120;

        overlayView = root;

        try {
            windowManager.addView(overlayView, params);
        } catch (Exception e) {
            Log.e(TAG, "Failed to add WASD overlay", e);
            overlayView = null;
        }
    }

    private void showSpaceButton() {
        if (spaceView != null) {
            return;
        }

        Button buttonSpace = createKeyButton(
                "SPACE",
                android.view.KeyEvent.KEYCODE_SPACE
        );

        spaceView = buttonSpace;

        spaceParams = createWindowParams(Gravity.BOTTOM | Gravity.END);
        spaceParams.x = 24;
        spaceParams.y = 120;

        buttonSpace.setOnTouchListener((v, event) -> {
            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    spaceInitialX = spaceParams.x;
                    spaceInitialY = spaceParams.y;
                    spaceInitialTouchX = (int) event.getRawX();
                    spaceInitialTouchY = (int) event.getRawY();

                    GameInputIME.keyDown(
                            android.view.KeyEvent.KEYCODE_SPACE
                    );
                    return true;

                case MotionEvent.ACTION_MOVE:
                    spaceParams.x = (int) (
                            spaceInitialX - event.getRawX()
                                    + spaceInitialTouchX
                    );

                    spaceParams.y = (int) (
                            spaceInitialY - event.getRawY()
                                    + spaceInitialTouchY
                    );

                    try {
                        windowManager.updateViewLayout(spaceView, spaceParams);
                    } catch (Exception e) {
                        Log.e(TAG, "Failed to move SPACE", e);
                    }
                    return true;

                case MotionEvent.ACTION_UP:
                case MotionEvent.ACTION_CANCEL:
                    GameInputIME.keyUp(
                            android.view.KeyEvent.KEYCODE_SPACE
                    );
                    return true;
            }

            return true;
        });

        try {
            windowManager.addView(spaceView, spaceParams);
        } catch (Exception e) {
            Log.e(TAG, "Failed to add SPACE button", e);
            spaceView = null;
        }
    }

    private void showNumberKeyboard() {
        if (numberView != null) {
            return;
        }

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(8, 8, 8, 8);

        GradientDrawable background = new GradientDrawable();
        background.setColor(Color.argb(220, 30, 30, 30));
        background.setCornerRadius(16);
        background.setStroke(2, Color.argb(180, 100, 180, 255));
        root.setBackground(background);

        LinearLayout titleRow = new LinearLayout(this);
        titleRow.setOrientation(LinearLayout.HORIZONTAL);
        titleRow.setGravity(Gravity.CENTER_VERTICAL);

        TextView title = new TextView(this);
        title.setText(" 1234567890 ");
        title.setTextColor(Color.WHITE);
        title.setTextSize(13);
        titleRow.addView(title);

        Button closeButton = new Button(this);
        closeButton.setText("×");
        closeButton.setTextColor(Color.WHITE);
        closeButton.setMinWidth(40);
        closeButton.setMinHeight(40);
        closeButton.setOnClickListener(v -> {
            removeNumberKeyboard();
            numberKeyboardVisible = false;
        });
        titleRow.addView(closeButton);
        root.addView(titleRow);

        LinearLayout firstRow = new LinearLayout(this);
        firstRow.setOrientation(LinearLayout.HORIZONTAL);

        for (int number = 1; number <= 5; number++) {
            firstRow.addView(createKeyButton(
                    String.valueOf(number),
                    getNumberKeyCode(number)
            ));
        }

        root.addView(firstRow);

        LinearLayout secondRow = new LinearLayout(this);
        secondRow.setOrientation(LinearLayout.HORIZONTAL);

        for (int number = 6; number <= 9; number++) {
            secondRow.addView(createKeyButton(
                    String.valueOf(number),
                    getNumberKeyCode(number)
            ));
        }

        secondRow.addView(createKeyButton(
                "0",
                android.view.KeyEvent.KEYCODE_0
        ));

        root.addView(secondRow);

        TextView dragHint = new TextView(this);
        dragHint.setText("⠿ drag");
        dragHint.setTextColor(Color.GRAY);
        dragHint.setTextSize(10);
        dragHint.setGravity(Gravity.CENTER);
        root.addView(dragHint);

        numberView = root;

        numberParams = createWindowParams(Gravity.BOTTOM | Gravity.END);
        numberParams.x = 24;
        numberParams.y = 300;

        root.setOnTouchListener((v, event) -> {
            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    numberInitialX = numberParams.x;
                    numberInitialY = numberParams.y;
                    numberInitialTouchX = (int) event.getRawX();
                    numberInitialTouchY = (int) event.getRawY();
                    return true;

                case MotionEvent.ACTION_MOVE:
                    numberParams.x = (int) (
                            numberInitialX - event.getRawX()
                                    + numberInitialTouchX
                    );

                    numberParams.y = (int) (
                            numberInitialY - event.getRawY()
                                    + numberInitialTouchY
                    );

                    try {
                        windowManager.updateViewLayout(numberView, numberParams);
                    } catch (Exception e) {
                        Log.e(TAG, "Failed to move number keyboard", e);
                    }
                    return true;
            }

            return false;
        });

        try {
            windowManager.addView(numberView, numberParams);
        } catch (Exception e) {
            Log.e(TAG, "Failed to add number keyboard", e);
            numberView = null;
        }
    }

    private int getNumberKeyCode(int number) {
        switch (number) {
            case 1: return android.view.KeyEvent.KEYCODE_1;
            case 2: return android.view.KeyEvent.KEYCODE_2;
            case 3: return android.view.KeyEvent.KEYCODE_3;
            case 4: return android.view.KeyEvent.KEYCODE_4;
            case 5: return android.view.KeyEvent.KEYCODE_5;
            case 6: return android.view.KeyEvent.KEYCODE_6;
            case 7: return android.view.KeyEvent.KEYCODE_7;
            case 8: return android.view.KeyEvent.KEYCODE_8;
            case 9: return android.view.KeyEvent.KEYCODE_9;
            default: return android.view.KeyEvent.KEYCODE_0;
        }
    }

    private Button createKeyButton(String text, int keyCode) {
        Button button = new Button(this);

        button.setText(text);
        button.setTextSize(16);
        button.setTextColor(Color.WHITE);
        button.setMinWidth(72);
        button.setMinHeight(72);
        button.setPadding(8, 8, 8, 8);

        GradientDrawable background = new GradientDrawable();
        background.setColor(Color.argb(220, 50, 50, 70));
        background.setCornerRadius(12);
        background.setStroke(2, Color.argb(200, 80, 160, 255));
        button.setBackground(background);

        button.setOnTouchListener((v, event) -> {
            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    GameInputIME.keyDown(keyCode);
                    return true;

                case MotionEvent.ACTION_UP:
                case MotionEvent.ACTION_CANCEL:
                    GameInputIME.keyUp(keyCode);
                    return true;
            }

            return true;
        });

        return button;
    }

    private void showAppMenu(View anchor) {
        PopupMenu menu = new PopupMenu(this, anchor);

        menu.getMenu().add(0, 0, 0, "Open app list");
        menu.getMenu().add(0, 1, 1, "Open Charge Controller");
        menu.getMenu().add(0, 2, 2, "Number Keyboard");

        menu.setOnMenuItemClickListener(item -> {
            if (item.getItemId() == 0) {
                Intent intent = new Intent(this, MainActivity.class);
                intent.addFlags(
                        Intent.FLAG_ACTIVITY_NEW_TASK
                                | Intent.FLAG_ACTIVITY_SINGLE_TOP
                );
                startActivity(intent);

            } else if (item.getItemId() == 1) {
                Intent intent = new Intent(this, MainActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(intent);

            } else if (item.getItemId() == 2) {
                if (numberKeyboardVisible) {
                    removeNumberKeyboard();
                    numberKeyboardVisible = false;
                } else {
                    showNumberKeyboard();
                    numberKeyboardVisible = numberView != null;
                }
            }

            return true;
        });

        menu.show();
    }

    private void removeOverlay() {
        if (overlayView != null && windowManager != null) {
            try {
                windowManager.removeView(overlayView);
            } catch (Exception e) {
                Log.w(TAG, "Error removing WASD overlay", e);
            }

            overlayView = null;
        }
    }

    private void removeSpaceButton() {
        if (spaceView != null && windowManager != null) {
            try {
                windowManager.removeView(spaceView);
            } catch (Exception e) {
                Log.w(TAG, "Error removing SPACE button", e);
            }

            spaceView = null;
        }
    }

    private void removeNumberKeyboard() {
        if (numberView != null && windowManager != null) {
            try {
                windowManager.removeView(numberView);
            } catch (Exception e) {
                Log.w(TAG, "Error removing number keyboard", e);
            }

            numberView = null;
        }
    }
            }
