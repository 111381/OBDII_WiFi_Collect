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
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.util.Log;
import android.widget.Toast;

import com.bitmaster.obdii_wifi_collect.obdwifi.obd2.SupportedPids;

/**
 * Created by renet on 3/12/14.
 */
public class TcpClientService extends Service {

    public static final int TCP_SERVER_PORT = 35000;
    public static final String SERVER_IP_ADDRESS = "192.168.0.10";
    /**
     * Command to service to set a new value.  This can be sent to the
     * service to supply a new value, and will be sent by the service to
     * any registered clients with the new value.
     */
    public static final int MSG_SET_VALUE = 3;

    private ArrayList<String> list = new ArrayList<String>();
    private SupportedPids pids = null;
    private Socket socket = null;
    private AsyncTask<String, Void, String> tcpTask = null;
    //TODO:interrupt by user
    private boolean continueRequests = true;
    //Static value for calculating process time
    //private static long time = System.currentTimeMillis();

    @Override
    public void onCreate() {

    }
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        return Service.START_STICKY;//Service is restarted if it gets terminated.
    }
    @Override
    public void onDestroy() {
        //All cleanup (stopping threads, unregistering receivers)
        // should be complete upon returning from onDestroy().
        if(tcpTask != null) {
            tcpTask.cancel(false);//do not interrupt finishing task
        }
    }
    /**
     * When binding to the service, we return an interface to our messenger
     * for sending messages to the service.
     */
    @Override
    public IBinder onBind(Intent arg0) {

        this.pids = new SupportedPids();
        tcpTask = new RequestToSocketTask().execute("ATZ");

        Toast.makeText(getApplicationContext(), "binding", Toast.LENGTH_SHORT).show();

        return mMessenger.getBinder();
    }
    /**
     * Handler of incoming messages from service.
     */
    class IncomingHandler extends Handler {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case TcpClientService.MSG_SET_VALUE:
                    Toast.makeText(getApplicationContext(), msg.obj.toString(), Toast.LENGTH_SHORT).show();//TODO: something
                    break;
                default:
                    super.handleMessage(msg);
            }
        }
    }
    /**
     * Target we publish for clients to send messages to IncomingHandler.
     */
    final Messenger mMessenger = new Messenger(new IncomingHandler());


    public List<String> getWordList() {

        return list;
    }

    private String runTcpClient(String outMsg) {
        try {
            InetAddress ip = InetAddress.getByName(SERVER_IP_ADDRESS);
            this.socket = new Socket(ip, TCP_SERVER_PORT);
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
            socket.close();

            return inMsg;

        } catch (IOException e) {
            e.printStackTrace();
            return e.getLocalizedMessage();
        }
    }

    private void requestLogic(String response) {

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
        @Override
        protected void onCancelled() {

            if(socket != null && socket.isConnected()) {
                try {
                    socket.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }
}
