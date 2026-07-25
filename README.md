# Anodyne

A minimal, offline-first music player for Android — no permissions asked.

## Features

- **Offline** — Plays audio from folders you pick on device. No network access, no accounts, no tracking.
- **No permissions asked** — Uses the Storage Access Framework (SAF) so no `READ_EXTERNAL_STORAGE` or similar runtime permissions are needed.
- **Minimal** — Focused on local music playback. Clean UI, no ads, no bloat.
- **Customization** — Theme (Light / Dark / OLED), primary color, playlist cover shape (Square / Rounded / Circle / Squircle / Clover / Hexagon / Star / Slanted), grid/list view, immersive mode.

## Tech Stack

- **UI**: Jetpack Compose + Material 3
- **Player**: Media3 ExoPlayer
- **Image Loading**: Coil
- **Storage**: DataStore (preferences), Disk cache (audio metadata)
- **Language**: Kotlin + Coroutines

## Setup

1. Clone the repo.
2. Open in Android Studio.
3. Build and run on Android 7.0+ (API 24).
