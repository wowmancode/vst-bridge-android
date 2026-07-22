# Wine/Box64 integration

The official Winlator source was inspected at commit
`e113da42beefc39c69c8944b27c19c3703bfa856`. Its display activity is deliberately
not exported, so VST Bridge cannot safely launch a plug-in inside another installed
Winlator app through an Android intent. A companion-app shortcut would depend on
private implementation details and would not provide reliable audio or lifecycle
control.

The runtime is integrated in-process, with the relevant LGPL
components and notices kept clearly separated from the Apache-licensed application
code.

## Components to adapt

1. Root filesystem installer and Wine-prefix management
2. Box64 guest-program launcher and environment construction
3. X server surface and Win32 input forwarding
4. SysV shared memory support
5. ALSA or PulseAudio bridge
6. Process supervision and bounded logs

The VST2 editor launch is equivalent to:

```text
wine explorer /desktop=vstbridge,1280x720 \
  C:\\vstbridge\\vst-bridge-host.exe editor <plugin-id> <plugin-path>
```

VST Bridge owns a tagged private Wine environment, creates it automatically, and starts the adapted X/Wine execution surface directly with this command. The Winlator main activity and container UI are not registered or launched. VST3 editor hosting and a real-time Android DAW transport remain future work.

## Packaging boundary

The existing `vst-bridge-runtime-debug-apk` already embeds the x86-64 Windows host.
Wine, Box64, the root filesystem, X server, audio libraries, and their pinned source are included. Optional gaming-focused DXVK/VKD3D and native Windows component archives are excluded in the Android build. The Windows host remains a downstream CI artifact so the runtime APK always contains a freshly tested host.

## Licensing

Winlator is LGPL-2.1. Any adapted source, build scripts, notices, and corresponding
source offer must ship with the runtime distribution. Wine, Box64, Mesa, and audio
components retain their respective upstream licenses. The imported Winlator license and exact source revision are recorded under `third_party/winlator`.
