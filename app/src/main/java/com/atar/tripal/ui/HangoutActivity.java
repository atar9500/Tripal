package com.atar.tripal.ui;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.IntentSender;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.design.widget.BottomSheetBehavior;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.atar.tripal.BuildConfig;
import com.atar.tripal.R;
import com.atar.tripal.adapters.FriendsAdapter;
import com.atar.tripal.adapters.RequestsAdapter;
import com.atar.tripal.callbacks.HangoutCallback;
import com.atar.tripal.callbacks.RequestsCallback;
import com.atar.tripal.db.DBConstants;
import com.atar.tripal.db.DBHandler;
import com.atar.tripal.db.Details;
import com.atar.tripal.net.ApiClient;
import com.atar.tripal.net.ApiInterface;
import com.atar.tripal.net.GettingMessageService;
import com.atar.tripal.net.LocationDetectorService;
import com.atar.tripal.net.NetConstants;
import com.atar.tripal.net.RequestsService;
import com.atar.tripal.net.StatusUpdaterService;
import com.atar.tripal.objects.User;
import com.atar.tripal.objects.Hangout;
import com.atar.tripal.objects.Message;
import com.atar.tripal.objects.PlaceInfo;
import com.atar.tripal.objects.Result;
import com.google.android.gms.common.api.ResolvableApiException;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;

public class HangoutActivity extends AppCompatActivity implements HangoutCallback, RequestsCallback {

    private static final String TAG = "HangoutActivity";
    private static final int PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION = 31;
    private static final int REQUEST_CHECK_SETTINGS = 0x1;

    public static final int CREATE_HANGOUT = 333;
    public static final int PRESS_HANGOUT = 444;
    public static final int PRESS_NOTIFICATION = 555;
    public static final int PRESS_REQUEST = 666;
    public static final String REQUEST_CODE = "code";

    @Override
    public void onRequestAccepted(int position) {
        Intent joinIntent = new Intent(this , RequestsService.class);
        joinIntent.putExtra(RequestsService.REQUEST, RequestsService.ACCEPT);
        joinIntent.putExtra(DBConstants.COL_ID, mRequestsList.get(position).getId());
        joinIntent.putExtra(DBConstants.COL_ID, mRequestsList.get(position).getId());
        joinIntent.putExtra("pos", position);
        startService(joinIntent);
        mRequestsList.remove(position);
    }

    @Override
    public void onRequestDenied(int position) {
        mHandler.deleteMessage(mRequestsList.get(position).getId());
        mRequestsList.remove(position);
        mRequestsAdapter.notifyDataSetChanged();
        showHideEmpty();
    }

    private class RequestReceiver extends BroadcastReceiver{

        @Override
        public void onReceive(Context context, Intent intent) {
            int pos = intent.getIntExtra(NetConstants.POSITION, -1);
            Message message = mHandler.getMessage(intent.getLongExtra(DBConstants.COL_ID, -1));
            switch(intent.getStringExtra(NetConstants.RESULT)){
                case NetConstants.RESULT_SUCCESS:
                    User user = new User();
                    user.setId(message.getSenderId());
                    user.setUsername(message.getSenderName());
                    mHangout.getFriends().add(user);
                    mFriendsList.add(user);
                    mFriendsAdapter.notifyDataSetChanged();
                    break;
                case NetConstants.RESULT_FULL:
                    mRequestsList.add(pos, message);
                    mRequestsAdapter.notifyDataSetChanged();
                    break;
                case NetConstants.RESULT_ALREADY_THERE:
                    mRequestsList.add(pos, message);
                    mRequestsAdapter.notifyDataSetChanged();
                    break;
                case NetConstants.RESULT_NOT_ACTIVE:
                    mHandler.deleteRequests(mHangout.getId());
                    mRequestsList.clear();
                    mRequestsAdapter.notifyDataSetChanged();
                    updateCount();
                    mHangout.setIsActive(false);
                    mLeave.setVisible(false);
                    break;
                case NetConstants.RESULT_FAILED:
                    mRequestsList.add(pos, message);
                    mRequestsAdapter.notifyDataSetChanged();
                    break;
                case NetConstants.RESULT_NOT_FOUND:
                    mRequestsList.add(pos, message);
                    mRequestsAdapter.notifyDataSetChanged();
                    break;
            }
            showHideEmpty();
        }
    }

    private class LocationReceiver extends BroadcastReceiver{

        @Override
        public void onReceive(Context context, Intent intent) {
            int i = intent.getIntExtra(LocationDetectorService.CODE, -1);
            switch (i){
                case LocationDetectorService.GPS_IS_NEEDED:
                    try {
                        if(intent.getExtras() != null){
                            Exception e = (Exception) intent.getExtras().getSerializable("E");
                            ResolvableApiException rae = (ResolvableApiException) e;
                            if(rae != null){
                                rae.startResolutionForResult(
                                        HangoutActivity.this, REQUEST_CHECK_SETTINGS);
                            }
                        }
                    } catch (IntentSender.SendIntentException sie) {
                        Log.i(TAG, "PendingIntent unable to execute request.");
                    }
                    break;
                case LocationDetectorService.END:
                    startOnline();
                    break;
                case LocationDetectorService.REFRESH_HANGOUT:
                    switch(intent.getStringExtra(NetConstants.RESULT)){
                        case "SUCCESS":
                            mHangout = (Hangout)intent.getSerializableExtra("hangout");
                            mTheme.setText(mHangout.getTheme());
                            if(!mHangout.getIsActive()){
                                mLeave.setVisible(false);
                            }
                            updateCount();
                            if(mFriendsSheet.getState() != BottomSheetBehavior.STATE_EXPANDED){
                                mBadge.setVisibility(View.INVISIBLE);
                            }
                            mFriendsList.add(mHangout.getHost());
                            mFriendsList.addAll(mHangout.getFriends());
                            mFriendsAdapter.notifyDataSetChanged();
                            mRequestsList.addAll(mHandler.getReceivedRequests(mHangout.getId()));
                            mRequestsAdapter.notifyDataSetChanged();
                            showHideEmpty();
                            if(mRequestCode == PRESS_NOTIFICATION || mRequestCode == PRESS_REQUEST){
                                if(findViewById(R.id.hangout_container) != null){
                                    getSupportFragmentManager().beginTransaction()
                                            .setCustomAnimations(R.anim.slide_in_bottom, R.anim.slide_out_top)
                                            .add(R.id.hangout_container, mChatFragment).commit();
                                }
                                if(mRequestCode == PRESS_REQUEST){
                                    mFriendsSheet.setState(BottomSheetBehavior.STATE_EXPANDED);
                                }
                            }
                            mChatFragment.refresh();
                            break;
                        case "FAILED":
                            Toast.makeText(context, R.string.no_connection,
                                    Toast.LENGTH_SHORT).show();
                            finish();
                            break;
                    }
                    break;
            }
        }
    }

    private class GettingMessagesReceiver extends BroadcastReceiver{
        @Override
        public void onReceive(Context context, Intent intent) {
            Message message = mHandler.getMessage(intent
                    .getLongExtra(DBConstants.COL_ID, -1));
            if(message != null){
                switch (message.getType()){
                    case Message.TYPE_JOIN:
                        mRequestsList.add(0, message);
                        mRequestsAdapter.notifyDataSetChanged();
                        updateCount();
                        if(mFriendsSheet.getState() == BottomSheetBehavior.STATE_HIDDEN){
                            mBadge.setVisibility(View.INVISIBLE);
                        }
                        showHideEmpty();
                        break;
                    case Message.TYPE_LEFT:
                        Intent broadcastIntent = new Intent(HangoutActivity.this,
                                LocationDetectorService.class);
                        broadcastIntent.putExtra(LocationDetectorService.CODE,
                                LocationDetectorService.REFRESH_HANGOUT);
                        broadcastIntent.putExtra(DBConstants.COL_HANGOUT_ID,
                                mHangout.getId());
                        startService(broadcastIntent);
                        break;
                    case Message.TYPE_PLACE:
                        Intent intent2 = new Intent(HangoutActivity.this,
                                LocationDetectorService.class);
                        intent2.putExtra(LocationDetectorService.CODE,
                                LocationDetectorService.REFRESH_HANGOUT);
                        intent2.putExtra(DBConstants.COL_HANGOUT_ID,
                                mHangout.getId());
                        startService(intent2);
                        break;
                }
            }
        }
    }

    private ChatFragment mChatFragment;
    private MapFragment mMapFragment;

    /**
     * Broadcast Receivers
     */
    private LocationReceiver mLocationReceiver;
    private GettingMessagesReceiver mGettingMessagesReceiver;
    private RequestReceiver mRequestReceiver;
    private ApiInterface mInterface;

    /**
     * UI Widgets
     */
    private TextView mBadge, mTheme, mRequestsLabel;
    private MenuItem mFriends, mLeave;
    private BottomSheetBehavior mFriendsSheet;
    private FriendsAdapter mFriendsAdapter;
    private RequestsAdapter mRequestsAdapter;

    /**
     * Data
     */
    private List<User> mFriendsList;
    private List<Message> mRequestsList;
    private DBHandler mHandler;
    private Hangout mHangout;
    private int mRequestCode;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_hangout);

        mInterface = ApiClient.getAPIClient().create(ApiInterface.class);

        Intent intent = getIntent();
        mRequestCode = intent.getIntExtra(REQUEST_CODE, -1);

        Toolbar toolbar = findViewById(R.id.hangout_toolbar);
        toolbar.setTitle("");
        setSupportActionBar(toolbar);

        mHandler = new DBHandler(this);

        initFragments();
        initUIWidgets();

        switch(mRequestCode){
            case CREATE_HANGOUT:
                Log.i(TAG, "User creates a new hangout");
                if(findViewById(R.id.hangout_container) != null &&
                        !(getCurrentFragment() instanceof MapFragment)){
                    getSupportFragmentManager().beginTransaction()
                            .setCustomAnimations(R.anim.slide_in_bottom, R.anim.slide_out_top)
                            .add(R.id.hangout_container, mMapFragment).commit();
                }
                LinearLayout title = findViewById(R.id.hangout_title);
                title.setVisibility(View.GONE);
                break;
            case PRESS_HANGOUT:
                if(findViewById(R.id.hangout_container) != null){
                    getSupportFragmentManager().beginTransaction()
                            .setCustomAnimations(R.anim.slide_in_bottom, R.anim.slide_out_top)
                            .add(R.id.hangout_container, mChatFragment).commit();
                }
                mHangout = (Hangout) intent.getSerializableExtra("hangout");
                if(mHangout == null){
                    Toast.makeText(this, "Something went wrong...", Toast.LENGTH_LONG).show();
                    finish();
                } else {
                    mFriendsList.add(mHangout.getHost());
                    mFriendsList.addAll(mHangout.getFriends());
                    mRequestsList.addAll(mHandler.getReceivedRequests(mHangout.getId()));
                    mTheme.setText(mHangout.getTheme());
                    if(!mHangout.getIsActive()){
                        mHandler.deleteRequests(mHangout.getId());
                    }
                }
                break;
            case PRESS_NOTIFICATION:
                Intent serviceIntent = new Intent(HangoutActivity.this,
                        LocationDetectorService.class);
                serviceIntent.putExtra(LocationDetectorService.CODE,
                        LocationDetectorService.REFRESH_HANGOUT);
                long i = intent.getLongExtra(DBConstants.COL_HANGOUT_ID, -1);
                serviceIntent.putExtra(DBConstants.COL_HANGOUT_ID, i);
                startService(serviceIntent);
                break;
            case PRESS_REQUEST:
                Intent servicesIntent = new Intent(HangoutActivity.this,
                        LocationDetectorService.class);
                servicesIntent.putExtra(LocationDetectorService.CODE,
                        LocationDetectorService.REFRESH_HANGOUT);
                long j = intent.getLongExtra(DBConstants.COL_HANGOUT_ID, -1);
                servicesIntent.putExtra(DBConstants.COL_HANGOUT_ID, j);
                startService(servicesIntent);
                break;
        }

    }

    @Override
    protected void onResume() {
        mLocationReceiver = new LocationReceiver();
        LocalBroadcastManager.getInstance(this).registerReceiver
                (mLocationReceiver, new IntentFilter(LocationDetectorService
                        .BROADCAST_IDENTIFIER_FOR_SERVICE_FINISHED_RESPONSE));
        mGettingMessagesReceiver = new GettingMessagesReceiver();
        LocalBroadcastManager.getInstance(this).registerReceiver
                (mGettingMessagesReceiver, new IntentFilter(GettingMessageService
                        .BROADCAST_IDENTIFIER_FOR_SERVICE_FINISHED_RESPONSE));
        mRequestReceiver = new RequestReceiver();
        LocalBroadcastManager.getInstance(this).registerReceiver
                (mRequestReceiver, new IntentFilter(RequestsService
                        .BROADCAST_IDENTIFIER_FOR_SERVICE_FINISHED_RESPONSE));
        super.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
        LocalBroadcastManager.getInstance(this).unregisterReceiver(mLocationReceiver);
        LocalBroadcastManager.getInstance(this).unregisterReceiver(mGettingMessagesReceiver);
        LocalBroadcastManager.getInstance(this).unregisterReceiver(mRequestReceiver);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        if(mRequestCode != CREATE_HANGOUT){
            menu.clear();
            getMenuInflater().inflate(R.menu.hangout_menu, menu);
            mFriends = menu.findItem(R.id.hangout_friends);
            mFriends.getActionView().setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    if(mFriendsSheet.getState() != BottomSheetBehavior.STATE_EXPANDED){
                        mFriendsSheet.setState(BottomSheetBehavior.STATE_EXPANDED);
                    }
                }
            });
            mLeave = menu.findItem(R.id.hangout_leave);
            mBadge = mFriends.getActionView().findViewById(R.id.badge);
            if(mHangout != null){
                if(!mHangout.getIsActive()){
                    mLeave.setVisible(false);
                }
                updateCount();
            }
        }
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if(item.getItemId() == R.id.hangout_leave){
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setMessage("Are you sure that you want to leave this hangout?");
            builder.setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialogInterface, int i) {}
            });
            builder.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialogInterface, int i) {

                    Call<Result> call = mInterface.leaveHangout(mHangout);
                    call.enqueue(new Callback<Result>() {
                        @Override
                        public void onResponse(@NonNull Call<Result> call,
                                               @NonNull retrofit2.Response<Result> response) {
                            Result result = response.body();
                            if(response.isSuccessful() && result != null){
                                switch(result.getResult()){
                                    case NetConstants.RESULT_SUCCESS:
                                        mHandler.deleteMessagesOfHangout(mHangout.getId());
                                        finish();
                                        break;
                                    case NetConstants.RESULT_NOT_ACTIVE:
                                        AlertDialog.Builder builder1 = new AlertDialog.Builder(HangoutActivity.this);
                                        builder1.setMessage(R.string.hangout_not_active);
                                        builder1.setNeutralButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                                            @Override
                                            public void onClick(DialogInterface dialogInterface, int i) {
                                                finish();
                                            }
                                        });
                                        break;
                                    case NetConstants.RESULT_NOT_THERE:
                                        Toast.makeText(HangoutActivity.this, R.string.already_left,
                                                Toast.LENGTH_SHORT).show();
                                        mHandler.deleteMessagesOfHangout(mHangout.getId());
                                        finish();
                                        break;
                                }
                            } else {
                                Toast.makeText(HangoutActivity.this, R.string.went_wrong,
                                        Toast.LENGTH_SHORT).show();
                            }
                        }

                        @Override
                        public void onFailure(@NonNull Call<Result> call, @NonNull Throwable t) {
                            Toast.makeText(HangoutActivity.this, R.string.no_connection,
                                    Toast.LENGTH_SHORT).show();
                        }
                    });
                }
            });
            builder.show();
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public Hangout getHangout() {
        return mHangout;
    }

    @Override
    public void onSetPlaceClick() {
        if(!(getCurrentFragment() instanceof MapFragment)){
            getSupportFragmentManager().beginTransaction()
                    .setCustomAnimations(R.anim.slide_in_top, R.anim.slide_out_bottom)
                    .replace(R.id.hangout_container, mMapFragment).commit();
        }
    }

    @Override
    public void onSetPlaceClick(final PlaceInfo info) {
        if(mRequestCode == CREATE_HANGOUT){
            Intent intent = new Intent(HangoutActivity.this, MainActivity.class);
            Bundle bundle = new Bundle();
            bundle.putSerializable("PLACE_INFO", info);
            intent.putExtras(bundle);
            setResult(RESULT_OK, intent);
            finish();
        } else {
            Hangout hangout = new Hangout();
            hangout.setId(mHangout.getId());
            hangout.setAddress(info.getAddress());
            hangout.setLatitude(info.getLatitude());
            hangout.setLongtitude(info.getLongtitude());
            hangout.setNameOfPlace(info.getName());
            hangout.setPlaceId(info.getId());
            hangout.getFriends().add(new User(Details.getProfileId(HangoutActivity.this)));
            Call<Result> call = mInterface.changeHangoutPlace(hangout);
            call.enqueue(new Callback<Result>() {
                @Override
                public void onResponse(@NonNull Call<Result> call,
                        @NonNull retrofit2.Response<Result> response) {
                    Result result = response.body();
                    if(response.isSuccessful() && result != null){
                        switch(result.getResult()){
                            case NetConstants.RESULT_SUCCESS:
                                Message message = new Message();
                                message.setType(Message.TYPE_PLACE);
                                message.setSenderName(Details.getUsername(HangoutActivity.this));
                                message.setStatus(Message.STATUS_SENT);
                                message.setHangoutTheme(mHangout.getTheme());
                                message.setHangoutId(mHangout.getId());
                                message.setSenderId(Details.getProfileId(HangoutActivity.this));
                                message.setContent(info.getName());
                                mHandler.addMessage(message);
                                mHangout.setPlaceId(info.getAddress());
                                mHangout.setLongtitude(info.getLongtitude());
                                mHangout.setLatitude(info.getLatitude());
                                mHangout.setNameOfPlace(info.getName());
                                mHangout.setAddress(info.getAddress());
                                mChatFragment.changePlace();
                                getSupportFragmentManager().beginTransaction()
                                        .setCustomAnimations(R.anim.slide_in_top, R.anim.slide_out_bottom)
                                        .replace(R.id.hangout_container, mChatFragment).commit();
                                break;
                            case NetConstants.RESULT_FAILED:
                                Toast.makeText(HangoutActivity.this, R.string.went_wrong,
                                        Toast.LENGTH_SHORT).show();
                                break;
                            case NetConstants.RESULT_NOT_FOUND:
                                AlertDialog.Builder builder1 = new AlertDialog.Builder(HangoutActivity.this);
                                builder1.setMessage(R.string.hangout_not_active);
                                builder1.setNeutralButton(android.R.string.ok,
                                        new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialogInterface, int i) {
                                        finish();
                                    }
                                });
                                break;
                        }
                    } else {
                        Toast.makeText(HangoutActivity.this, R.string.went_wrong,
                                Toast.LENGTH_SHORT).show();
                    }
                }

                @Override
                public void onFailure(@NonNull Call<Result> call, @NonNull Throwable t) {
                    Toast.makeText(HangoutActivity.this, R.string.no_connection,
                            Toast.LENGTH_SHORT).show();
                }
            });
        }
    }

    @Override
    public void onBackPressed() {
        if(mFriendsSheet.getState() != BottomSheetBehavior.STATE_HIDDEN){
            mFriendsSheet.setState(BottomSheetBehavior.STATE_HIDDEN);
        } else if(!(getCurrentFragment() instanceof ChatFragment) && mRequestCode != CREATE_HANGOUT){
            getSupportFragmentManager().beginTransaction()
                    .setCustomAnimations(R.anim.slide_in_bottom, R.anim.slide_out_top)
                    .replace(R.id.hangout_container, mChatFragment).commit();
            mFriends.setVisible(true);
            updateCount();
        } else if(getCurrentFragment() instanceof MapFragment){
            if(mMapFragment.getPlaceSheetState() != BottomSheetBehavior.STATE_HIDDEN){
                mMapFragment.hidePlaceSheet();
            } else if(mMapFragment.getNearbySheetState() != BottomSheetBehavior.STATE_COLLAPSED) {
                mMapFragment.collapseNearbySheet();
            } else if(mRequestCode == CREATE_HANGOUT) {
                new AlertDialog.Builder(this)
                        .setCancelable(false)
                        .setMessage("Are you sure that you want to cancel hangout?")
                        .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                setResult(RESULT_CANCELED);
                                finish();
                            }
                        })
                        .setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {}
                        }).show();
            }
        } else {
            finish();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        Log.i(TAG, "onRequestPermissionResult");
        if (requestCode == PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION) {
            if (grantResults.length <= 0) {
                Log.i(TAG, "User interaction was cancelled.");
            } else if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Log.i(TAG, "Permission granted, updates requested, starting location updates");
                startOnline();
            } else {
                showSnackbar(R.string.permission_location_rationale,
                        R.string.action_settings, new View.OnClickListener() {
                            @Override
                            public void onClick(View view) {
                                Intent intent = new Intent();
                                intent.setAction(
                                        Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                                Uri uri = Uri.fromParts("package",
                                        BuildConfig.APPLICATION_ID, null);
                                intent.setData(uri);
                                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                                startActivity(intent);
                            }
                        });
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case REQUEST_CHECK_SETTINGS:
                switch (resultCode) {
                    case RESULT_OK:
                        Log.i(TAG, "User agreed to make required location settings changes.");
                        startOnline();
                        break;
                    case RESULT_CANCELED:
                        Log.i(TAG, "User chose not to make required location settings changes.");
                        Intent goOffline = new Intent(this,
                                LocationDetectorService.class);
                        goOffline.putExtra(StatusUpdaterService.CODE, StatusUpdaterService.OFFLINE);
                        startService(goOffline);
                        break;
                }
                break;
        }
    }

    private void initFragments(){
        if(mChatFragment == null){
            mChatFragment = new ChatFragment();
        }
        if(mMapFragment == null){
            mMapFragment = new MapFragment();
        }
    }

    private void initUIWidgets(){
        mTheme = findViewById(R.id.hangout_theme);
        mFriendsSheet = BottomSheetBehavior.from((LinearLayout)findViewById(R.id.friends_sheet));
        mFriendsSheet.setState(BottomSheetBehavior.STATE_HIDDEN);
        mFriendsSheet.setBottomSheetCallback(new BottomSheetBehavior.BottomSheetCallback() {
            @Override
            public void onStateChanged(@NonNull View bottomSheet, int newState) {
                if(newState != BottomSheetBehavior.STATE_EXPANDED && newState != BottomSheetBehavior.STATE_DRAGGING){
                    updateCount();
                } else {
                    mBadge.setVisibility(View.INVISIBLE);
                }
            }
            @Override
            public void onSlide(@NonNull View bottomSheet, float slideOffset) {}
        });
        mRequestsLabel = findViewById(R.id.fs_requests_label);

        mFriendsList = new ArrayList<>();
        mRequestsList = new ArrayList<>();

        mFriendsAdapter = new FriendsAdapter(mFriendsList);
        mRequestsAdapter = new RequestsAdapter(mRequestsList, this);

        RecyclerView friendsList = findViewById(R.id.fs_list);
        friendsList.setLayoutManager(new LinearLayoutManager(this));
        friendsList.setAdapter(mFriendsAdapter);
        friendsList.setNestedScrollingEnabled(false);
        friendsList.setHasFixedSize(false);

        RecyclerView requestsList = findViewById(R.id.fs_requests);
        requestsList.setLayoutManager(new LinearLayoutManager(this));
        requestsList.setAdapter(mRequestsAdapter);
        requestsList.setNestedScrollingEnabled(false);
        requestsList.setHasFixedSize(false);
    }

    private Fragment getCurrentFragment(){
        if(findViewById(R.id.hangout_container) == null){
            return null;
        } else {
            return getSupportFragmentManager().findFragmentById(R.id.hangout_container);
        }
    }

    private boolean checkPermissions() {
        int permissionState = ActivityCompat.checkSelfPermission(this,
                android.Manifest.permission.ACCESS_FINE_LOCATION);
        return permissionState == PackageManager.PERMISSION_GRANTED;
    }

    private void startOnline(){
        if (checkPermissions()) {
            if(!Details.getStatus(HangoutActivity.this)){
                Intent intentService = new Intent(HangoutActivity.this,
                        LocationDetectorService.class);
                intentService.putExtra(LocationDetectorService.CODE, LocationDetectorService.START);
                startService(intentService);
            }
        } else if (!checkPermissions()) {
            requestPermissions(PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION);
        }
    }

    private void updateCount(){
        int numOfRequests = mHandler.getReceivedRequests(mHangout.getId()).size();
        if(mHangout.getHost().getId().equals(Details.getProfileId(this)) &&
                numOfRequests > 0 && mHangout.getIsActive()){
            mBadge.setText(String.valueOf(numOfRequests));
            mBadge.setVisibility(View.VISIBLE);
        } else {
            mBadge.setVisibility(View.INVISIBLE);
        }
    }

    private void showHideEmpty(){
        if(Details.getProfileId(this).equals(mHangout.getHost().getId())
                && mRequestsList.size() > 0){
            mRequestsLabel.setVisibility(View.VISIBLE);
        } else {
            mRequestsLabel.setVisibility(View.VISIBLE);
        }
    }

    private void requestPermissions(final int requestCode) {
        boolean shouldProvideRationale =
                ActivityCompat.shouldShowRequestPermissionRationale(this,
                        android.Manifest.permission.ACCESS_FINE_LOCATION);
        if (shouldProvideRationale) {
            Log.i(TAG, "Displaying permission rationale to provide additional context.");
            showSnackbar(R.string.permission_location_rationale,
                    android.R.string.ok, new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            // Request permission
                            ActivityCompat.requestPermissions(HangoutActivity.this,
                                    new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION},
                                    requestCode);
                        }
                    });
        } else {
            Log.i(TAG, "Requesting permission");
            ActivityCompat.requestPermissions(HangoutActivity.this,
                    new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION},
                    requestCode);
        }
    }

    private void showSnackbar(final int mainTextStringId, final int actionStringId,
                              View.OnClickListener listener) {
        Snackbar.make(
                findViewById(R.id.hangout_content),
                getString(mainTextStringId),
                Snackbar.LENGTH_INDEFINITE)
                .setAction(getString(actionStringId), listener).show();
    }

}
