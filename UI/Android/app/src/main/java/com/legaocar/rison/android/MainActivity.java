package com.legaocar.rison.android;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.TextView;

import com.legaocar.rison.android.control.streamservice.CameraStreamServiceActivity;
import com.legaocar.rison.android.server.LegoHttpServer;
import com.legaocar.rison.android.util.MLogUtil;
import com.legaocar.rison.android.util.NativeUtil;
import com.legaocar.rison.android.util.NetWorkUtil;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.Properties;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {
    @SuppressWarnings("unused")
    private static final String TAG = "MainActivity";
    private View mStartService;

    private LegoHttpServer mWebServer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main_activity);

        initViews();
        MLogUtil.i(TAG, NativeUtil.getInstance().stringFromJNI());

        if (mWebServer == null) {
            mWebServer = NetWorkUtil.getInstance().getWebServer(this);
            TextView start = (TextView) mStartService.findViewById(R.id.hint);
            if (mWebServer != null) {
                String address = "http://" + NetWorkUtil.getLocalIPAddress(this) + ":" + NetWorkUtil.ServerPort;
                start.setText(getString(R.string.main_service_hint_start, address));
            } else {
                start.setText(R.string.main_service_hint_stop);
            }
        }

    }

    private void initViews() {
        mStartService = findViewById(R.id.main_start_service);
        if (mStartService != null) {
            mStartService.setOnClickListener(this);
            mStartService.findViewById(R.id.toggle).setVisibility(View.INVISIBLE);
        }

        View debugJniView = findViewById(R.id.main_debug_jni);
        if (debugJniView != null) {
            debugJniView.setOnClickListener(this);
            ((TextView) debugJniView.findViewById(R.id.hint)).setText(R.string.main_debug_jni);
            debugJniView.findViewById(R.id.toggle).setVisibility(View.INVISIBLE);
        }
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.main_start_service: {
                CameraStreamServiceActivity.start(this);
            }
            break;
            case R.id.main_debug_jni: {
                mWebServer.registerStream("/video/capture.jpg", new LegoHttpServer.CommonGatewayInterface() {
                    @Override
                    public String run(Properties params) {
                        return null;
                    }

                    @Override
                    public InputStream streaming(Properties params) {

                        try {
                            InputStream in = getResources().openRawResource(R.raw.origin);
                            int length = in.available();
                            byte[] originYUV = new byte[length];
                            in.read(originYUV);
                            byte[] jpg = new byte[length];
                            int size = (int) NativeUtil.getInstance().compressYuvToJpeg(originYUV, jpg, 1, 75, 480, 320);
                            byte[] newJpg = new byte[size];
                            System.arraycopy(jpg, 0, newJpg, 0, size);
                            return new ByteArrayInputStream(newJpg);
                        } catch (final Exception e) {
                            return null;
                        }
                    }
                });
            }
            default:
                break;
        }
    }

    @Override
    protected void onDestroy() {

        if (mWebServer != null) {
            mWebServer.stop();
            mWebServer = null;
        }
        NetWorkUtil.getInstance().destroyWebServer();

        super.onDestroy();
    }

    /**
     * static最先加载，只填写模块名，不包含文件名前的lib和末尾的.so字段
     */
    static {
        System.loadLibrary("decodeYUV");
    }
}
