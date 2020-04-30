package com.example.myfirstapp.display;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.work.Data;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;

import android.Manifest;
import android.app.ProgressDialog;
import android.content.ComponentName;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.RemoteException;
import android.support.v4.media.MediaBrowserCompat;
import android.support.v4.media.session.MediaControllerCompat;
import android.support.v4.media.session.PlaybackStateCompat;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.SearchView;
import android.widget.TextView;
import android.widget.Toast;

import com.example.myfirstapp.R;
import com.example.myfirstapp.AudioBook;
import com.example.myfirstapp.Utils;
import com.example.myfirstapp.player.MediaPlaybackService;
import com.example.myfirstapp.player.PlayActivity;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.util.List;

public class DisplayListActivity extends AppCompatActivity {
    //    private static String TAG = "ASD";

    private static final String INTENT_UPDATE_MODEL = "com.example.myfirstapp.UPDATE";
    public static final String INTENT_PLAY_FILE = "com.example.myfirstapp.PLAY";
    public static final String INTENT_START_PLAYBACK = "com.example.myfirstapp.start";

    private static final int SELECT_DIRECTORY = 1;
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

    private boolean arePermissionsInvalid() {
        return ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_DENIED ||
               ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_DENIED;
    }

    public void chooseDirectory(@SuppressWarnings("unused") MenuItem item) {
        if (arePermissionsInvalid()) {
            ActivityCompat.requestPermissions(
                    this,
                    new String[]{Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE},
                    MY_PERMISSIONS_REQUEST_READ_STORAGE
            );
        } else {
            askUserForDirectory();
        }
    }

    private void askUserForDirectory() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE);
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        startActivityForResult(intent, SELECT_DIRECTORY);
    }


    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
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
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == SELECT_DIRECTORY && data != null && data.getData() != null) {
            try {
                final String message = "Loading. Please wait...";
                Data d = new Data.Builder().putString(FileScannerWorker.INPUT, data.getData().toString()).build();
                OneTimeWorkRequest request = new OneTimeWorkRequest.Builder(FileScannerWorker.class).setInputData(d).build();
                final ProgressDialog dialog = ProgressDialog.show(this, "", message, true);
                WorkManager.getInstance(this).enqueue(request);
                WorkManager.getInstance(this).getWorkInfoByIdLiveData(request.getId())
                        .observe(this, workInfo -> {
                            if (workInfo != null) {
                                int resultsCount = workInfo.getProgress().getInt("PROGRESS", 0);
                                if (resultsCount != 0) {
                                    dialog.setMessage(message + "\nFound " + resultsCount + " books.");
                                }
                                if (workInfo.getState().isFinished()) {
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
                            }
                        });
            } catch (Exception e) {
                Utils.logError(e, this);
                e.printStackTrace();
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


    MediaBrowserCompat.ConnectionCallback connectionCallbacks = new MediaBrowserCompat.ConnectionCallback(){
        @Override
        public void onConnected() {
            super.onConnected();
            try {
                controller = new MediaControllerCompat(
                        DisplayListActivity.this,
                        browser.getSessionToken());
                    controller.registerCallback(new MediaControllerCompat.Callback() {
                        @Override
                        public void onPlaybackStateChanged(PlaybackStateCompat state) {
                            if (state != null) {
                                FloatingActionButton fab = findViewById(R.id.fab);
                                if (state.getState() == PlaybackStateCompat.STATE_PLAYING) {
                                    fab.setImageDrawable(getDrawable(R.drawable.ic_pause));
                                } else {
                                    fab.setImageDrawable(getDrawable(R.drawable.ic_play));
                                }
                            }
                            super.onPlaybackStateChanged(state);
                        }
                    });
            } catch (RemoteException ignored) {}
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

