package com.github.mjhassanpur.spotifystreamer.fragments;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.support.v4.app.DialogFragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.github.mjhassanpur.spotifystreamer.R;
import com.github.mjhassanpur.spotifystreamer.services.MusicService;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.List;

import butterknife.ButterKnife;
import butterknife.InjectView;
import butterknife.OnClick;
import kaaes.spotify.webapi.android.models.ArtistSimple;
import kaaes.spotify.webapi.android.models.Image;
import kaaes.spotify.webapi.android.models.Track;

public class PlayerFragment extends DialogFragment {
    @InjectView(R.id.seekBar) SeekBar mSeekbar;
    @InjectView(R.id.album_image) ImageView mAlbumImage;
    @InjectView(R.id.track_name) TextView mTrackName;
    @InjectView(R.id.artist_name) TextView mArtistName;
    @InjectView(R.id.album_name) TextView mAlbumName;
    @InjectView(R.id.prev) ImageView mPrev;
    @InjectView(R.id.play_pause) ImageView mPlayPause;
    @InjectView(R.id.next) ImageView mNext;
    private Track mSelectedTrack;
    private List<Track> mTrackList;
    private Gson mGson;
    private MusicService mBoundService;
    private boolean mIsBound = false;
    private int mTrackPosition;
    private final String KEY_SELECTED_TRACK = "selectedTrack";
    private final String KEY_TRACKS = "tracks";
    private final Type mTrackListType = new TypeToken<List<Track>>() {}.getType();

    @Override public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mGson = new Gson();
        String json = null;
        if (savedInstanceState == null) {
            Bundle arguments = getArguments();
            if (arguments != null) {
                mTrackPosition = arguments.getInt(KEY_SELECTED_TRACK);
                json = arguments.getString(KEY_TRACKS);
            } else {
                mTrackPosition = getActivity().getIntent().getIntExtra(KEY_SELECTED_TRACK, 0);
                json = getActivity().getIntent().getStringExtra(KEY_TRACKS);
            }
        } else {
            mTrackPosition = savedInstanceState.getInt(KEY_SELECTED_TRACK);
            json = savedInstanceState.getString(KEY_TRACKS);
        }
        if (json != null) {
            mTrackList = mGson.fromJson(json, mTrackListType);
            mSelectedTrack = mTrackList.get(mTrackPosition);
        }
    }

    @Override public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_player, container, false);
        ButterKnife.inject(this, view);
        setupView();
        return view;
    }

    private void setupView() {
        List<Image> images = mSelectedTrack.album.images;
        String url = null;
        if (images != null && !images.isEmpty()) {
            url = images.get(0).url;
        }
        Glide.with(this).load(url).error(R.drawable.default_album_image).into(mAlbumImage);
        mTrackName.setText(mSelectedTrack.name);
        List<ArtistSimple> artists = mSelectedTrack.artists;
        if (artists != null && !artists.isEmpty()) {
            mArtistName.setText(artists.get(0).name);
        }
        mAlbumName.setText(mSelectedTrack.album.name);
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt(KEY_SELECTED_TRACK, mTrackPosition);
        outState.putString(KEY_TRACKS, mGson.toJson(mTrackList, mTrackListType));
    }

    @OnClick(R.id.play_pause) public void playPause() {
        if (mBoundService.isPlaying()) {
            mBoundService.pause();
            mPlayPause.setImageResource(android.R.drawable.ic_media_play);
        } else {
            mBoundService.play();
            mPlayPause.setImageResource(android.R.drawable.ic_media_pause);
        }
    }

    @OnClick(R.id.next) public void skipToNext() {
        if (mTrackPosition < mTrackList.size() - 1) {
            mTrackPosition++;
        } else {
            mTrackPosition = 0;
        }
        mSelectedTrack = mTrackList.get(mTrackPosition);
        mBoundService.skipToNext(mSelectedTrack.preview_url);
        setupView();
    }

    @OnClick(R.id.prev) public void skipToPrev() {
        if (mTrackPosition > 0) {
            mTrackPosition--;
        } else {
            mTrackPosition = mTrackList.size() - 1;
        }
        mSelectedTrack = mTrackList.get(mTrackPosition);
        mBoundService.skipToPrev(mSelectedTrack.preview_url);
        setupView();
    }

    @Override public void onDestroyView() {
        super.onDestroyView();
        ButterKnife.reset(this);
    }

    @Override public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        Intent intent = new Intent(getActivity(), MusicService.class);
        intent.setAction(MusicService.ACTION_PLAY);
        intent.putExtra(MusicService.KEY_URL, mSelectedTrack.preview_url);
        getActivity().startService(intent);
        doBindService();
    }

    @Override public void onDestroy() {
        super.onDestroy();
        getActivity().stopService(new Intent(getActivity(), MusicService.class));
        doUnbindService();
    }

    void doBindService() {
        getActivity().bindService(
                new Intent(getActivity(), MusicService.class), mConnection, Context.BIND_AUTO_CREATE);
        mIsBound = true;
    }

    void doUnbindService() {
        if (mIsBound) {
            getActivity().unbindService(mConnection);
            mIsBound = false;
        }
    }

    private ServiceConnection mConnection = new ServiceConnection() {
        public void onServiceConnected(ComponentName className, IBinder service) {
            mBoundService = ((MusicService.MusicBinder)service).getService();
        }

        public void onServiceDisconnected(ComponentName className) {
            mBoundService = null;
        }
    };
}
