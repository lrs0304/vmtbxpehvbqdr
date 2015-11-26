package com.legaocar.rison.android;

import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.view.Window;
import android.view.WindowManager;

import com.legaocar.rison.android.util.MToastUtil;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {
    private View mStartService;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        initViews();
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
}
