package com.tb24.fn.activity;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.tb24.fn.R;
import com.tb24.fn.event.ProfileUpdatedEvent;
import com.tb24.fn.model.FortItemStack;
import com.tb24.fn.model.FortMcpProfile;
import com.tb24.fn.model.assetdata.FortChallengeBundleItemDefinition;
import com.tb24.fn.model.assetdata.FortQuestItemDefinition;
import com.tb24.fn.util.ItemUtils;
import com.tb24.fn.util.LoadingViewController;
import com.tb24.fn.util.Utils;

import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

public class ChallengeBundleActivity extends BaseActivity {
	private RecyclerView list;
	private ChallengeAdapter adapter;
	private LoadingViewController lc;
	private JsonObject attributesFromProfile;
	private FortMcpProfile profileData;

	public static void populateQuestView(BaseActivity activity, View view, FortItemStack item) {
		ProgressBar questProgressBar = view.findViewById(R.id.quest_progress_bar);
		TextView questTitle = view.findViewById(R.id.quest_title);
		TextView questProgressText = view.findViewById(R.id.quest_progress_text);
		View questRewardParent = view.findViewById(R.id.quest_reward_parent);
		JsonElement jsonElement1 = activity.getThisApplication().itemRegistry.get(item.templateId);

		if (jsonElement1 == null) {
			questTitle.setText("Currently unavailable");
			questProgressBar.setVisibility(View.GONE);
			questProgressText.setVisibility(View.GONE);
			questRewardParent.setVisibility(View.GONE);
			return;
		}

		questProgressBar.setVisibility(View.VISIBLE);
		questProgressText.setVisibility(View.VISIBLE);
		questRewardParent.setVisibility(View.VISIBLE);
		FortQuestItemDefinition quest = activity.getThisApplication().gson.fromJson(jsonElement1.getAsJsonArray().get(0), FortQuestItemDefinition.class);
		questTitle.setText(quest.DisplayName);
		int completion = 0;
		int max = 0;
		String backendNameSuffix = "";

		if (quest.GameplayTags != null) {
			for (String tag : quest.GameplayTags.gameplay_tags) {
				if (tag.equals("Quest.Metadata.Glyph")) {
					backendNameSuffix = item.templateId.substring(item.templateId.lastIndexOf('_'));
					break;
				}
			}
		}

		for (FortQuestItemDefinition.Objective objective : quest.Objectives) {
			if (item.attributes != null) {
				String backendName = "completion_" + objective.BackendName.toLowerCase();

				if (item.attributes.has(backendName)) {
					completion += item.attributes.get(backendName).getAsInt();
				} else if (item.attributes.has(backendName + backendNameSuffix)) {
					completion += item.attributes.get(backendName + backendNameSuffix).getAsInt();
				}
			}

			max += objective.Count;
		}

		questProgressBar.setMax(max);
		questProgressBar.setProgress(completion);
		questProgressText.setText(TextUtils.concat(String.format("%,d", completion), " / ", String.format("%,d", max)));

		if (quest.Rewards == null) {
			questRewardParent.setVisibility(View.GONE);
		} else {
			questRewardParent.setVisibility(View.VISIBLE);
			FortItemStack rewardItem = quest.Rewards[0].asItemStack();
			JsonElement jsonElement = activity.getThisApplication().itemRegistry.get(rewardItem.templateId);

			if (jsonElement != null) {
				((ImageView) view.findViewById(R.id.quest_reward_icon)).setImageBitmap(ItemUtils.getBitmapImageFromItemStackData(activity, rewardItem, jsonElement.getAsJsonArray().get(0).getAsJsonObject()));
			}

			((TextView) view.findViewById(R.id.quest_reward_text)).setText(String.valueOf(rewardItem.quantity));
		}
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.common_loadable_recycler_view);
		setupActionBar();
		list = findViewById(R.id.main_recycler_view);
		int p = (int) Utils.dp(getResources(), 4);
		list.setPadding(p, p, p, p);
		list.setClipToPadding(false);
		list.setLayoutManager(new LinearLayoutManager(ChallengeBundleActivity.this));
		lc = new LoadingViewController(this, list, "Challenge data not found") {
			@Override
			public boolean shouldShowEmpty() {
				return adapter == null;
			}
		};
		displayData(getThisApplication().profileManager.profileData.get("athena"));
		getThisApplication().eventBus.register(this);
	}

	@Subscribe(threadMode = ThreadMode.MAIN)
	public void onProfileUpdated(ProfileUpdatedEvent event) {
		if (event.profileId.equals("athena")) {
			displayData(event.profileObj);
		}
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		getThisApplication().eventBus.unregister(this);
	}

	private void displayData(FortMcpProfile profile) {
		profileData = profile;

		if (profile == null) {
			lc.loading();
			return;
		}

		String bundleTemplateId = getIntent().getStringExtra("a");
		JsonElement a = getThisApplication().itemRegistry.get(bundleTemplateId);

		if (a == null) {
			lc.content();
			return;
		}

		for (Map.Entry<String, FortItemStack> entry : profile.items.entrySet()) {
			if (entry.getValue().templateId.equals(bundleTemplateId)) {
				attributesFromProfile = entry.getValue().attributes;
				break;
			}
		}

		FortChallengeBundleItemDefinition def = getThisApplication().gson.fromJson(a.getAsJsonArray().get(0).getAsJsonObject(), FortChallengeBundleItemDefinition.class);
		List<FortChallengeBundleItemDefinition.QuestInfo> data = Arrays.asList(def.QuestInfos);

		if (adapter == null) {
			list.setAdapter(adapter = new ChallengeAdapter(this, data));
		} else {
			adapter.data = data;
			adapter.notifyDataSetChanged();
		}

		lc.content();
	}

	private static class ChallengeAdapter extends RecyclerView.Adapter<ChallengeAdapter.ChallengeViewHolder> {
		private final ChallengeBundleActivity activity;
		private List<FortChallengeBundleItemDefinition.QuestInfo> data;

		public ChallengeAdapter(ChallengeBundleActivity activity, List<FortChallengeBundleItemDefinition.QuestInfo> data) {
			this.activity = activity;
			this.data = data;
		}

		@NonNull
		@Override
		public ChallengeViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
			return new ChallengeViewHolder(LayoutInflater.from(parent.getContext()).inflate(R.layout.test_quest, parent, false));
		}

		@Override
		public void onBindViewHolder(@NonNull ChallengeViewHolder holder, int position) {
			final FortChallengeBundleItemDefinition.QuestInfo item = data.get(position);
			FortItemStack itemStack = null;
			JsonArray grantedquestinstanceids = activity.attributesFromProfile.get("grantedquestinstanceids").getAsJsonArray();

			if (position < grantedquestinstanceids.size()) {
				itemStack = activity.profileData.items.get(grantedquestinstanceids.get(position).getAsString());
			}

			if (itemStack == null) {
				itemStack = new FortItemStack("Quest", item.QuestDefinition.asset_path_name.substring(item.QuestDefinition.asset_path_name.lastIndexOf('.') + 1), 1);
			}

			populateQuestView(activity, holder.itemView, itemStack);
			holder.itemView.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
				}
			});
			holder.itemView.setOnLongClickListener(new View.OnLongClickListener() {
				@Override
				public boolean onLongClick(View v) {
					return false;
				}
			});
		}

		@Override
		public int getItemCount() {
			return data.size();
		}

		static class ChallengeViewHolder extends RecyclerView.ViewHolder {
			ChallengeViewHolder(View itemView) {
				super(itemView);
			}
		}
	}

}
