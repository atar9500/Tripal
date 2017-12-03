package com.atar.tripal.callbacks;

import com.atar.tripal.objects.Hangout;
import com.atar.tripal.objects.PlaceInfo;

public interface HangoutCallback {
    Hangout getHangout();
    void onSetPlaceClick();
    void onSetPlaceClick(PlaceInfo info);
}
