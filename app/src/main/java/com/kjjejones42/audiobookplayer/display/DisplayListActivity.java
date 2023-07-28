package com.kjjejones42.audiobookplayer.display;

import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.content.res.AppCompatResources;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.ProgressDialog;
import android.content.ComponentName;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.v4.media.MediaBrowserCompat;
import android.support.v4.media.session.MediaControllerCompat;
import android.support.v4.media.session.PlaybackStateCompat;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.SearchView;
import android.widget.TextView;
import android.widget.Toast;

import com.kjjejones42.audiobookplayer.R;
import com.kjjejones42.audiobookplayer.AudioBook;
import com.kjjejones42.audiobookplayer.Utils;
import com.kjjejones42.audiobookplayer.player.MediaPlaybackService;
import com.kjjejones42.audiobookplayer.player.PlayActivity;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.util.List;

public class DisplayListActivity extends AppCompatActivity {

    private static final String INTENT_UPDATE_MODEL = "com.example.myfirstapp.UPDATE";
    public static final String INTENT_PLAY_FILE = "com.example.myfirstapp.PLAY";
    public static final String INTENT_START_PLAYBACK = "com.example.myfirstapp.start";

    private final String[] PERMISSIONS = new String[] {
            Manifest.permission.READ_MEDIA_IMAGES,
            Manifest.permission.READ_MEDIA_AUDIO,
            Manifest.permission.POST_NOTIFICATIONS,
    };


    private static final int MY_PERMISSIONS_REQUEST_READ_STORAGE = 3;
    private DisplayListViewModel model;
    private DisplayListAdapter mAdapter;
    private RecyclerView recyclerView;
    private TextView emptyView;
    private SearchView searchView;
    private AudioBook lastBookStarted;
    private MediaControllerCompat controller;
    private MediaBrowserCompat browser;

    public void setLastBookStarted(AudioBook lastBookStarted) {
        this.lastBookStarted = lastBookStarted;
    }

    private final ActivityResultLauncher<String[]> activityResultLauncher = registerForActivityResult(
            new ActivityResultContracts.RequestMultiplePermissions(),
            x -> askUserForDirectory()
    );

    public void chooseDirectory(@SuppressWarnings("unused") MenuItem item) {
        activityResultLauncher.launch(PERMISSIONS);
    }

    private void askUserForDirectory() {
        final String message = "Loading. Please wait...";
        OneTimeWorkRequest request = new OneTimeWorkRequest.Builder(FileScannerWorker.class).build();
        final ProgressDialog dialog = ProgressDialog.show(this, "", message, true);
        WorkManager.getInstance(this).enqueue(request);
        WorkManager.getInstance(this).getWorkInfoByIdLiveData(request.getId())
            .observe(this, workInfo -> {
                if (workInfo != null && workInfo.getState().isFinished()) {
                        try {
                            Intent intent = new Intent(getApplicationContext(), DisplayListActivity.class);
                            intent.putExtra(INTENT_UPDATE_MODEL, true);
                            dialog.cancel();
                            startActivity(intent);
                        } catch (Exception e) {
                            Utils.logError(e, this);
                            e.printStackTrace();
                        }
                    }
            });
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == MY_PERMISSIONS_REQUEST_READ_STORAGE) {
            int total = grantResults.length;
            for (int i = 0; i < permissions.length; i++) {
                total += grantResults[i];
            }
            boolean allApproved = total == grantResults.length;
            String result = allApproved ? "All" : "Not all";
            Toast.makeText(this, result + " permissions granted.", Toast.LENGTH_LONG).show();
            if (allApproved) {
                askUserForDirectory();
            }
        }
    }

    @Override
    public void onBackPressed() {
        if (searchView != null && !searchView.isIconified()) {
            searchView.setQuery("",false);
            searchView.clearFocus();
            searchView.setIconified(true);
        } else {
            super.onBackPressed();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu, menu);
        searchView = (SearchView) menu.findItem(R.id.app_bar_search).getActionView();
        assert searchView != null;
        searchView.setSubmitButtonEnabled(false);
        searchView.setOnCloseListener(() -> {
            mAdapter.filter(null);
            return false;
        });
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                mAdapter.filter(query);
                return false;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                mAdapter.filter(newText);
                return false;
            }
        });
        return true;
    }

    @SuppressLint("Range")
    void updateScreen() {
        mAdapter.recalculateListFromModel();
        List<AudioBook> list = model.getSavedBooks(this).getValue();
        if (list != null && list.isEmpty()) {
            recyclerView.setVisibility(View.GONE);
            emptyView.setVisibility(View.VISIBLE);
        } else {
            recyclerView.setVisibility(View.VISIBLE);
            emptyView.setVisibility(View.GONE);
        }

    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        if (intent.getBooleanExtra(INTENT_UPDATE_MODEL, false)) {
            model.loadFromDisk(this);
        }
        updateScreen();
    }

    private void resumeMostRecentBook() {
        List<AudioBook> books = model.getSavedBooks(this).getValue();
        if (books != null) {
            AudioBook mostRecent = null;
            long recent = Long.MIN_VALUE;
            for (AudioBook book : books) {
                if (book.lastSavedTimestamp > recent) {
                    recent = book.lastSavedTimestamp;
                    mostRecent = book;
                }
            }
            if (mostRecent != null) {
                Intent intent = new Intent(this, MediaPlaybackService.class);
                intent.putExtra(PlayActivity.INTENT_AUDIOBOOK, mostRecent);
                intent.putExtra(PlayActivity.INTENT_INDEX, mostRecent.getPositionInTrackList());
                setLastBookStarted(mostRecent);
                startService(intent);
            }
        }
    }


    public void onFloatingActionButtonClick(@SuppressWarnings("unused") View v) {
        if (controller.getPlaybackState() == null || controller.getPlaybackState().getState() != PlaybackStateCompat.STATE_PLAYING) {
            resumeMostRecentBook();
        } else {
            controller.getTransportControls().pause();
        }
    }

    @Override
    protected void onRestart() {
        mAdapter.recalculateListFromModel();
        super.onRestart();
    }


    final MediaBrowserCompat.ConnectionCallback connectionCallbacks = new MediaBrowserCompat.ConnectionCallback(){
        @Override
        public void onConnected() {
            super.onConnected();
            controller = new MediaControllerCompat(
                    DisplayListActivity.this,
                    browser.getSessionToken());
            controller.registerCallback(new MediaControllerCompat.Callback() {
                @Override
                public void onPlaybackStateChanged(PlaybackStateCompat state) {
                    if (state != null) {
                        FloatingActionButton fab = findViewById(R.id.fab);
                        int icon = (state.getState() == PlaybackStateCompat.STATE_PLAYING) ?
                                R.drawable.ic_pause : R.drawable.ic_play;
                        fab.setImageDrawable(AppCompatResources.getDrawable(getBaseContext(), icon));
                    }
                    super.onPlaybackStateChanged(state);
                }
            });
        }
    } ;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        setTheme(R.style.AppTheme);
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_display_list);

        recyclerView = findViewById(R.id.recyclerView);
        emptyView = findViewById(R.id.empty_view);

        model = new ViewModelProvider(this).get(DisplayListViewModel.class);
        browser = new MediaBrowserCompat(this, new ComponentName(this, MediaPlaybackService.class), connectionCallbacks, null);

        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        mAdapter = new DisplayListAdapter(model, recyclerView, this);
        recyclerView.setAdapter(mAdapter);
        model.loadFromDisk(this);
        updateScreen();
    }

    @Override
    protected void onResume() {
        super.onResume();
        List<AudioBook> books = model.getSavedBooks(this).getValue();
        if (books != null) {
            for (AudioBook book : books) {
                if (book.equals(lastBookStarted)) {
                    book.loadFromFile(this);
                    break;
                }
            }
        }
        updateScreen();
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (browser != null) {
            browser.connect();
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        model.saveToDisk(this);
        if (browser != null) {
            browser.disconnect();
        }
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        if (model != null) {
            model.saveToDisk(this);
        }
    }

}
