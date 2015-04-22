/******************************************************************************
 * Project:  Metro Access
 * Purpose:  Routing in subway for disabled.
 * Authors:  Baryshnikov Dmitriy aka Bishop (polimax@mail.ru), Stanislav Petriakov
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

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Rect;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Base64;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.google.android.gms.analytics.HitBuilders;
import com.google.android.gms.analytics.Tracker;
import com.nextgis.metroaccess.data.PortalItem;
import com.nextgis.metroaccess.data.StationItem;
import com.nhaarman.supertooltips.ToolTip;
import com.nhaarman.supertooltips.ToolTipRelativeLayout;
import com.nhaarman.supertooltips.ToolTipView;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.util.List;

import static com.nextgis.metroaccess.Constants.APP_REPORTS_DIR;
import static com.nextgis.metroaccess.Constants.APP_REPORTS_SCREENSHOT;
import static com.nextgis.metroaccess.Constants.BUNDLE_IMG_X;
import static com.nextgis.metroaccess.Constants.BUNDLE_IMG_Y;
import static com.nextgis.metroaccess.Constants.BUNDLE_PATH_KEY;
import static com.nextgis.metroaccess.Constants.BUNDLE_PORTALID_KEY;
import static com.nextgis.metroaccess.Constants.BUNDLE_STATIONID_KEY;
import static com.nextgis.metroaccess.Constants.KEY_PREF_TOOLTIPS;
import static com.nextgis.metroaccess.Constants.PARAM_ACTIVITY_FOR_RESULT;
import static com.nextgis.metroaccess.Constants.PARAM_DEFINE_AREA;
import static com.nextgis.metroaccess.Constants.PARAM_PORTAL_DIRECTION;
import static com.nextgis.metroaccess.Constants.PARAM_ROOT_ACTIVITY;
import static com.nextgis.metroaccess.Constants.PARAM_SCHEME_PATH;
import static com.nextgis.metroaccess.Constants.SUBSCREEN_PORTAL_RESULT;
import static com.nextgis.metroaccess.MainActivity.tintIcons;

public class StationImageView extends ActionBarActivity {
    private LayoutWebView mWebView;
    private Bundle bundle;

    private String msPath;
    private boolean isCrossReference = false;
    private boolean mIsRootActivity, isForLegend;
    RecyclerView rvPortals;

    private boolean mIsHintNotShowed = false;
    private boolean mIsPortalIn = false;
    private String mHintScreenName;
    private boolean mIsLimitations;

    private boolean mIsDefineArea = false;
    private boolean mIsAreaDefined = false;
    private int mImgWidth, mImgHeight;
    private int mImgX, mImgY;
    private int mSideGapX, mSideGapY;
    private float mRatio;
    private Rect mBounds;

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.station_image_view);
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        boolean isPortrait = getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT;

        RecyclerView.LayoutManager mLayoutManager = new LinearLayoutManager(this, isPortrait ? LinearLayoutManager.HORIZONTAL : LinearLayoutManager.VERTICAL, false);
        rvPortals = ((RecyclerView) findViewById(R.id.rvPortals));
        rvPortals.setLayoutManager(mLayoutManager);

        getSupportActionBar().setHomeButtonEnabled(true);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        bundle = getIntent().getExtras();   // TODO null bundle fix / MapActivity too
        if (bundle != null) {
            mIsDefineArea = bundle.getBoolean(PARAM_DEFINE_AREA, false);
            // PARAM_ROOT_ACTIVITY - determine is it called from StationMapActivity or StationExpandableListAdapter
            mIsRootActivity = bundle.getBoolean(PARAM_ROOT_ACTIVITY);
            isCrossReference = bundle.containsKey(PARAM_ROOT_ACTIVITY); // if PARAM_ROOT_ACTIVITY not contains, it called from another
            msPath = bundle.getString(PARAM_SCHEME_PATH);
            mIsPortalIn = bundle.getBoolean(PARAM_PORTAL_DIRECTION, true);

            StationItem station = MainActivity.GetGraph().GetStation(bundle.getInt(BUNDLE_STATIONID_KEY, 0)); // TODO global
            String title = station == null ? getString(R.string.sFileNotFound) :
                    String.format(getString(R.string.sSchema), getString(R.string.sLayout), station.GetName());
            setTitle(title);

            if (station != null && station.GetPortalsCount() > 0 && !mIsDefineArea &&
                    bundle.getBoolean(PARAM_ACTIVITY_FOR_RESULT, true)) {
                RVPortalAdapter adapter = new RVPortalAdapter(station.GetPortals(mIsPortalIn));
                rvPortals.setAdapter(adapter);
                rvPortals.setHasFixedSize(true);
                rvPortals.setItemAnimator(new DefaultItemAnimator());
                rvPortals.setVisibility(View.VISIBLE);

                mHintScreenName = getClass().getSimpleName();
                mIsHintNotShowed = !prefs.getString(KEY_PREF_TOOLTIPS, "").contains(mHintScreenName);

                if (mIsHintNotShowed)
                    showHint();
            }
        } else {
            isForLegend = true;
            setTitle(R.string.sLegend);

            Tracker t = ((Analytics) getApplication()).getTracker();
            t.setScreenName(Analytics.SCREEN_LAYOUT + " " + Analytics.LEGEND);
            t.send(new HitBuilders.AppViewBuilder().build());
        }

        mIsLimitations = LimitationsActivity.hasLimitations(this);

        ImageButton tvReport = (ImageButton) findViewById(R.id.ib_report);
        tvReport.setVisibility(mIsDefineArea || isForLegend ? View.GONE : View.VISIBLE);
//        tvReport.setPaintFlags(tvReport.getPaintFlags() | Paint.UNDERLINE_TEXT_FLAG);
        tvReport.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intentReport = new Intent(getApplicationContext(), ReportActivity.class);
                intentReport.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                intentReport.putExtra(BUNDLE_STATIONID_KEY, bundle.getInt(BUNDLE_STATIONID_KEY, -1));
                startActivity(intentReport);
            }
        });

        // load view
        mWebView = (LayoutWebView) findViewById(R.id.webView);
        // (*) this line make uses of the Zoom control
        mWebView.getSettings().setBuiltInZoomControls(!mIsDefineArea);
//        mWebView.getSettings().setJavaScriptEnabled(true);

        mWebView.getSettings().setLoadWithOverviewMode(true);
        mWebView.getSettings().setUseWideViewPort(true);

        mWebView.post(new Runnable() {
            @Override
            public void run() {
                final ProgressBar pbLoadingImage = (ProgressBar) findViewById(R.id.pdLoadingImage);
                final Animation fadeOut = AnimationUtils.loadAnimation(mWebView.getContext(), R.anim.fade_out);

                fadeOut.setAnimationListener(new Animation.AnimationListener() {
                    @Override
                    public void onAnimationStart(Animation animation) {
                    }

                    @Override
                    public void onAnimationEnd(Animation animation) {
                        pbLoadingImage.setVisibility(View.GONE);
                    }

                    @Override
                    public void onAnimationRepeat(Animation animation) {
                    }
                });

                if (!loadImage()) {
                    pbLoadingImage.startAnimation(fadeOut);
                    mWebView.setVisibility(View.GONE);
                    findViewById(R.id.tvLayoutError).setVisibility(View.VISIBLE);
                }

                mWebView.setWebViewClient(new WebViewClient() {
                    @Override
                    public void onPageFinished(WebView view, String url) {
                        super.onPageFinished(view, url);
                        pbLoadingImage.startAnimation(fadeOut);
                    }
                });
            }
        });

        mWebView.setOnLongClickListener(new View.OnLongClickListener() {
//        mWebView.setOnClickListener(new View.OnClickListener() {
            @Override
//            public void onClick(View view) {
            public boolean onLongClick(View view) {
//                saveScreenshot();

                return false;
            }
        });

        if (mIsDefineArea) {
            showReportingHint();

            mWebView.setOnTouchListener(new View.OnTouchListener() {
                @Override
                public boolean onTouch(View view, MotionEvent motionEvent) {
                    final int action = motionEvent.getAction();
                    float x = motionEvent.getX();
                    float y = motionEvent.getY();

                    switch (action & MotionEvent.ACTION_MASK) {
                        case MotionEvent.ACTION_MOVE:
                        case MotionEvent.ACTION_DOWN:
                            if (mBounds.contains((int) x, (int) y)) {
                                mWebView.defineArea(x, y);
                                mWebView.invalidate();
                                mImgX = (int) (x / mRatio - mSideGapX);
                                mImgY = (int) (y / mRatio - mSideGapY);
                            }
                            break;
                        case MotionEvent.ACTION_UP:
                            if (mBounds.contains((int) x, (int) y)) {
                                saveScreenshot();
                            }
                            break;
                    }

                    return false;
                }
            });
        }
    }

    private void saveScreenshot() {
        if (mIsAreaDefined)   // TODO back pressed
            return;

        Bitmap b = Bitmap.createBitmap(mWebView.getWidth(), mWebView.getHeight(), Bitmap.Config.ARGB_8888);
        Canvas c = new Canvas(b);
        mWebView.draw(c);

        FileOutputStream fos;
        File result = getExternalFilesDir(null);
        int resultCode = RESULT_CANCELED;
        final Intent outIntent = new Intent();

        try {
            if (mImgX > mImgWidth)
                mImgX = mImgWidth;

            if (mImgY > mImgHeight)
                mImgY = mImgHeight;

            if (mImgX < 0)
                mImgX = 0;

            if (mImgY < 0)
                mImgY = 0;

            if (result != null) {
                result = new File(result, APP_REPORTS_DIR);

                if (!result.mkdirs()) {
                    Toast.makeText(this, R.string.sIOError, Toast.LENGTH_SHORT).show();
                    return;
                }

                result = new File(result, APP_REPORTS_SCREENSHOT);
                fos = new FileOutputStream(result);

                b.compress(Bitmap.CompressFormat.JPEG, 25, fos);
                fos.close();

                outIntent.putExtra(BUNDLE_PATH_KEY, result.getAbsolutePath());
                outIntent.putExtra(BUNDLE_IMG_X, mImgX);
                outIntent.putExtra(BUNDLE_IMG_Y, mImgY);
                resultCode = RESULT_OK;
            } else
                Toast.makeText(this, R.string.sIOError, Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, R.string.sIOError, Toast.LENGTH_SHORT).show();
        }

        mIsAreaDefined = true;

        final int finalResultCode = resultCode;
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                setResult(finalResultCode, outIntent);
                finish();
            }
        }, 500);
    }

    @Override
    protected void onResume() {
        super.onResume();

        boolean was = mIsLimitations;
        mIsLimitations = LimitationsActivity.hasLimitations(this);

        if (was != mIsLimitations) {
            loadImage();    // TODO progress dialog > like onCreate
        }
    }

    private void showReportingHint() {
        final ToolTipRelativeLayout toolTipOverlay = (ToolTipRelativeLayout) findViewById(R.id.ttPortals);
        toolTipOverlay.setVisibility(View.VISIBLE);
        final ToolTip toolTip = new ToolTip()
                .withText(getString(R.string.sReportDefineAreaTooltip))
                .withColor(getResources().getColor(R.color.metro_color_main))
                .withAnimationType(ToolTip.AnimationType.FROM_MASTER_VIEW);

        if (Build.VERSION.SDK_INT <= 10)
            toolTip.withAnimationType(ToolTip.AnimationType.NONE);

        ToolTipView hint = toolTipOverlay.showToolTipForView(toolTip, mWebView);
        hint.setPadding(0, hint.getHeight(), 0, 0);
    }

    private void showHint() {
        String portalDirection = mIsPortalIn ? getString(R.string.sEntranceName) : getString(R.string.sExitName);
        final ToolTipRelativeLayout toolTipOverlay = (ToolTipRelativeLayout) findViewById(R.id.ttPortals);
        toolTipOverlay.setVisibility(View.VISIBLE);
        final ToolTip toolTip = new ToolTip()
                .withText(String.format(getString(R.string.sToolTipPortalFromLayout), portalDirection))
                .withColor(getResources().getColor(R.color.metro_color_main))
                .withAnimationType(ToolTip.AnimationType.FROM_MASTER_VIEW);

        if (Build.VERSION.SDK_INT <= 10)
            toolTip.withAnimationType(ToolTip.AnimationType.NONE);

        rvPortals.post(new Runnable() {
            @Override
            public void run() {
                ToolTipView hint = toolTipOverlay.showToolTipForView(toolTip, rvPortals.getChildAt(0).findViewById(R.id.btnPortal));
                hint.setPadding(0, (int) getResources().getDimension(R.dimen.strip_height), 0, 0);
                hint.setOnToolTipViewClickedListener(new ToolTipView.OnToolTipViewClickedListener() {
                    @Override
                    public void onToolTipViewClicked(ToolTipView toolTipView) {
                        hideHint(getApplicationContext(), mHintScreenName);
                    }
                });
            }
        });
    }

    public static void hideHint(Context context, String screen) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);

        String savedHintScreens = prefs.getString(KEY_PREF_TOOLTIPS, "");

        if(!savedHintScreens.contains(screen))
            prefs.edit().putString(KEY_PREF_TOOLTIPS, savedHintScreens + screen + ",").apply();
    }

    protected boolean loadImage() {
        Bitmap overlaidImage;

        // TODO OutOfMemoryError Bitmap on low memory devices
        if (isForLegend) {
            overlaidImage = BitmapFactory.decodeResource(getResources(), R.raw.schemes_legend);
            mWebView.clearCache(true);
        } else {
            File f = new File(msPath);
            String sParent = f.getParent();
            String sName = f.getName();
//            f = new File(f.getParent(), "/base/" + sName);

            if (!f.exists()) {
                return false;
            } else {
                Bitmap baseBitmap = BitmapFactory.decodeFile(f.getAbsolutePath());
                overlaidImage = baseBitmap.copy(Bitmap.Config.ARGB_8888, true);
                Canvas canvas = new Canvas(overlaidImage);

                baseBitmap = BitmapFactory.decodeFile(sParent + "/titles/" + sName);    // TODO localization
                overlayBitmap(canvas, baseBitmap);

                if (mIsLimitations) {
                    baseBitmap = BitmapFactory.decodeFile(sParent + "/numbers/" + sName);
//                    String[] s = sName.split(".png");
//                    baseBitmap = BitmapFactory.decodeFile(sParent + "/" + s[0] + "_num.png");
                    overlayBitmap(canvas, baseBitmap);
                }

                baseBitmap = BitmapFactory.decodeFile(sParent + "/info/" + sName);
                overlayBitmap(canvas, baseBitmap);
            }
        }

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        overlaidImage.compress(Bitmap.CompressFormat.PNG, 100, baos);
        byte[] byteArray = baos.toByteArray();
        String imageBase64 = Base64.encodeToString(byteArray, Base64.DEFAULT);
        String fix;
        String defineAreaFade = mIsDefineArea ? " bgcolor='#A6A6A6'" : "";

        double deviceRatio = 1.0 * mWebView.getHeight() / mWebView.getWidth();
        double imageRatio = 1.0 * overlaidImage.getHeight() / overlaidImage.getWidth();
        boolean isHorizontalScale = deviceRatio > imageRatio;

        if (Build.VERSION.SDK_INT > 10) {   // fix for old webkit versions
            fix = "max-width:100%;max-height:100%;'";
            fix += isHorizontalScale ? "width='100%' height='auto'" : "width='auto' height='100%'";
        } else
            fix = "'";

        // background-color: rgba(255, 255, 255, 0.01); alpha channel is a fix for showing image on some webkit versions
        String sCmd = "<html><body"+defineAreaFade+"><center><img style='background-color:rgba(255,255,255,0.01);position:absolute;margin:auto;top:0;left:0;right:0;bottom:0;" + fix +
                " src='data:image/png;base64," + imageBase64 + "'></center></body></html>";
//        String sCmd = "<html><center><img style='background-color:rgba(255,255,255,0.01);position:absolute;margin:auto;top:0;left:0;right:0;bottom:0;" +
//                "max-width:100%;max-height:100%;' " + fix + " src='data:image/png;base64," + imageBase64 + "'></center></html>";

        mWebView.loadData(sCmd, "text/html", "utf-8");

        mImgWidth = overlaidImage.getWidth();
        mImgHeight = overlaidImage.getHeight();

        int left = 0, top = 0;
        int right = mWebView.getWidth();
        int bottom = mWebView.getHeight();

        if (isHorizontalScale) {
            mRatio = 1f * right / mImgWidth;
            mSideGapY = (int) ((bottom - mImgHeight * mRatio) / 2);
            top = mSideGapY;
            bottom -= mSideGapY;
        } else {
            mRatio = 1f * bottom / mImgHeight;
            mSideGapX = (int) ((right - mImgWidth * mRatio) / 2);
            left = mSideGapX;
            right -= mSideGapX;
        }

        mBounds = new Rect(left, top, right, bottom);

        return true;
    }

    private void overlayBitmap(Canvas canvas, Bitmap bitmap) {
        if (bitmap != null)
            canvas.drawBitmap(bitmap, 0, 0, null);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater infl = getMenuInflater();
        infl.inflate(R.menu.menu_station_layout, menu);
        menu.findItem(R.id.btn_legend).setEnabled(!isForLegend).setVisible(!isForLegend);
        menu.findItem(R.id.btn_map).setEnabled(!isForLegend && isCrossReference).setVisible(!isForLegend && isCrossReference && !mIsDefineArea);
        tintIcons(menu, this);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                ((Analytics) getApplication()).addEvent(Analytics.SCREEN_LAYOUT, Analytics.BACK, Analytics.SCREEN_LAYOUT);
                finish();
                return true;
            case R.id.btn_map:
                ((Analytics) getApplication()).addEvent(Analytics.SCREEN_LAYOUT, Analytics.BTN_MAP, Analytics.ACTION_BAR);

                if (mIsRootActivity) {
                    Intent intentMap = new Intent(this, StationMapActivity.class);
                    intentMap.putExtras(bundle);
                    intentMap.putExtra(PARAM_ROOT_ACTIVITY, false);
                    startActivityForResult(intentMap, SUBSCREEN_PORTAL_RESULT);
                } else
                    finish();
                return true;
            case R.id.btn_legend:
                ((Analytics) getApplication()).addEvent(Analytics.SCREEN_LAYOUT, Analytics.LEGEND, Analytics.ACTION_BAR);
                onLegendClick();
                return true;
            case R.id.btn_limitations:
                ((Analytics) getApplication()).addEvent(Analytics.SCREEN_LAYOUT + " " + getDirection(), Analytics.LIMITATIONS, Analytics.MENU);
                startActivity(new Intent(this, LimitationsActivity.class));
//                startActivityForResult(new Intent(this, LimitationsActivity.class), PREF_RESULT);
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public void onBackPressed() {
        ((Analytics) getApplication()).addEvent(Analytics.SCREEN_LAYOUT, Analytics.BACK, Analytics.SCREEN_LAYOUT);

        super.onBackPressed();
    }

    public void onLegendClick() {
        Intent intentView = new Intent(this, StationImageView.class);
        startActivity(intentView);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case SUBSCREEN_PORTAL_RESULT:
                if (resultCode == RESULT_OK) {
                    setResult(RESULT_OK, data);
                    finish();
                }
                break;
            default:
                break;
        }
    }

    private String getDirection() {
        return mIsPortalIn ? Analytics.FROM : Analytics.TO;
    }

    class RVPortalAdapter extends RecyclerView.Adapter<ViewHolder> implements ViewHolder.IViewHolderClick {
        private List<PortalItem> mPortals;

        public RVPortalAdapter(List<PortalItem> portals) {
            mPortals = portals;
        }

        @Override
        public ViewHolder onCreateViewHolder(ViewGroup viewGroup, int i) {
            final View sView = LayoutInflater.from(viewGroup.getContext()).inflate(R.layout.single_portal_item, viewGroup, false);
            return new ViewHolder(sView);
        }

        @Override
        public void onBindViewHolder(ViewHolder viewHolder, int i) {
            viewHolder.setOnClickListener(this);

            PortalItem portal = mPortals.get(i);
            viewHolder.setName(portal.GetReadableMeetCode());

            if (portal.GetId() == bundle.getInt(BUNDLE_PORTALID_KEY, 0))
                viewHolder.setChecked();
            else
                viewHolder.setNormal();
        }

        @Override
        public int getItemCount() {
            return mPortals.size();
        }

        @Override
        public void onItemClick(View caller, int position) {
            if (mIsHintNotShowed)
                hideHint(caller.getContext(), mHintScreenName);

            ((Analytics) getApplication()).addEvent(Analytics.SCREEN_LAYOUT + " " + getDirection(), Analytics.PORTAL, Analytics.SCREEN_LAYOUT);

            Intent outIntent = new Intent();
            outIntent.putExtra(BUNDLE_STATIONID_KEY, bundle.getInt(BUNDLE_STATIONID_KEY, 0));
            outIntent.putExtra(BUNDLE_PORTALID_KEY, mPortals.get(position).GetId());
            setResult(RESULT_OK, outIntent);
            finish();
        }
    }
}
