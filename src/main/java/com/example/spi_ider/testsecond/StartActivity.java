package com.example.spi_ider.testsecond;

import android.app.Activity;
import android.app.ListActivity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.os.PersistableBundle;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.Toast;

import com.example.spi_ider.testsecond.voicecall.CallDialogManager;
import com.hyphenate.chat.EMClient;
import com.hyphenate.chat.EMMessage;
import com.hyphenate.easeui.EaseConstant;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class StartActivity extends Activity {
    private ListView contact_list;
    private android.support.v7.widget.Toolbar toolbar;
    private String guest;

    List<HashMap<String,Object>> friend_list;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_start);
        friend_list = new ArrayList<HashMap<String,Object>>();
        contact_list=(ListView)findViewById(R.id.contact_list);
       /* request.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                guest = guest_name.getText().toString().trim();
                if (!TextUtils.isEmpty(guest)){
                    Intent chat = new Intent(StartActivity.this,ChattingActivity.class);
                    chat.putExtra(EaseConstant.EXTRA_USER_ID,guest);  //对方账号
                    chat.putExtra(EaseConstant.EXTRA_CHAT_TYPE, EMMessage.ChatType.Chat); //单聊模式
                    startActivity(chat);
                }else{
                    Toast.makeText(StartActivity.this, "请输入要聊天的对方的账号", Toast.LENGTH_SHORT).show();
                }
            }
        });*/
        /*logout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Logout();
            }
        });联系人列表*/
        SimpleAdapter adapter = new SimpleAdapter(StartActivity.this,getFriends(),R.layout.list_item,new String[]{"icon","name","wisdom"},new int[]{R.id.friend_icon,R.id.friend_name,R.id.friend_wisdom});
        contact_list.setAdapter(adapter);
        contact_list.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Intent chat = new Intent(StartActivity.this,ChattingActivity.class);
                chat.putExtra(EaseConstant.EXTRA_USER_ID,""+friend_list.get(position).get("name"));  //对方账号
                chat.putExtra(EaseConstant.EXTRA_CHAT_TYPE, EMMessage.ChatType.Chat); //单聊模式
                startActivity(chat);
                //Toast.makeText(StartActivity.this,""+(friend_list.get(position).get("name")),Toast.LENGTH_LONG).show();
            }
        });
        toolbar = (Toolbar) findViewById(R.id.toolbar);
        toolbar.setTitle(R.string.start_title);
        toolbar.inflateMenu(R.menu.toolbar_item);
        toolbar.setOnMenuItemClickListener(new Toolbar.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                switch (item.getItemId()){
                    case R.id.add_friend_item:
                        Toast.makeText(StartActivity.this, "敬请期待", Toast.LENGTH_SHORT).show();
                        break;
                    case R.id.logout_item:
                        Logout();
                        break;
                }
                return false;
            }
        });
        IntentFilter callFilter = new IntentFilter(EMClient.getInstance().callManager().getIncomingCallBroadcastAction());
        //callFilter.addCategory("android.intent.category.DEFAULT");
        registerReceiver(new CallReceiver(), callFilter);//监听呼入通话
    }
    private List<HashMap<String,Object>> getFriends(){
        addFriend("cqxtest","个人签名",R.mipmap.man_avator_1);
        addFriend("nkutest","Story of my life",R.mipmap.man_avator_2);
        addFriend("test_user3","@#$%^&*((*&^",R.mipmap.man_avator_3);
        addFriend("test_user4","个人签名",R.mipmap.man_avator_4);
        return friend_list;
    }
    private List<HashMap<String,Object>> addFriend(String name,String wisdom ,Object img){
        HashMap<String,Object> new_friend=new HashMap<>();
        new_friend.put("icon",img==null?R.mipmap.list_item_icon:img);
        new_friend.put("name",name);
        new_friend.put("wisdom",wisdom);
        friend_list.add(new_friend);
        return friend_list;
    }
    private void Logout(){
        EMClient.getInstance().logout(true);
        startActivity(new Intent(StartActivity.this,MainActivity.class));
        finish();
    }
    private class CallReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context,final Intent intent) {
            //跳转到通话页面
            Intent i=new Intent(StartActivity.this, VoiceCallActivity.class);
            Bitmap image=intent.getParcelableExtra("image");
            i.putExtra("username", intent.getStringExtra("from")).putExtra("isComingCall", true).putExtra("image",image);
            startActivity(i);
        }
    }
}
