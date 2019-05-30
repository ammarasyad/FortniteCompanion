package com.tb24.fn.event;

import com.tb24.fn.model.EpicError;

public class ProfileQueryFailedEvent {
	public String profileId;
	public EpicError error;
	public Throwable throwable;

	public ProfileQueryFailedEvent(String profileId, EpicError error) {
		this.profileId = profileId;
		this.error = error;
	}

	public ProfileQueryFailedEvent(String profileId, Throwable throwable) {
		this.profileId = profileId;
		this.throwable = throwable;
	}
}
