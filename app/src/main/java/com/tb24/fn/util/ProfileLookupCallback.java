package com.tb24.fn.util;

import com.tb24.fn.model.GameProfile;

public interface ProfileLookupCallback {
	void onProfileLookupSucceeded(GameProfile profile);

	void onProfileLookupFailed(GameProfile profile, Exception exception);
}
