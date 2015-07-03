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
import android.os.Build;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.SectionIndexer;

import com.nextgis.metroaccess.R;
import com.nextgis.metroaccess.data.metro.StationItem;
import com.nextgis.metroaccess.data.adapter.StationIndexedExpandableListAdapter;
import com.nextgis.metroaccess.ui.activity.SelectStationActivity;
import com.nextgis.metroaccess.util.Constants;
import com.nhaarman.supertooltips.ToolTip;
import com.nhaarman.supertooltips.ToolTipRelativeLayout;
import com.nhaarman.supertooltips.ToolTipView;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import static com.nextgis.metroaccess.ui.activity.StationImageActivity.hideHint;

public class AlphabeticalStationListFragment extends SelectStationListFragment {
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        final View result = super.onCreateView(inflater, container, savedInstanceState);

        mTab = Constants.TAB_AZ;
        SelectStationActivity parentActivity = (SelectStationActivity) getActivity();
        m_oExpListAdapter = new AlphabeticalExpandableListAdapter(parentActivity, parentActivity.GetStationList());
        m_oExpListView.setAdapter(m_oExpListAdapter);

        if (parentActivity.isHintNotShowed())
            showHint(result);

        return result;
    }

    public void Update() {
        super.Update();

        if (m_oExpListAdapter != null) {
            SelectStationActivity parentActivity = (SelectStationActivity) getActivity();
            ((StationIndexedExpandableListAdapter) m_oExpListAdapter).Update(parentActivity.GetStationList());
            m_oExpListAdapter.notifyDataSetChanged();
        }
    }

    private void showHint(final View anchor) {
        m_oExpListView.post(new Runnable() {
            @Override
            public void run() {
                int counter = m_oExpListView.getFirstVisiblePosition();
                View button = null, child = null;

                while (child == null || counter < m_oExpListView.getLastVisiblePosition()) {
                    child = m_oExpListView.getChildAtPosition(counter++);
                    if (child == null)
                        continue;

                    button = child.findViewById(R.id.ibtnMenu);
                    if (button != null)
                        break;
                }

                if (button == null)
                    return;

                final ToolTipRelativeLayout toolTipOverlay = (ToolTipRelativeLayout) anchor.findViewById(R.id.ttSelectStation);
                toolTipOverlay.setVisibility(View.VISIBLE);
                final ToolTip toolTip = new ToolTip()
                        .withText(getString(R.string.sToolTipPopupMapLayout))
                        .withColor(getResources().getColor(R.color.metro_color_main))
                        .withAnimationType(ToolTip.AnimationType.FROM_MASTER_VIEW);

                if (Build.VERSION.SDK_INT <= 10)
                    toolTip.withAnimationType(ToolTip.AnimationType.NONE);

                final View view = button;
                button.post(new Runnable() {
                    @Override
                    public void run() {
                        ToolTipView hint = toolTipOverlay.showToolTipForView(toolTip, view);
                        hint.setPadding(0, 44, 0, 0);
                        hint.setOnToolTipViewClickedListener(new ToolTipView.OnToolTipViewClickedListener() {
                            @Override
                            public void onToolTipViewClicked(ToolTipView toolTipView) {
                                hideHint(getActivity(), ((SelectStationActivity) getActivity()).getHintScreenName());
                            }
                        });
                    }
                });
            }
        });
    }

    private class AlphabeticalExpandableListAdapter extends StationIndexedExpandableListAdapter implements SectionIndexer {

        public AlphabeticalExpandableListAdapter(Context c, List<StationItem> stationList) {
            super(c, stationList);

            onInit();
        }

        @Override
        protected void onInit() {
            Collections.sort(mStationList, new StationItemComparator());

            for (int i = 0; i < mStationList.size(); i++) {
                String stationName = mStationList.get(i).GetName();
                String firstChar = stationName.substring(0, 1).toUpperCase();

                if (!mSections.contains(firstChar)) {
                    mSections.add(firstChar);
                    mItems.add(new SectionItem(firstChar));
                    mIndexer.put(firstChar, i + mIndexer.size());
                }

                mItems.add(mStationList.get(i));
            }
        }

        protected class StationItemComparator implements Comparator<StationItem>
        {
            public int compare(StationItem left, StationItem right) {
                return left.GetName().compareTo( right.GetName() );
            }
        }
    }
}
