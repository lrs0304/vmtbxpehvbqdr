package com.legaocar.rison.android.server;

import android.content.Context;
import android.util.Log;

import com.legaocar.rison.android.util.MLog;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Properties;
import java.util.Random;

public class LegoServer extends NanoHTTPD {
    static final String TAG = "LegoServer";

    /**
     * @param port 端口
     * @param ctx  应用上下文
     */
    public LegoServer(int port, Context ctx) throws IOException {
        super(port, ctx.getAssets());
    }

    /**
     * @param port    端口
     * @param wwwPath 资源路径
     */
    @SuppressWarnings("unused")
    public LegoServer(int port, String wwwPath) throws IOException {
        super(port, new File(wwwPath).getAbsoluteFile());
    }

    @Override
    public Response serve(String uri, String method, Properties header, Properties parms, Properties files) {
        MLog.w(TAG, "httpd request >>" + method + " '" + uri + "' " + "   " + parms);

        if (uri.startsWith("/cgi/")) {
            return serveCGI(uri, method, header, parms, files);
        } else if (uri.startsWith("/stream/")) {
            return serveStream(uri, method, header, parms, files);
        } else {
            return super.serve(uri, method, header, parms, files);
        }
    }

    public Response serveStream(String uri, String method, Properties header, Properties parms, Properties files) {
        CommonGatewayInterface cgi = cgiEntries.get(uri);
        if (cgi == null)
            return null;

        InputStream ins;
        ins = cgi.streaming(parms);
        if (ins == null)
            return null;

        Random rnd = new Random();
        String etag = Integer.toHexString(rnd.nextInt());
        String mime = parms.getProperty("mime");
        if (mime == null)
            mime = "application/octet-stream";
        Response res = new Response(HTTP_OK, mime, ins);
        res.addHeader("ETag", etag);
        res.isStreaming = true;

        return res;
    }

    public Response serveCGI(String uri, String method, Properties header, Properties parms, Properties files) {
        CommonGatewayInterface cgi = cgiEntries.get(uri);
        if (cgi == null)
            return null;

        String msg = cgi.run(parms);
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
        }
    }

    public interface CommonGatewayInterface {
        String run(Properties parms);

        InputStream streaming(Properties parms);
    }

    private HashMap<String, CommonGatewayInterface> cgiEntries = new HashMap<>();

    public void registerCGI(String uri, CommonGatewayInterface cgi) {
        if (cgi != null)
            cgiEntries.put(uri, cgi);
    }

}
