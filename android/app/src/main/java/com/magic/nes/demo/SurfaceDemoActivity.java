package com.magic.nes.demo;

import android.app.Activity;
import android.os.Bundle;
import android.view.SurfaceView;

import androidx.annotation.Nullable;

import com.magic.nes.R;

class SurfaceDemoActivity extends Activity {
    SurfaceView surfaceView;
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_surface_demo);
        surfaceView = findViewById(R.id.surface);
    }

}
