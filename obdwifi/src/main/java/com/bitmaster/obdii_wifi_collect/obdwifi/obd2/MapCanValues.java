package com.bitmaster.obdii_wifi_collect.obdwifi.obd2;

import android.util.Log;

import java.math.BigInteger;

/**
 * Created by renet on 5/2/14.
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

    private static long lastSpeed;
    private static long lastTime;
    private static double acceleration;


    public static Message decodeCanMessage(String message) {
        try {
            if( message.length() == 19 && message.substring(0, 3).equalsIgnoreCase(SPEED)) {

                long speed = Long.parseLong(message.substring(5, 7), 16);

                acceleration = ((double)(speed - lastSpeed) / 3.6) / ((double)(System.currentTimeMillis() - lastTime) / 1000) ; // m/s2
                lastSpeed = speed;
                lastTime = System.currentTimeMillis();

                long odo = (Long.parseLong(message.substring(7, 9), 16) * 65536) +
                        (Long.parseLong(message.substring(9, 11), 16) * 256) +
                        Long.parseLong(message.substring(11, 13), 16);

                return new Message(SPEED, Long.toString(speed), Long.toString(odo));
            }
            if(message.length() == 19 && message.substring(0, 3).equalsIgnoreCase(SOC)) {

                double percent = ((double)Long.parseLong(message.substring(5, 7), 16) - 10) / 2;
                return new Message(SOC, Double.toString(percent), null);
            }
            if(message.length() == 19 && message.substring(0, 3).equalsIgnoreCase(POWER)) {

                double amp = ((double)(Long.parseLong(message.substring(7, 9), 16) * 256) +
                        (double)(Long.parseLong(message.substring(9, 11), 16) - (128 * 256))) / 100;

                double volt = ((double)(Long.parseLong(message.substring(11, 13), 16) * 256) +
                        (double)Long.parseLong(message.substring(13, 15), 16)) / 10;

                return new Message(POWER, Double.toString(amp), Double.toString(volt));
            }
            if(message.length() == 19 && message.substring(0, 3).equalsIgnoreCase(RANGE)) {

                return new Message(RANGE, Long.toString(Long.parseLong(message.substring(17, 19), 16)), null);
            }
            if(message.length() == 17 && message.substring(0, 3).equalsIgnoreCase(SHIFT)) {

                if(message.substring(3, 5).equalsIgnoreCase("50")) {
                    return new Message(SHIFT, PARK, null);
                }
                if(message.substring(3, 5).equalsIgnoreCase("52")) {
                    return new Message(SHIFT, REV, null);
                }
                if(message.substring(3, 5).equalsIgnoreCase("4E")) {
                    return new Message(SHIFT, NEUT, null);
                }
                if(message.substring(3, 5).equalsIgnoreCase("44")) {
                    return new Message(SHIFT, DRIVE, null);
                }
                if(message.substring(3, 5).equalsIgnoreCase("83")) {
                    return new Message(SHIFT, BR, null);
                }
                if(message.substring(3, 5).equalsIgnoreCase("32")) {
                    return new Message(SHIFT, ECO, null);
                }
            }

        } catch (NumberFormatException e) {
            Log.i("NumberEx", e.getLocalizedMessage());
            return null;
        }

        return null;
    }

    public static String getAcceleration() {

        return Double.toString(acceleration);
    }

    private static String hexToBin(String s) throws NumberFormatException {

        return new BigInteger(s, 16).toString(2);
    }

    private static String hexToDec(String s) throws NumberFormatException {

        return new BigInteger(s, 16).toString(10);
    }
}
