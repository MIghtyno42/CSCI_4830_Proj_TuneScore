package uk.wjdp.mp3player;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.IBinder;
import android.provider.MediaStore;
import android.support.v4.view.GestureDetectorCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.widget.ListView;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

import uk.wjdp.mp3player.SongList.Song;

public class MainActivity extends AppCompatActivity {

    ListView listView_songs;

    private GestureDetectorCompat GDetect;

    final String TAG = "MainActivity";

    private PlayerService.PlayerBinder myPlayerService;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        GDetect = new GestureDetectorCompat(this, new LearnGesture());
        Log.d(TAG, "onCreate");

        // Store ref to UI components

        listView_songs = (ListView)findViewById(R.id.song_list);


        // Create a thread to fetch the song list
        // We do this in a thread to prevent blocking the UI during the app's creation
        new Thread(new Runnable() {
            @Override
            public void run() {
                // Get phone's media, making this final ensures it's available to the next Runnable


                // We now need to update the UI, which we cannot do inside a thread as the Android
                // UI is not thread-safe. So we post to the message queue of the UI thread.
//                MainActivity.this.runOnUiThread(new Runnable() {
//                    @Override
//                    public void run() {
//                        Log.d(TAG, "adding songs to listView from posted Runnable");
//                        // Create an adapter from the song list
//                        final ArrayAdapter<Song> songItemsAdapter = new ArrayAdapter<Song>(MainActivity.this,
//                                android.R.layout.simple_list_item_1, songList.song_list);
//
//                        // Pass that adapter to the list view, the list will update with the contents of the adapter
//                        listView_songs.setAdapter(songItemsAdapter);
//                        for (int i = 0; i < songList.song_list.size(); i++){
//                            Song song = songList.song_list.get(i);
//                            songSelected(song);
//                        }
//                        // Set a click listener for the list view
//                        listView_songs.setOnItemClickListener(new AdapterView.OnItemClickListener() {
//                            @Override
//                            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
//                                // User 'clicks' a song and an activity method is fired with that song
//                                // As the listview has the full song objects we don't have to do any lookup here
//
//                                for (int i = 0; i < songList.song_list.size(); i++){
//                                    Song song = songList.song_list.get(i);
//                                    songSelected(song);
//                                }
//
//                            }
//
//                        });
//
//                    }
//                });

            }
        }).start();

    }


    @Override
    public boolean onTouchEvent(MotionEvent event){
        this.GDetect.onTouchEvent(event);
        return true;
    }

    class LearnGesture extends GestureDetector.SimpleOnGestureListener {

        @Override
        public boolean onDoubleTapEvent(MotionEvent e) {
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
                    myPlayerService.stop();
                }
                return true;
            }
            return false;
        }

        @Override
        public void onLongPress(MotionEvent event1) {
            myPlayerService.pause();

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
            FileOutputStream openFileOutput = null;
            File file;

            try {
                file = new File("D:\\TuneScoreGestureData.txt");
                openFileOutput = new FileOutputStream(file);
                if (!file.exists()) {
                    file.createNewFile();
                }
                byte[] contentInBytes = "LOLOLO".getBytes();
                openFileOutput.write(contentInBytes);
                openFileOutput.flush();
                openFileOutput.close();

                Log.d(TAG, "Succsesfully written");
            } catch (IOException e) {
                e.printStackTrace();
            }finally {
                try {
                    if (openFileOutput != null) {
                        openFileOutput.close();
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
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


