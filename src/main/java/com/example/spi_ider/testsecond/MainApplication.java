package com.example.spi_ider.testsecond;

import android.app.Application;

import com.hyphenate.chat.EMClient;
import com.hyphenate.chat.EMOptions;
import com.hyphenate.easeui.controller.EaseUI;

public class MainApplication extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        initEasemob();
    }
    public void initEasemob(){
        EMOptions options = new EMOptions();
        options.setAcceptInvitationAlways(false);
        EaseUI.getInstance().init(this, null);
        EMClient.getInstance().init(this, options);
        //EMClient.getInstance().setDebugMode(true);
    }
}
