package com.twitter.university.android.yamba.data;

import android.content.ContentProvider;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteQueryBuilder;
import android.net.Uri;
import android.util.Log;

import com.twitter.university.android.yamba.service.YambaContract;

import java.util.Map;


public class YambaProvider extends ContentProvider {
    private static final String TAG = "PROVIDER";

    private static final int MAX_TIMELINE_ITEM_TYPE = 1;
    private static final int TIMELINE_ITEM_TYPE = 2;
    private static final int TIMELINE_DIR_TYPE = 3;

    //  scheme                     authority                   path  [id]
    // content://com.twitter.university.android.yamba.timeline/timeline/7
    private static final UriMatcher MATCHER = new UriMatcher(UriMatcher.NO_MATCH);
    static {
        MATCHER.addURI(
            YambaContract.AUTHORITY,
            YambaContract.MaxTimeline.TABLE,
            MAX_TIMELINE_ITEM_TYPE);
        MATCHER.addURI(
            YambaContract.AUTHORITY,
            YambaContract.Timeline.TABLE + "/#",
            TIMELINE_ITEM_TYPE);
        MATCHER.addURI(
            YambaContract.AUTHORITY,
            YambaContract.Timeline.TABLE,
            TIMELINE_DIR_TYPE);
    }

    private static final ColumnMap COL_MAP_TIMELINE = new ColumnMap.Builder()
        .addColumn(
                YambaContract.Timeline.Columns.ID,
                YambaDbHelper.COL_ID,
                ColumnMap.Type.LONG)
        .addColumn(
                YambaContract.Timeline.Columns.TIMESTAMP,
                YambaDbHelper.COL_TIMESTAMP,
                ColumnMap.Type.LONG)
        .addColumn(
                YambaContract.Timeline.Columns.HANDLE,
                YambaDbHelper.COL_HANDLE,
                ColumnMap.Type.STRING)
        .addColumn(
                YambaContract.Timeline.Columns.TWEET,
                YambaDbHelper.COL_TWEET,
                ColumnMap.Type.STRING)
        .build();

    private static final Map<String, String> PROJ_MAP_TIMELINE = new ProjectionMap.Builder()
        .addColumn(YambaContract.Timeline.Columns.ID, YambaDbHelper.COL_ID)
        .addColumn(YambaContract.Timeline.Columns.TIMESTAMP, YambaDbHelper.COL_TIMESTAMP)
        .addColumn(YambaContract.Timeline.Columns.HANDLE, YambaDbHelper.COL_HANDLE)
        .addColumn(YambaContract.Timeline.Columns.TWEET, YambaDbHelper.COL_TWEET)
        .build()
        .getProjectionMap();

    private static final Map<String, String> PROJ_MAP_MAX_TIMELINE = new ProjectionMap.Builder()
        .addColumn(
            YambaContract.Timeline.Columns.MAX_TIMESTAMP,
            "max(" + YambaDbHelper.COL_TIMESTAMP + ")")
        .build()
        .getProjectionMap();

    private YambaDbHelper dbHelper;

    @Override
    public boolean onCreate() {
        Log.d(TAG, "provider created");
        dbHelper = new YambaDbHelper(getContext());
        return null != dbHelper;
    }

    @Override
    public String getType(Uri uri) {
        switch (MATCHER.match(uri)) {
            case MAX_TIMELINE_ITEM_TYPE:
                return YambaContract.MaxTimeline.ITEM_TYPE;
            case TIMELINE_ITEM_TYPE:
                return YambaContract.Timeline.ITEM_TYPE;
            case TIMELINE_DIR_TYPE:
                return YambaContract.Timeline.DIR_TYPE;
            default:
                return null;
        }
    }

    @SuppressWarnings("fallthrough")
    @Override
    public Cursor query(Uri uri, String[] proj, String sel, String[] selArgs, String sort) {
        Log.d(TAG, "query");

        long pk = -1;
        Map<String, String> projMap;
        switch (MATCHER.match(uri)) {
            case MAX_TIMELINE_ITEM_TYPE:
                projMap = PROJ_MAP_MAX_TIMELINE;
                break;
            case TIMELINE_ITEM_TYPE:
                pk = ContentUris.parseId(uri);
            case TIMELINE_DIR_TYPE:
                projMap = PROJ_MAP_TIMELINE;
                break;
            default:
                throw new IllegalArgumentException("Unexpected uri: " + uri);
        }

        SQLiteQueryBuilder qb = new SQLiteQueryBuilder();
        qb.setTables(YambaDbHelper.TABLE_TIMELINE);

        qb.setProjectionMap(projMap);

        if (0 < pk) { qb.appendWhere(YambaDbHelper.COL_ID + "=" + pk); }

        Cursor c = qb.query(getDb(), proj, sel, selArgs, null, null, sort);

        c.setNotificationUri(getContext().getContentResolver(), uri);

        return c;
    }

    @Override
    public int bulkInsert(Uri uri, ContentValues[] rows) {
        Log.d(TAG, "insert: " + rows.length);

        String table;
        switch (MATCHER.match(uri)) {
            case TIMELINE_DIR_TYPE:
                table = YambaDbHelper.TABLE_TIMELINE;
                break;
            default:
                throw new IllegalArgumentException("Unexpected uri: " + uri);
        }

        int count = 0;

        SQLiteDatabase db = getDb();
        try {
            db.beginTransaction();
            for (ContentValues row: rows) {
                if (0 < db.insert(
                        table,
                        null,
                        COL_MAP_TIMELINE.translateCols(row)))
                {
                    count++;
                }
            }
            db.setTransactionSuccessful();
        }
        finally {
            db.endTransaction();
        }

        if (0 < count) {
            getContext().getContentResolver().notifyChange(uri, null);
        }

        return count;
    }

    @Override
    public Uri insert(Uri arg0, ContentValues arg1) {
        throw new UnsupportedOperationException("insert not supported");
    }

    @Override
    public int update(Uri arg0, ContentValues arg1, String arg2, String[] arg3) {
        throw new UnsupportedOperationException("update not supported");
    }

    @Override
    public int delete(Uri arg0, String arg1, String[] arg2) {
        throw new UnsupportedOperationException("delete not supported");
    }

    private SQLiteDatabase getDb() { return dbHelper.getWritableDatabase(); }
}
