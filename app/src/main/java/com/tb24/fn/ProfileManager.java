package com.tb24.fn;

import android.preference.PreferenceManager;
import android.util.Log;

import com.google.gson.JsonObject;
import com.tb24.fn.event.ProfileUpdateFailedEvent;
import com.tb24.fn.event.ProfileUpdatedEvent;
import com.tb24.fn.model.EpicError;
import com.tb24.fn.model.FortItemStack;
import com.tb24.fn.model.FortMcpProfile;
import com.tb24.fn.model.FortMcpResponse;
import com.tb24.fn.util.JsonUtils;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Response;

public class ProfileManager {
	private Map<String, FortMcpProfile> profileData = new HashMap<>();
	private Map<String, Integer> profileRevisions = new HashMap<>();
	private final FortniteCompanionApp app;

	public ProfileManager(FortniteCompanionApp app) {
		this.app = app;
	}

	public void executeProfileChanges(FortMcpResponse response) {
		profileRevisions.put(response.profileId, response.profileRevision);

		if (response.profileChanges != null) {
			for (JsonObject obj : response.profileChanges) {
				String changeType = JsonUtils.getStringOr("changeType", obj, "");
				FortMcpProfile profile;

				if (changeType.equals("fullProfileUpdate")) {
					profile = app.gson.fromJson(obj.get("profile"), FortMcpProfile.class);
					profileData.put(response.profileId, profile);
					Log.d("MCP-Profile", String.format("Full profile update (rev=%d, version=%s@w=%d) for %s accountId=MCP:%s profileId=%s", profile.rvn, profile.version, profile.wipeNumber, app.currentLoggedIn == null ? "" : app.currentLoggedIn.getDisplayName(), profile.accountId, profile.profileId));
				} else {
					if (!hasProfileData(response.profileId)) {
						return;
					}

					profile = getProfileData(response.profileId);
					switch (changeType) {
						case "itemAdded":
							profile.items.put(obj.get("itemId").getAsString(), app.gson.fromJson(obj.get("item"), FortItemStack.class));
							app.eventBus.post(new ProfileUpdatedEvent(response.profileId, profile));
							break;
						case "itemRemoved":
							profile.items.remove(obj.get("itemId").getAsString());
							app.eventBus.post(new ProfileUpdatedEvent(response.profileId, profile));
							break;
						case "itemAttrChanged":
							profile.items.get(obj.get("itemId").getAsString()).attributes.add(obj.get("variants").getAsString(), obj.get("attributeValue"));
							break;
						case "statModified":
							profile.stats.attributes.add(obj.get("name").getAsString(), obj.get("value"));
							profile.reserializeAttrObject();
							break;
					}
				}

				if (profile != null) {
					app.eventBus.post(new ProfileUpdatedEvent(response.profileId, profile));
				}
			}
		}

		if (response.multiUpdate != null) {
			for (FortMcpResponse multiUpdateEntry : response.multiUpdate) {
				executeProfileChanges(multiUpdateEntry);
			}
		}
	}

	public Call<FortMcpResponse> requestProfileUpdate(final String profileId) {
		final Call<FortMcpResponse> call = app.fortnitePublicService.mcp(
				"QueryProfile",
				PreferenceManager.getDefaultSharedPreferences(app).getString("epic_account_id", ""),
				profileId,
				getRvn(profileId),
				new JsonObject());
		new Thread() {
			@Override
			public void run() {
				try {
					Response<FortMcpResponse> execute = call.execute();

					if (execute.isSuccessful()) {
						executeProfileChanges(execute.body());
					} else {
						app.eventBus.post(new ProfileUpdateFailedEvent(profileId, EpicError.parse(execute)));
					}
				} catch (IOException e) {
					Log.e("MCP-Profile", "Failed requesting profile update " + profileId, e);
					app.eventBus.post(new ProfileUpdateFailedEvent(profileId, e));
				}
			}
		}.start();
		return call;
	}

	public FortMcpProfile getProfileData(String profileId) {
		return profileData.get(profileId);
	}

	public boolean hasProfileData(String profileId) {
		return profileData.containsKey(profileId);
	}

	public void clearProfileData() {
		profileData.clear();
	}

	public int getRvn(String profileId) {
		int out = -1;
		Integer revision = profileRevisions.get(profileId);

		if (revision != null) {
			out = revision;
		}

		return out;
	}
}
