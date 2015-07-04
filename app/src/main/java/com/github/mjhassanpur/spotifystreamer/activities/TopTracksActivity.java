package com.github.mjhassanpur.spotifystreamer.activities;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.FragmentManager;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

import com.github.mjhassanpur.spotifystreamer.R;
import com.github.mjhassanpur.spotifystreamer.fragments.TopTracksFragment;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.List;

import kaaes.spotify.webapi.android.models.Track;

public class TopTracksActivity extends AppCompatActivity implements TopTracksFragment.Callback {

    private final String KEY_TRACKS = "tracks";
    private final String KEY_SELECTED_TRACK = "selectedTrack";
    private final Type mTrackListType = new TypeToken<List<Track>>() {}.getType();

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
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_top_tracks, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.action_settings) {
            Toast.makeText(this, "Settings coming soon...", Toast.LENGTH_SHORT).show();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onItemSelected(List<Track> tracks, int position) {
        Gson gson = new Gson();
        Intent intent = new Intent(this, PlayerActivity.class);
        intent.putExtra(KEY_TRACKS, gson.toJson(tracks, mTrackListType));
        intent.putExtra(KEY_SELECTED_TRACK, position);
        startActivity(intent);
    }
}
