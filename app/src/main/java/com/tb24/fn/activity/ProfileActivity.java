package com.tb24.fn.activity;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.view.ViewGroup;
import android.widget.ScrollView;
import android.widget.TextView;

import com.google.common.base.Function;
import com.google.common.base.Joiner;
import com.google.common.collect.Lists;
import com.tb24.fn.R;
import com.tb24.fn.model.EpicError;
import com.tb24.fn.model.WorldInfoResponse;
import com.tb24.fn.util.LoadingViewController;
import com.tb24.fn.util.Utils;

import java.io.IOException;
import java.util.Arrays;

import retrofit2.Call;
import retrofit2.Response;

public class ProfileActivity extends BaseActivity {
	private LoadingViewController lc;
	private Call<WorldInfoResponse> callWorldInfo;
	private WorldInfoResponse data;
	private TextView v;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.common_loadable_framed);
		setupActionBar();
		//TODO display past season data
		ViewGroup frame = findViewById(R.id.main_content);
		ScrollView scrollView = new ScrollView(this);
		this.v = new TextView(this);
		int p = (int) Utils.dp(getResources(), 16);
		v.setPadding(p, p, p, p);
		scrollView.addView(v);
		frame.addView(scrollView, -1, -1);
		lc = new LoadingViewController(this, frame, "");
	}

	private void loadStwWorldInfo() {
		lc.loading();
		callWorldInfo = getThisApplication().fortnitePublicService.pveWorldInfo();
		new Thread() {
			@Override
			public void run() {
				try {
					final Response<WorldInfoResponse> execute = callWorldInfo.execute();

					if (execute.isSuccessful()) {
						data = execute.body();
						runOnUiThread(new Runnable() {
							@Override
							public void run() {
								lc.content();
								displayData();
							}
						});
					} else {
						runOnUiThread(new Runnable() {
							@Override
							public void run() {
								lc.error(EpicError.parse(execute).getDisplayText());
							}
						});
					}
				} catch (final IOException e) {
					runOnUiThread(new Runnable() {
						@Override
						public void run() {
							lc.error(Utils.userFriendlyNetError(e));
						}
					});
				}
			}
		}.start();
	}

	private void displayData() {
//		v.setText(sb.toString());
	}
}
