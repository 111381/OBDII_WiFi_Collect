package com.bitmaster.obdii_wifi_collect.obdwifi.io;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;

import android.app.Service;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Binder;
import android.os.IBinder;
import android.util.Log;

import com.bitmaster.obdii_wifi_collect.obdwifi.MainActivity;

/**
 * Created by renet on 3/12/14.
 */
public class TcpClientService extends Service {

    //public static final int TCP_SERVER_PORT = 35000;
    //public static final String SERVER_IP_ADDRESS = "192.168.0.10";
    private static final int TCP_SERVER_PORT = 80;
    private static final String SERVER_IP_ADDRESS = "192.168.50.2";//proekspert.ee

    private final IBinder mBinder = new MyBinder();
    private ArrayList<String> list = new ArrayList<String>();

    //This allows you to communicate directly with the service.
    @Override
    public IBinder onBind(Intent arg0) {

        return mBinder;
    }
    @Override
    public void onCreate() {

        //runTcpClient();
        //this.stopSelf();
        list.add("onCreate");
    }
    // can be called several times
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        list.add("ATZ");
        list.add("0100");
        new DownloadWebpageTask().execute();

        return Service.START_NOT_STICKY;//Service is restarted if it gets terminated.
    }

    public class MyBinder extends Binder {

        public TcpClientService getService() {

            return TcpClientService.this;
        }
    }

    public List<String> getWordList() {

        return list;
    }

    // Uses AsyncTask to create a task away from the main UI thread. This task takes a
    // URL string and uses it to create an HttpUrlConnection. Once the connection
    // has been established, the AsyncTask downloads the contents of the webpage as
    // an InputStream. Finally, the InputStream is converted into a string, which is
    // displayed in the UI by the AsyncTask's onPostExecute method.
    private class DownloadWebpageTask extends AsyncTask<String, Void, String> {
        @Override
        protected String doInBackground(String... urls) {

            return TcpClientService.runTcpClient();
        }
        // onPostExecute displays the results of the AsyncTask.
        @Override
        protected void onPostExecute(String result) {
            list.add(result);
        }
    }

    private static String runTcpClient() {
        try {
            InetAddress ip = InetAddress.getByName(SERVER_IP_ADDRESS);
            Socket s = new Socket(ip, TCP_SERVER_PORT);
            BufferedReader in = new BufferedReader(new InputStreamReader(s.getInputStream()));
            BufferedWriter out = new BufferedWriter(new OutputStreamWriter(s.getOutputStream()));
            //send output msg
            String outMsg = "TCP connecting to " + TCP_SERVER_PORT + System.getProperty("line.separator");
            out.write(outMsg);
            out.flush();
            Log.i("TcpClient", "sent: " + outMsg);
            //accept server response
            String inMsg = in.readLine() + System.getProperty("line.separator");
            Log.i("TcpClient", "received: " + inMsg);
            //close connection
            s.close();

            return inMsg;

            //A service can terminate itself by calling the stopSelf() method.
            //this.stopSelf();
        } catch (UnknownHostException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return "failure";
    }
}
