# VST Bridge for Android

VST Bridge is an experimental Android host for **Windows VST2/VST3 plug-ins on
64-bit ARM phones**. The intended runtime is Wine plus Box64, with an Android
audio/MIDI layer and the plug-in's native Windows editor rendered through an
Android-compatible X server.

> [!IMPORTANT]
> Version 0.1 is the Android control-plane prototype. It imports and manages
> `.dll` and `.vst3` files and defines the runtime boundary, but it does **not**
> bundle Wine, Box64, or execute plug-ins yet. The UI says this explicitly.

## Current prototype

- Imports individual Windows `.dll` and `.vst3` files through Android's system
  file picker; no broad storage permission is requested.
- Keeps a private plug-in library and lets the user remove entries.
- Detects whether the device exposes the `arm64-v8a` ABI.
- Validates the Windows PE header and labels x86-64, 32-bit x86, and ARM64 binaries.
- Defines a versioned JSON launch/control protocol for the Wine-side host.
- Disables **Open editor** until a runtime provider reports that it is ready.
- Builds a debug APK entirely on GitHub Actions.

## Build on GitHub (phone-friendly)

1. Create a new empty GitHub repository.
2. Push this project to its `main` branch.
3. Open the repository's **Actions** tab and select **Android build**, or simply
   wait for the push-triggered run.
4. Open the completed run and download `vst-bridge-debug-apk` under Artifacts.
5. Unzip it and install `app-debug.apk` on the phone.

No Android SDK, NDK, or Gradle installation is needed on the phone.

## Runtime architecture

```text
Android UI / plug-in library
             |
       RuntimeBridge
             |
  native runtime service (planned)
             |
  Box64 -> Wine -> Windows VST host
             |                |
    Android audio/MIDI     X11 editor surface
```

The Windows side needs a small host executable. It will load the VST, negotiate
sample rate/block size, process audio, and expose parameter/MIDI messages over a
low-overhead IPC channel. Wine provides Win32 compatibility; Box64 translates
x86-64 code on ARM64. Neither component itself implements VST hosting.

See [`docs/ROADMAP.md`](docs/ROADMAP.md) for the integration plan and constraints.

## Local build (optional)

With Android SDK 35, Java 17, and Gradle 8.10.2 installed:

```sh
gradle lintDebug assembleDebug
```

The APK is written to `app/build/outputs/apk/debug/app-debug.apk`.

## Legal and compatibility notes

- Users must supply plug-ins they are licensed to use.
- Runtime binaries and their corresponding notices/source obligations must be
  handled before distribution; this repository currently bundles none.
- VST3 hosting and redistribution must follow Steinberg's current SDK license.
- VST2 SDK files must not be copied into this repository.
- Copy protection, installers, 32-bit-only plug-ins, unusual GPU APIs, and very
  small real-time buffer sizes may not work initially.

## License

Apache-2.0 for the original code in this repository. Third-party runtime
components will retain their own licenses.
