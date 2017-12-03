package com.atar.tripal.objects;

import java.util.ArrayList;
import java.util.List;

public class PlacePrediction {

    private String mPrimaryText, mSecondaryText, mId;
    private List<Integer> mTypes;

    public String getPrimaryText() {
        return mPrimaryText;
    }
    public void setPrimaryText(String primaryText) {
        mPrimaryText = primaryText;
    }

    public String getSecondaryText() {
        return mSecondaryText;
    }
    public void setSecondaryText(String secondaryText) {
        mSecondaryText = secondaryText;
    }

    public String getId() {
        return mId;
    }
    public void setId(String id) {
        mId = id;
    }

    public List<Integer> getTypes() {
        return mTypes;
    }
    public void setTypes(List<Integer> types) {
        mTypes.addAll(types);
    }

    public PlacePrediction(){
        mTypes = new ArrayList<>();
    }
}
