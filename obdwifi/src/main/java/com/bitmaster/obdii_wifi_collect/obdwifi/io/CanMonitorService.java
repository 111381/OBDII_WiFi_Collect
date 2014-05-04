package com.bitmaster.obdii_wifi_collect.obdwifi.io;

import android.app.IntentService;
import android.content.Intent;
import android.os.Bundle;
import android.os.ResultReceiver;
import android.util.Log;

import com.bitmaster.obdii_wifi_collect.obdwifi.MainActivity;
import com.bitmaster.obdii_wifi_collect.obdwifi.obd2.FilterLogic;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;

/**
 * Created by renet on 4/28/14.
 */
public class CanMonitorService extends IntentService {

    public CanMonitorService() {
        super("CanMonitorService");
    }

    @Override
    protected void onHandleIntent(Intent intent) {

        synchronized (this) {
            ResultReceiver rec = intent.getParcelableExtra("com.bitmaster.obdii_wifi_collect.obdwifi.io.receiverTag");
            String outMsg = intent.getStringExtra("com.bitmaster.obdii_wifi_collect.obdwifi.io.Request");
            Bundle b = new Bundle();
            Socket socket = new Socket();
            try {
                InetAddress ip = InetAddress.getByName(MainActivity.SERVER_IP_ADDRESS);
                socket.connect(new InetSocketAddress(ip, MainActivity.TCP_SERVER_PORT), MainActivity.SOCKET_CONN_TIMEOUT);
                BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                BufferedWriter out = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));
                //send output msg
                out.write(outMsg + "\r");
                out.flush();
                //accept server response
                int character = 0;
                String inMsg = "";
                long endTime = System.currentTimeMillis() + 100;

                while (((character = in.read()) != 62)) { // EOL == '>'
                    inMsg = inMsg + Character.toString((char) character);
                    //Log.i("Monitor1", Character.toString((char) character));
                    if(System.currentTimeMillis() > endTime){
                        //The monitoring mode can be stopped by sending a single RS232 character to the ELM327.
                        out.write(" ");
                        out.flush();
                        break;
                    }
                }
                //The IC will always finish a task that is in progress (printing a line, for example) before
                //printing ‘STOPPED’ and returning to wait for your input, so it is best to wait for the prompt character (‘>’)
                while (((character = in.read()) != 62)) { // EOL == '>'
                    inMsg = inMsg + Character.toString((char) character);
                    //Log.i("Monitor2", Character.toString((char) character));
                }

                socket.close();
                b.putString("ServiceTag", inMsg);
                rec.send(0, b);
            } catch (Exception e) {
                b.putString("ServiceTag", FilterLogic.TCP_ERROR);
                rec.send(0, b);
                Log.i("Monitor", FilterLogic.TCP_ERROR);
            }
        }
    }
}
