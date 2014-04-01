package com.bitmaster.obdii_wifi_collect.obdwifi.io;

import android.app.IntentService;
import android.content.Intent;
import android.os.Environment;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

/**
 * Created by uhh on 3/13/14.
 */
public class WriteDownService extends IntentService {

    private final String FILENAME = "OBDIILog.csv";

    public WriteDownService() {
        super("WriteDownService");
    }

    @Override
    protected void onHandleIntent(Intent intent) {

        synchronized (this) {
            //Checks if external storage is available for read and write
            String state = Environment.getExternalStorageState();
            if (Environment.MEDIA_MOUNTED.equals(state)) {

                String csvLine = intent.getStringExtra("com.bitmaster.obdii_wifi_collect.obdwifi.io.csvLine");

                File file = new File(Environment.getExternalStorageDirectory(), this.FILENAME);
                try {
                    FileOutputStream outputStream = new FileOutputStream(file, true);//append==true
                    outputStream.write(csvLine.getBytes());
                    outputStream.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }
}
