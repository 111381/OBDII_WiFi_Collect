package com.bitmaster.obdii_wifi_collect.obdwifi;

import android.app.ListActivity;
import android.content.Context;
import android.content.Intent;
import android.location.Location;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Handler;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import com.bitmaster.obdii_wifi_collect.obdwifi.googleapis.MakeRoute;
import com.bitmaster.obdii_wifi_collect.obdwifi.googleapis.RouteStep;
import com.bitmaster.obdii_wifi_collect.obdwifi.io.CanMonitorService;
import com.bitmaster.obdii_wifi_collect.obdwifi.io.TcpIntentService;
import com.bitmaster.obdii_wifi_collect.obdwifi.io.WriteDownService;
import com.bitmaster.obdii_wifi_collect.obdwifi.loc.GpsLocation;
import com.bitmaster.obdii_wifi_collect.obdwifi.obd2.FilterLogic;
import com.bitmaster.obdii_wifi_collect.obdwifi.prediction.CalculateMaps;
import com.bitmaster.obdii_wifi_collect.obdwifi.prediction.MapCanValues;
import com.bitmaster.obdii_wifi_collect.obdwifi.obd2.SupportedPids;
import com.bitmaster.obdii_wifi_collect.obdwifi.prediction.SaveMapsService;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;


public class MainActivity extends ListActivity implements ObdResultReceiver.Receiver {

    private ArrayAdapter<String> adapter = null;
    private List<String> wordList = null;
    private TextView textView = null;
    private GpsLocation gpsLocation = null;

    public ObdResultReceiver mReceiver;
    private SupportedPids pids = null;
    //private MakeRoute route = null;
    private boolean requestsEnabled = false;
    private boolean canRequests = true; //first priority block all futher OBDII responses
    private boolean stopCanMonitoring = false;
    private boolean setCanFilter = false;

    private WifiManager wifi = null;
    private WifiManager.WifiLock wifiLock = null;

    public static int TCP_SERVER_PORT = 35000;
    public static String SERVER_IP_ADDRESS = "192.168.0.10";
    //public static String SERVER_IP_ADDRESS = "192.168.1.5";
    public static int SOCKET_CONN_TIMEOUT = 30*1000;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        this.mapsToFile(false);//read

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
        //IP-192.168.1.10 - channel:6

        //SSID V-LINK
        //BSSID-AC:CF:23:21:E2:59
        //IP-192.168.1.150 - channel:1
        this.wifi = (WifiManager)this.getSystemService(Context.WIFI_SERVICE);
        this.wifiLock = wifi.createWifiLock(WifiManager.WIFI_MODE_FULL_HIGH_PERF, "HighPerf wifi lock");
        wifiLock.acquire();
        WifiInfo inf = wifi.getConnectionInfo();
        if(inf.getSSID().equalsIgnoreCase("V-LINK")){
            SERVER_IP_ADDRESS = "192.168.0.150";
        }
        this.textView = (TextView) findViewById(R.id.text_view);
        this.textView.setText(inf.getSSID());

        //this.route = new MakeRoute("59.3913274,24.6640021", "riida");
    }

    @Override
    public void onReceiveResult(int resultCode, Bundle resultData) {

        String response = resultData.getString("ServiceTag");
        if(this.canRequests) {
            this.onCanMessageReceive(response);
            if(this.stopCanMonitoring){
                return;
            }
            if(this.setCanFilter){
                this.setCanFilter = false;
                this.requestCanMonService("ATMA");
            } else {
                this.nextCanRequest();
            }
            return;
        }
        this.wordList.add(response);
        this.adapter.notifyDataSetChanged();
        //Stops requests by existing fault, restarts them by timer task
        FilterLogic filter = new FilterLogic(this);
        if(filter.isResponseFaulty(response)){
            this.requestsEnabled = false;
        }

        if(this.requestsEnabled) {
            this.nextRequestFromList();
        }
    }

    private void onCanMessageReceive(String received) {

        String response = received.replace("\n", "\r");
        int fromIndex = 0;
        int crIndex;
        //String row = "";
        while(true) {
            crIndex = response.indexOf(13, fromIndex);
            if(crIndex == -1) {
                //row = row + response.substring(fromIndex) + " <- No <CR>";
                return;
            }
            String message = response.substring(fromIndex, crIndex);//get single message
            //Log.i("CAN_Message", message);
            if(MapCanValues.decodeCanMessage(message)){ //if correct message

                MapCanValues.setMapValues();
                break;
            }
            fromIndex = crIndex + 1; //next occurrence
        }
        Log.i("Row", MapCanValues.row);
        this.wordList.add(MapCanValues.row);
        MapCanValues.row = "";
        this.adapter.notifyDataSetChanged();
    }

    private void nextCanRequest() {

        String pid;
        // call recursively:
        // First do init requests since response 'null'
        if(!pids.isCanInitDone()) {
            pid = pids.getNextCanInit();
            if(pid == null) {
                pids.setCanInitDone(true);
            }
        } else {
            pid = pids.getNextCan();
            this.setCanFilter = true; //request for can filtering
        }
        // next request with next PID
        if(pid == null) {
            saveToFile();
            pid = pids.getNextCan();
            this.setCanFilter = true; //request for can filtering
        }
        this.requestToTcpService(pid);
    }

    public void nextRequestFromList() {

        runOnUiThread(new Runnable(){
            @Override
            public void run() {
                requestsEnabled = true;
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
                    saveToFile();
                    pid = pids.getNextPid();
                }
                requestToTcpService(pid);
            }
        });
    }

    public void startRequests(final String request) {

        runOnUiThread(new Runnable(){
            @Override
            public void run() {
                clearList();
                requestsEnabled = true;
                //Create fresh queue of PID and start requests with reset
                pids = new SupportedPids();
                if(gpsLocation == null){
                    return;
                }
                gpsLocation.requestLocation();
                requestToTcpService(request);
            }
        });
    }

    private void requestToTcpService(String request) {

        Intent mServiceIntent = new Intent(this, TcpIntentService.class);
        mServiceIntent.putExtra("com.bitmaster.obdii_wifi_collect.obdwifi.io.Request", request);
        mServiceIntent.putExtra("com.bitmaster.obdii_wifi_collect.obdwifi.io.receiverTag", mReceiver);
        this.startService(mServiceIntent);
        Log.i("requestTcp", request);
    }

    private void requestCanMonService(String request) {

        Intent mServiceIntent = new Intent(this, CanMonitorService.class);
        mServiceIntent.putExtra("com.bitmaster.obdii_wifi_collect.obdwifi.io.Request", request);
        mServiceIntent.putExtra("com.bitmaster.obdii_wifi_collect.obdwifi.io.receiverTag", mReceiver);
        this.startService(mServiceIntent);
        Log.i("requestCanMon", request);
    }

    private void mapsToFile(boolean write) {
        Intent mServiceIntent = new Intent(this, SaveMapsService.class);//write = true, read = false
        mServiceIntent.putExtra("com.bitmaster.obdii_wifi_collect.obdwifi.prediction.Write", write);
        this.startService(mServiceIntent);
    }

    public void saveToFile() {

        Location loc = this.gpsLocation.getLocation();
        String latitude = "0";
        String longitude = "0";
        String altitude = "0";
        if(loc != null){
            latitude = Double.toString(loc.getLatitude());
            longitude = Double.toString(loc.getLongitude());
            altitude = Double.toString(loc.getAltitude());
        }
        String line = "";
        Iterator<String> it = this.wordList.iterator();
        while(it.hasNext()) {
            line += (it.next()).replace("\n", ",").replace("\r", ",");
        }
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        String currentDateandTime = sdf.format(new Date());
        String csvLine = currentDateandTime + "," + line + ","
                + latitude + "," + longitude + "," + altitude + ","
                + Double.toString(MapCanValues.acceleration) + ";\n";
                //+ Double.toString(CalculateMaps.calculatePowerAtSpeed(MapCanValues.speed)) + ","
                //+ Double.toString(CalculateMaps.calculateRangeAtSpeed(MapCanValues.speed)) + ",";

        Intent mServiceIntent = new Intent(this, WriteDownService.class);
        mServiceIntent.putExtra("com.bitmaster.obdii_wifi_collect.obdwifi.io.csvLine", csvLine);
        this.startService(mServiceIntent);

        this.gpsLocation.requestLocation();

        this.clearList();
    }

    private void clearList() {

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


   @Override
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
        // Handle item selection
        switch (item.getItemId()) {
            case R.id.action_startCan:
                this.canRequests = true;
                this.stopCanMonitoring = false;
                this.setCanFilter = false;
                this.pids = new SupportedPids();
                this.gpsLocation.requestLocation();
                this.nextCanRequest();
                this.textView.setText("CAN monitoring in progress");
                return true;
            case R.id.action_stopCan:
                this.stopCanMonitoring = true;
                this.textView.setText("Saving Map ...");
                this.mapsToFile(true);//write
                this.textView.setText("CAN monitoring stopped");
                return true;
            case R.id.action_startObd:
                this.canRequests = false;
                this.startRequests("ATWS");//warm reset, without LED test
                this.textView.setText("OBDII request sequence");
                return true;
            case R.id.action_stopObd:
                this.requestsEnabled = false;
                this.textView.setText("OBDII requests disabled");
                return true;
            case R.id.action_save:
                this.textView.setText("Saving to file");
                this.saveToFile();
                return true;
            case R.id.action_google:
               /* this.textView.setText("Calculating route to destination");
                List<RouteStep> routeSteps = this.route.getSteps();
                Iterator<RouteStep> it = routeSteps.iterator();
                while(it.hasNext()){

                    //TODO: values can be null
                    RouteStep s = it.next();
                    this.wordList.add(s.getStartLat());
                    this.wordList.add(s.getStartLng());
                    this.wordList.add(s.getEndLat());
                    this.wordList.add(s.getEndLng());
                    this.wordList.add(s.getDuration());
                    this.wordList.add(s.getInstructions());
                    this.wordList.add(s.getDistance());
                }
                this.adapter.notifyDataSetChanged();*/
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }
}
