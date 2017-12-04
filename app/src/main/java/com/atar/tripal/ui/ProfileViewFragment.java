package com.atar.tripal.ui;

import android.content.Context;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.atar.tripal.R;
import com.atar.tripal.callbacks.ProfileCallback;
import com.atar.tripal.objects.User;

import java.util.Calendar;

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
    private TextView mName, mOrigin, mAboutMe, mInterests, mMoviesBooks, mAge;
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
        mAge = mView.findViewById(R.id.pv_age);
    }

    public void showData(){
        mName.setText(mUser.getUsername());

        if(mUser.getIsMale()){
            mGender.setImageResource(R.mipmap.male);
        } else {
            mGender.setImageResource(R.mipmap.female);
        }

        mAge.setText(getDiffYears());

        String origin = mUser.getOrigin();
        if(origin != null && !origin.equals("")){
            mOrigin.setText(origin);
            mOrigin.setVisibility(View.VISIBLE);
        } else {
            mOrigin.setVisibility(View.INVISIBLE);
        }

        mAboutMe.setText(mUser.getAboutMe());
        mInterests.setText(mUser.getInterests());
        mMoviesBooks.setText(mUser.getMusicBooks());
    }

    private String getDiffYears() {
        Calendar a = User.getCalender(mUser.getBirthDate());
        Calendar b = Calendar.getInstance();
        int diff = b.get(Calendar.YEAR) - a.get(Calendar.YEAR);
        if (a.get(Calendar.MONTH) > b.get(Calendar.MONTH) ||
                (a.get(Calendar.MONTH) == b.get(Calendar.MONTH) && a.get(Calendar.DATE) > b.get(Calendar.DATE))) {
            diff--;
        }
        return diff + "";
    }

}
