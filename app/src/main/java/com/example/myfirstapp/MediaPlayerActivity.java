package com.example.myfirstapp;

import android.content.ComponentName;
import android.media.AudioManager;
import android.os.Bundle;
import android.os.RemoteException;
import android.support.v4.media.MediaBrowserCompat;
import android.support.v4.media.MediaMetadataCompat;
import android.support.v4.media.session.MediaControllerCompat;
import android.support.v4.media.session.MediaSessionCompat;
import android.support.v4.media.session.PlaybackStateCompat;
import android.view.View;
import android.widget.ImageView;

import androidx.appcompat.app.AppCompatActivity;

public class MediaPlayerActivity extends AppCompatActivity {
    private MediaBrowserCompat mediaBrowser;
    private final MediaBrowserCompat.ConnectionCallback connectionCallbacks =
            new MediaBrowserCompat.ConnectionCallback() {
                @Override
                public void onConnected() {

                    // Get the token for the MediaSession
                    MediaSessionCompat.Token token = mediaBrowser.getSessionToken();

                    // Create a MediaControllerCompat
                    MediaControllerCompat mediaController = null;
                    try {
                        mediaController = new MediaControllerCompat(MediaPlayerActivity.this, // Context
                                token);
                    } catch (RemoteException e) {
                        e.printStackTrace();
                    }

                    // Save the controller
                    MediaControllerCompat.setMediaController(MediaPlayerActivity.this, mediaController);

                    // Finish building the UI
                    buildTransportControls();
                }

                void buildTransportControls() {
                    // Grab the view for the play/pause button
                    ImageView playPause = findViewById(R.id.play_pause);

                    // Attach a listener to the button
                    playPause.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            // Since this is a play/pause button, you'll need to test the current state
                            // and choose the action accordingly

                            int pbState = MediaControllerCompat.getMediaController(MediaPlayerActivity.this).getPlaybackState().getState();
                            if (pbState == PlaybackStateCompat.STATE_PLAYING) {
                                MediaControllerCompat.getMediaController(MediaPlayerActivity.this).getTransportControls().pause();
                            } else {
                                MediaControllerCompat.getMediaController(MediaPlayerActivity.this).getTransportControls().play();
                            }
                        }});

                        MediaControllerCompat mediaController = MediaControllerCompat.getMediaController(MediaPlayerActivity.this);

                        // Register a Callback to stay in sync
                        mediaController.registerCallback(controllerCallback);
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
                public void onMetadataChanged(MediaMetadataCompat metadata) {}

                @Override
                public void onPlaybackStateChanged(PlaybackStateCompat state) {}
            };



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.media_player_activity);

        mediaBrowser = new MediaBrowserCompat(this,
                new ComponentName(this, MediaPlaybackService.class),
                connectionCallbacks,
                null); // optional Bundle

    }

    @Override
    public void onStart() {
        super.onStart();
        mediaBrowser.connect();
    }

    @Override
    public void onResume() {
        super.onResume();
        setVolumeControlStream(AudioManager.STREAM_MUSIC);
    }

    @Override
    public void onStop() {
        super.onStop();
        // (see "stay in sync with the MediaSession")
        if (MediaControllerCompat.getMediaController(MediaPlayerActivity.this) != null) {
            MediaControllerCompat.getMediaController(MediaPlayerActivity.this).unregisterCallback(controllerCallback);
        }
        mediaBrowser.disconnect();

    }
}
