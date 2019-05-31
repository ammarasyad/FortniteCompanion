package com.tb24.fn.model;

import com.google.gson.JsonObject;

import java.util.Date;

public class FortMcpResponse {
	public Integer profileRevision;
	public String profileId;
	public Integer profileChangesBaseRevision;
	public JsonObject[] profileChanges;
	public JsonObject[] notifications;
	public Integer profileCommandRevision;
	public Date serverTime;
	public FortMcpResponse[] multiUpdate;
	public String responseVersion;
}
