package com.tb24.fn.model;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.util.Date;

public class EventDownloadResponse {
	public AccountCompetitiveData player;
	public Event[] events;
	public Template[] templates;
	public JsonElement[] scores;

	public static class Event {
		public String gameId;
		public String eventId;
		public String[] regions;
		public JsonObject regionMappings;
		public String[] platforms;
		public JsonObject platformMappings;
		public String displayDataId;
		public Date announcementTime;
		// JsonObject if not null
		public JsonElement metadata;
		public EventWindow[] eventWindows;
		public Date beginTime;
		public Date endTime;

		public FortBasicDataResponse.TournamentDisplayInfo findDisplayInfo(FortBasicDataResponse basicDataResponse) {
			for (FortBasicDataResponse.TournamentDisplayInfo tournamentDisplayInfo : basicDataResponse.tournamentinformation.tournament_info.tournaments) {
				if (displayDataId.equals(tournamentDisplayInfo.tournament_display_id)) {
					return tournamentDisplayInfo;
				}
			}

			return null;
		}
	}

	public static class EventWindow {
		public String eventWindowId;
		public String eventTemplateId;
		public Date countdownBeginTime;
		public Date beginTime;
		public Date endTime;
		public JsonElement[] blackoutPeriods;
		public int round;
		public String leaderboardId;
		public int payoutDelay;
		public boolean isTBD;
		// locked, public
		public String visibility;
		public String[] requireAllTokens;
		public String[] requireAnyTokens;
		public String[] requireNoneTokensCaller;
		public String[] requireAllTokensCaller;
		public String[] requireAnyTokensCaller;
		public String[] additionalRequirements;
		public String teammateEligibility;
		// JsonObject if not null
		public JsonElement metadata;

		public Template findTemplate(EventDownloadResponse data) {
			for (Template template : data.templates) {
				if (eventTemplateId.equals(template.eventTemplateId)) {
					return template;
				}
			}

			return null;
		}
	}

	public static class Template {
		public String gameId;
		public String eventTemplateId;
		public String playlistId;
		public int matchCap;
		public boolean useIndividualScores;
		// null, Hype
		public String persistentScoreId;
		public ScoringRule[] scoringRules;
		public TiebreakerFormula tiebreakerFormula;
		public PayoutTableEntry[] payoutTable;
	}

	public static class ScoringRule {
		// PLACEMENT_STAT_INDEX, TEAM_ELIMS_STAT_INDEX
		public String trackedStat;
		// lte, gte
		public String matchRule;
		public RewardTier[] rewardTiers;
	}

	public static class RewardTier {
		public int keyValue;
		public int pointsEarned;
		public boolean multiplicative;
	}

	public static class TiebreakerFormula {
		public int basePointBits;
		public TiebreakerFormulaComponent[] components;
	}

	public static class TiebreakerFormulaComponent {
		// VICTORY_ROYALE_STAT, TEAM_ELIMS_STAT_INDEX, PLACEMENT_TIEBREAKER_STAT
		public String trackedStat;
		public int bits;
		public float multiplier;
		// sum, avg
		public String aggregation;
	}

	public static class PayoutTableEntry {
		// null, Hype
		public String persistentScoreId;
		// rank, value
		public String scoringType;
		public PayoutRank[] ranks;
	}

	public static class PayoutRank {
		public float threshold;
		public Payout[] payouts;
	}

	public static class Payout {
		public String rewardType;
		public String rewardMode;
		public String value;
		public int quantity;
	}
}
