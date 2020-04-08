package com.example.myfirstapp;

import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.content.res.AppCompatResources;
import androidx.core.graphics.ColorUtils;
import androidx.core.graphics.drawable.DrawableCompat;
import androidx.palette.graphics.Palette;

import android.annotation.SuppressLint;
import android.content.ComponentName;
import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.media.AudioManager;
import android.media.MediaMetadataRetriever;
import android.net.Uri;
import android.os.Bundle;
import android.os.RemoteException;
import android.support.v4.media.MediaBrowserCompat;
import android.support.v4.media.MediaMetadataCompat;
import android.support.v4.media.session.MediaControllerCompat;
import android.support.v4.media.session.PlaybackStateCompat;
import android.text.Html;
import android.text.style.StyleSpan;
import android.util.Log;
import android.view.View;
import android.view.Window;
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

    private String getMetaData(Uri uri) {
        if (uri == null) return "";
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
        return sb.toString();
    }

    private void playTrack(int position) {
        if (controller != null) {
            try {
                if (spinner != null) {
                    spinner.setSelection(position);
                }
                positionInTrackList = position;
                updateMetadata(position);
                Intent intent = new Intent(this, MediaPlaybackService.class);
                intent.putExtra("AUDIOBOOK", audioBook);
                intent.putExtra("INDEX", position);
                startService(intent);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }


    public void updatePosition(long position) {
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

        audioBook = (AudioBook) getIntent().getSerializableExtra(DisplayListActivity.PLAY_FILE);
        for (MediaItem track : audioBook.files) {
            track.generateTitle(this);
        }
        Objects.requireNonNull(getSupportActionBar()).setTitle(audioBook.displayName);
        setColor(audioBook.getAlbumArt(this));

        ArrayAdapter<MediaItem> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, audioBook.files);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(adapter);
        spinner.setSelection(0,false);
        spinner.setOnItemSelectedListener(this);

        setControlsEnabled(false);

        playTrack(positionInTrackList);
        updateMetadata(positionInTrackList);
   }

   private void setColor(Bitmap bitmap){
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
                    getWindow().setStatusBarColor(color);
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
                updatePosition(progress);
            }
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
        if (controller != null) {
            controller.unregisterCallback(controllerCallback);
        }
        mediaBrowser.disconnect();
    }

    void updateMetadata(int position){
        ImageView imView = findViewById(R.id.albumArtView);
        imView.setImageBitmap(audioBook.getAlbumArt(this));
        metadataText.setText(getMetaData(Uri.parse(audioBook.files.get(position).documentUri)));
        spinner.setTag(position);
        spinner.setSelection(position);
        if (controller != null) {
            durationText.setText(msToMMSS(controller.getMetadata().getLong(MediaMetadataCompat.METADATA_KEY_DURATION)));
        }
    }

    void buildTransportControls() {
        controller.registerCallback(controllerCallback);
        seekBar.setOnSeekBarChangeListener(this);
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
        setControlsEnabled(true);
    }

    void setControlsEnabled(boolean on){
        seekBar.setEnabled(on);
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
                    MediaControllerCompat mediaController = null;
                    try {
                        mediaController = new MediaControllerCompat(PlayActivity.this,
                                mediaBrowser.getSessionToken());
                        MediaControllerCompat.setMediaController(PlayActivity.this, mediaController);
                        controller = mediaController;
                        updateMetadata(positionInTrackList);
                        buildTransportControls();
                        playTrack(positionInTrackList);
                    } catch (RemoteException e) {
                        e.printStackTrace();
                    }
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
            int position = (int) metadata.getLong("AUDIOBOOK_ID");
            updateMetadata(position);
        }

        @Override
        public void onPlaybackStateChanged(PlaybackStateCompat state) {
            if (state.getState() != PlaybackStateCompat.STATE_STOPPED) {
                updatePosition(state.getPosition());
                if (state.getState() == PlaybackStateCompat.STATE_PLAYING) {
                    toggleButton.setImageResource(R.drawable.ic_pause);
                } else {
                    toggleButton.setImageResource(R.drawable.ic_play);
                }
            }
        }
    };
}
