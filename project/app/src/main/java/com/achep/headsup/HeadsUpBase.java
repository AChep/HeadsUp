/*
 * Copyright (C) 2015 AChep@xda <artemchep@gmail.com>
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

import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.TypedArray;
import android.graphics.PixelFormat;
import android.os.Handler;
import android.os.SystemClock;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.transition.TransitionManager;
import android.util.Log;
import android.view.ContextThemeWrapper;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.Space;

import com.achep.acdisplay.App;
import com.achep.acdisplay.Atomic;
import com.achep.acdisplay.Config;
import com.achep.acdisplay.notifications.NotificationPresenter;
import com.achep.acdisplay.notifications.NotificationUtils;
import com.achep.acdisplay.notifications.OpenNotification;
import com.achep.acdisplay.ui.widgets.notification.NotificationWidget;
import com.achep.acdisplay.utils.PendingIntentUtils;
import com.achep.base.Device;
import com.achep.base.content.ConfigBase;
import com.achep.base.tests.Check;
import com.achep.base.ui.animations.AnimationListenerAdapter;
import com.achep.base.utils.power.PowerUtils;

import java.util.ArrayList;

import static com.achep.base.Build.DEBUG;

/**
 * Created by Artem Chepurnoy on 26.09.2014.
 */
public class HeadsUpBase implements
        NotificationPresenter.OnNotificationListChangedListener,
        ConfigBase.OnConfigChangedListener {

    private static final String TAG = "HeadsUpBase";

    public static final String ACTION_ALLOW_HEADSUP = "com.achep.headsup.ACTION_ALLOW_HEADSUP";
    public static final String ACTION_DISALLOW_HEADSUP = "com.achep.headsup.ACTION_DISALLOW_HEADSUP";

    /**
     * Maximum period of time for which heads-up can be
     * disabled via remote intent.
     */
    private static final int MAX_DISABLE_INTENT_DURATION = 10 * 60 * 1000; // 10 min.

    /**
     * How long container's layout-animation is?
     */
    private static final int LAYOUT_ANIMATION_TIME = 300;

    private static HeadsUpBase sInstance;

    public static HeadsUpBase getInstance() {
        if (sInstance == null) {
            sInstance = new HeadsUpBase();
        }
        return sInstance;
    }

    // Why do we store all huge objects in holder?
    // Because we're too lazy to free each one manually.
    private Holder mHolder;

    /**
     * Passes atomic events to {@link #onStart(android.content.Context)}
     * and {@link #onStop()}.
     */
    private final Atomic mStartAtomic = new Atomic(new Atomic.Callback() {

        @Override
        public void onStart(Object... objects) {
            HeadsUpBase.this.onStart((Context) objects[0]);
        }

        @Override
        public void onStop(Object... objects) {
            HeadsUpBase.this.onStop();
        }
    });

    /**
     * Passes atomic events to {@link #onShow()}
     * and {@link #onHide(boolean)}.
     */
    private final Atomic mShowAtomic = new Atomic(new Atomic.Callback() {

        @Override
        public void onStart(Object... objects) {
            HeadsUpBase.this.onShow();
        }

        @Override
        public void onStop(Object... objects) {
            HeadsUpBase.this.onHide((boolean) objects[0]);
        }
    });

    private boolean mEnabled;
    private long mDisableIntentTime;

    private final Handler mHandler = new Handler();
    private final BroadcastReceiver mReceiver =
            new BroadcastReceiver() {

                @Override
                public void onReceive(Context context, Intent intent) {
                    switch (intent.getAction()) {
                        case ACTION_DISALLOW_HEADSUP:
                        case App.ACTION_EAT_HOME_PRESS_START: // for old versions of AcDisplay
                            mDisableIntentTime = SystemClock.elapsedRealtime();
                            hide(false);
                            break;
                        case ACTION_ALLOW_HEADSUP:
                        case App.ACTION_EAT_HOME_PRESS_STOP:  // for old versions of AcDisplay
                            mDisableIntentTime = 0;
                            break;
                        default:
                            Log.i(TAG, "Received unneeded intent=" + intent.toString());
                            break;
                    }
                }
            };

    private final NotificationWidget.OnClickListener mOnWidgetClickListener =
            new NotificationWidget.OnClickListener() {

                /**
                 * {@inheritDoc}
                 */
                @Override
                public void onClick(@NonNull NotificationWidget widget, @NonNull View v) {
                    OpenNotification notification = widget.getNotification();
                    Check.getInstance().isNonNull(notification);
                    assert notification != null;
                    notification.click();
                }

                /**
                 * {@inheritDoc}
                 */
                @Override
                public void onActionButtonClick(@NonNull NotificationWidget widget,
                                                @NonNull View v, @Nullable PendingIntent intent) {
                    PendingIntentUtils.sendPendingIntent(intent);
                    OpenNotification notification = widget.getNotification();
                    Check.getInstance().isNonNull(notification);
                    assert notification != null;
                    notification.dismiss();
                }
            };

    /**
     * A bunch of variables.
     */
    private static class Holder {

        Context context;
        WindowManager wm;

        // Views
        HeadsUpView rootView;
        ViewGroup containerView;
        int containerOffset;

        // Animations
        Animation exitAnimation;

        // Notifications
        ArrayList<HeadsUpNotificationView> widgetList;

    }

    private HeadsUpBase() { /* placeholder */ }

    //-- ATOMIC METHODS -------------------------------------------------------

    /**
     * Initializes resources and starts listening to notifications.
     * Note: this is safe to be run twice or more.
     *
     * @see #stop()
     */
    public final synchronized void start(@NonNull Context context) {
        mStartAtomic.start(context);
    }

    /**
     * Frees taken resources and stops listening to notifications.
     * Note: this is safe to be run twice or more.
     *
     * @see #start(android.content.Context)
     */
    public final synchronized void stop() {
        mStartAtomic.stop();
    }

    /**
     * @see #hide(boolean)
     */
    private void show() {
        mShowAtomic.start();
        Log.d(TAG, "Showing heads-up!");
    }

    /**
     * @param immediately {@code true} to immediately detach heads-up view from
     *                    window, {@code false} to hide it with animation.
     * @see #show()
     */
    public final synchronized void hide(boolean immediately) {
        mShowAtomic.stop(immediately);
    }

    //-- ON WHATEVER IS HAPPENING ---------------------------------------------

    void onStart(@NonNull Context context) {
        if (DEBUG) Log.d(TAG, "Starting heads-up...");

        mHolder = new Holder();
        mHolder.context = context;
        mHolder.wm = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        mHolder.widgetList = new ArrayList<>();

        // Setup views.
        LayoutInflater inflater = (LayoutInflater) context
                .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        mHolder.rootView = (HeadsUpView) inflater
                .inflate(R.layout.heads_up, new FrameLayout(context), false);
        mHolder.rootView.setHeadsUpManager(this);
        mHolder.containerView = (ViewGroup) mHolder.rootView.findViewById(R.id.content);

        // Load animations.
        mHolder.exitAnimation = AnimationUtils.loadAnimation(context, R.anim.heads_up_exit);
        mHolder.exitAnimation.setAnimationListener(new AnimationListenerAdapter() {
            @Override
            public void onAnimationEnd(@NonNull Animation animation) {
                super.onAnimationEnd(animation);
                onHide(true); // this is called inside of the #hide(boolean)
            }
        });

        IntentFilter filter = new IntentFilter();
        filter.addAction(ACTION_ALLOW_HEADSUP);
        filter.addAction(ACTION_DISALLOW_HEADSUP);
        filter.addAction(App.ACTION_EAT_HOME_PRESS_STOP);
        filter.addAction(App.ACTION_EAT_HOME_PRESS_START);
        mHolder.context.registerReceiver(mReceiver, filter);

        Config config = getConfig();
        config.registerListener(this);
        NotificationPresenter.getInstance().registerListener(this);

        mEnabled = config.isEnabled();
    }

    void onStop() {
        if (DEBUG) Log.d(TAG, "Stopping heads-up...");

        NotificationPresenter.getInstance().unregisterListener(this);
        getConfig().unregisterListener(this);
        mHolder.context.unregisterReceiver(mReceiver);
        mHandler.removeCallbacksAndMessages(null);

        // Because this class is Singleton, better
        // to release all sensitive resources manually.
        mHolder = null;
    }

    void onShow() {
        attachToWindow();
    }

    void onHide(boolean immediately) {
        if (immediately) {
            detachFromWindow();

            // Erase container view.
            mHandler.removeCallbacksAndMessages(null);
            mHolder.containerView.removeAllViews();
            mHolder.containerOffset = 0;
            mHolder.widgetList.clear();
        } else {
            final View view = mHolder.containerView;
            final Animation animation = mHolder.exitAnimation;
            view.startAnimation(animation);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onNotificationListChanged(@NonNull NotificationPresenter np, OpenNotification osbn,
                                          int event, boolean isLastEventInSequence) {
        if (!PowerUtils.isScreenOn(mHolder.context)) {
            // There's no point of showing heads-up
            // while screen is off.
            return;
        }

        assert osbn != null;

        // Block event.
        switch (event) {
            case NotificationPresenter.EVENT_POSTED:
            case NotificationPresenter.EVENT_CHANGED:
            case NotificationPresenter.EVENT_BATH:
                if (!canBeShown()) {
                    // Do not show this notification's heads-up
                    // because something important is happening.
                    if (DEBUG)
                        Log.d(TAG, "[Launch passed]: app is temporarily turned off by remote intent:"
                                + " enabled=" + mEnabled
                                + " disable_time=" + mDisableIntentTime);
                    return;
                }
                break;
        }

        // Handle event.
        switch (event) {
            case NotificationPresenter.EVENT_CHANGED:
            case NotificationPresenter.EVENT_POSTED:
                postHeadsUp(osbn);
                break;
            case NotificationPresenter.EVENT_REMOVED:
                removeHeadsUp(osbn);
                break;
            case NotificationPresenter.EVENT_BATH:
                // Fortunately there's no need to support bath
                // changing list of notification.
                break;
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onConfigChanged(@NonNull ConfigBase config,
                                @NonNull String key,
                                @NonNull Object value) {
        switch (key) {
            case Config.KEY_ENABLED:
                if (mEnabled = (boolean) value) {
                    hide(false);
                }
                break;
        }
    }

    /**
     * @return {@code true} if heads-up can be shown, {@code false} otherwise.
     */
    private boolean canBeShown() {
        long now = SystemClock.elapsedRealtime();
        long disableElapsedTime = now - mDisableIntentTime;
        return mEnabled && (disableElapsedTime > MAX_DISABLE_INTENT_DURATION || now == 0 /* emu */);
    }

    //-- ATTACH / DETACH VIEW -------------------------------------------------

    /**
     * Adds {@link #mHolder#rootView view} to window.
     *
     * @see #detachFromWindow()
     */
    private void attachToWindow() {
        // TODO: Optionally add some flags to allow heads-up to be
        // shown above the keyguard and status bar.
        WindowManager.LayoutParams lp = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.TYPE_SYSTEM_ERROR,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                        | WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL
                        | WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH
                        | WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
                PixelFormat.TRANSLUCENT);
        lp.gravity = Gravity.TOP | Gravity.CENTER_HORIZONTAL;
        mHolder.wm.addView(mHolder.rootView, lp);
    }

    /**
     * Removes {@link #mHolder#rootView view} from window.
     *
     * @see #attachToWindow()
     */
    private void detachFromWindow() {
        mHolder.wm.removeView(mHolder.rootView);
    }

    //-- HANDLING HEADS-UP ----------------------------------------------------

    public Config getConfig() {
        return Config.getInstance();
    }

    void postHeadsUp(@NonNull OpenNotification notification) {
        final ViewGroup container = mHolder.containerView;
        final ArrayList<HeadsUpNotificationView> list = mHolder.widgetList;

        int index = indexOf(notification);
        if (index != -1) {
            if (Device.hasKitKatApi() && mHolder.containerView.isLaidOut()) {
                TransitionManager.beginDelayedTransition(mHolder.containerView);
            }

            final int i = indexOf(notification);
            HeadsUpNotificationView widget = list.get(i);
            widget.setNotification(notification);
            widget.resetDecayTime();

            mHolder.rootView.preventInstantInteractivity();
        } else {

            // Get selected theme.
            // TODO: Implement custom themes.
            final String theme = getConfig().getTheme();
            final int themeRes = "dark".equals(theme)
                    ? R.style.HeadsUp_Theme_Dark
                    : R.style.HeadsUp_Theme;

            // Create a context with selected style.
            Context context = new ContextThemeWrapper(mHolder.context, themeRes);

            // Get layout resource.
            TypedArray typedArray = context.obtainStyledAttributes(
                    new int[]{R.styleable.Theme_headsUpNotificationLayout});
            final int layoutRes = typedArray.getInt(0, R.layout.heads_up_notification);
            typedArray.recycle();

            // Inflate notification widget.
            final LayoutInflater inflater = (LayoutInflater) context
                    .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            final HeadsUpNotificationView widget = (HeadsUpNotificationView) inflater
                    .inflate(layoutRes, container, false);

            // Setup widget
            widget.setHeadsUpManager(this);
            widget.setNotification(notification);
            widget.setOnClickListener(mOnWidgetClickListener);
            widget.resetDecayTime();

            int pos = container.getChildCount() - mHolder.containerOffset;
            mHolder.rootView.preventInstantInteractivity();
            container.addView(widget, pos);
            list.add(widget);

            show();
        }
    }

    public void removeHeadsUp(@NonNull OpenNotification notification) {
        int index = indexOf(notification);
        if (index != -1) {
            int size = mHolder.widgetList.size();
            if (size > 1) {
                // Remove view from the container.
                View view = mHolder.widgetList.get(index);
                addSpaceToContainer(view.getHeight());
                mHolder.containerView.removeView(view);
                mHolder.widgetList.remove(index);

                mHolder.rootView.preventInstantInteractivity();
            }
        }
    }

    /**
     * @return the position of given {@link com.achep.acdisplay.notifications.OpenNotification} in
     * {@link #mHolder#mWidgetList list}, or {@code -1} if not found.
     */
    int indexOf(final @NonNull OpenNotification notification) {
        final ArrayList<HeadsUpNotificationView> list = mHolder.widgetList;
        final int size = list.size();
        for (int i = 0; i < size; i++) {
            OpenNotification n2 = list.get(i).getNotification();
            if (NotificationUtils.hasIdenticalIds(notification, n2)) {
                return i;
            }
        }
        return -1;
    }

    /**
     * Adds empty space view to {@link #mHolder#containerView container} for a
     * short {@link #LAYOUT_ANIMATION_TIME}. This uses {@link #mHandler}!
     *
     * @param height height of space view in pixels.
     */
    private void addSpaceToContainer(final int height) {
        final Space space = new Space(mHolder.context);
        final ViewGroup.LayoutParams lp = new LinearLayout.LayoutParams(0, height);
        mHolder.containerView.addView(space, lp);
        mHolder.containerOffset++;

        // Remove space view after a while.
        mHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                mHolder.containerView.removeView(space);
                mHolder.containerOffset--;
            }
        }, LAYOUT_ANIMATION_TIME);
    }

}
