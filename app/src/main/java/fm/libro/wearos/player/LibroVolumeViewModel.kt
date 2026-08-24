package fm.libro.wearos.player

import android.os.Vibrator
import com.google.android.horologist.audio.SystemAudioRepository
import com.google.android.horologist.audio.ui.VolumeViewModel as HorologistVolumeViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class LibroVolumeViewModel
    @Inject
    constructor(
        systemAudioRepository: SystemAudioRepository,
        vibrator: Vibrator,
    ) : HorologistVolumeViewModel(
        volumeRepository = systemAudioRepository,
        audioOutputRepository = systemAudioRepository,
        vibrator = vibrator,
    )
