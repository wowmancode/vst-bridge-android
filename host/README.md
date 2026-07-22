# Windows VST3 host

`vst-bridge-host.exe` is the x86-64 Windows payload that will run under Wine and
Box64 on ARM64 Android. Its first implemented command loads a VST3 module using
Steinberg's official hosting API and emits newline-delimited protocol v1 messages.

```text
vst-bridge-host scan <plugin-id> <plugin-path>
```

The scanner returns stable nonzero exit codes for command-line errors, load
failures, empty factories, and modules with no audio-effect class. A separate Wine
process will be used for each scan so third-party plug-in crashes are isolated.

The VST3 SDK is not vendored. GitHub Actions checks out the pinned MIT-licensed
`v3.8.0_build_66` release recursively and publishes the resulting Windows
executable as a workflow artifact.
