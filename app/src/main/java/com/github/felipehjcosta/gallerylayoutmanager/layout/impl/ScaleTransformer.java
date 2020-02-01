package com.github.felipehjcosta.gallerylayoutmanager.layout.impl;

import android.view.View;

import com.github.felipehjcosta.layoutmanager.GalleryLayoutManager;

public class ScaleTransformer implements GalleryLayoutManager.ItemTransformer {

    private static final String TAG = "CurveTransformer";

    @Override
    public void transformItem(GalleryLayoutManager layoutManager, View item, int viewPosition, float fraction) {
        item.setPivotX(item.getWidth() / 2.f);
        item.setPivotY(item.getHeight() / 2.0f);
        float scale = 1 - 0.2f * Math.abs(fraction);
        item.setScaleX(scale);
        item.setScaleY(scale);
    }
}
