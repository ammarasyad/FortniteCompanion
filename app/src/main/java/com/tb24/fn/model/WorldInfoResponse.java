package com.tb24.fn.model;

import com.google.gson.JsonObject;

import java.util.Date;

public class WorldInfoResponse {
	public TheaterData[] theaters;
	public MissionData[] missions;
	public MissionAlertData[] missionAlerts;

	//theaters
	public static class TheaterData {
		public String displayName;
		public String uniqueId;
		public Integer theaterSlot;
		public Boolean bIsTestTheater;
		public Boolean bHideLikeTestTheater;
		public String requiredEventFlag;
		public String missionRewardNamedWeightsRowName;
		public String description;
		public JsonObject runtimeInfo;
		public JsonObject[] tiles;
		public JsonObject[] regions;
	}
	//end theaters

	// missions
	public static class MissionData extends WithTheater {
		public Mission[] availableMissions;
		public Date nextRefresh;
	}

	public static class Mission {
		public String missionGuid;
		public TierGroup missionRewards;
		public TierGroup bonusMissionRewards;
		public String missionGenerator;
		public MissionDifficultyInfo missionDifficultyInfo;
		public Integer tileIndex;
		public Date availableUntil;
	}

	public static class MissionDifficultyInfo {
		public String dataTable;
		public String rowName;
	}
	//end missions

	//missionAlerts
	public static class MissionAlertData extends WithTheater {
		public MissionAlert[] availableMissionAlerts;
		public Date nextRefresh;
	}

	public static class MissionAlert {
		public String name;
		public String categoryName;
		public String spreadDataName;
		public String missionAlertGuid;
		public Integer tileIndex;
		public Date availableUntil;
		public Integer totalSpreadRefreshes;
		public TierGroup missionAlertRewards;
		public TierGroup missionAlertModifiers;
	}
	//end missionAlerts

	public static class TierGroup {
		public String tierGroupName;
		public WorldInfoItemStack[] items;
	}

	public static class WorldInfoItemStack {
		public String itemType;
		public Integer quantity;

		public String getIdCategory() {
			return itemType.split(":")[0];
		}

		public String getIdName() {
			return itemType.split(":")[1];
		}
	}

	public static class WithTheater {
		public String theaterId;

		public TheaterData findTheater(WorldInfoResponse data) {
			for (TheaterData entry : data.theaters) {
				if (theaterId.equals(entry.uniqueId)) {
					return entry;
				}
			}

			return null;
		}
	}
}
