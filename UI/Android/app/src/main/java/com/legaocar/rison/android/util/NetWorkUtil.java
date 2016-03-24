package com.legaocar.rison.android.util;

import android.content.Context;
import android.net.wifi.WifiManager;
import android.text.TextUtils;

import com.legaocar.rison.android.R;
import com.legaocar.rison.android.server.LegoHttpServer;

import java.io.IOException;
import java.math.BigInteger;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.nio.ByteOrder;
import java.util.Enumeration;

/**
 * Created by rison on 16-3-21.
 * 网络控制工具类
 */
public class NetWorkUtil {

    private static final String TAG = "NetWorkUtil";
    /**
     * 应该使用service
     */
    private LegoHttpServer mWebServer;

    public static final int ServerPort = 8080;

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

    public LegoHttpServer getWebServer(Context context) {
        if (mWebServer != null) {
            return mWebServer;
        }

        String ipAddress = getLocalIPAddress(context);

        if (ipAddress != null) {
            try {
                mWebServer = new LegoHttpServer(ipAddress, ServerPort, context.getApplicationContext());
            } catch (IOException e) {
                mWebServer = null;
            }
        }
        return mWebServer;
    }

    public void destroyWebServer(){
        mWebServer = null;
    }

    /**
     * @return 当前链接的wifi地址
     */
    public static String getWifiIpAddress(Context context) {
        WifiManager wifiManager = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
        int ipAddress = wifiManager.getConnectionInfo().getIpAddress();

        // Convert little-endian to big-endian if needed
        if (ByteOrder.nativeOrder().equals(ByteOrder.LITTLE_ENDIAN)) {
            ipAddress = Integer.reverseBytes(ipAddress);
        }

        byte[] ipByteArray = BigInteger.valueOf(ipAddress).toByteArray();

        String ipAddressString;
        try {
            ipAddressString = InetAddress.getByAddress(ipByteArray).getHostAddress();
        } catch (UnknownHostException ex) {
            MLogUtil.e("WIFI IP", "Unable to get host address.");
            ipAddressString = null;
        }

        return ipAddressString;
    }

    public static String getLocalIPAddress(Context context) {
        String wifiAddress = getWifiIpAddress(context);
        if (!TextUtils.isEmpty(wifiAddress)) {
            // wifi address first
            return wifiAddress;
        }

        try {
            for (Enumeration<NetworkInterface> en = NetworkInterface.getNetworkInterfaces(); en.hasMoreElements(); ) {
                NetworkInterface intf = en.nextElement();
                for (Enumeration<InetAddress> enumIpAddr = intf.getInetAddresses(); enumIpAddr.hasMoreElements(); ) {
                    InetAddress inetAddress = enumIpAddr.nextElement();
                    if (!inetAddress.isLoopbackAddress() && inetAddress instanceof Inet4Address) {
                        MToastUtil.show(context, R.string.network_may_can_not_be_reach);
                        return inetAddress.getHostAddress();
                    }
                }
            }
        } catch (SocketException ex) {
            MLogUtil.d(TAG, "[getLocalIPAddress] Error retrieving IP address");
            ex.printStackTrace();
        }
        return "";
    }

}
