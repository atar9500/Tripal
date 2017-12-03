package com.atar.tripal.net;

import android.app.IntentService;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.support.annotation.NonNull;
import android.support.v4.content.LocalBroadcastManager;
import android.widget.Toast;

import com.atar.tripal.R;
import com.atar.tripal.db.DBConstants;
import com.atar.tripal.db.DBHandler;
import com.atar.tripal.objects.User;
import com.atar.tripal.objects.Hangout;
import com.atar.tripal.objects.Message;
import com.atar.tripal.objects.Result;

import retrofit2.Call;
import retrofit2.Callback;


public class RequestsService extends IntentService {

    public static final String BROADCAST_IDENTIFIER_FOR_SERVICE_FINISHED_RESPONSE =
            "com.atar.tripal.net.RequestsService.IdentifierForRequestsService";

    private static final String TAG = "RequestsService";

    public static final String REQUEST = "request";
    public static final int ACCEPT = 111;
    public static final int DENY = 222;

    public RequestsService() {
        super(TAG);
    }

    @Override
    protected void onHandleIntent(final Intent intent) {
        if (intent != null) {
            final DBHandler handler = new DBHandler(this);
            NotificationManager notificationManager =
                    (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
            final Message message = handler.getMessage(intent.getLongExtra
                    (DBConstants.COL_ID, -1));
            int j = intent.getIntExtra(REQUEST, -1);
            final int pos = intent.getIntExtra(NetConstants.POSITION, -1);
            switch(j){
                case ACCEPT:
                    Hangout hangout = new Hangout();
                    hangout.setId(message.getHangoutId());
                    hangout.getFriends().add(new User(message.getSenderId()));
                    ApiInterface apiInterface = ApiClient.getAPIClient().create(ApiInterface.class);
                    Call<Result> call = apiInterface.addToHangout(hangout);
                    call.enqueue(new Callback<Result>() {
                        @Override
                        public void onResponse(@NonNull Call<Result> call,
                                @NonNull retrofit2.Response<Result> response) {
                            Result result = response.body();
                            if(response.isSuccessful() && result != null){
                                switch(result.getResult()){
                                    case NetConstants.RESULT_SUCCESS:
                                        handler.deleteMessage(message.getId());
                                        Toast.makeText(RequestsService.this, message.getSenderName() +
                                                " " + getString(R.string.joined_the_hangout), Toast.LENGTH_SHORT).show();
                                        message.setType(Message.TYPE_JOINED);
                                        handler.addMessage(message);
                                        break;
                                    case NetConstants.RESULT_FULL:
                                        Toast.makeText(RequestsService.this, R.string.hangout_full,
                                                Toast.LENGTH_SHORT).show();
                                        break;
                                    case NetConstants.RESULT_NOT_FOUND:
                                        Toast.makeText(RequestsService.this, R.string.hangout_not_found,
                                                Toast.LENGTH_SHORT).show();
                                        break;
                                    case NetConstants.RESULT_NOT_ACTIVE:
                                        handler.deleteRequests(message.getHangoutId());
                                        Toast.makeText(RequestsService.this, R.string.hangout_not_active,
                                                Toast.LENGTH_SHORT).show();
                                        break;
                                    case NetConstants.RESULT_ALREADY_THERE:
                                        handler.deleteMessage(message.getId());
                                        Toast.makeText(RequestsService.this, message.getSenderName() + " " +
                                                getString(R.string.hangout_already_there), Toast.LENGTH_SHORT).show();
                                        message.setType(Message.TYPE_JOINED);
                                        handler.addMessage(message);
                                        break;
                                }
                                Intent broadcastIntent = new Intent(BROADCAST_IDENTIFIER_FOR_SERVICE_FINISHED_RESPONSE);
                                broadcastIntent.putExtra(DBConstants.COL_ID, message.getId());
                                broadcastIntent.putExtra(NetConstants.RESULT, result.getResult());
                                broadcastIntent.putExtra(NetConstants.POSITION, pos);
                                LocalBroadcastManager.getInstance(RequestsService.this)
                                        .sendBroadcast(broadcastIntent);
                            } else {
                                Toast.makeText(RequestsService.this, R.string.went_wrong,
                                        Toast.LENGTH_SHORT).show();
                                Intent broadcastIntent = new Intent(BROADCAST_IDENTIFIER_FOR_SERVICE_FINISHED_RESPONSE);
                                broadcastIntent.putExtra(DBConstants.COL_ID, message.getId());
                                broadcastIntent.putExtra(NetConstants.RESULT, NetConstants.RESULT_FAILED);
                                broadcastIntent.putExtra(NetConstants.POSITION, pos);
                                LocalBroadcastManager.getInstance(RequestsService.this)
                                        .sendBroadcast(broadcastIntent);
                            }
                        }

                        @Override
                        public void onFailure(@NonNull Call<Result> call, @NonNull Throwable t) {
                            Toast.makeText(RequestsService.this, getString(R.string.no_connection),
                                    Toast.LENGTH_SHORT).show();
                            Intent broadcastIntent = new Intent(BROADCAST_IDENTIFIER_FOR_SERVICE_FINISHED_RESPONSE);
                            broadcastIntent.putExtra(DBConstants.COL_ID, message.getId());
                            broadcastIntent.putExtra(NetConstants.RESULT, NetConstants.RESULT_FAILED);
                            broadcastIntent.putExtra(NetConstants.POSITION, pos);
                            LocalBroadcastManager.getInstance(RequestsService.this)
                                    .sendBroadcast(broadcastIntent);
                        }
                    });
                    break;
                case DENY:
                    handler.deleteMessage(message.getId());
                    break;
            }
            if(notificationManager != null){
                int i;
                try{
                    i = ((int)message.getHangoutId()) + message.getType();
                } catch (Exception e){
                    i = 0;
                }
                notificationManager.cancel(i);
            }
        }
    }

}
