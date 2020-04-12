package com.example.myfirstapp;

import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;
import androidx.palette.graphics.Palette;

import android.annotation.SuppressLint;
import android.content.ComponentName;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.drawable.ColorDrawable;
import android.media.AudioManager;
import android.os.Bundle;
import android.os.RemoteException;
import android.support.v4.media.MediaBrowserCompat;
import android.support.v4.media.MediaMetadataCompat;
import android.support.v4.media.session.MediaControllerCompat;
import android.support.v4.media.session.PlaybackStateCompat;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.Spinner;
import android.widget.TextView;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

public class PlayActivity extends AppCompatActivity implements SeekBar.OnSeekBarChangeListener, AdapterView.OnItemSelectedListener {

    private static String TAG = "ASD";

    private Spinner spinner;
    private ImageButton prevButton;
    private ImageButton rewindButton;
    private ImageButton toggleButton;
    private ImageButton forwardButton;
    private ImageButton nextButton;
    private TextView progressText;
    private TextView durationText;
    private TextView metadataText;
    private MediaBrowserCompat mediaBrowser;
    private MediaControllerCompat controller;
    private AudioBook audioBook;
    private int positionInTrackList = 0;
    private SeekBar seekBar;
    private PlayerViewModel model;
    private ImageView imView;

    private String msToMMSS(long ms){
        long seconds = ms / 1000 % 60;
        long minutes = ms / (60 * 1000);
        long hours = ms / (60 * 60 * 1000);
        if (hours > 0) {
            return String.format(Locale.getDefault(), "%02d:%02d:%02d", hours, minutes, seconds);
        }
        return String.format(Locale.getDefault(), "%02d:%02d", minutes, seconds);
    }

    private void setPositionInTrackList(int position) {
        positionInTrackList = position;
        spinner.setSelection(position);
    }

    private void initialiseMediaSession(int position) {
        if (controller != null) {
            try {
                controller.getTransportControls().stop();
//                model.setPosition(0);
                setPositionInTrackList(position);
                Intent intent = new Intent(this, MediaPlaybackService.class);
                intent.putExtra("AUDIOBOOK", audioBook);
                intent.putExtra("INDEX", position);
                startService(intent);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private String getSeekDetails(){
        return ""+ seekBar.getProgress() + " " + seekBar.getMax();
    }

    void setMetaData(MediaMetadataCompat metadata){
        Long duration = metadata.getLong(MediaMetadataCompat.METADATA_KEY_DURATION);
        if (duration > 0) {
            seekBar.setMax(duration.intValue());
            durationText.setText(msToMMSS(duration));
        }
        setPositionInTrackList((int) metadata.getLong(MediaMetadataCompat.METADATA_KEY_TRACK_NUMBER));
        imView.setImageBitmap(metadata.getBitmap(MediaMetadataCompat.METADATA_KEY_ALBUM_ART));
        metadataText.setText(metadata.getString(MediaMetadataCompat.METADATA_KEY_DISPLAY_DESCRIPTION));
        spinner.setTag(positionInTrackList);
        spinner.setSelection(positionInTrackList);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_play);

        prevButton = findViewById(R.id.prevButton);
        rewindButton = findViewById(R.id.rewindButton);
        toggleButton = findViewById(R.id.toggleButton);
        forwardButton = findViewById(R.id.forwardButton);
        nextButton = findViewById(R.id.nextButton);
        progressText = findViewById(R.id.progress_text);
        durationText = findViewById(R.id.duration_text);
        seekBar = findViewById(R.id.seekBar);
        spinner = findViewById(R.id.trackChooser);
        metadataText = findViewById(R.id.songInfo);
        imView = findViewById(R.id.albumArtView);

        mediaBrowser = new MediaBrowserCompat(
                this,
                new ComponentName(this, MediaPlaybackService.class),
                connectionCallbacks,
                null);
        audioBook = (AudioBook) getIntent().getSerializableExtra(DisplayListActivity.PLAY_FILE);
        setColorFromAlbumArt(audioBook.getAlbumArt(this));
        for (MediaItem track : audioBook.files) {
            track.generateTitle(this);
        }
        model = new ViewModelProvider(this).get(PlayerViewModel.class);
        model.getIsPlaying().observe(this, new Observer<Boolean>() {
            @Override
            public void onChanged(Boolean isPlaying) {
                Log.d(TAG, "isplaying: " + isPlaying);
                if (isPlaying) {
                    toggleButton.setImageResource(R.drawable.ic_pause);
                } else {
                    toggleButton.setImageResource(R.drawable.ic_play);
                }
            }
        });
        model.getPosition().observe(this, new Observer<Long>() {
            @Override
            public void onChanged(Long position) {
                if (position > 0) {
                    seekBar.setProgress(position.intValue());
                    progressText.setText(msToMMSS(position));
                }
            }
        });
        model.getMetadata().observe(this, new Observer<MediaMetadataCompat>() {
            @Override
            public void onChanged(MediaMetadataCompat metadata) {
                setMetaData(metadata);
            }
        });


        setControlsEnabled(false);
        seekBar.setOnSeekBarChangeListener(this);

        Objects.requireNonNull(getSupportActionBar()).setTitle(audioBook.displayName);
//        setColorFromAlbumArt(audioBook.getAlbumArt(this));

        ArrayAdapter<MediaItem> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, audioBook.files);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(adapter);
        spinner.setTag(positionInTrackList);
        spinner.setSelection(positionInTrackList);
        spinner.setOnItemSelectedListener(this);

        setPositionInTrackList(audioBook.getPositionInTrackList());
   }

   private void setColorFromAlbumArt(Bitmap bitmap){
        if (bitmap == null) {
            return;
        }
        try {
            Palette.from(bitmap).generate(new Palette.PaletteAsyncListener() {
                @Override
                public void onGenerated(@Nullable Palette palette) {
                int color = palette.getVibrantColor(getResources().getColor(R.color.colorAccent));
                List<ImageButton> list = Arrays.asList(prevButton, rewindButton, toggleButton, nextButton, forwardButton);
                for (ImageButton b : list) {
                    b.getBackground().setTint(color);
                }
                ActionBar bar = getSupportActionBar();
                bar.setBackgroundDrawable(new ColorDrawable(color));
                getWindow().setStatusBarColor(palette.getMutedColor(color));
                seekBar.getThumb().setTint(color);
                seekBar.getProgressDrawable().setTint(color);
                }
            });
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
        if (fromUser) {
            if (controller != null) {
                controller.getTransportControls().seekTo(progress);
            }
        }
    }

    @Override
    public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
        if ((Integer) spinner.getTag() != position) {
            initialiseMediaSession(position);
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
        if (controller != null) {
            controller.unregisterCallback(controllerCallback);
        }
        mediaBrowser.disconnect();
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
        prevButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                controller.getTransportControls().skipToPrevious();
            }
        });
        nextButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                controller.getTransportControls().skipToNext();
                model.setPosition(0);
            }
        });
        rewindButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                controller.getTransportControls().rewind();
            }
        });
        forwardButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                controller.getTransportControls().fastForward();
            }
        });
    }

    void setControlsEnabled(boolean on){
        int visibility = on ? View.VISIBLE : View.INVISIBLE;
        seekBar.setEnabled(on);
        seekBar.setVisibility(visibility);
        nextButton.setEnabled(on);
        prevButton.setEnabled(on);
        toggleButton.setEnabled(on);
        rewindButton.setEnabled(on);
        forwardButton.setEnabled(on);
    }

    private final MediaBrowserCompat.ConnectionCallback connectionCallbacks =
            new MediaBrowserCompat.ConnectionCallback() {
                @Override
                public void onConnected() {
                    try {
                        controller = new MediaControllerCompat(
                                PlayActivity.this,
                                mediaBrowser.getSessionToken());
                        MediaControllerCompat.setMediaController(PlayActivity.this, controller);
                        model.setMetadata(controller.getMetadata());
                        model.setPosition(controller.getPlaybackState().getPosition());
                        Log.d(TAG, "onConnected: " + getSeekDetails());
                        buildTransportControls();
                        if (!audioBook.displayName.equals(controller.getMetadata().getString(MediaMetadataCompat.METADATA_KEY_MEDIA_ID))) {
                            model.clear();
                            initialiseMediaSession(positionInTrackList);
                        }
                    } catch (RemoteException e) {
                        e.printStackTrace();
                    }
                }

                @Override
                public void onConnectionSuspended() {
                    setControlsEnabled(false);
                    Log.d("ASD", "STOP");
                }

                @Override
                public void onConnectionFailed() {
                    setControlsEnabled(false);
                }
            };

    MediaControllerCompat.Callback controllerCallback = new MediaControllerCompat.Callback() {
        @Override
        public void onMetadataChanged(MediaMetadataCompat metadata) {
            model.setMetadata(metadata);
            onPlaybackStateChanged(controller.getPlaybackState());
        }
        @Override
        public void onPlaybackStateChanged(PlaybackStateCompat state) {
            if (state.getState() != PlaybackStateCompat.STATE_STOPPED) {
                setControlsEnabled(true);
                model.setPosition(state.getPosition());
                boolean isPlaying = state.getState() == PlaybackStateCompat.STATE_PLAYING;
                if (isPlaying != model.getIsPlaying().getValue()){
                    model.setIsPlaying(state.getState() == PlaybackStateCompat.STATE_PLAYING);
                    onPlaybackStateChanged(controller.getPlaybackState());
                }
            }
        }
    };
}
