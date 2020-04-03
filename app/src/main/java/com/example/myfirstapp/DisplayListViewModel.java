package com.example.myfirstapp;

import android.content.Context;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.util.List;

public class DisplayListViewModel extends ViewModel {
        private MutableLiveData<List<AudioBook>> users;

        LiveData<List<AudioBook>> getUsers(Context context) {
            if (users == null) {
                users = new MutableLiveData<>();
                loadUsers(context);
            }
            return users;
        }

        private void loadUsers(Context context) {
            try {
                FileInputStream fis = context.openFileInput(FileScannerWorker.LIST_OF_DIRS);
                ObjectInputStream oos = new ObjectInputStream(fis);
                users.setValue((List<AudioBook>) oos.readObject());
                oos.close();
            } catch (IOException | ClassNotFoundException e) {
                e.printStackTrace();
            }
        }
}