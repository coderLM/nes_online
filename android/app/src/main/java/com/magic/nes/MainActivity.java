package com.magic.nes;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;

import androidx.annotation.Nullable;

import com.magic.nes.demo.SurfaceDemoActivity;

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
//        Intent intent = new Intent(this, BitmapTestActivity.class);
        Intent intent = new Intent(this, SurfaceDemoActivity.class);
        startActivity(intent);
    }
}
