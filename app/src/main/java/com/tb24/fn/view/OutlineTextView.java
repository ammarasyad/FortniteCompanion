package com.tb24.fn.view;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.util.TypedValue;

import androidx.appcompat.widget.AppCompatTextView;

import com.tb24.fn.R;

public class OutlineTextView extends AppCompatTextView {
	private static final float DEFAULT_STROKE_WIDTH = 0F;
	private boolean isDrawing = false;
	private int strokeColor = 0;
	private float strokeWidth = 0;

	public OutlineTextView(Context context) {
		super(context);
		init(context, null);
	}

	public OutlineTextView(Context context, AttributeSet attrs) {
		super(context, attrs);
		init(context, attrs);
	}

	public OutlineTextView(Context context, AttributeSet attrs, int defStyleAttr) {
		super(context, attrs, defStyleAttr);
		init(context, attrs);
	}

	private void init(Context context, AttributeSet attrs) {
		if (attrs != null) {
			TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.outlineAttrs);
			strokeColor = a.getColor(R.styleable.outlineAttrs_outlineColor, getCurrentTextColor());
			strokeWidth = a.getFloat(R.styleable.outlineAttrs_outlineWidth, DEFAULT_STROKE_WIDTH);
			a.recycle();
		} else {
			strokeColor = getCurrentTextColor();
			strokeWidth = DEFAULT_STROKE_WIDTH;
		}

		setStrokeWidth(TypedValue.COMPLEX_UNIT_DIP, strokeWidth);
	}

	public void setStrokeColor(int color) {
		strokeColor = color;
	}

	public void setStrokeWidth(float width) {
		strokeWidth = width;
	}

	public void setStrokeWidth(int unit, float width) {
		strokeWidth = TypedValue.applyDimension(unit, width, getResources().getDisplayMetrics());
	}

	@Override
	public void invalidate() {
		if (isDrawing) {
			return;
		}

		super.invalidate();
	}

	@Override
	public void onDraw(Canvas canvas) {
		if (strokeWidth > 0) {
			isDrawing = true;
			int originalTextColor = getCurrentTextColor();
			Paint p = getPaint();
			p.setStyle(Paint.Style.STROKE);
			p.setStrokeWidth(strokeWidth);
			setTextColor(strokeColor);
			super.onDraw(canvas);
			setTextColor(originalTextColor);
			p.setStyle(Paint.Style.FILL);
			super.onDraw(canvas);
			isDrawing = false;
		} else {
			super.onDraw(canvas);
		}
	}
}
