package com.bitmaster.obdii_wifi_collect.obdwifi.io;

import android.location.Location;
import android.os.Environment;
import android.util.Log;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Iterator;
import java.util.List;

/**
 * Created by uhh on 3/13/14.
 */
public class WriteDown {

    private final String FILENAME = "OBDIILog.csv";
    private double latitude = 0.0;
    private double longitude = 0.0;
    private double speed = 0.0;

    public WriteDown(List<String> rowsToWrite, Location location) throws IOException {

        if(location != null){
            this.latitude = location.getLatitude();
            this.longitude = location.getLongitude();
            this.speed = location.getSpeed();
        }
        String csvLine = this.formatToCsvLine(rowsToWrite);

        if(this.isExternalStorageWritable()) {

            File file = new File(Environment.getExternalStorageDirectory(), this.FILENAME);
            Log.i("WriteDown", "directory: " + Environment.getExternalStorageDirectory().getAbsolutePath());

            FileOutputStream outputStream = new FileOutputStream(file, true); //append==true
            outputStream.write(csvLine.getBytes());
            outputStream.close();

        } else {
            throw new IOException("External storage is not writable");
        }

    }

    private String formatToCsvLine(List<String> rows) {

        String line = "";
        Iterator<String> it = rows.iterator();
        while(it.hasNext()) {
            line += (it.next()).replace("\n", ",").replace("\r", ",");
        }
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        String currentDateandTime = sdf.format(new Date());

        return (currentDateandTime +
                "," + line +
                "," + Double.toString(this.latitude) +
                "," + Double.toString(this.longitude) +
                "," + Double.toString(this.speed) +
                ";\n");
    }

    /* Checks if external storage is available for read and write */
    private static boolean isExternalStorageWritable() {
        String state = Environment.getExternalStorageState();
        if (Environment.MEDIA_MOUNTED.equals(state)) {
            return true;
        }
        return false;
    }

}
