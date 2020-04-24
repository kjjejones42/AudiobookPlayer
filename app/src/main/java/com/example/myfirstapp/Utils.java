package com.example.myfirstapp;

import android.content.Context;
import android.net.Uri;
import android.os.Environment;
import android.widget.Toast;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.PrintWriter;

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
            ObjectOutputStream oos = new ObjectOutputStream(fos);
            rootUri = uri;
            oos.writeObject(rootUri.toString());
            oos.close();
        } catch (Exception e) {
            e.printStackTrace();
            Utils.getInstance().logError(e, context);
        }
    }

    private final File f = new File(Environment.getExternalStorageDirectory().getAbsolutePath() + File.separator + "MyAudiobookPlayerLog.txt");

    public void logError(Throwable e, Context context) {
        synchronized (f) {
            try {
                FileOutputStream fos = new FileOutputStream(f, true);
                PrintWriter p = new PrintWriter(fos);
                e.printStackTrace(p);
                p.write(System.lineSeparator() + "--------------------");
                p.close();
                if (context != null) {
                    Toast.makeText(context, "Uncaught exception written to log", Toast.LENGTH_SHORT).show();
                }
            } catch (FileNotFoundException ex) {
                try {
                    if (f.createNewFile()) {
                        logError(ex, context);
                    }
                } catch (IOException ei) {
                    ei.printStackTrace();
                }

            }
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
            Utils.getInstance().logError(e, context);
        }
        return uri;
    }
}
