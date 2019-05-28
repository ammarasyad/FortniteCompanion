package com.tb24.fn.view;

import android.content.Context;
import android.os.Handler;
import android.os.SystemClock;
import android.util.AttributeSet;

import androidx.appcompat.widget.AppCompatTextView;

import com.google.common.base.Supplier;

public class UpdateEverySecondTextView extends AppCompatTextView {
	private Handler handler = new Handler();
	private Supplier<CharSequence> textSupplier;
	private boolean mShouldRunTicker;
	private final Runnable mTicker = new Runnable() {
		public void run() {
			if (textSupplier != null) {
				setText(textSupplier.get());
			}

			long now = SystemClock.uptimeMillis();
			long next = now + (1000 - now % 1000);

			handler.postAtTime(mTicker, next);
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
			handler.removeCallbacks(mTicker);
		}
	}
}
