package com.bitmaster.obdii_wifi_collect.obdwifi.io;

import android.app.IntentService;
import android.content.Intent;
import android.os.Bundle;
import android.os.ResultReceiver;

import com.bitmaster.obdii_wifi_collect.obdwifi.obd2.FilterLogic;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;

/**
 * Created by uhh on 3/31/14.
 */
public class TcpIntentService extends IntentService {

    private static final int TCP_SERVER_PORT = 35000;
    private static final String SERVER_IP_ADDRESS = "192.168.0.10";
    private static final int SOCKET_CONN_TIMEOUT = 30*1000;

    /**
     * A constructor is required, and must call the super IntentService(String)
     * constructor with a name for the worker thread.
     */
    public TcpIntentService() {
        super("TcpIntentService");
    }

    /**
     * The IntentService calls this method from the default worker thread with
     * the intent that started the service. When this method returns, IntentService
     * stops the service, as appropriate.
     */
    @Override
    protected void onHandleIntent(Intent intent) {

        synchronized (this) {
            ResultReceiver rec = intent.getParcelableExtra("com.bitmaster.obdii_wifi_collect.obdwifi.io.receiverTag");
            String outMsg = intent.getStringExtra("com.bitmaster.obdii_wifi_collect.obdwifi.io.Request");
            Bundle b= new Bundle();
            Socket socket = new Socket();
            try {
                InetAddress ip = InetAddress.getByName(SERVER_IP_ADDRESS);
                socket.connect(new InetSocketAddress(ip, TCP_SERVER_PORT), SOCKET_CONN_TIMEOUT);
                BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                BufferedWriter out = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));
                //send output msg
                out.write(outMsg + "\r");
                out.flush();
                //accept server response
                int character = 0;
                String inMsg = "";
                int bytes = 0; // if end character not found
                //TODO: timeout for read lock
                while (((character = in.read()) != 62) && (bytes < 30)) { // EOL == '>'
                    inMsg = inMsg + Character.toString((char) character);
                    bytes++;
                }
                socket.close();
                b.putString("ServiceTag", inMsg);
                rec.send(0, b);
            } catch (Exception e) {
                b.putString("ServiceTag", FilterLogic.TCP_ERROR);
                rec.send(0, b);
            }
        }
        /*// Normally we would do some work here, like download a file.
        // For our sample, we just sleep for 5 seconds.
        long endTime = System.currentTimeMillis() + 5*1000;
        while (System.currentTimeMillis() < endTime) {
            synchronized (this) {
                try {
                    wait(endTime - System.currentTimeMillis());
                } catch (Exception e) {
                }
            }
        }*/
    }
}
