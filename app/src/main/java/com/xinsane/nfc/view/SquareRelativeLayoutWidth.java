package com.xinsane.nfc.view;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.RelativeLayout;

public class SquareRelativeLayoutWidth extends RelativeLayout {
    public SquareRelativeLayoutWidth(Context context) {
        super(context);
    }
    public SquareRelativeLayoutWidth(Context context, AttributeSet attrs) {
        super(context, attrs);
    }
    public SquareRelativeLayoutWidth(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }
    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, widthMeasureSpec);
    }
}