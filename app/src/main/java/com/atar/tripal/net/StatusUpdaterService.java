package com.atar.tripal.net;

import android.app.IntentService;
import android.content.Intent;
import android.support.annotation.Nullable;
import android.support.v4.content.LocalBroadcastManager;

import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.atar.tripal.db.Details;

import org.json.JSONException;
import org.json.JSONObject;

public class StatusUpdaterService extends IntentService {

    private static final String TAG = "StatusUpdaterService";

    public static final String BROADCAST_IDENTIFIER_FOR_SERVICE_FINISHED_RESPONSE =
            "com.atar.tripal.net.StatusUpdaterService.IdentifierForStatusUpdaterService";
    public static final String CODE = "code";

    public static final int ONLINE = 1;
    public static final int OFFLINE = 0;

    public StatusUpdaterService() {
        super(TAG);
    }

    @Override
    protected void onHandleIntent(@Nullable Intent intent) {
        if(intent != null){

            final int i = intent.getIntExtra(CODE, OFFLINE);

            JsonObjectRequest request = new JsonObjectRequest(Request.Method.GET,
                    NetConstants.HOST_NAME + "update_status?id=" +
                            Details.getProfileId(this) + "&online=" + i,
                    null, new Response.Listener<JSONObject>() {
                @Override
                public void onResponse(JSONObject response) {
                    try{
                        if(response.getString(NetConstants.RESULT).equals(NetConstants.RESULT_SUCCESS)){
                            Intent broadcastIntent = new Intent
                                    (BROADCAST_IDENTIFIER_FOR_SERVICE_FINISHED_RESPONSE);
                            broadcastIntent.putExtra(NetConstants.RESULT, NetConstants.RESULT_SUCCESS);
                            broadcastIntent.putExtra(CODE, i);
                            LocalBroadcastManager.getInstance(StatusUpdaterService.this)
                                    .sendBroadcast(broadcastIntent);
                        } else {
                            onFailed(i);
                        }
                    } catch (JSONException e){
                        e.printStackTrace();
                        onFailed(i);
                    }
                }
            }, new Response.ErrorListener() {
                @Override
                public void onErrorResponse(VolleyError error) {
                    onFailed(i);
                }
            });
            NetworkRequestQueue.getInstance(this).addToRequestQueue(request);
        } else {
            onFailed(OFFLINE);
        }
    }

    private void onFailed(int i){
        Details.saveStatus(StatusUpdaterService.this, false);
        Intent broadcastIntent = new Intent
                (BROADCAST_IDENTIFIER_FOR_SERVICE_FINISHED_RESPONSE);
        broadcastIntent.putExtra(NetConstants.RESULT, NetConstants.RESULT_FAILED);
        broadcastIntent.putExtra(CODE, i);
        LocalBroadcastManager.getInstance(StatusUpdaterService.this)
                .sendBroadcast(broadcastIntent);
    }

}
