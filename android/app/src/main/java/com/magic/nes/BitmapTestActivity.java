package com.magic.nes;

import android.app.Activity;
import android.graphics.Bitmap;
import android.os.Build;
import android.os.Bundle;
import android.widget.ImageView;

import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;

import com.alibaba.fastjson.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.Map;

/**
 * Created by LiMeng on 2021/1/11.
 */

public class BitmapTestActivity extends Activity {
    ImageView imageView;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_bitmaptest);
        imageView = findViewById(R.id.action_image);
    }

    @RequiresApi(api = Build.VERSION_CODES.N)
    @Override
    protected void onResume() {
        super.onResume();
//        file:///Users/sm-li/jsnes/example/nes-embed.html
        String json = getData();
        Map<Object, Object> map = (Map<Object, Object>) JSONObject.parse(json);
        int len = map.size();
        int[] arr = new int[len];
        for (int i = 0; i < len; i++) {
            ///模拟器返回的像素颜色为 AGBR，需要调换成 ARGB
            int value = ((Long) map.get(i + "")).intValue();
            int R = (value & 0x000000ff) << 16;
            int B = (value & 0x00ff0000) >> 16;
            arr[i] = (value & 0xff00ff00) | R | B;
        }
        imageView.setImageBitmap(Bitmap.createBitmap(arr, 256, 240, Bitmap.Config.ARGB_8888));
//        imageView.setScaleX(4.0f);
//        imageView.setScaleY(4.0f);
    }

    private String getData() {
        try {
            InputStreamReader inputReader = new InputStreamReader(getResources().getAssets().open("data/nes_frame.json"));
            BufferedReader bufReader = new BufferedReader(inputReader);
            String line = "";
            String result = "";
            while ((line = bufReader.readLine()) != null)
                result += line;
            return result;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return "";
    }

}
