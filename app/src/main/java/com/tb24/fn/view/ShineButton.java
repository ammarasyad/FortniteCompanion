package com.tb24.fn.view;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Region;
import android.os.SystemClock;
import android.util.AttributeSet;
import android.view.animation.Interpolator;
import android.view.animation.LinearInterpolator;

import androidx.appcompat.widget.AppCompatButton;

public class ShineButton extends AppCompatButton {
	private static final long SHINE_DURATION = 500;
	private static final long SHINE_DELAY_BETWEEN = 1500;
	private Paint paint;
	private Path path;
	private Path path2;
	private Interpolator interpolator = new LinearInterpolator();

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

	protected void init() {
		paint = new Paint();
		path = new Path();
		path2 = new Path();
	}

	@Override
	protected void onDraw(Canvas canvas) {
		super.onDraw(canvas);
		int width = getWidth();
		int height = getHeight();
		float indentXLow = width / 60.0F;
		float indentXHigh = width / 25.0F;
		float indentYLow = height / 16.0F;
		float indentYHigh = height / 8.0F;
		path.reset();
		path.moveTo(width - indentXLow, indentYLow);
		path.lineTo(indentXHigh, indentYHigh);
		path.lineTo(indentXLow, height - indentYLow);
		path.lineTo(width - indentXHigh, height - indentYHigh);
		path.close();
		canvas.save();
		canvas.clipPath(path, Region.Op.DIFFERENCE);
		paint.setColor(0x90FFFFFF);
		canvas.drawPaint(paint);
		canvas.restore();

		if (!isEnabled() || !isShown()) {
			return;
		}

		long a = SystemClock.uptimeMillis() % (SHINE_DURATION + SHINE_DELAY_BETWEEN);

		if (a <= SHINE_DURATION) {
			float shineWidth = 0.25F * width;
			float off = interpolator.getInterpolation((float) a / (float) SHINE_DURATION) * (shineWidth + width);
			path2.reset();
			float indentDelta = indentXHigh - indentXLow;
			path2.moveTo(shineWidth, 0);
			path2.lineTo(indentDelta, 0);
			path2.lineTo(0, height);
			path2.lineTo(shineWidth - indentDelta, height);
			path2.close();
			canvas.save();
			canvas.clipPath(path);
			canvas.translate(-shineWidth + off, 0.0F);
			paint.setColor(0xB0FFFFFF);
			canvas.drawPath(path2, paint);
			canvas.restore();
		}

		invalidate();
	}
}