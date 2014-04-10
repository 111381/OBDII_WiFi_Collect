package com.bitmaster.obdii_wifi_collect.obdwifi.obd2;

import android.os.Environment;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Created by uhh on 3/13/14.
 */
public class SupportedPids {

    private final String PID_FILE = "OBDIIPids.txt";
    private final String INIT_FILE = "OBDIIInit.txt";

    private List<String> commands = new ArrayList<String>();
    private List<String> initCommands = new ArrayList<String>();
    private Iterator<String> it = null;
    private Iterator<String> initIt = null;
    private boolean initDone = false;

    public SupportedPids() {

        //if reading from file unsuccessful
        if(!readFromFile(this.INIT_FILE, true)) {
            //initCommands.add("ATSPA3");//set ISO 9141-2 protocol, optional auto
            initCommands.add("ATTP3");//try ISO 9141-2 protocol, optional auto
            //initCommands.add("ATE0 ");// echo off
        }
        if(!readFromFile(this.PID_FILE, false)) {
            commands.add("0101");//Monitor status since DTCs cleared. (Includes malfunction indicator lamp (MIL) status and number of DTCs.)
            commands.add("0103");//Fuel system status
            commands.add("0104");//Calculated engine load value
            commands.add("0105");//Engine coolant temperature
            commands.add("0106");//Short term fuel % trim—Bank 1
            commands.add("0107");//Long term fuel % trim—Bank 1
            commands.add("010C");//Engine RPM
            commands.add("010D");//Vehicle speed
            commands.add("010E");//Timing advance
            commands.add("010F");//Intake air temperature
            commands.add("0110");//MAF air flow rate
            commands.add("0111");//Throttle position
            commands.add("0113");//Oxygen sensors present
            commands.add("0114");//Bank 1, Sensor 1: Oxygen sensor voltage,Short term fuel trim
            commands.add("0115");//Bank 1, Sensor 2: Oxygen sensor voltage, Short term fuel trim
            commands.add("011C");//OBD standards this vehicle conforms to
            commands.add("0121");//Distance traveled with malfunction indicator lamp (MIL) on

            commands.add("0100");
            commands.add("0120");
            commands.add("0140");
            commands.add("0160");
            commands.add("0180");
            commands.add("01A0");
            commands.add("01C0");

            //commands.add("ATMA");//monitor all
        }

        it = commands.listIterator(0);
        initIt = initCommands.listIterator(0);
    }

    private boolean readFromFile(String filename, boolean init) {

        if(! isExternalStorageReadable()){
            return false;
        }
        File file = new File(Environment.getExternalStorageDirectory(), filename);
        try {
            FileInputStream inputStream = new FileInputStream(file);
            BufferedReader in = new BufferedReader(new InputStreamReader(inputStream));
            String line = "";
            while((line = in.readLine()) != null){
                if(init){
                    initCommands.add(line);
                } else {
                    commands.add(line);
                }
            }
            inputStream.close();
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }

        return true;
    }

    /* Checks if external storage is available for read and write */
    private static boolean isExternalStorageReadable() {
        String state = Environment.getExternalStorageState();
        if (Environment.MEDIA_MOUNTED.equals(state)) {
            return true;
        }
        return false;
    }

    public String getNextPid() {

        if(this.it.hasNext()) {
            return it.next();
        } else {
            this.it = commands.listIterator(0);
            return null;
        }
    }

    public String getNextInit() {

        if(this.initIt.hasNext()) {
            return initIt.next();
        } else {
            return null;
        }
    }

    public boolean isInitDone() {
        return initDone;
    }

    public void setInitDone(boolean initDone) {
        this.initDone = initDone;
    }
}
