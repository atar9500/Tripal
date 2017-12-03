package com.atar.tripal.adapters;

import android.support.design.widget.FloatingActionButton;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.atar.tripal.R;
import com.atar.tripal.callbacks.SearchResultsCallback;
import com.atar.tripal.net.NetConstants;

public class NearbyPlacesAdapter extends RecyclerView.Adapter<NearbyPlacesAdapter.NearbySuggestionHolder> {

    private int[] mSuggestions = {R.string.bars, R.string.cafes, R.string.libraries, R.string.stores,
            R.string.restaurants, R.string.night_clubs, R.string.parks, R.string.shopping_malls};
    private SearchResultsCallback mCallback;

    public NearbyPlacesAdapter(SearchResultsCallback callback){
        mCallback = callback;
    }

    @Override
    public NearbySuggestionHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        return new NearbySuggestionHolder(LayoutInflater.from(parent.getContext())
                .inflate(R.layout.nearby_card, parent, false));
    }

    @Override
    public void onBindViewHolder(NearbySuggestionHolder holder, int position) {
        holder.mLabel.setText(mSuggestions[position]);
        final int i = mSuggestions[position];
        switch(i){
            case R.string.bars:
                holder.mBtn.setImageResource(R.mipmap.bar);
                break;
            case R.string.cafes:
                holder.mBtn.setImageResource(R.mipmap.cafe);
                break;
            case R.string.libraries:
                holder.mBtn.setImageResource(R.mipmap.library);
                break;
            case R.string.stores:
                holder.mBtn.setImageResource(R.mipmap.store);
                break;
            case R.string.restaurants:
                holder.mBtn.setImageResource(R.mipmap.restaurant);
                break;
            case R.string.night_clubs:
                holder.mBtn.setImageResource(R.mipmap.nightclub);
                break;
            case R.string.parks:
                holder.mBtn.setImageResource(R.mipmap.park);
                break;
            case R.string.shopping_malls:
                holder.mBtn.setImageResource(R.mipmap.shopping);
                break;
        }
        holder.itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String s = null;
                switch(i){
                    case R.string.bars:
                        s = NetConstants.BARS;
                        break;
                    case R.string.cafes:
                        s = NetConstants.CAFES;
                        break;
                    case R.string.libraries:
                        s = NetConstants.LIBRARIES;
                        break;
                    case R.string.stores:
                        s = NetConstants.STORES;
                        break;
                    case R.string.restaurants:
                        s = NetConstants.RESTAURANTS;
                        break;
                    case R.string.night_clubs:
                        s = NetConstants.NIGHTCLUBS;
                        break;
                    case R.string.parks:
                        s = NetConstants.PARKS;
                        break;
                    case R.string.shopping_malls:
                        s = NetConstants.SHOPPING_MALLS;
                        break;
                }
                mCallback.findNearby(s);
            }
        });
    }

    @Override
    public int getItemCount() {
        return mSuggestions.length;
    }

    public static class NearbySuggestionHolder extends RecyclerView.ViewHolder{

        private FloatingActionButton mBtn;
        private TextView mLabel;

        public NearbySuggestionHolder(View itemView) {
            super(itemView);
            mLabel = itemView.findViewById(R.id.map_nearby_label);
            mBtn = itemView.findViewById(R.id.map_nearby_button);
        }
    }
}
