package com.bitmaster.obdii_wifi_collect.obdwifi.googleapis;

/**
 * Created by renet on 5/7/14.
 */
public class RouteStep {

    private double startLat;
    private double startLng;
    private double endLat;
    private double endLng;
    private float duration;
    private float distance;
    private String instructions;



    public void setStartLat(String startLat) {
        this.startLat = Double.parseDouble(startLat);
    }

    public void setStartLng(String startLng) {
        this.startLng = Double.parseDouble(startLng);
    }

    public void setEndLat(String endLat) {
        this.endLat = Double.parseDouble(endLat);
    }

    public void setEndLng(String endLng) {
        this.endLng = Double.parseDouble(endLng);
    }

    public void setDuration(String duration) {
        this.duration = (Float.parseFloat(duration) / 3600);
    }

    public void setDistance(String distance) {
        this.distance = (Float.parseFloat(distance) / 1000);
    }

    public void setInstructions(String instructions) {
        this.instructions = instructions;
    }

    public double getStartLat() {
        return startLat;
    }

    public double getStartLng() {
        return startLng;
    }

    public double getEndLat() {
        return endLat;
    }

    public double getEndLng() {
        return endLng;
    }

    public float getDuration() {
        return duration;
    }

    public float getDistance() {
        return distance;
    }

    public String getInstructions() {
        return instructions;
    }
}
