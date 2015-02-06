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
package com.achep.acdisplay.ui.widgets.notification;

import android.annotation.SuppressLint;
import android.app.Notification;
import android.app.PendingIntent;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.ColorFilter;
import android.graphics.ColorMatrixColorFilter;
import android.graphics.drawable.Drawable;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.format.DateUtils;
import android.text.style.UnderlineSpan;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.achep.acdisplay.interfaces.INotificatiable;
import com.achep.acdisplay.notifications.Action;
import com.achep.acdisplay.notifications.Formatter;
import com.achep.acdisplay.notifications.NotificationPresenter;
import com.achep.acdisplay.notifications.NotificationUtils;
import com.achep.acdisplay.notifications.OpenNotification;
import com.achep.acdisplay.utils.BitmapUtils;
import com.achep.base.Device;
import com.achep.base.utils.ViewUtils;
import com.achep.headsup.R;

import java.util.Arrays;

/**
 * Simple notification widget that shows the title of notification,
 * its message, icon, actions and more.
 *
 * @author Artem Chepurnoy
 */
public class NotificationWidget extends LinearLayout implements INotificatiable {

    private final int mMessageLayoutRes;
    private final int mMessageMaxLines;
    private final int mActionLayoutRes;
    private final boolean mActionAddIcon;

    @Nullable
    private NotificationIcon mSmallIcon;
    private NotificationIcon mIcon;
    private TextView mTitleTextView;
    private TextView mWhenTextView;
    private TextView mSubtextTextView;
    private ViewGroup mMessageContainer;
    private View mActionsDivider;
    private ViewGroup mActionsContainer;

    private OnClickListener mOnClickListener;
    private OpenNotification mNotification;
    private ViewGroup mContent;

    private final ColorFilter mColorFilterDark;

    /**
     * Interface definition for a callback to be invoked
     * when a notification's views are clicked.
     */
    public interface OnClickListener {

        /**
         * Called on content view click.
         *
         * @param v clicked view
         * @see NotificationWidget#getNotification()
         */
        public void onClick(NotificationWidget widget, View v);

        /**
         * Called on action button click.
         *
         * @param v      clicked view
         * @param intent action's intent
         */
        void onActionButtonClick(NotificationWidget widget, View v, PendingIntent intent);
    }

    public NotificationWidget(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public NotificationWidget(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);

        TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.NotificationWidget);
        mActionLayoutRes = a.getResourceId(
                R.styleable.NotificationWidget_actionItemLayout,
                R.layout.notification_action);
        mMessageMaxLines = a.getInt(R.styleable.NotificationWidget_messageMaxLines, 4);
        mMessageLayoutRes = a.getResourceId(
                R.styleable.NotificationWidget_messageItemLayout,
                R.layout.notification_message);
        mActionAddIcon = a.getBoolean(R.styleable.NotificationWidget_actionItemShowIcon, true);
        a.recycle();

        float v = -1f;
        float[] colorMatrix = {
                v, 0, 0, 0, 0,
                0, v, 0, 0, 0,
                0, 0, v, 0, 0,
                0, 0, 0, 1, 0
        };
        mColorFilterDark = new ColorMatrixColorFilter(colorMatrix);
    }

    /**
     * Register a callback to be invoked when notification views are clicked.
     * If some of them are not clickable, they becomes clickable.
     */
    public void setOnClickListener(OnClickListener l) {
        View.OnClickListener listener = l == null
                ? null
                : new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mOnClickListener != null) {
                    NotificationWidget widget = NotificationWidget.this;
                    mOnClickListener.onClick(widget, v);
                }
            }
        };

        mOnClickListener = l;
        mContent.setOnClickListener(listener);
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();

        mContent = (ViewGroup) findViewById(R.id.content);
        mIcon = (NotificationIcon) findViewById(R.id.icon);
        mSmallIcon = (NotificationIcon) findViewById(R.id.icon_small);
        mTitleTextView = (TextView) findViewById(R.id.title);
        mMessageContainer = (ViewGroup) findViewById(R.id.message_container);
        mWhenTextView = (TextView) findViewById(R.id.when);
        mSubtextTextView = (TextView) findViewById(R.id.subtext);
        mActionsContainer = (ViewGroup) findViewById(R.id.actions);
        mActionsDivider = findViewById(R.id.actions_divider);

        if (mSmallIcon != null) mSmallIcon.setNotificationIndicateReadStateEnabled(false);
        mIcon.setNotificationIndicateReadStateEnabled(false);
    }

    private int getAverageRgb(int color) {
        final int r = Color.red(color);
        final int g = Color.green(color);
        final int b = Color.blue(color);
        return (r + g + b) / 3;
    }

    /**
     * @return {@code true} if given {@link android.widget.TextView} have dark
     * color of text (average of RGB is lower than 127), {@code false} otherwise.
     */
    protected boolean hasDarkTextColor(TextView textView) {
        int color = textView.getCurrentTextColor();
        return getAverageRgb(color) < 127;
    }

    /**
     * Updates {@link #mActionsContainer actions container} with actions
     * from given notification. Actually needs {@link android.os.Build.VERSION_CODES#KITKAT KitKat}
     * or higher Android version.
     */
    @SuppressLint("NewApi")
    private void setActions(@NonNull OpenNotification notification, @Nullable Action[] actions) {
        ViewUtils.setVisible(mActionsDivider, actions != null);
        if (actions == null) {
            mActionsContainer.removeAllViews();
            return;
        }

        int count = actions.length;
        View[] views = new View[count];

        // Find available views.
        int childCount = mActionsContainer.getChildCount();
        int a = Math.min(childCount, count);
        for (int i = 0; i < a; i++) {
            views[i] = mActionsContainer.getChildAt(i);
        }

        // Remove redundant views.
        for (int i = childCount - 1; i >= count; i--) {
            mActionsContainer.removeViewAt(i);
        }

        LayoutInflater inflater = null;
        for (int i = 0; i < count; i++) {
            final Action action = actions[i];
            View root = views[i];

            if (root == null) {
                // Initialize layout inflater only when we really need it.
                if (inflater == null) {
                    inflater = (LayoutInflater) getContext()
                            .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                    assert inflater != null;
                }

                root = inflater.inflate(
                        mActionLayoutRes,
                        mActionsContainer, false);
                root = initActionView(root);
                // We need to keep all IDs unique to make
                // TransitionManager.beginDelayedTransition(viewGroup, null)
                // work correctly!
                root.setId(mActionsContainer.getChildCount() + 1);
                mActionsContainer.addView(root);
            }

            if (action.intent != null) {
                root.setEnabled(true);
                root.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        if (mOnClickListener != null) {
                            NotificationWidget widget = NotificationWidget.this;
                            mOnClickListener.onActionButtonClick(widget, v, action.intent);
                        }
                    }
                });
            } else {
                root.setEnabled(false);
            }

            // Get message view and apply the content.
            TextView textView = root instanceof TextView
                    ? (TextView) root
                    : (TextView) root.findViewById(R.id.title);
            textView.setText(action.title);

            if (mActionAddIcon) {
                Drawable icon = NotificationUtils.getDrawable(getContext(), notification, action.icon);

                if (icon != null) {
                    final int size = getResources().getDimensionPixelSize(R.dimen.notification_action_icon_size);
                    icon.setBounds(0, 0, size, size);

                    if (hasDarkTextColor(textView)) {
                        icon = icon.mutate();
                        icon.setColorFilter(mColorFilterDark);
                    }
                }

                // Add/remove an icon.
                if (Device.hasJellyBeanMR1Api()) {
                    textView.setCompoundDrawablesRelative(icon, null, null, null);
                } else {
                    textView.setCompoundDrawables(icon, null, null, null);
                }
            } else {
                textView.setCompoundDrawables(null, null, null, null);
            }
        }
    }

    protected View initActionView(View view) {
        return view;
    }

    /**
     * Updates a message, trying to show only as much as
     * {@link #mMessageMaxLines max lines limit} allows to.
     *
     * @param lines an array of the lines of message.
     */
    @SuppressLint("CutPasteId")
    private void setMessageLines(@Nullable CharSequence[] lines) {
        if (lines == null) {
            // Hide message container. Do not delete all messages
            // because we may re-use them later.
            mMessageContainer.removeAllViews();
            return;
        }

        int[] maxlines;

        final int length = lines.length;
        maxlines = new int[length];

        int freeLines = mMessageMaxLines;
        int count = Math.min(length, freeLines);
        if (mMessageMaxLines > length) {

            // Initial setup.
            Arrays.fill(maxlines, 1);
            freeLines -= length;

            // Build list of lengths, so we don't have
            // to recalculate it every time.
            int[] msgLengths = new int[length];
            for (int i = 0; i < length; i++) {
                assert lines[i] != null;
                msgLengths[i] = lines[i].length();
            }

            while (freeLines > 0) {
                int pos = 0;
                float a = 0;
                for (int i = 0; i < length; i++) {
                    final float k = (float) msgLengths[i] / maxlines[i];
                    if (k > a) {
                        a = k;
                        pos = i;
                    }
                }
                maxlines[pos]++;
                freeLines--;
            }
        } else {
            // Show first messages.
            for (int i = 0; freeLines > 0; freeLines--, i++) {
                maxlines[i] = 1;
            }
        }

        View[] views = new View[count];

        // Find available views.
        int childCount = mMessageContainer.getChildCount();
        int a = Math.min(childCount, count);
        for (int i = 0; i < a; i++) {
            views[i] = mMessageContainer.getChildAt(i);
        }

        // Remove redundant views.
        for (int i = childCount - 1; i >= count; i--) {
            mMessageContainer.removeViewAt(i);
        }

        boolean highlightFirstLetter = count > 1;

        LayoutInflater inflater = null;
        for (int i = 0; i < count; i++) {
            View root = views[i];

            if (root == null) {
                // Initialize layout inflater only when we really need it.
                if (inflater == null) {
                    inflater = (LayoutInflater) getContext()
                            .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                    assert inflater != null;
                }

                root = inflater.inflate(
                        mMessageLayoutRes,
                        mMessageContainer, false);
                // We need to keep all IDs unique to make
                // TransitionManager.beginDelayedTransition(viewGroup, null)
                // work correctly!
                root.setId(mMessageContainer.getChildCount() + 1);
                mMessageContainer.addView(root);
            }

            CharSequence text;

            char symbol = lines[i].charAt(0);
            boolean isClear = Character.isLetter(symbol) || Character.isDigit(symbol);
            if (highlightFirstLetter && isClear) {
                SpannableString spannable = new SpannableString(lines[i]);
                spannable.setSpan(new UnderlineSpan(), 0, 1, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                text = spannable;
            } else {
                text = lines[i];
            }

            // Get message view and apply the content.
            TextView textView = root instanceof TextView
                    ? (TextView) root
                    : (TextView) root.findViewById(R.id.message);
            textView.setMaxLines(maxlines[i]);
            textView.setText(text);
        }
    }

    /**
     * Sets {@link #mSmallIcon icon} to track notification's small icon or
     * hides it if you pass {@code null} as parameter.
     *
     * @param notification a notification to load icon from, or {@code null} to hide view.
     */
    private void setSmallIcon(@Nullable OpenNotification notification) {
        if (mSmallIcon != null) {
            mSmallIcon.setNotification(notification);
            ViewUtils.setVisible(mSmallIcon, notification != null);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public OpenNotification getNotification() {
        return mNotification;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setNotification(OpenNotification osbn) {
        mNotification = osbn;
        if (osbn == null) {
            // TODO: Hide everything or show a notice to user.
            return;
        }

        Notification n = osbn.getNotification();
        Bitmap bitmap = n.largeIcon;
        if (bitmap != null) {
            int averageColor;
            if (BitmapUtils.hasTransparentCorners(bitmap) // Not a profile icon.
                    // Title text is dark.
                    && hasDarkTextColor(mTitleTextView)
                    // Icon has white color.
                    && Color.red(averageColor = BitmapUtils.getAverageColor(bitmap)) > 200
                    && Color.blue(averageColor) > 200
                    && Color.green(averageColor) > 200) {
                // The icon is PROBABLY not a profile icon,
                // NOT a dark one and we must reverse its color to
                // make it visible on white backgrounds.
                mIcon.setColorFilter(mColorFilterDark);
            } else {
                mIcon.setColorFilter(null);
            }

            // Disable tracking notification's icon
            // and set large icon.
            mIcon.setNotification(null);
            mIcon.setImageBitmap(bitmap);

            setSmallIcon(osbn);
        } else {
            mIcon.setNotification(osbn);
            mIcon.setColorFilter(hasDarkTextColor(mTitleTextView)
                    ? mColorFilterDark
                    : null);
            setSmallIcon(null);
        }

        Formatter formatter = NotificationPresenter.getInstance().getFormatter();
        Formatter.Data data = formatter.get(getContext(), osbn);

        mTitleTextView.setText(data.title);
        mSubtextTextView.setText(data.subtitle);
        mWhenTextView.setText(getTimestamp(n));

        setActions(osbn, data.actions);
        setMessageLines(data.messages);
    }

    private String getTimestamp(@NonNull Notification notification) {
        final long when = notification.when;
        return DateUtils.formatDateTime(getContext(), when, DateUtils.FORMAT_SHOW_TIME);
    }

}
