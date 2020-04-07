package com.example.myfirstapp;

import android.annotation.SuppressLint;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.media.AudioAttributes;
import android.media.MediaMetadata;
import android.media.MediaMetadataRetriever;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.media.MediaBrowserCompat;
import android.support.v4.media.MediaDescriptionCompat;
import android.support.v4.media.MediaMetadataCompat;
import android.support.v4.media.session.MediaControllerCompat;
import android.support.v4.media.session.MediaSessionCompat;
import android.support.v4.media.session.PlaybackStateCompat;
import android.util.Log;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;
import androidx.media.MediaBrowserServiceCompat;
import androidx.media.session.MediaButtonReceiver;

import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

public class MediaPlaybackService extends MediaBrowserServiceCompat implements MediaPlayer.OnPreparedListener, MediaPlayer.OnCompletionListener  {

    static final String SECONDS = "com.audiobook.SECONDS";

    private class MySessionCallback extends MediaSessionCompat.Callback {

        private void createNotification() {
            String CHANNEL_ID = "com.example.myfirstapp";

            MediaControllerCompat controller = mediaSession.getController();
            int playPauseIcon = isPlaying() ? R.drawable.ic_pause_white_24dp : R.drawable.ic_play_arrow_white_24dp;

            Notification notification;
            if (notificationBuilder == null) {
                Context context = getApplicationContext();
                notificationBuilder = new NotificationCompat.Builder(context, CHANNEL_ID);

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    CharSequence name = getString(R.string.channel_name);
                    int importance = NotificationManager.IMPORTANCE_DEFAULT;
                    NotificationChannel channel = new NotificationChannel(CHANNEL_ID, name, importance);
                    channel.setDescription(getString(R.string.channel_description));
                    getSystemService(NotificationManager.class).createNotificationChannel(channel);
                }
                MediaMetadataCompat mediaMetadata = controller.getMetadata();
                MediaDescriptionCompat description = mediaMetadata.getDescription();

                notificationBuilder
                        // Add the metadata for the currently playing track
                        .setContentTitle(audioBook.displayName)
                        .setContentText(description.getDescription())
                        .setSubText(description.getDescription())
                        .setLargeIcon(description.getIconBitmap())
                        .setOngoing(true)

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

                        // Add a pause button
                        .addAction(new NotificationCompat.Action(
                                playPauseIcon, getString(R.string.pause),
                                MediaButtonReceiver.buildMediaButtonPendingIntent(context,
                                        PlaybackStateCompat.ACTION_PLAY_PAUSE)))

                        // Take advantage of MediaStyle features
                        .setStyle(new androidx.media.app.NotificationCompat.MediaStyle()
                                .setMediaSession(mediaSession.getSessionToken())
                                .setShowActionsInCompactView(0)
                                // Add a cancel button
                                .setShowCancelButton(true)
                                .setCancelButtonIntent(MediaButtonReceiver.buildMediaButtonPendingIntent(context,
                                        PlaybackStateCompat.ACTION_STOP)));
                notification = notificationBuilder.build();
            } else {
                notification = notificationBuilder.build();
                notification.actions[0] = new Notification.Action(playPauseIcon, "play", MediaButtonReceiver.buildMediaButtonPendingIntent(getApplicationContext(),
                        PlaybackStateCompat.ACTION_PLAY_PAUSE));
            }
            startForeground(2, notification);
        }

        @Override
        public void onPlay() {
            createNotification();
            if (isMediaPlayerPrepared && !mediaPlayer.isPlaying()) {
                PlaybackStateCompat newState = stateBuilder
                        .setState(PlaybackStateCompat.STATE_PLAYING, mediaPlayer.getCurrentPosition() , 1)
                        .build();
                mediaSession.setPlaybackState(newState);
                mediaPlayer.start();
            }
        }

        @Override
        public void onSkipToQueueItem(long id) {
            playTrack((int) id);
        }

        @Override
        public void onSeekTo(long pos) {
            mediaPlayer.seekTo((int) pos);
        }

        @Override
        public void onPause() {
            createNotification();
            if (isMediaPlayerPrepared && mediaPlayer.isPlaying()) {
                PlaybackStateCompat newState = stateBuilder
                        .setState(PlaybackStateCompat.STATE_PAUSED, mediaPlayer.getCurrentPosition() , 1)
                        .build();
                mediaSession.setPlaybackState(newState);
                mediaPlayer.pause();
            }
        }

        @Override
        public void onStop() {
            PlaybackStateCompat newState = stateBuilder
                    .setState(PlaybackStateCompat.STATE_STOPPED, 0 , 1)
                    .build();
            mediaSession.setPlaybackState(newState);
            mediaSession.getController().getTransportControls().pause();
        }

        @Override
        public void onSkipToPrevious() {
            super.onSkipToPrevious();
            if (isMediaPlayerPrepared) {
                if (mediaPlayer.getCurrentPosition() > 10 * 1000) {
                    mediaPlayer.seekTo(0);
                } else {
                    mediaSession.setPlaybackState(
                    stateBuilder.setState(PlaybackStateCompat.STATE_SKIPPING_TO_PREVIOUS, 0, 1)
                            .build());
                    playTrack(positionInTrackList - 1);
                }
            }
        }

        @Override
        public void onSkipToNext() {
            super.onSkipToNext();
            if (isMediaPlayerPrepared) {
                mediaSession.setPlaybackState(
                stateBuilder.setState(PlaybackStateCompat.STATE_SKIPPING_TO_NEXT, 0, 1)
                    .build());
                playTrack(positionInTrackList + 1);
            }
        }
    }

    @SuppressLint("StaticFieldLeak")
    class PlayerUpdate extends AsyncTask<Void, Void, Void> {
        @Override
        protected void onProgressUpdate(Void... values) {
            super.onProgressUpdate(values);
            PlaybackStateCompat state = mediaSession.getController().getPlaybackState();
            stateBuilder.setState(state.getState(), mediaPlayer.getCurrentPosition(), 1);
            mediaSession.setPlaybackState(stateBuilder.build());
        }

        @Override
        protected Void doInBackground(Void... objects) {
            try {
                int lastSecond = 0;
                int second;
                while (!isCancelled()) {
                    if (isMediaPlayerPrepared && lastSecond != (second = mediaPlayer.getCurrentPosition() / 1000)) {
                        publishProgress();
                        lastSecond = second;
                    }
                }
            } catch (IllegalStateException e) {
                e.printStackTrace();
            }
            return null;
        }
    }
    private static final String MY_MEDIA_ROOT_ID = "media_root_id";
    private final String TAG = "ASD";

    private MediaSessionCompat mediaSession;
    private PlaybackStateCompat.Builder stateBuilder;
    private MediaMetadataCompat.Builder metadataBuilder;
    private NotificationCompat.Builder notificationBuilder;

    private final MediaPlayer mediaPlayer = new MediaPlayer();
    private boolean isMediaPlayerPrepared;
    private AudioBook audioBook;
    private AsyncTask<Void, Void, Void> task;
    private int positionInTrackList;

    private boolean isPlaying() {
        if (mediaSession == null){
            return false;
        }
        return mediaSession.getController().getPlaybackState().getState() == PlaybackStateCompat.STATE_PLAYING;
    }


    private void playTrack(int position) {
        if (position < 0 || position >= audioBook.files.size()){
            return;
        }
        MediaItem mediaItem = audioBook.files.get(position);
        mediaPlayer.reset();
        isMediaPlayerPrepared = false;
        if (task != null){
            task.cancel(true);
        }
        mediaPlayer.setAudioAttributes(new AudioAttributes.Builder()
                .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                .build());
        try {
            mediaPlayer.setOnPreparedListener(this);
            mediaPlayer.setOnCompletionListener(this);
            mediaPlayer.setDataSource(getApplicationContext(), Uri.parse(mediaItem.documentUri));
            mediaPlayer.prepare();
            positionInTrackList = position;
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        audioBook = (AudioBook) intent.getSerializableExtra("AUDIOBOOK");
        positionInTrackList = intent.getIntExtra("INDEX", 0);
//        mediaSession.setQueue(getQueue(audioBook.files));
        playTrack(positionInTrackList);
        return super.onStartCommand(intent, flags, startId);
    }

//    private List<MediaSessionCompat.QueueItem> getQueue(List<MediaItem> tracks) {
//        List<MediaSessionCompat.QueueItem> list = new ArrayList<>();
//        for (int i = 0; i < tracks.size(); i++) {
//            list.add(new MediaSessionCompat.QueueItem(trackToDescription(tracks.get(i)), i));
//        }
//        return list;
//    }
//
//    private MediaDescriptionCompat trackToDescription(MediaItem track) {
//        return new MediaDescriptionCompat.Builder()
//                .setMediaUri(Uri.parse(track.documentUri))
//                .setTitle(track.toString())
//                .setIconBitmap(track.getAlbumArt(getApplicationContext()))
//                .build();
//    }

    @Override
    public void onCreate() {
        super.onCreate();

        // Create a MediaSessionCompat
        mediaSession = new MediaSessionCompat(getApplicationContext(), "ASD");

        // Set an initial PlaybackState with ACTION_PLAY, so media buttons can start the player
        stateBuilder = new PlaybackStateCompat.Builder()
                .setActions(
                        PlaybackStateCompat.ACTION_PLAY |
                        PlaybackStateCompat.ACTION_PLAY_PAUSE |
                        PlaybackStateCompat.ACTION_STOP |
                        PlaybackStateCompat.ACTION_PAUSE |
                        PlaybackStateCompat.ACTION_SKIP_TO_NEXT |
                        PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS |
                PlaybackStateCompat.ACTION_SEEK_TO);
        mediaSession.setPlaybackState(stateBuilder.build());
        metadataBuilder = new MediaMetadataCompat.Builder();
        mediaSession.setMetadata(metadataBuilder.build());
        mediaSession.setCallback(new MySessionCallback());
        setSessionToken(mediaSession.getSessionToken());
    }

    private MediaMetadataCompat trackToMetaData(MediaItem item) {
        MediaMetadataRetriever mmr =  new MediaMetadataRetriever();
        mmr.setDataSource(this, Uri.parse(item.documentUri));
        long duration = Long.parseLong(mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION));
        return metadataBuilder
                .putString(MediaMetadataCompat.METADATA_KEY_TITLE, item.displayName)
                .putBitmap(MediaMetadataCompat.METADATA_KEY_ALBUM_ART, item.getAlbumArt(this))
                .putLong(MediaMetadataCompat.METADATA_KEY_DURATION, duration)
                .putLong("AUDIOBOOK_ID", audioBook.files.indexOf(item))
                .build();
    }

    @Override
    public void onCompletion(MediaPlayer mp) {
        int state = mediaSession.getController().getPlaybackState().getState();
        if (state != PlaybackStateCompat.STATE_SKIPPING_TO_NEXT && state != PlaybackStateCompat.STATE_SKIPPING_TO_PREVIOUS) {
            playTrack(positionInTrackList + 1);
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (task != null) {
            task.cancel(true);
        }
        mediaPlayer.release();
        mediaSession.release();
    }

    @Override
    public void onPrepared(MediaPlayer mp) {
        isMediaPlayerPrepared = true;
        mediaSession.setMetadata(trackToMetaData(audioBook.files.get(positionInTrackList)));
        mediaSession.getController().getTransportControls().play();
        task = new PlayerUpdate();
        task.execute();
//        LoudnessEnhancer ef = new LoudnessEnhancer(mediaPlayer.getAudioSessionId());
//        ef.setTargetGain(10000);
//        ef.setEnabled(true);
//        mediaPlayer.setAuxEffectSendLevel(1f);
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