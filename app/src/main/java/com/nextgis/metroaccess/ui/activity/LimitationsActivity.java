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

package com.nextgis.metroaccess.ui.activity;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.SwitchCompat;
import android.support.v7.widget.Toolbar;
import android.text.method.LinkMovementMethod;
import android.text.util.Linkify;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.nextgis.metroaccess.MetroApp;
import com.nextgis.metroaccess.R;
import com.nextgis.metroaccess.util.Constants;

import java.util.regex.Pattern;

public class LimitationsActivity extends PreferenceActivity implements Preference.OnPreferenceChangeListener {
    public static final String KEY_PREF_HAVE_LIMITS = "limits";
    public static final String KEY_PREF_MAX_WIDTH = "max_width";
    public static final String KEY_PREF_WHEEL_WIDTH = "wheel_width";
//    public static final String KEY_PREF_OFFICIAL_HELP = "official_help";

    private Toolbar mActionBar;
    private SharedPreferences prefs;
    private Preference mPreferenceWheel, mPreferenceMaxWidth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        addPreferencesFromResource(R.xml.limitations);
        mActionBar.setTitle(R.string.sLimits);

        prefs = PreferenceManager.getDefaultSharedPreferences(this);

        mPreferenceMaxWidth = findPreference(KEY_PREF_MAX_WIDTH);
        mPreferenceWheel = findPreference(KEY_PREF_WHEEL_WIDTH);
        mPreferenceMaxWidth.setSummary(prefs.getString(KEY_PREF_MAX_WIDTH, "40") + " " + getString(R.string.sCM));
        mPreferenceWheel.setSummary(prefs.getString(KEY_PREF_WHEEL_WIDTH, "40") + " " + getString(R.string.sCM));
        mPreferenceMaxWidth.setOnPreferenceChangeListener(this);
        mPreferenceWheel.setOnPreferenceChangeListener(this);
        setDependency(hasLimitations(this));

//        findPreference(KEY_PREF_OFFICIAL_HELP).setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
//            @Override
//            public boolean onPreferenceClick(Preference preference) {
//                String title = String.format(getString(R.string.sLimitationsHelpDialog), Analytics.getGraph().GetCurrentCityName());
//                AlertDialog builder = new AlertDialog.Builder(preference.getContext())
//                        .setTitle(title).setMessage(Analytics.getGraph().GetOfficialHelp())
//                        .setPositiveButton(android.R.string.ok, null).create();
//                builder.show();
//                TextView message = (TextView) builder.findViewById(android.R.id.message);
//                message.setMovementMethod(LinkMovementMethod.getInstance());
//                message.setLinksClickable(true);
//                Linkify.addLinks(message, Linkify.ALL);
//
//                return false;
//            }
//        });
    }

    @Override
    public void setContentView(int layoutResID) {
        ViewGroup contentView = (ViewGroup) LayoutInflater.from(this).inflate(R.layout.activity_preferences, new LinearLayout(this), false);

        mActionBar = (Toolbar) contentView.findViewById(R.id.action_bar);
        mActionBar.inflateMenu(R.menu.menu_switch);

        SwitchCompat switchLimitations = (SwitchCompat) mActionBar.findViewById(R.id.swLimitations);
        switchLimitations.setChecked(hasLimitations(this));

        switchLimitations.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                prefs.edit().putBoolean(KEY_PREF_HAVE_LIMITS, isChecked).apply();
                switchLimitations(isChecked);
            }
        });

        mActionBar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });

        Button officialHelp = (Button) contentView.findViewById(R.id.btHelp);
        officialHelp.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String title = String.format(getString(R.string.sLimitationsHelpDialog), MetroApp.getGraph().GetCurrentCityName());
                AlertDialog builder = new AlertDialog.Builder(v.getContext(), R.style.AppCompatDialog)
                        .setTitle(title).setMessage(MetroApp.getGraph().GetOfficialHelp())
                        .setPositiveButton(android.R.string.ok, null).create();
                builder.show();
                final TextView message = (TextView) builder.findViewById(android.R.id.message);
                message.setMovementMethod(LinkMovementMethod.getInstance());
                message.setLinksClickable(true);
                Linkify.addLinks(message, Linkify.WEB_URLS);

                Pattern pattern = Pattern.compile("\\+[0-9]+\\s\\(?[0-9]*\\)?\\s[0-9\\-]+");
                String scheme = "tel:";
                Linkify.addLinks(message, pattern, scheme);

                message.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        if (message.getSelectionStart() != -1 || message.getSelectionEnd() != -1) {
                            ((MetroApp) getApplication()).addEvent(Constants.SCREEN_LIMITATIONS, Constants.HELP_LINK, Constants.SCREEN_LIMITATIONS);
                        }
                    }
                });
            }
        });
        officialHelp.setVisibility(View.VISIBLE);

        ViewGroup contentWrapper = (ViewGroup) contentView.findViewById(R.id.content_wrapper);
        LayoutInflater.from(this).inflate(layoutResID, contentWrapper, true);

        getWindow().setContentView(contentView);
    }

    @Override
    public void finish() {
        setResult(RESULT_OK);
        super.finish();
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

    private void switchLimitations(boolean bHaveLimits) {
        if (bHaveLimits)
            ((MetroApp) getApplication()).addEvent(Constants.SCREEN_PREFERENCE, "Enable " + Constants.LIMITATIONS, Constants.PREFERENCE);
        else
            ((MetroApp) getApplication()).addEvent(Constants.SCREEN_PREFERENCE, "Disable " + Constants.LIMITATIONS, Constants.PREFERENCE);

        setDependency(bHaveLimits);
    }

    private void setDependency(boolean state) {
        mPreferenceMaxWidth.setEnabled(state);
        mPreferenceWheel.setEnabled(state);
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
//        if (preference.getKey().equals(KEY_PREF_HAVE_LIMITS)) {
//            switchLimitations((Boolean) newValue);
//            return true;
//        } else {//if (preference.getKey().equals(KEY_PREF_MAX_WIDTH) || preference.getKey().equals(KEY_PREF_WHEEL_WIDTH))
        	String sNewValue = ((String) newValue).trim();

            if (isInt(sNewValue)) {
                prefs.edit().putInt(preference.getKey() + "_int", Integer.parseInt(sNewValue) * 10).apply();
                preference.setSummary(sNewValue + " " + getString(R.string.sCM));
                return true;
            } else return false;
//        }
    }

    private static boolean isInt(String str) {
        try {
            return Integer.parseInt(str) > 0;
        } catch (NumberFormatException nfe) {}

        return false;
//        return str.matches("^[0-9]+$");
    }

    public static boolean hasLimitations(Context context) {
        return PreferenceManager.getDefaultSharedPreferences(context).getBoolean(KEY_PREF_HAVE_LIMITS, false);
    }

    public static int getMaxWidth(Context context) {
        return PreferenceManager.getDefaultSharedPreferences(context).getInt(LimitationsActivity.KEY_PREF_MAX_WIDTH + "_int", 400);
    }

    public static int getWheelWidth(Context context) {
        return PreferenceManager.getDefaultSharedPreferences(context).getInt(LimitationsActivity.KEY_PREF_WHEEL_WIDTH + "_int", 400);
    }

}
