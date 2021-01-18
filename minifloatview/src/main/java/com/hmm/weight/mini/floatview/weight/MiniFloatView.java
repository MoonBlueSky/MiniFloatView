package com.hmm.weight.mini.floatview.weight;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.PixelFormat;
import android.os.Build;
import android.os.Handler;
import android.os.Message;
import android.provider.Settings;
import android.util.AttributeSet;
import android.util.Log;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.WindowManager;
import android.view.animation.AccelerateInterpolator;
import android.widget.AbsoluteLayout;
import android.widget.ImageView;
import android.widget.RelativeLayout;


import com.hmm.weight.mini.floatview.utils.ScreenUtils;

import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class MiniFloatView extends RelativeLayout {
    private static final String TAG = "FloatingView";
    private int inputStartX = 0;
    private int inputStartY = 0;
    private int viewStartX = 0;
    private int viewStartY = 0;
    private int inMovingX = 0;
    private int inMovingY = 0;

    private WindowManager.LayoutParams mFloatBallParams;
    private WindowManager mWindowManager;
    private Context mContext;
    private int mScreenHeight;
    private int mScreenWidth;
    private boolean mIsShow;
    private ImageView mSdvCover;
    private int mDp167;
    private int mDp48;
    private int mFloatDpSize;
    private boolean mLoading;
    private ValueAnimator mValueAnimator;
    private boolean moveVertical;
    private static int mFloatBallParamsX = 0;
    private static int mFloatBallParamsY = 0;
    private ScheduledFuture<?> scheduledFuture;
    private ScheduledThreadPoolExecutor mDelayExecutor;

    private Handler mHandler;
    /**
     * 用于控制贴边后的隐藏的坐标 X
     * */
    private static int mFloatBallParamsLayoutX;
    /**
     * 用于控制贴边后的隐藏的坐标 Y
     * */
    private static int mFloatBallParamsLayoutY;

    public boolean isShow() {
        return mIsShow;
    }

    public MiniFloatView(Context context) {
        this(context, null);
    }

    public MiniFloatView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public MiniFloatView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);

        mContext = context;
        inflate(context,context.getResources().getIdentifier("hmm_mini_float_view","layout", context.getPackageName()) , this);

        mSdvCover = findViewById(context.getResources().getIdentifier("sdv_cover","id", context.getPackageName()));
        mHandler = new Handler(new Handler.Callback(){
            @Override
            public boolean handleMessage( Message message) {
                Log.i(TAG, "message what = " + message.what);
                if (message.what == 0x100){
                    updateImageViewLayoutParams(mFloatBallParamsLayoutX,mFloatBallParamsLayoutY);
                }
                return false;
            }
        });
        mDelayExecutor = new ScheduledThreadPoolExecutor(1,new ThreadPoolExecutor.AbortPolicy());
        mDelayExecutor.setMaximumPoolSize(1);
        initFloatBallParams(mContext);

        mScreenWidth = ScreenUtils.getScreenWidth(context);
        mScreenHeight = ScreenUtils.getScreenHeight(context);
        mDp167 = (int) ScreenUtils.dp2px(mContext, 167);
        mDp48 = (int) ScreenUtils.dp2px(mContext, 48);
        mFloatDpSize = (int) ScreenUtils.dp2px(mContext, 25);

        Log.e(TAG,"ScreenWidth:"+ScreenUtils.getScreenWidth(mContext)+",ScreenHeight:"+ ScreenUtils.getScreenHeight(mContext));
//        slop = ViewConfigurationCompat.getScaledPagingTouchSlop(ViewConfiguration.get(context));
        slop = 3;


    }


    private class AdsorptionTask implements Runnable{

        @Override
        public void run() {

            Log.e(TAG,"执行");
            mHandler.sendEmptyMessage(0x100);
        }
    }


    /**
     * 获取悬浮球的布局参数
     */
    private void initFloatBallParams(Context context) {

        mFloatBallParams = new WindowManager.LayoutParams();
        mFloatBallParams.flags = mFloatBallParams.flags
                | WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH
                | WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL
                | WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                //此处是为了让悬浮球显示在通知栏区域避免遮挡
                |WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS;
        mFloatBallParams.dimAmount = 0.2f;


        mFloatBallParams.height = WindowManager.LayoutParams.WRAP_CONTENT;
        mFloatBallParams.width = WindowManager.LayoutParams.WRAP_CONTENT;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            mFloatBallParams.layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES;
        }
        mFloatBallParams.gravity = Gravity.LEFT | Gravity.TOP;
        //android 6.0之后对悬浮窗有限制需要另外申请权限
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O){
            if (Settings.canDrawOverlays(mContext)) {
                mFloatBallParams.type = WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY;
            }
        }else {
            mFloatBallParams.type =  WindowManager.LayoutParams.TYPE_SYSTEM_ALERT;
        }
        mFloatBallParams.format = PixelFormat.RGBA_8888;
        // 设置整个窗口的透明度
        mFloatBallParams.alpha = 0.8f;
        // 显示悬浮球在屏幕左上角
        mFloatBallParams.x = 0;
        mFloatBallParams.y = 0;
        mWindowManager = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        //此处rootView是对话框的顶层View


    }

    private int slop;
    private boolean isDrag;



    private boolean isPressedShow  = false;
    @Override
    public boolean onTouchEvent(MotionEvent event) {
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                if (null != mValueAnimator && mValueAnimator.isRunning()) {
                    mValueAnimator.cancel();
                }
                if (scheduledFuture!=null&&!scheduledFuture.isDone()){
                    scheduledFuture.cancel(false);
                    scheduledFuture = null;
                }
                setPressed(true);
                isDrag = false;

                mScreenWidth = ScreenUtils.getScreenWidth(mContext);
                mScreenHeight = ScreenUtils.getScreenHeight(mContext);
                inputStartX = (int) event.getRawX();
                inputStartY = (int) event.getRawY();
                viewStartX = mFloatBallParams.x;
                viewStartY = mFloatBallParams.y;
                updateImageViewLayoutParams(0,0);
                Log.e(TAG,"welt -->>> viewStartX:"+viewStartX+";viewStartY:"+viewStartY);

                Log.e(TAG,"ScreenWidth:"+ScreenUtils.getScreenWidth(mContext)+",ScreenHeight:"+ ScreenUtils.getScreenHeight(mContext));
                break;
            case MotionEvent.ACTION_MOVE:
                inMovingX = (int) event.getRawX();
                inMovingY = (int) event.getRawY();
                int moveX = viewStartX + inMovingX - inputStartX;
                int moveY = viewStartY + inMovingY - inputStartY;

                if (mScreenHeight <= 0 || mScreenWidth <= 0) {
                    isDrag = false;
                    break;
                }

                if (Math.abs(inMovingX - inputStartX) > slop
                        && Math.abs(inMovingY - inputStartY) > slop) {
                    isDrag = true;
                    mFloatBallParams.x = moveX;
                    mFloatBallParams.y = moveY;
                    mFloatBallParamsLayoutX = 0;
                    mFloatBallParamsLayoutY = 0;
                    updateImageViewLayoutParams(mFloatBallParamsLayoutX,mFloatBallParamsLayoutY);
                } else {
                    isDrag = false;
                }
                break;
            case MotionEvent.ACTION_UP:
                if (isDrag) {
                    //恢复按压效果
                    setPressed(false);
                }
                //吸附贴边计算和动画
                welt();
                if (!isPressedShow&&!isDrag){
                    isPressedShow = true;

                    return false;
                }
                Log.d(TAG,"点击了 isPressedShow="+isPressedShow+";是否为单击 isDrag："+isDrag);
                break;
            default:
                break;
        }
        return isDrag || super.onTouchEvent(event);
    }

    private boolean isLeftSide() {
        return getX() == 0;
    }

    private boolean isRightSide() {
        return getX() == mScreenWidth - getWidth();
    }

    /**
     *  吸附贴边计算和动画
     */
    private void welt() {

        int movedX = mFloatBallParams.x;
        int movedY = mFloatBallParams.y;
        Log.e(TAG,"welt -->>> movedX:"+movedX+";movedY:"+movedY);
        moveVertical = false;
        /*
        *四种回弹效果的判断
        * 1.垂直方向移动位移不足自身高度，横向位移有效且在屏幕宽度范围内 （距离顶部较近，向上西服）
        * 2.垂直方向移动位移大于自身两杯高度，横向位移有效且在屏幕宽度范围内 （距离底部较近，向下吸附）
        * 3.
        * */


        //水平方向回弹的计算判定
        //1.垂直方向移动位移需要比自身高度大
        //2.水平方向位移大于有效移动范围
        //3.水平方向位移大于本身宽度且为有效移动
        if (mFloatBallParams.y < getHeight() && mFloatBallParams.x >= slop && mFloatBallParams.x <= mScreenWidth - getWidth() - slop) {
            movedY = 0;
            mFloatBallParamsLayoutX = 0;
            mFloatBallParamsLayoutY = -mFloatDpSize;
            Log.e(TAG,"welt1 -->>> movedX:"+movedX+";movedY:"+movedY);
        } else if (mFloatBallParams.y > mScreenHeight - getHeight() * 2 && mFloatBallParams.x >= slop && mFloatBallParams.x <= mScreenWidth - getWidth() - slop) {
            movedY = mScreenHeight;
            mFloatBallParamsLayoutX = 0;
            mFloatBallParamsLayoutY = mFloatDpSize;
            Log.e(TAG,"welt2 -->>> movedX:"+movedX+";movedY:"+movedY);
        } else {
            moveVertical = true;
            if (mFloatBallParams.x < mScreenWidth / 2 - getWidth() / 2) {
                movedX = 0;
                mFloatBallParamsLayoutY = 0;
                mFloatBallParamsLayoutX = -mFloatDpSize;
                Log.e(TAG,"welt3 -->>> movedX:"+movedX+";movedY:"+movedY);
            } else {
                movedX = mScreenWidth - getWidth();
                mFloatBallParamsLayoutY = 0;
                mFloatBallParamsLayoutX = mFloatDpSize;
                Log.e(TAG,"welt4 -->>> movedX:"+movedX+";movedY:"+movedY);
            }
        }


        int duration;
        if (moveVertical) {
            mValueAnimator = ValueAnimator.ofInt(mFloatBallParams.x, movedX);
            duration = movedX - mFloatBallParams.x;
        } else {
            mValueAnimator = ValueAnimator.ofInt(mFloatBallParams.y, movedY);
            duration = movedY - mFloatBallParams.y;
        }
        mValueAnimator.setDuration(Math.abs(duration));
        final int finalMovedX = movedX;
        final int finalMovedY = movedY;
        mValueAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {

            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                Integer level = (Integer) animation.getAnimatedValue();
                if (moveVertical) {
                    mFloatBallParams.x = level;
                    if (level== finalMovedX){
                        startAutoAdsorptionTimer();
                    }
                } else {
                    mFloatBallParams.y = level;
                    if (level== finalMovedY){
                        startAutoAdsorptionTimer();
                    }
                }
                updateWindowManager();
            }
        });
        mValueAnimator.setInterpolator(new AccelerateInterpolator());
        mValueAnimator.start();
    }

    @Override
    protected void onDetachedFromWindow() {
        if (null != mValueAnimator && mValueAnimator.isRunning()) {
            mValueAnimator.cancel();
        }//进入下个页面的时候贴边动画暂停，下个页面attached时候会继续动画， 你手速快的话还能在中途接住球继续拖动
        super.onDetachedFromWindow();
    }

    /**
     * 开启自动吸附的计时器
     * */
    private void startAutoAdsorptionTimer(){
        if (mDelayExecutor != null){
            if (scheduledFuture!=null&&!scheduledFuture.isDone()){
                scheduledFuture.cancel(false);
                scheduledFuture = null;
            }
            // 新建一个任务
            final AdsorptionTask adsorptionTask = new AdsorptionTask();
            scheduledFuture = mDelayExecutor.schedule(adsorptionTask, 3, TimeUnit.SECONDS);
            int i = 0;
            while (i<15){
                mDelayExecutor.schedule(adsorptionTask, 3, TimeUnit.SECONDS);

                Log.e(TAG,"mDelayExecutor queue size:"+mDelayExecutor.getQueue().size());
                i++;
            }
        }
    }


    /**
     * 更新图片坐标偏移
     * */
    private void updateImageViewLayoutParams(int x,int y){
        if (!mIsShow){
            return;
        }
        AbsoluteLayout.LayoutParams params = ((AbsoluteLayout.LayoutParams) mSdvCover.getLayoutParams());
        params.x = x;
        params.y = y;
        mSdvCover.setLayoutParams(params);
        mFloatBallParams.alpha = 0.8f;
        if (x!=0||y!=0){
            // 设置整个窗口的透明度
            mFloatBallParams.alpha = 0.3f;
            isPressedShow = false;
        }

        updateWindowManager();
    }

    /**
     * 获取状态栏高度
     *
     * @return
     */
    public int getStatusBarHeight() {
        int result = 0;
        int resourceId = mContext.getResources().getIdentifier("status_bar_height", "dimen",
                "android");
        if (resourceId > 0) {
            result = mContext.getResources().getDimensionPixelSize(resourceId);
        }
        return result;
    }

    /**
     * 贴图片
     */
    public ImageView circleImageView() {
        return mSdvCover;
    }

    /**
     * 显示悬浮
     */
    public void showFloat() {
        mIsShow = true;
        mFloatBallParamsLayoutX = 0;
        mFloatBallParamsLayoutY = 0;
        if (mFloatBallParamsX == -1 || mFloatBallParamsY == -1) {
            //首次打开时，初始化的位置
            mFloatBallParams.x = mScreenWidth - mDp48;
            mFloatBallParams.y = mScreenHeight - mDp167 - mDp48;

            mFloatBallParamsX = mFloatBallParams.x;
            mFloatBallParamsY = mFloatBallParams.y;
        } else {
            mFloatBallParams.x = mFloatBallParamsX;
            mFloatBallParams.y = mFloatBallParamsY;
        }

        mWindowManager.addView(this, mFloatBallParams);
        //吸附贴边计算和动画
        welt();
    }

    /**
     * 移除该view
     */
    public void dismissFloatView() {
        mIsShow = false;
        mWindowManager.removeViewImmediate(this);
        //关闭线程池任务
        if (mDelayExecutor != null){
            if (scheduledFuture!=null&&!scheduledFuture.isDone()){
                scheduledFuture.cancel(false);
                scheduledFuture = null;
            }
        }
    }

    /**
     * 更新位置，并保存到手机内存
     */
    public void updateWindowManager() {
        if (!mIsShow){
            return;
        }
        mWindowManager.updateViewLayout(this, mFloatBallParams);
        mFloatBallParamsX = mFloatBallParams.x;
        mFloatBallParamsY = mFloatBallParams.y;

        Log.e(TAG,"updateWindowManager ScreenWidth:"+ScreenUtils.getScreenWidth(mContext)+",ScreenHeight:"+ ScreenUtils.getScreenHeight(mContext));
    }


    private void loadData() {
        if (mLoading) {
            return;
        }
        mLoading = true;
        // TODO网络请求
    }





}
