package com.github.mjhassanpur.spotifystreamer.activities;

import android.app.SearchManager;
import android.content.Context;
import android.os.Bundle;
import android.support.v4.view.MenuItemCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.SearchView;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;

import com.github.mjhassanpur.spotifystreamer.R;
import com.github.mjhassanpur.spotifystreamer.fragments.ArtistSearchFragment;


public class ArtistSearchActivity extends AppCompatActivity {

    private MenuItem mSearchMenuItem;
    private SearchView mSearchView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_artist_search);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setTitle(R.string.title_activity_artist_search);
        if (savedInstanceState == null) {
            getSupportFragmentManager().beginTransaction()
                    .add(R.id.fragment_container, new ArtistSearchFragment())
                    .commit();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_artist_search, menu);
        mSearchMenuItem = menu.findItem(R.id.action_search);
        setUpSearchView();
        return true;
    }

    /**
     * Get the search menu item
     *
     * @return The search menu item
     */
    public MenuItem getSearchMenuItem() {
        return mSearchMenuItem;
    }

    /**
     * Get the SearchView
     *
     * @return The SearchView
     */
    public SearchView getSearchView() {
        return mSearchView;
    }

    /**
     * Sets up the SearchView
     *
     * @see <a href="http://developer.android.com/training/search/setup.html">Setting Up the Search Interface</a>
     */
    private void setUpSearchView() {
        SearchManager searchManager = (SearchManager) getSystemService(Context.SEARCH_SERVICE);
        mSearchView = (SearchView) MenuItemCompat.getActionView(mSearchMenuItem);
        mSearchView.setSearchableInfo(searchManager.getSearchableInfo(getComponentName()));
        mSearchView.setIconifiedByDefault(false);
    }

}
