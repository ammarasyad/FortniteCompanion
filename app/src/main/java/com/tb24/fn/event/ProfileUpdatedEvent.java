package com.tb24.fn.event;

import com.tb24.fn.model.FortMcpProfile;

public class ProfileUpdatedEvent {
	public final String profileId;
	public final FortMcpProfile profileObj;

	public ProfileUpdatedEvent(String profileId, FortMcpProfile profileObj) {
		this.profileId = profileId;
		this.profileObj = profileObj;
	}
}
