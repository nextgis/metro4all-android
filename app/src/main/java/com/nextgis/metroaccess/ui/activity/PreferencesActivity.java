/**
 * ***************************************************************************
 * Project:  Metro4All
 * Purpose:  Routing in subway.
 * Author:   Dmitry Baryshnikov (polimax@mail.ru)
 * Author:   Stanislav Petriakov, becomeglory@gmail.com
 * *****************************************************************************
 * Copyright (C) 2013-2015 NextGIS
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
 * **************************************************************************
 */
package com.nextgis.metroaccess.ui.activity;


import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceClickListener;
import android.preference.PreferenceActivity;
import android.preference.PreferenceCategory;
import android.preference.PreferenceManager;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.Toolbar;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;

import com.nextgis.metroaccess.MetroApp;
import com.nextgis.metroaccess.R;
import com.nextgis.metroaccess.data.metro.GraphDataItem;
import com.nextgis.metroaccess.data.metro.MAGraph;
import com.nextgis.metroaccess.util.Constants;
import com.nextgis.metroaccess.util.FileUtil;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static com.nextgis.metroaccess.util.Constants.APP_REPORTS_DIR;
import static com.nextgis.metroaccess.util.Constants.APP_REPORTS_SCREENSHOT;
import static com.nextgis.metroaccess.util.Constants.BUNDLE_CITY_CHANGED;
import static com.nextgis.metroaccess.util.Constants.KEY_PREF_RECENT_ARR_STATIONS;
import static com.nextgis.metroaccess.util.Constants.KEY_PREF_RECENT_DEP_STATIONS;
import static com.nextgis.metroaccess.util.Constants.REMOTE_METAFILE;
import static com.nextgis.metroaccess.util.Constants.ROUTE_DATA_DIR;

public class PreferencesActivity extends PreferenceActivity implements OnSharedPreferenceChangeListener, MetroApp.DownloadProgressListener {
    public static final String KEY_CAT_DATA = "data_cat";
    public static final String KEY_PREF_USER_TYPE = "user_type";
    public static final String KEY_PREF_UPDROUTEDATA = "update_route_data";
    public static final String KEY_PREF_CHANGE_CITY_BASES = "change_city_bases";
    public static final String KEY_PREF_DATA_LOCALE = "data_loc";
    public static final String KEY_PREF_LEGEND = "legend";
    public static final String KEY_PREF_CITY = "city";
    public static final String KEY_PREF_CITYLANG = "city_lang";
    public static final String KEY_PREF_MAX_ROUTE_COUNT = "max_route_count";
    public static final String KEY_PREF_GA = "ga_enabled";
    public static final String KEY_PREF_REPORT_WIFI = "reports_wifi";
    public static final String KEY_PREF_SAVED_EMAIL = "reports_email";

    private MAGraph mGraph;

    protected ListPreference mPrefLocale;
    protected ListPreference mPrefCity;
    private Toolbar mActionBar;

    private String mSelectedCity;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.preferences);
        mActionBar.setTitle(R.string.sSettings);
        mGraph = MetroApp.getGraph();

        //add button update data
        PreferenceCategory targetCategory = (PreferenceCategory) findPreference(KEY_CAT_DATA);
        Preference legendPref = findPreference(KEY_PREF_LEGEND);
        if (legendPref != null) {
            legendPref.setOnPreferenceClickListener(new OnPreferenceClickListener() {
                public boolean onPreferenceClick(Preference preference) {
                    ((MetroApp) getApplication()).addEvent(Constants.SCREEN_PREFERENCE, Constants.LEGEND, Constants.PREFERENCE);
                    Intent intentView = new Intent(getApplicationContext(), StationImageActivity.class);
                    startActivity(intentView);
                    return true;
                }
            });
        }

        mPrefCity = (ListPreference) findPreference(KEY_PREF_CITY);
        if (mPrefCity != null) {
            mSelectedCity = mGraph.GetCurrentCity();
            updateCityList();

            if (mGraph.IsRoutingDataExist())
                mPrefCity.setSummary(mGraph.GetCurrentCityName());
        }

        mPrefLocale = (ListPreference) findPreference(KEY_PREF_CITYLANG);
        if (mPrefLocale != null) {
            int index = mPrefLocale.findIndexOfValue(mPrefLocale.getValue());

            if (index >= 0) {
                mPrefLocale.setSummary(mPrefLocale.getEntries()[index]);
            } else {
                String currCityLang = mGraph.GetLocale();
                index = mPrefLocale.findIndexOfValue(currCityLang);
                if (index < 0) {
                    index = 0;
                }
                mPrefLocale.setValue((String) mPrefLocale.getEntryValues()[index]);
                mPrefLocale.setSummary(mPrefLocale.getEntries()[index]);
            }
        }

        Preference checkUpd = new Preference(this);
        checkUpd.setKey(KEY_PREF_UPDROUTEDATA);
        checkUpd.setTitle(R.string.sPrefUpdDataTitle);
        checkUpd.setOnPreferenceClickListener(new OnPreferenceClickListener() {
            public boolean onPreferenceClick(Preference preference) {
                AlertDialog.Builder builder = new AlertDialog.Builder(PreferencesActivity.this, R.style.AppCompatDialog);
                builder.setMessage(R.string.sAreYouSure)
                        .setTitle(R.string.sQuestion)
                        .setPositiveButton(R.string.sYes, new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                ArrayList<GraphDataItem> items = new ArrayList<>(mGraph.GetRouteMetadata().values());
                                MetroApp.downloadData(PreferencesActivity.this, items, null);
                            }
                        })
                        .setNegativeButton(R.string.sNo, new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                // User cancelled the dialog
                            }
                        });

                builder.create();
                builder.show();

                return true;
            }
        });

        Preference changeCityBases = new Preference(this);
        changeCityBases.setKey(KEY_PREF_CHANGE_CITY_BASES);
        changeCityBases.setTitle(R.string.sPrefChangeCityBasesTitle);
        changeCityBases.setOnPreferenceClickListener(new OnPreferenceClickListener() {
            public boolean onPreferenceClick(Preference preference) {
                //Add and remove bases
                File oDataFolder = new File(getExternalFilesDir(""), REMOTE_METAFILE);
                String sJSON = FileUtil.readFromFile(oDataFolder);
                mGraph.OnUpdateMeta(sJSON, false);

                final List<GraphDataItem> new_items = mGraph.HasChanges();
                Collections.sort(new_items);
                final List<GraphDataItem> exist_items = new ArrayList<>(mGraph.GetRouteMetadata().values());
                Collections.sort(exist_items);

                int count = new_items.size() + exist_items.size();
                if (count == 0)
                    return false;

                final boolean[] checkedItems = new boolean[count];
                final CharSequence[] checkedItemStrings = new CharSequence[count];

                for (int i = 0; i < exist_items.size(); i++)
                    checkedItems[i] = true;

                for (int i = 0; i < exist_items.size(); i++)
                    checkedItemStrings[i] = exist_items.get(i).GetLocaleName();

                for (int i = 0; i < new_items.size(); i++)
                    checkedItems[i + exist_items.size()] = false;

                for (int i = 0; i < new_items.size(); i++)
                    checkedItemStrings[i + exist_items.size()] = new_items.get(i).GetFullName();

                final AlertDialog.Builder builder = new AlertDialog.Builder(PreferencesActivity.this, R.style.AppCompatDialog);
                builder.setTitle(R.string.sPrefChangeCityBasesTitle)
                        .setMultiChoiceItems(checkedItemStrings, checkedItems,
                                new DialogInterface.OnMultiChoiceClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which, boolean isChecked) {
                                        checkedItems[which] = isChecked;
                                    }
                                })
                        .setPositiveButton(R.string.sPrefChangeCityBasesBtn,
                                new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int id) {
                                        List<GraphDataItem> itemsToDownload = new ArrayList<>();
                                        final List<File> itemsToDelete = new ArrayList<>();

                                        for (int i = 0; i < checkedItems.length; i++)
                                            if (!(!checkedItems[i] && i >= exist_items.size() || checkedItems[i] && i < exist_items.size())) {
                                                if (i >= exist_items.size()) { // add
                                                    itemsToDownload.add(new_items.get(i - exist_items.size()));
                                                } else { //delete
                                                    File oDataFolder = new File(getExternalFilesDir(ROUTE_DATA_DIR), exist_items.get(i).GetPath());
                                                    itemsToDelete.add(oDataFolder);
                                                }
                                            }

                                        new ChangeCitiesTask(itemsToDelete, itemsToDownload).execute();
                                    }
                                })
                        .setNegativeButton(R.string.sCancel,
                                new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int id) {
                                        dialog.cancel();
                                    }
                                });

                builder.create();
                builder.show();
                return true;
            }
        });

        targetCategory.addPreference(changeCityBases);
        targetCategory.addPreference(checkUpd);

        Preference gaPreference = findPreference(KEY_PREF_GA);
        gaPreference.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object o) {
                final boolean isGAEnabled = (Boolean) o;

                if (isGAEnabled)
                    ((MetroApp) getApplication()).addEvent(Constants.SCREEN_PREFERENCE, "Enable GA", Constants.PREFERENCE);

                ((MetroApp) getApplication()).reload(isGAEnabled);

                return true;
            }
        });

        int reportsCount = 0;
        File file = new File(getExternalFilesDir(null), APP_REPORTS_DIR);
        if (file.exists() && file.isDirectory())
            for (File report : file.listFiles())
                if (report.isFile() && !report.getName().equals(APP_REPORTS_SCREENSHOT))
                    reportsCount++;

        if (reportsCount > 0)
            findPreference(KEY_PREF_REPORT_WIFI).setSummary(reportsCount + " " + getString(R.string.sReportWaiting));
    }

    @Override
    public void setContentView(int layoutResID) {
        ViewGroup contentView = (ViewGroup) LayoutInflater.from(this).inflate(R.layout.activity_preferences, new LinearLayout(this), false);

        mActionBar = (Toolbar) contentView.findViewById(R.id.action_bar);
        mActionBar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });

        ViewGroup contentWrapper = (ViewGroup) contentView.findViewById(R.id.content_wrapper);
        LayoutInflater.from(this).inflate(layoutResID, contentWrapper, true);

        getWindow().setContentView(contentView);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                finish();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    protected void onPause() {
        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(this);
        sharedPref.unregisterOnSharedPreferenceChangeListener(this);

        super.onPause();
    }

    @Override
    public void onResume() {
        super.onResume();

        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(this);
        sharedPref.registerOnSharedPreferenceChangeListener(this);
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        CharSequence newVal;

        if (key.equals(KEY_PREF_CITY)) {
            newVal = sharedPreferences.getString(key, "msk");
            mGraph.SetCurrentCity((String) newVal);    // FIXME wait until selected city is not loaded
            mPrefCity.setSummary(mGraph.GetCurrentCityName());
            setResult(RESULT_OK, new Intent().putExtra(BUNDLE_CITY_CHANGED, !mGraph.GetCurrentCity().equals(mSelectedCity)));
        } else if (key.equals(KEY_PREF_CITYLANG)) {
            newVal = sharedPreferences.getString(key, "en");
            int nIndex = mPrefLocale.findIndexOfValue((String) newVal);
            if (nIndex >= 0) {
                mPrefLocale.setSummary(mPrefLocale.getEntries()[nIndex]);
            }
            mGraph.SetLocale((String) newVal);
        }
        /*else if(key.equals(KEY_PREF_USER_TYPE))
		{
			newVal = sharedPreferences.getString(key, "1");
        	String toIntStr = (String) newVal;
    		Editor editor = PreferenceManager.getDefaultSharedPreferences(this).edit();
    		int index = Integer.parseInt(toIntStr);
    		editor.putInt(key + "_int", index);
    		editor.commit();
    		
    		index--;           
            if(index >= 0){
            	mlsNaviType.setSummary((String) mlsNaviType.getEntries()[index]);
            }
        }*/
    }

    @Override
    public void onDownloadFinished() {
        updateCityList();
    }

    protected void updateCityList() {
        Map<String, GraphDataItem> oRouteMetadata = MAGraph.sortByLocalNames(mGraph.GetRouteMetadata());

        if (oRouteMetadata.size() > 0) {
            CharSequence[] ent = new CharSequence[oRouteMetadata.size()];
            CharSequence[] ent_val = new CharSequence[oRouteMetadata.size()];
            int nCounter = 0;

            for (Map.Entry<String, GraphDataItem> entry : oRouteMetadata.entrySet()) {
                ent[nCounter] = entry.getValue().GetLocaleName();
                ent_val[nCounter] = entry.getKey();
                nCounter++;
            }

            mPrefCity.setEntries(ent);
            mPrefCity.setEntryValues(ent_val);

            int index = mPrefCity.findIndexOfValue(mPrefCity.getValue());
            if (index < 0)
                mPrefCity.setValue(mGraph.GetCurrentCity());

            mPrefCity.setEnabled(true);
            mPrefCity.setSummary(mGraph.GetCurrentCityName());
        } else {
            mPrefCity.setEnabled(false);
            mPrefCity.setSummary(null);
        }
    }

    private class ChangeCitiesTask extends AsyncTask<Void, Void, Void> {
        private List<File> mItemsToDelete;
        private List<GraphDataItem> mItemsToDownload;
        private ProgressDialog mDeleteDialog;

        public ChangeCitiesTask(List<File> itemsToDelete, List<GraphDataItem> itemsToDownload) {
            this.mItemsToDelete = itemsToDelete;
            this.mItemsToDownload = itemsToDownload;
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();

            if (hasItemsToDelete()) {
                mDeleteDialog = new ProgressDialog(PreferencesActivity.this);
                mDeleteDialog.setMessage(getString(R.string.sDeleting));
                mDeleteDialog.setIndeterminate(true);
                mDeleteDialog.setCancelable(false);
                mDeleteDialog.show();
            }
        }

        @Override
        protected Void doInBackground(Void... params) {
            for (File cityDir : mItemsToDelete)
                FileUtil.deleteRecursive(cityDir);

            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            super.onPostExecute(aVoid);

            if (hasItemsToDelete()) {
                mGraph.FillRouteMetadata();
                updateCityList();
                mDeleteDialog.dismiss();
            }

            MetroApp.downloadData(PreferencesActivity.this, mItemsToDownload, PreferencesActivity.this);
        }

        private boolean hasItemsToDelete() {
            return !mItemsToDelete.isEmpty();
        }
    }

    public static void clearRecent(SharedPreferences prefs) {
        prefs.edit().remove(KEY_PREF_RECENT_DEP_STATIONS).remove(KEY_PREF_RECENT_ARR_STATIONS).apply();
    }
}