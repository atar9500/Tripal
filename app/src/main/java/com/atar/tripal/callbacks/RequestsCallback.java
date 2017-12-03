package com.atar.tripal.callbacks;

public interface RequestsCallback {
    void onRequestAccepted(int position);
    void onRequestDenied(int position);
}
