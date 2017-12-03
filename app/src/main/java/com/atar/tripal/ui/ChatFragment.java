package com.atar.tripal.ui;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.Fragment;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AnimationUtils;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.atar.tripal.R;
import com.atar.tripal.adapters.ChatAdapter;
import com.atar.tripal.callbacks.HangoutCallback;
import com.atar.tripal.db.DBConstants;
import com.atar.tripal.db.DBHandler;
import com.atar.tripal.db.Details;
import com.atar.tripal.net.GettingMessageService;
import com.atar.tripal.net.LocationDetectorService;
import com.atar.tripal.net.SendingMessageService;
import com.atar.tripal.objects.Hangout;
import com.atar.tripal.objects.Message;
import com.firebase.jobdispatcher.Constraint;
import com.firebase.jobdispatcher.FirebaseJobDispatcher;
import com.firebase.jobdispatcher.GooglePlayDriver;
import com.firebase.jobdispatcher.Job;
import com.firebase.jobdispatcher.Lifetime;
import com.firebase.jobdispatcher.RetryStrategy;

import java.util.ArrayList;
import java.util.List;

/**
 * A simple {@link Fragment} subclass.
 */
public class ChatFragment extends Fragment {

    private class ReceivingChatsBroadcastReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            long messageId = intent.getLongExtra
                    (DBConstants.COL_ID, -1);
            if(messageId > -1){
                Message message = mHandler.getMessage(messageId);
                if(message != null && message.getHangoutId() == mHangout.getId()
                        && message.getType() != Message.TYPE_JOIN){
                    mMessages.add(0, message);
                    mAdapter.notifyDataSetChanged();
                    showHideEmpty();
                } else if(message  == null) {
                    Toast.makeText(context, R.string.went_wrong, Toast.LENGTH_SHORT).show();
                }
            }
        }
    }

    private class SendingChatsBroadcastReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            long i = intent.getLongExtra(DBConstants.COL_ID, -1);
            if(i > -1){
                int position = mAdapter.getPositionById(i);
                mMessages.get(position).setStatus(Message.STATUS_SENT);
                mAdapter.notifyItemChanged(position);
            } else if(getContext() != null) {
                AlertDialog.Builder builder1 = new AlertDialog.Builder(getContext());
                builder1.setMessage(R.string.hangout_not_active);
                builder1.setNeutralButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        if(getActivity() != null){
                            getActivity().finish();
                        }
                    }
                });
            }
        }
    }

    /**
     * UI Widgets
     */
    private View mView;
    private ChatAdapter mAdapter;
    private RecyclerView mChats;
    private EditText mField;
    private FloatingActionButton mSend;
    private LinearLayout mEmpty;
    private HangoutCallback mCallback;
    private TextView mPlaceText, mPlaceAddress;

    private List<Message> mMessages;
    private Hangout mHangout;
    private DBHandler mHandler;
    private ReceivingChatsBroadcastReceiver mGettingReceiver;
    private SendingChatsBroadcastReceiver mSendingReceiver;

    public ChatFragment() {}

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        mView = inflater.inflate(R.layout.fragment_chat, container, false);

        mHandler = new DBHandler(getContext());

        initUIWidgets();

        return mView;
    }

    @Override
    public void onAttach(Context context) {
        mCallback = (HangoutCallback)context;
        super.onAttach(context);
    }

    @Override
    public void onStart() {
        if(getContext() != null){

            mGettingReceiver = new ReceivingChatsBroadcastReceiver();
            LocalBroadcastManager.getInstance(getContext()).registerReceiver
                    (mGettingReceiver, new IntentFilter(GettingMessageService.
                            BROADCAST_IDENTIFIER_FOR_SERVICE_FINISHED_RESPONSE));

            mSendingReceiver = new SendingChatsBroadcastReceiver();
            LocalBroadcastManager.getInstance(getContext()).registerReceiver
                    (mSendingReceiver, new IntentFilter(SendingMessageService.
                            BROADCAST_IDENTIFIER_FOR_SERVICE_FINISHED_RESPONSE));

            mHangout = mCallback.getHangout();
            mMessages = new ArrayList<>(mHandler.getAllMessagesOfHangout(mHangout.getId()));


            mAdapter = new ChatAdapter(Details.getProfileId(getContext()),
                    Details.getUsername(getContext()), mMessages, getContext());
            mChats.setAdapter(mAdapter);

            mPlaceText.setText(mHangout.getNameOfPlace());
            mPlaceAddress.setText(mHangout.getAddress());
        }

        initLayout();

        showHideEmpty();

        super.onStart();
    }

    @Override
    public void onStop() {
        if(getContext() != null){
            LocalBroadcastManager.getInstance(getContext()).unregisterReceiver(mGettingReceiver);
            LocalBroadcastManager.getInstance(getContext()).unregisterReceiver(mSendingReceiver);
        }
        super.onStop();
    }

    private void initUIWidgets() {

        // Initialling messages list.
        mChats = mView.findViewById(R.id.chat_list);
        LinearLayoutManager layoutManager = new LinearLayoutManager(getContext());
        layoutManager.setReverseLayout(true);
        mChats.setLayoutManager(layoutManager);
        mChats.setHasFixedSize(true);
        mEmpty = mView.findViewById(R.id.chat_empty);

        // Initialling sending messages field
        mField = mView.findViewById(R.id.chat_field);
        mSend = mView.findViewById(R.id.chat_send);
        mSend.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                FirebaseJobDispatcher dispatcher = new FirebaseJobDispatcher
                        (new GooglePlayDriver(getContext()));

                long id = saveMessage();
                mField.setText(null);

                Bundle bundle = new Bundle();
                bundle.putLong(DBConstants.COL_ID, id);

                Job myJob = dispatcher.newJobBuilder()
                        .setService(SendingMessageService.class)
                        .setTag(id + "")
                        .setRecurring(false)
                        .setLifetime(Lifetime.FOREVER)
                        .setReplaceCurrent(false)
                        .setConstraints(Constraint.ON_ANY_NETWORK)
                        .setRetryStrategy(RetryStrategy.DEFAULT_LINEAR)
                        .setExtras(bundle)
                        .build();

                dispatcher.mustSchedule(myJob);


            }
        });

        // Initialling the Place shower screen.
        LinearLayout placeScreen = mView.findViewById(R.id.chat_place_screen);
        placeScreen.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mCallback.onSetPlaceClick();
            }
        });
        mPlaceText = mView.findViewById(R.id.chat_place_name);
        mPlaceAddress = mView.findViewById(R.id.chat_place_address);
    }

    public void changePlace(){
        mHangout = mCallback.getHangout();
        mPlaceText.setText(mHangout.getNameOfPlace());
        mPlaceAddress.setText(mHangout.getAddress());
    }

    private long saveMessage(){
        Message message = new Message();
        message.setContent(mField.getText().toString());
        message.setSenderId(Details.getProfileId(getContext()));
        message.setHangoutId(mHangout.getId());
        message.setStatus(Message.STATUS_SENDING);
        message.setTimestamp(System.currentTimeMillis());
        message.setType(Message.TYPE_MESSAGE);
        message.setId(mHandler.addMessage(message));
        mMessages.add(0, message);
        mAdapter.notifyDataSetChanged();

        showHideEmpty();

        return message.getId();
    }

    private void showHideEmpty(){
        if(mAdapter.getItemCount() == 0 && !mEmpty.isShown()){
            mEmpty.startAnimation(AnimationUtils.loadAnimation(getContext(), R.anim.fade_in));
            mEmpty.setVisibility(View.VISIBLE);
        } else if(mAdapter.getItemCount() > 0 && mEmpty.isShown()){
            mEmpty.startAnimation(AnimationUtils.loadAnimation(getContext(), R.anim.fade_out));
            mEmpty.setVisibility(View.INVISIBLE);
        }
    }

    public void refresh(){
        if(mCallback != null){
            mHangout = mCallback.getHangout();
            if(mPlaceText != null && mPlaceAddress != null){
                mPlaceText.setText(mHangout.getNameOfPlace());
                mPlaceAddress.setText(mHangout.getAddress());
                initLayout();
            }
        }
    }

    private void initLayout(){
        if(!mHangout.getIsActive()){
            mSend.hide();
            mSend.setClickable(false);
            mField.setFocusable(false);
            mField.setClickable(false);
            mField.setFocusableInTouchMode(false);
            mField.setCursorVisible(false);
            mField.setHint("This hangout has ended.");
        } else if(mHangout.getFriends().size() == 0){
            mSend.hide();
            mSend.setClickable(false);
            mField.setFocusable(false);
            mField.setClickable(false);
            mField.setFocusableInTouchMode(false);
            mField.setCursorVisible(false);
            mField.setHint("No friends in this hangout yet.");
        } else {
            mSend.show();
            mSend.setClickable(true);
            mField.setFocusable(true);
            mField.setClickable(true);
            mField.setFocusableInTouchMode(true);
            mField.setCursorVisible(true);
            mField.setHint("Type a message...");
        }
    }

}
