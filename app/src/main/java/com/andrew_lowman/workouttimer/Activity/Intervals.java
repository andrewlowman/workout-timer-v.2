package com.andrew_lowman.workouttimer.Activity;

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.Manifest;
import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.graphics.Paint;
import android.icu.text.SimpleDateFormat;
import android.media.MediaPlayer;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.IBinder;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.andrew_lowman.workouttimer.Entities.IntervalsEntity;
import com.andrew_lowman.workouttimer.Entities.ReportEntity;
import com.andrew_lowman.workouttimer.R;
import com.andrew_lowman.workouttimer.Service.intervalService;
import com.andrew_lowman.workouttimer.ViewModel.IntervalsViewModel;
import com.andrew_lowman.workouttimer.ViewModel.ReportsViewModel;
import com.andrew_lowman.workouttimer.ui.IntervalsAdapter;
import com.google.android.material.navigation.NavigationBarView;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class Intervals extends AppCompatActivity {
    private intervalService mBoundService;
    private boolean mIsBound;
    private IntervalsAdapter intervalsAdapter;
    private List<Long> longTimes = new ArrayList<>();
    private List<String> stringTimes = new ArrayList<>();
    private List<CountDownTimer> timers = new ArrayList<>();
    private List<Long> backupTimes = new ArrayList<>();

    private TextView countdownTextView;
    private TextView nameTextView;
    private Button resetButton;
    private Button startButton;
    private Button newButton;
    private Animation blinkingAnimation;
    private Uri notification;
    private MediaPlayer mp;

    private ReportsViewModel reportsViewModel;
    private List<ReportEntity> reports = new ArrayList<>();
    private List<IntervalsEntity> intervals = new ArrayList<>();
    private ActivityResultLauncher<Intent> loadRingtoneActivityLauncher;
    private ActivityResultLauncher<Intent> loadIntervalActivityLauncher;

    private boolean running = false;
    private boolean started = false;
    private int longTimesCounter = 0;
    private int timersCounter = 0;
    private boolean ready = false;
    private boolean stopwatch = false;

    private  int intervalID;
    private String intervalName = "";

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
        try{
            unregisterReceiver(timeUpdateReceiver);
            unregisterReceiver(endTimeReceiver);
        }catch(Exception e){
            e.printStackTrace();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        try{
            unregisterReceiver(timeUpdateReceiver);
            unregisterReceiver(endTimeReceiver);
        }catch(Exception e){
            e.printStackTrace();
        }
    }

    @Override
    protected void onPostResume() {
        super.onPostResume();
        //intent filter to catch time updates
        IntentFilter timeUpdateFilter = new IntentFilter();
        timeUpdateFilter.addAction("IntervalTimerCounter");
        registerReceiver(timeUpdateReceiver,timeUpdateFilter);
        IntentFilter timerDoneFilter = new IntentFilter();
        timerDoneFilter.addAction("IntervalTimerDone");
        registerReceiver(endTimeReceiver,timerDoneFilter);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_intervals);
        ActivityCompat.requestPermissions(this,new String[]{Manifest.permission.FOREGROUND_SERVICE}, PackageManager.PERMISSION_GRANTED);
        //bind to service;
        doBindService();
        countdownTextView = findViewById(R.id.countdownTextView);
        nameTextView = findViewById(R.id.intervalsNameTextView);
        nameTextView.setVisibility(View.INVISIBLE);

        startButton = findViewById(R.id.intervalStartButton);
        resetButton = findViewById(R.id.intervalResetButton);
        newButton = findViewById(R.id.intervalNewButton);
        notification = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
        mp = MediaPlayer.create(getApplicationContext(),notification);

        blinkingAnimation = new AlphaAnimation(0.0f,1.0f);
        blinkingAnimation.setDuration(500);
        blinkingAnimation.setStartOffset(20);
        blinkingAnimation.setRepeatMode(Animation.REVERSE);
        blinkingAnimation.setRepeatCount(10);

        RecyclerView recyclerView = findViewById(R.id.intervalsRecyclerView);
        intervalsAdapter = new IntervalsAdapter(this);
        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(this);
        recyclerView.setLayoutManager(linearLayoutManager);
        recyclerView.setAdapter(intervalsAdapter);

        reportsViewModel = new ViewModelProvider(this).get(ReportsViewModel.class);
        reportsViewModel.getAllReports().observe(this, new Observer<List<ReportEntity>>() {
            @Override
            public void onChanged(List<ReportEntity> reportEntities) {
                reports.addAll(reportEntities);
            }
        });

        IntervalsViewModel intervalsViewModel = new ViewModelProvider(this).get(IntervalsViewModel.class);
        intervalsViewModel.getAllIntervals().observe(this, new Observer<List<IntervalsEntity>>() {
            @Override
            public void onChanged(List<IntervalsEntity> intervalsEntityList) {
                intervals.addAll(intervalsEntityList);
            }
        });

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

        ActivityResultLauncher<Intent> newIntervalActivityLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                new ActivityResultCallback<ActivityResult>() {
                    @Override
                    public void onActivityResult(ActivityResult result) {
                        if(result.getResultCode() == Activity.RESULT_OK){
                            Intent intent = result.getData();

                            mBoundService.cancelStopwatch();
                            stopwatch = false;
                            mBoundService.pauseCountdownTimer();

                            //reset everything
                            running = false;
                            started = false;
                            startButton.setText("Start");
                            longTimes.clear();
                            longTimesCounter = 0;

                            stringTimes = (List<String>) intent.getSerializableExtra("StringTimesArray");
                            longTimes = (List<Long>) intent.getSerializableExtra("LongTimesArray");
                            intervalID = intent.getIntExtra("intervalID", 0);
                            intervalName = intent.getStringExtra("name");
                            //System.out.println("IntervalID: " + intervalID);
                            ready = true;

                            //copy timers for reset
                            backupTimes.clear();
                            backupTimes.addAll(longTimes);

                            //have to put these in here to load recycler
                            intervalsAdapter.setTimes(stringTimes);
                            nameTextView.setText(intervalName);
                            countdownTextView.setText(convert(longTimes.get(0)));
                            nameTextView.setPaintFlags(nameTextView.getPaintFlags() |  Paint.UNDERLINE_TEXT_FLAG);
                            nameTextView.setVisibility(View.VISIBLE);
                        }
                    }
                });

        loadIntervalActivityLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                new ActivityResultCallback<ActivityResult>() {
                    @Override
                    public void onActivityResult(ActivityResult result) {
                        if(result.getResultCode() == Activity.RESULT_OK){
                            Intent intent = result.getData();
                            mBoundService.cancelStopwatch();
                            stopwatch = false;
                            mBoundService.pauseCountdownTimer();

                            //reset everything
                            running = false;
                            started = false;
                            startButton.setText("Start");
                            longTimes.clear();
                            longTimesCounter = 0;

                            longTimes = getLongFromString(intent.getStringExtra("code"));
                            stringTimes = convertLongListToString(longTimes);
                            intervalID = intent.getIntExtra("intervalID", 0);
                            intervalName = intent.getStringExtra("name");

                            ready = true;

                            //need a second array since restarting a timer alters it
                            backupTimes.clear();
                            backupTimes.addAll(longTimes);

                            //have to put these in here to load recycler
                            intervalsAdapter.setTimes(stringTimes);
                            countdownTextView.setText(convert(longTimes.get(0)));
                            nameTextView.setText(intervalName);
                            nameTextView.setPaintFlags(nameTextView.getPaintFlags() |  Paint.UNDERLINE_TEXT_FLAG);
                            nameTextView.setVisibility(View.VISIBLE);
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
                   if(!started){
                       runTheLoop();
                   }else{
                       //if a stopwatch is loaded
                       if(stopwatch){
                           mBoundService.cancelStopwatch();
                           intervalsAdapter.updateStopWatchEntry(longTimesCounter - 1,countdownTextView.getText().toString());
                           runTheLoop();
                           stopwatch = false;
                       }else{
                           if(running){
                               mBoundService.pauseCountdownTimer();
                               running = false;
                               startButton.setText("Start");
                           }else{
                               mBoundService.restartCountdownTimer();
                               running = true;
                               startButton.setText("Pause");
                           }
                       }
                   }
                }
            }
        });

        newButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(Intervals.this,IntervalsPlanner.class);
                newIntervalActivityLauncher.launch(intent);
            }
        });

        resetButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(longTimes.size()>0){
                    countdownTextView.setText(convert(longTimes.get(0)));
                }
                mBoundService.cancelStopwatch();
                intervalsAdapter.resetTimes();
                ready = true;
                longTimesCounter = 0;
                stopwatch = false;
                running = false;
                started = false;
                startButton.setText("Start");
                mBoundService.pauseCountdownTimer();
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

    //receiver for end of timer
    BroadcastReceiver endTimeReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            countdownTextView.startAnimation(blinkingAnimation);
            mp.start();
            runTheLoop();
        }
    };
    public void adaptTimes(List<String> stringTimes){
        //count to check if there is a report
        int count = 0;
        //string for interval name
        String name = "";
        //add date to start of string with an a to id it
        String date = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());
        String newString = "";
        newString += "a" + date + ",";
        for(int i = 0;i<stringTimes.size();i++){
            newString += stringTimes.get(i) + ",";
        }

        for(IntervalsEntity ie:intervals){
            if(ie.getIntervalID()==intervalID){
                name = ie.getName();
            }
        }
        if(reports.size() > 0){
            for(ReportEntity re:reports){
                if(re.getFkIntervalID()==intervalID){
                    count++;
                    reportsViewModel.updateTimesRun(re.getReportID(),re.getNumberOfTimesRun() + 1);
                    String time = re.getReportCode() + newString;
                    reportsViewModel.updateReport(re.getReportID(),time);
                    //System.out.println("Count == 1");
                }
            }
        }
        if(count==0){
            reportsViewModel.insertReport(new ReportEntity(intervalID,name,newString,1));
            //System.out.println("Count == 0");
        }
    }

    public String convertToMinutesSeconds(long time){
        int minutes = (int) ((time / (1000 * 60)) % 60);
        int secs = (int) (time / 1000) % 60;

        return String.format(Locale.getDefault(),"%02d:%02d",minutes,secs);
    }

    public List<String> convertLongListToString(List<Long> longList){
        List<String> strings = new ArrayList<>();
        for(long l:longList){
            if(l==0L){
                strings.add("Stopwatch");
            }else{
                strings.add(convertToMinutesSeconds(l));
            }
        }

        return strings;
    }

    //public void next(long time) {
    //start(time);
    //}

    public List<Long> getLongFromString(String code){
        String[] stringLong = code.split(",");

        List<Long> longs = new ArrayList<>();

        for(int i = 0;i<stringLong.length;i++){
            longs.add(Long.valueOf(stringLong[i]));
        }

        return longs;
    }

    //trying to keep running through the arrays in one method
    public void runTheLoop(){
        if(longTimesCounter < longTimes.size()){
            if(longTimes.get(longTimesCounter)==0){
                mBoundService.startStopwatch();
                stopwatch = true;
                startButton.setText("Interval");
                running = true;
            }else{
                long time = longTimes.get(longTimesCounter);
                mBoundService.setOriginalTime(time);
                mBoundService.startCountdownTimer();
                running = true;
                startButton.setText("Pause");
                timersCounter++;
            }
            started = true;
            intervalsAdapter.setBackground(longTimesCounter);
            longTimesCounter++;
        }else{
            ready = false;
            adaptTimes(intervalsAdapter.retrieveTimes());
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_intervals, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item){
        int id = item.getItemId();

        if(id == R.id.reportMenuItem){
            Intent intent = new Intent(Intervals.this,Reports.class);
            intent.putExtra("intervalID",intervalID);
            startActivity(intent);

            return true;
        }

        if(id == R.id.ringtoneMenuItem){
            mBoundService.cancelStopwatch();
            intervalsAdapter.resetTimes();
            ready = true;
            longTimesCounter = 0;
            stopwatch = false;
            running = false;
            started = false;
            startButton.setText("Start");
            mBoundService.pauseCountdownTimer();
            Intent loadRingtone = new Intent(RingtoneManager.ACTION_RINGTONE_PICKER);
            loadRingtone.putExtra(RingtoneManager.EXTRA_RINGTONE_TITLE,"Select alert tone");
            loadRingtone.putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_DEFAULT, true);
            loadRingtone.putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_SILENT, true);
            loadRingtone.putExtra(RingtoneManager.EXTRA_RINGTONE_TYPE, RingtoneManager.TYPE_NOTIFICATION);
            loadRingtoneActivityLauncher.launch(loadRingtone);
            return true;
        }

        if(id == R.id.loadIntervalMenuItem){
            mBoundService.cancelStopwatch();
            ready = true;
            longTimesCounter = 0;
            stopwatch = false;
            running = false;
            started = false;
            startButton.setText("Start");
            mBoundService.pauseCountdownTimer();
            intervalsAdapter.resetTimes();
            Intent loadTimer = new Intent(Intervals.this,LoadInterval.class);
            loadIntervalActivityLauncher.launch(loadTimer);
        }

        return super.onOptionsItemSelected(item);
    }

}