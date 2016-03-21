package com.legaocar.rison.android;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.TextView;

import com.legaocar.rison.android.server.LegoServer;
import com.legaocar.rison.android.util.MToastUtil;
import com.legaocar.rison.android.util.NetWorkUtil;

import java.io.InputStream;
import java.util.Properties;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {
    private static final String TAG = "MainActivity";
    private View mStartService;

    private LegoServer mWebServer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        initViews();

        if (mWebServer == null) {
            mWebServer = NetWorkUtil.initWebServer(this, doQuery);
            TextView start = (TextView) mStartService.findViewById(R.id.hint);
            if (mWebServer != null) {
                String address = "http://" + NetWorkUtil.wifiIpAddress(this) + ":" + NetWorkUtil.ServerPort;
                start.setText(getString(R.string.main_service_hint_start, address));
            } else {
                start.setText(R.string.main_service_hint_stop);
            }
        }

    }

    @SuppressWarnings("null")
    private void initViews() {
        mStartService = findViewById(R.id.main_start_service);
        if (mStartService != null) {
            mStartService.setOnClickListener(this);
            mStartService.findViewById(R.id.toggle).setVisibility(View.INVISIBLE);
        }

    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.main_start_service: {
                MToastUtil.show(this, "点击了启用服务");
            }
            break;
            default:
                break;
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        if (mWebServer != null) {
            mWebServer.stop();
            mWebServer = null;
        }
    }

    private LegoServer.CommonGatewayInterface doQuery = new LegoServer.CommonGatewayInterface() {
        @Override
        public String run(Properties parms) {
            String ret = "hello rison";
            return ret;
        }

        @Override
        public InputStream streaming(Properties parms) {
            return null;
        }
    };
}
