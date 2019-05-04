package com.tb24.fn.model;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.util.Date;

public class FortCatalogResponse {
	public int refreshIntervalHrs;
	public int dailyPurchaseHrs;
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
		public int dailyLimit;
		public int weeklyLimit;
		public int monthlyLimit;
		public String[] appStoreId;
		public Requirement[] requirements;
		public MetaInfo[] metaInfo;
		public String catalogGroup;
		public int catalogGroupPriority;
		public int sortPriority;
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
		public int regularPrice;
		public int finalPrice;
		//nonexistent if not sale
		public String saleType;
		//date
		public Date saleExpiration;
		public int basePrice;
	}

	public static class Requirement {
		public String requirementType;
		public String requiredId;
		public int minQuantity;
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