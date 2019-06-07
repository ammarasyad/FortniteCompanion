package com.tb24.fn.view;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.LinearLayout;

import androidx.annotation.Nullable;

public class LockerRowLayout extends LinearLayout {


    public LockerRowLayout(Context context) {
        super(context);
    }

    public LockerRowLayout(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    public LockerRowLayout(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        // TODO fit the contents if the width is too small, I'm not a pro on this so I can't do this now
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
//		Log.v("LockerRowLayout", "onMeasure w " + MeasureSpec.toString(widthMeasureSpec));
//		Log.v("LockerRowLayout", "onMeasure h " + MeasureSpec.toString(heightMeasureSpec));
//        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(0, (int) Utils.dp(getResources(), 96));
//        params.weight = 1F;

//        int childCount = this.getChildCount();
//        Log.v("childCount", String.valueOf(childCount));
//        for (int i = 0; i < childCount; i++) {
//            View view = this.getChildAt(i);
//            view.setLayoutParams(params);
//        }
//
//		if ((View.MEASURED_STATE_MASK & getMeasuredWidthAndState()) == View.MEASURED_STATE_TOO_SMALL) {
//			Log.d("LockerRowLayout", "too small !!!");
//		}
    }
}