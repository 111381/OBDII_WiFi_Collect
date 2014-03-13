package com.bitmaster.obdii_wifi_collect.obdwifi.obd2;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Created by uhh on 3/13/14.
 */
public class SupportedPids {
    private List<String> commands = new ArrayList<String>();
    private List<String> initCommands = new ArrayList<String>();
    private Iterator<String> it = null;
    private Iterator<String> initIt = null;

    public SupportedPids() {

        initCommands.add("ATZ"); //reset
        initCommands.add("ATDP"); //display protocol
        initCommands.add("ATSP3");//set ISO 9141-2 protocol
        initCommands.add("ATE0 ");// echo off
        initCommands.add("0100");//PIDs supported [01 - 20]
        initCommands.add("0120");//PIDs supported [21 - 40]
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

        it = commands.listIterator(0);
        initIt = initCommands.listIterator(0);
    }

    public String getNextPid() {

        if(!this.it.hasNext()) {
            this.it = commands.listIterator(0);
        }
        return it.next();
    }

    public String getNextInit() {

        if(this.initIt.hasNext()) {
            return initIt.next();
        } else {
            return null;
        }
    }
}
