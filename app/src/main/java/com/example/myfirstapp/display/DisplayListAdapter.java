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

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;


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

    private final List<ListItems.ListItem> model;
    private final RecyclerView rcv;
    private int selectedPos = RecyclerView.NO_POSITION;

    DisplayListAdapter(DisplayListViewModel model, RecyclerView rcv) {
        this.model = getGroups(model.getUsers(rcv.getContext()).getValue());
        this.rcv = rcv;
    }

    private List<ListItems.ListItem> getGroups(List<AudioBook> books) {
        List<ListItems.ListItem> list = new ArrayList<>();
        Collections.sort(books, new Comparator<AudioBook>() {
            @Override
            public int compare(AudioBook o1, AudioBook o2) {
                return o1.displayName.compareTo(o2.displayName);
            }
        });
        for (AudioBook book : books){
            list.add(new ListItems.AudioBookContainer(book));
        }
        List<String> letters = new ArrayList<>();
        for (ListItems.ListItem item : list){
            letters.add(item.getSnippet());
        }
        letters = new ArrayList<>(new HashSet<>(letters));
        for (String letter : letters) {
            list.add(new ListItems.Heading(letter));
        }
        Collections.sort(list, new Comparator<ListItems.ListItem>() {
            @Override
            public int compare(ListItems.ListItem o1, ListItems.ListItem o2) {
                int i = o1.getSnippet().compareTo(o2.getSnippet());
                if (i == 0) {
                    return o1.getType() - o2.getType();
                }
                return i;
            }
        });
        return list;
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
        int position = rcv.getChildLayoutPosition(v);
        if (model.get(position).getType() == ListItems.TYPE_ITEM) {
            AudioBook book = ((ListItems.AudioBookContainer) model.get(position)).book;
            selectedPos = position;
            notifyItemChanged(selectedPos);
            Intent intent = new Intent(v.getContext(), PlayActivity.class);
            intent.putExtra(DisplayListActivity.PLAY_FILE, book);
            v.getContext().startActivity(intent);
        }
    }

    @Override
    public boolean onLongClick(View v) {
        final int position = rcv.getChildLayoutPosition(v);
        if (model.get(position).getType() == ListItems.TYPE_ITEM) {
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
        }
        return false;
    }

    @Override
    public void onBindViewHolder(MyViewHolder holder, int position) {
        switch ( model.get(position).getType()) {
            case ListItems.TYPE_ITEM:
                AudioBook book = ((ListItems.AudioBookContainer) model.get(position)).book;
                holder.textView.setText(book.displayName);
                holder.image.setImageBitmap(book.getAlbumArt(rcv.getContext()));
                holder.textView.setSelected(selectedPos == position);
                break;

            case ListItems.TYPE_HEADING:
                String title = ((ListItems.Heading) model.get(position)).title;
                holder.textView.setText(title);
                holder.image.setVisibility(View.GONE);
                break;
        }
    }

    @Override
    public int getItemCount() {
        return model.size();
    }
}
