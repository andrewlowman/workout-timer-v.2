package com.andrew_lowman.workouttimer.Activity;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.IBinder;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.andrew_lowman.workouttimer.R;
import com.andrew_lowman.workouttimer.Service.intervalService;
import com.google.android.material.navigation.NavigationBarView;

import java.util.Locale;

public class Intervals extends AppCompatActivity {
    private intervalService mBoundService;
    private boolean mIsBound;

    private TextView countdownTextView;
    private Button startButton;
    
    
    private final ServiceConnection mConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            mBoundService = ((intervalService.LocalBinder)service).getService();

            Toast.makeText(Intervals.this,
                    "Making connection",
                    Toast.LENGTH_SHORT).show();
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            mBoundService = null;
            Toast.makeText(Intervals.this,"Disconnected",Toast.LENGTH_SHORT).show();
        }
    };

    void doBindService(){
        bindService(new Intent(Intervals.this, intervalService.class),mConnection, Context.BIND_AUTO_CREATE);
        mIsBound = true;
    }

    void doUnbindService(){
        if(mIsBound){
            unbindService(mConnection);
            mIsBound = false;
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        doUnbindService();
        unregisterReceiver(timeUpdateReceiver);
    }

    @Override
    protected void onPause() {
        super.onPause();
        unregisterReceiver(timeUpdateReceiver);
    }

    @Override
    protected void onPostResume() {
        super.onPostResume();
        //intent filter to catch time updates
        IntentFilter timeUpdateFilter = new IntentFilter();
        timeUpdateFilter.addAction("Counter");
        registerReceiver(timeUpdateReceiver,timeUpdateFilter);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_intervals);
        ActivityCompat.requestPermissions(this,new String[]{Manifest.permission.FOREGROUND_SERVICE}, PackageManager.PERMISSION_GRANTED);
        //bind to service;
        doBindService();
        //
        countdownTextView = findViewById(R.id.countdownTextView);

        startButton = findViewById(R.id.intervalStartButton);
        //registerReceiver(timeUpdateReceiver,timeUpdateFilter);

        NavigationBarView navigationView = findViewById(R.id.bottomNavigation);
        navigationView.setOnItemSelectedListener(new NavigationBarView.OnItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(@NonNull MenuItem item) {
                Intent intent;
                switch(item.getItemId()){
                    case R.id.stopwatch_menu_item:
                        intent =  new Intent(Intervals.this,Watch.class);
                        startActivity(intent);
                        return true;
                    case R.id.timer_menu_item:
                        intent = new Intent(Intervals.this,Timer.class);
                        startActivity(intent);
                        return true;
                }
                return false;
            }
        });

        startButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mBoundService.startCountdownTimer();
            }
        });
    }


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

    //receiver for time updates
    BroadcastReceiver timeUpdateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            long time = intent.getLongExtra("timeRemaining",0);
            countdownTextView.setText(convert(time));
        }
    };
}