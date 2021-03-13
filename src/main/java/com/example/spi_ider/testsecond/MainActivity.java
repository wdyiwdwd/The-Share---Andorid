package com.example.spi_ider.testsecond;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.hyphenate.EMCallBack;
import com.hyphenate.EMError;
import com.hyphenate.chat.EMClient;
import com.hyphenate.exceptions.HyphenateException;

public class MainActivity extends AppCompatActivity {
    private Button signup_btn;
    private Button login_btn;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        signup_btn = (Button)findViewById(R.id.SignUpBtn);
        login_btn = (Button)findViewById(R.id.LoginBtn);
        signup_btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
              signup();
            }
        });
        login_btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                login();
            }
        });
    }
    private void login(){
        String username = ((EditText)findViewById(R.id.username)).getText().toString().trim();
        String password = ((EditText)findViewById(R.id.password)).getText().toString().trim();
        EMClient.getInstance().login(username, password, new EMCallBack() {//回调
            @Override
            public void onSuccess() {
                runOnUiThread(new Runnable() {
                    public void run() {
                        EMClient.getInstance().groupManager().loadAllGroups();
                        EMClient.getInstance().chatManager().loadAllConversations();
                        startActivity(new Intent(MainActivity.this, StartActivity.class));
                        Log.d("main", "登录聊天服务器成功！");
                    }
                });
            }

            @Override
            public void onProgress(int progress, String status) {

            }

            @Override
            public void onError(final int code,final String message) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        //mDialog.dismiss();
                        Log.d("lzan13", "登录失败 Error code:" + code + ", message:" + message);
                        /**
                         * 关于错误码可以参考官方api详细说明
                         * http://www.easemob.com/apidoc/android/chat3.0/classcom_1_1hyphenate_1_1_e_m_error.html
                         */
                        switch (code) {
                            // 网络异常 2
                            case EMError.NETWORK_ERROR:
                                Toast.makeText(MainActivity.this, "网络错误 code: " + code + ", message:" + message, Toast.LENGTH_SHORT).show();
                                break;
                            // 无效的用户名 101
                            case EMError.INVALID_USER_NAME:
                                Toast.makeText(MainActivity.this, "无效的用户名 code: " + code + ", message:" + message, Toast.LENGTH_SHORT).show();
                                break;
                            // 无效的密码 102
                            case EMError.INVALID_PASSWORD:
                                Toast.makeText(MainActivity.this, "无效的密码 code: " + code + ", message:" + message, Toast.LENGTH_SHORT).show();
                                break;
                            // 用户认证失败，用户名或密码错误 202
                            case EMError.USER_AUTHENTICATION_FAILED:
                                Toast.makeText(MainActivity.this, "用户认证失败，用户名或密码错误 code: " + code + ", message:" + message, Toast.LENGTH_SHORT).show();
                                break;
                            // 用户不存在 204
                            case EMError.USER_NOT_FOUND:
                                Toast.makeText(MainActivity.this, "用户不存在 code: " + code + ", message:" + message, Toast.LENGTH_SHORT).show();
                                break;
                            // 无法访问到服务器 300
                            case EMError.SERVER_NOT_REACHABLE:
                                Toast.makeText(MainActivity.this, "无法访问到服务器 code: " + code + ", message:" + message, Toast.LENGTH_SHORT).show();
                                break;
                            // 等待服务器响应超时 301
                            case EMError.SERVER_TIMEOUT:
                                Toast.makeText(MainActivity.this, "等待服务器响应超时 code: " + code + ", message:" + message, Toast.LENGTH_SHORT).show();
                                break;
                            // 服务器繁忙 302
                            case EMError.SERVER_BUSY:
                                Toast.makeText(MainActivity.this, "服务器繁忙 code: " + code + ", message:" + message, Toast.LENGTH_SHORT).show();
                                break;
                            // 未知 Server 异常 303 一般断网会出现这个错误
                            case EMError.SERVER_UNKNOWN_ERROR:
                                Toast.makeText(MainActivity.this, "未知的服务器异常 code: " + code + ", message:" + message, Toast.LENGTH_SHORT).show();
                                break;
                            default:
                                Toast.makeText(MainActivity.this, "ml_sign_in_failed code: " + code + ", message:" + message, Toast.LENGTH_SHORT).show();
                                break;
                        }
                    }
                });
            }
        });
    }
    private void signup(){
        new Thread(new Runnable() {
            public void run() {
                try {
                    // call method in SDK
                    String username = ((EditText)findViewById(R.id.username)).getText().toString().trim();
                    String password = ((EditText)findViewById(R.id.password)).getText().toString().trim();
                    EMClient.getInstance().createAccount(username, password);
                    runOnUiThread(new Runnable() {
                        public void run() {
                            //DemoHelper.getInstance().setCurrentUserName(username);
                            Toast.makeText(getApplicationContext(), getResources().getString(R.string.Registered_successfully), Toast.LENGTH_SHORT).show();

                            //finish();
                        }
                    });
                } catch (final HyphenateException e) {
                    runOnUiThread(new Runnable() {
                        public void run() {
                            int errorCode=e.getErrorCode();
                            if(errorCode==EMError.NETWORK_ERROR){
                                Toast.makeText(getApplicationContext(), getResources().getString(R.string.network_anomalies), Toast.LENGTH_SHORT).show();
                            }else if(errorCode == EMError.USER_ALREADY_EXIST){
                                Toast.makeText(getApplicationContext(), getResources().getString(R.string.User_already_exists), Toast.LENGTH_SHORT).show();
                            }else if(errorCode == EMError.USER_AUTHENTICATION_FAILED){
                                Toast.makeText(getApplicationContext(), "授权失败", Toast.LENGTH_SHORT).show();
                            }else if(errorCode == EMError.USER_ILLEGAL_ARGUMENT){
                                Toast.makeText(getApplicationContext(), getResources().getString(R.string.illegal_user_name),Toast.LENGTH_SHORT).show();
                            }else{
                                Toast.makeText(getApplicationContext(),"注册失败", Toast.LENGTH_SHORT).show();
                            }
                        }
                    });
                }
            }
        }).start();
    }

}
