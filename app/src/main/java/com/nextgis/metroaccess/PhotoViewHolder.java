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

package com.nextgis.metroaccess;

import android.graphics.Bitmap;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;

public class PhotoViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {
    private ImageButton mPhotoRemove;
    private ImageView mPhoto;
    private IViewHolderClick mViewHolderClick;

    public static interface IViewHolderClick {
        public void onItemClick(View caller, int position);
    }

    public PhotoViewHolder(View itemView) {
        super(itemView);
        mPhotoRemove = (ImageButton) itemView.findViewById(R.id.ib_remove);
        mPhotoRemove.setOnClickListener(this);
        mPhoto = (ImageView) itemView.findViewById(R.id.iv_photo);
        mPhoto.setOnClickListener(this);
    }

    public void setControl() {
        mPhotoRemove.setVisibility(View.GONE);

        View parentBox = mPhoto.getRootView();
        GridLayoutManager.LayoutParams lp = (GridLayoutManager.LayoutParams) parentBox.getLayoutParams();
        lp.setMargins(lp.width / 5, lp.width / 5, lp.width / 5, lp.width / 5);
        parentBox.setLayoutParams(lp);
    }

    public void setPhoto(Bitmap photo) {
        mPhoto.setImageBitmap(photo);
    }

    public void setOnClickListener(IViewHolderClick listener) {
        mViewHolderClick = listener;
    }

    @Override
    public void onClick(View view) {
        mViewHolderClick.onItemClick(view, getPosition());
    }
}
