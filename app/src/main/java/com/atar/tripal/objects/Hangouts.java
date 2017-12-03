package com.atar.tripal.objects;

import com.atar.tripal.net.NetConstants;
import com.google.gson.annotations.SerializedName;

import java.util.List;

public class Hangouts {

    @SerializedName("hangouts")
    private List<Hangout> mHangouts;

    public List<Hangout> getHangouts() {
        return mHangouts;
    }

    public void setHangouts(List<Hangout> hangout) {
        mHangouts = hangout;
    }

    @SerializedName(NetConstants.RESULT)
    private String mResult;

    public String getResult() {
        return mResult;
    }

    public void setResult(String result) {
        mResult = result;
    }
}
