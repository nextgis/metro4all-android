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

package com.nextgis.metroaccess.data;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.pm.ActivityInfo;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Environment;
import android.widget.Toast;

import com.loopj.android.http.AsyncHttpResponseHandler;
import com.nextgis.metroaccess.MetroApp;
import com.nextgis.metroaccess.R;
import com.nextgis.metroaccess.data.metro.GraphDataItem;
import com.nextgis.metroaccess.util.FileUtil;

import org.apache.http.Header;
import org.json.JSONObject;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Enumeration;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import static com.nextgis.metroaccess.util.Constants.META;
import static com.nextgis.metroaccess.util.Constants.ROUTE_DATA_DIR;
import static com.nextgis.metroaccess.util.Constants.SERVER;

@SuppressWarnings("deprecation")
public class DataDownloader extends AsyncHttpResponseHandler {
    private ProgressDialog mDownloadDialog;
    private GraphDataItem mDataItem;
    private List<GraphDataItem> mDownloadData;
    private String mTmpOutFile;
    private Context mContext;

    public void reload(Context context, List<GraphDataItem> items) {
        mContext = context;
        mDownloadData = items;
    }

    @Override
    public void onStart() {
        super.onStart();
        mDataItem = mDownloadData.get(0);

        File dir = mContext.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS);
        File zipFile = new File(dir, mDataItem.GetPath() + ".zip");
        mTmpOutFile = zipFile.getAbsolutePath();

        mDownloadDialog = new ProgressDialog(mContext);
        mDownloadDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
        mDownloadDialog.setIndeterminate(true);
        mDownloadDialog.setMessage(getString(R.string.sDownLoading) + mDataItem.GetLocaleName());
        mDownloadDialog.setCanceledOnTouchOutside(false);
        mDownloadDialog.setCancelable(true);
        mDownloadDialog.setOnCancelListener(new DialogInterface.OnCancelListener() {
            @Override
            public void onCancel(DialogInterface dialog) {
                abort(R.string.sDownLoadingCancelled);
                MetroApp.cancel(mContext);
                sendCancelMessage();
                DataDownloader.this.onCancel();
            }
        });
        mDownloadDialog.show();
    }

    @Override
    public void onSuccess(int statusCode, Header[] headers, byte[] responseBody) {
        mDownloadDialog.dismiss();

        try {
            if (mDownloadData == null || mDownloadData.isEmpty())
                throw new IOException("Bad ZipArchive");

            unzip(responseBody);
        } catch (IOException e) {
            Toast.makeText(mContext, R.string.sNetworkInvalidData, Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onFailure(int statusCode, Header[] headers, byte[] responseBody, Throwable error) {
        int info = statusCode == 0 ? R.string.sNetworkUnreachErr : R.string.sNetworkGetErr;
        abort(info);
    }

    @Override
    public void onProgress(int bytesWritten, int totalSize) {
        super.onProgress(bytesWritten, totalSize);

        if (mDownloadDialog.isIndeterminate()) // connection established
        {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB)  // set format for api 11+
                mDownloadDialog.setProgressNumberFormat("%1d / %2d Kb");

            mDownloadDialog.setIndeterminate(false); // turn off indeterminate
            mDownloadDialog.setMax(totalSize / 1024); // max value in kb
        }

        mDownloadDialog.setProgress(bytesWritten / 1024);
    }

    private void abort(int info) {
        Toast.makeText(mContext, info, Toast.LENGTH_SHORT).show();
        mDownloadDialog.dismiss();
        mDownloadData.clear();
    }

    private void unzip(byte[] data) throws IOException {
        mDownloadDialog = new ProgressDialog(mContext);
        mDownloadDialog.setMessage(getString(R.string.sZipExtractionProcess));
        mDownloadDialog.setIndeterminate(true);
        mDownloadDialog.setCancelable(false);
        mDownloadDialog.show();

        OutputStream output = new FileOutputStream(mTmpOutFile);
        output.write(data);
        output.close();

        new UnZipTask(mDataItem, mContext.getExternalFilesDir(ROUTE_DATA_DIR) + File.separator + mDataItem.GetPath()).execute(mTmpOutFile);
    }

    private String getString(int id) {
        return mContext.getString(id);
    }

    private class UnZipTask extends AsyncTask<String, Void, Boolean> {
        private GraphDataItem mDataItem;
        private String mPath;

        public UnZipTask(GraphDataItem item, String path) {
            super();
            mDataItem = item;
            mPath = path;
        }

        @Override
        protected Boolean doInBackground(String... params) {
            String filePath = params[0];
            File archive = new File(filePath);
            try {
                ZipFile zipfile = new ZipFile(archive);
                if (zipfile.size() > 0) {
                    for (Enumeration<? extends ZipEntry> e = zipfile.entries(); e.hasMoreElements(); ) {
                        ZipEntry entry = e.nextElement();
                        unzipEntry(zipfile, entry, mPath);
                    }

                    zipfile.close();
                    archive.delete();

                    JSONObject oJSONRoot = new JSONObject();
                    oJSONRoot.put("name", mDataItem.GetName());

                    for (String key : mDataItem.GetLocaleNames().keySet())
                        oJSONRoot.put(key, mDataItem.GetLocaleNames().get(key));

                    oJSONRoot.put("ver", mDataItem.GetVersion());
                    oJSONRoot.put("directed", mDataItem.GetDirected());

                    String sJSON = oJSONRoot.toString();
                    File file = new File(mPath, META);
                    return FileUtil.writeToFile(file, sJSON);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }

            return false;
        }

        @Override
        protected void onPostExecute(Boolean result) {
            mDownloadDialog.dismiss();

            if (result && !mDownloadData.isEmpty()) {
                mDownloadData.remove(0);

                if (!mDownloadData.isEmpty()) {
                    MetroApp.get(mContext, SERVER + mDownloadData.get(0).GetPath() + ".zip", DataDownloader.this);
                } else {
                    ((Activity) mContext).setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR);
                    MetroApp.downloadFinish();
                }
            } else
                abort(R.string.sIOError);
        }

        private void unzipEntry(ZipFile zipfile, ZipEntry entry, String outputDir) throws IOException {
            if (entry.isDirectory()) {
                createDir(new File(outputDir, entry.getName()));
                return;
            }

            File outputFile = new File(outputDir, entry.getName());
            if (!outputFile.getParentFile().exists()) {
                createDir(outputFile.getParentFile());
            }

            BufferedInputStream inputStream = new BufferedInputStream(zipfile.getInputStream(entry));
            BufferedOutputStream outputStream = new BufferedOutputStream(new FileOutputStream(outputFile));
            try {
                byte[] _buffer = new byte[1024];
                copyStream(inputStream, outputStream, _buffer, 1024);
            } finally {
                outputStream.flush();
                outputStream.close();
                inputStream.close();
            }
        }

        private void copyStream(InputStream is, OutputStream os, byte[] buffer, int bufferSize) throws IOException {
            for (; ; ) {
                int count = is.read(buffer, 0, bufferSize);
                if (count == -1) {
                    break;
                }
                os.write(buffer, 0, count);
            }
        }

        private void createDir(File dir) {
            if (dir.exists()) {
                return;
            }
            if (!dir.mkdirs()) {
                throw new RuntimeException("Can not create dir " + dir);
            }
        }
    }
}
