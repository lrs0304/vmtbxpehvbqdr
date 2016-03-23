package com.legaocar.rison.android.control.streamservice;

import android.content.Context;
import android.content.Intent;
import android.hardware.Camera;
import android.os.Bundle;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.view.SurfaceView;
import android.view.WindowManager;

import com.legaocar.rison.android.R;
import com.legaocar.rison.android.server.LegoHttpServer;
import com.legaocar.rison.android.util.MLog;
import com.legaocar.rison.android.util.MToastUtil;
import com.legaocar.rison.android.util.NetWorkUtil;

import java.io.DataOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
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

    private DataOutputStream mStream;
    private LegoHttpServer mWebServer;
    private Handler mStreamingHandler;
    private static final int StreamingInterval = 10;//ms

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

        mWebServer = NetWorkUtil.getInstance().getWebServer(this);
        if (mWebServer == null) {
            MToastUtil.show(this, "web server error");
            finish();
            return;
        } else {
            initCamera();

            try {
                ServerSocket server = new ServerSocket(8080);
                Socket socket = server.accept();
                server.close();

                MLog.i(TAG, "New connection to :" + socket.getInetAddress());

                mStream = new DataOutputStream(socket.getOutputStream());
            } catch (Exception e) {
                mStream = null;
                MLog.e(TAG, e.getMessage());
            }

            mStreamingHandler = new Handler();
            mStreamingHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    doStreaming();
                }
            }, StreamingInterval);
        }
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

    private void doStreaming() {

    }
}
