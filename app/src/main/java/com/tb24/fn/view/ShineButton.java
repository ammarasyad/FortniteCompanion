package com.tb24.fn.view;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Region;
import android.os.SystemClock;
import android.util.AttributeSet;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.Interpolator;
import android.widget.Button;

import com.tb24.fn.util.Utils;

@SuppressLint("AppCompatCustomView")
public class ShineButton extends Button {
	private static final float SHINE_DURATION = 500.0F;
	private static final float SHINE_DELAY_BETWEEN = 2000.0F;
	private Paint paint;
	private Path path;
	private Path path2;
	private float indentLow;
	private float indentHigh;
	private Interpolator interpolator = new AccelerateDecelerateInterpolator();

	public ShineButton(Context context) {
		super(context);
		init();
	}

	public ShineButton(Context context, AttributeSet attrs) {
		super(context, attrs);
		init();
	}

	public ShineButton(Context context, AttributeSet attrs, int defStyleAttr) {
		super(context, attrs, defStyleAttr);
		init();
	}

	public ShineButton(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
		super(context, attrs, defStyleAttr, defStyleRes);
		init();
	}

	protected void init() {
		paint = new Paint();
		paint.setColor(0x90FFFFFF);
		path = new Path();
		path2 = new Path();
		indentLow = Utils.dpF(getResources(), 3.0F);
		indentHigh = Utils.dpF(getResources(), 6.0F);
	}

	@Override
	protected void onDraw(Canvas canvas) {
		if (!isShown()) {
			return;
		}

		super.onDraw(canvas);

		path.reset();
		path.moveTo(getWidth() - indentLow, indentLow);
		path.lineTo(indentHigh, indentHigh);
		path.lineTo(indentLow, getHeight() - indentLow);
		path.lineTo(getWidth() - indentHigh, getHeight() - indentHigh);
		path.close();
		canvas.save();
		canvas.clipPath(path, Region.Op.DIFFERENCE);
		canvas.drawRect(0, 0, getWidth(), getHeight(), paint);
		canvas.restore();

		float a = SystemClock.uptimeMillis() % (SHINE_DURATION + SHINE_DELAY_BETWEEN);

		if (a <= SHINE_DURATION) {
			float shineWidth = 0.2F * getWidth();
			float off = interpolator.getInterpolation(a / SHINE_DURATION) * (shineWidth + getWidth());
			path2.reset();
			float indentDelta = indentHigh - indentLow;
			path2.moveTo(shineWidth, 0);
			path2.lineTo(indentDelta, 0);
			path2.lineTo(0, getHeight());
			path2.lineTo(shineWidth - indentDelta, getHeight());
			path2.close();
			canvas.save();
			canvas.clipPath(path);
			canvas.translate(-shineWidth + off, 0.0F);
			canvas.drawPath(path2, paint);
			canvas.restore();
		}

		invalidate();
	}
}