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
import android.content.res.TypedArray;
import android.support.annotation.NonNull;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.achep.acdisplay.Config;
import com.achep.acdisplay.Timeout;
import com.achep.acdisplay.notifications.OpenNotification;
import com.achep.acdisplay.ui.widgets.notification.NotificationWidget;
import com.achep.base.Build;
import com.achep.base.utils.RippleUtils;
import com.achep.base.utils.ViewUtils;

/**
 * Created by Artem Chepurnoy on 16.09.2014.
 */
public class HeadsUpNotificationView extends NotificationWidget implements
        SwipeHelper.Callback, Timeout.OnTimeoutEventListener {

    private static final String TAG = "HeadsUpNotificationView";

    private final boolean mRipple;

    public final Timeout mTimeout;
    private Timeout.Gui mTimeoutGui;
    private ProgressBar mProgressBar;

    private boolean mDarkTheme;

    private SwipeHelper mSwipeHelper;

    private HeadsUpBase mHeadsUpBase;

    public HeadsUpNotificationView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public HeadsUpNotificationView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.HeadsUpNotificationView);
        mRipple = a.getBoolean(R.styleable.HeadsUpNotificationView_ripple, false);
        a.recycle();

        mTimeout = new Timeout();
    }

    public void setHeadsUpManager(HeadsUpBase headsUpBase) {
        mHeadsUpBase = headsUpBase;
    }

    /**
     * Resets decay timer.
     *
     * @see com.achep.acdisplay.Config#getNotifyDecayTime()
     */
    public void resetDecayTime() {
        int delay = mHeadsUpBase.getConfig().getNotifyDecayTime();
        if (delay > 0) {
            delay += Math.max(getNotification().getNotification().priority * 750, 0);
            if (!getNotification().isDismissible()) delay += 1000;
            mTimeout.set(delay, true);
            mTimeout.resume();
        }
    }

    /**
     * Dismisses this notification from system.
     */
    private void dismiss() {
        getNotification().dismiss();
    }

    /**
     * Hides this HeadsUp view.
     */
    private void hide() {
        OpenNotification notification = getNotification();
        notification.markAsRead();
        mHeadsUpBase.removeHeadsUp(notification);
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        TextView titleView = (TextView) findViewById(R.id.title);
        mDarkTheme = !hasDarkTextColor(titleView);

        if (mRipple) {
            View content = findViewById(R.id.content);
            RippleUtils.makeFor(false /* parent is scrollable */, mDarkTheme, content);
        }

        mProgressBar = (ProgressBar) findViewById(R.id.progress_bar);
        mTimeoutGui = new Timeout.Gui(mProgressBar);
    }

    @Override
    protected View initActionView(View view) {
        if (mRipple) {
            RippleUtils.makeFor(false /* parent is scrollable */, mDarkTheme, view);
        }
        return super.initActionView(view);
    }

    @Override
    public void onAttachedToWindow() {
        float densityScale = getResources().getDisplayMetrics().density;
        float pagingTouchSlop = ViewConfiguration.get(getContext()).getScaledPagingTouchSlop();
        mSwipeHelper = new SwipeHelper(SwipeHelper.X, this, densityScale, pagingTouchSlop);

        ViewUtils.setVisible(mProgressBar, false);
        mTimeout.registerListener(this);
        mTimeout.registerListener(mTimeoutGui);
        // Update the timeout
        resetDecayTime();
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();

        mTimeout.clear();
        mTimeout.unregisterListener(this);
        mTimeout.unregisterListener(mTimeoutGui);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean onInterceptTouchEvent(MotionEvent event) {
        return mSwipeHelper.onInterceptTouchEvent(event)
                || super.onInterceptTouchEvent(event);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean onTouchEvent(@NonNull MotionEvent event) {
        // Do not let notification to be timed-out while
        // we are touching it.
        resetDecayTime();

        // Translate touch event too to correspond with
        // view's translation changes and prevent lags
        // while swiping.
        final MotionEvent ev = MotionEvent.obtainNoHistory(event);
        ev.offsetLocation(getTranslationX(), getTranslationY());
        boolean handled = mSwipeHelper.onTouchEvent(ev);
        ev.recycle();

        return handled || super.onTouchEvent(event);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        float densityScale = getResources().getDisplayMetrics().density;
        mSwipeHelper.setDensityScale(densityScale);
        float pagingTouchSlop = ViewConfiguration.get(getContext()).getScaledPagingTouchSlop();
        mSwipeHelper.setPagingTouchSlop(pagingTouchSlop);
    }

    //-- TIMEOUT --------------------------------------------------------------

    @Override
    public void onTimeoutEvent(@NonNull Timeout timeout, int event) {
        switch (event) {
            case Timeout.EVENT_TIMEOUT:
                // Running #hide() it this thread may cause the
                // java.util.ConcurrentModificationException.
                post(new Runnable() {
                    @Override
                    public void run() {
                        hide();
                    }
                });
                break;
        }
    }

    //-- SWIPE HELPER'S METHODS -----------------------------------------------

    @Override
    public View getChildAtPosition(MotionEvent ev) {
        return this;
    }

    @Override
    public View getChildContentView(View v) {
        return this;
    }

    @Override
    public boolean canChildBeDismissed(View v) {
        return getNotification().isDismissible();
    }

    @Override
    public void onBeginDrag(View v) {
        requestDisallowInterceptTouchEvent(true);
    }

    @Override
    public void onChildDismissed(View v) {
        if (v.getTranslationX() == 0) Log.w(TAG, "Failed to detect the swipe\'s direction!" +
                " Assuming it\'s RTL...");

        final boolean toRight = v.getTranslationX() > 0;
        final int action = toRight
                ? Config.getInstance().getStrAction()
                : Config.getInstance().getStlAction();

        if (Build.DEBUG) Log.d(TAG, "swiped_to_right=" + toRight + " action=" + action);

        switch (action) {
            case Config.ST_DISMISS:
                dismiss();
                break;
            case Config.ST_HIDE:
                hide();
                break;
            case Config.ST_SNOOZE:
                // TODO: Implement swipe-to-snooze
            default:
                throw new IllegalArgumentException();
        }
    }

    @Override
    public void onDragCancelled(View v) {
        setAlpha(1f); // sometimes this isn't quite reset
    }

}
