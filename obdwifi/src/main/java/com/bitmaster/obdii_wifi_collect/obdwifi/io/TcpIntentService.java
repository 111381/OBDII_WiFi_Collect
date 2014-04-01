package com.bitmaster.obdii_wifi_collect.obdwifi.io;

import android.app.IntentService;
import android.content.Intent;
import android.util.Log;

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

    public static final int TCP_SERVER_PORT = 35000;
    public static final String SERVER_IP_ADDRESS = "192.168.0.10";
    public static final int SOCKET_CONN_TIMEOUT = 5000;

    private Socket socket = null;
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
        // Normally we would do some work here, like download a file.
        // For our sample, we just sleep for 5 seconds.
        long endTime = System.currentTimeMillis() + 5*1000;
        while (System.currentTimeMillis() < endTime) {
            synchronized (this) {
                try {
                    wait(endTime - System.currentTimeMillis());
                } catch (Exception e) {
                }
            }
        }
        String outMsg = " ";
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
            while(((character = in.read()) != 62) && (bytes < 30)) { // EOL == '>'
                inMsg = inMsg + Character.toString((char) character);
                bytes++;
            }
            Log.i("TcpClient", "received: " + inMsg);
            socket.close();

            return ;

        } catch (Exception e) {
            e.printStackTrace();

            return ;
        }
    }
}
