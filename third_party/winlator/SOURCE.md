# Winlator source provenance

- Upstream: https://github.com/brunodev85/winlator-app
- Commit: `e113da42beefc39c69c8944b27c19c3703bfa856`
- License: LGPL-2.1 (see `LICENSE` in this directory)
- Imported paths: Android Java sources, resources, native sources/libraries, and
  runtime assets from upstream `app/src/main`.

The imported code is kept in its upstream `com.winlator` namespace. Small
integration changes adapt its file-provider authority, storage path, and modern
Android permission gate for the `dev.vstbridge.android` application ID.

The corresponding source is this repository at the same revision as any APK
artifact. Generated Windows host binaries are added only by GitHub Actions.
