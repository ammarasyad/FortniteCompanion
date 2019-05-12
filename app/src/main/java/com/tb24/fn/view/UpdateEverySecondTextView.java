package com.tb24.fn.view;

import android.annotation.SuppressLint;
import android.content.Context;
import android.os.SystemClock;
import android.util.AttributeSet;
import android.widget.TextView;

import com.google.common.base.Supplier;

@SuppressLint("AppCompatCustomView")
public class UpdateEverySecondTextView extends TextView {
	private boolean mShouldRunTicker;
	private Supplier<CharSequence> textSupplier;
	private final Runnable mTicker = new Runnable() {
		public void run() {
			if (textSupplier != null) {
				setText(textSupplier.get());
			}

			long now = SystemClock.uptimeMillis();
			long next = now + (1000 - now % 1000);

			getHandler().postAtTime(mTicker, next);
		}
	};

	public UpdateEverySecondTextView(Context context) {
		super(context);
	}

	public UpdateEverySecondTextView(Context context, AttributeSet attrs) {
		super(context, attrs);
	}

	public UpdateEverySecondTextView(Context context, AttributeSet attrs, int defStyleAttr) {
		super(context, attrs, defStyleAttr);
	}

	public UpdateEverySecondTextView(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
		super(context, attrs, defStyleAttr, defStyleRes);
	}

	public void setTextSupplier(Supplier<CharSequence> textSupplier) {
		this.textSupplier = textSupplier;
	}

	@Override
	public void onVisibilityAggregated(boolean isVisible) {
		super.onVisibilityAggregated(isVisible);

		if (!mShouldRunTicker && isVisible) {
			mShouldRunTicker = true;
			mTicker.run();
		} else if (mShouldRunTicker && !isVisible) {
			mShouldRunTicker = false;
			getHandler().removeCallbacks(mTicker);
		}
	}
}
