package com.legaocar.rison.android.control.entity;

import com.legaocar.rison.android.server.LegoHttpServer;

import java.io.IOException;
import java.io.OutputStream;

/**
 * Created by rison on 16-3-27.
 * 对{@link DataStream}进行封装使之能够为产生MJpeg视频流提供方便。本类使用了双缓冲的方法来避免在帧
 * 在发送的时候被覆盖而产生的抖动的问题，同时视频的处理帧率，提升视频流畅度
 * <p>
 * ..............................--->拷贝--->++++++缓冲区+++++++<br>
 * .输入流 ---> 选择器.........................................选择器 ---> 输出流<br>
 * ..............................--->拷贝--->++++++缓冲区+++++++<br>
 * </p>
 * MJpeg视频流是一系列的Jpeg图片流，视频的原理实质上就是由一帧一帧的图片构成，电影每秒播放至少25帧，<br/>
 * MJpeg视频流只需要达到每秒15帧就可以看起来非常流畅。构造MJpeg视频流的方法比较简单，首先，需要在请<br/>
 * 求的头部声明是multipart/x-mixed-replace传输类型然后每一帧图片发送前，头部加上mime类型，<br/>
 * 也就是Content-type: image/jpeg。具体的头部格式请参考下面英文介绍<br/>
 * extend {@link DataStream}, provide exclusive function for generating mJpeg stream<br/>
 * <p>
 * ..............................--->copy--->++++++buffer+++++++<br>
 * .inputStream ---> selector...................................selector ---> outputStream<br>
 * ..............................--->copy--->++++++buffer+++++++<br>
 * </p>
 * MJpeg Stream is a serial of jpeg pictures, so the important thing is the header.<br>
 * The header format is show below, pay attention to the line break signal \r\n <br/>
 * <ul>
 * <li>HTTP/1.0 200 OK</li>
 * <li>Server: Lego Http Server\r\n</li>
 * <li>Connection: close\r\n</li>
 * <li>Max-Age: 0\r\n</li>
 * <li>Expires: -1\r\n</li>
 * <li>Cache-Control: no-store, no-cache, must-revalidate, pre-check=0, post-check=0, max-age=0\r\n</li>
 * <li>Pragma: no-cache\r\n</li>
 * <li>Access-Control-Allow-Origin: *\r\n</li>
 * <li>Content-Type: multipart/x-mixed-replace;boundary=LegoHttpServer.MULTIPART_BOUNDARY\r\n"</li>
 * <li>\r\n</li>
 * <li>--LegoHttpServer.MULTIPART_BOUNDARY\r\n"</li>
 * </ul>
 * <br/>
 * After adding header, you should construct each frame with this format
 * <ul>
 * <li>Content-type: image/jpeg\r\n</li>
 * <li>Content-Length: mJpegFrame.length</li>
 * <li>X-Timestamp: Timestamp\r\n</li>
 * <li>\r\n</li>
 * <li>mJpegFrame</li>
 * <li>\r\n--LegoHttpServer.MULTIPART_BOUNDARY\r\n</li>
 * </ul>
 * <p>
 */
public final class MJpegStream extends DataStream {

    @SuppressWarnings("unused")
    private static final String TAG = "MJpegStream";

    private static final String MJpegStreamBoundaryLine = "\r\n--" + LegoHttpServer.MULTIPART_BOUNDARY + "\r\n";

    /**
     * MJpeg Stream response header<br/>
     * MJpeg 视频流响应头部
     */
    private static final String MJpegStreamCommonHeader
            = "HTTP/1.0 200 OK\r\n" +
            "Server: Lego Http Server\r\n" +
            "Connection: close\r\n" +
            "Max-Age: 0\r\n" +
            "Expires: -1\r\n" +
            "Cache-Control: no-store, no-cache, must-revalidate, pre-check=0, post-check=0, max-age=0\r\n" +
            "Pragma: no-cache\r\n" +
            "Access-Control-Allow-Origin: *\r\n" +
            "Content-Type: multipart/x-mixed-replace; " +
            "boundary=" + LegoHttpServer.MULTIPART_BOUNDARY + "\r\n" +
            MJpegStreamBoundaryLine;

    private static final String MJpegStreamFrameHeader
            = "Content-type: image/jpeg" +
            "Content-Length: %1$s" +
            "X-Timestamp: %2$s\r\n" +
            "\r\n";

    /**
     * 参见 {@link MJpegStream}
     *
     * @throws IOException
     */
    public MJpegStream() throws IOException {
        super();
    }

    /**
     * Add a common mJpeg stream header, the common header format is bellow<br/>
     * 添加一个已知的MJpeg视频流请求头
     * 头部格式：
     */
    public void addCommonStreamHeader() throws IOException, NullPointerException {
        OutputStream outputStream = getOutputStream();
        if (outputStream == null) {
            throw new NullPointerException("[MJpegStream] mOutputStream is null");
        }
        outputStream.write(MJpegStreamCommonHeader.getBytes());
        outputStream.flush();
    }

    /**
     * send a frame of jpeg format to server<br/>
     * 发送一帧Jpeg图片到服务器
     *
     * @param jpegFrame JPeg图片数据，byte[]类型，
     * @param length    JPeg数据流长度
     * @param timestamp 时间戳
     */
    public void sendFrame(final byte[] jpegFrame, final int length, final long timestamp) throws IOException, NullPointerException {
        OutputStream outputStream = getOutputStream();
        if (outputStream == null) {
            throw new NullPointerException("[MJpegStream] mOutputStream is null");
        }

        //// TODO: 16-3-27 双缓冲
        String frameHeader = String.format(MJpegStreamFrameHeader, length, timestamp);
        outputStream.write(frameHeader.getBytes());
        outputStream.write(jpegFrame);
        outputStream.write(MJpegStreamBoundaryLine.getBytes());

        outputStream.flush();
    }
}
