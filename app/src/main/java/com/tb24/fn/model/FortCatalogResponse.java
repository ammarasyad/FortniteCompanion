package com.tb24.fn.model;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.util.Date;

public class FortCatalogResponse {
	public Integer refreshIntervalHrs;
	public Integer dailyPurchaseHrs;
	public Date expiration;
	public Storefront[] storefronts;

	public static class Storefront {
		public String name;
		public CatalogEntry[] catalogEntries;
	}

	public static class CatalogEntry {
		//***CurrencyStorefront***
		public String offerId;
		public String devName;
		public String offerType;
		public Price[] prices;
		public String[] categories;
		public Integer dailyLimit;
		public Integer weeklyLimit;
		public Integer monthlyLimit;
		public String[] appStoreId;
		public Requirement[] requirements;
		public MetaInfo[] metaInfo;
		public String catalogGroup;
		public Integer catalogGroupPriority;
		public Integer sortPriority;
		public String title;
		public String shortDescription;
		public String description;
		public String displayAssetPath;
		public FortItemStack[] itemGrants;

		//***BRWeeklyStorefront***
		//TODO unknown content
		public JsonElement[] fulfillmentIds;
		//"meta":{
		//"BannerOverride":"NewStyle"
		//"StoreToastHeader":"Updated",
		//"StoreToastBody":"ItemIsBack"
		//}
		public JsonObject meta;
		public String matchFilter;
		public float filterWeight;
		public GiftInfo giftInfo;
		public boolean refundable;
		// com.epicgames.fortnite.core.game.fulfillments.BattlePassTierFulfillment
		public String fulfillmentClass;
	}

	public static class Price {
		public String currencyType;
		public String currencySubType;
		public Integer regularPrice;
		public Integer finalPrice;
		//nonexistent if not sale
		public String saleType;
		//date
		public Date saleExpiration;
		public Integer basePrice;
	}

	public static class Requirement {
		public String requirementType;
		public String requiredId;
		public Integer minQuantity;
	}

	public static class MetaInfo {
		public String key;
		public String value;
	}

	public static class GiftInfo {
		public boolean bIsEnabled;
		public String forcedGiftBoxTemplateId;
		public JsonElement[] purchaseRequirements;
		public JsonElement[] giftRecordIds;
	}
}