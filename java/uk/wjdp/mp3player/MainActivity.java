package uk.wjdp.mp3player;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.res.AssetManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.IBinder;
import android.provider.MediaStore;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;

import android.support.v4.view.GestureDetectorCompat;

import android.view.GestureDetector;
import android.view.MotionEvent;

import java.io.FileOutputStream;


import uk.wjdp.mp3player.SongList.Song;

public class MainActivity extends AppCompatActivity {

    private GestureDetectorCompat GDetect;
    public static final int REQUEST_CODE = 1;
    final String TAG = "MainActivity";

    String SongGrabberName = null;
    private PlayerService.PlayerBinder myPlayerService;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        GDetect = new GestureDetectorCompat(this, new LearnGesture());
        Log.d(TAG, "onCreate");
        StringBuilder buf=new StringBuilder();


        String filename = "myfile";
        String string = "Hello world!";
        FileOutputStream outputStream;

        try {
            outputStream = openFileOutput(filename, Context.MODE_PRIVATE);
            outputStream.write(string.getBytes());
            outputStream.close();
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        try {
            super.onActivityResult(requestCode, resultCode, data);

            if (requestCode == REQUEST_CODE  && resultCode  == RESULT_OK) {

                String requiredValue = data.getStringExtra("Key");
                SongGrabberName = requiredValue;
                Log.d("COMPLETIO", requiredValue);
            }
        } catch (Exception ex) {

        }

    }

    @Override
    public boolean onTouchEvent(MotionEvent event){
        this.GDetect.onTouchEvent(event);
        return true;
    }

    class LearnGesture extends GestureDetector.SimpleOnGestureListener {

        @Override
        public boolean onDoubleTapEvent(MotionEvent e) {
            Log.d("GESTUREZONE", "ACTIVATE PLAY");
            myPlayerService.play();
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
                    myPlayerService.next();
                }
                else {
                    Log.d("GESTUREZONE","stop activated");
                    myPlayerService.stop();
                }
                return true;
            }
            else{
                if (distanceY > 0){
                    //startActivity(new Intent(MainActivity.this, MainActivity.class));
                }
                else{

                    Intent intent = new Intent(MainActivity.this, GestureSearcher.class);
                    startActivityForResult(intent, REQUEST_CODE);




                    /*Bundle extras = getIntent().getExtras();
                    String value = null;
                    while (value == null){
                        value = extras.getString("KEY");
                    }

                    if (value == "Last Train Home"){
                        Log.d("HEHE", "WESFEA");
                    }
                    else{
                        Log.d("HEHE", "MISS");
                    }*/
                }
            }
            return false;
        }

        @Override
        public void onLongPress(MotionEvent event1) {
            myPlayerService.pause();
            Log.d("GESTUREZONE","ZA WARUDO");

        }


        @Override
        public boolean onDown(MotionEvent event){
            return true;
        }


    }

    @Override
    protected void onStart() {
        super.onStart();
        Log.d(TAG, "onStart");
        Intent intent= new Intent(this, PlayerService.class);
        // Have to start using startService so service isn't killed when this activity unbinds from
        // it onStop
        startService(intent);

        bindService(intent, playerServiceConnection, 0);

        // Register a receiver to the service's callbacks
        registerReceiver(receiver, new IntentFilter(PlayerService.NOTIFICATION));
    }

    @Override
    protected void onStop() {
        super.onStop();
        Log.d(TAG, "onStop");
        // Unbind so activity can sleep
        unbindService(playerServiceConnection);
    }

    private ServiceConnection playerServiceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            Log.d(TAG, "onServiceConnected");
            // Store a ref to the service
            myPlayerService = (PlayerService.PlayerBinder) service;
            final SongList songList = getMedia();
            Log.d(TAG, "Songs: " + songList.song_list.size());
            for (int i = 0; i < songList.song_list.size(); i++){
                Song song = songList.song_list.get(i);
                songSelected(song);
            }
            // Request the service sends us the current state so UI can be updated
            myPlayerService.update_state();
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            Log.d(TAG, "onServiceDisconnected");
            myPlayerService = null;
        }
    };

    // Broadcast receiving

    protected BroadcastReceiver receiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            // Grab extra vars attached to the broadcast
            Bundle bundle = intent.getExtras();
            if (bundle != null) {
                // Extract vars
                String callback = bundle.getString(PlayerService.CALLBACK);
                String artist = bundle.getString(PlayerService.SONG_ARTIST);
                String title = bundle.getString(PlayerService.SONG_TITLE);
                int queue = bundle.getInt(PlayerService.QUEUE);

                // Perform some UI work depending on callback
                Boolean nextState = queue > 1;

            }
        }
    };

    // Media functions

    protected SongList getMedia() {
        // Get a list of songs from Android's external storage via a content provider
        SongList songList = new SongList();

        ContentResolver musicResolver = getContentResolver();
        Uri musicUri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
        Cursor musicCursor = musicResolver.query(musicUri, null, null, null, null);

        Log.d(TAG, "Scanning thru media");

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
                Log.d(TAG, songTitle);
            songList.addSong(new Song(songId, songTitle, songArtist, songPath));
        } while(musicCursor.moveToNext());
        }

        musicCursor.close();
        return songList;
    }

    // State changes

    void songSelected(Song song) {
        myPlayerService.setup(song);
    }


}


