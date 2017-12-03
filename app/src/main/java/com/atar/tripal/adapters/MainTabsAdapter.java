package com.atar.tripal.adapters;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.view.ViewGroup;

import com.atar.tripal.R;
import com.atar.tripal.ui.MyHangoutsFragment;
import com.atar.tripal.ui.HangoutsAroundFragment;

public class MainTabsAdapter extends FragmentPagerAdapter {

    private Fragment[] mFragments;
    private Context mContext;

    public MainTabsAdapter(FragmentManager fm, Context context) {
        super(fm);
        mContext = context;
        mFragments = new Fragment[]{new HangoutsAroundFragment(), new MyHangoutsFragment()};
    }

    @NonNull
    @Override
    public Object instantiateItem(ViewGroup container, int position) {
        switch(position){
            case 0:
                mFragments[position] = (HangoutsAroundFragment)super.instantiateItem(container, position);
                break;
            case 1:
                mFragments[position] = (MyHangoutsFragment)super.instantiateItem(container, position);
                break;
        }
        return mFragments[position];
    }

    @Override
    public Fragment getItem(int position) {
        return mFragments[position];
    }

    @Override
    public int getCount() {
        return mFragments.length;
    }

    @Override
    public CharSequence getPageTitle(int position) {
        switch (position) {
            case 0:
                return mContext.getString(R.string.whos_around);
            case 1:
                return mContext.getString(R.string.my_hangouts);
            default:
                return null;
        }
    }

}
