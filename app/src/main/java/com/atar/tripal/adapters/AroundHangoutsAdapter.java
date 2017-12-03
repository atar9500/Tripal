package com.atar.tripal.adapters;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import com.atar.tripal.R;
import com.atar.tripal.callbacks.AroundCallback;
import com.atar.tripal.objects.User;
import com.atar.tripal.objects.Hangout;

import java.util.ArrayList;
import java.util.List;

public class AroundHangoutsAdapter extends RecyclerView.Adapter<AroundHangoutsAdapter.AroundHangoutViewHolder> {

    private List<Hangout> mHangouts;
    private AroundCallback mCallback;
    private Context mContext;

    public AroundHangoutsAdapter(AroundCallback callback, Context context, List<Hangout> hangouts){
        mHangouts = hangouts;
        mCallback = callback;
        mContext = context;
    }

    @Override
    public AroundHangoutViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        return new AroundHangoutViewHolder(LayoutInflater.from(parent.getContext())
                .inflate(R.layout.around_hangout, parent, false), mCallback);
    }

    @Override
    public void onBindViewHolder(AroundHangoutViewHolder holder, int position) {
        Hangout hangout = mHangouts.get(position);
        List<User> users = new ArrayList<>(hangout.getFriends());
        users.add(0, hangout.getHost());
        holder.mPalPic1.setVisibility(View.GONE);
        holder.mPalPic2.setVisibility(View.GONE);
        holder.mPalPic3.setVisibility(View.GONE);
        holder.mPalPic4.setVisibility(View.GONE);
        holder.mPalPic5.setVisibility(View.GONE);
        holder.mNames.setText("");
        for(int i = 0; i < users.size(); i++){
            switch (i){
                case 0:
                    holder.mPalPic1.setVisibility(View.VISIBLE);
                    break;
                case 1:
                    holder.mPalPic2.setVisibility(View.VISIBLE);
                    break;
                case 2:
                    holder.mPalPic3.setVisibility(View.VISIBLE);
                    break;
                case 3:
                    holder.mPalPic4.setVisibility(View.VISIBLE);
                    break;
                case 4:
                    holder.mPalPic5.setVisibility(View.VISIBLE);
                    break;
            }
            if(i < users.size() - 1){
                holder.mNames.append(users.get(i).getUsername() + ", ");
            } else if(users.size() == 1 || i == users.size() - 2) {
                holder.mNames.append(users.get(i).getUsername());
            } else {
                holder.mNames.append(mContext.getResources().getText(R.string.and));
                holder.mNames.append(" " + users.get(i).getUsername());
            }
        }
        String s = mContext.getString(R.string.going_to) + " " + hangout.getTheme();
        holder.mTheme.setText(s);
        if(hangout.getRequestSent()){
            holder.mJoin.setText(R.string.request_sent);
            holder.mJoin.setEnabled(false);
        } else {
            holder.mJoin.setText(R.string.join);
            holder.mJoin.setEnabled(true);
        }
    }

    @Override
    public int getItemCount() {
        return mHangouts.size();
    }

    static class AroundHangoutViewHolder extends RecyclerView.ViewHolder{

        private TextView mTheme, mNames;
        private Button mJoin;
        private ImageView mPalPic1, mPalPic2, mPalPic3, mPalPic4, mPalPic5;

        private AroundCallback mCallback;

        AroundHangoutViewHolder(View itemView, AroundCallback callback) {
            super(itemView);

            mCallback = callback;

            mTheme = itemView.findViewById(R.id.around_vh_theme);
            mNames = itemView.findViewById(R.id.around_vh_names);
            mJoin = itemView.findViewById(R.id.around_vh_join);
            mJoin.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    mCallback.onHangoutJoin(getAdapterPosition());
                }
            });
            mPalPic1 = itemView.findViewById(R.id.around_vh_pic1);
            mPalPic2 = itemView.findViewById(R.id.around_vh_pic2);
            mPalPic3 = itemView.findViewById(R.id.around_vh_pic3);
            mPalPic4 = itemView.findViewById(R.id.around_vh_pic4);
            mPalPic5 = itemView.findViewById(R.id.around_vh_pic5);
        }
    }

}
