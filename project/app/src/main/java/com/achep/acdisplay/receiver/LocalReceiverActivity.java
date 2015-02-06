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
package com.achep.acdisplay.receiver;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.support.annotation.Nullable;
import android.util.Log;

import com.achep.base.utils.PackageUtils;

/**
 * Created by Artem on 12.03.14.
 */
public class LocalReceiverActivity extends Activity {

    private static final String TAG = "LocalReceiverActivity";

    private static final String HOST_LAUNCH_APP_INFO = "launch_app_info";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        handleIntent(getIntent());
        finish();
    }

    private void handleIntent(@Nullable Intent intent) {
        Uri data;
        String host;

        if (intent == null
                || (data = intent.getData()) == null
                || (host = data.getHost()) == null) return;

        switch (host) {
            case HOST_LAUNCH_APP_INFO:
                try {
                    startActivity(new Intent(
                            Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                            Uri.fromParts("package", PackageUtils.getName(this), null)));
                } catch (ActivityNotFoundException e) {
                    Log.wtf(TAG, "Failed to start ApplicationDetails activity.");
                }
                break;
        }
    }

}
