package com.andrew_lowman.workouttimer.Service;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Binder;
import android.os.Build;
import android.os.CountDownTimer;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

import com.andrew_lowman.workouttimer.Activity.Intervals;
import com.andrew_lowman.workouttimer.Model.modelService;
import com.andrew_lowman.workouttimer.R;

import java.util.Locale;

public class timerService extends Service implements modelService {
    private static final String CHANNEL_ID = "TimerChannel";
    private final IBinder mTimerBinder = new timerBinder();
    private long milliseconds;
    private long originalTime = 0;

    private CountDownTimer ct;

    private boolean started = false;

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        originalTime = intent.getLongExtra("originalTime",0);
        return mTimerBinder;
    }

    public class timerBinder extends Binder{
        public timerService getService(){
            return timerService.this;
        }
    }

   /* @Override
    public void onCreate() {
        super.onCreate();
            }*/

    //convert long to proper format
    public String convert(long time){
        int hours = (int) ((time / (1000*60*60)) % 24);
        int minutes = (int) ((time / (1000 * 60)) % 60);
        int secs = (int) (time / 1000) % 60;
        //int mils = (int) time % 1000;
        /*int secs = (int) time / 1000;
        int minutes = secs / 60;
        secs = secs % 60;
        int mils = (int) time % 1000;*/

        if(hours==0){
            return String.format(Locale.getDefault(), "%02d:%02d", minutes, secs);
        }else{
            return String.format(Locale.getDefault(), "%02d:%02d:%02d", hours, minutes, secs);
        }
    }

    @Override
    public void startCountdownTimer() {
        System.out.println("Start 2");
        System.out.println(originalTime);
        ct = new CountDownTimer(originalTime, 1) {

            @Override
            public void onTick(long millisUntilFinished) {
                //set millis global
                milliseconds = millisUntilFinished;
                //intent for broadcast and notification
                Intent intentLocal = new Intent();
                intentLocal.setAction("TimerCounter");
                intentLocal.putExtra("timeRemaining",milliseconds);
                sendBroadcast(intentLocal);
                NotificationUpdate(milliseconds);
            }

            @Override
            public void onFinish() {
                Intent timerDone = new Intent();
                timerDone.setAction("TimerDone");
                sendBroadcast(timerDone);
            }
        };
        ct.start();
    }

    @Override
    public void restartCountdownTimer() {
        ct = new CountDownTimer(milliseconds, 1) {

            @Override
            public void onTick(long millisUntilFinished) {
                //set millis global
                milliseconds = millisUntilFinished;
                //intent for broadcast and notification
                Intent intentLocal = new Intent();
                intentLocal.setAction("TimerCounter");
                intentLocal.putExtra("timeRemaining",milliseconds);
                sendBroadcast(intentLocal);
                NotificationUpdate(milliseconds);
            }

            @Override
            public void onFinish() {
                Intent timerDone = new Intent();
                timerDone.setAction("TimerDone");
                sendBroadcast(timerDone);
            }
        };
        ct.start();
    }

    public void setOriginalTime(long originalTime){
        this.originalTime = originalTime;
    }

    @Override
    public void pauseCountdownTimer() {
        ct.cancel();
    }

    @Override
    public void cancelCountdownTimer() {
        ct.cancel();
        //receiver sends last time so have to rest receiver time
        Intent intentLocal = new Intent();
        intentLocal.setAction("TimerCounter");
        intentLocal.putExtra("timeRemaining",originalTime);
        sendBroadcast(intentLocal);
    }

    public void NotificationUpdate(long timeLeft){
        try{
            Intent notificationIntent = new Intent(this, Intervals.class);
            final PendingIntent pendingIntent = PendingIntent.getActivity(this,0,notificationIntent,PendingIntent.FLAG_IMMUTABLE);
            final Notification[] notifications = {new NotificationCompat.Builder(this,CHANNEL_ID)
                    .setContentTitle("Workout Timer")
                    .setContentText(convert(timeLeft))
                    .setSmallIcon(R.drawable.ic_launcher_foreground)
                    .setContentIntent(pendingIntent)
                    .build()};

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O){
                NotificationChannel notificationChannel = new NotificationChannel(CHANNEL_ID,"Workout Timer", NotificationManager.IMPORTANCE_LOW);
                NotificationManager notificationManager = getSystemService(NotificationManager.class);
                notificationManager.createNotificationChannel(notificationChannel);
            }
            startForeground(1,notifications[0]);


        }catch(Exception e){
            e.printStackTrace();
        }


    }
    //------------------------------------------
    @Override
    public void pauseStopwatch() {

    }

    @Override
    public void cancelStopwatch() {

    }
    @Override
    public void restartStopwatch(long time) {

    }
    @Override
    public void startStopwatch() {

    }

}
