package com.andrew_lowman.workouttimer.Service;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.SystemClock;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

import com.andrew_lowman.workouttimer.Activity.Intervals;
import com.andrew_lowman.workouttimer.Model.modelService;
import com.andrew_lowman.workouttimer.R;

import java.util.Locale;

public class watchService extends Service implements modelService {
    private static final String CHANNEL_ID = "WatchChannel";
    private final IBinder mWatchBinder = new watchBinder();

    private long mainMillis;
    private long secondMillis;
    private long mainStartTime;
    private long mainUpdateTime;
    private long mainCurrentTime;
    private long secondStartTime;
    private long secondUpdateTime;
    private long secondCurrentTime;
    private boolean intervalRunning = false;
    private boolean running = false;
    final Handler mainHandler = new Handler();
    final Handler secondHandler = new Handler();

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return mWatchBinder;
    }

    public class watchBinder extends Binder{
        public watchService getService(){
            return watchService.this;
        }
    }

    @Override
    //start stopwatch
    public void startStopwatch() {
        mainStartTime = SystemClock.elapsedRealtime();
        mainHandler.postDelayed(mainRunnable,0);
        mainCurrentTime += mainMillis;
        running = true;
        if(intervalRunning){
            secondStartTime = SystemClock.elapsedRealtime();
            secondHandler.postDelayed(secondRunnable,0);
            secondCurrentTime += secondMillis;
        }
    }

    public void interval(){
        secondStartTime = SystemClock.elapsedRealtime();
        secondHandler.postDelayed(secondRunnable,0);
        intervalRunning = true;
    }

    @Override
    public void pauseStopwatch() {
        mainHandler.removeCallbacks(mainRunnable);
        running = false;
        if(intervalRunning){
            secondHandler.removeCallbacks(secondRunnable);
        }
    }

    @Override
    public void cancelStopwatch() {
        pauseStopwatch();
        mainMillis = 0L;
        mainStartTime = 0L;
        mainUpdateTime = 0L;
        mainCurrentTime = 0L;
        secondMillis = 0L;
        secondStartTime = 0L;
        secondUpdateTime = 0L;
        secondCurrentTime = 0L;
        running = false;
        intervalRunning = false;
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

    Runnable mainRunnable = new Runnable() {
        @Override
        public void run() {
            //subtract start from current clock
            mainMillis = SystemClock.elapsedRealtime() - mainStartTime;
            //add to
            mainUpdateTime = mainMillis + mainCurrentTime;
            //intent for broadcast to activity
            Intent intentLocal = new Intent();
            intentLocal.setAction("MainCounter");
            intentLocal.putExtra("timeRemaining",mainUpdateTime);
            sendBroadcast(intentLocal);
            //update notification with same time
            NotificationUpdate(mainUpdateTime);

            mainHandler.postDelayed(this, 0);
        }
    };

    Runnable secondRunnable = new Runnable() {
        @Override
        public void run() {
            secondMillis = SystemClock.elapsedRealtime() - secondStartTime;
            secondUpdateTime = secondMillis + secondCurrentTime;

            Intent secondIntent = new Intent();
            secondIntent.setAction("SecondCounter");
            secondIntent.putExtra("secondTimeRemaining",secondUpdateTime);
            sendBroadcast(secondIntent);
            NotificationUpdate(secondUpdateTime);
            secondHandler.postDelayed(this,0);
        }
    };

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


    //----------------------------------------------------------
    @Override
    public void cancelCountdownTimer() {

    }

    @Override
    public void startCountdownTimer() {

    }

    @Override
    public void pauseCountdownTimer() {

    }

    @Override
    public void restartCountdownTimer() {

    }


}
