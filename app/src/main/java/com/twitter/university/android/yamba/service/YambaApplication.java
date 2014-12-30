package com.twitter.university.android.yamba.service;

import android.app.Application;
import android.util.Log;

import com.marakana.android.yamba.clientlib.YambaClient;


public class YambaApplication extends Application {
    private static final String TAG = "APP";

    private String token;
    private YambaClient client;

    public synchronized void createClient(String tkn, String hdl, String pwd, String uri) {
        Log.d(TAG, "client: " + tkn + "#" + hdl + "," + pwd + "@" + uri);
        client = new YambaClient(hdl, pwd, uri);
        token = tkn;
    }

    public synchronized YambaClient getClientByToken(String tkn) {
        return ((null == token) || !token.equals(tkn))
            ? null
            : client;
    }
}
