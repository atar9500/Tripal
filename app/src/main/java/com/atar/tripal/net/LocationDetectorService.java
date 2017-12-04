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
import com.atar.tripal.db.Details;
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
    public static final int GET_AROUND_HANGOUTS = 21;

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

    @Override
    public void onCreate() {
        mFusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this);
        mSettingsClient = LocationServices.getSettingsClient(this);

        createLocationCallback();
        createLocationRequest();
        buildLocationSettingsRequest();

        super.onCreate();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if(intent != null){
            int i = intent.getIntExtra(CODE, 0);
            switch (i){
                case START:
                    startLocationUpdates();
                    break;
                case END:
                    stopLocationUpdates();
                    break;
            }
        } else {
            this.stopSelf();
        }
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
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
                            Details.saveStatus(LocationDetectorService.this, true);
                            mFusedLocationProviderClient.requestLocationUpdates(mLocationRequest,
                                    mLocationCallback, Looper.myLooper());
                            Intent broadcastIntent = new Intent(BROADCAST_IDENTIFIER_FOR_SERVICE_FINISHED_RESPONSE);
                            broadcastIntent.putExtra(CODE, START);
                            LocalBroadcastManager.getInstance(LocationDetectorService.this)
                                    .sendBroadcast(broadcastIntent);
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
                                        R.string.location_setting_unavailable, Toast.LENGTH_LONG).show();
                                break;
                        }
                    }
                });
    }

    /**
     * Removes location updates from the FusedLocationApi.
     */
    private void stopLocationUpdates() {
        mFusedLocationProviderClient.removeLocationUpdates(mLocationCallback)
                .addOnCompleteListener(new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(@NonNull Task<Void> task) {
                        Details.saveStatus(LocationDetectorService.this, false);
                        Intent broadcastIntent = new Intent
                                (BROADCAST_IDENTIFIER_FOR_SERVICE_FINISHED_RESPONSE);
                        broadcastIntent.putExtra(CODE, END);
                        LocalBroadcastManager.getInstance(LocationDetectorService.this)
                                .sendBroadcast(broadcastIntent);
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
                    Intent broadcastIntent = new Intent
                            (BROADCAST_IDENTIFIER_FOR_SERVICE_FINISHED_RESPONSE);
                    broadcastIntent.putExtra(CODE, LocationDetectorService.GET_AROUND_HANGOUTS);
                    LocalBroadcastManager.getInstance(LocationDetectorService.this)
                            .sendBroadcast(broadcastIntent);
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

}
