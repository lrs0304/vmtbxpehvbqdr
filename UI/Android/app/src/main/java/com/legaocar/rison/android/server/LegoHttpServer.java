package com.legaocar.rison.android.server;

import android.content.Context;

import com.legaocar.rison.android.util.MLogUtil;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Properties;
import java.util.Random;

/**
 * 对NanoHTTPd进行封装，方便升级以及以后操作
 */
@SuppressWarnings("unused")
public class LegoHttpServer extends NanoHTTPD {

    private static final String TAG = "LegoHttpServer";

    private HashMap<String, CommonGatewayInterface> cgiEntries = new HashMap<>();
    private HashMap<String, CommonGatewayInterface> streamEntries = new HashMap<>();

    public interface CommonGatewayInterface {
        String run(Properties params);

        InputStream streaming(Properties params);
    }

    /**
     * @param port 端口
     * @param ctx  应用上下文
     */
    public LegoHttpServer(String host, int port, Context ctx) throws IOException {
        super(host, port, ctx.getAssets());
    }

    /**
     * @param port    端口
     * @param wwwPath 资源路径
     */
    @SuppressWarnings("unused")
    public LegoHttpServer(String host, int port, String wwwPath) throws IOException {
        super(host, port, new File(wwwPath).getAbsoluteFile());
    }

    @Override
    public Response serve(String uri, String method, Properties header, Properties parms, Properties files) {
        MLogUtil.w(TAG, "httpd request >>" + method + " '" + uri + "' " + "   " + parms);

        if (uri.startsWith("/cgi/")) {
            return serveCGI(uri, method, header, parms, files);
        } else if (uri.startsWith("/video/")) {
            return serveStream(uri, method, header, parms, files);
        } else {
            return super.serve(uri, method, header, parms, files);
        }
    }

    public Response serveStream(String uri, String method, Properties header, Properties params, Properties files) {
        CommonGatewayInterface stream = streamEntries.get(uri);
        if (stream == null)
            return null;

        InputStream ins;
        ins = stream.streaming(params);
        if (ins == null)
            return null;

        Random rnd = new Random();
        String eTag = Integer.toHexString(rnd.nextInt());
        String mime = params.getProperty("mime");
        //        if (mime == null)
        //            mime = "application/octet-stream";
        Response res = new Response(HTTP_OK, mime, ins);
        res.addHeader("ETag", eTag);

        res.isStreaming = uri.contains("live");//live.mjpg

        return res;
    }

    /**
     * 相应指令操作
     */
    public Response serveCGI(String uri, String method, Properties header, Properties params, Properties files) {
        CommonGatewayInterface cgi = cgiEntries.get(uri);
        if (cgi == null)
            return null;

        String msg = cgi.run(params);
        if (msg == null)
            return null;

        return new Response(HTTP_OK, MIME_PLAINTEXT, msg);
    }

    @Override
    public void serveDone(Response r) {
        try {
            if (r.isStreaming) {
                r.data.close();
            }
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }

    public void registerCGI(String uri, CommonGatewayInterface cgi) {
        if (cgi != null)
            cgiEntries.put(uri, cgi);
    }

    public void registerStream(String uri, CommonGatewayInterface stream) {
        streamEntries.put(uri, stream);
    }
}
