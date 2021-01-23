package com.magic.nes;

import android.app.Activity;
import android.graphics.Bitmap;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.os.Bundle;
import android.os.SystemClock;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;

import androidx.annotation.Nullable;

import com.alibaba.fastjson.JSONObject;
import com.eclipsesource.v8.V8Array;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.Map;

import static java.nio.charset.StandardCharsets.US_ASCII;

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
                break;
        }
    }

    JSExecutor jsExecutor;

    private void startGame() {
        jsExecutor = JSExecutor.getInstance(this);
        String string;
        byte[] bytes = FileUtils.getFileDataByReader(
                this,
                "data/fly.nes");
        jsExecutor.callMethodWithString("nes_start", new String(bytes,US_ASCII));
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
        stoped = false;
        new Thread(new Runnable() {
            @Override
            public void run() {
                while (!stoped) {
                    SystemClock.sleep(30);
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
            SystemClock.sleep(1000 * 20);
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
