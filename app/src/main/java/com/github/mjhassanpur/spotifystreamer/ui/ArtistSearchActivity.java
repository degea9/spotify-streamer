package com.github.mjhassanpur.spotifystreamer.ui;

import android.app.SearchManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.support.v4.app.FragmentManager;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.SearchView;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.view.Menu;
import android.view.MenuItem;

import com.github.mjhassanpur.spotifystreamer.MusicService;
import com.github.mjhassanpur.spotifystreamer.R;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.List;

import kaaes.spotify.webapi.android.models.Artist;
import kaaes.spotify.webapi.android.models.Track;

public class ArtistSearchActivity extends AppCompatActivity implements ArtistSearchFragment.Callback,
        TopTracksFragment.Callback, MusicService.Callback {

    private String mQuery;
    private MenuItem mSearchItem;
    private SearchView mSearchView;
    private ArtistSearchFragment mSearchFragment;
    private final String KEY_QUERY = "query";
    private static final String TOP_TRACKS_FRAGMENT_TAG = "TTFTAG";
    private final String KEY_ARTIST = "artist";
    private final String KEY_TRACKS = "tracks";
    private final String KEY_SELECTED_TRACK = "selectedTrack";
    private final Type mArtistType = new TypeToken<Artist>() {}.getType();
    private final Type mTrackListType = new TypeToken<List<Track>>() {}.getType();
    private boolean mTwoPane;
    private boolean mRetainTopTracks;
    private MusicService mBoundService;
    private boolean mIsBound = false;
    private int mTrackPosition;
    private List<Track> mTracks;
    private boolean mServiceStarted = false;
    private MenuItem mPlayingItem;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_artist_search);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setTitle(R.string.title_activity_artist_search);

        FragmentManager fm = getSupportFragmentManager();
        mSearchFragment = (ArtistSearchFragment) fm.findFragmentById(R.id.fragment_artist_search);

        if (findViewById(R.id.top_tracks_container) != null) {
            mTwoPane = true;
            if (savedInstanceState == null) {
                replaceTopTracks(new TopTracksFragment());
            }
        } else {
            mTwoPane = false;
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_artist_search, menu);
        mSearchItem = menu.findItem(R.id.action_search);
        mPlayingItem = menu.findItem(R.id.action_playing);
        setupSearchView();
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.action_settings) {
            Intent intent = new Intent(this, SettingsActivity.class);
            startActivity(intent);
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
    protected void onStart() {
        super.onStart();
        doBindService();
    }

    @Override
    protected void onStop() {
        super.onStop();
        doUnbindService();
    }

    @Override
    public void onBackPressed() {
        if (mSearchItem.isActionViewExpanded()) {
            mSearchItem.collapseActionView();
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
        mSearchView = (SearchView) mSearchItem.getActionView();
        mSearchView.setSearchableInfo(searchManager.getSearchableInfo(getComponentName()));
        mSearchView.setIconifiedByDefault(false);
        mSearchView.setOnQueryTextListener(new SearchViewTextListener());
        if (mQuery != null) {
            // Retain search query on screen rotation
            if (!mQuery.isEmpty()) {
                mRetainTopTracks = true;
            }
            String savedSearch = mQuery;
            mSearchItem.expandActionView();
            mRetainTopTracks = false;
            mQuery = savedSearch;
            mSearchView.setQuery(mQuery, false);
            if (mQuery.isEmpty()) {
                mSearchFragment.showDefaultSearchMessage();
                if (mTwoPane) {
                    replaceTopTracks(new TopTracksFragment());
                }
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
                if (mTwoPane && !mRetainTopTracks) {
                    replaceTopTracks(new TopTracksFragment());
                }
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

    private void replaceTopTracks(TopTracksFragment fragment) {
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.top_tracks_container, fragment, TOP_TRACKS_FRAGMENT_TAG)
                .commit();
    }

    @Override
    public void onItemSelected(Artist artist) {
        Gson gson = new Gson();
        if (mTwoPane) {
            Bundle args = new Bundle();
            args.putString(KEY_ARTIST, gson.toJson(artist, mArtistType));
            TopTracksFragment fragment = new TopTracksFragment();
            fragment.setArguments(args);
            replaceTopTracks(fragment);
        } else {
            Intent intent = new Intent(this, TopTracksActivity.class);
            intent.putExtra(KEY_ARTIST, gson.toJson(artist, mArtistType));
            startActivity(intent);
        }
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
        }
    }

    private void registerCallback(MusicService boundService) {
        boundService.registerCallback(this);
    }

    private void unregisterCallback(MusicService boundService) {
        boundService.unregisterCallback(this);
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
            unregisterCallback(mBoundService);
            mBoundService = null;
        }
    };
}
