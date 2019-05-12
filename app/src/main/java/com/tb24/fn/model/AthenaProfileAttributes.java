package com.tb24.fn.model;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.util.Date;

public class AthenaProfileAttributes extends ProfileAttributes {
	public AthenaPastSeasonData[] past_seasons;
	public Integer season_match_boost;
	public String favorite_victorypose;
	public Boolean mfa_reward_claimed;
	//	"quest_manager":{
//		"dailyLoginInterval":"2019-04-11T05:02:50.959Z",
//		"dailyQuestRerolls":1
//	},
	public QuestManager quest_manager;
	public Integer book_level;
	public Integer season_num;
	public String favorite_consumableemote;
	public String banner_color;
	public String favorite_callingcard;
	public String favorite_character;
	public String[] favorite_spray;
	public Integer book_xp;
	public String favorite_loadingscreen;
	public JsonElement[] permissions;
	public WithStat season;
	public String[] favorite_itemwraps;
	public Integer lifetime_wins;
	public Boolean book_purchased;
	public String party_assist_quest;
	public String favorite_hat;
	//[{
	//"id":"v2:/6c7a67b164ba9dc6abc4351404189766264834de74b1d2364bc8a4d12c6e1e70season6_v2",
	//"count":1
	//}]
	// or {} if never bought
	public JsonElement purchased_battle_pass_tier_offers;
	public Integer level;
	public String favorite_battlebus;
	public String favorite_mapmarker;
	public String favorite_vehicledeco;
	public Integer accountLevel;
	public JsonObject competitiveIdentity;
	public String favorite_backpack;
	public String[] favorite_dance;
	public Integer inventory_limit_bonus;
	public String favorite_skydivecontrail;
	public String favorite_pickaxe;
	public String favorite_glider;
	public JsonObject daily_rewards;
	public Integer xp;
	public Integer season_friend_match_boost;
	public String favorite_musicpack;
	public String banner_icon;

	public static class WithStat {
		public Integer numWins;
		public Integer numHighBracket;
		public Integer numLowBracket;
	}

	public static class AthenaPastSeasonData extends WithStat {
		public Integer seasonNumber;
		public Integer seasonXp;
		public Integer seasonLevel;
		public Integer bookXp;
		public Integer bookLevel;
		public Boolean purchasedVIP;
	}

	public static class QuestManager {
		public Date dailyLoginInterval;
		public Integer dailyQuestRerolls;
	}
}
