package com.github.mjhassanpur.spotifystreamer.fragments;

import android.content.Intent;
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
    private SpotifyService mSpotifyService;
    private final String KEY_ARTIST = "artist";
    private final String KEY_ARTISTS = "artists";

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mSpotifyService = new SpotifyApi().getService();
        mGson = new Gson();
        mArtistListType = new TypeToken<List<Artist>>() {}.getType();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_artist_search, container, false);
        mDefaultMessageView = rootView.findViewById(R.id.message_container);
        mRecyclerView = (RecyclerView) rootView.findViewById(R.id.artist_recycler_view);
        setupRecyclerView();
        if (savedInstanceState == null) {
            mRecyclerView.setAdapter(new ArtistAdapter(new ArrayList<Artist>()));
            showDefaultSearchMessage();
        } else {
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
        super.onSaveInstanceState(outState);
        outState.putString(KEY_ARTISTS, mGson.toJson(mArtistList, mArtistListType));
    }

    private void setupRecyclerView() {
        mRecyclerView.setLayoutManager(new LinearLayoutManager(getActivity()));
        mRecyclerView.addItemDecoration(new DividerItemDecoration(getActivity(), null));
        mRecyclerView.addOnItemTouchListener(new RecyclerItemClickListener(getActivity(), new OnItemClickListener()));
    }

    public void searchArtists(String query) {
        if (query != null && !query.trim().isEmpty()) {
            new SearchArtistsTask().execute(query);
        }
    }

    private boolean updateArtistAdapter(List<Artist> artists) {
        if (artists != null && !artists.isEmpty()) {
            mArtistAdapter = new ArtistAdapter(new ArrayList<>(artists));
            mRecyclerView.setAdapter(mArtistAdapter);
            showArtistList();
            return true;
        }
        return false;
    }

    public void showArtistList() {
        mDefaultMessageView.setVisibility(View.GONE);
        mRecyclerView.setVisibility(View.VISIBLE);
    }

    public void showDefaultSearchMessage() {
        mRecyclerView.setVisibility(View.GONE);
        mDefaultMessageView.setVisibility(View.VISIBLE);
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
            String query = params[0];
            mArtistList = mSpotifyService.searchArtists(query).artists.items;
            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            if (!updateArtistAdapter(mArtistList)) {
                mRecyclerView.setAdapter(new ArtistAdapter(new ArrayList<Artist>()));
                showDefaultSearchMessage();
                Toast.makeText(getActivity(), "No artists found. Please refine search.", Toast.LENGTH_SHORT).show();
            }
        }
    }
}
