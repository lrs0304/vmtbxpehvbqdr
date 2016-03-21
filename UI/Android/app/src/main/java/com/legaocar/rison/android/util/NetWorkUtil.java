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

    public static final int ServerPort = 8080;
    public static final int StreamingPort = 8088;

    public static LegoServer initWebServer(Context context, LegoServer.CommonGatewayInterface doQuery) {
        String ipAddr = wifiIpAddress(context);
        LegoServer webServer = null;
        if (ipAddr != null) {
            try {
                webServer = new LegoServer(ServerPort, context);
                webServer.registerCGI("/cgi/query", doQuery);
            } catch (IOException e) {
                webServer = null;
            }
        }
        return webServer;
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
