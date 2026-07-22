# Windows plug-in host

`vst-bridge-host.exe` is the x86-64 Windows payload that will run under Wine and
Box64 on ARM64 Android. It scans VST3 modules through Steinberg’s official host
API and VST2 `.dll` files through a pinned BSD-licensed clean-room ABI.

```text
vst-bridge-host scan <plugin-id> <plugin-path>
```

The scanner validates the DLL entry point, effect signature, metadata, channel
counts, parameters, and editor capability. Stable nonzero exit codes cover bad
commands, load failures, invalid effects, empty factories, and missing audio-effect
classes. Each scan will run in a separate Wine process to isolate plug-in crashes.

Neither SDK is vendored. GitHub Actions checks out pinned revisions and publishes
the resulting Windows executable as a workflow artifact.
