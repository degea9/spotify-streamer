package com.github.mjhassanpur.spotifystreamer.fragments;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.media.MediaPlayer;
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
import com.github.mjhassanpur.spotifystreamer.Types;
import com.github.mjhassanpur.spotifystreamer.services.MusicService;
import com.google.gson.Gson;

import java.util.List;

import kaaes.spotify.webapi.android.models.ArtistSimple;
import kaaes.spotify.webapi.android.models.Image;
import kaaes.spotify.webapi.android.models.Track;

public class PlayerFragment extends DialogFragment {

    private Track mSelectedTrack;
    private List<Track> mTrackList;
    private Gson mGson;
    private MediaPlayer mPlayer;
    private SeekBar mSeekbar;
    private ImageView mAlbumImage;
    private TextView mTrackName;
    private TextView mArtistName;
    private TextView mAlbumName;
    private ImageView mPrev;
    private ImageView mPlayPause;
    private ImageView mNext;
    private int mTrackPosition;
    private final String KEY_SELECTED_TRACK = "selectedTrack";
    private final String KEY_TRACKS = "tracks";
    private MusicService mBoundService;
    private boolean mIsBound = false;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        mGson = new Gson();
        mTrackPosition = getActivity().getIntent().getIntExtra(KEY_SELECTED_TRACK, 0);
        String json = getActivity().getIntent().getStringExtra(KEY_TRACKS);
        if (json != null) {
            mTrackList = mGson.fromJson(json, Types.TRACK_LIST);
            mSelectedTrack = mTrackList.get(mTrackPosition);

        }
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_player, container, false);
        mAlbumImage = (ImageView) rootView.findViewById(R.id.album_image);
        mTrackName = (TextView) rootView.findViewById(R.id.track_name);
        mArtistName = (TextView) rootView.findViewById(R.id.artist_name);
        mAlbumName = (TextView) rootView.findViewById(R.id.album_name);
        mSeekbar = (SeekBar) rootView.findViewById(R.id.seekBar);
        mPrev = (ImageView) rootView.findViewById(R.id.prev);
        mPlayPause = (ImageView) rootView.findViewById(R.id.play_pause);
        mNext = (ImageView) rootView.findViewById(R.id.next);

        mTrackName.setText(mSelectedTrack.name);

        List<ArtistSimple> artists = mSelectedTrack.artists;
        if (artists != null && !artists.isEmpty()) {
            mArtistName.setText(artists.get(0).name);
        }

        mAlbumName.setText(mSelectedTrack.album.name);

        mPlayPause.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (mBoundService.isPlaying()) {
                    mBoundService.pause();
                } else {
                    mBoundService.play();
                }
            }
        });


        return rootView;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        List<Image> images = mSelectedTrack.album.images;
        String url = null;
        if (images != null && !images.isEmpty()) {
            url = images.get(0).url;
        }
        Glide.with(getActivity()).load(url).error(R.drawable.default_album_image).into(mAlbumImage);
        Intent intent = new Intent(getActivity(), MusicService.class);
        intent.setAction(MusicService.ACTION_PLAY);
        intent.putExtra(MusicService.KEY_URL, mSelectedTrack.preview_url);
        getActivity().startService(intent);
        doBindService();
    }

    @Override
    public void onDestroy() {
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
