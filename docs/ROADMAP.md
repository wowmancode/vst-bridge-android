# Runtime roadmap

## Why this is more than “run the DLL with Wine”

A VST plug-in is a dynamic library, not an application. Wine needs a Windows
host executable to load it, call the VST ABI, create its editor window, and feed
it real-time audio and MIDI. Box64 translates the x86-64 host and plug-in code
when the Android device is ARM64. An X11/graphics bridge then presents the Wine
window on Android.

## Milestone 1 — Android control plane (implemented)

- Android app shell and accessible file import
- private plug-in library
- ARM64 capability check
- `RuntimeBridge` contract
- GitHub Actions APK artifact

## Milestone 2 — headless VST validation

- Build a minimal x86-64 Windows host executable in CI (scanner implemented)
- Start a Wine/Box64 container from an Android foreground service
- Copy or bind the selected plug-in into the container
- Validate Portable Executable machine type before starting the runtime (implemented)
- Scan plug-in metadata in a crash-isolated child process
- Return name, vendor, buses, parameters, and architecture to Android

Success criterion: a known free x86-64 VST3 is detected on an ARM64 phone without
opening its GUI.

## Milestone 3 — editor rendering

- Embed or adapt an Android X server surface
- Launch the Wine editor at a phone-appropriate virtual desktop size
- Forward touch, mouse, keyboard, clipboard, and window-resize events
- Add per-plug-in DPI and renderer settings

Success criterion: the test plug-in's native editor is interactive and survives
screen rotation/backgrounding.

## Milestone 4 — real-time audio and MIDI

- Add a native low-latency Android audio engine (AAudio/Oboe)
- Use fixed-size shared-memory ring buffers between Android and the Windows host
- Keep UI, disk I/O, IPC control, and allocation off the audio callback
- Route Android MIDI input and expose an on-screen keyboard
- Report xruns, effective latency, CPU load, sample rate, and block size

Success criterion: stable stereo processing for 10 minutes at a realistic phone
buffer size, with no audible discontinuities under normal UI interaction.

## Milestone 5 — distributable runtime

- Pin and reproducibly build Wine, Box64, the root filesystem, graphics/audio
  bridges, and the Windows host
- Publish source/version manifests and all required license notices
- Split large runtime payloads from the small Android application when practical
- Add signed release builds after a secure signing-key workflow is chosen

## Initial scope

The first runtime should target ARM64 Android devices and x86-64 Windows VST3
effects. VST2, instruments, 32-bit plug-ins, multi-output routing, automation,
and protected commercial products should be added only after that path is stable.
