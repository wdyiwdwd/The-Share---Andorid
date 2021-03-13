package com.example.spi_ider.testsecond;

import android.annotation.SuppressLint;
import android.os.Bundle;

import com.hyphenate.easeui.ui.EaseBaseActivity;

@SuppressLint("Registered")
public class BaseActivity extends EaseBaseActivity {
    @Override
    protected void onCreate(Bundle arg0) {
        super.onCreate(arg0);
    }

    @Override
    protected void onResume() {
        super.onResume();
        // umeng
        //MobclickAgent.onResume(this);
    }

    @Override
    protected void onStart() {
        super.onStart();
        // umeng
       // MobclickAgent.onPause(this);
    }

}
