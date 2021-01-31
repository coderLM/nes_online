package com.magic.nes;

import android.graphics.Bitmap;
import android.opengl.EGL14;
import android.opengl.EGLConfig;
import android.opengl.EGLContext;
import android.opengl.EGLDisplay;
import android.opengl.EGLSurface;
import android.opengl.GLES11Ext;
import android.opengl.GLES20;
import android.opengl.GLUtils;
import android.opengl.Matrix;
import android.util.Log;
import android.view.SurfaceHolder;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.nio.ShortBuffer;

/**
 * Created by LiMeng on 1/31/21.
 */

class FrameUtil {
    SurfaceHolder surfaceHolder;
    private static String TAG = "FrameUtil";
    EGLDisplay eglDis;
    EGLConfig eglConfig;
    EGLContext eglCtx;
    EGLSurface eglSurface;
    int programId;

    private final float[] vertexData = { //渲染顶点坐标数据
            -1.0f, 1.0f,    //左上角
            -1.0f, -1.0f,   //左下角
            1.0f, 1.0f,   //右下角
            1.0f, -1.0f     //右上角
    };
    final float[] textureVertexData = { //渲染纹理坐标数据
            0.0f, 0.0f,
            0.0f, 1.0f,
            1.0f, 0.0f,
            1.0f, 1.0f,
    };
    //画点顺序
    final short[] draw = {
            (short) 0,
            (short) 1,
            (short) 3,

            (short) 0,
            (short) 2,
            (short) 3,
    };

    private float[] mSTMatrix = new float[16];
    private float[] mProjectionMatrix = new float[16];
    private float[] mViewMatrix = new float[16];
    private int uSTMMatrixHandle;
    private int mProjectionMatrixHandle;
    private int aPositionHandle;
    private int aTextureCoordHandle;
    private int uniformTexture;
    private FloatBuffer vertexBuffer;
    private FloatBuffer textureVertexBuffer;

    public void initEGL(SurfaceHolder mSurfaceHolder) {
        Log.e(TAG, "init EGL");
        surfaceHolder = mSurfaceHolder;
        if (eglDis == null) {
            eglDis = EGL14.eglGetDisplay(EGL14.EGL_DEFAULT_DISPLAY);
            if (eglDis == null || eglDis.equals(EGL14.EGL_NO_DISPLAY)) {
                Log.e(TAG, "eglGetDisplay error :" + EGL14.eglGetError());
            }
        }
        boolean success;
        final int[] version = new int[2];
        success = EGL14.eglInitialize(eglDis, version, 0, version, 1);
        if (!success) {
            Log.e(TAG, "Unable to initialize EGL");
        }
        int confAttr[] = {
                EGL14.EGL_RENDERABLE_TYPE, EGL14.EGL_OPENGL_ES2_BIT,
                EGL14.EGL_RED_SIZE, 5,
                EGL14.EGL_GREEN_SIZE, 6,
                EGL14.EGL_BLUE_SIZE, 5, //EGL_RED_SIZE，EGL_GREEN_SIZE，EGL_BLUE_SIZE 表示我们最终渲染的图形是RGB565格式
                EGL14.EGL_NONE   //常量结束符
        };//获取framebuffer格式和能力
        int MaxConfigs = 10;
        EGLConfig[] configs = new EGLConfig[MaxConfigs];
        int[] confAttribute = new int[MaxConfigs + 1];
        confAttribute[MaxConfigs] = EGL14.EGL_NONE;
        success = EGL14.eglGetConfigs(eglDis, null, 0, MaxConfigs, confAttribute, 0);

        success = EGL14.eglChooseConfig(eglDis, confAttr, 0, configs, 0, MaxConfigs, confAttribute, 0);
        if (!success) {
            Log.e(TAG, "some config is wrong :" + EGL14.eglGetError());
        }
        eglConfig = configs[0];
        //创建OpenGL上下文
        int ctxAttr[] = {
                EGL14.EGL_CONTEXT_CLIENT_VERSION, 2,// openGL 2.0
                EGL14.EGL_NONE
        };
        eglCtx = EGL14.eglCreateContext(eglDis, configs[0], EGL14.EGL_NO_CONTEXT, ctxAttr, 0);
        if (eglCtx == EGL14.EGL_NO_CONTEXT) {
            Log.e(TAG, "context failed:" + EGL14.eglGetError());
        }
        int[] surfaceAttr = {
                EGL14.EGL_NONE
        };
        //OpenGL显示层和本地窗口ANativeWindow的绑定
        eglSurface = EGL14.eglCreateWindowSurface(eglDis, configs[0], surfaceHolder, surfaceAttr, 0);
        if (eglSurface == EGL14.EGL_NO_SURFACE) {
            switch (EGL14.eglGetError()) {
                case EGL14.EGL_BAD_ALLOC:
                    // Not enough resources available. Handle and recover
                    Log.e(TAG, "Not enough resources available");
                    break;
                case EGL14.EGL_BAD_CONFIG:
                    // Verify that provided EGLConfig is valid
                    Log.e(TAG, "provided EGLConfig is invalid");
                    break;
                case EGL14.EGL_BAD_PARAMETER:
                    // Verify that the EGL_WIDTH and EGL_HEIGHT are
                    // non-negative values
                    Log.e(TAG, "provided EGL_WIDTH and EGL_HEIGHT is invalid");
                    break;
                case EGL14.EGL_BAD_MATCH:
                    // Check window and EGLConfig attributes to determine
                    // compatibility and pbuffer-texture parameters
                    Log.e(TAG, "Check window and EGLConfig attributes");
                    break;
            }
        } else
            Log.e(TAG, "create native window success eglSurface : " + eglSurface);
        EGL14.eglMakeCurrent(eglDis, eglSurface, eglSurface, eglCtx);
        EGL14.eglBindAPI(EGL14.EGL_OPENGL_ES_API);

    }

    public void initShader() {
        Log.e(TAG, "init shader");
        programId = createProgram(vertexShader, fragmentShader);
        vertexBuffer = floatBufferUtil(vertexData);
        textureVertexBuffer = floatBufferUtil(textureVertexData);
    }

    public static int createProgram(String vertexSource, String fragmentSource) {
        //加载顶点着色器
        int vertexShaderIndex = LoadShader(GLES20.GL_VERTEX_SHADER, vertexSource);
        if (vertexShaderIndex == 0) {
            return GLES20.GL_FALSE;
        }
        //加载片元着色器
        int pixelShader = LoadShader(GLES20.GL_FRAGMENT_SHADER, fragmentSource);
        if (pixelShader == 0) {
            return GLES20.GL_FALSE;
        }

        //创建程序
        int program = GLES20.glCreateProgram();
        if (program != 0) {
            //向程序中加入顶点着色器
            GLES20.glAttachShader(program, vertexShaderIndex);
            //  checkGlError("glAttachShader");
            //向程序中加入片元着色器
            GLES20.glAttachShader(program, pixelShader);
            // checkGlError("glAttachShader");
            //链接程序
            GLES20.glLinkProgram(program);
            int[] linkStatus = new int[1];
            //获取program的连接情况
            GLES20.glGetProgramiv(program, GLES20.GL_LINK_STATUS, linkStatus, 0);
            if (linkStatus[0] != GLES20.GL_TRUE) {
                //若连接失败则报错并删除程序
                Log.e(TAG, "Could not link program: ");
                Log.e(TAG, GLES20.glGetProgramInfoLog(program));
                GLES20.glDeleteProgram(program);
                program = 0;
            }
        }
        GLES20.glDeleteShader(vertexShaderIndex);
        GLES20.glDeleteShader(pixelShader);
        return program;
    }

    /**
     * 加载着色器方法
     * <p>
     * 流程 :
     * <p>
     * ① 创建着色器
     * ② 加载着色器脚本
     * ③ 编译着色器
     * ④ 获取着色器编译结果
     *
     * @param shaderType 着色器类型,顶点着色器(GLES20.GL_FRAGMENT_SHADER), 片元着色器(GLES20.GL_FRAGMENT_SHADER)
     * @param source     着色脚本字符串
     * @return 返回的是着色器的引用, 返回值可以代表加载的着色器
     */
    public static int LoadShader(int shaderType, String source) {
        //1.创建一个着色器, 并记录所创建的着色器的id, 如果id==0, 那么创建失败
        int shader = GLES20.glCreateShader(shaderType);
        if (shader != 0) {
            //2.如果着色器创建成功, 为创建的着色器加载脚本代码
            GLES20.glShaderSource(shader, source);
            //3.编译已经加载脚本代码的着色器
            GLES20.glCompileShader(shader);
            //存放shader的编译情况
            int[] compiled = new int[1];
            //4.获取着色器的编译情况, 如果结果为0, 说明编译失败
            GLES20.glGetShaderiv(shader, GLES20.GL_COMPILE_STATUS, compiled, 0);
            if (compiled[0] == 0) {
                //若编译失败则显示错误日志并删除此shader
                Log.e(TAG, "Could not compile shader " + shaderType + ":");
                Log.e(TAG, GLES20.glGetShaderInfoLog(shader));
                GLES20.glDeleteShader(shader);
                shader = 0;
            }
        }
        return shader;
    }

    /*片段着色器脚本 fragment shader
     *gl_FragColor:
     * Fragment Shader的输出，它是一个四维变量（或称为 vec4）。
     * 表示在经过着色器代码处理后，正在呈现的像素的 R、G、B、A 值。
     * */
    private String fragmentShader =
            "#extension GL_OES_EGL_image_external : require\n" +
                    "precision mediump float;\n" +
                    "varying vec2 v_texCoord;                       \n" +
                    "uniform sampler2D u_samplerTexture;\n" +
                    "void main()                                          \n" +
                    "{                                                    \n" +
                    "  gl_FragColor = texture2D(u_samplerTexture, v_texCoord);\n" +
                    "}";

    /*顶点着色器脚本 vertex shader
     * gl_Position:原始的顶点数据在Vertex Shader中经过平移、旋转、缩放等数学变换后，
     * 生成新的顶点位置（一个四维 (vec4) 变量，包含顶点的 x、y、z 和 w 值）。
     * 新的顶点位置通过在Vertex Shader中写入gl_Position传递到渲染管线的后继阶段继续处理。
     * */
    private String vertexShader =
            "uniform mat4 u_MVPMatrix;  \n" + //纹理变化矩阵传入接口
                    "uniform mat4 uMatrix;      \n" +   //坐标变化矩阵传入接口
                    "attribute vec4 a_position; \n" +    //顶点坐标
                    "attribute vec4 a_texCoord; \n" +    //S  T 纹理坐标
                    "varying vec2 v_texCoord;   \n" +
                    "void main()                \n" +
                    "{                          \n" +
                    "    gl_Position = uMatrix*a_position;\n" +
                    "    v_texCoord  = (u_MVPMatrix*a_texCoord).xy;   \n" +
                    "}";

    public void render(Bitmap bmp, int width, int height) {
        //width, height是surfaceView的宽高
        GLES20.glViewport(0, 0, width, height);

        /*
         * 获取着色器的属性引用id法(传入的字符串时着色器脚本中的属性名)
         * */
        aPositionHandle = GLES20.glGetAttribLocation(programId, "a_position");//坐标传入接口
        aTextureCoordHandle = GLES20.glGetAttribLocation(programId, "a_texCoord");//纹理坐标传入接口

        //投影变换矩阵
        uSTMMatrixHandle = GLES20.glGetUniformLocation(programId, "u_MVPMatrix");
        mProjectionMatrixHandle = GLES20.glGetUniformLocation(programId, "uMatrix");

        uniformTexture = GLES20.glGetUniformLocation(programId, "u_samplerTexture");
        if (programId == 0) {
            Log.e(TAG, "create shaderUtils failed");
            return;
        }else{
            Log.e(TAG, "create shaderUtils succeed");
        }

        GLES20.glUseProgram(programId); //绘制时使用着色程序
        //矩阵变化
        updateProjection(bmp, width, height);
        GLES20.glVertexAttribPointer(aPositionHandle,
                2,
                GLES20.GL_FLOAT,
                false,
                2 * 4,
                vertexBuffer);
        GLES20.glEnableVertexAttribArray(aPositionHandle); //坐标激活，并传入着色器

        GLES20.glVertexAttribPointer(aTextureCoordHandle,
                2,
                GLES20.GL_FLOAT,
                false,
                2 * 4,
                textureVertexBuffer);
        GLES20.glEnableVertexAttribArray(aTextureCoordHandle);//纹理激活并传入着色器
        GLES20.glUniform1i(uniformTexture, 0);

        drawFrameRender(bmp);
        // 交换显存(将surface显存和显示器的显存交换)
        EGL14.eglSwapBuffers(eglDis, eglSurface);
        releaseBuffer();
    }

    private void drawFrameRender(Bitmap bmp) {
        int[] textureId = new int[1];
        GLES20.glGenTextures(1, textureId, 0);
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureId[0]);

        GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, textureId[0]);
        GLES20.glTexParameterf(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES20.GL_TEXTURE_MIN_FILTER,
                GLES20.GL_NEAREST);
        GLES20.glTexParameterf(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES20.GL_TEXTURE_MAG_FILTER,
                GLES20.GL_LINEAR);
        // Set filtering
        //设置缩小过滤为使用纹理中坐标最接近的一个像素的颜色作为需要绘制的像素颜色
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_NEAREST);
        //设置放大过滤为使用纹理中坐标最接近的若干个颜色，通过加权平均算法得到需要绘制的像素颜色
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR);

        //设置环绕方向S，截取纹理坐标到[1/2n,1-1/2n]。将导致永远不会与border融合
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE);
        //设置环绕方向T，截取纹理坐标到[1/2n,1-1/2n]。将导致永远不会与border融合
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE);

        // Load the bitmap into the bound texture.
        //根据以上指定的参数，生成一个2D纹理
        GLUtils.texImage2D(GLES20.GL_TEXTURE_2D, 0, bmp, 0);

        GLES20.glClearColor(0.0f, 0.0f, 0.0f, 0.0f);
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT | GLES20.GL_DEPTH_BUFFER_BIT);
        //纹理坐标转换
        GLES20.glUniformMatrix4fv(mProjectionMatrixHandle, 1, false, mProjectionMatrix, 0);
        GLES20.glUniformMatrix4fv(uSTMMatrixHandle, 1, false, mSTMatrix, 0);

//        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D,textureId[0]);
        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4); //画图实现方法1
        //这两种画图用一种就行了。关于世界坐标和纹理坐标的对应法则，可以搜搜其他大佬的博客。
//        GLES20.glDrawElements(GLES20.GL_TRIANGLE_STRIP, 6, GLES20.GL_UNSIGNED_SHORT, shortBufferUtils(draw)); /画图实现方法2
        GLES20.glDisableVertexAttribArray(aPositionHandle);
        GLES20.glDisableVertexAttribArray(aTextureCoordHandle);
        //删除纹理数据，如果不delete，则大量的纹理数据会导致程序crash
        GLES20.glDeleteTextures(1, textureId, 0);
    }

    //float[]数组转化成FloatBuffer
    private FloatBuffer floatBufferUtil(float[] arr) {
        FloatBuffer mbuffer;
        // 初始化ByteBuffer，长度为arr.length * 4,因为float占4个字节
        ByteBuffer qbb = ByteBuffer.allocateDirect(arr.length * 4);
        // 数组排列用nativeOrder
        qbb.order(ByteOrder.nativeOrder());

        mbuffer = qbb.asFloatBuffer();
        mbuffer.put(arr);
        mbuffer.position(0);
        return mbuffer;
    }

    //int[]数组转换成IntBuffer
    private IntBuffer intBufferUtil(int[] arr) {
        IntBuffer mbuffer;
        // 初始化ByteBuffer，长度为arr.length * 4,因为float占4个字节
        ByteBuffer qbb = ByteBuffer.allocateDirect(arr.length * 4);
        // 数组排列用nativeOrder
        qbb.order(ByteOrder.nativeOrder());

        mbuffer = qbb.asIntBuffer();
        mbuffer.put(arr);
        mbuffer.position(0);

        return mbuffer;
    }

    //short[]数组转换成ShortBuffer
    private ShortBuffer shortBufferUtils(short[] arr) {
        ShortBuffer mbuffer;
        // 初始化ByteBuffer，长度为arr.length * 4,因为float占4个字节
        ByteBuffer qbb = ByteBuffer.allocateDirect(arr.length * 4);
        // 数组排列用nativeOrder
        qbb.order(ByteOrder.nativeOrder());

        mbuffer = qbb.asShortBuffer();
        mbuffer.put(arr);
        mbuffer.position(0);

        return mbuffer;
    }

    //着色坐标转换矩阵
    private void updateProjection(Bitmap bmp, int screenWidth, int screenHeight) {
        float screenRatio = (float) screenWidth / screenHeight;
        float videoRatio = (float) screenWidth / screenHeight;
        if (videoRatio > screenRatio) {
            Matrix.orthoM(mProjectionMatrix, 0, -1f, 1f, -videoRatio / screenRatio, videoRatio / screenRatio, -3f, 7f);
        } else
            Matrix.orthoM(mProjectionMatrix, 0, -screenRatio / videoRatio, screenRatio / videoRatio, -1f, 1f, -3f, 7f);
        //设置相机位置
        Matrix.setLookAtM(mViewMatrix, 0, 0, 0, 7.0f, 0f, 0f, 0f, 0f, 1.0f, 0.0f);
        //计算变换矩阵
        Matrix.multiplyMM(mSTMatrix, 0, mProjectionMatrix, 0, mViewMatrix, 0);
    }

    public void release() {
        Log.e(TAG, "release surface and display");
        surfaceHolder = null;
        EGL14.eglMakeCurrent(eglDis, eglSurface, eglSurface, eglCtx);
        if (eglSurface != EGL14.EGL_NO_SURFACE) {
            EGL14.eglMakeCurrent(eglDis, EGL14.EGL_NO_SURFACE, EGL14.EGL_NO_SURFACE, EGL14.EGL_NO_CONTEXT);
            EGL14.eglDestroySurface(eglDis, eglSurface);
            eglSurface = EGL14.EGL_NO_SURFACE;
        }
        if (eglCtx != EGL14.EGL_NO_CONTEXT) {
            EGL14.eglDestroyContext(eglDis, eglCtx);
            eglCtx = EGL14.EGL_NO_CONTEXT;
        }
        if (eglDis != EGL14.EGL_NO_DISPLAY) {
            EGL14.eglTerminate(eglDis);
            eglDis = EGL14.EGL_NO_DISPLAY;
        }
        eglDis = EGL14.EGL_NO_DISPLAY;
        eglSurface = EGL14.EGL_NO_SURFACE;
        eglCtx = EGL14.EGL_NO_CONTEXT;
    }

    private void releaseBuffer() {
        vertexBuffer.clear();
        textureVertexBuffer.clear();
    }


}
