package com.tb24.fn.activity;

import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.tb24.fn.R;
import com.tb24.fn.event.ProfileUpdatedEvent;
import com.tb24.fn.model.AthenaProfileAttributes;
import com.tb24.fn.model.EpicError;
import com.tb24.fn.model.FortItemStack;
import com.tb24.fn.model.FortMcpProfile;
import com.tb24.fn.model.FortMcpResponse;
import com.tb24.fn.model.assetdata.FortChallengeBundleItemDefinition;
import com.tb24.fn.model.assetdata.FortQuestItemDefinition;
import com.tb24.fn.model.command.FortRerollDailyQuest;
import com.tb24.fn.util.ItemUtils;
import com.tb24.fn.util.JsonUtils;
import com.tb24.fn.util.LoadingViewController;
import com.tb24.fn.util.Utils;

import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Response;

public class ChallengeBundleActivity extends BaseActivity {
	private RecyclerView list;
	private ChallengeAdapter adapter;
	private LoadingViewController lc;
	private FortMcpProfile profileData;
	private JsonObject attributesFromProfile;
	private Map<String, FortItemStack> questsFromProfile = new HashMap<>();
	private Call<FortMcpResponse> callReroll;

	public static FortQuestItemDefinition populateQuestView(BaseActivity activity, View view, FortItemStack item) {
		ProgressBar questProgressBar = view.findViewById(R.id.quest_progress_bar);
		TextView questTitle = view.findViewById(R.id.quest_title);
		TextView questProgressText = view.findViewById(R.id.quest_progress_text);
		View questRewardParent = view.findViewById(R.id.quest_reward_parent);
		boolean done = item.attributes != null && JsonUtils.getStringOr("quest_state", item.attributes, "").equals("Claimed");
		view.findViewById(R.id.quest_main_container).setAlpha(done ? 0.6F : 1.0F);
		view.findViewById(R.id.quest_done).setVisibility(done ? View.VISIBLE : View.GONE);
		FortQuestItemDefinition quest = (FortQuestItemDefinition) item.getDefData();

		if (quest == null) {
			questTitle.setText("Currently unavailable");
			questProgressBar.setVisibility(View.INVISIBLE);
			questProgressText.setVisibility(View.INVISIBLE);
			questRewardParent.setVisibility(View.INVISIBLE);
			return null;
		}

		questProgressBar.setVisibility(View.VISIBLE);
		questProgressText.setVisibility(View.VISIBLE);
		questRewardParent.setVisibility(View.VISIBLE);
		questTitle.setText(quest.DisplayName);
		int completion = 0;
		int max = 0;
		String backendNameSuffix = quest.GameplayTags != null && Arrays.binarySearch(quest.GameplayTags.gameplay_tags, "Quest.Metadata.Glyph") >= 0 ? item.templateId.substring(item.templateId.lastIndexOf('_')) : "";

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

		return quest;
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
		if ((profileData = profile) == null) {
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

		for (JsonElement jsonElement : attributesFromProfile.get("grantedquestinstanceids").getAsJsonArray()) {
			String s = jsonElement.getAsString();
			questsFromProfile.put(s, profile.items.get(s));
		}

		FortChallengeBundleItemDefinition def = getThisApplication().gson.fromJson(a.getAsJsonArray().get(0).getAsJsonObject(), FortChallengeBundleItemDefinition.class);
		setTitle(Utils.color(def.DisplayName, def.DisplayStyle.AccentColor.asInt()));
		getWindow().setStatusBarColor(def.DisplayStyle.PrimaryColor.asInt());
		getActionBar().setBackgroundDrawable(new ColorDrawable(def.DisplayStyle.PrimaryColor.asInt()));
		List<FortChallengeBundleItemDefinition.QuestInfo> data = Arrays.asList(def.QuestInfos);

		if (adapter == null) {
			list.setAdapter(adapter = new ChallengeAdapter(this, data));
		} else {
			adapter.updateData(data);
		}

		lc.content();
	}

	private static class ChallengeAdapter extends RecyclerView.Adapter<ChallengeAdapter.ChallengeViewHolder> {
		private final ChallengeBundleActivity activity;
		private List<FortChallengeBundleItemDefinition.QuestInfo> data;
		private int expandedPosition = -1;

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
		public void onBindViewHolder(@NonNull final ChallengeViewHolder holder, final int position) {
			final FortChallengeBundleItemDefinition.QuestInfo entryDef = data.get(position);
			List<FortQuestItemDefinition> questDefChain = new ArrayList<>();
			List<FortItemStack> questItemChain = new ArrayList<>();
			String questItemName = entryDef.QuestDefinition.asset_path_name.substring(entryDef.QuestDefinition.asset_path_name.lastIndexOf('/') + 1, entryDef.QuestDefinition.asset_path_name.lastIndexOf('.')).toLowerCase();

			for (FortItemStack entry : activity.questsFromProfile.values()) {
				if (entry.templateId.equals("Quest:" + questItemName)) {
					fillQuestItemsRecursive(entry, questItemChain);
					break;
				}
			}

			FortItemStack activeQuestItem = null;

			if (!questItemChain.isEmpty()) {
				activeQuestItem = questItemChain.get(questItemChain.size() - 1);
				fillQuestDefsRecursive((FortQuestItemDefinition) questItemChain.get(0).getDefData(), questDefChain);
			}

			if (activeQuestItem == null) {
				activeQuestItem = new FortItemStack("Quest", questItemName, 1);
			}

			FortQuestItemDefinition quest = populateQuestView(activity, holder.itemView, activeQuestItem);
			TextView questTitle = holder.itemView.findViewById(R.id.quest_title);
			questTitle.setText((questDefChain.size() > 1 ? String.format("Stage %d of %d - ", questItemChain.size(), questDefChain.size()) : "") + questTitle.getText());
			holder.actions.setVisibility(expandedPosition == position ? View.VISIBLE : View.GONE);
			boolean done = activeQuestItem.attributes != null && JsonUtils.getStringOr("quest_state", activeQuestItem.attributes, "").equals("Claimed");
			final boolean isEligibleForReplacement = !done && quest != null && quest.export_type.equals("AthenaDailyQuestDefinition") && ((AthenaProfileAttributes) activity.profileData.stats.attributesObj).quest_manager.dailyQuestRerolls > 0;
			final boolean isEligibleForAssist = !done && quest != null && quest.GameplayTags != null && Arrays.binarySearch(quest.GameplayTags.gameplay_tags, "Quest.Metadata.PartyAssist") >= 0;
			holder.btnReplace.setVisibility(isEligibleForReplacement ? View.VISIBLE : View.GONE);
			holder.btnAssist.setVisibility(isEligibleForAssist ? View.VISIBLE : View.GONE);
			holder.btnReplace.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(final View v) {
					if (!isEligibleForReplacement) {
						return;
					}

					v.setEnabled(false);
					FortRerollDailyQuest payload = new FortRerollDailyQuest();
					activity.callReroll = activity.getThisApplication().fortnitePublicService.mcp("FortRerollDailyQuest", PreferenceManager.getDefaultSharedPreferences(activity).getString("epic_account_id", ""), "athena", -1, payload);
					new Thread("Reroll Daily Quest Worker") {
						@Override
						public void run() {
							try {
								Response<FortMcpResponse> response = activity.callReroll.execute();

								if (response.isSuccessful()) {
									activity.runOnUiThread(new Runnable() {
										@Override
										public void run() {
											notifyDataSetChanged();
										}
									});
								} else {
									Utils.dialogError(activity, EpicError.parse(response).getDisplayText());
								}
							} catch (IOException e) {
								Utils.throwableDialog(activity, e);
							} finally {
								activity.runOnUiThread(new Runnable() {
									@Override
									public void run() {
										v.setEnabled(true);
									}
								});
							}
						}
					}.start();
				}
			});
			holder.btnAssist.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					if (!isEligibleForAssist) {
						return;
					}

					// TODO set party assist quest
//					SetPartyAssistQuest payload = new SetPartyAssistQuest();
//					Call<FortMcpResponse> call = activity.getThisApplication().fortnitePublicService.mcp("SetPartyAssistQuest", PreferenceManager.getDefaultSharedPreferences(activity).getString("epic_account_id", ""), "athena", -1, payload);
//					new Thread("Set Party Assist Worker") {
//						@Override
//						public void run() {
//						}
//					}.start();
				}
			});
			holder.itemView.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					if (isEligibleForReplacement || isEligibleForAssist) {
						boolean newState = expandedPosition != holder.getAdapterPosition();

						if (expandedPosition >= 0) {
							notifyItemChanged(expandedPosition);
						}

						if (newState) {
							expandedPosition = holder.getAdapterPosition();
							notifyItemChanged(holder.getAdapterPosition());
						} else {
							expandedPosition = -1;
						}
					}
				}
			});
			holder.itemView.setOnLongClickListener(new View.OnLongClickListener() {
				@Override
				public boolean onLongClick(View v) {
					return false;
				}
			});
		}

		private void fillQuestDefsRecursive(FortQuestItemDefinition questDef, List<FortQuestItemDefinition> outQuestDefs) {
			if (questDef == null) {
				return;
			}

			outQuestDefs.add(questDef);

			for (FortQuestItemDefinition.Reward reward : questDef.Rewards) {
				FortItemStack stack = reward.asItemStack();
				if (stack.getIdCategory().equals("Quest")) {
					fillQuestDefsRecursive((FortQuestItemDefinition) stack.getDefData(), outQuestDefs);
				}
			}
		}

		private void fillQuestItemsRecursive(FortItemStack chainedQuestItem, List<FortItemStack> outStacks) {
			if (chainedQuestItem == null) {
				return;
			}

			outStacks.add(chainedQuestItem);
			String challenge_linked_quest_given = JsonUtils.getStringOr("challenge_linked_quest_given", chainedQuestItem.attributes, null);

			if (challenge_linked_quest_given != null && !challenge_linked_quest_given.isEmpty()) {
				fillQuestItemsRecursive(activity.questsFromProfile.get(challenge_linked_quest_given), outStacks);
			}
		}

		@Override
		public int getItemCount() {
			return data.size();
		}

		public void updateData(List<FortChallengeBundleItemDefinition.QuestInfo> data) {
			this.data = data;
			notifyDataSetChanged();
		}

		static class ChallengeViewHolder extends RecyclerView.ViewHolder {
			ViewGroup actions;
			Button btnAssist;
			Button btnReplace;

			ChallengeViewHolder(View itemView) {
				super(itemView);
				actions = itemView.findViewById(R.id.quest_options);
				btnAssist = itemView.findViewById(R.id.quest_btn_assist);
				btnReplace = itemView.findViewById(R.id.quest_btn_replace);
			}
		}
	}

}
