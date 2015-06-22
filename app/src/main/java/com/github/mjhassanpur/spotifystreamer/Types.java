package com.github.mjhassanpur.spotifystreamer;

import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.List;

import kaaes.spotify.webapi.android.models.Artist;
import kaaes.spotify.webapi.android.models.Track;

public class Types {

    public static final Type ARTIST = new TypeToken<Artist>() {}.getType();
    public static final Type ARTIST_LIST = new TypeToken<List<Artist>>() {}.getType();
    public static final Type TRACK_LIST = new TypeToken<List<Track>>() {}.getType();
}
