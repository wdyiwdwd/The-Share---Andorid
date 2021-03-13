package com.example.spi_ider.testsecond;

/**
 * Created by Spi-ider on 2017/5/4.
 */

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Configuration;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.AudioManager;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;

import android.provider.MediaStore;
import android.util.Log;

import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.widget.Toast;

import com.getbase.floatingactionbutton.FloatingActionButton;
import com.getbase.floatingactionbutton.FloatingActionsMenu;
import com.hyphenate.EMMessageListener;
import com.hyphenate.chat.EMCallStateChangeListener;
import com.hyphenate.chat.EMClient;

import com.hyphenate.chat.EMConversation;
import com.hyphenate.chat.EMImageMessageBody;
import com.hyphenate.chat.EMMessage;
import com.hyphenate.chat.EMMessageBody;
import com.hyphenate.easeui.controller.EaseUI;
import com.hyphenate.util.EMLog;

import org.json.JSONObject;

import java.io.File;
import java.net.URL;
import java.util.List;
import java.util.UUID;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import com.hyphenate.util.ImageUtils;

import static com.example.spi_ider.testsecond.Constant.isAssist;

/**
 * 语音通话页面
 *
 */
public class VoiceCallActivity extends CallActivity {
    protected static final int REQUEST_CODE_PIC = 1;
    AlertDialog dialog=null;
    private boolean endCallTriggerByMe = false;
    private FloatingActionsMenu menu;
    private FloatingActionButton hangup_btn;
    private FloatingActionButton pen_set_btn;
    //private String imagePath;
    String st1;
    String paintTAG = "For painting";
    String paintString = "paintString";
    String paintImageString="paintImageString";
    private boolean monitor = false;
    PaletteView paletteView=null;
    private EMConversation conversation;
    private PaintInfo paintInfo;
    private int pen_color;
    private GsonBuilder builder;
    private Gson gson;
    private String jsonTest;

    private EMMessageListener msgListener=new EMMessageListener() {
        @Override
        public void onMessageReceived(final List<EMMessage> messages) {
            //收到消息,画到画板上
            String result;
            for(EMMessage message:messages){
                if(message.getBooleanAttribute(isAssist,true)){
                    if(message.getStringAttribute(paintString,null)!=null){
                        result=message.getStringAttribute(paintString,null);
                        paintInfo=gson.fromJson(result, PaintInfo.class);
                        paletteView.draw(paintInfo);
                    }
                    else if(message.getBooleanAttribute(paintImageString,false)){
                        String imageRemoteUrl = ((EMImageMessageBody) message.getBody()).getRemoteUrl();// 获取远程原图片地址
                        try{
                            URL url = new URL(imageRemoteUrl);
                            Bitmap bitmap = BitmapFactory.decodeStream(url.openStream());
                            paletteView.setBgBitmap(bitmap);
                        }catch (Exception e){

                        }
                    }
                }
                conversation.markMessageAsRead(message.getMsgId());
            }
            Log.d("接收消息","onMessageReceived in Voice call");

        }

        @Override
        public void onCmdMessageReceived(List<EMMessage> messages) {
            //收到透传消息
        }

        @Override
        public void onMessageRead(List<EMMessage> messages) {
            //收到已读回执

        }

        @Override
        public void onMessageDelivered(List<EMMessage> message) {
            //收到已送达回执

        }

        @Override
        public void onMessageChanged(EMMessage message, Object change) {
            //消息状态变动

        }
    };
    @Override
    public void onConfigurationChanged(Configuration newConfig){
        super.onConfigurationChanged(newConfig);
    }
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if(savedInstanceState != null){
            finish();
            return;
        }
        //窗口属性
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON | WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD
                | WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED | WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON);
        setContentView(R.layout.activity_thevoicecall);
        //先获得各种属性值
        builder=new GsonBuilder();
        gson=builder.create();
        username = getIntent().getStringExtra("username");
        isInComingCall = getIntent().getBooleanExtra("isComingCall", false);
        //获得屏幕尺寸
        WindowManager wm = this.getWindowManager();
        int width = wm.getDefaultDisplay().getWidth();
        int height = wm.getDefaultDisplay().getHeight();
        paletteView=(PaletteView)findViewById(R.id.palette);
        paletteView.setCanvasSize(width,height);
        paletteView.setSendPointHelper(new PaletteView.sendPointHelper() {
            @Override
            public void sendPoint(PaintInfo p) {
                jsonTest=gson.toJson(p, PaintInfo.class);
                sendCoorMessage(jsonTest);
            }
        });
        msgid = UUID.randomUUID().toString();
        new Runnable(){
            @Override
            public void run() {
                createCallDialog();
                addCallStateListener();//监听通话状态
            }
        }.run();
        if (!isInComingCall) {// outgoing call
            st1 = getResources().getString(R.string.Are_connected_to_each_other);
            handler.sendEmptyMessage(MSG_CALL_MAKE_VOICE);
            handler.postDelayed(new Runnable() {//间隔300ms 发送来电声音
                public void run() {
                    streamID = playMakeCallSounds();//父类方法
                }
            }, 300);
        } else { // incoming call
            Uri ringUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE);
            audioManager.setMode(AudioManager.MODE_RINGTONE);
            audioManager.setSpeakerphoneOn(true);
            ringtone = RingtoneManager.getRingtone(this, ringUri);
            ringtone.play();
        }
        final int MAKE_CALL_TIMEOUT = 50 * 1000;//呼叫时间上限
        handler.removeCallbacks(timeoutHangup);
        handler.postDelayed(timeoutHangup, MAKE_CALL_TIMEOUT);//发送MSG_CALL_END
    }

    private void createCallDialog() {
        //创建来电对话框
        AlertDialog.Builder builder=new AlertDialog.Builder(VoiceCallActivity.this);
        builder.setTitle(R.string.call_title).setMessage("来自 "+username+" 的请求");
        if(isInComingCall) {
            builder.setPositiveButton("接受", new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int which) {
                    openSpeakerOn();
                    EMClient.getInstance().chatManager().addMessageListener(msgListener);
                    handler.sendEmptyMessage(MSG_CALL_ANSWER);
                }
            });
        }
        builder.setNegativeButton("取消",new  DialogInterface.OnClickListener(){
            public void onClick(DialogInterface dialog, int which) {
                isRefused = true;
                if(isInComingCall)
                    handler.sendEmptyMessage(MSG_CALL_REJECT);
                else
                    endCall();//主动发起的话

            }
        });
        dialog=builder.create();
        dialog.show();
    }
    private void initComponent() {
        menu = (FloatingActionsMenu)findViewById(R.id.fab_menu);
        hangup_btn = (FloatingActionButton)findViewById(R.id.fab_hung_up);
        pen_set_btn=(FloatingActionButton)findViewById(R.id.fab_select_image);
        //事件监听
        hangup_btn.setOnClickListener(new View.OnClickListener() {//挂断电话
            @Override
            public void onClick(View v) {
                endCall();
            }
        });
        pen_set_btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                selectPicFromLocal(REQUEST_CODE_PIC);
                changeMenuState();
            }
        });
    }
    protected void selectPicFromLocal(int code) {
        Intent intent;
        if (Build.VERSION.SDK_INT < 19) {
            intent = new Intent(Intent.ACTION_GET_CONTENT);
            intent.setType("image/*");
        } else {
            intent = new Intent(Intent.ACTION_PICK, android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        }
        startActivityForResult(intent, code);
    }
    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == Activity.RESULT_OK &&requestCode==REQUEST_CODE_PIC) {
            Uri selectImg = data.getData();
            Bitmap bitmap = getBitmap(selectImg);
            paletteView.setBgBitmap(bitmap);
            sendImage(selectImg);
        }
    }
    private void sendImage(Uri selectedImage){
        String[] filePathColumn = {MediaStore.Images.Media.DATA};
        Cursor cursor = getApplication().getContentResolver().query(selectedImage, filePathColumn, null, null, null);
        if (cursor != null) {
            cursor.moveToFirst();
            int columnIndex = cursor.getColumnIndex(filePathColumn[0]);
            String picturePath = cursor.getString(columnIndex);
            cursor.close();
            if (picturePath == null || picturePath.equals("null")) {
                return;
            }
            sendImageMessage(picturePath);
        } else {
            File file = new File(selectedImage.getPath());
            if (!file.exists()) {
                return;
            }
            sendImageMessage(file.getAbsolutePath());
        }
    }
    private Bitmap getBitmap(Uri uri){
        try
        {// 读取uri所在的图片
            Bitmap bitmap = MediaStore.Images.Media.getBitmap(this.getContentResolver(), uri);
            return bitmap;
        }
        catch (Exception e)
        {
            Log.e(TAG, "目录为：" + uri);
            e.printStackTrace();
            return null;
        }
    }
    //消息发送(底层支持函数，参数为string类型)
    public void sendCoorMessage(String content){//消息内容  接收者
        EMMessage message = EMMessage.createTxtSendMessage(paintTAG,username);
        message.setAttribute(isAssist,true);
        message.setAttribute(paintString,content);
        Log.v("发送消息",""+message.getBooleanAttribute(isAssist,false));
        //发送消息
        EMClient.getInstance().chatManager().sendMessage(message);
    }
    public void sendImageMessage(String imagePath){
        EMMessage message = EMMessage.createImageSendMessage(imagePath,true,username);
        message.setAttribute(isAssist,true);
        message.setAttribute(paintImageString,true);
        Log.v("发送消息",""+message.getBooleanAttribute(isAssist,false));
        //发送消息
        EMClient.getInstance().chatManager().sendMessage(message);
    }
    public void changeMenuState(){//自动收缩
        if(menu.isExpanded())
            menu.collapse();
    }


    @Override
    public void onResume() {
        super.onResume();
        EaseUI.getInstance().pushActivity(VoiceCallActivity.this);
        //添加消息响应
        EMClient.getInstance().chatManager().addMessageListener(msgListener);
    }
    @Override
    public void onStop() {
        super.onStop();
        // unregister this event listener when this activity enters the
        // background
        EMClient.getInstance().chatManager().removeMessageListener(msgListener);
        // remove activity from foreground activity list
        EaseUI.getInstance().popActivity(VoiceCallActivity.this);
    }
    @Override
    public boolean onTouchEvent(MotionEvent event) {
        return paletteView.onTouchEvent(event);
    }
    /**
     * set call state listener
     */
    void addCallStateListener() {//结尾注册listener
        callStateListener = new EMCallStateChangeListener() {
            @Override
            public void onCallStateChanged(final CallState callState, final CallError error) {
                //Message msg = handler.obtainMessage();
                EMLog.d("EMCallManager", "onCallStateChanged:" + callState);
                switch (callState) {

                    case CONNECTING:
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                               Toast.makeText(VoiceCallActivity.this,R.string.connecting,Toast.LENGTH_SHORT).show();
                            }
                        });
                        break;
                    case CONNECTED:
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {

                            }
                        });
                        break;

                    case ACCEPTED:
                        handler.removeCallbacks(timeoutHangup);
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                try {
                                    if (soundPool != null)
                                        soundPool.stop(streamID);
                                    dialog.dismiss();
                                } catch (Exception e) {
                                }
                                //初始化画板、工具栏
                                initComponent();
                                openSpeakerOn();//永远为免提状态  不解释原因..
                                callingState = CallingState.NORMAL;//这里只是开启计时器 改变textView
                                startMonitor();
                            }
                        });

                        break;
                    case NETWORK_UNSTABLE:
                        runOnUiThread(new Runnable() {
                            public void run() {
                                Toast.makeText(VoiceCallActivity.this,R.string.network_unstable,Toast.LENGTH_SHORT).show();
                            }
                        });
                        break;
                    case NETWORK_NORMAL:
                        runOnUiThread(new Runnable() {
                            public void run() {
                                //netwrokStatusVeiw.setVisibility(View.INVISIBLE);
                            }
                        });
                        break;
                    case VOICE_PAUSE:
                        runOnUiThread(new Runnable() {
                            public void run() {
                                Toast.makeText(getApplicationContext(), "VOICE_PAUSE", Toast.LENGTH_SHORT).show();
                            }
                        });
                        break;
                    case VOICE_RESUME:
                        runOnUiThread(new Runnable() {
                            public void run() {
                                Toast.makeText(getApplicationContext(), "VOICE_RESUME", Toast.LENGTH_SHORT).show();
                            }
                        });
                        break;
                    case DISCONNECTED:
                        handler.removeCallbacks(timeoutHangup);
                        @SuppressWarnings("UnnecessaryLocalVariable") final CallError fError = error;
                        runOnUiThread(new Runnable() {
                            private void postDelayedCloseMsg() {
                                handler.postDelayed(new Runnable() {

                                    @Override
                                    public void run() {
                                        runOnUiThread(new Runnable() {
                                            @Override
                                            public void run() {
                                                Log.d("AAA", "CALL DISCONNETED");
                                                removeCallStateListener();
                                                Animation animation = new AlphaAnimation(1.0f, 0.0f);
                                                animation.setDuration(800);
                                                findViewById(R.id.base_layout).startAnimation(animation);
                                                finish();
                                            }
                                        });
                                    }
                                }, 200);
                            }

                            @Override
                            public void run() {
                                String st1 = getResources().getString(R.string.Refused);
                                String st2 = getResources().getString(R.string.The_other_party_refused_to_accept);
                                String st3 = getResources().getString(R.string.Connection_failure);
                                String st4 = getResources().getString(R.string.The_other_party_is_not_online);
                                String st5 = getResources().getString(R.string.The_other_is_on_the_phone_please);
                                String st6 = getResources().getString(R.string.The_other_party_did_not_answer_new);
                                String st7 = getResources().getString(R.string.hang_up);
                                String st11 = getResources().getString(R.string.The_other_is_hang_up);

                                String st9 = getResources().getString(R.string.did_not_answer);
                                String st10 = getResources().getString(R.string.Has_been_cancelled);
                                String st8 = getResources().getString(R.string.hang_up);

                                if (fError == CallError.REJECTED) {
                                    callingState = CallingState.BEREFUSED;
                                    Toast.makeText(VoiceCallActivity.this, st2, Toast.LENGTH_SHORT).show();
                                    //callStateTextView.setText(st2);
                                } else if (fError == CallError.ERROR_TRANSPORT) {
                                    Toast.makeText(VoiceCallActivity.this, st3, Toast.LENGTH_SHORT).show();
                                   // callStateTextView.setText(st3);
                                } else if (fError == CallError.ERROR_UNAVAILABLE) {
                                    callingState = CallingState.OFFLINE;
                                    Toast.makeText(VoiceCallActivity.this, st4, Toast.LENGTH_SHORT).show();
                                    //callStateTextView.setText(st4);
                                } else if (fError == CallError.ERROR_BUSY) {
                                    Toast.makeText(VoiceCallActivity.this, st5, Toast.LENGTH_SHORT).show();
                                    callingState = CallingState.BUSY;
                                    //callStateTextView.setText(st5);
                                } else if (fError == CallError.ERROR_NORESPONSE) {
                                    Toast.makeText(VoiceCallActivity.this, st6, Toast.LENGTH_SHORT).show();
                                    callingState = CallingState.NO_RESPONSE;
                                   // callStateTextView.setText(st6);
                                } else if (fError == CallError.ERROR_LOCAL_SDK_VERSION_OUTDATED || fError == CallError.ERROR_REMOTE_SDK_VERSION_OUTDATED){
                                    callingState = CallingState.VERSION_NOT_SAME;
                                   // callStateTextView.setText("R.string.call_version_inconsistent");
                                } else {
                                    if (isRefused) {
                                        callingState = CallingState.REFUSED;
                                        //callStateTextView.setText(st1);
                                        Toast.makeText(VoiceCallActivity.this, st1, Toast.LENGTH_SHORT).show();
                                    }
                                    else if (isAnswered) {
                                        callingState = CallingState.NORMAL;
                                        if (endCallTriggerByMe) {
//                                        callStateTextView.setText(st7);
                                            Toast.makeText(VoiceCallActivity.this, st7, Toast.LENGTH_SHORT).show();
                                        } else {
                                            //callStateTextView.setText(st8);
                                            Toast.makeText(VoiceCallActivity.this, st8, Toast.LENGTH_SHORT).show();
                                        }
                                    } else {
                                        if (isInComingCall) {
                                            callingState = CallingState.UNANSWERED;
                                            //callStateTextView.setText(st9);
                                            Toast.makeText(VoiceCallActivity.this, st9, Toast.LENGTH_SHORT).show();
                                        } else {
                                            if (callingState != CallingState.NORMAL) {
                                                callingState = CallingState.CANCELLED;
                                               // callStateTextView.setText(st10);
                                                Toast.makeText(VoiceCallActivity.this, st10, Toast.LENGTH_SHORT).show();
                                            }else {
                                                //callStateTextView.setText(st11);
                                                Toast.makeText(VoiceCallActivity.this, st11, Toast.LENGTH_SHORT).show();
                                            }
                                        }
                                    }
                                }
                                postDelayedCloseMsg();
                            }

                        });

                        break;

                    default:
                        break;
                }

            }
        };
        EMClient.getInstance().callManager().addCallStateChangeListener(callStateListener);
    }

    void endCall(){
        handler.sendEmptyMessage(MSG_CALL_END);
    }
    void removeCallStateListener() {
        EMClient.getInstance().callManager().removeCallStateChangeListener(callStateListener);
    }


    @Override
    protected void onDestroy() {
        //DemoHelper.getInstance().isVoiceCalling = false;
        EMClient.getInstance().chatManager().removeMessageListener(msgListener);
        stopMonitor();
        super.onDestroy();
    }

    @Override
    public void onBackPressed() {
        Confirm();
        super.onBackPressed();
    }

    private void Confirm() {
        AlertDialog.Builder builder=new AlertDialog.Builder(VoiceCallActivity.this);
        builder.setTitle(R.string.call_title).setMessage("确定结束通话吗?");

        builder.setPositiveButton("确定", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                endCall();
            }
        });
        builder.setNegativeButton("取消",null);
        dialog=builder.create();
        dialog.show();
    }

    /**
     * for debug & testing, you can remove this when release
     */
    void startMonitor(){
        monitor = true;
        new Thread(new Runnable() {
            public void run() {

                while(monitor){
                    try {
                        Thread.sleep(1500);
                    } catch (InterruptedException e) {
                    }
                }
            }
        }, "CallMonitor").start();
    }

    void stopMonitor() {

    }


}
