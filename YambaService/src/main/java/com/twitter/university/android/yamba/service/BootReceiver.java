package com.twitter.university.android.yamba.service;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;


public class BootReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context ctxt, Intent i) {
        YambaService.startPoller(ctxt);
    }
}
