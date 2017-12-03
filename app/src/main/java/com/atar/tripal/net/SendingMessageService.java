package com.atar.tripal.net;

import android.content.Intent;
import android.support.annotation.NonNull;
import android.support.v4.content.LocalBroadcastManager;

import com.atar.tripal.db.DBConstants;
import com.atar.tripal.db.DBHandler;
import com.atar.tripal.objects.Message;
import com.atar.tripal.objects.Result;
import com.firebase.jobdispatcher.JobParameters;
import com.firebase.jobdispatcher.JobService;

import retrofit2.Call;
import retrofit2.Callback;

public class SendingMessageService extends JobService {

    public static final String BROADCAST_IDENTIFIER_FOR_SERVICE_FINISHED_RESPONSE =
            "com.atar.tripal.net.SendingMessageService.IdentifierForSendingMessageService";

    private DBHandler mHandler;

    @Override
    public boolean onStartJob(final JobParameters job) {
        mHandler = new DBHandler(this);
        if(job.getExtras() != null){
            final long messageId = job.getExtras().getLong(DBConstants.COL_ID);
            Message message = mHandler.getMessage(messageId);
            if(message != null){
                ApiInterface apiInterface = ApiClient.getAPIClient().create(ApiInterface.class);
                Call<Result> call = apiInterface.sendMessage(message);
                call.enqueue(new Callback<Result>() {
                    @Override
                    public void onResponse(@NonNull Call<Result> call,
                                           @NonNull retrofit2.Response<Result> response) {
                        Result result = response.body();
                        if(response.isSuccessful() && result != null){
                            switch(result.getResult()){
                                case NetConstants.RESULT_SUCCESS:
                                    mHandler.updateStatusMessage(messageId, Message.STATUS_SENT);
                                    jobFinished(job, false);
                                    Intent broadcastIntent = new Intent
                                            (BROADCAST_IDENTIFIER_FOR_SERVICE_FINISHED_RESPONSE);
                                    broadcastIntent.putExtra(DBConstants.COL_ID, messageId);
                                    LocalBroadcastManager.getInstance(SendingMessageService.this)
                                            .sendBroadcast(broadcastIntent);
                                    break;
                                case NetConstants.RESULT_FAILED:
                                    jobFinished(job, true);
                                    break;
                                case NetConstants.RESULT_NOT_ACTIVE:
                                    jobFinished(job, false);
                                    mHandler.deleteMessage(messageId);
                                    Intent i = new Intent(BROADCAST_IDENTIFIER_FOR_SERVICE_FINISHED_RESPONSE);
                                    i.putExtra(DBConstants.COL_ID, -1);
                                    LocalBroadcastManager.getInstance(SendingMessageService.this)
                                            .sendBroadcast(i);
                                    break;
                            }
                        } else {
                            jobFinished(job, true);
                        }
                    }
                    @Override
                    public void onFailure(@NonNull Call<Result> call, @NonNull Throwable t) {
                        jobFinished(job, true);
                    }
                });
                return true;
            } else {
                return false;
            }
        } else {
            return false;
        }
    }

    @Override
    public boolean onStopJob(JobParameters job) {
        jobFinished(job, true);
        return true;
    }

}
