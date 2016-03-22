package com.legaocar.rison.android.control.streamservice;

import android.content.Context;
import android.content.Intent;
import android.hardware.Camera;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.SurfaceView;
import android.view.WindowManager;

import com.legaocar.rison.android.R;

import java.util.concurrent.locks.ReentrantLock;

/**
 * Created by rison on 16-3-22.
 * 摄像头数据采集
 */
public class CameraStreamServiceActivity extends AppCompatActivity implements CameraView.CameraReadyCallback {
    private static final String TAG = CameraStreamServiceActivity.class.getSimpleName();

    private static final int PictureWidth = 480;
    private static final int PictureHeight = 360;

    private CameraView mCameraView;
    private ReentrantLock mPreviewLock = new ReentrantLock();

    public static void start(Context context) {
        Intent intent = new Intent(context, CameraStreamServiceActivity.class);
        context.startActivity(intent);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.camera_stream_activity);

        initViews();

        initCamera();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (mCameraView != null) {
            mPreviewLock.lock();
            mCameraView.StartPreview();
            mCameraView.AutoFocus();
            mPreviewLock.unlock();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (mCameraView != null) {
            mPreviewLock.lock();
            mCameraView.StopPreview();
            mPreviewLock.unlock();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mCameraView != null) {
            mPreviewLock.lock();
            mCameraView.StopPreview();
            mCameraView.Release();
            mPreviewLock.unlock();
        }
    }

    @Override
    public void onBackPressed() {
        //false时，仅当activity为task根（即首个activity例如启动activity之类的）时才生效
        moveTaskToBack(true);
    }

    @Override
    public void onCameraReady() {
        mCameraView.StopPreview();
        mCameraView.setupCamera(PictureWidth, PictureHeight, 4, 25.0, previewCallBack);
        mCameraView.StartPreview();
        mCameraView.AutoFocus();
    }

    private void initViews() {

    }

    private void initCamera() {
        SurfaceView cameraSurface = (SurfaceView) findViewById(R.id.surface_camera);
        mCameraView = new CameraView(cameraSurface);
        mCameraView.setCameraReadyCallback(this);
    }

    private Camera.PreviewCallback previewCallBack = new Camera.PreviewCallback() {
        public void onPreviewFrame(byte[] frame, Camera c) {
            mPreviewLock.lock();
            c.addCallbackBuffer(frame);
            mPreviewLock.unlock();
        }
    };
}
