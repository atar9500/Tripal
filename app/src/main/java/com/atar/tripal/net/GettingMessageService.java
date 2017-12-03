package com.atar.tripal.net;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.media.RingtoneManager;
import android.net.Uri;
import android.support.v4.app.NotificationCompat;
import android.support.v4.content.LocalBroadcastManager;

import com.atar.tripal.R;
import com.atar.tripal.db.DBConstants;
import com.atar.tripal.db.DBHandler;
import com.atar.tripal.db.Details;
import com.atar.tripal.objects.Message;
import com.atar.tripal.ui.HangoutActivity;
import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

import java.util.Map;

public class GettingMessageService extends FirebaseMessagingService {

    public static final String BROADCAST_IDENTIFIER_FOR_SERVICE_FINISHED_RESPONSE =
            "com.atar.tripal.net.GettingMessageService.IdentifierForGettingMessageService";

    private static final String TAG = "GettingMessageService";

    @Override
    public void onMessageReceived(RemoteMessage remoteMessage) {

        Map<String, String> data = remoteMessage.getData();

        Message message = new Message();
        message.setType(Integer.parseInt(data.get(DBConstants.COL_TYPE)));
        message.setHangoutId(Long.parseLong(data.get(DBConstants.COL_HANGOUT_ID)));
        message.setStatus(Message.STATUS_SENT);
        message.setSenderId(data.get(DBConstants.COL_SENDER_ID));
        message.setTimestamp(System.currentTimeMillis());
        message.setHangoutTheme(data.get(DBConstants.COL_THEME));
        message.setSenderName(data.get(DBConstants.COL_SENDER_NAME));
        message.setContent(data.get(DBConstants.COL_CONTENT));

        DBHandler handler = new DBHandler(this);
        message.setId(handler.addMessage(message));

        notifyMessaging(message);

        Intent broadcastIntent = new Intent(BROADCAST_IDENTIFIER_FOR_SERVICE_FINISHED_RESPONSE);
        broadcastIntent.putExtra(DBConstants.COL_ID, message.getId());
        broadcastIntent.putExtra(DBConstants.COL_TYPE, message.getType());
        broadcastIntent.putExtra(DBConstants.COL_HANGOUT_ID, message.getHangoutId());
        LocalBroadcastManager.getInstance(this).sendBroadcast(broadcastIntent);
    }

    private void notifyMessaging(Message message) {

        Intent intent = new Intent(this , HangoutActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        intent.putExtra(DBConstants.COL_HANGOUT_ID, message.getHangoutId());

        Uri notificationSoundURI = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);

        String s = "";

        NotificationCompat.Builder mNotificationBuilder = new NotificationCompat.Builder
                (this, message.getHangoutId() + "")
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentTitle(getString(R.string.going_to) + " " + message.getHangoutTheme())
                .setAutoCancel(true)
                .setSound(notificationSoundURI);

        switch(message.getType()){
            case Message.TYPE_JOIN:
                intent.putExtra(HangoutActivity.REQUEST_CODE, HangoutActivity.PRESS_REQUEST);

                s = message.getSenderName() + " " + getString(R.string.join_message);

                Intent leaveIntent = new Intent(this , RequestsService.class);
                leaveIntent.putExtra(RequestsService.REQUEST, RequestsService.DENY);
                leaveIntent.putExtra(DBConstants.COL_ID, message.getId());
                mNotificationBuilder.addAction(R.mipmap.close_small, getString(android.R.string.cancel),
                        PendingIntent.getService(this, RequestsService.DENY, leaveIntent,
                                PendingIntent.FLAG_UPDATE_CURRENT));

                Intent joinIntent = new Intent(this , RequestsService.class);
                joinIntent.putExtra(RequestsService.REQUEST, RequestsService.ACCEPT);
                joinIntent.putExtra(DBConstants.COL_ID, message.getId());
                mNotificationBuilder.addAction(R.mipmap.join_small, getString(R.string.accept),
                        PendingIntent.getService(this, RequestsService.ACCEPT, joinIntent,
                                PendingIntent.FLAG_UPDATE_CURRENT));
                break;
            case Message.TYPE_LEFT:
                intent.putExtra(HangoutActivity.REQUEST_CODE, HangoutActivity.PRESS_NOTIFICATION);
                s = message.getSenderName() + " " + getString(R.string.left_message);
                break;
            case Message.TYPE_JOINED:
                intent.putExtra(HangoutActivity.REQUEST_CODE, HangoutActivity.PRESS_NOTIFICATION);
                if(message.getSenderName().equals(Details.getUsername(this))){
                    s = getString(R.string.you_are_in);
                } else {
                    s = message.getSenderName() + " " + getString(R.string.joined_message);
                }
                break;
            case Message.TYPE_MESSAGE:
                intent.putExtra(HangoutActivity.REQUEST_CODE, HangoutActivity.PRESS_NOTIFICATION);
                s = message.getSenderName() + ": " + message.getContent();
                break;
            case Message.TYPE_PLACE:
                intent.putExtra(HangoutActivity.REQUEST_CODE, HangoutActivity.PRESS_NOTIFICATION);
                s = message.getSenderName() + " " + getString(R.string.changed_place_to) +
                        " " + message.getContent();
                break;
        }

        PendingIntent resultIntent = PendingIntent.getActivity(this, 0, intent,
                PendingIntent.FLAG_ONE_SHOT);
        mNotificationBuilder.setContentIntent(resultIntent)
                .setContentText(s);

        NotificationManager notificationManager =
                (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

        if(notificationManager != null){
            int i;
            try{
                i = ((int)message.getHangoutId()) + message.getType();
            } catch (Exception e){
                i = 0;
            }
            notificationManager.notify(i, mNotificationBuilder.build());
        }

    }

}
