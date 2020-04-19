package com.example.myfirstapp.display;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.lifecycle.LifecycleOwner;
import androidx.recyclerview.widget.RecyclerView;

import com.example.myfirstapp.player.PlayActivity;
import com.example.myfirstapp.R;
import com.example.myfirstapp.defs.AudioBook;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;


public class DisplayListAdapter extends RecyclerView.Adapter<DisplayListAdapter.MyViewHolder> {

    static class MyViewHolder extends RecyclerView.ViewHolder {
        TextView textView;
        TextView artist;
        ImageView image;
        View v;

        MyViewHolder(View v, boolean isItem) {
            super(v);
            this.v = v;
            if (isItem) {
                textView = v.findViewById(R.id.listItemText);
                image = v.findViewById(R.id.listImageView);
                artist = v.findViewById(R.id.artist);
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


    private View.OnClickListener ocl = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            int position = rcv.getChildLayoutPosition(v);
            if (finalList.get(position).getHeadingOrItem() == ListItems.TYPE_ITEM) {
                AudioBook book = ((ListItems.AudioBookContainer) finalList.get(position)).book;
                selectedPos = position;
                notifyItemChanged(position);
                Intent intent = new Intent(v.getContext(), PlayActivity.class);
                intent.putExtra(DisplayListActivity.PLAY_FILE, book);
                v.getContext().startActivity(intent);
            }
        }
    };

    private View.OnLongClickListener olcl = v -> {
//        final int position = rcv.getChildLayoutPosition(v);
//        if (finalList.get(position).getType() == ListItems.TYPE_ITEM) {
//            final EditText et = new EditText(v.getContext());
//            et.setInputType(InputType.TYPE_CLASS_TEXT);
//            new AlertDialog.Builder(v.getContext()).setMessage("Enter title")
//                    .setView(et)
//                    .setPositiveButton("Change", new DialogInterface.OnClickListener() {
//                        @Override
//                        public void onClick(DialogInterface dialog, int which) {
//                            Log.d(TAG, "onClick: " + et.getText());
//                        }
//                    })
//                    .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
//                        @Override
//                        public void onClick(DialogInterface dialog, int which) {
//                            dialog.cancel();
//                        }
//                    })
//                    .show();
//        }
        return false;
    };

    DisplayListAdapter(DisplayListViewModel model, RecyclerView rcv, LifecycleOwner lco) {
        this.model = model;
        this.context = rcv.getContext();
        model.getUsers(rcv.getContext()).observe(lco, this::getGroups);
        this.rcv = rcv;
        registerAdapterDataObserver(new RecyclerView.AdapterDataObserver() {
            @Override
            public void onChanged() {
                super.onChanged();
                updateFromDisk();
            }
        });
    }

    private void updateFromDisk() {
        getGroups(DisplayListAdapter.this.model.getUsers(context).getValue());
    }

    @Override
    public int getItemViewType(int position) {
        return finalList.get(position).getHeadingOrItem();
    }

    private void getGroups(List<AudioBook> books) {
        List<ListItems.ListItem> list = new ArrayList<>();
        Collections.sort(books, (o1, o2) -> o1.displayName.compareTo(o2.displayName));
        for (AudioBook book : books) {
            book.loadFromFile(context);
            list.add(new ListItems.AudioBookContainer(book));
        }
        List<Integer> letters = new ArrayList<>();
        for (ListItems.ListItem item : list) {
            letters.add(item.getCategory());
        }
        letters = new ArrayList<>(new HashSet<>(letters));
        for (Integer letter : letters) {
            list.add(new ListItems.Heading(letter));
        }
        Collections.sort(list, (o1, o2) -> {
            int i = o1.getCategory() - o2.getCategory();
            if (i == 0) {
                int j = o1.getHeadingOrItem() - o2.getHeadingOrItem();
                if (j == 0) {
                    return (int) (o2.getTimeStamp() - o1.getTimeStamp());
                }
                return j;
            }
            return i;
        });
        this.finalList = list;
    }

    void filter(String filterTerm) {
        List<AudioBook> books = model.getUsers(context).getValue();
        if (books != null) {
            List<AudioBook> filtered = new ArrayList<>();
            for (AudioBook book : books) {
                if (book.toString().toUpperCase().contains(filterTerm.toUpperCase())) {
                    filtered.add(book);
                }
            }
            getGroups(filtered);
        }
    }

    @NonNull
    @Override
    public MyViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v;
        if (viewType == ListItems.TYPE_HEADING) {
            v = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.display_list_group, parent, false);
            return new MyViewHolder(v, false);
        }
        v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.display_list_item, parent, false);
        v.setOnClickListener(ocl);
        v.setOnLongClickListener(olcl);
        return new MyViewHolder(v, true);
    }


    @Override
    public void onBindViewHolder(@NonNull MyViewHolder holder, int position) {
        switch (finalList.get(position).getHeadingOrItem()) {
            case ListItems.TYPE_ITEM:
                AudioBook book = ((ListItems.AudioBookContainer) finalList.get(position)).book;
                holder.textView.setText(book.displayName);
                holder.artist.setText(book.author);
                holder.textView.setSelected(selectedPos == position);
                new Thread(() -> {
                    Bitmap thumbnail = book.getThumbnail((Activity) rcv.getContext());
                    holder.v.post(() -> {
                        holder.image.setImageBitmap(thumbnail);
                        holder.image.setVisibility(View.VISIBLE);
                    });
                }).start();
                break;

            case ListItems.TYPE_HEADING:
                String title = ((ListItems.Heading) finalList.get(position)).getHeadingTitle();
                holder.textView.setText(title);
                break;
        }
    }

    @Override
    public int getItemCount() {
        return finalList.size();
    }
}
