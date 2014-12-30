package com.twitter.university.android.yamba.service;

import android.app.AlarmManager;
import android.app.IntentService;
import android.app.PendingIntent;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.database.Cursor;
import android.text.TextUtils;
import android.util.Log;

import com.marakana.android.yamba.clientlib.YambaClient;
import com.marakana.android.yamba.clientlib.YambaClientException;
import com.twitter.university.android.yamba.data.YambaProvider;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;


public class YambaService extends IntentService {
    private static final String TAG = "SVC";

    private static final int POLLER = 666;

    public static void startPoller(Context ctxt) {
        Intent i = new Intent(ctxt, YambaService.class);
        i.putExtra(YambaContract.Service.PARAM_OP, YambaContract.Service.OP_START_POLLING);
        ctxt.startService(i);
    }

    public static void sync(Context ctxt) {
        Intent i = new Intent(ctxt, YambaService.class);
        i.putExtra(YambaContract.Service.PARAM_OP, YambaContract.Service.OP_SYNC);
        ctxt.startService(i);
    }


    private volatile int pollSize;
    private volatile long pollInterval;

    public YambaService() { super(TAG); }

    @Override
    public void onCreate() {
        super.onCreate();
        if (BuildConfig.DEBUG) { Log.d(TAG, "created"); }

        Resources rez = getResources();
        pollSize = rez.getInteger(R.integer.poll_size);
        pollInterval = rez.getInteger(R.integer.poll_interval) * 60 * 1000;

        doStartPoller();
   }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (BuildConfig.DEBUG) { Log.d(TAG, "destroyed"); }
    }

    @Override
    protected void onHandleIntent(Intent i) {
        int op = i.getIntExtra(YambaContract.Service.PARAM_OP, 0);
        if (BuildConfig.DEBUG) { Log.d(TAG, "exec: " + op); }
        switch (op) {
            case YambaContract.Service.OP_SYNC:
                doSync();
                break;

            case YambaContract.Service.OP_START_POLLING:
                doStartPoller();
                break;

            case YambaContract.Service.OP_STOP_POLLING:
                doStopPoller();
                break;

            default:
                Log.e(TAG, "Unexpected op: " + op);
        }
    }

    private void doStartPoller() {
        if (0 >= pollInterval) { return; }
        ((AlarmManager) getSystemService(Context.ALARM_SERVICE))
            .setInexactRepeating(
                AlarmManager.RTC,
                System.currentTimeMillis() + 100,
                pollInterval,
                createPollingIntent());
    }

    private void doStopPoller() {
        ((AlarmManager) getSystemService(Context.ALARM_SERVICE))
            .cancel(createPollingIntent());
    }

    private void doSync() {
        if (BuildConfig.DEBUG) { Log.d(TAG, "sync"); }

        YambaClient client;
        try { client = getClient(); }
        catch (YambaClientException e) {
            Log.e(TAG, "Failed to get client", e);
            return;
        }

        if (null == client) {
            Log.e(TAG, "Client is null");
            return;
        }

        try { notifyPost(postPending(client)); }
        catch (YambaClientException e) {
            Log.e(TAG, "post failed", e);
        }

        try { notifyTimelineUpdate(parseTimeline(client.getTimeline(pollSize))); }
        catch (YambaClientException e) {
            Log.e(TAG, "poll failed", e);
        }
    }

    private void notifyPost(int count) {
        if (BuildConfig.DEBUG) { Log.d(TAG, "post: " + count); }
        if (count <= 0) { return; }
        Intent i = new Intent(YambaContract.Service.ACTION_POST_COMPLETE);
        i.putExtra(YambaContract.Service.PARAM_POST_SUCCEEDED, count);
        sendBroadcast(i, YambaContract.Service.PERMISSION_RECEIVE_POST_COMPLETE);
    }

    private PendingIntent createPollingIntent() {
        Intent i = new Intent(this, YambaService.class);
        i.putExtra(YambaContract.Service.PARAM_OP, YambaContract.Service.OP_SYNC);
        return PendingIntent.getService(
            this,
            POLLER,
            i,
            PendingIntent.FLAG_UPDATE_CURRENT);
    }

    private int postPending(YambaClient client) throws YambaClientException {
        ContentResolver cr = getContentResolver();

        String xactId = UUID.randomUUID().toString();

        int n = beginUpdate(cr, xactId);
        if (0 >= n) { return 0; }

        List<String> posted = new ArrayList<String>();
        Cursor cur = null;
        try {
            cur = cr.query(
                YambaContract.Posts.URI,
                null,
                YambaContract.Posts.Columns.TRANSACTION + "=?",
                new String[] { xactId },
                YambaContract.Posts.Columns.TIMESTAMP + " ASC");
            postTweets(client, cur, posted);
        }
        finally {
            if (null != cur) {
                try { cur.close(); } catch (Exception ignore) { }
                try { updateSucceeded(cr, posted); }
                finally { endUpdate(cr, xactId); }
            }
        }

        return posted.size();
    }

    private void postTweets(YambaClient client, Cursor c, List<String> posted)
        throws YambaClientException
    {
        int idIdx = c.getColumnIndex(YambaContract.Posts.Columns.ID);
        int tweetIdx = c.getColumnIndex(YambaContract.Posts.Columns.TWEET);

        while (c.moveToNext()) {
            String tweet = c.getString(tweetIdx);
            // failure here will abort subsequent posts
            // and post order will be retained.
            client.postStatus(tweet);
            if (BuildConfig.DEBUG) { Log.d(TAG, "posted: " + tweet); }
            posted.add(c.getString(idIdx));
        }
    }

    private int parseTimeline(List<YambaClient.Status> timeline) {
        long latest = getLatestTweetTime();
        if (BuildConfig.DEBUG) { Log.d(TAG, "latest: " + latest); }

        List<ContentValues> vals = new ArrayList<ContentValues>();

        for (YambaClient.Status tweet: timeline) {
            long t = tweet.getCreatedAt().getTime();
            if (t <= latest) { continue; }

            ContentValues cv = new ContentValues();
            cv.put(YambaContract.Timeline.Columns.ID, Long.valueOf(tweet.getId()));
            cv.put(YambaContract.Timeline.Columns.TIMESTAMP, Long.valueOf(t));
            cv.put(YambaContract.Timeline.Columns.HANDLE, tweet.getUser());
            cv.put(YambaContract.Timeline.Columns.TWEET, tweet.getMessage());
            vals.add(cv);
        }

        int n = vals.size();
        if (0 >= n) { return 0; }
        n = getContentResolver().bulkInsert(
            YambaContract.Timeline.URI,
            vals.toArray(new ContentValues[n]));

        if (BuildConfig.DEBUG) { Log.d(TAG, "inserted: " + n); }
        return n;
    }

    private long getLatestTweetTime() {
        Cursor c = null;
        try {
            c = getContentResolver().query(
                YambaContract.MaxTimeline.URI,
                null,
                null,
                null,
                null);
            return ((null == c) || (!c.moveToNext()))
                ? Long.MIN_VALUE
                : c.getLong(0);
        }
        finally {
            if (null != c) { c.close(); }
        }
    }

    private void notifyTimelineUpdate(int count) {
        if (BuildConfig.DEBUG) { Log.d(TAG, "timeline: " + count); }
        if (count <= 0) { return; }
        Intent i = new Intent(YambaContract.Service.ACTION_TIMELINE_UPDATED);
        i.putExtra(YambaContract.Service.PARAM_COUNT, count);
        sendBroadcast(i, YambaContract.Service.PERMISSION_RECEIVE_TIMELINE_UPDATE);
    }

    private int beginUpdate(ContentResolver cr, String xactId) {
        ContentValues row = new ContentValues();
        row.put(YambaContract.Posts.Columns.TRANSACTION, xactId);
        int n = cr.update(
            YambaContract.Posts.URI,
            row,
            YambaProvider.CONSTRAINT_NEEDS_SYNC,
            null);
        if (BuildConfig.DEBUG) { Log.d(TAG, "begin update: " + n); }
        return n;
    }

    private void updateSucceeded(ContentResolver cr, List<String> posted) {
        int n = posted.size();
        if (BuildConfig.DEBUG) { Log.d(TAG, "update succeeded: " + n); }
        if (0 >= n) { return; }

        ContentValues row = new ContentValues();
        row.put(YambaContract.Posts.Columns.SENT, System.currentTimeMillis());
        cr.update(
            YambaContract.Posts.URI,
            row,
            YambaProvider.CONSTRAINT_IDS + "(" + TextUtils.join(",", posted) + ")",
            null);
    }

    private void endUpdate(ContentResolver cr, String xactId) {
        ContentValues row = new ContentValues();
        row.putNull(YambaContract.Posts.Columns.TRANSACTION);
        int n = cr.update(
            YambaContract.Posts.URI,
            row,
            YambaProvider.CONSTRAINT_XACT,
            new String[] { xactId });
        if (BuildConfig.DEBUG) { Log.d(TAG, "update complete: " + n); }
    }

    private YambaClient getClient() throws YambaClientException {
        return ((YambaApplication) getApplication()).getYambaClient();
    }
}
