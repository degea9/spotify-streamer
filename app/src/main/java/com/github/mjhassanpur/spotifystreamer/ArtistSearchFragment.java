package com.github.mjhassanpur.spotifystreamer;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.TextView;

import java.util.ArrayList;


public class ArtistSearchFragment extends Fragment {

    private RecyclerView mRecyclerView;
    private RecyclerView.Adapter mAdapter;
    private RecyclerView.LayoutManager mLayoutManager;
    private EditText mSearchBox;

    public ArtistSearchFragment() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_artist_search, container, false);
        mSearchBox = (EditText) rootView.findViewById(R.id.artist_search_box);
        mSearchBox.setOnEditorActionListener(new EditText.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView textView, int i, KeyEvent keyEvent) {
                if (i == EditorInfo.IME_ACTION_DONE) {
                    mRecyclerView.setVisibility(View.VISIBLE);
                }
                return false;
            }
        });
        mRecyclerView = (RecyclerView) rootView.findViewById(R.id.artist_recycler_view);
        if (savedInstanceState == null) {
            mRecyclerView.setVisibility(View.GONE);
        }
        return rootView;
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        mLayoutManager = new LinearLayoutManager(getActivity());
        mRecyclerView.setLayoutManager(mLayoutManager);
        ArrayList<String> dummyData = new ArrayList<>();
        for (int i = 0; i < 30; i++) {
            dummyData.add("Item " + i);
        }
        mAdapter = new ArtistAdapter(dummyData);
        mRecyclerView.setAdapter(mAdapter);
    }

    public class ArtistAdapter extends RecyclerView.Adapter<ArtistAdapter.ViewHolder> {

        private ArrayList<String> mDataset;

        public class ViewHolder extends RecyclerView.ViewHolder {
            public TextView mTextView;
            public ViewHolder(TextView v) {
                super(v);
                mTextView = v;
            }
        }

        public ArtistAdapter(ArrayList<String> dataset) {
            mDataset = dataset;
        }

        @Override
        public ArtistAdapter.ViewHolder onCreateViewHolder(ViewGroup parent,
                                                       int viewType) {
            View v = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.list_item_artist, parent, false);
            TextView tv = (TextView) v.findViewById(R.id.list_item_artist_text_view);
            ViewHolder vh = new ViewHolder(tv);
            return vh;
        }

        @Override
        public void onBindViewHolder(ViewHolder holder, int position) {
            holder.mTextView.setText(mDataset.get(position));
        }

        @Override
        public int getItemCount() {
            return mDataset.size();
        }
    }
}
