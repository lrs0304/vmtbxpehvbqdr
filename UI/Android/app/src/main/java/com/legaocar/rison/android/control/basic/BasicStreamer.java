package com.legaocar.rison.android.control.basic;

import android.hardware.Camera;

import com.legaocar.rison.android.server.LegoHttpServer;

/**
 * Created by rison on 16-3-28.
 * 流处理-基类
 */
public abstract class BasicStreamer {

    protected LegoHttpServer mLegoHttpServer;

    protected int mCameraWidth = 480, mCameraHeight = 360, mFPS = 15;


    /**
     * get frame data from the camera callback
     * 必须被Camera调用，否则无法获取帧数据
     */
    @SuppressWarnings("deprecation")
    public Camera.PreviewCallback previewCallBack;

    /**
     * 设置一个本地服务器负责转发
     *
     * @param legoHttpServer 服务器
     */
    public void setServer(LegoHttpServer legoHttpServer) {
        mLegoHttpServer = legoHttpServer;
    }

    /**
     * 同步摄像头参数
     *
     * @param cameraWidth  摄像头宽度
     * @param cameraHeight 摄像头高度
     * @param fps          帧率
     */
    public void config(int cameraWidth, int cameraHeight, int fps) {
        mFPS = fps;
        mCameraWidth = cameraWidth;
        mCameraHeight = cameraHeight;
    }

    /**
     * 是否已经销毁
     */
    public abstract boolean isAlive();

    /**
     * 释放占用的指针
     */
    public void destroy() {
        mLegoHttpServer = null;
    }
}
