package com.bitmaster.obdii_wifi_collect.obdwifi.obd2;

/**
 * Created by renet on 5/2/14.
 */
public class Message {

    private String pid;
    private String value1;
    private String value2;

    public Message(String pid, String value1, String value2) {
        this.pid = pid;
    }

    public String getPid() {
        return pid;
    }

    public void setPid(String pid) {
        this.pid = pid;
    }

    public String getValue1() {
        return value1;
    }

    public void setValue1(String value1) {
        this.value1 = value1;
    }

    public String getValue2() {
        return value2;
    }

    public void setValue2(String value2) {
        this.value2 = value2;
    }
}
