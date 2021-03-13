package com.example.spi_ider.testsecond;

import android.app.Fragment;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

/**
 * Created by Spi-ider on 2017/5/2.
 */

public class DecideFragment extends Fragment {
    private Button answer_btn;
    private Button reject_btn;
    private onDecideFragmentListener clickListener;
    public interface onDecideFragmentListener{
        void onClickAnswerBtn();
        void onClickRejectBtn();
    }
    public void setDecideListener(onDecideFragmentListener d){
        clickListener = d;
    }
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState){
        View view = inflater.inflate(R.layout.fragment_decide, container, false);
        answer_btn = (Button) view.findViewById(R.id.answer_call);
        reject_btn = (Button)view.findViewById(R.id.reject_call);
        answer_btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                clickListener.onClickAnswerBtn();
            }
        });
        reject_btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                clickListener.onClickRejectBtn();
            }
        });
        return view;
    }

}
