package com.andrew_lowman.workouttimer.Activity;

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import android.Manifest;
import android.app.Activity;
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
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.andrew_lowman.workouttimer.R;
import com.andrew_lowman.workouttimer.Service.timerService;
import com.google.android.material.navigation.NavigationBarView;

import java.util.Locale;

public class Timer extends AppCompatActivity {
    private TextView timerTextView;
    private Button startButton;
    private Button addButton;
    private Button resetButton;
    private timerService mTimerService;
    private boolean mIsBound;
    private Intent intentService;
    private boolean started = false;
    private boolean ready = false;
    private boolean running = false;
    private long originalTime;
    private Animation blinkingAnimation;
    private Uri notification;
    private MediaPlayer mp;
    private ActivityResultLauncher<Intent> loadRingtoneActivityLauncher;

    private final ServiceConnection mConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            mTimerService = ((timerService.timerBinder)service).getService();
            Toast.makeText(Timer.this,
                    "Making connection 2",
                    Toast.LENGTH_SHORT).show();
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            mTimerService = null;
            Toast.makeText(Timer.this,"Disconnecting 2", Toast.LENGTH_SHORT).show();
        }
    };

    void doBindService(){
        bindService(new Intent(Timer.this,timerService.class),mConnection, Context.BIND_AUTO_CREATE);
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
        unregisterReceiver(broadcastReceiver);
        unregisterReceiver(timerDoneReceiver);
    }

    @Override
    protected void onPause() {
        super.onPause();
        unregisterReceiver(broadcastReceiver);
        unregisterReceiver(timerDoneReceiver);
    }

    @Override
    protected void onPostResume() {
        super.onPostResume();
        //intent filter to catch time updates
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction("TimerCounter");
        registerReceiver(broadcastReceiver,intentFilter);
        IntentFilter timerDoneFilter = new IntentFilter();
        timerDoneFilter.addAction("TimerDone");
        registerReceiver(timerDoneReceiver,timerDoneFilter);
    }


    @RequiresApi(api = Build.VERSION_CODES.P)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_timer);
        ActivityCompat.requestPermissions(this,new String[]{Manifest.permission.FOREGROUND_SERVICE}, PackageManager.PERMISSION_GRANTED);
        doBindService();
        timerTextView = findViewById(R.id.countdownTimerTextView);
        startButton = findViewById(R.id.countdownTimerStartButton);
        addButton = findViewById(R.id.countdownTimerAddButton);
        resetButton = findViewById(R.id.countdownTimerResetButton);

        notification = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
        mp = MediaPlayer.create(getApplicationContext(),notification);

        blinkingAnimation = new AlphaAnimation(0.0f,1.0f);
        blinkingAnimation.setDuration(500);
        blinkingAnimation.setStartOffset(20);
        blinkingAnimation.setRepeatMode(Animation.REVERSE);
        blinkingAnimation.setRepeatCount(10);

        //bottom bar nav
        NavigationBarView navigationView = findViewById(R.id.bottomNavigation);
        navigationView.setOnItemSelectedListener(new NavigationBarView.OnItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(@NonNull MenuItem item) {
                Intent intent;
                switch(item.getItemId()){
                    case R.id.stopwatch_menu_item:
                        intent =  new Intent(Timer.this,Watch.class);
                        startActivity(intent);
                        return true;
                    case R.id.intervals_menu_item:
                        intent = new Intent(Timer.this,Intervals.class);
                        startActivity(intent);
                        return true;
                    case R.id.timer_menu_item:
                        return true;
                }
                return false;
            }
        });
        //registerReceiver(broadcastReceiver,intentFilter);

        ActivityResultLauncher<Intent> addTimerLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                new ActivityResultCallback<ActivityResult>() {
                    @Override
                    public void onActivityResult(ActivityResult result) {
                        if(result.getResultCode() == Activity.RESULT_OK){
                            Intent intent = result.getData();
                            originalTime = intent.getLongExtra("Milliseconds",0);
                            if(started){
                                mTimerService.pauseCountdownTimer();
                                started = false;
                                running = false;
                                startButton.setText("Start");
                            }
                            mTimerService.setOriginalTime(originalTime);
                            timerTextView.setText(convert(originalTime));
                           // intentService = new Intent(Timer.this,timerService.class);
                            //intentService.setAction("sendToTimerService");
                            //intentService.putExtra("originalTime",originalTime);
                            //bindService(intentService,mConnection,Context.BIND_AUTO_CREATE);
                            //mIsBound = true;
                            //sendBroadcast(intentService);
                            ready = true;
                        }
                    }
                }
        );

        loadRingtoneActivityLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                new ActivityResultCallback<ActivityResult>() {
                    @Override
                    public void onActivityResult(ActivityResult result) {
                        if(result.getResultCode() == Activity.RESULT_OK){
                            Intent intent = result.getData();
                            Uri uri = intent.getParcelableExtra(RingtoneManager.EXTRA_RINGTONE_PICKED_URI);
                            mp = MediaPlayer.create(getApplicationContext(),uri);

                        }
                    }
                }
        );


        startButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(ready){
                    //not started not running
                    if(!started){
                        mTimerService.startCountdownTimer();
                        started = true;
                        running = true;
                        startButton.setText("Pause");
                        System.out.println("Option 1");
                    }else {
                        //started running
                        if(running){
                            mTimerService.pauseCountdownTimer();
                            running = false;
                            startButton.setText("Start");
                            System.out.println("Option 2");
                            //started not running
                        }else{
                            mTimerService.restartCountdownTimer();
                            running = true;
                            startButton.setText("Pause");
                            System.out.println("Option 3");
                        }
                    }
                }
            }
        });

        resetButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(ready){
                    if(started){
                        mTimerService.cancelCountdownTimer();
                        started = false;
                        //timerTextView.setText(convert(originalTime));
                        running = false;
                        startButton.setText("Start");
                    }
                }
            }
        });

        addButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(Timer.this,AddTimer.class);
                addTimerLauncher.launch(intent);
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
    BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            long time = intent.getLongExtra("timeRemaining",0);
            timerTextView.setText(convert(time));
        }
    };

    BroadcastReceiver timerDoneReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            timerTextView.startAnimation(blinkingAnimation);
            mp.start();
        }
    };

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_stopwatch, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item){
        int id = item.getItemId();

        if(id == R.id.stopwatchRingtoneMenuItem){
            Intent loadRingtone = new Intent(RingtoneManager.ACTION_RINGTONE_PICKER);
            loadRingtone.putExtra(RingtoneManager.EXTRA_RINGTONE_TITLE,"Select alert tone");
            loadRingtone.putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_DEFAULT, true);
            loadRingtone.putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_SILENT, true);
            loadRingtone.putExtra(RingtoneManager.EXTRA_RINGTONE_TYPE, RingtoneManager.TYPE_NOTIFICATION);
            loadRingtoneActivityLauncher.launch(loadRingtone);
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

}