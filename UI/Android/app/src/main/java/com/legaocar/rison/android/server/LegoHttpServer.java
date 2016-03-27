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
 * 对NanoHTTPd进行封装，方便升级以及以后操作，可操作的方法有
 * <ul>
 * <li>{@link #LegoHttpServer(String, int, Context)} 传入ip地址以及绑定端口初始化{@link LegoHttpServer}</li>
 * <li>{@link #registerStream(String, CommonGatewayInterface)} 注册流响应</li>
 * <li>{@link #registerCGI(String, CommonGatewayInterface)} 注册指令响应</li>
 * <li>{@link #serve(String, String, Properties, Properties, Properties)} 对服务器请求做出响应的响应</li>
 * </ul>
 */
public class LegoHttpServer extends NanoHTTPD {

    private static final String TAG = "LegoHttpServer";

    private HashMap<String, CommonGatewayInterface> cgiEntries = new HashMap<>();
    private HashMap<String, CommonGatewayInterface> streamEntries = new HashMap<>();

    /**
     * 由于重写了serve函数以服务传输指令和视频流类型，因此需要提供一个回调函数以让serve函数获取相应的数据响应服务器<br/>
     * 函数 {@link #run(Properties)} 相应服务器cgi指令<br/>
     * 函数 {@link #streaming(Properties)} 相应服务器流传输}
     */
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

    /**
     * 重写serve服务函数，可同时服务指令控制，截图传输，视频传输<br/>
     * 函数用法参考{@link #serveCGI(String, String, Properties, Properties, Properties)}
     *
     * @param uri    Percent-decoded URI without parameters, for example "/index.cgi"
     * @param method "GET", "POST" etc.
     * @param header Header entries, percent decoded
     * @param params Parsed, percent decoded parameters from URI and, in case of POST, data.
     */
    @Override
    public Response serve(String uri, String method, Properties header, Properties params, Properties files) {
        MLogUtil.w(TAG, "http request >>" + method + " '" + uri + "' " + "   " + params);

        if (uri.startsWith("/cgi/")) {
            return serveCGI(uri, method, header, params, files);
        } else if (uri.startsWith("/video/")) {
            return serveStream(uri, method, header, params, files);
        } else {
            return super.serve(uri, method, header, params, files);
        }
    }

    /**
     * 参考 {@link #serveCGI(String, String, Properties, Properties, Properties)}<br/>
     * 由于这里是服务流，因此把header忽略，由inputStream控制头部
     */
    @SuppressWarnings("unused")
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
     * 提供给网页端控制面板，通过控制面板操控配置应用，获取本地传感器数据或者摄像头数据等等
     *
     * @param uri    Api指令
     * @param method 网络请求方法 GET, POST, PUT, DELETE 等等
     * @param header 请求头
     * @param params 请求参数
     * @param files  文件列表
     */
    @SuppressWarnings("unused")
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

    /**
     * 绑定对来自服务器Api的相应操作
     *
     * @param uri Api指令
     * @param cgi 本地指令相应
     */
    public void registerCGI(String uri, CommonGatewayInterface cgi) {
        if (cgi != null)
            cgiEntries.put(uri, cgi);
    }

    /**
     * 绑定对来自服务器流请求的相应操作
     *
     * @param uri    Api指令
     * @param stream 本地的输入流
     */
    public void registerStream(String uri, CommonGatewayInterface stream) {
        streamEntries.put(uri, stream);
    }
}
