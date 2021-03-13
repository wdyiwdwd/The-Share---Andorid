package com.example.spi_ider.testsecond;

import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

/**
 * Created by DELL on 2017/3/25.
 */

public class WidthPickerDialog extends Dialog {

    private float width;  //宽度
    private Button addWidth;
    private Button subWidth;
    private TextView theWidth;
    private ImageView showWidth;
    private OnChangeWidthListener wListener;

    //width:初始宽度
    public WidthPickerDialog(Context context, float width , OnChangeWidthListener wListener){
        super(context);
        this.width=width;
        this.wListener=wListener;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.setContentView(R.layout.width_picker);
        this.initAll();
    }

    private void initAll(){
        addWidth=(Button)findViewById(R.id.addWidth);
        subWidth=(Button)findViewById(R.id.subWidth);
        theWidth=(TextView)findViewById(R.id.theWidth);

        addWidth.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(width < 42)
                    width+=2;
                wListener.changeWidth(width);
                updateData();
            }
        });

        subWidth.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(width > 2)
                    width-=2;
                wListener.changeWidth(width);
                updateData();
            }
        });

        theWidth.setText(""+width);
    }

    private void updateData(){
        theWidth.setText(""+width);
    }

    public interface OnChangeWidthListener{
        void changeWidth(float width);
    }
}
