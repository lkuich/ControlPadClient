# ControlPad Client

ControlPad Client is the Android half of an old phone-as-controller experiment. It turns an Android device into a touch controller that can drive a companion Windows app, which then exposes the input to the PC as either a virtual Xbox 360 controller or normal mouse/keyboard events.

The client is written in Kotlin/Android and talks to the desktop server over the local Wi-Fi network using UDP discovery plus gRPC streams.

## What It Does

- Provides a landscape touch controller with Xbox-style buttons, bumpers, triggers, thumbsticks, and a D-pad.
- Supports two input modes:
  - **Xbox 360**: sends gamepad button, trigger, and thumbstick events to the Windows server.
  - **Mouse & Keyboard**: sends keyboard and mouse events to the Windows server.
- Finds the desktop server automatically on the LAN, with a manual IP fallback.
- Lets the user customize the on-screen control layout and key mappings.
- Stores layouts, selected profile, and the last server IP in Firebase Realtime Database after Google sign-in.

## How It Works

1. The Windows server broadcasts its LAN IP address over UDP port `58385`.
2. `HomeActivity` listens for that broadcast, acknowledges it, and enables the connect button.
3. When the user connects, the app opens a plaintext gRPC channel to the server on port `50051`.
4. Controller UI events are streamed to the matching gRPC service:
   - `XboxButtons` for Xbox button, trigger, and thumbstick input.
   - `StandardInput` for keyboard and mouse input.
5. The Windows server converts those messages into either ViGEm Xbox 360 reports or native Windows input.

The shared service contract lives in [`app/src/main/proto/services.proto`](app/src/main/proto/services.proto). The generated Java/Kotlin gRPC bindings are produced by the protobuf Gradle plugin.

## Main Screens

- [`LoginActivity`](app/src/main/java/lkuich/xdroid/LoginActivity.kt): Google/Firebase sign-in and layout loading.
- [`HomeActivity`](app/src/main/java/lkuich/xdroid/HomeActivity.kt): server discovery, manual connection, and input-mode selection.
- [`XboxActivity`](app/src/main/java/lkuich/xdroid/XboxActivity.kt): Xbox 360 controller surface.
- [`StandardInputActivity`](app/src/main/java/lkuich/xdroid/StandardInputActivity.kt): mouse and keyboard control surface.
- [`CustomizeLayoutActivity`](app/src/main/java/lkuich/xdroid/CustomizeLayoutActivity.kt): draggable controls and key remapping.

## Requirements

- Android Studio with Android Gradle Plugin `4.0.1` era support.
- Android SDK 29 / Build Tools `30.0.2`.
- JDK compatible with older Android Gradle tooling.
- A Firebase project configured for:
  - Google Sign-In
  - Firebase Auth
  - Firebase Realtime Database
- The companion Windows server running on the same Wi-Fi network.

This project includes an old `google-services.json`, but Firebase projects and OAuth clients can expire or be removed. If you revive the app, expect to create a new Firebase project and replace that file plus the `web_client_id` string.

## Building

From this folder:

```sh
./gradlew assembleDebug
```

The release APK task also exists through the standard Android Gradle plugin, but the checked-in keystore and release artifact are historical and should not be treated as production signing material.

## Running

1. Start the Windows companion app from the `ControlPadServer` project.
2. Put the Android device and PC on the same Wi-Fi network.
3. Launch the Android app and sign in.
4. Choose **Xbox 360** or **Mouse & Keyboard**.
5. Tap the connect button once the server IP appears, or use the manual IP dialog if discovery fails.

## Input Notes

- Xbox mode maps touch controls to Xbox 360 button flags, trigger pressure values, and thumbstick axes.
- Standard input mode maps buttons to Windows virtual key codes and mouse buttons.
- The left stick in standard mode is treated like `WASD` movement.
- The right stick in standard mode moves the mouse.
- Long-pressing controls in the layout editor allows repositioning; tapping supported controls opens the key-mapping dialog.

## Project Status

This is an old experimental app. It uses legacy Android APIs and dependencies such as `jcenter()`, Firebase SDK versions from 2020, Kotlin Android Extensions, and plaintext gRPC. It is useful as a working prototype/reference, but would need dependency updates and security review before being published again.

## Related Project

The companion desktop server is expected to live beside this project as `../ControlPadServer`.
