package com.atar.tripal.adapters;

import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.atar.tripal.R;
import com.atar.tripal.objects.User;

import java.util.List;

public class FriendsAdapter extends RecyclerView.Adapter<FriendsAdapter.FriendViewHolder> {

    private List<User> mUsers;

    public FriendsAdapter(List<User> users){
        mUsers = users;
    }

    @Override
    public FriendViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        return new FriendViewHolder(LayoutInflater.from(parent.getContext())
                .inflate(R.layout.friend, parent, false));
    }

    @Override
    public void onBindViewHolder(FriendViewHolder holder, int position) {
        holder.username.setText(mUsers.get(position).getUsername());
    }

    @Override
    public int getItemCount() {
        return mUsers.size();
    }

    public static class FriendViewHolder extends RecyclerView.ViewHolder{

        private ImageView img;
        private TextView username;

        public FriendViewHolder(View itemView) {
            super(itemView);
            img = itemView.findViewById(R.id.friend_vh_image);
            username = itemView.findViewById(R.id.friend_vh_username);
        }
    }

}
