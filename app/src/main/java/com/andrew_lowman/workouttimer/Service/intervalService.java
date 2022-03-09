package com.andrew_lowman.workouttimer.Service;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.Build;
import android.os.CountDownTimer;
import android.os.Handler;
import android.os.IBinder;
import android.os.SystemClock;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

import com.andrew_lowman.workouttimer.Activity.Intervals;
import com.andrew_lowman.workouttimer.Model.modelService;
import com.andrew_lowman.workouttimer.R;

import java.util.Locale;

public class intervalService extends Service implements modelService {
    private final IBinder mBinder = new LocalBinder();

    private static final String CHANNEL_ID = "IntervalChannel";
    private long milliseconds;
    private long originalTime = 250000;
    private CountDownTimer ct;
    private long startTime;
    private long updateTime;
    private long currentTime;
    final Handler mainHandler = new Handler();
    private boolean started = false;
    private boolean running = false;

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    //set service to binder
    public class LocalBinder extends Binder{
        public intervalService getService(){
            return intervalService.this;
        }
    }

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

    //STOPWATCH STUFF******************************************************************
    //
    //
    //start stopwatch
    public void startStopwatch() {
        //set start from system clock
        startTime = SystemClock.elapsedRealtime();
        //start handler with the runnable
        mainHandler.postDelayed(mainRunnable,0);
        //current time
        currentTime += milliseconds;
    }

    @Override
    public void pauseStopwatch() {
        mainHandler.removeCallbacks(mainRunnable);
        running = false;
    }

    @Override
    public void cancelStopwatch() {
        pauseStopwatch();
        milliseconds = 0L;
        startTime = 0L;
        updateTime = 0L;
        currentTime = 0L;
        running = false;
    }


    Runnable mainRunnable = new Runnable() {
        @Override
        public void run() {
            //subtract start from current clock
            milliseconds = SystemClock.elapsedRealtime() - startTime;
            //add to
            updateTime = milliseconds + currentTime;
            //intent for broadcast to activity
            Intent intentLocal = new Intent();
            intentLocal.setAction("IntervalTimerCounter");
            intentLocal.putExtra("timeRemaining",updateTime);
            sendBroadcast(intentLocal);
            //update notification with same time
            NotificationUpdate(updateTime);

            mainHandler.postDelayed(this, 0);
        }
    };

    //TIMER STUFF*************************************************
    //
    //
    //
    @Override
    public void startCountdownTimer() {
        System.out.println("Start CT Timer");
        ct = new CountDownTimer(originalTime, 1) {

            @Override
            public void onTick(long millisUntilFinished) {
                //set millis global
                milliseconds = millisUntilFinished;
                //intent for broadcast and notification
                Intent intentLocal = new Intent();
                intentLocal.setAction("IntervalTimerCounter");
                intentLocal.putExtra("timeRemaining",milliseconds);
                sendBroadcast(intentLocal);
                NotificationUpdate(milliseconds);
            }

            @Override
            public void onFinish() {
                Intent timerDone = new Intent();
                timerDone.setAction("IntervalTimerDone");
                sendBroadcast(timerDone);
            }
        };
        ct.start();
    }

    @Override
    public void pauseCountdownTimer() {
        if(ct != null){
            ct.cancel();
        }
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
                intentLocal.setAction("IntervalTimerCounter");
                intentLocal.putExtra("timeRemaining",milliseconds);
                sendBroadcast(intentLocal);
                NotificationUpdate(milliseconds);
            }

            @Override
            public void onFinish() {
                Intent timerDone = new Intent();
                timerDone.setAction("IntervalTimerDone");
                sendBroadcast(timerDone);
            }
        };
        ct.start();
    }

    @Override
    public void cancelCountdownTimer() {
        ct.cancel();
        //set time to starting time
        Intent intentLocal = new Intent();
        intentLocal.setAction("IntervalTimerCounter");
        intentLocal.putExtra("timeRemaining",originalTime);
        sendBroadcast(intentLocal);
    }

    public void setOriginalTime(long originalTime){
        this.originalTime = originalTime;
    }

    //NOTIFICATION STUFF*****************************


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
                NotificationChannel notificationChannel = new NotificationChannel(CHANNEL_ID,"My Counter Service", NotificationManager.IMPORTANCE_LOW);
                NotificationManager notificationManager = getSystemService(NotificationManager.class);
                notificationManager.createNotificationChannel(notificationChannel);
            }
            startForeground(1,notifications[0]);


        }catch(Exception e){
            e.printStackTrace();
        }


    }


}
