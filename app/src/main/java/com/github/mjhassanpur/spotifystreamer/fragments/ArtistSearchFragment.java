package com.github.mjhassanpur.spotifystreamer.fragments;

import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.view.MenuItemCompat;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.SearchView;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import com.github.mjhassanpur.spotifystreamer.DividerItemDecoration;
import com.github.mjhassanpur.spotifystreamer.R;
import com.github.mjhassanpur.spotifystreamer.activities.ArtistSearchActivity;
import com.github.mjhassanpur.spotifystreamer.activities.TopTracksActivity;
import com.github.mjhassanpur.spotifystreamer.adapters.ArtistAdapter;
import com.github.mjhassanpur.spotifystreamer.listeners.RecyclerItemClickListener;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

import kaaes.spotify.webapi.android.SpotifyApi;
import kaaes.spotify.webapi.android.SpotifyService;
import kaaes.spotify.webapi.android.models.Artist;


public class ArtistSearchFragment extends Fragment {

    private RecyclerView mRecyclerView;
    private View mDefaultMessageView;
    private ArtistAdapter mArtistAdapter;
    private List<Artist> mArtistList;
    private Type mArtistListType;
    private Gson mGson;
    private String mSearch;
    private SpotifyService mSpotifyService;
    private final String KEY_SEARCH = "search";
    private final String KEY_ARTIST = "artist";
    private final String KEY_ARTISTS = "artists";

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        // Get an instance of the SpotifyApi
        mSpotifyService = new SpotifyApi().getService();
        // Get an instance of Gson for serialization / deserialization
        mGson = new Gson();
        // The artist list type
        mArtistListType = new TypeToken<List<Artist>>() {}.getType();
        // Must be true for onCreateOptionsMenu to be called
        setHasOptionsMenu(true);
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_artist_search, container, false);
        mRecyclerView = (RecyclerView) rootView.findViewById(R.id.artist_recycler_view);
        setUpRecyclerView();
        mDefaultMessageView = rootView.findViewById(R.id.message_container);
        if (savedInstanceState == null) {
            mRecyclerView.setAdapter(new ArtistAdapter(new ArrayList<Artist>()));
            showDefaultSearchMessage();
        } else {
            // If instance is being recreated from a previous state
            mSearch = savedInstanceState.getString(KEY_SEARCH);
            String json = savedInstanceState.getString(KEY_ARTISTS);
            if (json != null) {
                mArtistList = mGson.fromJson(json, mArtistListType);
                updateArtistAdapter(mArtistList);
            }
        }
        return rootView;
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        ArtistSearchActivity activity = (ArtistSearchActivity) getActivity();
        SearchView searchView = activity.getSearchView();
        searchView.setOnQueryTextListener(new SearchViewTextListener());
        if (mSearch != null) {
            // Retain search query on screen rotation
            String savedSearch = mSearch;
            MenuItemCompat.expandActionView(activity.getSearchMenuItem());
            mSearch = savedSearch;
            searchView.setQuery(mSearch, false);
            if (mSearch.isEmpty()) {
                showDefaultSearchMessage();
            }
            searchView.clearFocus();
        }
        super.onCreateOptionsMenu(menu, inflater);
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        outState.putString(KEY_SEARCH, mSearch);
        outState.putString(KEY_ARTISTS, mGson.toJson(mArtistList, mArtistListType));
        super.onSaveInstanceState(outState);
    }

    /**
     * Sets up the RecyclerView for displaying artists
     */
    private void setUpRecyclerView() {
        mRecyclerView.setLayoutManager(new LinearLayoutManager(getActivity()));
        mRecyclerView.addItemDecoration(new DividerItemDecoration(getActivity(), null));
        mRecyclerView.addOnItemTouchListener(new RecyclerItemClickListener(getActivity(),
                new OnItemClickListener()));
    }

    /**
     * Search for artists that match the keyword string
     *
     * @param keyword The keyword string
     */
    public void searchArtists(String keyword) {
        if (keyword != null && !keyword.trim().isEmpty()) {
            new SearchArtistsTask().execute(keyword);
        }
    }

    /**
     * Update the artist adapter and set it on the RecyclerView
     *
     * @param artists The list of artists to be added to the adapter
     * @return true if the adapter was set on the RecyclerView
     */
    private boolean updateArtistAdapter(List<Artist> artists) {
        if (artists != null && !artists.isEmpty()) {
            mArtistAdapter = new ArtistAdapter(new ArrayList<>(artists));
            mRecyclerView.setAdapter(mArtistAdapter);
            showArtistList();
            return true;
        }
        return false;
    }

    /**
     * Shows only the artist list and hides all other views
     */
    public void showArtistList() {
        mDefaultMessageView.setVisibility(View.GONE);
        mRecyclerView.setVisibility(View.VISIBLE);
    }

    /**
     * Shows only the default search message and hides all other views
     */
    public void showDefaultSearchMessage() {
        mRecyclerView.setVisibility(View.GONE);
        mDefaultMessageView.setVisibility(View.VISIBLE);
    }

    /**
     * Custom click listener for RecyclerView items
     */
    private class OnItemClickListener extends RecyclerItemClickListener.SimpleOnItemClickListener {

        @Override
        public void onItemClick(View childView, int position) {
            Intent intent = new Intent(getActivity(), TopTracksActivity.class);
            Artist artist = mArtistList.get(position);
            Type artistType = new TypeToken<Artist>() {}.getType();
            intent.putExtra(KEY_ARTIST, mGson.toJson(artist, artistType));
            startActivity(intent);
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
            // If text is same as previous search
            if (mSearch != null && mSearch.equals(newText)) {
                showArtistList();
                return false;
            }

            mSearch = newText;

            // If text is empty
            if (mSearch.isEmpty()) {
                showDefaultSearchMessage();
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
                    searchArtists(mSearch);
                }
            };

            // Delay the search
            handler.postDelayed(delayedAction, DELAY_IN_MILLISECONDS);
            return false;
        }
    }

    /**
     * Background task for searching artists
     */
    private class SearchArtistsTask extends AsyncTask<String, Void, Void> {

        @Override
        protected Void doInBackground(String... params) {
            String keyword = params[0];
            if (keyword != null && !keyword.trim().isEmpty()) {
                // Get the list of artists
                mArtistList = mSpotifyService.searchArtists(keyword).artists.items;
            }
            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            if (!updateArtistAdapter(mArtistList)) {
                // If no artists were found
                mRecyclerView.setAdapter(new ArtistAdapter(new ArrayList<Artist>()));
                showDefaultSearchMessage();
                Toast.makeText(getActivity(),
                        "No artists found. Please refine search.",
                        Toast.LENGTH_SHORT).show();
            }
        }

    }
}
