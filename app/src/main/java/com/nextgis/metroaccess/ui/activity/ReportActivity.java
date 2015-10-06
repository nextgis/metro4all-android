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

import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Paint;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v7.app.AppCompatActivity;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.Base64;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.keenfin.easypicker.PhotoPicker;
import com.loopj.android.http.AsyncHttpResponseHandler;
import com.nextgis.metroaccess.MetroApp;
import com.nextgis.metroaccess.R;
import com.nextgis.metroaccess.data.metro.StationItem;
import com.nextgis.metroaccess.util.Constants;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import cz.msebera.android.httpclient.Header;

import static com.nextgis.metroaccess.util.Constants.APP_REPORTS_DIR;
import static com.nextgis.metroaccess.util.Constants.BUNDLE_IMG_X;
import static com.nextgis.metroaccess.util.Constants.BUNDLE_IMG_Y;
import static com.nextgis.metroaccess.util.Constants.BUNDLE_PATH_KEY;
import static com.nextgis.metroaccess.util.Constants.BUNDLE_STATIONID_KEY;
import static com.nextgis.metroaccess.util.Constants.DEFINE_AREA_RESULT;
import static com.nextgis.metroaccess.util.Constants.PARAM_DEFINE_AREA;
import static com.nextgis.metroaccess.util.Constants.PARAM_SCHEME_PATH;

public class ReportActivity extends AppCompatActivity implements View.OnClickListener {
    private final static int REQUIRED_ATTACHMENT_SIZE = 1920;

    private Map<String, Integer> mStations;
    private int mStationId;
    private int mX = -1, mY = -1;
    private Bitmap mScreenshot;
    private EditText mEtEmail, mEtText;
    private Spinner mSpCategories;
    private TextView mTvDefine;
    private CheckBox mCbRemember;
    private PhotoPicker mPhotoPicker;
    private StationItem mStation;
    private SharedPreferences mPreferences;

    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_report);
        assert getSupportActionBar() != null;
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        final Bundle extras = getIntent().getExtras();
        mStationId = extras == null ? -1 : extras.getInt(BUNDLE_STATIONID_KEY, -1);
        mStations = new HashMap<>();
        String stationName = getString(R.string.sNotSet);

        Map<Integer, StationItem> stations = MetroApp.getGraph().GetStations();
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
        CategoryAdapter categories = new CategoryAdapter(this, android.R.layout.simple_list_item_1,
                getResources().getTextArray(R.array.report_categories));
        mSpCategories.setAdapter(categories);

        mTvDefine = (TextView) findViewById(R.id.tv_report_define_area);
        mTvDefine.setVisibility(mStationId == -1 ? View.GONE : View.VISIBLE);
        mTvDefine.setPaintFlags(mTvDefine.getPaintFlags() | Paint.UNDERLINE_TEXT_FLAG);
        mTvDefine.setOnClickListener(this);

        final ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, keys);
        spStations.setAdapter(adapter);
        spStations.setSelection(keys.indexOf(stationName));

        spStations.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                mStationId = mStations.get(adapter.getItem(i));
                mStation = mStationId >= 0 ? MetroApp.getGraph().GetStation(mStationId) : null;
                mScreenshot = null;
                mX = mY = -1;
                mTvDefine.setVisibility(mStationId == -1 ? View.GONE : View.VISIBLE);
                mTvDefine.setText(R.string.sReportDefineArea);
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {
                mStationId = -1;
                mStation = null;
                mScreenshot = null;
                mX = mY = -1;
                mTvDefine.setVisibility(View.GONE);
            }
        });

        final Button btnReport = (Button) findViewById(R.id.btn_report_send);
        btnReport.setOnClickListener(this);

        mEtEmail = (EditText) findViewById(R.id.et_report_email);
        mEtText = (EditText) findViewById(R.id.et_report_body);
        mEtText.addTextChangedListener(new TextWatcher() {
            public void afterTextChanged(Editable s) { }
            public void beforeTextChanged(CharSequence s, int start, int count, int after) { }

            public void onTextChanged(CharSequence s, int start, int before, int count) {
                btnReport.setEnabled(!TextUtils.isEmpty(s.toString().trim()));
            }
        });

        mPreferences = PreferenceManager.getDefaultSharedPreferences(ReportActivity.this);
        mCbRemember = (CheckBox) findViewById(R.id.cb_save_email);
        mCbRemember.setChecked(mPreferences.contains(PreferencesActivity.KEY_PREF_SAVED_EMAIL));
        mEtEmail.setText(mPreferences.getString(PreferencesActivity.KEY_PREF_SAVED_EMAIL, ""));

        mPhotoPicker = (PhotoPicker) findViewById(R.id.rv_photos);
    }

    @Override
    protected void onPause() {
        super.onPause();

        if (mCbRemember.isChecked() && android.util.Patterns.EMAIL_ADDRESS.matcher(mEtEmail.getText()).matches())
            mPreferences.edit().putString(PreferencesActivity.KEY_PREF_SAVED_EMAIL, mEtEmail.getText().toString()).apply();
        else
            mPreferences.edit().remove(PreferencesActivity.KEY_PREF_SAVED_EMAIL).apply();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                ((MetroApp) getApplication()).addEvent(Constants.SCREEN_LAYOUT, Constants.BACK, Constants.SCREEN_LAYOUT);
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
                        mTvDefine.setText(R.string.sReportRedefineArea);
                    }
                }
                break;
            default:
                mPhotoPicker.onActivityResult(requestCode, resultCode, data);
                super.onActivityResult(requestCode, resultCode, data);
                break;
        }
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.tv_report_define_area:
                if (mStation == null)
                    return;

                Intent intentView = new Intent(this, StationImageActivity.class);

                File schemaFile = new File(MetroApp.getGraph().GetCurrentRouteDataPath() + "/schemes", mStation.GetNode() + ".png");
                final Bundle bundle = new Bundle();
                bundle.putInt(BUNDLE_STATIONID_KEY, mStation.GetId());
                bundle.putString(PARAM_SCHEME_PATH, schemaFile.getPath());
                bundle.putBoolean(PARAM_DEFINE_AREA, true);

                intentView.putExtras(bundle);
                startActivityForResult(intentView, DEFINE_AREA_RESULT);
                break;
            case R.id.btn_report_send:
                if (TextUtils.isEmpty(mEtText.getText().toString().trim())) {
                    Toast.makeText(this, R.string.sReportTextNotNull, Toast.LENGTH_SHORT).show();
                    return;
                }

                if (TextUtils.isEmpty(MetroApp.getGraph().GetCurrentCity())) {
                    Toast.makeText(this, R.string.sReportCityNotNull, Toast.LENGTH_SHORT).show();
                    return;
                }

                if (!TextUtils.isEmpty(mEtEmail.getText().toString().trim()) &&
                        !android.util.Patterns.EMAIL_ADDRESS.matcher(mEtEmail.getText()).matches()) {
                    Toast.makeText(this, R.string.sReportIncorrectEmail, Toast.LENGTH_SHORT).show();
                    return;
                }

                new ReportSender().execute();
                break;
        }
    }

    private String makeJSON() {
        JSONObject jsonObject = new JSONObject();

        try {
            jsonObject.accumulate("time", System.currentTimeMillis());

            jsonObject.accumulate("city_name", MetroApp.getGraph().GetCurrentCity());
            jsonObject.accumulate("package_version", MetroApp.getGraph().GetCurrentCityDataVersion());
            jsonObject.accumulate("lang_data", MetroApp.getGraph().GetLocale());
            jsonObject.accumulate("lang_device", Locale.getDefault().getLanguage());
            jsonObject.accumulate("text", mEtText.getText().toString().trim());
            jsonObject.accumulate("cat_id", mSpCategories.getSelectedItemId());

            if (mStation != null) {
                jsonObject.accumulate("id_node", mStation.GetNode());
            }

            if (android.util.Patterns.EMAIL_ADDRESS.matcher(mEtEmail.getText()).matches()) {
                jsonObject.accumulate("email", mEtEmail.getText().toString().trim());
            }

            if (mX >= 0 && mY >= 0) {
                jsonObject.accumulate("coord_x", mX);
                jsonObject.accumulate("coord_y", mY);
            }

            ByteArrayOutputStream stream = new ByteArrayOutputStream();
            byte[] byteArray;
            String imageBase64;

            if (mScreenshot != null) {
                mScreenshot.compress(Bitmap.CompressFormat.JPEG, 50, stream);
                byteArray = stream.toByteArray();
                mScreenshot.recycle();
                mScreenshot = null;
                imageBase64 = Base64.encodeToString(byteArray, Base64.DEFAULT);
                jsonObject.accumulate("screenshot", imageBase64);
            }

            JSONArray photos = new JSONArray();
            for (String photoPath : mPhotoPicker.getImagesPath()) {
                stream.reset();

                if (photoPath == null)
                    continue;

                Bitmap photo = PhotoPicker.getBitmap(photoPath, REQUIRED_ATTACHMENT_SIZE);

                if (photo != null) {
                    photo.compress(Bitmap.CompressFormat.JPEG, 50, stream);
                    byteArray = stream.toByteArray();
                    photo.recycle();
                    imageBase64 = Base64.encodeToString(byteArray, Base64.DEFAULT);
                    photos.put(imageBase64);
                }
            }

            if (photos.length() > 0)
                jsonObject.accumulate("photos", photos);
        } catch (JSONException e) {
            e.printStackTrace();
        } catch (OutOfMemoryError e) {
            Toast.makeText(ReportActivity.this, getString(R.string.photo_fail_attach), Toast.LENGTH_SHORT).show();
        }

        return jsonObject.toString();
    }

    private class ReportSender extends AsyncTask<Void, Void, String> {
        private ProgressDialog mProgressDialog;

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            mProgressDialog = ProgressDialog.show(ReportActivity.this, null,
                    getString(R.string.sReportSending), true, false);
        }

        @Override
        protected String doInBackground(Void... objects) {
            return makeJSON();
        }

        @SuppressWarnings("deprecation")
        @Override
        protected void onPostExecute(final String jsonResult) {
            super.onPostExecute(jsonResult);

            ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
            NetworkInfo info = cm.getActiveNetworkInfo();
            boolean onWiFiOnly = mPreferences.getBoolean(PreferencesActivity.KEY_PREF_REPORT_WIFI, false);

            if (onWiFiOnly && info != null && info.getType() != ConnectivityManager.TYPE_WIFI)
                saveReport(jsonResult);
            else {
                AsyncHttpResponseHandler handler = new AsyncHttpResponseHandler() {
                    @Override
                    public void onSuccess(int statusCode, Header[] headers, byte[] responseBody) {
                        Toast.makeText(ReportActivity.this, R.string.sReportSent, Toast.LENGTH_SHORT).show();
                    }

                    @Override
                    public void onFailure(int statusCode, Header[] headers, byte[] responseBody, Throwable error) {
                        saveReport(jsonResult);
                    }
                };

                MetroApp.postJSON(getParent(), jsonResult, handler);
            }

            mProgressDialog.dismiss();
            finish();
        }

        private void saveReport(String json) {
            Toast.makeText(ReportActivity.this, R.string.sReportSentFail, Toast.LENGTH_SHORT).show();

            File file = new File(getExternalFilesDir(null), APP_REPORTS_DIR);
            if (!file.exists())
                if (!file.mkdir())
                    return;

            file = new File(file, System.currentTimeMillis() + ".json");
            FileOutputStream stream;
            try {
                stream = new FileOutputStream(file);
                stream.write(json.getBytes());
                stream.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private class CategoryAdapter extends ArrayAdapter<CharSequence> {
        private int[] mIds;

        private CategoryAdapter(Context context, int resource, CharSequence[] objects) {
            super(context, resource, objects);
            mIds = getResources().getIntArray(R.array.report_categories_id);
        }

        @Override
        public long getItemId(int position) {
            return mIds[position];
        }
    }
}
