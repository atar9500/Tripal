package com.atar.tripal.objects;

import java.util.Calendar;

public class Message {

    public static final int STATUS_SENDING = 111;
    public static final int STATUS_SENT = 222;

    public static final int TYPE_MESSAGE = -99;
    public static final int TYPE_JOIN = -88;
    public static final int TYPE_LEFT = -77;
    public static final int TYPE_JOINED = -66;
    public static final int TYPE_PLACE = -55;

    private long id;

    private long timestamp;

    private long hangoutId;

    private int status;
    private int type;

    private String content;
    private String hangoutTheme;
    private String senderName;
    private String senderId;

    public Message() {}

    public long getId() {
        return id;
    }
    public void setId(long id) {
        this.id = id;
    }

    public String getContent() {
        return content;
    }
    public void setContent(String content) {
        this.content = content;
    }

    public long getTimestamp(){
        return timestamp;
    }
    public void setTimestamp(long timestamp){
        this.timestamp = timestamp;
    }

    public int getType(){
        return type;
    }
    public void setType(int type){
        this.type = type;
    }

    public long getHangoutId(){
        return hangoutId;
    }
    public void setHangoutId(long hangoutId){
        this.hangoutId = hangoutId;
    }

    public String getHangoutTheme() {
        return hangoutTheme;
    }
    public void setHangoutTheme(String hangoutTheme) {
        this.hangoutTheme = hangoutTheme;
    }

    public String getSenderId() {
        return senderId;
    }
    public void setSenderId(String senderId) {
        this.senderId = senderId;
    }

    public int getStatus() {
        return status;
    }
    public void setStatus(int status) {
        this.status = status;
    }

    public String getSenderName() {
        return senderName;
    }
    public void setSenderName(String senderName) {
        this.senderName = senderName;
    }

    public String getTime() {
        Calendar start = Calendar.getInstance();
        start.setTimeInMillis(timestamp);
        String min = start.get(Calendar.MINUTE) + "";
        if(min.length() == 1){
            min = 0 + min;
        }
        String hour = start.get(Calendar.HOUR_OF_DAY) + "";
        if(hour.length() == 1){
            hour = 0 + hour;
        }
        return hour + ":" + min;
    }

    public String getDate(boolean showYear) {
        Calendar start = Calendar.getInstance();
        start.setTimeInMillis(timestamp);
        String day = start.get(Calendar.DAY_OF_MONTH) + "";
        if(day.length() == 1){
            day = '0' + day;
        }
        String month = (start.get(Calendar.MONTH) + 1) + "";
        if(month.length() == 1){
            month = '0' + month;
        }
        if(showYear){
            return day + "/" + month + "/" + start.get(Calendar.YEAR);
        } else {
            return month + "/" + day;
        }
    }

    public String getYear() {
        Calendar start = Calendar.getInstance();
        start.setTimeInMillis(timestamp);
        return start.get(Calendar.YEAR) + "";
    }

}
