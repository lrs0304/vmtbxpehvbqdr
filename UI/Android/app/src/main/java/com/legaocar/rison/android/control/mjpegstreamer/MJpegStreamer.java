package com.legaocar.rison.android.control.mjpegstreamer;

import com.legaocar.rison.android.control.basic.BasicStreamer;

/**
 * Created by rison on 16-3-28.
 * 向服务器推送MJpeg视频流<br/>
 * 目前压缩方法为使用系统自带的YUVImage.compressToJpg方法，需要找时间封装为C代码实现，实现最大限度流畅<br/>
 * <p>
 * 需要在 {@link com.legaocar.rison.android.server.LegoHttpServer} 处注明为流传输处理
 */
public class MJpegStreamer extends BasicStreamer {

    private static final String TAG = "MJpegStreamer";

    private static final String CAPTURE_URL = "/mjpeg/capture.jpg";
    private static final String VIDEO_STREAM_URL = "/mjpeg/live.mjpg";

    public MJpegStreamer() {
        //// TODO: 16-3-28 1
    }

    @Override
    public boolean isAlive() {
        return false;
    }

    @Override
    public void destroy() {
        super.destroy();
        //// TODO: 16-3-28 释放
    }
}
