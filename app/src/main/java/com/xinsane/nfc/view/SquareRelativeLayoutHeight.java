package com.xinsane.nfc.view;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.RelativeLayout;

public class SquareRelativeLayoutHeight extends RelativeLayout {
    public SquareRelativeLayoutHeight(Context context) {
        super(context);
    }
    public SquareRelativeLayoutHeight(Context context, AttributeSet attrs) {
        super(context, attrs);
    }
    public SquareRelativeLayoutHeight(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }
    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(heightMeasureSpec, heightMeasureSpec);
    }
}