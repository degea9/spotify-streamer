package com.github.mjhassanpur.spotifystreamer.adapters;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.github.mjhassanpur.spotifystreamer.R;

import java.util.ArrayList;
import java.util.List;

import kaaes.spotify.webapi.android.models.Image;
import kaaes.spotify.webapi.android.models.Track;

/**
 * A custom RecyclerView adapter for holding tracks
 *
 * @see <a href="https://developer.android.com/training/material/lists-cards.html">Creating Lists and Cards</a>
 */
public class TrackAdapter extends RecyclerView.Adapter<TrackAdapter.ViewHolder> {

    private Context mContext;
    private ArrayList<Track> mTracks;

    public class ViewHolder extends RecyclerView.ViewHolder {
        public ImageView mAlbumImageView;
        public TextView mTrackTextView;
        public TextView mAlbumTextView;
        public ViewHolder(View v) {
            super(v);
            mAlbumImageView = (ImageView) v.findViewById(R.id.list_item_album_image);
            mTrackTextView = (TextView) v.findViewById(R.id.list_item_track_name);
            mAlbumTextView = (TextView) v.findViewById(R.id.list_item_album_name);
        }
    }

    public TrackAdapter(ArrayList<Track> trackList) {
        mTracks = trackList;
    }

    @Override
    public TrackAdapter.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        mContext = parent.getContext();
        View v = LayoutInflater.from(mContext)
                .inflate(R.layout.list_item_track, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        Track track = mTracks.get(position);
        holder.mTrackTextView.setText(track.name);
        holder.mAlbumTextView.setText(track.album.name);
        String url = null;
        List<Image> images = track.album.images;
        if (images != null && !images.isEmpty()) {
            url = images.get(0).url;
        }
        Glide.with(mContext)
                .load(url)
                .error(R.drawable.default_album_image)
                .into(holder.mAlbumImageView);
    }

    @Override
    public int getItemCount() {
        return mTracks.size();
    }
}
