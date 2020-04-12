package com.example.myfirstapp.display;

import android.content.DialogInterface;
import android.content.Intent;
import android.text.InputType;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.recyclerview.widget.RecyclerView;

import com.example.myfirstapp.player.PlayActivity;
import com.example.myfirstapp.R;
import com.example.myfirstapp.defs.AudioBook;

import java.util.List;


public class DisplayListAdapter extends RecyclerView.Adapter<DisplayListAdapter.MyViewHolder> implements View.OnClickListener, View.OnLongClickListener{


    class MyViewHolder extends RecyclerView.ViewHolder {
        TextView textView;
        ImageView image;

        MyViewHolder(View v) {
            super(v);
            textView = v.findViewById(R.id.listItemText);
            image = v.findViewById(R.id.listImageView);
        }
    }

    private final List<AudioBook> model;
    private final RecyclerView rcv;
    private int selectedPos = RecyclerView.NO_POSITION;

    DisplayListAdapter(DisplayListViewModel model, RecyclerView rcv) {
        this.model = model.getUsers(rcv.getContext()).getValue();
        this.rcv = rcv;
    }

    @NonNull
    @Override
    public MyViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.display_list_item, parent, false);
        v.setOnClickListener(this);
        v.setOnLongClickListener(this);
        return new MyViewHolder(v);
    }

    @Override
    public void onClick(View v) {
        int itemPosition = rcv.getChildLayoutPosition(v);
        AudioBook book = model.get(itemPosition);
        selectedPos = itemPosition;
        notifyItemChanged(selectedPos);
        Intent intent = new Intent(v.getContext(), PlayActivity.class);
        intent.putExtra(DisplayListActivity.PLAY_FILE, book);
        v.getContext().startActivity(intent);
    }

    @Override
    public boolean onLongClick(View v) {
        final int position = rcv.getChildLayoutPosition(v);
        final EditText et = new EditText(v.getContext());
        et.setInputType(InputType.TYPE_CLASS_TEXT);
        new AlertDialog.Builder(v.getContext()).setMessage("Enter title")
                .setView(et)
                .setPositiveButton("Change", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        Log.d("ASD", "onClick: " + et.getText());
                    }
                })
                .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.cancel();
                    }
                })
                .show();
        return false;
    }

    @Override
    public void onBindViewHolder(MyViewHolder holder, int position) {
//        ListItem item = model.get(position);
//        switch (item.getType()) {
//            case ListItem.TYPE_ITEM:
                AudioBook book = model.get(position);
                holder.textView.setText(book.displayName);
                holder.image.setImageBitmap(book.getAlbumArt(rcv.getContext()));
                holder.textView.setSelected(selectedPos == position);
//                break;
//
//            case ListItem.TYPE_HEADING:
//                holder.textView.setText("HEADING");
//        }
    }

    @Override
    public int getItemCount() {
        return model.size();
    }
}
