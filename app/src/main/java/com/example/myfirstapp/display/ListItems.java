package com.example.myfirstapp.display;

import com.example.myfirstapp.defs.AudioBook;

class ListItems {
    final static int TYPE_HEADING = 0;
    final static int TYPE_ITEM = 1;

    public static abstract class ListItem  {
        public abstract int getType();
        public abstract String getSnippet();
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
        public String getSnippet() {
            return book.displayName.substring(0,1);
        }

    }

    public static class Heading extends ListItem {
        final String title;
        Heading(String title) {
            this.title = title;
        }
        @Override
        public int getType() {
            return TYPE_HEADING;
        }

        @Override
        public String getSnippet() {
            return title;
        }
    }
}
