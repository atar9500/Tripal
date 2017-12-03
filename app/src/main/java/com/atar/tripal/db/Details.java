package com.atar.tripal.db;

import android.content.Context;
import android.content.SharedPreferences;
import android.location.Location;

import com.atar.tripal.net.NetConstants;
import com.atar.tripal.objects.User;
import com.google.android.gms.maps.model.LatLng;

public class Details {

    private static final String PROFILE = "profile_menu";

    private static final String USERNAME = "username";
    private static final String IS_ONLINE = "is_online";
    private static final String PROFILE_ID = "id";
    private static final String TOKEN = "token";
    private static final String ABOUT_ME = "about_me";
    private static final String INTERESTS = "interests";
    private static final String BOOKS_MOVIES = "books_movies";
    private static final String GENDER = "gender";
    private static final String ORIGIN = "origin";
    private static final String DATE = "date";
    private static final String PHOTO_PATH = "photo_path";

    public static void saveUsername(String body, Context context){
        SharedPreferences settingsFile = context.getSharedPreferences(PROFILE, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = settingsFile.edit();
        editor.putString(USERNAME, body);
        editor.apply();
    }

    public static String getUsername(Context context){
        SharedPreferences settingsFile = context.getSharedPreferences(PROFILE, Context.MODE_PRIVATE);
        return settingsFile.getString(USERNAME, "");
    }

    public static void saveProfileId(String id, Context context){
        SharedPreferences settingsFile = context.getSharedPreferences(PROFILE, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = settingsFile.edit();
        editor.putString(PROFILE_ID, id);
        editor.apply();
    }

    public static String getProfileId(Context context){
        SharedPreferences settingsFile = context.getSharedPreferences(PROFILE, Context.MODE_PRIVATE);
        return settingsFile.getString(PROFILE_ID, null);
    }

    public static void saveToken(String body, Context context){
        SharedPreferences settingsFile = context.getSharedPreferences(PROFILE, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = settingsFile.edit();
        editor.putString(TOKEN, body);
        editor.apply();
    }

    public static String getToken(Context context){
        SharedPreferences settingsFile = context.getSharedPreferences(PROFILE, Context.MODE_PRIVATE);
        return settingsFile.getString(TOKEN, "");
    }

    public static void saveStatus(Context context, boolean isOnline){
        SharedPreferences settingsFile = context.getSharedPreferences(PROFILE, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = settingsFile.edit();
        editor.putBoolean(IS_ONLINE, isOnline);
        editor.apply();
    }

    public static boolean getStatus(Context context){
        SharedPreferences settingsFile = context.getSharedPreferences(PROFILE, Context.MODE_PRIVATE);
        return settingsFile.getBoolean(IS_ONLINE, false);
    }

    public static void saveLocation(Location location, Context context){
        SharedPreferences settingsFile = context.getSharedPreferences(PROFILE, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = settingsFile.edit();
        editor.putLong(NetConstants.LATITUDE, Double.doubleToRawLongBits(location.getLatitude()));
        editor.putLong(NetConstants.LONGTITUDE, Double.doubleToRawLongBits(location.getLongitude()));
        editor.apply();
    }

    public static LatLng getLocation(Context context){
        SharedPreferences settingsFile = context.getSharedPreferences(PROFILE, Context.MODE_PRIVATE);
        double lat = Double.longBitsToDouble(settingsFile.getLong(NetConstants.LATITUDE, 0));
        double lng = Double.longBitsToDouble(settingsFile.getLong(NetConstants.LONGTITUDE, 0));
        return new LatLng(lat, lng);
    }

    public static void saveProfile(User user, Context context){
        SharedPreferences settingsFile = context.getSharedPreferences(PROFILE, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = settingsFile.edit();
        editor.putString(PROFILE_ID, user.getId());
        editor.putString(USERNAME, user.getUsername());
        editor.putString(ABOUT_ME, user.getAboutMe());
        editor.putString(INTERESTS, user.getInterests());
        editor.putString(BOOKS_MOVIES, user.getMusicBooks());
        editor.putBoolean(GENDER, user.getIsMale());
        editor.putString(DATE, user.getBirthDate());
        editor.putString(ORIGIN, user.getOrigin());
        editor.apply();
    }

    public static User getProfile(Context context){
        SharedPreferences settingsFile = context.getSharedPreferences(PROFILE, Context.MODE_PRIVATE);
        User user = new User(settingsFile.getString(PROFILE_ID, null));
        user.setAboutMe(settingsFile.getString(ABOUT_ME, null));
        user.setInterests(settingsFile.getString(INTERESTS, null));
        user.setMusicBooks(settingsFile.getString(BOOKS_MOVIES, null));
        user.setIsMale(settingsFile.getBoolean(GENDER, true));
        user.setBirthDate(settingsFile.getString(DATE, null));
        user.setUsername(settingsFile.getString(USERNAME, null));
        user.setOrigin(settingsFile.getString(ORIGIN, null));
        return user;
    }

    public static void savePhotoPath(String s, Context context){
        SharedPreferences settingsFile = context.getSharedPreferences(PROFILE, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = settingsFile.edit();
        editor.putString(PHOTO_PATH, s);
        editor.apply();
    }

    public static String getPhotoPath(Context context){
        SharedPreferences settingsFile = context.getSharedPreferences(PROFILE, Context.MODE_PRIVATE);
        return settingsFile.getString(PHOTO_PATH, null);
    }
}
