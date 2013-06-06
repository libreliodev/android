package com.librelio.activity;

import android.content.res.Configuration;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.util.Log;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import com.librelio.fragment.WebViewFragment;
import com.niveales.wind.R;

public class HTMLViewerActivity extends FragmentActivity {

    private ViewPager mPager;
    private PagerAdapter mPagerAdapter;
    private int mOrientation;
    private float mPageWidth;
    private boolean mActionBarVisible;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_html_viewer);

        mOrientation = getResources().getConfiguration().orientation;

//        if (mOrientation == Configuration.ORIENTATION_LANDSCAPE) {
//            mPageWidth = 0.5f;
//        } else {
            mPageWidth = 1f;
//        }

        mPager = (ViewPager) findViewById(R.id.pager);
        mPagerAdapter = new ScreenSlidePagerAdapter(getSupportFragmentManager());
        mPager.setAdapter(mPagerAdapter);
        mPager.setOffscreenPageLimit(2);

        mPager.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LOW_PROFILE);
        getActionBar().hide();
        mActionBarVisible = false;

    }

     private void toggleActionBar() {
        if (mActionBarVisible) {
            getActionBar().hide();
        } else {
            getActionBar().show();
        }
        mActionBarVisible = !mActionBarVisible;
    }

    private class ScreenSlidePagerAdapter extends FragmentStatePagerAdapter {
        public ScreenSlidePagerAdapter(FragmentManager fm) {
            super(fm);
        }

        @Override
        public Fragment getItem(int position) {

            WebViewFragment webViewFragment;
//            if ((mOrientation == Configuration.ORIENTATION_LANDSCAPE) && (position == 0 || position == getCount() - 1)) {
//                return new Fragment();
//            } else {
                webViewFragment = WebViewFragment.newInstance("file:///android_asset/magazine/Page_" + position + ".html");
//            }
//            webViewFragment.setOnWebViewClickListener(new OnWebViewClickListener() {
//
//                @Override
//                public void onWebViewClick() {
//                    toggleActionBar();
//                }
//            });
            return webViewFragment;
        }

        @Override
        public int getCount() {
//            return 7 + (mOrientation == Configuration.ORIENTATION_LANDSCAPE? 2 : 0);
            return 5;
        }

        @Override
        public float getPageWidth(int position) {
            return mPageWidth;
        }
    }

    public abstract class OnWebViewClickListener {
        public abstract void onWebViewClick();
    }

}