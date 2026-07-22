# Runtime quickstart

Use the `vst-bridge-runtime-debug-apk` artifact from the **Windows plugin host
build** workflow. The plain Android artifact does not contain the Windows host.

1. Install the runtime APK on an ARM64 Android device.
2. Open **VST Bridge** and tap **Set up runtime**.
3. Wait while VST Bridge installs the bundled root filesystem and automatically
   creates its private Wine environment. No Winlator screen or manual container is used.
4. Tap **Done**. The Runtime card should report **ready**.
5. Optionally tap **Load audio** for a native dry preview.
6. Import an x86-64 Windows VST2 `.dll` and tap **Open editor**.
7. The timeline opens on the left and the Windows VST editor opens in the right panel; startup stages and host errors stay visible.

The app atomically refreshes and size-verifies `vst-bridge-host.exe`, then copies the selected DLL and host to
`C:\vstbridge` inside the container, starts Box64 and Wine, opens an X server
surface, and hosts the plug-in editor in a Win32 parent window.

Current boundaries:

- VST2 x86-64 DLL editor hosting is implemented.
- VST3 module scanning exists, but VST3 editor hosting is not implemented yet.
- Native dry audio preview is implemented. Real-time audio/MIDI processing through the Windows VST is not implemented yet. The runtime
  audio bridge can serve Wine, but the host currently provides editor lifecycle
  rather than a real-time processing graph.
- The APK intentionally targets Android API 28, matching Winlator's executable
  runtime compatibility model. It is a sideloaded experimental build, not a
  Play Store-ready package.
