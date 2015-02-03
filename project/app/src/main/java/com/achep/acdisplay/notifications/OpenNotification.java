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
package com.achep.acdisplay.notifications;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.app.Notification;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.SystemClock;
import android.service.notification.StatusBarNotification;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.graphics.Palette;
import android.text.TextUtils;

import com.achep.acdisplay.graphics.IconFactory;
import com.achep.base.Device;
import com.achep.base.async.AsyncTask;
import com.achep.base.interfaces.ISubscriptable;
import com.achep.base.utils.PackageUtils;
import com.achep.base.utils.smiley.SmileyParser;

import java.util.ArrayList;

/**
 * @author Artem Chepurnoy
 */
public abstract class OpenNotification implements
        ISubscriptable<OpenNotification.OnNotificationDataChangedListener> {

    private static final String TAG = "OpenNotification";

    @TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR2)
    @NonNull
    public static OpenNotification newInstance(@NonNull StatusBarNotification sbn) {
        Notification n = sbn.getNotification();
        if (Device.hasLollipopApi()) {
            return new OpenNotificationLollipop(sbn, n);
        } else if (Device.hasKitKatWatchApi()) {
            return new OpenNotificationKitKatWatch(sbn, n);
        }

        return new OpenNotificationJellyBeanMR2(sbn, n);
    }

    @NonNull
    public static OpenNotification newInstance(@NonNull Notification n) {
        if (Device.hasJellyBeanMR2Api()) {
            throw new RuntimeException("Use StatusBarNotification instead!");
        }

        return new OpenNotificationJellyBean(n);
    }

    //-- BEGIN ----------------------------------------------------------------

    public static final int EVENT_ICON = 1;
    public static final int EVENT_READ = 2;
    public static final int EVENT_BRAND_COLOR = 4;

    @Nullable
    private final StatusBarNotification mStatusBarNotification;
    @NonNull
    private final Notification mNotification;
    @Nullable
    private Action[] mActions;
    private boolean mEmoticonsEnabled;
    private boolean mMine;
    private boolean mRead;
    private long mLoadedTimestamp;
    private int mNumber;

    // Extracted
    @Nullable
    public CharSequence titleBigText;
    @Nullable
    public CharSequence titleText;
    @Nullable
    public CharSequence messageBigText;
    private CharSequence messageBigTextOrigin;
    @Nullable
    public CharSequence messageText;
    private CharSequence messageTextOrigin;
    @Nullable
    public CharSequence[] messageTextLines;
    private CharSequence[] messageTextLinesOrigin;
    @Nullable
    public CharSequence infoText;
    @Nullable
    public CharSequence subText;
    @Nullable
    public CharSequence summaryText;

    // Notification icon.
    @Nullable
    private Bitmap mIconBitmap;
    @Nullable
    private AsyncTask<Void, Void, Bitmap> mIconWorker;
    @NonNull
    private final IconFactory.IconAsyncListener mIconCallback =
            new IconFactory.IconAsyncListener() {
                @Override
                public void onGenerated(@NonNull Bitmap bitmap) {
                    mIconWorker = null;
                    setIcon(bitmap);
                }
            };

    // Brand color.
    private int mBrandColor = Color.WHITE;
    @Nullable
    private android.os.AsyncTask<Bitmap, Void, Palette> mPaletteWorker;

    // Listeners
    @NonNull
    private final ArrayList<OnNotificationDataChangedListener> mListeners = new ArrayList<>(3);

    protected OpenNotification(@Nullable StatusBarNotification sbn, @NonNull Notification n) {
        mStatusBarNotification = sbn;
        mNotification = n;
    }

    public void load(@NonNull Context context) {
        mLoadedTimestamp = SystemClock.elapsedRealtime();
        mMine = TextUtils.equals(getPackageName(), PackageUtils.getName(context));
        mActions = Action.makeFor(mNotification);
        mNumber = mNotification.number;

        // Load the brand color.
        try {
            String packageName = getPackageName();
            Drawable appIcon = context.getPackageManager().getApplicationIcon(packageName);

            final Bitmap bitmap = Bitmap.createBitmap(
                    appIcon.getMinimumWidth(),
                    appIcon.getMinimumHeight(),
                    Bitmap.Config.ARGB_4444);
            appIcon.draw(new Canvas(bitmap));
            AsyncTask.stop(mPaletteWorker);
            mPaletteWorker = Palette.generateAsync(bitmap, new Palette.PaletteAsyncListener() {
                @Override
                public void onGenerated(Palette palette) {
                    mBrandColor = palette.getVibrantColor(Color.WHITE);
                    notifyListeners(EVENT_BRAND_COLOR);
                    bitmap.recycle();
                }
            });
        } catch (PackageManager.NameNotFoundException e) { /* do nothing */ }

        // Load notification icon.
        AsyncTask.stop(mIconWorker);
        mIconWorker = IconFactory.generateAsync(context, this, mIconCallback);

        // Load all other things, such as title text, message text
        // and more and more.
        new Extractor().loadTexts(context, this);
        messageText = ensureNotEmpty(messageText);
        messageBigText = ensureNotEmpty(messageBigText);

        messageTextOrigin = messageText;
        messageBigTextOrigin = messageBigText;
        messageTextLinesOrigin = messageTextLines == null ? null : messageTextLines.clone();

        // Initially load emoticons.
        if (mEmoticonsEnabled) {
            mEmoticonsEnabled = false;
            setEmoticonsEnabled(true);
        }
    }

    @Nullable
    private CharSequence ensureNotEmpty(@Nullable CharSequence cs) {
        return TextUtils.isEmpty(cs) ? null : cs;
    }

    /**
     * @return The {@link android.service.notification.StatusBarNotification} or
     * {@code null}.
     */
    @Nullable
    public StatusBarNotification getStatusBarNotification() {
        return mStatusBarNotification;
    }

    /**
     * @return The {@link Notification} supplied to
     * {@link android.app.NotificationManager#notify(int, Notification)}.
     */
    @NonNull
    public Notification getNotification() {
        return mNotification;
    }

    /**
     * Array of all {@link Action} structures attached to this notification.
     */
    @Nullable
    public Action[] getActions() {
        return mActions;
    }

    @Nullable
    public Bitmap getIcon() {
        return mIconBitmap;
    }

    /**
     * The number of events that this notification represents. For example, in a new mail
     * notification, this could be the number of unread messages.
     * <p/>
     * The system may or may not use this field to modify the appearance of the notification. For
     * example, before {@link android.os.Build.VERSION_CODES#HONEYCOMB}, this number was
     * superimposed over the icon in the status bar. Starting with
     * {@link android.os.Build.VERSION_CODES#HONEYCOMB}, the template used by
     * {@link Notification.Builder} has displayed the number in the expanded notification view.
     * <p/>
     * If the number is 0 or negative, it is never shown.
     */
    public int getNumber() {
        return mNumber;
    }

    public int getBrandColor() {
        return mBrandColor;
    }

    /**
     * @return {@code true} if user has seen the notification,
     * {@code false} otherwise.
     * @see #markAsRead()
     * @see #setRead(boolean)
     */
    public boolean isRead() {
        return mRead;
    }

    //-- COMPARING INSTANCES --------------------------------------------------

    /**
     * {@inheritDoc}
     */
    @Override
    public abstract int hashCode();

    /**
     * {@inheritDoc}
     */
    @SuppressWarnings("EqualsWhichDoesntCheckParameterClass")
    @Override
    public abstract boolean equals(Object o);

    /**
     * Note, that method does not equals with {@link #equals(Object)} method.
     *
     * @param n notification to compare with.
     * @return {@code true} if notifications are from the same source and will
     * be handled by system as same notifications, {@code false} otherwise.
     */
    @SuppressLint("NewApi")
    @SuppressWarnings("ConstantConditions")
    public abstract boolean hasIdenticalIds(@Nullable OpenNotification n);

    //-- NOTIFICATION DATA ----------------------------------------------------

    /**
     * Interface definition for a callback to be invoked
     * when date of notification is changed.
     */
    public interface OnNotificationDataChangedListener {

        /**
         * @see #EVENT_ICON
         * @see #EVENT_READ
         */
        public void onNotificationDataChanged(@NonNull OpenNotification notification, int event);

    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void registerListener(@NonNull OnNotificationDataChangedListener listener) {
        mListeners.add(listener);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void unregisterListener(@NonNull OnNotificationDataChangedListener listener) {
        mListeners.remove(listener);
    }

    /**
     * Notifies all listeners about this event.
     *
     * @see com.achep.acdisplay.notifications.OpenNotification.OnNotificationDataChangedListener
     * @see #registerListener(com.achep.acdisplay.notifications.OpenNotification.OnNotificationDataChangedListener)
     */
    private void notifyListeners(int event) {
        for (OnNotificationDataChangedListener listener : mListeners) {
            listener.onNotificationDataChanged(this, event);
        }
    }

    private void setIcon(@Nullable Bitmap bitmap) {
        if (mIconBitmap == (mIconBitmap = bitmap)) return;
        notifyListeners(EVENT_ICON);
    }

    //-- EMOTICONS ------------------------------------------------------------

    public void setEmoticonsEnabled(boolean enabled) {
        if (mEmoticonsEnabled == (mEmoticonsEnabled = enabled)) return;
        reformatTexts();
    }

    //-- BASICS ---------------------------------------------------------------

    private void reformatTexts() {
        messageText = reformatMessage(messageTextOrigin);
        messageBigText = reformatMessage(messageBigTextOrigin);
        if (messageTextLines != null) {
            for (int i = 0; i < messageTextLines.length; i++) {
                messageTextLines[i] = reformatMessage(messageTextLinesOrigin[i]);
            }
        }
    }

    private CharSequence reformatMessage(@Nullable CharSequence cs) {
        if (cs == null) return null;
        if (mEmoticonsEnabled) cs = SmileyParser.getInstance().addSmileySpans(cs);
        return cs;
    }

    /**
     * Marks the notification as read.
     *
     * @see #setRead(boolean)
     */
    public void markAsRead() {
        NotificationPresenter.getInstance().setNotificationRead(this, true);
    }

    /**
     * Sets the state of the notification.
     *
     * @param isRead {@code true} if user has seen the notification,
     *               {@code false} otherwise.
     * @see #markAsRead()
     */
    void setRead(boolean isRead) {
        if (mRead == (mRead = isRead)) return;
        notifyListeners(EVENT_READ);
    }

    /**
     * Dismisses this notification from system.
     *
     * @see NotificationUtils#dismissNotification(OpenNotification)
     */
    public void dismiss() {
        NotificationUtils.dismissNotification(this);
    }

    /**
     * Performs a click on notification.<br/>
     * To be clear it is not a real click but launching its content intent.
     *
     * @return {@code true} if succeed, {@code false} otherwise
     * @see NotificationUtils#startContentIntent(OpenNotification)
     */
    public boolean click() {
        return NotificationUtils.startContentIntent(this);
    }

    /**
     * Clears some notification's resources.
     */
    public void recycle() {
        AsyncTask.stop(mPaletteWorker);
        AsyncTask.stop(mIconWorker);
    }

    /**
     * @return {@code true} if notification has been posted from my own application,
     * {@code false} otherwise (or the package name can not be get).
     */
    public boolean isMine() {
        return mMine;
    }

    /**
     * @return {@code true} if notification can be dismissed by user, {@code false} otherwise.
     */
    public boolean isDismissible() {
        return isClearable();
    }

    /**
     * Convenience method to check the notification's flags for
     * either {@link Notification#FLAG_ONGOING_EVENT} or
     * {@link Notification#FLAG_NO_CLEAR}.
     */
    public boolean isClearable() {
        return ((mNotification.flags & Notification.FLAG_ONGOING_EVENT) == 0)
                && ((mNotification.flags & Notification.FLAG_NO_CLEAR) == 0);
    }

    /**
     * @return the package name of notification, or a random string
     * if not possible to get the package name.
     */
    @NonNull
    public abstract String getPackageName();

    /**
     * Time since notification has been loaded; in {@link android.os.SystemClock#elapsedRealtime()}
     * format.
     */
    public long getLoadTimestamp() {
        return mLoadedTimestamp;
    }

    @Nullable
    public String getGroupKey() {
        return null;
    }

    public boolean isGroupChild() {
        return false;
    }

    public boolean isGroupSummary() {
        return false;
    }

}
