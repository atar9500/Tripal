package com.atar.tripal.objects;

import com.atar.tripal.net.NetConstants;
import com.google.gson.annotations.SerializedName;

import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

public class User implements Serializable {

    public static String FORMAT_DATE = "MMM dd, yyyy";

    //Fields
    @SerializedName(NetConstants.USERNAME)
    private String mUsername;
    @SerializedName(NetConstants.ID)
    private String mId;
    @SerializedName("isMale")
    private boolean mIsMale = true;
    @SerializedName("birthday")
    private String mBirthDate;
    @SerializedName("origin")
    private String mOrigin;
    @SerializedName("aboutMe")
    private String mAboutMe;
    @SerializedName("interests")
    private String mInterests;
    @SerializedName("musicBooks")
    private String mMusicBooks;
    @SerializedName("token")
    private String mToken;

    //Constructors
    public User(){}
    public User(String id){
        setId(id);
    }

    //Getters
    public String getUsername() {
        return mUsername;
    }
    public String getId() {
        return mId;
    }
    public boolean getIsMale() {
        return mIsMale;
    }
    public String getBirthDate() {
        return mBirthDate;
    }
    public String getOrigin() {
        return mOrigin;
    }
    public String getAboutMe() {
        return mAboutMe;
    }
    public String getInterests() {
        return mInterests;
    }
    public String getMusicBooks() {
        return mMusicBooks;
    }
    public void setToken(String token) {
        mToken = token;
    }

    //Setters
    public void setUsername(String username) {
        mUsername = username;
    }
    public void setId(String id) {
        mId = id;
    }
    public void setIsMale(boolean isMale) {
        mIsMale = isMale;
    }
    public void setBirthDate(String birthDate) {
        mBirthDate = birthDate;
    }
    public void setOrigin(String origin) {
        mOrigin = origin;
    }
    public void setAboutMe(String aboutMe) {
        mAboutMe = aboutMe;
    }
    public void setInterests(String interests) {
        mInterests = interests;
    }
    public void setMusicBooks(String musicBooks) {
        mMusicBooks = musicBooks;
    }
    public String getToken() {
        return mToken;
    }

    public static Calendar getCalender(String birthDate){
        try {
            Date date = new SimpleDateFormat(FORMAT_DATE, Locale.getDefault())
                    .parse(birthDate);
            Calendar calendar = Calendar.getInstance(Locale.getDefault());
            calendar.setTime(date);
            return calendar;
        } catch (Exception e) {
            e.printStackTrace();
            Calendar calendar = Calendar.getInstance(Locale.getDefault());
            calendar.set(calendar.get(Calendar.YEAR) - 18, calendar.get(Calendar.MONTH),
                    calendar.get(Calendar.DAY_OF_MONTH));
            return calendar;
        }
    }

}
