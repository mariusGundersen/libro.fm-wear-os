package fm.libro.wearos.offload

import android.annotation.SuppressLint
import androidx.media3.exoplayer.ExoPlayer.AudioOffloadListener

@SuppressLint("UnsafeOptInUsageError")
class AudioOffloadListenerList : AudioOffloadListener {
    private val listeners = mutableListOf<AudioOffloadListener>()

    fun addListener(listener: AudioOffloadListener) {
        synchronized(listeners) {
            listeners.add(listener)
        }
    }

    fun removeListener(listener: AudioOffloadListener) {
        synchronized(listeners) {
            listeners.remove(listener)
        }
    }

    override fun onSleepingForOffloadChanged(isSleepingForOffload: Boolean) {
        synchronized(listeners) {
            for (it in listeners) {
                it.onSleepingForOffloadChanged(isSleepingForOffload)
            }
        }
    }

    override fun onOffloadedPlayback(isOffloadedPlayback: Boolean) {
        synchronized(listeners) {
            for (it in listeners) {
                it.onOffloadedPlayback(isOffloadedPlayback)
            }
        }
    }
}
