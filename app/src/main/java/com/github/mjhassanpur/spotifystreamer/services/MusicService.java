package com.github.mjhassanpur.spotifystreamer.services;

import android.app.Service;
import android.content.Intent;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.os.Binder;
import android.os.IBinder;
import android.util.Log;

import com.github.mjhassanpur.spotifystreamer.Playback;

import java.io.IOException;

public class MusicService extends Service implements Playback, MediaPlayer.OnPreparedListener,
        MediaPlayer.OnSeekCompleteListener, MediaPlayer.OnCompletionListener, MediaPlayer.OnErrorListener {
    public static final String ACTION_PLAY = "com.github.mjhassanpur.spotifystreamer.ACTION_PLAY";
    public static final String KEY_URL = "url";
    private static final String TAG = "MusicService";
    private String mUrl;
    private MediaPlayer mMediaPlayer = null;
    private final IBinder mBinder = new MusicBinder();

    @Override public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent.getAction().equals(ACTION_PLAY)) {
            mUrl = intent.getStringExtra(KEY_URL);
            try {
                initMediaPlayer();
                mMediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
                mMediaPlayer.setDataSource(mUrl);
                mMediaPlayer.prepareAsync();
            } catch (IOException e) {
                Log.e(TAG, "Error occurred while preparing media player");
            }
        }
        return START_STICKY;
    }

    @Override public IBinder onBind(Intent intent) {
        return mBinder;
    }

    @Override public void onPrepared(MediaPlayer mediaPlayer) {
        mediaPlayer.start();
    }

    @Override public void onSeekComplete(MediaPlayer mediaPlayer) {
    }

    @Override public void onCompletion(MediaPlayer mediaPlayer) {
        mediaPlayer.reset();
    }

    @Override public boolean onError(MediaPlayer mediaPlayer, int i, int i1) {
        mediaPlayer.reset();
        return false;
    }

    @Override public boolean isPlaying() {
        return mMediaPlayer.isPlaying();
    }

    @Override public void play() {
        mMediaPlayer.start();
    }

    @Override public void pause() {
        mMediaPlayer.pause();
    }

    @Override public void seekTo(int position) {
        mMediaPlayer.seekTo(position);
    }

    @Override public void skipToNext(String url) {
        mUrl = url;
        mMediaPlayer.stop();
        mMediaPlayer.reset();
        try {
            mMediaPlayer.setDataSource(mUrl);
            mMediaPlayer.prepareAsync();
        } catch (IOException e) {
            Log.e(TAG, "Error occurred while updating data source for next track");
        }
    }

    @Override public void skipToPrev(String url) {
        mUrl = url;
        mMediaPlayer.stop();
        mMediaPlayer.reset();
        try {
            mMediaPlayer.setDataSource(mUrl);
            mMediaPlayer.prepareAsync();
        } catch (IOException e) {
            Log.e(TAG, "Error occurred while updating data source for prev track");
        }
    }

    @Override public void onDestroy() {
        if (mMediaPlayer != null) {
            mMediaPlayer.release();
        }
    }

    private void initMediaPlayer() {
        mMediaPlayer = new MediaPlayer();
        mMediaPlayer.setOnPreparedListener(this);
        mMediaPlayer.setOnSeekCompleteListener(this);
        mMediaPlayer.setOnCompletionListener(this);
        mMediaPlayer.setOnErrorListener(this);
    }

    public class MusicBinder extends Binder {
        public MusicService getService() {
            return MusicService.this;
        }
    }
}
