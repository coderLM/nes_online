package com.magic.nes;

import android.content.Context;
import android.util.Log;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

import static android.content.ContentValues.TAG;

/**
 * Created by LiMeng on 2021/1/18.
 */

class FileUtils {
    public static String getScriptFromAssets(Context context, String fileName) {
        InputStream input = null;
        ByteArrayOutputStream output = null;
        try {
            input = context.getAssets().open(fileName);
            output = new ByteArrayOutputStream();
            byte[] buffer = new byte[4096];
            int len = 0;
            while ((len = input.read(buffer)) != -1) {
                output.write(buffer, 0, len);
            }
            output.flush();
            return output.toString();
        } catch (IOException e) {
            Log.e("nes","getScriptFromAssets error"+e.getMessage());
        } finally {
            try {
                if (output != null) {
                    output.close();
                }
                if (input != null) {
                    input.close();
                }
            } catch (IOException e) {
            }
        }
        return null;
    }
    public static  String getFilePathFromAsset(Context context,String filePath){
        //todo: mod this
        return "asstes/jsnes/src/"+filePath;
    }

}
