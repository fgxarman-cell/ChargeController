package com.armancraft.chargecontroller;

import android.accessibilityservice.AccessibilityServiceInfo;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.BatteryManager;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.view.View;
import android.view.accessibility.AccessibilityManager;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private TextView tvStatus;
    private TextView tvSelectedApp;
    private Button btnOverlayPermission;
    private Button btnAccessibility;
    private Button btnSelectApp;
    private Button btnStartService;
    private Button btnStopService;

    private SharedPreferences prefs;
    private static final String PREFS_NAME = "ChargeControllerPrefs";
    private static final String KEY_SELECTED_PACKAGE = "selected_package";
    private static final String KEY_SELECTED_LABEL = "selected_label";

    private BroadcastReceiver statusReceiver;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);

        tvStatus = findViewById(R.id.tvStatus);
        tvSelectedApp = findViewById(R.id.tvSelectedApp);
        btnOverlayPermission = findViewById(R.id.btnOverlayPermission);
        btnAccessibility = findViewById(R.id.btnAccessibility);
        btnSelectApp = findViewById(R.id.btnSelectApp);
        btnStartService = findViewById(R.id.btnStartService);
        btnStopService = findViewById(R.id.btnStopService);

        updateSelectedAppLabel();
        checkInitialChargeState();

        btnOverlayPermission.setOnClickListener(v -> requestOverlayPermission());
        btnAccessibility.setOnClickListener(v -> openAccessibilitySettings());
        btnSelectApp.setOnClickListener(v -> showAppPicker());
        btnStartService.setOnClickListener(v -> startOverlayService());
        btnStopService.setOnClickListener(v -> stopOverlayService());

        // Register for power events while activity is visible (extra status update)
        statusReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                String action = intent.getAction();
                if (Intent.ACTION_POWER_CONNECTED.equals(action)) {
                    tvStatus.setText(R.string.cable_connected);
                } else if (Intent.ACTION_POWER_DISCONNECTED.equals(action)) {
                    tvStatus.setText(R.string.cable_disconnected);
                }
            }
        };
        IntentFilter filter = new IntentFilter();
        filter.addAction(Intent.ACTION_POWER_CONNECTED);
        filter.addAction(Intent.ACTION_POWER_DISCONNECTED);
        registerReceiver(statusReceiver, filter);
    }

    @Override
    protected void onResume() {
        super.onResume();
        checkInitialChargeState();
        updateSelectedAppLabel();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (statusReceiver != null) {
            try {
                unregisterReceiver(statusReceiver);
            } catch (Exception ignored) {
            }
        }
    }

    private void checkInitialChargeState() {
        IntentFilter ifilter = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
        Intent batteryStatus = registerReceiver(null, ifilter);
        if (batteryStatus != null) {
            int status = batteryStatus.getIntExtra(BatteryManager.EXTRA_STATUS, -1);
            boolean isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING
                    || status == BatteryManager.BATTERY_STATUS_FULL;
            int plugged = batteryStatus.getIntExtra(BatteryManager.EXTRA_PLUGGED, -1);
            boolean isPlugged = plugged == BatteryManager.BATTERY_PLUGGED_AC
                    || plugged == BatteryManager.BATTERY_PLUGGED_USB
                    || plugged == BatteryManager.BATTERY_PLUGGED_WIRELESS;

            if (isPlugged || isCharging) {
                tvStatus.setText(R.string.cable_connected);
            } else {
                tvStatus.setText(R.string.cable_disconnected);
            }
        }
    }

    private void requestOverlayPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (!Settings.canDrawOverlays(this)) {
                // On Android 11 the settings page may behave differently; still use the official intent
                Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                        Uri.parse("package:" + getPackageName()));
                startActivity(intent);
                Toast.makeText(this, R.string.overlay_permission_needed, Toast.LENGTH_LONG).show();
            } else {
                Toast.makeText(this, "Overlay permission already granted", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void openAccessibilitySettings() {
        Intent intent = new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS);
        startActivity(intent);
        Toast.makeText(this, R.string.accessibility_permission_needed, Toast.LENGTH_LONG).show();
    }

    private boolean isAccessibilityServiceEnabled() {
        AccessibilityManager am = (AccessibilityManager) getSystemService(ACCESSIBILITY_SERVICE);
        if (am == null) return false;
        List<AccessibilityServiceInfo> enabled = am.getEnabledAccessibilityServiceList(
                AccessibilityServiceInfo.FEEDBACK_ALL_MASK);
        for (AccessibilityServiceInfo info : enabled) {
            if (info.getResolveInfo().serviceInfo.packageName.equals(getPackageName())) {
                return true;
            }
        }
        return false;
    }

    private void showAppPicker() {
        PackageManager pm = getPackageManager();
        Intent mainIntent = new Intent(Intent.ACTION_MAIN, null);
        mainIntent.addCategory(Intent.CATEGORY_LAUNCHER);
        List<ResolveInfo> apps = pm.queryIntentActivities(mainIntent, 0);

        final List<AppItem> items = new ArrayList<>();
        for (ResolveInfo ri : apps) {
            String label = ri.loadLabel(pm).toString();
            String pkg = ri.activityInfo.packageName;
            items.add(new AppItem(label, pkg));
        }
        Collections.sort(items, new Comparator<AppItem>() {
            @Override
            public int compare(AppItem o1, AppItem o2) {
                return o1.label.compareToIgnoreCase(o2.label);
            }
        });

        String[] labels = new String[items.size()];
        for (int i = 0; i < items.size(); i++) {
            labels[i] = items.get(i).label + "\n(" + items.get(i).packageName + ")";
        }

        new AlertDialog.Builder(this)
                .setTitle(R.string.select_app)
                .setItems(labels, (dialog, which) -> {
                    AppItem selected = items.get(which);
                    prefs.edit()
                            .putString(KEY_SELECTED_PACKAGE, selected.packageName)
                            .putString(KEY_SELECTED_LABEL, selected.label)
                            .apply();
                    updateSelectedAppLabel();
                    // Launch the selected app
                    Intent launch = pm.getLaunchIntentForPackage(selected.packageName);
                    if (launch != null) {
                        launch.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                        startActivity(launch);
                    }
                    Toast.makeText(this, "Selected: " + selected.label, Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void updateSelectedAppLabel() {
        String label = prefs.getString(KEY_SELECTED_LABEL, null);
        if (label != null) {
            tvSelectedApp.setText("Target: " + label);
        } else {
            tvSelectedApp.setText(R.string.no_app_selected);
        }
    }

    private void startOverlayService() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(this)) {
            Toast.makeText(this, R.string.overlay_permission_needed, Toast.LENGTH_LONG).show();
            requestOverlayPermission();
            return;
        }
        Intent intent = new Intent(this, OverlayService.class);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(intent);
        } else {
            startService(intent);
        }
        Toast.makeText(this, "Overlay service started", Toast.LENGTH_SHORT).show();
    }

    private void stopOverlayService() {
        Intent intent = new Intent(this, OverlayService.class);
        stopService(intent);
        Toast.makeText(this, "Overlay service stopped", Toast.LENGTH_SHORT).show();
    }

    private static class AppItem {
        final String label;
        final String packageName;

        AppItem(String label, String packageName) {
            this.label = label;
            this.packageName = packageName;
        }
    }
}
