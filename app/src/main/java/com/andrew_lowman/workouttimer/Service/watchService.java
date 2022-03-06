package com.andrew_lowman.workouttimer.Service;

import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;

import androidx.annotation.Nullable;

import com.andrew_lowman.workouttimer.Model.modelService;

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
    public void startStopwatch() {

    }

    @Override
    public void restartStopwatch(long time) {

    }

    @Override
    public void pauseStopwatch() {

    }

    @Override
    public void cancelStopwatch() {

    }

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
