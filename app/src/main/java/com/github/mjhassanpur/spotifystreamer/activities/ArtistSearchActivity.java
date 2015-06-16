package com.github.mjhassanpur.spotifystreamer.activities;

import android.app.SearchManager;
import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.view.MenuItemCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.SearchView;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;

import com.github.mjhassanpur.spotifystreamer.R;
import com.github.mjhassanpur.spotifystreamer.fragments.ArtistSearchFragment;


public class ArtistSearchActivity extends AppCompatActivity {

    private String mSearch;
    private SearchView mSearchView;
    private final String KEY_SEARCH = "search";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_artist_search);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayShowTitleEnabled(false);
        if (savedInstanceState == null) {
            getSupportFragmentManager().beginTransaction()
                    .add(R.id.fragment_container, new ArtistSearchFragment())
                    .commit();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_artist_search, menu);
        MenuItem searchMenuItem = menu.findItem(R.id.action_search);
        setUpSearchView(searchMenuItem);
        return true;
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        outState.putString(KEY_SEARCH, mSearch);
        super.onSaveInstanceState(outState);
    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        mSearch = savedInstanceState.getString(KEY_SEARCH);
        super.onRestoreInstanceState(savedInstanceState);
    }

    /**
     * Search for artists matching the keyword
     *
     * @param query The keyword string
     */
    private void searchArtists(String query) {
        ArtistSearchFragment fragment =
                (ArtistSearchFragment) getSupportFragmentManager()
                        .findFragmentById(R.id.fragment_container);
        fragment.searchArtists(query);
    }

    /**
     * Sets up the SearchView
     *
     * @param menuItem The search menu item
     * @see <a href="http://developer.android.com/training/search/setup.html">Setting Up the Search Interface</a>
     */
    private void setUpSearchView(MenuItem menuItem) {
        SearchManager searchManager = (SearchManager) getSystemService(Context.SEARCH_SERVICE);
        mSearchView = (SearchView) MenuItemCompat.getActionView(menuItem);
        mSearchView.setSearchableInfo(searchManager.getSearchableInfo(getComponentName()));
        mSearchView.setIconifiedByDefault(false);
        mSearchView.setOnQueryTextListener(new SearchViewTextListener());
        if (mSearch != null) {
            // Retain search query on screen rotation
            MenuItemCompat.expandActionView(menuItem);
            mSearchView.setQuery(mSearch, false);
            mSearchView.clearFocus();
        }
    }

    /**
     * Custom SearchView text listener
     *
     * @see <a href="http://stackoverflow.com/questions/10900348/edittext-textchangelistener">EditText & TextChangeListener</a>
     */
    private class SearchViewTextListener implements SearchView.OnQueryTextListener {

        private Handler handler = new Handler();
        private Runnable delayedAction = null;
        private final int DELAY_IN_MILLISECONDS = 300;

        @Override
        public boolean onQueryTextSubmit(String query) {
            searchArtists(query);
            return false;
        }

        @Override
        public boolean onQueryTextChange(final String newText) {
            // If text is empty or same as previous search
            if (newText.isEmpty() ||
                    (mSearch != null && mSearch.equals(newText))) {
                return false;
            }

            mSearch = newText;

            // Cancel previous search
            if (delayedAction != null) {
                handler.removeCallbacks(delayedAction);
            }

            // Define a new search
            delayedAction = new Runnable() {
                @Override
                public void run() {
                    searchArtists(mSearch);
                }
            };

            // Delay the search
            handler.postDelayed(delayedAction, DELAY_IN_MILLISECONDS);
            return false;
        }
    }

}
