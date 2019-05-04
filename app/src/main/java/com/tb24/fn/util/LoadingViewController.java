package com.tb24.fn.util;

import android.app.Activity;
import android.text.method.LinkMovementMethod;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.TextView;

import com.tb24.fn.R;

/**
 * A helper class that manages show/hide loading spinner.
 */
public class LoadingViewController {
	public final View mLoadingView;
	public final View mContentView;
	public final TextView mEmptyView;
	private State state;

	public LoadingViewController(View loadingView, View contentView, TextView emptyView) {
		mLoadingView = loadingView;
		mContentView = contentView;
		mEmptyView = emptyView;
	}

	public LoadingViewController(Activity activity, View main, CharSequence emptyText) {
		this(activity.getWindow().getDecorView(), main, emptyText);
	}

	public LoadingViewController(View wholeView, View main, CharSequence emptyText) {
		this(defaultLoader(wholeView), main, emptyText == null ? null : defaultEmptyText(wholeView, emptyText));
	}

	private static void setViewShown(final View view, boolean shown, boolean animate) {
		if (view == null) {
			return;
		}

		if (animate) {
			Animation animation = AnimationUtils.loadAnimation(view.getContext(), shown ? android.R.anim.fade_in : android.R.anim.fade_out);
			if (shown) {
				view.setVisibility(View.VISIBLE);
			} else {
				animation.setAnimationListener(new Animation.AnimationListener() {
					@Override
					public void onAnimationStart(Animation animation) {
					}

					@Override
					public void onAnimationRepeat(Animation animation) {
					}

					@Override
					public void onAnimationEnd(Animation animation) {
						view.setVisibility(View.INVISIBLE);
					}
				});
			}
			view.startAnimation(animation);
		} else {
			view.clearAnimation();
			view.setVisibility(shown ? View.VISIBLE : View.INVISIBLE);
		}
	}

	private static TextView defaultEmptyText(View whole, CharSequence text) {
		TextView textview = whole.findViewById(R.id.loadable_empty_text);
		textview.setText(text);
		textview.setMovementMethod(LinkMovementMethod.getInstance());
		return textview;
	}

	private static View defaultLoader(View whole) {
		return whole.findViewById(R.id.loadable_loading);
	}

	public void loading() {
		if (state == State.LOADING) {
			return;
		}

		boolean fromUnstated = state == null;
		setViewShown(mLoadingView, true, !fromUnstated);
		setViewShown(mContentView, false, !fromUnstated && state == State.CONTENT);

		if (mEmptyView != null) {
			setViewShown(mEmptyView, false, !fromUnstated && state == State.EMPTY);
		}

		state = State.LOADING;
	}

	public void content() {
		content(shouldShowEmpty());
	}

	private void content(boolean empty) {
		State newState = empty ? State.EMPTY : State.CONTENT;
		boolean fromLoading = state == State.LOADING, fromUnstated = state == null;
		setViewShown(mLoadingView, false, fromLoading);

		if (state != newState) {
			setViewShown(mContentView, !empty, !fromUnstated && (state == State.CONTENT && empty || !(empty && fromLoading)));
			setViewShown(mEmptyView, empty, !fromUnstated && (state == State.EMPTY && !empty || !(!empty && fromLoading)));
		}

		this.state = newState;
	}

	public void error(CharSequence error) {
		mEmptyView.setText(error);
		content(true);
	}

	public boolean shouldShowEmpty() {
		return false;
	}

	private enum State {
		CONTENT, EMPTY, LOADING
	}
}