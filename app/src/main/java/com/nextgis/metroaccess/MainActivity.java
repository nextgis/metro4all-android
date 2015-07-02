/******************************************************************************
 * Project:  Metro4All
 * Purpose:  Routing in subway.
 * Author:   Dmitry Baryshnikov (polimax@mail.ru)
 * Author:   Stanislav Petriakov, becomeglory@gmail.com
 ******************************************************************************
*   Copyright (C) 2013-2015 NextGIS
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

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.PictureDrawable;
import android.location.Location;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.util.TypedValue;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.Toast;

import com.caverock.androidsvg.SVG;
import com.caverock.androidsvg.SVGParseException;
import com.google.android.gms.analytics.GoogleAnalytics;
import com.loopj.android.http.AsyncHttpResponseHandler;
import com.nextgis.metroaccess.data.GraphDataItem;
import com.nextgis.metroaccess.data.MAGraph;
import com.nextgis.metroaccess.data.PortalItem;
import com.nextgis.metroaccess.data.RouteItem;
import com.nextgis.metroaccess.data.StationItem;
import com.nextgis.metroaccess.util.Constants;
import com.nextgis.metroaccess.util.FileUtil;

import org.apache.http.Header;
import org.osmdroid.views.overlay.mylocation.GpsMyLocationProvider;
import org.osmdroid.views.overlay.mylocation.IMyLocationConsumer;
import org.osmdroid.views.overlay.mylocation.IMyLocationProvider;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import edu.asu.emit.qyan.alg.model.Path;
import edu.asu.emit.qyan.alg.model.abstracts.BaseVertex;

import static com.nextgis.metroaccess.util.Constants.ARRIVAL_RESULT;
import static com.nextgis.metroaccess.util.Constants.BUNDLE_CITY_CHANGED;
import static com.nextgis.metroaccess.util.Constants.BUNDLE_ENTRANCE_KEY;
import static com.nextgis.metroaccess.util.Constants.BUNDLE_EVENTSRC_KEY;
import static com.nextgis.metroaccess.util.Constants.BUNDLE_PATHCOUNT_KEY;
import static com.nextgis.metroaccess.util.Constants.BUNDLE_PATH_KEY;
import static com.nextgis.metroaccess.util.Constants.BUNDLE_PORTALID_KEY;
import static com.nextgis.metroaccess.util.Constants.BUNDLE_STATIONID_KEY;
import static com.nextgis.metroaccess.util.Constants.BUNDLE_WEIGHT_KEY;
import static com.nextgis.metroaccess.util.Constants.DEPARTURE_RESULT;
import static com.nextgis.metroaccess.util.Constants.ICONS_RAW;
import static com.nextgis.metroaccess.util.Constants.LOCATING_TIMEOUT;
import static com.nextgis.metroaccess.util.Constants.META;
import static com.nextgis.metroaccess.util.Constants.PREF_RESULT;
import static com.nextgis.metroaccess.util.Constants.REMOTE_METAFILE;
import static com.nextgis.metroaccess.util.Constants.SERVER;
import static com.nextgis.metroaccess.util.Constants.STATUS_FINISH_LOCATING;
import static com.nextgis.metroaccess.util.Constants.STATUS_INTERRUPT_LOCATING;
import static com.nextgis.metroaccess.util.Constants.TAG;
import static com.nextgis.metroaccess.PreferencesActivity.clearRecent;

//https://code.google.com/p/k-shortest-paths/

public class MainActivity extends AppCompatActivity implements MetroApp.DownloadProgressListener {
    public static MAGraph mGraph;
    private AsyncHttpResponseHandler mMetaHandler;

    protected boolean m_bInterfaceLoaded;
    protected ButtonListAdapter m_laListButtons;
    protected ListView m_lvListButtons;
    protected Button m_oSearchButton;
    protected ImageView mBtnLimitations;
    private GpsMyLocationProvider mGpsMyLocationProvider;
    private SharedPreferences mPreferences;
    private Menu menu;

    protected int m_nDepartureStationId, m_nArrivalStationId;
    protected int m_nDeparturePortalId, m_nArrivalPortalId;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
        setContentView(R.layout.empty_activity_main);

        mGpsMyLocationProvider = new GpsMyLocationProvider(this);
        mGraph = MetroApp.getGraph();

        m_bInterfaceLoaded = false;

        // initialize the default settings
        PreferenceManager.setDefaultValues(this, R.xml.preferences, false);
		mPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        m_nDepartureStationId = mPreferences.getInt("dep_" + BUNDLE_STATIONID_KEY, -1);
        m_nArrivalStationId = mPreferences.getInt("arr_" + BUNDLE_STATIONID_KEY, -1);
        m_nDeparturePortalId = mPreferences.getInt("dep_" + BUNDLE_PORTALID_KEY, -1);
        m_nArrivalPortalId = mPreferences.getInt("arr_" + BUNDLE_PORTALID_KEY, -1);

		createHandler();
        getRemoteMeta();

        // check for data exist
        if (isRoutingDataExists()) {
            // then load main interface
            loadInterface();
		}

        // initialize google analytics
        boolean disableGA = mPreferences.getBoolean(PreferencesActivity.KEY_PREF_GA, true);
        ((MetroApp) getApplication()).reload(disableGA);
        GoogleAnalytics.getInstance(this).setDryRun(true);
	}

    @SuppressWarnings("deprecation")
    protected void createHandler(){
        mMetaHandler = new AsyncHttpResponseHandler() {
            private String mPayload = "";

            @Override
            public void onSuccess(int statusCode, Header[] headers, byte[] responseBody) {
                getPayload(responseBody);
            }

            @Override
            public void onFailure(int statusCode, Header[] headers, byte[] responseBody, Throwable error) {
                int info = statusCode == 0 ? R.string.sNetworkUnreachErr : R.string.sNetworkGetErr;
                showToast(info);
                getPayload(responseBody);
            }

            @Override
            public void onFinish() {
                super.onFinish();

                askToDownloadData(mPayload, isRoutingDataExists());
            }

            private void getPayload(byte[] body) {
                if (body != null)
                    mPayload = new String(body);
                else {
                    File file = new File(getExternalFilesDir(null), REMOTE_METAFILE);
                    mPayload = FileUtil.readFromFile(file);
                }
            }
        };
	}

	protected void loadInterface() {
        String currentCity = mPreferences.getString(PreferencesActivity.KEY_PREF_CITY, mGraph.GetCurrentCity());

        if (currentCity == null)
            return;

        if (currentCity.length() < 2) {
        	//find first city and load it
        	mGraph.SetFirstCityAsCurrent();
        } else {
        	mGraph.SetCurrentCity(currentCity);
        }

        if(!mGraph.IsValid()) {
            showToast(mGraph.GetLastError());
            return;
        }

		m_bInterfaceLoaded = true;
		setContentView(R.layout.activity_main);

        mBtnLimitations = (ImageView) findViewById(R.id.ivLimitations);
        mBtnLimitations.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                ((MetroApp) getApplication()).addEvent(Constants.SCREEN_MAIN, Constants.LIMITATIONS, Constants.SCREEN_MAIN);
                onSettings(true);
            }
        });

        setLimitationsColor(getLimitationsColor());

		m_lvListButtons = (ListView)findViewById(R.id.lvButtList);
		m_laListButtons = new ButtonListAdapter(this);
		// set adapter to list view
        m_lvListButtons.addFooterView(new View(this), null, true);
		m_lvListButtons.setAdapter(m_laListButtons);
		m_lvListButtons.setOnItemClickListener(new OnItemClickListener() {
			@Override
			public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
	        	switch(position){
	        	case 0: //from
                    ((MetroApp) getApplication()).addEvent(Constants.SCREEN_MAIN, Constants.FROM, Constants.PANE);
	        		onSelectDepatrure();
	        		break;
	        	case 1: //to
                    ((MetroApp) getApplication()).addEvent(Constants.SCREEN_MAIN, Constants.TO, Constants.PANE);
	        		onSelectArrival();
	        		break;
	        	}
	        }
	    });

        m_oSearchButton = (Button) findViewById(R.id.btSearch);
        m_oSearchButton.setEnabled(false);
        m_oSearchButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                ((MetroApp) getApplication()).addEvent(Constants.SCREEN_MAIN, "Search route", Constants.SCREEN_MAIN);
                onSearch();
            }
        });

    	updateInterface();
	}

    protected void updateInterface(){
        setLimitationsColor(getLimitationsColor());

        boolean isCorrectStationsAndPortals = m_nDepartureStationId != m_nArrivalStationId && m_nDepartureStationId != -1 &&
                m_nArrivalStationId != -1 && m_nDeparturePortalId != -1 && m_nArrivalPortalId != -1;

        if(m_oSearchButton != null)
            m_oSearchButton.setEnabled(isCorrectStationsAndPortals);

        if (mGraph.HasStations()) {
            if (m_laListButtons == null)
                return;

            StationItem station = mGraph.GetStation(m_nDepartureStationId);
            PortalItem portal;
            if (station != null) {
                m_laListButtons.setFromStation(station);
                m_laListButtons.setFromPortal(m_nDeparturePortalId);

                portal = station.GetPortal(m_nDeparturePortalId);
                if (portal == null)
                    m_nDeparturePortalId = -1;
            } else {
                m_laListButtons.setFromStation(null);
                m_laListButtons.setFromPortal(0);
                m_nDepartureStationId = -1;
            }

            station = mGraph.GetStation(m_nArrivalStationId);
            if(station != null && m_laListButtons != null){
                m_laListButtons.setToStation(station);
                m_laListButtons.setToPortal(m_nArrivalPortalId);

                portal = station.GetPortal(m_nArrivalPortalId);
                if (portal == null)
                    m_nArrivalPortalId = -1;
            } else {
                m_laListButtons.setToStation(null);
                m_laListButtons.setToPortal(0);
                m_nArrivalStationId = -1;
            }

            m_laListButtons.notifyDataSetChanged();
        }
    }

    private void resetInterface() {
        setContentView(R.layout.empty_activity_main);
        m_bInterfaceLoaded = false;
        m_laListButtons = null;
        m_lvListButtons = null;
        m_oSearchButton = null;
    }

    private int getLimitationsColor() {
        return LimitationsActivity.hasLimitations(this) ? getResources().getColor(android.R.color.white) : getResources().getColor(R.color.metro_material_dark);
    }

    private void setLimitationsColor(int color) {
        if (mBtnLimitations == null)
            return;

        if (LimitationsActivity.hasLimitations(this))
            mBtnLimitations.setBackgroundResource(R.drawable.btn_selector);
        else
            mBtnLimitations.setBackgroundResource(R.drawable.btn_limitations_off_selector);

        Bitmap bitmap = getBitmapFromSVG(this, R.raw.wheelchair_icon, color);
        mBtnLimitations.setImageBitmap(bitmap);
    }

    protected void onSettings(boolean isLimitations) {
        if (isLimitations)
            startActivityForResult(new Intent(this, LimitationsActivity.class), PREF_RESULT);
        else {
            startActivityForResult(new Intent(this, PreferencesActivity.class), PREF_RESULT);
        }

        //intentSet.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);        
        //Bundle bundle = new Bundle();
        //bundle.putParcelable(BUNDLE_METAMAP_KEY, mGraph);
        //intentSet.putExtras(bundle);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
        this.menu = menu;

        getMenuInflater().inflate(R.menu.menu_main, menu);
        tintIcons(menu, this);
		return true;
	}

	@Override
    public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
        case android.R.id.home:
            // app icon in action bar clicked; go home
            return false;
        case R.id.btn_settings:
            ((MetroApp) getApplication()).addEvent(Constants.SCREEN_MAIN, Constants.MENU_SETTINGS, Constants.MENU);
            onSettings(false);
            return true;
        case R.id.btn_limitations:
            ((MetroApp) getApplication()).addEvent(Constants.SCREEN_MAIN, Constants.LIMITATIONS, Constants.MENU);
            onSettings(true);
            return true;
        case R.id.btn_report:
            Intent intentReport = new Intent(this, ReportActivity.class);
            intentReport.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intentReport);
            return true;
        case R.id.btn_about:
            ((MetroApp) getApplication()).addEvent(Constants.SCREEN_MAIN, Constants.MENU_ABOUT, Constants.MENU);
            Intent intentAbout = new Intent(this, AboutActivity.class);
            intentAbout.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intentAbout);
            return true;
        case R.id.btn_locate:
            if (!item.isEnabled()) return true;

            if (!m_bInterfaceLoaded) {
                showToast(R.string.sLocationNoCitySelected);
                return true;
            }

            ((MetroApp) getApplication()).addEvent(Constants.SCREEN_MAIN, "Locate closest entrance", Constants.ACTION_BAR);

            final Context context = this;
            if (isProviderDisabled(context, false)) {
                showLocationInfoDialog(context, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        if (isProviderDisabled(context, true))
                            showToast(R.string.sLocationFail);
                        else
                            locateClosestEntrance();
                    }
                });
            } else
                locateClosestEntrance();
            break;
        case R.id.btn_reverse:
            if (!m_bInterfaceLoaded)
                showToast(R.string.sLocationNoCitySelected);

            swapStations();
            break;
        }
		return super.onOptionsItemSelected(item);
	}

    private void swapStations() {
        int t = m_nDeparturePortalId;
        m_nDeparturePortalId = m_nArrivalPortalId;
        m_nArrivalPortalId = t;
        t = m_nDepartureStationId;
        m_nDepartureStationId = m_nArrivalStationId;
        m_nArrivalStationId = t;

        updateInterface();
    }

    public static void showLocationInfoDialog(final Context context, DialogInterface.OnClickListener onNegativeButtonClicked) {
        LocationManager locationManager = (LocationManager) context.getSystemService(LOCATION_SERVICE);
        final boolean isGPSEnabled = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
        final boolean isNetworkEnabled = locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER);
        final boolean isLocationDisabled = isProviderDisabled(context, true);

        if (isProviderDisabled(context, false)) {   // at least one provider is turned off
            String network, gps, info;
            network = gps = "";

            if (!isNetworkEnabled)
                network = "\r\n- " + context.getString(R.string.sLocationNetwork);

            if(!isGPSEnabled)
                gps = "\r\n- " + context.getString(R.string.sLocationGPS);

            if (isLocationDisabled)
                info = context.getString(R.string.sLocationDisabledMsg);
            else
                info = context.getString(R.string.sLocationInaccuracy) + network + gps;

            AlertDialog.Builder builder = new AlertDialog.Builder(context);
            builder.setTitle(R.string.sLocationAccuracy).setMessage(info)
                    .setPositiveButton(R.string.sSettings,
                            new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int id) {
                                    context.startActivity(new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS));
                                }
                            })
                    .setNegativeButton(R.string.sCancel, onNegativeButtonClicked);
            builder.create();
            builder.show();
        }
    }

    public static boolean isProviderDisabled(Context context, boolean both) {
        LocationManager locationManager = (LocationManager) context.getSystemService(LOCATION_SERVICE);
        final boolean isGPSEnabled = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
        final boolean isNetworkEnabled = locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER);

        return both ? !isGPSEnabled && !isNetworkEnabled : !isGPSEnabled || !isNetworkEnabled;
    }

    private void locateClosestEntrance() {
        menu.findItem(R.id.btn_locate).setEnabled(false);
        showToast(R.string.sLocationStart);

        final Handler h = new Handler(){
            private boolean isLocationFound = false;

            public void handleMessage(Message msg) {
                switch (msg.what) {
                    case STATUS_INTERRUPT_LOCATING:
                        if(!isLocationFound)
                            Toast.makeText(getApplicationContext(), R.string.sLocationFail, Toast.LENGTH_LONG).show();
                    case STATUS_FINISH_LOCATING:
                        mGpsMyLocationProvider.stopLocationProvider();
                        isLocationFound = true;
                        menu.findItem(R.id.btn_locate).setEnabled(true);
                        break;
                }
            }
        };

        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                h.sendEmptyMessage(STATUS_INTERRUPT_LOCATING);
            }
        }, LOCATING_TIMEOUT);

        mGpsMyLocationProvider.startLocationProvider(new IMyLocationConsumer() {
            StationItem stationClosest = null;
            PortalItem portalClosest = null;

            @Override
            public void onLocationChanged(Location location, IMyLocationProvider iMyLocationProvider) {
                double currentLat = location.getLatitude();
                double currentLon = location.getLongitude();

                float shortest = Float.MAX_VALUE;
                float distance[] = new float[1];
                List<StationItem> stations = new ArrayList<>(mGraph.GetStations().values());

                for (int i = 0; i < stations.size(); i++) {  // find closest station first
                    Location.distanceBetween(currentLat, currentLon, stations.get(i).GetLatitude(), stations.get(i).GetLongitude(), distance);

                    if (distance[0] < shortest) {
                        shortest = distance[0];
                        stationClosest = stations.get(i);
                    }
                }

                if (stationClosest != null) {  // and then closest station's portal
                    shortest = Float.MAX_VALUE;
                    List<PortalItem> portals = stationClosest.GetPortals(true);

                    for (int i = 0; i < portals.size(); i++) {
                        Location.distanceBetween(currentLat, currentLon, portals.get(i).GetLatitude(), portals.get(i).GetLongitude(), distance);

                        if (distance[0] < shortest) {
                            shortest = distance[0];
                            portalClosest = portals.get(i);
                        }
                    }

                    Intent intent = new Intent();
                    intent.putExtra(BUNDLE_STATIONID_KEY, stationClosest.GetId());
                    intent.putExtra(BUNDLE_PORTALID_KEY, portalClosest.GetId());
                    onActivityResult(DEPARTURE_RESULT, RESULT_OK, intent);
                }

                h.sendEmptyMessage(STATUS_FINISH_LOCATING);

                if (stationClosest != null && portalClosest != null) {
                    String portalName = portalClosest.GetReadableMeetCode();
                    portalName = portalName.equals("") ? ": " + portalClosest.GetName() : " " + portalName + ": " + portalClosest.GetName();

                    Toast.makeText(getApplicationContext(), String.format(getString(R.string.sStationPortalName), stationClosest.GetName(),
                            getString(R.string.sEntranceName), portalName), Toast.LENGTH_LONG).show();
                }
            }
        });
    }

    protected void getRemoteMeta(){
        MetroApp.get(this, SERVER + META, mMetaHandler);
	}

	//check if data for routing is downloaded
	protected boolean isRoutingDataExists(){
		return mGraph.IsRoutingDataExist();
	}

	protected void askToDownloadData(String sJSON, boolean isUpdate){
		mGraph.OnUpdateMeta(sJSON, isUpdate);
		final List<GraphDataItem> items = mGraph.HasChanges();
        Collections.sort(items);

		int count = items.size();
		if (count == 0)
			return;

		final boolean[] checkedItems = new boolean[count];
	    for (int i = 0; i < count; i++)
	    	checkedItems[i] = isUpdate;

	    final CharSequence[] checkedItemStrings = new CharSequence[count];
	    for(int i = 0; i < count; i++){
	    	checkedItemStrings[i] = items.get(i).GetFullName();
	    }

		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setTitle(isUpdate ? R.string.sUpdateAvaliable : R.string.sSelectDataToDownload)
		.setMultiChoiceItems(checkedItemStrings, checkedItems,
                new DialogInterface.OnMultiChoiceClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which, boolean isChecked) {
                        checkedItems[which] = isChecked;
                    }
                })
		.setPositiveButton(R.string.sDownload,
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int id) {
                        List<GraphDataItem> itemsToDownload = new ArrayList<>();

                        for (int i = 0; i < checkedItems.length; i++) {
                            if (checkedItems[i]) {
                                itemsToDownload.add(items.get(i));
                            }
                        }

                        MetroApp.downloadData(MainActivity.this, itemsToDownload, MainActivity.this);
                    }
                })
		.setNegativeButton(R.string.sCancel, null);

		builder.create();
		builder.show();
    }

    @Override
    public void onDownloadFinished() {
        if (isRoutingDataExists() && !m_bInterfaceLoaded)
            loadInterface();
    }

	@Override
	protected void onPause() {
		final SharedPreferences.Editor edit = mPreferences.edit();

		//store departure and arrival
		edit.putInt("dep_" + BUNDLE_STATIONID_KEY, m_nDepartureStationId);
		edit.putInt("arr_" + BUNDLE_STATIONID_KEY, m_nArrivalStationId);
		edit.putInt("dep_" + BUNDLE_PORTALID_KEY, m_nDeparturePortalId);
		edit.putInt("arr_" + BUNDLE_PORTALID_KEY, m_nArrivalPortalId);
		edit.apply();

		super.onPause();
	}

//	@Override
//	protected void onResume() {
//		super.onResume();
//
//	    m_nDepartureStationId = mPreferences.getInt("dep_" + BUNDLE_STATIONID_KEY, -1);
//	    m_nArrivalStationId = mPreferences.getInt("arr_" + BUNDLE_STATIONID_KEY, -1);
//	    m_nDeparturePortalId = mPreferences.getInt("dep_" + BUNDLE_PORTALID_KEY, -1);
//	    m_nArrivalPortalId = mPreferences.getInt("arr_" + BUNDLE_PORTALID_KEY, -1);
//
//        if (!isRoutingDataExists())
//            resetInterface();
//        else if (m_bInterfaceLoaded)
//            updateInterface();
//        else
//            loadInterface();
//    }

	protected void 	onSelectDepatrure(){
	    Intent intent = new Intent(this, SelectStationActivity.class);
	    Bundle bundle = new Bundle();
	    bundle.putInt(BUNDLE_EVENTSRC_KEY, DEPARTURE_RESULT);
        //bundle.putSerializable(BUNDLE_STATIONMAP_KEY, (Serializable) mmoStations);
        bundle.putBoolean(BUNDLE_ENTRANCE_KEY, true);
        bundle.putInt(BUNDLE_STATIONID_KEY, m_nDepartureStationId);
        bundle.putInt(BUNDLE_PORTALID_KEY, m_nDeparturePortalId);
	    intent.putExtras(bundle);
	    startActivityForResult(intent, DEPARTURE_RESULT);
	}

	protected void 	onSelectArrival(){
	    Intent intent = new Intent(this, SelectStationActivity.class);
	    Bundle bundle = new Bundle();
	    bundle.putInt(BUNDLE_EVENTSRC_KEY, ARRIVAL_RESULT);
        //bundle.putSerializable(BUNDLE_STATIONMAP_KEY, (Serializable) mmoStations);
        bundle.putBoolean(BUNDLE_ENTRANCE_KEY, false);
        bundle.putInt(BUNDLE_STATIONID_KEY, m_nArrivalStationId);
        bundle.putInt(BUNDLE_PORTALID_KEY, m_nArrivalPortalId);
	    intent.putExtras(bundle);
	    startActivityForResult(intent, ARRIVAL_RESULT);
	}

	@Override
	  protected void onActivityResult(int requestCode, int resultCode, Intent data) {
    	int nStationId = -1;
    	int nPortalId = -1;
        boolean isCityChanged = false;

    	if(data != null) {
            nStationId = data.getIntExtra(BUNDLE_STATIONID_KEY, -1);
            nPortalId = data.getIntExtra(BUNDLE_PORTALID_KEY, -1);
            isCityChanged = data.getBooleanExtra(BUNDLE_CITY_CHANGED, false);
        } else {
            switch (requestCode) {
                case DEPARTURE_RESULT:
                    nStationId = m_nDepartureStationId;
                    nPortalId = m_nDeparturePortalId;
                    break;
                case ARRIVAL_RESULT:
                    nStationId = m_nArrivalStationId;
                    nPortalId = m_nArrivalPortalId;
                    break;
            }
        }

        final SharedPreferences.Editor edit = mPreferences.edit();

	    switch(requestCode) {
	    case DEPARTURE_RESULT:
	       	m_nDepartureStationId = nStationId;
	    	m_nDeparturePortalId = nPortalId;

            if (isCityChanged && nStationId != -1)
                m_nArrivalPortalId = m_nArrivalStationId = -1;

                    break;
	    case ARRIVAL_RESULT:
	    	m_nArrivalStationId = nStationId;
	    	m_nArrivalPortalId = nPortalId;

            if (isCityChanged && nStationId != -1)
                m_nDeparturePortalId = m_nDepartureStationId = -1;

            break;
	    case PREF_RESULT:
            isCityChanged = !isRoutingDataExists();
	    	break;
    	default:
    		break;
	    }

        if (isCityChanged) {
            if (nStationId == -1)
                m_nArrivalPortalId = m_nDeparturePortalId = m_nDepartureStationId = m_nArrivalStationId = -1;

            clearRecent(mPreferences);
        }

        edit.putInt("dep_" + BUNDLE_STATIONID_KEY, m_nDepartureStationId);
        edit.putInt("dep_" + BUNDLE_PORTALID_KEY, m_nDeparturePortalId);
        edit.putInt("arr_" + BUNDLE_STATIONID_KEY, m_nArrivalStationId);
        edit.putInt("arr_" + BUNDLE_PORTALID_KEY, m_nArrivalPortalId);
	    edit.apply();

        if (!isRoutingDataExists())
            resetInterface();
        else if (m_bInterfaceLoaded)
            updateInterface();
        else
            loadInterface();
	}

	protected void onSearch(){

		final ProgressDialog progressDialog = new ProgressDialog(this);
		progressDialog.setCancelable(false);
		progressDialog.setIndeterminate(true);
		progressDialog.setMessage(getString(R.string.sSearching));
		progressDialog.show();

		new Thread() {

			public void run() {

				//BellmanFordShortestPath
				/*List<DefaultWeightedEdge> path = BellmanFordShortestPath.findPathBetween(mGraph, stFrom.getId(), stTo.getId());
				if(path != null){
					for(DefaultWeightedEdge edge : path) {
		                	Log.d("Route", mmoStations.get(mGraph.getEdgeSource(edge)) + " - " + mmoStations.get(mGraph.getEdgeTarget(edge)) + " " + edge);
		                }
				}*/
				//DijkstraShortestPath
				/*List<DefaultWeightedEdge> path = DijkstraShortestPath.findPathBetween(mGraph, stFrom.getId(), stTo.getId());
				if(path != null){
					for(DefaultWeightedEdge edge : path) {
		                	Log.d("Route", mmoStations.get(mGraph.getEdgeSource(edge)) + " - " + mmoStations.get(mGraph.getEdgeTarget(edge)) + " " + edge);
		                }
				}*/
		        //KShortestPaths
				/*
				KShortestPaths<Integer, DefaultWeightedEdge> kPaths = new KShortestPaths<Integer, DefaultWeightedEdge>(mGraph, stFrom.getId(), 2);
		        List<GraphPath<Integer, DefaultWeightedEdge>> paths = null;
		        try {
		            paths = kPaths.getPaths(stTo.getId());
		            for (GraphPath<Integer, DefaultWeightedEdge> path : paths) {
		                for (DefaultWeightedEdge edge : path.getEdgeList()) {
		                	Log.d("Route", mmoStations.get(mGraph.getEdgeSource(edge)) + " - " + mmoStations.get(mGraph.getEdgeTarget(edge)) + " " + edge);
		                }
		                Log.d("Route", "Weight: " + path.getWeight());
		            }
		        } catch (IllegalArgumentException e) {
		        	e.printStackTrace();
		        }*/

				//YenTopKShortestPaths

			    int nMaxRouteCount = mPreferences.getInt(PreferencesActivity.KEY_PREF_MAX_ROUTE_COUNT, 3);
				List<Path> shortest_paths_list = mGraph.GetShortestPaths(m_nDepartureStationId, m_nArrivalStationId, nMaxRouteCount);

				if(shortest_paths_list.size() == 0){
					//MainActivity.this.showToast(R.string.sCannotGetPath);
					//Toast.makeText(MainActivity.this, R.string.sCannotGetPath, Toast.LENGTH_SHORT).show();
					Log.d(TAG, MainActivity.this.getString(R.string.sCannotGetPath));
				}
				else {
			        Intent intentView = new Intent(MainActivity.this, com.nextgis.metroaccess.StationListView.class);
			        //intentView.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);

			        int nCounter = 0;
			        Bundle bundle = new Bundle();
			        bundle.putInt("dep_" + BUNDLE_PORTALID_KEY, m_nDeparturePortalId);
			        bundle.putInt("arr_" + BUNDLE_PORTALID_KEY, m_nArrivalPortalId);

			        for (Path path : shortest_paths_list) {
						ArrayList<Integer> IndexPath = new  ArrayList<>();
                        double time = path.get_weight();
                        time += mGraph.GetStation(m_nDepartureStationId).GetPortal(m_nDeparturePortalId).GetTime();
                        time += mGraph.GetStation(m_nArrivalStationId).GetPortal(m_nArrivalPortalId).GetTime();
						Log.d(TAG, "Route# " + nCounter + " weight: " + time);
			            for (BaseVertex v : path.get_vertices()) {
			            	IndexPath.add(v.get_id());
			            	Log.d(TAG, "<" + mGraph.GetStation(v.get_id()));
			            }
			            intentView.putIntegerArrayListExtra(BUNDLE_PATH_KEY + nCounter, IndexPath);
			            intentView.putExtra(BUNDLE_WEIGHT_KEY + nCounter, time);
			            nCounter++;
			        }

			        bundle.putInt(BUNDLE_PATHCOUNT_KEY, nCounter);
			        //bundle.putSerializable(BUNDLE_STATIONMAP_KEY, (Serializable) mmoStations);
			        //bundle.putSerializable(BUNDLE_CROSSESMAP_KEY, (Serializable) mmoCrosses);

					intentView.putExtras(bundle);

			        MainActivity.this.startActivity(intentView);

				}

				progressDialog.dismiss();

			}

		}.start();

	}

	public void showToast(String message) {
		Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
	}

	public void showToast(int resource) {
		Toast.makeText(this, getString(resource), Toast.LENGTH_SHORT).show();
	}

    /**
     * Get bitmap from SVG file
     *
     * @param path  Path to SVG file
     * @return      Bitmap
     */
    public static Bitmap getBitmapFromSVG(String path) {
        File svgFile = new File(path);
        SVG svg = null;

        try {
            FileInputStream is = new FileInputStream(svgFile);
            svg = SVG.getFromInputStream(is);
        } catch (FileNotFoundException | SVGParseException e) {
            e.printStackTrace();
        }

        return getBitmapFromSVG(svg, Color.TRANSPARENT);
    }

    /**
     * Get bitmap from SVG resource file
     *
     * @param context   Current context
     * @param id        SVG resource id
     * @return          Bitmap
     */
    public static Bitmap getBitmapFromSVG(Context context, int id) {
        return getBitmapFromSVG(context, id, Color.TRANSPARENT);
    }

    /**
     * Get bitmap from SVG resource file with proper station icon and color overlay
     *
     * @param context   Current context
     * @param id        SVG resource id
     * @param color     String color to overlay
     * @return          Bitmap
     */
    public static Bitmap getBitmapFromSVG(Context context, int id, String color) {
        Bitmap bitmap = null;

        try {
            int c = Color.parseColor(color);
            bitmap = getBitmapFromSVG(context, id, c);
        } catch (Exception ignored) {
        }

        return bitmap;
    }

    /**
     * Get bitmap from SVG resource file with color overlay
     *
     * @param context   Current context
     * @param id        SVG resource id
     * @param color     Color to overlay
     * @return          Bitmap
     */
    public static Bitmap getBitmapFromSVG(Context context, int id, int color) {
        SVG svg = null;

        try {
            svg = SVG.getFromResource(context, id);
        } catch (SVGParseException e) {
            e.printStackTrace();
        }

        return getBitmapFromSVG(svg, color);
    }

    /**
     * Get bitmap from SVG with color overlay
     *
     * @param svg       SVG object
     * @param color     Color to overlay. Color.TRANSPARENT = no overlay
     * @return          Bitmap
     */
    public static Bitmap getBitmapFromSVG(SVG svg, int color) {
        Bitmap bitmap = null;

        if (svg != null && svg.getDocumentWidth() != -1) {
            PictureDrawable pd = new PictureDrawable(svg.renderToPicture());
            bitmap = Bitmap.createBitmap(pd.getIntrinsicWidth(), pd.getIntrinsicHeight(), Bitmap.Config.ARGB_8888);
            Canvas canvas = new Canvas(bitmap);
            canvas.drawPicture(pd.getPicture());

            if (color != Color.TRANSPARENT) {   // overlay color
                Paint p = new Paint();
                ColorFilter filter = new PorterDuffColorFilter(color, PorterDuff.Mode.SRC_ATOP);
                p.setColorFilter(filter);

                canvas = new Canvas(bitmap);
                canvas.drawBitmap(bitmap, 0, 0, p);
            }
        }

        return bitmap;
    }

    /**
     * Get bitmap from SVG resource file with proper route item icon and color overlay
     *
     * @param context   Current context
     * @param entry     RouteItem to get it's color and icon type
     * @param subItem   SubItem = _8.svg / x8.png
     * @return          Bitmap
     */
    public static Bitmap getBitmapFromSVG(Context context, RouteItem entry, boolean subItem) {
        String color = MetroApp.getGraph().GetLineColor(entry.GetLine());
        Bitmap bitmap = null;
        int type = subItem ? 8 : entry.GetType();

        switch (type) {
            case 6:
            case 7:
                bitmap = getBitmapFromSVG(MetroApp.getGraph().GetCurrentRouteDataPath() + "/icons/metro.svg");
                break;
            default:
                try {
                    int c = Color.parseColor(color);
                    bitmap = getBitmapFromSVG(context, ICONS_RAW[type], c);
                } catch (Exception ignored) {
                }
                break;
        }

        return bitmap;
    }

    public static void tintIcons(Menu menu, Context context) {
        MenuItem item;
        Drawable tintIcon;
        TypedValue typedValue = new TypedValue();
        context.getTheme().resolveAttribute(android.R.attr.textColorPrimary, typedValue, true);
//        int color = typedValue.data;
        int color = Color.WHITE;
        int colorDark = Color.BLACK;

        for(int i = 0; i < menu.size(); i++) {
            item = menu.getItem(i);
            tintIcon = item.getIcon();

            if (tintIcon != null) {
                Bitmap bitmap = Bitmap.createBitmap(tintIcon.getIntrinsicWidth(), tintIcon.getIntrinsicHeight(), Bitmap.Config.ARGB_8888);
                Canvas canvas = new Canvas(bitmap);

                tintIcon.setBounds(0, 0, bitmap.getWidth(), bitmap.getHeight());
                tintIcon.draw(canvas);

                Paint p = new Paint();
                ColorFilter filter = new PorterDuffColorFilter(colorDark, PorterDuff.Mode.SRC_ATOP);
                p.setColorFilter(filter);
                canvas.drawBitmap(bitmap, 0, 0, p);
                canvas.drawBitmap(bitmap, 0, 0, p);
//                canvas.drawBitmap(bitmap, 0, 0, p);
                filter = new PorterDuffColorFilter(color, PorterDuff.Mode.SRC_ATOP);
                p.setColorFilter(filter);
                canvas.drawBitmap(bitmap, 0, 0, p);

//                tintIcon.mutate().setColorFilter(colorDark, PorterDuff.Mode.SRC_ATOP);
                item.setIcon(new BitmapDrawable(context.getResources(), bitmap));
            }
        }
    }
}
