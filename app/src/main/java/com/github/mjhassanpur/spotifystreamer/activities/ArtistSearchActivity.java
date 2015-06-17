package com.github.mjhassanpur.spotifystreamer.activities;

import android.app.SearchManager;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.FragmentManager;
import android.support.v4.view.MenuItemCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.SearchView;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

import com.github.mjhassanpur.spotifystreamer.R;
import com.github.mjhassanpur.spotifystreamer.fragments.ArtistSearchFragment;

public class ArtistSearchActivity extends AppCompatActivity {

    private String mQuery;
    private MenuItem mSearchItem;
    private SearchView mSearchView;
    private ArtistSearchFragment mSearchFragment;
    private final String KEY_QUERY = "query";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_artist_search);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setTitle(R.string.title_activity_artist_search);

        FragmentManager fm = getSupportFragmentManager();
        mSearchFragment = (ArtistSearchFragment) fm.findFragmentById(R.id.fragment_container);

        if (mSearchFragment == null) {
            mSearchFragment = new ArtistSearchFragment();
            fm.beginTransaction().add(R.id.fragment_container, mSearchFragment).commit();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_artist_search, menu);
        mSearchItem = menu.findItem(R.id.action_search);
        setupSearchView();
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
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString(KEY_QUERY, mQuery);
    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        mQuery = savedInstanceState.getString(KEY_QUERY);
    }

    @Override
    public void onBackPressed() {
        if (MenuItemCompat.isActionViewExpanded(mSearchItem)) {
            MenuItemCompat.collapseActionView(mSearchItem);
        } else {
            super.onBackPressed();
        }
    }

    @Override
    protected void onNewIntent(Intent intent) {
        setIntent(intent);
        handleIntent(intent);
    }

    private void handleIntent(Intent intent) {
        if (Intent.ACTION_SEARCH.equals(intent.getAction())) {
            String query = intent.getStringExtra(SearchManager.QUERY);
            if (!TextUtils.equals(mQuery, query)) {
                mSearchView.setQuery(query, false);
                mSearchView.clearFocus();
            }
        }
    }

    /**
     * @see <a href="http://developer.android.com/training/search/setup.html">Setting Up the Search Interface</a>
     */
    private void setupSearchView() {
        SearchManager searchManager = (SearchManager) getSystemService(Context.SEARCH_SERVICE);
        mSearchView = (SearchView) MenuItemCompat.getActionView(mSearchItem);
        mSearchView.setSearchableInfo(searchManager.getSearchableInfo(getComponentName()));
        mSearchView.setIconifiedByDefault(false);
        mSearchView.setOnQueryTextListener(new SearchViewTextListener());
        if (mQuery != null) {
            // Retain search query on screen rotation
            String savedSearch = mQuery;
            MenuItemCompat.expandActionView(mSearchItem);
            mQuery = savedSearch;
            mSearchView.setQuery(mQuery, false);
            if (mQuery.isEmpty()) {
                mSearchFragment.showDefaultSearchMessage();
            }
            mSearchView.clearFocus();
        }
    }

    /**
     * @see <a href="http://stackoverflow.com/questions/10900348/edittext-textchangelistener">EditText & TextChangeListener</a>
     */
    private class SearchViewTextListener implements SearchView.OnQueryTextListener {

        private Handler handler = new Handler();
        private Runnable delayedAction = null;
        private final int DELAY_IN_MILLISECONDS = 300;

        @Override
        public boolean onQueryTextSubmit(String query) {
            mSearchView.clearFocus();
            return false;
        }

        @Override
        public boolean onQueryTextChange(final String newText) {
            if (mSearchFragment == null) {
                return false;
            }

            if (TextUtils.equals(mQuery, newText)) {
                mSearchFragment.showArtistList();
                return false;
            }

            mQuery = newText;

            if (TextUtils.isEmpty(mQuery)) {
                mSearchFragment.showDefaultSearchMessage();
                return false;
            }

            // Cancel previous search
            if (delayedAction != null) {
                handler.removeCallbacks(delayedAction);
            }

            // Define a new search
            delayedAction = new Runnable() {
                @Override
                public void run() {
                    mSearchFragment.searchArtists(mQuery);
                }
            };

            // Delay the search
            handler.postDelayed(delayedAction, DELAY_IN_MILLISECONDS);
            return false;
        }
    }
}
