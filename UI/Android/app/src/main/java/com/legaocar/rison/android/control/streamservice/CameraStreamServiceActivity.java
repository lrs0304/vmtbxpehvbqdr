package com.legaocar.rison.android.control.streamservice;

import android.content.Context;
import android.content.Intent;
import android.support.v7.app.AppCompatActivity;

/**
 * Created by rison on 16-3-22.
 * 摄像头数据采集
 */
public class CameraStreamServiceActivity extends AppCompatActivity {
    private static final String TAG = CameraStreamServiceActivity.class.getSimpleName();

    public static void start(Context context) {
        Intent intent = new Intent(context, CameraStreamServiceActivity.class);
        context.startActivity(intent);
    }

    @Override
    public void onBackPressed() {
        moveTaskToBack(true);//仅当activity为task根（即首个activity例如启动activity之类的）时才生效
        super.onBackPressed();
    }
}
