package com.example.myfirstapp;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.os.Bundle;
import android.util.Log;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.util.List;

public class DisplayListActivity extends AppCompatActivity {

    private List<AudioBook> message;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        DisplayListViewModel model = new ViewModelProvider(this).get(DisplayListViewModel.class);
        model.getUsers().observe(this, new Observer<List<AudioBook>>() {
            @Override
            public void onChanged(List<AudioBook> audioBooks) {
                Log.d("ASD", "" + audioBooks);
            }
        });

        try {
            FileInputStream fis = openFileInput(FileScannerWorker.LIST_OF_DIRS);
            ObjectInputStream oos = new ObjectInputStream(fis);
            message = (List<AudioBook>) oos.readObject();
            oos.close();
        } catch (IOException | ClassNotFoundException e) {
            e.printStackTrace();
        }
        setContentView(R.layout.activity_display_list);

        RecyclerView recyclerView = findViewById(R.id.recyclerView);
        RecyclerView.LayoutManager layoutManager = new LinearLayoutManager(this);
        recyclerView.setLayoutManager(layoutManager);
        RecyclerView.Adapter<DisplayListAdapter.MyViewHolder> mAdapter =
                new DisplayListAdapter(message, recyclerView);
        recyclerView.setAdapter(mAdapter);
    }
}

