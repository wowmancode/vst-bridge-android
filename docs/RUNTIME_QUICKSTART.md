# Runtime quickstart

Use the `vst-bridge-runtime-debug-apk` artifact from the **Windows plugin host
build** workflow. The plain Android artifact does not contain the Windows host.

1. Install the runtime APK on an ARM64 Android device.
2. Open **VST Bridge** and tap **Set up runtime**.
3. Wait for the bundled root filesystem to finish installing.
4. In the container screen, tap **+**, keep the defaults, and create one
   container. VST Bridge currently uses the first container.
5. Return to VST Bridge. The Runtime card should report **ready**.
6. Import an x86-64 Windows VST2 `.dll` and tap **Open editor**.

The app copies the selected DLL and `vst-bridge-host.exe` to
`C:\vstbridge` inside the container, starts Box64 and Wine, opens an X server
surface, and hosts the plug-in editor in a Win32 parent window.

Current boundaries:

- VST2 x86-64 DLL editor hosting is implemented.
- VST3 module scanning exists, but VST3 editor hosting is not implemented yet.
- Audio/MIDI transport into an Android DAW is not implemented yet. The runtime
  audio bridge can serve Wine, but the host currently provides editor lifecycle
  rather than a real-time processing graph.
- The APK intentionally targets Android API 28, matching Winlator's executable
  runtime compatibility model. It is a sideloaded experimental build, not a
  Play Store-ready package.
