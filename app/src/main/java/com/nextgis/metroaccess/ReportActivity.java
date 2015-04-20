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
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Paint;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.text.TextUtils;
import android.util.Base64;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.nextgis.metroaccess.data.StationItem;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import static com.nextgis.metroaccess.Constants.BUNDLE_IMG_X;
import static com.nextgis.metroaccess.Constants.BUNDLE_IMG_Y;
import static com.nextgis.metroaccess.Constants.BUNDLE_PATH_KEY;
import static com.nextgis.metroaccess.Constants.BUNDLE_STATIONID_KEY;
import static com.nextgis.metroaccess.Constants.DEFINE_AREA_RESULT;
import static com.nextgis.metroaccess.Constants.PARAM_DEFINE_AREA;
import static com.nextgis.metroaccess.Constants.PARAM_SCHEME_PATH;

public class ReportActivity extends ActionBarActivity implements View.OnClickListener {
    private Map<String, Integer> mStations;
    private int mStationId;
    private int mX = -1, mY = -1;
    private Bitmap mScreenshot;
    private EditText mEtEmail, mEtText;
    private Spinner mSpCategories;

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

        mSpCategories = (Spinner) findViewById(R.id.sp_category);
        ArrayAdapter<CharSequence> categories = ArrayAdapter.createFromResource(this,
                R.array.report_categories, R.layout.support_simple_spinner_dropdown_item);
        mSpCategories.setAdapter(categories);

        final TextView tvDefine = (TextView) findViewById(R.id.tv_report_define_area);
        tvDefine.setVisibility(mStationId == -1 ? View.GONE : View.VISIBLE);
        tvDefine.setPaintFlags(tvDefine.getPaintFlags() | Paint.UNDERLINE_TEXT_FLAG);
        tvDefine.setOnClickListener(this);

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

        Button btnReport = (Button) findViewById(R.id.btn_report_send);
        btnReport.setOnClickListener(this);

        mEtEmail = (EditText) findViewById(R.id.et_report_email);
        mEtText = (EditText) findViewById(R.id.et_report_body);
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
                    if (data != null) {
                        mX = data.getIntExtra(BUNDLE_IMG_X, -1);
                        mY = data.getIntExtra(BUNDLE_IMG_Y, -1);
                        mScreenshot = BitmapFactory.decodeFile(data.getStringExtra(BUNDLE_PATH_KEY));
                    }
                }
                break;
            default:
                super.onActivityResult(requestCode, resultCode, data);
                break;
        }
    }

    @Override
    public void onClick(View view) {
        final StationItem station = mStationId >= 0 ? Analytics.getGraph().GetStation(mStationId) : null;

        switch (view.getId()) {
            case R.id.tv_report_define_area:
                if (station == null)
                    return;

                Intent intentView = new Intent(this, StationImageView.class);

                File schemaFile = new File(MainActivity.GetGraph().GetCurrentRouteDataPath() + "/schemes", station.GetNode() + ".png");
                final Bundle bundle = new Bundle();
                bundle.putInt(BUNDLE_STATIONID_KEY, station.GetId());
                bundle.putString(PARAM_SCHEME_PATH, schemaFile.getPath());
                bundle.putBoolean(PARAM_DEFINE_AREA, true);

                intentView.putExtras(bundle);
                startActivityForResult(intentView, DEFINE_AREA_RESULT);
                break;
            case R.id.btn_report_send:
                if (TextUtils.isEmpty(mEtText.getText())) {
                    Toast.makeText(this, getString(R.string.sReportTextNotNull), Toast.LENGTH_SHORT).show();
                    return;
                }

                if (TextUtils.isEmpty(Analytics.getGraph().GetCurrentCity())) {
                    Toast.makeText(this, getString(R.string.sReportCityNotNull), Toast.LENGTH_SHORT).show();
                    return;
                }

                // enhancement
                // http://stackoverflow.com/a/17647211
                // http://loopj.com/android-async-http/

                Thread thr = new Thread(new Runnable() {
                    @Override
                    public void run() {
                        HttpURLConnection conn = null;

                        try {
                            JSONObject jsonObject = new JSONObject();
                            jsonObject.accumulate("time", System.currentTimeMillis());
                            jsonObject.accumulate("city_name", Analytics.getGraph().GetCurrentCity());
                            jsonObject.accumulate("package_version", Analytics.getGraph().GetCurrentCityDataVersion());
                            jsonObject.accumulate("lang_data", Analytics.getGraph().GetLocale());
                            jsonObject.accumulate("lang_device", Locale.getDefault().getLanguage());
                            jsonObject.accumulate("text", mEtText.getText());
                            jsonObject.accumulate("cat_id", mSpCategories.getSelectedItemId());

                            if (station != null) {
                                jsonObject.accumulate("id_node", station.GetNode());
                            }

                            if (!TextUtils.isEmpty(mEtEmail.getText())) {
                                jsonObject.accumulate("email", mEtEmail.getText());
                            }

                            if (mX >= 0 && mY >= 0) {
                                jsonObject.accumulate("coord_x", mX);
                                jsonObject.accumulate("coord_y", mY);
                            }

                            if (mScreenshot != null) {
                                ByteArrayOutputStream stream = new ByteArrayOutputStream();
                                mScreenshot.compress(Bitmap.CompressFormat.JPEG, 50, stream);
                                byte[] byteArray = stream.toByteArray();
                                String imageBase64 = Base64.encodeToString(byteArray, Base64.DEFAULT);
                                jsonObject.accumulate("screenshot", imageBase64);
                            }

                            URL url = new URL("http://");
                            conn = (HttpURLConnection) url.openConnection();
                            conn.setDoInput(true);
                            conn.setDoOutput(true);
                            conn.setRequestMethod("POST");
                            conn.setRequestProperty("Content-Type", "application/json;charset=UTF-8");
                            conn.setConnectTimeout(10000);
                            conn.setReadTimeout(10000);
                            conn.connect();

                            OutputStream out = new BufferedOutputStream(conn.getOutputStream());
                            BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(out, "UTF-8"));
                            writer.write(jsonObject.toString());
                            writer.flush();
                            writer.close();
                            out.close();

                            if (conn.getResponseCode() == HttpURLConnection.HTTP_OK)
                                Toast.makeText(getApplicationContext(), "200 OK", Toast.LENGTH_SHORT).show();

                            InputStream in = new BufferedInputStream(conn.getInputStream());
                            BufferedReader reader = new BufferedReader(new InputStreamReader(in, "UTF-8"));
                            Toast.makeText(getApplicationContext(), reader.readLine(), Toast.LENGTH_LONG).show();
                        } catch (JSONException | IOException e) {
                            e.printStackTrace();
                        } finally {
                            if (conn != null)
                                conn.disconnect();
                        }
                    }
                });
                thr.start();
                break;
        }
    }
}
