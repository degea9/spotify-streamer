package com.github.mjhassanpur.spotifystreamer.fragments;

import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.github.mjhassanpur.spotifystreamer.R;
import com.github.mjhassanpur.spotifystreamer.adapters.ArtistAdapter;

import java.util.ArrayList;
import java.util.List;

import kaaes.spotify.webapi.android.SpotifyApi;
import kaaes.spotify.webapi.android.SpotifyService;
import kaaes.spotify.webapi.android.models.Artist;


public class ArtistSearchFragment extends Fragment {

    private RecyclerView mRecyclerView;
    private ArtistAdapter mAdapter;
    private List<Artist> mArtistList;
    private SpotifyService mSpotifyService;

    public ArtistSearchFragment() {
        // Get an instance of the SpotifyApi
        mSpotifyService = new SpotifyApi().getService();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_artist_search, container, false);
        EditText mSearchBox = (EditText) rootView.findViewById(R.id.artist_search_box);
        mSearchBox.setOnEditorActionListener(new EditText.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                String keyword = v.getText().toString();
                if (actionId == EditorInfo.IME_ACTION_DONE && !keyword.trim().isEmpty()) {
                    // Get the artists that match the keyword search
                    new SearchArtistsTask().execute(v.getText().toString());
                }
                return false;
            }
        });
        mRecyclerView = (RecyclerView) rootView.findViewById(R.id.artist_recycler_view);
        mRecyclerView.setLayoutManager(new LinearLayoutManager(getActivity()));
        mRecyclerView.setAdapter(new ArtistAdapter(new ArrayList<Artist>()));
        return rootView;
    }

    public class SearchArtistsTask extends AsyncTask<String, Void, Void> {

        @Override
        protected Void doInBackground(String... params) {
            String keyword = params[0];
            if (keyword != null && !keyword.trim().isEmpty()) {
                mArtistList = mSpotifyService.searchArtists(keyword).artists.items;
            }
            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            if (mArtistList != null && !mArtistList.isEmpty()) {
                mAdapter = new ArtistAdapter(new ArrayList<>(mArtistList));
                mRecyclerView.setAdapter(mAdapter);
            } else {
                // If no artists were found
                mRecyclerView.setAdapter(new ArtistAdapter(new ArrayList<Artist>()));
                Toast.makeText(getActivity(),
                        "No artists found. Please refine search.",
                        Toast.LENGTH_SHORT).show();
            }
        }
    }
}
