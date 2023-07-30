package com.kjjejones42.audiobookplayer.player;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.support.v4.media.MediaDescriptionCompat;
import android.support.v4.media.session.MediaControllerCompat;
import android.support.v4.media.session.MediaSessionCompat;
import android.support.v4.media.session.PlaybackStateCompat;

import androidx.core.app.NotificationCompat;
import androidx.media.session.MediaButtonReceiver;

import com.kjjejones42.audiobookplayer.R;

public class PlayerNotificationManager {
    static final private String CHANNEL_ID = "com.kjjejones42.audiobookplayer";

    private final NotificationCompat.Builder notificationBuilder;

    private final MediaSessionCompat mediaSession;

    private final Context context;

    public PlayerNotificationManager(MediaSessionCompat mediaSession, Context context) {
        this.mediaSession = mediaSession;
        this.context = context;
        notificationBuilder = new NotificationCompat.Builder(context, CHANNEL_ID);
        initializeNotification();
    }

    private void initializeNotification() {
        CharSequence name = context.getString(R.string.channel_name);
        int importance = NotificationManager.IMPORTANCE_LOW;
        NotificationChannel channel = new NotificationChannel(CHANNEL_ID, name, importance);
        channel.setLockscreenVisibility(Notification.VISIBILITY_PUBLIC);
        context.getSystemService(NotificationManager.class).createNotificationChannel(channel);
        notificationBuilder
                .setContentIntent(mediaSession.getController().getSessionActivity())
                .setDeleteIntent(MediaButtonReceiver.buildMediaButtonPendingIntent(context,
                        PlaybackStateCompat.ACTION_STOP))
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .setSmallIcon(R.drawable.ic_logo)
                .setShowWhen(false)
                .setStyle(new androidx.media.app.NotificationCompat.MediaStyle()
                        .setMediaSession(mediaSession.getSessionToken())
                        .setShowActionsInCompactView(1, 2, 3)
                        .setShowCancelButton(true)
                        .setCancelButtonIntent(MediaButtonReceiver.buildMediaButtonPendingIntent(context,
                                PlaybackStateCompat.ACTION_STOP)));
        setActions(false);
    }

    private void setActions(boolean playing) {
        int playPauseIcon = playing ? R.drawable.ic_pause : R.drawable.ic_play;
        String playPauseText = playing ? "Pause" : "Play";
        notificationBuilder
                .clearActions()
                .addAction(R.drawable.ic_skip_prev,
                        "Prev", MediaButtonReceiver.buildMediaButtonPendingIntent(
                        context, PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS))
                .addAction(R.drawable.ic_replay_30,
                        "Rewind", MediaButtonReceiver.buildMediaButtonPendingIntent(
                        context, PlaybackStateCompat.ACTION_REWIND))
                .addAction(
                        playPauseIcon, playPauseText,
                        MediaButtonReceiver.buildMediaButtonPendingIntent(context,
                                PlaybackStateCompat.ACTION_PLAY_PAUSE))
                .addAction(R.drawable.ic_forward_30,
                        "Forward", MediaButtonReceiver.buildMediaButtonPendingIntent(
                        context, PlaybackStateCompat.ACTION_FAST_FORWARD))
                .addAction(R.drawable.ic_skip_next,
                        "Next", MediaButtonReceiver.buildMediaButtonPendingIntent(
                        context, PlaybackStateCompat.ACTION_SKIP_TO_NEXT));

    }

    public Notification updateNotification(boolean playing, String bookName) {

        MediaControllerCompat controller = mediaSession.getController();

        MediaDescriptionCompat description = controller.getMetadata().getDescription();

        notificationBuilder
                .setContentText(description.getTitle())
                .setLargeIcon(description.getIconBitmap())
                .setOngoing(playing)
                .setContentTitle(bookName);
        setActions(playing);
        return notificationBuilder.build();
    }

}
