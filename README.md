# Charge Controller

Android application that shows a small draggable W/A/S/D gamepad overlay when the phone is charging.

**Target:** Android 11 (API 30), Redmi Note 8 Pro / ARM64  
**Language:** Java  
**Min SDK:** 23 **Target SDK:** 30 **Compile SDK:** 35

## Important limitations (read carefully)

### Key injection (W / A / S / D)

Android **does not** provide a public, reliable API that lets an `AccessibilityService` inject real hardware key events (`KEYCODE_W`, `KEYCODE_A`, `KEYCODE_S`, `KEYCODE_D`) into another application (including GameHub).

- `performGlobalAction()` only supports a fixed set of global actions.
- `dispatchGesture()` can simulate touch gestures, **not** key events.
- Only system-signed apps, root, or an Input Method Editor (IME) can inject keys with any reliability.
- Therefore the code **explicitly does not pretend** that key injection works.  
  Methods contain `TODO` comments and always return `false`.  
  A toast informs the user of the restriction.

**Sending W/A/S/D to GameHub is NOT guaranteed by Android.**

### Permissions

| Permission | Why it is needed |
|------------|------------------|
| `SYSTEM_ALERT_WINDOW` | Draw the gamepad overlay on top of other apps |
| `FOREGROUND_SERVICE` / `FOREGROUND_SERVICE_SPECIAL_USE` | Keep the overlay alive while the device is charging |
| `BIND_ACCESSIBILITY_SERVICE` | Declare the AccessibilityService (user must enable it manually) |
| `QUERY_ALL_PACKAGES` | List launcher apps so the user can pick a target |

The app **never** tries to enable Accessibility or Overlay permissions silently.  
The user must grant them explicitly.

## Project structure

```
ChargeController/
├── settings.gradle
├── build.gradle
├── gradle.properties
├── README.md
└── app/
    ├── build.gradle
    ├── proguard-rules.pro
    └── src/main/
        ├── AndroidManifest.xml
        ├── java/com/armancraft/chargecontroller/
        │   ├── MainActivity.java
        │   ├── OverlayService.java
        │   ├── MyAccessibilityService.java
        │   └── PowerReceiver.java
        └── res/
            ├── layout/activity_main.xml
            ├── values/
            │   ├── strings.xml
            │   ├── styles.xml
            │   └── colors.xml
            ├── xml/accessibility_service_config.xml
            ├── drawable/ic_launcher_foreground.xml
            └── mipmap-anydpi-v26/
                ├── ic_launcher.xml
                └── ic_launcher_round.xml
```

## Building the APK

### Requirements
- Android Studio Ladybug / Hedgehog or newer, **or**
- Command-line: JDK 17 +, Android SDK with build-tools and platform 35

### With Android Studio
1. Open the `ChargeController` folder as a project.
2. Let Gradle sync.
3. Build → Build Bundle(s) / APK(s) → Build APK(s).
4. The APK will be at  
   `app/build/outputs/apk/debug/app-debug.apk`  
   (rename to `ChargeController.apk` if desired).

### Command line (Gradle wrapper)
If the wrapper is missing, generate it once from Android Studio or run:

```bash
cd ChargeController
# (optional) generate wrapper if not present
gradle wrapper --gradle-version 8.9

./gradlew assembleDebug
```

APK location:
```
app/build/outputs/apk/debug/app-debug.apk
```

Rename it:
```bash
cp app/build/outputs/apk/debug/app-debug.apk ChargeController.apk
```

## Installing on the phone

1. Enable **Developer options** → **USB debugging**.
2. Connect the device (or copy the APK to the phone).
3. Install:

```bash
adb install -r ChargeController.apk
```

Or open the APK file on the device and install it (allow “Install from unknown sources” if prompted).

## Granting Overlay permission (SYSTEM_ALERT_WINDOW)

1. Open **Charge Controller**.
2. Tap **“Grant Overlay Permission”**.
3. On the system screen, enable **“Allow display over other apps”** for Charge Controller.
4. Return to the app.

On Android 11 the exact wording may vary; the official `ACTION_MANAGE_OVERLAY_PERMISSION` intent is used.

## Enabling Accessibility Service

1. Open **Charge Controller**.
2. Tap **“Open Accessibility Settings”**.
3. Find **Charge Controller** (or “Charge Controller Accessibility Service”).
4. Turn it **ON**.
5. Confirm the system dialog.

The service is **never** enabled automatically.  
Without it the overlay still appears, but key-injection attempts will simply report that the feature is not supported.

## Testing charge connect / disconnect

1. Make sure Overlay permission is granted.
2. (Optional) Enable the Accessibility Service.
3. Plug in a USB / charger cable.
   - The overlay (W / A / S / D panel) should appear at the bottom-left.
   - Status text in the main activity shows “🔌 Cable connected”.
4. Unplug the cable.
   - The overlay disappears.
   - Status text shows “🔌 Cable disconnected”.
5. You can also start/stop the overlay manually with the buttons in the main activity.
6. Drag the overlay panel to reposition it.
7. Tap the ☰ menu to open the main activity / app list.
8. Select a target app (e.g. GameHub) from the list; it will be launched.

## Notes for Redmi / MIUI

- MIUI may place Overlay permission under **Settings → Apps → Manage apps → Charge Controller → Other permissions → Display pop-up windows**.
- Accessibility services sometimes appear under **Settings → Additional settings → Accessibility**.
- Battery optimization can kill the foreground service; consider disabling optimization for Charge Controller.

## License

Provided as-is for educational / personal use.  
No warranty regarding key injection into third-party applications.
