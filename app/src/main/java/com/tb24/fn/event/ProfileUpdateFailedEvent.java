package com.tb24.fn.event;

import com.tb24.fn.model.EpicError;

public class ProfileUpdateFailedEvent {
	public final String profileId;
	public final EpicError error;

	public ProfileUpdateFailedEvent(String profileId, EpicError error) {
		this.profileId = profileId;
		this.error = error;
	}
}
