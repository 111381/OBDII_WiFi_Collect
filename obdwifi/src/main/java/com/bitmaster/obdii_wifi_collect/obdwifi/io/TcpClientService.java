package com.bitmaster.obdii_wifi_collect.obdwifi.io;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;

import android.app.Service;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.util.Log;
import android.widget.Toast;

import com.bitmaster.obdii_wifi_collect.obdwifi.obd2.SupportedPids;

/**
 * Created by renet on 3/12/14.
 */
public class TcpClientService extends Service {

    public static final int TCP_SERVER_PORT = 35000;
    public static final String SERVER_IP_ADDRESS = "192.168.0.10";
    public static final int SOCKET_CONN_TIMEOUT = 5000;
    /**
     * Command to service to set a new value.  This can be sent to the
     * service to supply a new value, and will be sent by the service to
     * any registered clients with the new value.
     */
    public static final int MSG_START_REQUESTS = 0;
    public static final int MSG_OBDII_RESPONSE = 1;
    public static final int MSG_WRITE_LIST_TO_FILE = 2;
    public static final int MSG_STOP_REQUESTS = 3;
    private Messenger client = null;

    private SupportedPids pids = null;
    private Socket socket = null;
    private AsyncTask<String, Void, String> tcpTask = null;

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
            tcpTask.cancel(true);//do interrupt finishing task
        }
    }
    @Override
    public boolean onUnbind(Intent arg0) {
        if(tcpTask != null) {
            tcpTask.cancel(true);//do interrupt finishing task
        }
        return false;//Return true if you would like to have the service's onRebind(Intent) method later called when new clients bind to it.
    }
    /**
     * When binding to the service, we return an interface to our messenger
     * for sending messages to the service.
     */
    @Override
    public IBinder onBind(Intent arg0) {

        return mMessenger.getBinder();
    }
    /**
     * Handler of incoming messages from service.
     */
    class IncomingHandler extends Handler {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case TcpClientService.MSG_START_REQUESTS:
                    //Get messenger to send messages to UI
                    client = msg.replyTo;
                    //Create fresh queue of PID and start requests with reset
                    pids = new SupportedPids();
                    tcpTask = new RequestToSocketTask().execute("ATZ");
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

    /**
     * Sends response string to client after every response
     * @param line
     */
    private void sendStringToClient(String line) {

        if(client != null) {
            Message msg = Message.obtain(null, TcpClientService.MSG_OBDII_RESPONSE, line);
            try {
                client.send(msg);
            } catch (RemoteException e) {
                e.printStackTrace();
                Toast.makeText(getApplicationContext(), "Cannot send messages to UI", Toast.LENGTH_LONG).show();
            }
        } else {
            Toast.makeText(getApplicationContext(), "Cannot send messages to UI", Toast.LENGTH_LONG).show();
        }
    }

    /**
     * Sends message to client after cycle of PID list
     */
    private void saveToFileMessage() {
        if(client != null) {
            Message msg = Message.obtain(null, TcpClientService.MSG_WRITE_LIST_TO_FILE);
            try {
                client.send(msg);
            } catch (RemoteException e) {
                e.printStackTrace();
                Toast.makeText(getApplicationContext(), "Cannot send messages to UI", Toast.LENGTH_LONG).show();
            }
        } else {
            Toast.makeText(getApplicationContext(), "Cannot send messages to UI", Toast.LENGTH_LONG).show();
        }
    }

    /**
     * Sends message to client to unbind and destroy self
     * @param reason
     */
    private void requestToStopService(String reason) {

        if(client != null) {
            Message msg = Message.obtain(null, TcpClientService.MSG_STOP_REQUESTS, reason);
            try {
                client.send(msg);
            } catch (RemoteException e) {
                e.printStackTrace();
                Toast.makeText(getApplicationContext(), "Cannot send messages to UI", Toast.LENGTH_LONG).show();
            }
        } else {
            Toast.makeText(getApplicationContext(), "Cannot send messages to UI", Toast.LENGTH_LONG).show();
        }
    }

    /**
     * works as backround task of async thread
     * @param outMsg
     * @return
     */
    private String runTcpClient(String outMsg) {
        try {
            InetAddress ip = InetAddress.getByName(SERVER_IP_ADDRESS);
            this.socket = new Socket();
            this.socket.connect(new InetSocketAddress(ip, TCP_SERVER_PORT), SOCKET_CONN_TIMEOUT);
            BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            BufferedWriter out = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));
            //send output msg
            out.write(outMsg + "\r");
            out.flush();
            Log.i("TcpClient", "sent: " + outMsg);
            //accept server response
            int character = 0;
            String inMsg = "";
            int bytes = 0; // if end character not found  && if task is not cancelled
            while(((character = in.read()) != 62) && (bytes < 30) && (!tcpTask.isCancelled())) { // EOL == '>'
                inMsg = inMsg + Character.toString((char) character);
                bytes++;
            }
            Log.i("TcpClient", "received: " + inMsg);
            socket.close();

            return inMsg;

        } catch (Exception e) {
            e.printStackTrace();
            requestToStopService(e.getLocalizedMessage());
            tcpTask.cancel(true);
            return e.getLocalizedMessage();
        }
    }

    private void requestLogic(String response) {

        sendStringToClient(response);

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
            tcpTask = new RequestToSocketTask().execute(pid);
        } else { //switch boolean to write down list on next response
            saveToFileMessage();
            pid = pids.getNextPid();
            tcpTask = new RequestToSocketTask().execute(pid);
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
