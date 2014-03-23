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
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Toast;

import com.bitmaster.obdii_wifi_collect.obdwifi.io.TcpClientService;
import com.bitmaster.obdii_wifi_collect.obdwifi.io.WriteDown;
import com.bitmaster.obdii_wifi_collect.obdwifi.loc.GpsLocation;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;


public class MainActivity extends ListActivity {

    //private TcpClientService service = null;
    private ArrayAdapter<String> adapter = null;
    private List<String> wordList = null;
    private boolean mIsBound = false;
    /** Messenger for communicating with service. */
    private Messenger mService = null;
    private GpsLocation gpsLocation = null;
    private static final int RESTART_CYCLE = 10;//restart dongle after writing lines
    private static final long RESTART_PERIOD = 60*1000;//restart service after destroy self by msg
    private static int restartCount = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        wordList = new ArrayList<String>();
        adapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, android.R.id.text1, wordList);
        this.setListAdapter(adapter);

        this.gpsLocation = new GpsLocation(this);
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

    public void onClick(View view) {

        bindToOBDII(view);
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
            Toast.makeText(MainActivity.this, "TcpServiceDisconnected", Toast.LENGTH_LONG).show();
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
            Toast.makeText(MainActivity.this, "Unbinding Service", Toast.LENGTH_LONG).show();
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
                    try {
                        new WriteDown(wordList, loc);
                    } catch (IOException e) {
                        e.printStackTrace();
                        Toast.makeText(getApplicationContext(), e.getLocalizedMessage(), Toast.LENGTH_LONG).show();
                    }
                    //we do request location after initial OBD requests
                    gpsLocation.requestLocation();
                    //clear screen
                    wordList.clear();
                    adapter.notifyDataSetChanged();
                    //Do restart dongle sometimes
                    /*if(++restartCount == RESTART_CYCLE) {
                        //Create fresh queue of PID and start requests with reset
                        msg = Message.obtain(null, TcpClientService.MSG_START_REQUESTS);
                        msg.replyTo = mMessenger;
                        try {
                            mService.send(msg);
                        } catch (RemoteException e) {
                            e.printStackTrace();
                        }
                        restartCount = 0;
                    }*/
                    break;
                case TcpClientService.MSG_STOP_REQUESTS:
                    Toast.makeText(getApplicationContext(), msg.obj.toString(), Toast.LENGTH_SHORT).show();
                    doUnbindService();
                    //set Timer for restarting service
                    TimerTask restartTask = new RestartServiceTask();
                    Timer restartTimer = new Timer();
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
