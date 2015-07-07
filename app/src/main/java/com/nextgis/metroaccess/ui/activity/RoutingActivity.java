/*******************************************************************************
 * Project:  Metro Access
 * Purpose:  Routing in subway for disabled.
 * Author:   Baryshnikov Dmitriy aka Bishop (polimax@mail.ru)
 * Author:   Stanislav Petriakov, becomeglory@gmail.com
 ******************************************************************************
*   Copyright (C) 2013,2015 NextGIS
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
*******************************************************************************/

package com.nextgis.metroaccess.ui.activity;

import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.ExpandableListView;
import android.widget.TextView;

import com.nextgis.metroaccess.MetroApp;
import com.nextgis.metroaccess.R;
import com.nextgis.metroaccess.data.adapter.RouteExpandableListAdapter;
import com.nextgis.metroaccess.data.metro.BarrierItem;
import com.nextgis.metroaccess.data.metro.PortalItem;
import com.nextgis.metroaccess.data.metro.RouteItem;
import com.nextgis.metroaccess.data.metro.StationItem;
import com.nextgis.metroaccess.util.Constants;
import com.nextgis.metroaccess.util.TimeUtil;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Map;

import static com.nextgis.metroaccess.util.Constants.BUNDLE_PATHCOUNT_KEY;
import static com.nextgis.metroaccess.util.Constants.BUNDLE_PATH_KEY;
import static com.nextgis.metroaccess.util.Constants.BUNDLE_PORTALID_KEY;
import static com.nextgis.metroaccess.util.Constants.BUNDLE_WEIGHT_KEY;
import static com.nextgis.metroaccess.util.Constants.STATION_STOP_TIME;

public class RoutingActivity extends AppCompatActivity implements ActionBar.OnNavigationListener {
	protected int mnType;
	protected int mnMaxWidth, mnWheelWidth;
	protected ExpandableListView mExpListView;
    protected TextView mTvTime;
	protected int mnPathCount, mnDeparturePortalId, mnArrivalPortalId;
	protected boolean m_bHaveLimits;
	protected boolean firstLaunch = true;   // fix for GA, first item selected onCreate by default
    protected int mEntry, mExit;
    protected Calendar mNow = Calendar.getInstance();

	protected Map<Integer, StationItem> mmoStations;
	protected Map<String, int[]> mmoCrosses;
	public static final char DEGREE_CHAR = (char) 0x00B0;
	
	protected RouteExpandableListAdapter moAdapters[];

	public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //mbFilled = false;
        setContentView(R.layout.activity_routing);

	    final ActionBar actionBar = getSupportActionBar();
        assert actionBar != null;
        final Context context = actionBar.getThemedContext();
        
	    actionBar.setHomeButtonEnabled(true);
	    actionBar.setDisplayHomeAsUpEnabled(true);
        
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
		mnType = prefs.getInt(PreferencesActivity.KEY_PREF_USER_TYPE + "_int", 2);
        mnMaxWidth = LimitationsActivity.getMaxWidth(this);
        mnWheelWidth = LimitationsActivity.getWheelWidth(this);
		m_bHaveLimits = LimitationsActivity.hasLimitations(this);

	    Bundle extras = getIntent().getExtras(); 
	    if(extras != null) {
	    	mnDeparturePortalId = extras.getInt("dep_" + BUNDLE_PORTALID_KEY);
	    	mnArrivalPortalId = extras.getInt("arr_" + BUNDLE_PORTALID_KEY);
	    	
	    	mnPathCount = extras.getInt(BUNDLE_PATHCOUNT_KEY);
	    	mmoStations = MetroApp.getGraph().GetStations();//(Map<Integer, StationItem>) extras.getSerializable(MainActivity.BUNDLE_STATIONMAP_KEY);
	    	mmoCrosses = MetroApp.getGraph().GetCrosses();//(Map<String, int[]>) extras.getSerializable(MainActivity.BUNDLE_CROSSESMAP_KEY);

	    	String[] data = new String[mnPathCount];
	    	for(int i = 0; i < mnPathCount; i++){
	    		data[i] = getString(R.string.sRoute) + " " + (i + 1);
	    	}

		    ArrayAdapter<CharSequence> adapter= new ArrayAdapter<CharSequence>(context, android.R.layout.simple_spinner_item, data);
			adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);

		    actionBar.setDisplayShowTitleEnabled(false);
		    actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_LIST);
		    actionBar.setListNavigationCallbacks(adapter, this);

		    if(mnPathCount > 0){
		    	moAdapters = new RouteExpandableListAdapter[mnPathCount];
			    fillAdapter();
		    }
	    }

	    // load list
    	mExpListView = (ExpandableListView) findViewById(R.id.lvPathList);

    	// set adapter to list view
	    mExpListView.setAdapter(moAdapters[0]);

        mExpListView.setFastScrollEnabled(true);
 
        mExpListView.setGroupIndicator(null);
        
        //mExpListView.setOnGroupClickListener(this);

        mTvTime = (TextView) findViewById(R.id.tv_time);
        mTvTime.bringToFront();
        mTvTime.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ((MetroApp) getApplication()).addEvent(Constants.SCREEN_ROUTING, Constants.EXTENDED_TIME, Constants.SCREEN_ROUTING);
                AlertDialog builder = new AlertDialog.Builder(context)
                        .setMessage(String.format(getString(R.string.sRoutingInfo), getString(R.string.sEntranceName),
                                TimeUtil.formatTime(context, mEntry),
                                TimeUtil.formatTime(context, moAdapters[actionBar.getSelectedNavigationIndex()].getWeight()),
                                getString(R.string.sExitName),
                                TimeUtil.formatTime(context, mExit)))
                        .setPositiveButton(android.R.string.ok, null).create();
                builder.show();
            }
        });
    }

    private void fillAdapter() {
        Bundle extras = getIntent().getExtras();

        if (extras != null && moAdapters != null) {
            List<Integer> list;
            int weight;

            for (int i = 0; i < mnPathCount; i++) {
                list = extras.getIntegerArrayList(BUNDLE_PATH_KEY + i);
                moAdapters[i] = CreateAndFillAdapter(list);
                weight = (int) extras.getDouble(BUNDLE_WEIGHT_KEY + i);
                moAdapters[i].setWeight((int) Math.floor((weight + STATION_STOP_TIME * (list.size() - 1)) / 60.0));
                mEntry = (int) Math.floor(MetroApp.getGraph().GetStation(list.get(0)).GetPortal(mnDeparturePortalId).GetTime() / 60.0);
                mExit = (int) Math.floor(MetroApp.getGraph().GetStation(list.get(list.size() - 1)).GetPortal(mnArrivalPortalId).GetTime() / 60.0);
            }
        }
    }

    protected RouteExpandableListAdapter CreateAndFillAdapter(List<Integer> list) {
   		boolean bCross = false;
   		boolean bCrossCross;
   		if(list != null){
    			
			//add entrance
	   		List<RouteItem> routeList = new ArrayList<>();
            StationItem sit = mmoStations.get(list.get(0));
            PortalItem pit = null;
            String meetcode = "";

            if (sit != null)
                pit = sit.GetPortal(mnDeparturePortalId);

            if (pit != null)
                meetcode += " " + pit.GetReadableMeetCode();

            RouteItem oEntrance = new RouteItem(mnDeparturePortalId, getString(R.string.sEntranceName) + meetcode, list.get(0), -1, 6);
            routeList.add(FillBarriersForEntrance(oEntrance, list.get(0)));

		    for(int i = 0; i < list.size(); i++){
		    	bCrossCross = false;
	    		int nId = list.get(i);
	    		int nType = 5;
				if(bCross){
					if(i != list.size() - 1){
						int nNextId = list.get(i + 1);
						int nLineFrom = mmoStations.get(nId).GetLine();
						int nLineTo = mmoStations.get(nNextId).GetLine();
						if(nLineFrom != nLineTo){
							nType = 9;
							bCrossCross = true;
							bCross = true;
						}
						else{
							bCross = false;
							nType = 3;
						}
					}
					else{
						bCross = false;
						nType = 3;
					}
				}
				
				if(!bCrossCross && i != list.size() - 1){
					int nNextId = list.get(i + 1);
					int nLineFrom = mmoStations.get(nId).GetLine();
					int nLineTo = mmoStations.get(nNextId).GetLine();
					if(nLineFrom != nLineTo){
						bCross = true;
						nType = 4;
					}
				}
				
	    		StationItem entry = mmoStations.get(nId);

//                if (entry.GetPortal(mnArrivalPortalId) != null)   // bug, use entry.GetId() and separate fields mDep/mArr
//				    nType = 2;
//
//                if (entry.GetPortal(mnDeparturePortalId) != null)
//                    nType = 1;

                RouteItem oSta = new RouteItem(entry.GetId(), entry.GetName(), entry.GetLine(), entry.GetNode(), nType);

                if(i == list.size() - 1){
					//routeList.add(FillBarriersForExit(oSta, mnArrivalPortalId));
					routeList.add(FillBarriers(oSta, entry.GetId(), -1));
				}
				else{
					routeList.add(FillBarriers(oSta, entry.GetId(), list.get(i + 1)));
				}
	    	}
		    		
		    //add exit
//		    StationItem sit = mmoStations.get(list.get(list.size() - 1));
//			RouteItem oExit = new RouteItem(sit.GetId(), getString(R.string.sExitName), sit.GetLine(), -1, 7);
//			routeList.add(FillBarriersForExit(oExit, mnArrivalPortalId));

            sit = mmoStations.get(list.get((list.size() - 1)));
            pit = null;
            meetcode = "";

            if (sit != null)
                pit = sit.GetPortal(mnArrivalPortalId);

            if (pit != null)
                meetcode += " " + pit.GetReadableMeetCode();

            RouteItem oExit = new RouteItem(mnArrivalPortalId, getString(R.string.sExitName) + meetcode, list.get(list.size() - 1), -1, 7);
            routeList.add(FillBarriersForEntrance(oExit, list.get(list.size() - 1)));

            if (LimitationsActivity.hasLimitations(this)) {
                int[] naBarriers = {0,0,0,0,0,0,0,0,0};
                for(RouteItem rit : routeList){
                    List<BarrierItem> bits = rit.GetProblems();
                    if(bits != null){
                        for(BarrierItem bit : bits){
                            if(bit.GetId() == 0){
                                if(naBarriers[0] == 0 || naBarriers[0] > bit.GetValue()){
                                    naBarriers[0] = bit.GetValue();
                                }
                            }
                            else if(bit.GetId() == 1){
                                naBarriers[1] += bit.GetValue();
                            }
                            else if(bit.GetId() == 2){
                                naBarriers[2] += bit.GetValue();
                            }
                            else if(bit.GetId() == 3){
                                naBarriers[3] += bit.GetValue();
                            }
                            else if(bit.GetId() == 4){
                                naBarriers[4] += bit.GetValue();
                            }
                            else if(bit.GetId() == 5){
                                if(naBarriers[5] == 0){
                                    naBarriers[5] = bit.GetValue();
                                }
                                else if(naBarriers[5] > bit.GetValue()){
                                    naBarriers[5] = bit.GetValue();
                                }
                            }
                            else if(bit.GetId() == 6){
                                if(naBarriers[6] < bit.GetValue()){
                                    naBarriers[6] = bit.GetValue();
                                }
                            }
                            else if(bit.GetId() == 7){
                                if(naBarriers[7] < bit.GetValue()){
                                    naBarriers[7] = bit.GetValue();
                                }
                            }
                            else if(bit.GetId() == 8){
                                if(naBarriers[8] < bit.GetValue()){
                                    naBarriers[8] = bit.GetValue();
                                }
                            }
                        }
                    }
                }

                RouteItem oSumm = new RouteItem(-1, getString(R.string.sSummary), 0, 0, 0);
                FillWithData(naBarriers, oSumm, false);
                routeList.add(0, oSumm);
            }

	        // create new adapter
		    RouteExpandableListAdapter expListAdapter = new RouteExpandableListAdapter(this, routeList);
            expListAdapter.setDepartureArrivalStations(list.get(0), list.get(list.size() - 1));
            expListAdapter.setDepartureArrivalPortals(mnDeparturePortalId, mnArrivalPortalId);

		    return expListAdapter;
   		}
   		return null;
	}

	protected RouteItem FillBarriers(RouteItem it, int StationFromId, int StationToId){
		int[] naBarriers = mmoCrosses.get("" + StationFromId + "->" + StationToId);
		if(naBarriers != null && naBarriers.length == 9 && LimitationsActivity.hasLimitations(this)){
			FillWithData(naBarriers, it, false);
		}
		return it;
	}

	protected RouteItem FillBarriersForEntrance(RouteItem it, int StationId){
		StationItem sit = mmoStations.get(StationId);
		if(sit != null){
			PortalItem pit = sit.GetPortal(it.GetId());
			if(pit != null && LimitationsActivity.hasLimitations(this)){
				FillWithData(pit.GetDetails(), it, false);
			}
			it.SetLine(sit.GetLine());
		}		
		return it;
	}	

	protected void FillWithData(int[] naBarriers, RouteItem it, boolean bWithZeroes){
		if(bWithZeroes || naBarriers[0] > 0){//max_width
			boolean bProblem = m_bHaveLimits && naBarriers[0] < mnMaxWidth;
			String sName = getString(R.string.sMaxWCWidth) + ": " + naBarriers[0] / 10 + " " + getString(R.string.sCM);
			BarrierItem bit = new BarrierItem(0, sName, bProblem, naBarriers[0]);
			it.AddBarrier(bit);
		}
        if(bWithZeroes || naBarriers[8] > 0){//escalator
            String sName = getString(R.string.sEscalator) + ": " + naBarriers[8];
            BarrierItem bit = new BarrierItem(8, sName, false, naBarriers[8]);
            it.AddBarrier(bit);
        }
        else{
            String sName = getString(R.string.sEscalator) + ": " + getString(R.string.sNo);
            BarrierItem bit = new BarrierItem(8, sName, false, naBarriers[8]);
            it.AddBarrier(bit);
        }
        if(bWithZeroes || naBarriers[3] > 0){//lift
            String sName = getString(R.string.sLift) + ": " + naBarriers[3];
            BarrierItem bit = new BarrierItem(3, sName, false, naBarriers[3]);
            it.AddBarrier(bit);
        }
        else{
            String sName = getString(R.string.sLift) + ": " + getString(R.string.sNo);
            BarrierItem bit = new BarrierItem(3, sName, false, naBarriers[3]);
            it.AddBarrier(bit);
        }
        if(bWithZeroes || naBarriers[1] > 0){//min_step
			String sName = getString(R.string.sStairsCount) + ": " + naBarriers[1];
			BarrierItem bit = new BarrierItem(1, sName, false, naBarriers[1]);
			it.AddBarrier(bit);
		}
//		if(bWithZeroes || naBarriers[2] > 0){//min_step_ramp
		if(naBarriers[1] > 0){//min_step_ramp
			String sName = getString(R.string.sStairsWORails) + ": " + naBarriers[2];
			BarrierItem bit = new BarrierItem(2, sName, false, naBarriers[2]);
			it.AddBarrier(bit);
		}
		if(bWithZeroes || naBarriers[4] > 0){//lift_minus_step
			String sName = getString(R.string.sLiftEconomy) + ": " + naBarriers[4];
			BarrierItem bit = new BarrierItem(4, sName, false, naBarriers[4]);
			it.AddBarrier(bit);
		}
        if(bWithZeroes || naBarriers[5] > 0 || naBarriers[6] > 0){
           String sName = getString(R.string.sRailWidth) +  ": " + naBarriers[5] / 10 + " - "  + naBarriers[6] / 10 + " " + getString(R.string.sCM);
            boolean bCanRoll = !m_bHaveLimits || naBarriers[7] == 0
                    || naBarriers[5] <= mnWheelWidth
                    && (naBarriers[6] == 0 || mnWheelWidth <= naBarriers[6]);
            BarrierItem bit = new BarrierItem(56, sName, !bCanRoll, naBarriers[6] - naBarriers[5]);
            it.AddBarrier(bit);
        }
		/*if(bWithZeroes || naBarriers[5] > 0){//min_rail_width
			String sName = getString(R.string.sMinRailWidth) + ": " + naBarriers[5] / 10 + " " + getString(R.string.sCM);
			boolean bCanRoll = naBarriers[5] < mnWheelWidth && naBarriers[6] > mnWheelWidth;
			if(!bCanRoll && !m_bHaveLimits)
				bCanRoll = true;
			BarrierItem bit = new BarrierItem(5, sName, !bCanRoll, naBarriers[5]);
			it.AddBarrier(bit);
		}
		if(bWithZeroes || naBarriers[6] > 0){//max_rail_width
			String sName = getString(R.string.sMaxRailWidth) + ": " + naBarriers[6] / 10 + " " + getString(R.string.sCM);
			boolean bCanRoll = naBarriers[5] < mnWheelWidth && naBarriers[6] > mnWheelWidth;
			if(!bCanRoll && !m_bHaveLimits)
				bCanRoll = true;
			BarrierItem bit = new BarrierItem(6, sName, !bCanRoll, naBarriers[6]);
			it.AddBarrier(bit);
		}*/
		if(bWithZeroes || naBarriers[7] > 0){//max_angle
			String sName = getString(R.string.sMaxAngle) + ": " + naBarriers[7] + DEGREE_CHAR;
			BarrierItem bit = new BarrierItem(7, sName, false, naBarriers[7]);
			it.AddBarrier(bit);
		}
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater infl = getMenuInflater();
        infl.inflate(R.menu.menu_routing, menu);
        return true;
    }

    @Override
     public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                ((MetroApp) getApplication()).addEvent(Constants.SCREEN_ROUTING, Constants.BACK, Constants.SCREEN_ROUTING);

                finish();
                return true;
            case R.id.btn_limitations:
                ((MetroApp) getApplication()).addEvent(Constants.SCREEN_ROUTING, Constants.LIMITATIONS, Constants.MENU);
                startActivity(new Intent(this, LimitationsActivity.class));
//                startActivityForResult(new Intent(this, LimitationsActivity.class), PREF_RESULT);
                return true;
            case R.id.btn_report:
                Intent intentReport = new Intent(this, ReportActivity.class);
                intentReport.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(intentReport);
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();

        boolean was = m_bHaveLimits;
        m_bHaveLimits = LimitationsActivity.hasLimitations(this);
        boolean limitationsChanged = LimitationsActivity.getWheelWidth(this) != mnWheelWidth ||
                LimitationsActivity.getMaxWidth(this) != mnMaxWidth;

        if (was != m_bHaveLimits || limitationsChanged) {
            mnWheelWidth = LimitationsActivity.getWheelWidth(this);
            mnMaxWidth = LimitationsActivity.getMaxWidth(this);
            fillAdapter();
            firstLaunch = true;
            onNavigationItemSelected(0, 0);
        }
    }

    @Override
    public void onBackPressed() {
        ((MetroApp) getApplication()).addEvent(Constants.SCREEN_ROUTING, Constants.BACK, Constants.SCREEN_ROUTING);

        super.onBackPressed();
    }

	@Override
	public boolean onNavigationItemSelected(int itemPosition, long itemId) {
    	// set adapter to list view

        if (firstLaunch)
            firstLaunch = false;
        else
            ((MetroApp) getApplication()).addEvent(Constants.SCREEN_ROUTING, "Selected option " + itemPosition + 1, Constants.ACTION_ITEM);

	    mExpListView.setAdapter(moAdapters[itemPosition]);
        int minutes = moAdapters[itemPosition].getWeight() + mEntry + mExit;
        Calendar eta = (Calendar) mNow.clone();
        eta.add(Calendar.MINUTE, minutes);
        DateFormat time = SimpleDateFormat.getTimeInstance(DateFormat.SHORT);
        mTvTime.setText(TimeUtil.formatTime(this, minutes) + " (" + time.format(mNow.getTime()) + " - " + time.format(eta.getTime()) + ")");

		return true;
	}
}
