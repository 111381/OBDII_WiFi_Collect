package com.bitmaster.obdii_wifi_collect.obdwifi;

import android.app.ListActivity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.location.Location;
import android.os.Handler;
import android.os.IBinder;
import android.os.Bundle;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Toast;

import com.bitmaster.obdii_wifi_collect.obdwifi.io.TcpClientService;
import com.bitmaster.obdii_wifi_collect.obdwifi.io.TcpIntentService;
import com.bitmaster.obdii_wifi_collect.obdwifi.io.WriteDownService;
import com.bitmaster.obdii_wifi_collect.obdwifi.loc.GpsLocation;
import com.bitmaster.obdii_wifi_collect.obdwifi.obd2.SupportedPids;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;


public class MainActivity extends ListActivity implements ObdResultReceiver.Receiver {

    //private TcpClientService service = null;
    private ArrayAdapter<String> adapter = null;
    private List<String> wordList = null;
    private boolean mIsBound = false;
    /** Messenger for communicating with service. */
    private Messenger mService = null;
    private GpsLocation gpsLocation = null;
    private static final int RESTART_CYCLE = 10;//restart dongle after writing lines
    private static final long RESTART_PERIOD = 10*1000;//restart service after destroy self by msg
    private static int restartCount = 0;
    private Timer restartTimer = null;

    public ObdResultReceiver mReceiver;
    private SupportedPids pids = null;
    private boolean requestsEnabled = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        this.wordList = new ArrayList<String>();
        this.adapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, android.R.id.text1, wordList);
        this.setListAdapter(adapter);

        this.gpsLocation = new GpsLocation(this);
        this.restartTimer = new Timer();

        this.mReceiver = new ObdResultReceiver(new Handler());
        this.mReceiver.setReceiver(this);
    }

    @Override
    public void onReceiveResult(int resultCode, Bundle resultData) {

        this.wordList.add(resultData.getString("ServiceTag"));
        this.adapter.notifyDataSetChanged();

        if(this.requestsEnabled) {
            this.requestLogic();
        }
    }
    public void onClick(View view) {

        if(this.requestsEnabled) {
            this.requestsEnabled = false;
            return;
        }
        this.requestsEnabled = true;
        //Create fresh queue of PID and start requests with reset
        this.pids = new SupportedPids();
        this.gpsLocation.requestLocation();

        Intent mServiceIntent = new Intent(this, TcpIntentService.class);
        mServiceIntent.putExtra("com.bitmaster.obdii_wifi_collect.obdwifi.io.Request", "ATZ");
        mServiceIntent.putExtra("com.bitmaster.obdii_wifi_collect.obdwifi.io.receiverTag", mReceiver);
        this.startService(mServiceIntent);

        //bindToOBDII(view);
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
        Intent mServiceIntent = new Intent(this, TcpIntentService.class);
        mServiceIntent.putExtra("com.bitmaster.obdii_wifi_collect.obdwifi.io.Request", pid);
        mServiceIntent.putExtra("com.bitmaster.obdii_wifi_collect.obdwifi.io.receiverTag", mReceiver);
        this.startService(mServiceIntent);
    }

    private void saveToFile() {

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
        //clear screen
        this.wordList.clear();
        this.adapter.notifyDataSetChanged();
    }




    @Override
    protected void onResume() {
        super.onResume();
        //doBindService();
    }

    @Override
    protected void onPause() {
        super.onPause();
        //doUnbindService();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        this.doUnbindService();
    }

    public void bindToOBDII(View v) {

        if(this.mIsBound) {
            this.doUnbindService();
        } else {
            this.doBindService();
        }
    }
    /**
     * Class for interacting with the main interface of the service.
     */
    private ServiceConnection mConnection = new ServiceConnection() {

        public void onServiceConnected(ComponentName className, IBinder binder) {
            // This is called when the connection with the service has been
            // established, giving us the service object we can use to
            // interact with the service.  We are communicating with our
            // service through an IDL interface, so get a client-side
            // representation of that from the raw service object.
            mService = new Messenger(binder);
            Toast.makeText(MainActivity.this, "TcpServiceConnected", Toast.LENGTH_SHORT).show();
            //Create fresh queue of PID and start requests with reset
            Message msg = Message.obtain(null, TcpClientService.MSG_START_REQUESTS);
            //add to first message client side Messenger which has incoming handler
            msg.replyTo = mMessenger;
            try {
                mService.send(msg);
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        }

        public void onServiceDisconnected(ComponentName className) {
            // This is called when the connection with the service has been
            // unexpectedly disconnected -- that is, its process crashed.
            // Because it is running in our same process, we should never
            // see this happen.
            Toast.makeText(MainActivity.this, "TcpServiceDisconnected", Toast.LENGTH_SHORT).show();
            mService = null;
        }
    };

    void doBindService() {
        // Establish a connection with the service.  We use an explicit
        // class name because we want a specific service implementation that
        // we know will be running in our own process (and thus won't be
        // supporting component replacement by other applications).
        bindService(new Intent(MainActivity.this, TcpClientService.class), mConnection, Context.BIND_AUTO_CREATE);
        Toast.makeText(MainActivity.this, "BindingService", Toast.LENGTH_SHORT).show();
        mIsBound = true;
        //clear screen
        wordList.clear();
        adapter.notifyDataSetChanged();
    }

    void doUnbindService() {
        if (mIsBound) {
            // Detach our existing connection.
            unbindService(mConnection);
            Toast.makeText(MainActivity.this, "Unbinding Service", Toast.LENGTH_SHORT).show();
            mIsBound = false;
        }
    }

    // The Handler that gets information back from the Service
    class IncomingHandler extends Handler {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case TcpClientService.MSG_OBDII_RESPONSE:
                    wordList.add(msg.obj.toString());
                    adapter.notifyDataSetChanged();
                    break;
                case TcpClientService.MSG_WRITE_LIST_TO_FILE:
                    Location loc = gpsLocation.getLocation();
                   /* try {
                        new WriteDownService(wordList, loc);
                    } catch (IOException e) {
                        e.printStackTrace();
                        Toast.makeText(getApplicationContext(), e.getLocalizedMessage(), Toast.LENGTH_LONG).show();
                    }*/
                    //we do request location after initial OBD requests
                    gpsLocation.requestLocation();
                    //clear screen
                    wordList.clear();
                    adapter.notifyDataSetChanged();
                    //Do restart dongle sometimes
                    /*if(++restartCount == RESTART_CYCLE) {
                        doUnbindService();
                        //set Timer for restarting service
                        TimerTask restartTask = new RestartServiceTask();
                        restartTimer.schedule(restartTask, RESTART_PERIOD);
                        restartCount = 0;
                    }*/
                    break;
                case TcpClientService.MSG_STOP_REQUESTS:
                    Toast.makeText(getApplicationContext(), msg.obj.toString(), Toast.LENGTH_SHORT).show();
                    doUnbindService();
                    //set Timer for restarting service
                    TimerTask restartTask = new RestartServiceTask();
                    restartTimer.schedule(restartTask, RESTART_PERIOD);
                    break;
                default:
                    super.handleMessage(msg);
            }
        }
    };
    /**
     * Target we publish for clients to send messages to IncomingHandler.
     */
    final Messenger mMessenger = new Messenger(new IncomingHandler());

    /**
     * Automatic restart service by timer after MSG_STOP_REQUESTS message from service
     */
    class RestartServiceTask extends TimerTask {
        public void run() {
            runOnUiThread(new Runnable(){

                @Override
                public void run() {
                    doBindService();
                }});
        }
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
