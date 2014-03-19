package com.bitmaster.obdii_wifi_collect.obdwifi;

import android.app.ListActivity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
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

import java.util.ArrayList;
import java.util.List;


public class MainActivity extends ListActivity {

    //private TcpClientService service = null;
    private ArrayAdapter<String> adapter = null;
    private List<String> wordList = null;
    private boolean mIsBound = false;
    /** Messenger for communicating with service. */
    private Messenger mService = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        wordList = new ArrayList<String>();
        adapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, android.R.id.text1, wordList);
        this.setListAdapter(adapter);

        //runTcpClientAsService();
        this.doBindService();
    }

    @Override
    protected void onResume() {
        super.onResume();
        doBindService();
    }

    @Override
    protected void onPause() {
        super.onPause();
        doUnbindService();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        this.doUnbindService();
    }

    public void onClick(View view) {

        sayHello(view);

        /*if (service != null) {
            Toast.makeText(this, "Number of elements" + service.getWordList().size(), Toast.LENGTH_SHORT).show();
            wordList.clear();
            wordList.addAll(service.getWordList());
            adapter.notifyDataSetChanged();
        }*/
    }
    public void sayHello(View v) {
        if (!mIsBound) return;
        // Create and send a message to the service, using a supported 'what' value
        Message msg = Message.obtain(null, TcpClientService.MSG_SET_VALUE, "MESSAGE");
        try {
            mService.send(msg);
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }



    // If the startService(intent) method is called and the service is not yet running,
    // the service object is created and the onCreate() method of the service is called.

    // If startService(intent) is called while the service is running, its onStartCommand() is also called.
    // Therefore your service needs to be prepared that onStartCommand() can be called several time
    private void runTcpClientAsService() {
        // use this to start and trigger a service
        Intent i = new Intent(this.getApplicationContext(), TcpClientService.class);
        this.startService(i);

        //one call to the stopService() method stops the service
        //this.stopService(i);
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
            // We want to monitor the service for as long as we are
            // connected to it.
            /*try {
                Message msg = Message.obtain(null, TcpClientService.MSG_REGISTER_CLIENT);
                msg.replyTo = mMessenger;
                mService.send(msg);

                // Give it some value as an example.
                msg = Message.obtain(null, TcpClientService.MSG_SET_VALUE, this.hashCode(), 0);
                mService.send(msg);
            } catch (RemoteException e) {
                // In this case the service has crashed before we could even
                // do anything with it; we can count on soon being
                // disconnected (and then reconnected if it can be restarted)
                // so there is no need to do anything here.
            }*/
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
        //Toast.makeText(MainActivity.this, "BindingService", Toast.LENGTH_SHORT).show();
        mIsBound = true;
    }

    void doUnbindService() {
        if (mIsBound) {
            // Detach our existing connection.
            unbindService(mConnection);
            Toast.makeText(MainActivity.this, "Unbind Service", Toast.LENGTH_LONG).show();
            mIsBound = false;
        }
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
        int id = item.getItemId();
        if (id == R.id.action_settings) {
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
