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
	public Map<String, FortMcpProfile> profileData = new HashMap<>();
	private final FortniteCompanionApp app;

	public ProfileManager(FortniteCompanionApp app) {
		this.app = app;
	}

	public void executeProfileChanges(FortMcpResponse response) {
		if (response.profileChanges != null) {
			for (JsonObject obj : response.profileChanges) {
				String changeType = JsonUtils.getStringOr("changeType", obj, "");

				if (changeType.equals("fullProfileUpdate")) {
					FortMcpProfile parsed = app.gson.fromJson(obj.get("profile"), FortMcpProfile.class);
					profileData.put(response.profileId, parsed);
					app.eventBus.post(new ProfileUpdatedEvent(response.profileId, parsed));
					Log.d("MCP-Profile", String.format("Full profile update (rev=%d, version=%s@w=%d) for %s accountId=MCP:%s profileId=%s", parsed.rvn, parsed.version, parsed.wipeNumber, app.currentLoggedIn == null ? "" : app.currentLoggedIn.getDisplayName(), parsed.accountId, parsed.profileId));
				} else if (changeType.equals("itemAdded")) {
					if (!profileData.containsKey(response.profileId)) {
						return;
					}

					FortMcpProfile profile = profileData.get(response.profileId);
					profile.items.put(obj.get("itemId").getAsString(), app.gson.fromJson(obj.get("item"), FortItemStack.class));
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

	public Call<FortMcpResponse> requestFullProfileUpdate(final String profileId) {
		final Call<FortMcpResponse> call = app.fortnitePublicService.mcp("QueryProfile", PreferenceManager.getDefaultSharedPreferences(app).getString("epic_account_id", ""), profileId, -1, new JsonObject());
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
}
