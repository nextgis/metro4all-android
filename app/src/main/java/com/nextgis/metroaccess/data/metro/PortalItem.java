/******************************************************************************
 * Project:  Metro Access
 * Purpose:  Routing in subway for disabled.
 * Author:   Baryshnikov Dmitriy (aka Bishop), polimax@mail.ru
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

package com.nextgis.metroaccess.data.metro;

import android.os.Parcel;
import android.os.Parcelable;

public class PortalItem implements Parcelable {
	private String sName;
	private int nDirection;// 1 - in, 2 - out, 3 - both
	private int nId, nMeetCode = -1;
	private int nStationId, nTime;
	private int[] anDetails;
    private double nLatitude;
    private double nLongitude;

	public PortalItem(int nId, String sName, int nStationId, int nDirection,
                      int[] anDetails, double nLat, double nLong, int nMeetCode, int nTime) {
		this.sName = sName;
		this.nId = nId;
		this.nDirection = nDirection;
		this.nStationId = nStationId;
		this.nStationId = nStationId;
		this.anDetails = anDetails;
        this.nLatitude = nLat;
        this.nLongitude = nLong;
        this.nMeetCode = nMeetCode;
        this.nTime = nTime;
	}
	
	public String GetName(){
		if(sName.length() == 0){
			return "#" + nId;
		}
		return sName;
	}
	
	public int GetId(){
		return nId;
	}
	
	public int GetDirection(){
		return nDirection;
	}
	
	public int GetStationId(){
		return nStationId;
	}
	
	public int[] GetDetails(){
		return anDetails;
	}

    public double GetLatitude(){
        return nLatitude;
    }

    public double GetLongitude(){
        return nLongitude;
    }

    public int GetMeetCode() {
        return nMeetCode;
    }

    public int GetTime() {
        return nTime;
    }

    public String GetReadableMeetCode() {
        return nMeetCode == -1 ? "" : "#" + nMeetCode;
    }

    @Override
	public int describeContents() {
		return 0;
	}

	@Override
	public void writeToParcel(Parcel out, int flags) {
		out.writeString(sName);
		out.writeInt(nId);
		out.writeInt(nStationId);
		out.writeInt(nDirection);
		out.writeInt(anDetails.length);

		for (int anDetail : anDetails)
			out.writeInt(anDetail);

        out.writeDouble(nLatitude);
        out.writeDouble(nLongitude);
    }

	public static final Parcelable.Creator<PortalItem> CREATOR
    = new Parcelable.Creator<PortalItem>() {
	    public PortalItem createFromParcel(Parcel in) {
	        return new PortalItem(in);
	    }
	
	    public PortalItem[] newArray(int size) {
	        return new PortalItem[size];
	    }
	};
	
	private PortalItem(Parcel in) {
		sName = in.readString();
		nId = in.readInt();
		nStationId = in.readInt();
		nDirection = in.readInt();
		int size = in.readInt();
		anDetails = new int[size];
		for(int i = 0; i < size; i++){
			anDetails[i] = in.readInt();
		}
        nLatitude = in.readDouble();
        nLongitude = in.readDouble();
    }

	@Override
	public String toString() {
		return GetName();
	}
	
}
