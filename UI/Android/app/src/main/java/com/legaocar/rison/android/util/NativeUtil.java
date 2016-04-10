package com.legaocar.rison.android.util;

/**
 * Created by rison on 16-4-3.
 * 加载Jni函数
 */

public class NativeUtil {
    private static final String TAG = "NativeUtil";
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

    public native void initJpegEncoder(int width, int height);

    public native long compressYuvToJpeg(byte[] yuv, byte[] jpg, int format, int quality, int width, int height);

    public native void releaseJpegEncoder();
}
