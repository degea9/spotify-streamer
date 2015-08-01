package com.github.mjhassanpur.spotifystreamer.ui;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.support.v4.app.FragmentManager;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;

import com.github.mjhassanpur.spotifystreamer.MusicService;
import com.github.mjhassanpur.spotifystreamer.R;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.List;

import kaaes.spotify.webapi.android.models.Track;

public class TopTracksActivity extends AppCompatActivity implements TopTracksFragment.Callback,
        MusicService.Callback {

    private final String KEY_TRACKS = "tracks";
    private final String KEY_SELECTED_TRACK = "selectedTrack";
    private final Type mTrackListType = new TypeToken<List<Track>>() {}.getType();
    private MusicService mBoundService;
    private boolean mIsBound = false;
    private int mTrackPosition;
    private List<Track> mTracks;
    private boolean mServiceStarted = false;
    private MenuItem mPlayingItem;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_top_tracks);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        FragmentManager fm = getSupportFragmentManager();
        TopTracksFragment tracksFragment = (TopTracksFragment) fm.findFragmentById(R.id.fragment_container);

        if (tracksFragment == null) {
            tracksFragment = new TopTracksFragment();
            fm.beginTransaction().add(R.id.fragment_container, tracksFragment).commit();
        }
        doBindService();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_top_tracks, menu);
        mPlayingItem = menu.findItem(R.id.action_playing);
        if (mBoundService != null && mBoundService.hasServiceStarted()) {
            mPlayingItem.setVisible(true);
        }
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.action_settings) {
            Intent intent = new Intent(this, SettingsActivity.class);
            startActivity(intent);
            return true;
        } else if (item.getItemId() == R.id.action_playing) {
            Gson gson = new Gson();
            Intent intent = new Intent(this, PlayerActivity.class);
            intent.putExtra(KEY_TRACKS, gson.toJson(mTracks, mTrackListType));
            intent.putExtra(KEY_SELECTED_TRACK, mTrackPosition);
            startActivity(intent);
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onItemSelected(List<Track> tracks, int position) {
        mTracks = tracks;
        mTrackPosition = position;
        Gson gson = new Gson();
        Intent intent = new Intent(this, PlayerActivity.class);
        intent.putExtra(KEY_TRACKS, gson.toJson(tracks, mTrackListType));
        intent.putExtra(KEY_SELECTED_TRACK, position);
        startActivity(intent);
    }

    @Override
    protected void onStart() {
        super.onStart();
    }

    @Override
    protected void onStop() {
        super.onStop();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        doUnbindService();
    }

    @Override
    public void onTrackChange(int position) {
        mTrackPosition = position;
    }

    @Override
    public void onServiceStateChange(boolean started) {
        mServiceStarted = started;
        if (mServiceStarted) {
            mPlayingItem.setVisible(true);
        } else {
            mPlayingItem.setVisible(false);
            doUnbindService();
        }
    }

    private void registerCallback(MusicService boundService) {
        boundService.registerCallback(this);
    }

    void doBindService() {
        bindService(new Intent(this, MusicService.class), mConnection, Context.BIND_AUTO_CREATE);
        mIsBound = true;
    }

    void doUnbindService() {
        if (mIsBound) {
            unbindService(mConnection);
            mIsBound = false;
        }
    }

    private ServiceConnection mConnection = new ServiceConnection() {

        public void onServiceConnected(ComponentName className, IBinder service) {
            mBoundService = ((MusicService.MusicBinder)service).getService();
            registerCallback(mBoundService);
        }

        public void onServiceDisconnected(ComponentName className) {
            mBoundService = null;
        }
    };
}
