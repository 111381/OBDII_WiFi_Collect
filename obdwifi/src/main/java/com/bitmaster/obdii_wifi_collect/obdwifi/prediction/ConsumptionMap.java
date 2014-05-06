package com.bitmaster.obdii_wifi_collect.obdwifi.prediction;

import android.os.Environment;

import com.bitmaster.obdii_wifi_collect.obdwifi.obd2.MapCanValues;
import com.bitmaster.obdii_wifi_collect.obdwifi.obd2.Message;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;

/**
 * Created by uhh on 5/4/14.
 */
public class ConsumptionMap {

    private static final String POWER_MAP_FILENAME = "EPowerMap.csv";
    private static final int SPEED_MAP_SIZE = 128;
    private static final int ACC_MAP_SIZE = 256;
    //power accuracy: 1 watt                      speed       acceleration
    private static int [][] driving = new int[SPEED_MAP_SIZE][ACC_MAP_SIZE]; //32768 * 4 bytes
    private static int power;
    private static int speed;
    private static int acceleration;

    public static void setMapValue(Message message) {

        if(message.getPid().equalsIgnoreCase(MapCanValues.POWER)) {

            power = (int)(Double.parseDouble(message.getValue1()) * Double.parseDouble(message.getValue2())); //amp * volt
        } else if(message.getPid().equalsIgnoreCase(MapCanValues.SPEED)) {

            speed = Integer.parseInt(message.getValue1());                                        //1 km/h unit
            acceleration = ((int)(Double.parseDouble(MapCanValues.getAcceleration()) * 25)) + 128;//0,04 m/s2 unit
        } else if(message.getPid().equalsIgnoreCase(MapCanValues.SHIFT)
                && message.getValue1().equalsIgnoreCase(MapCanValues.DRIVE)) { //only for shift DRIVE position
            // check if fits to array
            if((0 <= speed) && (speed < SPEED_MAP_SIZE) && (0 <= acceleration) && (acceleration < ACC_MAP_SIZE)) {
                int existValue = driving[speed][acceleration];
                if(existValue == 0) {
                    driving[speed][acceleration] = power;
                } else {
                    driving[speed][acceleration] = (int)(((double)power + (double)existValue) / 2);
                }

            }
        }
    }

    public static void writePowerMapToFile() {

        if (Environment.MEDIA_MOUNTED.equals(Environment.getExternalStorageState())) {
            File file = new File(Environment.getExternalStorageDirectory(), POWER_MAP_FILENAME);
            try {
                FileOutputStream outputStream = new FileOutputStream(file, false);//append==false
                for(int i = 0; i < SPEED_MAP_SIZE; i++) {     //speed as row
                    for(int j = 0; j < ACC_MAP_SIZE; j++) { //acceleration as column
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

    public static void readPowerMapFromFile() {

        if (Environment.MEDIA_MOUNTED.equals(Environment.getExternalStorageState())) {
            File file = new File(Environment.getExternalStorageDirectory(), POWER_MAP_FILENAME);
            try {
                FileInputStream inputStream = new FileInputStream(file);
                BufferedReader in = new BufferedReader(new InputStreamReader(inputStream));
                String line = "";
                int i = 0;
                while(((line = in.readLine()) != null) && (i < SPEED_MAP_SIZE)) { //speed as row
                    String[] items = line.split(",");
                    for (int j = 0; j < ACC_MAP_SIZE; j++) { //acceleration as column
                        driving[i][j] = Integer.parseInt(items[j]);
                    }
                    i++;
                }
                inputStream.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}
