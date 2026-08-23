package fm.libro.wearos.player

import android.annotation.SuppressLint
import androidx.media3.common.MediaItem
import androidx.media3.session.LibraryResult
import androidx.media3.session.MediaLibraryService
import androidx.media3.session.MediaSession
import androidx.media3.session.SessionError
import com.google.android.horologist.media3.logging.ErrorReporter
import com.google.android.horologist.media3.service.SuspendingMediaLibrarySessionCallback
import com.google.common.collect.ImmutableList
import kotlinx.coroutines.CoroutineScope

class LibroMediaLibrarySessionCallback(
    serviceScope: CoroutineScope,
    appEventLogger: ErrorReporter,
) : SuspendingMediaLibrarySessionCallback(serviceScope, appEventLogger) {

    @SuppressLint("UnsafeOptInUsageError")
    override suspend fun onGetLibraryRootInternal(
        session: MediaLibraryService.MediaLibrarySession,
        browser: MediaSession.ControllerInfo,
        params: MediaLibraryService.LibraryParams?,
    ): LibraryResult<MediaItem> {
        return LibraryResult.ofError(SessionError.ERROR_BAD_VALUE)
    }

    @SuppressLint("UnsafeOptInUsageError")
    override suspend fun onGetItemInternal(
        session: MediaLibraryService.MediaLibrarySession,
        browser: MediaSession.ControllerInfo,
        mediaId: String,
    ): LibraryResult<MediaItem> {
        return LibraryResult.ofError(SessionError.ERROR_BAD_VALUE)
    }

    @SuppressLint("UnsafeOptInUsageError")
    override suspend fun onGetChildrenInternal(
        session: MediaLibraryService.MediaLibrarySession,
        browser: MediaSession.ControllerInfo,
        parentId: String,
        page: Int,
        pageSize: Int,
        params: MediaLibraryService.LibraryParams?,
    ): LibraryResult<ImmutableList<MediaItem>> {
        return LibraryResult.ofError(SessionError.ERROR_BAD_VALUE)
    }
}
