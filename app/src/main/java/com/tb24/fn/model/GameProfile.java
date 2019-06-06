package com.tb24.fn.model;

import com.google.gson.annotations.SerializedName;

import java.util.Map;

public class GameProfile {
	@SerializedName("id")
	private String id;
	@SerializedName("displayName")
	private String displayName;
	@SerializedName("externalAuths")
	private Map<String, ExternalAuth> externalAuths;

	public GameProfile() {
	}

	public GameProfile(String id, String displayName) {
		setId(id);
		setDisplayName(displayName);
	}

	public String getId() {
		return id;
	}

	public void setId(String id) {
		if (id == null) {
			this.id = null;
			return;
		}

		if (id.length() != 32) throw new IllegalArgumentException("length != 32");
		this.id = id;
	}

	public String getDisplayName() {
		return displayName;
	}

	public void setDisplayName(String displayName) {
		if (displayName == null) {
			this.displayName = null;
			return;
		}

		if (displayName.length() < 3) throw new IllegalArgumentException("length < 3");
		this.displayName = displayName;
	}

	public Map<String, ExternalAuth> getExternalAuths() {
		return externalAuths;
	}

	@Override
	public String toString() {
		return "GameProfile{" +
				"id='" + id + '\'' +
				", displayName='" + displayName + '\'' +
				'}';
	}
}
