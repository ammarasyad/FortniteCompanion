package com.tb24.fn.model.assetdata;

import com.google.gson.JsonObject;

public class FortChallengeBundleItemDefinition {
	public QuestInfo[] QuestInfos;
	public BundleCompletionReward[] BundleCompletionRewards;
	public DisplayStyle DisplayStyle;
	public String DisplayName;
	public Asset SmallPreviewImage;
	public Asset LargePreviewImage;

	public static class BundleCompletionReward {
		public Integer CompletionCount;
		public Reward[] Rewards;
	}

	public static class DisplayImage {
		public String asset_path_name;
		public String sub_path_string;
	}

	public static class DisplayStyle {
		public FloatColor PrimaryColor;
		public FloatColor SecondaryColor;
		public FloatColor AccentColor;
		public Asset DisplayImage;
	}

	public static class QuestInfo {
		public Asset QuestDefinition;
		public String QuestUnlockType;
		public Integer UnlockValue;
		public RewardGiftBox RewardGiftBox;
	}

	public static class Reward {
		public Asset ItemDefinition;
		public String TemplateId;
		public Integer quantity;
		public RewardGiftBox RewardGiftBox;
		public Boolean IsChaseReward;
		public String RewardType;
	}

	public static class RewardGiftBox {
		public Asset GiftBoxToUse;
		public JsonObject[] GiftBoxFormatData;
	}
}
