package com.tb24.fn.activity;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.TooltipCompat;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.tb24.fn.R;
import com.tb24.fn.event.ProfileUpdatedEvent;
import com.tb24.fn.model.AthenaProfileAttributes;
import com.tb24.fn.model.CalendarTimelineResponse;
import com.tb24.fn.model.EpicError;
import com.tb24.fn.model.FortItemStack;
import com.tb24.fn.model.FortMcpProfile;
import com.tb24.fn.model.FortMcpResponse;
import com.tb24.fn.model.assetdata.AssetReference;
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
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import retrofit2.Call;
import retrofit2.Response;

public class ChallengeBundleActivity extends BaseActivity {
	public static final String VALUE_DAILY_CHALLENGES = "_daily__challenges";
	private static final Map<String, String> cheatsheetUrls = new HashMap<>();

	static {
		// credit to u/thesquatingdog for the original cheatsheets
		cheatsheetUrls.put("ChallengeBundle:questbundle_s9_week_001", "https://i.redd.it/eoa6qwoe8fx21.jpg");
		cheatsheetUrls.put("ChallengeBundle:questbundle_s9_week_002", "https://i.redd.it/s5o3wgbqdly21.jpg");
		cheatsheetUrls.put("ChallengeBundle:questbundle_s9_week_003", "https://i.redd.it/hkczsox5pyz21.jpg");
	}

	private RecyclerView list;
	private ChallengeAdapter adapter;
	private LoadingViewController lc;
	private FortMcpProfile profileData;
	private Map<String, FortItemStack> questsFromProfile = new HashMap<>();
	private Call<FortMcpResponse> callReroll;
	private String cheatsheetUrl;
	private FortChallengeBundleItemDefinition challengeBundleDef;
	private CalendarTimelineResponse.ActiveEvent calendarEventTag;

	public static FortQuestItemDefinition populateQuestView(BaseActivity activity, View view, FortItemStack item) {
		TextView questTitle = view.findViewById(R.id.quest_title);
		View questProgressParent = view.findViewById(R.id.quest_progress_parent);
		ProgressBar questProgressBar = view.findViewById(R.id.quest_progress_bar);
		TextView questProgressText = view.findViewById(R.id.quest_progress_text);
		View questRewardParent = view.findViewById(R.id.quest_reward_parent);
		ImageView rewardIcon = view.findViewById(R.id.quest_reward_icon);
		TextView rewardText = view.findViewById(R.id.quest_reward_text);
		boolean done = item.attributes != null && JsonUtils.getStringOr("quest_state", item.attributes, "").equals("Claimed");
		view.findViewById(R.id.quest_main_container).setAlpha(done ? 0.6F : 1.0F);
		view.findViewById(R.id.quest_done).setVisibility(done ? View.VISIBLE : View.GONE);
		FortQuestItemDefinition quest = (FortQuestItemDefinition) item.getDefData();

		if (quest == null) {
			questProgressParent.setVisibility(View.GONE);
			questRewardParent.setVisibility(View.GONE);
		} else if (done) {
			questProgressParent.setVisibility(View.GONE);
			questRewardParent.setVisibility(View.VISIBLE);
		} else {
			questProgressParent.setVisibility(View.VISIBLE);
			questRewardParent.setVisibility(View.VISIBLE);
		}

		if (quest == null) {
			questTitle.setText("Currently unavailable");
			return null;
		}

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
		questProgressText.setText(TextUtils.concat(Utils.color(String.format("%,d", completion), Utils.getTextColorPrimary(activity)), " / ", String.format("%,d", max)));

		if (quest.Rewards == null) {
			questRewardParent.setVisibility(View.GONE);
			questRewardParent.setOnLongClickListener(null);
		} else {
			questRewardParent.setVisibility(View.VISIBLE);
			FortItemStack rewardItem = quest.Rewards[0].asItemStack();
			JsonElement jsonElement = activity.getThisApplication().itemRegistry.get(rewardItem.templateId);
			int size;

			if (rewardItem.getIdCategory().equals("AccountResource") || rewardItem.quantity > 1) {
				size = 36;
				rewardText.setVisibility(View.VISIBLE);
			} else {
				size = 56;
				rewardText.setVisibility(View.GONE);
			}

			size = (int) Utils.dp(activity.getResources(), size);
			rewardIcon.setLayoutParams(new LinearLayout.LayoutParams((int) Utils.dp(activity.getResources(), 56), size));

			if (jsonElement != null) {
				JsonObject jsonObject = jsonElement.getAsJsonArray().get(0).getAsJsonObject();
				Bitmap bitmapImageFromItemStackData = ItemUtils.getBitmapImageFromItemStackData(activity, rewardItem, jsonObject);
				rewardIcon.setImageBitmap(bitmapImageFromItemStackData);
				questRewardParent.setVisibility(bitmapImageFromItemStackData == null ? View.GONE : View.VISIBLE);
				TooltipCompat.setTooltipText(questRewardParent, JsonUtils.getStringOr("DisplayName", jsonObject, ""));
			} else {
				TooltipCompat.setTooltipText(questRewardParent, null);
			}

			rewardText.setText(String.valueOf(rewardItem.quantity));
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
		list.setBackgroundColor(0x40003468);
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

		if (callReroll != null) {
			callReroll.cancel();
		}
	}

	private void refreshUi(FortMcpProfile profile) {
		if ((profileData = profile) == null) {
			lc.loading();
			return;
		}

		String bundleTemplateId = getIntent().getStringExtra("a");
		questsFromProfile.clear();
		List<FortChallengeBundleItemDefinition.QuestInfo> data;

		if (bundleTemplateId.equals("Quest:" + VALUE_DAILY_CHALLENGES)) {
			setTitle("Daily");
			data = new ArrayList<>();

			for (Map.Entry<String, FortItemStack> entry : profile.items.entrySet()) {
				if (entry.getValue().getIdCategory().equals("Quest") && entry.getValue().getDefData() != null && entry.getValue().getDefData().export_type.equals("AthenaDailyQuestDefinition") && entry.getValue().attributes != null && JsonUtils.getStringOr("quest_state", entry.getValue().attributes, "").equals("Active")) {
					questsFromProfile.put(entry.getKey(), entry.getValue());
					FortChallengeBundleItemDefinition.QuestInfo questInfo = new FortChallengeBundleItemDefinition.QuestInfo();
					String name = entry.getValue().getIdName();
					questInfo.QuestDefinition = new AssetReference("/Game/Athena/Items/Quests/DailyQuests/" + name + '.' + name, "");
					data.add(questInfo);
				}
			}
		} else {
			JsonElement a = getThisApplication().itemRegistry.get(bundleTemplateId);

			if (a == null) {
				lc.content();
				return;
			}

			JsonObject attributesFromProfile = null;

			for (Map.Entry<String, FortItemStack> entry : profile.items.entrySet()) {
				if (entry.getValue().templateId.equals(bundleTemplateId)) {
					attributesFromProfile = entry.getValue().attributes;
					break;
				}
			}

			if (attributesFromProfile == null) {
				lc.content();
				return;
			}

			for (JsonElement jsonElement : attributesFromProfile.get("grantedquestinstanceids").getAsJsonArray()) {
				String s = jsonElement.getAsString();
				questsFromProfile.put(s, profile.items.get(s));
			}

			challengeBundleDef = getThisApplication().gson.fromJson(a.getAsJsonArray().get(0), FortChallengeBundleItemDefinition.class);
			setTitle(Utils.color(challengeBundleDef.DisplayName, challengeBundleDef.DisplayStyle.AccentColor.asInt()));
			getWindow().setStatusBarColor(challengeBundleDef.DisplayStyle.PrimaryColor.asInt());
			getActionBar().setBackgroundDrawable(new ColorDrawable(challengeBundleDef.DisplayStyle.PrimaryColor.asInt()));
			data = Arrays.asList(challengeBundleDef.QuestInfos);

			if (challengeBundleDef.CalendarEventTag != null) {
				for (CalendarTimelineResponse.ActiveEvent activeEvent : getThisApplication().calendarDataBase.channels.get("client-events").states[0].activeEvents) {
					if (activeEvent.eventType.equals(challengeBundleDef.CalendarEventTag)) {
						calendarEventTag = activeEvent;
						break;
					}
				}
			}

			if (cheatsheetUrls.containsKey(bundleTemplateId)) {
				cheatsheetUrl = cheatsheetUrls.get(bundleTemplateId);
			}
		}

		if (adapter == null) {
			list.setAdapter(adapter = new ChallengeAdapter(this, data));
		} else {
			adapter.updateData(data);
		}

		lc.content();
	}

	private String findItemId(String templateId) {
		for (Map.Entry<String, FortItemStack> entry : profileData.items.entrySet()) {
			if (entry.getValue().templateId.equals(templateId)) {
				return entry.getKey();
			}
		}

		return null;
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
			View inflate = LayoutInflater.from(parent.getContext()).inflate(viewType == 1 ? R.layout.log_out_settings_button : R.layout.quest_entry, parent, false);

			if (viewType == 1) {
				inflate.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
			}

			return new ChallengeViewHolder(inflate);
		}

		@Override
		public void onBindViewHolder(@NonNull final ChallengeViewHolder holder, final int position) {
			if (getItemViewType(position) == 1) {
				holder.btnCheatsheet.setText("Cheatsheet plz");
				holder.btnCheatsheet.setOnClickListener(new View.OnClickListener() {
					@Override
					public void onClick(View v) {
						activity.startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(activity.cheatsheetUrl)));
					}
				});
				return;
			}

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

			if (quest == null) {
				holder.actions.setVisibility(View.GONE);
				holder.itemView.setOnClickListener(null);
				return;
			}

			TextView questTitle = holder.itemView.findViewById(R.id.quest_title);
			boolean isChain = questDefChain.size() > 1;
			String questState = activeQuestItem.attributes == null ? "" : JsonUtils.getStringOr("quest_state", activeQuestItem.attributes, "");
			boolean done = questState.equals("Claimed");
			final boolean isEligibleForReplacement = !done && quest.export_type.equals("AthenaDailyQuestDefinition") && questState.equals("Active") && ((AthenaProfileAttributes) activity.profileData.stats.attributesObj).quest_manager.dailyQuestRerolls > 0;
			final boolean isEligibleForAssist = !done && quest.GameplayTags != null && Arrays.binarySearch(quest.GameplayTags.gameplay_tags, "Quest.Metadata.PartyAssist") >= 0;
			questTitle.setText(TextUtils.concat(isChain ? Utils.color(String.format("Stage %,d of %,d", questItemChain.size(), questDefChain.size()), Utils.getTextColorPrimary(activity)) : "", (isChain ? " - " : "") + questTitle.getText()));
			holder.itemView.findViewById(R.id.quest_progress_parent).setVisibility(done ? View.GONE : View.VISIBLE);

			if (entryDef.QuestUnlockType != null && entryDef.QuestUnlockType.equals("EChallengeBundleQuestUnlockType::DaysFromEventStart")) {
				if (entryDef.UnlockValue == 100) {
					questTitle.setText(activity.challengeBundleDef.UniqueLockedMessage);
					holder.itemView.findViewById(R.id.quest_progress_parent).setVisibility(View.GONE);
				} else {
					Calendar calendar = Calendar.getInstance();
					calendar.setTime(activity.calendarEventTag.activeSince);
					calendar.add(Calendar.DAY_OF_MONTH, entryDef.UnlockValue);
					long delta = calendar.getTimeInMillis() - System.currentTimeMillis();

					if (delta > 0) {
						long days = TimeUnit.MILLISECONDS.toDays(delta);
						String s = "Unlocks in " + days + " day" + (days == 1L ? "" : "s");

						if (days < 1L) {
							long hours = TimeUnit.MILLISECONDS.toHours(delta % 86400000);
							s = "Unlocks in " + hours + " hour" + (hours == 1L ? "" : "s");

							if (hours < 1L) {
								s = "Unlocking soon";
							}
						}

						questTitle.setText(TextUtils.concat(Utils.color(s, 0xFFE1564B), " - " + questTitle.getText()));
					}
				}
			}

			holder.actions.setVisibility(expandedPosition == position ? View.VISIBLE : View.GONE);
			holder.btnReplace.setVisibility(isEligibleForReplacement ? View.VISIBLE : View.GONE);
			holder.btnAssist.setVisibility(isEligibleForAssist ? View.VISIBLE : View.GONE);
			final FortItemStack finalActiveQuestItem = activeQuestItem;
			holder.btnReplace.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(final View v) {
					if (!isEligibleForReplacement) {
						return;
					}

					v.setEnabled(false);
					FortRerollDailyQuest payload = new FortRerollDailyQuest();
					payload.questId = activity.findItemId(finalActiveQuestItem.templateId);
					activity.callReroll = activity.getThisApplication().fortnitePublicService.mcp("FortRerollDailyQuest", PreferenceManager.getDefaultSharedPreferences(activity).getString("epic_account_id", ""), "athena", -1, payload);
					new Thread("Reroll Daily Quest Worker") {
						@Override
						public void run() {
							try {
								Response<FortMcpResponse> response = activity.callReroll.execute();

								if (response.isSuccessful()) {
									activity.getThisApplication().profileManager.executeProfileChanges(response.body());
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
					Toast.makeText(activity, "Setting party assist challenge not available yet", Toast.LENGTH_SHORT).show();
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
		}

		private void fillQuestDefsRecursive(FortQuestItemDefinition questDef, List<FortQuestItemDefinition> outQuestDefs) {
			if (questDef == null) {
				return;
			}

			outQuestDefs.add(questDef);

			if (questDef.Rewards == null) {
				return;
			}

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
			String challengeLinkedQuestGiven = JsonUtils.getStringOr("challenge_linked_quest_given", chainedQuestItem.attributes, null);

			if (challengeLinkedQuestGiven != null && !challengeLinkedQuestGiven.isEmpty()) {
				fillQuestItemsRecursive(activity.questsFromProfile.get(challengeLinkedQuestGiven), outStacks);
			}
		}

		@Override
		public int getItemViewType(int position) {
			return activity.cheatsheetUrl != null && position == data.size() ? 1 : 0;
		}

		@Override
		public int getItemCount() {
			return data.size() + (activity.cheatsheetUrl != null ? 1 : 0);
		}

		public void updateData(List<FortChallengeBundleItemDefinition.QuestInfo> data) {
			this.data = data;
			notifyDataSetChanged();
		}

		static class ChallengeViewHolder extends RecyclerView.ViewHolder {
			ViewGroup actions;
			Button btnAssist;
			Button btnReplace;
			Button btnCheatsheet;

			ChallengeViewHolder(View itemView) {
				super(itemView);
				actions = itemView.findViewById(R.id.quest_options);
				btnAssist = itemView.findViewById(R.id.quest_btn_assist);
				btnReplace = itemView.findViewById(R.id.quest_btn_replace);
				btnCheatsheet = itemView.findViewById(R.id.log_out_button);
			}
		}
	}

}
