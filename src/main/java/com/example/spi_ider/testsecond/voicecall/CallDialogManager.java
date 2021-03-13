package com.example.spi_ider.testsecond.voicecall;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;

import com.example.spi_ider.testsecond.R;


/**
 * Created by Spi-ider on 2017/5/8.
 */

public class CallDialogManager {//松耦合开发初衷  又被我写成紧耦合了..有时间再优化成全局DialogManager吧
    private  static CallDialogManager manager=null;
    private static Context mContext;
    private static Boolean inComing;
    private CallDialogManager(Context context){
        this.mContext=context;

    }
    public static synchronized CallDialogManager getInstance(Context context){
        if(manager==null){
            manager=new CallDialogManager(context);
        }
        return manager;
    }

    public static AlertDialog createVoiceCallDialog(String f,Boolean isComingCall,DialogInterface.OnClickListener l){
        if(l==null)
            l=new DialogInterface.OnClickListener(){//默认的listener
                @Override
                public void onClick(DialogInterface dialog, int which) {}
            };

        AlertDialog.Builder builder=new AlertDialog.Builder(mContext);
        builder.setTitle(R.string.call_title).setMessage("来自 "+f+" 的请求")
                .setNegativeButton("取消",l);

        if(isComingCall){//接电话的话再加个按钮
            builder.setPositiveButton("接受",l);
        }
        AlertDialog dialog=builder.create();
        return dialog;
    }

}
