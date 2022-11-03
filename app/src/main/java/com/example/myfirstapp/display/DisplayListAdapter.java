package com.example.myfirstapp.display;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.Intent;
import android.graphics.Bitmap;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.myfirstapp.AudioBook;
import com.example.myfirstapp.R;
import com.example.myfirstapp.player.PlayActivity;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;


public class DisplayListAdapter extends RecyclerView.Adapter<DisplayListAdapter.MyViewHolder> {

    private DisplayListViewModel model;
    @SuppressWarnings("CanBeFinal")
    private RecyclerView rcv;
    private DisplayListActivity activity;
    private int selectedPos = RecyclerView.NO_POSITION;
    private final View.OnClickListener onClickListener = v -> {
        int position = rcv.getChildLayoutPosition(v);
        List<ListItem> items = model.getListItems(activity).getValue();
        assert items != null;
        if (items.get(position).getHeadingOrItem() == ListItem.TYPE_ITEM) {
            AudioBook book = ((ListItem.AudioBookContainer) items.get(position)).book;
            selectedPos = position;
            notifyItemChanged(position);
            startAudioBook(book);
        }
    };
    private List<ListItem> currentItems;
    private final View.OnLongClickListener onLongClickListener = v -> {
        List<ListItem> items = model.getListItems(activity).getValue();
        String[] statuses = AudioBook.getStatusMap().values().toArray(new String[0]);
        assert items != null;
        AudioBook book = ((ListItem.AudioBookContainer) items.get(rcv.getChildLayoutPosition(v))).book;
        new AlertDialog.Builder(v.getContext())
                .setSingleChoiceItems(statuses, book.getStatus(), (dialog, which) -> {
                    book.setStatus(which);
                    book.saveConfig(v.getContext(), false);
                    recalculateListFromModel();
                    dialog.dismiss();
                }).setTitle("Choose this book's status.")
                .setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss())
                .show();
        return false;
    };

    private void startAudioBook(AudioBook book) {
        Intent intent = new Intent(activity, PlayActivity.class);
        intent.putExtra(DisplayListActivity.INTENT_PLAY_FILE, book);
        intent.putExtra(DisplayListActivity.INTENT_START_PLAYBACK, true);
        activity.setLastBookStarted(book);
        activity.startActivity(intent);
    }

    DisplayListAdapter(@NonNull DisplayListViewModel model, @NonNull RecyclerView rcv, DisplayListActivity activity) {
        this.model = model;
        this.activity = activity;
        this.rcv = rcv;
        model.getSavedBooks(activity).observe(activity, this::recalculateList);
        model.getListItems(activity).observe(activity, this::selectivelyNotify);
        setHasStableIds(true);
    }

    void recalculateListFromModel() {
        recalculateList(model.getSavedBooks(activity).getValue());
    }

    @SuppressLint("NotifyDataSetChanged")
    private void selectivelyNotify(List<ListItem> newItems) {
        List<ListItem> oldItems = currentItems;
        currentItems = newItems;
        if (newItems.equals(oldItems)) {
            return;
        }
        if (oldItems == null) {
            notifyItemRangeInserted(0, newItems.size());
            return;
        }
        if (oldItems.size() == newItems.size()) {
            notifyDataSetChanged();
            return;
        }
        boolean remove = oldItems.size() > newItems.size();
        List<ListItem> larger = remove ? oldItems : newItems;
        List<ListItem> smaller = remove ? newItems : oldItems;
        List<Integer> indexes = new ArrayList<>();
        for (int i = 0; i < larger.size(); i++) {
            if (!smaller.contains(larger.get(i))) {
                indexes.add(i);
            }
        }
        Collections.sort(indexes, (o1, o2) -> o2 - o1);
        if (remove) {
            for (int i : indexes) {
                notifyItemRemoved(i);
            }
        } else {
            for (int i : indexes) {
                notifyItemInserted(i);
            }
        }
        rcv.scrollToPosition(0);
    }

    private List<ListItem> getItems() {
        if (currentItems != null) {
            return currentItems;
        }
        return Objects.requireNonNull(model.getListItems(activity).getValue());
    }

    @Override
    public long getItemId(int position) {
        return getItems().get(position).getId();
    }

    @Override
    public int getItemViewType(int position) {
        return getItems().get(position).getHeadingOrItem();
    }

    private List<ListItem> getItemsFromBooks(List<AudioBook> books) {
        List<ListItem> list = new ArrayList<>();
        Collections.sort(books, (o1, o2) -> o1.displayName.compareTo(o2.displayName));
        for (AudioBook book : books) {
            list.add(new ListItem.AudioBookContainer(book));
        }
        List<Integer> letters = new ArrayList<>();
        for (ListItem item : list) {
            letters.add(item.getCategory());
        }
        letters = new ArrayList<>(new HashSet<>(letters));
        for (Integer letter : letters) {
            list.add(new ListItem.Heading(letter));
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
        return list;
    }

    private void recalculateList(List<AudioBook> books) {
        List<ListItem> list = getItemsFromBooks(books);
        if (!list.equals(getItems())) {
            model.setListItems(list);
        }
    }

    void filter(String filterTerm) {
        List<AudioBook> books = model.getSavedBooks(activity).getValue();
        if (books != null) {
            if (filterTerm == null) {
                recalculateList(books);
            } else {
                List<AudioBook> filtered = new ArrayList<>();
                for (AudioBook book : books) {
                    if (book.toString().toUpperCase().contains(filterTerm.toUpperCase())) {
                        filtered.add(book);
                    }
                }
                recalculateList(filtered);
            }
        }
    }

    @NonNull
    @Override
    public MyViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v;
        if (viewType == ListItem.TYPE_HEADING) {
            v = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.display_list_group, parent, false);
            return new MyViewHolder(v, false);
        } else {
            v = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.display_list_item, parent, false);
            v.setOnClickListener(onClickListener);
            v.setOnLongClickListener(onLongClickListener);
            return new MyViewHolder(v, true);
        }
    }

    @SuppressLint("SetTextI18n")
    @Override
    public void onBindViewHolder(@NonNull MyViewHolder holder, int position) {
        List<ListItem> items = getItems();
        assert items != null;
        switch (items.get(position).getHeadingOrItem()) {
            case ListItem.TYPE_ITEM:
                AudioBook book = ((ListItem.AudioBookContainer) items.get(position)).book;
                holder.textView.setText(book.displayName + " | " + PlayActivity.msToMMSS(book.getTotalDuration()));
                holder.artist.setText(book.author);
                holder.textView.setSelected(selectedPos == position);
                new Thread(() -> {
                    Bitmap thumbnail = book.getThumbnail(activity);
                    holder.v.post(() -> {
                        holder.image.setImageBitmap(thumbnail);
                        holder.image.setVisibility(View.VISIBLE);
                    });
                }).start();
                break;

            case ListItem.TYPE_HEADING:
                String title = ((ListItem.Heading) items.get(position)).getHeadingTitle();
                holder.textView.setText(title);
                break;
        }
    }

    @Override
    public int getItemCount() {
        return getItems().size();
    }

    static class MyViewHolder extends RecyclerView.ViewHolder {
        final TextView textView;
        final View v;
        TextView artist;
        ImageView image;

        MyViewHolder(View v, boolean isItem) {
            super(v);
            this.v = v;
            if (isItem) {
                textView = v.findViewById(R.id.listItemText);
                image = v.findViewById(R.id.listImageView);
                artist = v.findViewById(R.id.artist);
                image.setVisibility(View.INVISIBLE);
            } else {
                textView = (TextView) v;
            }
        }
    }
}
