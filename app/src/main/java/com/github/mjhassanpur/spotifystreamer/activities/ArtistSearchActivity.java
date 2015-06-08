package com.github.mjhassanpur.spotifystreamer.activities;

import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.view.Menu;

import com.github.mjhassanpur.spotifystreamer.R;
import com.github.mjhassanpur.spotifystreamer.fragments.ArtistSearchFragment;


public class ArtistSearchActivity extends ActionBarActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_artist_search);
        if (savedInstanceState == null) {
            getSupportFragmentManager().beginTransaction()
                    .add(R.id.fragment_container, new ArtistSearchFragment())
                    .commit();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_artist_search, menu);
        return true;
    }

    @Override
    public void onPostCreate(Bundle savedInstanceState) {
        ArtistSearchFragment fragment =
                (ArtistSearchFragment) getSupportFragmentManager()
                        .findFragmentById(R.id.fragment_container);
        fragment.addSearchBoxListeners();
        super.onPostCreate(savedInstanceState);
    }
}
