package fm.libro.wearos.player

import androidx.media3.session.MediaLibraryService
import androidx.media3.session.MediaSession
import com.google.android.horologist.media3.service.LifecycleMediaLibraryService
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class PlaybackService : LifecycleMediaLibraryService() {
    @Inject
    public override lateinit var mediaLibrarySession: MediaLibraryService.MediaLibrarySession

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaLibraryService.MediaLibrarySession {
        return mediaLibrarySession
    }
}
