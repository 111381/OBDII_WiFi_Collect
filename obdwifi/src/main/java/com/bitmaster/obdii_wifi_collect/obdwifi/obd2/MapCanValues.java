package com.bitmaster.obdii_wifi_collect.obdwifi.obd2;

import java.math.BigInteger;

/**
 * Created by renet on 5/2/14.
 */
public class MapCanValues {

    public static final String SPEED = "412";
    public static final String SOC = "374";
    public static final String POWER = "373";
    public static final String RANGE = "346";
    public static final String SHIFT = "418";


    public static Message decodeCanMessage(String message) {
        try {
            if(message.substring(0, 3).equalsIgnoreCase(SPEED) && message.length() == 19) {

                String speed = hexToDec(message.substring(5, 7));
                long odo = (Long.parseLong(message.substring(7, 9), 16) * 65536) +
                        (Long.parseLong(message.substring(9, 11), 16) * 256) +
                        Long.parseLong(message.substring(11, 13), 16);

                return new Message(SPEED, speed, Long.toString(odo));
            }
            if(message.substring(0, 3).equalsIgnoreCase(SOC) && message.length() == 19) {

                long percent = (Long.parseLong(message.substring(5, 7), 16) - 10) / 2;
                return new Message(SOC, Long.toString(percent), null);
            }
            if(message.substring(0, 3).equalsIgnoreCase(POWER) && message.length() == 19) {

                long amp = ((Long.parseLong(message.substring(7, 9), 16) * 256) +
                        (Long.parseLong(message.substring(9, 11), 16) - (128 * 256))) / 100;
                long volt = ((Long.parseLong(message.substring(11, 13), 16) * 256) +
                        Long.parseLong(message.substring(13, 15), 16) / 10);

                return new Message(POWER, Long.toString(amp), Long.toString(volt));
            }
            if(message.substring(0, 3).equalsIgnoreCase(RANGE) && message.length() == 19) {

                return new Message(RANGE, hexToDec(message.substring(17, 19)), null);
            }
            if(message.substring(0, 3).equalsIgnoreCase(SHIFT) && message.length() == 17) {

                if(message.substring(3, 5).equalsIgnoreCase("50")) {
                    return new Message(SHIFT, "P", null);
                }
                if(message.substring(3, 5).equalsIgnoreCase("52")) {
                    return new Message(SHIFT, "R", null);
                }
                if(message.substring(3, 5).equalsIgnoreCase("4E")) {
                    return new Message(SHIFT, "N", null);
                }
                if(message.substring(3, 5).equalsIgnoreCase("44")) {
                    return new Message(SHIFT, "D", null);
                }
                if(message.substring(3, 5).equalsIgnoreCase("83")) {
                    return new Message(SHIFT, "B", null);
                }
                if(message.substring(3, 5).equalsIgnoreCase("32")) {
                    return new Message(SHIFT, "C", null);
                }
            }

        } catch (NumberFormatException e) {
            return null;
        }
        return null;
    }



    private static String hexToBin(String s) throws NumberFormatException {

        return new BigInteger(s, 16).toString(2);
    }

    private static String hexToDec(String s) throws NumberFormatException {

        return new BigInteger(s, 16).toString(10);
    }
}
