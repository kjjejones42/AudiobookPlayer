package com.example.myfirstapp;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import android.annotation.SuppressLint;
import android.content.ComponentName;
import android.content.Intent;
import android.graphics.Bitmap;
import android.media.AudioAttributes;
import android.media.AudioManager;
import android.media.MediaMetadataRetriever;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.PersistableBundle;
import android.os.RemoteException;
import android.support.v4.media.MediaBrowserCompat;
import android.support.v4.media.MediaMetadataCompat;
import android.support.v4.media.session.MediaControllerCompat;
import android.support.v4.media.session.MediaSessionCompat;
import android.support.v4.media.session.PlaybackStateCompat;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.Spinner;
import android.widget.TextView;

import java.io.IOException;
import java.io.Serializable;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

public class PlayActivity extends AppCompatActivity implements SeekBar.OnSeekBarChangeListener, AdapterView.OnItemSelectedListener {

    private Spinner spinner;
    private ImageButton prevButton;
    private ImageButton toggleButton;
    private ImageButton nextButton;
    private TextView progressText;
    private TextView durationText;
    private MediaBrowserCompat mediaBrowser;
    private MediaControllerCompat controller;
    private AudioBook audioBook;
    private int positionInTrackList = 0;
    private SeekBar seekBar;

    public void play() {
        if (controller != null) {
            int pbState = controller.getPlaybackState().getState();
            if (pbState != PlaybackStateCompat.STATE_PLAYING) {
                controller.getTransportControls().play();
            }
            getMetaData(controller.getMetadata().getDescription().getMediaUri());
        }
        ImageButton button = findViewById(R.id.toggleButton);
        button.setImageResource(R.drawable.ic_pause_white_24dp);
    }

    private String msToMMSS(long ms){
        long minutes = ms /(1000 * 60);
        long seconds = ms / 1000 % 60;
        return String.format(Locale.getDefault(), "%02d:%02d", minutes, seconds);
    }

    private long getDuration() {
        if (controller != null) {
            return controller.getMetadata().getLong(MediaMetadataCompat.METADATA_KEY_DURATION);
        }
        return 0;
    }

    private void getMetaData(Uri uri) {
        if (uri == null) return;
        MediaMetadataRetriever mmr =  new MediaMetadataRetriever();
        mmr.setDataSource(getApplicationContext(), uri);
        List<String> list = Arrays.asList("METADATA_KEY_CD_TRACK_NUMBER", "METADATA_KEY_ALBUM", "METADATA_KEY_ARTIST", "METADATA_KEY_AUTHOR", "METADATA_KEY_COMPOSER", "METADATA_KEY_DATE", "METADATA_KEY_GENRE", "METADATA_KEY_TITLE", "METADATA_KEY_YEAR", "METADATA_KEY_DURATION", "METADATA_KEY_NUM_TRACKS", "METADATA_KEY_WRITER", "METADATA_KEY_MIMETYPE", "METADATA_KEY_ALBUMARTIST", "METADATA_KEY_DISC_NUMBER", "METADATA_KEY_COMPILATION", "METADATA_KEY_HAS_AUDIO", "METADATA_KEY_HAS_VIDEO", "METADATA_KEY_VIDEO_WIDTH", "METADATA_KEY_VIDEO_HEIGHT", "METADATA_KEY_BITRATE", "METADATA_KEY_TIMED_TEXT_LANGUAGES", "METADATA_KEY_IS_DRM", "METADATA_KEY_LOCATION", "METADATA_KEY_VIDEO_ROTATION", "METADATA_KEY_CAPTURE_FRAMERATE", "METADATA_KEY_HAS_IMAGE", "METADATA_KEY_IMAGE_COUNT", "METADATA_KEY_IMAGE_PRIMARY", "METADATA_KEY_IMAGE_WIDTH", "METADATA_KEY_IMAGE_HEIGHT", "METADATA_KEY_IMAGE_ROTATION", "METADATA_KEY_VIDEO_FRAME_COUNT", "METADATA_KEY_EXIF_OFFSET", "METADATA_KEY_EXIF_LENGTH");
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < list.size(); i++) {
            try {
                String meta;
                if ((meta = mmr.extractMetadata(i)) != null) {
                    sb.append(list.get(i)).append(" | ").append(meta).append(System.lineSeparator());
                }
            } catch (Exception e){
                e.printStackTrace();
            }
        }
        TextView text = findViewById(R.id.songInfo);
        text.setText(sb.toString());
    }

    private void playTrack(int position) {
        try {
            if (spinner != null){
                spinner.setSelection(position);
            }
            positionInTrackList = position;
            Intent intent = new Intent(this, MediaPlaybackService.class);
            intent.putExtra("AUDIOBOOK", audioBook);
            intent.putExtra("INDEX", position);
            startService(intent);
            updateMetadata(position);
            play();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    public void updateSeekBar(long position) {
        seekBar.setMax((int) getDuration());
        seekBar.setProgress((int) position);
        progressText.setText(msToMMSS(position));
    }

    @SuppressLint("StaticFieldLeak")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mediaBrowser = new MediaBrowserCompat(this,
                new ComponentName(this, MediaPlaybackService.class),
                connectionCallbacks,
                null);

        if (savedInstanceState != null) {
            positionInTrackList = savedInstanceState.getInt("TRACK_LIST_PROGRESS");
        }
        setContentView(R.layout.activity_play);

        prevButton = findViewById(R.id.prevButton);
        toggleButton = findViewById(R.id.toggleButton);
        nextButton = findViewById(R.id.nextButton);
        progressText = findViewById(R.id.progress_text);
        durationText = findViewById(R.id.duration_text);
        seekBar = findViewById(R.id.seekBar);
        spinner = findViewById(R.id.trackChooser);

        audioBook = (AudioBook) getIntent().getSerializableExtra(DisplayListActivity.PLAY_FILE);
        for (MediaItem track : audioBook.files) {
            track.generateTitle(this);
        }
        Objects.requireNonNull(getSupportActionBar()).setTitle(audioBook.displayName);

        seekBar.setOnSeekBarChangeListener(this);

        ArrayAdapter<MediaItem> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, audioBook.files);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(adapter);
        spinner.setSelection(0,false);
        spinner.setOnItemSelectedListener(this);
        playTrack(positionInTrackList);

        updateMetadata(positionInTrackList);
   }

    @Override
    public void onSaveInstanceState(Bundle outState, PersistableBundle outPersistentState) {
        outState.putInt("TRACK_LIST_PROGRESS", positionInTrackList);
        super.onSaveInstanceState(outState, outPersistentState);
    }

    @Override
    public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
        if (fromUser) {
            if (controller != null) {
                controller.getTransportControls().seekTo(progress);
            }
            progressText.setText(msToMMSS(progress));
        }
    }

    @Override
    public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
        if ((Integer) spinner.getTag() != position) {
            playTrack(position);
        }
    }

    @Override
    public void onStartTrackingTouch(SeekBar seekBar) {}

    @Override
    public void onStopTrackingTouch(SeekBar seekBar) {}

    @Override
    public void onNothingSelected(AdapterView<?> parent) {}

    @Override
    protected void onStart() {
        super.onStart();
        mediaBrowser.connect();
    }

    @Override
    protected void onResume() {
        super.onResume();
        setVolumeControlStream(AudioManager.STREAM_MUSIC);
    }

    @Override
    protected void onStop() {
        super.onStop();
        // (see "stay in sync with the MediaSession")
        if (controller != null) {
            controller.unregisterCallback(controllerCallback);
        }
        mediaBrowser.disconnect();
    }

    void updateMetadata(int position){
        ImageView imView = findViewById(R.id.albumArtView);
        imView.setImageBitmap(audioBook.getAlbumArt(this));
        getMetaData(Uri.parse(audioBook.files.get(position).documentUri));
        spinner.setTag(position);
        spinner.setSelection(position);
        if (controller != null) {
            durationText.setText(msToMMSS(controller.getMetadata().getLong(MediaMetadataCompat.METADATA_KEY_DURATION)));
        }
    }

    void buildTransportControls() {
        controller.registerCallback(controllerCallback);
        toggleButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                int pbState = controller.getPlaybackState().getState();
                if (pbState == PlaybackStateCompat.STATE_PLAYING) {
                    controller.getTransportControls().pause();
                } else {
                    controller.getTransportControls().play();
                }
            }});
        nextButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                controller.getTransportControls().skipToNext();
            }
        });
        prevButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                controller.getTransportControls().skipToPrevious();
            }
        });
    }


    private final MediaBrowserCompat.ConnectionCallback connectionCallbacks =
            new MediaBrowserCompat.ConnectionCallback() {
                @Override
                public void onConnected() {
                    MediaControllerCompat mediaController = null;
                    try {
                        mediaController = new MediaControllerCompat(PlayActivity.this,
                                mediaBrowser.getSessionToken());
                        MediaControllerCompat.setMediaController(PlayActivity.this, mediaController);
                        controller = mediaController;
                        updateMetadata(positionInTrackList);
                        buildTransportControls();
                        play();
                    } catch (RemoteException e) {
                        e.printStackTrace();
                    }
                }

                @Override
                public void onConnectionSuspended() {
                    // The Service has crashed. Disable transport controls until it automatically reconnects
                }

                @Override
                public void onConnectionFailed() {
                    // The Service has refused our connection
                }
            };

    MediaControllerCompat.Callback controllerCallback = new MediaControllerCompat.Callback() {
        @Override
        public void onMetadataChanged(MediaMetadataCompat metadata) {
            int position = (int) metadata.getLong("AUDIOBOOK_ID");
            updateMetadata(position);
        }

        @Override
        public void onPlaybackStateChanged(PlaybackStateCompat state) {
            updateSeekBar(state.getPosition());
            if (state.getState() == PlaybackStateCompat.STATE_PLAYING) {
                toggleButton.setImageResource(R.drawable.ic_pause_white_24dp);
            } else {
                toggleButton.setImageResource(R.drawable.ic_play_arrow_white_24dp);
            }
        }
    };
}
