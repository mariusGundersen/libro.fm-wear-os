# Libro.fm Wear OS

Android Wear OS audiobook player for [Libro.fm](https://libro.fm). Single-module Kotlin project using Jetpack Compose for Wear OS.

## Build & Run

```bash
./gradlew assembleDebug
```

No Gradle wrapper is checked in; use system `gradle` or generate via `gradle wrapper`.

## Architecture

Single `app` module. No DI framework — ViewModels use `AndroidViewModel(application)` and access singletons directly.

### Navigation Flow

```
Login → Downloaded Books → Downloaded Book Detail → Player
                  ↓
              Library (paginated) → Book Detail → Player
```

Six routes in `navigation/NavGraph.kt` via `SwipeDismissableNavHost`. Book objects are passed between screens through a static `BookDetailViewModel.bookCache` companion map (keyed by ISBN), not through navigation arguments.

### Key Layers

| Layer | Files | Pattern |
|-------|-------|---------|
| API | `api/LibroFmClient.kt` (singleton), `api/LibroFmApi.kt` (Retrofit interface), `api/LibraryPagingSource.kt` | Retrofit + OkHttp with Paging 3. Bearer token passed manually via `Authorization` header. |
| Auth | `auth/AuthManager.kt` | DataStore Preferences (`librofm_auth`). Stores token, username, password. |
| Database | `data/AppDatabase.kt` | Room v2 (`librofm.db`). Two entities: `DownloadedBookEntity`, `PlaybackProgressEntity`. DAOs expose `Flow` for reactive UI. |
| Downloads | `download/DownloadManager.kt`, `download/AudiobookDownloadWorker.kt` | WorkManager `CoroutineWorker` running as foreground service. Downloads M4B (single file) or MP3 (ZIP parts). |
| Player | `player/PlaybackService.kt`, `player/PlayerViewModel.kt` | Media3 ExoPlayer in `MediaSessionService`. Communicates via Intent actions. Progress saved to Room on ViewModel clear. |

### Data Flow

1. **Library loading:** `LibraryPagingSource` fetches paginated audiobooks from `GET /api/v7/library`. For each book, concurrently fetches `DownloadManifest` from `GET /api/v9/download-manifest` and attaches it to `Audiobook.manifest` (transient field).

2. **Download:** `BookDetailViewModel.startDownload()` enqueues a `OneTimeWorkRequest` with unique work name (`audiobook_download_{isbn}`). `AudiobookDownloadWorker` deserializes the manifest, calls `DownloadManager.downloadAubiobook()`, reports progress via `setProgress()` + `setForegroundAsync()`. ViewModel observes `WorkManager.getWorkInfosForUniqueWorkFlow()`.

3. **Playback:** `PlayerViewModel` loads `DownloadedBookEntity` + `PlaybackProgressEntity` from Room. Starts/stops `PlaybackService` via Intent extras. Saves position on `onCleared()`.

### Room Entities

- `downloaded_books`: isbn (PK), title, author, coverUrl, coverLocalPath, format, filePath, fileSizeBytes, durationSeconds, trackCount, tracksJson, downloadedAt
- `playback_progress`: isbn (PK), trackIndex, positionMs, playbackSpeed, updatedAt

Database migrations are in `AppDatabase.kt`. Current version: 2.

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
├── download/
│   ├── DownloadManager.kt             # Core download logic (M4B/MP3/ZIP)
│   └── AudiobookDownloadWorker.kt     # WorkManager CoroutineWorker
├── library/
│   ├── LibraryViewModel.kt            # Paging library books
│   ├── LibraryScreen.kt               # Paginated book list + BookCard
│   ├── BookDetailViewModel.kt         # Online book detail + download trigger
│   ├── BookDetailScreen.kt            # Book info + download/play/delete UI
│   ├── DownloadedBooksViewModel.kt    # Room-backed downloaded books list
│   ├── DownloadedBooksScreen.kt       # Downloaded books list UI
│   ├── DownloadedBookDetailViewModel.kt  # Local book detail from Room
│   └── DownloadedBookDetailScreen.kt  # Local book detail UI
├── navigation/
│   └── NavGraph.kt                    # 6-route navigation graph
├── player/
│   ├── PlayerViewModel.kt             # Playback state + Media3 control
│   ├── PlayerScreen.kt                # Player UI with controls
│   └── PlaybackService.kt            # MediaSessionService (ExoPlayer)
└── ui/theme/
    └── Theme.kt                       # Minimal Wear Compose theme
```
