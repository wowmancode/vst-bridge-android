# VST Bridge for Android

VST Bridge is an experimental Android host for **Windows VST2/VST3 plug-ins on
64-bit ARM phones**. The intended runtime is Wine plus Box64, with an Android
audio/MIDI layer and the plug-in's native Windows editor rendered through an
Android-compatible X server.

> [!IMPORTANT]
> Version 0.2 integrates the pinned Winlator runtime, including Wine, Box64, the
> X server, and audio runtime. It can launch x86-64 VST2 `.dll` editors. VST3
> scanning is present, while VST3 editor hosting and DAW audio/MIDI transport remain work in progress.

## Current prototype

- Imports `.dll` and `.vst3` files through Android’s system picker even when
  storage reports an unknown MIME type; no broad storage permission is requested.
- Keeps a private plug-in library and lets the user remove entries.
- Detects whether the device exposes the `arm64-v8a` ABI.
- Validates the Windows PE header and labels x86-64, 32-bit x86, and ARM64 binaries.
- Maps imported plug-ins into a private Wine container and launches the Windows host.
- Cloud-builds an x86-64 Windows host that scans VST3 modules and VST2 `.dll` files.
- Produces a separate runtime APK with the tested Windows host embedded.
- Installs the bundled rootfs, creates Winlator containers, and renders VST2 editors through X11.
- Builds a debug APK entirely on GitHub Actions.

## Build on GitHub (phone-friendly)

1. Create a new empty GitHub repository.
2. Push this project to its `main` branch.
3. Open the repository's **Actions** tab and select **Windows plugin host build**, or simply
   wait for the push-triggered run.
4. Open the completed run and download `vst-bridge-runtime-debug-apk` under Artifacts.
5. Unzip it and install `app-debug.apk` on the phone.

No Android SDK, NDK, or Gradle installation is needed on the phone.

## Runtime architecture

```text
Android UI / plug-in library
             |
       RuntimeBridge
             |
  integrated Winlator runtime
             |
  Box64 -> Wine -> Windows VST host
             |                |
    Android audio/MIDI     X11 editor surface
```

The Windows host currently loads VST2 DLLs and owns their native editor window. A later processing service will negotiate sample rate/block size and expose audio, parameter, and MIDI messages over a low-overhead IPC channel. Wine provides Win32 compatibility; Box64 translates
x86-64 code on ARM64. Neither component itself implements VST hosting.

See [`docs/RUNTIME_QUICKSTART.md`](docs/RUNTIME_QUICKSTART.md) for device setup and current limitations.
The Wine/Box64 embedding boundary is documented in [`docs/WINLATOR_INTEGRATION.md`](docs/WINLATOR_INTEGRATION.md).

## Local build (optional)

With Android SDK 35, NDK 24.0.8215888, CMake 3.22.1, Java 17, and Gradle 8.10.2 installed:

```sh
gradle lintDebug assembleDebug
```

The APK is written to `app/build/outputs/apk/debug/app-debug.apk`.

## Legal and compatibility notes

- Users must supply plug-ins they are licensed to use.
- The runtime is imported from pinned Winlator source under LGPL-2.1; provenance and
  corresponding source are included under `third_party/winlator`.
- VST3 hosting and redistribution must follow Steinberg's current SDK license.
- VST2 SDK files must not be copied into this repository.
- Copy protection, installers, 32-bit-only plug-ins, unusual GPU APIs, and very
  small real-time buffer sizes may not work initially.

## License

Apache-2.0 for the original code in this repository. Third-party runtime
components will retain their own licenses.
