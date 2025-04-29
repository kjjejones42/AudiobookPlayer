package com.kjjejones42.audiobookplayer.database;

import androidx.room.TypeConverter;

import com.kjjejones42.audiobookplayer.MediaItem;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.Base64;
import java.util.List;

public class DataConverter {
    @TypeConverter
    public String fromMediaItemList(List<MediaItem> list) {
        try {
            if (list == null) return null;
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ObjectOutputStream oos = new ObjectOutputStream( baos );
            oos.writeObject(list);
            oos.close();
            return Base64.getEncoder().encodeToString(baos.toByteArray());
        } catch (IOException ignored) {
            return null;
        }
    }
    @TypeConverter
    @SuppressWarnings("unchecked")
    public List<MediaItem> toMediaItemList(String string) {
        try {
            if (string == null) return null;
            byte[] data = Base64.getDecoder().decode(string);
            ObjectInputStream ois = new ObjectInputStream(new ByteArrayInputStream(data));
            Object o = ois.readObject();
            ois.close();
            return (List<MediaItem>) o;
        } catch (IOException | ClassNotFoundException ignored) {
            return null;
        }
    }
}
