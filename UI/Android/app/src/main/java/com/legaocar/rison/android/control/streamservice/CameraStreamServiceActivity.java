package com.legaocar.rison.android.control.streamservice;

import android.content.Context;
import android.content.Intent;
import android.graphics.ImageFormat;
import android.graphics.Rect;
import android.graphics.YuvImage;
import android.hardware.Camera;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.SurfaceView;
import android.view.WindowManager;

import com.legaocar.rison.android.R;
import com.legaocar.rison.android.server.LegoHttpServer;
import com.legaocar.rison.android.util.MLog;
import com.legaocar.rison.android.util.MToastUtil;
import com.legaocar.rison.android.util.NetWorkUtil;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.Properties;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Created by rison on 16-3-22.
 * 摄像头数据采集
 */
public class CameraStreamServiceActivity extends AppCompatActivity implements CameraView.CameraReadyCallback {
    private static final String TAG = CameraStreamServiceActivity.class.getSimpleName();

    private static final String CAPTURE_URL = "/video/capture.jpg";

    private int mPictureWidth = 480;
    private int mPictureHeight = 360;

    private CameraView mCameraView;
    private ReentrantLock mPreviewLock = new ReentrantLock();

    private int mVideoPreViewFormat = ImageFormat.NV21;
    private byte[] mFrameData = null;
    private byte[] mFrameConvertImage = null;

    private LegoHttpServer mWebServer;

    boolean isProcessing = false;
    ExecutorService executor = Executors.newFixedThreadPool(3);
    ImageEncodingTask mImageEncodingTask = new ImageEncodingTask();

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
        } else {
            initCamera();
            // todo mWebServer.registerStream("/video/live.mjpg", videoProcessor);
            mWebServer.registerStream(CAPTURE_URL, mCaptureProcessor);
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
        if (mCameraView != null) {
            mPreviewLock.lock();
            mCameraView.StopPreview();
            mCameraView.Release();
            mPreviewLock.unlock();
        }

        mCaptureProcessor = null;
        if (mWebServer != null) {
            mWebServer.registerStream(CAPTURE_URL, null);
        }

        super.onDestroy();
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        //false时，仅当activity为task根（即首个activity例如启动activity之类的）时才生效
        //todo moveTaskToBack(true);
    }

    @Override
    public void onCameraReady() {
        mCameraView.StopPreview();
        mCameraView.setupCamera(mPictureWidth, mPictureHeight, 4, 25.0, previewCallBack);
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

            mVideoPreViewFormat = c.getParameters().getPreviewFormat();
            mPictureWidth = mCameraView.Width();
            mPictureHeight = mCameraView.Height();
            convertYUVFrameToImageFrame(frame);

            c.addCallbackBuffer(frame);
            mPreviewLock.unlock();
        }
    };

    private void convertYUVFrameToImageFrame(byte[] yuvFrame) {

        if (isProcessing) { // 这里有疑问，应该处理完再处理当前帧，还是全部拷贝到frameData，
            return;
        }

        if (mFrameData == null) {
            mFrameData = new byte[yuvFrame.length];
        }
        System.arraycopy(yuvFrame, 0, mFrameData, 0, yuvFrame.length);

        isProcessing = true;
        executor.execute(mImageEncodingTask);
    }

    //// TODO: 16-3-23 应该用C代码加快转换速度
    private class ImageEncodingTask implements Runnable {

        private ByteArrayOutputStream mOutputStream = new ByteArrayOutputStream();
        private Rect mArea;
        private YuvImage mYuvImage;

        public ImageEncodingTask() {
        }

        public void run() {

            if (mFrameData == null) {
                isProcessing = false;
                return;
            }

            switch (mVideoPreViewFormat) {
                case ImageFormat.JPEG:
                    if (mFrameConvertImage == null) {
                        mFrameConvertImage = new byte[mFrameData.length];
                    }
                    System.arraycopy(mFrameData, 0, mFrameConvertImage, 0, mFrameData.length);
                    break;

                case ImageFormat.NV16:
                case ImageFormat.NV21:
                case ImageFormat.YUY2:
                case ImageFormat.YV12:
                    if (mArea == null) {
                        mArea = new Rect(0, 0, mPictureWidth, mPictureHeight);
                    }

                    mOutputStream.reset();
                    mYuvImage = new YuvImage(mFrameData, mVideoPreViewFormat, mPictureWidth, mPictureHeight, null);
                    mYuvImage.compressToJpeg(mArea, 80, mOutputStream);
                    if (mFrameConvertImage == null || mFrameConvertImage.length < mOutputStream.size()) {
                        mFrameConvertImage = new byte[mOutputStream.size()];
                    }
                    System.arraycopy(mOutputStream.toByteArray(), 0, mFrameConvertImage, 0, mOutputStream.size());
                    break;

                default:
                    mOutputStream.reset();
                    MLog.e(TAG, "no frame image");
            }

            //synchronized (CameraStreamServiceActivity.this) {
            // 锁
            //}

            isProcessing = false;
        }
    }

    private LegoHttpServer.CommonGatewayInterface mCaptureProcessor = new LegoHttpServer.CommonGatewayInterface() {
        InputStream mInputStream;

        @Override
        public String run(Properties params) {
            return null;
        }

        @Override
        public InputStream streaming(Properties params) {
            if (mFrameConvertImage != null) {
                mInputStream = new ByteArrayInputStream(mFrameConvertImage);
                params.put("mime", "image/jpeg");
                return mInputStream;
            }
            return null;
        }
    };

}
