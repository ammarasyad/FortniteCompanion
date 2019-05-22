package com.tb24.fn.activity;

import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import com.google.common.base.Predicate;
import com.google.common.collect.HashBasedTable;
import com.google.common.collect.Table;
import com.tb24.fn.R;
import com.tb24.fn.model.EpicError;
import com.tb24.fn.model.FortStatsV2Response;
import com.tb24.fn.model.GameProfile;
import com.tb24.fn.util.LoadingViewController;
import com.tb24.fn.util.Utils;

import java.io.IOException;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import retrofit2.Call;
import retrofit2.Response;

public class BRStatsActivity extends BaseActivity implements View.OnClickListener {
	private static final List<String> DEFAULT_SOLO_PLAYLISTS = Arrays.asList("defaultsolo", "deimos_solo", "deimos_solo_winter");
	private static final List<String> DEFAULT_DUO_PLAYLISTS = Arrays.asList("defaultduo", "deimos_duo", "deimos_duo_winter");
	private static final List<String> DEFAULT_SQUAD_PLAYLISTS = Arrays.asList("defaultsquad", "deimos_squad", "deimos_squad_winter");
	private static final List<StatName> SOLO_DISPLAY = Arrays.asList(StatName.placetop1, StatName.placetop10, StatName.placetop25, StatName.kills, StatName.matchesplayed, StatName.__kd, StatName.__winrate, StatName.__top10rate, StatName.minutesplayed, StatName.playersoutlived, StatName.score);
	private static final List<StatName> DUO_DISPLAY = Arrays.asList(StatName.placetop1, StatName.placetop5, StatName.placetop12, StatName.kills, StatName.matchesplayed, StatName.__kd, StatName.__winrate, StatName.__top5rate, StatName.minutesplayed, StatName.playersoutlived, StatName.score);
	private static final List<StatName> SQUAD_DISPLAY = Arrays.asList(StatName.placetop1, StatName.placetop3, StatName.placetop6, StatName.kills, StatName.matchesplayed, StatName.__kd, StatName.__winrate, StatName.__top3rate, StatName.minutesplayed, StatName.playersoutlived, StatName.score);
	private static final List<StatName> LTM_DISPLAY = Arrays.asList(StatName.placetop1, StatName.score, StatName.playersoutlived, StatName.placetop3, StatName.placetop5, StatName.placetop6, StatName.placetop12, StatName.placetop25, StatName.kills, StatName.matchesplayed, StatName.minutesplayed);
	private Button btn1, btn2;
	private ViewGroup heroGroup, extrasGroup;
	private Input selectedInput = Input.ALL;
	private GameMode selectedGameMode = GameMode.SOLO;
	// br_playersoutlived_touch_m0_playlist_snipers_duos
	// stat name, platform, playlist name, stat value
	private Table<String, String, Integer> table = HashBasedTable.create();
	private boolean loaded;
	private FortStatsV2Response statsData;
	private ArrayList<String> allNormal;
	private Predicate<String> shouldGoIn = new Predicate<String>() {
		@Override
		public boolean apply(String input) {
			if (selectedGameMode == GameMode.LTM) {
				if (allNormal == null) {
					allNormal = new ArrayList<>();
					allNormal.addAll(DEFAULT_SOLO_PLAYLISTS);
					allNormal.addAll(DEFAULT_DUO_PLAYLISTS);
					allNormal.addAll(DEFAULT_SQUAD_PLAYLISTS);
				}

				return !allNormal.contains(input);
			} else {
				return selectedGameMode.playlistsDefinition.contains(input);
			}
		}
	};
	private LoadingViewController lc;

	public static void openStats(Context ctx, GameProfile profile) {
		Intent intent = new Intent(ctx, BRStatsActivity.class);
		intent.putExtra("a", profile.getId());
		intent.putExtra("b", profile.getDisplayName());
		ctx.startActivity(intent);
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.common_loadable_framed);
		String id = getIntent().getStringExtra("a");
		String name = getIntent().getStringExtra("b");
		setupActionBar();
		getActionBar().setTitle(PreferenceManager.getDefaultSharedPreferences(this).getString("epic_account_id", "").equals(id) ? "You" : name == null ? id : name);
		final View frame = findViewById(R.id.main_content);
		getLayoutInflater().inflate(R.layout.br_stat_view, (ViewGroup) frame);
		btn1 = findViewById(R.id.br_stats_btn_platform_cycle);
		btn2 = findViewById(R.id.br_stats_btn_mode_cycle);
		btn1.setOnClickListener(this);
		btn2.setOnClickListener(this);
		btntxt();
		btn2txt();
		heroGroup = findViewById(R.id.br_stats_hero);
		extrasGroup = findViewById(R.id.br_stats_extras);
		lc = new LoadingViewController(this, frame, "There are no stats to display.") {
			@Override
			public boolean shouldShowEmpty() {
				return statsData == null || statsData.stats.isEmpty();
			}
		};
		fetchStatsIdReady(id);
	}

	private void btntxt() {
		if (selectedInput != Input.ALL) {
			Drawable drawable = getDrawable(selectedInput.img);
			int i = (int) Utils.dp(getResources(), 24);
			drawable.setBounds(0, 0, i, i);
			drawable.setTint(btn1.getCurrentTextColor());
			btn1.setCompoundDrawables(null, null, drawable, null);
		} else {
			btn1.setCompoundDrawables(null, null, null, null);
		}

		btn1.setText("Input: " + selectedInput);
	}

	private void btn2txt() {
		btn2.setText("Mode: " + selectedGameMode);
	}

	@Override
	public void onClick(View v) {
		if (v.getId() == R.id.br_stats_btn_platform_cycle) {
			selectedInput = Input.values()[(selectedInput.ordinal() + 1) % Input.values().length];
			btntxt();

			if (loaded) {
				refreshUi();
			}
		} else if (v.getId() == R.id.br_stats_btn_mode_cycle) {
			selectedGameMode = GameMode.values()[(selectedGameMode.ordinal() + 1) % GameMode.values().length];
			btn2txt();

			if (loaded) {
				refreshUi();
			}
		}
	}

	private void fetchStatsIdReady(String id) {
		loaded = false;
		final Call<FortStatsV2Response> call = getThisApplication().fortnitePublicService.statsV2(id);
		lc.loading();
		new Thread(new Runnable() {
			@Override
			public void run() {
				String errorText = "";
				try {
					Response<FortStatsV2Response> response = call.execute();

//					label:
					if (response.isSuccessful()) {
//						if (response.code() == 204) break label;

						statsData = response.body();
						runOnUiThread(new Runnable() {
							@Override
							public void run() {
								refreshUi();
								lc.content();
							}
						});
					} else {
						errorText = EpicError.parse(response).getDisplayText();
					}
				} catch (IOException e) {
					errorText = e.toString();
				}

				final String finalText = errorText;
				runOnUiThread(new Runnable() {
					@Override
					public void run() {
						if (!finalText.isEmpty()) {
							lc.error(finalText);
						}
					}
				});
				loaded = true;
			}
		}).start();
	}

	private void refreshUi() {
		if (statsData == null || statsData.stats.size() == 0) {
			return;
		}

		table.clear();
		final Map<String, Object> statNameAndSums = new HashMap<>();

		// Input summed
		for (Map.Entry<String, Integer> entry : statsData.stats.entrySet()) {
			String[] splitted = entry.getKey().split("_m0_playlist_");
			String[] statAndInput = splitted[0].substring(3).split("_");
			String stat = statAndInput[0];
			String input = statAndInput[1];
			String playlistName = splitted[1];

			if (selectedInput != Input.ALL && selectedInput != Input.valueOf(input.toUpperCase())) {
				continue;
			}

			if (!table.contains(stat, playlistName)) {
				table.put(stat, playlistName, 0);
			}

			int value = entry.getValue();
			table.put(stat, playlistName, table.get(stat, playlistName) + value);

			// Stat name summed TODO move out of this block to be able to view multiple modes
			if (shouldGoIn.apply(playlistName)) {
				if (!statNameAndSums.containsKey(stat)) {
					statNameAndSums.put(stat, 0);
				}

				statNameAndSums.put(stat, (Integer) statNameAndSums.get(stat) + value);
			}
		}

		int wins = !statNameAndSums.containsKey("placetop1") ? 0 : (int) statNameAndSums.get("placetop1");
		int matches = !statNameAndSums.containsKey("matchesplayed") ? 0 : (int) statNameAndSums.get("matchesplayed");
		int kills = !statNameAndSums.containsKey("kills") ? 0 : (int) statNameAndSums.get("kills");
		int deaths = matches - wins;
		statNameAndSums.put("__kd", deaths == 0 ? (kills == 0 ? 0 : "\u221e") : ((float) kills / (float) deaths));
		statNameAndSums.put("__winrate", matches == 0 ? 0 : ((float) wins / (float) matches));
		int n = selectedGameMode.topSecondN;

		if (n > 0) {
			int placeTopSecond = !statNameAndSums.containsKey("placetop" + n) ? 0 : (int) statNameAndSums.get("placetop" + n);
			statNameAndSums.put("__top" + n + "rate", matches == 0 ? 0 : (placeTopSecond == 0 ? 0 : ((float) placeTopSecond / (float) matches)));
		}

		List<StatName> displays = selectedGameMode.displays;
		extrasGroup.removeAllViews();

		for (int i = 0; i < displays.size(); i++) {
			StatName statName = displays.get(i);
			Object value = statNameAndSums.containsKey(statName.asString) ? statNameAndSums.get(statName.asString) : 0;

			if (i < 3) {
				makeItRain((ViewGroup) heroGroup.getChildAt(i), statName.format(BRStatsActivity.this, value), statName, i * 75);
			} else {
				ViewGroup inflate = (ViewGroup) LayoutInflater.from(BRStatsActivity.this).inflate(R.layout.br_stats_extra_entry, extrasGroup, false);
				((TextView) inflate.getChildAt(0)).setText(statName.friendlyName);
				((TextView) inflate.getChildAt(1)).setText(statName.format(BRStatsActivity.this, value));
				extrasGroup.addView(inflate);
				inflate.setScaleY(0);
				inflate.setPivotY(0);
				inflate.animate().setDuration(50L).setStartDelay((i - 3) * 50L).scaleY(1);
				attachMoreStatDetails(inflate, statName);
			}
		}
	}

	private void makeItRain(ViewGroup view, CharSequence value, final StatName statName, long animDelay) {
		TextView text = (TextView) view.getChildAt(0);
		final TextView text1 = (TextView) view.getChildAt(1);
		text.setText(value);
		text1.setText(statName.friendlyName);
		view.setScaleX(1.5F);
		view.setScaleY(1.5F);
		view.setAlpha(0.0F);
		view.animate().setStartDelay(animDelay).setDuration(125L).scaleX(1).scaleY(1).alpha(1);
		attachMoreStatDetails(view, statName);
	}

	private void attachMoreStatDetails(ViewGroup view, final StatName statName) {
		if (statName.custom) {
			return;
		}

		view.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				StringBuilder sb = new StringBuilder();

				for (Map.Entry<String, Integer> e : table.row(statName.asString).entrySet()) {
					if (shouldGoIn.apply(e.getKey())) {
						sb.append(e.getKey() + ": " + statName.format(BRStatsActivity.this, e.getValue()) + '\n');
					}
				}

				new AlertDialog.Builder(BRStatsActivity.this).setTitle(statName.friendlyName).setMessage(sb.toString()).setPositiveButton(android.R.string.ok, null).show();
			}
		});
	}

	public enum Input {
		ALL(-1), KEYBOARDMOUSE(R.drawable.t_ui_icon_input_keyboardmouse_128), GAMEPAD(R.drawable.t_ui_icon_input_controller_128), TOUCH(R.drawable.t_ui_icon_input_touch_128);

		public final int img;

		Input(int img) {
			this.img = img;
		}
	}

	public enum GameMode {
		SOLO(10, 25, DEFAULT_SOLO_PLAYLISTS, SOLO_DISPLAY), DUO(5, 23, DEFAULT_DUO_PLAYLISTS, DUO_DISPLAY), SQUAD(3, 6, DEFAULT_SQUAD_PLAYLISTS, SQUAD_DISPLAY), LTM(-1, -1, null, LTM_DISPLAY);

		public final int topSecondN;
		public final int topThirdN;
		public final List<String> playlistsDefinition;
		public final List<StatName> displays;

		GameMode(int topSecondN, int topThirdN, List<String> playlistsDefinition, List<StatName> displays) {
			this.topSecondN = topSecondN;
			this.topThirdN = topThirdN;
			this.playlistsDefinition = playlistsDefinition;
			this.displays = displays;
		}
	}

	public enum StatName {
		placetop1("placetop1", "Wins"),
		placetop3("placetop3", "Top 3"),
		placetop5("placetop5", "Top 5"),
		placetop6("placetop6", "Top 6"),
		placetop10("placetop10", "Top 10"),
		placetop12("placetop12", "Top 12"),
		placetop25("placetop25", "Top 25"),
		kills("kills", "Eliminations"),
		matchesplayed("matchesplayed", "Matches Played"),
		minutesplayed("minutesplayed", "Time Played"),
		playersoutlived("playersoutlived", "Players Outlasted"),
		score("score", "Score"),
		__kd("__kd", "K/D Ratio", true),
		__winrate("__winrate", "Win %", true),
		__top3rate("__top3rate", "Top 3 %", true),
		__top5rate("__top5rate", "Top 5 %", true),
		__top10rate("__top10rate", "Top 10 %", true);

		private static final NumberFormat PERCENT_INSTANCE;

		static {
			PERCENT_INSTANCE = NumberFormat.getPercentInstance();
			PERCENT_INSTANCE.setMinimumFractionDigits(2);
		}

		public final String asString;
		public final String friendlyName;
		public final boolean custom;

		StatName(String asString, String friendlyName) {
			this(asString, friendlyName, false);
		}

		StatName(String asString, String friendlyName, boolean custom) {
			this.asString = asString;
			this.friendlyName = friendlyName;
			this.custom = custom;
		}

		public CharSequence format(Context ctx, Object input) {
			if (input instanceof String) return input.toString();
			switch (this) {
				case placetop1:
				case placetop3:
				case placetop5:
				case placetop6:
				case placetop10:
				case placetop12:
				case placetop25:
				case kills:
				case matchesplayed:
				case playersoutlived:
				case score:
					return String.format("%,d", (Integer) input);
				case __kd:
					if (input == Integer.valueOf(0)) input = 0.0F;
					return String.format("%.2f", (float) input);
				case __winrate:
				case __top3rate:
				case __top5rate:
				case __top10rate:
					if (input == Integer.valueOf(0)) input = 0.0F;
//					return String.format("%.1f %%", (float) input * 100.0F);
					return PERCENT_INSTANCE.format(input);
				case minutesplayed:
					return Utils.formatElapsedTime(ctx, TimeUnit.MINUTES.toMillis(((Integer) input).longValue()), false);
				default:
					return input.toString();
			}
		}
	}
}
