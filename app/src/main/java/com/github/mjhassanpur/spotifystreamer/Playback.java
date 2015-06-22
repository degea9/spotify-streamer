package com.github.mjhassanpur.spotifystreamer;

public interface Playback {

    boolean isPlaying();
    void play();
    void pause();
    void seekTo(int position);
}
