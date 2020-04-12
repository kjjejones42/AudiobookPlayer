package com.example.myfirstapp.display;

import com.example.myfirstapp.defs.AudioBook;

import java.util.HashMap;

class ListItems {
    final static int TYPE_HEADING = 0;
    final static int TYPE_ITEM = 1;
    private static HashMap<Integer, String> map;

    private static HashMap<Integer, String> getMap(){
        if (map == null) {
            map = new HashMap<>();
            map.put(AudioBook.STATUS_FINISHED, "Finished");
            map.put(AudioBook.STATUS_IN_PROGRESS, "In Progress");
            map.put(AudioBook.STATUS_NOT_BEGUN, "Not Begun");
        }
        return map;
    }

    public static abstract class ListItem  {
        public abstract int getType();
        public abstract int getSnippet();
    }

    public static class AudioBookContainer extends ListItem {
        final AudioBook book;
        AudioBookContainer(AudioBook book){
            this.book = book;
        }
        @Override
        public int getType() {
            return TYPE_ITEM;
        }

        @Override
        public int getSnippet() {
            return book.getStatus();
//            return getMap().get(book.getStatus());
        }

    }

    public static class Heading extends ListItem {
        final int title;
        Heading(int title) {
            this.title = title;
        }
        @Override
        public int getType() {
            return TYPE_HEADING;
        }

        @Override
        public int getSnippet() {
            return title;
        }

        public String getTitle(){
            return getMap().get(title);
        }
    }
}
