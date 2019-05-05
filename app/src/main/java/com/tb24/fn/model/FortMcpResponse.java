package com.tb24.fn.model;

import com.google.gson.JsonObject;

import java.util.Date;
import java.util.List;
import java.util.Map;

public class FortMcpResponse {
	public int profileRevision;
	public String profileId;
	public String profileChangesBaseRevision;
	public List<McpProfileChangeEntry> profileChanges;
	public int profileCommandRevision;
	public Date serverTime;
	public String responseVersion;

	public static class McpProfileChangeEntry {
		public String changeType;
		public McpProfile profile;
	}

	public static class McpProfile {
		public String _id;
		public Date created;
		public Date updated;
		public int rvn;
		public int wipeNumber;
		public String accountId;
		public String profileId;
		public String version;
		public Map<String, FortItemStack> items;
		public Stats stats;
		public int commandRevision;
	}

	public static class Stats {
		public JsonObject attributes;
	}
}
