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
package com.achep.acdisplay.ui.fragments.settings;

import android.os.Bundle;
import android.preference.ListPreference;
import android.preference.Preference;
import android.support.annotation.NonNull;
import android.text.TextUtils;

import com.achep.acdisplay.App;
import com.achep.acdisplay.Config;
import com.achep.base.content.ConfigBase;
import com.achep.headsup.R;
import com.achep.base.permissions.Permission;
import com.achep.base.ui.fragments.PreferenceFragment;

/**
 * Created by Artem on 09.02.14.
 */
public class KeyguardSettings extends PreferenceFragment implements ConfigBase.OnConfigChangedListener {

    private final ListPreferenceSetter mListPreferenceNotifyPrioritySetter =
            new ListPreferenceSetter() {

                @Override
                public void updateSummary(@NonNull Preference preference,
                                          @NonNull Config.Option option,
                                          @NonNull Object value) {
                    int pos = -(int) value + 2;
                    ListPreference cbp = (ListPreference) preference;
                    cbp.setSummary(cbp.getEntries()[pos]);
                }

            };

    private final ListPreferenceSetter mListPreferenceThemeSetter =
            new ListPreferenceSetter() {

                @Override
                public void updateSummary(@NonNull Preference preference,
                                          @NonNull Config.Option option,
                                          @NonNull Object value) {
                    String theme = (String) value;
                    ListPreference cbp = (ListPreference) preference;

                    CharSequence[] themes = cbp.getEntryValues();
                    for (int i = 0; i < themes.length; i++) {
                        if (TextUtils.equals(theme, themes[i])) {
                            cbp.setSummary(getString(
                                    R.string.settings_theme_summary,
                                    cbp.getEntries()[i]));
                            break;
                        }
                    }
                }

                @Override
                public void setValue(@NonNull Preference preference,
                                     @NonNull ConfigBase.Option option,
                                     @NonNull Object value) {
                    ListPreference cbp = (ListPreference) preference;
                    cbp.setValue((String) value);
                }

                @NonNull
                @Override
                public Object getValue(@NonNull Object value) {
                    return value;
                }

            };

    private Preference mNotifyDecayTimePreference;

    @Override
    public Config getConfig() {
        return Config.getInstance();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Permission[] permissions = App.getAccessManager().getMasterPermissions().permissions;
        requestMasterSwitch(Config.KEY_ENABLED, permissions);
        addPreferencesFromResource(R.xml.settings_fragment);
        syncPreference(Config.KEY_NOTIFY_MIN_PRIORITY, mListPreferenceNotifyPrioritySetter);
        syncPreference(Config.KEY_NOTIFY_MAX_PRIORITY, mListPreferenceNotifyPrioritySetter);
        syncPreference(Config.KEY_UI_THEME, mListPreferenceThemeSetter);
        syncPreference(Config.KEY_UI_OVERLAP_STATUS_BAR);
        syncPreference(Config.KEY_UI_OVERRIDE_FONTS);
        syncPreference(Config.KEY_UI_EMOTICONS);
        syncPreference(Config.KEY_HIDE_ON_TOUCH_OUTSIDE);
        syncPreference(Config.KEY_SHOW_ON_KEYGUARD);

        mNotifyDecayTimePreference = findPreference(Config.KEY_NOTIFY_DECAY_TIME);
    }

    @Override
    public void onResume() {
        super.onResume();
        Config config = getConfig();
        config.registerListener(this);

        updateNotifyDecayTimeSummary(config);
    }

    @Override
    public void onPause() {
        super.onPause();
        Config config = getConfig();
        config.unregisterListener(this);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onConfigChanged(@NonNull ConfigBase configBase,
                                @NonNull String key,
                                @NonNull Object value) {
        Config config = (Config) configBase;
        switch (key) {
            case Config.KEY_NOTIFY_DECAY_TIME:
                updateNotifyDecayTimeSummary(config);
                break;
        }
    }

    private void updateNotifyDecayTimeSummary(Config config) {
        mNotifyDecayTimePreference.setSummary(getString(
                R.string.settings_notify_decay_time_summary,
                Float.toString(config.getNotifyDecayTime() / 1000f)));
    }

}