package com.legaocar.rison.android.util;

/**
 * Created by rison on 16-4-3.
 * 加载Jni函数
 */
@SuppressWarnings("all")
public class NativeUtil {
    private static NativeUtil mInstance;

    public static NativeUtil getInstance() {
        if (mInstance == null) {
            synchronized (NativeUtil.class) {
                if (mInstance == null) {
                    mInstance = new NativeUtil();
                }
            }
        }
        return mInstance;
    }

    public native String stringFromJNI();

}
