package com.tb24.fn.model;

import com.google.gson.JsonObject;
import com.google.gson.annotations.SerializedName;

import java.util.Date;

public class CalendarTimelineResponse {
	public Channels channels;
	public Float eventsTimeOffsetHrs;
	public Date currentTime;

	public static class Channels {
		@SerializedName("client-matchmaking")
		public States clientMatchmaking;
		public States tk;
		@SerializedName("client-events")
		public States clientEvents;
	}

	public static class States {
		public State[] states;
		public Date cacheExpire;
	}

	public static class State {
		public Date validFrom;
		public ActiveEvent[] activeEvents;
		public JsonObject state;
	}

	public static class ClientEventState {
		public String[] activeStorefronts;
		public JsonObject eventNamedWeights;
		public ActiveEvent2[] activeEvents;
		public Integer seasonNumber;
		public String seasonTemplateId;
		public Integer matchXpBonusPoints;
		public Date seasonBegin;
		public Date seasonEnd;
		public Date seasonDisplayedEnd;
		public Date weeklyStoreEnd;
		public Date stwEventStoreEnd;
		public Date stwWeeklyStoreEnd;
		public Date dailyStoreEnd;
	}

	public static class ActiveEvent2 {
		public String instanceId;
		public String devName;
		public String eventName;
		public String eventStart;
		public String eventEnd;
		public String eventType;
	}

	public static class ActiveEvent {
		public String eventType;
		public Date activeUntil;
		public Date activeSince;
	}
}
