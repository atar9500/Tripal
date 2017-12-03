package com.atar.tripal.ui;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.IntentSender;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.PersistableBundle;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.design.widget.BottomSheetBehavior;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.design.widget.TabLayout;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.SwitchCompat;
import android.support.v7.widget.Toolbar;

import android.support.v4.view.ViewPager;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.atar.tripal.BuildConfig;
import com.atar.tripal.R;
import com.atar.tripal.adapters.MainTabsAdapter;
import com.atar.tripal.db.Details;
import com.atar.tripal.net.ApiClient;
import com.atar.tripal.net.ApiInterface;
import com.atar.tripal.net.LocationDetectorService;
import com.atar.tripal.net.NetConstants;
import com.atar.tripal.net.RequestsService;
import com.atar.tripal.net.StatusUpdaterService;
import com.atar.tripal.objects.User;
import com.atar.tripal.objects.Hangout;
import com.atar.tripal.objects.PlaceInfo;
import com.atar.tripal.objects.Result;
import com.google.android.gms.common.api.ResolvableApiException;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.rengwuxian.materialedittext.MaterialEditText;
import com.squareup.picasso.MemoryPolicy;
import com.squareup.picasso.Picasso;

import java.io.File;

import retrofit2.Call;
import retrofit2.Callback;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";
    private static final int PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION = 31;
    private static final int PERMISSIONS_REQUEST_WRITE_EXTERNAL = 32;
    private static final int REQUEST_CHECK_SETTINGS = 0x1;

    private class RequestsDetector extends BroadcastReceiver{

        @Override
        public void onReceive(Context context, Intent intent) {
            if(intent != null && intent.getStringExtra(NetConstants.RESULT)
                    .equals(NetConstants.RESULT_SUCCESS)){
                Intent intentService = new Intent(MainActivity.this,
                        LocationDetectorService.class);
                intentService.putExtra(LocationDetectorService.CODE,
                        LocationDetectorService.GET_MY_HANGOUTS);
                startService(intentService);
            }
        }
    }

    private class LocationDetectorReceiver extends BroadcastReceiver{

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
                                        MainActivity.this, REQUEST_CHECK_SETTINGS);
                            }
                        }
                    } catch (IntentSender.SendIntentException sie) {
                        Log.i(TAG, "PendingIntent unable to execute request.");
                    }
                    break;
                case LocationDetectorService.RENEW:
                    switchOnline();
                    break;
            }
        }
    }

    private class StatusUpdaterReceiver extends BroadcastReceiver{

        @Override
        public void onReceive(Context context, Intent intent) {
            int i = intent.getIntExtra(StatusUpdaterService.CODE, -1);
            String s = intent.getStringExtra(NetConstants.RESULT);
            if(i == StatusUpdaterService.ONLINE){
                switch (s){
                    case NetConstants.RESULT_SUCCESS:
                        Details.saveStatus(MainActivity.this, true);
                        Intent startLocating = new Intent(MainActivity.this,
                                LocationDetectorService.class);
                        startLocating.putExtra(LocationDetectorService.CODE,
                                LocationDetectorService.START);
                        startService(startLocating);
                        mStatus.setAnimation(AnimationUtils.loadAnimation(MainActivity.this,
                                R.anim.fade_in));
                        mStatus.setText(R.string.available);
                        mAddNewHangout.show();
                        mAddNewHangout.setClickable(true);
                        break;
                    case NetConstants.RESULT_FAILED:
                        Snackbar.make(findViewById(R.id.main_activity),
                                R.string.could_not_connect,
                                Snackbar.LENGTH_LONG).show();
                        mLocationEnabler.setChecked(false);
                        mStatus.setAnimation(AnimationUtils.loadAnimation(MainActivity.this,
                                R.anim.fade_in));
                        mStatus.setText(R.string.offline);
                        mAddNewHangout.hide();
                        mAddNewHangout.setClickable(false);
                        break;
                }
            } else if(i == StatusUpdaterService.OFFLINE) {
                switch (s){
                    case NetConstants.RESULT_SUCCESS:
                        Intent startLocating = new Intent(MainActivity.this,
                                LocationDetectorService.class);
                        startLocating.putExtra(LocationDetectorService.CODE,
                                LocationDetectorService.END);
                        startService(startLocating);
                        Toast.makeText(context, "NOW OFFLINE", Toast.LENGTH_SHORT).show();
                        break;
                    case NetConstants.RESULT_FAILED:
                        Toast.makeText(MainActivity.this, R.string.could_not_connect,
                                Toast.LENGTH_SHORT).show();
                        break;
                }
                mStatus.setAnimation(AnimationUtils.loadAnimation(MainActivity.this,
                        R.anim.fade_in));
                mStatus.setText(R.string.offline);
                mAddNewHangout.hide();
                mAddNewHangout.setClickable(false);
            }
        }
    }

    /**
     * UI WIDGETS
     */
    private SwitchCompat mLocationEnabler;
    private TextView mStatus;
    private BottomSheetBehavior mNewHangoutSheet;
    private MaterialEditText mSetTheme;
    private ImageView mProfilePic;
    private FloatingActionButton mAddNewHangout;
    private Hangout mNewHangout;

    /**
     * Data
     */
    private ApiInterface mInterface;

    /**
     * Broadcast Receivers
     */
    private LocationDetectorReceiver mLocationDetectorReceiver;
    private StatusUpdaterReceiver mStatusUpdaterReceiver;
    private RequestsDetector mRequestsDetector;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mInterface = ApiClient.getAPIClient().create(ApiInterface.class);

        initUIWidgets();
    }

    @Override
    public void onResume() {
        super.onResume();

        mLocationDetectorReceiver = new LocationDetectorReceiver();
        LocalBroadcastManager.getInstance(this).registerReceiver
                (mLocationDetectorReceiver, new IntentFilter(LocationDetectorService
                        .BROADCAST_IDENTIFIER_FOR_SERVICE_FINISHED_RESPONSE));

        mStatusUpdaterReceiver = new StatusUpdaterReceiver();
        LocalBroadcastManager.getInstance(this).registerReceiver
                (mStatusUpdaterReceiver, new IntentFilter(StatusUpdaterService
                        .BROADCAST_IDENTIFIER_FOR_SERVICE_FINISHED_RESPONSE));

        mRequestsDetector = new RequestsDetector();
        LocalBroadcastManager.getInstance(this).registerReceiver
                (mRequestsDetector, new IntentFilter(RequestsService
                        .BROADCAST_IDENTIFIER_FOR_SERVICE_FINISHED_RESPONSE));

        if (!checkPermissionLocation()) {
            requestPermissions(PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION);
        }

        String path = Details.getPhotoPath(this);
        if(path != null){
            Picasso.with(this).load(new File(path)).memoryPolicy(MemoryPolicy.NO_CACHE)
                    .memoryPolicy(MemoryPolicy.NO_STORE)
                    .resize(150, 150)
                    .into(mProfilePic, new com.squareup.picasso.Callback() {
                @Override
                public void onSuccess() {}
                @Override
                public void onError() {
                    Details.savePhotoPath(null, MainActivity.this);
                    getProfilePhoto();
                }
            });
        } else {
            getProfilePhoto();
        }
    }

    @Override
    public void onSaveInstanceState(Bundle outState, PersistableBundle outPersistentState) {
        super.onSaveInstanceState(outState, outPersistentState);
    }

    @Override
    protected void onPause() {
        super.onPause();
        LocalBroadcastManager.getInstance(this).unregisterReceiver(mLocationDetectorReceiver);
        LocalBroadcastManager.getInstance(this).unregisterReceiver(mStatusUpdaterReceiver);
        LocalBroadcastManager.getInstance(this).unregisterReceiver(mRequestsDetector);
    }

    @Override
    public void onBackPressed() {
        if(mNewHangoutSheet.getState() != BottomSheetBehavior.STATE_HIDDEN){
            mNewHangoutSheet.setState(BottomSheetBehavior.STATE_HIDDEN);
        } else {
            super.onBackPressed();
        }
    }

    /**
     * Callback received when a permissions request has been completed.
     */
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        Log.i(TAG, "onRequestPermissionResult");
        if (requestCode == PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION) {
            if (grantResults.length <= 0) {
                Log.i(TAG, "User interaction was cancelled.");
                mLocationEnabler.setChecked(false);
            } else if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Log.i(TAG, "Permission granted, updates requested, starting location updates");
                if(mLocationEnabler.isChecked()){
                    switchOnline();
                }
            } else {
                mLocationEnabler.setChecked(false);
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
        } else if(requestCode == PERMISSIONS_REQUEST_WRITE_EXTERNAL){
            if (grantResults.length <= 0) {
                Log.i(TAG, "User interaction was cancelled.");
            } else if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Log.i(TAG, "Permission granted, updates requested, starting location updates");
                if(mLocationEnabler.isChecked()){
                    switchOnline();
                }
            } else {
                mLocationEnabler.setChecked(false);
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
                        Intent intentGPS = new Intent(MainActivity.this,
                                LocationDetectorService.class);
                        intentGPS.putExtra(LocationDetectorService.CODE,
                                LocationDetectorService.START);
                        startService(intentGPS);
                        break;
                    case RESULT_CANCELED:
                        Log.i(TAG, "User chose not to make required " +
                                "location settings changes.");
                        mLocationEnabler.setChecked(false);
                        break;
                }
                break;
            case HangoutActivity.CREATE_HANGOUT:
                switch (resultCode){
                    case RESULT_OK:
                        Log.i(TAG, "User set up a place for his hangout.");
                        if(data.getExtras() != null){
                            PlaceInfo info = (PlaceInfo)data.getExtras()
                                    .getSerializable("PLACE_INFO");
                            mNewHangout.updatePlace(info);
                            sendHangoutToServer();
                        }
                        break;
                    case RESULT_CANCELED:
                        Log.i(TAG, "User did not set a hangout.");
                        break;
                }
                break;
        }
    }

    private void initUIWidgets(){
        setSupportActionBar((Toolbar) findViewById(R.id.ma_toolbar));
        if(getSupportActionBar() != null){
            getSupportActionBar().setDisplayShowTitleEnabled(false);
        }

        mStatus = findViewById(R.id.ma_status);
        if(Details.getStatus(this)){
            mStatus.setText(R.string.available);
        }

        MainTabsAdapter adapter = new MainTabsAdapter(getSupportFragmentManager(), this);
        ViewPager viewPager = findViewById(R.id.la_container);
        viewPager.setAdapter(adapter);
        TabLayout tabLayout = findViewById(R.id.ma_tabs);
        tabLayout.setupWithViewPager(viewPager);


        mAddNewHangout = findViewById(R.id.ma_hangout);
        mAddNewHangout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(mNewHangout == null){
                    mNewHangout = new Hangout();
                }
                mNewHangout.setHost(new User(Details.getProfileId(MainActivity.this)));
                mNewHangoutSheet.setState(BottomSheetBehavior.STATE_EXPANDED);
            }
        });

        if(!Details.getStatus(this)){
            mAddNewHangout.hide();
            mAddNewHangout.setClickable(false);
        }

        mNewHangoutSheet = BottomSheetBehavior.from((LinearLayout)findViewById(R.id.nh_sheet));
        mNewHangoutSheet.setState(BottomSheetBehavior.STATE_HIDDEN);
        mNewHangoutSheet.setBottomSheetCallback(new BottomSheetBehavior.BottomSheetCallback() {
            @Override
            public void onStateChanged(@NonNull View bottomSheet, int newState) {
                if(newState == BottomSheetBehavior.STATE_HIDDEN){
                    mSetTheme.setText(null);
                }
            }
            @Override
            public void onSlide(@NonNull View bottomSheet, float slideOffset) {}
        });
        mSetTheme = findViewById(R.id.nh_theme);
        Button dismiss = findViewById(R.id.nh_cancel);
        dismiss.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mNewHangoutSheet.setState(BottomSheetBehavior.STATE_HIDDEN);
            }
        });
        Button setHangout = findViewById(R.id.nh_place);
        setHangout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(mSetTheme.isCharactersCountValid() &&
                        !mSetTheme.getText().toString().equals("")){
                    mNewHangout.setTheme(mSetTheme.getText().toString());
                    Intent intent = new Intent(MainActivity.this,
                            HangoutActivity.class);
                    intent.putExtra(HangoutActivity.REQUEST_CODE,
                            HangoutActivity.CREATE_HANGOUT);
                    startActivityForResult(intent, HangoutActivity.CREATE_HANGOUT);
                    mNewHangoutSheet.setState(BottomSheetBehavior.STATE_HIDDEN);
                }
            }
        });

        mProfilePic = findViewById(R.id.ma_profile_pic);
        mProfilePic.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(MainActivity.this, ProfileActivity.class);
                intent.putExtra(ProfileActivity.CODE, ProfileActivity.PROFILE_CODE);
                startActivity(intent);
            }
        });
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
                            ActivityCompat.requestPermissions(MainActivity.this,
                                    new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION},
                                    requestCode);
                        }
                    });
        } else {
            Log.i(TAG, "Requesting permission");
            ActivityCompat.requestPermissions(MainActivity.this,
                    new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION},
                    requestCode);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater menuInflater = getMenuInflater();
        menuInflater.inflate(R.menu.menu_main, menu);
        MenuItem switchItem = menu.findItem(R.id.main_toolbar_switch);
        switchItem.setActionView(R.layout.location_switcher);
        mLocationEnabler = menu.findItem(R.id.main_toolbar_switch).getActionView()
                .findViewById(R.id.location_switcher);

        mLocationEnabler.setChecked(Details.getStatus(this));

        mLocationEnabler.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                if(b){
                    switchOnline();
                } else {
                    switchOffline();
                }
            }
        });

        mLocationEnabler.setChecked(Details.getStatus(MainActivity.this));
        if(!Details.getStatus(MainActivity.this)){
            mAddNewHangout.hide();
            mAddNewHangout.setClickable(false);
        }

        return true;
    }

    private void showSnackbar(final int mainTextStringId, final int actionStringId,
                              View.OnClickListener listener) {
        Snackbar.make(
                findViewById(R.id.main_activity),
                getString(mainTextStringId),
                Snackbar.LENGTH_INDEFINITE)
                .setAction(getString(actionStringId), listener).show();
    }

    private void switchOnline(){
        if(!Details.getStatus(this)){
            if (checkPermissionLocation()) {
                Intent intentService = new Intent(MainActivity.this,
                        StatusUpdaterService.class);
                intentService.putExtra(StatusUpdaterService.CODE, StatusUpdaterService.ONLINE);
                startService(intentService);
            } else if (!checkPermissionLocation()) {
                requestPermissions(PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION);
            }
        }
    }

    private void switchOffline(){
        if(Details.getStatus(getApplicationContext())){
            Intent goOffline = new Intent(MainActivity.this,
                    StatusUpdaterService.class);
            goOffline.putExtra(StatusUpdaterService.CODE, StatusUpdaterService.OFFLINE);
            startService(goOffline);
            Details.saveStatus(getApplicationContext(), false);
        }
        Intent stopLocating = new Intent(MainActivity.this,
                LocationDetectorService.class);
        stopLocating.putExtra(StatusUpdaterService.CODE, LocationDetectorService.END);
        startService(stopLocating);
        Details.saveStatus(getApplicationContext(), false);
    }

    private void sendHangoutToServer(){
        mNewHangout.getHost().setId(Details.getProfileId(this));
        Call<Result> call = mInterface.sendHangout(mNewHangout);
        call.enqueue(new Callback<Result>() {
            @Override
            public void onResponse(@NonNull Call<Result> call, @NonNull retrofit2.Response<Result> response) {
                Result result = response.body();
                if(response.isSuccessful() && result != null){
                    if(result.getResult().equals("SUCCESS")){
                        Toast.makeText(MainActivity.this, "Hangout Added",
                                Toast.LENGTH_SHORT).show();
                        Intent intentService = new Intent(MainActivity.this,
                                LocationDetectorService.class);
                        intentService.putExtra(LocationDetectorService.CODE,
                                LocationDetectorService.GET_MY_HANGOUTS);
                        startService(intentService);
                    } else {
                        Toast.makeText(MainActivity.this, R.string.went_wrong,
                                Toast.LENGTH_SHORT).show();
                    }
                } else {
                    Toast.makeText(MainActivity.this, R.string.went_wrong,
                            Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(@NonNull Call<Result> call, @NonNull Throwable t) {
                Toast.makeText(MainActivity.this, R.string.no_connection, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private boolean checkPermissionLocation() {
        int permissionState = ActivityCompat.checkSelfPermission(this,
                android.Manifest.permission.ACCESS_FINE_LOCATION);
        return permissionState == PackageManager.PERMISSION_GRANTED;
    }

    private void getProfilePhoto(){
        StorageReference storageRef = FirebaseStorage.getInstance().getReference();
        StorageReference profilePhotosRef = storageRef.child("users/" + Details
                .getProfileId(this) + "/profilePicture.jpg");
        profilePhotosRef.getDownloadUrl().addOnSuccessListener(new OnSuccessListener<Uri>() {
            @Override
            public void onSuccess(Uri uri) {
                Picasso.with(MainActivity.this).load(uri).into(mProfilePic);
            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                e.printStackTrace();
            }
        });
    }

}
