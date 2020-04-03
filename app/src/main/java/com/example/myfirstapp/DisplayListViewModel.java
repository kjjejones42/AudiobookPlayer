package com.example.myfirstapp;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import java.util.List;

public class DisplayListViewModel extends ViewModel {
        private MutableLiveData<List<AudioBook>> users;

        public LiveData<List<AudioBook>> getUsers() {
            if (users == null) {
                users = new MutableLiveData<>();
                loadUsers();
            }
            return users;
        }

        private void loadUsers() {
            // Do an asynchronous operation to fetch users.
        }
}