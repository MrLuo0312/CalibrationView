package com.mrl.calibrationview;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.support.annotation.FloatRange;
import android.support.annotation.IntDef;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.util.Log;
import android.util.TypedValue;
import android.view.MotionEvent;
import android.view.VelocityTracker;
import android.view.View;
import android.view.ViewConfiguration;
import android.widget.Scroller;

/**
 * @author : MrL
 * @date : 2018/6/30  10:11
 * @remark :
 **/
public class CalibrationView extends View {

    private float gapWidth;
    private float shortHeight;
    private float longHeight;
    private float triangleHeight;
    private int startInt;
    private int endInt;
    private float midMark;
    private Paint cellPaint;
    private float cellWidth;
    private Paint textCellPaint;
    private Paint pathPaint;

    public static final int Point = 1;
    public static final int View = 2;

    private int currentTouchMode = View;
    private float lastX;
    private Scroller scroller;
    private int minimumFlingVelocity;
    private int scaledTouchSlop;
    private VelocityTracker velocityTracker;
    private float mSpeed = 1f;
    private Path trianglePath;
    private Paint textScalePaint;
    private float startScale;

    @IntDef({Point, View})
    public @interface TouchMode {
    }

    public void setMode(@TouchMode int mode) {
        currentTouchMode = mode;
    }

    public CalibrationView(Context context) {
        super(context);
    }

    public CalibrationView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        initPaint(context);
        applyAttrs(context, attrs);
    }

    private void applyAttrs(Context context, AttributeSet attrs) {
        TypedArray array = context.obtainStyledAttributes(attrs, R.styleable.CalibrationView);
        gapWidth = DiaplayUtil.dip2px(array.getDimension(R.styleable.CalibrationView_gapWidth, 5), context);
        shortHeight = DiaplayUtil.dip2px(array.getDimension(R.styleable.CalibrationView_shortHeight, 16), context);
        triangleHeight = shortHeight / 3 * 2;

        longHeight = DiaplayUtil.dip2px(array.getDimension(R.styleable.CalibrationView_longHeight, 26), context);
        startInt = array.getInteger(R.styleable.CalibrationView_startInt, 0);
        endInt = array.getInteger(R.styleable.CalibrationView_endInt, 100);
        array.recycle();
        if (endInt < startInt) {
            throw new IllegalArgumentException("刻度结束值应大于起始值!!!");
        }
        scroller = new Scroller(getContext());
        ViewConfiguration viewConfiguration = ViewConfiguration.get(getContext());
        minimumFlingVelocity = viewConfiguration.getScaledMinimumFlingVelocity();
        scaledTouchSlop = viewConfiguration.getScaledTouchSlop();

        startScale = 0f * gapWidth;
    }

    public CalibrationView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        initPaint(context);
        applyAttrs(context, attrs);
    }

    private void initPaint(Context context) {
        cellPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        cellPaint.setColor(Color.BLUE);
        cellWidth = DiaplayUtil.dip2px(2, context);
        cellPaint.setStrokeWidth(cellWidth);
        cellPaint.setStyle(Paint.Style.FILL);

        textCellPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        textCellPaint.setColor(Color.DKGRAY);
        textCellPaint.setTextSize(TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP,
                14, context.getResources().getDisplayMetrics()));

        pathPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        pathPaint.setColor(Color.YELLOW);
        pathPaint.setStyle(Paint.Style.FILL);

        textScalePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        textScalePaint.setColor(Color.WHITE);
        textScalePaint.setTextSize(TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP,
                14, context.getResources().getDisplayMetrics()));
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        int widthPixels = getContext().getResources().getDisplayMetrics().widthPixels;
        if (widthPixels > (endInt - startInt) * gapWidth) {
            midMark = widthPixels / 2f;
        } else
            midMark = w / 2f;
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        int widthMode = MeasureSpec.getMode(widthMeasureSpec);
        int heightMode = MeasureSpec.getMode(heightMeasureSpec);
        int heightSize = MeasureSpec.getSize(heightMeasureSpec);
        int widthSize = MeasureSpec.getSize(widthMeasureSpec);
        if (heightMode == MeasureSpec.AT_MOST) {
            heightMeasureSpec = MeasureSpec.makeMeasureSpec((int) DiaplayUtil.dip2px(80, getContext()), MeasureSpec.EXACTLY);
        }
        if (widthMode == MeasureSpec.AT_MOST) {
            widthMeasureSpec = MeasureSpec.makeMeasureSpec((int) DiaplayUtil.dip2px(gapWidth * (endInt - startInt), getContext()), MeasureSpec.EXACTLY);
        }
        setMeasuredDimension(widthMeasureSpec, heightMeasureSpec);
    }


    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        drawScale(canvas);
        drawTriangle(canvas);
    }

    private void drawScaleText(Canvas canvas) {
        String text = "当前刻度位置:" + String.valueOf((int) (midMark / gapWidth));
        float width = textScalePaint.measureText(text);
        canvas.drawText(text, getWidth() / 2 - width / 2, getHeight() - 2 * textScalePaint.descent(), textScalePaint);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (velocityTracker == null)
            velocityTracker = VelocityTracker.obtain();
        velocityTracker.addMovement(event);
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                scroller.forceFinished(true);
                lastX = event.getX();
                break;
            case MotionEvent.ACTION_MOVE:
                float dx = event.getX() - lastX;
                lastX = event.getX();
                // Log.i("golf", "dx = " + dx);
                if (Math.abs(dx) > scaledTouchSlop) {
                    if (currentTouchMode == Point) {
                        midMark += dx * mSpeed;
                        if (midMark < 0) {
                            midMark = 0;
                        }
                        if (midMark > getWidth()) {
                            midMark = getWidth();
                        }
                        invalidate();
                    } else if (currentTouchMode == View) {
                        startScale += dx * mSpeed;
                        if (startScale % gapWidth != 0f) {  // 校正处理
                            startScale -= startScale % gapWidth;
                        }
                        invalidate();
                    }
                }
                break;
            case MotionEvent.ACTION_CANCEL:
            case MotionEvent.ACTION_UP:
                if (currentTouchMode == Point) {
                    velocityTracker.computeCurrentVelocity(1000);
                    float xVelocity = velocityTracker.getXVelocity();
                    if (Math.abs(xVelocity) > minimumFlingVelocity) {
                        scroller.fling((int) (midMark + 0.5), 0, (int) xVelocity, 0,
                                0, (int) ((endInt - startInt) * gapWidth + 0.5), 0, 0);
                        postInvalidate();
                    }
                }else if(currentTouchMode == View){
                    velocityTracker.computeCurrentVelocity(1000);
                    float xVelocity = velocityTracker.getXVelocity();
                    if (Math.abs(xVelocity) > minimumFlingVelocity) {
                        scroller.fling((int) (startScale + 0.5), 0, (int) xVelocity, 0,
                                0, (int) ((endInt - startInt) * gapWidth + 0.5), 0, 0);
                        postInvalidate();
                    }
                }
                break;
        }
        return true;
    }

    @Override
    public void computeScroll() {
        super.computeScroll();
        if (scroller.computeScrollOffset()) {
            if(currentTouchMode == Point){
                midMark = scroller.getCurrX();
            }else if(currentTouchMode == View){
                startScale = scroller.getCurrX();
            }
            invalidate();
        }
    }

    public void setMoveSpeed(@FloatRange(from = 0.0, to = 1.0) float speed) {
        mSpeed = speed;
    }

    private void drawTriangle(Canvas canvas) {
        if (trianglePath == null) {
            trianglePath = new Path();
        }
        trianglePath.reset();
        if (midMark > getWidth()) {
            midMark = getWidth();
        }
        if (midMark % gapWidth != 0f) {  // 校正处理
            midMark -= midMark % gapWidth;
        }
        trianglePath.moveTo(midMark - triangleHeight, 0);
        trianglePath.lineTo(midMark + triangleHeight, 0);
        trianglePath.lineTo(midMark, triangleHeight);
        trianglePath.close();
        canvas.drawLine(midMark, 0, midMark, getMeasuredHeight(), pathPaint);
        canvas.drawPath(trianglePath, pathPaint);

        drawScaleText(canvas);
    }

    private void drawScale(Canvas canvas) {
        int currentInt = 0;
        float total = (endInt - startInt) * gapWidth;
        float midCell = cellWidth / 2;
        float currentScale = startScale;
        while (currentScale < total + gapWidth) { // 多绘制一格
            Log.i("golf", "currentScale = " + currentScale);
            if (currentInt % 10 == 0f) {  // 10进制刻度尺，刻度稍长
                cellPaint.setColor(Color.BLUE);
                cellPaint.setStrokeWidth(cellWidth);
                canvas.drawRect(currentScale - midCell, 0f, currentScale + midCell, longHeight, cellPaint);
                canvas.drawText(String.valueOf(currentInt), currentScale + 10, longHeight + 5, textCellPaint);
            } else {
                cellPaint.setColor(Color.BLACK);
                cellPaint.setStrokeWidth(cellWidth / 2);
                canvas.drawRect(currentScale - midCell, 0f, currentScale + midCell, shortHeight, cellPaint);
            }
            currentInt += 1;
            currentScale += gapWidth;
        }
    }
}
