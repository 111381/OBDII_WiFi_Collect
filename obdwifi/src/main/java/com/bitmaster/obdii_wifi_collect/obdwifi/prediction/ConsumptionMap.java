package com.bitmaster.obdii_wifi_collect.obdwifi.prediction;

import android.os.Environment;

import com.bitmaster.obdii_wifi_collect.obdwifi.obd2.MapCanValues;
import com.bitmaster.obdii_wifi_collect.obdwifi.obd2.Message;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

/**
 * Created by uhh on 5/4/14.
 */
public class ConsumptionMap {

    private static final String POWER_MAP_FILENAME = "EPowerMap.csv";
    private static final int MAP_SIZE = 256;
    //power accuracy: 1 watt                   speed  acceleration
    private static int [][] driving = new int[MAP_SIZE][MAP_SIZE]; //65536 * 4 bytes
    private static int power;
    private static int speed;
    private static int acceleration;

    public static void setMapValue(Message message) {

        if(message.getPid().equalsIgnoreCase(MapCanValues.POWER)) {

            power = (int)(Double.parseDouble(message.getValue1()) * Double.parseDouble(message.getValue2())); //amp * volt
        } else if(message.getPid().equalsIgnoreCase(MapCanValues.SPEED)) {

            speed = Integer.parseInt(message.getValue1());
            acceleration = (int)(Double.parseDouble(MapCanValues.getAcceleration()) * 100);//255/100 = 2,55 m/s2
        } else if(message.getPid().equalsIgnoreCase(MapCanValues.SHIFT)
                && message.getValue1().equalsIgnoreCase(MapCanValues.DRIVE)) { //only for shift DRIVE position
            // check if fits to array
            if((0 <= speed) && (speed < MAP_SIZE) && (0 <= acceleration) && (acceleration < MAP_SIZE)) {
                driving[speed][acceleration] = power;
            }
        }
    }

    public static void writePowerMapToFile() {

        if (Environment.MEDIA_MOUNTED.equals(Environment.getExternalStorageState())) {
            File file = new File(Environment.getExternalStorageDirectory(), POWER_MAP_FILENAME);
            try {
                FileOutputStream outputStream = new FileOutputStream(file, false);//append==false
                for(int i = 0; i < MAP_SIZE; i++) {     //speed as row
                    for(int j = 0; j < MAP_SIZE; j++) { //acceleration as column
                        String item = Integer.toString(driving[i][j]) + ",";
                        outputStream.write(item.getBytes());
                    }
                    outputStream.write(";\n".getBytes()); //end of line
                }
                outputStream.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
