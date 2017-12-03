package com.atar.tripal.callbacks;

public interface MyHangoutCallback {
    void onHangoutClick(int position, boolean isActive);
    void onHangoutLeave(int position);
}
