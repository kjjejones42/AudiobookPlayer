package com.kjjejones42.audiobookplayer.player;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.media.AudioAttributes;
import android.media.AudioFocusRequest;
import android.media.AudioManager;
import android.media.MediaMetadataRetriever;
import android.media.MediaPlayer;
import android.net.Uri;
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

import com.kjjejones42.audiobookplayer.AudioBook;
import com.kjjejones42.audiobookplayer.MediaItem;
import com.kjjejones42.audiobookplayer.R;
import com.kjjejones42.audiobookplayer.Utils;
import com.kjjejones42.audiobookplayer.display.DisplayListActivity;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

public class MediaPlaybackService extends MediaBrowserServiceCompat {

    final public static String EVENT_REACHED_END = "REACHED_END";
    final private static String TAG = "ASD";
    private final MediaPlayer mediaPlayer = new MediaPlayer();
    private PlaybackStateCompat.Builder stateBuilder;
    private MediaMetadataCompat.Builder metadataBuilder;
    private NotificationCompat.Builder notificationBuilder;
    private MediaSessionCompat mediaSession;
    private AudioFocusRequest audioFocusRequest;
    private AudioAttributes audioAttributes;
    private Notification notification;
    private AudioBook mAudiobook;
    private Timer updateTask;
    private int positionInTrackList;
    private int positionInTrack;
    private boolean isMediaPlayerPrepared;
    private int intentId;

    private final IntentFilter intentFilter = new IntentFilter(AudioManager.ACTION_AUDIO_BECOMING_NOISY);
    private final BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (AudioManager.ACTION_AUDIO_BECOMING_NOISY.equals(intent.getAction())) {
                mediaSession.getController().getTransportControls().pause();
            }
        }
    };
    private final AudioManager.OnAudioFocusChangeListener onAudioFocusChangeListener = new AudioManager.OnAudioFocusChangeListener() {
        @Override
        public void onAudioFocusChange(int focusChange) {
            if (focusChange != AudioManager.AUDIOFOCUS_GAIN) {
                mediaSession.getController().getTransportControls().pause();
            } else {
                mediaSession.getController().getTransportControls().play();
            }
        }
    };
    private final MediaPlayer.OnCompletionListener onCompletionListener = new MediaPlayer.OnCompletionListener() {
        @Override
        public void onCompletion(MediaPlayer mp) {
            int state = mediaSession.getController().getPlaybackState().getState();
            if (state != PlaybackStateCompat.STATE_SKIPPING_TO_NEXT && state != PlaybackStateCompat.STATE_SKIPPING_TO_PREVIOUS) {
                mediaSession.getController().getTransportControls().pause();
                if (updateTask != null) {
                    updateTask.cancel();
                }
                mAudiobook.setPositionInTrack(1);
                mAudiobook.saveConfig(MediaPlaybackService.this);
                playTrack(positionInTrackList + 1);
            }
        }
    };

    private AudioFocusRequest getRequest() {
        if (audioFocusRequest == null) {
            audioFocusRequest = new AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN)
                    .setAudioAttributes(getAudioAttributes())
                    .setOnAudioFocusChangeListener(onAudioFocusChangeListener)
                    .build();
        }
        return audioFocusRequest;
    }

    private void initializeNotification() {
        String CHANNEL_ID = "com.example.myfirstapp";
        Context context = getApplicationContext();
        CharSequence name = getString(R.string.channel_name);
        int importance = NotificationManager.IMPORTANCE_LOW;
        NotificationChannel channel = new NotificationChannel(CHANNEL_ID, name, importance);
        channel.setLockscreenVisibility(Notification.VISIBILITY_PUBLIC);
        getSystemService(NotificationManager.class).createNotificationChannel(channel);
        notificationBuilder = new NotificationCompat.Builder(context, CHANNEL_ID);
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
        if (notificationBuilder == null) {
            initializeNotification();
        }
        MediaDescriptionCompat description = controller.getMetadata().getDescription();

        notificationBuilder
                .setContentText(description.getTitle())
                .setLargeIcon(description.getIconBitmap())
                .setOngoing(playing);

        if (mAudiobook != null) {
            notificationBuilder.setContentTitle(mAudiobook.displayName);
        }

        notification = notificationBuilder.build();
        notification.actions[2] = new Notification.Action(
                playPauseIcon, playPauseText,
                MediaButtonReceiver.buildMediaButtonPendingIntent(context,
                        PlaybackStateCompat.ACTION_PLAY_PAUSE));
        startForeground(2, notification);
    }

    private AudioAttributes getAudioAttributes() {
        if (audioAttributes == null) {
            audioAttributes = new AudioAttributes.Builder()
                    .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                    .build();
        }
        return audioAttributes;
    }

    private boolean getIsMediaPlayerPrepared() {
        return (mediaSession.getController().getPlaybackState() != null) && isMediaPlayerPrepared;
    }

    private boolean isMediaPlayerPlaying() {
        try {
            return mediaPlayer.isPlaying();
        } catch (IllegalStateException e) {
            return false;
        }
    }

    @SuppressWarnings("SynchronizeOnNonFinalField")
    private void setPlaybackState(PlaybackStateCompat state) {
        synchronized (mediaSession) {
            mediaSession.setPlaybackState(state);
        }
    }

    private boolean isPlaying() {
        if (mediaSession == null || mediaSession.getController().getPlaybackState() == null) {
            return false;
        }
        return mediaSession.getController().getPlaybackState().getState() == PlaybackStateCompat.STATE_PLAYING;
    }

    private void playTrack(int position) {
        if (position < 0) {
            return;
        }
        if (position >= mAudiobook.files.size()) {
            mAudiobook.setStatus(AudioBook.STATUS_FINISHED);
            mAudiobook.saveConfig(this);
            mediaSession.getController().getTransportControls().sendCustomAction(EVENT_REACHED_END, null);
            return;
        }
        positionInTrackList = position;
        mAudiobook.loadFromFile(this);
        positionInTrack = mAudiobook.getPositionInTrack();
        MediaItem mediaItem = mAudiobook.files.get(position);
        mediaPlayer.reset();
        isMediaPlayerPrepared = false;
        if (updateTask != null) {
            updateTask.cancel();
        }
        mediaPlayer.setAudioAttributes(getAudioAttributes());
        try {
            mediaPlayer.setOnCompletionListener(onCompletionListener);
            mediaPlayer.setDataSource(this, Uri.parse(mediaItem.filePath));
            mediaPlayer.prepare();
            isMediaPlayerPrepared = true;
            mediaSession.setMetadata(trackToMetaData(mediaItem));
            mediaSession.getController().getTransportControls().seekTo(positionInTrack);
            saveAudiobookProgress();
            mediaSession.getController().getTransportControls().play();
        } catch (IOException e) {
            Utils.logError(e, mediaItem.filePath ,getApplicationContext());
            e.printStackTrace();
            onError();
        }
    }

    private void initialiseTimer() {
        if (updateTask != null) {
            updateTask.cancel();
        }
        updateTask = new Timer();
        updateTask.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                try {
                    int position;
                    if (isMediaPlayerPlaying() && (position = mediaPlayer.getCurrentPosition()) != 0) {
                        PlaybackStateCompat state = mediaSession.getController().getPlaybackState();
                        stateBuilder.setState(state.getState(), position, 1);
                        setPlaybackState(stateBuilder.build());
                    }
                } catch (Exception ignored) {
                }
            }
        }, 0, 1000);

    }

    private void onError() {
        stateBuilder.setState(PlaybackStateCompat.STATE_ERROR, 0, 1);
        setPlaybackState(stateBuilder.build());
        stopSelf(intentId);
    }

    private MediaMetadataCompat trackToMetaData(MediaItem item) {
        MediaMetadataRetriever mmr = new MediaMetadataRetriever();
        mmr.setDataSource(this, Uri.parse(item.filePath));
        String durationStr = mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION);
        assert durationStr != null;
        long duration = Long.parseLong(durationStr);
        return metadataBuilder
                .putString(MediaMetadataCompat.METADATA_KEY_TITLE, item.toString())
                .putBitmap(MediaMetadataCompat.METADATA_KEY_ALBUM_ART, mAudiobook.getAlbumArt())
                .putLong(MediaMetadataCompat.METADATA_KEY_DURATION, duration)
                .putLong(MediaMetadataCompat.METADATA_KEY_TRACK_NUMBER, mAudiobook.files.indexOf(item))
                .putString(MediaMetadataCompat.METADATA_KEY_MEDIA_ID, mAudiobook.getUniqueId())
                .build();
    }

    private void saveAudiobookProgress() {
        PlaybackStateCompat state;
        if (mAudiobook != null && (state = mediaSession.getController().getPlaybackState()) != null) {
            int position = (int) state.getPosition();
            mAudiobook.setPositionInTrack(position);
            mAudiobook.setPositionInTrackList(positionInTrackList);
            mAudiobook.saveConfig(this);
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        intentId = startId;
        if (intent != null) {
            try {
                if (updateTask != null) {
                    updateTask.cancel();
                }

                saveAudiobookProgress();
                mAudiobook = (AudioBook) intent.getSerializableExtra(PlayActivity.INTENT_AUDIOBOOK);
                assert mAudiobook != null;
                mAudiobook.loadFromFile(this);

                int position = intent.getIntExtra(PlayActivity.INTENT_INDEX, 0);

                Intent resumeIntent = new Intent(this, PlayActivity.class);
                resumeIntent.putExtra(DisplayListActivity.INTENT_PLAY_FILE, mAudiobook);
                mediaSession.setSessionActivity(PendingIntent.getActivity(this, 2, resumeIntent, PendingIntent.FLAG_IMMUTABLE));

                if (mAudiobook.getStatus() == AudioBook.STATUS_FINISHED) {
                    positionInTrackList = 0;
                    positionInTrack = 0;
                    mAudiobook.setStatus(AudioBook.STATUS_IN_PROGRESS);
                    mAudiobook.saveConfig(this);
                } else {
                    positionInTrackList = position;
                    if (positionInTrackList == mAudiobook.getPositionInTrackList()) {
                        positionInTrack = mAudiobook.getPositionInTrack();
                    } else {
                        positionInTrack = 0;
                    }
                }
                setPlaybackState(stateBuilder.setState(PlaybackStateCompat.STATE_PLAYING, positionInTrack, 1).build());
                playTrack(positionInTrackList);

            } catch (Exception e) {
                Utils.logError(e, getApplicationContext());
                e.printStackTrace();
                onError();
            }
        }
        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public void onCreate() {
        super.onCreate();
        mediaSession = new MediaSessionCompat(getApplicationContext(), TAG);

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
                                PlaybackStateCompat.ACTION_SEEK_TO
                );

        metadataBuilder = new MediaMetadataCompat.Builder();
        mediaSession.setMetadata(metadataBuilder.build());

        mediaSession.setCallback(new MySessionCallback());
        setSessionToken(mediaSession.getSessionToken());

        registerReceiver(broadcastReceiver, intentFilter);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (updateTask != null) {
            updateTask.cancel();
        }
        mediaPlayer.release();
        mediaSession.release();
        try {
            unregisterReceiver(broadcastReceiver);
        } catch (Exception ignored) {
        }
    }

    @Nullable
    @Override
    public BrowserRoot onGetRoot(@NonNull String clientPackageName, int clientUid, @Nullable Bundle rootHints) {
        return new BrowserRoot("", null);
    }

    @Override
    public void onLoadChildren(@NonNull String parentId, @NonNull Result<List<MediaBrowserCompat.MediaItem>> result) {
        result.sendResult(new ArrayList<>());
    }

    private class MySessionCallback extends MediaSessionCompat.Callback {

        @Override
        public void onCustomAction(String action, Bundle extras) {
            super.onCustomAction(action, extras);
            saveAudiobookProgress();
            mediaSession.sendSessionEvent(action, extras);
            if (EVENT_REACHED_END.equals(action)) {
                MediaPlaybackService.this.stopForeground(true);
                onStop();
            }
        }

        @Override
        public void onPlay() {
            AudioManager am = (AudioManager) MediaPlaybackService.this.getSystemService(Context.AUDIO_SERVICE);
            if (getIsMediaPlayerPrepared() && !isMediaPlayerPlaying()) {
                int result;
                result = am.requestAudioFocus(getRequest());
                if (result == AudioManager.AUDIOFOCUS_REQUEST_GRANTED) {
                    mediaSession.setActive(true);
                    if (mAudiobook.getStatus() == AudioBook.STATUS_NOT_BEGUN) {
                        mAudiobook.setStatus(AudioBook.STATUS_IN_PROGRESS);
                        saveAudiobookProgress();
                    }
                    PlaybackStateCompat newState = stateBuilder
                            .setState(PlaybackStateCompat.STATE_PLAYING, mAudiobook.getPositionInTrack(), 1)
                            .build();
                    setPlaybackState(newState);
                    mediaPlayer.start();
                    initialiseTimer();
                }
                updateNotification();
            }
        }

        @Override
        public void onSeekTo(long pos) {
            if (getIsMediaPlayerPrepared()) {
                if (pos < 0) {
                    pos = 0;
                } else {
                    int duration = mediaPlayer.getDuration();
                    if (pos > duration) {
                        pos = duration - 1;
                    }
                }
                setPlaybackState(
                        stateBuilder
                                .setState(mediaSession.getController().getPlaybackState().getState(), pos, 1)
                                .build()
                );
                mediaPlayer.seekTo((int) pos);
            }
        }

        @Override
        public void onPause() {
            if (getIsMediaPlayerPrepared()) {
                try {
                    if (isMediaPlayerPlaying()) {
                        PlaybackStateCompat newState = stateBuilder
                                .setState(PlaybackStateCompat.STATE_PAUSED, mediaPlayer.getCurrentPosition(), 1)
                                .build();
                        setPlaybackState(newState);
                        mediaPlayer.pause();
                        updateNotification();
                        ((NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE)).notify(2, notification);
                        stopForeground(false);
                    }
                } catch (IllegalStateException e) {
                    Utils.logError(e, getApplicationContext());
                    e.printStackTrace();
                }
            }
            saveAudiobookProgress();
        }

        @Override
        public void onStop() {
            if (getIsMediaPlayerPrepared()) {
                AudioManager am = (AudioManager) MediaPlaybackService.this.getSystemService(Context.AUDIO_SERVICE);
                am.abandonAudioFocusRequest(audioFocusRequest);
                mediaSession.setActive(false);
                updateTask.cancel();
                saveAudiobookProgress();
                PlaybackStateCompat newState = stateBuilder
                        .setState(PlaybackStateCompat.STATE_STOPPED, 0, 1)
                        .build();
                setPlaybackState(newState);
                stopForeground(false);
                stopSelf(intentId);
            }
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
            if (getIsMediaPlayerPrepared()) {
                if (mediaPlayer.getCurrentPosition() > 5 * 1000) {
                    mediaSession.getController().getTransportControls().seekTo(0);
                } else {
                    onPause();
                    setPlaybackState(stateBuilder
                            .setState(PlaybackStateCompat.STATE_SKIPPING_TO_PREVIOUS, 0, 1)
                            .build());
                    playTrack(positionInTrackList - 1);
                }
            }
        }

        @Override
        public void onSkipToNext() {
            super.onSkipToNext();
            if (getIsMediaPlayerPrepared()) {
                onPause();
                positionInTrack = 0;
                setPlaybackState(
                        stateBuilder.setState(PlaybackStateCompat.STATE_SKIPPING_TO_NEXT, positionInTrack, 1)
                                .build());
                playTrack(positionInTrackList + 1);
            }
        }
    }
}