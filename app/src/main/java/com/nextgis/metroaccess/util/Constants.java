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
package com.nextgis.metroaccess.util;


import com.nextgis.metroaccess.R;

public interface Constants {
    int[] ICONS_RAW = { R.raw._0, R.raw._5, R.raw._5, R.raw._3, R.raw._4, R.raw._5, R.raw._6, R.raw._7, R.raw._8, R.raw._9 };

    String TAG = "metro4all";
    String SERVER = "http://metro4all.org/data/v0.0/";

    String META = "meta.json";
    String REMOTE_METAFILE = "remotemeta_v2.3.json";
    String ROUTE_DATA_DIR = "rdata_v2.3";
    String APP_VERSION = "app_version";
    String APP_REPORTS_DIR = "reports";
    String APP_REPORTS_PHOTOS_DIR = "Metro4All";
    String APP_REPORTS_SCREENSHOT = "screenshot.jpg";

    String CSV_CHAR = ";";

    String BUNDLE_EVENTSRC_KEY = "eventsrc";
    String BUNDLE_ENTRANCE_KEY = "in";
    String BUNDLE_PATHCOUNT_KEY = "pathcount";
    String BUNDLE_PATH_KEY = "path_";
    String BUNDLE_WEIGHT_KEY = "weight_";
    String BUNDLE_STATIONID_KEY = "stationid";
    String BUNDLE_PORTALID_KEY = "portalid";
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
    int STATION_STOP_TIME = 30;

    // Google Analytics
    String PANE = "Pane";
    String MENU = "Menu";
    String PREFERENCE = "Preference";
    String ACTION_BAR = "ActionBar";
    String ACTION_ITEM = "List Item";

    String SCREEN_MAIN = "Main Screen";
    String SCREEN_PREFERENCE = "Preferences Screen";
    String SCREEN_MAP = "Map Screen";
    String SCREEN_LAYOUT = "Layout Screen";
    String SCREEN_SELECT_STATION = "Select Station Screen";
    String SCREEN_ROUTING = "Routing Screen";
    String SCREEN_LIMITATIONS = "Limitations Screen";

    String FROM = "From";
    String TO = "To";
    String TAB_AZ = "Tab A...Z";
    String TAB_LINES = "Tab Lines";
    String TAB_RECENT = "Tab Recent";

    String BTN_MAP = "Map";
    String BTN_LAYOUT = "Layout";
    String MENU_ABOUT = "About";
    String MENU_SETTINGS = "Settings";
    String BACK = "Back";
    String LIMITATIONS = "Limitations";
    String PORTAL = "Portal selected";
    String HEADER = "Header clicked";
    String STATION_EXPAND = "Station expanded";
    String STATION_COLLAPSE = "Station collapsed";
    String LEGEND = "Legend";
    String HELP_LINK = "Help link";
}
