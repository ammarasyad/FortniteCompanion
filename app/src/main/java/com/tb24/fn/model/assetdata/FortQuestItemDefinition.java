package com.tb24.fn.model.assetdata;

import com.google.gson.JsonElement;
import com.tb24.fn.model.FortItemStack;

public class FortQuestItemDefinition extends FortItemDefinition {
	public String QuestType;
	public Boolean bShouldDisplayOverallQuestInformation;
	public Boolean bAthenaMustCompleteInSingleMatch;
	public Boolean bIncludedInCategories;
	public Integer ObjectiveCompletionCount;
	public Reward[] Rewards;
	public Objective[] Objectives;
	public DataTableRowReference Category;
	public Float Weight;
	public String CompletionText;
	public String GrantToProfileType;

	public static class Reward {
		public ItemAsset ItemPrimaryAssetId;
		public Integer Quantity;
		private FortItemStack stack;

		public FortItemStack asItemStack() {
			return stack == null ? stack = new FortItemStack(ItemPrimaryAssetId.PrimaryAssetType.Name, ItemPrimaryAssetId.PrimaryAssetName, Quantity) : stack;
		}
	}

	public static class Objective {
		public String BackendName;
		public DataTableRowReference ObjectiveStatHandle;
		public JsonElement[] AlternativeStatHandles;
		public String ItemEvent;
		public Boolean bHidden;
		public Boolean bRequirePrimaryMissionCompletion;
		public Boolean bCanProgressInZone;
		public Boolean bDisplayDynamicAnnouncementUpdate;
		public String DynamicStatusUpdateType;
		public String LinkVaultTab;
		public String LinkToItemManagement;
		public AssetReference ItemReference;
		public String ItemTemplateIdOverride;
		public String LinkSquadID;
		public Integer LinkSquadIndex;
		public String Description;
		public String HudShortDescription;
		public AssetReference HudIcon;
		public Integer Count;
		public Integer Stage;
		public Integer DynamicStatusUpdatePercentInterval;
		public Float DynamicUpdateCompletionDelay;
		public AssetReference ScriptedAction;
	}

	public static class DataTableRowReference {
		public String DataTable;
		public String RowName;
	}
}
