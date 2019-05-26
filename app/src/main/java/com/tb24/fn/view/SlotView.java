package com.tb24.fn.view;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.view.ViewGroup;
import android.widget.RelativeLayout;

import com.tb24.fn.util.Utils;

public class SlotView extends RelativeLayout {
	private Paint paint;
	private Rect rect = new Rect();
	private int one;

	public SlotView(Context context) {
		super(context);
		init();
	}

	public SlotView(Context context, AttributeSet attrs) {
		super(context, attrs);
		init();
	}

	public SlotView(Context context, AttributeSet attrs, int defStyleAttr) {
		super(context, attrs, defStyleAttr);
		init();
	}

	public SlotView(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
		super(context, attrs, defStyleAttr, defStyleRes);
		init();
	}

	protected void init() {
		paint = new Paint();
		paint.setStyle(Paint.Style.STROKE);
		paint.setStrokeWidth(Utils.dp(getResources(), 3));
		one = (int) Utils.dp(getResources(), 1);
	}

	@Override
	protected void onAttachedToWindow() {
		super.onAttachedToWindow();

		if (getParent() != null && ((ViewGroup) getParent()).getClipChildren()) {
			((ViewGroup) getParent()).setClipChildren(false);
		}
	}

	@Override
	protected void onDraw(Canvas canvas) {
		super.onDraw(canvas);

		if (isSelected() || isHovered() || isFocused() || isPressed()) {
			getDrawingRect(rect);
			int by = (int) -paint.getStrokeWidth() + one + 1;
			rect.inset(by, by);
			paint.setColor(isPressed() ? 0xFFFFFFFF : 0xFFFFF500);
			canvas.drawRect(rect, paint);
		}
	}
}
