package com.jay.ui;

import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.graphics.drawable.DrawableCompat;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v7.widget.Toolbar;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.jay.ui.entity.Photo;
import com.jay.ui.widget.TouchImageView;

import java.util.List;
import java.util.Locale;

/**
 * Created on 2016/4/12
 *
 * @author Q.Jay
 * @version 1.0.0
 */
public class PhotoPreviewFragment extends Fragment implements ViewPager.OnPageChangeListener {

    private static int currentIndex;
    private static List<Photo> mPhotos;
    private ViewPager viewPager;
    private Toolbar toolbar;

    public static PhotoPreviewFragment newInstance(List<Photo> photos, int index){
        mPhotos = photos;
        currentIndex = index;
        return new PhotoPreviewFragment();
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        final Context context = getContext();
        FrameLayout rootView = new FrameLayout(context);
        rootView.setBackgroundColor(Color.BLACK);
        viewPager = new ViewPager(context);
        viewPager.addOnPageChangeListener(this);
        rootView.addView(viewPager);

        toolbar = new Toolbar(context);
        toolbar.setNavigationIcon(R.drawable.ic_arrow_back_black_24dp);
        toolbar.setTitleTextColor(Color.WHITE);
        toolbar.setBackgroundColor(Color.parseColor("#33000000"));
        Drawable navigationIcon = toolbar.getNavigationIcon();
        //noinspection ConstantConditions
        Drawable wrap = DrawableCompat.wrap(navigationIcon);
        DrawableCompat.setTint(wrap, Color.WHITE);

        TypedValue tv = new TypedValue();
        int actionBarSize = 0;
        if (getContext().getTheme().resolveAttribute(android.R.attr.actionBarSize, tv, true)){
            actionBarSize = TypedValue.complexToDimensionPixelSize(tv.data,getResources().getDisplayMetrics());
        }else if (getContext().getTheme().resolveAttribute(android.support.v7.appcompat.R.attr.actionBarSize, tv, true)){
            actionBarSize = TypedValue.complexToDimensionPixelSize(tv.data,getResources().getDisplayMetrics());
        }

        Toolbar.LayoutParams toolbarParams = new Toolbar.LayoutParams(Toolbar.LayoutParams.MATCH_PARENT,actionBarSize);
        toolbar.setLayoutParams(toolbarParams);

        toolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                getActivity().onBackPressed();
            }
        });

        rootView.addView(toolbar);
        return rootView;
    }

    private void initTitle(){

        String title = String.format(Locale.getDefault(), "%d/%d", viewPager.getCurrentItem() + 1, mPhotos.size());
        toolbar.setTitle(title);
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        PhotoPageAdapter adapter = new PhotoPageAdapter();
        viewPager.setOffscreenPageLimit(4);
        viewPager.setAdapter(adapter);
        viewPager.setCurrentItem(currentIndex);
        initTitle();
    }

    @Override
    public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {

    }

    @Override
    public void onPageSelected(int position) {
        initTitle();
    }

    @Override
    public void onPageScrollStateChanged(int state) {

    }

    class PhotoPageAdapter extends PagerAdapter{

        @Override
        public int getCount() {
            if (mPhotos == null) {
                return 0;
            }
            return mPhotos.size();
        }

        @Override
        public boolean isViewFromObject(View view, Object object) {
            return view == object;
        }

        @Override
        public Object instantiateItem(ViewGroup container, int position) {
            TouchImageView imageView = new TouchImageView(getContext());
            container.addView(imageView);
            Photo photo = mPhotos.get(position);
            Glide.with(PhotoPreviewFragment.this).load(photo.uri).thumbnail(0.1f)
                    .dontAnimate()
                    .dontTransform().diskCacheStrategy(DiskCacheStrategy.RESULT)
                    .override(800, 800).into(imageView);
            imageView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    getActivity().onBackPressed();
                }
            });
            return imageView;
        }
        @Override
        public void destroyItem(ViewGroup container, int position, Object object) {
            container.removeView((View) object);
        }

        @Override
        public int getItemPosition (Object object) { return POSITION_NONE; }
    }
}
