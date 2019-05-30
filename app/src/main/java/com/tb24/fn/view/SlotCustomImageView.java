package com.tb24.fn.view;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.util.AttributeSet;

import androidx.appcompat.widget.AppCompatImageView;

public class SlotCustomImageView extends AppCompatImageView {
	private Paint paint;
	private Path path;
	private float density;
	private boolean scaleImage;
	private boolean fancyBackgroundEnabled;

	public SlotCustomImageView(Context context) {
		super(context);
		init();
	}

	public SlotCustomImageView(Context context, AttributeSet attrs) {
		super(context, attrs);
		init();
	}

	public SlotCustomImageView(Context context, AttributeSet attrs, int defStyleAttr) {
		super(context, attrs, defStyleAttr);
		init();
	}

	protected void init() {
		paint = new Paint();
		path = new Path();
		paint.setColor(0x18000000);
		density = getResources().getDisplayMetrics().density;
	}

	public void setScaleImage(boolean scaleImage) {
		this.scaleImage = scaleImage;
	}

	public void setFancyBackgroundEnabled(boolean fancyBackgroundEnabled) {
		this.fancyBackgroundEnabled = fancyBackgroundEnabled;
	}

	@Override
	protected void onDraw(Canvas canvas) {
		// RIP old phones
		if (!fancyBackgroundEnabled) {
			super.onDraw(canvas);
			return;
		}

		float indent = getHeight() / 10.0F;
		float off = getHeight() / 6.5F;
		path.reset();
		path.moveTo(getWidth(), off);
		path.lineTo(0.0F, off + indent);
		path.lineTo(0.0F, getHeight() - off);
		path.lineTo(getWidth(), getHeight() - off - indent);
		path.close();
		canvas.drawPath(path, paint);
		path.reset();
		path.moveTo(getWidth(), 0.0F);
		path.lineTo(0.0F, 0.0F);
		path.lineTo(0.0F, getHeight() - off);
		path.lineTo(getWidth(), getHeight() - off - indent);
		path.close();
		canvas.save();
		canvas.clipPath(path);

		if (scaleImage) {
			canvas.translate(0.0F, -(getHeight() / 30.0F));
			canvas.scale(1.25F, 1.25F, getWidth() / 2.0F, getHeight() / 2.0F);
		}

		super.onDraw(canvas);
		canvas.restore();
	}
}
