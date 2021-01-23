package com.magic.nes;

import android.content.Context;
import android.text.TextUtils;
import android.util.Log;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;

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
            return new String(output.toByteArray(), StandardCharsets.UTF_8);
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
    public static byte[] getFileDataByReader(Context context, String fileName) {
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
            return output.toByteArray();
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
    public static  String getFilePathFromAsset(Context context,String filePath,ArrayList<String> searchDirArray){
       return getFilePathFromAssetExt(context,filePath,searchDirArray);
    }
    public static String getFilePathFromAssetExt(Context context, String filePath, ArrayList<String> searchDirArray) {
        String prefix = "./";
        if (filePath.startsWith(prefix)) {
            filePath = filePath.substring(prefix.length());
        }

        String[] filePathSplitList = filePath.split("/");
        int filePathDeep = filePathSplitList.length;

        String absolutePath = "";

        ArrayList<String> extensions = new ArrayList<>();
        extensions.add(".js");
        extensions.add(".ddc.js");
        extensions.add(".lib.js");

        try {
            for (int i = 0; i < searchDirArray.size(); i++) {
                String curSearchDir = searchDirArray.get(i);

                for (int j = 0; j < filePathDeep; j++) {
                    String[] files = context.getAssets().list(curSearchDir);
                    boolean isFind = false;
                    for (int k = 0; k < files.length; k++) {
                        if (j == filePathDeep - 1) {
                            if (filePathSplitList[j].endsWith(".js")) {
                                if (filePathSplitList[j].equals(files[k])) {
                                    curSearchDir += ("/" + filePathSplitList[j]);
                                    absolutePath = curSearchDir;
                                    isFind = true;
                                    break;
                                }
                            } else {
                                for (String ext : extensions) {
                                    String tempFile = filePathSplitList[j] + ext;

                                    if (tempFile.equals(files[k])) {
                                        curSearchDir += ("/" + tempFile);
                                        absolutePath = curSearchDir;
                                        isFind = true;
                                        break;
                                    }
                                }
                            }
                        } else if (filePathSplitList[j].equals(files[k])) {
                            curSearchDir += ("/" + filePathSplitList[j]);
                            isFind = true;
                        }
                        if (isFind) {
                            break;
                        }
                    }
                    if (!isFind) {
                        break;
                    }
                }
                if (!TextUtils.isEmpty(absolutePath)) {
                    break;
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "", e);
        }

        return absolutePath;
    }

}
