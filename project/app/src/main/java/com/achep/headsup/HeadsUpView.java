/*
 * Copyright (C) 2014 AChep@xda <artemchep@gmail.com>
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston,
 * MA  02110-1301, USA.
 */
package com.achep.headsup;

import android.content.Context;
import android.content.res.Configuration;
import android.support.annotation.NonNull;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.widget.FrameLayout;

/**
 * Created by Artem Chepurnoy on 16.09.2014.
 */
public class HeadsUpView extends FrameLayout implements
        SwipeHelper.Callback {

    private final SwipeHelper mSwipeHelperY;
    private final int mTouchSensitivityDelay;
    private long mStartTouchTime;

    private HeadsUpBase mManager;
    private ViewGroup mContent;

    public HeadsUpView(Context context) {
        this(context, null);
    }

    public HeadsUpView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public HeadsUpView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        mTouchSensitivityDelay = getResources().getInteger(R.integer.heads_up_sensitivity_delay);
        float densityScale = getResources().getDisplayMetrics().density;
        float pagingTouchSlop = ViewConfiguration.get(getContext()).getScaledPagingTouchSlop();
        mSwipeHelperY = new SwipeHelper(SwipeHelper.TOP, this, densityScale, pagingTouchSlop, false);
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        mContent = (ViewGroup) findViewById(R.id.content);
    }

    public void setHeadsUpManager(HeadsUpBase manager) {
        mManager = manager;

        mSwipeHelperY.setPowerSaveDetector(mManager.getPowerSaveDetector());
    }

    public void setShownOnTop(boolean shownOnTop) {
        int swipeDirY = shownOnTop ? SwipeHelper.TOP : SwipeHelper.BOTTOM;
        mSwipeHelperY.setSwipeDirection(swipeDirY);
    }

    /**
     * Calling this method means that the view won't do any interactivity
     * such as touches for some {@link com.achep.headsup.R.integer#heads_up_sensitivity_delay time}.
     */
    public void preventInstantInteractivity() {
        mStartTouchTime = System.currentTimeMillis() + mTouchSensitivityDelay;
    }

    /**
     * @return {@code true} if this touch should be ignored
     * (mainly because of {@link #mTouchSensitivityDelay touch sensitivity delay}),
     * {@code false} otherwise.
     */
    private boolean ignoreAnyInteractivity() {
        return System.currentTimeMillis() < mStartTouchTime;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean onInterceptTouchEvent(MotionEvent event) {
        return ignoreAnyInteractivity()
                || mSwipeHelperY.onInterceptTouchEvent(event)
                || super.onInterceptTouchEvent(event);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean onTouchEvent(@NonNull MotionEvent event) {
        if (ignoreAnyInteractivity()) {
            return false;
        }

        switch (event.getAction()) {
            case MotionEvent.ACTION_OUTSIDE:
                if (mManager.getConfig().isHideOnTouchOutsideEnabled()) {
                    mManager.hide(false);
                }
                return true;
            default:
                return mSwipeHelperY.onTouchEvent(event) || super.onTouchEvent(event);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        float densityScale = getResources().getDisplayMetrics().density;
        mSwipeHelperY.setDensityScale(densityScale);
        float pagingTouchSlop = ViewConfiguration.get(getContext()).getScaledPagingTouchSlop();
        mSwipeHelperY.setPagingTouchSlop(pagingTouchSlop);
    }

    //-- SWIPE HELPER'S METHODS -----------------------------------------------

    @Override
    public View getChildAtPosition(MotionEvent ev) {
        return mContent;
    }

    @Override
    public View getChildContentView(View v) {
        return mContent;
    }

    @Override
    public boolean canChildBeDismissed(View v) {
        return true;
    }

    @Override
    public void onBeginDrag(View v) {
        requestDisallowInterceptTouchEvent(true);
    }

    @Override
    public void onChildDismissed(View v) {
        mManager.hide(false);
    }

    @Override
    public void onDragCancelled(View v) {
    }

}
