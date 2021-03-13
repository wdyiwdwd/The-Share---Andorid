package com.example.spi_ider.testsecond;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.view.Window;

import com.hyphenate.chat.EMClient;
import com.hyphenate.easeui.ui.EaseChatFragment;

/**
 * Created by Spi-ider on 2017/4/28.
 */

public class ChattingActivity extends FragmentActivity {
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.activity_chatting);

        //EaseChatFragment easeChatFragment = new EaseChatFragment();    //环信聊天界面
        //easeChatFragment.setArguments(getIntent().getExtras()); //需要的参数
        ChatFragment chatFragment = new ChatFragment();
        chatFragment.setArguments(getIntent().getExtras());
        getSupportFragmentManager().beginTransaction().add(R.id.layout_chat,chatFragment).commit();  //Fragment切换
    }

}
