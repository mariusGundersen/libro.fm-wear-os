# Libro.fm Wear OS

Android Wear OS audiobook player for [Libro.fm](https://libro.fm). Single-module Kotlin project using Jetpack Compose for Wear OS.

## Build & Run

```bash
./gradlew assembleDebug
```

No Gradle wrapper is checked in; use system `gradle` or generate via `gradle wrapper`.

## Testing

```bash
./gradlew testDebugUnitTest
```

Unit tests run on the JVM under Robolectric (Media3 needs a real `Looper`). See
[Testing Conventions](#testing-conventions) before adding player tests.

`lintDebug` currently fails on a pre-existing `UnsafeOptInUsageError` in
`di/OffloadModule.kt` (`ExoPlayer.AudioOffloadListener` is `@UnstableApi`). It is unrelated to
new work and `assembleDebug` is unaffected.

## Architecture

Single `app` module. Dependency injection uses **Hilt** (`di/`, KSP, `hilt-android:2.56.2`).
ViewModels are `AndroidViewModel` subclasses that obtain collaborators via `@HiltViewModel`
constructor injection, not by reaching for singletons directly.

### Navigation Flow

```
Login → Library (paginated) → Book Detail → Chapters → Player
                 ↓
        Book Detail (downloaded) → Player
```

Routes live in the `Routes` object in `navigation/NavGraph.kt` (`login`, `library`,
`book/{isbn}`, `book/{isbn}/chapters`) and are registered in `additionalNavRoutes` of Horologist's
`MediaPlayerScaffold`. Only the ISBN travels through navigation arguments. The detail screen loads
the book from Room by ISBN; online books (which have no row yet) are served from
`library/BookRepository.kt`, whose in-memory cache is warmed automatically as pages of the online
library are presented.

### Key Layers

| Layer | Files | Pattern |
|-------|-------|---------|
| API | `api/LibroFmClient.kt` (singleton), `api/LibroFmApi.kt` (Retrofit interface), `api/LibraryPagingSource.kt` | Retrofit + OkHttp with Paging 3. Bearer token passed manually via `Authorization` header. |
| Auth | `auth/AuthManager.kt` | DataStore Preferences (`librofm_auth`). Stores token, username, password. |
| Database | `data/AppDatabase.kt` | Room v2 (`librofm.db`). Two entities: `DownloadedBookEntity`, `PlaybackProgressEntity`. DAOs expose `Flow` for reactive UI. |
| Downloads | `download/DownloadManager.kt`, `download/AudiobookDownloadWorker.kt` | WorkManager `CoroutineWorker` running as foreground service. Downloads M4B (single file) or MP3 (ZIP parts). |
| Player | `player/PlaybackService.kt`, `player/PlayerProgressPersister.kt`, `player/PlayerViewModel.kt` | Media3 ExoPlayer in a Horologist `LifecycleMediaLibraryService`. Progress is written by `PlayerProgressPersister`, a `Player.Listener` attached to the player from `di/PlaybackServiceModule.kt`. |

### Data Flow

1. **Library loading:** `LibraryPagingSource` fetches paginated audiobooks from `GET /api/v7/library`. For each book, concurrently fetches `DownloadManifest` from `GET /api/v9/download-manifest` and attaches it to `Audiobook.manifest` (transient field).

2. **Download:** `BookDetailViewModel.startDownload()` enqueues a `OneTimeWorkRequest` with unique work name (`audiobook_download_{isbn}`). `AudiobookDownloadWorker` deserializes the manifest, calls `DownloadManager.downloadAubiobook()`, reports progress via `setProgress()` + `setForegroundAsync()`. ViewModel observes `WorkManager.getWorkInfosForUniqueWorkFlow()`.

3. **Playback:** `PlayerViewModel` loads `DownloadedBookEntity` + `PlaybackProgressEntity` from Room and restores the last position only when the player has no media items yet. Starts/stops `PlaybackService` via Intent extras.

### Progress Persistence

`PlayerProgressPersister` is the single owner of playback-position writes. It is provided by Hilt
and injected into `mediaLibrarySession(...)` in `PlaybackServiceModule.kt` — if that injection is
removed the persister is silently never instantiated, and nothing is saved.

- **ISBN source:** Horologist `0.7.15` never sets `MediaItem.tag` or maps `Media.extras`, so the
  ISBN is parsed from the media id, which is `"${isbn}_$trackIndex"`. Use
  `substringBeforeLast("_")` (never `substringBefore`, which breaks on ISBNs containing `_`).
- **Save triggers:** pause (`!isPlaying && !playWhenReady`), `EVENT_MEDIA_ITEM_TRANSITION`,
  `STATE_ENDED`, `STATE_IDLE`, `onPlayerError`, seek discontinuities, an explicit `flush()` on
  service destroy, and a 5s periodic save while playing.
- **Throttling:** writes are deduplicated on `(isbn, trackIndex, positionMs)` and otherwise
  throttled to 5s, except on the forced paths above. The clock is injected as `nowMs` for tests.
- **Never write a position of `0` deliberately:** position 0 is a valid save (e.g. right after an
  automatic track transition) and is written as-is.
- **Ordering:** `setLastPlayingIsbn(isbn)` must be called *before* `setMediaList(...)` / `play()`
  so the persisted ISBN is available when the session callback resolves the current book.
- **Thread affinity:** every `player.*` read (`currentMediaItem`, `currentPosition`,
  `currentMediaItemIndex`, `playbackParameters`, `isPlaying`) must happen on the player's
  application thread, or ExoPlayer throws "Player is accessed on the wrong thread". The injected
  `@ForApplicationScope` scope runs on `Dispatchers.Default`, so the periodic loop launches with
  `Dispatchers.Main.immediate` (`scope.launch(Dispatchers.Main.immediate) { ... }`). Listener
  callbacks (`onEvents`, `onPositionDiscontinuity`, `onPlayerError`) and `flush()` are already on
  the right thread. Room writes may go anywhere.

Player state is read lazily from ExoPlayer, so a single coalesced state update may skip the
intermediate "playing" state entirely. Do not gate saving on having observed an intermediate
state — that loses the pause save, which is the bug this class exists to prevent.

### Room Entities

- `downloaded_books`: isbn (PK), title, author, coverUrl, coverLocalPath, format, filePath, fileSizeBytes, durationSeconds, trackCount, tracksJson, downloadedAt
- `playback_progress`: isbn (PK), trackIndex, positionMs, playbackSpeed, updatedAt

Database migrations are in `AppDatabase.kt`. Current version: 3.

### API Endpoints

- `POST /oauth/token` — form-urlencoded login
- `GET /api/v7/library?page=N` — paginated library
- `GET /api/v9/download-manifest?isbn=X` — download URLs + track metadata

Base URL: `https://libro.fm/`. User-Agent mimics the official app (`okhttp/5.3.2`, app version `7.34.8`).

## Key Conventions

- **ViewModels** are `AndroidViewModel` subclasses using `SavedStateHandle` for nav args
- **UI state** is a single `data class` held in `MutableStateFlow`, collected via `collectAsState()`
- **Wear OS specifics:** `ScalingLazyColumn` for lists, `SwipeDismissableNavHost` for navigation, `CircularProgressIndicator` from `wear.compose.material3`
- **Book model:** `Audiobook` has `@Transient var manifest: DownloadManifest?` attached at runtime by `LibraryPagingSource`. The `DownloadedBookDetailViewModel` converts `DownloadedBookEntity` → `Audiobook` via a private extension function.
- **No comments** in code unless explicitly requested
- **Progress calculation:** `Audiobook` has computed properties (`listeningProgressPercent`, `remainingTimeString`) that use actual track durations from `manifest.tracks` when available, falling back to average track length approximation

## Testing Conventions

Player tests use Robolectric plus Media3's `SimpleBasePlayer` (`FakeAudioPlayer`). Three traps
that cost real debugging time:

- **`onEvents` state is read lazily.** `SimpleBasePlayer` reads `getState()` when the posted
  runnable executes, so a `play()` / seek / `pause()` sequence followed by a single `drain()`
  collapses into one event carrying only the *final* state. Anything that latches a flag from an
  observed intermediate state (e.g. "has played", `lastPlayingIsbn`) will never see it.
- **Always `drain()` after player calls:** `shadowOf(Looper.getMainLooper()).idle()`, then
  `scheduler.runCurrent()` so writes queued on the background-dispatcher scope land. Robolectric
  fails the test with an unexecuted-runnables `AssertionError` otherwise. `drain()` must use
  `runCurrent()` and never `advanceUntilIdle()` — see below.
- **Never use `runTest` for these tests.** It drains the scheduler on exit, and
  `PlayerProgressPersister`'s 5s periodic loop never terminates. Drive virtual time with
  `scheduler.advanceTimeBy(...)` instead, and keep `advanceTimeBy` calls bounded by less than one
  loop period so a single tick fires.
- **Thread affinity is not testable here.** `Dispatchers.setMain(UnconfinedTestDispatcher(...))`
  resumes continuations inline, so the `Dispatchers.Main.immediate` hop that keeps player reads on
  the application thread is a no-op under test. A regression there passes the suite and only shows
  up on device. Keep the hop.

Inject virtual time into the persister via the `nowMs` parameter
(`PlayerProgressPersister(player, dao, scope) { scheduler.currentTime }`) so throttle behaviour is
deterministic; real `System.currentTimeMillis()` never advances in tests.

## File Map

```
app/src/main/java/fm/libro/wearos/
├── MainActivity.kt                    # Single activity, sets Compose content
├── LibroFmApp.kt                      # Application class, lazy DB init
├── api/
│   ├── LibroFmApi.kt                  # Retrofit interface (3 endpoints)
│   ├── LibroFmClient.kt              # Singleton HTTP client wrapper
│   ├── LibraryPagingSource.kt         # PagingSource with concurrent manifest fetching
│   └── models/
│       ├── Audiobook.kt               # Core data model + progress helpers
│       ├── DownloadManifest.kt        # Download URL + track metadata
│       └── TokenMetadata.kt           # OAuth token response
├── auth/
│   ├── AuthManager.kt                 # DataStore-based credential storage
│   ├── LoginViewModel.kt             # Login logic
│   ├── LoginScreen.kt                 # Wear OS login UI
│   └── TextInputActivity.kt          # Wear OS text input helper
├── data/
│   ├── AppDatabase.kt                 # Room database singleton (v2)
│   ├── DownloadedBookEntity.kt        # Downloaded book entity
│   ├── DownloadedBookDao.kt           # Book queries (Flow + suspend)
│   ├── PlaybackProgressEntity.kt      # Playback position entity
│   ├── PlaybackProgressDao.kt         # Progress queries
│   └── StoredTrack.kt                 # Track metadata for JSON serialization
├── di/                                # Hilt modules
│   ├── AppModule.kt
│   ├── Annotations.kt                 # @ForApplicationScope qualifier
│   ├── DatabaseModule.kt
│   ├── NetworkModule.kt
│   ├── OffloadModule.kt               # Audio offload + application CoroutineScope
│   ├── PlaybackServiceModule.kt       # mediaLibrarySession + progress persister wiring
│   └── ViewModelModule.kt
├── download/
│   ├── DownloadManager.kt             # Core download logic (M4B/MP3/ZIP)
│   └── AudiobookDownloadWorker.kt     # WorkManager CoroutineWorker
├── library/
│   ├── LibraryViewModel.kt            # Paging library books
│   ├── LibraryScreen.kt               # Paginated book list + BookCard
│   ├── BookDetailViewModel.kt         # Online book detail + download trigger
│   ├── BookDetailScreen.kt            # Book info + download/play/delete UI
│   ├── ChaptersViewModel.kt           # Chapter list + track-level playback
│   ├── ChaptersScreen.kt
│   ├── DownloadedBooksViewModel.kt    # Room-backed downloaded books list
│   ├── DownloadedBooksScreen.kt       # Downloaded books list UI
│   ├── DownloadedBookDetailViewModel.kt  # Local book detail from Room
│   └── DownloadedBookDetailScreen.kt  # Local book detail UI
├── navigation/
│   └── NavGraph.kt                    # 4-route navigation graph
├── player/
│   ├── PlayerViewModel.kt             # Playback state + Media3 control
│   ├── PlayerScreen.kt                # Player UI with controls
│   ├── PlayerProgressPersister.kt     # Player.Listener writing progress to Room
│   ├── PlayerStateRepository.kt       # Last-playing ISBN + stored speed
│   ├── AudiobookMediaMapper.kt        # DownloadedBookEntity → MediaItem list
│   ├── LibroMediaLibrarySessionCallback.kt
│   └── PlaybackService.kt             # LifecycleMediaLibraryService (ExoPlayer)
└── ui/theme/
    └── Theme.kt                       # Minimal Wear Compose theme
```

Tests live in `app/src/test/java/fm/libro/wearos/player/`:

```
├── FakeAudioPlayer.kt                 # SimpleBasePlayer-based Player for listener tests
├── FakePlaybackProgressDao.kt         # In-memory PlaybackProgressDao
└── PlayerProgressPersisterTest.kt     # Save-trigger, dedup and throttle coverage
```
