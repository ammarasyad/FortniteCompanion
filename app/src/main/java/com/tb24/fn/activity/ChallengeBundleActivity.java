package com.tb24.fn.activity;

import android.graphics.drawable.ColorDrawable;
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

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.tb24.fn.R;
import com.tb24.fn.event.ProfileUpdatedEvent;
import com.tb24.fn.model.FortItemStack;
import com.tb24.fn.model.FortMcpProfile;
import com.tb24.fn.model.assetdata.FortChallengeBundleItemDefinition;
import com.tb24.fn.model.assetdata.FortQuestItemDefinition;
import com.tb24.fn.util.ItemUtils;
import com.tb24.fn.util.JsonUtils;
import com.tb24.fn.util.LoadingViewController;
import com.tb24.fn.util.Utils;

import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class ChallengeBundleActivity extends BaseActivity {
	private RecyclerView list;
	private ChallengeAdapter adapter;
	private LoadingViewController lc;
	private JsonObject attributesFromProfile;
	private Set<FortItemStack> set = new HashSet<>();

	public static void populateQuestView(BaseActivity activity, View view, FortItemStack item) {
		ProgressBar questProgressBar = view.findViewById(R.id.quest_progress_bar);
		TextView questTitle = view.findViewById(R.id.quest_title);
		TextView questProgressText = view.findViewById(R.id.quest_progress_text);
		View questRewardParent = view.findViewById(R.id.quest_reward_parent);
		boolean done = item.attributes != null && JsonUtils.getStringOr("quest_state", item.attributes, "").equals("Claimed");
		view.setAlpha(done ? 0.6F : 1.0F);
		view.findViewById(R.id.quest_done).setVisibility(done ? View.VISIBLE : View.GONE);
		JsonElement jsonElement1 = activity.getThisApplication().itemRegistry.get(item.templateId);

		if (jsonElement1 == null) {
			questTitle.setText("Currently unavailable");
			questProgressBar.setVisibility(View.INVISIBLE);
			questProgressText.setVisibility(View.INVISIBLE);
			questRewardParent.setVisibility(View.INVISIBLE);
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

		if (quest.ObjectiveCompletionCount != null) {
			max = quest.ObjectiveCompletionCount;
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
		list.setLayoutManager(new LinearLayoutManager(ChallengeBundleActivity.this));
		lc = new LoadingViewController(this, list, "Challenge data not found") {
			@Override
			public boolean shouldShowEmpty() {
				return adapter == null;
			}
		};
		refreshUi(getThisApplication().profileManager.profileData.get("athena"));
		getThisApplication().eventBus.register(this);
	}

	@Subscribe(threadMode = ThreadMode.MAIN)
	public void onProfileUpdated(ProfileUpdatedEvent event) {
		if (event.profileId.equals("athena")) {
			refreshUi(event.profileObj);
		}
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		getThisApplication().eventBus.unregister(this);
	}

	private void refreshUi(FortMcpProfile profile) {
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

		for (JsonElement s : attributesFromProfile.get("grantedquestinstanceids").getAsJsonArray()) {
			set.add(profile.items.get(s.getAsString()));
		}

		FortChallengeBundleItemDefinition def = getThisApplication().gson.fromJson(a.getAsJsonArray().get(0).getAsJsonObject(), FortChallengeBundleItemDefinition.class);
		setTitle(Utils.color(def.DisplayName, def.DisplayStyle.AccentColor.asInt()));
		getWindow().setStatusBarColor(def.DisplayStyle.PrimaryColor.asInt());
		getActionBar().setBackgroundDrawable(new ColorDrawable(def.DisplayStyle.PrimaryColor.asInt()));
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
			return new ChallengeViewHolder(LayoutInflater.from(parent.getContext()).inflate(R.layout.quest_entry, parent, false));
		}

		@Override
		public void onBindViewHolder(@NonNull ChallengeViewHolder holder, int position) {
			final FortChallengeBundleItemDefinition.QuestInfo item = data.get(position);
			FortItemStack itemStack = null;
			String questName = item.QuestDefinition.asset_path_name.substring(item.QuestDefinition.asset_path_name.lastIndexOf('/') + 1, item.QuestDefinition.asset_path_name.lastIndexOf('.')).toLowerCase();

			for (FortItemStack entry : activity.set) {
				if (entry.templateId.equals("Quest:" + questName)) {
					itemStack = entry;
					break;
				}
			}

			if (itemStack == null) {
				itemStack = new FortItemStack("Quest", questName, 1);
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
