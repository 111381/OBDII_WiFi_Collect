package com.bitmaster.obdii_wifi_collect.obdwifi.io;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.InetAddress;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

import android.app.Service;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Binder;
import android.os.IBinder;
import android.util.Log;

import com.bitmaster.obdii_wifi_collect.obdwifi.obd2.SupportedPids;

/**
 * Created by renet on 3/12/14.
 */
public class TcpClientService extends Service {

    public static final int TCP_SERVER_PORT = 35000;
    public static final String SERVER_IP_ADDRESS = "192.168.0.10";
    //private static final int TCP_SERVER_PORT = 80;
    //private static final String SERVER_IP_ADDRESS = "192.168.50.2";//proekspert.ee

    private final IBinder mBinder = new MyBinder();
    private ArrayList<String> list = new ArrayList<String>();
    private SupportedPids pids = null;
    //TODO:interrupt by user
    private boolean continueRequests = true;
    private Socket socket = null;
    private InetAddress ip = null;
    //Static value for calculating process time
    //private static long time = System.currentTimeMillis();

    //This allows you to communicate directly with the service.
    @Override
    public IBinder onBind(Intent arg0) {

        return mBinder;
    }
    @Override
    public void onCreate() {

    }
    @Override
    public void onDestroy() {

        //TODO: close connection
        //socket.close();
    }
    // can be called several times
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        new RequestToCreateSocket().execute("");

        return Service.START_STICKY;//Service is restarted if it gets terminated.
    }

    public class MyBinder extends Binder {

        public TcpClientService getService() {

            return TcpClientService.this;
        }
    }

    public List<String> getWordList() {

        return list;
    }

    private void requestLogic(String response) {

        if(socket == null || socket.isClosed()) {
            new RequestToCreateSocket().execute("");
            return;
        }
        list.add(response);

        String pid;
        // call recursively:
        // First do init requests since response 'null'
        if(!pids.isInitDone()) {
            pid = pids.getNextInit();
            if(pid == null) {
                pids.setInitDone(true);
            }
        } else {
            pid = pids.getNextPid();
        }
        // next request with next PID
        if(pid != null) {
            new RequestToSocketTask().execute(pid);
        } else { //write list to file and clear list
            try {
                new WriteDown(list);
                list.clear();
            } catch (IOException e) {
                e.printStackTrace();
                list.add(e.getLocalizedMessage());
            }
            if(continueRequests) { //next iteration until interrupt
                pid = pids.getNextPid();
                new RequestToSocketTask().execute(pid);
            }
        }
    }

    // Uses AsyncTask to create a task away from the main UI thread.
    private class RequestToCreateSocket extends AsyncTask<String, Void, String> {
        @Override
        protected String doInBackground(String... msg) {
            try {
                if (socket != null && socket.isConnected()) {
                    socket.close();
                }
                ip = InetAddress.getByName(SERVER_IP_ADDRESS);
                socket = new Socket(ip, TCP_SERVER_PORT);
            } catch (IOException e) {
                e.printStackTrace();
            }
            pids = new SupportedPids();

            return "ATZ"; //run reset command as result task
        }

        @Override
        protected void onPostExecute(String result) {

            //start requests to server with reset command
            new RequestToSocketTask().execute(result);
        }
    }

    // Uses AsyncTask to create a task away from the main UI thread.
    //Once the socket is created, the AsyncTask sends string to server.
    // responsed InputStream is converted into a string, which is
    // added to List by the AsyncTask's onPostExecute method.
    private class RequestToSocketTask extends AsyncTask<String, Void, String> {
        @Override
        protected String doInBackground(String... msg) {

            return runTcpClient(msg[0]);
        }
        // onPostExecute displays the results of the AsyncTask.
        @Override
        protected void onPostExecute(String result) {

            requestLogic(result);
        }
    }

    private String runTcpClient(String outMsg) {
        try {
            BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            BufferedWriter out = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));
            //send output msg
            out.write(outMsg + "\r");
            out.flush();
            Log.i("TcpClient", "sent: " + outMsg);
            //accept server response
            int character = 0;
            String inMsg = "";
            while((character = in.read()) != 62) { // EOL == '>'
                inMsg = inMsg + Character.toString((char) character);
            }
            Log.i("TcpClient", "received: " + inMsg);

            return inMsg;

        } catch (IOException e) {
            e.printStackTrace();
            return e.getLocalizedMessage();
        }
    }
}
