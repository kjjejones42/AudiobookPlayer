package com.kjjejones42.audiobookplayer.player;

import android.content.Context;
import android.support.v4.media.MediaMetadataCompat;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.kjjejones42.audiobookplayer.AudioBook;
import com.kjjejones42.audiobookplayer.database.AudiobookDatabase;

public class PlayerViewModel extends ViewModel {

    static private MediaMetadataCompat emptyMetadata;

    private final MutableLiveData<Boolean> isPlaying = new MutableLiveData<>();
    private final MutableLiveData<Long> position = new MutableLiveData<>();
    private final MutableLiveData<MediaMetadataCompat> metadata = new MutableLiveData<>();
    private final MutableLiveData<AudioBook> audioBook = new MutableLiveData<>();
    private final MutableLiveData<Boolean> startPlayback = new MutableLiveData<>();

    @NonNull
    LiveData<AudioBook> getAudioBook() {
        return audioBook;
    }

    void setAudioBook(AudioBook audioBook) {
        this.audioBook.setValue(audioBook);
    }

    LiveData<Boolean> getStartPlayback() {
        return startPlayback;
    }

    void setStartPlayback(boolean startPlayback) {
        Boolean b = this.startPlayback.getValue();
        if (b != null && b != startPlayback) {
            this.startPlayback.setValue(startPlayback);
        }
    }

    public PlayerViewModel(){
        super();
        isPlaying.setValue(true);
        clear();
    }

    private MediaMetadataCompat getEmptyMetadata(){
        if (emptyMetadata == null){
            emptyMetadata = new MediaMetadataCompat.Builder()
                            .putString(MediaMetadataCompat.METADATA_KEY_TITLE, "")
                            .putBitmap(MediaMetadataCompat.METADATA_KEY_ALBUM_ART, null)
                            .putLong(MediaMetadataCompat.METADATA_KEY_DURATION, 0L)
                            .putLong(MediaMetadataCompat.METADATA_KEY_TRACK_NUMBER, 0)
                            .putString(MediaMetadataCompat.METADATA_KEY_MEDIA_ID, "")
                            .build();
        }
        return emptyMetadata;
    }

    private void clear() {
        position.setValue(0L);
        metadata.setValue(getEmptyMetadata());
    }

    @NonNull
    LiveData<Boolean> getIsPlaying() {
        return isPlaying;
    }

    void setIsPlaying(boolean isPlaying) {
        Log.w("", "setIsPlaying | " + isPlaying);
        Boolean b = this.isPlaying.getValue();
        if (b != null && b != isPlaying) {
            this.isPlaying.setValue(isPlaying);
        }
    }

    @NonNull
    LiveData<Long> getPosition() {
        return position;
    }

    void setPosition(long position) {
        Log.w("", "Set Position" + position);
        Long l = this.position.getValue();
        if (l != null && l != position) {
            if (position > 0) {
                this.position.setValue(position);
            }
        }
    }

    public void updateBookFromDatabase(Context context) {
        AudioBook book = audioBook.getValue();
        if (book != null) {
            String bookId = audioBook.getValue().displayName;
            book = AudiobookDatabase.getInstance(context).audiobookDao().findByName(bookId);
            audioBook.setValue(book);
        }
    }

    @NonNull
    LiveData<MediaMetadataCompat> getMetadata() {
        return metadata;
    }

    void setMetadata(MediaMetadataCompat metadata){
        this.metadata.setValue(metadata);
        }


}
