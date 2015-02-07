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

import android.service.notification.NotificationListenerService;
import android.service.notification.StatusBarNotification;
import android.support.annotation.NonNull;

import com.achep.acdisplay.services.MediaService;

/**
 * Created by Artem Chepurnoy on 15.01.2015.
 */
class NotificationListenerJellyBeanMR2 extends NotificationListener {

    @Override
    public void onListenerBind(@NonNull MediaService service) {

    }

    // Never gets called on pre-Lollipop.
    @Override
    public void onListenerConnected(@NonNull NotificationListenerService service) { /* unused */ }

    @Override
    public void onNotificationPosted(@NonNull NotificationListenerService service,
                                     @NonNull StatusBarNotification sbn) {
        NotificationPresenter np = NotificationPresenter.getInstance();
        np.postNotificationFromMain(service, OpenNotification.newInstance(sbn), 0);
    }

    @Override
    public void onNotificationRemoved(@NonNull NotificationListenerService service,
                                      @NonNull StatusBarNotification sbn) {
        NotificationPresenter np = NotificationPresenter.getInstance();
        np.removeNotificationFromMain(OpenNotification.newInstance(sbn));
    }

    @Override
    public void onListenerUnbind(@NonNull MediaService mediaService) {
        NotificationPresenter.getInstance().clearFromMain(true);
    }

}
