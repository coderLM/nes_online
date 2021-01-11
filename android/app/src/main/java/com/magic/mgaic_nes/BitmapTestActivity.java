package com.magic.mgaic_nes;

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
import java.util.Iterator;
import java.util.Map;
import java.util.function.ToIntFunction;

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
        String json = getData();
        Map<Object, Object> map = (Map<Object, Object>) JSONObject.parse(json);
        System.out.println("map len:::" + map.size());
        System.out.println("type of value:::"+map.get("0"));
        int[] arr = new int[map.size()];
        Long[] array = (Long[]) map.values().toArray(new Long[0]);
        System.out.println("int value:::"+array[0]);
        for (int i = 0; i < array.length; i++) {
            arr[i] = (int) (array[i]&(0xffffffff));
        }
        System.out.println("arr[0]"+arr[0]);
        System.out.println("arr[0]"+arr[1]);
        System.out.println("arr[0]"+arr[2]);
        System.out.println("arr[0]"+arr[3]);
        System.out.println("0xff000000:::"+(0xff000000));
        imageView.setImageBitmap(Bitmap.createBitmap(arr, 256, 240, Bitmap.Config.ARGB_8888));
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
