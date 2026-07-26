<img width="1080" height="2400" alt="Settings" src="https://github.com/user-attachments/assets/73c303f6-9707-4986-adb8-24a92af55a2b" />
<img width="1080" height="2400" alt="Playlist" src="https://github.com/user-attachments/assets/47b6d973-0b82-495c-9f04-9452501cbee5" />
<img width="1080" height="2400" alt="Home" src="https://github.com/user-attachments/assets/f1a71996-46e2-4ef5-95dd-8bf13c35bb16" />
<img width="1080" height="2400" alt="Currently_playing" src="https://github.com/user-attachments/assets/4487cf91-e1d8-42b1-b346-9cb0f3c2242f" />
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
