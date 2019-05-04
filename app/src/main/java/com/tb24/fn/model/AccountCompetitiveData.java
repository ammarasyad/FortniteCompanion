package com.tb24.fn.model;

import com.google.gson.JsonObject;

public class AccountCompetitiveData {
	public String gameId;
	public String accountId;
	public String[] tokens;
	public JsonObject teams;
	public String[] pendingPayouts;
	//{Hype:42}
	public JsonObject persistentScores;
}
