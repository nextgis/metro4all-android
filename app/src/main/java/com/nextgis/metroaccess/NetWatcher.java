/*
 * Project:  Metro4All
 * Purpose:  Routing in subway.
 * Author:   Stanislav Petriakov, becomeglory@gmail.com
 * *****************************************************************************
 * Copyright (c) 2015 NextGIS, info@nextgis.com
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.nextgis.metroaccess;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.preference.PreferenceManager;

import com.loopj.android.http.AsyncHttpResponseHandler;

import org.apache.http.Header;

import java.io.File;
import java.io.FileInputStream;

import static com.nextgis.metroaccess.util.Constants.APP_REPORTS_DIR;
import static com.nextgis.metroaccess.util.Constants.APP_REPORTS_SCREENSHOT;

@SuppressWarnings("deprecation")
public class NetWatcher extends BroadcastReceiver {
    @SuppressWarnings("TryFinallyCanBeTryWithResources")
    @Override
    public void onReceive(Context context, Intent i) {
        try {
            ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
            NetworkInfo info = cm.getActiveNetworkInfo();
            SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
            boolean onWiFiOnly = preferences.getBoolean(PreferencesActivity.KEY_PREF_REPORT_WIFI, false);

            if (info != null && info.isConnected()) {
                if (onWiFiOnly && info.getType() != ConnectivityManager.TYPE_WIFI)
                    return;

                File file = new File(context.getExternalFilesDir(null), APP_REPORTS_DIR);
                if (file.exists() && file.isDirectory()) {
                    for (final File report : file.listFiles()) {
                        if (report.getName().equals(APP_REPORTS_SCREENSHOT))
                            continue;

                        int length = (int) report.length();
                        byte[] bytes = new byte[length];
                        FileInputStream in = new FileInputStream(report);

                        try {
                            if (in.read(bytes) > 0) {
                                String json = new String(bytes);

                                AsyncHttpResponseHandler handler = new AsyncHttpResponseHandler() {
                                    @SuppressWarnings("ResultOfMethodCallIgnored")
                                    @Override
                                    public void onSuccess(int statusCode, Header[] headers, byte[] responseBody) {
                                        report.delete();
                                    }

                                    @Override
                                    public void onFailure(int statusCode, Header[] headers, byte[] responseBody, Throwable error) { }
                                };

                                MetroApp.postJSON(context, json, handler);
                            }
                        } finally {
                            in.close();
                        }
                    }
                }
            }
        } catch (Exception ignored) { }
    }
}