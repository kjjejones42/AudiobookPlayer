package com.example.myfirstapp;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.media.MediaBrowserCompat;
import android.support.v4.media.MediaDescriptionCompat;
import android.support.v4.media.MediaMetadataCompat;
import android.support.v4.media.session.MediaControllerCompat;
import android.support.v4.media.session.MediaSessionCompat;
import android.support.v4.media.session.PlaybackStateCompat;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;
import androidx.media.MediaBrowserServiceCompat;
import androidx.media.session.MediaButtonReceiver;

import java.util.ArrayList;
import java.util.List;

public class MediaPlaybackService extends MediaBrowserServiceCompat {

    private class MySessionCallback extends MediaSessionCompat.Callback {

        private void createNotification(){
            String CHANNEL_ID = "com.example.myfirstapp";

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                CharSequence name = getString(R.string.channel_name);
                String description = getString(R.string.channel_description);
                int importance = NotificationManager.IMPORTANCE_DEFAULT;
                NotificationChannel channel = new NotificationChannel(CHANNEL_ID, name, importance);
                channel.setDescription(description);
                NotificationManager notificationManager = getSystemService(NotificationManager.class);
                notificationManager.createNotificationChannel(channel);
            }
            MediaControllerCompat controller = mediaSession.getController();
            MediaMetadataCompat mediaMetadata = controller.getMetadata();
            MediaDescriptionCompat description = mediaMetadata.getDescription();
            Context context = getApplicationContext();
            NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID);

            builder
                    // Add the metadata for the currently playing track
                    .setContentTitle(description.getTitle())
                    .setContentText(description.getSubtitle())
                    .setSubText(description.getDescription())
                    .setLargeIcon(description.getIconBitmap())

                    // Enable launching the player by clicking the notification
                    .setContentIntent(controller.getSessionActivity())

                    // Stop the service when the notification is swiped away
                    .setDeleteIntent(MediaButtonReceiver.buildMediaButtonPendingIntent(context,
                            PlaybackStateCompat.ACTION_STOP))

                    // Make the transport controls visible on the lockscreen
                    .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)

                    // Add an app icon and set its accent color
                    // Be careful about the color
                    .setSmallIcon(R.drawable.ic_play_arrow_white_24dp)
//                    .setColor(ContextCompat.getColor(context, R.color.colorAccent))

                    // Add a pause button
                    .addAction(new NotificationCompat.Action(
                            R.drawable.ic_pause_white_24dp, getString(R.string.pause),
                            MediaButtonReceiver.buildMediaButtonPendingIntent(context,
                                    PlaybackStateCompat.ACTION_PLAY_PAUSE)))

                    .addAction(new NotificationCompat.Action(
                            R.drawable.ic_play_arrow_white_24dp, getString(R.string.pause),
                            MediaButtonReceiver.buildMediaButtonPendingIntent(context,
                                    PlaybackStateCompat.ACTION_PAUSE)))


                    // Take advantage of MediaStyle features
                    .setStyle(new androidx.media.app.NotificationCompat.MediaStyle()
                            .setMediaSession(mediaSession.getSessionToken())
                            .setShowActionsInCompactView(0)
                            // Add a cancel button
                            .setShowCancelButton(true)
                            .setCancelButtonIntent(MediaButtonReceiver.buildMediaButtonPendingIntent(context,
                                    PlaybackStateCompat.ACTION_STOP)));

            startForeground(2, builder.build());

        }

        @Override
        public void onPlay() {
            createNotification();
            Log.d("ASD", "onPlay");

        }

        @Override
        public void onPause() {
            Log.d("ASD", "onPause");
//            mExoPlayer.setPlayWhenReady(false);
        }

        @Override
        public void onStop() {
            Log.d("ASD", "STOP");
        }

        @Override
        public void onSkipToPrevious() {
//            mExoPlayer.seekTo(0);
            Log.d("ASD", "onSkipToPrevious");
        }
    }

    private static final String MY_MEDIA_ROOT_ID = "media_root_id";

    private MediaSessionCompat mediaSession;
    private PlaybackStateCompat.Builder stateBuilder;

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        MediaItem track = (MediaItem) intent.getSerializableExtra("URI");
        if (mediaSession != null) {
            mediaSession.setMetadata(trackToMetaData(track));
        }
        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public void onCreate() {
        super.onCreate();

        // Create a MediaSessionCompat
        mediaSession = new MediaSessionCompat(getApplicationContext(), "ASD");

        // Enable callbacks from MediaButtons and TransportControls
        mediaSession.setFlags(
                MediaSessionCompat.FLAG_HANDLES_MEDIA_BUTTONS |
                        MediaSessionCompat.FLAG_HANDLES_TRANSPORT_CONTROLS);

        // Set an initial PlaybackState with ACTION_PLAY, so media buttons can start the player
        stateBuilder = new PlaybackStateCompat.Builder()
                .setActions(
                        PlaybackStateCompat.ACTION_PLAY |
                        PlaybackStateCompat.ACTION_PLAY_PAUSE |
                        PlaybackStateCompat.ACTION_STOP |
                        PlaybackStateCompat.ACTION_PAUSE |
                        PlaybackStateCompat.ACTION_SKIP_TO_NEXT |
                        PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS);
        mediaSession.setPlaybackState(stateBuilder.build());
        mediaSession.setMetadata(new MediaMetadataCompat.Builder().build());
        mediaSession.setCallback(new MySessionCallback());
        setSessionToken(mediaSession.getSessionToken());
    }

    private MediaMetadataCompat trackToMetaData(MediaItem item) {
        return new MediaMetadataCompat.Builder()
                .putString(MediaMetadataCompat.METADATA_KEY_TITLE, item.displayName)
                .putBitmap(MediaMetadataCompat.METADATA_KEY_ALBUM_ART, item.getAlbumArt(this)).build();
    }

    @Nullable
    @Override
    public BrowserRoot onGetRoot(@NonNull String clientPackageName, int clientUid, @Nullable Bundle rootHints) {
        return new BrowserRoot(MY_MEDIA_ROOT_ID, null);
    }

    @Override
    public void onLoadChildren(@NonNull String parentId, @NonNull Result<List<MediaBrowserCompat.MediaItem>> result) {

        List<MediaBrowserCompat.MediaItem> mediaItems = new ArrayList<>();
        // Check if this is the root menu:
        if (MY_MEDIA_ROOT_ID.equals(parentId)) {
            // Build the MediaItem objects for the top level,
            // and put them in the mediaItems list...
        } else {
            // Examine the passed parentMediaId to see which submenu we're at,
            // and put the children of that menu in the mediaItems list...
        }
        result.sendResult(mediaItems);
    }


}