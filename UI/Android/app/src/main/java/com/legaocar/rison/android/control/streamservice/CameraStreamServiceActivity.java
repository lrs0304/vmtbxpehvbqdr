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
import com.legaocar.rison.android.control.basic.DataStream;
import com.legaocar.rison.android.control.mjpegstreamer.MJpegStream;
import com.legaocar.rison.android.server.LegoHttpServer;
import com.legaocar.rison.android.util.MLogUtil;
import com.legaocar.rison.android.util.MToastUtil;
import com.legaocar.rison.android.util.NativeUtil;
import com.legaocar.rison.android.util.NetWorkUtil;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
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

    private static final String ORIGIN_URL = "/video/origin.yuv";
    private static final String CAPTURE_URL = "/video/capture.jpg";
    private static final String VIDEO_STREAM_URL = "/video/live.mjpg";

    private static final int COMPRESS_QUALITY = 60;

    private int mPictureWidth = 480;
    private int mPictureHeight = 360;

    private CameraView mCameraView;
    private ReentrantLock mPreviewLock = new ReentrantLock();

    private int mVideoPreViewFormat = ImageFormat.NV21;
    private byte[] mFrameData = null, mTempBuffer = null;
    private int mFrameConvertImageLength = 0;
    private byte[] mFrameConvertImage = null;

    private LegoHttpServer mWebServer;

    boolean isProcessing = false;
    ExecutorService mImageExecutor = Executors.newFixedThreadPool(3);
    ImageEncodingTask mImageEncodingTask = new ImageEncodingTask();

    // 负责处理视频流
    private boolean isStreamingVideo;
    private MJpegVideoEncoder mVideoEncoder = null;
    private List<MJpegStream> mVideoStreams = null;
    private List<MJpegStream> mTempVideoStreams = null;

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
            mWebServer.registerStream(ORIGIN_URL, mOriginProcessor);
            mWebServer.registerStream(CAPTURE_URL, mCaptureProcessor);
            mWebServer.registerStream(VIDEO_STREAM_URL, mVideoProcessor);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (mCameraView != null) {
            mPreviewLock.lock();
            mCameraView.StartPreview();
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

        releaseVideoStream();

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
    }

    private void initViews() {

    }

    private void initCamera() {
        SurfaceView cameraSurface = (SurfaceView) findViewById(R.id.surface_camera);
        mCameraView = new CameraView(cameraSurface);
        mCameraView.setCameraReadyCallback(this);


        isStreamingVideo = false;
        if (mVideoStreams == null)
            mVideoStreams = new ArrayList<>();
        if (mTempVideoStreams == null)
            mTempVideoStreams = new ArrayList<>(1);
    }

    private void releaseVideoStream() {

        mCaptureProcessor = null;
        mVideoProcessor = null;
        if (mWebServer != null) {
            mWebServer.registerCGI(ORIGIN_URL, null);
            mWebServer.registerStream(CAPTURE_URL, null);
            mWebServer.registerStream(VIDEO_STREAM_URL, null);
        }

        // 释放
        isStreamingVideo = false;
        for (DataStream videoStream : mVideoStreams) {
            videoStream.release();
        }

        NativeUtil.getInstance().releaseJpegEncoder();
    }

    @SuppressWarnings("deprecation")
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
            // 初始化编码模块
            NativeUtil.getInstance().initJpegEncoder(mPictureWidth, mPictureHeight);
            MLogUtil.i(TAG, "image:width=" + mPictureWidth + ", height=" + mPictureHeight);
        }
        System.arraycopy(yuvFrame, 0, mFrameData, 0, yuvFrame.length);

        isProcessing = true;
        mImageExecutor.execute(mImageEncodingTask);
    }

    private class ImageEncodingTask implements Runnable {

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
                    if (mFrameConvertImage == null) {
                        mFrameConvertImage = new byte[mFrameData.length << 1];
                        mTempBuffer = new byte[mFrameConvertImage.length];
                    }

                    mFrameConvertImageLength = (int) NativeUtil.getInstance()
                            .compressYuvToJpeg(
                                    mFrameData,
                                    mTempBuffer,
                                    mVideoPreViewFormat,
                                    COMPRESS_QUALITY,
                                    mPictureWidth,
                                    mPictureHeight);
                    System.arraycopy(mTempBuffer, 0, mFrameConvertImage, 0, mFrameConvertImageLength);
                    break;

                default:
                    MLogUtil.e(TAG, "no frame image");
            }

            isProcessing = false;
        }
    }

    /**
     * 负责上传单张图片
     */
    private LegoHttpServer.CommonGatewayInterface mCaptureProcessor = new LegoHttpServer.CommonGatewayInterface() {

        @Override
        public String run(Properties params) {
            return null;
        }

        @Override
        public InputStream streaming(Properties params) {
            if (mFrameConvertImage != null) {
                params.put("mime", "image/jpeg");
                return new ByteArrayInputStream(mFrameConvertImage, 0, mFrameConvertImageLength);
            }
            return null;
        }
    };

    /**
     * 负责上传源图像
     */
    private LegoHttpServer.CommonGatewayInterface mOriginProcessor = new LegoHttpServer.CommonGatewayInterface() {

        @Override
        public String run(Properties params) {
            return null;
        }

        @Override
        public InputStream streaming(Properties params) {
            if (mFrameConvertImage != null) {
                return new ByteArrayInputStream(mFrameData);
            }
            return null;
        }
    };

    /**
     * 负责处理MJpeg图片流
     */
    private LegoHttpServer.CommonGatewayInterface mVideoProcessor = new LegoHttpServer.CommonGatewayInterface() {
        @Override
        public String run(Properties params) {
            return null;
        }

        @Override
        public InputStream streaming(Properties params) {
            // 准备一个视频输入流
            MJpegStream videoStream;
            try {
                videoStream = new MJpegStream();
            } catch (Exception e) {
                e.printStackTrace();
                MLogUtil.d(TAG, "error on creating video stream");
                return null;
            }

            InputStream is;
            try {
                is = videoStream.getInputStream();
                videoStream.addCommonStreamHeader();
            } catch (Exception e) {
                videoStream.release();
                return null;
            }

            mTempVideoStreams.add(videoStream);

            if (!isStreamingVideo) {
                mVideoEncoder = new MJpegVideoEncoder();
                mVideoEncoder.start();
            }
            return is;
        }
    };

    /**
     * 处理MJpeg视频流
     */
    private class MJpegVideoEncoder extends Thread {

        @Override
        public void run() {

            if (isStreamingVideo) {
                return;
            }

            isStreamingVideo = true;

            List<DataStream> invalidVideoStreams = new ArrayList<>();
            long timeStamp;
            while (true) {
                if (!isStreamingVideo) {
                    break;
                }

                timeStamp = System.currentTimeMillis();
                for (MJpegStream videoStream : mVideoStreams) {
                    if (mFrameConvertImage == null) {
                        break;
                    }
                    try {
                        videoStream.sendFrame(mFrameConvertImage, mFrameConvertImageLength, timeStamp);
                        if (!videoStream.isAlive()) {
                            invalidVideoStreams.add(videoStream);
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                        invalidVideoStreams.add(videoStream);
                    }
                }

                // 对无效的Stream清理，释放空间
                if (invalidVideoStreams.size() > 0) {
                    for (DataStream videoStream : invalidVideoStreams) {
                        videoStream.release();
                        mVideoStreams.remove(videoStream);
                    }
                    invalidVideoStreams.clear();
                }

                if (mTempVideoStreams.size() > 0) {
                    mVideoStreams.addAll(mTempVideoStreams);
                    mTempVideoStreams.clear();
                }
            }
            isStreamingVideo = false;
        }

    }

}
