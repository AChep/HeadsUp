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
package com.achep.acdisplay;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.achep.acdisplay.plugins.powertoggles.ToggleReceiver;
import com.achep.base.content.ConfigBase;
import com.achep.headsup.R;

import java.util.HashMap;

/**
 * Saves all the configurations for the app.
 *
 * @author Artem Chepurnoy
 * @since 21.01.14
 */
@SuppressWarnings("ConstantConditions")
public final class Config extends ConfigBase {

    private static final String TAG = "Config";

    public static final String KEY_ENABLED = "enabled";

    // notifications
    public static final String KEY_NOTIFY_MIN_PRIORITY = "notify_min_priority";
    public static final String KEY_NOTIFY_MAX_PRIORITY = "notify_max_priority";

    // interface
    public static final String KEY_UI_THEME = "ui_theme";
    public static final String KEY_UI_SHOW_AT_TOP = "ui_at_top";
    public static final String KEY_UI_OVERLAP_STATUS_BAR = "ui_overlap_sb";
    public static final String KEY_UI_OVERRIDE_FONTS = "ui_override_fonts";
    public static final String KEY_UI_EMOTICONS = "ui_emoticons";

    // behavior
    public static final String KEY_NOTIFY_DECAY_TIME = "behavior_notify_decay_time";
    public static final String KEY_HIDE_ON_TOUCH_OUTSIDE = "behavior_hide_on_touch";
    public static final String KEY_SHOW_ON_KEYGUARD = "behavior_show_on_keyguard";
    public static final String KEY_PRIVACY = "privacy_mode";
    public static final int PRIVACY_HIDE_CONTENT_MASK = 1;
    public static final int PRIVACY_HIDE_ACTIONS_MASK = 2;
    public static final String KEY_UX_STR_ACTION = "ux_str_action";
    public static final String KEY_UX_STL_ACTION = "ux_stl_action";
    public static final int ST_DISMISS = 0;
    public static final int ST_HIDE = 1;
    public static final int ST_SNOOZE = 2;

    // triggers
    public static final String KEY_TRIG_PREVIOUS_VERSION = "trigger_previous_version";
    public static final String KEY_TRIG_HELP_READ = "trigger_dialog_help";
    public static final String KEY_TRIG_TRANSLATED = "trigger_translated";
    public static final String KEY_TRIG_LAUNCH_COUNT = "trigger_launch_count";
    public static final String KEY_TRIG_DONATION_ASKED = "trigger_donation_asked";

    private static Config sConfig;

    private boolean mEnabled;
    private int mNotifyMinPriority;
    private int mNotifyMaxPriority;
    private int mPrivacyMode;
    private int mUxStrAction;
    private int mUxStlAction;
    private int mUxNotifyDecayTime;
    private boolean mUxHideOnTouchOutside;
    private boolean mUxShowOnKeyguard;
    private boolean mUiShowAtTop;
    private boolean mUiOverlapSb;
    private boolean mUiEmoticons;
    private boolean mUiOverrideFonts;
    private String mUiTheme;

    private final Triggers mTriggers;
    private int mTrigPreviousVersion;
    private int mTrigLaunchCount;
    private boolean mTrigTranslated;
    private boolean mTrigHelpRead;
    private boolean mTrigDonationAsked;

    @NonNull
    public static synchronized Config getInstance() {
        if (sConfig == null) {
            sConfig = new Config();
        }
        return sConfig;
    }

    private Config() {
        mTriggers = new Triggers();
    }

    /**
     * Loads saved values from shared preferences.
     * This is called on {@link App app's} create.
     */
    void init(@NonNull Context context) {
        Resources res = context.getResources();
        SharedPreferences prefs = getSharedPreferences(context);
        mEnabled = prefs.getBoolean(KEY_ENABLED,
                res.getBoolean(R.bool.config_default_enabled));

        // notifications
        mNotifyMinPriority = prefs.getInt(KEY_NOTIFY_MIN_PRIORITY,
                res.getInteger(R.integer.config_default_notify_min_priority));
        mNotifyMaxPriority = prefs.getInt(KEY_NOTIFY_MAX_PRIORITY,
                res.getInteger(R.integer.config_default_notify_max_priority));

        // interface
        mUiTheme = prefs.getString(KEY_UI_THEME,
                res.getString(R.string.config_default_ui_theme));
        mUiShowAtTop = prefs.getBoolean(KEY_UI_SHOW_AT_TOP,
                res.getBoolean(R.bool.config_default_ui_at_top));
        mUiOverlapSb = prefs.getBoolean(KEY_UI_OVERLAP_STATUS_BAR,
                res.getBoolean(R.bool.config_default_ui_overlap_sb));
        mUiOverrideFonts = prefs.getBoolean(KEY_UI_OVERRIDE_FONTS,
                res.getBoolean(R.bool.config_default_ui_override_fonts));
        mUiEmoticons = prefs.getBoolean(KEY_UI_EMOTICONS,
                res.getBoolean(R.bool.config_default_ui_emoticons));

        // behavior
        mUxNotifyDecayTime = prefs.getInt(KEY_NOTIFY_DECAY_TIME,
                res.getInteger(R.integer.config_default_notify_decay_time));
        mUxHideOnTouchOutside = prefs.getBoolean(KEY_HIDE_ON_TOUCH_OUTSIDE,
                res.getBoolean(R.bool.config_default_hide_on_touch_outside_enabled));
        mUxShowOnKeyguard = prefs.getBoolean(KEY_SHOW_ON_KEYGUARD,
                res.getBoolean(R.bool.config_default_show_on_keyguard));
        mPrivacyMode = prefs.getInt(KEY_PRIVACY,
                res.getInteger(R.integer.config_default_privacy_mode));
        mUxStrAction = prefs.getInt(KEY_UX_STR_ACTION,
                res.getInteger(R.integer.config_default_str_action));
        mUxStlAction = prefs.getInt(KEY_UX_STL_ACTION,
                res.getInteger(R.integer.config_default_stl_action));

        // triggers
        mTrigHelpRead = prefs.getBoolean(KEY_TRIG_HELP_READ, false);
        mTrigTranslated = prefs.getBoolean(KEY_TRIG_TRANSLATED, false);
        mTrigPreviousVersion = prefs.getInt(KEY_TRIG_PREVIOUS_VERSION, 0);
        mTrigLaunchCount = prefs.getInt(KEY_TRIG_LAUNCH_COUNT, 0);
        mTrigDonationAsked = prefs.getBoolean(KEY_TRIG_DONATION_ASKED, false);
    }

    @Override
    protected void onCreateHashMap(@NonNull HashMap<String, ConfigBase.Option> hashMap) {
        hashMap.put(KEY_ENABLED, new ConfigBase.Option(
                "mEnabled", "setEnabled", "isEnabled", boolean.class));

        // notifications
        hashMap.put(KEY_NOTIFY_MIN_PRIORITY, new ConfigBase.Option(
                "mNotifyMinPriority", null, null, int.class));
        hashMap.put(KEY_NOTIFY_MAX_PRIORITY, new ConfigBase.Option(
                "mNotifyMaxPriority", null, null, int.class));

        // interface
        hashMap.put(KEY_UI_THEME, new ConfigBase.Option(
                "mUiTheme", null, null, String.class));
        hashMap.put(KEY_UI_EMOTICONS, new ConfigBase.Option(
                "mUiEmoticons", null, null, boolean.class));
        hashMap.put(KEY_UI_OVERLAP_STATUS_BAR, new ConfigBase.Option(
                "mUiOverlapSb", null, null, boolean.class));
        hashMap.put(KEY_UI_SHOW_AT_TOP, new ConfigBase.Option(
                "mUiShowAtTop", null, null, boolean.class));
        hashMap.put(KEY_UI_OVERRIDE_FONTS, new ConfigBase.Option(
                "mUiOverrideFonts", null, null, boolean.class));

        // other
        hashMap.put(KEY_NOTIFY_DECAY_TIME, new ConfigBase.Option(
                "mUxNotifyDecayTime", null, null, int.class));
        hashMap.put(KEY_HIDE_ON_TOUCH_OUTSIDE, new ConfigBase.Option(
                "mUxHideOnTouchOutside", null, null, boolean.class));
        hashMap.put(KEY_SHOW_ON_KEYGUARD, new ConfigBase.Option(
                "mUxShowOnKeyguard", null, null, boolean.class));
        hashMap.put(KEY_PRIVACY, new ConfigBase.Option(
                "mPrivacyMode", null, null, int.class));
        hashMap.put(KEY_UX_STR_ACTION, new ConfigBase.Option(
                "mUxStrAction", null, null, int.class));
        hashMap.put(KEY_UX_STL_ACTION, new ConfigBase.Option(
                "mUxStlAction", null, null, int.class));

        // triggers
        hashMap.put(KEY_TRIG_DONATION_ASKED, new ConfigBase.Option(
                "mTrigDonationAsked", null, null, boolean.class));
        hashMap.put(KEY_TRIG_HELP_READ, new ConfigBase.Option(
                "mTrigHelpRead", null, null, boolean.class));
        hashMap.put(KEY_TRIG_LAUNCH_COUNT, new ConfigBase.Option(
                "mTrigLaunchCount", null, null, int.class));
        hashMap.put(KEY_TRIG_PREVIOUS_VERSION, new ConfigBase.Option(
                "mTrigPreviousVersion", null, null, int.class));
        hashMap.put(KEY_TRIG_TRANSLATED, new ConfigBase.Option(
                "mTrigTranslated", null, null, boolean.class));
    }

    @Override
    protected void onOptionChanged(@NonNull Option option, @NonNull String key) {
        switch (key) {
            case KEY_ENABLED:
                ToggleReceiver.sendStateUpdate(ToggleReceiver.class, mEnabled, getContext());
                break;
        }
    }

    /**
     * Separated group of different internal triggers.
     */
    @NonNull
    public Triggers getTriggers() {
        return mTriggers;
    }

    // //////////////////////////////////////////
    // ///////////// -- OPTIONS -- //////////////
    // //////////////////////////////////////////

    /**
     * Setter for the entire app enabler.
     */
    public void setEnabled(@NonNull Context context, boolean enabled,
                           @Nullable OnConfigChangedListener listener) {
        writeFromMain(context, getOption(KEY_ENABLED), enabled, listener);
    }

    /**
     * @return minimal {@link android.app.Notification#priority} of notification to be shown.
     * @see #getNotifyMaxPriority()
     * @see android.app.Notification#priority
     */
    public int getNotifyMinPriority() {
        return mNotifyMinPriority;
    }

    /**
     * @return maximum {@link android.app.Notification#priority} of notification to be shown.
     * @see #getNotifyMinPriority()
     * @see android.app.Notification#priority
     */
    public int getNotifyMaxPriority() {
        return mNotifyMaxPriority;
    }

    public int getPrivacyMode() {
        return mPrivacyMode;
    }

    public String getTheme() {
        return mUiTheme;
    }

    public boolean isEnabled() {
        return mEnabled;
    }

    public boolean isOverridingFontsEnabled() {
        return mUiOverrideFonts;
    }

    public boolean isEmoticonsEnabled() {
        return mUiEmoticons;
    }

    /**
     * @return how long notification should be shown (in millis).
     */
    public int getNotifyDecayTime() {
        return mUxNotifyDecayTime;
    }

    /**
     * @return the action of the swipe-to-right.
     * @see #getStlAction()
     * @see #ST_DISMISS
     * @see #ST_HIDE
     * @see #ST_SNOOZE
     */
    public int getStrAction() {
        return mUxStrAction;
    }

    /**
     * @return the action of the swipe-to-left.
     * @see #getStrAction()
     * @see #ST_DISMISS
     * @see #ST_HIDE
     * @see #ST_SNOOZE
     */
    public int getStlAction() {
        return mUxStlAction;
    }

    /**
     * @return {@code true} if popups should hide on touch outside of it,
     * {@code false} otherwise.
     */
    public boolean isHideOnTouchOutsideEnabled() {
        return mUxHideOnTouchOutside;
    }

    public boolean isShownOnKeyguard() {
        return mUxShowOnKeyguard;
    }

    public boolean isShownAtTop() {
        return mUiShowAtTop;
    }

    public boolean isStatusBarOverlapEnabled() {
        return mUiOverlapSb;
    }

    // //////////////////////////////////////////
    // //////////// -- TRIGGERS -- //////////////
    // //////////////////////////////////////////

    /**
     * Contains
     *
     * @author Artem Chepurnoy
     */
    public class Triggers {

        public void setPreviousVersion(@NonNull Context context, int versionCode,
                                       @Nullable OnConfigChangedListener listener) {
            writeFromMain(context, getOption(KEY_TRIG_PREVIOUS_VERSION), versionCode, listener);
        }

        public void setHelpRead(@NonNull Context context, boolean isRead,
                                @Nullable OnConfigChangedListener listener) {
            writeFromMain(context, getOption(KEY_TRIG_HELP_READ), isRead, listener);
        }

        public void setDonationAsked(@NonNull Context context, boolean isAsked,
                                     @Nullable OnConfigChangedListener listener) {
            writeFromMain(context, getOption(KEY_TRIG_DONATION_ASKED), isAsked, listener);
        }

        public void setTranslated(@NonNull Context context, boolean translated,
                                  @Nullable OnConfigChangedListener listener) {
            writeFromMain(context, getOption(KEY_TRIG_TRANSLATED), translated, listener);
        }

        /**
         * @see #setLaunchCount(android.content.Context, int, com.achep.base.content.ConfigBase.OnConfigChangedListener)
         * @see #getLaunchCount()
         */
        public void incrementLaunchCount(@NonNull Context context,
                                         @Nullable OnConfigChangedListener listener) {
            setLaunchCount(context, getLaunchCount() + 1, listener);
        }

        /**
         * @see #incrementLaunchCount(android.content.Context, com.achep.base.content.ConfigBase.OnConfigChangedListener)
         * @see #getLaunchCount()
         */
        public void setLaunchCount(@NonNull Context context, int launchCount,
                                   @Nullable OnConfigChangedListener listener) {
            writeFromMain(context, getOption(KEY_TRIG_LAUNCH_COUNT), launchCount, listener);
        }

        /**
         * As set by {@link com.achep.acdisplay.ui.activities.MainActivity}, it returns version
         * code of previously installed AcDisplay, {@code 0} if first install.
         *
         * @return version code of previously installed AcDisplay, {@code 0} on first installation.
         * @see #setPreviousVersion(android.content.Context, int, Config.OnConfigChangedListener)
         */
        public int getPreviousVersion() {
            return mTrigPreviousVersion;
        }

        /**
         * @return the number of {@link com.achep.acdisplay.ui.activities.AcDisplayActivity}'s creations.
         * @see #incrementLaunchCount(android.content.Context, com.achep.base.content.ConfigBase.OnConfigChangedListener)
         * @see #setLaunchCount(android.content.Context, int, com.achep.base.content.ConfigBase.OnConfigChangedListener)
         */
        public int getLaunchCount() {
            return mTrigLaunchCount;
        }

        /**
         * @return {@code true} if {@link com.achep.base.ui.fragments.dialogs.HelpDialog} been read,
         * {@code false} otherwise
         * @see #setHelpRead(android.content.Context, boolean, Config.OnConfigChangedListener)
         */
        public boolean isHelpRead() {
            return mTrigHelpRead;
        }

        /**
         * @return {@code true} if the app is fully translated to currently used locale,
         * {@code false} otherwise.
         * @see #setDonationAsked(android.content.Context, boolean, com.achep.base.content.ConfigBase.OnConfigChangedListener)
         */
        public boolean isTranslated() {
            return mTrigTranslated;
        }

        public boolean isDonationAsked() {
            return mTrigDonationAsked;
        }

    }

}
