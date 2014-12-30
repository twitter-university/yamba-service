package com.twitter.university.android.yamba.data;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;


class YambaDbHelper extends SQLiteOpenHelper {
    private static final String TAG = "DB";

    public static final String DATABASE = "yamba.db";
    public static final int VERSION = 4;

    static final String TABLE_TIMELINE = "p_timeline";
    static final String COL_ID = "p_id";
    static final String COL_TIMESTAMP = "p_timestamp";
    static final String COL_HANDLE = "p_handle";
    static final String COL_TWEET = "p_tweet";

    static final String TABLE_POSTS = "p_posts";
    static final String COL_XACT = "p_xact";
    static final String COL_SENT = "p_sent";


    public YambaDbHelper(Context context) {
        super(context, DATABASE, null, VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        Log.d(TAG, "create db");
        db.execSQL(
            "CREATE TABLE " + TABLE_TIMELINE + "("
                + COL_ID + " INTEGER PRIMARY KEY,"
                + COL_TIMESTAMP + " INTEGER NOT NULL,"
                + COL_HANDLE + " STRING NOT NULL,"
                + COL_TWEET + " STRING NOT NULL" + ")");
        db.execSQL(
            "CREATE TABLE " + TABLE_POSTS + "("
                + COL_ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
                + COL_TIMESTAMP + " INTEGER NOT NULL,"
                + COL_XACT + " STRING,"
                + COL_SENT + " INTEGER,"
                + COL_TWEET + " STRING NOT NULL" + ")");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        Log.d(TAG, "update db");
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_POSTS);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_TIMELINE);
        onCreate(db);
    }
}
