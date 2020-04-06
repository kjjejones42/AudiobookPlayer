package com.example.myfirstapp;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.Observer;
import androidx.lifecycle.SavedStateViewModelFactory;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.NavController;
import androidx.navigation.NavGraph;
import androidx.navigation.Navigation;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.work.Data;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkInfo;
import androidx.work.WorkManager;

import android.Manifest;
import android.app.ProgressDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.webkit.MimeTypeMap;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;

import java.util.Objects;

public class DisplayListActivity extends AppCompatActivity {

    public static final String PLAY_FILE = "com.example.myfirstapp.PLAY";
    public static final int SELECT_DIRECTORY = 1;
    private static final int MY_PERMISSIONS_REQUEST_READ_STORAGE = 3;
    private DisplayListViewModel model;

    public void chooseDirectory(View view) {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE);
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        startActivityForResult(intent, SELECT_DIRECTORY);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == MY_PERMISSIONS_REQUEST_READ_STORAGE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "granted", Toast.LENGTH_SHORT).show();
            }
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
                        .observe(this, new Observer<WorkInfo>() {
                            @Override
                            public void onChanged(WorkInfo workInfo) {
                                if (workInfo != null && workInfo.getState().isFinished()) {
                                    try {
                                        Intent intent = new Intent(getApplicationContext(), DisplayListActivity.class);
                                        dialog.dismiss();
                                        startActivity(intent);
                                    } catch (Exception e) {
                                        e.printStackTrace();
                                    }
                                }
                            }
                        });
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu, menu);
        return true;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        startActivity(new Intent(this, MediaPlayerActivity.class));

        setContentView(R.layout.activity_display_list);


        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {
            Toast.makeText(this, "request", Toast.LENGTH_SHORT).show();

            if (!ActivityCompat.shouldShowRequestPermissionRationale(this,
                    Manifest.permission.READ_EXTERNAL_STORAGE)) {
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.READ_EXTERNAL_STORAGE},
                        MY_PERMISSIONS_REQUEST_READ_STORAGE);
            }
        }

        model = new ViewModelProvider(this).get(DisplayListViewModel.class);
        final RecyclerView recyclerView = findViewById(R.id.recyclerView);
        final TextView emptyView = findViewById(R.id.empty_view);

        RecyclerView.LayoutManager layoutManager = new LinearLayoutManager(this);
        recyclerView.setLayoutManager(layoutManager);
        RecyclerView.Adapter<DisplayListAdapter.MyViewHolder> mAdapter =
                new DisplayListAdapter(model, recyclerView);
        recyclerView.setAdapter(mAdapter);

        if (Objects.requireNonNull(model.getUsers(this)).getValue().isEmpty()){
            recyclerView.setVisibility(View.GONE);
            emptyView.setVisibility(View.VISIBLE);
        } else {
            recyclerView.setVisibility(View.VISIBLE);
            emptyView.setVisibility(View.GONE);
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

