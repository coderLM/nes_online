package com.magic.nes;

import android.app.Activity;
import android.graphics.Bitmap;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.os.Build;
import android.os.Bundle;
import android.os.SystemClock;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;

import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.eclipsesource.v8.NodeJS;
import com.eclipsesource.v8.V8Array;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Map;

import static java.nio.charset.StandardCharsets.ISO_8859_1;
import static java.nio.charset.StandardCharsets.US_ASCII;
import static java.nio.charset.StandardCharsets.UTF_16;
import static java.nio.charset.StandardCharsets.UTF_16BE;
import static java.nio.charset.StandardCharsets.UTF_8;

/**
 * Created by LiMeng on 2021/1/11.
 */

public class BitmapTestActivity extends Activity implements View.OnClickListener {
    ImageView imageView;
    Button button;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_bitmaptest);
        initView();
    }

    private void initView() {
        imageView = findViewById(R.id.action_image);
        button = findViewById(R.id.test_button);
        button.setOnClickListener(this);
    }


    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.test_button:
                initAudioTack();
                startGame();
                getAudioFrame();
//                testNodeJS();
                break;
        }
    }

    private void testNodeJS() {
//        NodeJS nodeJS = NodeJS.createNodeJS();
//        nodeJS.getRuntime();

    }

    private void testParseChar() {
        String chrome = "786983262100000000001202391581622391591912391581542391591682391581421322391581421664239158142032442324423216239159187442321623915918723915816964239158141236423915816963239158141632239158142632239158169152391581623223915814173223915913817623915814001530452391581682391581620239158169123915814122642391581690239158141226423915816982391581333023915818522647411831239159134302391591442391591822391591682391591603239159144239159163239158165312391591333223915917662391591333323915917622391581653223915815316";
        String v8_2 = "786983262100000000001202391901622391911912391901542391911682391901421322391901421664239190142032442324423216239191187442321623919118723919016964239190141236423919016963239190141632239190142632239190169152391901623223919014173223919113817623919014001530452391901682391901620239190169123919014122642391901690239190141226423919016982391901333023919018522647411831239191134302391911442391911822391911682391911603239191144239191163239190165312391911333223919117662391911333323919117622391901653223919015316";
        int len = chrome.length();
        StringBuffer sb0 = new StringBuffer();
        StringBuffer sb1 = new StringBuffer();
        for (int i = 0; i < len; i++) {
            if (chrome.charAt(i) != v8_2.charAt(i)) {
                sb0.append(chrome.charAt(i));
                sb1.append(v8_2.charAt(i));
            }
        }
        System.out.println("c:" + sb0.toString());
        System.out.println("v:" + sb1.toString());
        char[] arr = new char[]{158, 159, 190, 191};
        System.out.println("arr:" + new String(arr));
    }

    JSExecutor jsExecutor;

    private void startGame() {
        jsExecutor = JSExecutor.getInstance(this);
        byte[] result = FileUtils.getFileDataByReader(
                getApplicationContext(),
                "data/fly.nes");
        System.out.println("result:::" + result.length);

        jsExecutor.callMethodWithBytes("nes_start", result);
//        jsExecutor.callMethodWithString("nes_start", new String(result, US_ASCII));
    }

    private AudioTrack track = null;// 录音文件播放对象

    private int frequence = 8000;// 采样率 8000

    private int channelOutConfig = AudioFormat.CHANNEL_OUT_STEREO;// 定义采样通道

    private int audioEncoding = AudioFormat.ENCODING_PCM_FLOAT;

    private int bufferSize = -1;// 播放缓冲大小
    private boolean stoped = true;

    private void initAudioTack() {
        // 获取缓冲 大小
        bufferSize = AudioTrack.getMinBufferSize(frequence, channelOutConfig,
                audioEncoding);
        // 实例AudioTrack
        track = new AudioTrack(AudioManager.STREAM_MUSIC, frequence,
                channelOutConfig, audioEncoding, bufferSize,
                AudioTrack.MODE_STATIC);
        track.setVolume(AudioTrack.getMaxVolume());
    }

    private void getAudioFrame() {
        //数据源对比
        //第八帧数据对比 toJSON 导出数据
        //
        stoped = false;
        new Thread(new Runnable() {
            @Override
            public void run() {
                while (!stoped) {
                    SystemClock.sleep(50);
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            //len：单声道的数据长度为 bufferSize 字节==>bufferSize/4 float32
                            V8Array v8Array = jsExecutor.callArrayMethodWithInteger("get_audio", 512);
//                            System.out.println("vaArray len = "+v8Array.length());
                            //返回数据格式为 [左声道... 右声道...]
                            float[] floatArray = new float[1024];
                            for (int i = 0; i < v8Array.length(); i++) {
                                floatArray[i] = (float) v8Array.get(i);
                            }
                            v8Array.close();
                            System.out.println("vaArray  = " + JSON.toJSONString(floatArray));

                            int result0 = track.write(floatArray, 0, 512, AudioTrack.WRITE_BLOCKING);
//                            int result1 = track.write(floatArray, bufferSize / 4, bufferSize / 4, AudioTrack.WRITE_BLOCKING);
//                            int result1 = track.write(dataArray, bufferSize, bufferSize);
//                            if (!writeSuccess(result0)||!writeSuccess(result1)) {
                            if (!writeSuccess(result0)) {
                                //出异常情况
                                release();
                                System.out.println("write error result0=" + result0);
//                                System.out.println("write error result1=" + result1);
                                return;
                            }
                            if (track.getState() != AudioTrack.STATE_INITIALIZED) {
                                System.out.println("AudioTrack state = " + track.getState());
                                return;
                            }
                            track.play();
                        }
                    });
                }
            }
        }).start();

        new Thread(() -> {
            SystemClock.sleep(1000 * 50);
            stoped = true;
        }).start();
    }

    private boolean writeSuccess(int result) {
//        System.out.println("write result:" + result);
        return !(result == AudioTrack.ERROR_INVALID_OPERATION || result == AudioTrack.ERROR_BAD_VALUE
                || result == AudioTrack.ERROR_DEAD_OBJECT || result == AudioTrack.ERROR);
    }

    private void release() {
        stoped = true;
        track.release();
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

    private void testRenderArray() {
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
}
