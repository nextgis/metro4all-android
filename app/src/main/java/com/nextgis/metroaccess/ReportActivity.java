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

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Paint;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.provider.MediaStore;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.Base64;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.loopj.android.http.AsyncHttpResponseHandler;
import com.nextgis.metroaccess.data.StationItem;
import com.nextgis.metroaccess.util.FileUtil;

import org.apache.http.Header;
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

import static com.nextgis.metroaccess.Constants.APP_REPORTS_DIR;
import static com.nextgis.metroaccess.Constants.APP_REPORTS_PHOTOS_DIR;
import static com.nextgis.metroaccess.Constants.BUNDLE_IMG_X;
import static com.nextgis.metroaccess.Constants.BUNDLE_IMG_Y;
import static com.nextgis.metroaccess.Constants.BUNDLE_PATH_KEY;
import static com.nextgis.metroaccess.Constants.BUNDLE_STATIONID_KEY;
import static com.nextgis.metroaccess.Constants.CAMERA_REQUEST;
import static com.nextgis.metroaccess.Constants.DEFINE_AREA_RESULT;
import static com.nextgis.metroaccess.Constants.PARAM_DEFINE_AREA;
import static com.nextgis.metroaccess.Constants.PARAM_SCHEME_PATH;
import static com.nextgis.metroaccess.Constants.PICK_REQUEST;

public class ReportActivity extends ActionBarActivity implements View.OnClickListener {
    private final static int IMAGES_PER_ROW = 3;

    private Map<String, Integer> mStations;
    private int mStationId;
    private int mX = -1, mY = -1;
    private Bitmap mScreenshot;
    private EditText mEtEmail, mEtText;
    private Spinner mSpCategories;
    private RecyclerView mRecyclerView;
    private PhotoAdapter mPhotoAdapter;
    private int mRowHeight;
    private StationItem mStation;

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
        CategoryAdapter categories = new CategoryAdapter(this, android.R.layout.simple_list_item_1,
                getResources().getTextArray(R.array.report_categories));
        mSpCategories.setAdapter(categories);

        final TextView tvDefine = (TextView) findViewById(R.id.tv_report_define_area);
        tvDefine.setVisibility(mStationId == -1 ? View.GONE : View.VISIBLE);
        tvDefine.setPaintFlags(tvDefine.getPaintFlags() | Paint.UNDERLINE_TEXT_FLAG);
        tvDefine.setOnClickListener(this);

        final ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, keys);
        spStations.setAdapter(adapter);
        spStations.setSelection(keys.indexOf(stationName));

        spStations.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                mStationId = mStations.get(adapter.getItem(i));
                mStation = mStationId >= 0 ? Analytics.getGraph().GetStation(mStationId) : null;
                mScreenshot = null;
                mX = mY = -1;
                tvDefine.setVisibility(mStationId == -1 ? View.GONE : View.VISIBLE);
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {
                mStationId = -1;
                mStation = null;
                mScreenshot = null;
                mX = mY = -1;
                tvDefine.setVisibility(View.GONE);
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

        mRecyclerView = (RecyclerView) findViewById(R.id.rv_photos);
        mRecyclerView.setHasFixedSize(true);
        mRecyclerView.setLayoutManager(new GridLayoutManager(this, IMAGES_PER_ROW));
        mPhotoAdapter = new PhotoAdapter();
        mRecyclerView.setAdapter(mPhotoAdapter);
        mRecyclerView.post(new Runnable() {
            @Override
            public void run() {
                mRowHeight = mRecyclerView.getHeight();
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
                    if (data != null) {
                        mX = data.getIntExtra(BUNDLE_IMG_X, -1);
                        mY = data.getIntExtra(BUNDLE_IMG_Y, -1);
                        mScreenshot = BitmapFactory.decodeFile(data.getStringExtra(BUNDLE_PATH_KEY));
                    }
                }
                break;
            case CAMERA_REQUEST:
            case PICK_REQUEST:
                mPhotoAdapter.onActivityResult(requestCode, resultCode, data);
                break;
            default:
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

                Intent intentView = new Intent(this, StationImageView.class);

                File schemaFile = new File(MainActivity.GetGraph().GetCurrentRouteDataPath() + "/schemes", mStation.GetNode() + ".png");
                final Bundle bundle = new Bundle();
                bundle.putInt(BUNDLE_STATIONID_KEY, mStation.GetId());
                bundle.putString(PARAM_SCHEME_PATH, schemaFile.getPath());
                bundle.putBoolean(PARAM_DEFINE_AREA, true);

                intentView.putExtras(bundle);
                startActivityForResult(intentView, DEFINE_AREA_RESULT);
                break;
            case R.id.btn_report_send:
                if (TextUtils.isEmpty(mEtText.getText().toString().trim())) {
                    Toast.makeText(this, getString(R.string.sReportTextNotNull), Toast.LENGTH_SHORT).show();
                    return;
                }

                if (TextUtils.isEmpty(Analytics.getGraph().GetCurrentCity())) {
                    Toast.makeText(this, getString(R.string.sReportCityNotNull), Toast.LENGTH_SHORT).show();
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

            jsonObject.accumulate("city_name", Analytics.getGraph().GetCurrentCity());
            jsonObject.accumulate("package_version", Analytics.getGraph().GetCurrentCityDataVersion());
            jsonObject.accumulate("lang_data", Analytics.getGraph().GetLocale());
            jsonObject.accumulate("lang_device", Locale.getDefault().getLanguage());
            jsonObject.accumulate("text", mEtText.getText().toString().trim());
            jsonObject.accumulate("cat_id", mSpCategories.getSelectedItemId());

            if (mStation != null) {
                jsonObject.accumulate("id_node", mStation.GetNode());
            }

            if (!TextUtils.isEmpty(mEtEmail.getText().toString().trim())) {
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
                imageBase64 = Base64.encodeToString(byteArray, Base64.DEFAULT);
                jsonObject.accumulate("screenshot", imageBase64);
            }

            JSONArray photos = new JSONArray();
            for (String photoPath : mPhotoAdapter.getImagesPath()) {
                stream.reset();

                if (photoPath == null)
                    continue;

                Bitmap photo = BitmapFactory.decodeFile(photoPath);
                photo.compress(Bitmap.CompressFormat.JPEG, 50, stream);
                byteArray = stream.toByteArray();
                imageBase64 = Base64.encodeToString(byteArray, Base64.DEFAULT);
                photos.put(imageBase64);
            }

            if (photos.length() > 0)
                jsonObject.accumulate("photos", photos);
        } catch (JSONException e) {
            e.printStackTrace();
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
            boolean onWiFiOnly = PreferenceManager.getDefaultSharedPreferences(ReportActivity.this)
                    .getBoolean(PreferencesActivity.KEY_PREF_REPORT_WIFI, false);

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

                Analytics.postJSON(getParent(), jsonResult, handler);
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

    private class PhotoAdapter extends RecyclerView.Adapter<PhotoViewHolder> implements PhotoViewHolder.IViewHolderClick {
        private List<Bitmap> mImages;
        private List<String> mImagesPath;
        private Uri mPhotoUri;

        public PhotoAdapter() {
            mImages = new ArrayList<>();
            mImagesPath = new ArrayList<>();
            mImages.add(BitmapFactory.decodeResource(getResources(), R.drawable.ic_add_white_48dp));
            mImagesPath.add(null);
        }

        @Override
        public PhotoViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.photo_item, parent, false);
            return new PhotoViewHolder(view);
        }

        @Override
        public void onBindViewHolder(final PhotoViewHolder holder, final int position) {
            holder.setOnClickListener(this);
            holder.setPhoto(mImages.get(position));

            if (position == mImages.size() - 1)
                holder.setControl();
        }

        @Override
        public int getItemCount() {
            return mImages.size();
        }

        public List<String> getImagesPath() {
            ArrayList<String> images = new ArrayList<>();
            images.addAll(mImagesPath);
            images.remove(images.size() - 1);
            return images;
        }

        @Override
        public void onItemClick(View caller, int position) {
            switch (caller.getId()) {
                case R.id.iv_photo:
                    if (position == mImages.size() - 1) {
                        AlertDialog.Builder builder = new AlertDialog.Builder(ReportActivity.this);
                        builder.setTitle(R.string.sReportPhotoAdd);
                        builder.setItems(R.array.report_add_photos, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int item) {
                                Intent intent;
                                switch (item) {
                                    case 0:
                                        intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                                        File photo = new File(Environment.getExternalStoragePublicDirectory(
                                                Environment.DIRECTORY_DCIM), APP_REPORTS_PHOTOS_DIR);

                                        if (!photo.mkdirs() && !photo.exists()) {
                                            Toast.makeText(ReportActivity.this, R.string.sIOError, Toast.LENGTH_SHORT).show();
                                            return;
                                        }

                                        photo = new File(photo, System.currentTimeMillis() + ".jpg");
                                        mPhotoUri = Uri.fromFile(photo);
                                        intent.putExtra(android.provider.MediaStore.EXTRA_OUTPUT, mPhotoUri);
                                        startActivityForResult(intent, CAMERA_REQUEST);
                                        break;
                                    case 1:
                                        intent = new Intent(Intent.ACTION_GET_CONTENT);
                                        intent.setType("image/*");
                                        startActivityForResult(
                                                Intent.createChooser(intent, getString(R.string.sReportPhotoPick)), PICK_REQUEST);
                                        break;
                                }
                            }
                        });
                        builder.show();
                    }
                    break;
                case R.id.ib_remove:
                    mImages.remove(position);
                    mImagesPath.remove(position);
                    notifyItemRemoved(position);
                    measureParent();
                    break;
            }
        }

        public void onActivityResult(int requestCode, int resultCode, Intent data) {
            if (resultCode == RESULT_OK) {
                Bitmap selectedImage;
                String selectedImagePath = null;

                switch (requestCode) {
                    case CAMERA_REQUEST:
                        selectedImagePath = mPhotoUri.getPath();
                        break;
                    case PICK_REQUEST:
                        selectedImagePath = FileUtil.getPath(ReportActivity.this, data.getData());
                        break;
                }

                selectedImage = getThumbnail(selectedImagePath);

                if (selectedImage == null) {
                    Toast.makeText(ReportActivity.this, getString(R.string.sReportPhotoPickFail), Toast.LENGTH_SHORT).show();
                    return;
                }

                int position = mImages.size() - 1;
                mImages.add(position, selectedImage);
                mImagesPath.add(position, selectedImagePath);
                notifyItemInserted(position);
                measureParent();
            }
        }

        private Bitmap getThumbnail(String path) {
            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inJustDecodeBounds = true;
            BitmapFactory.decodeFile(path, options);
            final int REQUIRED_SIZE = 200;
            int scale = 1;

            while (options.outWidth / scale / 2 >= REQUIRED_SIZE && options.outHeight / scale / 2 >= REQUIRED_SIZE)
                scale *= 2;

            options.inSampleSize = scale;
            options.inJustDecodeBounds = false;

            return BitmapFactory.decodeFile(path, options);
        }

        private void measureParent() {
            LinearLayout.LayoutParams params = (LinearLayout.LayoutParams) mRecyclerView.getLayoutParams();
            params.height = (int) Math.ceil(1f * mImages.size() / IMAGES_PER_ROW) * mRowHeight;
            mRecyclerView.setLayoutParams(params);
        }
    }
}
