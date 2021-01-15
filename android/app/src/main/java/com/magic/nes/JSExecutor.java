package com.magic.nes;

import com.eclipsesource.v8.V8;

/**
 * Created by LiMeng on 2021/1/15.
 */

class JSExecutor {
    public static V8 runtime;

    public void startRuntime() {
        if (runtime == null) runtime = V8.createV8Runtime();
    }

    public void execute(String path){

    }

}
