package com.legaocar.rison.android.util;

import android.content.Context;
import android.widget.Toast;

/**
 * Created by rison on 16-3-21.
 * toast 辅助类
 */
public class MToastUtil {

    private static Toast mToast;

    public static void show(Context context, int resourceId) {
        show(context, context.getString(resourceId));
    }

    public static void show(Context context, String content) {
        show(context, content, Toast.LENGTH_SHORT);
    }

    public static void showLong(Context context, int resourceId) {
        showLong(context, context.getString(resourceId));
    }

    public static void showLong(Context context, String content) {
        show(context, content, Toast.LENGTH_LONG);
    }

    public static void show(Context context, String content, int duration) {
        if (content == null) {
            return;
        }

        if (mToast == null) {
            mToast = Toast.makeText(context, content, duration);
        } else {
            mToast.cancel();
            mToast = Toast.makeText(context, content, duration);
        }

        mToast.show();
    }
}
