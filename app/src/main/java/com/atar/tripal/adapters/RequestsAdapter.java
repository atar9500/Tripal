package com.atar.tripal.adapters;

import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.atar.tripal.R;
import com.atar.tripal.callbacks.RequestsCallback;
import com.atar.tripal.objects.Message;

import java.util.List;

public class RequestsAdapter extends RecyclerView.Adapter<RequestsAdapter.RequestViewHolder> {

    private List<Message> mMessages;
    private RequestsCallback mCallback;

    public RequestsAdapter(List<Message> messages, RequestsCallback callback){
        mMessages = messages;
        mCallback = callback;
    }

    @Override
    public RequestViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        return new RequestViewHolder(LayoutInflater.from(parent.getContext())
                .inflate(R.layout.request_to_join, parent, false), mCallback);
    }

    @Override
    public void onBindViewHolder(RequestViewHolder holder, int position) {
        holder.username.setText(mMessages.get(position).getSenderName());
    }

    @Override
    public int getItemCount() {
        return mMessages.size();
    }

    public static class RequestViewHolder extends RecyclerView.ViewHolder{

        private ImageView img, add, delete;
        private TextView username;

        private RequestsCallback mCallback;

        public RequestViewHolder(View itemView, RequestsCallback callback) {
            super(itemView);

            mCallback = callback;

            img = itemView.findViewById(R.id.request_vh_image);
            add = itemView.findViewById(R.id.request_vh_add);
            add.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    mCallback.onRequestAccepted(getAdapterPosition());
                }
            });
            delete = itemView.findViewById(R.id.request_vh_delete);
            delete.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    mCallback.onRequestDenied(getAdapterPosition());
                }
            });
            username = itemView.findViewById(R.id.request_vh_username);
        }
    }
}
