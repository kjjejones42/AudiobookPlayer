package com.example.myfirstapp;

import android.content.Intent;
import android.os.Parcelable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class DisplayListAdapter extends RecyclerView.Adapter<DisplayListAdapter.MyViewHolder> {

    static class MyViewHolder extends RecyclerView.ViewHolder {
        TextView textView;
        ImageView image;
        MyViewHolder(View v) {
            super(v);
            textView = v.findViewById(R.id.listItemText);
            image = v.findViewById(R.id.listImageView);
        }
    }

    static private List<AudioBook> mDataset;
    private int selectedPos = RecyclerView.NO_POSITION;
    private final RecyclerView rcv;
    private final View.OnClickListener mOnClickListener = new View.OnClickListener(){
        @Override
        public void onClick(View v) {
        int itemPosition = rcv.getChildLayoutPosition(v);
        AudioBook item = mDataset.get(itemPosition);
        notifyItemChanged(selectedPos);
        selectedPos = itemPosition;
        notifyItemChanged(selectedPos);
        Intent intent = new Intent(v.getContext(), PlayActivity.class);
        intent.putExtra(MainActivity.PLAY_FILE, (Parcelable) item.files.get(0));
        v.getContext().startActivity(intent);
        }
    };

    DisplayListAdapter(List<AudioBook> myDataset, RecyclerView rcv) {
        if (myDataset != null){
            mDataset = myDataset;
        }
        this.rcv = rcv;
    }

    @NonNull
    @Override
    public MyViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.display_list_item, parent, false);
        v.setOnClickListener(mOnClickListener);
        return new MyViewHolder(v);

    }


    @Override
    public void onBindViewHolder(MyViewHolder holder, int position) {
        AudioBook item = mDataset.get(position);
        holder.textView.setText(item.name);
        holder.image.setImageBitmap(item.getAlbumArt(rcv.getContext()));
        holder.textView.setSelected(selectedPos == position);
    }

    @Override
    public int getItemCount() {
        return mDataset.size();
    }
}
