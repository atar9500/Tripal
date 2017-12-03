package com.atar.tripal.adapters;

import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.atar.tripal.R;
import com.atar.tripal.callbacks.SuggestionsCallback;
import com.atar.tripal.objects.PlacePrediction;
import com.google.android.gms.location.places.Place;

import java.util.ArrayList;
import java.util.List;

public class PlacesPredictionsAdapter extends RecyclerView.Adapter
        <PlacesPredictionsAdapter.PlacePredictionHolder> {

    private List<PlacePrediction> mPlacePredictions;
    private SuggestionsCallback mCallback;

    public PlacesPredictionsAdapter(SuggestionsCallback callback){
        mPlacePredictions = new ArrayList<>();
        mCallback = callback;
    }

    @Override
    public PlacePredictionHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        return new PlacePredictionHolder(LayoutInflater.from(parent.getContext())
                .inflate(R.layout.place_prediction, parent, false));
    }

    @Override
    public void onBindViewHolder(PlacePredictionHolder holder, int position) {
        final PlacePrediction prediction = mPlacePredictions.get(position);
        holder.mSec.setText(prediction.getSecondaryText());
        holder.mPrime.setText(prediction.getPrimaryText());
        if(prediction.getTypes().contains(Place.TYPE_CAFE)){
            holder.mImage.setImageResource(R.drawable.cafe);
        } else if(prediction.getTypes().contains(Place.TYPE_BAR)
                || prediction.getTypes().contains(Place.TYPE_LIQUOR_STORE)
                || prediction.getTypes().contains(Place.TYPE_NIGHT_CLUB)){
            holder.mImage.setImageResource(R.drawable.bar);
        } else if(prediction.getTypes().contains(Place.TYPE_MEAL_TAKEAWAY)){
            holder.mImage.setImageResource(R.drawable.food);
        } else if(prediction.getTypes().contains(Place.TYPE_BANK)
                || prediction.getTypes().contains(Place.TYPE_CITY_HALL)
                || prediction.getTypes().contains(Place.TYPE_LIBRARY)){
            holder.mImage.setImageResource(R.drawable.bank);
        } else if(prediction.getTypes().contains(Place.TYPE_SHOPPING_MALL)){
            holder.mImage.setImageResource(R.drawable.shopping);
        } else if(prediction.getTypes().contains(Place.TYPE_STORE)){
            holder.mImage.setImageResource(R.drawable.store);
        } else {
            holder.mImage.setImageResource(R.drawable.place_marker);
        }
        holder.itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mCallback.onSuggestionClick(prediction.getId());
            }
        });
    }

    @Override
    public int getItemCount() {
        return mPlacePredictions.size();
    }

    public void setPlacePredictions(List<PlacePrediction> placePredictions){
        int previous = mPlacePredictions.size();
        mPlacePredictions.clear();
        notifyItemRangeRemoved(0, previous);
        if(placePredictions != null){
            mPlacePredictions.addAll(placePredictions);
            notifyItemRangeInserted(0, placePredictions.size());
        }
    }

    public static class PlacePredictionHolder extends RecyclerView.ViewHolder{

        private ImageView mImage;
        private TextView mPrime, mSec;

        public PlacePredictionHolder(View itemView) {
            super(itemView);
            mImage = itemView.findViewById(R.id.pred_image);
            mPrime = itemView.findViewById(R.id.pred_primary_text);
            mSec = itemView.findViewById(R.id.pred_secondary_text);
        }
    }
}
