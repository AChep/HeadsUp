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
package com.achep.base.ui.fragments.dialogs;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.graphics.Color;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.text.Html;
import android.text.Spanned;
import android.util.Log;
import android.view.ContextThemeWrapper;
import android.view.View;
import android.widget.Toast;

import com.achep.headsup.R;
import com.achep.base.Build;
import com.achep.base.ui.DialogBuilder;
import com.achep.base.utils.ToastUtils;

/**
 * Dialog fragment that shows some info about this application.
 *
 * @author Artem Chepurnoy
 */
public class AboutDialog extends DialogFragment {

    private static final String TAG = "AboutDialog";

    private static final int EASTER_EGGS_CLICK_NUMBER = 5;

    private int mTitleClickNumber;
    private ToastUtils.SingleToast mTimeStampToast;

    /**
     * Merges app name and version name into one.
     */
    @NonNull
    public static Spanned getVersionName(@NonNull Context context) {
        PackageManager pm = context.getPackageManager();
        String packageName = context.getPackageName();
        String versionName;
        try {
            PackageInfo info = pm.getPackageInfo(packageName, 0);
            versionName = info.versionName;

            // Make the info part of version name a bit smaller.
            if (versionName.indexOf('-') >= 0) {
                versionName = versionName.replaceFirst("\\-", "<small>-") + "</small>";
            }
        } catch (PackageManager.NameNotFoundException e) {
            versionName = "N/A";
        }

        /*
            int[] attribute = {android.R.attr.textColorHint};
            TypedArray a = context.obtainStyledAttributes(android.R.attr.textAppearance, attribute);
            int color = a.getColor(0, 0xFF888888);
            a.recycle();
        */
        // TODO: Get the color from current theme.
        int color = 0xFF888888;

        Resources res = context.getResources();
        return Html.fromHtml(
                res.getString(R.string.about_dialog_title,
                        res.getString(R.string.app_name),
                        versionName,
                        Integer.toHexString(Color.red(color))
                                + Integer.toHexString(Color.green(color))
                                + Integer.toHexString(Color.blue(color)))
        );
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        mTimeStampToast = new ToastUtils.SingleToast(activity);
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        ContextThemeWrapper context = getActivity();
        assert context != null;

        String year = Build.TIME_STAMP.substring(Build.TIME_STAMP.lastIndexOf(' ') + 1);
        if (year.charAt(0) != '2') Log.w(TAG, "The build year is corrupted! Check build script.");
        CharSequence credits = getString(R.string.about_dialog_credits);
        CharSequence message = Html.fromHtml(getString(R.string.about_dialog_message, credits, year));

        View view = new DialogBuilder(context)
                .setIcon(R.drawable.ic_action_about_white)
                .setTitle(getVersionName(context))
                .setMessage(message)
                .createView();
        View title = view.findViewById(R.id.title);
        title.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                switch (mTitleClickNumber++) {
                    case EASTER_EGGS_CLICK_NUMBER:
                        /*
                        Intent intent = new Intent(getActivity(), MainActivity.class)
                                .addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
                        startActivity(intent);
                        */

                        // Reset click counter.
                        mTitleClickNumber = 0;
                        break;
                    default:
                        mTimeStampToast.show(Build.TIME_STAMP, Toast.LENGTH_LONG);
                }
            }

        });

        return new AlertDialog.Builder(context)
                .setView(view)
                .setNegativeButton(R.string.close, null)
                .create();
    }

}
