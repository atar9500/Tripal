package com.atar.tripal.objects;

import com.atar.tripal.net.NetConstants;
import com.google.gson.annotations.SerializedName;

public class Result {

    @SerializedName(NetConstants.RESULT)
    private String mResult;

    public Result() {}

    public String getResult() {
        return mResult;
    }
    public void setResult(String mResult) {
        this.mResult = mResult;
    }
}
