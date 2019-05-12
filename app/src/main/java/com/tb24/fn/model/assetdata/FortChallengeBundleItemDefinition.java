package com.tb24.fn.model.assetdata;

import com.google.gson.JsonObject;

public class FortChallengeBundleItemDefinition {
	public QuestInfo[] QuestInfos;
	public BundleCompletionReward[] BundleCompletionRewards;
	public DisplayStyle DisplayStyle;
	public String DisplayName;
	public AssetReference SmallPreviewImage;
	public AssetReference LargePreviewImage;

	public static class BundleCompletionReward {
		public Integer CompletionCount;
		public Reward[] Rewards;
	}

	public static class DisplayStyle {
		public FloatColor PrimaryColor;
		public FloatColor SecondaryColor;
		public FloatColor AccentColor;
		public AssetReference DisplayImage;
	}

	public static class QuestInfo {
		public AssetReference QuestDefinition;
		public String QuestUnlockType;
		public Integer UnlockValue;
		public RewardGiftBox RewardGiftBox;
	}

	public static class Reward {
		public AssetReference ItemDefinition;
		public String TemplateId;
		public Integer quantity;
		public RewardGiftBox RewardGiftBox;
		public Boolean IsChaseReward;
		public String RewardType;
	}

	public static class RewardGiftBox {
		public AssetReference GiftBoxToUse;
		public JsonObject[] GiftBoxFormatData;
	}
}
