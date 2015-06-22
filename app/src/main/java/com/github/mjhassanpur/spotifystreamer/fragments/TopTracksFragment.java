package com.github.mjhassanpur.spotifystreamer.fragments;

import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import com.github.mjhassanpur.spotifystreamer.DividerItemDecoration;
import com.github.mjhassanpur.spotifystreamer.R;
import com.github.mjhassanpur.spotifystreamer.activities.PlayerActivity;
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
import retrofit.RetrofitError;

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
    private final String KEY_SELECTED_TRACK = "selectedTrack";
    private final String KEY_ARTIST = "artist";
    private final static String LOG_TAG = "TopTracksFragment";

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        mSpotifyService = new SpotifyApi().getService();
        mGson = new Gson();
        mArtistType = new TypeToken<Artist>() {}.getType();
        mTrackListType = new TypeToken<List<Track>>() {}.getType();
        String json = getActivity().getIntent().getStringExtra(KEY_ARTIST);
        if (json != null) {
            mArtist = mGson.fromJson(json, mArtistType);
        }
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_top_tracks, container, false);
        mRecyclerView = (RecyclerView) rootView.findViewById(R.id.track_recycler_view);
        setupRecyclerView();
        if (savedInstanceState == null) {
            fetchTopTracks();
        } else {
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
        super.onSaveInstanceState(outState);
        outState.putString(KEY_TRACKS, mGson.toJson(mTrackList, mTrackListType));
    }

    private void setupRecyclerView() {
        mRecyclerView.setLayoutManager(new LinearLayoutManager(getActivity()));
        mRecyclerView.addItemDecoration(new DividerItemDecoration(getActivity(), null));
        mRecyclerView.addOnItemTouchListener(new RecyclerItemClickListener(getActivity(), new OnItemClickListener()));
    }

    private void fetchTopTracks() {
        new FetchTopTracksTask().execute();
    }

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
            Intent intent = new Intent(getActivity(), PlayerActivity.class);
            intent.putExtra(KEY_TRACKS, mGson.toJson(mTrackList, mTrackListType));
            intent.putExtra(KEY_SELECTED_TRACK, position);
            startActivity(intent);
        }
    }

    public class FetchTopTracksTask extends AsyncTask<Void, Void, Void> {

        @Override
        protected Void doInBackground(Void... params) {
            Map<String,Object> options = new HashMap<>();
            options.put("country", "US");
            try {
                mTrackList = mSpotifyService.getArtistTopTrack(mArtist.id, options).tracks;
            } catch (RetrofitError e) {
                Log.e(LOG_TAG, "An error occurred when attempting to retrieve tracks");
            }
            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            if (!updateTrackAdapter(mTrackList)) {
                mRecyclerView.setAdapter(new TrackAdapter(new ArrayList<Track>()));
                Toast.makeText(getActivity(), "Sorry, no tracks found.", Toast.LENGTH_SHORT).show();
            }
        }
    }
}
