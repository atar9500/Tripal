package com.atar.tripal.ui;


import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Configuration;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.StaggeredGridLayoutManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.TextView;
import android.widget.Toast;

import com.atar.tripal.R;
import com.atar.tripal.adapters.AroundHangoutsAdapter;
import com.atar.tripal.callbacks.AroundCallback;
import com.atar.tripal.db.DBConstants;
import com.atar.tripal.db.DBHandler;
import com.atar.tripal.db.Details;
import com.atar.tripal.net.ApiClient;
import com.atar.tripal.net.ApiInterface;
import com.atar.tripal.net.GettingMessageService;
import com.atar.tripal.net.LocationDetectorService;
import com.atar.tripal.net.NetConstants;
import com.atar.tripal.objects.Hangouts;
import com.atar.tripal.objects.User;
import com.atar.tripal.objects.Hangout;
import com.atar.tripal.objects.Message;
import com.atar.tripal.objects.Result;
import com.google.android.gms.maps.model.LatLng;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;


/**
 * A simple {@link Fragment} subclass.
 */
public class HangoutsAroundFragment extends Fragment implements AroundCallback {

    private class LocationUpdateReceiver extends BroadcastReceiver{

        @SuppressWarnings("unchecked")
        @Override
        public void onReceive(Context context, Intent intent) {
            switch(intent.getIntExtra(LocationDetectorService.CODE, -1)){
                case LocationDetectorService.START:
                    showOrHideMessage();
                    mRefresh.setRefreshing(true);
                    break;
                case LocationDetectorService.END:
                    mHangouts.clear();
                    mAdapter.notifyDataSetChanged();
                    showOrHideMessage();
                    break;
                case LocationDetectorService.GET_AROUND_HANGOUTS:
                    getAroundHangouts();
                    break;
            }
        }
    }

    private class RequestsUpdateReceiver extends BroadcastReceiver{

        @Override
        public void onReceive(Context context, Intent intent) {
            int type = intent.getIntExtra(DBConstants.COL_TYPE, -1);
            String name = intent.getStringExtra(DBConstants.DB_NAME);
            final long hangoutId = intent.getLongExtra(DBConstants.COL_HANGOUT_ID, -1);
            if(type == Message.TYPE_JOINED && name.equals(Details.getUsername(getContext()))){
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        for(int i = 0; i < mHangouts.size(); i++){
                            if(mHangouts.get(i).getId() == hangoutId){
                                mHangouts.remove(i);
                                mAdapter.notifyItemRemoved(i);
                                showOrHideMessage();
                            }
                        }
                    }
                }).run();
            }
        }
    }

    private View mView;

    private RequestsUpdateReceiver mRequestsReceiver;
    private LocationUpdateReceiver mReceiver;
    private ApiInterface mInterface;

    /**
     * UI Widgets
     */
    private TextView mMsgMain, mMsgSub;
    private AroundHangoutsAdapter mAdapter;
    private SwipeRefreshLayout mRefresh;
    private RecyclerView mHangoutsList;

    /**
     * Data
     */
    private List<Hangout> mHangouts;
    private DBHandler mHandler;

    public HangoutsAroundFragment() {}

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        mView = inflater.inflate(R.layout.fragment_around, container, false);

        mInterface = ApiClient.getAPIClient().create(ApiInterface.class);

        if(mHangouts == null){
            mHangouts = new ArrayList<>();
        }

        setRetainInstance(true);

        mHandler = new DBHandler(getContext());

        initUIWidgets();

        showOrHideMessage();

        return mView;
    }

    @Override
    public void onStart() {
        if(getContext() != null){
            mReceiver = new LocationUpdateReceiver();
            LocalBroadcastManager.getInstance(getContext()).registerReceiver
                    (mReceiver, new IntentFilter(LocationDetectorService.BROADCAST_IDENTIFIER_FOR_SERVICE_FINISHED_RESPONSE));
            mRequestsReceiver = new RequestsUpdateReceiver();
            LocalBroadcastManager.getInstance(getContext()).registerReceiver
                    (mRequestsReceiver, new IntentFilter(GettingMessageService.BROADCAST_IDENTIFIER_FOR_SERVICE_FINISHED_RESPONSE));
        }
        super.onStart();
    }

    @Override
    public void onStop() {
        if(getContext() != null){
            LocalBroadcastManager.getInstance(getContext()).unregisterReceiver(mReceiver);
            LocalBroadcastManager.getInstance(getContext()).unregisterReceiver(mRequestsReceiver);
        }
        super.onStop();
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        adjustList();
    }

    @Override
    public void onHangoutJoin(final int position) {
        Hangout hangout = new Hangout();
        hangout.setId(mHangouts.get(position).getId());
        hangout.getFriends().add(new User(Details.getProfileId(getContext())));
        Call<Result> call = mInterface.sendRequestToJoin(hangout);
        call.enqueue(new Callback<Result>() {
            @Override
            public void onResponse(@NonNull Call<Result> call,
                    @NonNull retrofit2.Response<Result> response) {
                Result result = response.body();
                if(response.isSuccessful() && result != null){
                    switch (result.getResult()){
                        case NetConstants.RESULT_SUCCESS:
                            Message message = new Message();
                            message.setHangoutId(mHangouts.get(position).getId());
                            message.setType(Message.TYPE_JOIN);
                            message.setSenderId(Details.getProfileId(getContext()));
                            message.setStatus(Message.STATUS_SENT);
                            message.setHangoutTheme(mHangouts.get(position).getTheme());
                            message.setSenderName(Details.getUsername(getContext()));
                            mHandler.addMessage(message);
                            mHangouts.get(position).setRequestSent(true);
                            mAdapter.notifyItemChanged(position);
                            break;
                        case NetConstants.RESULT_FULL:
                            Toast.makeText(getContext(), R.string.hangout_full,
                                    Toast.LENGTH_SHORT).show();
                            break;
                        case NetConstants.RESULT_NOT_ACTIVE:
                            Toast.makeText(getContext(), R.string.hangout_not_active,
                                    Toast.LENGTH_SHORT).show();
                            mHangouts.remove(position);
                            mAdapter.notifyItemRemoved(position);
                            break;
                        case NetConstants.RESULT_ALREADY_THERE:
                            Toast.makeText(getContext(), R.string.already_in_hangout,
                                    Toast.LENGTH_SHORT).show();
                            mHangouts.remove(position);
                            mAdapter.notifyItemRemoved(position);
                            break;
                    }
                } else {
                    Toast.makeText(getContext(), R.string.went_wrong, Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(@NonNull Call<Result> call, @NonNull Throwable t) {
                Toast.makeText(getContext(), R.string.no_connection, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void initUIWidgets(){

        // Screens
        mMsgMain = mView.findViewById(R.id.ar_msg_title);
        mMsgSub = mView.findViewById(R.id.ar_msg_sub);

        // RecyclerView that fits the hangouts
        mHangoutsList = mView.findViewById(R.id.ar_list);
        adjustList();
        mAdapter = new AroundHangoutsAdapter(this, getContext(), mHangouts);
        mHangoutsList.setAdapter(mAdapter);
        mHangoutsList.setHasFixedSize(true);

        // Swipe to Refresh
        mRefresh = mView.findViewById(R.id.ar_refresh);
        int [] colors = new int[]{R.color.colorAccent, R.color.colorPrimary};
        mRefresh.setColorSchemeResources(colors);
        mRefresh.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                getAroundHangouts();
            }
        });

    }

    public void showOrHideMessage(){
        Animation fadeIn = AnimationUtils.loadAnimation(getContext(), android.R.anim.fade_in);

        // If the user is offline, the swipe refresh layout will be hidden and an appropriate
        // message will appear
        if(!Details.getStatus(getContext())) {
            if(mRefresh.isShown()){
                mRefresh.startAnimation(fadeIn);
                mRefresh.setVisibility(View.GONE);
            }

            mMsgMain.startAnimation(fadeIn);
            mMsgMain.setVisibility(View.VISIBLE);
            mMsgMain.setText(R.string.you_are_offline);
            mMsgSub.startAnimation(fadeIn);
            mMsgSub.setVisibility(View.VISIBLE);
            mMsgSub.setText(R.string.go_online);

        // If the user is online and no hangouts are around him, swipe refresh layout will be shown with an appropriate message
        } else if(mAdapter.getItemCount() == 0){

            mMsgMain.startAnimation(fadeIn);
            mMsgMain.setVisibility(View.VISIBLE);
            mMsgMain.setText(R.string.no_hangouts_around);
            mMsgSub.startAnimation(fadeIn);
            mMsgSub.setVisibility(View.VISIBLE);
            mMsgSub.setText(R.string.setup_hangout);

            if(!mRefresh.isShown()){
                mRefresh.startAnimation(fadeIn);
                mRefresh.setVisibility(View.VISIBLE);
            }

        // If the user is online and there are hangouts around him, swipe to refresh layout will be shown and messages will disappear;
        } else {
            mMsgMain.setVisibility(View.INVISIBLE);
            mMsgSub.setVisibility(View.INVISIBLE);
            if(!mRefresh.isShown()){
                mRefresh.startAnimation(fadeIn);
                mRefresh.setVisibility(View.VISIBLE);
            }
        }
        mRefresh.setRefreshing(false);
    }

    private void adjustList(){
        if(mHangoutsList != null){
            boolean isPhone = mView.findViewById(R.id.ar_view) != null;
            if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT) {
                if(isPhone){
                    mHangoutsList.setLayoutManager(new LinearLayoutManager(getContext()));
                } else {
                    mHangoutsList.setLayoutManager(new StaggeredGridLayoutManager
                            (2, StaggeredGridLayoutManager.VERTICAL));
                }
            } else {
                if(isPhone){
                    mHangoutsList.setLayoutManager(new StaggeredGridLayoutManager
                            (2, StaggeredGridLayoutManager.VERTICAL));
                } else {
                    mHangoutsList.setLayoutManager(new StaggeredGridLayoutManager
                            (3, StaggeredGridLayoutManager.VERTICAL));
                }
            }
        }
    }

    private synchronized void getAroundHangouts(){
        LatLng currentLocation = Details.getLocation(getContext());
        Call<Hangouts> call = mInterface.getHangoutsAroundMe(currentLocation.latitude,
                currentLocation.longitude, Details.getProfileId(getContext()));
        call.enqueue(new Callback<Hangouts>() {
            @Override
            public void onResponse(@NonNull Call<Hangouts> call,
                                   @NonNull retrofit2.Response<Hangouts> response) {
                mHangouts.clear();
                mAdapter.notifyDataSetChanged();
                Hangouts hangouts = response.body();
                if(response.isSuccessful() && hangouts != null){
                    List<Hangout> hangoutList = hangouts.getHangouts();
                    if(hangoutList != null){
                        for(int i = 0; i < hangoutList.size() - 1; i++){
                            Hangout hangout = hangoutList.get(i);
                            hangout.setRequestSent(mHandler.getSentRequests(hangout.getId()) > 0);
                            mHangouts.add(hangout);
                            mAdapter.notifyItemInserted(mHangouts.size() - 1);
                        }
                    }
                    showOrHideMessage();
                } else if(!response.isSuccessful()) {
                    Toast.makeText(getContext(), R.string.went_wrong, Toast.LENGTH_SHORT).show();
                } else {
                    showOrHideMessage();
                }
            }

            @Override
            public void onFailure(@NonNull Call<Hangouts> call, @NonNull Throwable t) {
                t.printStackTrace();
                mHangouts.clear();
                mAdapter.notifyDataSetChanged();
                Toast.makeText(getContext(), R.string.no_connection, Toast.LENGTH_SHORT).show();
            }
        });
    }

}
