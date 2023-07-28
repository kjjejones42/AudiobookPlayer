package com.kjjejones42.audiobookplayer.display;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.kjjejones42.audiobookplayer.AudioBook;

import java.util.HashMap;
import java.util.Map;

abstract class ListItem {

    final static int TYPE_HEADING = 0;
    final static int TYPE_ITEM = 1;

    final private static Map<String, Long> idMap = new HashMap<>();

    static long getId(String name) {
        long id;
        if (idMap.containsKey(name)) {
            Long value = idMap.get(name);
            if (value == null) {
                throw new RuntimeException();
            }
            id = value;
        } else {
            id = idMap.entrySet().size();
            idMap.put(name, id);
        }
        return id;
    }

    public abstract long getId();

    public abstract int getCategory();

    public abstract long getTimeStamp();

    public abstract int getHeadingOrItem();

    @Override
    public boolean equals(@Nullable Object obj) {
        if (obj instanceof ListItem) {
            return obj.toString().equals(this.toString());
        }
        return super.equals(obj);
    }

    public static class AudioBookContainer extends ListItem {
        final AudioBook book;
        final private long id;

        AudioBookContainer(AudioBook book) {
            this.book = book;
            this.id = getId(book.getUniqueId());
        }

        @Override
        public long getId() {
            return id;
        }

        @Override
        public int getCategory() {
            return book.getStatus();
        }

        @Override
        public int getHeadingOrItem() {
            return TYPE_ITEM;
        }


        @Override
        public long getTimeStamp() {
            return book.lastSavedTimestamp;
        }

        @NonNull
        @Override
        public String toString() {
            return book.displayName;
        }
    }

    public static class Heading extends ListItem {
        final private int category;
        final private long id;

        Heading(int title) {
            this.category = title;
            this.id = getId(getHeadingTitle());
        }

        @Override
        public long getId() {
            return id;
        }

        @Override
        public int getCategory() {
            return category;
        }

        @Override
        public int getHeadingOrItem() {
            return TYPE_HEADING;
        }

        @Override
        public long getTimeStamp() {
            return 0;
        }

        String getHeadingTitle() {
            return AudioBook.getStatusMap().get(category);
        }

        @NonNull
        @Override
        public String toString() {
            return getHeadingTitle();
        }

    }
}
