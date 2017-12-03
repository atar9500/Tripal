package com.atar.tripal.callbacks;

import com.atar.tripal.objects.User;

public interface LoginListener {
    void onSignInClick(String email, String password);
    void onCreateAccount(User user, String email, String password);
    void onResetClick(String email);
}
