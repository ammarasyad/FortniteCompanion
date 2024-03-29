package com.tb24.fn.activity;

import android.content.Intent;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.common.base.Predicate;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.ImmutableList;
import com.google.gson.JsonObject;
import com.tb24.fn.R;
import com.tb24.fn.event.ProfileUpdatedEvent;
import com.tb24.fn.model.EpicError;
import com.tb24.fn.model.FortItemStack;
import com.tb24.fn.model.FortMcpProfile;
import com.tb24.fn.model.FortMcpResponse;
import com.tb24.fn.model.assetdata.FortChallengeBundleItemDefinition;
import com.tb24.fn.util.JsonUtils;
import com.tb24.fn.util.LoadingViewController;
import com.tb24.fn.util.Utils;

import org.checkerframework.checker.nullness.compatqual.NullableDecl;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.io.IOException;
import java.text.NumberFormat;
import java.util.Comparator;
import java.util.List;

import retrofit2.Call;
import retrofit2.Response;

public class ChallengesActivity extends BaseActivity {
	private RecyclerView list;
	private ChallengeBundleAdapter adapter;
	private LoadingViewController lc;
	private FortMcpProfile profileData;

	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.common_loadable_recycler_view);
		setupActionBar();
		list = findViewById(R.id.main_recycler_view);
		list.setLayoutManager(new LinearLayoutManager(ChallengesActivity.this));
		lc = new LoadingViewController(ChallengesActivity.this, list, "");
		refreshUi();
		getThisApplication().eventBus.register(this);
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
		getThisApplication().eventBus.unregister(this);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		menu.add(0, 500, 0, "Refresh challenges");
		return super.onCreateOptionsMenu(menu);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		if (item.getItemId() == 500) {
			final Call<FortMcpResponse> call = getThisApplication().fortnitePublicService.mcp(
					"ClientQuestLogin",
					PreferenceManager.getDefaultSharedPreferences(this).getString("epic_account_id", ""),
					"athena",
					getThisApplication().profileManager.getRvn("athena"),
					new JsonObject());
			new Thread("Client Quest Login Worker") {
				@Override
				public void run() {
					try {
						Response<FortMcpResponse> response = call.execute();

						if (response.isSuccessful()) {
							getThisApplication().profileManager.executeProfileChanges(response.body());
						} else {
							Utils.dialogError(ChallengesActivity.this, EpicError.parse(response).getDisplayText());
						}
					} catch (IOException e) {
						Utils.throwableDialog(ChallengesActivity.this, e);
					}
				}
			}.start();
		}

		return super.onOptionsItemSelected(item);
	}

	@Subscribe(threadMode = ThreadMode.MAIN)
	public void onProfileUpdated(ProfileUpdatedEvent event) {
		if (event.profileId.equals("athena")) {
			refreshUi();
		}
	}

	public void refreshUi() {
		profileData = getThisApplication().profileManager.getProfileData("athena");

		if (profileData == null) {
			lc.loading();
			return;
		} else {
			lc.content();
		}

		ImmutableList<FortItemStack> data = FluentIterable.from(profileData.items.values()).filter(new Predicate<FortItemStack>() {
			@Override
			public boolean apply(@NullableDecl FortItemStack input) {
				return input != null && input.getIdCategory().equals("ChallengeBundle");
			}
		}).append(new FortItemStack("Quest", ChallengeBundleActivity.VALUE_DAILY_CHALLENGES, 1)).toSortedList(new Comparator<FortItemStack>() {
			@Override
			public int compare(FortItemStack o1, FortItemStack o2) {
				return o1.getIdName().compareToIgnoreCase(o2.getIdName());
			}
		});

		if (adapter == null) {
			list.setAdapter(adapter = new ChallengeBundleAdapter(data, this));
		} else {
			adapter.data = data;
			adapter.notifyDataSetChanged();
		}
	}

	private static class ChallengeBundleAdapter extends RecyclerView.Adapter<ChallengeBundleAdapter.ChallengeBundleViewHolder> {
		private List<FortItemStack> data;
		private final BaseActivity activity;

		public ChallengeBundleAdapter(List<FortItemStack> data, BaseActivity activity) {
			this.data = data;
			this.activity = activity;
		}

		@NonNull
		@Override
		public ChallengeBundleViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
			return new ChallengeBundleViewHolder(LayoutInflater.from(parent.getContext()).inflate(R.layout.challenge_bundle_entry, parent, false));
		}

		@Override
		public void onBindViewHolder(@NonNull ChallengeBundleViewHolder holder, int position) {
			final FortItemStack entry = data.get(position);

			if (entry.getIdName().equals(ChallengeBundleActivity.VALUE_DAILY_CHALLENGES)) {
				holder.title.setText("Daily");
				holder.itemView.setBackground(null);
				holder.completionContainer.setVisibility(View.GONE);
			} else {

				FortChallengeBundleItemDefinition def = (FortChallengeBundleItemDefinition) entry.getDefData();

				if (def == null) {
					holder.title.setText(entry.getIdName());
					holder.itemView.setBackground(null);
				} else {
					holder.title.setText(def.DisplayName);
					holder.itemView.setBackground(new ChallengeBundleActivity.TitleBackgroundDrawable(activity, def.DisplayStyle));
				}

				if (entry.attributes != null) {
					int max = def == null ? 1 : def.QuestInfos.length;
					int progress = JsonUtils.getIntOr("num_progress_quests_completed", entry.attributes, 0);
					holder.completionProgress.setMax(max);
					Utils.progressBarSetProgressAnimateFromEmpty(holder.completionProgress, progress);
					holder.completionText.setText(NumberFormat.getPercentInstance().format((float) progress / (float) max));
					holder.completionContainer.setVisibility(View.VISIBLE);
				} else {
					holder.completionContainer.setVisibility(View.GONE);
				}
			}

			holder.itemView.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					Intent intent = new Intent(activity, ChallengeBundleActivity.class);
					intent.putExtra("a", entry.templateId);
					activity.startActivity(intent);
				}
			});
		}

		@Override
		public int getItemCount() {
			return data.size();
		}

		class ChallengeBundleViewHolder extends RecyclerView.ViewHolder {
			ImageView image;
			TextView title;
			ViewGroup completionContainer;
			ProgressBar completionProgress;
			TextView completionText;

			ChallengeBundleViewHolder(View itemView) {
				super(itemView);
				image = itemView.findViewById(R.id.item_img);
				title = itemView.findViewById(R.id.item_text1);
				completionContainer = itemView.findViewById(R.id.quest_completion_container);
				completionProgress = itemView.findViewById(R.id.progress_horizontal);
				completionText = itemView.findViewById(R.id.item_text2);
			}
		}
	}
}