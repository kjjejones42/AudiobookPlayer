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
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;
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

import java.io.File;
import java.util.Arrays;
import java.util.List;

public class DisplayListActivity extends AppCompatActivity {

//    private static String TAG = "ASD";

    public static final String INTENT_UPDATE_MODEL = "com.example.myfirstapp.UPDATE";
    public static final String INTENT_PLAY_FILE = "com.example.myfirstapp.PLAY";
    public static final String INTENT_START_PLAYBACK = "com.example.myfirstapp.start";


    public static final int SELECT_DIRECTORY = 1;
    private static final int MY_PERMISSIONS_REQUEST_READ_STORAGE = 3;
    private DisplayListViewModel model;
    DisplayListAdapter mAdapter;

    public void chooseDirectory(MenuItem item) {
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
            String result = total == grantResults.length ? "All" : "Not all";
            Toast.makeText(this, result + " permissions granted.", Toast.LENGTH_LONG).show();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == SELECT_DIRECTORY && data != null && data.getData() != null) {
            try {
                Data d = new Data.Builder().putString(FileScannerWorker.INPUT, data.getData().toString()).build();
                OneTimeWorkRequest request = new OneTimeWorkRequest.Builder(FileScannerWorker.class).setInputData(d).build();
                final ProgressDialog dialog = ProgressDialog.show(this, "",
                        "Loading. Please wait...", true);
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
            } catch (Exception e) {
                Utils.logError(e, this);
                e.printStackTrace();
            }
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu, menu);
        final SearchView s = (SearchView) menu.findItem(R.id.app_bar_search).getActionView();
        s.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                mAdapter.filter(query);
                return false;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                return false;
            }
        });
//        final MenuItem r = menu.findItem(R.id.resume);
//        r.setOnMenuItemClickListener(item -> {
//            resumeMostRecentBook(null);
//            return true;
//        });
        return true;
    }

    void updateScreen() {
        mAdapter.notifyDataSetChanged();
        List<AudioBook> list = model.getUsers(this).getValue();
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

    RecyclerView recyclerView;
    TextView emptyView;

    public void resumeMostRecentBook(View v) {
        List<AudioBook> books = model.getUsers(this).getValue();
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
                startService(intent);
            }
        }

    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        setTheme(R.style.AppTheme);
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_display_list);

        recyclerView = findViewById(R.id.recyclerView);
        emptyView = findViewById(R.id.empty_view);

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED || ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE},
                    MY_PERMISSIONS_REQUEST_READ_STORAGE);
        }

        model = new ViewModelProvider(this).get(DisplayListViewModel.class);

        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        mAdapter = new DisplayListAdapter(model, recyclerView, this);
        recyclerView.setAdapter(mAdapter);
        updateScreen();
    }

    @Override
    protected void onResume() {
        super.onResume();
        List<AudioBook> list = model.getUsers(this).getValue();
        if (list != null) {
            for (AudioBook book : list) {
                book.loadFromFile(this);
            }
        }
        updateScreen();
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        if (model != null) {
            model.saveToDisk(this);
        }
    }
}

