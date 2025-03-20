package com.kjjejones42.audiobookplayer.player;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.support.v4.media.MediaDescriptionCompat;
import android.support.v4.media.session.MediaControllerCompat;
import android.support.v4.media.session.MediaSessionCompat;
import android.support.v4.media.session.PlaybackStateCompat;

import androidx.core.app.NotificationCompat;
import androidx.media.app.NotificationCompat.MediaStyle;
import androidx.media.session.MediaButtonReceiver;

import com.kjjejones42.audiobookplayer.R;
import com.kjjejones42.audiobookplayer.display.DisplayListActivity;

public class PlayerNotificationManager {
    static final private String CHANNEL_ID = "com.kjjejones42.audiobookplayer";
    private final MediaSessionCompat mediaSession;

    private final Context context;

    public PlayerNotificationManager(MediaSessionCompat mediaSession, Context context) {
        this.mediaSession = mediaSession;
        this.context = context;
        initializeNotification(context);
    }

    private void initializeNotification(Context context) {
        CharSequence name = context.getString(R.string.channel_name);
        int importance = NotificationManager.IMPORTANCE_LOW;
        NotificationChannel channel = new NotificationChannel(CHANNEL_ID, name, importance);
        channel.setLockscreenVisibility(Notification.VISIBILITY_PUBLIC);
        context.getSystemService(NotificationManager.class).createNotificationChannel(channel);
    }

    public Notification updateNotification(boolean playing, String bookName) {

        MediaControllerCompat controller = mediaSession.getController();

        MediaDescriptionCompat description = controller.getMetadata().getDescription();

        Intent intent = new Intent(context, PlayActivity.class);
        intent.putExtra(DisplayListActivity.INTENT_PLAY_FILE, bookName);
        PendingIntent pendingIntent = PendingIntent.getActivity(context, 1, intent,
                PendingIntent.FLAG_CANCEL_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        MediaStyle style = new MediaStyle()
                .setMediaSession(mediaSession.getSessionToken())
                .setShowActionsInCompactView(1, 2, 3)
                .setShowCancelButton(true)
                .setCancelButtonIntent(MediaButtonReceiver.buildMediaButtonPendingIntent(context,
                        PlaybackStateCompat.ACTION_STOP));

        NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(context, CHANNEL_ID);
        notificationBuilder
                .setDeleteIntent(MediaButtonReceiver.buildMediaButtonPendingIntent(context,
                        PlaybackStateCompat.ACTION_STOP))
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .setSmallIcon(R.drawable.ic_logo)
                .setShowWhen(false)
                .setStyle(style)
                .setContentIntent(pendingIntent)
                .setContentTitle(description.getTitle())
                .setContentText(description.getSubtitle())
                .setLargeIcon(description.getIconBitmap())
                .setOngoing(playing);
        return notificationBuilder.build();
    }

}
