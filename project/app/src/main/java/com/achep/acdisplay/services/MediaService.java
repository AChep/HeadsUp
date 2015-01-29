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
package com.achep.acdisplay.services;

import android.annotation.TargetApi;
import android.content.Intent;
import android.os.Build;
import android.os.IBinder;
import android.service.notification.NotificationListenerService;
import android.service.notification.StatusBarNotification;
import android.support.annotation.NonNull;

import com.achep.acdisplay.notifications.NotificationListener;

/**
 * Created by achep on 07.06.14.
 *
 * @author Artem Chepurnoy
 */
@TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR2)
public class MediaService extends NotificationListenerService {

    private static final String TAG = "MediaService";

    public static MediaService sService;

    private final NotificationListener mNotificationListener = NotificationListener.newInstance();

    @Override
    public IBinder onBind(@NonNull Intent intent) {
        switch (intent.getAction()) {
            default:
                sService = this;
                mNotificationListener.onListenerBind(this);
                return super.onBind(intent);
        }
    }

    @Override
    public boolean onUnbind(@NonNull Intent intent) {
        switch (intent.getAction()) {
            default:
                mNotificationListener.onListenerUnbind(this);
                sService = null;
                break;
        }
        return super.onUnbind(intent);
    }

    //-- HANDLING NOTIFICATIONS -----------------------------------------------

    @Override
    public void onListenerConnected() {
        super.onListenerConnected();
        mNotificationListener.onListenerConnected(this);
    }

    @Override
    public void onNotificationPosted(@NonNull StatusBarNotification notification) {
        mNotificationListener.onNotificationPosted(this, notification);
    }

    @Override
    public void onNotificationRemoved(@NonNull StatusBarNotification notification) {
        mNotificationListener.onNotificationRemoved(this, notification);
    }

}
