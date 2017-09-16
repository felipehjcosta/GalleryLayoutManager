package com.github.felipehjcosta.gallerylayoutmanager;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.app.AppCompatActivity;

import com.github.felipehjcosta.gallerylayoutmanager.util.StatusBarCompat;

import butterknife.ButterKnife;

public class MainActivity extends AppCompatActivity {
    public static final int SHOW_PAGER = 1;
    public static final int SHOW_GALLERY = 2;

    private static final String TAG = "MainActivity";

    ViewPagerFragment mViewPagerFragment;
    TestFragment mTestFragment;
    MainFragment mMainFragment;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        initWindow();
        super.onCreate(savedInstanceState);
        setContentView(com.github.felipehjcosta.gallerylayoutmanager.R.layout.activity_main);
        ButterKnife.bind(this);
        if (savedInstanceState != null) {
            mViewPagerFragment = ViewPagerFragment.findFragment(getSupportFragmentManager());
            mTestFragment = TestFragment.findFragment(getSupportFragmentManager());
            mMainFragment = MainFragment.findFragment(getSupportFragmentManager());
        }
        if (mViewPagerFragment == null) {
            mViewPagerFragment = ViewPagerFragment.newInstance();
        }
        if (mTestFragment == null) {
            mTestFragment = TestFragment.newInstance();
        }
        if (mMainFragment == null) {
            mMainFragment = MainFragment.newInstance();
        }
        if (savedInstanceState == null) {
            initView();
        }
    }

    private void initWindow() {
        StatusBarCompat.translucentStatusBar2(this);
    }

    private void showFragment(Fragment fragment) {
        getSupportFragmentManager().beginTransaction()
                .add(com.github.felipehjcosta.gallerylayoutmanager.R.id.activity_main, fragment)
                .hide(mMainFragment)
                .addToBackStack(fragment.getClass().getSimpleName())
                .commitAllowingStateLoss();
    }

    private void initView() {
        getSupportFragmentManager().beginTransaction()
                .replace(com.github.felipehjcosta.gallerylayoutmanager.R.id.activity_main, mMainFragment).commit();
    }

    public void gotoFragment(int type) {
        switch (type) {
            case SHOW_GALLERY:
                showFragment(mTestFragment);
                break;
            case SHOW_PAGER:
                showFragment(mViewPagerFragment);
                break;
            default:
                throw new IllegalStateException("unknow type :" + type);
        }
    }


}
