package com.bitmaster.obdii_wifi_collect.obdwifi.io;

import android.os.Environment;
import android.util.Log;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Created by uhh on 3/13/14.
 */
public class WriteDown {

    private String filename = "OBDII_logfile.txt";
    private File file = null;
    private FileOutputStream outputStream = null;
    private List<String> rows = new ArrayList<String>();

    public WriteDown() throws IOException {

        if(this.isExternalStorageWritable()) {
            this.file = new File(Environment.getExternalStorageDirectory(), this.filename);
            Log.i("WriteDown", "directory: " + Environment.getExternalStorageDirectory().getAbsolutePath());
        } else throw new IOException("External storage is not writable");

    }

    public void addRow (String row) {

        rows.add(row);
    }

    public void writeToFile () throws IOException {

        this.outputStream = new FileOutputStream(this.file, true);
        Iterator<String> it = rows.iterator();
        while(it.hasNext()) {
            String row = it.next() + "\n";
            outputStream.write(row.getBytes());
        }
        outputStream.close();
    }

    /* Checks if external storage is available for read and write */
    private boolean isExternalStorageWritable() {
        String state = Environment.getExternalStorageState();
        if (Environment.MEDIA_MOUNTED.equals(state)) {
            return true;
        }
        return false;
    }

}
