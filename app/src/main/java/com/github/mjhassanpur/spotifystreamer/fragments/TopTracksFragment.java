package com.github.mjhassanpur.spotifystreamer.fragments;

import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import com.github.mjhassanpur.spotifystreamer.DividerItemDecoration;
import com.github.mjhassanpur.spotifystreamer.R;
import com.github.mjhassanpur.spotifystreamer.adapters.TrackAdapter;
import com.github.mjhassanpur.spotifystreamer.listeners.RecyclerItemClickListener;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import kaaes.spotify.webapi.android.SpotifyApi;
import kaaes.spotify.webapi.android.SpotifyService;
import kaaes.spotify.webapi.android.models.Artist;
import kaaes.spotify.webapi.android.models.Track;


public class TopTracksFragment extends Fragment {

    private RecyclerView mRecyclerView;
    private TrackAdapter mTrackAdapter;
    private List<Track> mTrackList;
    private Type mTrackListType;
    private Artist mArtist;
    private Type mArtistType;
    private Gson mGson;
    private SpotifyService mSpotifyService;
    private final String KEY_TRACKS = "tracks";
    private final String KEY_ARTIST = "artist";

    public TopTracksFragment() {
        // Get an instance of the SpotifyApi
        mSpotifyService = new SpotifyApi().getService();
        // Get an instance of Gson for serialization / deserialization
        mGson = new Gson();
        // The artist type
        mArtistType = new TypeToken<Artist>() {}.getType();
        // The track list type
        mTrackListType = new TypeToken<List<Track>>() {}.getType();
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        String json = getActivity().getIntent().getStringExtra(KEY_ARTIST);
        if (json != null) {
            mArtist = mGson.fromJson(json, mArtistType);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_top_tracks, container, false);
        mRecyclerView = (RecyclerView) rootView.findViewById(R.id.track_recycler_view);
        mRecyclerView.setLayoutManager(new LinearLayoutManager(getActivity()));
        mRecyclerView.addItemDecoration(new DividerItemDecoration(getActivity(), null));
        mRecyclerView.addOnItemTouchListener(new RecyclerItemClickListener(getActivity(),
                new OnItemClickListener()));
        if (savedInstanceState == null) {
            fetchTopTracks();
        } else {
            // If instance is being recreated from a previous state
            String json = savedInstanceState.getString(KEY_TRACKS);
            if (json != null) {
                mTrackList = mGson.fromJson(json, mTrackListType);
                updateTrackAdapter(mTrackList);
            }
        }

        return rootView;
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        outState.putString(KEY_TRACKS, mGson.toJson(mTrackList, mTrackListType));
        super.onSaveInstanceState(outState);
    }

    /**
     * Fetch the top tracks for the artist
     */
    private void fetchTopTracks() {
        new FetchTopTracksTask().execute();
    }

    /**
     * Update the track adapter and set it on the RecyclerView
     *
     * @param tracks The list of tracks to be added to the adapter
     * @return true if the adapter was set on the RecyclerView
     */
    private boolean updateTrackAdapter(List<Track> tracks) {
        if (tracks != null && !tracks.isEmpty()) {
            mTrackAdapter = new TrackAdapter(new ArrayList<>(tracks));
            mRecyclerView.setAdapter(mTrackAdapter);
            return true;
        }
        return false;
    }

    private class OnItemClickListener extends RecyclerItemClickListener.SimpleOnItemClickListener {

        @Override
        public void onItemClick(View childView, int position) {
            // Do something
        }

    }

    public class FetchTopTracksTask extends AsyncTask<Void, Void, Void> {

        @Override
        protected Void doInBackground(Void... params) {
            Map<String,Object> options = new HashMap<>();
            options.put("country", "US");
            mTrackList = mSpotifyService.getArtistTopTrack(mArtist.id, options).tracks;
            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            if (!updateTrackAdapter(mTrackList)) {
                // If no tracks were found
                mRecyclerView.setAdapter(new TrackAdapter(new ArrayList<Track>()));
                Toast.makeText(getActivity(),
                        "Sorry, no tracks found.",
                        Toast.LENGTH_SHORT).show();
            }
        }
    }
}
