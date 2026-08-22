# Implementation Plan: Align with Horologist Architecture

## Phase 1: Finish the Playback Path

The `MediaBrowser` is wired in DI but nothing pushes content to the player.

### 1.1 Wire media items from detail screens to player
- **Files**: `BookDetailScreen.kt`, `DownloadedBookDetailScreen.kt`
- When user taps "Play", convert `Audiobook` tracks to `MediaItem` list and pass via `PlayerRepository.setMediaItems()`
- Need a mapper: `Audiobook` + `DownloadManifest` → `List<MediaItem>`
- Store the `Audiobook` in a place accessible to `PlayerViewModel` (e.g. static cache or DI-scoped)

### 1.2 Resume playback from last position
- **Files**: `PlayerViewModel.kt`
- On player start, seek to saved `PlaybackProgressEntity.positionMs`
- Observe `PlaybackProgressEntity` changes and persist periodically

### 1.3 Add `setMedia*` call before play
- **Files**: `LibroPlayerViewModel.kt` or detail screen composables
- Before `playerUiController.play()`, ensure media items are set on the underlying player

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
- Delete or refactor to a thin wrapper around the Hilt-provided `LibroFmApi`
- Update all call sites: `LoginViewModel`, `LibraryPagingSource`, `AudiobookDownloadWorker`

### 3.3 Add `NetworkAwareCallFactory` integration
- **File**: `di/NetworkModule.kt`
- Add `horologist-network-awareness-okhttp` dependency
- Wrap `OkHttpClient` with `NetworkAwareCallFactory` tagging requests as `ApiRequest` or `StreamRequest`
- Already have `horologist-network-awareness-okhttp` and `horologist-network-awareness-ui` in dependencies

---

## Phase 4: Implement Catalog Sync

Fill the empty `SyncModule` stub.

### 4.1 Create `PlaylistRepositorySyncable`
- **New file**: `sync/PlaylistRepositorySyncable.kt`
- `syncWith()`: diff remote catalog vs local Room `DownloadedBookEntity` table
- Fetch library page from API, compare with local, apply changes
- Implement `ChangeListVersionRepository` with a real version counter

### 4.2 Create `NetworkChangeListService`
- **New file**: `api/NetworkChangeListService.kt`
- Since the Libro.fm API lacks a changelist endpoint, fabricate one by:
  - Fetching full catalog
  - Diffing against last-known state
  - Returning added/removed ISBNs

### 4.3 Wire into `SyncModule`
- **File**: `sync/SyncModule.kt`
- Replace `emptyArray<Syncable>()` with the real `PlaylistRepositorySyncable`
- Configure notification channel for sync progress
- Provide real `ChangeListVersionRepository` backed by DataStore

### 4.4 Trigger sync on app launch and periodically
- **File**: `LibroFmApp.kt` or `MainActivity.kt`
- Call `Sync.initialize()` (already called in `LibroFmApp.kt:18`)
- Configure sync interval and constraints (WiFi only, charging, etc.)

---

## Phase 5: Upgrade Navigation

### 5.1 Migrate to `MediaPlayerScaffold`
- **File**: `navigation/NavGraph.kt`
- Replace `SwipeDismissableNavHost` with Horologist's `MediaPlayerScaffold`
- Get free player, browse, volume, settings routes
- Keep custom routes for login, book detail, downloaded book detail

### 5.2 Add type-safe navigation
- **File**: `navigation/NavGraph.kt`
- Convert string routes to `@Serializable` data objects/classes
- Use Horologist's `composable<T>` helper for type-safe destination registration

### 5.3 Add deep links
- **Files**: `AndroidManifest.xml`, navigation
- Create `NavDeepLinkIntentBuilder` for `{prefix}/player?isbn=X` deep links
- Support auto-play from tiles/complications via intent extras

---

## Phase 6: Add Missing Infrastructure

### 6.1 Watch-face complications
- **New file**: `complication/MediaStatusComplicationService.kt`
- Create `DataUpdates` listener attached to the player
- Provide current track title/artwork to complication providers
- Register in manifest with `BIND_COMPLICATION_PROVIDER` permission

### 6.2 Tiles
- **New file**: `tile/MediaCollectionsTileService.kt`
- Render recently played / downloaded books as tile shortcuts
- Deep-link into playback with extras

### 6.3 Proto DataStore settings
- **New file**: `data/settings/SettingsSerializer.kt`
- Migrate `AuthManager` (DataStore Preferences) to include playback settings:
  - `suppressSpeakerPlayback`
  - `cacheItems` (use cache vs stream)
  - `audioOffloadEnabled`
- Already have `datastore-preferences` dependency

### 6.4 Audio offload manager
- **File**: `di/PlaybackServiceModule.kt`
- Connect `AudioOffloadManager` to player when API level ≥ 30
- Pass real `AudioOffloadListener` to `audioSink()`

### 6.5 Speaker suppression
- **Files**: `di/PlaybackServiceModule.kt`, `di/Annotations.kt`
- `@SuppressSpeakerPlayback` qualifier already exists
- Wire it to `ExoPlayer.Builder.setSuppressPlaybackOnUnsuitableOutput()`

### 6.6 SuspendingMediaLibrarySessionCallback
- **File**: `player/LibroMediaLibrarySessionCallback.kt`
- Extend Horologist's `SuspendingMediaLibrarySessionCallback` instead of raw interface
- Allows suspend-based overrides when browse content is eventually implemented

---

## Phase 7: UI Improvements

### 7.1 Add settings screen
- **New file**: `settings/SettingsScreen.kt`, `settings/SettingsViewModel.kt`
- Toggle speaker suppression, audio offload, cache mode
- Read/write Proto DataStore settings

### 7.2 Add browsing screen for library
- Create a `MediaPlayerScaffold`-compatible browse screen
- Show downloaded books with artwork, progress, and play button
- Integrate with `PlayerRepository` for media item management

### 7.3 Add volume control screen
- Use Horologist's built-in volume screen from `MediaPlayerScaffold`
- Or create a custom volume screen using `horologist-audio-ui-material3`

### 7.4 Add audio debug screen
- Player state, buffer status, audio offload status
- Useful during development

---

## Execution Order

| Priority | Phase | Effort | Risk |
|----------|-------|--------|------|
| 1 | Phase 1: Finish playback path | Low | Low — missing `setMedia*` calls |
| 2 | Phase 6.6: SuspendingMediaLibrarySessionCallback | Trivial | None |
| 3 | Phase 3: Move API behind Hilt | Medium | Low — refactor singleton to DI |
| 4 | Phase 6.3-6.5: Settings, offload, suppression | Medium | Low — config plumbing |
| 5 | Phase 7.1-7.2: Settings + browse UI | Medium | Low |
| 6 | Phase 4: Catalog sync | Medium | Medium — needs API diffing strategy |
| 7 | Phase 2: Replace download system | High | High — touches core download/playback path |
| 8 | Phase 5: Upgrade navigation | Medium | Medium — scaffold migration |
| 9 | Phase 6.1-6.2: Complications + tiles | Low | Low — additive features |
| 10 | Phase 7.3-7.4: Volume + debug UI | Low | Low — additive features |
