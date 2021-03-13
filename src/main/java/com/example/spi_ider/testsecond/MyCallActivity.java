package com.example.spi_ider.testsecond;

import android.app.Activity;
import android.app.Fragment;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.content.Context;
import android.media.AudioManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;
import android.os.PersistableBundle;
import android.os.SystemClock;
import android.telecom.Call;
import android.util.Log;
import android.view.View;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.widget.Chronometer;
import android.widget.Toast;

import com.hyphenate.EMCallBack;
import com.hyphenate.EMError;
import com.hyphenate.chat.EMCallManager;
import com.hyphenate.chat.EMCallStateChangeListener;
import com.hyphenate.chat.EMClient;
import com.hyphenate.chat.EMConversation;
import com.hyphenate.chat.EMMessage;
import com.hyphenate.exceptions.EMServiceNotReadyException;
import com.hyphenate.util.EMLog;

import java.security.spec.ECField;

/**
 * Created by Spi-ider on 2017/5/2.
 */

public class MyCallActivity extends Activity implements DecideFragment.onDecideFragmentListener,HangOutFragment.onHangOutFragmentListener {
    public final static String TAG = "陈齐翔,CallActivity";
    protected final int MSG_CALL_MAKE_VOICE = 1;
    protected final int MSG_CALL_ANSWER = 2;
    protected final int MSG_CALL_REJECT = 3;
    protected final int MSG_CALL_END = 4;
    protected final int MSG_CALL_RELEASE_HANDLER = 5;

    private String toChatUsername;
    private String callDruationText ="0";
    private Boolean isComingCall = false;
    private Boolean isAnswered = false;

    protected EMCallStateChangeListener callStateListener;
    protected AudioManager audioManager;
    private EMCallManager.EMCallPushProvider pushProvider;

    private Chronometer chronometer;

    private FragmentManager fm;
    private FragmentTransaction ft;
    private DecideFragment decidecall = null;
    private HangOutFragment hangout = null;//一个是来电时的样子 一个是拨打时的样子

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if(savedInstanceState != null){
            finish();
            return;
        }
        toChatUsername = getIntent().getExtras().getString("username");//先获取对方username
        isComingCall = getIntent().getExtras().getBoolean("isComingCall");
        setContentView(R.layout.activity_voicecall);

        if (!isComingCall) {// 拨打方
            //comingBtnContainer.setVisibility(View.INVISIBLE);
           // hangupBtn.setVisibility(View.VISIBLE);
            //st1 = getResources().getString(R.string.Are_connected_to_each_other);
            //callStateTextView.setText(st1);
            handler.sendEmptyMessage(MSG_CALL_MAKE_VOICE);
//            handler.postDelayed(new Runnable() {
//                public void run() {
//                    streamID = playMakeCallSounds();
//                }
//            }, 300);
        } else { // incoming call
           // voiceContronlLayout.setVisibility(View.INVISIBLE);
           // Uri ringUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE);
            audioManager.setMode(AudioManager.MODE_RINGTONE);
            audioManager.setSpeakerphoneOn(true);

        }

        chronometer = (Chronometer) findViewById(R.id.duration_clock);
        addCallStateListener();

        //handler.sendEmptyMessage(MSG_CALL_MAKE_VOICE);//发送初始消息 (接电话的怎么办?)
        setDefaultFragment();//生成界面
        audioManager = (AudioManager) this.getSystemService(Context.AUDIO_SERVICE);
        pushProvider = new EMCallManager.EMCallPushProvider() {//不知道干嘛用的

            void updateMessageText(final EMMessage oldMsg, final String to) {
                // update local message text
                EMConversation conv = EMClient.getInstance().chatManager().getConversation(oldMsg.getTo());
                conv.removeMessage(oldMsg.getMsgId());
            }

            @Override
            public void onRemoteOffline(final String to) {

                //this function should exposed & move to Demo
                EMLog.d(TAG, "onRemoteOffline, to:" + to);

                final EMMessage message = EMMessage.createTxtSendMessage("You have an incoming call", to);
                // set the user-defined extension field
                message.setAttribute("em_apns_ext", true);

                message.setAttribute("is_voice_call", true);

                message.setMessageStatusCallback(new EMCallBack(){

                    @Override
                    public void onSuccess() {
                        EMLog.d(TAG, "onRemoteOffline success");
                        updateMessageText(message, to);
                    }

                    @Override
                    public void onError(int code, String error) {
                        EMLog.d(TAG, "onRemoteOffline Error");
                        updateMessageText(message, to);
                    }

                    @Override
                    public void onProgress(int progress, String status) {
                    }
                });
                // send messages
                EMClient.getInstance().chatManager().sendMessage(message);
            }
        };
        EMClient.getInstance().callManager().setPushProvider(pushProvider);

    }
    @Override
    protected void onDestroy() {
        audioManager.setMode(AudioManager.MODE_NORMAL);
        audioManager.setMicrophoneMute(false);

        if(callStateListener != null)
            EMClient.getInstance().callManager().removeCallStateChangeListener(callStateListener);

        if (pushProvider != null) {
            EMClient.getInstance().callManager().setPushProvider(null);
            pushProvider = null;
        }
        releaseHandler();
        super.onDestroy();
    }
    private HandlerThread voicecallHandlerThread = new HandlerThread("voicecallHandlerThread");//快速获得looper以创建handler
    {voicecallHandlerThread.start();}
    private Handler handler = new Handler(voicecallHandlerThread.getLooper()){//处理通话页面动作
        @Override
        public void handleMessage(Message msg) {//在无限循环中从消息队列抽取消息并执行
            super.handleMessage(msg);
            switch (msg.what){
                case MSG_CALL_MAKE_VOICE:
                    try {
                        makeVoiceCall();
                    }catch (final EMServiceNotReadyException e){
                        e.printStackTrace();
                        runOnUiThread(new Runnable() {//因为现在这里不是UI线程，是handlerThread内的无限循环体
                            public void run() {
                                String st2 = e.getMessage();
                                if (e.getErrorCode() == EMError.CALL_REMOTE_OFFLINE) {
                                    st2 = "对方不在线，气不气？";
                                } else if (e.getErrorCode() == EMError.USER_NOT_LOGIN) {
                                    st2 = "尚未连接至服务器";
                                } else if (e.getErrorCode() == EMError.INVALID_USER_NAME) {
                                    st2 = "非法的用户名,气不气？";
                                } else if (e.getErrorCode() == EMError.CALL_BUSY) {
                                    st2 = "对方正在通话中，气不气！？";
                                } else if (e.getErrorCode() == EMError.NETWORK_ERROR) {
                                    st2 = "网络错误，无法连接至服务器，气不气？";
                                }
                                Toast.makeText(MyCallActivity.this, st2, Toast.LENGTH_SHORT).show();
                                finish();
                            }
                        });
                    }
                    break;
                case MSG_CALL_ANSWER:
                    EMLog.d(TAG, "MSG_CALL_ANSWER");

                    if (isComingCall) {
                        try {
                            answerCall();
                            isAnswered = true;
//                        }
                        } catch (Exception e) {
                            e.printStackTrace();
                            //saveCallRecord();
                            finish();
                            return;
                        }
                    } else {
                        EMLog.d(TAG, "answer call isInComingCall:false");
                    }
                    break;
                case MSG_CALL_REJECT:
                    try {
                        rejectCall();
                    } catch (Exception e1) {
                        e1.printStackTrace();
                        //saveCallRecord();
                        finish();
                    }
                    break;
                case MSG_CALL_END:
                    try {
                        endCall();
                    } catch (Exception e) {
                        //saveCallRecord();
                        finish();
                    }
                    break;
                case MSG_CALL_RELEASE_HANDLER:
                    try {
                        EMClient.getInstance().callManager().endCall();
                    } catch (Exception e) {
                    }
                    //handler.removeCallbacks(timeoutHangup);  不设置最高通话时间呢？
                    handler.removeMessages(MSG_CALL_MAKE_VOICE);
                    handler.removeMessages(MSG_CALL_ANSWER);
                    handler.removeMessages(MSG_CALL_REJECT);
                    handler.removeMessages(MSG_CALL_END);
//                    if(hangout!=null)
//                        ft.remove(hangout);    //怎么释放这个资源？
//                    if(decidecall!=null)
//                        ft.remove(decidecall);
                    voicecallHandlerThread.quit();
                    break;
                default:
                    break;
            }
            EMLog.d("EMCallManager CallActivity", "handleMessage ---exit block--- msg.what:" + msg.what);
        }
    };
    void releaseHandler() {
        handler.sendEmptyMessage(MSG_CALL_RELEASE_HANDLER);
    }
    private void setDefaultFragment(){
        fm = getFragmentManager();
        ft=fm.beginTransaction();
        if(isComingCall){
            decidecall = new DecideFragment();
            decidecall.setDecideListener(this);
            ft.replace(R.id.shift_content,decidecall);
        }else{
            hangout = new HangOutFragment();
            hangout.setHangoutListener(this);
            ft.replace(R.id.shift_content,hangout);
        }
        ft.commit();
    }

    private void makeVoiceCall () throws EMServiceNotReadyException{
        EMClient.getInstance().callManager().makeVoiceCall(toChatUsername);
    }
    private void answerCall ()throws Exception{//用错了 是先调用answerCall再发消息吧？
        EMClient.getInstance().callManager().answerCall();
        isAnswered = true;
        if(decidecall!=null)
            ft.remove(decidecall);
        hangout = new HangOutFragment();
        ft.replace(R.id.shift_content,hangout).commit();
    }
    private void rejectCall() throws Exception{
        EMClient.getInstance().callManager().rejectCall();
        finish();
    }
    private void endCall() throws Exception{
        EMClient.getInstance().callManager().endCall();
        finish();
    }

    void addCallStateListener() {//在这里监听通话状态
        callStateListener = new EMCallStateChangeListener() {

            @Override
            public void onCallStateChanged(CallState callState, final CallError error) {
                // Message msg = handler.obtainMessage();
                EMLog.d("EMCallManager", "onCallStateChanged:" + callState);
                switch (callState) {
                    case CONNECTING:
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                //callStateTextView.setText(st1);
                                Toast.makeText(MyCallActivity.this,"正在连接",Toast.LENGTH_SHORT).show();
                            }
                        });
                        break;
                    case CONNECTED:
                        runOnUiThread(new Runnable() {

                            @Override
                            public void run() {
                                Toast.makeText(MyCallActivity.this,"已连接",Toast.LENGTH_SHORT).show();
                            }
                        });
                        break;

                    case ACCEPTED:
                        //handler.removeCallbacks(timeoutHangup);
                        runOnUiThread(new Runnable() {

                            @Override
                            public void run() {

//                                if(!isHandsfreeState) 之后再开启免提功能
//                                    closeSpeakerOn();
                               //show relay or direct call, for testing purpose

                                chronometer.setVisibility(View.VISIBLE);
                                chronometer.setBase(SystemClock.elapsedRealtime());
                                // duration start
                                chronometer.start();
                                //String str4 = getResources().getString(R.string.In_the_call);
                                //startMonitor();
                            }
                        });
                        break;
                    case NETWORK_UNSTABLE:
                        runOnUiThread(new Runnable() {
                            public void run() {
//                                //netwrokStatusVeiw.setVisibility(View.VISIBLE);
//                                if(error == CallError.ERROR_NO_DATA){
//                                    netwrokStatusVeiw.setText(R.string.no_call_data);
//                                }else{
//                                    netwrokStatusVeiw.setText(R.string.network_unstable);
//                                }
                                Toast.makeText(MyCallActivity.this,"网络连接不稳定",Toast.LENGTH_SHORT).show();
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
                        //handler.removeCallbacks(timeoutHangup);
                        @SuppressWarnings("UnnecessaryLocalVariable") final CallError fError = error;
                        runOnUiThread(new Runnable() {
                            private void postDelayedCloseMsg() {
                                handler.postDelayed(new Runnable() {

                                    @Override
                                    public void run() {
                                        runOnUiThread(new Runnable() {
                                            @Override
                                            public void run() {
                                                Log.d(TAG, "CALL DISCONNETED");
                                                removeCallStateListener();
                                                //saveCallRecord();
                                                Animation animation = new AlphaAnimation(1.0f, 0.0f);//渐变透明度动画效果
                                                animation.setDuration(800);
                                                findViewById(R.id.root_layout).startAnimation(animation);
                                                finish();
                                            }
                                        });
                                    }
                                }, 200);
                            }

                            @Override
                            public void run() {
                                chronometer.stop();
                                callDruationText = chronometer.getText().toString();
                                Toast.makeText(MyCallActivity.this,"通话时长："+callDruationText,Toast.LENGTH_SHORT).show();
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


    void removeCallStateListener() {
        EMClient.getInstance().callManager().removeCallStateChangeListener(callStateListener);
    }



    @Override
    public void  onClickAnswerBtn(){
        //closeSpeakerOn();
        handler.sendEmptyMessage(MSG_CALL_ANSWER);//切换fragment
    }

    @Override
    public void onClickRejectBtn(){
        handler.sendEmptyMessage(MSG_CALL_REJECT);
    }
    @Override
    public void onClickHangOutBtn(){
        handler.sendEmptyMessage(MSG_CALL_END);
        Toast.makeText(this,"已取消通话",Toast.LENGTH_SHORT).show();
        //先释放资源 再finish
    }
}
