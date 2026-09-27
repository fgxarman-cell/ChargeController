package com.armancraft.chargecontroller;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
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

/**
 * Foreground service that shows a small draggable W/A/S/D overlay
 * when the device is charging.
 */
public class OverlayService extends Service {

    private static final String TAG = "OverlayService";
    private static final String CHANNEL_ID = "charge_controller_overlay";
    private static final int NOTIFICATION_ID = 1001;

    private WindowManager windowManager;
    private View overlayView;
    private WindowManager.LayoutParams params;

    private float initialX, initialY;
    private int initialTouchX, initialTouchY;

    private SharedPreferences prefs;

    @Override
    public void onCreate() {
        super.onCreate();
        prefs = getSharedPreferences("ChargeControllerPrefs", MODE_PRIVATE);
        windowManager = (WindowManager) getSystemService(WINDOW_SERVICE);
        createNotificationChannel();
        startForeground(NOTIFICATION_ID, buildNotification());
        showOverlay();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (overlayView == null) {
            showOverlay();
        }
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        removeOverlay();
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
                    NotificationManager.IMPORTANCE_LOW);
            channel.setDescription("Keeps the gamepad overlay alive while charging");
            NotificationManager nm = getSystemService(NotificationManager.class);
            if (nm != null) {
                nm.createNotificationChannel(channel);
            }
        }
    }

    private Notification buildNotification() {
        Intent open = new Intent(this, MainActivity.class);
        PendingIntent pi = PendingIntent.getActivity(this, 0, open,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        return new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("Charge Controller")
                .setContentText("Gamepad overlay active while charging")
                .setSmallIcon(android.R.drawable.ic_menu_compass)
                .setContentIntent(pi)
                .setOngoing(true)
                .build();
    }

    private void showOverlay() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(this)) {
            Log.w(TAG, "Cannot show overlay – SYSTEM_ALERT_WINDOW not granted");
            stopSelf();
            return;
        }
        if (overlayView != null) return;

        // Build overlay UI programmatically (no extra layout file required)
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(8, 8, 8, 8);

        GradientDrawable bg = new GradientDrawable();
        bg.setColor(Color.argb(200, 30, 30, 30));
        bg.setCornerRadius(16);
        bg.setStroke(2, Color.argb(180, 100, 180, 255));
        root.setBackground(bg);

        // Menu button (top-left of the panel)
        LinearLayout topRow = new LinearLayout(this);
        topRow.setOrientation(LinearLayout.HORIZONTAL);
        topRow.setGravity(Gravity.START);

        Button menuBtn = new Button(this);
        menuBtn.setText("☰");
        menuBtn.setTextSize(14);
        menuBtn.setMinWidth(0);
        menuBtn.setMinHeight(0);
        menuBtn.setPadding(12, 4, 12, 4);
        menuBtn.setBackgroundColor(Color.TRANSPARENT);
        menuBtn.setTextColor(Color.WHITE);
        menuBtn.setOnClickListener(v -> showAppMenu(v));
        topRow.addView(menuBtn);

        TextView title = new TextView(this);
        title.setText(" WASD ");
        title.setTextColor(Color.LTGRAY);
        title.setTextSize(11);
        title.setPadding(4, 8, 4, 0);
        topRow.addView(title);

        root.addView(topRow);

        // W button
        Button btnW = createKeyButton("W", () -> sendKeyAttempt("W"));
        LinearLayout wRow = new LinearLayout(this);
        wRow.setGravity(Gravity.CENTER_HORIZONTAL);
        wRow.addView(btnW);
        root.addView(wRow);

        // A S D row
        LinearLayout asdRow = new LinearLayout(this);
        asdRow.setOrientation(LinearLayout.HORIZONTAL);
        asdRow.setGravity(Gravity.CENTER_HORIZONTAL);

        Button btnA = createKeyButton("A", () -> sendKeyAttempt("A"));
        Button btnS = createKeyButton("S", () -> sendKeyAttempt("S"));
        Button btnD = createKeyButton("D", () -> sendKeyAttempt("D"));

        asdRow.addView(btnA);
        asdRow.addView(btnS);
        asdRow.addView(btnD);
        root.addView(asdRow);

        // Drag handle hint
        TextView dragHint = new TextView(this);
        dragHint.setText("⠿ drag");
        dragHint.setTextColor(Color.GRAY);
        dragHint.setTextSize(10);
        dragHint.setGravity(Gravity.CENTER);
        root.addView(dragHint);

        // Make the whole panel draggable
        root.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        initialX = params.x;
                        initialY = params.y;
                        initialTouchX = (int) event.getRawX();
                        initialTouchY = (int) event.getRawY();
                        return true;
                    case MotionEvent.ACTION_MOVE:
                        params.x = (int) (initialX + (event.getRawX() - initialTouchX));
                        params.y = (int) (initialY + (event.getRawY() - initialTouchY));
                        windowManager.updateViewLayout(overlayView, params);
                        return true;
                }
                return false;
            }
        });

        int type = Build.VERSION.SDK_INT >= Build.VERSION_CODES.O
                ? WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
                : WindowManager.LayoutParams.TYPE_PHONE;

        params = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                type,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                        | WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN
                        | WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
                PixelFormat.TRANSLUCENT);

        params.gravity = Gravity.BOTTOM | Gravity.START;
        params.x = 24;
        params.y = 120;

        overlayView = root;
        try {
            windowManager.addView(overlayView, params);
            Log.i(TAG, "Overlay added");
        } catch (Exception e) {
            Log.e(TAG, "Failed to add overlay", e);
            stopSelf();
        }
    }

    private Button createKeyButton(String label, Runnable action) {
        Button b = new Button(this);
        b.setText(label);
        b.setTextSize(16);
        b.setTextColor(Color.WHITE);
        b.setMinWidth(72);
        b.setMinHeight(72);
        b.setPadding(8, 8, 8, 8);

        GradientDrawable d = new GradientDrawable();
        d.setColor(Color.argb(220, 50, 50, 70));
        d.setCornerRadius(12);
        d.setStroke(2, Color.argb(200, 80, 160, 255));
        b.setBackground(d);

        b.setOnClickListener(v -> action.run());
        // Prevent drag when pressing buttons – consume the touch
        b.setOnTouchListener((v, event) -> {
            // Let the button handle click; do not propagate to root drag
            if (event.getAction() == MotionEvent.ACTION_UP) {
                v.performClick();
            }
            return true;
        });
        return b;
    }

    private void sendKeyAttempt(String key) {
        MyAccessibilityService service = MyAccessibilityService.getInstance();
        if (service == null) {
            Toast.makeText(this,
                    "Accessibility Service not enabled.\nOpen Settings → Accessibility and enable Charge Controller.",
                    Toast.LENGTH_LONG).show();
            return;
        }
        boolean ok;
        switch (key) {
            case "W": ok = service.pressW(); break;
            case "A": ok = service.pressA(); break;
            case "S": ok = service.pressS(); break;
            case "D": ok = service.pressD(); break;
            default: ok = false;
        }
        if (!ok) {
            // Honest feedback – do not pretend it worked
            Toast.makeText(this,
                    "Key injection not supported by Android public APIs.\n" +
                    "See README / source comments. GameHub injection is NOT guaranteed.",
                    Toast.LENGTH_LONG).show();
        }
    }

    private void showAppMenu(View anchor) {
        PopupMenu menu = new PopupMenu(this, anchor);
        menu.getMenu().add(0, 0, 0, "Open app list (from MainActivity)");
        menu.getMenu().add(0, 1, 1, "Open Charge Controller");
        menu.setOnMenuItemClickListener(item -> {
            if (item.getItemId() == 0) {
                // Bring MainActivity to front so user can pick an app
                Intent i = new Intent(this, MainActivity.class);
                i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                startActivity(i);
            } else if (item.getItemId() == 1) {
                Intent i = new Intent(this, MainActivity.class);
                i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(i);
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
                Log.w(TAG, "Error removing overlay", e);
            }
            overlayView = null;
        }
    }
}
