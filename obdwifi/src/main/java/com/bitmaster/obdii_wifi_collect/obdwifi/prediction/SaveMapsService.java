package com.bitmaster.obdii_wifi_collect.obdwifi.prediction;

import android.app.IntentService;
import android.content.Intent;
import android.os.Environment;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;

/**
 * Created by renet on 5/7/14.
 */
public class SaveMapsService extends IntentService {

    private static final String POWER_MAP_FILENAME = "EPowerMap.csv";
    private static final String FREQ_MAP_FILENAME = "FreqMap.csv";

    public SaveMapsService() {
        super("SaveMapsService");
    }

    @Override
    protected void onHandleIntent(Intent intent) {

        synchronized (this) {
            if (Environment.MEDIA_MOUNTED.equals(Environment.getExternalStorageState())) {
                boolean write = intent.getBooleanExtra("com.bitmaster.obdii_wifi_collect.obdwifi.prediction.Write", false);
                if (write) {
                    writePowerMapToFile();
                    writeFrequencyMapToFile();
                } else {
                    readPowerMapFromFile();
                    readFrequencyMapFromFile();
                }
            }
        }
    }

    private static void writePowerMapToFile() {

        File file = new File(Environment.getExternalStorageDirectory(), POWER_MAP_FILENAME);
        try {
            FileOutputStream outputStream = new FileOutputStream(file, false);//append==false
            for(int i = 0; i < MapCanValues.driveConsumption.length; i++) {     //speed as row
                for(int j = 0; j < MapCanValues.driveConsumption[i].length; j++) { //acceleration as column
                    String item = Integer.toString(MapCanValues.driveConsumption[i][j]) + ",";
                    outputStream.write(item.getBytes());
                }
                outputStream.write(";\n".getBytes()); //end of line
            }
            outputStream.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void writeFrequencyMapToFile() {

        File file = new File(Environment.getExternalStorageDirectory(), FREQ_MAP_FILENAME);
        try {
            FileOutputStream outputStream = new FileOutputStream(file, false);//append==false
            for(int i = 0; i < MapCanValues.driveFrequency.length; i++) {     //speed as row
                for(int j = 0; j < MapCanValues.driveFrequency[i].length; j++) { //acceleration as column
                    String item = Integer.toString(MapCanValues.driveFrequency[i][j]) + ",";
                    outputStream.write(item.getBytes());
                }
                outputStream.write(";\n".getBytes()); //end of line
            }
            outputStream.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void readPowerMapFromFile() {

        File file = new File(Environment.getExternalStorageDirectory(), POWER_MAP_FILENAME);
        try {
            FileInputStream inputStream = new FileInputStream(file);
            BufferedReader in = new BufferedReader(new InputStreamReader(inputStream));
            String line = "";
            int i = 0;
            while(((line = in.readLine()) != null) && (i < MapCanValues.driveConsumption.length)) { //speed as row
                String[] items = line.split(",");
                for (int j = 0; j < MapCanValues.driveConsumption[i].length; j++) { //acceleration as column
                    MapCanValues.driveConsumption[i][j] = Integer.parseInt(items[j]);
                }
                i++;
            }
            inputStream.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void readFrequencyMapFromFile() {

        File file = new File(Environment.getExternalStorageDirectory(), FREQ_MAP_FILENAME);
        try {
            FileInputStream inputStream = new FileInputStream(file);
            BufferedReader in = new BufferedReader(new InputStreamReader(inputStream));
            String line = "";
            int i = 0;
            while(((line = in.readLine()) != null) && (i < MapCanValues.driveFrequency.length)) { //speed as row
                String[] items = line.split(",");
                for (int j = 0; j < MapCanValues.driveFrequency[i].length; j++) { //acceleration as column
                        MapCanValues.driveFrequency[i][j] = Integer.parseInt(items[j]);
                }
                i++;
            }
            inputStream.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
