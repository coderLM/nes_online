package com.magic.nes;

import android.app.Activity;
import android.graphics.Bitmap;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.opengl.GLUtils;
import android.os.Bundle;
import android.os.Handler;
import android.os.SystemClock;
import android.view.Surface;
import android.view.SurfaceView;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;

import androidx.annotation.Nullable;

import com.alibaba.fastjson.JSONObject;
import com.eclipsesource.v8.JavaVoidCallback;
import com.eclipsesource.v8.V8Array;
import com.eclipsesource.v8.V8Object;
import com.eclipsesource.v8.V8TypedArray;
import com.eclipsesource.v8.debug.mirror.Frame;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Created by LiMeng on 2021/1/11.
 */

public class BitmapTestActivity extends Activity implements View.OnClickListener {
    ImageView imageView;
    Button button;
    private GLSurfaceView mGLView;
    SurfaceView surfaceView;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_bitmaptest);
        initView();
//        setContentView(mGLView);

    }

    private void initView() {
        imageView = findViewById(R.id.action_image);
        button = findViewById(R.id.test_button);
        button.setOnClickListener(this);
        surfaceView = findViewById(R.id.surface_view);
    }


    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.test_button:
                testGame();
                intEGLUtil();
                break;
        }
    }

    FrameUtil frameUtil;

    private void intEGLUtil() {
        frameUtil = new FrameUtil();
        frameUtil.initEGL(surfaceView.getHolder());
        frameUtil.initShader();
    }

    private void testGame() {
        initAudioTack();
        startGame();
        getAudioFrame();
    }

    JSExecutor jsExecutor;

    private void startGame() {
        jsExecutor = JSExecutor.getInstance(this);
        jsExecutor.setVoidCallback("java_receive_frame", new JavaVoidCallback() {
            @Override
            public void invoke(V8Object v8Object, V8Array v8Array) {
                V8TypedArray array = (V8TypedArray) v8Array.get(0);
                renderFrame(array.getIntegers(0, array.length()));
            }
        });
        jsExecutor.setVoidCallback("java_receive_audio", new JavaVoidCallback() {
            @Override
            public void invoke(V8Object v8Object, V8Array v8Array) {
                V8Array array = (V8Array) v8Array.get(0);
                float[] audioData = new float[array.length()];
                for (int i = 0; i < array.length(); i++) {
                    audioData[i] = (float) array.get(i);
                }
                play(audioData);
            }
        });
        byte[] result = FileUtils.getFileDataByReader(
                getApplicationContext(),
                "data/fly.nes");
        jsExecutor.callMethodWithBytes("nes_start", result);
    }

    private AudioTrack track = null;// 录音文件播放对象

    private int frequence = 44100;// 采样率 8000

    private int channelOutConfig = AudioFormat.CHANNEL_OUT_STEREO;// 定义采样通道

    private int audioEncoding = AudioFormat.ENCODING_PCM_FLOAT;

    private int bufferSize = -1;// 播放缓冲大小
    private boolean stoped = true;

    private void initAudioTack() {
        // 获取缓冲 大小
        bufferSize = AudioTrack.getMinBufferSize(frequence, channelOutConfig,
                audioEncoding);
        System.out.println("bufferSize  = " + bufferSize);
        // 实例AudioTrack
        track = new AudioTrack(AudioManager.STREAM_MUSIC, frequence,
                channelOutConfig, audioEncoding, bufferSize,
                AudioTrack.MODE_STREAM);
    }

    private void getAudioFrame() {
        stoped = false;
        new Thread(new Runnable() {
            @Override
            public void run() {
                while (!stoped) {
                    SystemClock.sleep(16);
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            jsExecutor.callMethod("frame");
                        }
                    });
                }
            }
        }).start();
        new Handler().postDelayed(() -> stoped = true, 1000 * 30);
    }

    boolean firstPlay = true;

    private void play(float[] floatArray) {
        int result0 = track.write(floatArray, 0, floatArray.length, AudioTrack.WRITE_BLOCKING);
        if (!writeSuccess(result0)) {
            //出异常情况
            release();
            System.out.println("write error result0=" + result0);
            return;
        }
        if (track.getState() != AudioTrack.STATE_INITIALIZED) {
            System.out.println("AudioTrack state = " + track.getState());
            return;
        }
        if (firstPlay) {
            track.play();
            firstPlay = false;
        }
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

    int renderCount;
    Bitmap testBitmap;
    private void renderFrame(int[] arr) {
        for (int i = 0; i < arr.length; i++) {
            ///模拟器返回的像素颜色为 AGBR，需要调换成 ARGB
            int value = arr[i];
            int R = (value & 0x0000ff) << 16;
            int B = (value & 0xff0000) >> 16;
            arr[i] = 0xff000000 | R | (value & 0x00ff00) | B;
        }
        Bitmap bitmap = Bitmap.createBitmap(arr, 256, 240, Bitmap.Config.ARGB_8888);
        imageView.setImageBitmap(bitmap);
        renderCount++;
        if (renderCount == 10) {
            testBitmap=bitmap;
            new Handler(getMainLooper()).postDelayed(new Runnable() {
                @Override
                public void run() {
                    frameUtil.render(testBitmap, 256, 240);
//                    GLUtils.texImage2D(GLES20.GL_TEXTURE_2D, 0, testBitmap, 0);
                }
            }, 1000);

        }
    }
    /**
     * 向surfaceview中绘制bitmap
     *
     * EGL是介于诸如OpenGL 或OpenVG的Khronos渲染API与底层本地平台窗口系统的接口。
     * 它被用于处理图形管理、表面/缓冲捆绑、渲染同步及支援使用其他Khronos API进行的高效、加速、混合模式2D和3D渲染。
     */
}

