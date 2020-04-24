package com.example.myfirstapp;

import android.content.Context;
import android.net.Uri;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

public class Utils {
    private static Utils instance;
    private static String FILE_NAME = "ROOT_URI";

    public static Utils getInstance() {
        if (instance == null) {
            instance = new Utils();
        }
        return instance;
    }

    private Uri rootUri;

    private Utils() {}

    public Uri getRoot(Context context) {
        if (rootUri == null) {
            rootUri = loadUri(context);
        }
        return rootUri;
    }

    public void saveRoot(Uri uri, Context context) {
        try {
            FileOutputStream fos = context.openFileOutput(FILE_NAME, Context.MODE_PRIVATE);
            ObjectOutputStream oos= new ObjectOutputStream(fos);
            rootUri = uri;
            oos.writeObject(rootUri.toString());
            oos.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private Uri loadUri(Context context) {
        Uri uri = null;
        try {
            FileInputStream fis = context.openFileInput(FILE_NAME);
            ObjectInputStream ois = new ObjectInputStream(fis);
            uri = Uri.parse((String) ois.readObject());
            ois.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return uri;
    }
}
