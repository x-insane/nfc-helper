package com.xinsane.nfc.view;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

import com.xinsane.nfc.fragment.PaintListFragment;

public class PaintView extends View {

    private byte[] data = new byte[8];
    private Paint paint_1 = new Paint();
    private Paint paint_0 = new Paint();
    private boolean enable = true;

    private void initData(AttributeSet attrs) {
        byte[] data = PaintListFragment.hexStringToByteArray(attrs.getAttributeValue("android", "text"));
        for (int i = 0; i < data.length && i < 8; ++i)
            this.data[7-i] = data[data.length-1-i];
    }
    private void initPaint() {
        paint_0.setARGB(255, 199, 133, 200);
        paint_0.setStyle(Paint.Style.STROKE);
        paint_0.setStrokeWidth(5);
        paint_1.setARGB(255, 199, 133, 200);
    }
    public PaintView(Context context) {
        super(context);
        initPaint();
    }
    public PaintView(Context context, AttributeSet attrs) {
        super(context, attrs);
        initData(attrs);
        initPaint();
    }
    public PaintView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        initData(attrs);
        initPaint();
    }

    public void setData(byte[] data) {
        this.data = new byte[8];
        for (int i = 0; i < data.length && i < 8; ++i)
            this.data[7-i] = data[data.length-1-i];
        requestLayout();
        invalidate();
    }

    public byte[] getData() {
        return data;
    }

    public void setEnable(boolean enable) {
        this.enable = enable;
    }

    private int x = -1, y = -1;

    @SuppressLint("ClickableViewAccessibility")
    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (!enable)
            return super.onTouchEvent(event);
        final float x = event.getX();
        final float y = event.getY();
        int height = getHeight() - padding * 2;
        int width = getWidth() - padding * 2;
        int bound = Math.min(height, width);
        float d = (float) (bound / 8.0);
        int cx = (int) ((x - padding) / d);
        int cy = (int) ((y - padding) / d);
        if (cx != this.x || cy != this.y) {
            this.x = cx;
            this.y = cy;
            if (cx >= 0 && cx <= 7 && cy >= 0 && cy <= 7) {
                data[cy] ^= (0x80 >> cx);
                requestLayout();
                invalidate();
            }
        }
        if (event.getAction() == MotionEvent.ACTION_UP) {
            this.x = -1;
            this.y = -1;
        }
        return true;
    }

    private int padding = 20;

    @Override
    protected void onDraw(Canvas canvas) {
        int height = getHeight() - padding * 2;
        int width = getWidth() - padding * 2;
        int bound = Math.min(height, width);
        float d = (float) (bound / 8.0);
        float r = (float) (bound / 16.0);

        for (int i = 0; i < 8; ++i) {
            for (int j = 0; j < 8; ++j) {
                if ((data[i] & (0x80 >> j)) != 0)
                    canvas.drawCircle(padding + j*d+r, padding + i*d+r, r-10, paint_1);
                else
                    canvas.drawCircle(padding + j*d+r, padding + i*d+r, r-10, paint_0);
            }
        }
    }
}
