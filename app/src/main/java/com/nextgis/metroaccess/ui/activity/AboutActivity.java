/******************************************************************************
 * Project:  Metro4All
 * Purpose:  Routing in subway.
 * Author:   Dmitry Baryshnikov (polimax@mail.ru)
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
 ****************************************************************************/
package com.nextgis.metroaccess.ui.activity;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.net.Uri;
import android.os.Bundle;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.text.method.LinkMovementMethod;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import com.nextgis.metroaccess.R;

public class AboutActivity extends AppCompatActivity {
    private String versionName = "unknown";
    private String versionCode = "unknown";

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_about);

        TextView txtVersion = (TextView) findViewById(R.id.txtVersion);
        ImageView imgLogo = (ImageView) findViewById(R.id.imgLogo);

        Button btnCredits = (Button) findViewById(R.id.btnAcknowledgements);
        btnCredits.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                AlertDialog builder = new AlertDialog.Builder(view.getContext(), R.style.AppCompatDialog).setTitle(R.string.acknowledgements_caption)
                        .setMessage(R.string.acknowledgements_text)
                        .setPositiveButton(android.R.string.ok, null).create();
                builder.show();
                ((TextView) builder.findViewById(android.R.id.message)).setMovementMethod(LinkMovementMethod.getInstance());
                ((TextView) builder.findViewById(android.R.id.message)).setLinksClickable(true);
            }
        });

        imgLogo.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                onLogoClicked();
            }
        });

        String pkgName = this.getPackageName();
        try {
            PackageManager pm = this.getPackageManager();
            versionName = pm.getPackageInfo(pkgName, 0).versionName;
            versionCode = Integer.toString(pm.getPackageInfo(this.getPackageName(), 0).versionCode);
        } catch (NameNotFoundException ignored) {
        }

        txtVersion.setText("v. " + versionName + " (rev. " + versionCode + ")");

        assert getSupportActionBar() != null;
        getSupportActionBar().setHomeButtonEnabled(true);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
    }

    private void onLogoClicked() {
        Intent browserIntent = new Intent("android.intent.action.VIEW",
                Uri.parse("http://nextgis.ru"));
        startActivity(browserIntent);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                // app icon in action bar clicked; go home
                Intent intent = new Intent(this, MainActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                startActivity(intent);
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }
}
