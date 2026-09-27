package com.armancraft.chargecontroller;

import android.accessibilityservice.AccessibilityService;
import android.accessibilityservice.GestureDescription;
import android.content.Intent;
import android.os.Build;
import android.util.Log;
import android.view.KeyEvent;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;

/**
 * AccessibilityService intended to attempt injection of keyboard events.
 *
 * IMPORTANT ANDROID LIMITATION:
 * Android does NOT provide a public, reliable API for an AccessibilityService
 * to inject arbitrary KEYCODE_W / KEYCODE_A / KEYCODE_S / KEYCODE_D events
 * into another application (including GameHub).
 *
 * - performGlobalAction() only supports a limited set of global actions.
 * - dispatchGesture() can simulate touch gestures, not hardware key events.
 * - There is no official "injectKeyEvent" method that works across apps
 *   for third-party packages without system privileges / being the IME.
 *
 * Therefore this service deliberately does NOT pretend that key injection works.
 * The methods below contain explicit TODOs and log the restriction.
 *
 * Users must enable this service manually from Settings → Accessibility.
 * The service never tries to enable itself silently.
 */
public class MyAccessibilityService extends AccessibilityService {

    private static final String TAG = "MyAccessibilityService";
    private static MyAccessibilityService instance;

    public static MyAccessibilityService getInstance() {
        return instance;
    }

    @Override
    public void onServiceConnected() {
        super.onServiceConnected();
        instance = this;
        Log.i(TAG, "AccessibilityService connected. Key injection is NOT guaranteed.");
    }

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        // We do not process events for key injection; this service exists mainly
        // so the user can enable it and so the app can check its status.
    }

    @Override
    public void onInterrupt() {
        Log.i(TAG, "AccessibilityService interrupted");
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        instance = null;
    }

    /**
     * Attempt to send a key event to the currently focused window.
     *
     * TODO: Android does not expose a reliable public API for injecting
     * hardware key events (KEYCODE_W, KEYCODE_A, KEYCODE_S, KEYCODE_D)
     * into another application from an AccessibilityService.
     *
     * Possible (but incomplete / restricted) approaches that still do not
     * guarantee delivery to GameHub or similar apps:
     * 1. Being an Input Method Editor (IME) – requires different architecture.
     * 2. Using Instrumentation (only works in instrumentation tests / same process).
     * 3. Root / system-signed apps (not available for normal Play Store / sideload apps).
     *
     * Therefore this method only logs the attempt and returns false.
     * Do not claim that W/A/S/D works for GameHub.
     *
     * @param keyCode one of KeyEvent.KEYCODE_W, KEYCODE_A, KEYCODE_S, KEYCODE_D
     * @return always false – injection is not supported by public APIs
     */
    public boolean tryInjectKey(int keyCode) {
        Log.w(TAG, "tryInjectKey(" + keyCode + ") called. "
                + "Android AccessibilityService cannot reliably inject KEYCODE events "
                + "into third-party apps (including GameHub). See source comments.");
        // Explicitly do nothing that pretends to work.
        // No performGlobalAction, no fake success.
        return false;
    }

    /**
     * Convenience helpers – they all delegate to tryInjectKey and therefore fail.
     */
    public boolean pressW() { return tryInjectKey(KeyEvent.KEYCODE_W); }
    public boolean pressA() { return tryInjectKey(KeyEvent.KEYCODE_A); }
    public boolean pressS() { return tryInjectKey(KeyEvent.KEYCODE_S); }
    public boolean pressD() { return tryInjectKey(KeyEvent.KEYCODE_D); }
}
