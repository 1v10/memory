# memory for Android

The Android companion to [memhogs](https://github.com/cicerothoma/memhogs):
open it and see which apps are using your phone's memory, largest first,
with every helper process rolled up into the app that owns it. Chrome's
sandboxed renderers count toward Chrome, not toward twelve rows of
`sandboxed_process` noise.

<p>
  <img src="docs/screenshot-list.png" width="270" alt="memhogs showing apps ranked by memory with proportional bars" />
  <img src="docs/screenshot-expanded.png" width="270" alt="an expanded app after reclaiming 120.7 MiB from Google" />
  <img src="docs/screenshot-setup.png" width="270" alt="the terminal-style setup screen shown before Shizuku is running" />
</p>

## Why it needs Shizuku

Android does not let a normal app see other apps' memory. That is why
every "RAM monitor" on the Play Store shows one total and nothing else.
memhogs reads per-app memory the same way a USB debugger would, through
[Shizuku](https://shizuku.rikka.app/), which grants shell-level access
without root:

1. Install Shizuku from the Play Store, or from its
   [GitHub releases page](https://github.com/RikkaApps/Shizuku/releases/latest)
   if the Play Store reports it as incompatible with your device. That
   happens on very new Android versions before the listing catches up;
   the APK itself installs and works fine.
2. In Shizuku, start the service. On Android 11+ this takes one tap via
   wireless debugging; it survives until the next reboot.
3. Open memhogs and grant it access when prompted.

Without Shizuku, memhogs still shows the device-wide used/total bar from
public APIs, and explains how to unlock the rest.

## What the numbers mean

Memory is PSS (proportional set size), read from `dumpsys meminfo`. A
page shared by N processes counts 1/N toward each, so summing all apps
never counts the same physical memory twice. This is the same fair-share
metric the memhogs CLI uses on Linux, and the same one Android itself
uses to decide which app to kill under pressure.

- Blue numbers are memory, cyan names are installed apps, green names
  are system daemons that do not resolve to an installed package.
- An app using 15% or more of RAM gets its share flagged in red.
- Tap a row to expand its member processes with their PIDs.
- The filter field narrows the list by name. Data loads once on launch;
  pull down at the top of the list to refresh it manually.

## Reclaiming memory

Expanding an app offers a reclaim button. It runs `am kill` through the
Shizuku shell, which kills the app's background processes only: the
same reclaim Android itself performs under memory pressure, so nothing
in the foreground and no active service is touched. memhogs re-measures
afterward and reports exactly how much came back. Apps the system
considers busy may return "nothing to reclaim right now", which is the
safety working as intended. System daemons never offer the button.

## Install

Download `memory-1.0.0.apk` from this repository's **Releases** page once its
Release workflow has completed. `SHA256SUMS` accompanies the signed APK.
Debug builds are available in **Actions → Build → Artifacts → memory-debug**.

Requires Android 8.0 or newer and a running, authorized
[Shizuku](https://shizuku.rikka.app/) service for per-app memory data.
Release builds retain the existing memory signing key and can update the local
preview directly. See [RELEASING.md](RELEASING.md) for signing and publishing setup.

## Building

```sh
./gradlew assembleDebug   # APK in app/build/outputs/apk/debug/
./gradlew test            # unit tests
```

Needs JDK 17 or newer. See [CONTRIBUTING.md](CONTRIBUTING.md) for the full
check CI runs and the code standards, and [ARCHITECTURE.md](ARCHITECTURE.md)
for how the pieces fit.

Kotlin and Jetpack Compose, with the Shizuku API as the only non-AndroidX
dependency.

## Automated builds and releases

Pushes to `main`, pull requests and manual **Build** runs produce a debug APK
and check reports. To publish **memory 1.0.0**, first configure the single
`MEMORY_SIGNING` secret described in [RELEASING.md](RELEASING.md), then push the `v1.0.0` tag
or manually run **Release** for that existing tag. The workflow publishes a
signed, optimized APK and SHA-256 checksums after all checks pass.

## License

MIT

## Local customization

The Android app is named `memory` and uses a light theme with blue memory
accents. The header shows the device-wide usage percentage. Data loads once
on launch, then stays unchanged until you pull down at the top of the list.
Returning from the background does not refresh it, and there is no automatic
refresh timer.
