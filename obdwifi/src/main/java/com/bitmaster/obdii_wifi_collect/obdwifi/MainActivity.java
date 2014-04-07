package com.bitmaster.obdii_wifi_collect.obdwifi;

import android.app.ListActivity;
import android.content.Context;
import android.content.Intent;
import android.location.Location;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Handler;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;

import com.bitmaster.obdii_wifi_collect.obdwifi.io.TcpIntentService;
import com.bitmaster.obdii_wifi_collect.obdwifi.io.WriteDownService;
import com.bitmaster.obdii_wifi_collect.obdwifi.loc.GpsLocation;
import com.bitmaster.obdii_wifi_collect.obdwifi.obd2.FilterLogic;
import com.bitmaster.obdii_wifi_collect.obdwifi.obd2.SupportedPids;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;


public class MainActivity extends ListActivity implements ObdResultReceiver.Receiver {

    private ArrayAdapter<String> adapter = null;
    private List<String> wordList = null;
    private GpsLocation gpsLocation = null;

    public ObdResultReceiver mReceiver;
    private SupportedPids pids = null;
    private boolean requestsEnabled = false;

    private WifiManager wifi = null;
    private WifiManager.WifiLock wifiLock = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        this.wordList = new ArrayList<String>();
        this.adapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, android.R.id.text1, wordList);
        this.setListAdapter(adapter);

        this.gpsLocation = new GpsLocation(this);
        this.gpsLocation.requestLocation();

        this.mReceiver = new ObdResultReceiver(new Handler());
        this.mReceiver.setReceiver(this);

        //SSID-WiFi_OBDII
        //BSSID-00:0E:C6:9F:0D:24
        //id-4
        this.wifi = (WifiManager)this.getSystemService(Context.WIFI_SERVICE);
        this.wifiLock = wifi.createWifiLock(WifiManager.WIFI_MODE_FULL_HIGH_PERF, "HighPerf wifi lock");
        wifiLock.acquire();
        WifiInfo inf = wifi.getConnectionInfo();
        if(!inf.getSSID().equalsIgnoreCase("WiFi_OBDII")){
            wifi.enableNetwork(4, true);                   //ID!!!!!
        }
    }

    public void onClick(View view) {

        if(this.requestsEnabled) {
            this.requestsEnabled = false;
            return;
        }
        this.startRequests();
    }

    @Override
    public void onReceiveResult(int resultCode, Bundle resultData) {

        if(!wifi.isWifiEnabled()){ //TODO reenabling
            this.wordList.add("Wifi is not enabled");
            this.requestsEnabled = false;
        }
        String response = resultData.getString("ServiceTag");
        this.wordList.add(response);
        this.adapter.notifyDataSetChanged();
        //Stops requests by existing fault, restarts them by timer task
        FilterLogic filter = new FilterLogic(this);
        if(filter.isResponseFaulty(response)){
            this.requestsEnabled = false;
        }

        if(this.requestsEnabled) {
            this.requestLogic();
        }
    }

    private void requestLogic() {

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
        if(pid == null) {
            this.saveToFile();
            pid = pids.getNextPid();
        }
        this.requestToTcpService(pid);
    }

    private void requestToTcpService(String request) {

        Intent mServiceIntent = new Intent(this, TcpIntentService.class);
        mServiceIntent.putExtra("com.bitmaster.obdii_wifi_collect.obdwifi.io.Request", request);
        mServiceIntent.putExtra("com.bitmaster.obdii_wifi_collect.obdwifi.io.receiverTag", mReceiver);
        this.startService(mServiceIntent);
    }

    public void startRequests() {

        runOnUiThread(new Runnable(){
            @Override
            public void run() {
                requestsEnabled = true;
                //Create fresh queue of PID and start requests with reset
                pids = new SupportedPids();
                gpsLocation.requestLocation();
                requestToTcpService("ATZ");
            }
        });
    }

    public void saveToFile() {

        Location loc = this.gpsLocation.getLocation();
        String latitude = "0";
        String longitude = "0";
        String speed = "0";
        if(loc != null){
            latitude = Double.toString(loc.getLatitude());
            longitude = Double.toString(loc.getLongitude());
            speed = Double.toString(loc.getSpeed());
        }
        String line = "";
        Iterator<String> it = this.wordList.iterator();
        while(it.hasNext()) {
            line += (it.next()).replace("\n", ",").replace("\r", ",");
        }
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        String currentDateandTime = sdf.format(new Date());
        String csvLine = currentDateandTime + "," + line + "," + latitude + "," + longitude + "," + speed + ";\n";

        Intent mServiceIntent = new Intent(this, WriteDownService.class);
        mServiceIntent.putExtra("com.bitmaster.obdii_wifi_collect.obdwifi.io.csvLine", csvLine);
        this.startService(mServiceIntent);

        this.gpsLocation.requestLocation();

        this.clearList();
    }

    public void clearList() {

        this.wordList.clear();
        this.adapter.notifyDataSetChanged();
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        this.wifiLock.release();
        this.gpsLocation = null;
    }


   /* @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        if (id == R.id.action_settings) {
            return true;
        }
        return super.onOptionsItemSelected(item);
    }*/
}
