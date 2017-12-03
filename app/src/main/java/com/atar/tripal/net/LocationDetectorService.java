package com.atar.tripal.net;

import android.app.Service;
import android.content.Intent;
import android.location.Location;
import android.os.Bundle;
import android.os.IBinder;
import android.os.Looper;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.widget.Toast;

import com.atar.tripal.R;
import com.atar.tripal.db.DBConstants;
import com.atar.tripal.db.DBHandler;
import com.atar.tripal.db.Details;
import com.atar.tripal.objects.User;
import com.atar.tripal.objects.Hangout;
import com.atar.tripal.objects.Hangouts;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.LocationSettingsRequest;
import com.google.android.gms.location.LocationSettingsResponse;
import com.google.android.gms.location.LocationSettingsStatusCodes;
import com.google.android.gms.location.SettingsClient;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;

public class LocationDetectorService extends Service {

    private static final String TAG = "LocationDetectorService";
    private static final long UPDATE_INTERVAL = 1000*60*5;
    private static final long FASTEST_UPDATE_INTERVAL = UPDATE_INTERVAL / 2;

    public static final String BROADCAST_IDENTIFIER_FOR_SERVICE_FINISHED_RESPONSE =
            "com.atar.tripal.net.LocationDetectorService.IdentifierForLocationDetectorService";
    public static final String CODE = "code";

    public static final int GPS_IS_NEEDED = 18;
    public static final int START = 19;
    public static final int END = 20;
    public static final int RENEW = 21;
    public static final int GET_AROUND_HANGOUTS = 22;
    public static final int START_GETTING_HANGOUTS = 23;
    public static final int GET_MY_HANGOUTS = 24;
    public static final int REFRESH_HANGOUT = 25;

    public static final int HANGOUT_TIME = 1000*60*60*6;

    /**
     * Provides access to the Fused Location Provider API.
     */
    private FusedLocationProviderClient mFusedLocationProviderClient;

    /**
     * Provides access to the Location Settings API.
     */
    private SettingsClient mSettingsClient;

    /**
     * Stores parameters for requests to the FusedLocationProviderApi.
     */
    private LocationRequest mLocationRequest;

    /**
     * Stores the types of location services the client is interested in using. Used for checking
     * settings to determine if the device has optimal location settings.
     */
    private LocationSettingsRequest mLocationSettingsRequest;

    /**
     * Callback for Location events.
     */
    private LocationCallback mLocationCallback;

    /**
     * Current Location.
     */
    private Location mCurrentLocation;

    private ApiInterface mInterface;

    private DBHandler mHandler;

    @Override
    public void onCreate() {
        mFusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this);
        mSettingsClient = LocationServices.getSettingsClient(this);

        mInterface = ApiClient.getAPIClient().create(ApiInterface.class);

        mHandler = new DBHandler(this);

        createLocationCallback();
        createLocationRequest();
        buildLocationSettingsRequest();

        super.onCreate();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if(intent != null){
            int i = intent.getIntExtra(CODE, 0);
            final Intent broadcastIntent = new Intent
                    (BROADCAST_IDENTIFIER_FOR_SERVICE_FINISHED_RESPONSE);
            switch (i){
                case START:
                    startLocationUpdates();
                    broadcastIntent.putExtra(CODE, START);
                    LocalBroadcastManager.getInstance(LocationDetectorService.this)
                            .sendBroadcast(broadcastIntent);
                    break;
                case END:
                    stopLocationUpdates();
                    broadcastIntent.putExtra(CODE, END);
                    LocalBroadcastManager.getInstance(LocationDetectorService.this)
                            .sendBroadcast(broadcastIntent);
                    break;
                case GET_MY_HANGOUTS:
                    Call<Hangouts> call = mInterface.getMyHangouts
                            (Details.getProfileId(this));
                    call.enqueue(new Callback<Hangouts>() {
                        @Override
                        public void onResponse(@NonNull Call<Hangouts> call,
                                @NonNull retrofit2.Response<Hangouts> response) {
                            Hangouts set = response.body();
                            if(response.isSuccessful() && set != null){
                                Intent broadcastIntent = new Intent
                                        (BROADCAST_IDENTIFIER_FOR_SERVICE_FINISHED_RESPONSE);
                                Bundle bundle = new Bundle();

                                ArrayList<Hangout> pastHangouts = new ArrayList<>();
                                ArrayList<Hangout> activeHangouts = new ArrayList<>();

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
                                            activeHangouts.add(hangout);
                                        } else {
                                            pastHangouts.add(hangout);
                                        }
                                    }
                                }

                                bundle.putSerializable("past_hangouts", pastHangouts);
                                bundle.putSerializable("active_hangouts", activeHangouts);
                                broadcastIntent.putExtras(bundle);
                                broadcastIntent.putExtra(CODE, GET_MY_HANGOUTS);
                                broadcastIntent.putExtra(NetConstants.RESULT, NetConstants.RESULT_SUCCESS);
                                LocalBroadcastManager.getInstance(LocationDetectorService.this)
                                        .sendBroadcast(broadcastIntent);
                            } else {
                                Intent broadcastIntent = new Intent
                                        (BROADCAST_IDENTIFIER_FOR_SERVICE_FINISHED_RESPONSE);
                                broadcastIntent.putExtra(CODE, GET_MY_HANGOUTS);
                                broadcastIntent.putExtra(NetConstants.RESULT, NetConstants.RESULT_FAILED);
                                LocalBroadcastManager.getInstance(LocationDetectorService.this)
                                        .sendBroadcast(broadcastIntent);
                            }
                        }

                        @Override
                        public void onFailure(@NonNull Call<Hangouts> call, @NonNull Throwable t) {
                            Intent broadcastIntent = new Intent
                                    (BROADCAST_IDENTIFIER_FOR_SERVICE_FINISHED_RESPONSE);
                            broadcastIntent.putExtra(CODE, GET_MY_HANGOUTS);
                            broadcastIntent.putExtra(NetConstants.RESULT, NetConstants.RESULT_FAILED);
                            LocalBroadcastManager.getInstance(LocationDetectorService.this)
                                    .sendBroadcast(broadcastIntent);
                        }
                    });
                    break;
                case REFRESH_HANGOUT:
                    Call<Hangout> hangoutCall = mInterface.getHangout(intent.getLongExtra
                            (DBConstants.COL_HANGOUT_ID, -1));
                    hangoutCall.enqueue(new Callback<Hangout>() {
                        @Override
                        public void onResponse(@NonNull Call<Hangout> call,
                                @NonNull retrofit2.Response<Hangout> response) {
                            Intent broadcastIntent = new Intent
                                    (BROADCAST_IDENTIFIER_FOR_SERVICE_FINISHED_RESPONSE);
                            Hangout hangout = response.body();
                            if(response.isSuccessful() && hangout != null){
                                List<User> users = hangout.getFriends();
                                if(users.size() > 0){
                                    users.remove(users.size() - 1);
                                }
                                hangout.setIsActive(hangout.getTimestamp() + HANGOUT_TIME
                                        > System.currentTimeMillis());
                                Bundle bundle = new Bundle();
                                bundle.putSerializable("hangout", hangout);
                                broadcastIntent.putExtras(bundle);
                                broadcastIntent.putExtra(NetConstants.RESULT, NetConstants.RESULT_SUCCESS);
                                broadcastIntent.putExtra(CODE, REFRESH_HANGOUT);
                                LocalBroadcastManager.getInstance(LocationDetectorService.this)
                                        .sendBroadcast(broadcastIntent);
                            } else {
                                broadcastIntent.putExtra(NetConstants.RESULT, NetConstants.RESULT_FAILED);
                                broadcastIntent.putExtra(CODE, REFRESH_HANGOUT);
                                LocalBroadcastManager.getInstance(LocationDetectorService.this)
                                        .sendBroadcast(broadcastIntent);
                            }
                        }

                        @Override
                        public void onFailure(@NonNull Call<Hangout> call, @NonNull Throwable t) {
                            t.printStackTrace();
                            Toast.makeText(LocationDetectorService.this, R.string.no_connection,
                                    Toast.LENGTH_SHORT).show();
                        }
                    });
                    break;
                case GET_AROUND_HANGOUTS:
                    getAroundHangouts();
                    break;
            }
        } else {
            this.stopSelf();
        }
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        Details.saveStatus(LocationDetectorService.this, false);
        stopLocationUpdates();
        super.onDestroy();
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    /**
     * Requests location updates from the FusedLocationApi. Note: we don't call this unless location
     * runtime permission has been granted.
     */
    private void startLocationUpdates() {
        mSettingsClient.checkLocationSettings(mLocationSettingsRequest)
                .addOnSuccessListener(new OnSuccessListener<LocationSettingsResponse>() {
                    @Override
                    public void onSuccess(LocationSettingsResponse locationSettingsResponse) {
                        Log.i(TAG, "All location settings are satisfied.");
                        try{
                            mFusedLocationProviderClient.requestLocationUpdates(mLocationRequest,
                                    mLocationCallback, Looper.myLooper());
                        } catch (SecurityException e){
                            e.printStackTrace();
                        }
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        int statusCode = ((ApiException) e).getStatusCode();
                        switch (statusCode) {
                            case LocationSettingsStatusCodes.RESOLUTION_REQUIRED:
                                Log.i(TAG, "Location settings are not satisfied. Attempting to upgrade " +
                                        "location settings");
                                Intent broadcastIntent = new Intent
                                        (BROADCAST_IDENTIFIER_FOR_SERVICE_FINISHED_RESPONSE);
                                broadcastIntent.putExtra(CODE, LocationDetectorService.GPS_IS_NEEDED);
                                Bundle bundle = new Bundle();
                                bundle.putSerializable("E", e);
                                broadcastIntent.putExtras(bundle);
                                LocalBroadcastManager.getInstance(LocationDetectorService.this)
                                        .sendBroadcast(broadcastIntent);
                                break;
                            case LocationSettingsStatusCodes.SETTINGS_CHANGE_UNAVAILABLE:
                                Toast.makeText(LocationDetectorService.this,
                                        "Please enable Location Services for Tripal " +
                                                "in order to start using the app", Toast.LENGTH_LONG).show();
                                break;
                        }
                    }
                });
    }

    /**
     * Removes location updates from the FusedLocationApi.
     */
    private void stopLocationUpdates() {
        if (!Details.getStatus(this)) {
            Log.d(TAG, "stopLocationUpdates: updates never requested, no-op.");
            return;
        }
        mFusedLocationProviderClient.removeLocationUpdates(mLocationCallback)
                .addOnCompleteListener(new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(@NonNull Task<Void> task) {
                        Toast.makeText(LocationDetectorService.this, "Location " +
                                "Detection Stopped", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    /**
     * Creates a callback for receiving location events.
     */
    private void createLocationCallback() {
        if(mLocationCallback == null){
            mLocationCallback = new LocationCallback() {
                @Override
                public void onLocationResult(LocationResult locationResult) {
                    super.onLocationResult(locationResult);
                    mCurrentLocation = locationResult.getLastLocation();
                    Details.saveLocation(mCurrentLocation, LocationDetectorService.this);
                    getAroundHangouts();
                }
            };
        }
    }

    /**
     * Uses a {@link com.google.android.gms.location.LocationSettingsRequest.Builder} to build
     * a {@link com.google.android.gms.location.LocationSettingsRequest} that is used for checking
     * if a device has the needed location settings.
     */
    private void buildLocationSettingsRequest() {
        if(mLocationSettingsRequest == null){
            LocationSettingsRequest.Builder builder = new LocationSettingsRequest.Builder();
            builder.addLocationRequest(mLocationRequest);
            mLocationSettingsRequest = builder.build();
        }
    }

    private void createLocationRequest() {
        if(mLocationRequest == null){
            mLocationRequest = new LocationRequest();
            mLocationRequest.setInterval(UPDATE_INTERVAL);
            mLocationRequest.setFastestInterval(FASTEST_UPDATE_INTERVAL);
            mLocationRequest.setPriority(LocationRequest.PRIORITY_BALANCED_POWER_ACCURACY);
        }
    }

    private synchronized void getAroundHangouts(){
        Call<Hangouts> call = mInterface.getHangoutsAroundMe(mCurrentLocation.getLatitude(),
                mCurrentLocation.getLongitude(), Details.getProfileId(LocationDetectorService.this));
        call.enqueue(new Callback<Hangouts>() {
            @Override
            public void onResponse(@NonNull Call<Hangouts> call,
                                   @NonNull retrofit2.Response<Hangouts> response) {
                Hangouts hangouts = response.body();
                if(response.isSuccessful() && hangouts != null){
                    if(hangouts.getResult().equals(NetConstants.RESULT_SUCCESS)){
                        List<Hangout> hangoutList = hangouts.getHangouts();
                        Intent broadcastIntent = new Intent
                                (BROADCAST_IDENTIFIER_FOR_SERVICE_FINISHED_RESPONSE);
                        if(hangoutList != null){
                            hangoutList.remove(hangoutList.size() - 1);
                            Collections.reverse(hangoutList);
                            for(int i = 0; i < hangoutList.size(); i++){
                                Hangout hangout = hangoutList.get(i);
                                hangout.setRequestSent(mHandler.getSentRequests(hangout.getId()) > 0);
                                Bundle bundle = new Bundle();
                                bundle.putSerializable("hangout", hangout);
                                broadcastIntent.putExtras(bundle);
                                if(i > 0){
                                    broadcastIntent.putExtra(CODE, GET_AROUND_HANGOUTS);
                                } else {
                                    broadcastIntent.putExtra(CODE, START_GETTING_HANGOUTS);
                                }
                                LocalBroadcastManager.getInstance(LocationDetectorService.this)
                                        .sendBroadcast(broadcastIntent);
                            }
                        } else {
                            broadcastIntent.putExtra(CODE, START_GETTING_HANGOUTS);
                            LocalBroadcastManager.getInstance(LocationDetectorService.this)
                                    .sendBroadcast(broadcastIntent);
                        }
                    } else if(hangouts.getResult().equals(NetConstants.RESULT_NOT_ACTIVE)) {
                        stopLocationUpdates();
                        Details.saveStatus(LocationDetectorService.this, false);
                        Intent broadcastIntent = new Intent
                                (BROADCAST_IDENTIFIER_FOR_SERVICE_FINISHED_RESPONSE);
                        broadcastIntent.putExtra(CODE, RENEW);
                        LocalBroadcastManager.getInstance(LocationDetectorService.this)
                                .sendBroadcast(broadcastIntent);
                    }
                } else {
                    Toast.makeText(LocationDetectorService.this,
                            R.string.went_wrong, Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(@NonNull Call<Hangouts> call, @NonNull Throwable t) {
                t.printStackTrace();
                Toast.makeText(LocationDetectorService.this,
                        R.string.no_connection, Toast.LENGTH_SHORT).show();
            }
        });
    }

}
