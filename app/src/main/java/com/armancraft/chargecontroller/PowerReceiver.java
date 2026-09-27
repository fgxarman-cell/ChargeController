package com.armancraft.chargecontroller;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.util.Log;

/**
 * Receives ACTION_POWER_CONNECTED and ACTION_POWER_DISCONNECTED.
 * Starts / stops OverlayService accordingly.
 */
public class PowerReceiver extends BroadcastReceiver {

    private static final String TAG = "PowerReceiver";

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent == null || intent.getAction() == null) return;

        String action = intent.getAction();
        Log.d(TAG, "Received: " + action);

        if (Intent.ACTION_POWER_CONNECTED.equals(action)) {
            // Start overlay service
            Intent serviceIntent = new Intent(context, OverlayService.class);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(serviceIntent);
            } else {
                context.startService(serviceIntent);
            }
        } else if (Intent.ACTION_POWER_DISCONNECTED.equals(action)) {
            // Stop overlay service
            Intent serviceIntent = new Intent(context, OverlayService.class);
            context.stopService(serviceIntent);
        }
    }
}
