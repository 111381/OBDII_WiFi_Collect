package com.bitmaster.obdii_wifi_collect.obdwifi.googleapis;

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
    private  List<RouteStep> routeSteps;

    public MakeRoute (MainActivity activity, String origin, String destination) {

        this.origin = origin;
        this.destination = destination;

        RequestGoogleDirections request = (RequestGoogleDirections) new RequestGoogleDirections(activity).execute(
                "http://maps.googleapis.com/maps/api/directions/xml?origin=" + this.origin
                        + "&destination=" + this.destination + "&sensor=true"
        );
        this.routeSteps = request.getRouteSteps();
    }

    public List<String> calculateRouteStepsAsStringList() {

        List<String> result = new ArrayList<String>();
        Iterator<RouteStep> it = this.routeSteps.iterator();
        while(it.hasNext()){
            //TODO: values can be null
            RouteStep s = it.next();
            result.add(s.getInstructions());
            //result.add(s.getStartLat() + ", " + s.getStartLng());
            int speed = Math.round(s.getDistance() / s.getDuration());
            result.add(Integer.toString(speed) + " km/h");
            result.add(Float.toString(s.getDistance()) + " km");
            double expense= CalculateMaps.calculateExpenseAtSpeedAndTime(speed, (double)s.getDuration()); //%
            result.add(Double.toString(expense) + " %");
        }

        return result;
    }

    public List<RouteStep> getRouteSteps() {
        return this.routeSteps;
    }
}
