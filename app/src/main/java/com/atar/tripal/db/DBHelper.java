package com.atar.tripal.db;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

public class DBHelper extends SQLiteOpenHelper {

    private Context mContext;

    public DBHelper(Context context) {
        super(context, DBConstants.DB_NAME, null, DBConstants.DB_VERSION);
        mContext = context;
    }

    @Override
    public void onCreate(SQLiteDatabase sqLiteDatabase) {

        String i = Details.getProfileId(mContext);

        String messagesTable = "CREATE TABLE IF NOT EXISTS " +
                DBConstants.MESSAGES_TABLE + i + " ( " + DBConstants.COL_ID + " INTEGER PRIMARY KEY, " +
                DBConstants.COL_HANGOUT_ID + " INTEGER, " + DBConstants.COL_SENDER_ID + " TEXT, " +
                DBConstants.COL_THEME + " INTEGER, " + DBConstants.COL_SENDER_NAME + " INTEGER, " +
                DBConstants.COL_CONTENT + " TEXT, " + DBConstants.COL_TYPE + " INTEGER, " +
                DBConstants.COL_STATUS + " INTEGER, " + DBConstants.COL_TIMESTAMP + " INTEGER )";
        try {
            sqLiteDatabase.execSQL(messagesTable);
            Log.i("message:", DBConstants.MESSAGES_TABLE + i + " table created");
        } catch (SQLiteException e){
            e.printStackTrace();
        }

    }

    @Override
    public void onUpgrade(SQLiteDatabase sqLiteDatabase, int i, int i1) {
        onCreate(sqLiteDatabase);
    }
}
