package com.twitter.twitteru.android.yamba.service;

import android.app.Application;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.preference.PreferenceManager;
import android.util.Log;

import com.twitter.twitteru.android.yamba.service.YambaService;

import com.marakana.android.yamba.clientlib.YambaClient;
import com.marakana.android.yamba.clientlib.YambaClientException;


public class YambaApplication extends Application
    implements SharedPreferences.OnSharedPreferenceChangeListener
{
    private static final String TAG = "APP";


    private YambaClient yamba;
    private String hdlKey;
    private String pwdKey;
    private String uriKey;

    @Override
    public void onCreate() {
        super.onCreate();

        Resources rez = getResources();
        hdlKey = rez.getString(R.string.prefs_key_handle);
        pwdKey = rez.getString(R.string.prefs_key_pass);
        uriKey = rez.getString(R.string.prefs_key_uri);

        YambaService.startPoller(this);

        // Don't use an anonymous class to handle this event!
        // http://stackoverflow.com/questions/3799038/onsharedpreferencechanged-not-fired-if-change-occurs-in-separate-activity
        PreferenceManager.getDefaultSharedPreferences(this)
            .registerOnSharedPreferenceChangeListener(this);
    }

    @Override
    public synchronized void onSharedPreferenceChanged(SharedPreferences prefs, String key) {
        if (BuildConfig.DEBUG) { Log.d(TAG, "prefs changed"); }
        yamba = null;
    }

    public synchronized YambaClient getYambaClient() throws YambaClientException {
        if (null == yamba) {
            SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
            String hdl = prefs.getString(hdlKey, null);
            String pwd = prefs.getString(pwdKey, null);
            String uri = prefs.getString(uriKey, null);

            if (BuildConfig.DEBUG) {
                Log.d(TAG, "new handle: " + hdl + "," + pwd  + " @" + uri);
            }
            try { yamba = new YambaClient(hdl, pwd, uri); }
            catch (IllegalArgumentException e) {
                Log.d(TAG, "failed to create client");
                throw new YambaClientException("failed to create client", e);
            }
        }

        return yamba;
    }
}
