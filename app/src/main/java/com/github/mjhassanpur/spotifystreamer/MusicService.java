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

package com.github.mjhassanpur.spotifystreamer;

import android.app.PendingIntent;
import android.app.Service;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.os.Binder;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.SystemClock;
import android.support.v4.media.MediaMetadataCompat;
import android.support.v4.media.session.MediaSessionCompat;
import android.support.v4.media.session.PlaybackStateCompat;

import com.github.mjhassanpur.spotifystreamer.media.MediaButtonReceiver;
import com.github.mjhassanpur.spotifystreamer.media.MediaNotificationManager;
import com.github.mjhassanpur.spotifystreamer.media.MediaPlayback;
import com.github.mjhassanpur.spotifystreamer.media.MediaProvider;
import com.github.mjhassanpur.spotifystreamer.media.Playback;
import com.github.mjhassanpur.spotifystreamer.ui.PlayerActivity;
import com.github.mjhassanpur.spotifystreamer.ui.PlayerFragment;
import com.github.mjhassanpur.spotifystreamer.utils.LogHelper;
import com.github.mjhassanpur.spotifystreamer.utils.PreferenceHelper;
import com.github.mjhassanpur.spotifystreamer.utils.QueueHelper;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.ref.WeakReference;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import kaaes.spotify.webapi.android.models.Track;

/**
 * A service for streaming music
 *
 * @see <a href="https://github.com/googlesamples/android-UniversalMusicPlayer/blob/master/mobile/src/main/java/com/example/android/uamp/MusicService.java"></a>
 * @see <a href="https://github.com/chrisanderson79/android-UniversalMusicPlayer/blob/master/mobile/src/main/java/com/example/android/uamp/MusicService.java"></a>
 */
public class MusicService extends Service implements Playback.Callback {

    private static final String TAG = LogHelper.makeLogTag(MusicService.class);

    // The action of the incoming Intent indicating that it contains a command
    // to be executed (see {@link #onStartCommand})
    public static final String ACTION_CMD = "com.github.mjhassanpur.spotifystreamer.ACTION_CMD";
    // The key in the extras of the incoming Intent indicating the command that
    // should be executed (see {@link #onStartCommand})
    public static final String CMD_NAME = "CMD_NAME";
    // A value of a CMD_NAME key in the extras of the incoming Intent that
    // indicates that the music playback should be paused (see {@link #onStartCommand})
    public static final String CMD_PAUSE = "CMD_PAUSE";
    // A value of a CMD_NAME key in the extras of the incoming Intent that
    // indicates that the music playback should play (see {@link #onStartCommand})
    public static final String CMD_PLAY = "CMD_PLAY";

    // Delay stopSelf by using a handler.
    private static final int STOP_DELAY = 30000;

    private MediaProvider mMediaProvider;
    private MediaSessionCompat mSession;
    private MediaSessionCompat.Token mSessionToken;
    private List<MediaSessionCompat.QueueItem> mPlayingQueue;
    private int mCurrentIndexOnQueue = -1;
    private MediaPlayback mPlayback;
    private List<Track> mTracks;
    private Map<Integer, Callback> mCallbacks;

    private MediaNotificationManager mMediaNotificationManager;

    private boolean mServiceStarted;
    private DelayedStopHandler mDelayedStopHandler = new DelayedStopHandler(this);

    private Handler mHandler = new Handler();
    private Runnable mDelayedAction = null;

    private final IBinder mBinder = new MusicBinder();

    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    @Override
    public void onRebind(Intent intent) {
        mHandler.removeCallbacks(mDelayedAction);
        super.onRebind(intent);
    }

    @Override
    public boolean onUnbind(Intent intent) {
        mDelayedAction = new Runnable() {
            @Override
            public void run() {
                stopSelf();
            }
        };
        mHandler.postDelayed(mDelayedAction, 1000);
        return true;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        LogHelper.d(TAG, "onCreate");

        mPlayingQueue = new ArrayList<>();
        mMediaProvider = new MediaProvider();

        ComponentName eventReceiver = new ComponentName(getPackageName(), MediaButtonReceiver.class.getName());

        Intent mediaButtonIntent = new Intent(Intent.ACTION_MEDIA_BUTTON);
        mediaButtonIntent.setComponent(eventReceiver);
        PendingIntent mediaPendingIntent = PendingIntent.getBroadcast(this, 0, mediaButtonIntent, 0);

        mSession = new MediaSessionCompat(this, "MusicService", eventReceiver, mediaPendingIntent);

        final MediaSessionCallback cb = new MediaSessionCallback();
        mSession.setCallback(cb);

        setSessionToken(mSession.getSessionToken());
        mSession.setFlags(MediaSessionCompat.FLAG_HANDLES_MEDIA_BUTTONS |
                MediaSessionCompat.FLAG_HANDLES_TRANSPORT_CONTROLS);

        mPlayback = new MediaPlayback(this, mMediaProvider);
        mPlayback.setState(PlaybackStateCompat.STATE_NONE);
        mPlayback.setCallback(this);
        mPlayback.start();

        Context context = getApplicationContext();
        Intent intent = new Intent(context, PlayerActivity.class);
        PendingIntent pi = PendingIntent.getActivity(context, 99 /*request code*/,
                intent, PendingIntent.FLAG_UPDATE_CURRENT);

        mSession.setSessionActivity(pi);

        Bundle sessionExtras = new Bundle();
        mSession.setExtras(sessionExtras);

        updatePlaybackState(null);

        if (PreferenceHelper.isNotificationsEnabled(this)) {
            mMediaNotificationManager = new MediaNotificationManager(this);
        }

        mCallbacks = new HashMap<>();
    }

    @Override
    public int onStartCommand(Intent startIntent, int flags, int startId) {
        if (startIntent != null) {
            String action = startIntent.getAction();

            boolean resumeCurrentTrack = false;
            int trackPosition = startIntent.getIntExtra(PlayerFragment.KEY_SELECTED_TRACK, -1);
            String json = startIntent.getStringExtra(PlayerFragment.KEY_TRACKS);
            if (json != null && !json.equals("null")) {
                final Type mTrackListType = new TypeToken<List<Track>>() {}.getType();
                List<Track> tracks = new Gson().fromJson(json, mTrackListType);
                if (mMediaProvider.isEqual(tracks, mTracks) && mCurrentIndexOnQueue != -1) {
                    int indexOnQueue =
                            QueueHelper.getMediaIndexOnQueue(mPlayingQueue, tracks.get(trackPosition).id);
                    if (mCurrentIndexOnQueue == indexOnQueue) {
                        // Resumes current track when selected from top tracks list
                        resumeCurrentTrack = true;
                    } else {
                        mCurrentIndexOnQueue = indexOnQueue;
                    }
                } else {
                    mTracks = tracks;
                    mMediaProvider.setMusicList(tracks);
                    mPlayingQueue = QueueHelper.getPlayingQueue(mMediaProvider);
                    mCurrentIndexOnQueue =
                            QueueHelper.getMediaIndexOnQueue(mPlayingQueue, tracks.get(trackPosition).id);
                }
            } else {
                // Resumes current track when player UI is launched from notification
                resumeCurrentTrack = true;
            }

            String command = startIntent.getStringExtra(CMD_NAME);
            if (ACTION_CMD.equals(action)) {
                if (CMD_PLAY.equals(command) && !resumeCurrentTrack) {
                    handlePlayRequest();
                } else if (CMD_PAUSE.equals(command)) {
                    if (mPlayback != null && mPlayback.isPlaying()) {
                        handlePauseRequest();
                    }
                }
            }
        }
        // Reset the delay handler to enqueue a message to stop the service if
        // nothing is playing.
        mDelayedStopHandler.removeCallbacksAndMessages(null);
        mDelayedStopHandler.sendEmptyMessageDelayed(0, STOP_DELAY);
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        LogHelper.d(TAG, "onDestroy");
        // Service is being killed, so make sure we release our resources
        handleStopRequest(null);

        mDelayedStopHandler.removeCallbacksAndMessages(null);
        // Always release the MediaSession to clean up resources
        // and notify associated MediaController(s).
        mSession.release();
    }

    /**
     * Call to set the media session.
     * <p>
     * This must be called before onCreate returns.
     *
     * @return The media session token, must not be null.
     */
    public void setSessionToken(MediaSessionCompat.Token token) {
        if (token == null) {
            throw new IllegalStateException(this.getClass().getName()
                    + ".onCreateSession() set invalid MediaSession.Token");
        }
        mSessionToken = token;
    }

    /**
     * Gets the session token, or null if it has not yet been created
     * or if it has been destroyed.
     */
    public MediaSessionCompat.Token getSessionToken() {
        return mSessionToken;
    }

    private final class MediaSessionCallback extends MediaSessionCompat.Callback {

        @Override
        public void onPlay() {
            LogHelper.d(TAG, "play");
            if (mPlayingQueue != null && !mPlayingQueue.isEmpty()) {
                handlePlayRequest();
            }
        }

        @Override
        public void onSkipToQueueItem(long queueId) {
            LogHelper.d(TAG, "OnSkipToQueueItem:" + queueId);
            if (mPlayingQueue != null && !mPlayingQueue.isEmpty()) {
                mCurrentIndexOnQueue = QueueHelper.getMediaIndexOnQueue(mPlayingQueue, queueId);
                handlePlayRequest();
            }
        }

        @Override
        public void onSeekTo(long position) {
            LogHelper.d(TAG, "onSeekTo:", position);
            mPlayback.seekTo((int) position);
        }

        @Override
        public void onPause() {
            LogHelper.d(TAG, "pause. current state=" + mPlayback.getState());
            handlePauseRequest();
        }

        @Override
        public void onStop() {
            LogHelper.d(TAG, "stop. current state=" + mPlayback.getState());
            handleStopRequest(null);
        }

        @Override
        public void onSkipToNext() {
            LogHelper.d(TAG, "skipToNext");
            mCurrentIndexOnQueue++;
            if (mPlayingQueue != null && mCurrentIndexOnQueue >= mPlayingQueue.size()) {
                mCurrentIndexOnQueue = 0;
            }
            notifyTrackChanged();
            if (QueueHelper.isIndexPlayable(mCurrentIndexOnQueue, mPlayingQueue)) {
                handlePlayRequest();
            } else {
                LogHelper.e(TAG, "skipToNext: cannot skip to next. next Index=" +
                        mCurrentIndexOnQueue + " queue length=" +
                        (mPlayingQueue == null ? "null" : mPlayingQueue.size()));
                handleStopRequest("Cannot skip");
            }
        }

        @Override
        public void onSkipToPrevious() {
            LogHelper.d(TAG, "skipToPrevious");
            mCurrentIndexOnQueue--;
            if (mPlayingQueue != null && mCurrentIndexOnQueue < 0) {
                mCurrentIndexOnQueue = 0;
            }
            notifyTrackChanged();
            if (QueueHelper.isIndexPlayable(mCurrentIndexOnQueue, mPlayingQueue)) {
                handlePlayRequest();
            } else {
                LogHelper.e(TAG, "skipToPrevious: cannot skip to previous. previous Index=" +
                        mCurrentIndexOnQueue + " queue length=" +
                        (mPlayingQueue == null ? "null" : mPlayingQueue.size()));
                handleStopRequest("Cannot skip");
            }
        }
    }

    private void handlePlayRequest() {
        LogHelper.d(TAG, "handlePlayRequest: mState=" + mPlayback.getState());

        mDelayedStopHandler.removeCallbacksAndMessages(null);
        if (!mServiceStarted) {
            LogHelper.v(TAG, "Starting service");
            // The service needs to keep running until we no longer need to play media.
            startService(new Intent(getApplicationContext(), MusicService.class));
            mServiceStarted = true;
            notifyServiceStateChanged();
        }

        if (QueueHelper.isIndexPlayable(mCurrentIndexOnQueue, mPlayingQueue)) {
            updateMetadata();
            mPlayback.play(mPlayingQueue.get(mCurrentIndexOnQueue));
        }

        // With the compatibility library this has to be set after the metadata.
        if (!mSession.isActive()) {
            mSession.setActive(true);
        }
    }

    private void handlePauseRequest() {
        LogHelper.d(TAG, "handlePauseRequest: mState=" + mPlayback.getState());
        mPlayback.pause();
        // reset the delayed stop handler.
        mDelayedStopHandler.removeCallbacksAndMessages(null);
        mDelayedStopHandler.sendEmptyMessageDelayed(0, STOP_DELAY);
    }

    private void handleStopRequest(String withError) {
        LogHelper.d(TAG, "handleStopRequest: mState=" + mPlayback.getState() + " error=", withError);
        mPlayback.stop(true);
        // reset the delayed stop handler.
        mDelayedStopHandler.removeCallbacksAndMessages(null);
        mDelayedStopHandler.sendEmptyMessageDelayed(0, STOP_DELAY);

        updatePlaybackState(withError);

        // service is no longer necessary. Will be started again if needed.
        stopSelf();
        mServiceStarted = false;
        notifyServiceStateChanged();
    }

    private void updateMetadata() {
        if (!QueueHelper.isIndexPlayable(mCurrentIndexOnQueue, mPlayingQueue)) {
            LogHelper.e(TAG, "Can't retrieve current metadata.");
            updatePlaybackState("Error, no metadata");
            return;
        }
        MediaSessionCompat.QueueItem queueItem = mPlayingQueue.get(mCurrentIndexOnQueue);
        String mediaId = queueItem.getDescription().getMediaId();
        MediaMetadataCompat track = mMediaProvider.getMusic(mediaId);
        final String trackId = track.getString(MediaMetadataCompat.METADATA_KEY_MEDIA_ID);
        if (!mediaId.equals(trackId)) {
            IllegalStateException e = new IllegalStateException("track ID should match mediaId.");
            LogHelper.e(TAG, "track ID should match mediaId.",
                    " mediaId=", mediaId, " trackId=", trackId,
                    " mediaId from queueItem=", queueItem.getDescription().getMediaId(),
                    " title from queueItem=", queueItem.getDescription().getTitle(),
                    " mediaId from track=", track.getDescription().getMediaId(),
                    " title from track=", track.getDescription().getTitle(),
                    " source.hashcode from track=",
                        track.getString(MediaProvider.CUSTOM_METADATA_TRACK_SOURCE).hashCode(),
                    e);
            throw e;
        }
        LogHelper.d(TAG, "Updating metadata for MediaID= " + mediaId);
        mSession.setMetadata(track);
    }

    /**
     * Update the current media player state, optionally showing an error message.
     *
     * @param error if not null, error message to present to the user.
     */
    private void updatePlaybackState(String error) {
        LogHelper.d(TAG, "updatePlaybackState, playback state=" + mPlayback.getState());
        long position = PlaybackStateCompat.PLAYBACK_POSITION_UNKNOWN;
        if (mPlayback != null && mPlayback.isConnected()) {
            position = mPlayback.getCurrentStreamPosition();
        }

        PlaybackStateCompat.Builder stateBuilder = new PlaybackStateCompat.Builder()
                .setActions(getAvailableActions());

        int state = mPlayback.getState();

        // If there is an error message, send it to the playback state:
        if (error != null) {
            // Error states are really only supposed to be used for errors that cause playback to
            // stop unexpectedly and persist until the user takes action to fix it.
            stateBuilder.setErrorMessage(error);
            state = PlaybackStateCompat.STATE_ERROR;
        }
        stateBuilder.setState(state, position, 1.0f, SystemClock.elapsedRealtime());

        mSession.setPlaybackState(stateBuilder.build());

        if ((state == PlaybackStateCompat.STATE_PLAYING || state == PlaybackStateCompat.STATE_PAUSED)
                && mMediaNotificationManager != null) {
            mMediaNotificationManager.startNotification();
        }
    }

    private long getAvailableActions() {
        long actions = PlaybackStateCompat.ACTION_PLAY;
        if (mPlayingQueue == null || mPlayingQueue.isEmpty()) {
            return actions;
        }
        if (mPlayback.isPlaying()) {
            actions |= PlaybackStateCompat.ACTION_PAUSE;
        }
        if (mCurrentIndexOnQueue > 0) {
            actions |= PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS;
        }
        if (mCurrentIndexOnQueue < mPlayingQueue.size() - 1) {
            actions |= PlaybackStateCompat.ACTION_SKIP_TO_NEXT;
        }
        return actions;
    }

    @Override
    public void onCompletion() {
        // The media player finished playing the current track, so we go ahead
        // and start the next.
        if (mPlayingQueue != null && !mPlayingQueue.isEmpty()) {
            mCurrentIndexOnQueue++;
            if (mCurrentIndexOnQueue >= mPlayingQueue.size()) {
                mCurrentIndexOnQueue = 0;
            }
            notifyTrackChanged();
            handlePlayRequest();
        } else {
            // If there is nothing to play, we stop and release the resources:
            handleStopRequest(null);
        }
    }

    @Override
    public void onPlaybackStatusChanged(int state) {
        updatePlaybackState(null);
    }

    @Override
    public void onError(String error) {
        updatePlaybackState(error);
    }

    @Override
    public void onMetadataChanged(String mediaId) {
        LogHelper.d(TAG, "onMetadataChanged", mediaId);
        List<MediaSessionCompat.QueueItem> queue = QueueHelper.getPlayingQueue(mMediaProvider);
        int index = QueueHelper.getMediaIndexOnQueue(queue, mediaId);
        if (index > -1) {
            mCurrentIndexOnQueue = index;
            mPlayingQueue = queue;
            updateMetadata();
        }
    }

    public boolean hasServiceStarted() {
        return mServiceStarted;
    }

    private void notifyTrackChanged() {
        for (Callback cb : mCallbacks.values()) {
            cb.onTrackChange(mCurrentIndexOnQueue);
        }
    }

    private void notifyServiceStateChanged() {
        for (Callback cb : mCallbacks.values()) {
            cb.onServiceStateChange(mServiceStarted);
        }
    }

    public interface Callback {
        void onTrackChange(int position);
        void onServiceStateChange(boolean started);
    }

    public void registerCallback(Callback cb) {
        mCallbacks.put(cb.hashCode(), cb);
    }

    public void unregisterCallback(Callback cb) {
        if (mCallbacks.containsKey(cb.hashCode())) {
            mCallbacks.remove(cb.hashCode());
        }
    }

    public class MusicBinder extends Binder {

        public MusicService getService() {
            return MusicService.this;
        }
    }

    /**
     * A simple handler that stops the service if playback is not active (playing)
     */
    private static class DelayedStopHandler extends Handler {

        private final WeakReference<MusicService> mWeakReference;

        private DelayedStopHandler(MusicService service) {
            mWeakReference = new WeakReference<>(service);
        }

        @Override
        public void handleMessage(Message msg) {
            MusicService service = mWeakReference.get();
            if (service != null && service.mPlayback != null) {
                if (service.mPlayback.isPlaying()) {
                    LogHelper.d(TAG, "Ignoring delayed stop since the media player is in use.");
                    return;
                }
                LogHelper.d(TAG, "Stopping service with delay handler.");
                service.stopSelf();
                service.mServiceStarted = false;
                service.notifyServiceStateChanged();
            }
        }
    }
}