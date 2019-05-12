package com.tb24.fn.activity;

import android.os.Bundle;
import android.text.util.Linkify;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import com.squareup.picasso.Picasso;
import com.tb24.fn.R;
import com.tb24.fn.model.FortBasicDataResponse;
import com.tb24.fn.util.LoadingViewController;

import java.util.List;

public class NewsActivity extends WithBasicDataActivity {
	private LoadingViewController lc;
	private LinearLayout mainLayout;
	private Picasso picasso;
	private int mode;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.common_loadable_framed);
		setupActionBar();
		mainLayout = new LinearLayout(this);
		mainLayout.setOrientation(LinearLayout.VERTICAL);
		ScrollView scrollView = new ScrollView(this);
		scrollView.addView(mainLayout, ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
		((ViewGroup) findViewById(R.id.main_content)).addView(scrollView);
		lc = new LoadingViewController(this, findViewById(R.id.main_content), "");
		picasso = Picasso.get();
		mode = getIntent().getIntExtra("a", 0);
		setTitle(mode == 0 ? "Save the World News" : "Battle Royale News");
		load(lc);
	}

	@Override
	protected void display(FortBasicDataResponse data) {
		lc.content();
		FortBasicDataResponse.NewsRoot newsType = mode == 0 ? data.savetheworldnews : data.battleroyalenews;
		List<FortBasicDataResponse.CommonUISimpleMessageBase> news = newsType.news.messages;

		for (FortBasicDataResponse.CommonUISimpleMessageBase entry : news) {
			View view = LayoutInflater.from(this).inflate(R.layout.news_entry, mainLayout, false);
			TextView adspace = view.findViewById(R.id.news_entry_adspace);
			adspace.setVisibility(entry.adspace == null || entry.adspace.isEmpty() ? View.GONE : View.VISIBLE);
			adspace.setText(entry.adspace);
			ImageView img = view.findViewById(R.id.news_entry_image);
			picasso.load(entry.image).into(img);
			((TextView) view.findViewById(R.id.news_entry_title)).setText(entry.title);
			TextView bodyText = view.findViewById(R.id.news_entry_body);
			bodyText.setText(entry.body);
			Linkify.addLinks(bodyText, Linkify.WEB_URLS);
			mainLayout.addView(view);
		}
	}
}
