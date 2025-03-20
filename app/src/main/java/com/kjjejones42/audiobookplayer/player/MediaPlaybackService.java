package com.kjjejones42.audiobookplayer.player;

import android.app.Notification;
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
import android.os.Bundle;
import android.support.v4.media.MediaBrowserCompat;
import android.support.v4.media.MediaMetadataCompat;
import android.support.v4.media.session.MediaControllerCompat;
import android.support.v4.media.session.MediaSessionCompat;
import android.support.v4.media.session.PlaybackStateCompat;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.media.MediaBrowserServiceCompat;

import com.kjjejones42.audiobookplayer.AudioBook;
import com.kjjejones42.audiobookplayer.MediaItem;
import com.kjjejones42.audiobookplayer.R;
import com.kjjejones42.audiobookplayer.database.AudiobookDao;
import com.kjjejones42.audiobookplayer.database.AudiobookDatabase;
import com.kjjejones42.audiobookplayer.display.DisplayListActivity;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

public class MediaPlaybackService extends MediaBrowserServiceCompat {

    final public static String EVENT_REACHED_END = "EVENT_REACHED_END";
    final private static String EVENT_REWIND = "EVENT_REWIND";
    final private static String EVENT_FAST_FORWARD = "EVENT_FAST_FORWARD";
    final private static String TAG = "ASD";
    private final MediaPlayer mediaPlayer = new MediaPlayer();

    private PlayerNotificationManager notificationManager;
    private PlaybackStateCompat.Builder stateBuilder;
    private MediaMetadataCompat.Builder metadataBuilder;
    private MediaSessionCompat mediaSession;
    private AudioFocusRequest audioFocusRequest;
    private AudioAttributes audioAttributes;
    private String bookId;
    private Timer updateTask;
    private boolean isMediaPlayerPrepared;
    private int intentId;
    private final AudioManager.OnAudioFocusChangeListener onAudioFocusChangeListener = new AudioManager.OnAudioFocusChangeListener() {

        int lastKnownAudioFocusState;
        boolean wasPlayingWhenTransientLoss;

        @Override
        public void onAudioFocusChange(int focusChange) {
            MediaControllerCompat.TransportControls controls = mediaSession.getController().getTransportControls();
            switch (focusChange) {
                case AudioManager.AUDIOFOCUS_GAIN:
                case AudioManager.AUDIOFOCUS_GAIN_TRANSIENT:
                case AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_EXCLUSIVE:
                case AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK:
                    switch(lastKnownAudioFocusState) {
                        case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK:
                        case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT:
                            if(wasPlayingWhenTransientLoss) {
                                controls.play();
                            }
                            break;
                        default:
                            controls.play();
                    }
                    break;
                case AudioManager.AUDIOFOCUS_LOSS:
                    controls.pause();
                    break;
                case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT:
                case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK:
                    wasPlayingWhenTransientLoss = mediaPlayer.isPlaying();
                    controls.pause();
                    break;
            }
            lastKnownAudioFocusState = focusChange;
        }
    };

    private AudiobookDao dao;

    private final IntentFilter intentFilter = new IntentFilter(AudioManager.ACTION_AUDIO_BECOMING_NOISY);
    private boolean hasBeenInterrupted;
    private final BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (AudioManager.ACTION_AUDIO_BECOMING_NOISY.equals(intent.getAction())) {
                mediaSession.getController().getTransportControls().pause();
                hasBeenInterrupted = true;
            }
        }
    };

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        intentId = startId;
        if (intent != null) {
            try {
                if (updateTask != null) {
                    updateTask.cancel();
                }

                int positionInTrackList = intent.getIntExtra(PlayActivity.INTENT_INDEX, 0);
                bookId = intent.getSerializableExtra(PlayActivity.INTENT_AUDIOBOOK, String.class);
                assert bookId != null;

                Intent resumeIntent = new Intent(this, PlayActivity.class);
                resumeIntent.putExtra(DisplayListActivity.INTENT_PLAY_FILE, bookId);
                mediaSession.setSessionActivity(PendingIntent.getActivity(this, 2, resumeIntent, PendingIntent.FLAG_IMMUTABLE));
                int positionInTrack = getDao().getPositionInTrack(bookId);
                if (getDao().getStatus(bookId) == AudioBook.STATUS_FINISHED) {
                    updateStatus(AudioBook.STATUS_IN_PROGRESS);
                } else {
                    if (positionInTrackList != getDao().getPositionInTrackList(bookId))  {
                        positionInTrack = 1;
                        getDao().updatePositionInTrack(bookId, 1, System.currentTimeMillis());
                    }
                }
                setPlaybackState(stateBuilder.setState(PlaybackStateCompat.STATE_PLAYING, positionInTrack, 1).build());
                playTrack(positionInTrackList);

            } catch (Exception e) {
                onError();
                throw e;
            }
        }
        return super.onStartCommand(intent, flags, startId);
    }    private final MediaPlayer.OnCompletionListener onCompletionListener = new MediaPlayer.OnCompletionListener() {
        @Override
        public void onCompletion(MediaPlayer mp) {
            int state = mediaSession.getController().getPlaybackState().getState();
            if (state != PlaybackStateCompat.STATE_SKIPPING_TO_NEXT && state != PlaybackStateCompat.STATE_SKIPPING_TO_PREVIOUS) {
                mediaSession.getController().getTransportControls().pause();
                if (updateTask != null) {
                    updateTask.cancel();
                }
                getDao().updatePositionInTrack(bookId, 1, System.currentTimeMillis());
                playTrack(getDao().getPositionInTrackList(bookId) + 1);
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

    private AudiobookDao getDao() {
        if (dao == null) {
            dao = AudiobookDatabase.getInstance(this).audiobookDao();
        }
        return dao;
    }

    private boolean isPlaying() {
        if (mediaSession == null || mediaSession.getController().getPlaybackState() == null) {
            return false;
        }
        return mediaSession.getController().getPlaybackState().getState() == PlaybackStateCompat.STATE_PLAYING;
    }

    private void updateStatus(int status) {
        AudioBook book = getDao().findByName(bookId);
        book.setStatus(status);
        getDao().update(book);
    }

    @SuppressWarnings("SynchronizeOnNonFinalField")
    private void setPlaybackState(PlaybackStateCompat state) {
        synchronized (mediaSession) {
            int positionInTrack = (int) state.getPosition();
            if (positionInTrack != 0) {
                getDao().updatePositionInTrack(bookId, (int) state.getPosition(), System.currentTimeMillis());
            }
            mediaSession.setPlaybackState(state);
        }
    }

    private void playTrack(int positionInTrackList) {
        if (positionInTrackList < 0) {
            return;
        }
        List<MediaItem> files = getDao().findByName(bookId).files;
        if (positionInTrackList >= files.size()) {
            updateStatus(AudioBook.STATUS_FINISHED);
            mediaSession.getController().getTransportControls().sendCustomAction(EVENT_REACHED_END, null);
            return;
        }
        getDao().updatePositionInTrackList(bookId, positionInTrackList);
        MediaItem mediaItem = files.get(positionInTrackList);
        mediaPlayer.reset();
        isMediaPlayerPrepared = false;
        if (updateTask != null) {
            updateTask.cancel();
        }
        mediaPlayer.setAudioAttributes(getAudioAttributes());
        try {
            mediaPlayer.setOnCompletionListener(onCompletionListener);
            mediaPlayer.setDataSource(this, mediaItem.getUri());
            mediaPlayer.prepare();
            isMediaPlayerPrepared = true;
            mediaSession.setMetadata(trackToMetaData(mediaItem));
            mediaSession.getController().getTransportControls().seekTo(getDao().getPositionInTrack(bookId));
            mediaSession.getController().getTransportControls().play();
        } catch (IOException e) {
            onError();
            throw new RuntimeException(e);
        }
    }

    private void onError() {
        stateBuilder.setState(PlaybackStateCompat.STATE_ERROR, 0, 1);
        setPlaybackState(stateBuilder.build());
        stopSelf(intentId);
    }

    private MediaMetadataCompat trackToMetaData(MediaItem item) {
        try (MediaMetadataRetriever mmr = new MediaMetadataRetriever()) {
            mmr.setDataSource(this, item.getUri());
                String durationStr = mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION);
                assert durationStr != null;
                long duration = Long.parseLong(durationStr);
                AudioBook book = getDao().findByName(bookId);
                metadataBuilder
                    .putString(MediaMetadataCompat.METADATA_KEY_ALBUM, item.toString())
                    .putString(MediaMetadataCompat.METADATA_KEY_TITLE, book.displayName)
                    .putBitmap(MediaMetadataCompat.METADATA_KEY_ALBUM_ART, book.getAlbumArt(this))
                    .putLong(MediaMetadataCompat.METADATA_KEY_DURATION, duration)
                    .putLong(MediaMetadataCompat.METADATA_KEY_TRACK_NUMBER, book.files.indexOf(item))
                    .putString(MediaMetadataCompat.METADATA_KEY_MEDIA_ID, book.getUniqueId());
            return metadataBuilder.build();
        } catch (IOException ignored) {
            return null;
        }
    }

    private void initialiseTimer() {
        if (updateTask != null) {
            updateTask.cancel();
        }
        updateTask = new Timer();
        updateTask.schedule(new TimerTask() {
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

    @Override
    public void onCreate() {
        super.onCreate();
        mediaSession = new MediaSessionCompat(getApplicationContext(), TAG);

        notificationManager = new PlayerNotificationManager(mediaSession, this);

        stateBuilder = new PlaybackStateCompat.Builder()
                .addCustomAction(EVENT_REWIND, "Rewind", R.drawable.ic_replay_30)
                .addCustomAction(EVENT_FAST_FORWARD, "Forward", R.drawable.ic_forward_30)
                .setActions(
                        PlaybackStateCompat.ACTION_PLAY |
                                PlaybackStateCompat.ACTION_PLAY_PAUSE |
                                PlaybackStateCompat.ACTION_STOP |
                                PlaybackStateCompat.ACTION_PAUSE |
                                PlaybackStateCompat.ACTION_FAST_FORWARD |
                                PlaybackStateCompat.ACTION_REWIND |
                                PlaybackStateCompat.ACTION_SEEK_TO
                );

        metadataBuilder = new MediaMetadataCompat.Builder();
        mediaSession.setMetadata(metadataBuilder.build());

        mediaSession.setCallback(new MySessionCallback());
        setSessionToken(mediaSession.getSessionToken());

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

    private void updateNotification() {
        Notification notification = notificationManager.updateNotification(isPlaying(), bookId);
        startForeground(2, notification);
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
            mediaSession.sendSessionEvent(action, extras);
            switch (action) {
                case EVENT_REACHED_END: {
                    onStop();
                    break;
                }
                case EVENT_REWIND: {
                    onRewind();
                    break;
                }
                case EVENT_FAST_FORWARD: {
                    onFastForward();
                    break;
                }
            }
        }

        @Override
        public void onPlay() {
            if (hasBeenInterrupted) {
                hasBeenInterrupted = false;
                return;
            }
            registerReceiver(broadcastReceiver, intentFilter);
            AudioManager am = (AudioManager) MediaPlaybackService.this.getSystemService(Context.AUDIO_SERVICE);
            if (getIsMediaPlayerPrepared() && !isMediaPlayerPlaying()) {
                int result;
                result = am.requestAudioFocus(getRequest());
                if (result == AudioManager.AUDIOFOCUS_REQUEST_GRANTED) {
                    mediaSession.setActive(true);
                    if (getDao().getStatus(bookId) == AudioBook.STATUS_NOT_BEGUN) {
                        updateStatus(AudioBook.STATUS_IN_PROGRESS);
                    }
                    PlaybackStateCompat newState = stateBuilder
                            .setState(PlaybackStateCompat.STATE_PLAYING, getDao().getPositionInTrack(bookId), 1)
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
                if (pos < 1) {
                    pos = 1;
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
            if (getIsMediaPlayerPrepared() && isMediaPlayerPlaying()) {
                PlaybackStateCompat newState = stateBuilder
                        .setState(PlaybackStateCompat.STATE_PAUSED, mediaPlayer.getCurrentPosition(), 1)
                        .build();
                setPlaybackState(newState);
                mediaPlayer.pause();
                updateNotification();
            }
        }

        @Override
        public void onStop() {
            unregisterReceiver(broadcastReceiver);
            if (getIsMediaPlayerPrepared()) {
                AudioManager am = (AudioManager) MediaPlaybackService.this.getSystemService(Context.AUDIO_SERVICE);
                am.abandonAudioFocusRequest(audioFocusRequest);
                mediaSession.setActive(false);
                updateTask.cancel();
                PlaybackStateCompat newState = stateBuilder
                        .setState(PlaybackStateCompat.STATE_STOPPED, 0, 1)
                        .build();
                setPlaybackState(newState);
                stopForeground(STOP_FOREGROUND_REMOVE);
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
        public void onSkipToNext() {
            super.onSkipToNext();
            if (getIsMediaPlayerPrepared()) {
                onPause();
                getDao().updatePositionInTrack(bookId, 1, System.currentTimeMillis());
                setPlaybackState(
                        stateBuilder.setState(PlaybackStateCompat.STATE_SKIPPING_TO_NEXT, 1, 1)
                                .build());
                playTrack(getDao().getPositionInTrackList(bookId)+ 1);
            }
        }

        @Override
        public void onSkipToPrevious() {
            super.onSkipToPrevious();
            if (getIsMediaPlayerPrepared()) {
                if (mediaPlayer.getCurrentPosition() > 5 * 1000) {
                    mediaSession.getController().getTransportControls().seekTo(1);
                } else {
                    onPause();
                    getDao().updatePositionInTrack(bookId, 1, System.currentTimeMillis());
                    setPlaybackState(stateBuilder
                            .setState(PlaybackStateCompat.STATE_SKIPPING_TO_PREVIOUS, 1, 1)
                            .build());
                    playTrack(getDao().getPositionInTrackList(bookId) - 1);
                }
            }
        }
    }


}
