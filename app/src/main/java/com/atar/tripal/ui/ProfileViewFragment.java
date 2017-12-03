package com.atar.tripal.ui;


import android.content.Context;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
import android.support.v4.view.ViewCompat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.atar.tripal.R;
import com.atar.tripal.callbacks.ProfileCallback;
import com.atar.tripal.objects.User;

import java.util.Calendar;

import static java.util.Calendar.DATE;
import static java.util.Calendar.MONTH;
import static java.util.Calendar.YEAR;

public class ProfileViewFragment extends Fragment {


    public ProfileViewFragment() {}

    /**
     * Data
     */
    private User mUser;
    private ProfileCallback mCallback;

    /**
     * UI Widgets
     */
    private View mView;
    private TextView mName, mOrigin, mAboutMe, mInterests, mMoviesBooks;
    private ImageView mGender;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
            ViewGroup container, Bundle savedInstanceState) {

        mView = inflater.inflate(R.layout.fragment_profile_view, container, false);

        mUser = mCallback.getCurrentUser();

        setRetainInstance(true);

        initUIWidgets();

        showData();

        return mView;
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        mCallback = (ProfileCallback)context;
    }

    public void initUIWidgets(){
        mName = mView.findViewById(R.id.pv_name);
        mOrigin = mView.findViewById(R.id.pv_origin);
        mAboutMe = mView.findViewById(R.id.pv_about_me);
        mInterests = mView.findViewById(R.id.pv_interests);
        mMoviesBooks = mView.findViewById(R.id.pv_movies_and_books);
        mGender = mView.findViewById(R.id.pv_gender);
    }

    public void showData(){
        mName.setText(mUser.getUsername());
        if(mUser.getIsMale()){
            mGender.setImageResource(R.mipmap.male);
        } else {
            mGender.setImageResource(R.mipmap.female);
        }
        String s = "" + getDiffYears();
        if(mUser.getOrigin() != null && !mUser.getOrigin().equals(""))
        s += ", " + mUser.getOrigin();
        mOrigin.setText(s);
        mAboutMe.setText(mUser.getAboutMe());
        mInterests.setText(mUser.getInterests());
        mMoviesBooks.setText(mUser.getMusicBooks());
    }

    private int getDiffYears() {
        Calendar a = User.getCalender(mUser.getBirthDate());
        Calendar b = Calendar.getInstance();
        int diff = b.get(YEAR) - a.get(YEAR);
        if (a.get(MONTH) > b.get(MONTH) ||
                (a.get(MONTH) == b.get(MONTH) && a.get(DATE) > b.get(DATE))) {
            diff--;
        }
        return diff;
    }

}
