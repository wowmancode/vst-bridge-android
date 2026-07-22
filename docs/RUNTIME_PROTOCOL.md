# Runtime protocol v1

The Android control plane and the Wine-side host communicate through versioned
newline-delimited JSON control messages. Audio samples will not travel through
JSON; milestone 4 will use fixed-size shared-memory ring buffers for that path.

## Android to host

```json
{"protocolVersion":1,"command":"launch","pluginId":"uuid","pluginPath":"/private/path/plugin.vst3","sampleRate":48000,"blockSize":256,"openEditor":true}
```

Before emitting this request, Android verifies that the imported file has a valid
Portable Executable header and an x86-64 machine type. The runtime provider is
responsible for mapping the Android-private path into the Wine prefix and replacing
it with the corresponding `Z:\\...` path before invoking the Windows host.

## Host to Android

All responses include `protocolVersion`, `pluginId`, and a monotonically increasing
`sequence` number. Initial message types are:

- `state`: `starting`, `scanning`, `ready`, `stopped`, or `crashed`
- `metadata`: plug-in name, vendor, version, class ID, buses, and parameter count
- `editor`: logical width, height, scale, and visibility
- `metrics`: processing load, xruns, sample rate, and block size
- `error`: stable machine-readable code plus a human-readable message

Unknown fields must be ignored. Unknown protocol versions must be rejected with an
`UNSUPPORTED_PROTOCOL` error so incompatible Android and host builds fail clearly.

## Process isolation

Scanning and hosting occur in disposable Wine child processes. A malformed or
crashing plug-in must not take down the Android UI or corrupt the library index.
The runtime service owns process lifetime and records the final exit code and the
last bounded log output for troubleshooting.
