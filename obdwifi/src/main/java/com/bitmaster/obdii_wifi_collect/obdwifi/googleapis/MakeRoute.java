package com.bitmaster.obdii_wifi_collect.obdwifi.googleapis;

import java.util.List;

/**
 * Created by renet on 5/7/14.
 */
public class MakeRoute {

    private String origin;
    private String destination;

    public MakeRoute (String origin, String destination) {

        this.origin = origin;
        this.destination = destination;

        new RequestGoogleDirections().execute(
                "http://maps.googleapis.com/maps/api/directions/xml?origin=" + this.origin
                        + "&destination=" + this.destination + "&sensor=true"
        );
    }

    public List<RouteStep> getSteps() {
        return RequestGoogleDirections.routeSteps;
    }
}
