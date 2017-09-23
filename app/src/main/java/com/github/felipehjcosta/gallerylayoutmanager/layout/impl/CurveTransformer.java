package com.github.felipehjcosta.gallerylayoutmanager.layout.impl;

import android.view.View;

import com.github.felipehjcosta.layoutmanager.GalleryLayoutManager;

import org.jetbrains.annotations.NotNull;

public class CurveTransformer implements GalleryLayoutManager.ItemTransformer {
    public static final int ROTATE_ANGEL = 7;
    private static final String TAG = "CurveTransformer";

    @Override
    public void transformItem(@NotNull GalleryLayoutManager layoutManager, @NotNull View item, int viewPosition, float fraction) {
        if (layoutManager.getOrientation() == GalleryLayoutManager.VERTICAL) {
            return;
        }
        int width = item.getWidth();
        int height = item.getHeight();
        item.setPivotX(width / 2.f);
        item.setPivotY(height);
        float scale = 1 - 0.1f * Math.abs(fraction);
        item.setScaleX(scale);
        item.setScaleY(scale);
        item.setRotation(ROTATE_ANGEL * fraction);
        item.setTranslationY((float) (Math.sin(2 * Math.PI * ROTATE_ANGEL * Math.abs(fraction) / 360.f) * width / 2.0f));
        item.setTranslationX((float) ((1 - scale) * width / 2.0f / Math.cos(2 * Math.PI * ROTATE_ANGEL * fraction / 360.f)));
        if (fraction > 0) {
            item.setTranslationX(-item.getTranslationX());
        }
        item.setAlpha(1 - 0.2f * Math.abs(fraction));
    }
}
