package com.example.myfirstapp.player;

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
import androidx.annotation.RequiresApi;
import androidx.core.app.NotificationCompat;
import androidx.media.MediaBrowserServiceCompat;
import androidx.media.session.MediaButtonReceiver;

import com.example.myfirstapp.R;
import com.example.myfirstapp.AudioBook;
import com.example.myfirstapp.MediaItem;
import com.example.myfirstapp.Utils;
import com.example.myfirstapp.display.DisplayListActivity;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

public class MediaPlaybackService extends MediaBrowserServiceCompat {

    private class MySessionCallback extends MediaSessionCompat.Callback {

        AudioManager.OnAudioFocusChangeListener oufcl = new AudioManager.OnAudioFocusChangeListener() {
            @Override
            public void onAudioFocusChange(int focusChange) {
                if (focusChange != AudioManager.AUDIOFOCUS_GAIN) {
                    mediaSession.getController().getTransportControls().pause();
                } else {
                    mediaSession.getController().getTransportControls().play();
                }
            }
        };

        private void initializeNotification() {
            String CHANNEL_ID = "com.example.myfirstapp";
            Context context = getApplicationContext();
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                CharSequence name = getString(R.string.channel_name);
                int importance = NotificationManager.IMPORTANCE_LOW;
                NotificationChannel channel = new NotificationChannel(CHANNEL_ID, name, importance);
                channel.setLockscreenVisibility(Notification.VISIBILITY_PUBLIC);
                getSystemService(NotificationManager.class).createNotificationChannel(channel);
            }
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

        @Override
        public void onCustomAction(String action, Bundle extras) {
            super.onCustomAction(action, extras);
            saveProgress();
            mediaSession.sendSessionEvent(action, extras);
            if (EVENT_REACHED_END.equals(action)) {
                onStop();
                MediaPlaybackService.this.stopForeground(true);
                MediaPlaybackService.this.stopSelf();
            }
        }

        private AudioFocusRequest audioFocusRequest;

        @RequiresApi(api = Build.VERSION_CODES.O)
        private AudioFocusRequest getRequest() {
            if (audioFocusRequest == null) {
                audioFocusRequest = new AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN)
                        .setAudioAttributes(getAudioAttributes())
                        .setOnAudioFocusChangeListener(oufcl)
                        .build();
            }
            return audioFocusRequest;
        }

        @Override
        public void onPlay() {
            AudioManager am = (AudioManager) MediaPlaybackService.this.getSystemService(Context.AUDIO_SERVICE);
            int result;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                result = am.requestAudioFocus(getRequest());
            } else {
                result = am.requestAudioFocus(oufcl, AudioAttributes.CONTENT_TYPE_SPEECH, AudioManager.AUDIOFOCUS_GAIN);
            }
            if (result == AudioManager.AUDIOFOCUS_REQUEST_GRANTED) {
                if (isMediaPlayerPrepared && !mediaPlayer.isPlaying()) {
                    mediaSession.setActive(true);
                    if (mAudiobook.getStatus() == AudioBook.STATUS_NOT_BEGUN) {
                        mAudiobook.setStatus(AudioBook.STATUS_IN_PROGRESS);
                        saveProgress();
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
            if (isMediaPlayerPrepared) {
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
            if (isMediaPlayerPrepared) {
                try {
                    if (mediaPlayer.isPlaying()) {
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
                    Utils.getInstance().logError(e, getApplicationContext());
                    e.printStackTrace();
                }
            }
            saveProgress();
        }

        @Override
        public void onStop() {
            if (isMediaPlayerPrepared) {
                AudioManager am = (AudioManager) MediaPlaybackService.this.getSystemService(Context.AUDIO_SERVICE);
                am.abandonAudioFocus(oufcl);
                mediaSession.setActive(false);
                updateTask.cancel();
                PlaybackStateCompat newState = stateBuilder
                        .setState(PlaybackStateCompat.STATE_STOPPED, 0, 1)
                        .build();
                setPlaybackState(newState);
                saveProgress();
                stopForeground(false);
                mAudiobook.saveConfig(getApplicationContext());
                stopSelf();
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
            if (isMediaPlayerPrepared) {
                if (mediaPlayer.getCurrentPosition() > 5 * 1000) {
                    mediaSession.getController().getTransportControls().seekTo(0);
                } else {
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
            if (isMediaPlayerPrepared) {
                positionInTrack = 0;
                setPlaybackState(
                        stateBuilder.setState(PlaybackStateCompat.STATE_SKIPPING_TO_NEXT, positionInTrack, 1)
                                .build());
                playTrack(positionInTrackList + 1);
            }
        }
    }

    @SuppressWarnings("FieldCanBeLocal")
    private static String TAG = "ASD";

    final public static String EVENT_REACHED_END = "REACHED_END";

    private IntentFilter intentFilter = new IntentFilter(AudioManager.ACTION_AUDIO_BECOMING_NOISY);

    private BroadcastReceiver myNoisyAudioStreamReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (AudioManager.ACTION_AUDIO_BECOMING_NOISY.equals(intent.getAction())) {
                mediaSession.getController().getTransportControls().pause();
            }
        }
    };

    private MediaSessionCompat mediaSession;
    private PlaybackStateCompat.Builder stateBuilder;
    private MediaMetadataCompat.Builder metadataBuilder;
    private NotificationCompat.Builder notificationBuilder;

    private final MediaPlayer mediaPlayer = new MediaPlayer();
    private boolean isMediaPlayerPrepared;
    private AudioBook mAudiobook;
    private Timer updateTask;
    private int positionInTrackList;
    private int positionInTrack;
    private Notification notification;

    private AudioAttributes audioAttributes;

    private AudioAttributes getAudioAttributes(){
        if (audioAttributes == null) {
            audioAttributes = new AudioAttributes.Builder()
                    .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                    .build();
        }
        return audioAttributes;
    }


    @SuppressWarnings("SynchronizeOnNonFinalField")
    private void setPlaybackState(PlaybackStateCompat state) {
        synchronized (mediaSession) {
            mediaSession.setPlaybackState(state);
        }
    }

    MediaPlayer.OnCompletionListener ocl = new MediaPlayer.OnCompletionListener() {
        @Override
        public void onCompletion(MediaPlayer mp) {
            int state = mediaSession.getController().getPlaybackState().getState();
            if (state != PlaybackStateCompat.STATE_SKIPPING_TO_NEXT && state != PlaybackStateCompat.STATE_SKIPPING_TO_PREVIOUS) {
                playTrack(positionInTrackList + 1);
            }
        }
    };

    boolean isPlaying() {
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
            mAudiobook.setFinished(getApplicationContext());
            mediaSession.getController().getTransportControls().sendCustomAction(EVENT_REACHED_END, null);
            return;
        }
        positionInTrackList = position;
        mAudiobook.loadFromFile(MediaPlaybackService.this);
        MediaItem mediaItem = mAudiobook.files.get(position);
        mediaPlayer.reset();
        isMediaPlayerPrepared = false;
        if (updateTask != null) {
            updateTask.cancel();
        }
        mediaPlayer.setAudioAttributes(getAudioAttributes());
        try {
            mediaPlayer.setOnCompletionListener(ocl);
            mediaPlayer.setDataSource(getApplicationContext(), Uri.parse(mediaItem.documentUri));
//            LoudnessEnhancer ef = new LoudnessEnhancer(mediaPlayer.getAudioSessionId());
//            ef.setTargetGain(10000);
//            ef.setEnabled(true);
//            mediaPlayer.attachAuxEffect(ef.getId());
//            mediaPlayer.setAuxEffectSendLevel(1f);
            mediaPlayer.prepare();
            isMediaPlayerPrepared = true;
            mediaSession.setMetadata(trackToMetaData(mediaItem));
            mediaSession.getController().getTransportControls().seekTo(positionInTrack);
            saveProgress();
            initialiseTimer();
            mediaSession.getController().getTransportControls().play();
        } catch (IOException e) {
            Utils.getInstance().logError(e, getApplicationContext());
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
                    if (mediaPlayer.isPlaying() && (position =  mediaPlayer.getCurrentPosition()) != 0) {
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
        stopSelf();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null) {
            try {
                 if (updateTask != null) {
                     updateTask.cancel();
                 }

                saveProgress();
                mAudiobook = (AudioBook) intent.getSerializableExtra(PlayActivity.INTENT_AUDIOBOOK);
                mAudiobook.loadFromFile(this);

                int position = intent.getIntExtra(PlayActivity.INTENT_INDEX, 0);

                Intent resumeIntent = new Intent(this, PlayActivity.class);
                resumeIntent.putExtra(DisplayListActivity.INTENT_PLAY_FILE, mAudiobook);
                mediaSession.setSessionActivity(PendingIntent.getActivity(this, 2, resumeIntent, PendingIntent.FLAG_UPDATE_CURRENT));

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
                playTrack(positionInTrackList);

            } catch (Exception e) {
                Utils.getInstance().logError(e, getApplicationContext());
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

        registerReceiver(myNoisyAudioStreamReceiver, intentFilter);
    }

    private MediaMetadataCompat trackToMetaData(MediaItem item) {
        MediaMetadataRetriever mmr = new MediaMetadataRetriever();
        mmr.setDataSource(this, Uri.parse(item.documentUri));
        long duration = Long.parseLong(mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION));
        return metadataBuilder
                .putString(MediaMetadataCompat.METADATA_KEY_TITLE, item.toString())
                .putBitmap(MediaMetadataCompat.METADATA_KEY_ALBUM_ART, mAudiobook.getAlbumArt(this))
                .putLong(MediaMetadataCompat.METADATA_KEY_DURATION, duration)
                .putLong(MediaMetadataCompat.METADATA_KEY_TRACK_NUMBER, mAudiobook.files.indexOf(item))
                .putString(MediaMetadataCompat.METADATA_KEY_MEDIA_ID, mAudiobook.getUniqueId())
                .build();
    }

    private void saveProgress() {
        PlaybackStateCompat state;
        if (mAudiobook != null && (state = mediaSession.getController().getPlaybackState()) != null) {
            int position = (int) state.getPosition();
            mAudiobook.setPositionInTrack(position);
            mAudiobook.setPositionInTrackList(positionInTrackList);
            mAudiobook.saveConfig(this);
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (updateTask != null) {
            updateTask.cancel();
        }
        saveProgress();
        mediaPlayer.release();
        mediaSession.release();
        try {
            unregisterReceiver(myNoisyAudioStreamReceiver);
        } catch (Exception ignored) {}
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

}