package uk.wjdp.mp3player;


import android.content.ContentResolver;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.v4.view.GestureDetectorCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.GestureDetector;
import android.view.MotionEvent;


public class TouchScreen extends AppCompatActivity {
    private GestureDetectorCompat GDetect;
    private PlayerService.PlayerBinder myPlayerService = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_touch_screen);

        GDetect = new GestureDetectorCompat(this, new LearnGesture());

        final SongList songList = getMedia();

        for (int i = 0; i < songList.song_list.size(); i++){
            SongList.Song song = songList.song_list.get(i);
            songSelected(song);
        }


    }

    protected SongList getMedia() {
        // Get a list of songs from Android's external storage via a content provider
        SongList songList = new SongList();

        ContentResolver musicResolver = getContentResolver();
        Uri musicUri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
        Cursor musicCursor = musicResolver.query(musicUri, null, null, null, null);



        if (musicCursor != null && musicCursor.moveToFirst()) {
            // Grab column indexes for the fields we're interested in
            int idColumn     = musicCursor.getColumnIndex(MediaStore.Audio.Media._ID);
            int titleColumn  = musicCursor.getColumnIndex(MediaStore.Audio.Media.TITLE);
            int artistColumn = musicCursor.getColumnIndex(MediaStore.Audio.Media.ARTIST);
            int pathColumn = musicCursor.getColumnIndex(MediaStore.Audio.Media.DATA);

            // Iterate over the cursor
            do {
                // Transpose data retrieved from the cursor into a songList
                long songId = musicCursor.getLong(idColumn);
                String songTitle = musicCursor.getString(titleColumn);
                String songArtist = musicCursor.getString(artistColumn);
                String songPath = musicCursor.getString(pathColumn);

                songList.addSong(new SongList.Song(songId, songTitle, songArtist, songPath));
            } while(musicCursor.moveToNext());
        }

        musicCursor.close();
        return songList;
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        this.GDetect.onTouchEvent(event);
        return true;
    }

    class LearnGesture extends GestureDetector.SimpleOnGestureListener {

        @Override
        public boolean onDoubleTapEvent(MotionEvent e) {
            startActivity(new Intent(TouchScreen.this, MainActivity.class));
            return true;
        }


        @Override
        public boolean onFling(MotionEvent event1, MotionEvent event2,
                               float velocityX, float velocityY) {

            float distanceX = event2.getX() - event1.getX();
            float distanceY = event2.getY() - event1.getY();
            if (Math.abs(distanceX) > Math.abs(distanceY)) {
                if (distanceX > 0) {
                    Log.d("ORAORAORA", "THIS IS NEXT ");

                } else {

                }
                return true;
            }
            return false;
        }

        @Override
        public void onLongPress(MotionEvent event1) {


        }


        @Override
        public boolean onDown(MotionEvent event) {
            return true;
        }


    }
    void songSelected(SongList.Song song) {
        myPlayerService.setup(song);
    }
}