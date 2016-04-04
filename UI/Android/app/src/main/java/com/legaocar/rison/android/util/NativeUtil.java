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

    public void testJni() {
        String love = "我爱你";
        MLogUtil.i(TAG, "start:" + love);
        byte[] returnString = new byte[love.getBytes().length];
        compressYuvToJpeg(love.getBytes(), returnString, 1, 1, 1, 1);
        MLogUtil.i(TAG, "end :" + new String(returnString));
    }

    public native void compressYuvToJpeg(byte[] yuv, byte[] jpg, int format, int quality, int width, int height);

}
