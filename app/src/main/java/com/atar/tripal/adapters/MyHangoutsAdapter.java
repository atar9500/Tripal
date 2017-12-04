package com.atar.tripal.adapters;

import android.content.Context;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import com.atar.tripal.R;
import com.atar.tripal.callbacks.MyHangoutCallback;
import com.atar.tripal.objects.User;
import com.atar.tripal.objects.Hangout;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.squareup.picasso.Picasso;

import java.util.ArrayList;
import java.util.List;

public class MyHangoutsAdapter extends RecyclerView.Adapter<MyHangoutsAdapter.MyHangoutViewHolder> {

    private List<Hangout> mHangouts;
    private boolean mIsActive;
    private MyHangoutCallback mCallback;
    private Context mContext;

    public MyHangoutsAdapter(List<Hangout> hangouts, boolean isActive,
            MyHangoutCallback callback, Context context){
        mHangouts = hangouts;
        mIsActive = isActive;
        mCallback = callback;
        mContext = context;
    }

    @Override
    public MyHangoutViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        return new MyHangoutViewHolder(LayoutInflater.from(parent.getContext())
                .inflate(R.layout.my_hangout, parent, false), mIsActive, mCallback);
    }

    @Override
    public void onBindViewHolder(MyHangoutViewHolder holder, int position) {
        Hangout hangout = mHangouts.get(position);
        List<User> users = new ArrayList<>(hangout.getFriends());
        users.add(0, hangout.getHost());
        switch(users.size()){
            case 1:
                holder.mPalPic2.setVisibility(View.GONE);
                holder.mPalPic3.setVisibility(View.GONE);
                holder.mPalPic4.setVisibility(View.GONE);
                holder.mPalPic5.setVisibility(View.GONE);
                break;
            case 2:
                holder.mPalPic3.setVisibility(View.GONE);
                holder.mPalPic4.setVisibility(View.GONE);
                holder.mPalPic5.setVisibility(View.GONE);
                break;
            case 3:
                holder.mPalPic4.setVisibility(View.GONE);
                holder.mPalPic5.setVisibility(View.GONE);
                break;
            case 4:
                holder.mPalPic5.setVisibility(View.GONE);
                break;
        }
        holder.mNames.setText("");
        for(int i = 0; i < users.size(); i++){
            User user = users.get(i);
            switch (i){
                case 0:
                    getProfilePhoto(user.getId(), holder.mPalPic1);
                    holder.mPalPic1.setVisibility(View.VISIBLE);
                    break;
                case 1:
                    getProfilePhoto(user.getId(), holder.mPalPic2);
                    holder.mPalPic2.setVisibility(View.VISIBLE);
                    break;
                case 2:
                    getProfilePhoto(user.getId(), holder.mPalPic3);
                    holder.mPalPic3.setVisibility(View.VISIBLE);
                    break;
                case 3:
                    getProfilePhoto(user.getId(), holder.mPalPic4);
                    holder.mPalPic4.setVisibility(View.VISIBLE);
                    break;
                case 4:
                    getProfilePhoto(user.getId(), holder.mPalPic5);
                    holder.mPalPic5.setVisibility(View.VISIBLE);
                    break;
            }
            if(i < users.size() - 2){
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

        if(!holder.mIsActive){
            holder.mLeave.setTextColor(ContextCompat.getColor(mContext, R.color.text_income));
        } else {
            holder.mLeave.setTextColor(ContextCompat.getColor(mContext, R.color.registerColor));
        }

    }

    private void getProfilePhoto(String userId, final ImageView profilePic){
        StorageReference storageRef = FirebaseStorage.getInstance().getReference();
        StorageReference profilePhotosRef = storageRef.child("users/" + userId + "/profilePicture.jpg");
        profilePhotosRef.getDownloadUrl().addOnSuccessListener(new OnSuccessListener<Uri>() {
            @Override
            public void onSuccess(Uri uri) {
                Picasso.with(mContext)
                        .load(uri)
                        .into(profilePic);
            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                e.printStackTrace();
            }
        });
    }

    @Override
    public int getItemCount() {
        return mHangouts.size();
    }

    static class MyHangoutViewHolder extends RecyclerView.ViewHolder{

        private TextView mTheme, mNames;
        private Button mLeave;
        private ImageView mPalPic1, mPalPic2, mPalPic3, mPalPic4, mPalPic5;

        private boolean mIsActive;

        private MyHangoutCallback mCallback;

        MyHangoutViewHolder(View itemView, boolean isActive, MyHangoutCallback callback) {
            super(itemView);

            mIsActive = isActive;

            mCallback = callback;

            mTheme = itemView.findViewById(R.id.my_vh_theme);
            mNames = itemView.findViewById(R.id.my_vh_names);
            mLeave = itemView.findViewById(R.id.my_vh_leave);
            if(!isActive){
                mLeave.setEnabled(false);
                mLeave.setText(R.string.hangout_ended);
            } else {
                mLeave.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        mCallback.onHangoutLeave(getAdapterPosition());
                    }
                });
            }
            mPalPic1 = itemView.findViewById(R.id.my_vh_pic1);
            mPalPic2 = itemView.findViewById(R.id.my_vh_pic2);
            mPalPic3 = itemView.findViewById(R.id.my_vh_pic3);
            mPalPic4 = itemView.findViewById(R.id.my_vh_pic4);
            mPalPic5 = itemView.findViewById(R.id.my_vh_pic5);

            itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    mCallback.onHangoutClick(getAdapterPosition(), mIsActive);
                }
            });
        }
    }

}
