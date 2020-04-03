package com.example.myfirstapp;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.annotation.SuppressLint;
import android.graphics.Bitmap;
import android.media.AudioAttributes;
import android.media.MediaMetadataRetriever;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.Spinner;
import android.widget.TextView;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

public class PlayActivity extends AppCompatActivity implements SeekBar.OnSeekBarChangeListener, AdapterView.OnItemSelectedListener, MediaPlayer.OnPreparedListener, MediaPlayer.OnCompletionListener {

    private final MediaPlayer mediaPlayer = new MediaPlayer();
    private SeekBar seekBar;
    private AsyncTask<Void, Void, Void> task;
    private int positionInTrack = 0;
    private int positionInTrackList = 0;
    private AudioBook audioBook;
    private boolean isMediaPlayerPrepared;

    public void pause(View view) {
        if (isMediaPlayerPrepared && mediaPlayer.isPlaying()) {
            mediaPlayer.pause();
        }
        updateSeekBar();
    }

    public void play(View view) {
        if (isMediaPlayerPrepared && !mediaPlayer.isPlaying()) {
            mediaPlayer.start();
        }
        updateSeekBar();
    }

    private void getMetaData(Uri uri) {
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

    @SuppressLint("StaticFieldLeak")
    private void playTrack(int position) {
        if (position >= audioBook.files.size()){
            return;
        }
        final MediaItem track = audioBook.files.get(position);

        getMetaData(Uri.parse(track.documentUri));
        Bitmap art = audioBook.getAlbumArt(getApplicationContext());
        ImageView imView = findViewById(R.id.albumArtView);
        imView.setImageBitmap(art);

        seekBar = findViewById(R.id.seekBar);
        seekBar.setOnSeekBarChangeListener(this);

        mediaPlayer.reset();
        if (task != null){
            task.cancel(true);
        }
        mediaPlayer.setAudioAttributes(new AudioAttributes.Builder()
                .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                .build());
        try {
            mediaPlayer.setOnPreparedListener(this);
            mediaPlayer.setOnCompletionListener(this);
            mediaPlayer.setDataSource(getApplicationContext(), Uri.parse(track.documentUri));
            isMediaPlayerPrepared = false;
            mediaPlayer.prepare();
            task = new AsyncTask<Void, Void, Void>() {
                @Override
                protected Void doInBackground(Void... objects) {
                    try {
                        while (!isCancelled()) {
                            if (mediaPlayer.isPlaying()) updateSeekBar();
                        }
                    } catch (IllegalStateException e) {
                        e.printStackTrace();
                    }
                    return null;
                }
            };
        } catch (IOException e) {
            e.printStackTrace();
        }
        positionInTrackList = position;
    }

    public void updateSeekBar() {
        if (isMediaPlayerPrepared) {
            seekBar.setProgress(mediaPlayer.getCurrentPosition());
        }
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        if (mediaPlayer.isPlaying()) {
            outState.putInt("TRACK_PROGRESS", mediaPlayer.getCurrentPosition());
            outState.putInt("TRACK_LIST_PROGRESS", mediaPlayer.getCurrentPosition());
        }
    }

    @SuppressLint("StaticFieldLeak")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (savedInstanceState != null) {
            positionInTrack = savedInstanceState.getInt("TRACK_PROGRESS");
            positionInTrackList = savedInstanceState.getInt("TRACK_LIST_PROGRESS");
        }
        setContentView(R.layout.activity_play);

        audioBook = (AudioBook) getIntent().getSerializableExtra(MainActivity.PLAY_FILE);
        Objects.requireNonNull(getSupportActionBar()).setTitle(audioBook.name);

        Spinner spinner = findViewById(R.id.trackChooser);
        ArrayAdapter<MediaItem> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, audioBook.files);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(adapter);
        spinner.setOnItemSelectedListener(this);
    }

    @Override
    public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
        if (fromUser) {
            pause(seekBar);
            mediaPlayer.seekTo(progress);
            play(seekBar);
        }
    }

    @Override
    public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
        playTrack(position);
    }

    @Override
    public void onStartTrackingTouch(SeekBar seekBar) {}

    @Override
    public void onStopTrackingTouch(SeekBar seekBar) {}

    @Override
    public void onNothingSelected(AdapterView<?> parent) {}

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mediaPlayer.release();
        if (task != null) {
            task.cancel(true);
        }
    }

    @Override
    public void onCompletion(MediaPlayer mp) {
        if (isMediaPlayerPrepared) {
            playTrack(positionInTrackList + 1);
        }
    }

    @Override
    public void onPrepared(MediaPlayer mp) {
        isMediaPlayerPrepared = true;
        if (positionInTrack != 0) {
            mediaPlayer.seekTo(positionInTrack);
        }
        seekBar.setMax(mediaPlayer.getDuration());
        task.execute();
        play(seekBar);
//        LoudnessEnhancer ef = new LoudnessEnhancer(mediaPlayer.getAudioSessionId());
//        ef.setTargetGain(10000);
//        ef.setEnabled(true);
//        mediaPlayer.setAuxEffectSendLevel(1f);
    }
}
