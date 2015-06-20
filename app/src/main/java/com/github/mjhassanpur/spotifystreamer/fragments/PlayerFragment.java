package com.github.mjhassanpur.spotifystreamer.fragments;

import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.github.mjhassanpur.spotifystreamer.R;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.List;

import kaaes.spotify.webapi.android.models.ArtistSimple;
import kaaes.spotify.webapi.android.models.Image;
import kaaes.spotify.webapi.android.models.Track;

public class PlayerFragment extends DialogFragment {

    private Track mTrack;
    private Type mTrackType;
    private ImageView mAlbumImageView;
    private Gson mGson;
    private final String KEY_TRACK = "track";

    @Override
    public void onCreate(Bundle savedInstanceState) {
        mGson = new Gson();
        mTrackType = new TypeToken<Track>() {}.getType();
        String json = getActivity().getIntent().getStringExtra(KEY_TRACK);
        if (json != null) {
            mTrack = mGson.fromJson(json, mTrackType);
        }
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_player, container, false);

        mAlbumImageView = (ImageView) rootView.findViewById(R.id.album_image);

        TextView trackNameView = (TextView) rootView.findViewById(R.id.track_name);
        trackNameView.setText(mTrack.name);

        List<ArtistSimple> artists = mTrack.artists;
        if (artists != null && !artists.isEmpty()) {
            TextView artistNameView = (TextView) rootView.findViewById(R.id.artist_name);
            artistNameView.setText(artists.get(0).name);
        }

        TextView albumNameView = (TextView) rootView.findViewById(R.id.album_name);
        albumNameView.setText(mTrack.album.name);

        return rootView;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        List<Image> images = mTrack.album.images;
        String url = null;
        if (images != null && !images.isEmpty()) {
            url = images.get(0).url;
        }
        Glide.with(getActivity()).load(url).error(R.drawable.default_album_image).into(mAlbumImageView);
    }
}
