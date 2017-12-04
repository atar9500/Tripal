package com.atar.tripal.ui;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Configuration;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.Snackbar;
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
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.atar.tripal.R;
import com.atar.tripal.adapters.MyHangoutsAdapter;
import com.atar.tripal.callbacks.MyHangoutCallback;
import com.atar.tripal.db.DBConstants;
import com.atar.tripal.db.DBHandler;
import com.atar.tripal.db.Details;
import com.atar.tripal.net.ApiClient;
import com.atar.tripal.net.ApiInterface;
import com.atar.tripal.net.NetConstants;
import com.atar.tripal.net.RequestsService;
import com.atar.tripal.net.SendingMessageService;
import com.atar.tripal.objects.Hangouts;
import com.atar.tripal.objects.Message;
import com.atar.tripal.objects.User;
import com.atar.tripal.objects.Hangout;
import com.atar.tripal.objects.Result;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;

import static com.atar.tripal.net.NetConstants.HANGOUT_TIME;

public class MyHangoutsFragment extends Fragment implements MyHangoutCallback {

    private class RequestsDetector extends BroadcastReceiver{

        @Override
        public void onReceive(Context context, Intent intent) {
            String result = intent.getStringExtra(NetConstants.RESULT);
            if(!result.equals(NetConstants.RESULT_FULL)){
                final long hangoutId = intent.getLongExtra(DBConstants.COL_HANGOUT_ID, -1);
                for(int i = 0; i < mActiveHangouts.size(); i++){
                    if(mActiveHangouts.get(i).getId() == hangoutId){
                        refreshHangout(i, hangoutId);
                        break;
                    }
                }
            }
        }
    }

    private class HangoutUpdater extends BroadcastReceiver{
        @Override
        public void onReceive(Context context, Intent intent) {
            if(intent != null){
                int typeOfMessage = intent.getIntExtra(DBConstants.COL_TYPE, -1);
                String username = intent.getStringExtra(DBConstants.DB_NAME);
                if(typeOfMessage == Message.TYPE_JOINED && username.equals(Details.getUsername(getContext()))){
                    getMyHangouts();
                } else if(typeOfMessage == Message.TYPE_JOINED || typeOfMessage == Message.TYPE_LEFT){
                    final long hangoutId = intent.getLongExtra(DBConstants.COL_HANGOUT_ID, -1);
                    new Thread(new Runnable() {
                        @Override
                        public void run() {
                            for(int i = 0; i < mActiveHangouts.size(); i++){
                                if(mActiveHangouts.get(i).getId() == hangoutId){
                                    refreshHangout(i, hangoutId);
                                    break;
                                }
                            }
                        }
                    }).run();
                }

            }
        }
    }

    /**
     * Data
     */
    private ApiInterface mInterface;
    private List<Hangout> mPastHangouts, mActiveHangouts;
    private DBHandler mHandler;

    /**
     * Broadcast Receivers
     */
    private HangoutUpdater mHangoutUpdaterReceiver;
    private RequestsDetector mRequestsReceiver;

    /**
     * UI Widgets
     */
    private View mView;
    private LinearLayout mNoHangoutsActive, mMyHangouts;
    private TextView mPastHangoutsLabel, mMsgTitle, mMsgSub;
    private MyHangoutsAdapter mPastAdapter, mActiveAdapter;
    private SwipeRefreshLayout mRefresh;
    private RecyclerView mActiveHangoutsList, mPastHangoutsList;

    public MyHangoutsFragment() {}

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        if(mPastHangouts == null){
            mPastHangouts = new ArrayList<>();
        }
        if(mActiveHangouts == null){
            mActiveHangouts = new ArrayList<>();
        }

        setRetainInstance(true);

        mHandler = new DBHandler(getContext());

        mView = inflater.inflate(R.layout.fragment_my_hangouts, container, false);

        mInterface = ApiClient.getAPIClient().create(ApiInterface.class);

        initUIWidgets();

        arrangeMessages();

        if(savedInstanceState == null){
            getMyHangouts();
        }

        return mView;
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        adjustList();
    }

    @Override
    public void onStart() {
        if(getContext() != null){
            mHangoutUpdaterReceiver = new HangoutUpdater();
            LocalBroadcastManager.getInstance(getContext()).registerReceiver
                    (mHangoutUpdaterReceiver, new IntentFilter
                    (SendingMessageService.BROADCAST_IDENTIFIER_FOR_SERVICE_FINISHED_RESPONSE));
            mRequestsReceiver = new RequestsDetector();
            LocalBroadcastManager.getInstance(getContext()).registerReceiver
                    (mRequestsReceiver, new IntentFilter
                    (RequestsService.BROADCAST_IDENTIFIER_FOR_SERVICE_FINISHED_RESPONSE));
        }
        super.onStart();
    }

    @Override
    public void onStop() {
        if(getContext() != null){
            LocalBroadcastManager.getInstance(getContext()).unregisterReceiver(mHangoutUpdaterReceiver);
            LocalBroadcastManager.getInstance(getContext()).unregisterReceiver(mRequestsReceiver);
        }
        super.onStop();
    }

    @Override
    public void onHangoutClick(int position, boolean isActive) {
        Hangout hangout;
        if(isActive){
            hangout = mActiveHangouts.get(position);
        } else {
            hangout = mPastHangouts.get(position);
        }
        Intent intent = new Intent(getActivity(), HangoutActivity.class);
        Bundle bundle = new Bundle();
        bundle.putSerializable("hangout", hangout);
        intent.putExtras(bundle);
        intent.putExtra(HangoutActivity.REQUEST_CODE, HangoutActivity.PRESS_HANGOUT);
        startActivity(intent);
    }

    @Override
    public void onHangoutLeave(final int position) {
        final Hangout hangout = new Hangout();
        hangout.setId(mActiveHangouts.get(position).getId());
        hangout.getFriends().add(new User(Details.getProfileId(getContext())));
        Call<Result> call = mInterface.leaveHangout(hangout);
        call.enqueue(new Callback<Result>() {
            @Override
            public void onResponse(@NonNull Call<Result> call,
                    @NonNull retrofit2.Response<Result> response) {
                Result result = response.body();
                if(response.isSuccessful() && result != null){
                    switch(result.getResult()){
                        case NetConstants.RESULT_SUCCESS:
                            mActiveHangouts.remove(position);
                            mActiveAdapter.notifyItemRemoved(position);
                            mHandler.deleteMessagesOfHangout(hangout.getId());
                            break;
                        case NetConstants.RESULT_NOT_ACTIVE:
                            Toast.makeText(getContext(), R.string.hangout_not_active,
                                    Toast.LENGTH_SHORT).show();
                            mActiveHangouts.remove(position);
                            mActiveAdapter.notifyItemRemoved(position);
                            mPastHangouts.add(0, hangout);
                            mPastAdapter.notifyItemInserted(0);
                            break;
                        case NetConstants.RESULT_NOT_THERE:
                            Toast.makeText(getContext(), R.string.already_left,
                                    Toast.LENGTH_SHORT).show();
                            mActiveHangouts.remove(position);
                            mActiveAdapter.notifyItemRemoved(position);
                            mHandler.deleteMessagesOfHangout(hangout.getId());
                            break;
                    }
                } else {
                    Toast.makeText(getContext(), R.string.went_wrong, Toast.LENGTH_SHORT).show();
                }
                arrangeMessages();
            }

            @Override
            public void onFailure(@NonNull Call<Result> call, @NonNull Throwable t) {
                arrangeMessages();
                Toast.makeText(getContext(), R.string.no_connection, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void initUIWidgets(){
        mRefresh = mView.findViewById(R.id.my_refresh);
        int [] colors = {R.color.colorAccent, R.color.colorPrimary};
        mRefresh.setColorSchemeResources(colors);
        mRefresh.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                getMyHangouts();
            }
        });

        mMyHangouts = mView.findViewById(R.id.my_hangouts);
        mNoHangoutsActive = mView.findViewById(R.id.my_active_empty);
        mPastHangoutsLabel = mView.findViewById(R.id.my_past_label);
        mMsgTitle = mView.findViewById(R.id.my_msg_title);
        mMsgSub = mView.findViewById(R.id.my_msg_sub);

        mActiveAdapter = new MyHangoutsAdapter(mActiveHangouts, true, this, getContext());
        mActiveHangoutsList = mView.findViewById(R.id.my_recent_list);

        mPastAdapter = new MyHangoutsAdapter(mPastHangouts, false, this, getContext());
        mPastHangoutsList = mView.findViewById(R.id.my_past_list);

        adjustList();

        mActiveHangoutsList.setAdapter(mActiveAdapter);
        mActiveHangoutsList.setHasFixedSize(false);
        mActiveHangoutsList.setNestedScrollingEnabled(false);

        mPastHangoutsList.setAdapter(mPastAdapter);
        mPastHangoutsList.setHasFixedSize(false);
        mPastHangoutsList.setNestedScrollingEnabled(false);

    }

    private void arrangeMessages(){
        mRefresh.setRefreshing(false);

        Animation animationIn = AnimationUtils.loadAnimation(getContext(), R.anim.fade_in);

        // If user has no hangouts at all the hangout lists will be hidden and a message wil appear
        if(mActiveHangouts.size() == 0 && mPastHangouts.size() == 0){
            mMyHangouts.setVisibility(View.GONE);
            mMsgTitle.setText(R.string.no_hangouts_yet);
            if(!mMsgTitle.isShown()){
                mMsgTitle.startAnimation(animationIn);
                mMsgTitle.setVisibility(View.VISIBLE);
            }
            mMsgSub.setText(R.string.start_hanging);
            if(!mMsgSub.isShown()){
                mMsgSub.startAnimation(animationIn);
                mMsgSub.setVisibility(View.VISIBLE);
            }

        // If user has only past hangouts and no active hangouts, hangouts lists will be shown with
        // an appropriate message
        } else if(mActiveHangouts.size() == 0 && mPastHangouts.size() > 0) {
            mMsgTitle.setVisibility(View.INVISIBLE);
            mMsgSub.setVisibility(View.INVISIBLE);
            if(!mMyHangouts.isShown()){
                mMyHangouts.setAnimation(animationIn);
                mMyHangouts.setVisibility(View.VISIBLE);
            }
            if(!mPastHangoutsLabel.isShown()){
                mPastHangoutsLabel.setAnimation(animationIn);
                mPastHangoutsLabel.setVisibility(View.VISIBLE);
            }
            if(!mNoHangoutsActive.isShown()){
                mNoHangoutsActive.setAnimation(animationIn);
                mNoHangoutsActive.setVisibility(View.VISIBLE);
            }

        // If user has only active hangouts and no past hangouts, hangouts lists will be shown with
        // an appropriate message
        } else if(mPastHangouts.size() == 0 && mActiveHangouts.size() > 0) {
            mMsgTitle.setVisibility(View.INVISIBLE);
            mMsgSub.setVisibility(View.INVISIBLE);
            if(!mMyHangouts.isShown()){
                mMyHangouts.setAnimation(animationIn);
                mMyHangouts.setVisibility(View.VISIBLE);
            }
            mNoHangoutsActive.setVisibility(View.GONE);
            mPastHangoutsLabel.setVisibility(View.INVISIBLE);

        // If user has active hangouts and past hangouts, all the lists will be shown
        // and all the messages will be hidden
        } else {
            mMsgTitle.setVisibility(View.INVISIBLE);
            mMsgSub.setVisibility(View.INVISIBLE);
            if(!mMyHangouts.isShown()){
                mMyHangouts.setAnimation(animationIn);
                mMyHangouts.setVisibility(View.VISIBLE);
            }
            mNoHangoutsActive.setVisibility(View.GONE);
            if(!mPastHangoutsLabel.isShown()){
                mPastHangoutsLabel.setAnimation(animationIn);
                mPastHangoutsLabel.setVisibility(View.VISIBLE);
            }
        }
    }

    private void adjustList(){
        if(mActiveHangoutsList != null && mPastHangoutsList != null){
            boolean isPhone = mView.findViewById(R.id.my_view) != null;
            if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT) {
                if(isPhone){
                    mActiveHangoutsList.setLayoutManager(new LinearLayoutManager(getContext()));
                    mPastHangoutsList.setLayoutManager(new LinearLayoutManager(getContext()));
                } else {
                    mActiveHangoutsList.setLayoutManager(new StaggeredGridLayoutManager
                            (2, StaggeredGridLayoutManager.VERTICAL));
                    mPastHangoutsList.setLayoutManager(new StaggeredGridLayoutManager
                            (2, StaggeredGridLayoutManager.VERTICAL));
                }
            } else {
                if(isPhone){
                    mActiveHangoutsList.setLayoutManager(new StaggeredGridLayoutManager
                            (2, StaggeredGridLayoutManager.VERTICAL));
                    mPastHangoutsList.setLayoutManager(new StaggeredGridLayoutManager
                            (2, StaggeredGridLayoutManager.VERTICAL));
                } else {
                    mActiveHangoutsList.setLayoutManager(new StaggeredGridLayoutManager
                            (3, StaggeredGridLayoutManager.VERTICAL));
                    mPastHangoutsList.setLayoutManager(new StaggeredGridLayoutManager
                            (3, StaggeredGridLayoutManager.VERTICAL));
                }
            }
        }
    }

    public void getMyHangouts(){
        mRefresh.setRefreshing(true);
        Call<Hangouts> call = mInterface.getMyHangouts
                (Details.getProfileId(getContext()));
        call.enqueue(new Callback<Hangouts>() {
            @Override
            public void onResponse(@NonNull Call<Hangouts> call,
                                   @NonNull retrofit2.Response<Hangouts> response) {

                int emptyActive = mActiveHangouts.size();
                mActiveHangouts.clear();
                mActiveAdapter.notifyItemRangeRemoved(0, emptyActive);
                int emptyPast = mPastHangouts.size();
                mPastHangouts.clear();
                mPastAdapter.notifyItemRangeRemoved(0, emptyPast);

                Hangouts set = response.body();
                if(response.isSuccessful() && set != null){
                    List<Hangout> data = set.getHangouts();
                    if(data != null){
                        for(int i = 0; i < data.size() - 1; i++){
                            Hangout hangout = data.get(i);
                            List<User> users = hangout.getFriends();
                            if(users != null && users.size() > 1){
                                users.remove(users.size() - 1);
                            }
                            hangout.setIsActive(hangout.getTimestamp() + HANGOUT_TIME
                                    > System.currentTimeMillis());
                            if(hangout.getIsActive()){
                                mActiveHangouts.add(hangout);
                                mActiveAdapter.notifyItemInserted(mActiveHangouts.size() - 1);
                            } else {
                                mPastHangouts.add(hangout);
                                mPastAdapter.notifyItemInserted(mPastHangouts.size() - 1);
                            }
                            arrangeMessages();
                        }
                    }
                }
                arrangeMessages();
            }

            @Override
            public void onFailure(@NonNull Call<Hangouts> call, @NonNull Throwable t) {
                int emptyActive = mActiveHangouts.size();
                mActiveHangouts.clear();
                mActiveAdapter.notifyItemRangeRemoved(0, emptyActive);
                int emptyPast = mPastHangouts.size();
                mPastHangouts.clear();
                mPastAdapter.notifyItemRangeRemoved(0, emptyPast);
                arrangeMessages();
                Snackbar.make(mView, R.string.no_connection, Snackbar.LENGTH_LONG).show();
            }
        });
    }

    private void refreshHangout(final int position, long hangoutId){
        Call<Hangout> hangoutCall = mInterface.getHangout(hangoutId);
        hangoutCall.enqueue(new Callback<Hangout>() {
            @Override
            public void onResponse(@NonNull Call<Hangout> call,
                                   @NonNull retrofit2.Response<Hangout> response) {
                Hangout hangout = response.body();
                if(response.isSuccessful() && hangout != null){
                    List<User> users = hangout.getFriends();
                    if(users.size() > 0){
                        users.remove(users.size() - 1);
                    }
                    hangout.setIsActive(hangout.getTimestamp() + HANGOUT_TIME
                            > System.currentTimeMillis());
                    mActiveHangouts.set(position, hangout);
                    mActiveAdapter.notifyItemChanged(position);
                } else {
                    Toast.makeText(getContext(), R.string.went_wrong, Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(@NonNull Call<Hangout> call, @NonNull Throwable t) {
                t.printStackTrace();
                Snackbar.make(mView, R.string.no_connection, Snackbar.LENGTH_LONG).show();
            }
        });
    }

}
