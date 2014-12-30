package com.twitter.university.android.yamba.service;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.text.TextUtils;
import android.util.Log;

import com.marakana.android.yamba.clientlib.YambaClient;
import com.marakana.android.yamba.clientlib.YambaClientException;
import com.twitter.university.android.yamba.data.YambaProvider;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;


public class YambaService {
    private static final String TAG = "SVC";

    private final Context ctxt;
    private final int pollSize;

    public YambaService(Context ctxt) {
        this.ctxt = ctxt;
        this.pollSize = ctxt.getResources().getInteger(R.integer.poll_size);
    }

    public void doSync(YambaClient client) {
        if (BuildConfig.DEBUG) { Log.d(TAG, "sync"); }

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
        ctxt.sendBroadcast(i, YambaContract.Service.PERMISSION_RECEIVE_POST_COMPLETE);
    }

    private int postPending(YambaClient client) throws YambaClientException {
        ContentResolver cr = ctxt.getContentResolver();
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
        n = ctxt.getContentResolver().bulkInsert(
            YambaContract.Timeline.URI,
            vals.toArray(new ContentValues[n]));

        if (BuildConfig.DEBUG) { Log.d(TAG, "inserted: " + n); }
        return n;
    }

    private long getLatestTweetTime() {
        Cursor c = null;
        try {
            c = ctxt.getContentResolver().query(
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
        ctxt.sendBroadcast(i, YambaContract.Service.PERMISSION_RECEIVE_TIMELINE_UPDATE);
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
}
