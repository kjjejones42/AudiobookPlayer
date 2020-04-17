package com.example.myfirstapp.player;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.graphics.ColorUtils;
import androidx.lifecycle.ViewModelProvider;
import androidx.palette.graphics.Palette;

import android.content.ComponentName;
import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.media.AudioManager;
import android.os.Bundle;
import android.os.RemoteException;
import android.support.v4.media.MediaBrowserCompat;
import android.support.v4.media.MediaMetadataCompat;
import android.support.v4.media.session.MediaControllerCompat;
import android.support.v4.media.session.PlaybackStateCompat;
import android.util.TypedValue;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.Spinner;
import android.widget.TextView;

import com.example.myfirstapp.R;
import com.example.myfirstapp.defs.AudioBook;
import com.example.myfirstapp.defs.MediaItem;
import com.example.myfirstapp.display.DisplayListActivity;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

public class PlayActivity extends AppCompatActivity implements SeekBar.OnSeekBarChangeListener, AdapterView.OnItemSelectedListener {

//    private static String TAG = "ASD";

    private Spinner spinner;
    private ImageButton prevButton;
    private ImageButton rewindButton;
    private ImageButton toggleButton;
    private ImageButton forwardButton;
    private ImageButton nextButton;
    private TextView progressText;
    private TextView durationText;
    private MediaBrowserCompat mediaBrowser;
    private MediaControllerCompat controller;
    private AudioBook audioBook;
    private SeekBar seekBar;
    private PlayerViewModel model;
    private ImageView imView;
    private ConstraintLayout buttonContainer;

    private String msToMMSS(long ms){
        long seconds = TimeUnit.MILLISECONDS.toSeconds(ms) % 60;
        long minutes = TimeUnit.MILLISECONDS.toMinutes(ms) % 60;
        long hours = TimeUnit.MILLISECONDS.toHours(ms);
        if (hours > 0) {
            return String.format(Locale.getDefault(), "%02d:%02d:%02d", hours, minutes, seconds);
        }
        return String.format(Locale.getDefault(), "%02d:%02d", minutes, seconds);
    }

    private void initialiseMediaSession(int trackNo) {
        if (controller != null) {
            try {
                Intent intent = new Intent(this, MediaPlaybackService.class);
                intent.putExtra("AUDIOBOOK", audioBook);
                intent.putExtra("INDEX", trackNo);
                startService(intent);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    void setDuration(Long duration){
        if (duration > 0) {
            seekBar.setMax(duration.intValue());
            durationText.setText(msToMMSS(duration));
        }
    }

    void setImage(Bitmap bitmap){
        if (bitmap != null) {
            imView.setImageBitmap(bitmap);
        }

    }

    void setMetaData(MediaMetadataCompat metadata){
        setDuration(metadata.getLong(MediaMetadataCompat.METADATA_KEY_DURATION));
        setImage(metadata.getBitmap(MediaMetadataCompat.METADATA_KEY_ALBUM_ART));
        int position = (int) metadata.getLong(MediaMetadataCompat.METADATA_KEY_TRACK_NUMBER);
        spinner.setTag(position);
        spinner.setSelection(position);

    }

    private void initializeModelObservers(){
        model.getIsPlaying().observe(this, isPlaying -> {
            if (isPlaying) {
                toggleButton.setImageResource(R.drawable.ic_pause);
            } else {
                toggleButton.setImageResource(R.drawable.ic_play);
            }
        });
        model.getPosition().observe(this, position -> {
            if (position > 0) {
                seekBar.setProgress(position.intValue());
                progressText.setText(msToMMSS(position));
            }
        });
        model.getMetadata().observe(this, this::setMetaData);
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
        imView = findViewById(R.id.albumArtView);
        buttonContainer = findViewById(R.id.buttonContainer);

        buttonContainer.setVisibility(View.INVISIBLE);

        mediaBrowser = new MediaBrowserCompat(this, new ComponentName(this, MediaPlaybackService.class), connectionCallbacks,null);
        AudioBook newbook = (AudioBook) getIntent().getSerializableExtra(DisplayListActivity.PLAY_FILE);
        if (newbook != null) {
            audioBook = newbook;
        }
        audioBook.loadFromFile(this);
        int position = audioBook.getPositionInTrackList();
        setColorFromAlbumArt(audioBook.getAlbumArt(this));
        model = new ViewModelProvider(this).get(PlayerViewModel.class);
        initializeModelObservers();

        setControlsEnabled(false);
        seekBar.setOnSeekBarChangeListener(this);

        Objects.requireNonNull(getSupportActionBar()).setTitle(audioBook.displayName);


        ArrayAdapter<MediaItem> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, audioBook.files);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(adapter);
        spinner.setTag(position);
        spinner.setSelection(position);
        spinner.setOnItemSelectedListener(this);


   }

   private void onConnected(){
       try {
           controller = new MediaControllerCompat(
                   PlayActivity.this,
                   mediaBrowser.getSessionToken());
           MediaControllerCompat.setMediaController(PlayActivity.this, controller);
           buildTransportControls();
           if (!audioBook.displayName.equals(controller.getMetadata().getString(MediaMetadataCompat.METADATA_KEY_MEDIA_ID))) {
               model.clear();
               initialiseMediaSession(audioBook.getPositionInTrackList());
           } else {
               model.setMetadata(controller.getMetadata());
               model.setPosition(controller.getPlaybackState().getPosition());
           }
           Animation bottomUp = AnimationUtils.loadAnimation(this,
                   R.anim.bottom_up);
           buttonContainer.startAnimation(bottomUp);
           buttonContainer.setVisibility(View.VISIBLE);
       } catch (RemoteException e) {
           e.printStackTrace();
       }
   }

   private void updateButtonColor(int color){
       List<ImageButton> list = Arrays.asList(prevButton, rewindButton, toggleButton, nextButton, forwardButton);
       for (ImageButton b : list) {
           b.getBackground().setTint(color);
       }
       ActionBar bar = getSupportActionBar();
       if (bar != null) {
           bar.setBackgroundDrawable(new ColorDrawable(color));
       }
       getWindow().setStatusBarColor(ColorUtils.blendARGB(color, Color.BLACK, 0.25f));
       seekBar.getThumb().setTint(color);
       seekBar.getProgressDrawable().setTint(color);
   }

   private void updateStatusBarColor(int color){
       ActionBar bar = getSupportActionBar();
       if (bar != null) {
           bar.setBackgroundDrawable(new ColorDrawable(color));
       }
       getWindow().setStatusBarColor(ColorUtils.blendARGB(color, Color.BLACK, 0.25f));
   }

   private void setColorFromAlbumArt(Bitmap bitmap){
        if (bitmap == null) {
            return;
        }
        try {
            Palette.from(bitmap).generate(palette -> {
                if (palette != null && !audioBook.isArtGenerated()) {
                    boolean nightMode = (getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES;
                    int backColor = nightMode ? palette.getDarkMutedColor(Color.TRANSPARENT) : palette.getLightMutedColor(Color.TRANSPARENT);
                    PlayActivity.this.findViewById(R.id.playerBackground).setBackgroundColor(backColor);
                    int color = palette.getVibrantColor(getResources().getColor(R.color.colorAccent));
                    updateButtonColor(color);
                    updateStatusBarColor(color);
                } else {
                    TypedValue tv = new TypedValue();
                    getTheme().resolveAttribute(R.attr.colorAccent, tv, true);
                    updateButtonColor(tv.data);
                    getTheme().resolveAttribute(R.attr.colorPrimary, tv, true);
                    updateStatusBarColor(tv.data);
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
        audioBook.loadFromFile(this);
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
        toggleButton.setOnClickListener(v -> {
            int pbState = controller.getPlaybackState().getState();
            if (pbState == PlaybackStateCompat.STATE_PLAYING) {
                controller.getTransportControls().pause();
            } else {
                controller.getTransportControls().play();
            }
        });
        prevButton.setOnClickListener(v -> controller.getTransportControls().skipToPrevious());
        nextButton.setOnClickListener(v -> {
            controller.getTransportControls().skipToNext();
            model.setPosition(0);
        });
        rewindButton.setOnClickListener(v -> controller.getTransportControls().rewind());
        forwardButton.setOnClickListener(v -> controller.getTransportControls().fastForward());
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
                    PlayActivity.this.onConnected();
                }

                @Override
                public void onConnectionSuspended() {
                    setControlsEnabled(false);
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
        public void onSessionEvent(String event, Bundle extras) {
            super.onSessionEvent(event, extras);
            if ("REACHED_END".equals(event)) {
                mediaBrowser.disconnect();
                audioBook.setFinished(PlayActivity.this);
                audioBook.saveConfig(PlayActivity.this);
                onBackPressed();
            }
        }

        @Override
        public void onPlaybackStateChanged(PlaybackStateCompat state) {
            if (state.getState() != PlaybackStateCompat.STATE_STOPPED) {
                setControlsEnabled(true);
                model.setPosition(state.getPosition());
                boolean isPlaying = state.getState() == PlaybackStateCompat.STATE_PLAYING;
                Boolean b = model.getIsPlaying().getValue();
                if (b != null && isPlaying != b) {
                    model.setIsPlaying(isPlaying);
                }
            }
        }
    };
}
