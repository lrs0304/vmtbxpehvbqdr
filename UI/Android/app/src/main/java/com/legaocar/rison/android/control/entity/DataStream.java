package com.legaocar.rison.android.control.entity;

import android.net.LocalServerSocket;
import android.net.LocalSocket;
import android.net.LocalSocketAddress;
import android.os.ParcelFileDescriptor;
import android.util.Log;

import java.io.FileDescriptor;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * A fifo server provide inputStream for NanoHttpD server
 * 基于NanoHttpD的设计逻辑，如果需要传输流到网页或移动设备，需要提供一个输入流到NanoHttpD迷你服务器
 */
public final class DataStream {
    @SuppressWarnings("unused")
    private static final String TAG = "DataStream";

    // 由于 Android5.0 对 LocalSocket 做出来安全限制，使用 ParcelFileDescriptor 取代
    private ParcelFileDescriptor[] mParcelFileDescriptors;
    protected ParcelFileDescriptor mParcelRead;
    protected ParcelFileDescriptor mParcelWrite;

    protected InputStream mInputStream;
    protected OutputStream mOutputStream;

    public DataStream() throws IOException {
        mParcelFileDescriptors = ParcelFileDescriptor.createPipe();
        mParcelRead = new ParcelFileDescriptor(mParcelFileDescriptors[0]);
        mParcelWrite = new ParcelFileDescriptor(mParcelFileDescriptors[1]);
    }

    /**
     * inputStream, get data from outputStream and throw to consumer (like NanoHttpServer etc.)
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
