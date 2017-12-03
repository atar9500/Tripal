package com.atar.tripal.db;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.util.Log;

import com.atar.tripal.objects.Message;

import java.util.ArrayList;
import java.util.List;

public class DBHandler {

    private static final String TAG = "DBHandler";

    private String mProfileId;
    private DBHelper mHelper;

    public DBHandler(Context context) {
        mProfileId = Details.getProfileId(context);
        mHelper = new DBHelper(context);
    }

    @SuppressWarnings("TryFinallyCanBeTryWithResources")
    public long addMessage(Message message) {
        Log.e("message", "start adding");
        SQLiteDatabase db = mHelper.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(DBConstants.COL_CONTENT, message.getContent());
        values.put(DBConstants.COL_TIMESTAMP, System.currentTimeMillis());
        values.put(DBConstants.COL_HANGOUT_ID, message.getHangoutId());
        values.put(DBConstants.COL_SENDER_ID, message.getSenderId());
        values.put(DBConstants.COL_SENDER_NAME, message.getSenderName());
        values.put(DBConstants.COL_STATUS, message.getStatus());
        values.put(DBConstants.COL_TYPE, message.getType());
        values.put(DBConstants.COL_THEME, message.getHangoutTheme());
        try {
            Log.e(TAG, "Message added to the DB");
            return db.insertOrThrow(DBConstants.MESSAGES_TABLE +
                    mProfileId, null, values);
        } catch (SQLiteException e) {
            e.printStackTrace();
            return -1;
        } finally {
            db.close();
        }
    }

    public int getSentRequests(long hangoutId){
        SQLiteDatabase db = mHelper.getReadableDatabase();
        Cursor cursor = db.query(DBConstants.MESSAGES_TABLE + mProfileId,
                new String[]{DBConstants.COL_HANGOUT_ID}, DBConstants.COL_TYPE + " = ? AND " +
                DBConstants.COL_SENDER_ID + " = ? AND " + DBConstants.COL_HANGOUT_ID + " = ?",
                new String[]{Message.TYPE_JOIN + "", mProfileId + "", hangoutId + ""},
                null, null, null);
        try{
            return cursor.getCount();
        } catch (SQLiteException e){
            e.printStackTrace();
            return 0;
        } finally {
            cursor.close();
            mHelper.close();
            db.close();
        }
    }

    public List<Message> getReceivedRequests(long hangoutId){
        SQLiteDatabase db = mHelper.getReadableDatabase();
        Cursor cursor = db.query(DBConstants.MESSAGES_TABLE + mProfileId,
                null, DBConstants.COL_TYPE + " = ? AND " +
                DBConstants.COL_HANGOUT_ID + " = ? AND " + DBConstants.COL_SENDER_ID + " != ?",
                new String[]{Message.TYPE_JOIN + "", hangoutId + "", mProfileId + ""},
                null, null, null);
        List<Message> requests = new ArrayList<>();
        try{
            while (cursor.moveToNext()){
                if(!mProfileId.equals(cursor.getString(cursor.getColumnIndex(DBConstants.COL_SENDER_ID)))){
                    Message message = new Message();
                    message.setId(cursor.getLong(cursor.getColumnIndex(DBConstants.COL_ID)));
                    message.setContent(cursor.getString(cursor.getColumnIndex(DBConstants.COL_CONTENT)));
                    message.setTimestamp(cursor.getLong(cursor.getColumnIndex(DBConstants.COL_TIMESTAMP)));
                    message.setSenderName(cursor.getString(cursor.getColumnIndex(DBConstants.COL_SENDER_NAME)));
                    message.setSenderId(cursor.getString(cursor.getColumnIndex(DBConstants.COL_SENDER_ID)));
                    message.setHangoutId(cursor.getLong(cursor.getColumnIndex(DBConstants.COL_HANGOUT_ID)));
                    message.setStatus(cursor.getInt(cursor.getColumnIndex(DBConstants.COL_STATUS)));
                    message.setType(cursor.getInt(cursor.getColumnIndex(DBConstants.COL_TYPE)));
                    message.setHangoutTheme(cursor.getString(cursor.getColumnIndex(DBConstants.COL_THEME)));
                    requests.add(message);
                }
            }
            return requests;
        } catch (SQLiteException e){
            e.printStackTrace();
            return null;
        } finally {
            cursor.close();
            mHelper.close();
            db.close();
        }
    }

    public Message getMessage(long id){
        SQLiteDatabase db = mHelper.getReadableDatabase();
        Cursor cursor = db.query(DBConstants.MESSAGES_TABLE + mProfileId,
                null, DBConstants.COL_ID + " = ?", new String[]{id + ""},
                null, null, null);
        try{
            if(cursor.moveToNext()){
                Message message = new Message();
                message.setId(cursor.getLong(cursor.getColumnIndex(DBConstants.COL_ID)));
                message.setContent(cursor.getString(cursor.getColumnIndex(DBConstants.COL_CONTENT)));
                message.setTimestamp(cursor.getLong(cursor.getColumnIndex(DBConstants.COL_TIMESTAMP)));
                message.setSenderName(cursor.getString(cursor.getColumnIndex(DBConstants.COL_SENDER_NAME)));
                message.setSenderId(cursor.getString(cursor.getColumnIndex(DBConstants.COL_SENDER_ID)));
                message.setHangoutId(cursor.getLong(cursor.getColumnIndex(DBConstants.COL_HANGOUT_ID)));
                message.setStatus(cursor.getInt(cursor.getColumnIndex(DBConstants.COL_STATUS)));
                message.setType(cursor.getInt(cursor.getColumnIndex(DBConstants.COL_TYPE)));
                message.setHangoutTheme(cursor.getString(cursor.getColumnIndex(DBConstants.COL_THEME)));
                return message;
            } else {
                return null;
            }
        } catch (SQLiteException e){
            e.printStackTrace();
            return null;
        } finally {
            cursor.close();
            mHelper.close();
            db.close();
        }
    }

    @SuppressWarnings("TryFinallyCanBeTryWithResources")
    public void updateStatusMessage(long messageId, int status) {
        SQLiteDatabase db = mHelper.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(DBConstants.COL_STATUS, status);
        String[] selectionToUpdate = {"" + messageId};
        try {
            db.update(DBConstants.MESSAGES_TABLE + mProfileId,
                    values, DBConstants.COL_ID + " = ?", selectionToUpdate);
        } catch (SQLiteException e) {
            e.printStackTrace();
        } finally {
            db.close();
        }
    }

    @SuppressWarnings("TryFinallyCanBeTryWithResources")
    public void deleteMessage(long id) {
        SQLiteDatabase db = mHelper.getWritableDatabase();
        String[] selection_args = {"" + id};
        try {
            db.delete(DBConstants.MESSAGES_TABLE + mProfileId,
                    DBConstants.COL_ID + " = ?", selection_args);
        } catch (SQLiteException e) {
            e.printStackTrace();
        } finally {
            db.close();
        }
    }

    @SuppressWarnings("TryFinallyCanBeTryWithResources")
    public void deleteRequests(long hangoutId){
        SQLiteDatabase db = mHelper.getWritableDatabase();
        String[] selection_args = {"" + hangoutId, "" + Message.TYPE_JOIN};
        try {
            db.delete(DBConstants.MESSAGES_TABLE + mProfileId,
                    DBConstants.COL_ID + " = ? AND " +
                    DBConstants.COL_TYPE + " = ?", selection_args);
        } catch (SQLiteException e) {
            e.printStackTrace();
        } finally {
            db.close();
        }
    }

    @SuppressWarnings("TryFinallyCanBeTryWithResources")
    public void deleteMessagesOfHangout(long hangoutId) {
        SQLiteDatabase db = mHelper.getWritableDatabase();
        String[] selection_args = {"" + hangoutId};
        try {
            db.delete(DBConstants.MESSAGES_TABLE + mProfileId,
                    DBConstants.COL_HANGOUT_ID + " = ?", selection_args);
        } catch (SQLiteException e) {
            e.printStackTrace();
        } finally {
            db.close();
        }
    }

    @SuppressWarnings("TryFinallyCanBeTryWithResources")
    public List<Message> getAllMessagesOfHangout(long hangoutId){
        List<Message> messages = new ArrayList<>();
        SQLiteDatabase db = mHelper.getReadableDatabase();
        Cursor cursor = db.query(DBConstants.MESSAGES_TABLE +
                mProfileId, null, DBConstants.COL_HANGOUT_ID + " = ? AND " +
                DBConstants.COL_TYPE + " != ?",
                new String[]{hangoutId + "", Message.TYPE_JOIN + ""}, null, null,
                DBConstants.COL_TIMESTAMP + " DESC");
        try {
            while(cursor.moveToNext()) {
                Message message = new Message();
                message.setId(cursor.getLong(cursor.getColumnIndex(DBConstants.COL_ID)));
                message.setContent(cursor.getString(cursor.getColumnIndex(DBConstants.COL_CONTENT)));
                message.setTimestamp(cursor.getLong(cursor.getColumnIndex(DBConstants.COL_TIMESTAMP)));
                message.setSenderName(cursor.getString(cursor.getColumnIndex(DBConstants.COL_SENDER_NAME)));
                message.setSenderId(cursor.getString(cursor.getColumnIndex(DBConstants.COL_SENDER_ID)));
                message.setHangoutId(cursor.getLong(cursor.getColumnIndex(DBConstants.COL_HANGOUT_ID)));
                message.setStatus(cursor.getInt(cursor.getColumnIndex(DBConstants.COL_STATUS)));
                message.setType(cursor.getInt(cursor.getColumnIndex(DBConstants.COL_TYPE)));
                message.setHangoutTheme(cursor.getString(cursor.getColumnIndex(DBConstants.COL_THEME)));
                messages.add(message);
            }
        } catch (SQLiteException e) {
            e.printStackTrace();
        } finally {
            cursor.close();
            mHelper.close();
            db.close();
        }
        return messages;
    }

}
