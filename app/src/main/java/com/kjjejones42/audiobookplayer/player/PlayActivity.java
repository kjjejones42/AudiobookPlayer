package com.kjjejones42.audiobookplayer.player;

import android.annotation.SuppressLint;
import android.content.ComponentName;
import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.media.AudioManager;
import android.os.Bundle;
import android.support.v4.media.MediaBrowserCompat;
import android.support.v4.media.MediaMetadataCompat;
import android.support.v4.media.session.MediaControllerCompat;
import android.support.v4.media.session.PlaybackStateCompat;
import android.util.TypedValue;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.ColorUtils;
import androidx.lifecycle.ViewModelProvider;
import androidx.palette.graphics.Palette;

import com.kjjejones42.audiobookplayer.AudioBook;
import com.kjjejones42.audiobookplayer.MediaItem;
import com.kjjejones42.audiobookplayer.R;
import com.kjjejones42.audiobookplayer.database.AudiobookDao;
import com.kjjejones42.audiobookplayer.database.AudiobookDatabase;
import com.kjjejones42.audiobookplayer.display.DisplayListActivity;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

public class PlayActivity extends AppCompatActivity {

    public final static String INTENT_AUDIOBOOK = "AUDIOBOOK";
    public final static String INTENT_INDEX = "INDEX";

    private static String msToMMSS(long ms) {
        long seconds = TimeUnit.MILLISECONDS.toSeconds(ms) % 60;
        long minutes = TimeUnit.MILLISECONDS.toMinutes(ms) % 60;
        long hours = TimeUnit.MILLISECONDS.toHours(ms);
        if (hours > 0) {
            return String.format(Locale.getDefault(), "%d:%02d:%02d", hours, minutes, seconds);
        }
        return String.format(Locale.getDefault(), "%02d:%02d", minutes, seconds);
    }

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
    private SeekBar seekBar;
    private PlayerViewModel model;
    private ImageView imView;
    private final AudiobookDao audiobookDao = AudiobookDatabase.getInstance(this).audiobookDao();

    private final SeekBar.OnSeekBarChangeListener onSeekBarChangeListener = new SeekBar.OnSeekBarChangeListener() {
        @Override
        public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
            if (fromUser && controller != null) {
                controller.getTransportControls().seekTo(progress);
            }
        }

        @Override
        public void onStartTrackingTouch(SeekBar seekBar) {
        }

        @Override
        public void onStopTrackingTouch(SeekBar seekBar) {
        }
    };

    private final AdapterView.OnItemSelectedListener onItemSelectedListener = new AdapterView.OnItemSelectedListener() {
        @Override
        public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
            if ((Integer) spinner.getTag() != position) {
                initialiseMediaSession(position);
            }
        }

        @Override
        public void onNothingSelected(AdapterView<?> parent) {
        }
    };

    private final MediaControllerCompat.Callback controllerCallback = new MediaControllerCompat.Callback() {
        @Override
        public void onMetadataChanged(MediaMetadataCompat metadata) {
            model.setMetadata(metadata);
            onPlaybackStateChanged(controller.getPlaybackState());
        }

        @Override
        public void onSessionEvent(String event, Bundle extras) {
            super.onSessionEvent(event, extras);
            if (MediaPlaybackService.EVENT_REACHED_END.equals(event)) {
                AudioBook audioBook = model.getAudioBook().getValue();
                mediaBrowser.disconnect();
                if (audioBook != null) {
                    audioBook = audiobookDao.findByName(audioBook.displayName);
                    audioBook.setStatus(AudioBook.STATUS_FINISHED);
                    audiobookDao.update(audioBook);
                }
                onBackPressed();
            }
        }

        @SuppressLint("SwitchIntDef")
        @Override
        public void onPlaybackStateChanged(PlaybackStateCompat state) {
            if (state == null) return;
            switch (state.getState()) {
                case PlaybackStateCompat.STATE_STOPPED:
                case PlaybackStateCompat.STATE_NONE:
                    break;
                case PlaybackStateCompat.STATE_ERROR:
                    Toast.makeText(PlayActivity.this, "Playback Error", Toast.LENGTH_SHORT).show();
                    break;
                default:
                    model.setPosition(state.getPosition());
                    boolean isPlaying = state.getState() == PlaybackStateCompat.STATE_PLAYING;
                    Boolean b = model.getIsPlaying().getValue();
                    if (b != null && isPlaying != b) {
                        model.setIsPlaying(isPlaying);
                }
            }
        }
    };

    private final MediaBrowserCompat.ConnectionCallback connectionCallbacks = new MediaBrowserCompat.ConnectionCallback() {
            @Override
            public void onConnected() {
                PlayActivity.this.onConnected();
            }
    };

    private void initialiseMediaSession(int trackNo) {
        AudioBook book = model.getAudioBook().getValue();
        if (controller == null || book == null) return;
        Intent intent = new Intent(this, MediaPlaybackService.class);
        intent.putExtra(INTENT_AUDIOBOOK, book.displayName);
        intent.putExtra(INTENT_INDEX, trackNo);
        startService(intent);
    }

    private void setImage(Bitmap bitmap) {
        imView.setImageBitmap(bitmap);

    }
    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_play);

        mediaBrowser = new MediaBrowserCompat(this, new ComponentName(this, MediaPlaybackService.class), connectionCallbacks, null);

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

        model = new ViewModelProvider(this).get(PlayerViewModel.class);
        initializeModelObservers();

        setControlsEnabled(false);
        seekBar.setOnSeekBarChangeListener(onSeekBarChangeListener);
        if (getIntent() != null) {
            onNewIntent(getIntent());
        }
    }

    private void initializeModelObservers() {
        model.getIsPlaying().observe(this, this::onIsPlayingSet);
        model.getPosition().observe(this, this::onPositionSet);
        model.getMetadata().observe(this, this::onMetadataSet);
        model.getAudioBook().observe(this, this::onAudioBookSet);
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        boolean shouldStart = intent.getBooleanExtra(DisplayListActivity.INTENT_START_PLAYBACK, false);
        String newBookId = intent.getSerializableExtra(DisplayListActivity.INTENT_PLAY_FILE, String.class);
        AudioBook newBook = audiobookDao.findByName(newBookId);
        model.setStartPlayback(shouldStart);
        if (newBook != null) {
            model.setAudioBook(newBook);
        }
    }

    private void onIsPlayingSet(Boolean isPlaying) {
        int image = isPlaying ? R.drawable.ic_pause : R.drawable.ic_play;
        toggleButton.setImageResource(image);
    }

    private void setDuration(Long duration) {
        if (duration > 0) {
            seekBar.setMax(duration.intValue());
            durationText.setText(msToMMSS(duration));
        }
    }

    private void onPositionSet(Long position) {
        if (position > 0) {
            seekBar.setProgress(position.intValue());
            progressText.setText(msToMMSS(position));
        }
    }

    private void updateButtonColor(int color) {
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

    private void updateStatusBarColor(int color) {
        ActionBar bar = getSupportActionBar();
        if (bar != null) {
            bar.setBackgroundDrawable(new ColorDrawable(color));
        }
        getWindow().setStatusBarColor(ColorUtils.blendARGB(color, Color.BLACK, 0.25f));
    }

    private void onMetadataSet(MediaMetadataCompat metadata) {
        long duration = metadata.getLong(MediaMetadataCompat.METADATA_KEY_DURATION);
        if (duration != 0) {
            setDuration(duration);
            setImage(metadata.getBitmap(MediaMetadataCompat.METADATA_KEY_ALBUM_ART));
            int position = (int) metadata.getLong(MediaMetadataCompat.METADATA_KEY_TRACK_NUMBER);
            spinner.setTag(position);
            spinner.setSelection(position);
        }

    }

    private void buildTransportControls() {
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
        setControlsEnabled(true);
    }

    private void setControlsEnabled(boolean on) {
        int visibility = on ? View.VISIBLE : View.INVISIBLE;
        seekBar.setEnabled(on);
        seekBar.setVisibility(visibility);
        nextButton.setEnabled(on);
        prevButton.setEnabled(on);
        toggleButton.setEnabled(on);
        rewindButton.setEnabled(on);
        forwardButton.setEnabled(on);
    }

    private void onAudioBookSet(AudioBook book) {
        if (book == null) return;
        setColorFromAlbumArt(book);
        ActionBar bar = getSupportActionBar();
        if (bar != null) {
            bar.setTitle(book.displayName);
        }
        List<MediaItem> sortedFiles = book.files.stream().sorted().collect(Collectors.toList());
        ArrayAdapter<MediaItem> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, sortedFiles);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(adapter);
        spinner.setOnItemSelectedListener(onItemSelectedListener);
        int position = Math.min(sortedFiles.size() - 1, book.getPositionInTrackList());
        spinner.setTag(position);
        spinner.setSelection(position);
    }

    private void setColorFromAlbumArt(AudioBook book) {
        if (book == null) return;
        setImage(book.getAlbumArt(this));
        if (!book.isArtGenerated()) {
            Palette palette = book.getAlbumArtPalette(this);
            boolean nightMode = (getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES;
            int backColor = nightMode ? palette.getDarkMutedColor(Color.TRANSPARENT) : palette.getLightMutedColor(Color.TRANSPARENT);
            PlayActivity.this.findViewById(R.id.playerBackground).setBackgroundColor(backColor);
            int color = palette.getVibrantColor(getResources().getColor(R.color.colorAccent, getTheme()));
            updateButtonColor(color);
            updateStatusBarColor(color);
        } else {
            TypedValue tv = new TypedValue();
            getTheme().resolveAttribute(R.attr.colorAccent, tv, true);
            updateButtonColor(tv.data);
            getTheme().resolveAttribute(R.attr.colorPrimary, tv, true);
            updateStatusBarColor(tv.data);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        model.updateBookFromDatabase(this);
        setVolumeControlStream(AudioManager.STREAM_MUSIC);
    }

    @Override
    protected void onStart() {
        super.onStart();
        mediaBrowser.connect();
        AudioBook book = model.getAudioBook().getValue();
        if (book != null) {
            model.setPosition(book.getPositionInTrack());
            setDuration(model.getAudioBook().getValue().getDurationOfMostRecentTrack());
        }
    }

    private void onConnected() {
        AudioBook audioBook = model.getAudioBook().getValue();
        controller = new MediaControllerCompat(
                PlayActivity.this,
                mediaBrowser.getSessionToken());
        MediaControllerCompat.setMediaController(PlayActivity.this, controller);
        buildTransportControls();
        if (audioBook != null) {
            if (!audioBook.getUniqueId().equals(controller.getMetadata().getString(MediaMetadataCompat.METADATA_KEY_MEDIA_ID))) {
                initialiseMediaSession(audioBook.getPositionInTrackList());
            } else {
                model.setMetadata(controller.getMetadata());
                model.setPosition(controller.getPlaybackState().getPosition());
                model.setIsPlaying(controller.getPlaybackState().getState() == PlaybackStateCompat.STATE_PLAYING);
            }
        }
        Boolean b = model.getStartPlayback().getValue();
        if (b != null && b) {
            model.setStartPlayback(false);
            controller.getTransportControls().play();
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (controller != null) {
            controller.unregisterCallback(controllerCallback);
        }
        mediaBrowser.disconnect();
    }

}
