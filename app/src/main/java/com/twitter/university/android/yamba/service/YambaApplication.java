package com.twitter.university.android.yamba.service;

import android.app.Application;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.preference.PreferenceManager;
import android.util.Log;

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
    private String hdlDefault;
    private String pwdDefault;
    private String uriDefault;

    @Override
    public void onCreate() {
        super.onCreate();

        Resources rez = getResources();
        hdlKey = rez.getString(R.string.prefs_key_handle);
        hdlDefault  = rez.getString(R.string.prefs_default_handle);
        pwdKey = rez.getString(R.string.prefs_key_pwd);
        pwdDefault  = rez.getString(R.string.prefs_default_pwd);
        uriKey = rez.getString(R.string.prefs_key_uri);
        uriDefault  = rez.getString(R.string.prefs_default_uri);

        // Don't use an anonymous class to handle this event!
        // http://stackoverflow.com/questions/3799038/onsharedpreferencechanged-not-fired-if-change-occurs-in-separate-activity
        PreferenceManager.getDefaultSharedPreferences(this)
            .registerOnSharedPreferenceChangeListener(this);

        YambaService.startPoller(this);
    }

    @Override
    public synchronized void onSharedPreferenceChanged(SharedPreferences prefs, String key) {
        if (BuildConfig.DEBUG) { Log.d(TAG, "prefs changed"); }
        yamba = null;
    }

    public synchronized YambaClient getYambaClient() throws YambaClientException {
        if (null == yamba) {
            SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
            String hdl = prefs.getString(hdlKey, hdlDefault);
            String pwd = prefs.getString(pwdKey, pwdDefault);
            String uri = prefs.getString(uriKey, uriDefault);

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
