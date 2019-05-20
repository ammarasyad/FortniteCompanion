package com.tb24.fn.activity;

import android.os.Bundle;
import android.view.ViewGroup;
import android.widget.ScrollView;
import android.widget.TextView;

import com.tb24.fn.R;
import com.tb24.fn.model.WorldInfoResponse;
import com.tb24.fn.util.LoadingViewController;
import com.tb24.fn.util.Utils;

import retrofit2.Call;

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
		ViewGroup frame = findViewById(R.id.main_content);
		ScrollView scrollView = new ScrollView(this);
		this.v = new TextView(this);
		int p = (int) Utils.dp(getResources(), 16);
		v.setPadding(p, p, p, p);
		scrollView.addView(v);
		frame.addView(scrollView, -1, -1);
		lc = new LoadingViewController(this, frame, "");
		lc.content();
	}

	private void refreshUi() {
		//TODO display past season data
//		v.setText(sb.toString());
	}
}
