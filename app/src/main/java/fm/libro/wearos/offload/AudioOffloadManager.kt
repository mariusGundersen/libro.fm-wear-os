package fm.libro.wearos.offload

import android.annotation.SuppressLint
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.media3.common.Format
import androidx.media3.common.TrackSelectionParameters.AudioOffloadPreferences.DEFAULT
import androidx.media3.exoplayer.DecoderReuseEvaluation
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.analytics.AnalyticsListener
import com.google.android.horologist.media3.logging.ErrorReporter
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

@SuppressLint("UnsafeOptInUsageError")
class AudioOffloadManager(
    private val errorReporter: ErrorReporter,
) {
    private val _offloadStatus = MutableStateFlow(
        AudioOffloadStatus(
            offloadSchedulingEnabled = false,
            sleepingForOffload = false,
            trackOffload = false,
            format = null,
            isPlaying = false,
            errors = listOf(),
            offloadTimes = OffloadTimes(),
            audioOffloadPreferences = DEFAULT,
        ),
    )
    val offloadStatus: StateFlow<AudioOffloadStatus> = _offloadStatus.asStateFlow()

    val audioOffloadListener: ExoPlayer.AudioOffloadListener =
        object : ExoPlayer.AudioOffloadListener {
            override fun onSleepingForOffloadChanged(isSleepingForOffload: Boolean) {
                _offloadStatus.update {
                    it.copy(
                        sleepingForOffload = isSleepingForOffload,
                        offloadTimes = it.offloadTimes.timesToNow(
                            it.sleepingForOffload,
                            it.isPlaying,
                        ),
                    )
                }
                errorReporter.logMessage("sleeping for offload $isSleepingForOffload")
            }

            override fun onOffloadedPlayback(isOffloadedPlayback: Boolean) {
                _offloadStatus.update {
                    it.copy(trackOffload = isOffloadedPlayback)
                }
            }
        }

    @RequiresApi(Build.VERSION_CODES.Q)
    private val analyticsListener: AnalyticsListener =
        object : AnalyticsListener {
            override fun onAudioInputFormatChanged(
                eventTime: AnalyticsListener.EventTime,
                format: Format,
                decoderReuseEvaluation: DecoderReuseEvaluation?,
            ) {
                _offloadStatus.update { it.copy(format = format) }
            }

            override fun onIsPlayingChanged(
                eventTime: AnalyticsListener.EventTime,
                isPlaying: Boolean,
            ) {
                _offloadStatus.update {
                    it.copy(
                        isPlaying = isPlaying,
                        offloadTimes = it.offloadTimes.timesToNow(
                            sleepingForOffload = _offloadStatus.value.sleepingForOffload,
                            updatedIsPlaying = isPlaying,
                        ),
                    )
                }
            }

            override fun onAudioUnderrun(
                eventTime: AnalyticsListener.EventTime,
                bufferSize: Int,
                bufferSizeMs: Long,
                elapsedSinceLastFeedMs: Long,
            ) {
                addError("Audio Underrun")
            }

            override fun onAudioSinkError(
                eventTime: AnalyticsListener.EventTime,
                audioSinkError: Exception,
            ) {
                addError("Audio Sink Error: ${audioSinkError.message}")
            }
        }

    private fun addError(message: String) {
        _offloadStatus.update {
            it.copy(errors = it.errors + AudioError(System.currentTimeMillis(), message))
        }
    }

    @RequiresApi(29)
    fun connect(exoPlayer: ExoPlayer) {
        _offloadStatus.value = AudioOffloadStatus(
            offloadSchedulingEnabled = false,
            sleepingForOffload = exoPlayer.isSleepingForOffload,
            trackOffload = false,
            format = exoPlayer.audioFormat,
            isPlaying = exoPlayer.isPlaying,
            errors = listOf(),
            offloadTimes = OffloadTimes(),
            audioOffloadPreferences = exoPlayer.trackSelectionParameters.audioOffloadPreferences,
        )

        exoPlayer.addAudioOffloadListener(audioOffloadListener)
        exoPlayer.addAnalyticsListener(analyticsListener)
    }
}
