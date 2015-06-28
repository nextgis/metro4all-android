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

package com.nextgis.metroaccess.util;

import android.content.Context;

import com.nextgis.metroaccess.R;

public class TimeUtil {

    /**
     * Format time in minutes to "HH h MM min" or "HH h" or "MM min".
     *
     * @param context The context.
     * @param time Time in minutes.
     * @return Formatted string.
     */
    public static String formatTime(Context context, int time) {
        String min = context.getString(R.string.sTimeUnitMinute), hour = context.getString(R.string.sTimeUnitHour);
        String result = time + " " + min;

        if (time >= 60) {
            result = String.format("%d %s", time / 60, hour);

            if (time % 60 != 0)
                result = String.format("%s %d %s", result, time % 60, min);
        }

        return result;
    }
}
