# Libro.fm Wear OS vs Horologist mediasample

## Architecture Convergence

Our app already closely follows the mediasample's Hilt + Horologist pattern. The `PlayerRepositoryImpl`/`MediaBrowser` wiring in `ViewModelModule` is essentially identical. Both `PlaybackService` classes are minimal `LifecycleMediaLibraryService` shells.

## Major Differences

### 1. Data source

We use a real REST API (Retrofit + Gson + Bearer auth) with Paging 3. They fetch a static JSON catalog from GCS via Moshi, wrapped in Horologist's `NetworkAwareCallFactory` for bandwidth-aware networking.

### 2. Domain model

Our `Audiobook` carries audiobook-specific logic (progress math, track durations, cover URLs). Their models are generic `Media`/`Playlist` types from Horologist core — dumb containers with all state in `PlayerRepository`.

### 3. Downloads

Fundamentally different: we have a custom `DownloadManager` doing raw OkHttp byte-copying to `filesDir/audiobooks/{isbn}` (M4B or ZIP extraction). They use media3's `DownloadManager` + `SimpleCache` + `MediaDownloadService` with resumable index-based downloads shared between download and playback.

### 4. Session callback

Neither actually serves library content. Both return `ERROR_BAD_VALUE` from all methods. Theirs uses `SuspendingMediaLibrarySessionCallback` (suspend-based); ours is synchronous `ListenableFuture`.

### 5. Sync

They have a working `Syncable` that syncs catalog→Room via `PlaylistRepositorySyncable`. We have the same `SyncModule` scaffold but it's wired to `emptyArray<Syncable>()`.

### 6. Navigation

They use `MediaPlayerScaffold` with type-safe serializable routes, deep links, and scaffold-provided player/browse/settings/volume screens. We have a hand-rolled 6-route `SwipeDismissableNavHost` without deep links.

### 7. UI screens

They have ~15 screens (browse variants, playlists, settings, developer options, audio debug, Google sign-in, two player UIs). We have 7 (login, downloaded list, library, two book details, player, text input).

### 8. Missing from our app

- `MediaBrowser` is wired but nothing calls `setMedia*` to push content to the player
- Watch-face complications
- Tiles
- Proto DataStore settings
- Audio-offload manager
- Speaker-suppression
- Network-aware routing

## Actionable Deltas (ours → theirs)

1. **Downloads**: replace custom OkHttp/ZIP downloader with media3 `DownloadManager` + Horologist `MediaDownloadService` + shared `SimpleCache` read-through in playback
2. **API/Hilt**: register Retrofit behind Hilt with a network-aware call factory instead of the `LibroFmClient` singleton
3. **Sync**: implement a real `Syncable` (catalog→Room) behind the existing empty `SyncModule` stub
4. **Navigation**: consider Horologist `MediaPlayerScaffold` for player/settings/volume routes + typed serializable routes and deep links
5. **Session callback**: swap to `SuspendingMediaLibrarySessionCallback` if/when browse content is ever served
6. **Finish repository playback path**: wire `PlayerRepository.setMediaList/setMedia` from detail screens (currently missing entirely)
7. **Nice-to-haves**: complications, tile, proto-datastore settings, speaker-suppression qualifier, audio-offload manager, debug/benchmark screens
