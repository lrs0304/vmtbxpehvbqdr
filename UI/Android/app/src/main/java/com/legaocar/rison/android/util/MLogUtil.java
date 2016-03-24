package com.legaocar.rison.android.util;

import android.util.Log;

import com.legaocar.rison.android.config.BuildConfig;

/**
 * Created by rison on 16-3-22.
 * 自定义的Log输出
 */
public class MLogUtil {

    public static void i(String tag, String content) {
        Log.i(tag, content);
    }

    public static void w(String tag, String content) {
        if (BuildConfig.DEBUG) {
            Log.w(tag, content);
        }
    }

    public static void d(String tag, String content) {
        if (BuildConfig.DEBUG) {
            Log.d(tag, content);
        }
    }

    public static void e(String tag, String content) {
        Log.e(tag, content);
    }
}
