package com.atar.tripal.net;

import android.content.Context;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.Volley;

public class NetworkRequestQueue {

    private static NetworkRequestQueue mInstance;
    private RequestQueue mRequestQueue;
    private static Context mContext;

    private NetworkRequestQueue(Context context){
        mContext = context;
        mRequestQueue = getRequestQueue();
    }

    public RequestQueue getRequestQueue(){
        if(mRequestQueue == null){
            mRequestQueue = Volley.newRequestQueue(mContext.getApplicationContext());
        }
        return mRequestQueue;
    }

    public static synchronized NetworkRequestQueue getInstance(Context context){
        if(mInstance == null){
            mInstance = new NetworkRequestQueue(context);
        }
        return mInstance;
    }

    public void addToRequestQueue(Request request){
        mRequestQueue.add(request);
    }

}
