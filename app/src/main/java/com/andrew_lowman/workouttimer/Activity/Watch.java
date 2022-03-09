package com.andrew_lowman.workouttimer.Activity;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.media.MediaPlayer;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.IBinder;
import android.view.MenuItem;
import android.view.View;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.andrew_lowman.workouttimer.R;
import com.andrew_lowman.workouttimer.Service.timerService;
import com.andrew_lowman.workouttimer.Service.watchService;
import com.andrew_lowman.workouttimer.ui.WatchAdapter;
import com.google.android.material.navigation.NavigationBarView;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class Watch extends AppCompatActivity {
    private watchService mWatchService;
    private boolean mIsBound;
    private Button startButton;
    private Button intervalButton;
    private Button resetButton;
    private TextView timerTextView;
    private TextView intervalTextView;
    private List<String> times;
    private WatchAdapter watchAdapter;
    private boolean running = false;
    private boolean intervalTextViewVisible = false;
    public boolean intervalRunning = false;

    private final ServiceConnection mConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            mWatchService = ((watchService.watchBinder)service).getService();
            Toast.makeText(Watch.this,
                    "Making connection watch",
                    Toast.LENGTH_SHORT).show();
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            mWatchService = null;
            Toast.makeText(Watch.this,"Disconnecting Watch", Toast.LENGTH_SHORT).show();
        }
    };

    void doBindService(){
        bindService(new Intent(Watch.this,watchService.class),mConnection, Context.BIND_AUTO_CREATE);
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
        try{
            unregisterReceiver(mainReceiver);
            unregisterReceiver(secondReceiver);
        }catch(Exception e){
            e.printStackTrace();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        try{
            unregisterReceiver(mainReceiver);
            unregisterReceiver(secondReceiver);
        }catch(Exception e){
            e.printStackTrace();
        }
    }
    @Override
    protected void onPostResume() {
        super.onPostResume();
        //intent filter to catch time updates
        IntentFilter mainFilter = new IntentFilter();
        mainFilter.addAction("MainCounter");
        registerReceiver(mainReceiver,mainFilter);
        IntentFilter secondFilter = new IntentFilter();
        secondFilter.addAction("SecondCounter");
        registerReceiver(secondReceiver,secondFilter);
    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_watch);
        doBindService();
        ActivityCompat.requestPermissions(this,new String[]{Manifest.permission.FOREGROUND_SERVICE}, PackageManager.PERMISSION_GRANTED);
        times = new ArrayList<>();
        startButton = findViewById(R.id.watchStartButton);
        intervalButton = findViewById(R.id.watchIntervalButton);
        resetButton = findViewById(R.id.watchCancelButton);
        timerTextView = findViewById(R.id.watchTimerTextView);
        intervalTextView = findViewById(R.id.watchIntervalTextView);

        NavigationBarView navigationView = findViewById(R.id.bottomNavigation);
        navigationView.setOnItemSelectedListener(new NavigationBarView.OnItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(@NonNull MenuItem item) {
                Intent intent;
                switch(item.getItemId()){
                    case R.id.timer_menu_item:
                        intent =  new Intent(Watch.this,Timer.class);
                        startActivity(intent);
                        return true;
                    case R.id.intervals_menu_item:
                        intent = new Intent(Watch.this,Intervals.class);
                        startActivity(intent);
                        return true;
                    case R.id.stopwatch_menu_item:
                        return true;
                }
                return false;
            }
        });

        RecyclerView recyclerView = findViewById(R.id.watchRecyclerView);
        watchAdapter = new WatchAdapter(this);
        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(this);
        linearLayoutManager.setReverseLayout(true);
        linearLayoutManager.setStackFromEnd(true);
        recyclerView.setLayoutManager(linearLayoutManager);
        recyclerView.setAdapter(watchAdapter);
        watchAdapter.setTimes(times);

        startButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(!running){
                    mWatchService.startStopwatch();
                    running = true;
                    startButton.setText("Pause");
                }else{
                    mWatchService.pauseStopwatch();
                    running = false;
                    startButton.setText("Start");
                }
            }
        });

        intervalButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(running){
                    if(!intervalTextViewVisible){
                        intervalTextView.setVisibility(View.VISIBLE);
                        intervalTextViewVisible = true;
                    }
                    mWatchService.interval();
                    if(intervalRunning){
                        times.add(intervalTextView.getText().toString());
                    }else{
                        times.add(timerTextView.getText().toString());
                    }
                    watchAdapter.notifyDataSetChanged();
                    intervalRunning = true;
                }
            }
        });

        resetButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mWatchService.cancelStopwatch();
                startButton.setText("Start");
                times.clear();
                running = false;
                intervalRunning = false;
                intervalTextViewVisible = false;
                timerTextView.setText("00:00:000");
                intervalTextView.setText("00:00:000");
                intervalTextView.setVisibility(View.INVISIBLE);
                watchAdapter.notifyDataSetChanged();
            }
        });
    }

    BroadcastReceiver mainReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            long time = intent.getLongExtra("timeRemaining",0);
            timerTextView.setText(convert(time));
        }
    };

    BroadcastReceiver secondReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            long time = intent.getLongExtra("secondTimeRemaining",0);
            intervalTextView.setText(convert(time));
        }
    };

    public String convert(long time){
        int hours = (int) ((time / (1000*60*60)) % 24);
        int minutes = (int) ((time / (1000 * 60)) % 60);
        int secs = (int) (time / 1000) % 60;
        int mils = (int) time % 1000;
        /*int secs = (int) time / 1000;
        int minutes = secs / 60;
        secs = secs % 60;
        int mils = (int) time % 1000;*/

        if(hours==0){
            return String.format(Locale.getDefault(), "%02d:%02d:%03d", minutes, secs,mils);
        }else{
            return String.format(Locale.getDefault(), "%02d:%02d:%02d:%03d", hours, minutes, secs,mils);
        }
    }

}