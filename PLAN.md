# Implementation Plan: Align with Horologist Architecture

## Phase 1: Finish the Playback Path

The `MediaBrowser` is wired in DI but nothing pushes content to the player.

### 1.1 Wire media items from detail screens to player
- **Files**: `BookDetailScreen.kt`
- When user taps "Play", convert `Audiobook` tracks to `Media` list and pass via `PlayerRepository.setMediaList()`
- Mapper: `AudiobookMediaMapper` converts `Audiobook` + `DownloadManifest` → `List<Media>` (manifest-based) or `List<StoredTrack>` → `List<Media>` (downloaded)
- Book data passed between screens via `BookStore` (Hilt `@Singleton`)

### 1.2 Resume playback from last position
- **Files**: `BookDetailViewModel.kt`, `PlayerViewModel.kt`
- `BookDetailViewModel.playBook()` reads `PlaybackProgressEntity` from Room, passes `trackIndex` + `positionMs` to `playerRepository.setMediaList()`
- `DownloadManager.saveProgress()` writes `userMetadata.trackIndex` + `trackSeconds` → `PlaybackProgressEntity`

### 1.3 Add `setMedia*` call before play
- **Files**: `BookDetailViewModel.kt`
- `playBook()` calls `playerRepository.setMediaList()` then `playerRepository.play()`
- Both online (manifest-based) and downloaded (stored tracks) paths implemented

---

## Phase 2: Replace Custom Download System

The biggest architectural divergence. Replace raw OkHttp byte-copying with media3's built-in download framework.

### 2.1 Add media3 download dependencies
- **File**: `app/build.gradle.kts`
- Add: `media3-exoplayer-datasource-okhttp`, `media3-exoplayer-offline`
- Already have `media3-exoplayer-workmanager`

### 2.2 Create `MediaDownloadService`
- **New file**: `download/MediaDownloadServiceImpl.kt`
- Extend Horologist's `MediaDownloadService` (from `horologist-media-data`)
- Inject: `DownloadManager`, `WorkManagerScheduler`, `DownloadNotificationHelper`, `IntentBuilder`, `DownloadManagerListener`
- Register in `AndroidManifest.xml` with `dataSync` foreground service type

### 2.3 Create `DownloadModule`
- **New file**: `di/DownloadModule.kt`
- Provide: `OkHttpDataSource.Factory`, `StandaloneDatabaseProvider`, media3 `DownloadManager` with `SimpleCache`, `WorkManagerScheduler`, `DownloadManagerListener`, `DownloadProgressMonitor`
- `SimpleCache` in `cacheDir/media3cache` with `NoOpCacheEvictor`

### 2.4 Create cache-aware `MediaSourceFactory`
- **File**: `di/PlaybackServiceModule.kt`
- Replace plain `DefaultMediaSourceFactory(context)` with a `CacheDataSource.Factory` that reads through the `SimpleCache` when content is downloaded
- Toggle based on whether the media URI exists in the download index

### 2.5 Update `DownloadManager` integration
- **Files**: `download/DownloadManager.kt`, `download/AudiobookDownloadWorker.kt`
- Either: (a) keep WorkManager for triggering, have it enqueue a media3 `DownloadRequest`, or (b) replace WorkManager entirely with media3's `WorkManagerScheduler`
- Keep the foreground notification from `AudiobookDownloadWorker` or move to `MediaDownloadService`

### 2.6 Migrate download state tracking
- **Files**: `DownloadedBookEntity.kt`, `DownloadedBookDao.kt`
- Media3 tracks download state internally via its `DownloadIndex`
- Decide: keep our Room table for book metadata, or use media3's download metadata
- Recommendation: keep our Room table for rich audiobook data, sync download status from media3 index

### 2.7 Handle M4B and MP3 ZIP formats
- Media3 natively handles M4B (single file)
- For MP3 ZIPs: either (a) pre-extract and register extracted files with media3, or (b) write a custom `DataSource` that unwraps ZIP on-the-fly

---

## Phase 3: Move API Behind Hilt

The `LibroFmClient` singleton bypasses DI.

### 3.1 Create `NetworkModule`
- **New file**: `di/NetworkModule.kt`
- Provide: `Retrofit` instance, `LibroFmApi` interface, `OkHttpClient`
- Add Horologist's `NetworkAwareCallFactory` for bandwidth-aware networking
- Move Gson converter setup here

### 3.2 Remove `LibroFmClient` singleton
- **File**: `api/LibroFmClient.kt`
- Deleted. All call sites use Hilt-provided `LibroFmApi`
- `LoginViewModel`, `LibraryPagingSource` inject `LibroFmApi` directly

### 3.3 Add `NetworkAwareCallFactory` integration
- **File**: `di/NetworkModule.kt`
- `NetworkAwareCallFactory` wraps `Call.Factory` for Retrofit (`RequestType.ApiRequest`) and Coil (`RequestType.ImageRequest`)
- Provided via `AppModule` for image loading, `NetworkModule` for API

---

## Phase 4: Implement Catalog Sync

> Not planned. The Libro.fm API lacks a changelist/diff endpoint, and the current browse → tap → download flow covers the real use case on Wear OS.

---

## Phase 5: Upgrade Navigation

### 5.1 Migrate to `MediaPlayerScaffold`
- **File**: `navigation/NavGraph.kt`
- `MediaPlayerScaffold` is the navigation backbone
- Pager: page 0 = player, page 1 = downloaded books browse
- Extra routes in `additionalNavRoutes`: login, book detail, full library

### 5.2 Add type-safe navigation
- **File**: `navigation/NavGraph.kt`
- String routes used. Type-safe `@Serializable` navigation not implemented (low priority).

### 5.3 Add deep links
- **Files**: `AndroidManifest.xml`, navigation
- `deepLinkPrefix = "librofm"` configured. Full deep link support not implemented.

---

## Phase 6: Add Missing Infrastructure

### 6.1 Watch-face complications
- **New file**: `complication/MediaStatusComplicationService.kt`
- Not implemented.

### 6.2 Tiles
- **New file**: `tile/MediaCollectionsTileService.kt`
- Not implemented.

### 6.3 Proto DataStore settings
- **New file**: `settings/SettingsViewModel.kt`
- `SettingsViewModel` uses Preferences DataStore with `suppress_speaker` and `audio_offload` keys
- Display-only settings UI (no write toggles yet)

### 6.4 Audio offload manager
- **Files**: `offload/AudioOffloadManager.kt`, `offload/AudioOffloadListenerList.kt`, `offload/AudioOffloadStatus.kt`, `offload/OffloadTimes.kt`, `offload/AudioError.kt`
- Connected to player via `PlaybackServiceModule` when API ≥ 30
- `AudioOffloadListener` passed to `audioSink()`

### 6.5 Speaker suppression
- **Files**: `di/PlaybackServiceModule.kt`, `di/Annotations.kt`
- `@SuppressSpeakerPlayback` qualifier exists
- Wired to `ExoPlayer.Builder.setSuppressPlaybackOnUnsuitableOutput()`

### 6.6 SuspendingMediaLibrarySessionCallback
- **File**: `player/PlaybackService.kt`
- `SuspendingMediaLibrarySessionCallback` wired with `CoroutineScope` + `ErrorReporter`

---

## Phase 7: UI Improvements

### 7.1 Add settings screen
- **Files**: `settings/SettingsScreen.kt`, `settings/SettingsViewModel.kt`
- Display-only settings in `MediaPlayerScaffold` settings route
- Toggle controls not implemented yet

### 7.2 Add browsing screen for library
- `LibroBrowseScreen`: downloaded books on pager page 1
- `LibraryScreen`: full paginated API library on `library` route
- "Browse All Books" button navigates from downloaded list to full library

### 7.3 Add volume control screen
- Rotary volume via `volumeViewModel` on `PlayerScreen`
- Uses Horologist's stateful `PlayerScreen` with `volumeRotaryBehavior`

### 7.4 Add audio debug screen
- Not implemented.

---

## Execution Order

| Status | Phase | Notes |
|--------|-------|-------|
| ✅ Done | Phase 1: Finish playback path | MediaMapper, playAudiobook, resume from saved position |
| ✅ Done | Phase 3: Move API behind Hilt | NetworkModule, LibroFmClient deleted, NetworkAwareCallFactory |
| ✅ Done | Phase 5: Upgrade navigation | MediaPlayerScaffold, additional routes |
| ✅ Done | Phase 6.3-6.5: Settings, offload, suppression | SettingsViewModel, AudioOffloadManager, @SuppressSpeakerPlayback |
| ✅ Done | Phase 6.6: SuspendingMediaLibrarySessionCallback | CoroutineScope + ErrorReporter |
| ✅ Done | Phase 7.1-7.3: Settings + browse + volume UI | SettingsScreen, LibroBrowseScreen, LibraryScreen, rotary volume |
| ❌ Not planned | Phase 4: Catalog sync | API lacks changelist endpoint |
| ❌ Not started | Phase 2: Replace download system | High risk — touches core download/playback path |
| ❌ Not started | Phase 6.1-6.2: Complications + tiles | Additive features |
| ❌ Not started | Phase 7.4: Audio debug screen | Additive feature |
