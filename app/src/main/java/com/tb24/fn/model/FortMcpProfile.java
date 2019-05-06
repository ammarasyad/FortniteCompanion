package com.tb24.fn.model;

import com.google.gson.Gson;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.annotations.Expose;

import java.lang.reflect.Type;
import java.util.Date;
import java.util.Map;

public class FortMcpProfile {
	public String _id;
	public Date created;
	public Date updated;
	public int rvn;
	public int wipeNumber;
	public String accountId;
	public String profileId;
	public String version;
	public Map<String, FortItemStack> items;
	@Expose(deserialize = false, serialize = false)
	public FortMcpProfile.Stats stats;
	public int commandRevision;

	public static class Stats {
		public JsonObject attributes;
		public ProfileAttributes attributesObj;
	}

	public static class Serializer implements JsonDeserializer<FortMcpProfile> {
		@Override
		public FortMcpProfile deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
			FortMcpProfile profile = new Gson().fromJson(json, FortMcpProfile.class);
			JsonObject profileJson = profile.stats.attributes;
			ProfileAttributes profileAttributes;

			switch (profile.profileId) {
				case "common_core":
					profileAttributes = context.deserialize(profileJson, CommonCoreProfileAttributes.class);
					break;
				case "athena":
					profileAttributes = context.deserialize(profileJson, AthenaProfileAttributes.class);
					break;
				default:
					profileAttributes = new ProfileAttributes();
			}

			profile.stats.attributesObj = profileAttributes;
			return profile;
		}
	}

}
