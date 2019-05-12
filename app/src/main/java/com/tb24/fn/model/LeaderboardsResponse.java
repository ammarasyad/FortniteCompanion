package com.tb24.fn.model;

import com.google.gson.annotations.Expose;

import java.util.Date;
import java.util.List;
import java.util.Map;

public class LeaderboardsResponse {
	public String gameId;
	public String eventId;
	public String eventWindowId;
	public Integer page;
	public Integer totalPages;
	public List<LeaderboardEntry> entries;

	public static class LeaderboardEntry {
		public String gameId;
		public String eventId;
		public String eventWindowId;
		public String teamId;
		public String[] teamAccountIds;
		public String liveSessionId;
		public Integer pointsEarned;
		public Float score;
		public Integer rank;
		public Float percentile;
		public Map<String, PointBreakdown> pointBreakdown;
		public SessionHistory[] sessionHistory;
		public String[] tokens;
		@Expose(serialize = false, deserialize = false)
		public List<GameProfile> modifiedTeamAccountIds;
	}

	public static class SessionHistory {
		public String sessionId;
		public Date endTime;
		public Map<String, Integer> trackedStats;
	}

	public static class PointBreakdown {
		public Integer timesAchieved;
		public Integer pointsEarned;
	}
}
