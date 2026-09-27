package com.armancraft.chargecontroller;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.content.SharedPreferences;
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
import android.widget.Toast;

import androidx.core.app.NotificationCompat;

public class OverlayService extends Service {

    private static final String TAG = "OverlayService";
    private static final String CHANNEL_ID = "charge_controller_overlay";
    private static final int NOTIFICATION_ID = 1001;

    private WindowManager windowManager;
    private View overlayView;
    private WindowManager.LayoutParams params;

    private float initialX;
    private float initialY;
    private int initialTouchX;
    private int initialTouchY;

    private SharedPreferences prefs;

    @Override
    public void onCreate() {
        super.onCreate();

        prefs = getSharedPreferences(
                "ChargeControllerPrefs",
                MODE_PRIVATE
        );

        windowManager =
                (WindowManager) getSystemService(WINDOW_SERVICE);

        createNotificationChannel();

        startForeground(
                NOTIFICATION_ID,
                buildNotification()
        );

        showOverlay();
    }

    @Override
    public int onStartCommand(
            Intent intent,
            int flags,
            int startId
    ) {

        if (overlayView == null) {
            showOverlay();
        }

        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        removeOverlay();
        super.onDestroy();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    private void createNotificationChannel() {

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {

            NotificationChannel channel =
                    new NotificationChannel(
                            CHANNEL_ID,
                            "Charge Controller Overlay",
                            NotificationManager.IMPORTANCE_LOW
                    );

            channel.setDescription(
                    "Charge Controller overlay"
            );

            NotificationManager manager =
                    getSystemService(NotificationManager.class);

            if (manager != null) {
                manager.createNotificationChannel(channel);
            }
        }
    }

    private Notification buildNotification() {

        Intent intent =
                new Intent(this, MainActivity.class);

        PendingIntent pendingIntent =
                PendingIntent.getActivity(
                        this,
                        0,
                        intent,
                        PendingIntent.FLAG_UPDATE_CURRENT |
                                PendingIntent.FLAG_IMMUTABLE
                );

        return new NotificationCompat.Builder(
                this,
                CHANNEL_ID
        )
                .setContentTitle("Charge Controller")
                .setContentText("Controller overlay active")
                .setSmallIcon(
                        android.R.drawable.ic_menu_compass
                )
                .setContentIntent(pendingIntent)
                .setOngoing(true)
                .build();
    }

    private void showOverlay() {

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M &&
                !Settings.canDrawOverlays(this)) {

            Log.w(
                    TAG,
                    "Overlay permission not granted"
            );

            stopSelf();
            return;
        }

        if (overlayView != null) {
            return;
        }

        LinearLayout root =
                new LinearLayout(this);

        root.setOrientation(
                LinearLayout.VERTICAL
        );

        root.setPadding(
                8,
                8,
                8,
                8
        );

        GradientDrawable background =
                new GradientDrawable();

        background.setColor(
                Color.argb(
                        210,
                        30,
                        30,
                        30
                )
        );

        background.setCornerRadius(18);

        background.setStroke(
                2,
                Color.argb(
                        180,
                        100,
                        180,
                        255
                )
        );

        root.setBackground(background);

        LinearLayout topRow =
                new LinearLayout(this);

        topRow.setOrientation(
                LinearLayout.HORIZONTAL
        );

        topRow.setGravity(
                Gravity.CENTER_VERTICAL
        );

        Button menuButton =
                new Button(this);

        menuButton.setText("☰");
        menuButton.setTextSize(14);
        menuButton.setTextColor(Color.WHITE);

        menuButton.setMinWidth(0);
        menuButton.setMinHeight(0);

        menuButton.setPadding(
                12,
                4,
                12,
                4
        );

        menuButton.setBackgroundColor(
                Color.TRANSPARENT
        );

        menuButton.setOnClickListener(
                v -> showAppMenu(v)
        );

        topRow.addView(menuButton);

        TextView title =
                new TextView(this);

        title.setText(" WASD ");
        title.setTextColor(Color.LTGRAY);
        title.setTextSize(11);

        title.setPadding(
                4,
                8,
                4,
                0
        );

        topRow.addView(title);

        root.addView(topRow);

        Button buttonW =
                createKeyButton(
                        "W",
                        android.view.KeyEvent.KEYCODE_W
                );

        LinearLayout wRow =
                new LinearLayout(this);

        wRow.setGravity(
                Gravity.CENTER_HORIZONTAL
        );

        wRow.addView(buttonW);

        root.addView(wRow);

        LinearLayout asdRow =
                new LinearLayout(this);

        asdRow.setOrientation(
                LinearLayout.HORIZONTAL
        );

        asdRow.setGravity(
                Gravity.CENTER_HORIZONTAL
        );

        Button buttonA =
                createKeyButton(
                        "A",
                        android.view.KeyEvent.KEYCODE_A
                );

        Button buttonS =
                createKeyButton(
                        "S",
                        android.view.KeyEvent.KEYCODE_S
                );

        Button buttonD =
                createKeyButton(
                        "D",
                        android.view.KeyEvent.KEYCODE_D
                );

        asdRow.addView(buttonA);
        asdRow.addView(buttonS);
        asdRow.addView(buttonD);

        root.addView(asdRow);

        TextView dragHint =
                new TextView(this);

        dragHint.setText("⠿ drag");
        dragHint.setTextColor(Color.GRAY);
        dragHint.setTextSize(10);
        dragHint.setGravity(Gravity.CENTER);

        root.addView(dragHint);

        root.setOnTouchListener(
                new View.OnTouchListener() {

                    @Override
                    public boolean onTouch(
                            View v,
                            MotionEvent event
                    ) {

                        switch (event.getAction()) {

                            case MotionEvent.ACTION_DOWN:

                                initialX =
                                        params.x;

                                initialY =
                                        params.y;

                                initialTouchX =
                                        (int) event.getRawX();

                                initialTouchY =
                                        (int) event.getRawY();

                                return true;

                            case MotionEvent.ACTION_MOVE:

                                params.x =
                                        (int) (
                                                initialX +
                                                        event.getRawX()
                                                                -
                                                                initialTouchX
                                        );

                                params.y =
                                        (int) (
                                                initialY +
                                                        event.getRawY()
                                                                -
                                                                initialTouchY
                                        );

                                try {

                                    windowManager.updateViewLayout(
                                            overlayView,
                                            params
                                    );

                                } catch (Exception e) {

                                    Log.e(
                                            TAG,
                                            "Failed to move overlay",
                                            e
                                    );
                                }

                                return true;
                        }

                        return false;
                    }
                }
        );

        int windowType;

        if (Build.VERSION.SDK_INT >=
                Build.VERSION_CODES.O) {

            windowType =
                    WindowManager.LayoutParams
                            .TYPE_APPLICATION_OVERLAY;

        } else {

            windowType =
                    WindowManager.LayoutParams.TYPE_PHONE;
        }

        params =
                new WindowManager.LayoutParams(
                        WindowManager.LayoutParams.WRAP_CONTENT,
                        WindowManager.LayoutParams.WRAP_CONTENT,
                        windowType,
                        WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                                |
                                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN
                                |
                                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
                        PixelFormat.TRANSLUCENT
                );

        params.gravity =
                Gravity.BOTTOM |
                        Gravity.START;

        params.x = 24;
        params.y = 120;

        overlayView = root;

        try {

            windowManager.addView(
                    overlayView,
                    params
            );

            Log.i(
                    TAG,
                    "Overlay added"
            );

        } catch (Exception e) {

            Log.e(
                    TAG,
                    "Failed to add overlay",
                    e
            );

            overlayView = null;
            stopSelf();
        }
    }

    private Button createKeyButton(
            String text,
            int keyCode
    ) {

        Button button =
                new Button(this);

        button.setText(text);
        button.setTextSize(16);
        button.setTextColor(Color.WHITE);

        button.setMinWidth(72);
        button.setMinHeight(72);

        button.setPadding(
                8,
                8,
                8,
                8
        );

        GradientDrawable background =
                new GradientDrawable();

        background.setColor(
                Color.argb(
                        220,
                        50,
                        50,
                        70
                )
        );

        background.setCornerRadius(12);

        background.setStroke(
                2,
                Color.argb(
                        200,
                        80,
                        160,
                        255
                )
        );

        button.setBackground(background);

        button.setOnClickListener(
                v -> sendKey(keyCode)
        );

        return button;
    }

    private void sendKey(int keyCode) {

        boolean result =
                GameInputIME.sendKey(keyCode);

        if (!result) {

            Toast.makeText(
                    this,
                    "Keyboard connection is not active",
                    Toast.LENGTH_SHORT
            ).show();
        }
    }

    private void showAppMenu(View anchor) {

        PopupMenu menu =
                new PopupMenu(
                        this,
                        anchor
                );

        menu.getMenu().add(
                0,
                0,
                0,
                "Open app list"
        );

        menu.getMenu().add(
                0,
                1,
                1,
                "Open Charge Controller"
        );

        menu.setOnMenuItemClickListener(
                item -> {

                    if (item.getItemId() == 0) {

                        Intent intent =
                                new Intent(
                                        this,
                                        MainActivity.class
                                );

                        intent.addFlags(
                                Intent.FLAG_ACTIVITY_NEW_TASK |
                                        Intent.FLAG_ACTIVITY_SINGLE_TOP
                        );

                        startActivity(intent);

                    } else if (
                            item.getItemId() == 1
                    ) {

                        Intent intent =
                                new Intent(
                                        this,
                                        MainActivity.class
                                );

                        intent.addFlags(
                                Intent.FLAG_ACTIVITY_NEW_TASK
                        );

                        startActivity(intent);
                    }

                    return true;
                }
        );

        menu.show();
    }

    private void removeOverlay() {

        if (overlayView != null &&
                windowManager != null) {

            try {

                windowManager.removeView(
                        overlayView
                );

            } catch (Exception e) {

                Log.w(
                        TAG,
                        "Error removing overlay",
                        e
                );
            }

            overlayView = null;
        }
    }
}
