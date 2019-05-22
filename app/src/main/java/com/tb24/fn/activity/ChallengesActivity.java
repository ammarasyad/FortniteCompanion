package com.tb24.fn.activity;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.google.common.base.Predicate;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.ImmutableList;
import com.tb24.fn.R;
import com.tb24.fn.event.ProfileUpdatedEvent;
import com.tb24.fn.model.FortItemStack;
import com.tb24.fn.model.FortMcpProfile;
import com.tb24.fn.model.assetdata.FortChallengeBundleItemDefinition;
import com.tb24.fn.util.JsonUtils;
import com.tb24.fn.util.LoadingViewController;
import com.tb24.fn.util.Utils;

import org.checkerframework.checker.nullness.compatqual.NullableDecl;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.text.NumberFormat;
import java.util.Comparator;
import java.util.List;

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

	@Subscribe(threadMode = ThreadMode.MAIN)
	public void onProfileUpdated(ProfileUpdatedEvent event) {
		if (event.profileId.equals("athena")) {
			refreshUi();
		}
	}

	public void refreshUi() {
		profileData = getThisApplication().profileManager.profileData.get("athena");

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
		}).toSortedList(new Comparator<FortItemStack>() {
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
			FortChallengeBundleItemDefinition def = (FortChallengeBundleItemDefinition) entry.getDefData();

			if (def == null) {
				holder.title.setText(entry.getIdName());
				holder.itemView.setBackground(null);
			} else {
				holder.title.setText(def.DisplayName);
				holder.itemView.setBackgroundColor(def.DisplayStyle.PrimaryColor.asInt());
			}

			if (entry.attributes != null) {
				int max = def == null ? 1 : def.QuestInfos.length;
				int progress = JsonUtils.getIntOr("num_progress_quests_completed", entry.attributes, 0);
				holder.completionProgress.setMax(max);
				Utils.progressBarSetProgressAnimateFromEmpty(holder.completionProgress, progress);
				holder.completionText.setText(NumberFormat.getPercentInstance().format((float) progress / (float) max));
			} else {
				holder.completionProgress.setMax(1);
				holder.completionProgress.setProgress(0);
				holder.completionText.setText(null);
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
			ProgressBar completionProgress;
			TextView completionText;

			ChallengeBundleViewHolder(View itemView) {
				super(itemView);
				image = itemView.findViewById(R.id.item_img);
				title = itemView.findViewById(R.id.item_text1);
				completionProgress = itemView.findViewById(R.id.progress_horizontal);
				completionText = itemView.findViewById(R.id.item_text2);
			}
		}
	}
}