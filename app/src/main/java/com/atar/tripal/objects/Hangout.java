package com.atar.tripal.objects;

import com.atar.tripal.net.NetConstants;
import com.google.gson.annotations.SerializedName;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class Hangout implements Serializable {

    @SerializedName(NetConstants.LONGTITUDE)
    private double mLongtitude;
    @SerializedName(NetConstants.LATITUDE)
    private double mLatitude;

    @SerializedName(NetConstants.ADDRESS)
    private String mAddress;
    @SerializedName(NetConstants.PLACE_ID)
    private String mPlaceId;
    @SerializedName(NetConstants.NAME_OF_PLACE)
    private String mNameOfPlace;
    @SerializedName(NetConstants.THEME)
    private String mTheme;

    @SerializedName(NetConstants.FRIENDS)
    private List<User> mUsers;

    @SerializedName(NetConstants.HOST)
    private User mHost;

    @SerializedName(NetConstants.ID)
    private long mId;

    @SerializedName(NetConstants.TIMESTAMP)
    private long mTimestamp;

    private boolean mIsActive;
    private boolean mRequestSent = false;

    public double getLongtitude() {
        return mLongtitude;
    }
    public void setLongtitude(double longtitude) {
        mLongtitude = longtitude;
    }

    public double getLatitude() {
        return mLatitude;
    }
    public void setLatitude(double latitude) {
        mLatitude = latitude;
    }

    public String getAddress() {
        return mAddress;
    }
    public void setAddress(String address) {
        mAddress = address;
    }

    public String getPlaceId() {
        return mPlaceId;
    }
    public void setPlaceId(String placeId) {
        mPlaceId = placeId;
    }

    public String getNameOfPlace() {
        return mNameOfPlace;
    }
    public void setNameOfPlace(String nameOfPlace) {
        mNameOfPlace = nameOfPlace;
    }

    public String getTheme() {
        return mTheme;
    }
    public void setTheme(String theme) {
        mTheme = theme;
    }

    public User getHost(){
        return mHost;
    }
    public void setHost(User host){
        mHost = host;
    }

    public List<User> getFriends() {
        return mUsers;
    }
    public void setFriends(List<User> users) {
        mUsers.addAll(users);
    }

    public long getId(){
        return mId;
    }
    public void setId(long id){
        mId = id;
    }

    public boolean getIsActive(){
        return mIsActive;
    }
    public void setIsActive(boolean isActive){
        mIsActive = isActive;
    }

    public boolean getRequestSent(){
        return mRequestSent;
    }
    public void setRequestSent(boolean requestSent){
        mRequestSent = requestSent;
    }

    public void updatePlace(PlaceInfo info){
        setAddress(info.getAddress());
        setLongtitude(info.getLongtitude());
        setLatitude(info.getLatitude());
        setPlaceId(info.getId());
        setNameOfPlace(info.getName());
    }

    public Hangout() {
        mUsers = new ArrayList<>();
    }

    public long getTimestamp() {
        return mTimestamp;
    }

    public void setTimestamp(long mTimestamp) {
        this.mTimestamp = mTimestamp;
    }
}
