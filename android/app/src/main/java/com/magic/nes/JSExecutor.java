package com.magic.nes;

import android.content.Context;
import android.text.TextUtils;
import android.util.Log;

import com.eclipsesource.v8.JavaCallback;
import com.eclipsesource.v8.JavaVoidCallback;
import com.eclipsesource.v8.V8;
import com.eclipsesource.v8.V8Array;
import com.eclipsesource.v8.V8Object;

import java.util.Stack;

/**
 * Created by LiMeng on 2021/1/15.
 */

class JSExecutor {
    private static String TAG = "JSExecutor";
    Context context;
    public V8 runtime;
    private static JSExecutor instance;

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
            JavaVoidCallback print = (v8Object, args) -> Log.d(TAG, args.get(0).toString());
            runtime.registerJavaMethod(print, "print");
            JavaCallback require = new JavaCallback() {
                @Override
                public Object invoke(V8Object v8Object, V8Array args) {
                    V8Object exports = null;
                    String filePath = args.get(0).toString();
//                    String absolutePath =  FileUtil.getFilePathFromAsset(mContext.mFlutterPluginBinding.getApplicationContext(), filePath, searchDirArray);
//                    if (!TextUtils.isEmpty(absolutePath)) {
//                        exports = JSModule.require(filePath, absolutePath, MXJSExecutor.runtime, isFromAsset);
//                        if (exports == null) {
//                            jsExecutor.executeScript("throw 'not found'", new MXJSExecutor.ExecuteScriptCallback() {
//                                @Override
//                                public void onComplete(Object value) {
//
//                                }
//                            });
//                            return null;
//                        }
//                    } else {
//                        Log.e(TAG,"require js file fail,file name: %s"+filePath);
//                    }
                    return exports;
                }
            };
            runtime.registerJavaMethod(require, "print");
        }
    }

    public void execute(String path) {
        String jsCode = FileUtil.getScriptFromAssets(context, path);
        runtime.executeVoidScript(jsCode);
    }
    public  void dispose(){
        context=null;
        runtime.close();
    }
}
