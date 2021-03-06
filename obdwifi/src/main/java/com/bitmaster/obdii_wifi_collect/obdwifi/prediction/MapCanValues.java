package com.bitmaster.obdii_wifi_collect.obdwifi.prediction;

import android.location.Location;
import android.util.Log;

import com.bitmaster.obdii_wifi_collect.obdwifi.googleapis.RouteStep;
import com.bitmaster.obdii_wifi_collect.obdwifi.loc.GpsLocation;
import com.bitmaster.obdii_wifi_collect.obdwifi.weather.RequestTemperature;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;

/**
 * Created by renet on 5/2/14.
 * Class with static constants and methods.
 * Holds probe results.
 */
public class MapCanValues {

    public static final String POWER = "373";
    public static final String SPEED = "412";
    public static final String SHIFT = "418";
    public static final String RANGE = "346";
    public static final String SOC = "374";

    public static final String PARK = "P";
    public static final String REV = "R";
    public static final String NEUT = "N";
    public static final String DRIVE = "D";
    public static final String BR = "B";
    public static final String ECO = "C";

    private static final int SPEED_MAP_SIZE = 128;
    private static final int ACC_MAP_SIZE = 256;
    private static final int ACC_OFFSET = 128;
    private static final int TEMP_AREA = 80;
    private static final int TEMP_OFFSET = 39;

    public static final double CAPACITY = 16000.0;

    public static List<RouteStep> routeStepList = null;
    private static RouteStep step = null;
    private static List<Integer> stepSpeedList = new ArrayList<Integer>();
    private static List<Integer> stepPowerList = new ArrayList<Integer>();

    private static long lastSpeed;
    private static long lastTime;
    private static double lastAmp;
    private static double lastVolt;

    public static int speed;
    public static int odo;
    public static int range;
    public static double acceleration;
    public static double soc;
    public static double amp;
    public static double volt;
    public static String shift;

    public static String currentPID;
    public static String row = "";
    public static int temperature = 0;

    public static int [][] driveConsumption = new int[SPEED_MAP_SIZE][ACC_MAP_SIZE]; //32768 * 4 bytes
    public static int [][] driveFrequency = new int[SPEED_MAP_SIZE][ACC_MAP_SIZE]; //32768 * 4 bytes

    //public static int [][] speedDiff = new int[SPEED_MAP_SIZE][24];//24h, speed zero at middle

    public static int [][] staticConsumption = new int[TEMP_AREA][2];
    /*
     * returns false if no match in message or exception
     */
    public static boolean decodeCanMessage(String message) {
        try {
            if( message.length() == 19 && message.substring(0, 3).equalsIgnoreCase(SPEED)) {

                currentPID = SPEED;

                long currentSpeed = Long.parseLong(message.substring(5, 7), 16);
                acceleration = ((double)(currentSpeed - lastSpeed) / 3.6) / ((double)(System.currentTimeMillis() - lastTime) / 1000) ; // m/s2
                speed = (int) Math.round(((double)currentSpeed + (double)lastSpeed) / 2);

                lastSpeed = currentSpeed;
                lastTime = System.currentTimeMillis();

                odo = (Integer.parseInt(message.substring(7, 9), 16) * 65536) +
                        (Integer.parseInt(message.substring(9, 11), 16) * 256) +
                        Integer.parseInt(message.substring(11, 13), 16);
                row = row + currentPID + "\r" + Integer.toString(speed) + "\r" + Integer.toString(odo) + "\r";
                return true;
            }
            if(message.length() == 19 && message.substring(0, 3).equalsIgnoreCase(SOC)) {

                currentPID = SOC;

                soc = ((double)Long.parseLong(message.substring(5, 7), 16) - 10) / 2;
                row = row + currentPID + "\r" + Double.toString(soc) + "\r";
                return true;
            }
            if(message.length() == 19 && message.substring(0, 3).equalsIgnoreCase(POWER)) {

                currentPID = POWER;

                double currentAmp = ((double)(Long.parseLong(message.substring(7, 9), 16) * 256) +
                        (double)(Long.parseLong(message.substring(9, 11), 16) - (128 * 256))) / 100;
                amp = (currentAmp + lastAmp) / 2;

                lastAmp = currentAmp;

                double currentVolt = ((double)(Long.parseLong(message.substring(11, 13), 16) * 256) +
                        (double)Long.parseLong(message.substring(13, 15), 16)) / 10;
                volt = (currentVolt + lastVolt) /2;

                lastVolt = currentVolt;
                row = row + currentPID + "\r" + Double.toString(amp) + "\r" + Double.toString(volt) +"\r";
                return true;
            }
            if(message.length() == 19 && message.substring(0, 3).equalsIgnoreCase(RANGE)) {

                currentPID = RANGE;

                range = Integer.parseInt(message.substring(17, 19), 16);
                row = row + currentPID + "\r" + Integer.toString(range) + "\r";
                return true;
            }
            if(message.length() == 17 && message.substring(0, 3).equalsIgnoreCase(SHIFT)) {

                currentPID = SHIFT;

                if(message.substring(3, 5).equalsIgnoreCase("50")) {
                    shift = PARK;
                }
                if(message.substring(3, 5).equalsIgnoreCase("52")) {
                    shift = REV;
                }
                if(message.substring(3, 5).equalsIgnoreCase("4E")) {
                    shift = NEUT;
                }
                if(message.substring(3, 5).equalsIgnoreCase("44")) {
                    shift = DRIVE;
                }
                if(message.substring(3, 5).equalsIgnoreCase("83")) {
                    shift = BR;
                }
                if(message.substring(3, 5).equalsIgnoreCase("32")) {
                    shift = ECO;
                }
                row = row + currentPID + "\r" + shift + "\r";
                return true;
            }

        } catch (NumberFormatException e) {
            Log.i("NumberEx", e.getLocalizedMessage());
            return false;
        }

        return false;
    }

    public static void setMapValues() {

        if(currentPID.equals(SHIFT) && shift.equals(DRIVE)) { //only for shift DRIVE position
            int power = (int)(amp * volt); //amp * volt
            int acc = ((int)(acceleration * 25.0)) + ACC_OFFSET;//0,04 m/s2 unit
            // check if fits to array
            if((0 <= speed) && (speed < SPEED_MAP_SIZE) && (0 <= acc) && (acc < ACC_MAP_SIZE)) {
                int existValue = driveConsumption[speed][acc];
                int existFrequency = driveFrequency[speed][acc];

                driveConsumption[speed][acc] = (int)(((double)power + (double)(existValue * existFrequency))
                        / (double)(existFrequency + 1));

                driveFrequency[speed][acc]++;
            }
        }
    }

    /**
     * When saving maps to file
     */
    public static void subtractStaticConsumption() {

        //update Static Consumption map values: zero speed, zero acceleration
        staticConsumption[TEMP_OFFSET + temperature][0] = driveConsumption[0][ACC_OFFSET];
        staticConsumption[TEMP_OFFSET + temperature][1] = driveFrequency[0][ACC_OFFSET];

        //subtract current static value from all consumptions
        driveFrequency[0][ACC_OFFSET] -= staticConsumption[TEMP_OFFSET + temperature][1];
        for(int i = 0; i < driveFrequency.length; i++) {     //speed as row
            for(int j = 0; j < driveFrequency[i].length; j++) { //acceleration as column
                if(driveFrequency[i][j] > 0) {
                    //subtract static power at temperature - zero is 40.th row
                    driveConsumption[i][j] -= staticConsumption[TEMP_OFFSET + temperature][0];
                }
            }
        }
    }

    /**
     * When reading maps from file
     */
    public static void addStaticConsumption() {

        //update Freq and Consumption map values: zero speed, zero acceleration
        driveConsumption[0][ACC_OFFSET] = staticConsumption[TEMP_OFFSET + temperature][0];
        driveFrequency[0][ACC_OFFSET] = staticConsumption[TEMP_OFFSET + temperature][1];

        //add current static value to all consumptions
        driveFrequency[0][ACC_OFFSET] += staticConsumption[TEMP_OFFSET + temperature][1];
        for(int i = 0; i < MapCanValues.driveFrequency.length; i++) {     //speed as row
            for(int j = 0; j < MapCanValues.driveFrequency[i].length; j++) { //acceleration as column

                if(MapCanValues.driveFrequency[i][j] > 0) {
                    //subtract static power at temperature - zero is 40
                    MapCanValues.driveConsumption[i][j] += MapCanValues.staticConsumption[MapCanValues.TEMP_OFFSET + MapCanValues.temperature][0];
                }
            }
        }
    }

    /*public static String realSpeedAndPowerInSteps(Location loc) {

        if(routeStepList == null || routeStepList.isEmpty() || loc == null) {
            return null;
        }
        stepSpeedList.add(speed);
        stepPowerList.add((int)(amp * volt));
        float[] distance = {0};
        if(step == null) {
            step = routeStepList.remove(0);
        }
        Location.distanceBetween(loc.getLatitude(), loc.getLongitude(), step.getEndLat(), step.getEndLng(), distance);
        //reached to end location of step
        if(distance[0] < loc.getAccuracy()) {
            //average speed
            Iterator<Integer> it = stepSpeedList.iterator();
            int sum = 0;
            while(it.hasNext()) {
                sum += it.next();
            }
            Integer speedOfStep = Math.round((float)sum / (float)stepSpeedList.size());//km/h
            Integer plannedSpeed = Math.round(step.getDistance() / step.getDuration());//km/h
            setSpeedDiffMapValues(plannedSpeed, speedOfStep);
            stepSpeedList.clear();
            step = null;
            //average power
            Iterator<Integer> ip = stepPowerList.iterator();
            int all = 0;
            while(ip.hasNext()) {
                all += ip.next();
            }
            Integer powerOfStep = Math.round((float)all / (float)stepPowerList.size());

            //     planned speed                   average real speed             distance to step endpoint
            return plannedSpeed.toString() + "," + speedOfStep.toString() + "," + Float.toString(distance[0])
                    //                     planned power expense in percent
                    + "," + Double.toString(CalculateMaps.calculateExpenseAtSpeedAndTime(plannedSpeed, (double)step.getDuration()))
                    //      average real power
                    + "," + powerOfStep.toString();
        }
        return null;
    }

    public static void setSpeedDiffMapValues(int plannedSpeed, int realSpeed) {

        SimpleDateFormat sdf = new SimpleDateFormat("H");
        String currentDateandTime = sdf.format(new Date());
        if(speedDiff[plannedSpeed][Integer.parseInt(currentDateandTime)] == 0) {
            speedDiff[plannedSpeed][Integer.parseInt(currentDateandTime)] = plannedSpeed - realSpeed;
        } else {
            speedDiff[plannedSpeed][Integer.parseInt(currentDateandTime)] = Math.round(
                    ((float)(plannedSpeed - realSpeed) + (float)speedDiff[plannedSpeed][Integer.parseInt(currentDateandTime)]) / 2);
        }
    }*/
}
