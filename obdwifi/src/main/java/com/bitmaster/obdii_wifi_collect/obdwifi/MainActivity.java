package com.bitmaster.obdii_wifi_collect.obdwifi;

import android.app.ListActivity;
import android.content.Context;
import android.content.Intent;
import android.location.Location;
import android.net.ConnectivityManager;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Handler;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

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
import com.bitmaster.obdii_wifi_collect.obdwifi.weather.RequestTemperature;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;


public class MainActivity extends ListActivity implements ObdResultReceiver.Receiver {

    private ArrayAdapter<String> adapter = null;
    private List<String> wordList = null;
    private TextView textView = null;
    private EditText edittext= null;
    private Button startButton = null;
    private Button stopButton = null;
    private GpsLocation gpsLocation = null;
    public ObdResultReceiver mReceiver;
    private SupportedPids pids = null;
    private MakeRoute route = null;
    private boolean requestsEnabled = false;
    private boolean canRequests = true; //first priority block all futher OBDII responses
    private boolean stopCanMonitoring = false;
    private boolean setCanFilter = false;

    private WifiManager.WifiLock wifiLock = null;

    public static int TCP_SERVER_PORT = 35000;
    public static String SERVER_IP_ADDRESS = "192.168.0.10";
    //public static String SERVER_IP_ADDRESS = "192.168.1.5";
    public static int SOCKET_CONN_TIMEOUT = 10*1000;

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

        this.textView = (TextView) findViewById(R.id.text_view);
        this.addGoogleServiceListener(this);
        this.setupButtons();


        //SSID-WiFi_OBDII
        //BSSID-00:0E:C6:9F:0D:24
        //id-4
        //IP-192.168.1.10 - channel:6

        //SSID V-LINK
        //BSSID-AC:CF:23:21:E2:59
        //IP-192.168.1.150 - channel:1

        /*WifiManager wifiManager = (WifiManager) this.getSystemService(Context.WIFI_SERVICE);
        wifiManager.setWifiEnabled(false);
        ConnectivityManager connManager = (ConnectivityManager) this.getSystemService(Context.CONNECTIVITY_SERVICE);
        connManager.setNetworkPreference(ConnectivityManager.TYPE_MOBILE);
        this.textView.setText(connManager.getActiveNetworkInfo().getTypeName());*/

        //Request temperature an set static consumption to map
        new RequestTemperature(this).execute(
                "http://www.ilmateenistus.ee/ilma_andmed/xml/observations.php"
        );

        WifiManager wifiManager = (WifiManager)this.getSystemService(Context.WIFI_SERVICE);
        this.wifiLock = wifiManager.createWifiLock(WifiManager.WIFI_MODE_FULL_HIGH_PERF, "HighPerf wifi lock");
        WifiInfo inf = wifiManager.getConnectionInfo();
        if(inf.getSSID().equalsIgnoreCase("V-LINK")){
            SERVER_IP_ADDRESS = "192.168.0.150";
        }
        this.textView.setText(inf.getSSID());
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
                + Double.toString(MapCanValues.acceleration) + ",";
                //+ Double.toString(CalculateMaps.calculatePowerAtSpeed(MapCanValues.speed)) + ","
                //+ Double.toString(CalculateMaps.calculateRangeAtSpeed(MapCanValues.speed)) + ",";
        /*String sectionSpeedAndPower = MapCanValues.realSpeedAndPowerInSteps(loc);
        if(sectionSpeedAndPower != null) {
            csvLine = csvLine + sectionSpeedAndPower + ",";
        }*/
        csvLine = csvLine + ";\n";

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
        if(wifiLock.isHeld())
            this.wifiLock.release();
        this.gpsLocation = null;
    }

    private void addGoogleServiceListener(final MainActivity activity) {
        // get edittext component
        edittext = (EditText) findViewById(R.id.destination);
        // add a keylistener to monitor the keaybord avitvity...
        edittext.setOnKeyListener(new View.OnKeyListener() {
            public boolean onKey(View v, int keyCode, KeyEvent event) {
                // if the users pressed a button and that button was "Enter"
                if ((event.getAction() == KeyEvent.ACTION_DOWN) && (keyCode == KeyEvent.KEYCODE_ENTER)) {
                    //Request destination routes
                    String origin = "59.3913274,24.6640021";
                    Location loc = gpsLocation.getLocation();
                    if(loc != null) {
                        origin = Double.toString(loc.getLatitude()) + "," + Double.toString(loc.getLongitude());
                    }
                    route = new MakeRoute(activity, origin, edittext.getText().toString());
                    textView.setText("Google Service Request: " + edittext.getText());
                    clearList();
                    return true;
                }
                return false;
            }
        });
    }

    private void setupButtons() {

        this.startButton = (Button) findViewById(R.id.button_start_can);
        startButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                wifiLock.acquire();
                mapsToFile(false);//read
                canRequests = true;
                stopCanMonitoring = false;
                setCanFilter = false;
                pids = new SupportedPids();
                gpsLocation.requestLocation();
                nextCanRequest();
                textView.setText("CAN monitoring in progress");
                edittext.setVisibility(View.GONE);
                startButton.setVisibility(View.GONE);
                stopButton.setVisibility(View.VISIBLE);
            }
        });
        this.stopButton = (Button) findViewById(R.id.button_stop_can);
        stopButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                wifiLock.release();
                stopCanMonitoring = true;
                textView.setText("Saving Map ...");
                mapsToFile(true);//write
                textView.setText("CAN monitoring stopped");
                //hide buttons and write maps to file
                startButton.setVisibility(View.GONE);
                stopButton.setVisibility(View.GONE);
                edittext.setVisibility(View.GONE);
            }
        });
    }

    public void googleServiceRequestCompleted(final String status) {

        runOnUiThread(new Runnable(){
            @Override
            public void run() {
                textView.setText("Google Service Request Status: " + status +
                        ". Temperature: " + Integer.toString(MapCanValues.temperature));
                if(status.equalsIgnoreCase("OK")) {
                    MapCanValues.routeStepList = route.getRouteSteps();
                    wordList.addAll(route.calculateRouteStepsAsStringList());
                    adapter.notifyDataSetChanged();
                }
            }
        });
    }

    public void temperatureRequestCompleted(final String temperature) {

        runOnUiThread(new Runnable(){
            @Override
            public void run() {
                textView.setText("Temperature: " + temperature);
                float temp = Float.parseFloat(temperature);

                MapCanValues.temperature = Math.round(temp);

                startButton.setVisibility(View.VISIBLE);
            }
        });
    }

/*    @Override
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
                this.wifiLock.acquire();
                this.canRequests = true;
                this.stopCanMonitoring = false;
                this.setCanFilter = false;
                this.pids = new SupportedPids();
                this.gpsLocation.requestLocation();
                this.nextCanRequest();
                this.textView.setText("CAN monitoring in progress");
                return true;
            case R.id.action_stopCan:
                this.wifiLock.release();
                this.stopCanMonitoring = true;
                this.textView.setText("Saving Map ...");
                this.mapsToFile(true);//write
                this.textView.setText("CAN monitoring stopped");
                return true;
            case R.id.action_startObd:
                this.wifiLock.acquire();
                this.canRequests = false;
                this.startRequests("ATWS");//warm reset, without LED test
                this.textView.setText("OBDII request sequence");
                return true;
            case R.id.action_stopObd:
                this.wifiLock.release();
                this.requestsEnabled = false;
                this.textView.setText("OBDII requests disabled");
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }
*/
}
