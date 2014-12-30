package com.twitter.university.android.yamba.sync;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;

import com.twitter.university.android.yamba.service.YambaApplication;


public class SyncService extends Service {
    public static final String ACTION_BIND_SYNC = "android.content.SyncAdapter";
    public static final String ACTION_BIND_AUTH = "android.accounts.AccountAuthenticator";


    private SyncAdapter synchronizer;
    private AccountMgr authenticator;

    @Override
    public void onCreate() {
        super.onCreate();
        YambaApplication app = (YambaApplication) getApplication();
        authenticator = new AccountMgr(app);
        synchronizer = new SyncAdapter(app, true);
    }

    @Override
    public IBinder onBind(Intent intent) {
        String action = intent.getAction();
        if (ACTION_BIND_AUTH.equals(action)) {
            return authenticator.getIBinder();
        }
        if (ACTION_BIND_SYNC.equals(action)) {
            return synchronizer.getSyncAdapterBinder();
        }
        return null;
    }
}
