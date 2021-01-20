
package com.magic.nes;

import android.text.TextUtils;

import com.eclipsesource.v8.V8;
import com.eclipsesource.v8.V8Object;
import com.eclipsesource.v8.V8ScriptException;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

public class JSModule {

    public static final String TAG = "JSModule";

    public static HashMap<String, String> CORE_MODULES = new HashMap<>();    //<moduleClassName, modulePath>

    public static HashMap<String, Class> CORE_MODULE_CLASSES = new HashMap<>();  //<moduleClassName, module Class>

    public static V8Object sGlobalModuleCache;

    V8Object mExports;

    private V8Object mContext;
    private String mId;
    private String mFileName;

    public JSModule(String id, String fileName, V8Object context) {
        mId = id;
        mFileName = fileName;
        this.mContext = context;

        mExports = new V8Object(context.getRuntime());
    }

    public static void initGlobalModuleCache(V8 runtime) {
        sGlobalModuleCache = new V8Object(runtime);
        runtime.add("_loadedMoudleCache", sGlobalModuleCache);
    }

    public static boolean isCoreModule(String moduleClassName) {
        return CORE_MODULES.containsKey(moduleClassName);
    }

    public static Class classForModule(String moduleClassName) {
        if (isCoreModule(moduleClassName))
            return CORE_MODULE_CLASSES.get(moduleClassName);
        return JSModule.class;
    }

    public static String coreModuleFullPath(String moduleClassName) {
        if (!isCoreModule(moduleClassName))
            return null;
        return CORE_MODULES.get(moduleClassName);
    }

    public static String resolveModuleAsFile(String moduleClassName) {
        File moduleFile = new File(moduleClassName);
        if (moduleFile.exists()) {
            return moduleClassName;
        }

        moduleFile = new File(moduleClassName + ".js");
        if (moduleFile.exists()) {
            return moduleClassName + ".js";
        }

        return null;
    }


    public static List<String> nodeModulePaths(String start) {
        String[] paths = start.split("/");
        ArrayList<String> pathList = new ArrayList<String>(Arrays.asList(paths));
        int rootIndex = pathList.indexOf("node_modules");
        if (rootIndex <= 0) {
            rootIndex = 0;
        }

        int i = pathList.size() - 1;
        ArrayList<String> dirs = new ArrayList<String>();
        while (i > rootIndex) {
            String component = pathList.get(i);
            if (component.equals("node_modules")) {
                i -= 1;
                continue;
            }
            String dirPath = "";
            List<String> temp = pathList.subList(0, i);
            for (int j = 0; j < temp.size(); j++) {
                if (j == 0) {
                    dirPath += pathList.get(j);
                } else {
                    dirPath += ("/" + pathList.get(j));
                }
            }
            dirs.add(dirPath);
        }

        return dirs;
    }

    public static void clearModuleCache(V8 runtime) {
        sGlobalModuleCache.close();
        sGlobalModuleCache = null;
    }

    public static boolean isCached(String fullModulePath) {
        return sGlobalModuleCache.contains(fullModulePath);
    }

    public static V8Object getCacheModule(String fullModulePath) {
        return sGlobalModuleCache.getObject(fullModulePath);
    }

    public static void cacheModule(String fullModulePath, V8Object exports) {
        sGlobalModuleCache.add(fullModulePath, exports);
    }

    public static V8Object require(String moduleClassName, String fullModulePath, boolean fromAsset) {
        return require(moduleClassName, fullModulePath, JSExecutor.getInstance(null).runtime, fromAsset);
    }

    public static V8Object require(String moduleClassName, String fullModulePath, V8Object context, boolean fromAsset) {
        if (TextUtils.isEmpty(moduleClassName) || TextUtils.isEmpty(fullModulePath) || context == null)
            return null;

        if (isCached(fullModulePath)) {
            android.util.Log.i(TAG, "JSModule Use Cache " + moduleClassName);
            return getCacheModule(fullModulePath);
        }

        Class moduleClass = classForModule(moduleClassName);
        JSModule newModule = (JSModule) ClassUtils.getInstance(moduleClass,
                new Class[]{String.class, String.class, V8Object.class},
                new Object[]{fullModulePath, fullModulePath, context});

        String script = FileUtils.getScriptFromAssets(JSExecutor.getInstance(null).context,fullModulePath);

        V8Object platformObj = new V8Object(context.getRuntime());

        try {
            String exportScript = String.format("(function() { var module = { exports: {}}; var exports = module.exports; \n%s\n; return module.exports; })();", script);
            V8Object value = context.getRuntime().executeObjectScript(exportScript);

            if (value != null) {
                newModule.mExports = value;
            }

        } catch (V8ScriptException e) {
            android.util.Log.e(TAG, "V8ScriptException:JSMessage:" + e);
            android.util.Log.e(TAG, "V8ScriptException:JSFilePath:" + fullModulePath);
        }

        context.add("platform", platformObj);

        cacheModule(fullModulePath, newModule.mExports);

        return newModule.mExports;
    }
}