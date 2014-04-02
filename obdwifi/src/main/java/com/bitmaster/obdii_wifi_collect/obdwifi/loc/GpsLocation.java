package com.bitmaster.obdii_wifi_collect.obdwifi.loc;

import android.content.Context;
import android.location.GpsStatus;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;

import java.io.IOException;

/**
 * Created by renet on 3/21/14.
 */
public class GpsLocation {

    private LocationManager locationManager = null;
    private boolean locationUpdated = false;
    private final String PROVIDER = LocationManager.GPS_PROVIDER;

    public GpsLocation(Context context) {
        /********** get Gps location service LocationManager object ***********/
        this.locationManager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
    }

    public Location getLocation() {
        if(!this.locationManager.isProviderEnabled(PROVIDER)){
            return null;
        }
        return this.locationManager.getLastKnownLocation(PROVIDER);
    }
    public boolean getLocationUpdated() {
        if(!this.locationManager.isProviderEnabled(PROVIDER)){
            return false;
        }
        return this.locationUpdated;
    }

    public void requestLocation() {

        this.locationUpdated = false;

        if(!this.locationManager.isProviderEnabled(PROVIDER)){
            return;
        }

        this.locationManager.requestSingleUpdate(PROVIDER, new LocationListener(){

            @Override
            public void onLocationChanged(Location loc) {

                locationUpdated = true;
            }
            @Override
            public void onProviderDisabled(String provider) {}
            @Override
            public void onProviderEnabled(String provider) {}
            @Override
            public void onStatusChanged(String provider, int status, Bundle extras) {}
        }, null);
    }
}
