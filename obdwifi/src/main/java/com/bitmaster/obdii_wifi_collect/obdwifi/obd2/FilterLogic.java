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
    public static final String TCP_ERROR = "TcpErr";
    private static final String ATTP = "ATTP";

    private MainActivity main = null;

    public FilterLogic(MainActivity main) {

        this.main = main;
    }

    public boolean isResponseFaulty(String response) {

        if(response.contains(NO_DATA)){
            Timer timer = new Timer();
            TimerTask restartRequestsTask = new RestartRequestsTask();
            timer.schedule(restartRequestsTask, 3000);

            main.saveToFile();
            return true;
        }

        if(response.contains(ERROR) || response.contains(TCP_ERROR)){
            Timer timer = new Timer();
            TimerTask restartRequestsTask = new RestartRequestsTask();
            timer.schedule(restartRequestsTask, 30000);

            //main.saveToFile();
            return true;
        }

        if(response.contains(ATTP)){
            Timer timer = new Timer();
            TimerTask continueRequestTask = new ContinueRequestTask();
            timer.schedule(continueRequestTask, 3000);

            return true;
        }

        return false;
    }

    private class RestartRequestsTask extends TimerTask {
        public void run() {
            main.startRequests("ATWS");
        }//warm reset, without LED test
    }
    //Continue requests
    private class ContinueRequestTask extends TimerTask {
        public void run() {
            main.nextRequestFromList();
        }
    }
}
