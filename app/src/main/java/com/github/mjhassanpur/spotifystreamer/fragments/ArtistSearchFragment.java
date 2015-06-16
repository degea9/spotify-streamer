package com.github.mjhassanpur.spotifystreamer.fragments;

import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import com.github.mjhassanpur.spotifystreamer.DividerItemDecoration;
import com.github.mjhassanpur.spotifystreamer.R;
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
    private ArtistAdapter mArtistAdapter;
    private List<Artist> mArtistList;
    private Type mArtistListType;
    private Gson mGson;
    private SpotifyService mSpotifyService;
    private final String KEY_ARTIST = "artist";
    private final String KEY_ARTISTS = "artists";

    public ArtistSearchFragment() {
        // Get an instance of the SpotifyApi
        mSpotifyService = new SpotifyApi().getService();
        // Get an instance of Gson for serialization / deserialization
        mGson = new Gson();
        // The artist list type
        mArtistListType = new TypeToken<List<Artist>>() {}.getType();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_artist_search, container, false);
        mRecyclerView = (RecyclerView) rootView.findViewById(R.id.artist_recycler_view);
        mRecyclerView.setLayoutManager(new LinearLayoutManager(getActivity()));
        mRecyclerView.addItemDecoration(new DividerItemDecoration(getActivity(), null));
        mRecyclerView.addOnItemTouchListener(new RecyclerItemClickListener(getActivity(),
                new OnItemClickListener()));
        if (savedInstanceState == null) {
            mRecyclerView.setAdapter(new ArtistAdapter(new ArrayList<Artist>()));
        } else {
            // If instance is being recreated from a previous state
            String json = savedInstanceState.getString(KEY_ARTISTS);
            if (json != null) {
                mArtistList = mGson.fromJson(json, mArtistListType);
                updateArtistAdapter(mArtistList);
            }
        }
        return rootView;
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        outState.putString(KEY_ARTISTS, mGson.toJson(mArtistList, mArtistListType));
        super.onSaveInstanceState(outState);
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
            return true;
        }
        return false;
    }

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
                Toast.makeText(getActivity(),
                        "No artists found. Please refine search.",
                        Toast.LENGTH_SHORT).show();
            }
        }
    }
}
