package com.github.mjhassanpur.spotifystreamer.fragments;

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
import retrofit.RetrofitError;

public class ArtistSearchFragment extends Fragment {

    private RecyclerView mRecyclerView;
    private View mDefaultMessageView;
    private ArtistAdapter mArtistAdapter;
    private List<Artist> mArtistList;
    private Gson mGson;
    private SpotifyService mSpotifyService;
    private final String KEY_ARTISTS = "artists";
    private final static String LOG_TAG = "ArtistSearchFragment";
    private final Type mArtistListType = new TypeToken<List<Artist>>() {}.getType();

    public interface Callback {
        void onItemSelected(Artist artist);
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mSpotifyService = new SpotifyApi().getService();
        mGson = new Gson();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_artist_search, container, false);
        mRecyclerView = (RecyclerView) view.findViewById(R.id.artist_recycler_view);
        mDefaultMessageView = view.findViewById(R.id.message_container);
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
        return view;
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

        @Override public void onItemClick(View childView, int position) {
            Artist artist = mArtistList.get(position);
            ((Callback) getActivity()).onItemSelected(artist);
        }
    }

    private class SearchArtistsTask extends AsyncTask<String, Void, Void> {

        @Override
        protected Void doInBackground(String... params) {
            String query = params[0];
            try {
                mArtistList = mSpotifyService.searchArtists(query).artists.items;
            } catch (RetrofitError e) {
                Log.e(LOG_TAG, "An error occurred when attempting to retrieve artists");
            }
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
