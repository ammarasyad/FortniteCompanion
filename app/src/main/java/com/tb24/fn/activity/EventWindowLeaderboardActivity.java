package com.tb24.fn.activity;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.LayerDrawable;
import android.graphics.drawable.RippleDrawable;
import android.graphics.drawable.ShapeDrawable;
import android.graphics.drawable.StateListDrawable;
import android.graphics.drawable.shapes.RectShape;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.DisplayMetrics;
import android.util.Log;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.Px;
import androidx.appcompat.app.AlertDialog;
import androidx.core.widget.TextViewCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.common.base.Function;
import com.google.common.base.Joiner;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.tb24.fn.FortniteCompanionApp;
import com.tb24.fn.R;
import com.tb24.fn.model.EpicError;
import com.tb24.fn.model.GameProfile;
import com.tb24.fn.model.LeaderboardsResponse;
import com.tb24.fn.util.BaseAdapter;
import com.tb24.fn.util.LoadingViewController;
import com.tb24.fn.util.ProfileLookupCallback;
import com.tb24.fn.util.ProfileNotFoundException;
import com.tb24.fn.util.Utils;

import java.io.IOException;
import java.text.NumberFormat;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import retrofit2.Call;
import retrofit2.Response;

public class EventWindowLeaderboardActivity extends BaseActivity implements BaseAdapter.OnReloadClickListener {
	public static final String ARG_EVENT_ID = "a";
	public static final String ARG_WINDOW_ID = "b";
	public static final String ARG_TITLE = "c";
	public static final String ARG_SUBTITLE = "d";
	public static final String ARG_COLOR = "e";
	public static final int PAGE_SIZE = 100;
	public static final Function<GameProfile, String> GAME_PROFILE_TO_DISPLAY_NAME = new Function<GameProfile, String>() {
		@Override
		public String apply(GameProfile input) {
			return input.getDisplayName() == null ? "[UNKNOWN]" : input.getDisplayName();
		}
	};
	public static final Joiner AMP_JOINER = Joiner.on(" & ");
	private static final String TAG = EventWindowLeaderboardActivity.class.getSimpleName();
	private static final int ENTRIES_PER_PAGE = 100;
	private static final int MAX_FAIL_COUNT = 3;
	private static final int DELAY_BETWEEN_PAGES = 100;
	private static final int DELAY_BETWEEN_FAILURES = 750;
	private static final NumberFormat PERCENT_INSTANCE;

	static {
		PERCENT_INSTANCE = NumberFormat.getPercentInstance();
		PERCENT_INSTANCE.setMinimumFractionDigits(2);
	}

	private int page = -1, totalPages;
	private boolean loading;
	private boolean stopLoading;
	private LeaderboardAdapter adapter;
	private LoadingViewController lc;
	private int color;
	private boolean sideBySide;
	private ViewGroup rightSide;
	private RecyclerView list;

	private static void sumKV(Map<String, Integer> map, Map.Entry<String, Integer> entry) {
		if (map.containsKey(entry.getKey())) {
			map.put(entry.getKey(), map.get(entry.getKey()) + entry.getValue());
		} else {
			map.put(entry.getKey(), entry.getValue());
		}
	}

	@Override
	protected void onCreate(@Nullable Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_event_leaderboard);
		setupActionBar();
		Intent intent = getIntent();
		setTitle(intent.getStringExtra(ARG_TITLE));
		getSupportActionBar().setSubtitle(intent.getStringExtra(ARG_SUBTITLE));
		color = (int) Long.parseLong(intent.getStringExtra(ARG_COLOR), 16);
		DisplayMetrics displayMetrics = getResources().getDisplayMetrics();
		sideBySide = (displayMetrics.widthPixels / displayMetrics.density) >= FortniteCompanionApp.MIN_DP_FOR_TWOCOLUMN;
		rightSide = findViewById(R.id.lb_right_side);
		rightSide.setVisibility(sideBySide ? View.VISIBLE : View.GONE);
		list = findViewById(R.id.main_recycler_view);
		final LinearLayoutManager layoutManager = new LinearLayoutManager(this);
		list.setLayoutManager(layoutManager);
		list.setAdapter(adapter = new LeaderboardAdapter());
		adapter.setOnReloadClickListener(this);
		adapter.addFooter();
		fetchMoreEntriesAsync();
		list.addOnScrollListener(new RecyclerView.OnScrollListener() {
			@Override
			public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
				super.onScrolled(recyclerView, dx, dy);
				int visibleItemCount = layoutManager.getChildCount();
				int totalItemCount = layoutManager.getItemCount();
				int firstVisibleItemPosition = layoutManager.findFirstVisibleItemPosition();

				if (!isLastPage()) {
					if (visibleItemCount + firstVisibleItemPosition >= totalItemCount && firstVisibleItemPosition >= 0 && totalItemCount >= PAGE_SIZE) {
						fetchMoreEntriesAsync();
					}
				}
			}
		});
		lc = new LoadingViewController(this, list, "No Leaderboard Entries Available\nCheck Back Later") {
			@Override
			public boolean shouldShowEmpty() {
				return stopLoading && adapter.isEmpty();
			}
		};
		lc.content();
	}

	private boolean isLastPage() {
		return page >= totalPages - 1 && page >= 0 && totalPages > 0;
	}

	private void fetchMoreEntriesAsync() {
		if (!loading && !stopLoading) {
			loading = true;
			adapter.updateFooter(BaseAdapter.FooterType.LOAD_MORE);
			new Thread("leaderboard-fetcher") {
				@Override
				public void run() {
					String epicAccountId = PreferenceManager.getDefaultSharedPreferences(EventWindowLeaderboardActivity.this).getString("epic_account_id", null);
					Intent intent = getIntent();
					Call<LeaderboardsResponse> call = getThisApplication().eventsPublicServiceLive.leaderboards(intent.getStringExtra(ARG_EVENT_ID), intent.getStringExtra(ARG_WINDOW_ID), epicAccountId, page + 1, 0, "", "Fortnite", true);

					try {
						final Response<LeaderboardsResponse> response = call.execute();

						if (response.isSuccessful()) {
							final LeaderboardsResponse body = response.body();
							page = body.page;
							totalPages = body.totalPages;
							Set<String> criteriaIds = new HashSet<>();

							for (LeaderboardsResponse.LeaderboardEntry entry : body.entries) {
								criteriaIds.addAll(Arrays.asList(entry.teamAccountIds));
							}

							final Map<String, GameProfile> map = new HashMap<>();
							fetchNames(criteriaIds, new ProfileLookupCallback() {
								@Override
								public void onProfileLookupSucceeded(GameProfile profile) {
									map.put(profile.getId(), profile);
								}

								@Override
								public void onProfileLookupFailed(GameProfile profile, Exception exception) {
									Log.e(TAG, "Profile " + profile.getId() + " failed to find name", exception);
								}
							});

							for (LeaderboardsResponse.LeaderboardEntry entry : body.entries) {
								if (entry.modifiedTeamAccountIds == null) {
									entry.modifiedTeamAccountIds = Lists.transform(Arrays.asList(entry.teamAccountIds), new Function<String, GameProfile>() {
										@Override
										public GameProfile apply(String input) {
											if (!map.containsKey(input)) {
												return new GameProfile(input, null);
											}

											return map.get(input);
										}
									});
								}
							}

							runOnUiThread(new Runnable() {
								@Override
								public void run() {
									adapter.removeFooter();
									adapter.addAll(body.entries);

									if (isLastPage()) {
										stopLoading = true;
									} else {
										adapter.addFooter();
									}

									lc.content();
								}
							});
						} else {
							EpicError.parse(response);
							stopLoading = true;
							setFooterToErrorFromNonMain();
						}
					} catch (IOException var5) {
						Log.e(TAG, "Couldn\'t fetch leaderboard entries of page " + (page + 1), var5);
						stopLoading = true;
						setFooterToErrorFromNonMain();
					} finally {
						loading = false;
					}
				}
			}.start();
		}
	}

	private void fetchNames(Set<String> criteriaIds, ProfileLookupCallback callback) {
		for (final List<String> request : Iterables.partition(criteriaIds, ENTRIES_PER_PAGE)) {
			int failCount = 0;
			boolean failed;

			do {
				failed = false;

				try {
					final Response<GameProfile[]> response = getThisApplication().accountPublicService.accountMultiple(request).execute();

					if (response.isSuccessful()) {
						failCount = 0;
						GameProfile[] body = response.body();
						final Set<String> missing = Sets.newHashSet(request);

						for (final GameProfile profile : body) {
//							Log.d(TAG, "Successfully looked up profile " + profile);
							missing.remove(profile.getId().toLowerCase());
							callback.onProfileLookupSucceeded(profile);
						}

						for (final String id : missing) {
							Log.d(TAG, "Couldn't find profile " + id);
							callback.onProfileLookupFailed(new GameProfile(id, null), new ProfileNotFoundException("Server did not find the requested profile"));
						}

						try {
							Thread.sleep(DELAY_BETWEEN_PAGES);
						} catch (final InterruptedException ignored) {
						}
					} else {
						EpicError.parse(response);
					}
				} catch (final IOException e) {
					failCount++;

					if (failCount == MAX_FAIL_COUNT) {
						for (final String id : request) {
//							Log.d(TAG, "Couldn't find profile " + id + " because of a server error");
							callback.onProfileLookupFailed(new GameProfile(null, id), e);
						}
					} else {
						try {
							Thread.sleep(DELAY_BETWEEN_FAILURES);
						} catch (final InterruptedException ignored) {
						}
						failed = true;
					}
				}
			} while (failed);
		}
	}

	private void setFooterToErrorFromNonMain() {
		runOnUiThread(new Runnable() {
			@Override
			public void run() {
				adapter.updateFooter(BaseAdapter.FooterType.ERROR);
			}
		});
	}

	@Override
	public void onReloadClick() {
		stopLoading = false;
		fetchMoreEntriesAsync();
	}

	@NonNull
	private View leaderboardEntryView(Context ctx, LeaderboardsResponse.LeaderboardEntry entry) {
		View view = LayoutInflater.from(ctx).inflate(R.layout.template_table, null);
		TableLayout table = view.findViewById(R.id.lb_s_table);
		Map<String, Integer> map = new HashMap<>();
		int matchesPlayed = entry.sessionHistory.length, wins = 0, placementPoints = 0, elimPoints = 0;

		// TODO this algorithm isn't correct
		for (LeaderboardsResponse.SessionHistory session : entry.sessionHistory) {
			int placementStatIndex = session.trackedStats.get("PLACEMENT_STAT_INDEX");
			int teamElimsStatIndex = session.trackedStats.get("TEAM_ELIMS_STAT_INDEX");

			for (Map.Entry<String, LeaderboardsResponse.PointBreakdown> pointBreakdownEntry : entry.pointBreakdown.entrySet()) {
				if (pointBreakdownEntry.getKey().equals("PLACEMENT_STAT_INDEX:" + placementStatIndex)) {
					placementPoints += pointBreakdownEntry.getValue().pointsEarned;
				} else {
					if (pointBreakdownEntry.getKey().equals("TEAM_ELIMS_STAT_INDEX:" + teamElimsStatIndex)) {
						elimPoints += pointBreakdownEntry.getValue().pointsEarned;
					}
				}
			}

			for (Map.Entry<String, Integer> entry1 : session.trackedStats.entrySet()) {
				sumKV(map, entry1);
			}

			wins += placementStatIndex == 1 ? 1 : 0;
		}

		addRow("Matches Played", String.format("%,d", matchesPlayed), table);
		addRow("Victory Royales", String.format("%,d", wins), table);
		addRow("Avg. Elims", String.format("%.1f", (float) map.get("TEAM_ELIMS_STAT_INDEX") / matchesPlayed), table);
		addRow("Avg. Placement", String.format("%.1f", (float) map.get("PLACEMENT_STAT_INDEX") / matchesPlayed), table);
		addRow("Avg. PTS", String.format("%.1f", (float) entry.pointsEarned / matchesPlayed), table);
		addRow("Placement Point %", PERCENT_INSTANCE.format((double) placementPoints / entry.pointsEarned), table);
		addRow("Elims PTS %", PERCENT_INSTANCE.format((double) elimPoints / entry.pointsEarned), table);
		return view;
	}

	private void addRow(String k, String v, TableLayout table) {
		TableRow row = new TableRow(this);
		row.setGravity(Gravity.CENTER_VERTICAL);
		TextView one = new TextView(this), two = new TextView(this);
		TextViewCompat.setTextAppearance(one, R.style.BurbankSmall_Bold);
		TextViewCompat.setTextAppearance(two, R.style.BurbankSmall_Black);
		two.setGravity(Gravity.CENTER_HORIZONTAL);
		two.setTextSize(TypedValue.COMPLEX_UNIT_SP, 24);
		one.setText(k);
		two.setText(v);
		row.addView(one);
		row.addView(two);
		table.addView(row);
	}

	private AlertDialog showPlayerProfilesDialog(final Context ctx, final LeaderboardsResponse.LeaderboardEntry entry) {
		return new AlertDialog.Builder(ctx)
				.setTitle("Player Profiles")
				.setItems(Iterables.toArray(Iterables.transform(entry.modifiedTeamAccountIds, GAME_PROFILE_TO_DISPLAY_NAME), String.class), new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
						BRStatsActivity.openStats(ctx, entry.modifiedTeamAccountIds.get(which));
					}
				})
				.setNegativeButton(android.R.string.cancel, null)
				.show();
	}

	private AlertDialog showMatchHistoryDialog(final Context ctx, final LeaderboardsResponse.LeaderboardEntry entry) {
		View view = LayoutInflater.from(ctx).inflate(R.layout.dialog_match_history, null);
		((TextView) view.findViewById(R.id.lb_mh_event)).setText(getIntent().getStringExtra(ARG_SUBTITLE));
		((TextView) view.findViewById(R.id.lb_mh_team)).setText(AMP_JOINER.join(Iterables.transform(entry.modifiedTeamAccountIds, GAME_PROFILE_TO_DISPLAY_NAME)));
		TableLayout table = view.findViewById(R.id.lb_s_table);
		int size = (int) Utils.dp(getResources(), 24);
		Drawable drawableElim = getDrawable(R.drawable.t_icon_broadcasteliminations);
		drawableElim.setBounds(0, 0, size, size);
		Drawable drawableTrophy = getDrawable(R.drawable.t_icon_trophy_64);
		drawableTrophy.setBounds(0, 0, size, size);
		LeaderboardsResponse.SessionHistory[] sessionHistory = entry.sessionHistory;

		for (int i = sessionHistory.length - 1; i >= 0; i--) {
			LeaderboardsResponse.SessionHistory session = sessionHistory[i];
			View row = LayoutInflater.from(ctx).inflate(R.layout.event_leaderboard_match_history_entry, null);
			((TextView) row.findViewById(R.id.lb_mh_n)).setText("#" + (i + 1));
			((TextView) row.findViewById(R.id.lb_mh_d)).setText(String.format("%s Minutes", session.trackedStats.containsKey("TIME_ALIVE_STAT") ? TimeUnit.SECONDS.toMinutes(session.trackedStats.get("TIME_ALIVE_STAT")) : "??"));
			TextView tElims = row.findViewById(R.id.lb_mh_e);
			tElims.setCompoundDrawables(drawableElim, null, null, null);
			tElims.setText(String.valueOf(session.trackedStats.get("TEAM_ELIMS_STAT_INDEX")));
			TextView tPoints = row.findViewById(R.id.lb_mh_p);
			tPoints.setCompoundDrawables(drawableTrophy, null, null, null);
			int points = 0;

			for (Map.Entry<String, LeaderboardsResponse.PointBreakdown> pointBreakdownEntry : entry.pointBreakdown.entrySet()) {
				if (pointBreakdownEntry.getKey().equals("PLACEMENT_STAT_INDEX:" + session.trackedStats.get("PLACEMENT_STAT_INDEX"))
						|| pointBreakdownEntry.getKey().equals("TEAM_ELIMS_STAT_INDEX:" + session.trackedStats.get("TEAM_ELIMS_STAT_INDEX"))) {
					points += pointBreakdownEntry.getValue().pointsEarned;
				}
			}

			tPoints.setText(String.valueOf(points));
			table.addView(row);
		}

		int padd = (int) Utils.dp(getResources(), 24);
		view.setPadding(padd, padd, padd, padd);
		return new AlertDialog.Builder(ctx)
				.setTitle("Match History")
				.setView(view)
				.setNegativeButton("Close", null)
				.show();
	}

	private void updateRightSide(View view) {
		rightSide.removeAllViews();
		rightSide.addView(view, -1, -1);
	}

	private static class LeaderboardEntryViewHolder extends RecyclerView.ViewHolder {
		TextView place, title, points;

		LeaderboardEntryViewHolder(View itemView) {
			super(itemView);
			place = itemView.findViewById(R.id.lb_place);
			title = itemView.findViewById(R.id.lb_title);
			points = itemView.findViewById(R.id.lb_points);
		}
	}

	private static class FooterViewHolder extends RecyclerView.ViewHolder {
		ViewGroup loadingFrameLayout;
		ViewGroup errorRelativeLayout;
		ProgressBar loadingImageView;
		TextView errorText;
		Button reloadButton;

		FooterViewHolder(View view) {
			super(view);
			loadingFrameLayout = view.findViewById(R.id.loading_fl);
			errorRelativeLayout = view.findViewById(R.id.error_rl);
			loadingImageView = view.findViewById(R.id.loading_iv);
			errorText = view.findViewById(R.id.error_tv);
			reloadButton = view.findViewById(R.id.reload_btn);
		}
	}

	private class LeaderboardAdapter extends BaseAdapter<LeaderboardsResponse.LeaderboardEntry> {
		private final int colorControlHighlight;
		private final ShapeDrawable mask;
		private FooterViewHolder footerViewHolder;
		private int selectedItem = 0;

		LeaderboardAdapter() {
			TypedValue typedValue = new TypedValue();
			TypedArray typedArray = getTheme().obtainStyledAttributes(typedValue.data, new int[]{android.R.attr.colorControlHighlight});
			colorControlHighlight = typedArray.getColor(0, 0xFFFF00FF);
			typedArray.recycle();
			mask = new ShapeDrawable(new ParallelogramShape((int) Utils.dp(getResources(), 12)));
			mask.getPaint().setColor(0xFFFFFFFF);
		}

		@Override
		public void onAttachedToRecyclerView(@NonNull final RecyclerView recyclerView) {
			super.onAttachedToRecyclerView(recyclerView);

			// Handle key up and key down and attempt to move selection
			recyclerView.setOnKeyListener(new View.OnKeyListener() {
				@Override
				public boolean onKey(View v, int keyCode, KeyEvent event) {
					if (!sideBySide) {
						return false;
					}

					RecyclerView.LayoutManager lm = recyclerView.getLayoutManager();

					// Return false if scrolled to the bounds and allow focus to move off the list
					if (event.getAction() == KeyEvent.ACTION_DOWN) {
						if (keyCode == KeyEvent.KEYCODE_DPAD_DOWN) {
							return tryMoveSelection(lm, 1);
						} else if (keyCode == KeyEvent.KEYCODE_DPAD_UP) {
							return tryMoveSelection(lm, -1);
						}
					}

					return false;
				}
			});
		}

		private boolean tryMoveSelection(RecyclerView.LayoutManager lm, int direction) {
			int nextSelectItem = selectedItem + direction;

			if (nextSelectItem >= 0 && nextSelectItem < getItemCount()) {
				notifyItemChanged(selectedItem);
				selectedItem = nextSelectItem;
				notifyItemChanged(selectedItem);
				lm.scrollToPosition(selectedItem);
				return true;
			}

			return false;
		}

		@Override
		public int getItemViewType(int position) {
			return isLastPosition(position) && isFooterAdded ? FOOTER : ITEM;
		}

		@Override
		public void bindItemViewHolder(final RecyclerView.ViewHolder holder, final int position) {
			final Context ctx = holder.itemView.getContext();
			final LeaderboardEntryViewHolder holder1 = (LeaderboardEntryViewHolder) holder;
			final LeaderboardsResponse.LeaderboardEntry entry = getItem(holder.getAdapterPosition());

			if (sideBySide) {
				holder1.itemView.setSelected(position == selectedItem);
			}

			holder1.itemView.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					View view = leaderboardEntryView(ctx, entry);
					int padd = (int) Utils.dp(getResources(), 24);

					if (sideBySide) {
						if (selectedItem != position) {
							view.setPadding(padd, padd, padd, padd);
							notifyItemChanged(selectedItem);
							selectedItem = position;
							notifyItemChanged(selectedItem);
							updateRightSide(view);
						}
					} else {
						view.setPadding(padd, padd, padd, 0);
						DialogInterface.OnClickListener listener = new DialogInterface.OnClickListener() {
							@Override
							public void onClick(final DialogInterface dialog, int which) {
								DialogInterface.OnDismissListener dismissListener = new DialogInterface.OnDismissListener() {
									@Override
									public void onDismiss(DialogInterface dialog1) {
										((AlertDialog) dialog).show();
									}
								};

								if (which == DialogInterface.BUTTON_NEGATIVE) {
									showPlayerProfilesDialog(ctx, entry).setOnDismissListener(dismissListener);
								} else if (which == DialogInterface.BUTTON_NEUTRAL) {
									showMatchHistoryDialog(ctx, entry).setOnDismissListener(dismissListener);
								}
							}
						};
						new AlertDialog.Builder(ctx)
								.setTitle(entry.teamAccountIds.length == 1 ? "Player Stats" : entry.teamAccountIds.length == 2 ? "Duo Stats" : entry.teamAccountIds.length == 3 ? "Trio Stats" : "Squad Stats")
								.setView(view)
								.setNeutralButton("Match Stats", listener)
								.setNegativeButton("Profiles", listener)
								.setPositiveButton("Close", listener)
								.show();
					}
				}

			});
//			holder1.itemView.setOnLongClickListener(new View.OnLongClickListener() {
//				@Override
//				public boolean onLongClick(View view) {
//					showPlayerProfilesDialog(ctx, entry);
//					return true;
//				}
//			});

			if (position % 2 == 0) {
				//odd
				holder1.itemView.setBackgroundColor(0);
			} else {
				holder1.itemView.setBackgroundColor(0x08 << 24 | color);
			}

			if (entry.liveSessionId != null) {
				//TODO live indicator
			}

			holder1.place.setText("#" + entry.rank);
			holder1.title.setText(AMP_JOINER.join(Iterables.transform(entry.modifiedTeamAccountIds, GAME_PROFILE_TO_DISPLAY_NAME)));
			holder1.points.setText(String.valueOf(entry.pointsEarned));
		}

		@Override
		protected void bindFooterViewHolder(RecyclerView.ViewHolder viewHolder) {
			footerViewHolder = (FooterViewHolder) viewHolder;
		}

		@Override
		protected void displayLoadMoreFooter() {
			if (footerViewHolder != null) {
				footerViewHolder.errorRelativeLayout.setVisibility(View.GONE);
				footerViewHolder.loadingFrameLayout.setVisibility(View.VISIBLE);
			}
		}

		@Override
		protected void displayErrorFooter() {
			if (footerViewHolder != null) {
				footerViewHolder.loadingFrameLayout.setVisibility(View.GONE);
				footerViewHolder.errorRelativeLayout.setVisibility(View.VISIBLE);
			}
		}

		@Override
		public void addFooter() {
			if (isFooterAdded) {
				return;
			}

			isFooterAdded = true;
			add(null);
		}

		@Override
		protected RecyclerView.ViewHolder createHeaderViewHolder(ViewGroup parent) {
			return null;
		}

		@Override
		protected RecyclerView.ViewHolder createItemViewHolder(ViewGroup parent) {
			View inflate = LayoutInflater.from(EventWindowLeaderboardActivity.this).inflate(R.layout.event_leaderboard_entry, parent, false);
			StateListDrawable sld = new StateListDrawable();
//			sld.setEnterFadeDuration(0);
//			sld.setExitFadeDuration(0);
			ShapeDrawable shapeDrawable = new ShapeDrawable(new ParallelogramShape((int) Utils.dp(getResources(), 12)));
			shapeDrawable.getPaint().setColor(colorControlHighlight);
			sld.addState(new int[]{android.R.attr.state_selected}, shapeDrawable);
			inflate.findViewById(R.id.lb_bg).setBackground(new LayerDrawable(new Drawable[]{sld, new RippleDrawable(ColorStateList.valueOf(colorControlHighlight), null, mask)}));
			return new LeaderboardEntryViewHolder(inflate);
		}

		@Override
		protected RecyclerView.ViewHolder createFooterViewHolder(ViewGroup parent) {
			View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.adapter_footer, parent, false);

			final FooterViewHolder holder = new FooterViewHolder(v);
			holder.errorText.setText("Network error");
			holder.reloadButton.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					if (onReloadClickListener != null) {
						onReloadClickListener.onReloadClick();
					}
				}
			});

			return holder;
		}

		@Override
		protected void bindHeaderViewHolder(RecyclerView.ViewHolder viewHolder) {
		}
	}

//	private class HeaderVH extends RecyclerView.ViewHolder {
//		public HeaderVH(View itemView) {
//			super(itemView);
//		}
//	}

	public static class ParallelogramShape extends RectShape {
		private final int indent;
		private final Path path = new Path();

		public ParallelogramShape(@Px int indent) {
			this.indent = indent;
		}

		@Override
		public void draw(Canvas canvas, Paint paint) {
			path.reset();
			path.moveTo(getWidth(), 0.0F);
			path.lineTo(indent, 0.0F);
			path.lineTo(0.0F, getHeight());
			path.lineTo(getWidth() - indent, getHeight());
			path.close();
			canvas.drawPath(path, paint);
		}
	}
}
