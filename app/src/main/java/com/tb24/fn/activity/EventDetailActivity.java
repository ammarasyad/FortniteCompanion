package com.tb24.fn.activity;

import android.content.Intent;
import android.os.Bundle;
import android.text.SpannableStringBuilder;
import android.text.TextUtils;
import android.text.format.DateFormat;
import android.text.format.DateUtils;
import android.text.style.StrikethroughSpan;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.google.gson.JsonObject;
import com.tb24.fn.R;
import com.tb24.fn.model.EventDownloadResponse;
import com.tb24.fn.model.FortBasicDataResponse;
import com.tb24.fn.util.Utils;

import java.util.Date;
import java.util.concurrent.TimeUnit;

public class EventDetailActivity extends BaseActivity {
	private EventDownloadResponse.Event thisEventData;
	private FortBasicDataResponse.TournamentDisplayInfo displayInfo;
	private java.text.DateFormat dateFormat;
	private java.text.DateFormat timeFormat;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		Intent intent = getIntent();

		if (!(intent.hasExtra("a") && intent.hasExtra("b")) || getThisApplication().eventData == null) {
			finish();
			return;
		}

		setContentView(R.layout.activity_event_detail);
		setupActionBar();
		dateFormat = DateFormat.getDateFormat(this);
		timeFormat = DateFormat.getTimeFormat(this);
		thisEventData = getThisApplication().gson.fromJson(intent.getStringExtra("a"), EventDownloadResponse.Event.class);
		displayInfo = getThisApplication().gson.fromJson(intent.getStringExtra("b"), FortBasicDataResponse.TournamentDisplayInfo.class);
		setTitle(displayInfo.long_format_title);
		boolean started = new Date().after(thisEventData.beginTime);
		boolean ended = new Date().after(thisEventData.endTime);
		((TextView) findViewById(R.id.event_is_started)).setText(!started && !ended ? String.format("This event starts %s!", DateUtils.isToday(thisEventData.beginTime.getTime()) ? "today" : "in " + TimeUnit.MILLISECONDS.toDays(thisEventData.beginTime.getTime() - System.currentTimeMillis()) + " days") : started && !ended ? TextUtils.concat("This event is ", Utils.color("live now!", Utils.getTextColorPrimary(this))) : started && ended ? "This event has ended." : "???");
		((TextView) findViewById(R.id.event_details_description)).setText(displayInfo.details_description);
		((TextView) findViewById(R.id.event_flavor_description)).setText(displayInfo.flavor_description);
		SpannableStringBuilder ssb = new SpannableStringBuilder("");
		EventDownloadResponse.EventWindow[] eventWindows = thisEventData.eventWindows;

		for (int i = 0; i < eventWindows.length; i++) {
			EventDownloadResponse.EventWindow input = eventWindows[i];
			CharSequence cs;

			if (Utils.isSameDay(input.beginTime, input.endTime)) {
				cs = dateFormat.format(input.beginTime) + ' ' + timeFormat.format(input.beginTime) + " - " + timeFormat.format(input.endTime);
			} else {
				cs = Utils.formatDateSimple(input.beginTime) + " - " + Utils.formatDateSimple(input.endTime);
			}

			ssb.append(cs, new Date().after(input.endTime) ? new StrikethroughSpan() : null, 0).append(i == eventWindows.length - 1 ? "" : "\n");
		}

		((TextView) findViewById(R.id.event_dates)).setText(ssb);
		((TextView) findViewById(R.id.event_short_format_title)).setText(displayInfo.short_format_title);
		ViewGroup session = findViewById(R.id.event_sessions);

		for (final EventDownloadResponse.EventWindow window : thisEventData.eventWindows) {
			Button btn = new Button(this);
			btn.setGravity(Gravity.END);
			final boolean windowStarted = new Date().after(window.beginTime);
			final boolean windowEnded = new Date().after(window.endTime);
			btn.setText(TextUtils.concat(windowEnded ? Utils.color("Ended" + '\n', 0xFFFF0000) : "", dateFormat.format(window.endTime) + '\n', "Session " + window.round));
			btn.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					if (window.leaderboardId == null || !windowStarted) {
						Toast.makeText(EventDetailActivity.this, "Session does not have a leaderboard or the session hasn't started", Toast.LENGTH_SHORT).show();
						return;
					}

					Intent intent = new Intent(EventDetailActivity.this, EventWindowLeaderboardActivity.class);
					intent.putExtra(EventWindowLeaderboardActivity.ARG_EVENT_ID, thisEventData.eventId);
					intent.putExtra(EventWindowLeaderboardActivity.ARG_WINDOW_ID, window.eventWindowId);
					intent.putExtra(EventWindowLeaderboardActivity.ARG_COLOR, displayInfo.secondary_color);
					intent.putExtra(EventWindowLeaderboardActivity.ARG_TITLE, displayInfo.title_line_1 + " " + displayInfo.title_line_2);
					String metadataRoundType = null;

					if (window.metadata instanceof JsonObject) {
						JsonObject asJsonObject = window.metadata.getAsJsonObject();

						if (asJsonObject.has("RoundType")) {
							metadataRoundType = asJsonObject.get("RoundType").getAsString();
						}
					}

					// TODO round is kinda not true
					intent.putExtra(EventWindowLeaderboardActivity.ARG_SUBTITLE, (metadataRoundType == null ? "" : metadataRoundType + " - ") + "Session " + window.round + " - " + getThisApplication().eventDataRegion.name);
					startActivity(intent);
				}
			});
			session.addView(btn, -1, -2);
		}
	}
}
