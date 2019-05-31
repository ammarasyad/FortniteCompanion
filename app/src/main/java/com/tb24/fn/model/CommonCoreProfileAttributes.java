package com.tb24.fn.model;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.util.Date;
import java.util.Map;

public class CommonCoreProfileAttributes extends ProfileAttributes {
	public JsonObject survey_data;
	public JsonObject personal_offers;
	public JsonObject import_friends_claimed;
	public MtxPurchaseHistory mtx_purchase_history;
	public JsonArray undo_cooldowns;
	public Date mtx_affiliate_set_time;
	public Integer inventory_limit_bonus;
	public EMtxPlatform current_mtx_platform;
	public String mtx_affiliate;
	public PurchaseList weekly_purchases;
	public PurchaseList daily_purchases;
	public JsonObject ban_history;
	public InAppPurchases in_app_purchases;
	public JsonArray permissions;
	public Date undo_timeout;
	public PurchaseList monthly_purchases;
	public Boolean allowed_to_send_gifts;
	public Boolean mfa_enabled;
	public Boolean allowed_to_receive_gifts;
	public GiftHistory giftHistory;

	public static class MtxPurchaseHistory {
		public Integer refundsUsed;
		public Integer refundCredits;
		public Purchase[] purchases;
	}

	public static class Purchase {
		public String purchaseId;
		public String offerId;
		public Date purchaseDate;
		public JsonArray fulfillments;
		public LootResult lootResult;
		public Integer totalMtxPaid;
		// let's play safe, make it generic JsonElement, who knows it isn't a string
		public Map<String, JsonElement> metadata;
	}

	public static class LootResult {
		/**
		 * Template ID
		 */
		public String itemType;
		public String itemGuid;
		public String itemProfile;
		public Integer quantity;
	}

	public static class PurchaseList {
		public Date interval;
		public Map<String, Integer> purchaseList;
	}

	public static class InAppPurchases {
		public String[] receipts;
		public String[] ignoredReceipts;
		public Map<String, Integer> fulfillmentCounts;
	}

	public static class GiftHistory {
		public Integer num_sent;
		/**
		 * String: account ID, Date: date sent
		 */
		public Map<String, Date> sentTo;
		public Integer num_received;
		/**
		 * String: account ID, Date: date received
		 */
		public Map<String, Date> receivedFrom;
		public Gift[] gifts;
	}

	public static class Gift {
		public Date data;
		public String offerId;
		public String toAccountId;
	}
}
