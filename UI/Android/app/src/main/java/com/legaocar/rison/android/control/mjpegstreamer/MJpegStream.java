package com.legaocar.rison.android.control.mjpegstreamer;

import com.legaocar.rison.android.control.basic.DataStream;
import com.legaocar.rison.android.server.LegoHttpServer;
import com.legaocar.rison.android.util.MLogUtil;

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
 * <p/>
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
            = "Server: Lego Http Server 1.0\r\n" +
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
     * 有时候可能机器比较老，无法及时传递下一帧进来，为了节省带宽，我们可以等到下一帧拷贝完成了再传输<br/>
     * To save the bandwidth of network, give the picture a tag indicate whether is a new frame. <br/>
     * If it's not a new frame, do not send the old buffer.
     */
    private static final byte NEW_JPG_NONE = 0x0, NEW_JPG_A = 0x1, NEW_JPG_B = 0x2;
    private byte isNewJpeg;
    /**
     * 双缓冲
     */
    private byte[] mBufferA;
    private byte[] mBufferB;
    private int mBufferALength, mBufferBLength;
    private long mTimeStampA;
    private long mTimeStampB;
    private int mBufferLength;
    private boolean isStreamingBufferA;

    /**
     * Worker,send frames one by one
     * 用于将传来的缓存一帧一帧的分发
     */
    private Thread mWorker;
    private boolean isWorkerRunning;

    /**
     * 参见 {@link MJpegStream}
     *
     * @throws IOException
     */
    public MJpegStream() throws IOException {
        super();

        isNewJpeg = NEW_JPG_NONE;

        isWorkerRunning = true;
        mWorker = new Thread(mWorkerRunnable);
        mWorker.start();
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

        final byte[] sendBuffer;
        if (isStreamingBufferA) {// 如果此时正在发送BufferA，我们可以先把图像拷贝给BufferB
            if (mBufferB == null || mBufferB.length < length) {
                mBufferB = new byte[length];//todo 找个时间了解下这个操作是否为深拷贝jpegFrame.clone()
            }

            sendBuffer = mBufferB;
            mBufferBLength = length;
            mTimeStampB = timestamp;
            isNewJpeg = NEW_JPG_A;
        } else {
            if (mBufferA == null || mBufferA.length < length) {
                mBufferA = new byte[length];
            }

            sendBuffer = mBufferA;
            mBufferALength = length;
            mTimeStampA = timestamp;
            isNewJpeg = NEW_JPG_B;
        }


        mBufferLength = length;
        System.arraycopy(jpegFrame, 0, sendBuffer, 0, length);
    }

    /**
     * do release the worker and memory
     */
    @Override
    public void release() {
        MLogUtil.d(TAG, "release mJpeg stream");
        stopWorker();
        super.release();
    }

    /**
     * 由于现在的逻辑，外部结构无法获知outputStream是否关闭
     *
     * @return 是否
     */
    public boolean isAlive() {
        OutputStream outputStream;
        try {
            outputStream = getOutputStream();
        } catch (final Exception e) {
            e.printStackTrace();
            return false;
        }
        return isWorkerRunning && outputStream != null;
    }

    /**
     * stop worker and release the object it holds
     * 停止帧转发线程
     */
    private void stopWorker() {
        if (!isWorkerRunning || mWorker == null) {
            return;
        }
        isWorkerRunning = false;
        mWorker.interrupt();
        mWorkerRunnable = null;
    }

    /**
     * 参见 {@link #mWorker}
     * send frames one by one
     */
    private Runnable mWorkerRunnable = new Runnable() {
        @Override
        public void run() {

            byte[] bufferSend;
            byte[] boundaryLine = MJpegStreamBoundaryLine.getBytes();
            long timestamp;
            int bufferLength;
            String frameHeader;
            OutputStream outputStream;

            while (isWorkerRunning) {

                /**
                 *  wait for a new jpg frame
                 *  当产生新的数据时才继续线程，否则等待
                 */
                while (isNewJpeg == NEW_JPG_NONE) {
                    try {
                        /**
                         * 对于视频来说，60帧已经非常流畅，因此我们可以把休眠间隔加长些。最坏的情况是延迟了 4 毫秒
                         */
                        Thread.sleep(2);
                    } catch (final InterruptedException e) {
                        // ignore
                    }
                }

                isNewJpeg = NEW_JPG_NONE;

                // change another buffer to stream
                isStreamingBufferA = !isStreamingBufferA;
                if (isStreamingBufferA) {
                    bufferSend = mBufferA;
                    bufferLength = mBufferALength;
                    timestamp = mTimeStampA;
                } else {
                    bufferSend = mBufferB;
                    bufferLength = mBufferBLength;
                    timestamp = mTimeStampB;
                }

                // send to server
                frameHeader = String.format(MJpegStreamFrameHeader, mBufferLength, timestamp);
                try {
                    outputStream = getOutputStream();
                    outputStream.write(frameHeader.getBytes());
                    outputStream.write(bufferSend, 0, bufferLength);
                    outputStream.write(boundaryLine);
                    outputStream.flush();

                } catch (final Exception e) {
                    // TODO: 16-3-27 应该想办法告知线程
                    e.printStackTrace();
                    stopWorker();
                    break;
                }
            }
        }
    };
}
