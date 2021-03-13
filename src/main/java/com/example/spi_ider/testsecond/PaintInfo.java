package com.example.spi_ider.testsecond;

import java.io.Serializable;

/**
 * Created by DELL on 2017/4/5.
 */

public class PaintInfo implements Serializable{
    public int style;
    public int color;
    public int width;
    public float x;
    public float y;
    public int alpha;
    public int type = 0;

    public PaintInfo(int style,int alpha, int color, int width, float x, float y,int type) {
        this.style = style;
        this.alpha = alpha;
        this.color = color;
        this.width = width;
        this.x = x;
        this.y = y;
        this.type = type;
    }

    @Override
    public String toString() {
        String str = "";
        str=str+alpha+" "+color+" "+width;
        return str;
    }
}
