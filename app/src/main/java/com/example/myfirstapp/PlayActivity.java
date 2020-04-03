package com.example.myfirstapp;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.graphics.Bitmap;
import android.media.AudioAttributes;
import android.media.MediaMetadataRetriever;
import android.media.MediaPlayer;
import android.media.audiofx.AudioEffect;
import android.media.audiofx.LoudnessEnhancer;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;

public class PlayActivity extends AppCompatActivity {

    private final MediaPlayer mediaPlayer = new MediaPlayer();
    private SeekBar seekBar;
    private AsyncTask<Void, Void, Void> task;
    private int initialPosition = 0;

    public void pause(View view) {
        mediaPlayer.pause();
        setPosition();
    }

    public void play(View view) {
        mediaPlayer.start();
        setPosition();
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

    public void setPosition() {
        seekBar.setProgress(mediaPlayer.getCurrentPosition());
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt("PROGRESS", mediaPlayer.getCurrentPosition());
    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        initialPosition = savedInstanceState.getInt("PROGRESS");
    }

    @SuppressLint("StaticFieldLeak")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_play);

        Intent intent = getIntent();
        MediaItem message = intent.getParcelableExtra(MainActivity.PLAY_FILE);
        Objects.requireNonNull(getSupportActionBar()).setTitle(message.displayName);
        getMetaData(Uri.parse(message.documentUri));

        Bitmap art = message.getAlbumArt(getApplicationContext());
        ImageView imView = findViewById(R.id.albumArtView);
        imView.setImageBitmap(art);

        seekBar =  findViewById(R.id.seekBar);
        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener(){
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (fromUser) {
                    pause(seekBar);
                    mediaPlayer.seekTo(progress);
                    play(seekBar);
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });
        try {
            if (mediaPlayer.isPlaying()) {
                mediaPlayer.stop();
            }
            mediaPlayer.setAudioAttributes(new AudioAttributes.Builder()
                            .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                            .build());
            mediaPlayer.setDataSource(getApplicationContext(), Uri.parse(message.documentUri));
            mediaPlayer.setOnPreparedListener(new MediaPlayer.OnPreparedListener(){
                @Override
                public void onPrepared(MediaPlayer mp) {
                    if (initialPosition != 0) {
                        mediaPlayer.seekTo(initialPosition);
                    }
                    seekBar.setMax(mediaPlayer.getDuration());
                    play(seekBar);
//                    LoudnessEnhancer ef = new LoudnessEnhancer(mediaPlayer.getAudioSessionId());
//                    ef.setTargetGain(10000);
//                    ef.setEnabled(true);
//                    mediaPlayer.setAuxEffectSendLevel(1f);
                }
            });
            mediaPlayer.prepareAsync();
            task = new AsyncTask<Void, Void, Void>(){
                @Override
                protected Void doInBackground(Void... objects) {
                    try {
                        while (!isCancelled()){
                            if (mediaPlayer.isPlaying()) {
                                setPosition();
                            }
                        }
                    } catch (IllegalStateException ignored) {

                    }
                    return null;
                }
            };
            task.execute();
        } catch (Exception e){
            e.printStackTrace();
        }
    }


    @Override
    protected void onDestroy() {
        super.onDestroy();
        mediaPlayer.release();
        if (task != null) {
            task.cancel(true);
        }
    }
}
