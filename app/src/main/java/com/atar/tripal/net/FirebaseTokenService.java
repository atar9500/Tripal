package com.atar.tripal.net;

import android.util.Log;

import com.atar.tripal.db.Details;
import com.google.firebase.iid.FirebaseInstanceId;
import com.google.firebase.iid.FirebaseInstanceIdService;


public class FirebaseTokenService extends FirebaseInstanceIdService {

    private static final String TAG = "FirebaseTokenService";

    public FirebaseTokenService() {}

    @Override
    public void onTokenRefresh() {

        String refreshedToken = FirebaseInstanceId.getInstance().getToken();
        Log.d(TAG, "Refreshed token: " + refreshedToken);
        Details.saveToken(refreshedToken, getApplicationContext());
    }

}
