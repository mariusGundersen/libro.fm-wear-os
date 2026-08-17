# Libro.fm Wear OS Audiobook Player - Implementation Plan

## Architecture Overview

**Stack**: Kotlin + Jetpack Compose for Wear OS + Compose Material3 + Media3 (ExoPlayer) + Retrofit/OkHttp + Room for local storage

**Project structure**: Single-module Android project targeting Wear OS 3.0+ (min SDK 30, target SDK 34)

---

## Phase 1: Project Setup & API Layer

### 1.1 Gradle project setup
- Android project with Wear OS application plugin
- Dependencies: Compose for Wear OS, Material3, Navigation Compose, Media3, Room, Retrofit/OkHttp, Coil (cover images)
- `wearApp()` module config in `build.gradle.kts`

### 1.2 Libro.fm API client (port from `jedwards1230/libro-client` TypeScript source)
- `POST https://libro.fm/oauth/token` — grant_type=password, returns `access_token`
- `GET https://libro.fm/api/v7/library?page=N` — paginated audiobook list
- `GET https://libro.fm/api/v9/download-manifest?isbn=X` — returns download URLs (MP3 parts or M4B)
- Required headers: `User-Agent: okhttp/5.3.2`, `X-LibroFm-AppVer: 7.34.8`
- Token stored securely in `EncryptedSharedPreferences`

### 1.3 Data models
- `Audiobook` — isbn, title, authors, cover_url, duration, track_count, user_metadata (progress)
- `DownloadManifest` — parts (urls), tracks, expires_at
- `TokenMetadata` — access_token, created_at
- Room entities: `DownloadedBook`, `PlaybackProgress`

---

## Phase 2: Authentication & Login

### 2.1 Login screen
- Email input field (Wear OS text input via speech or rotary)
- Password input field
- "Log in" button
- Store token in `EncryptedSharedPreferences` after successful login
- Auto-login on app start if token exists

### 2.2 Token management
- Store `access_token` + `created_at`
- Simple token expiry check (no refresh token in the API — re-login when expired)

---

## Phase 3: Library Screen

### 3.1 Library list
- `ScalingLazyColumn` showing user's purchased audiobooks
- Each item: cover thumbnail (Coil), title, author, duration
- Cover art loaded from `cover_url`
- Tap to open book detail/download screen
- Pull-to-refresh or swipe refresh

### 3.2 Book detail screen
- Cover art, title, author, narrator, duration, description
- Download button (if not downloaded)
- Play button (if downloaded)
- Delete button (to free space)

---

## Phase 4: Download System

### 4.1 Download manager
- Background service using `ForegroundService` (visible notification for downloads)
- Fetch download manifest by ISBN
- If M4B URL available → download single M4B file
- If M4B unavailable → download all MP3 parts and store as playlist
- Save files to app's internal storage (`getFilesDir()`)
- Track download progress (update Room DB + notification)
- Check if already downloaded before re-downloading

### 4.2 Storage management
- Show downloaded books with size
- Delete functionality to free space
- Handle partial downloads (resume on re-download)

---

## Phase 5: Audiobook Player

### 5.1 Player screen (Compose for Wear OS)
- Cover art background (blurred or dimmed)
- Title + author display
- Seek bar / progress indicator (curved or linear)
- Current time / remaining time
- Controls: play/pause, skip back 30s, skip forward 30s
- Playback speed toggle (0.75x, 1x, 1.25x, 1.5x, 2x)
- Chapter indicator (if M4B with chapters, or track number for MP3)

### 5.2 Media3/ExoPlayer integration
- `MediaSessionService` for background playback
- Media notification with controls (play/pause, skip, seek)
- Handle headphone button controls
- Save playback position to Room DB on pause/stop
- Resume from last position on book open
- Handle audio focus (pause on other audio, etc.)

### 5.3 Chapter/track navigation
- For M4B: chapter list from manifest tracks metadata
- For MP3: track list with names from manifest
- Swipe left/right or tap to navigate chapters

---

## Phase 6: Navigation & UI

### 6.1 Navigation graph
```
Login → Library → BookDetail → Player
                 ↘ DownloadManager
```
- SwipeDismissableNavHost for Wear OS back navigation
- Remember scroll position in library list

### 6.2 Wear OS optimizations
- TimeText at top of screens
- Vignette effect for edge screens
- Rotary input support for scrolling
- Curved text where appropriate
- Large touch targets for controls
- Always-on display mode option while playing

---

## Phase 7: Polish & Edge Cases

- Handle network errors gracefully (retry on library fetch, download failures)
- Low storage warnings before download
- Battery optimization awareness (foreground service)
- Cover art caching with Coil
- Offline mode: library screen shows only downloaded books when no network
- Handle token expiry (redirect to login)

---

## File Structure

```
app/src/main/java/fm/libro/wearos/
├── MainActivity.kt
├── LibroFmApp.kt
├── api/
│   ├── LibroFmApi.kt          (Retrofit interface)
│   ├── LibroFmClient.kt       (API wrapper)
│   └── models/                 (data classes)
├── auth/
│   ├── AuthManager.kt          (EncryptedSharedPrefs token storage)
│   └── LoginScreen.kt
├── library/
│   ├── LibraryViewModel.kt
│   └── LibraryScreen.kt
├── download/
│   ├── DownloadManager.kt      (ForegroundService)
│   ├── DownloadService.kt
│   └── DownloadProgress.kt
├── player/
│   ├── PlayerViewModel.kt
│   ├── PlayerScreen.kt
│   ├── PlaybackService.kt      (MediaSessionService)
│   └── PlaybackNotification.kt
├── data/
│   ├── AppDatabase.kt          (Room)
│   ├── DownloadedBookDao.kt
│   └── PlaybackProgressDao.kt
├── navigation/
│   └── NavGraph.kt
└── ui/theme/
    └── Theme.kt
```

---

## Key Risks & Mitigations

1. **API is undocumented/reverse-engineered** — The `X-LibroFm-AppVer` header may rotate. Mitigation: make it configurable, follow the burntcookie90 reference implementation
2. **Download URLs expire** — `expires_at` in manifest. Mitigation: download immediately after fetching manifest
3. **Wear OS storage is limited** — audiobooks are large (300MB-1GB). Mitigation: show book sizes, warn before download, allow deletion
4. **On-watch typing is painful** — Mitigation: use voice input for email field, rotary encoder for scrolling through keyboard, and keep the input flow minimal
