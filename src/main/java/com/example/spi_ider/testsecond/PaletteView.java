package com.example.spi_ider.testsecond;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.drawable.BitmapDrawable;
import android.util.AttributeSet;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;


import java.util.ArrayList;

public class PaletteView extends SurfaceView implements Runnable,
        SurfaceHolder.Callback {
    private Context context;

    private boolean widthSwitch = true; //控制两个对话框每次只出现一次的开关
    private boolean colorSwitch = true;
    private boolean alphaSwitch = true; // 控制透明度的开关
    private boolean emptySwitch = true;
    private ColorPickerDialog colorDialog;
    private WidthPickerDialog widthDialog;

    private Paint mPaint = null;
    // 画板的坐标以及宽高
    private int bgBitmapX = 0;
    private int bgBitmapY = 0;
    private int bgBitmapHeight = 700;
    private int bgBitmapWidth = 1800;
    // 画笔选项工具栏的坐标,宽高,每行和每列小框个数
    private int toolsX = 30;
    private int toolsSide = 80;
    private int toolsY = bgBitmapHeight - toolsSide*8/5;
    private int toolsHeightNum = 1;
    private int toolsWidthNum = 11;
   /* // 后退工具框坐标,大小
    private int backX = toolsX + toolsWidthNum*toolsSide;
    private int backY = toolsY;
    private int backWidth = toolsSide;
    private int backHeight = toolsSide;
    // 前进工具框坐标,大小
    private int forwardX = backX+ backWidth;
    private int forwardY = backY ;
    private int forwardWidth = backWidth;
    private int forwardHeight = backHeight;*/
    // 当前的已经选择的画笔参数
    private int currentPaintTool = 0; // 0~6
    private int currentColor = Color.BLACK;
    private int currentSize = 5; // 1,3,5
    private int currentAlpha = 255; //透明度
    //设置远方传来的画笔参数
    private int farPaintTool = 0; // 0~6
    private int farColor = Color.BLACK;
    private int farSize = 5; // 1,3,5
    private int farAlpha = 255; //透明度

    private int currentPaintIndex = -1;
    private boolean isBackPressed = false;
    private boolean isForwardPressed = false;
    private int statement = 0;
    // 已选选择单元背景色
    private int selectedCellColor = Color.rgb(197,229,229);
    // 存储所有的动作
    private ArrayList<Action> actionList = null;
    // 当前的画笔实例
    private Action curAction = null;
    private Action farAction = null;
    // 线程结束标志位
    boolean mLoop = true;
    SurfaceHolder mSurfaceHolder = null;
    // 绘图区背景图片
    Bitmap bgBitmap = null;
    // 临时画板用来显示之前已经绘制过的图像
    Bitmap newbit=null;
    sendPointHelper sendhelper;

    private long moveSent = 0;
    private int sentNum = 4;

    public void setCanvasSize(int width,int height){
        this.bgBitmapHeight = height - toolsSide*8/5 ;
        this.bgBitmapWidth = width;
        toolsY = bgBitmapHeight;
        // 后退工具框坐标,大小
        //backY = toolsY;
        //forwardY = backY;
    }

    public PaletteView(Context context, AttributeSet arr) {
        super(context, arr);
        this.context = context;
        mPaint = new Paint();
        actionList = new ArrayList<Action>();
        mSurfaceHolder = this.getHolder();
        mSurfaceHolder.addCallback(this);
        this.setFocusable(true);
        mLoop = true;

       /* bgBitmap = ((BitmapDrawable) (getResources()
                .getDrawable(R.drawable.pic1))).getBitmap();*/
        bgBitmap = Bitmap.createBitmap(bgBitmapWidth, bgBitmapHeight,
                Config.ARGB_4444);
        newbit = Bitmap.createBitmap(bgBitmapWidth, bgBitmapHeight,
                Config.ARGB_4444);
        new Thread(this).start();
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {

        return super.onKeyDown(keyCode, event);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        int action = event.getAction();
        if (action == MotionEvent.ACTION_CANCEL) {
            return false;
        }

        float touchX = event.getX();
        float touchY = event.getY();

        // 点击时
        if (action == MotionEvent.ACTION_DOWN) {
            // 检测点击点是否在主绘图区
            if (testTouchMainPallent(touchX, touchY)) {
                setCurAction(getRealX(touchX), getRealY(touchY));
                clearSpareAction();

                //发送的地方
                PaintInfo info  = this.createPaintInfo(touchX/bgBitmapWidth,touchY/bgBitmapHeight,0);
                sendhelper.sendPoint(info);
            }
            // 检测点击点是否在画笔选择区
            testTouchToolsPanel(touchX, touchY);
            // 检测点击点是否在按钮上
            //testTouchButton(touchX, touchY);
        }
        // 拖动时
        if (action == MotionEvent.ACTION_MOVE) {
            if (curAction != null) {
                curAction.move(getRealX(touchX), getRealY(touchY));

                //发送的地方
                moveSent ++ ;
                if(moveSent%sentNum==0) {
                    PaintInfo info = this.createPaintInfo(touchX / bgBitmapWidth, touchY / bgBitmapHeight, 1);
                    sendhelper.sendPoint(info);
                }
            }
        }
        // 抬起时
        if (action == MotionEvent.ACTION_UP) {
            if (curAction != null) {
                curAction.move(getRealX(touchX), getRealY(touchY));
                actionList.add(curAction);
                currentPaintIndex++;
                curAction = null;

                //发送的地方
                PaintInfo info  = this.createPaintInfo(touchX/bgBitmapWidth,touchY/bgBitmapHeight,2);
                sendhelper.sendPoint(info);

            }
            isBackPressed = false;
            isForwardPressed = false;
        }
        return super.onTouchEvent(event);
    }
    public interface sendPointHelper{
        void sendPoint(PaintInfo p);
    }
    public void setSendPointHelper(sendPointHelper helper){
        sendhelper = helper;
    }
    // 绘图
    protected void Draw() {
        Canvas canvas = mSurfaceHolder.lockCanvas();
        if (mSurfaceHolder == null || canvas == null) {
            return;
        }

        // 填充背景
        canvas.drawColor(Color.WHITE);
        // 画主画板
        drawMainPallent(canvas);
        // 画工具栏
        drawToolsPanel(canvas);

        mSurfaceHolder.unlockCanvasAndPost(canvas);
    }

    @Override
    public void run() {
        while (mLoop) {
            try {
                Thread.sleep(50);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            synchronized (mSurfaceHolder) {
                Draw();
            }
        }
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        new Thread(this).start();
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width,
                               int height) {

    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        mLoop = false;
    }

    // 检测点击事件，是否在按钮上
   /* public boolean testTouchButton(float x, float y) {
        if (x > backX + 2 && y > backY + 2 && x < backX + backWidth - 2
                && y < backY + backHeight - 2) {
            //发送的地方
            PaintInfo info  = this.createPaintInfo(0,0,3);
            sendhelper.sendPoint(info);


            if (isBackPressed) {
                return false;
            }
            if (currentPaintIndex >= 0) {
                currentPaintIndex--;
            }
            isBackPressed = true;
            return true;
        }
        if (x > forwardX + 2 && y > forwardY + 2
                && x < forwardX + forwardWidth - 2
                && y < forwardY + forwardHeight - 2) {
            //发送的地方
            PaintInfo info  = this.createPaintInfo(0,0,4);
            sendhelper.sendPoint(info);

            if (isForwardPressed) {
                return false;
            }
            if ((currentPaintIndex + 1) < actionList.size()) {
                currentPaintIndex++;
            }
            isForwardPressed = true;
            return true;
        }

        return false;
    }*/

    // 检测点击事件，是否在主绘图区
    public boolean testTouchMainPallent(float x, float y) {
        if (x > bgBitmapX + 2 && y > bgBitmapY + 2
                && x < bgBitmapX + bgBitmapWidth - 2
                && y < bgBitmapY + bgBitmapHeight - 2) {
            return true;
        }

        return false;
    }

    // 检测点击事件，是否在画笔类型选择区域
    public boolean testTouchToolsPanel(float x, float y) {
        if (x > toolsX && y > toolsY && x < toolsX + toolsSide * toolsWidthNum
                && y < toolsY + toolsSide * toolsHeightNum) {
            if(x > toolsX + toolsSide * 7  && y > toolsY&& x < toolsX + toolsSide * toolsWidthNum
                    && y < toolsY + toolsSide * toolsHeightNum){
                // 得到选择的画笔坐标
                int tx = (int) ((x - toolsX) / toolsSide);
                int ty = (int) ((y - toolsY) / toolsSide);
                // 设置当前画笔
                currentPaintTool = tx + ty * toolsWidthNum;
                this.setCurAction(this.getRealX(x),this.getRealY(y));
                return true;
            }
            // 得到选择的画笔坐标
            int tx = (int) ((x - toolsX) / toolsSide);
            int ty = (int) ((y - toolsY) / toolsSide);
            // 设置当前画笔
            currentPaintTool = tx + ty * toolsWidthNum;
            return true;
        }

        return false;
    }

    // 得到当前画笔的类型，并进行实例
    public void setFarAction(float x, float y) {
        switch (farPaintTool) {
            case 0:
                farAction = new MyPath(x, y, farSize, farColor, farAlpha);
                break;
            case 1:
                farAction = new MyLine(x, y, farSize, farColor, farAlpha);
                break;
            case 2:
                farAction = new MyRect(x, y, farSize, farColor, farAlpha);
                break;
            case 3:
                farAction = new MyCircle(x, y, farSize, farColor, farAlpha);
                break;
            case 4:
                farAction = new MyFillRect(x, y, farSize, farColor, farAlpha);
                break;
            case 5:
                farAction = new MyFillCircle(x, y, farSize, farColor, farAlpha);
                break;
            case 6:
                farAction = new MyEraser(x, y, farSize, farColor, farAlpha);
                break;
            case 10:
                currentPaintIndex = -1;
                break;
        }
    }

    // 得到当前画笔的类型，并进行实例
    public void setCurAction(float x, float y) {
        switch (currentPaintTool) {
            case 0:
                curAction = new MyPath(x, y, currentSize, currentColor,currentAlpha);
                statement = 0;
                break;
            case 1:
                curAction = new MyLine(x, y, currentSize, currentColor,currentAlpha);
                statement = 1;
                break;
            case 2:
                curAction = new MyRect(x, y, currentSize, currentColor,currentAlpha);
                statement = 2;
                break;
            case 3:
                curAction = new MyCircle(x, y, currentSize, currentColor,currentAlpha);
                statement = 3;
                break;
            case 4:
                curAction = new MyFillRect(x, y, currentSize, currentColor,currentAlpha);
                statement = 4;
                break;
            case 5:
                curAction = new MyFillCircle(x, y, currentSize, currentColor,currentAlpha);
                statement = 5;
                break;
            case 6:
                curAction = new MyEraser(x, y, currentSize, currentColor,currentAlpha);
                statement = 6;
                break;
            case 7:
                colorDialog=new ColorPickerDialog(context, currentColor, "ColorPicker", new ColorPickerDialog.OnColorChangedListener() {
                    @Override
                    public void colorChanged(int color) {
                        currentColor = color;
                    }
                });
                if(colorSwitch == true) {
                    colorDialog.show();
                    colorSwitch = false;
                }
                else{
                    colorSwitch = true;
                }
                currentPaintTool = statement;

                break;
            case 8:
                widthDialog=new WidthPickerDialog(context, currentSize, new WidthPickerDialog.OnChangeWidthListener() {
                    @Override
                    public void changeWidth(float width) {
                        currentSize = (int)width;
                    }
                });
                if(widthSwitch == true) {
                    widthDialog.show();
                    widthSwitch = false;
                }
                else{
                    widthSwitch = true;
                }
                currentPaintTool = statement;

                break;
            case 9:
                if(alphaSwitch == true){
                    if(currentAlpha == 255) {
                        this.currentAlpha = 100;
                    }
                    else{
                        this.currentAlpha = 255;
                    }
                    alphaSwitch = false;
                }
                else{
                    alphaSwitch = true;
                }
                currentPaintTool = statement;
                break;
            case 10:
                if(emptySwitch == true){
                    emptySwitch = false;
                    new AlertDialog.Builder(context).setTitle("系统提示")//设置对话框标题
                            .setMessage("画板清空后无法撤销,是否继续！")//设置显示的内容
                            .setPositiveButton("确定",new DialogInterface.OnClickListener() {//添加确定按钮
                                @Override
                                public void onClick(DialogInterface dialog, int which) {//确定按钮的响应事件
                                    // TODO Auto-generated method stub

                                    //发送的地方


                                    PaintInfo info = createPaintInfo(10,0,0,0);
                                    sendhelper.sendPoint(info);


                                    currentPaintIndex = -1;
                                }
                            }).setNegativeButton("返回",new DialogInterface.OnClickListener() {//添加返回按钮
                        @Override
                        public void onClick(DialogInterface dialog, int which) {//响应事件
                            // TODO Auto-generated method stub
                        }
                    }).show();//在按键响应事件中显示此对话框
                }
                else{
                    emptySwitch = true;
                }
                currentPaintTool = statement;
                break;
        }

    }




    // 画工具栏
    private void drawToolsPanel(Canvas canvas) {

        // 绘制画笔选项的工具栏边框
        drawPaintToolsPanel(canvas);

        // 绘制前进后退按钮
        //drawBackForwardPanel(canvas);
    }

    /*// 绘制前进后退按钮
    private void drawBackForwardPanel(Canvas canvas) {
        Paint paint = new Paint();

        paint.setAntiAlias(true);
        paint.setColor(Color.BLACK);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(2);

        int cellX = backX;
        int cellY = backY;
        int cellBX = backX + backWidth;
        int cellBY = backY + backHeight;
        // 绘制边框
        //canvas.drawRect(cellX, cellY, cellBX, cellBY, paint);
        // 绘制按钮被点击时背景
        if (isBackPressed) {
            paint.setColor(selectedCellColor);
            paint.setStyle(Paint.Style.FILL);
            canvas.drawRect(cellX + 2, cellY + 2, cellBX - 2, cellBY - 2, paint);
            paint.setColor(Color.BLACK);
            paint.setStyle(Paint.Style.STROKE);
        }
        // 绘制边框中内容
        drawCellText(canvas, cellX, cellY, cellBX, cellBY, "<-");

        cellX = forwardX;
        cellY = forwardY;
        cellBX = forwardX + forwardWidth;
        cellBY = forwardY + forwardHeight;

        paint.setAntiAlias(true);
        paint.setColor(Color.BLACK);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(2);
        // 绘制边框
        //canvas.drawRect(cellX, cellY, cellBX, cellBY, paint);
        // 绘制按钮被点击时背景
        if (isForwardPressed) {
            paint.setColor(selectedCellColor);
            paint.setStyle(Paint.Style.FILL);
            canvas.drawRect(cellX + 2, cellY + 2, cellBX - 2, cellBY - 2, paint);
            paint.setColor(Color.BLACK);
            paint.setStyle(Paint.Style.STROKE);
        }
        // 绘制边框中内容
        drawCellText(canvas, cellX, cellY, cellBX, cellBY, "->");

    }*/

    // 绘制画笔选项的工具栏
    private void drawPaintToolsPanel(Canvas canvas) {
        Paint paint = new Paint();

        paint.setAntiAlias(true);
        paint.setColor(Color.BLUE);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(2);

        // 绘制画笔选项的工具栏边框
        for (int i = 0; i < toolsWidthNum; i++)
            for (int j = 0; j < toolsHeightNum; j++) {
                int cellX = toolsX + i * toolsSide;
                int cellY = toolsY + j * toolsSide;
                int cellBX = toolsX + (i + 1) * toolsSide;
                int cellBY = toolsY + (j + 1) * toolsSide;
                int paintTool = j * toolsWidthNum + i;
                // 绘制边框
                //canvas.drawRect(cellX, cellY, cellBX, cellBY, paint);
                // 绘制已选工具背景
                if (paintTool == currentPaintTool) {
                    paint.setColor(selectedCellColor);
                    paint.setStyle(Paint.Style.FILL);
                    canvas.drawRect(cellX + 2, cellY + 2, cellBX - 2,
                            cellBY - 2, paint);
                    paint.setColor(Color.BLACK);
                    paint.setStyle(Paint.Style.STROKE);
                }
                if(i == 9 && this.currentAlpha==100){
                    paint.setColor(selectedCellColor);
                    paint.setStyle(Paint.Style.FILL);
                    canvas.drawRect(cellX + 2, cellY + 2, cellBX - 2,
                            cellBY - 2, paint);
                    paint.setColor(Color.BLACK);
                    paint.setStyle(Paint.Style.STROKE);
                }
                // 绘制边框中内容
                drawToolsText(canvas, cellX, cellY, cellBX, cellBY, paintTool);
            }
    }

    // 绘制画笔选项的工具栏边框中内容
    private void drawToolsText(Canvas canvas, int cellX, int cellY, int cellBX,
                               int cellBY, int paintTool) {
        Bitmap mBitmap = null;
        switch (paintTool) {
            case 0:
                mBitmap = ((BitmapDrawable) getResources().getDrawable(R.drawable.p0)).getBitmap();
                this.drawCellBitmap(canvas, cellX, cellY, cellBX, cellBY, mBitmap);
                break;
            case 1:
                mBitmap = ((BitmapDrawable) getResources().getDrawable(R.drawable.p1)).getBitmap();
                this.drawCellBitmap(canvas, cellX, cellY, cellBX, cellBY, mBitmap);
                break;
            case 2:
                mBitmap = ((BitmapDrawable) getResources().getDrawable(R.drawable.p2)).getBitmap();
                this.drawCellBitmap(canvas, cellX, cellY, cellBX, cellBY, mBitmap);
                break;
            case 3:
                mBitmap = ((BitmapDrawable) getResources().getDrawable(R.drawable.p3)).getBitmap();
                this.drawCellBitmap(canvas, cellX, cellY, cellBX, cellBY, mBitmap);
                break;
            case 4:
                mBitmap = ((BitmapDrawable) getResources().getDrawable(R.drawable.p4)).getBitmap();
                this.drawCellBitmap(canvas, cellX, cellY, cellBX, cellBY, mBitmap);
                break;
            case 5:
                mBitmap = ((BitmapDrawable) getResources().getDrawable(R.drawable.p5)).getBitmap();
                this.drawCellBitmap(canvas, cellX, cellY, cellBX, cellBY, mBitmap);
                break;
            case 6:
                mBitmap = ((BitmapDrawable) getResources().getDrawable(R.drawable.p6)).getBitmap();
                this.drawCellBitmap(canvas, cellX, cellY, cellBX, cellBY, mBitmap);
                break;
            case 7:
                mBitmap = ((BitmapDrawable) getResources().getDrawable(R.drawable.p7)).getBitmap();
                this.drawCellBitmap(canvas, cellX, cellY, cellBX, cellBY, mBitmap);
                break;
            case 8:
                mBitmap = ((BitmapDrawable) getResources().getDrawable(R.drawable.p8)).getBitmap();
                this.drawCellBitmap(canvas, cellX, cellY, cellBX, cellBY, mBitmap);
                break;
            case 9:
                mBitmap = ((BitmapDrawable) getResources().getDrawable(R.drawable.p9)).getBitmap();
                this.drawCellBitmap(canvas, cellX, cellY, cellBX, cellBY, mBitmap);
                break;
            case 10:
                mBitmap = ((BitmapDrawable) getResources().getDrawable(R.drawable.p10)).getBitmap();
                this.drawCellBitmap(canvas, cellX, cellY, cellBX, cellBY, mBitmap);
                break;
        }
    }

    // 绘制单元格中的文字
    private void drawCellText(Canvas canvas, int cellX, int cellY, int cellBX,
                              int cellBY, String text) {
        Paint paint = new Paint();
        paint.setFlags(Paint.ANTI_ALIAS_FLAG);
        paint.setColor(Color.BLUE);
        paint.setTextSize((cellBY - cellY) / 4 * 3);
        int textX = cellX + (cellBX - cellX) / 5;
        int textY = cellBY - (cellBY - cellY) / 5;
        canvas.drawText(text, textX, textY, paint);

    }

    private void drawCellBitmap(Canvas canvas, int cellX, int cellY, int cellBX,
                                int cellBY,Bitmap bitmap) {
        Paint paint = new Paint();
        paint.setFlags(Paint.ANTI_ALIAS_FLAG);
        //paint.setColor(Color.BLUE);
        int edge = (cellBY - cellY) / 4 * 3;
        int textX = cellX + (cellBX - cellX) / 5;
        int textY = cellBY - (cellBY - cellY) / 5;
        //canvas.drawText(text, textX, textY, paint);
        Rect rect1 = new Rect(0,0,bitmap.getWidth(),bitmap.getHeight());
        //Rect rect2 = new Rect(textX,textY,edge,edge);
        Rect rect2 = new Rect(cellX,cellY,cellBX,cellBY);
        canvas.drawBitmap(bitmap,rect1,rect2,paint);

    }

    // 绘制单元格中的颜色
    private void drawCellColor(Canvas canvas, int cellX, int cellY, int cellBX,
                               int cellBY, int color) {
        Paint paint = new Paint();
        // 绘制备选颜色边框以及其中颜色
        paint.setColor(color);
        paint.setStyle(Paint.Style.FILL);
        canvas.drawRect(cellX + 4, cellY + 4, cellBX - 4, cellBY - 4, paint);
    }

    // 画主画板
    private void drawMainPallent(Canvas canvas) {
        // 设置画笔没有锯齿，空心
        mPaint.setAntiAlias(true);
        mPaint.setStyle(Paint.Style.STROKE);

        // 画板绘图区背景图片
        canvas.drawBitmap(bgBitmap, bgBitmapX, bgBitmapY, null);

        newbit=Bitmap.createBitmap(bgBitmapWidth, bgBitmapHeight,
                Config.ARGB_4444);
        Canvas canvasTemp = new Canvas(newbit);
        canvasTemp.drawColor(Color.TRANSPARENT);

        for(int i=0;i<=currentPaintIndex;i++){
            actionList.get(i).draw(canvasTemp);
        }
        // 画当前画笔痕迹
        if (curAction != null) {
            curAction.draw(canvasTemp);
        }
        if(farAction!=null){
            farAction.draw(canvasTemp);
        }

        // 在主画板上绘制临时画布上的图像
        canvas.drawBitmap(newbit, bgBitmapX, bgBitmapY, null);
    }

    // 根据接触点x坐标得到画板上对应x坐标
    public float getRealX(float x) {

        return x-bgBitmapX;
    }

    // 根据接触点y坐标得到画板上对应y坐标
    public float getRealY(float y) {

        return y-bgBitmapY;
    }

    //旋转图片
    /**
     * 选择变换
     *
     * @param origin 原图
     * @param alpha  旋转角度，可正可负
     * @return 旋转后的图片
     */
    private Bitmap rotateBitmap(Bitmap origin, float alpha) {
        if (origin == null) {
            return null;
        }
        int width = origin.getWidth();
        int height = origin.getHeight();
        Matrix matrix = new Matrix();
        matrix.setRotate(alpha);
        // 围绕原地进行旋转
        Bitmap newBM = Bitmap.createBitmap(origin, 0, 0, width, height, matrix, false);
        if (newBM.equals(origin)) {
            return newBM;
        }
        origin.recycle();
        return newBM;
    }
    //缩放图片
    public Bitmap zoomImg(Bitmap bm, int newWidth ,int newHeight){
        // 获得图片的宽高
        int width = bm.getWidth();
        int height = bm.getHeight();
        if(width < height){
            bm = rotateBitmap(bm,-90);
            width = bm.getWidth();
            height = bm.getHeight();
        }
        // 计算缩放比例
        float scaleWidth = ((float) newWidth) / width;
        float scaleHeight = ((float) newHeight) / height;
        // 取得想要缩放的matrix参数
        Matrix matrix = new Matrix();
        matrix.postScale(scaleWidth, scaleHeight);
        // 得到新的图片
        Bitmap newbm = Bitmap.createBitmap(bm, 0, 0, width, height, matrix, true);
        return newbm;
    }

    public void setBgBitmap(Bitmap bgBitmap) {
        int btHeight = bgBitmap.getHeight();
        int btWidth = bgBitmap.getWidth();
        //float width = (btWidth<bgBitmapWidth)?btWidth:bgBitmapWidth;
        //float height = (btHeight<bgBitmapHeight)?btWidth:bgBitmapHeight;
        this.bgBitmap = zoomImg(bgBitmap,bgBitmapWidth,bgBitmapHeight);
    }

    // 后退前进完成后，缓存的动作
    private void clearSpareAction() {
        for (int i = actionList.size() - 1; i > currentPaintIndex; i--) {
            actionList.remove(i);
        }
    }

    //根据服务器发来的消息画画
    public void draw(PaintInfo info){
        this.farAlpha = info.alpha;
        this.farSize = info.width;
        this.farPaintTool = info.style;
        this.farColor = info.color;
        if(info.type == 0) {
            setFarAction(getRealX(info.x*bgBitmapWidth), getRealY(info.y*bgBitmapHeight));
            clearSpareAction();
        }
        else if(info.type == 1) {
            farAction.move(getRealX(info.x*bgBitmapWidth), getRealY(info.y*bgBitmapHeight));
        }
        else if(info.type == 2) {
            farAction.move(getRealX(info.x*bgBitmapWidth), getRealY(info.y*bgBitmapHeight));
            actionList.add(farAction);
            currentPaintIndex++;
            farAction = null;
        }
        /*else if(info.type == 3){
            currentPaintIndex--;
        }
        else if(info.type == 4 ){
            currentPaintIndex++;
        }*/
    }
    //根据当前信息生成一个Paintinfo对象
    PaintInfo createPaintInfo(float x,float y,int type){
        return new PaintInfo(currentPaintTool,currentAlpha,currentColor,currentSize,x,y,type);
    }
    PaintInfo createPaintInfo(int tool,float x,float y,int type){
        return new PaintInfo(tool,currentAlpha,currentColor,currentSize,x,y,type);
    }
}
