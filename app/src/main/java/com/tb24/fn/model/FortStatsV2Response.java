package com.tb24.fn.model;

import com.google.gson.annotations.SerializedName;

import java.util.Map;

public class FortStatsV2Response {
	@SerializedName("accountId")
	public String accountId;
	@SerializedName("startTime")
	public long startTime;
	@SerializedName("endTime")
	public long endTime;
	@SerializedName("stats")
	public Map<String, Integer> stats;
}
