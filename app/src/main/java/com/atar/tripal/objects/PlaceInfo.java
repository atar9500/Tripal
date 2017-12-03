package com.atar.tripal.objects;

import com.atar.tripal.R;
import com.google.android.gms.location.places.Place;

import java.io.Serializable;

public class PlaceInfo implements Serializable {

    private String mAddress, mName, mId;
    private double mLatitude, mLongtitude;
    private float mStars;
    private int mType;

    public String getAddress() {
        return mAddress;
    }
    public String getName() {
        return mName;
    }
    public int getType() {
        return mType;
    }
    public double getLatitude() {
        return mLatitude;
    }
    public double getLongtitude() {
        return mLongtitude;
    }
    public float getStars() {
        return mStars;
    }
    public String getId() {
        return mId;
    }

    public void setAddress(String address) {
        mAddress = address;
    }
    public void setName(String name) {
        mName = name;
    }
    public void setType(int type) {
        mType = type;
    }
    public void setLatitude(double latitude) {
        mLatitude = latitude;
    }
    public void setLongtitude(double longtitude) {
        mLongtitude = longtitude;
    }
    public void setStars(float stars) {
        mStars = stars;
    }
    public void setId(String id) {
        mId = id;
    }

    public PlaceInfo(Place place) {
        mAddress = place.getAddress().toString();
        mName = place.getName().toString();
        for(int i = 0; i < place.getPlaceTypes().size(); i++){
            switch (place.getPlaceTypes().get(0)){
                case Place.TYPE_BAR:
                    setType(R.string.bar);
                    break;
                case Place.TYPE_CAFE:
                    setType(R.string.cafe);
                    break;
                case Place.TYPE_LIQUOR_STORE:
                    setType(R.string.liquor_store);
                    break;
                case Place.TYPE_MEAL_TAKEAWAY:
                    setType(R.string.restaurant);
                    break;
                case Place.TYPE_CITY_HALL:
                    setType(R.string.city_hall);
                    break;
                case Place.TYPE_LIBRARY:
                    setType(R.string.library);
                    break;
                case Place.TYPE_NIGHT_CLUB:
                    setType(R.string.night_club);
                    break;
                case Place.TYPE_SHOPPING_MALL:
                    setType(R.string.shopping_mall);
                    break;
                case Place.TYPE_STORE:
                    setType(R.string.store);
                    break;
                case Place.TYPE_PARK:
                    setType(R.string.park);
                    break;
            }
            if(mType != 0){
                break;
            }
        }
        mLatitude = place.getLatLng().latitude;
        mLongtitude = place.getLatLng().longitude;
        mStars = place.getRating();
        mId = place.getId();
    }

    public PlaceInfo() {}

}
