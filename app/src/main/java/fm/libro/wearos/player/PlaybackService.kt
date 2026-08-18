package fm.libro.wearos.player

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService

class PlaybackService : MediaSessionService() {

    private var mediaSession: MediaSession? = null
    private var exoPlayer: ExoPlayer? = null

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        exoPlayer = ExoPlayer.Builder(this).build()
        mediaSession = MediaSession.Builder(this, exoPlayer!!).build()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)

        when (intent?.action) {
            ACTION_PLAY -> {
                val filePath = intent.getStringExtra(EXTRA_FILE_PATH) ?: return START_NOT_STICKY
                val positionMs = intent.getLongExtra(EXTRA_POSITION_MS, 0)

                exoPlayer?.apply {
                    val mediaItem = MediaItem.fromUri("file://$filePath")
                    setMediaItem(mediaItem)
                    seekTo(positionMs)
                    prepare()
                    playWhenReady = true
                }

                startForeground(NOTIFICATION_ID, buildNotification())
            }
            ACTION_PAUSE -> {
                exoPlayer?.playWhenReady = false
            }
            ACTION_STOP -> {
                exoPlayer?.stop()
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
            }
        }

        return START_NOT_STICKY
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? {
        return mediaSession
    }

    override fun onBind(intent: Intent): IBinder? {
        return super.onBind(intent)
    }

    override fun onDestroy() {
        mediaSession?.run {
            player.release()
            release()
        }
        mediaSession = null
        exoPlayer = null
        super.onDestroy()
    }

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            "Audiobook Playback",
            NotificationManager.IMPORTANCE_LOW,
        )
        val manager = getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(channel)
    }

    private fun buildNotification(): Notification {
        val intent = Intent(this, fm.libro.wearos.MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Libro.fm")
            .setContentText("Playing audiobook")
            .setSmallIcon(android.R.drawable.ic_media_play)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .build()
    }

    companion object {
        const val ACTION_PLAY = "fm.libro.wearos.player.PLAY"
        const val ACTION_PAUSE = "fm.libro.wearos.player.PAUSE"
        const val ACTION_STOP = "fm.libro.wearos.player.STOP"
        const val EXTRA_FILE_PATH = "file_path"
        const val EXTRA_POSITION_MS = "position_ms"
        const val EXTRA_ISBN = "isbn"
        const val CHANNEL_ID = "audiobook_playback"
        const val NOTIFICATION_ID = 1001
    }
}
