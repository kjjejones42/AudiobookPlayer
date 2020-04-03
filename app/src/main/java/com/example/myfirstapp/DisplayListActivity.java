package com.example.myfirstapp;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import java.util.Objects;

public class DisplayListActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_display_list);

        final DisplayListViewModel model = new ViewModelProvider(this).get(DisplayListViewModel.class);
        final RecyclerView recyclerView = findViewById(R.id.recyclerView);
        final TextView emptyView = findViewById(R.id.empty_view);

        RecyclerView.LayoutManager layoutManager = new LinearLayoutManager(this);
        recyclerView.setLayoutManager(layoutManager);
        RecyclerView.Adapter<DisplayListAdapter.MyViewHolder> mAdapter =
                new DisplayListAdapter(model, recyclerView);
        recyclerView.setAdapter(mAdapter);

        if (Objects.requireNonNull(model.getUsers(this).getValue()).isEmpty()){
            recyclerView.setVisibility(View.GONE);
            emptyView.setVisibility(View.VISIBLE);
        } else {
            recyclerView.setVisibility(View.VISIBLE);
            emptyView.setVisibility(View.GONE);
        }
    }
}

