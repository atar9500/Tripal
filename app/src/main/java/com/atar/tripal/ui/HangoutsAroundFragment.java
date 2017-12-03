package com.atar.tripal.ui;


import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Configuration;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkInfo;
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
import com.atar.tripal.db.DBHandler;
import com.atar.tripal.db.Details;
import com.atar.tripal.net.ApiClient;
import com.atar.tripal.net.ApiInterface;
import com.atar.tripal.net.LocationDetectorService;
import com.atar.tripal.net.NetConstants;
import com.atar.tripal.objects.User;
import com.atar.tripal.objects.Hangout;
import com.atar.tripal.objects.Message;
import com.atar.tripal.objects.Result;

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
                    if(mMsgMain.isShown()){
                        mMsgMain.startAnimation(AnimationUtils.loadAnimation(getContext(), R.anim.fade_out));
                        mMsgMain.setVisibility(View.INVISIBLE);
                    }
                    if(!mMsgSub.isShown()){
                        mMsgSub.startAnimation(AnimationUtils.loadAnimation(getContext(), R.anim.fade_out));
                        mMsgSub.setVisibility(View.INVISIBLE);
                    }
                    mRefresh.setRefreshing(true);
                    break;
                case LocationDetectorService.END:
                    mHangouts.clear();
                    mAdapter.notifyDataSetChanged();
                    mRefresh.setRefreshing(false);
                    break;
                case LocationDetectorService.GET_AROUND_HANGOUTS:
                    mRefresh.setRefreshing(false);
                    mHangouts.add(0, (Hangout)intent.getSerializableExtra("hangout"));
                    mAdapter.notifyItemInserted(0);
                    break;
                case LocationDetectorService.START_GETTING_HANGOUTS:
                    mRefresh.setRefreshing(false);
                    mHangouts.clear();
                    mAdapter.notifyDataSetChanged();
                    Hangout hangout = (Hangout)intent.getSerializableExtra("hangout");
                    if(hangout != null){
                        mHangouts.add(0, (Hangout)intent.getSerializableExtra("hangout"));
                        mAdapter.notifyItemInserted(0);
                    }
                    break;
            }
            showOrHideMessage();
        }
    }

    private View mView;

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

        return mView;
    }

    @Override
    public void onStart() {
        if(getContext() != null){
            mReceiver = new LocationUpdateReceiver();
            LocalBroadcastManager.getInstance(getContext()).registerReceiver
                    (mReceiver, new IntentFilter(LocationDetectorService.BROADCAST_IDENTIFIER_FOR_SERVICE_FINISHED_RESPONSE));
        }
        super.onStart();
    }

    @Override
    public void onStop() {
        if(getContext() != null){
            LocalBroadcastManager.getInstance(getContext()).unregisterReceiver(mReceiver);
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
        mMsgMain = mView.findViewById(R.id.around_msg_title);
        mMsgSub = mView.findViewById(R.id.around_msg_sub);

        // RecyclerView that fits the hangouts
        mHangoutsList = mView.findViewById(R.id.around_list);
        adjustList();
        mAdapter = new AroundHangoutsAdapter(this, getContext(), mHangouts);
        mHangoutsList.setAdapter(mAdapter);
        mHangoutsList.setHasFixedSize(true);

        // Swipe to Refresh
        mRefresh = mView.findViewById(R.id.around_refresh);
        int [] colors = new int[]{R.color.colorAccent, R.color.colorPrimary};
        mRefresh.setColorSchemeResources(colors);
        mRefresh.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                if(getActivity() != null && connectedToInternet()){
                    Intent intentService = new Intent(getContext(),
                            LocationDetectorService.class);
                    intentService.putExtra(LocationDetectorService.CODE,
                            LocationDetectorService.GET_AROUND_HANGOUTS);
                    getActivity().startService(intentService);
                } else if(connectedToInternet()){
                    Toast.makeText(getContext(), R.string.no_connection, Toast.LENGTH_SHORT).show();
                }
            }
        });

    }

    public void showOrHideMessage(){
        Animation fadeIn = AnimationUtils.loadAnimation(getContext(), R.anim.fade_in);
        Animation fadeOut = AnimationUtils.loadAnimation(getContext(), R.anim.fade_out);
        if(!Details.getStatus(getContext())) {
            if(!mMsgMain.isShown()){
                mMsgMain.startAnimation(fadeIn);
                mMsgMain.setVisibility(View.VISIBLE);
            }
            mMsgMain.setText(R.string.you_are_offline);
            if(!mMsgSub.isShown()){
                mMsgSub.startAnimation(fadeIn);
                mMsgSub.setVisibility(View.VISIBLE);
            }
            mMsgSub.setText(R.string.go_online);
            if(mRefresh.isShown()){
                mRefresh.startAnimation(fadeIn);
                mRefresh.setVisibility(View.GONE);
            }
        } else if(mAdapter.getItemCount() == 0){
            if(!mMsgMain.isShown()){
                mMsgMain.startAnimation(fadeIn);
                mMsgMain.setVisibility(View.VISIBLE);
            }
            mMsgMain.setText(R.string.no_hangouts_around);
            if(!mMsgSub.isShown()){
                mMsgSub.startAnimation(fadeIn);
                mMsgSub.setVisibility(View.VISIBLE);
            }
            mMsgSub.setText(R.string.setup_hangout);
            if(!mRefresh.isShown()){
                mRefresh.startAnimation(fadeIn);
                mRefresh.setVisibility(View.VISIBLE);
            }
        } else {
            if(mMsgMain.isShown()){
                mMsgMain.startAnimation(fadeOut);
                mMsgMain.setVisibility(View.INVISIBLE);
            }
            if(mMsgSub.isShown()){
                mMsgSub.startAnimation(fadeOut);
                mMsgSub.setVisibility(View.INVISIBLE);
            }
            if(!mRefresh.isShown()){
                mRefresh.startAnimation(fadeIn);
                mRefresh.setVisibility(View.VISIBLE);
            }
        }
        mRefresh.setRefreshing(false);
    }

    private boolean connectedToInternet(){
        if(getActivity() != null){
            ConnectivityManager cm = (ConnectivityManager)getActivity().getSystemService(Context.CONNECTIVITY_SERVICE);
            if(cm != null){
                Network[] activeNetworks = cm.getAllNetworks();
                for (Network n: activeNetworks) {
                    NetworkInfo nInfo = cm.getNetworkInfo(n);
                    if(nInfo.isConnected())
                        return true;
                }
            }
        }
        return false;
    }

    private void adjustList(){
        if(mHangoutsList != null){
            if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT) {
                if(getResources().getBoolean(R.bool.is_tablet)){
                    mHangoutsList.setLayoutManager(new LinearLayoutManager(getContext()));
                } else {
                    mHangoutsList.setLayoutManager(new StaggeredGridLayoutManager
                            (2, StaggeredGridLayoutManager.VERTICAL));
                }
            } else {
                if(getResources().getBoolean(R.bool.is_tablet)){
                    mHangoutsList.setLayoutManager(new StaggeredGridLayoutManager
                            (2, StaggeredGridLayoutManager.VERTICAL));
                } else {
                    mHangoutsList.setLayoutManager(new StaggeredGridLayoutManager
                            (3, StaggeredGridLayoutManager.VERTICAL));
                }
            }
        }
    }

}
