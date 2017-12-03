package com.atar.tripal.net;

import com.atar.tripal.objects.User;
import com.atar.tripal.objects.Hangout;
import com.atar.tripal.objects.Hangouts;
import com.atar.tripal.objects.Message;
import com.atar.tripal.objects.Result;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.Query;

public interface ApiInterface {

    // Working
    @GET("my_hangouts")
    Call<Hangouts> getMyHangouts(@Query(NetConstants.ID) String id);

    // Working
    @GET("around_me")
    Call<Hangouts> getHangoutsAroundMe(@Query("lat") double lat,
            @Query("lng") double lng, @Query(NetConstants.ID) String id);

    // Working
    @GET("hangout")
    Call<Hangout> getHangout(@Query(NetConstants.ID) long id);

    // Working
    @POST("enter_google")
    Call<User> signWithGoogle(@Body User user);

    // Working
    @POST("add_hangout")
    Call<Result> sendHangout(@Body Hangout hangout);

    // Working
    @POST("register")
    Call<Result> registerUser(@Body User user);

    // Working
    @POST("enter")
    Call<User> signWithEmail(@Body User user);

    // Working
    @POST("set_place")
    Call<Result> changeHangoutPlace(@Body Hangout hangout);

    // Working
    @POST("request_to_hangout")
    Call<Result> sendRequestToJoin(@Body Hangout hangout);

    // Working
    @POST("leave_hangout")
    Call<Result> leaveHangout(@Body Hangout hangout);

    // Working
    @POST("add_to_hangout")
    Call<Result> addToHangout(@Body Hangout hangout);

    // Working
    @POST("send")
    Call<Result> sendMessage(@Body Message message);

    // Working
    @POST("update_user")
    Call<Result> updateUserInfo(@Body User user);
}
