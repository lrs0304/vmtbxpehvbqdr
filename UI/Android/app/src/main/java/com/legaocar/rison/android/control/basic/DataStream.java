package com.legaocar.rison.android.control.basic;

import android.os.ParcelFileDescriptor;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * A fifo server provide inputStream for NanoHttpD server<br/>
 * 基于 {@link com.legaocar.rison.android.server.NanoHTTPD}的设计逻辑，如果需要传输流到网页或移动设备，需要提供一个输入流到NanoHttpD迷你服务器
 */
public class DataStream {
    @SuppressWarnings("unused")
    private static final String TAG = "DataStream";

    /**
     * 由于 Android5.0 对 LocalSocket 做出来安全限制，使用 ParcelFileDescriptor 取代
     * http://www.apihome.cn/api/android/ParcelFileDescriptor.html
     */
    private ParcelFileDescriptor[] mParcelFileDescriptors;
    protected ParcelFileDescriptor mParcelRead;
    protected ParcelFileDescriptor mParcelWrite;

    private InputStream mInputStream;
    private OutputStream mOutputStream;

    /**
     * 参见 {@link DataStream}
     *
     * @throws IOException
     */
    public DataStream() throws IOException {
        mParcelFileDescriptors = ParcelFileDescriptor.createPipe();
        mParcelRead = new ParcelFileDescriptor(mParcelFileDescriptors[0]);
        mParcelWrite = new ParcelFileDescriptor(mParcelFileDescriptors[1]);
    }

    /**
     * inputStream, get data from outputStream and throw to consumer (like NanoHttpServer etc.)
     * 输入流，提供给消费者或者服务端
     *
     * @throws IOException
     */
    public InputStream getInputStream() throws IOException {
        if (mParcelRead == null) {
            return null;
        }

        if (mInputStream == null) {
            mInputStream = new ParcelFileDescriptor.AutoCloseInputStream(mParcelRead);
        }
        return mInputStream;
    }

    /**
     * outputStream, which enables you to write data to server
     * 输出流，面向内容产生，内部将输入的内容提供给输入流，从而形成fifo结构
     *
     * @throws IOException
     */
    public OutputStream getOutputStream() throws IOException {
        if (mParcelWrite == null) {
            return null;
        }

        if (mOutputStream == null) {
            mOutputStream = new ParcelFileDescriptor.AutoCloseOutputStream(mParcelWrite);
        }

        return mOutputStream;
    }

    /**
     * close streamer if do not use any more
     * Android机器上的资源比较紧张，既是释放
     */
    public void release() {

        try {
            if (mParcelRead != null) {
                mParcelRead.close();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        try {
            if (mParcelWrite != null) {
                mParcelWrite.close();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        mParcelRead = null;
        mParcelWrite = null;
        mParcelFileDescriptors = null;
    }

    @Override
    public String toString() {
        return "DataStream [receiver=" + mParcelRead + ", sender=" + mParcelWrite + "]";
    }

}
