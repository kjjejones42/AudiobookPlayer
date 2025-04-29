package com.kjjejones42.audiobookplayer.player

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.support.v4.media.session.MediaSessionCompat
import android.support.v4.media.session.PlaybackStateCompat
import androidx.media.app.NotificationCompat
import androidx.media.session.MediaButtonReceiver
import com.kjjejones42.audiobookplayer.R
import com.kjjejones42.audiobookplayer.display.DisplayListActivity

private const val CHANNEL_ID = "com.kjjejones42.audiobookplayer"

class PlayerNotificationManager(
    private val mediaSession: MediaSessionCompat,
    private val context: Context
) {


    init {
        initializeNotification(context)
    }

    private fun initializeNotification(context: Context) {
        val name: CharSequence = context.getString(R.string.channel_name)
        val importance = NotificationManager.IMPORTANCE_LOW
        val channel = NotificationChannel(CHANNEL_ID, name, importance)
        channel.lockscreenVisibility = Notification.VISIBILITY_PUBLIC
        context.getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }

    fun updateNotification(playing: Boolean, bookName: String?): Notification {
        val controller = mediaSession.controller

        val description = controller.metadata.description

        val intent = Intent(context, PlayActivity::class.java)
        intent.putExtra(DisplayListActivity.INTENT_PLAY_FILE, bookName)
        val pendingIntent = PendingIntent.getActivity(
            context, 1, intent,
            PendingIntent.FLAG_CANCEL_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val style = NotificationCompat.MediaStyle()
            .setMediaSession(mediaSession.sessionToken)
            .setShowActionsInCompactView(1, 2, 3)
            .setShowCancelButton(true)
            .setCancelButtonIntent(
                MediaButtonReceiver.buildMediaButtonPendingIntent(
                    context,
                    PlaybackStateCompat.ACTION_STOP
                )
            )

        val notificationBuilder = androidx.core.app.NotificationCompat.Builder(context, CHANNEL_ID)
        notificationBuilder
            .setDeleteIntent(
                MediaButtonReceiver.buildMediaButtonPendingIntent(
                    context,
                    PlaybackStateCompat.ACTION_STOP
                )
            )
            .setVisibility(androidx.core.app.NotificationCompat.VISIBILITY_PUBLIC)
            .setSmallIcon(R.drawable.ic_logo)
            .setShowWhen(false)
            .setStyle(style)
            .setContentIntent(pendingIntent)
            .setContentTitle(description.title)
            .setContentText(description.subtitle)
            .setLargeIcon(description.iconBitmap)
            .setOngoing(playing)
        return notificationBuilder.build()
    }
}
