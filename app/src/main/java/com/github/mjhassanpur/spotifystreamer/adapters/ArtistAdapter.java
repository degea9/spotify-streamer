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

import kaaes.spotify.webapi.android.models.Artist;
import kaaes.spotify.webapi.android.models.Image;

/**
 * A custom RecyclerView adapter for holding artists
 *
 * @see <a href="https://developer.android.com/training/material/lists-cards.html">Creating Lists and Cards</a>
 */
public class ArtistAdapter extends RecyclerView.Adapter<ArtistAdapter.ViewHolder> {

    private Context mContext;
    private ArrayList<Artist> mArtists;

    public class ViewHolder extends RecyclerView.ViewHolder {
        public ImageView mImageView;
        public TextView mTextView;
        public ViewHolder(View v) {
            super(v);
            mImageView = (ImageView) v.findViewById(R.id.list_item_artist_image);
            mTextView = (TextView) v.findViewById(R.id.list_item_artist_name);
        }
    }

    public ArtistAdapter(ArrayList<Artist> artistList) {
        mArtists = artistList;
    }

    @Override
    public ArtistAdapter.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        mContext = parent.getContext();
        View v = LayoutInflater.from(mContext)
                .inflate(R.layout.list_item_artist, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        Artist artist = mArtists.get(position);
        holder.mTextView.setText(artist.name);
        String url = null;
        List<Image> images = artist.images;
        if (images != null && !images.isEmpty()) {
            url = images.get(0).url;
        }
        Glide.with(mContext)
                .load(url)
                .error(R.drawable.default_profile_image)
                .into(holder.mImageView);
    }

    @Override
    public int getItemCount() {
        return mArtists.size();
    }
}
