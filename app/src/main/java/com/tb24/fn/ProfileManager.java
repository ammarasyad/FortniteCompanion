package com.tb24.fn;

import android.preference.PreferenceManager;
import android.util.Log;

import com.google.gson.JsonObject;
import com.tb24.fn.event.ProfileQueryFailedEvent;
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
	private static final String TAG = "MCP-Profile";
	private Map<String, FortMcpProfile> profileData = new HashMap<>();
	private Map<String, Integer> profileRevisions = new HashMap<>();
	private final FortniteCompanionApp app;

	public ProfileManager(FortniteCompanionApp app) {
		this.app = app;
	}

	public void executeProfileChanges(FortMcpResponse response) {
		profileRevisions.put(response.profileId, response.profileRevision);

		if (response.profileChanges != null) {
			for (JsonObject profileChangeEntryObj : response.profileChanges) {
				String changeType = JsonUtils.getStringOr("changeType", profileChangeEntryObj, "");
				FortMcpProfile profile;

				if (changeType.equals("fullProfileUpdate")) {
					profile = app.gson.fromJson(profileChangeEntryObj.get("profile"), FortMcpProfile.class);
					profileData.put(response.profileId, profile);
					Log.i(TAG, String.format("Full profile update (rev=%d, version=%s@w=%d) for %s accountId=MCP:%s profileId=%s", profile.rvn, profile.version, profile.wipeNumber, app.currentLoggedIn == null ? "" : app.currentLoggedIn.getDisplayName(), profile.accountId, profile.profileId));
				} else {
					if (hasProfileData(response.profileId)) {
						profile = getProfileData(response.profileId);
						switch (changeType) {
							case "itemAdded":
								FortItemStack itemToAdd = app.gson.fromJson(profileChangeEntryObj.get("item"), FortItemStack.class);
								profile.items.put(JsonUtils.getStringOr("itemId", profileChangeEntryObj, null), itemToAdd);
								Log.i(TAG, String.format("%s accountId=MCP:%s profileId=%s gained %s", app.currentLoggedIn == null ? "" : app.currentLoggedIn.getDisplayName(), profile.accountId, profile.profileId, itemToAdd.toString()));
								break;
							case "itemRemoved":
								FortItemStack itemToRemove = profile.items.get(JsonUtils.getStringOr("itemId", profileChangeEntryObj, null));

								if (itemToRemove != null) {
									profile.items.remove(JsonUtils.getStringOr("itemId", profileChangeEntryObj, null));
									Log.i(TAG, String.format("%s accountId=MCP:%s profileId=%s lost %s", app.currentLoggedIn == null ? "" : app.currentLoggedIn.getDisplayName(), profile.accountId, profile.profileId, itemToRemove.toString()));
								} else {
									Log.w(TAG, "itemRemoved: Item ID " + JsonUtils.getStringOr("itemId", profileChangeEntryObj, null) + " not found");
								}

								break;
							case "itemQuantityChanged":
								FortItemStack itemQuantityToChange = profile.items.get(JsonUtils.getStringOr("itemId", profileChangeEntryObj, null));

								if (itemQuantityToChange != null) {
									if (itemQuantityToChange.attributes == null) {
										itemQuantityToChange.attributes = new JsonObject();
									}

									itemQuantityToChange.quantity = JsonUtils.getIntOr("quantity", profileChangeEntryObj, -1);
								} else {
									Log.w(TAG, "itemQuantityChanged: Item ID " + JsonUtils.getStringOr("itemId", profileChangeEntryObj, null) + " not found");
								}

								break;
							case "itemAttrChanged":
								FortItemStack itemAttrToChange = profile.items.get(JsonUtils.getStringOr("itemId", profileChangeEntryObj, null));

								if (itemAttrToChange != null) {
									if (itemAttrToChange.attributes == null) {
										itemAttrToChange.attributes = new JsonObject();
									}

									itemAttrToChange.attributes.add(JsonUtils.getStringOr("attributeName", profileChangeEntryObj, null), profileChangeEntryObj.get("attributeValue"));
								} else {
									Log.w(TAG, "itemAttrChanged: Item ID " + JsonUtils.getStringOr("itemId", profileChangeEntryObj, null) + " not found");
								}

								break;
							case "statModified":
								profile.stats.attributes.add(JsonUtils.getStringOr("name", profileChangeEntryObj, null), profileChangeEntryObj.get("value"));
								profile.reserializeAttrObject();
								break;
							default:
								Log.w(TAG, "Unknown change type '" + changeType + "'. If you're reading this, please inform the author of this app.");
								break;
						}
					} else {
						Log.w(TAG, "Change type '" + changeType + "' isn't Full Profile Update and Profile ID '" + response.profileId + "' haven't performed a Full Profile Update. This is definitely a bug. If you're reading this, please inform the author of this app.");
					}
				}
			}

			if (hasProfileData(response.profileId)) {
				app.eventBus.post(new ProfileUpdatedEvent(response.profileId, getProfileData(response.profileId)));
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
						app.eventBus.post(new ProfileQueryFailedEvent(profileId, EpicError.parse(execute)));
					}
				} catch (IOException e) {
					Log.e(TAG, "Failed requesting profile update " + profileId, e);
					app.eventBus.post(new ProfileQueryFailedEvent(profileId, e));
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
		profileRevisions.clear();
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
