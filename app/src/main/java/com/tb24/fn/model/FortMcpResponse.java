package com.tb24.fn.model;

import com.google.gson.JsonObject;

import java.util.Date;

public class FortMcpResponse {
	public int profileRevision;
	public String profileId;
	public String profileChangesBaseRevision;
	public JsonObject[] profileChanges;
	public JsonObject[] notifications;
	public int profileCommandRevision;
	public Date serverTime;
	public FortMcpResponse[] multiUpdate;
	public String responseVersion;
}
