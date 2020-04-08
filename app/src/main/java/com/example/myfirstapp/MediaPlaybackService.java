package com.example.myfirstapp;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.media.AudioAttributes;
import android.media.MediaMetadataRetriever;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.media.MediaBrowserCompat;
import android.support.v4.media.MediaDescriptionCompat;
import android.support.v4.media.MediaMetadataCompat;
import android.support.v4.media.session.MediaControllerCompat;
import android.support.v4.media.session.MediaSessionCompat;
import android.support.v4.media.session.PlaybackStateCompat;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;
import androidx.media.MediaBrowserServiceCompat;
import androidx.media.session.MediaButtonReceiver;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

public class MediaPlaybackService extends MediaBrowserServiceCompat implements MediaPlayer.OnPreparedListener, MediaPlayer.OnCompletionListener  {

    private class MySessionCallback extends MediaSessionCompat.Callback {

        private void initializeNotification(){
            String CHANNEL_ID = "com.example.myfirstapp";
            Context context = getApplicationContext();
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                CharSequence name = getString(R.string.channel_name);
                int importance = NotificationManager.IMPORTANCE_DEFAULT;
                NotificationChannel channel = new NotificationChannel(CHANNEL_ID, name, importance);
                channel.setLockscreenVisibility(Notification.VISIBILITY_PUBLIC);
                channel.setDescription(getString(R.string.channel_description));
                getSystemService(NotificationManager.class).createNotificationChannel(channel);
            }
            notificationBuilder = new NotificationCompat.Builder(context, CHANNEL_ID);
            notificationBuilder
                    .setContentIntent(mediaSession.getController().getSessionActivity())
                    .setDeleteIntent(MediaButtonReceiver.buildMediaButtonPendingIntent(context,
                            PlaybackStateCompat.ACTION_STOP))
                    .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                    .setSmallIcon(R.drawable.ic_play)
                    .setStyle(new androidx.media.app.NotificationCompat.MediaStyle()
                            .setMediaSession(mediaSession.getSessionToken())
                            .setShowActionsInCompactView(1, 2, 3)
                            .setShowCancelButton(true)
                            .setCancelButtonIntent(MediaButtonReceiver.buildMediaButtonPendingIntent(context,
                                    PlaybackStateCompat.ACTION_STOP)))
                    .addAction(new NotificationCompat.Action(R.drawable.ic_skip_prev,
                            "Prev", MediaButtonReceiver.buildMediaButtonPendingIntent(
                            context, PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS)))
                    .addAction(new NotificationCompat.Action(R.drawable.ic_replay_30,
                            "Rewind", MediaButtonReceiver.buildMediaButtonPendingIntent(
                            context, PlaybackStateCompat.ACTION_REWIND)))
                    .addAction(new NotificationCompat.Action(
                            R.drawable.ic_play, "Play",
                            MediaButtonReceiver.buildMediaButtonPendingIntent(context,
                                    PlaybackStateCompat.ACTION_PLAY_PAUSE)))
                    .addAction(new NotificationCompat.Action(R.drawable.ic_forward_30,
                            "Forward", MediaButtonReceiver.buildMediaButtonPendingIntent(
                            context, PlaybackStateCompat.ACTION_FAST_FORWARD)))
                    .addAction(new NotificationCompat.Action(R.drawable.ic_skip_next,
                            "Next", MediaButtonReceiver.buildMediaButtonPendingIntent(
                            context, PlaybackStateCompat.ACTION_SKIP_TO_NEXT)));

        }

        private void updateNotification() {

            MediaControllerCompat controller = mediaSession.getController();

            boolean playing = isPlaying();
            int playPauseIcon = playing ? R.drawable.ic_pause : R.drawable.ic_play;
            String playPauseText = playing ? "Pause" : "Play";

            Context context = getApplicationContext();
            if (notificationBuilder == null){
                initializeNotification();
            }
            MediaDescriptionCompat description = controller.getMetadata().getDescription();

            notificationBuilder
                    .setContentTitle(audioBook.displayName)
                    .setContentText(description.getTitle())
                    .setLargeIcon(description.getIconBitmap())
                    .setOngoing(playing);

            notification = notificationBuilder.build();
            notification.actions[2] = new Notification.Action(
                    playPauseIcon, playPauseText,
                    MediaButtonReceiver.buildMediaButtonPendingIntent(context,
                            PlaybackStateCompat.ACTION_PLAY_PAUSE));
            startForeground(2, notification);
        }

        @Override
        public void onPlay() {
            if (isMediaPlayerPrepared && !mediaPlayer.isPlaying()) {
                PlaybackStateCompat newState = stateBuilder
                        .setState(PlaybackStateCompat.STATE_PLAYING, mediaPlayer.getCurrentPosition() , 1)
                        .build();
                mediaSession.setPlaybackState(newState);
                mediaPlayer.start();
            }
            updateNotification();
        }

        @Override
        public void onSkipToQueueItem(long id) {
            playTrack((int) id);
        }

        @Override
        public void onSeekTo(long pos) {
            if (pos < 0){
                pos = 0;
            } else {
                int duration = mediaPlayer.getDuration();
                if (pos > duration){
                    pos = duration;
                }
            }
            mediaSession.setPlaybackState(
            stateBuilder
                    .setState(mediaSession.getController().getPlaybackState().getState(), pos, 1)
                    .build()
            );
            mediaPlayer.seekTo((int) pos);
        }

        @Override
        public void onPause() {
            if (isMediaPlayerPrepared && mediaPlayer.isPlaying()) {
                PlaybackStateCompat newState = stateBuilder
                        .setState(PlaybackStateCompat.STATE_PAUSED, mediaPlayer.getCurrentPosition() , 1)
                        .build();
                mediaSession.setPlaybackState(newState);
                mediaPlayer.pause();
            }
            updateNotification();
            stopForeground(false);
        }

        @Override
        public void onStop() {
            PlaybackStateCompat newState = stateBuilder
                    .setState(PlaybackStateCompat.STATE_STOPPED, 0 , 1)
                    .build();
            mediaSession.setPlaybackState(newState);
        }

        @Override
        public void onFastForward() {
            super.onFastForward();
            mediaSession.getController().getTransportControls().seekTo(
                    mediaPlayer.getCurrentPosition() + 30 * 1000);

        }

        @Override
        public void onRewind() {
            super.onRewind();
            mediaSession.getController().getTransportControls().seekTo(
                    mediaPlayer.getCurrentPosition() - 30 * 1000);
        }

        @Override
        public void onSkipToPrevious() {
            super.onSkipToPrevious();
            if (isMediaPlayerPrepared) {
                if (mediaPlayer.getCurrentPosition() > 1000) {
                    mediaSession.getController().getTransportControls().seekTo(0);
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

    private static final String MY_MEDIA_ROOT_ID = "media_root_id";
    private final String TAG = "ASD";

    private MediaSessionCompat mediaSession;
    private PlaybackStateCompat.Builder stateBuilder;
    private MediaMetadataCompat.Builder metadataBuilder;
    private NotificationCompat.Builder notificationBuilder;

    private final MediaPlayer mediaPlayer = new MediaPlayer();
    private boolean isMediaPlayerPrepared;
    private AudioBook audioBook;
    private Timer updateTask;
    private int positionInTrackList;
    private Notification notification;

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
        if (updateTask != null){
            updateTask.cancel();
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
        playTrack(positionInTrackList);
        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public void onCreate() {
        super.onCreate();
        mediaSession = new MediaSessionCompat(getApplicationContext(), "ASD");
        stateBuilder = new PlaybackStateCompat.Builder()
                .setActions(
                        PlaybackStateCompat.ACTION_PLAY |
                        PlaybackStateCompat.ACTION_PLAY_PAUSE |
                        PlaybackStateCompat.ACTION_STOP |
                        PlaybackStateCompat.ACTION_PAUSE |
                        PlaybackStateCompat.ACTION_SKIP_TO_NEXT |
                        PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS |
                                PlaybackStateCompat.ACTION_FAST_FORWARD |
                                PlaybackStateCompat.ACTION_REWIND |
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
                .putString(MediaMetadataCompat.METADATA_KEY_TITLE, item.toString())
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
        if (updateTask != null) {
            updateTask.cancel();
        }
        mediaPlayer.release();
        mediaSession.release();
    }

    @Override
    public void onPrepared(MediaPlayer mp) {
        isMediaPlayerPrepared = true;
        mediaSession.setMetadata(trackToMetaData(audioBook.files.get(positionInTrackList)));
        mediaSession.getController().getTransportControls().play();
        updateTask = new Timer();
        updateTask.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
            PlaybackStateCompat state = mediaSession.getController().getPlaybackState();
            stateBuilder.setState(state.getState(), mediaPlayer.getCurrentPosition(), 1);
            mediaSession.setPlaybackState(stateBuilder.build());
            }
        }, 0, 1000);
//        task.execute();
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