package com.tb24.fn.util;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.RectF;
import android.graphics.drawable.BitmapDrawable;
import android.util.AttributeSet;
import android.widget.ImageView;

public class FitWidthAtBottomImageView extends ImageView {
	public FitWidthAtBottomImageView(Context context) {
		super(context);
	}

	public FitWidthAtBottomImageView(Context context, AttributeSet attrs) {
		super(context, attrs);
	}

	public FitWidthAtBottomImageView(Context context, AttributeSet attrs, int defStyleAttr) {
		super(context, attrs, defStyleAttr);
	}

	public FitWidthAtBottomImageView(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
		super(context, attrs, defStyleAttr, defStyleRes);
	}

	@Override
	protected void onDraw(Canvas canvas) {
		int i = getWidth() - getPaddingLeft() - getPaddingRight();
		int j = getHeight() - getPaddingTop() - getPaddingBottom();
		if (getBackground() != null) {
			getBackground().draw(canvas);
		}
		if (getDrawable() != null && getDrawable() instanceof BitmapDrawable) {
			Bitmap bitmap = ((BitmapDrawable) getDrawable()).getBitmap();
			int h = bitmap.getHeight() * i / bitmap.getWidth();
			canvas.drawBitmap(bitmap, null, new RectF(getPaddingLeft(), getPaddingTop() + j - h, getPaddingLeft() + i, getHeight() - getPaddingBottom()), null);
		} else {
			super.onDraw(canvas);
		}

	}
}