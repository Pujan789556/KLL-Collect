package com.kll.collect.android.logic;

/**
 * Created by Narotam on 6/18/2015.
 */
public class FormDetailListAdapter {

    private String versionNew;
    private String versionOld;



    public String getVersionNew() {
        return versionNew;
    }

    public void setVersionNew(String versionNew) {
        this.versionNew = versionNew;
    }

    public String getVersionOld() {
        return versionOld;
    }

    public void setVersionOld(String versionOld) {
        this.versionOld = versionOld;
    }

    public FormDetailListAdapter( String versionNew, String versionOld) {

        this.versionNew = versionNew;
        this.versionOld = versionOld;
    }

}
