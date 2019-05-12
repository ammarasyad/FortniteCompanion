package com.tb24.fn.event;

import com.tb24.fn.model.EpicError;

public class ProfileUpdateFailedEvent {
	public String profileId;
	public EpicError error;
	public Throwable throwable;

	public ProfileUpdateFailedEvent(String profileId, EpicError error) {
		this.profileId = profileId;
		this.error = error;
	}

	public ProfileUpdateFailedEvent(String profileId, Throwable throwable) {
		this.profileId = profileId;
		this.throwable = throwable;
	}
}
