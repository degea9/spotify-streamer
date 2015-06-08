package com.github.mjhassanpur.spotifystreamer.adapters;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.github.mjhassanpur.spotifystreamer.R;
import com.squareup.picasso.Picasso;

import java.util.ArrayList;

import kaaes.spotify.webapi.android.models.Artist;


/**
 * A custom RecyclerView.Adapter for holding artists
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
        if (!artist.images.isEmpty()) {
            Picasso.with(mContext).load(artist.images.get(0).url).into(holder.mImageView);
        } else {
            // If an artist image was not found
            holder.mImageView.setImageResource(R.drawable.ic_launcher);
        }
    }

    @Override
    public int getItemCount() {
        return mArtists.size();
    }
}