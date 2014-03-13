package com.bitmaster.obdii_wifi_collect.obdwifi;

import android.content.Intent;
import android.support.v7.app.ActionBarActivity;
import android.support.v4.app.Fragment;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;

import com.bitmaster.obdii_wifi_collect.obdwifi.io.TcpClientService;


public class MainActivity extends ActionBarActivity {

    //public static final int TCP_SERVER_PORT = 35000;
    //public static final String SERVER_IP_ADDRESS = "192.168.0.10";
    public static final int TCP_SERVER_PORT = 80;
    public static final String SERVER_IP_ADDRESS = "192.168.50.2";//proekspert.ee

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        if (savedInstanceState == null) {
            getSupportFragmentManager().beginTransaction()
                    .add(R.id.container, new PlaceholderFragment())
                    .commit();
        }

        this.runTcpClientAsService();
        //finish();
    }

    // If the startService(intent) method is called and the service is not yet running,
    // the service object is created and the onCreate() method of the service is called.

    // If startService(intent) is called while the service is running, its onStartCommand() is also called.
    // Therefore your service needs to be prepared that onStartCommand() can be called several time
    private void runTcpClientAsService() {
        // use this to start and trigger a service
        Intent i = new Intent(this.getApplicationContext(), TcpClientService.class);
        // potentially add data to the intent
        i.putExtra("KEY1", "Value to be used by the service");
        //Alternatively, you can also start a service via the bindService() method call.
        //This allows you to communicate directly with the service.
        this.startService(i);

        //one call to the stopService() method stops the service
        //this.stopService(i);
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

    /**
     * A placeholder fragment containing a simple view.
     */
    public static class PlaceholderFragment extends Fragment {

        public PlaceholderFragment() {
        }

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container,
                Bundle savedInstanceState) {
            View rootView = inflater.inflate(R.layout.fragment_main, container, false);
            return rootView;
        }
    }
}
