package com.bitmaster.obdii_wifi_collect.obdwifi.receivers;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.bitmaster.obdii_wifi_collect.obdwifi.io.TcpClientService;

/**
 * Created by renet on 3/13/14.
 */
public class StartServiceReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {

        Intent service = new Intent(context, TcpClientService.class);
        context.startService(service);
    }
}
