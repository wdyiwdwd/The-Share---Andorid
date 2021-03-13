package com.example.spi_ider.testsecond;

import android.app.Fragment;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

/**
 * Created by Spi-ider on 2017/5/3.
 */

public class HangOutFragment extends Fragment {
    private Button hangout_btn;
    private onHangOutFragmentListener hangoutListener;
    public interface onHangOutFragmentListener{
        void onClickHangOutBtn();
    }
    public void setHangoutListener(onHangOutFragmentListener l){
        hangoutListener = l;
    }
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState){
        View view = inflater.inflate(R.layout.fragment_hangout,container,false);
        hangout_btn = (Button) view.findViewById(R.id.hangout);
        hangout_btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                hangoutListener.onClickHangOutBtn();
            }
        });
        return view;
    }
}
