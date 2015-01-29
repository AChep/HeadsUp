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
package com.achep.acdisplay.ui.activities;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.graphics.BitmapFactory;
import android.media.RingtoneManager;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;

import com.achep.acdisplay.App;
import com.achep.acdisplay.Config;
import com.achep.acdisplay.DialogHelper;
import com.achep.base.content.ConfigBase;
import com.achep.base.ui.activities.ActivityBase;
import com.achep.base.utils.PackageUtils;
import com.achep.headsup.R;

/**
 * Created by Artem on 21.01.14.
 */
public class MainActivity extends ActivityBase implements ConfigBase.OnConfigChangedListener {

    private static final String TAG = "MainActivity";

    private static void sendTestNotification(@NonNull Context context) {
        final int id = App.ID_NOTIFY_TEST;
        final Resources res = context.getResources();

        PendingIntent pendingIntent = PendingIntent.getActivity(context,
                id, new Intent(context, MainActivity.class),
                PendingIntent.FLAG_UPDATE_CURRENT);
        Notification.Builder builder = new Notification.Builder(context)
                .setContentTitle(res.getString(R.string.app_name))
                .setContentText(res.getString(R.string.notification_test_message))
                .setContentIntent(pendingIntent)
                .setLargeIcon(BitmapFactory.decodeResource(res, R.mipmap.ic_launcher))
                .setSmallIcon(R.drawable.stat_acdisplay)
                .setAutoCancel(true)
                .setSound(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION));

        NotificationManager nm = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        nm.notify(id, builder.build());
    }

    private MenuItem mSendTestNotificationMenuItem;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        requestCheckout();
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);

        Config.Triggers triggers = Config.getInstance().getTriggers();
        if (!triggers.isDonationAsked() && triggers.getLaunchCount() >= 15) {
            triggers.setDonationAsked(this, true, null);
            DialogHelper.showCryDialog(this);
        }

        handleAppUpgrade();
    }

    @Override
    protected void onResume() {
        super.onResume();

        Config.getInstance().registerListener(this);
        updateSendTestNotificationMenuItem();
    }

    @Override
    protected void onPause() {
        Config.getInstance().unregisterListener(this);
        super.onPause();
    }

    private void updateSendTestNotificationMenuItem() {
        if (mSendTestNotificationMenuItem != null) {
            boolean enabled = Config.getInstance().isEnabled();
            mSendTestNotificationMenuItem.setVisible(enabled);
        }
    }

    private void handleAppUpgrade() {
        PackageInfo pi;
        try {
            pi = getPackageManager().getPackageInfo(PackageUtils.getName(this), 0);
        } catch (PackageManager.NameNotFoundException e) {
            Log.wtf(TAG, "Failed to find my PackageInfo.");
            return;
        }

        Config.Triggers triggers = Config.getInstance().getTriggers();

        final int versionCode = pi.versionCode;
        final int versionCodeOld = triggers.getPreviousVersion();

        if (versionCodeOld < versionCode) {
            triggers.setPreviousVersion(this, pi.versionCode, null);

            if (versionCodeOld <= 3 /* version 1.0.2 */) {
                DialogHelper.showCompatDialog(MainActivity.this);
            }
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
                updateSendTestNotificationMenuItem();
                break;
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main, menu);

        mSendTestNotificationMenuItem = menu.findItem(R.id.test_action);
        updateSendTestNotificationMenuItem();
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.test_action:
                sendTestNotification(this);
                break;

            //-- DIALOGS ------------------------------------------------------

            case R.id.donate_action:
                DialogHelper.showDonateDialog(this);
                break;
            case R.id.feedback_action:
                DialogHelper.showFeedbackDialog(this);
                break;
            case R.id.about_action:
                DialogHelper.showAboutDialog(this);
                break;
            case R.id.help_action:
                DialogHelper.showHelpDialog(this);
                break;
            default:
                return super.onOptionsItemSelected(item);
        }
        return true;
    }

}
