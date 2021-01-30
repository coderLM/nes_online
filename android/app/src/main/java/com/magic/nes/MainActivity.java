package com.magic.nes;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;

import androidx.annotation.Nullable;

import java.util.Timer;
import java.util.TimerTask;

import io.flutter.embedding.android.FlutterActivity;

public class MainActivity extends FlutterActivity {
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                openTestPage();
            }
        },3000);
    }

    @Override
    protected void onResume() {
        super.onResume();
       
    }
    private  void openTestPage(){
        Intent intent = new Intent(this, BitmapTestActivity.class);
        startActivity(intent);
    }
}
