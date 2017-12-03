package com.atar.tripal.net;

import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class ApiClient {

    public static Retrofit mRetrofit = null;

    public static Retrofit getAPIClient(){
        if(mRetrofit == null){
            mRetrofit = new Retrofit.Builder().baseUrl(NetConstants.HOST_NAME)
                    .addConverterFactory(GsonConverterFactory.create()).build();
        }
        return mRetrofit;
    }

}
