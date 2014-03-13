package com.bitmaster.obdii_wifi_collect.obdwifi;

import android.app.ListActivity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.support.v4.app.Fragment;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Toast;

import com.bitmaster.obdii_wifi_collect.obdwifi.io.TcpClientService;

import java.util.ArrayList;
import java.util.List;


public class MainActivity extends ListActivity {

    private TcpClientService s = null;
    private ArrayAdapter<String> adapter = null;
    private List<String> wordList = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        wordList = new ArrayList<String>();
        adapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, android.R.id.text1, wordList);
        this.setListAdapter(adapter);

        this.runTcpClientAsService();
        //finish();
    }

    @Override
    protected void onResume() {
        super.onResume();
        Intent intent= new Intent(this, TcpClientService.class);
        bindService(intent, mConnection, Context.BIND_AUTO_CREATE);
    }

    @Override
    protected void onPause() {
        super.onPause();
        unbindService(mConnection);
    }

    private ServiceConnection mConnection = new ServiceConnection() {

        public void onServiceConnected(ComponentName className, IBinder binder) {
            TcpClientService.MyBinder b = (TcpClientService.MyBinder) binder;
            s = b.getService();
            Toast.makeText(MainActivity.this, "Connected", Toast.LENGTH_SHORT).show();
        }

        public void onServiceDisconnected(ComponentName className) {
            Toast.makeText(MainActivity.this, "Disconnected", Toast.LENGTH_SHORT).show();
            s = null;
        }
    };

    public void onClick(View view) {
        if (s != null) {
            Toast.makeText(this, "Number of elements" + s.getWordList().size(), Toast.LENGTH_SHORT).show();
            wordList.clear();
            wordList.addAll(s.getWordList());
            adapter.notifyDataSetChanged();
        }
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
}
