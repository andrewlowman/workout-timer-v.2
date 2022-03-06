package com.andrew_lowman.workouttimer.Activity;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;

import com.andrew_lowman.workouttimer.R;
import com.google.android.material.navigation.NavigationBarView;

public class Watch extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_watch);

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
    }
}