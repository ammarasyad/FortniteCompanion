package com.tb24.fn.model.assetdata;

public class FortChallengeBundleItemDefinition extends FortItemDefinition {
	public QuestInfo[] QuestInfos;
	public String CalendarEventTag;
	public BundleCompletionReward[] BundleCompletionRewards;
	public DisplayStyle DisplayStyle;
	public AssetReference BundleHidenImageMaterial;
	public String UniqueLockedMessage;

	public static class BundleCompletionReward {
		public Integer CompletionCount;
		public Reward[] Rewards;
	}

	public static class DisplayStyle {
		public FloatColor PrimaryColor;
		public FloatColor SecondaryColor;
		public FloatColor AccentColor;
		public AssetReference DisplayImage;
		public AssetReference CustomBackground;
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
		public Integer Quantity;
		public RewardGiftBox RewardGiftBox;
		public Boolean IsChaseReward;
		public String RewardType;
	}

	public static class RewardGiftBox {
		public AssetReference GiftBoxToUse;
		public StringAsset[] GiftBoxFormatData;
	}

	public static class StringAsset {
		public String StringAssetType;
		public String StringData;
	}
}
