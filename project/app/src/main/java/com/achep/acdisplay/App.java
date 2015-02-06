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

import android.app.Application;
import android.support.annotation.NonNull;

import com.achep.acdisplay.blacklist.Blacklist;
import com.achep.acdisplay.permissions.AccessManager;
import com.achep.base.billing.CheckoutInternal;
import com.achep.base.utils.smiley.SmileyParser;

import org.solovyev.android.checkout.Checkout;
import org.solovyev.android.checkout.ProductTypes;
import org.solovyev.android.checkout.Products;

import java.util.Arrays;

/**
 * Created by Artem on 22.02.14.
 */
public class App extends Application {

    private static final String TAG = "App";

    public static final int ID_NOTIFY_INIT = 30;
    public static final int ID_NOTIFY_TEST = 40;

    public static final String ACTION_ENABLE = "com.achep.headsup.ENABLE";
    public static final String ACTION_DISABLE = "com.achep.headsup.DISABLE";
    public static final String ACTION_TOGGLE = "com.achep.headsup.TOGGLE";

    public static final String ACTION_EAT_HOME_PRESS_START = "com.achep.acdisplay.EAT_HOME_PRESS_START";
    public static final String ACTION_EAT_HOME_PRESS_STOP = "com.achep.acdisplay.EAT_HOME_PRESS_STOP";

    @NonNull
    private static final Products sProducts = Products.create()
            .add(ProductTypes.IN_APP, Arrays.asList(
                    "donation_1",
                    "donation_4",
                    "donation_10",
                    "donation_20",
                    "donation_50",
                    "donation_99"))
            .add(ProductTypes.SUBSCRIPTION, Arrays.asList(""));

    /**
     * Application wide {@link org.solovyev.android.checkout.Checkout} instance
     * (can be used anywhere in the app). This instance contains all available
     * products in the app.
     */
    @NonNull
    private final CheckoutInternal mCheckoutInternal = new CheckoutInternal(this, sProducts);

    @NonNull
    private AccessManager mAccessManager;

    @NonNull
    private static App instance;

    public App() {
        instance = this;
    }

    @Override
    public void onCreate() {
        mAccessManager = new AccessManager(this);

        Config.getInstance().init(this);
        Blacklist.getInstance().init(this);
        SmileyParser.init(this);

        super.onCreate();
    }

    @Override
    public void onLowMemory() {
        Config.getInstance().onLowMemory();
        Blacklist.getInstance().onLowMemory();
        mAccessManager.onLowMemory();
        super.onLowMemory();
    }

    @NonNull
    public static App get() {
        return instance;
    }

    @NonNull
    public static Checkout getCheckout() {
        return instance.mCheckoutInternal.getCheckout();
    }

    @NonNull
    public static CheckoutInternal getCheckoutInternal() {
        return instance.mCheckoutInternal;
    }

    @NonNull
    public static AccessManager getAccessManager() {
        return instance.mAccessManager;
    }

}
