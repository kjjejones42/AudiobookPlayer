package com.example.myfirstapp.display;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.text.InputType;
import android.util.Log;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.LifecycleOwner;
import androidx.lifecycle.Observer;
import androidx.recyclerview.widget.RecyclerView;

import com.example.myfirstapp.player.PlayActivity;
import com.example.myfirstapp.R;
import com.example.myfirstapp.defs.AudioBook;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;


public class DisplayListAdapter extends RecyclerView.Adapter<DisplayListAdapter.MyViewHolder> implements View.OnClickListener, View.OnLongClickListener{

    class MyViewHolder extends RecyclerView.ViewHolder {
        TextView textView;
        ImageView image;

        MyViewHolder(View v, boolean isItem) {
            super(v);
            if (isItem) {
                textView = v.findViewById(R.id.listItemText);
                image = v.findViewById(R.id.listImageView);
            } else {
                textView = (TextView) v;
            }
        }
    }

    private List<ListItems.ListItem> finalList;
    private final DisplayListViewModel model;
    private final RecyclerView rcv;
    private int selectedPos = RecyclerView.NO_POSITION;
    private Context context;

    DisplayListAdapter(DisplayListViewModel model, RecyclerView rcv, LifecycleOwner lco) {
        this.model = model;
        this.context = rcv.getContext();
        model.getUsers(rcv.getContext()).observe(lco, new Observer<List<AudioBook>>() {
            @Override
            public void onChanged(List<AudioBook> audioBooks) {
                getGroups(audioBooks);
            }
        });
        this.rcv = rcv;
        registerAdapterDataObserver(new RecyclerView.AdapterDataObserver() {
            @Override
            public void onChanged() {
                super.onChanged();
                getGroups(DisplayListAdapter.this.model.getUsers(context).getValue());
            }
        });
    }

    @Override
    public int getItemViewType(int position) {
        return finalList.get(position).getType();
    }

    public void getGroups(List<AudioBook> books) {
        List<ListItems.ListItem> list = new ArrayList<>();
        Collections.sort(books, new Comparator<AudioBook>() {
            @Override
            public int compare(AudioBook o1, AudioBook o2) {
                return o1.displayName.compareTo(o2.displayName);
            }
        });
        for (AudioBook book : books){
            book.loadFromFile(context);
            list.add(new ListItems.AudioBookContainer(book));
        }
        List<Integer> letters = new ArrayList<>();
        for (ListItems.ListItem item : list){
            letters.add(item.getSnippet());
        }
        letters = new ArrayList<>(new HashSet<>(letters));
        for (Integer letter : letters) {
            list.add(new ListItems.Heading(letter));
        }
        Collections.sort(list, new Comparator<ListItems.ListItem>() {
            @Override
            public int compare(ListItems.ListItem o1, ListItems.ListItem o2) {
                int i = o1.getSnippet() - o2.getSnippet();
                if (i == 0) {
                    return o1.getType() - o2.getType();
                }
                return i;
            }
        });
        this.finalList = list;
    }

    public void filter(String filterTerm){
        List<AudioBook> books = model.getUsers(context).getValue();
        List<AudioBook> filtered = new ArrayList<>();
        for (AudioBook book : books) {
            if (book.displayName.toUpperCase().contains(filterTerm.toUpperCase())){
                filtered.add(book);
            }
        }
        Log.d("ASD", "filter: " + filtered.size());
        getGroups(filtered);
    }

    @NonNull
    @Override
    public MyViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v;
        switch (viewType){
            case ListItems.TYPE_HEADING:
                v = LayoutInflater.from(parent.getContext())
                        .inflate(R.layout.display_list_group, parent, false);
                return new MyViewHolder(v, false);
            case ListItems.TYPE_ITEM:
                v = LayoutInflater.from(parent.getContext())
                        .inflate(R.layout.display_list_item, parent, false);
                v.setOnClickListener(this);
                v.setOnLongClickListener(this);
                return new MyViewHolder(v, true);
        }
        return null;
    }

    @Override
    public void onClick(View v) {
        int position = rcv.getChildLayoutPosition(v);
        if (finalList.get(position).getType() == ListItems.TYPE_ITEM) {
            AudioBook book = ((ListItems.AudioBookContainer) finalList.get(position)).book;
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
        if (finalList.get(position).getType() == ListItems.TYPE_ITEM) {
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
        switch (finalList.get(position).getType()) {
            case ListItems.TYPE_ITEM:
                AudioBook book = ((ListItems.AudioBookContainer) finalList.get(position)).book;
                holder.textView.setText(book.displayName);
                holder.image.setImageBitmap(book.getAlbumArt(context));
                holder.image.setVisibility(View.VISIBLE);
                holder.textView.setSelected(selectedPos == position);
                break;

            case ListItems.TYPE_HEADING:
                String title = ((ListItems.Heading) finalList.get(position)).getTitle();
                holder.textView.setText(title);
                break;
        }
    }

    @Override
    public int getItemCount() {
        return finalList.size();
    }
}
