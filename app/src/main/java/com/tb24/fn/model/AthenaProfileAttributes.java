package com.tb24.fn.model;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.util.Date;
import java.util.List;

public class AthenaProfileAttributes {
	public List<AthenaPastSeasonData> past_seasons;
	public int season_match_boost;
	public String favorite_victorypose;
	public boolean mfa_reward_claimed;
	//	"quest_manager":{
//		"dailyLoginInterval":"2019-04-11T05:02:50.959Z",
//		"dailyQuestRerolls":1
//	},
	public QuestManager quest_manager;
	public int book_level;
	public int season_num;
	public String favorite_consumableemote;
	public String banner_color;
	public String favorite_callingcard;
	public String favorite_character;
	public List<JsonElement> favorite_spray;
	public int book_xp;
	public String favorite_loadingscreen;
	public WithStat season;
	public List<String> favorite_itemwraps;
	public int lifetime_wins;
	public boolean book_purchased;
	public String party_assist_quest;
	public String favorite_hat;
	//[{
	//"id":"v2:/6c7a67b164ba9dc6abc4351404189766264834de74b1d2364bc8a4d12c6e1e70season6_v2",
	//"count":1
	//}]
	// or {} if never bought
	public JsonElement purchased_battle_pass_tier_offers;
	public int level;
	public String favorite_battlebus;
	public String favorite_mapmarker;
	public String favorite_vehicledeco;
	public int accountLevel;
	public String favorite_backpack;
	public List<String> favorite_dance;
	public int inventory_limit_bonus;
	public String favorite_skydivecontrail;
	public String favorite_pickaxe;
	public String favorite_glider;
	public JsonObject daily_rewards;
	public int xp;
	public int season_friend_match_boost;
	public String favorite_musicpack;
	public String banner_icon;


	public static class WithStat {
		public int numWins;
		public int numHighBracket;
		public int numLowBracket;
	}

	public static class AthenaPastSeasonData extends WithStat {
		public int seasonNumber;
		public int seasonXp;
		public int seasonLevel;
		public int bookXp;
		public int bookLevel;
		public boolean purchasedVIP;
	}

	public static class QuestManager {
		public Date dailyLoginInterval;
		public int dailyQuestRerolls;
	}
}
