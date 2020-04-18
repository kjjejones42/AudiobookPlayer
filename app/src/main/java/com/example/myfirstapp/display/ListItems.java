package com.example.myfirstapp.display;

import com.example.myfirstapp.defs.AudioBook;

import java.util.HashMap;

class ListItems {
    final static int TYPE_HEADING = 0;
    final static int TYPE_ITEM = 1;
    private static HashMap<Integer, String> map;

    private static HashMap<Integer, String> getMap() {
        if (map == null) {
            map = new HashMap<>();
            map.put(AudioBook.STATUS_FINISHED, "Finished");
            map.put(AudioBook.STATUS_IN_PROGRESS, "In Progress");
            map.put(AudioBook.STATUS_NOT_BEGUN, "Not Begun");
        }
        return map;
    }

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
            return getMap().get(category);
        }
    }
}
