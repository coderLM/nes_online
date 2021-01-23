package com.magic.nes;

import android.content.Context;
import android.text.TextUtils;
import android.util.Log;

import com.eclipsesource.v8.JavaCallback;
import com.eclipsesource.v8.JavaVoidCallback;
import com.eclipsesource.v8.NodeJS;
import com.eclipsesource.v8.V8;
import com.eclipsesource.v8.V8Array;
import com.eclipsesource.v8.V8ArrayBuffer;
import com.eclipsesource.v8.V8Function;
import com.eclipsesource.v8.V8Object;
import com.eclipsesource.v8.V8TypedArray;
import com.eclipsesource.v8.V8Value;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by LiMeng on 2021/1/15.
 */

public class JSExecutor {
    private static String TAG = "JSExecutor";
    Context context;
    public V8 runtime;
    private static JSExecutor instance;
    public static final ArrayList<String> searchDirArray = new ArrayList<>();
    private static String mainPath="jsnes/main.js";
    static {
        searchDirArray.add("jsnes");
        searchDirArray.add("jsnes/src");
    }

    private JSExecutor() {

    }

    public static JSExecutor getInstance(Context context) {
        if (instance == null) {
            synchronized (JSExecutor.class) {
                if (instance == null) {
                    instance = new JSExecutor();
                    instance.setContext(context);
                    instance.startRuntime();
                }
            }
        }

        return instance;
    }

    private void setContext(Context context) {
        this.context = context;
    }

    public void startRuntime() {
        if (runtime == null) {
            runtime = V8.createV8Runtime();
            JavaVoidCallback print = (v8Object, args) -> {
                Log.d(TAG, args.get(0).toString());
            };
            runtime.registerJavaMethod(print, "print");
            JavaCallback require = (v8Object, args) -> {
                try{
                    V8Object exports = null;
                    String filePath = args.get(0).toString();
                    String absolutePath = FileUtils.getFilePathFromAsset(context, filePath,searchDirArray );
                    if (!TextUtils.isEmpty(absolutePath)) {
                        exports = JSModule.require(filePath, absolutePath, runtime, false);
                        if (exports == null) {
                            Log.e(TAG, "require js file fail,file name: %s" + filePath);
                            return null;
                        }
                    } else {
                        Log.e(TAG, "require js file fail,file name: %s" + filePath);
                    }
                    return exports;
                }catch (Exception e){
                    e.printStackTrace();
                }
                return null;
            };
            runtime.registerJavaMethod(require, "require");
            JSModule.initGlobalModuleCache(runtime);
            execute(mainPath);
        }
    }

    private void execute(String path) {
        String jsCode = FileUtils.getScriptFromAssets(context, path);
        runtime.executeVoidScript(jsCode);

    }
    public void callMethodWithBytes(String method, byte[] bytes){
        V8Function function = (V8Function) runtime.getObject(method);
        V8Array parameters = new V8Array(runtime);
        V8ArrayBuffer buffer = new V8ArrayBuffer(runtime, bytes.length);
        buffer.put(bytes);
        V8TypedArray v8TypedArray =  new V8TypedArray(runtime, buffer, V8Value.BYTE, 0, bytes.length);
        parameters.push(v8TypedArray);
        Object result = function.call(runtime, parameters);
    }
    public void callMethodWithString(String method, String data){
        V8Function function = (V8Function) runtime.getObject(method);
        V8Array parameters = new V8Array(runtime);
        parameters.push(data);
        System.out.println("data java len:"+data.length());
        System.out.println("data java content:"+data.substring(0,20)+"...");
        Object result = function.call(runtime, parameters);
    }

    public V8Array callArrayMethodWithInteger(String method,int len){
        V8Array parameters =  new V8Array(runtime);
        parameters.push(len);
        return runtime.executeArrayFunction(method, parameters);
    }

    public void dispose() {
        context = null;
        runtime.close();
    }
}
