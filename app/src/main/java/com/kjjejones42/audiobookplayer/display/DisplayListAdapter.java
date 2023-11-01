package com.kjjejones42.audiobookplayer.display;

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

import com.kjjejones42.audiobookplayer.AudioBook;
import com.kjjejones42.audiobookplayer.R;
import com.kjjejones42.audiobookplayer.database.AudiobookDao;
import com.kjjejones42.audiobookplayer.database.AudiobookDatabase;
import com.kjjejones42.audiobookplayer.player.PlayActivity;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.concurrent.TimeUnit;


public class DisplayListAdapter extends RecyclerView.Adapter<DisplayListAdapter.MyViewHolder> {

    private DisplayListViewModel model;
    private RecyclerView rcv;
    final private DisplayListActivity activity;
    private int selectedPos = RecyclerView.NO_POSITION;
    private final View.OnClickListener onClickListener = v -> {
        int position = rcv.getChildLayoutPosition(v);
        List<ListItem> items = model.getListItems().getValue();
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
        List<ListItem> items = model.getListItems().getValue();
        String[] statuses = AudioBook.getStatusMap().values().toArray(new String[0]);
        assert items != null;
        AudiobookDao dao = AudiobookDatabase.getInstance(v.getContext()).audiobookDao();
        ListItem.AudioBookContainer container = ((ListItem.AudioBookContainer) items.get(rcv.getChildLayoutPosition(v)));
        String bookId = container.book.displayName;
        new AlertDialog.Builder(v.getContext())
                .setSingleChoiceItems(statuses, dao.getStatus(bookId), (dialog, which) -> {
                    AudioBook book = dao.findByName(bookId);
                    book.setStatus(which);
                    dao.update(book);
                    dialog.dismiss();
                }).setTitle("Choose this book's status.")
                .setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss())
                .show();
        return false;
    };

    DisplayListAdapter(@NonNull DisplayListViewModel model, @NonNull RecyclerView rcv, DisplayListActivity activity) {
        this.model = model;
        this.activity = activity;
        this.rcv = rcv;
        model.getListItems().observe(activity, this::selectivelyNotify);
        setHasStableIds(true);
    }

    private void startAudioBook(AudioBook book) {
//        AudiobookDatabase.getInstance(activity).audiobookDao()
//                .getAllAndObserve().removeObservers(activity);
        Intent intent = new Intent(activity, PlayActivity.class);
        intent.putExtra(DisplayListActivity.INTENT_PLAY_FILE, book.displayName);
        intent.putExtra(DisplayListActivity.INTENT_START_PLAYBACK, true);
        activity.startActivity(intent);
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
        indexes.sort((o1, o2) -> o2 - o1);
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
        return Objects.requireNonNull(model.getListItems().getValue());
    }

    @Override
    public long getItemId(int position) {
        return getItems().get(position).getId();
    }

    @Override
    public int getItemViewType(int position) {
        return getItems().get(position).getHeadingOrItem();
    }


    void filter(String filterTerm) {
        List<AudioBook> books = model.getSavedBooks().getValue();
        if (books != null && filterTerm != null) {
            List<AudioBook> filtered = new ArrayList<>();
            for (AudioBook book : books) {
                if (book.toString().toUpperCase().contains(filterTerm.toUpperCase())) {
                    filtered.add(book);
                }
            }
            model.setFilteredListItems(filtered);
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
                holder.duration.setText(msToReadableDuration(book.getTotalDuration()));
                holder.textView.setText(book.displayName);
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

    private String msToReadableDuration(long ms) {
        long minutes = TimeUnit.MILLISECONDS.toMinutes(ms) % 60;
        long hours = TimeUnit.MILLISECONDS.toHours(ms);
        if (hours > 0) {
            return String.format(Locale.getDefault(), "%dh %02dm", hours, minutes);
        }
        return String.format(Locale.getDefault(), "%02dm", minutes);
    }

    @Override
    public int getItemCount() {
        return getItems().size();
    }

    static class MyViewHolder extends RecyclerView.ViewHolder {
        final TextView textView;
        final View v;
        TextView artist;

        TextView duration;
        ImageView image;

        MyViewHolder(View v, boolean isItem) {
            super(v);
            this.v = v;
            if (isItem) {
                textView = v.findViewById(R.id.listItemText);
                image = v.findViewById(R.id.listImageView);
                artist = v.findViewById(R.id.artist);
                duration = v.findViewById(R.id.listItemDuration);
                image.setVisibility(View.INVISIBLE);
            } else {
                textView = (TextView) v;
            }
        }
    }
}
