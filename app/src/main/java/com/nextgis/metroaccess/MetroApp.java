/******************************************************************************
 * Project:  Metro4All
 * Purpose:  Routing in subway.
 * Author:   Stanislav Petriakov, becomeglory@gmail.com
 ******************************************************************************
*   Copyright (C) 2014,2015 NextGIS
*
*    This program is free software: you can redistribute it and/or modify
*    it under the terms of the GNU General Public License as published by
*    the Free Software Foundation, either version 3 of the License, or
*    (at your option) any later version.
*
*    This program is distributed in the hope that it will be useful,
*    but WITHOUT ANY WARRANTY; without even the implied warranty of
*    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
*    GNU General Public License for more details.
*
*    You should have received a copy of the GNU General Public License
*    along with this program.  If not, see <http://www.gnu.org/licenses/>.
 ****************************************************************************/
package com.nextgis.metroaccess;

import android.app.Activity;
import android.app.Application;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.preference.PreferenceManager;

import com.google.android.gms.analytics.GoogleAnalytics;
import com.google.android.gms.analytics.HitBuilders;
import com.google.android.gms.analytics.Logger;
import com.google.android.gms.analytics.Tracker;
import com.loopj.android.http.AsyncHttpClient;
import com.loopj.android.http.AsyncHttpResponseHandler;
import com.nextgis.metroaccess.data.DataDownloader;
import com.nextgis.metroaccess.data.metro.GraphDataItem;
import com.nextgis.metroaccess.data.metro.MAGraph;
import com.nextgis.metroaccess.ui.activity.PreferencesActivity;
import com.nextgis.metroaccess.util.ConstantsSecured;
import com.nextgis.metroaccess.util.FileUtil;

import org.json.JSONArray;

import java.io.File;
import java.util.List;
import java.util.Locale;

import cz.msebera.android.httpclient.entity.StringEntity;
import cz.msebera.android.httpclient.message.BasicHeader;
import cz.msebera.android.httpclient.protocol.HTTP;

import static com.nextgis.metroaccess.util.Constants.APP_VERSION;
import static com.nextgis.metroaccess.util.Constants.KEY_PREF_RECENT_ARR_STATIONS;
import static com.nextgis.metroaccess.util.Constants.KEY_PREF_RECENT_DEP_STATIONS;
import static com.nextgis.metroaccess.util.Constants.ROUTE_DATA_DIR;
import static com.nextgis.metroaccess.util.Constants.SERVER;
import static com.nextgis.metroaccess.ui.activity.SelectStationActivity.getRecentStations;
import static com.nextgis.metroaccess.ui.activity.SelectStationActivity.indexOf;

public class MetroApp extends Application {
//    private static final String PROPERTY_ID = "UA-57998948-1";

    private Tracker mTracker;
    private static MAGraph mGraph;
    private static AsyncHttpClient mClient = new AsyncHttpClient();
    private static DataDownloader mDataHandler;
    private static DownloadProgressListener mListener;

    public interface DownloadProgressListener {
        void onDownloadFinished();
    }

    @Override
    public void onCreate() {
        super.onCreate();
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);

        updateApplicationStructure(prefs);

        String sCurrentCity = prefs.getString(PreferencesActivity.KEY_PREF_CITY, "");
        String sCurrentCityLang = prefs.getString(PreferencesActivity.KEY_PREF_CITYLANG, Locale.getDefault().getLanguage());
        mGraph = new MAGraph(this, sCurrentCity, getExternalFilesDir(null), sCurrentCityLang);
        mDataHandler = new DataDownloader(this);
    }

    public static MAGraph getGraph(){
		return mGraph;
	}

    public synchronized Tracker getTracker() {
        if (mTracker == null) {
            GoogleAnalytics analytics = GoogleAnalytics.getInstance(this);
            analytics.getLogger().setLogLevel(Logger.LogLevel.WARNING);
            mTracker = analytics.newTracker(R.xml.app_tracker);
            mTracker.enableAdvertisingIdCollection(true);
        }

        return mTracker;
    }

    public void reload(boolean disableGA) {
        disableGA = !disableGA;
        GoogleAnalytics.getInstance(getApplicationContext()).setAppOptOut(disableGA);

        if (!disableGA)
            getTracker().send(new HitBuilders.ScreenViewBuilder().build());
    }

    public void addEvent(String category, String action, String label, Integer... value) {
        HitBuilders.EventBuilder event = new HitBuilders.EventBuilder()
                .setCategory(category)
                .setAction(action)
                .setLabel(label);

        if (value != null && value.length > 0)
            event.setValue(value[0]);

        getTracker().send(event.build());
    }

    @SuppressWarnings("deprecation")
    public static void postJSON(Context context, String json, AsyncHttpResponseHandler handler) {
        StringEntity se = new StringEntity(json, "UTF-8");
        se.setContentType("application/json;charset=UTF-8");
        se.setContentEncoding(new BasicHeader(HTTP.CONTENT_TYPE, "application/json;charset=UTF-8"));

        mClient.post(context, ConstantsSecured.REPORT_SERVER, se, "application/json", handler);
    }

    public static void get(Context context, String URL, AsyncHttpResponseHandler handler) {
        mClient.get(context, URL, handler);
    }

    public static void downloadData(Context context, List<GraphDataItem> items, DownloadProgressListener listener) {
        if (items == null || items.isEmpty())
            return;

        int currentOrientation = ActivityInfo.SCREEN_ORIENTATION_NOSENSOR;
        if (context.getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE) {
            currentOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE;
        }

        ((Activity) context).setRequestedOrientation(currentOrientation);

        mListener = listener;
        mDataHandler.reload(context, items);
        get(context, SERVER + items.get(0).GetPath() + ".zip", mDataHandler);
    }

    public static void downloadFinish() {
        mGraph.FillRouteMetadata();

        if (mListener != null)
            mListener.onDownloadFinished();

        mListener = null;
    }

    public static void cancel(Context context) {
        mClient.cancelRequests(context, true);
    }

    private void updateApplicationStructure(SharedPreferences prefs) {
        try {
            int currentVersionCode = getPackageManager().getPackageInfo(getPackageName(), 0).versionCode;
            int savedVersionCode = prefs.getInt(APP_VERSION, 0);

            switch (savedVersionCode) {
                case 0:
                    // ==========Improvement==========
                    File oDataFolder = getExternalFilesDir(ROUTE_DATA_DIR);
                    FileUtil.deleteRecursive(oDataFolder);
                    // ==========End Improvement==========
                case 14:
                case 15:
                    // delete unnecessary data
                    SharedPreferences.Editor editor = prefs.edit();
                    editor.remove("recent_dep_counter");
                    editor.remove("recent_arr_counter");

                    JSONArray depStationsIds = getRecentStations(prefs, true);
                    JSONArray arrStationsIds = getRecentStations(prefs, false);

                    // convert recent stations to new format
                    for (int i = 0; i < 10; i++) {
                        int dep = prefs.getInt("recent_dep_stationid" + i, -1);
                        int arr = prefs.getInt("recent_arr_stationid" + i, -1);
                        editor.remove("recent_dep_stationid" + i);
                        editor.remove("recent_arr_stationid" + i);
                        editor.remove("recent_dep_portalid" + i);
                        editor.remove("recent_arr_portalid" + i);

                        if(dep != -1 && indexOf(depStationsIds, dep) == -1)
                            depStationsIds.put(dep);

                        if(arr != -1 && indexOf(arrStationsIds, arr) == -1)
                            arrStationsIds.put(arr);
                    }

                    editor.putString(KEY_PREF_RECENT_DEP_STATIONS, depStationsIds.toString());
                    editor.putString(KEY_PREF_RECENT_ARR_STATIONS, arrStationsIds.toString());
                    editor.apply();
                    break;
                default:
                    break;
            }

            if(savedVersionCode < currentVersionCode) { // update from previous version or clean install
                // save current version to preferences
                prefs.edit().putInt(APP_VERSION, currentVersionCode).apply();
            }
        } catch (PackageManager.NameNotFoundException e) {
//            e.printStackTrace();
        }
    }
}
