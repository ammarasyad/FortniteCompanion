package com.tb24.fn.view;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.util.AttributeSet;

import androidx.appcompat.widget.AppCompatTextView;

import com.tb24.fn.util.Utils;

public class StrikeTextView extends AppCompatTextView {
	public static final int Y_OFF = 4;
	private Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
	private float parsedOff;
	private int strikeColor = 0xFFE1564B;

	public StrikeTextView(Context context) {
		super(context);
		init(context);
	}

	public StrikeTextView(Context context, AttributeSet attrs) {
		super(context, attrs);
		init(context);
	}

	public StrikeTextView(Context context, AttributeSet attrs, int defStyleAttr) {
		super(context, attrs, defStyleAttr);
		init(context);
	}

	public void setStrikeColor(int strikeColor) {
		this.strikeColor = strikeColor;
		paint.setColor(strikeColor);
		invalidate();
	}

	public void init(Context context) {
		paint.setStrokeWidth(Utils.dp(context.getResources(), 2));
		paint.setColor(strikeColor);
		parsedOff = Utils.dp(context.getResources(), Y_OFF);
	}

	@Override
	protected void onDraw(Canvas canvas) {
		super.onDraw(canvas);
		canvas.drawLine(0, getHeight() - parsedOff, getWidth(), parsedOff, paint);
	}
}