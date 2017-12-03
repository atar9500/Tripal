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
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.atar.tripal.R;
import com.atar.tripal.adapters.MyHangoutsAdapter;
import com.atar.tripal.callbacks.MyHangoutCallback;
import com.atar.tripal.db.DBHandler;
import com.atar.tripal.db.Details;
import com.atar.tripal.net.ApiClient;
import com.atar.tripal.net.ApiInterface;
import com.atar.tripal.net.LocationDetectorService;
import com.atar.tripal.net.NetConstants;
import com.atar.tripal.objects.User;
import com.atar.tripal.objects.Hangout;
import com.atar.tripal.objects.Result;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;

/**
 * A simple {@link Fragment} subclass.
 */
public class MyHangoutsFragment extends Fragment implements MyHangoutCallback {

    private class HangoutsUpdaterReceiver extends BroadcastReceiver{

        @SuppressWarnings("unchecked")
        @Override
        public void onReceive(Context context, Intent intent) {

            if(intent.getIntExtra(LocationDetectorService.CODE, -1) ==
                    LocationDetectorService.GET_MY_HANGOUTS){
                switch(intent.getStringExtra(NetConstants.RESULT)){
                    case NetConstants.RESULT_SUCCESS:
                        int previousPast = mPastHangouts.size();
                        mPastHangouts.clear();
                        mPastAdapter.notifyItemRangeRemoved(0, previousPast);
                        mPastHangouts.addAll((ArrayList<Hangout>) intent
                                .getSerializableExtra("past_hangouts"));
                        mPastAdapter.notifyDataSetChanged();

                        int previousActive = mActiveHangouts.size();
                        mActiveHangouts.clear();
                        mActiveAdapter.notifyItemRangeRemoved(0, previousActive);
                        mActiveHangouts.addAll((ArrayList<Hangout>) intent
                                .getSerializableExtra("active_hangouts"));
                        mActiveAdapter.notifyDataSetChanged();
                        break;
                    case NetConstants.RESULT_FAILED:
                        int emptyActive = mActiveHangouts.size();
                        mActiveHangouts.clear();
                        mActiveAdapter.notifyItemRangeRemoved(0, emptyActive);
                        int emptyPast = mPastHangouts.size();
                        mPastHangouts.clear();
                        mPastAdapter.notifyItemRangeRemoved(0, emptyPast);
                        break;
                }

            }
            mRefresh.setRefreshing(false);
            arrangeMessages();

        }
    }

    private View mView;

    private HangoutsUpdaterReceiver mReceiver;
    private ApiInterface mInterface;

    /**
     * UI Widgets
     */
    private LinearLayout mNoHangoutsActive, mMyHangouts;
    private TextView mNoHangoutsPast, mMsgTitle, mMsgSub;
    private MyHangoutsAdapter mPastAdapter, mActiveAdapter;
    private List<Hangout> mPastHangouts, mActiveHangouts;
    private SwipeRefreshLayout mRefresh;
    private RecyclerView mActiveHangoutsList, mPastHangoutsList;

    private DBHandler mHandler;

    public MyHangoutsFragment() {
        // Required empty public constructor
    }

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
            mReceiver = new HangoutsUpdaterReceiver();
            LocalBroadcastManager.getInstance(getContext()).registerReceiver
                    (mReceiver, new IntentFilter(LocationDetectorService.BROADCAST_IDENTIFIER_FOR_SERVICE_FINISHED_RESPONSE));
        }
        if(getActivity() != null && connectedToInternet()){
            mRefresh.setRefreshing(true);
            Intent intentService = new Intent(getActivity(), LocationDetectorService.class);
            intentService.putExtra(LocationDetectorService.CODE, LocationDetectorService.GET_MY_HANGOUTS);
            getActivity().startService(intentService);
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
            }

            @Override
            public void onFailure(@NonNull Call<Result> call, @NonNull Throwable t) {
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
                if(getActivity() != null && connectedToInternet()){
                    Intent intentService = new Intent(getActivity(), LocationDetectorService.class);
                    intentService.putExtra(LocationDetectorService.CODE, LocationDetectorService.GET_MY_HANGOUTS);
                    getActivity().startService(intentService);
                }
            }
        });

        mMyHangouts = mView.findViewById(R.id.my_hangouts);
        mNoHangoutsActive = mView.findViewById(R.id.my_active_empty);
        mNoHangoutsPast = mView.findViewById(R.id.my_past_empty);
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
        Animation animationIn = AnimationUtils.loadAnimation(getContext(), R.anim.fade_in);
        Animation animationOut = AnimationUtils.loadAnimation(getContext(), R.anim.fade_out);
        if(!connectedToInternet()){
            if(mMyHangouts.isShown()){
                mMyHangouts.setAnimation(animationOut);
                mMyHangouts.setVisibility(View.GONE);
            }
            mMsgTitle.setText(R.string.we_are_sorry);
            if(!mMsgTitle.isShown()){
                mMsgTitle.startAnimation(animationIn);
                mMsgTitle.setVisibility(View.VISIBLE);
            }
            mMsgSub.setText(R.string.no_connection);
            if(!mMsgSub.isShown()){
                mMsgSub.startAnimation(animationIn);
                mMsgSub.setVisibility(View.VISIBLE);
            }
        } else {
            if(mActiveHangouts.size() == 0 && mPastHangouts.size() == 0){
                if(mMyHangouts.isShown()){
                    mMyHangouts.setAnimation(animationOut);
                    mMyHangouts.setVisibility(View.GONE);
                }
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
            } else if(mActiveHangouts.size() == 0 && mPastHangouts.size() > 0) {
                if(mMsgTitle.isShown()){
                    mMsgTitle.startAnimation(animationOut);
                    mMsgTitle.setVisibility(View.INVISIBLE);
                }
                if(mMsgSub.isShown()){
                    mMsgSub.startAnimation(animationOut);
                    mMsgSub.setVisibility(View.INVISIBLE);
                }
                if(!mMyHangouts.isShown()){
                    mMyHangouts.setAnimation(animationIn);
                    mMyHangouts.setVisibility(View.VISIBLE);
                }
                if(mNoHangoutsPast.isShown()){
                    mNoHangoutsPast.setAnimation(animationOut);
                    mNoHangoutsPast.setVisibility(View.GONE);
                }
                if(!mNoHangoutsActive.isShown()){
                    mNoHangoutsActive.setAnimation(animationIn);
                    mNoHangoutsActive.setVisibility(View.VISIBLE);
                }
            } else if(mPastHangouts.size() == 0 && mActiveHangouts.size() > 0) {
                if(mMsgTitle.isShown()){
                    mMsgTitle.startAnimation(animationOut);
                    mMsgTitle.setVisibility(View.INVISIBLE);
                }
                if(mMsgSub.isShown()){
                    mMsgSub.startAnimation(animationOut);
                    mMsgSub.setVisibility(View.INVISIBLE);
                }
                if(!mMyHangouts.isShown()){
                    mMyHangouts.setAnimation(animationIn);
                    mMyHangouts.setVisibility(View.VISIBLE);
                }
                if(mNoHangoutsActive.isShown()){
                    mNoHangoutsActive.setAnimation(animationOut);
                    mNoHangoutsActive.setVisibility(View.GONE);
                }
                if(!mNoHangoutsPast.isShown()){
                    mNoHangoutsPast.setAnimation(animationIn);
                    mNoHangoutsPast.setVisibility(View.VISIBLE);
                }
            } else {
                if(mMsgTitle.isShown()){
                    mMsgTitle.startAnimation(animationOut);
                    mMsgTitle.setVisibility(View.INVISIBLE);
                }
                if(mMsgSub.isShown()){
                    mMsgSub.startAnimation(animationOut);
                    mMsgSub.setVisibility(View.INVISIBLE);
                }
                if(!mMyHangouts.isShown()){
                    mMyHangouts.setAnimation(animationIn);
                    mMyHangouts.setVisibility(View.VISIBLE);
                }
                if(mNoHangoutsActive.isShown()){
                    mNoHangoutsActive.setAnimation(animationOut);
                    mNoHangoutsActive.setVisibility(View.GONE);
                }
                if(mNoHangoutsPast.isShown()){
                    mNoHangoutsPast.setAnimation(animationOut);
                    mNoHangoutsPast.setVisibility(View.GONE);
                }
            }
        }
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
        if(mActiveHangoutsList != null && mPastHangoutsList != null){
            if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT) {
                if(!getResources().getBoolean(R.bool.is_tablet)){
                    mActiveHangoutsList.setLayoutManager(new LinearLayoutManager(getContext()));
                    mPastHangoutsList.setLayoutManager(new LinearLayoutManager(getContext()));
                } else {
                    mActiveHangoutsList.setLayoutManager(new StaggeredGridLayoutManager
                            (2, StaggeredGridLayoutManager.VERTICAL));
                    mPastHangoutsList.setLayoutManager(new StaggeredGridLayoutManager
                            (2, StaggeredGridLayoutManager.VERTICAL));
                }
            } else {
                if(!getResources().getBoolean(R.bool.is_tablet)){
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

}
