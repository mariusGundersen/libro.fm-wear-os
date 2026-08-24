package fm.libro.wearos.offload

import android.annotation.SuppressLint
import androidx.media3.common.Format
import androidx.media3.common.TrackSelectionParameters.AudioOffloadPreferences

@SuppressLint("UnsafeOptInUsageError")
data class AudioOffloadStatus(
    val offloadSchedulingEnabled: Boolean,
    val sleepingForOffload: Boolean,
    val trackOffload: Boolean = false,
    val format: Format?,
    val isPlaying: Boolean,
    val errors: List<AudioError>,
    val offloadTimes: OffloadTimes,
    val audioOffloadPreferences: AudioOffloadPreferences,
) {
    fun updateToNow(): OffloadTimes = offloadTimes.timesToNow(
        sleepingForOffload,
        isPlaying,
    )

    fun trackOffloadDescription(): String = if (trackOffload) "HW" else "SW"

    companion object {
        @SuppressLint("UnsafeOptInUsageError")
        val Disabled: AudioOffloadStatus = AudioOffloadStatus(
            offloadSchedulingEnabled = false,
            sleepingForOffload = false,
            trackOffload = false,
            format = null,
            isPlaying = false,
            errors = listOf(),
            offloadTimes = OffloadTimes(),
            audioOffloadPreferences = AudioOffloadPreferences.Builder().build(),
        )
    }
}
