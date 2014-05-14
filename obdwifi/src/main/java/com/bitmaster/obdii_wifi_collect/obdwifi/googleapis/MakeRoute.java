package com.bitmaster.obdii_wifi_collect.obdwifi.googleapis;

import android.location.Location;

import com.bitmaster.obdii_wifi_collect.obdwifi.MainActivity;
import com.bitmaster.obdii_wifi_collect.obdwifi.prediction.CalculateMaps;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Created by renet on 5/7/14.
 */
public class MakeRoute {

    private String origin;
    private String destination;

    public MakeRoute (MainActivity activity, String origin, String destination) {

        this.origin = origin;
        this.destination = destination;

        new RequestGoogleDirections(activity).execute(
                "http://maps.googleapis.com/maps/api/directions/xml?origin=" + this.origin
                        + "&destination=" + this.destination + "&sensor=true"
        );
    }

    public List<String> calculateRouteSteps() {

        List<String> result = new ArrayList<String>();
        Iterator<RouteStep> it = RequestGoogleDirections.routeSteps.iterator();
        while(it.hasNext()){
            //TODO: values can be null
            RouteStep s = it.next();
            /*result.add(s.getStartLat());
            result.add(s.getStartLng());
            result.add(s.getEndLat());
            result.add(s.getEndLng());
            result.add(s.getDuration());
            result.add(s.getInstructions());
            result.add(s.getDistance());*/
            result.add(s.getInstructions());
            //result.add(s.getStartLat() + ", " + s.getStartLng());


            long speed = Math.round(s.getDistance() / s.getDuration());
            result.add(Long.toString(speed) + " km/h");
            result.add(Float.toString(s.getDistance()) + " km");
            double expense= CalculateMaps.calculateExpenseAtSpeedAndTime((int) speed, (double)s.getDuration()); //%
            result.add(Double.toString(expense) + " %");
            //TODO: real speed
            float[] d = {0};
            Location.distanceBetween(s.getStartLat(), s.getStartLng(), s.getEndLat(), s.getEndLng(), d);
            result.add(Float.toString(d[0] / 1000));
        }

        return result;
    }
}
