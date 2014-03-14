package com.bitmaster.obdii_wifi_collect.obdwifi.io;

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

    private final String FILENAME = "OBDIILog.txt";

    public WriteDown(List<String> rowsToWrite) throws IOException {

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

    private static  String formatToCsvLine(List<String> rows) {

        String line = "";
        Iterator<String> it = rows.iterator();
        while(it.hasNext()) {
            line += (it.next() + ",");
        }
        SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd_HHmmss");
        String currentDateandTime = sdf.format(new Date());

        return (currentDateandTime + "," + line + ";\n");
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
