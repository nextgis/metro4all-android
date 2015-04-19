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

import android.content.Intent;
import android.graphics.Paint;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.TextView;

import com.nextgis.metroaccess.data.StationItem;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.nextgis.metroaccess.Constants.BUNDLE_STATIONID_KEY;
import static com.nextgis.metroaccess.Constants.DEFINE_AREA_RESULT;
import static com.nextgis.metroaccess.Constants.PARAM_DEFINE_AREA;
import static com.nextgis.metroaccess.Constants.PARAM_SCHEME_PATH;

public class ReportActivity extends ActionBarActivity {
    private Map<String, Integer> mStations;
    private int mStationId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_report);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        final Bundle extras = getIntent().getExtras();
        mStationId = extras == null ? -1 : extras.getInt(BUNDLE_STATIONID_KEY, -1);
        mStations = new HashMap<>();
        String stationName = getString(R.string.sNotSet);

        Map<Integer, StationItem> stations = Analytics.getGraph().GetStations();
        for (Map.Entry<Integer, StationItem> station : stations.entrySet()) {
            mStations.put(station.getValue().GetName(), station.getKey());

            if (mStationId == station.getKey())
                stationName = station.getValue().GetName();
        }

        Spinner spStations = (Spinner) findViewById(R.id.sp_station);
        List<String> keys = new ArrayList<>(mStations.keySet());
        Collections.sort(keys);
        keys.add(0, getString(R.string.sNotSet));
        mStations.put(getString(R.string.sNotSet), -1);

        Spinner spCategories = (Spinner) findViewById(R.id.sp_category);
        ArrayAdapter<CharSequence> categories = ArrayAdapter.createFromResource(this,
                R.array.report_categories, R.layout.support_simple_spinner_dropdown_item);
        spCategories.setAdapter(categories);

        final TextView tvDefine = (TextView) findViewById(R.id.tv_report_define_area);
        tvDefine.setVisibility(mStationId == -1 ? View.GONE : View.VISIBLE);
        tvDefine.setPaintFlags(tvDefine.getPaintFlags() | Paint.UNDERLINE_TEXT_FLAG);
        tvDefine.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intentView = new Intent(tvDefine.getContext(), StationImageView.class);

                StationItem station = Analytics.getGraph().GetStation(mStationId);
                File schemaFile = new File(MainActivity.GetGraph().GetCurrentRouteDataPath() + "/schemes", station.GetNode() + ".png");
                final Bundle bundle = new Bundle();
                bundle.putInt(BUNDLE_STATIONID_KEY, station.GetId());
                bundle.putString(PARAM_SCHEME_PATH, schemaFile.getPath());
                bundle.putBoolean(PARAM_DEFINE_AREA, true);

                intentView.putExtras(bundle);
                startActivityForResult(intentView, DEFINE_AREA_RESULT);
            }
        });

        final ArrayAdapter<String> adapter = new ArrayAdapter<>(this, R.layout.support_simple_spinner_dropdown_item, keys);
        spStations.setAdapter(adapter);
        spStations.setSelection(keys.indexOf(stationName));

        spStations.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                mStationId = mStations.get(adapter.getItem(i));
                tvDefine.setVisibility(mStationId == -1 ? View.GONE : View.VISIBLE);
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {
                mStationId = -1;
                tvDefine.setVisibility(View.GONE);
            }
        });
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                ((Analytics) getApplication()).addEvent(Analytics.SCREEN_LAYOUT, Analytics.BACK, Analytics.SCREEN_LAYOUT);
                finish();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case DEFINE_AREA_RESULT:
                if (resultCode == RESULT_OK) {
                    // TODO add attachment screen
                }
                break;
            default:
                super.onActivityResult(requestCode, resultCode, data);
                break;
        }
    }
}
