package com.atar.tripal.adapters;

import android.content.Context;
import android.graphics.Bitmap;
import android.support.annotation.NonNull;
import android.support.v4.view.PagerAdapter;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import com.atar.tripal.R;

import java.util.List;

public class PlacePhotoAdapter extends PagerAdapter {

    private List<Bitmap> mImages;
    private LayoutInflater mInflater;

    public PlacePhotoAdapter(List<Bitmap> images, Context context){
        mImages = images;
        mInflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
    }

    @Override
    public int getCount() {
        return mImages.size();
    }

    @Override
    public int getItemPosition(@NonNull Object object) {
        if (mImages.contains(object)) {
            return mImages.indexOf(object);
        } else {
            return POSITION_NONE;
        }
    }

    @Override
    public boolean isViewFromObject(@NonNull View view, @NonNull Object object) {
        return view.equals(object);
    }

    @NonNull
    @Override
    public Object instantiateItem(@NonNull ViewGroup container, int position) {
        View img = mInflater.inflate(R.layout.place_img, container, false);
        ImageView image = img.findViewById(R.id.place_img);
        image.setImageBitmap(mImages.get(position));
        container.addView(img);
        return img;
    }

    @Override
    public void destroyItem(@NonNull ViewGroup container, int position, @NonNull Object object) {
        container.removeView((ImageView)object);
    }
}
