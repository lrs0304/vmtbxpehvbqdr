package com.legaocar.rison.android;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.TextView;

import com.legaocar.rison.android.control.streamservice.CameraStreamServiceActivity;
import com.legaocar.rison.android.server.LegoHttpServer;
import com.legaocar.rison.android.util.NetWorkUtil;

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

        if (mWebServer == null) {
            mWebServer = NetWorkUtil.getInstance().getWebServer(this);
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
                CameraStreamServiceActivity.start(this);
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
}
