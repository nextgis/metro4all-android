/******************************************************************************
 * Project:  Metro Access
 * Purpose:  Routing in subway for disabled.
 * Author:   Baryshnikov Dmitriy aka Bishop (polimax@mail.ru)
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
package com.nextgis.metroaccess.ui.fragment;

import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.SectionIndexer;

import com.nextgis.metroaccess.MetroApp;
import com.nextgis.metroaccess.data.metro.StationItem;
import com.nextgis.metroaccess.data.adapter.StationIndexedExpandableListAdapter;
import com.nextgis.metroaccess.ui.activity.SelectStationActivity;
import com.nextgis.metroaccess.util.Constants;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

public class LinesStationListFragment extends SelectStationListFragment {
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View result = super.onCreateView(inflater, container, savedInstanceState);

        mTab = Constants.TAB_LINES;
        SelectStationActivity parentActivity = (SelectStationActivity) getActivity();
        m_oExpListAdapter = new LinesExpandableListAdapter(parentActivity, parentActivity.getStationList(), MetroApp.getGraph().GetLines());
        m_oExpListView.setAdapter(m_oExpListAdapter);

        return result;
    }

    public void update() {
        super.update();

        if (m_oExpListAdapter != null) {
            SelectStationActivity parentActivity = (SelectStationActivity) getActivity();
            ((LinesExpandableListAdapter) m_oExpListAdapter).Update(parentActivity.getStationList());
            m_oExpListAdapter.notifyDataSetChanged();
        }
    }

    private class LinesExpandableListAdapter extends StationIndexedExpandableListAdapter implements SectionIndexer {
        protected Map<Integer, String> momLines;

        public LinesExpandableListAdapter(Context c, List<StationItem> stationList, Map<Integer, String> omLines) {
            super(c, stationList);

            momLines = omLines;
            onInit();
        }

        @Override
        protected void onInit() {
            Collections.sort(mStationList, new StationItemComparator());

            StationItem station;

            for (int i = 0; i < mStationList.size(); i++) {
                station = mStationList.get(i);
                int stationLine = station.GetLine();
                String lineName = stationLine + ". " + momLines.get(stationLine);

                if (!mSections.contains(stationLine + "")) {
                    mSections.add(stationLine + "");
                    mItems.add(new SectionItem(lineName));
                    mIndexer.put(stationLine + "", i + mIndexer.size());
                }

                mItems.add(station);
            }
        }

        protected class StationItemComparator implements Comparator<StationItem>
        {
            public int compare(StationItem left, StationItem right) {
                if(left.GetLine() == right.GetLine())
                    return left.GetOrder()  - right.GetOrder();//left.GetName().compareTo( right.GetName() );
                else {
                    return left.GetLine() - right.GetLine();
                }
            }
        }
    }
}
