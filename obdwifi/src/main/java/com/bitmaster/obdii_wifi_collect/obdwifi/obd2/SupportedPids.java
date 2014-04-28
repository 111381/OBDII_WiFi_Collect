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
    private final String CAN_INIT_FILE = "CANInit.txt";

    private List<String> commands = new ArrayList<String>();
    private List<String> initCommands = new ArrayList<String>();
    private List<String> canInitCommands = new ArrayList<String>();
    private Iterator<String> it = null;
    private Iterator<String> initIt = null;
    private Iterator<String> canInitIt = null;
    private boolean initDone = false;

    public SupportedPids() {

        if(!readFromFile(this.CAN_INIT_FILE)) {
            //canInitCommands.add("ATD");//factory settings
            canInitCommands.add("ATWS");//factory settings
            canInitCommands.add("ATTP6");//use CAN 11-bit MsgID, 500,000 baud
            canInitCommands.add("ATH1");// headers on
            canInitCommands.add("ATL0");//No CrLf
            canInitCommands.add("ATS0");//Suppress Spaces
            canInitCommands.add("ATCAF0");//disable can autoformat
            //canInitCommands.add("ATCRA");//clean address
            //canInitCommands.add("ATCRA412");//Speed and Odometer
            canInitCommands.add("ATCF 412");
            canInitCommands.add("ATCM FFF");
            //canInitCommands.add("ATE0");//Echo Off
            //canInitCommands.add("ATMA");//monitor all
        }
        //if reading from file unsuccessful
        if(!readFromFile(this.INIT_FILE)) {
            initCommands.add("ATTP6");//use CAN 11-bit MsgID, 500,000 baud
            initCommands.add("ATH1");// headers on
            initCommands.add("ATL0");//No CrLf
            initCommands.add("ATS0");//Suppress Spaces
            initCommands.add("ATE0");//Echo Off
            //initCommands.add("ATMA");//monitor all
            initCommands.add("ATCAF0");//disable can autoformat
            //STSBR 500000 ; sets Baud Rate to 500,000 baud
        }
        if(!readFromFile(this.PID_FILE)) {
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


        }

        it = commands.listIterator(0);
        initIt = initCommands.listIterator(0);
        canInitIt = canInitCommands.listIterator(0);
    }

    private boolean readFromFile(String filename) {

        if(! isExternalStorageReadable()){
            return false;
        }
        File file = new File(Environment.getExternalStorageDirectory(), filename);
        try {
            FileInputStream inputStream = new FileInputStream(file);
            BufferedReader in = new BufferedReader(new InputStreamReader(inputStream));
            String line = "";
            while((line = in.readLine()) != null){
                if(filename.equalsIgnoreCase(this.INIT_FILE)){
                    initCommands.add(line);
                }
                if(filename.equalsIgnoreCase(this.PID_FILE)){
                    commands.add(line);
                }
                if(filename.equalsIgnoreCase(this.CAN_INIT_FILE)){
                    canInitCommands.add(line);
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

    public String getNextCanInit() {
        if(this.canInitIt.hasNext()) {
            return canInitIt.next();
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
