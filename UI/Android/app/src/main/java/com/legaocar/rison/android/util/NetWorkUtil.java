package com.legaocar.rison.android.util;

import android.content.Context;
import android.net.wifi.WifiManager;
import android.util.Log;

import com.legaocar.rison.android.server.LegoServer;

import java.io.IOException;
import java.math.BigInteger;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.ByteOrder;

/**
 * Created by rison on 16-3-21.
 * 网络控制工具类
 */
public class NetWorkUtil {

    private LegoServer mWebServer;

    public static final int ServerPort = 8080;
    public static final int StreamingPort = 8088;

    private static NetWorkUtil mInstance;

    public static NetWorkUtil getInstance() {
        if (mInstance == null) {
            synchronized (NetWorkUtil.class) {
                if (mInstance == null) {
                    mInstance = new NetWorkUtil();
                }
            }
        }
        return mInstance;
    }

    public LegoServer getWebServer(Context context) {
        if (mWebServer != null) {
            return mWebServer;
        }

        String ipAddr = wifiIpAddress(context);
        if (ipAddr != null) {
            try {
                mWebServer = new LegoServer(ServerPort, context.getApplicationContext());
            } catch (IOException e) {
                mWebServer = null;
            }
        }
        return mWebServer;
    }

    /**
     * @return 当前链接的wifi地址
     */
    public static String wifiIpAddress(Context context) {
        WifiManager wifiManager = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
        int ipAddress = wifiManager.getConnectionInfo().getIpAddress();

        // Convert little-endian to big-endianif needed
        if (ByteOrder.nativeOrder().equals(ByteOrder.LITTLE_ENDIAN)) {
            ipAddress = Integer.reverseBytes(ipAddress);
        }

        byte[] ipByteArray = BigInteger.valueOf(ipAddress).toByteArray();

        String ipAddressString;
        try {
            ipAddressString = InetAddress.getByAddress(ipByteArray).getHostAddress();
        } catch (UnknownHostException ex) {
            Log.e("WIFIIP", "Unable to get host address.");
            ipAddressString = null;
        }

        return ipAddressString;
    }
}
