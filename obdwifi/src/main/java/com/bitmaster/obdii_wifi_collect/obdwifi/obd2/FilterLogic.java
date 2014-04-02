package com.bitmaster.obdii_wifi_collect.obdwifi.obd2;

import com.bitmaster.obdii_wifi_collect.obdwifi.MainActivity;

import java.util.Timer;
import java.util.TimerTask;

/**
 * Created by renet on 4/2/14.
 * Finds exceptions, otherwise no action
 * By exception, saves data and sets up timer with new request
 */
public class FilterLogic {

    private static final String NO_DATA = "NO DATA";
    private static final String ERROR = "ERROR";

    private MainActivity main = null;

    public FilterLogic(MainActivity main) {

        this.main = main;
    }

    public boolean isResponseFaulty(String response) {

        if(response.contains(NO_DATA)){
            Timer timer = new Timer();
            TimerTask restartRequestsTask = new RestartRequestsTask();
            timer.schedule(restartRequestsTask, 1000);

            main.saveToFile();
            return true;
        }

        if(response.contains(ERROR)){
            Timer timer = new Timer();
            TimerTask restartRequestsTask = new RestartRequestsTask();
            timer.schedule(restartRequestsTask, 10000);

            main.saveToFile();
            return true;
        }

        return false;
    }

    private class RestartRequestsTask extends TimerTask {
        public void run() {
            main.startRequests();
        }
    }
}
