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
package com.achep.acdisplay.notifications;

import android.annotation.TargetApi;
import android.app.Notification;
import android.app.NotificationManager;
import android.content.Context;
import android.content.res.Resources;
import android.os.Build;
import android.os.Handler;
import android.service.notification.NotificationListenerService;
import android.service.notification.StatusBarNotification;
import android.support.annotation.NonNull;

import com.achep.acdisplay.App;
import com.achep.acdisplay.services.MediaService;
import com.achep.headsup.R;

/**
 * Created by Artem Chepurnoy on 15.01.2015.
 */
class NotificationListenerJellyBeanMR2 extends NotificationListener {

    private boolean mInitialized;

    @Override
    public void onListenerBind(@NonNull MediaService service) {
        mInitialized = false;

        // What is the idea of init notification?
        // Well the main goal is to access #getActiveNotifications()
        // what seems to be not possible without dirty and buggy
        // workarounds.
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                MediaService service = MediaService.sService;
                if (service == null) return;

                Resources res = service.getResources();
                Notification.Builder builder = new Notification.Builder(service)
                        .setContentTitle(res.getString(R.string.app_name))
                        .setContentText(res.getString(R.string.notification_init_text))
                        .setSmallIcon(R.drawable.stat_notify)
                        .setPriority(Notification.PRIORITY_MIN)
                        .setAutoCancel(true);

                String name = Context.NOTIFICATION_SERVICE;
                NotificationManager nm = (NotificationManager) service.getSystemService(name);
                nm.notify(App.ID_NOTIFY_INIT, builder.build());
            }
        }, 2000);
    }

    // Never gets called on pre-Lollipop.
    @Override
    public void onListenerConnected(@NonNull NotificationListenerService service) { /* unused */ }

    @Override
    public void onNotificationPosted(@NonNull NotificationListenerService service,
                                     @NonNull StatusBarNotification sbn) {
        if (mInitialized || !postActiveNotifications(service)) {
            NotificationPresenter np = NotificationPresenter.getInstance();
            np.postNotificationFromMain(service, OpenNotification.newInstance(sbn), 0);
        }
    }

    @Override
    public void onNotificationRemoved(@NonNull NotificationListenerService service,
                                      @NonNull StatusBarNotification sbn) {
        if (mInitialized || !postActiveNotifications(service)) {
            NotificationPresenter np = NotificationPresenter.getInstance();
            np.removeNotificationFromMain(OpenNotification.newInstance(sbn));
        }
    }

    @Override
    public void onListenerUnbind(@NonNull MediaService mediaService) {
        NotificationPresenter.getInstance().clearFromMain(true);
    }

    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
    @TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR2)
    private boolean postActiveNotifications(@NonNull NotificationListenerService service) {
        StatusBarNotification[] an = service.getActiveNotifications();
        if (an == null) return false;
        NotificationPresenter np = NotificationPresenter.getInstance();
        np.init(service, an);
        return mInitialized = true;
    }

}
