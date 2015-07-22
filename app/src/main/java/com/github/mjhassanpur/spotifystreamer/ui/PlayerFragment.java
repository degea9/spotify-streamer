/*
 * Copyright (C) 2014 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.github.mjhassanpur.spotifystreamer.ui;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.RemoteException;
import android.os.SystemClock;
import android.support.v4.app.DialogFragment;
import android.support.v4.media.MediaMetadataCompat;
import android.support.v4.media.session.MediaControllerCompat;
import android.support.v4.media.session.PlaybackStateCompat;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.github.mjhassanpur.spotifystreamer.MusicService;
import com.github.mjhassanpur.spotifystreamer.R;
import com.github.mjhassanpur.spotifystreamer.utils.LogHelper;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import kaaes.spotify.webapi.android.models.Track;

import static android.view.View.INVISIBLE;
import static android.view.View.VISIBLE;

public class PlayerFragment extends DialogFragment implements MusicService.Callback {

    private static final String TAG = LogHelper.makeLogTag(PlayerActivity.class);

    private static final long PROGRESS_UPDATE_INTERNAL = 500;
    private static final long PROGRESS_UPDATE_INITIAL_INTERVAL = 100;

    public static final String KEY_SELECTED_TRACK = "selectedTrack";
    public static final String KEY_TRACKS = "tracks";

    private SeekBar mSeekbar;
    private ImageView mAlbumImage;
    private TextView mTrackName;
    private TextView mArtistName;
    private TextView mAlbumName;
    private ImageView mSkipPrev;
    private ImageView mPlayPause;
    private ImageView mSkipNext;

    private MediaControllerCompat mMediaController;

    private MusicService mBoundService;
    private boolean mIsBound = false;

    private Gson mGson;
    private int mTrackPosition;
    private Track mSelectedTrack;
    private List<Track> mTrackList;
    private final Type mTrackListType = new TypeToken<List<Track>>() {}.getType();

    private Handler mHandler = new Handler();
    private final Runnable mUpdateProgressTask = new Runnable() {
        @Override
        public void run() {
            updateProgress();
        }
    };

    private final ScheduledExecutorService mExecutorService =
            Executors.newSingleThreadScheduledExecutor();

    private ScheduledFuture<?> mScheduleFuture;
    private PlaybackStateCompat mLastPlaybackState;

    private MediaControllerCompat.Callback mCallback = new MediaControllerCompat.Callback() {
        @Override
        public void onPlaybackStateChanged(PlaybackStateCompat state) {
            LogHelper.d(TAG, "onPlaybackstate changed", state);
            updatePlaybackState(state);
        }

        @Override
        public void onMetadataChanged(MediaMetadataCompat metadata) {
            if (metadata != null) {
                updateMediaDescription(metadata);
                updateDuration(metadata);
            }
        }
    };

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mGson = new Gson();
        String json;
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
        if (json != null && !json.equals("null")) {
            mTrackList = mGson.fromJson(json, mTrackListType);
            mSelectedTrack = mTrackList.get(mTrackPosition);
        }
        setHasOptionsMenu(true);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_player, container, false);
        mSeekbar = (SeekBar) rootView.findViewById(R.id.seekBar);
        mAlbumImage = (ImageView) rootView.findViewById(R.id.album_image);
        mTrackName = (TextView) rootView.findViewById(R.id.track_name);
        mArtistName = (TextView) rootView.findViewById(R.id.artist_name);
        mAlbumName = (TextView) rootView.findViewById(R.id.album_name);
        mSkipPrev = (ImageView) rootView.findViewById(R.id.prev);
        mPlayPause = (ImageView) rootView.findViewById(R.id.play_pause);
        mSkipNext = (ImageView) rootView.findViewById(R.id.next);

        mSkipPrev.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                MediaControllerCompat.TransportControls controls = mMediaController.getTransportControls();
                controls.skipToPrevious();
            }
        });

        mPlayPause.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                PlaybackStateCompat state = mMediaController.getPlaybackState();
                MediaControllerCompat.TransportControls controls = mMediaController.getTransportControls();
                switch (state.getState()) {
                    case PlaybackStateCompat.STATE_PLAYING:
                    case PlaybackStateCompat.STATE_BUFFERING:
                        controls.pause();
                        stopSeekbarUpdate();
                        break;
                    case PlaybackStateCompat.STATE_PAUSED:
                    case PlaybackStateCompat.STATE_STOPPED:
                        controls.play();
                        scheduleSeekbarUpdate();
                        break;
                    default:
                        LogHelper.d(TAG, "onClick with state ", state.getState());
                }
            }
        });

        mSkipNext.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                MediaControllerCompat.TransportControls controls = mMediaController.getTransportControls();
                controls.skipToNext();
            }
        });

        mSeekbar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                stopSeekbarUpdate();
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                mMediaController.getTransportControls().seekTo(seekBar.getProgress());
                scheduleSeekbarUpdate();
            }
        });

        return rootView;
    }

    @Override
    public void onPrepareOptionsMenu(Menu menu) {
        super.onPrepareOptionsMenu(menu);
        menu.findItem(R.id.action_share).setVisible(true);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.action_share) {
            Intent i = new Intent(Intent.ACTION_SEND);
            i.setType("text/plain");
            String url = mTrackList.get(mTrackPosition).external_urls.get("spotify");
            i.putExtra(Intent.EXTRA_TEXT, url);
            startActivity(Intent.createChooser(i, "Share Track"));
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt(KEY_SELECTED_TRACK, mTrackPosition);
        outState.putString(KEY_TRACKS, mGson.toJson(mTrackList, mTrackListType));
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        Intent intent = new Intent(getActivity(), MusicService.class);
        intent.setAction(MusicService.ACTION_CMD);
        intent.putExtra(MusicService.CMD_NAME, MusicService.CMD_PLAY);
        intent.putExtra(KEY_SELECTED_TRACK, mTrackPosition);
        intent.putExtra(KEY_TRACKS, mGson.toJson(mTrackList, mTrackListType));
        getActivity().startService(intent);
    }

    @Override
    public void onStart() {
        super.onStart();
        doBindService();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        stopSeekbarUpdate();
        mExecutorService.shutdown();
    }

    @Override
    public void onStop() {
        super.onStop();
        doUnbindService();
        if (mMediaController != null) {
            mMediaController.unregisterCallback(mCallback);
        }
    }

    private void scheduleSeekbarUpdate() {
        stopSeekbarUpdate();
        if (!mExecutorService.isShutdown()) {
            mScheduleFuture = mExecutorService.scheduleAtFixedRate(
                    new Runnable() {
                        @Override
                        public void run() {
                            mHandler.post(mUpdateProgressTask);
                        }
                    }, PROGRESS_UPDATE_INITIAL_INTERVAL,
                    PROGRESS_UPDATE_INTERNAL, TimeUnit.MILLISECONDS);
        }
    }

    private void stopSeekbarUpdate() {
        if (mScheduleFuture != null) {
            mScheduleFuture.cancel(false);
        }
    }

    private void updateMediaDescription(MediaMetadataCompat metadata) {
        if (metadata == null) {
            return;
        }
        LogHelper.d(TAG, "updateMediaDescription called ");
        mTrackName.setText(metadata.getString(MediaMetadataCompat.METADATA_KEY_TITLE));
        mArtistName.setText(metadata.getString(MediaMetadataCompat.METADATA_KEY_ARTIST));
        mAlbumName.setText(metadata.getString(MediaMetadataCompat.METADATA_KEY_ALBUM));
        Glide.with(this)
                .load(metadata.getString(MediaMetadataCompat.METADATA_KEY_ALBUM_ART_URI))
                .error(R.drawable.default_album_image)
                .into(mAlbumImage);
    }

    private void updateDuration(MediaMetadataCompat metadata) {
        if (metadata == null) {
            return;
        }
        LogHelper.d(TAG, "updateDuration called ");
        int duration = (int) metadata.getLong(MediaMetadataCompat.METADATA_KEY_DURATION);
        mSeekbar.setMax(duration);
    }

    private void updatePlaybackState(PlaybackStateCompat state) {
        if (state == null) {
            return;
        }
        mLastPlaybackState = state;

        switch (state.getState()) {
            case PlaybackStateCompat.STATE_PLAYING:
                mPlayPause.setVisibility(VISIBLE);
                mPlayPause.setImageResource(R.drawable.ic_pause_primary_dark_48dp);
                scheduleSeekbarUpdate();
                break;
            case PlaybackStateCompat.STATE_PAUSED:
                mPlayPause.setVisibility(VISIBLE);
                mPlayPause.setImageResource(R.drawable.ic_play_arrow_primary_dark_48dp);
                stopSeekbarUpdate();
                break;
            case PlaybackStateCompat.STATE_NONE:
            case PlaybackStateCompat.STATE_STOPPED:
                mPlayPause.setVisibility(VISIBLE);
                mPlayPause.setImageResource(R.drawable.ic_play_arrow_primary_dark_48dp);
                stopSeekbarUpdate();
                break;
            case PlaybackStateCompat.STATE_BUFFERING:
                mPlayPause.setVisibility(INVISIBLE);
                stopSeekbarUpdate();
                break;
            default:
                LogHelper.d(TAG, "Unhandled state ", state.getState());
        }
    }

    private void updateProgress() {
        if (mLastPlaybackState == null) {
            return;
        }
        long currentPosition = mLastPlaybackState.getPosition();
        if (mLastPlaybackState.getState() != PlaybackStateCompat.STATE_PAUSED) {
            // Calculate the elapsed time between the last position update and now and unless
            // paused, we can assume (delta * speed) + current position is approximately the
            // latest position. This ensure that we do not repeatedly call the getPlaybackState()
            // on MediaController.
            long timeDelta = SystemClock.elapsedRealtime() -
                    mLastPlaybackState.getLastPositionUpdateTime();
            currentPosition += (int) timeDelta * mLastPlaybackState.getPlaybackSpeed();
        }
        mSeekbar.setProgress((int) currentPosition);
    }

    @Override
    public void onTrackChange(int position) {
        mTrackPosition = position;
    }

    @Override
    public void onServiceStateChange(boolean started) {

    }

    private void registerCallback(MusicService boundService) {
        boundService.registerCallback(this);
    }

    private void unregisterCallback(MusicService boundService) {
        boundService.unregisterCallback(this);
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
            try {
                mMediaController = new MediaControllerCompat(getActivity(), mBoundService.getSessionToken());
                mMediaController.registerCallback(mCallback);
                registerCallback(mBoundService);
                PlaybackStateCompat state = mMediaController.getPlaybackState();
                updatePlaybackState(state);
                MediaMetadataCompat metadata = mMediaController.getMetadata();
                if (metadata != null) {
                    updateMediaDescription(metadata);
                    updateDuration(metadata);
                }
                updateProgress();
                if (state != null && (state.getState() == PlaybackStateCompat.STATE_PLAYING ||
                        state.getState() == PlaybackStateCompat.STATE_BUFFERING)) {
                    scheduleSeekbarUpdate();
                }
            } catch (RemoteException ex) {
                ex.printStackTrace();
            }
        }

        public void onServiceDisconnected(ComponentName className) {
            unregisterCallback(mBoundService);
            mBoundService = null;
        }
    };
}
