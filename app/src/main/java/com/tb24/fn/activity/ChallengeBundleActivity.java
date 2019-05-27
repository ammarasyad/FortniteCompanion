package com.tb24.fn.activity;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.graphics.Shader;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.LayerDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.TooltipCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.common.base.Function;
import com.google.common.collect.Lists;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.tb24.fn.R;
import com.tb24.fn.event.CalendarDataLoadedEvent;
import com.tb24.fn.event.ProfileUpdatedEvent;
import com.tb24.fn.model.AthenaProfileAttributes;
import com.tb24.fn.model.CalendarTimelineResponse;
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

import org.checkerframework.checker.nullness.compatqual.NullableDecl;
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
	private JsonObject attributesFromProfile;

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
			questTitle.setText("Unknown Quest: " + item.templateId);
			return null;
		} else if (done) {
			questProgressParent.setVisibility(View.GONE);
		} else {
			questProgressParent.setVisibility(View.VISIBLE);
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

		if (quest.Rewards == null || quest.Rewards.length == 0) {
			questRewardParent.setVisibility(View.GONE);
			TooltipCompat.setTooltipText(questRewardParent, null);
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

			rewardText.setText(String.format("%,d", rewardItem.quantity));
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
		refreshUi();
		getThisApplication().eventBus.register(this);
	}

	@Subscribe(threadMode = ThreadMode.MAIN)
	public void onProfileUpdated(ProfileUpdatedEvent event) {
		if (event.profileId.equals("athena")) {
			refreshUi();
		}
	}

	@Subscribe(threadMode = ThreadMode.MAIN)
	public void onCalendarDataLoaded(CalendarDataLoadedEvent event) {
		refreshUi();
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		getThisApplication().eventBus.unregister(this);

		if (callReroll != null) {
			callReroll.cancel();
		}
	}

	private void refreshUi() {
		if ((profileData = getThisApplication().profileManager.profileData.get("athena")) == null) {
			lc.loading();
			return;
		}

		String bundleTemplateId = getIntent().getStringExtra("a");
		questsFromProfile.clear();
		List<QuestEntry> data = new ArrayList<>();

		if (bundleTemplateId.equals("Quest:" + VALUE_DAILY_CHALLENGES)) {
			setTitle("Daily");

			for (Map.Entry<String, FortItemStack> entry : profileData.items.entrySet()) {
				if (entry.getValue().getIdCategory().equals("Quest") && entry.getValue().getDefData() != null && entry.getValue().getDefData().export_type.equals("AthenaDailyQuestDefinition") && entry.getValue().attributes != null && JsonUtils.getStringOr("quest_state", entry.getValue().attributes, "").equals("Active")) {
					questsFromProfile.put(entry.getKey(), entry.getValue());
					data.add(new DefaultQuestEntry(this, entry.getValue().getIdName(), null));
				}
			}
		} else {
			JsonElement a = getThisApplication().itemRegistry.get(bundleTemplateId);

			if (a == null) {
				lc.content();
				return;
			}

			for (Map.Entry<String, FortItemStack> entry : profileData.items.entrySet()) {
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
				questsFromProfile.put(s, profileData.items.get(s));
			}

			challengeBundleDef = getThisApplication().gson.fromJson(a.getAsJsonArray().get(0), FortChallengeBundleItemDefinition.class);
			setTitle(Utils.color(challengeBundleDef.DisplayName, 0xFFFFFFFF));
			getWindow().setStatusBarColor(challengeBundleDef.DisplayStyle.PrimaryColor.asInt());
			getActionBar().setBackgroundDrawable(new LayerDrawable(new Drawable[]{new ColorDrawable(challengeBundleDef.DisplayStyle.PrimaryColor.asInt()), new TitleBackgroundDrawable(this, challengeBundleDef.DisplayStyle)}));
			List<CompletionRewardQuestEntry> completionRewardQuestEntries = new ArrayList<>();

			for (FortChallengeBundleItemDefinition.BundleCompletionReward bundleCompletionReward : challengeBundleDef.BundleCompletionRewards) {
				boolean add = false;

				for (FortChallengeBundleItemDefinition.Reward reward : bundleCompletionReward.Rewards) {
					if (!reward.RewardType.equals("EAthenaRewardItemType::HiddenReward")) {
						add = true;
					}
				}

				if (add) {
					completionRewardQuestEntries.add(new CompletionRewardQuestEntry(bundleCompletionReward));
				}
			}

			if (!completionRewardQuestEntries.isEmpty()) {
				data.add(new HeaderQuestEntry("Completion Rewards"));
				data.addAll(completionRewardQuestEntries);
			}

			data.add(new HeaderQuestEntry("Challenges"));
			data.addAll(Lists.transform(Arrays.asList(challengeBundleDef.QuestInfos), new Function<FortChallengeBundleItemDefinition.QuestInfo, QuestEntry>() {
				@Override
				public QuestEntry apply(@NullableDecl FortChallengeBundleItemDefinition.QuestInfo input) {
					return new DefaultQuestEntry(ChallengeBundleActivity.this, null, input);
				}
			}));

			if (challengeBundleDef.CalendarEventTag != null && getThisApplication().calendarDataBase != null) {
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
			fillQuestItemsRecursive(questsFromProfile.get(challengeLinkedQuestGiven), outStacks);
		}
	}

	private static class ChallengeAdapter extends RecyclerView.Adapter<ChallengeViewHolder> {
		private static final int VIEW_TYPE_HEADER = 1;
		private static final int VIEW_TYPE_COMPLETION_REWARD = 2;
		private static final int VIEW_TYPE_CHEATSHEET = 3;
		private final ChallengeBundleActivity activity;
		private List<QuestEntry> data;
		private int expandedPosition = -1;

		public ChallengeAdapter(ChallengeBundleActivity activity, List<QuestEntry> data) {
			this.activity = activity;
			this.data = data;
		}

		@NonNull
		@Override
		public ChallengeViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
			View inflate = null;
			LayoutInflater inflater = LayoutInflater.from(parent.getContext());

			if (viewType == 0 || viewType == VIEW_TYPE_COMPLETION_REWARD) {
				inflate = inflater.inflate(R.layout.quest_entry, parent, false);
			} else if (viewType == VIEW_TYPE_HEADER) {
				inflate = inflater.inflate(R.layout.quest_header, parent, false);
			} else if (viewType == VIEW_TYPE_CHEATSHEET) {
				inflate = inflater.inflate(R.layout.log_out_settings_button, parent, false);
				FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.WRAP_CONTENT);
				params.topMargin = (int) Utils.dp(activity.getResources(), 16);
				inflate.setLayoutParams(params);
			}

			return new ChallengeViewHolder(inflate);
		}

		@Override
		public void onBindViewHolder(@NonNull final ChallengeViewHolder holder, final int position) {
			if (getItemViewType(position) == VIEW_TYPE_CHEATSHEET) {
				holder.btnCheatsheet.setText("Cheatsheet plz");
				holder.btnCheatsheet.setOnClickListener(new View.OnClickListener() {
					@Override
					public void onClick(View v) {
						activity.startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(activity.cheatsheetUrl)));
					}
				});
				return;
			}

			data.get(position).bindToViewHolder(activity, this, holder, position);
		}

		@Override
		public int getItemViewType(int position) {
			return activity.cheatsheetUrl != null && position == data.size() ? VIEW_TYPE_CHEATSHEET : data.get(position).getItemViewType();
		}

		@Override
		public int getItemCount() {
			return data.size() + (activity.cheatsheetUrl != null ? 1 : 0);
		}

		public void updateData(List<QuestEntry> data) {
			this.data = data;
			notifyDataSetChanged();
		}
	}

	private static class ChallengeViewHolder extends RecyclerView.ViewHolder {
		public ViewGroup questMainContainer;
		public TextView questDone;
		public TextView questTitle;
		public ViewGroup questProgressParent;
		public ProgressBar questProgressBar;
		public TextView questProgressText;
		public ViewGroup questRewardParent;
		public ImageView questRewardIcon;
		public TextView questRewardText;
		public ViewGroup actions;
		public Button btnAssist;
		public Button btnReplace;
		public Button btnCheatsheet;

		public ChallengeViewHolder(View itemView) {
			super(itemView);
			questMainContainer = itemView.findViewById(R.id.quest_main_container);
			questDone = itemView.findViewById(R.id.quest_done);
			questTitle = itemView.findViewById(R.id.quest_title);
			questProgressParent = itemView.findViewById(R.id.quest_progress_parent);
			questProgressBar = itemView.findViewById(R.id.quest_progress_bar);
			questProgressText = itemView.findViewById(R.id.quest_progress_text);
			questRewardParent = itemView.findViewById(R.id.quest_reward_parent);
			questRewardIcon = itemView.findViewById(R.id.quest_reward_icon);
			questRewardText = itemView.findViewById(R.id.quest_reward_text);
			actions = itemView.findViewById(R.id.quest_options);
			btnAssist = itemView.findViewById(R.id.quest_btn_assist);
			btnReplace = itemView.findViewById(R.id.quest_btn_replace);
			btnCheatsheet = itemView.findViewById(R.id.log_out_button);
		}
	}

	private static abstract class QuestEntry {
		public abstract void bindToViewHolder(ChallengeBundleActivity activity, ChallengeAdapter adapter, RecyclerView.ViewHolder holder, int position);

		public int getItemViewType() {
			return 0;
		}
	}

	private static class HeaderQuestEntry extends QuestEntry {
		private final CharSequence title;

		public HeaderQuestEntry(CharSequence title) {
			this.title = title;
		}

		@Override
		public void bindToViewHolder(ChallengeBundleActivity activity, ChallengeAdapter adapter, RecyclerView.ViewHolder holder, int position) {
			((TextView) ((ViewGroup) holder.itemView).getChildAt(0)).setText(title);
		}

		@Override
		public int getItemViewType() {
			return ChallengeAdapter.VIEW_TYPE_HEADER;
		}
	}

	private static class CompletionRewardQuestEntry extends QuestEntry {
		private final FortChallengeBundleItemDefinition.BundleCompletionReward bundleCompletionReward;

		public CompletionRewardQuestEntry(FortChallengeBundleItemDefinition.BundleCompletionReward bundleCompletionReward) {
			this.bundleCompletionReward = bundleCompletionReward;
		}

		@Override
		public void bindToViewHolder(ChallengeBundleActivity activity, ChallengeAdapter adapter, RecyclerView.ViewHolder holder_, int position) {
			ChallengeViewHolder holder = (ChallengeViewHolder) holder_;
			int completion = JsonUtils.getIntOr("num_progress_quests_completed", activity.attributesFromProfile, 0);
			boolean done = activity.attributesFromProfile != null && completion >= bundleCompletionReward.CompletionCount;
			holder.questMainContainer.setAlpha(done ? 0.6F : 1.0F);
			holder.questDone.setText("Earned!");
			holder.questDone.setVisibility(done ? View.VISIBLE : View.GONE);
			holder.questProgressParent.setVisibility(done ? View.GONE : View.VISIBLE);
			String s;

			if (bundleCompletionReward.CompletionCount == -1 || bundleCompletionReward.CompletionCount == 1) {
				s = "any challenge";
			} else {
				s = String.format((bundleCompletionReward.CompletionCount >= activity.challengeBundleDef.QuestInfos.length ? "all" : "any") + " %,d challenges", bundleCompletionReward.CompletionCount);
			}

			holder.questTitle.setText(TextUtils.replace("Complete %s to earn the reward item", new String[]{"%s"}, new CharSequence[]{Utils.color(s.toUpperCase(), Utils.getTextColorPrimary(activity))}));
			holder.questProgressBar.setMax(bundleCompletionReward.CompletionCount == -1 ? 1 : bundleCompletionReward.CompletionCount);
			holder.questProgressBar.setProgress(completion);
			holder.questProgressText.setText(TextUtils.concat(Utils.color(String.format("%,d", completion), Utils.getTextColorPrimary(activity)), " / ", String.format("%,d", bundleCompletionReward.CompletionCount)));

			if (bundleCompletionReward.Rewards == null || bundleCompletionReward.Rewards.length == 0) {
				holder.questRewardParent.setVisibility(View.GONE);
				TooltipCompat.setTooltipText(holder.questRewardParent, null);
			} else {
				holder.questRewardParent.setVisibility(View.VISIBLE);
				FortChallengeBundleItemDefinition.Reward reward = bundleCompletionReward.Rewards[0];
				JsonElement jsonElement = reward.TemplateId != null && !reward.TemplateId.isEmpty() ? activity.getThisApplication().itemRegistry.get(reward.TemplateId) : activity.getThisApplication().itemRegistry.getWithAnyNamespace(Utils.parseUPath2(reward.ItemDefinition.asset_path_name));
				int size;

				if (reward.Quantity > 1) {
					size = 36;
					holder.questRewardText.setVisibility(View.VISIBLE);
				} else {
					size = 56;
					holder.questRewardText.setVisibility(View.GONE);
				}

				size = (int) Utils.dp(activity.getResources(), size);
				holder.questRewardIcon.setLayoutParams(new LinearLayout.LayoutParams((int) Utils.dp(activity.getResources(), 56), size));

				if (jsonElement != null) {
					JsonObject jsonObject = jsonElement.getAsJsonArray().get(0).getAsJsonObject();
					Bitmap bitmapImageFromItemStackData = ItemUtils.getBitmapImageFromItemStackData(activity, null, jsonObject);
					holder.questRewardIcon.setImageBitmap(bitmapImageFromItemStackData);
					holder.questRewardParent.setVisibility(bitmapImageFromItemStackData == null ? View.GONE : View.VISIBLE);
					TooltipCompat.setTooltipText(holder.questRewardParent, JsonUtils.getStringOr("DisplayName", jsonObject, ""));
				} else {
					TooltipCompat.setTooltipText(holder.questRewardParent, null);
				}

				holder.questRewardText.setText(String.format("%,d", reward.Quantity));
			}
		}

		@Override
		public int getItemViewType() {
			return ChallengeAdapter.VIEW_TYPE_COMPLETION_REWARD;
		}
	}

	private static class DefaultQuestEntry extends QuestEntry {
		private final FortChallengeBundleItemDefinition.QuestInfo entryDef;
		private final List<FortQuestItemDefinition> questDefChain = new ArrayList<>();
		private final List<FortItemStack> questItemChain = new ArrayList<>();
		private FortItemStack activeQuestItem = null;

		public DefaultQuestEntry(ChallengeBundleActivity activity, String questItemTemplateId, FortChallengeBundleItemDefinition.QuestInfo entryDef) {
			this.entryDef = entryDef;

			if (questItemTemplateId == null) {
				questItemTemplateId = entryDef.QuestDefinition.asset_path_name.substring(entryDef.QuestDefinition.asset_path_name.lastIndexOf('/') + 1, entryDef.QuestDefinition.asset_path_name.lastIndexOf('.')).toLowerCase();
			}

			for (FortItemStack entry : activity.questsFromProfile.values()) {
				if (entry.templateId.equals("Quest:" + questItemTemplateId)) {
					activity.fillQuestItemsRecursive(entry, questItemChain);
					break;
				}
			}

			if (!questItemChain.isEmpty()) {
				activeQuestItem = questItemChain.get(questItemChain.size() - 1);
				activity.fillQuestDefsRecursive((FortQuestItemDefinition) questItemChain.get(0).getDefData(), questDefChain);
			}

			if (activeQuestItem == null) {
				activeQuestItem = new FortItemStack("Quest", questItemTemplateId, 1);
			}
		}

		@Override
		public void bindToViewHolder(final ChallengeBundleActivity activity, final ChallengeAdapter adapter, RecyclerView.ViewHolder holder_, int position) {
			final ChallengeViewHolder holder = (ChallengeViewHolder) holder_;
			FortQuestItemDefinition quest = populateQuestView(activity, holder.itemView, activeQuestItem);

			if (quest == null) {
				holder.actions.setVisibility(View.GONE);
				holder.itemView.setOnClickListener(null);
				return;
			}

			boolean isChain = questDefChain.size() > 1;
			String questState = activeQuestItem.attributes == null ? "" : JsonUtils.getStringOr("quest_state", activeQuestItem.attributes, "");
			boolean done = questState.equals("Claimed");
			final boolean isEligibleForReplacement = !done && quest.export_type.equals("AthenaDailyQuestDefinition") && questState.equals("Active") && ((AthenaProfileAttributes) activity.profileData.stats.attributesObj).quest_manager.dailyQuestRerolls > 0;
			final boolean isEligibleForAssist = !done && quest.GameplayTags != null && Arrays.binarySearch(quest.GameplayTags.gameplay_tags, "Quest.Metadata.PartyAssist") >= 0;
			holder.questTitle.setText(TextUtils.concat(isChain ? Utils.color(String.format("Stage %,d of %,d", questItemChain.size(), questDefChain.size()), Utils.getTextColorPrimary(activity)) : "", (isChain ? " - " : "") + holder.questTitle.getText()));
			holder.questProgressParent.setVisibility(done ? View.GONE : View.VISIBLE);

			if (entryDef != null && entryDef.QuestUnlockType != null && entryDef.QuestUnlockType.equals("EChallengeBundleQuestUnlockType::DaysFromEventStart")) {
				if (entryDef.UnlockValue == 100) {
					holder.questTitle.setText(activity.challengeBundleDef.UniqueLockedMessage);
					holder.questProgressParent.setVisibility(View.GONE);
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

						holder.questTitle.setText(TextUtils.concat(Utils.color(s, 0xFFE1564B), " - " + holder.questTitle.getText()));
					}
				}
			}

			holder.actions.setVisibility(adapter.expandedPosition == position ? View.VISIBLE : View.GONE);
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
											adapter.notifyDataSetChanged();
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
						boolean newState = adapter.expandedPosition != holder.getAdapterPosition();

						if (adapter.expandedPosition >= 0) {
							adapter.notifyItemChanged(adapter.expandedPosition);
						}

						if (newState) {
							adapter.expandedPosition = holder.getAdapterPosition();
							adapter.notifyItemChanged(holder.getAdapterPosition());
						} else {
							adapter.expandedPosition = -1;
						}
					}
				}
			});
		}
	}

	private static class TitleBackgroundDrawable extends Drawable {
		private static final float HEIGHT = 12.0F;
		private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
		private final Path path = new Path();
		private final float density;
		private final FortChallengeBundleItemDefinition.DisplayStyle displayStyle;

		public TitleBackgroundDrawable(Context ctx, FortChallengeBundleItemDefinition.DisplayStyle displayStyle) {
			density = ctx.getResources().getDisplayMetrics().density;
			this.displayStyle = displayStyle;
		}

		@Override
		public void draw(@NonNull Canvas canvas) {
			Rect rect = getBounds();
			float yoff = rect.height() - HEIGHT * density;
			path.reset();
			path.moveTo(rect.width(), yoff + 0.0F + HEIGHT / 3.0F * density);
			path.lineTo(rect.width() / 2.0F - 2.0F * density, yoff);
			path.lineTo(rect.width() / 2.0F + 2.0F * density, yoff + HEIGHT / 2.0F * density);
			path.lineTo(0.0F, yoff + HEIGHT / 3.0F * density);
			path.lineTo(0.0F, rect.height());
			path.lineTo(rect.width(), rect.height());
			path.close();
			// TODO gradient
			paint.setShader(new LinearGradient(0.0F, 0.0F, rect.width(), 0.0F, displayStyle.AccentColor.asInt(), displayStyle.AccentColor.asInt(), Shader.TileMode.CLAMP));
			canvas.drawPath(path, paint);
		}

		@Override
		public void setAlpha(int alpha) {
			paint.setAlpha(alpha);
		}

		@Override
		public void setColorFilter(@Nullable ColorFilter colorFilter) {
			paint.setColorFilter(colorFilter);
		}

		@Override
		public int getOpacity() {
			return PixelFormat.OPAQUE;
		}
	}
}
