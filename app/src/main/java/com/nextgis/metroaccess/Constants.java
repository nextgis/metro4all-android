/******************************************************************************
 * Project:  Metro4All
 * Purpose:  Routing in subway.
 * Authors:  Dmitry Baryshnikov (polimax@mail.ru), Stanislav Petriakov
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


public interface Constants {
    int[] ICONS_RAW = { R.raw._0, R.raw._5, R.raw._5, R.raw._3, R.raw._4, R.raw._5, R.raw._6, R.raw._7, R.raw._8, R.raw._9 };

    String TAG = "metro4all";
    String SERVER = "http://metro4all.org/data/v2.7/";

    String META = "meta.json";
    String REMOTE_METAFILE = "remotemeta_v2.3.json";
    String ROUTE_DATA_DIR = "rdata_v2.3";
    String APP_VERSION = "app_version";
    String APP_REPORTS_DIR = "reports";
    String APP_REPORTS_PHOTOS_DIR = "Metro4All";
    String APP_REPORTS_SCREENSHOT = "screenshot.jpg";

    String CSV_CHAR = ";";

    String BUNDLE_MSG_KEY = "msg";
    String BUNDLE_PAYLOAD_KEY = "json";
    String BUNDLE_ERRORMARK_KEY = "error";
    String BUNDLE_EVENTSRC_KEY = "eventsrc";
    String BUNDLE_ENTRANCE_KEY = "in";
    String BUNDLE_PATHCOUNT_KEY = "pathcount";
    String BUNDLE_PATH_KEY = "path_";
    String BUNDLE_WEIGHT_KEY = "weight_";
    String BUNDLE_STATIONMAP_KEY = "stationmap";
    String BUNDLE_CROSSESMAP_KEY = "crossmap";
    String BUNDLE_STATIONID_KEY = "stationid";
    String BUNDLE_PORTALID_KEY = "portalid";
    String BUNDLE_METAMAP_KEY = "metamap";
    String BUNDLE_CITY_CHANGED = "city_changed";
    String BUNDLE_IMG_X = "coord_x";
    String BUNDLE_IMG_Y = "coord_y";
    String BUNDLE_ATTACHED_IMAGES = "attached_images";

    int DEPARTURE_RESULT = 1;
    int ARRIVAL_RESULT = 2;
    int PREF_RESULT = 3;
    int SUBSCREEN_PORTAL_RESULT = 4;
    int DEFINE_AREA_RESULT = 5;
    int CAMERA_REQUEST = 6;
    int PICK_REQUEST = 7;
    int MAX_RECENT_ITEMS = 10;

    String PARAM_PORTAL_DIRECTION = "PORTAL_DIRECTION";
    String PARAM_SCHEME_PATH = "image_path";
    String PARAM_ROOT_ACTIVITY = "root_activity";
    String PARAM_ACTIVITY_FOR_RESULT = "NEED_RESULT";
    String PARAM_DEFINE_AREA = "define_area";

    String KEY_PREF_RECENT_DEP_STATIONS = "recent_dep_stations";
    String KEY_PREF_RECENT_ARR_STATIONS = "recent_arr_stations";
    String KEY_PREF_TOOLTIPS = "tooltips_showed";

    int STATUS_INTERRUPT_LOCATING = 0;
    int STATUS_FINISH_LOCATING = 1;
    int LOCATING_TIMEOUT = 15000;
}
