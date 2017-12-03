package com.atar.tripal.adapters;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.atar.tripal.R;
import com.atar.tripal.objects.Message;

import java.util.Calendar;
import java.util.List;

public class ChatAdapter extends RecyclerView.Adapter<ChatAdapter.ChatViewHolder> {

    private List<Message> mMessages;
    private String mProfileId;
    private String mProfileName;
    private Context mContext;

    public ChatAdapter(String profileId, String profileName, List<Message> messages, Context context){
        mMessages = messages;
        mProfileId = profileId;
        mProfileName = profileName;
        mContext = context;
    }

    @Override
    public ChatViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view;
        boolean isSent = false;
        switch(viewType){
            case 0:
                view = LayoutInflater.from(parent.getContext()).inflate(
                        R.layout.chat_income, parent, false);
                break;
            case 1:
                view = LayoutInflater.from(parent.getContext()).inflate(
                        R.layout.chat_outcome, parent, false);
                isSent = true;
                break;
            default:
                view = LayoutInflater.from(parent.getContext()).inflate(
                        R.layout.info_message, parent, false);
                break;
        }
        return new ChatViewHolder(view, isSent);
    }

    @Override
    public void onBindViewHolder(ChatViewHolder holder, int position) {
        Message message = mMessages.get(position);

        switch(message.getType()){
            case Message.TYPE_MESSAGE:
                holder.mBody.setText(message.getContent());
                holder.mTime.setText(message.getTime());

                if(message.getStatus() == Message.STATUS_SENDING){
                    holder.mBody.setCompoundDrawablesWithIntrinsicBounds(
                            0, 0, R.drawable.sending_outcome, 0);
                } else if(message.getStatus() == Message.STATUS_SENT){
                    holder.mBody.setCompoundDrawablesWithIntrinsicBounds(0, 0, 0, 0);
                }

                if(position == mMessages.size() - 1 || message.getTimestamp() -
                        mMessages.get(position + 1).getTimestamp() > (1000*60*10)){
                    String time;
                    if(getCurrentDate().equals(message.getDate(true))){
                        time = "Today " + message.getTime();
                    } else if(getCurrentYear().equals(message.getYear())){
                        time = message.getDate(false) + " " + message.getTime();
                    } else {
                        time = message.getDate(true) + " " + message.getTime();
                    }
                    holder.mTime.setText(time);
                    holder.mTime.setVisibility(View.VISIBLE);
                } else {
                    holder.mTime.setVisibility(View.GONE);
                }

                if(!holder.mIsSent){

                    holder.mName.setText(message.getSenderName());
                    holder.mName.setVisibility(View.GONE);
                    if(position == mMessages.size() - 1){
                        holder.mName.setVisibility(View.VISIBLE);
                    } else {
                        Message message1 = mMessages.get(position + 1);
                        if(!message1.getSenderId().equals(message.getSenderId()) ||
                                message1.getType() != Message.TYPE_MESSAGE){
                            holder.mName.setVisibility(View.VISIBLE);
                        }
                    }

                    holder.mImage.setVisibility(View.INVISIBLE);
                    if(position == 0){
                        holder.mImage.setVisibility(View.VISIBLE);
                    } else {
                        Message message1 = mMessages.get(position - 1);
                        if(message1.getType() != Message.TYPE_MESSAGE ||
                                !message1.getSenderId().equals(message.getSenderId())){
                            holder.mImage.setVisibility(View.VISIBLE);
                        }
                    }
                }
                break;
            case Message.TYPE_JOINED:
                String time;
                if(getCurrentDate().equals(message.getDate(true))){
                    time = "Today " + message.getTime();
                } else if(getCurrentYear().equals(message.getYear())){
                    time = message.getDate(false) + " " + message.getTime();
                } else {
                    time = message.getDate(true) + " " + message.getTime();
                }
                holder.mTime.setText(time);
                holder.mTime.setVisibility(View.VISIBLE);
                if(message.getSenderName().equals(mProfileName)){
                    holder.mInfo.setText(R.string.you_joined);
                } else {
                    String s = message.getSenderName() + " " + mContext.getString(R.string.joined);
                    holder.mInfo.setText(s);
                }
                break;
            case Message.TYPE_LEFT:
                String t;
                if(getCurrentDate().equals(message.getDate(true))){
                    t = "Today " + message.getTime();
                } else if(getCurrentYear().equals(message.getYear())){
                    t = message.getDate(false) + " " + message.getTime();
                } else {
                    t = message.getDate(true) + " " + message.getTime();
                }
                holder.mTime.setText(t);
                holder.mTime.setVisibility(View.VISIBLE);
                String s = message.getSenderName() + " " + mContext.getString(R.string.left_the_hangout);
                holder.mInfo.setText(s);
                break;
            case Message.TYPE_PLACE:
                String j;
                if(getCurrentDate().equals(message.getDate(true))){
                    j = "Today " + message.getTime();
                } else if(getCurrentYear().equals(message.getYear())){
                    j = message.getDate(false) + " " + message.getTime();
                } else {
                    j = message.getDate(true) + " " + message.getTime();
                }
                holder.mTime.setText(j);
                holder.mTime.setVisibility(View.VISIBLE);
                if(message.getSenderName().equals(mProfileName)){
                    String k = mContext.getString(R.string.you_changed_the_place_to) + " " + message.getContent();
                    holder.mInfo.setText(k);
                } else {
                    String k = message.getSenderName() + " " + mContext.getString(R.string.changed_the_place_to) +
                            " " + message.getContent();
                    holder.mInfo.setText(k);
                }
                break;
        }
    }

    @Override
    public int getItemCount() {
        return mMessages.size();
    }

    @Override
    public int getItemViewType(int position) {
        Message message = mMessages.get(position);
        if(message.getType() == Message.TYPE_MESSAGE){
            if(message.getSenderId().equals(mProfileId)){
                return 1;
            } else {
                return 0;
            }
        } else {
            return 2;
        }
    }

    public int getPositionById(long messageId){
        int position = 0;
        while(position < mMessages.size() && mMessages.get(position).getId() != messageId){
            position++;
        }
        return position;
    }

    private String getCurrentDate(){
        Calendar now = Calendar.getInstance();
        String month = (now.get(Calendar.MONTH) + 1) + "";
        if(month.length() == 1){
            month = '0' + month;
        }
        String day = now.get(Calendar.DAY_OF_MONTH) + "";
        if(day.length() == 1){
            day = '0' + day;
        }
        String year = now.get(Calendar.YEAR) + "";
        return day + "/" + month + "/" + year;
    }

    private String getCurrentYear(){
        Calendar now = Calendar.getInstance();
        return now.get(Calendar.YEAR) + "";
    }

    public static class ChatViewHolder extends RecyclerView.ViewHolder{

        private TextView mBody, mTime, mInfo, mName;
        private ImageView mImage;
        private boolean mIsSent;

        public ChatViewHolder(View itemView, boolean isSent) {
            super(itemView);
            mIsSent = isSent;
            if(isSent){
                mBody = itemView.findViewById(R.id.outcome_text);
                mTime = itemView.findViewById(R.id.outcome_time);
            } else {
                mInfo = itemView.findViewById(R.id.info_text);
                if(mInfo == null){
                    mBody = itemView.findViewById(R.id.income_text);
                    mTime = itemView.findViewById(R.id.income_time);
                    mImage = itemView.findViewById(R.id.income_img);
                    mName = itemView.findViewById(R.id.income_name);
                } else {
                    mTime = itemView.findViewById(R.id.info_time);
                }
            }
        }
    }
}
