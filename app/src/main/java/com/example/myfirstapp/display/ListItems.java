package com.example.myfirstapp.display;

import androidx.annotation.NonNull;

import com.example.myfirstapp.AudioBook;

class ListItems {
    final static int TYPE_HEADING = 0;
    final static int TYPE_ITEM = 1;

    public static abstract class ListItem {
        public abstract int getCategory();

        public abstract long getTimeStamp();

        public abstract int getHeadingOrItem();
    }

    public static class AudioBookContainer extends ListItem {
        final AudioBook book;

        AudioBookContainer(AudioBook book) {
            this.book = book;
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
        final int category;

        Heading(int title) {
            this.category = title;
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
