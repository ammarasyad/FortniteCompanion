package com.tb24.fn.activity;

import android.os.Bundle;
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

public class StwWorldInfoActivity extends BaseActivity {
	public static final Joiner NEWLINE = Joiner.on('\n');
	public static final Function<WorldInfoResponse.WorldInfoItemStack, String> ITEM_STACK_STRING_FUNCTION = new Function<WorldInfoResponse.WorldInfoItemStack, String>() {
		@Override
		public String apply(WorldInfoResponse.WorldInfoItemStack input) {
			return "  " + (input.itemType == null ? "" : input.itemType);
		}
	};
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
		loadStwWorldInfo();
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
								refreshUi();
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

	private void refreshUi() {
		StringBuilder sb = new StringBuilder();

		WorldInfoResponse.TheaterData[] theaters = data.theaters;
		for (int i = 0; i < theaters.length; i++) {
			WorldInfoResponse.TheaterData theaterData = theaters[i];
			sb.append("ZONE: " + theaterData.displayName + '\n');
			sb.append("   >> " + theaterData.description + "\n\n");
			final int finalI = i;
			sb.append(NEWLINE.join(Lists.transform(Arrays.asList(data.missions[i].availableMissions), new Function<WorldInfoResponse.Mission, String>() {
				@Override
				public String apply(WorldInfoResponse.Mission input) {
					StringBuilder text = new StringBuilder("Mission:\n  " + input.missionGenerator + "\n  (" + input.missionGuid + ")\n");

					for (WorldInfoResponse.MissionAlert entry : data.missionAlerts[finalI].availableMissionAlerts) {
						if (entry.tileIndex.equals(input.tileIndex)) {
							if (entry.missionAlertModifiers != null) {
								text.append("Mission Alert Modifiers\n");
								text.append(NEWLINE.join(Lists.transform(Arrays.asList(entry.missionAlertModifiers.items), ITEM_STACK_STRING_FUNCTION)) + '\n');
							}

							text.append("Mission Alert Rewards\n");
							text.append(NEWLINE.join(Lists.transform(Arrays.asList(entry.missionAlertRewards.items), ITEM_STACK_STRING_FUNCTION)) + '\n');
						}
					}

					if (input.missionRewards != null) {
						text.append("Mission Rewards\n");
						text.append(NEWLINE.join(Lists.transform(Arrays.asList(input.missionRewards.items), ITEM_STACK_STRING_FUNCTION)) + '\n');
					}

					if (input.bonusMissionRewards != null) {
						text.append("Bonus Mission Rewards\n");
						text.append(NEWLINE.join(Lists.transform(Arrays.asList(input.missionRewards.items), ITEM_STACK_STRING_FUNCTION)) + '\n');
					}

					return text.toString();
				}
			})) + "\n\n\n");
		}

		v.setText(sb.toString());
	}
}
